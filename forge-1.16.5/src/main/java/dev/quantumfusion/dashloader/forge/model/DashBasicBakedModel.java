package dev.quantumfusion.dashloader.forge.model;

import dev.quantumfusion.dashloader.forge.mixin.accessor.ItemOverrideAccessor;
import dev.quantumfusion.dashloader.forge.mixin.accessor.ItemOverrideListAccessor;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverride;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

/**
 * Forge 1.16.5 port of modern {@code DashBasicBakedModel}
 * ({@code fabric-1.21.4}).
 *
 * <p>Yarn -&gt; MCP mapping:
 * <ul>
 *   <li>yarn {@code BasicBakedModel} -&gt; MCP {@code SimpleBakedModel} (same
 *       role: the default JSON-baked model implementation).</li>
 *   <li>yarn {@code BakedModel} -&gt; MCP {@code IBakedModel}.</li>
 *   <li>Field mapping: {@code usesAo -&gt; ambientOcclusion}
 *       ({@link SimpleBakedModel#isAmbientOcclusion()}),
 *       {@code hasDepth -&gt; gui3d} ({@link SimpleBakedModel#isGui3d()}),
 *       {@code isSideLit} unchanged,
 *       {@code transformation (ModelTransformation) -&gt; cameraTransforms
 *       (ItemCameraTransforms)}, {@code sprite -&gt; particle texture}
 *       ({@link SimpleBakedModel#getParticleTexture()}).</li>
 * </ul>
 *
 * <p>Snapshot/restore uses only public getters
 * ({@link IBakedModel#getQuads}, {@code isAmbientOcclusion}, ...), so no
 * accessor mixin is needed for this class. Quads are read with a
 * {@code null} state, mirroring the modern implementation (vanilla
 * {@code SimpleBakedModel} ignores the state argument).
 *
 * <p>Intentional simplifications (documented):
 * <ul>
 *   <li>{@code ItemCameraTransforms} is carried field-by-field as float
 *       triples via the Gson adapter ({@code CacheGson}).</li>
 *   <li>{@code ItemOverrideList} is preserved as predicate maps + target
 *       model-id strings (see {@link DashItemOverride}). Only overrides whose
 *       target resolves to an already-restored (cached) model are rebuilt;
 *       the rest are skipped with a warning (vanilla fallback for that
 *       override). Targets that are not staged top models are skipped at SAVE
 *       the same way.</li>
 * </ul>
 */
public final class DashBasicBakedModel {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-model");

    public final DashBakedQuadCollection generalQuads;
    public final Map<Direction, DashBakedQuadCollection> faceQuads;
    public final boolean ambientOcclusion;
    public final boolean gui3d;
    public final boolean sideLit;
    public final ResourceLocation particleSpriteId;
    /** Runtime object, serialized field-by-field via {@code CacheGson}. */
    public final ItemCameraTransforms cameraTransforms;
    /** Override entries (predicates + target model id); empty when none. Never null for new snapshots. */
    public final List<DashItemOverride> itemOverrides;

    public DashBasicBakedModel(DashBakedQuadCollection generalQuads,
            Map<Direction, DashBakedQuadCollection> faceQuads,
            boolean ambientOcclusion, boolean gui3d, boolean sideLit,
            ResourceLocation particleSpriteId,
            ItemCameraTransforms cameraTransforms,
            List<DashItemOverride> itemOverrides) {
        this.generalQuads = generalQuads;
        this.faceQuads = faceQuads;
        this.ambientOcclusion = ambientOcclusion;
        this.gui3d = gui3d;
        this.sideLit = sideLit;
        this.particleSpriteId = particleSpriteId;
        this.cameraTransforms = cameraTransforms;
        this.itemOverrides = itemOverrides == null
                ? new ArrayList<DashItemOverride>()
                : new ArrayList<DashItemOverride>(itemOverrides);
    }

    /** Legacy overload: no override context, overrides snapshot as empty. */
    public static DashBasicBakedModel toDash(SimpleBakedModel model) {
        return toDash(model, part -> {
            throw new IllegalArgumentException("No model-id context for overrides");
        });
    }

    /**
     * Snapshot a vanilla model, resolving override targets through
     * {@code modelIds}. Override targets that are not staged top models are
     * skipped with a warning (vanilla fallback for that override) instead of
     * failing the whole model.
     */
    public static DashBasicBakedModel toDash(SimpleBakedModel model,
            Function<IBakedModel, String> modelIds) {
        Random random = new Random();
        DashBakedQuadCollection general = DashBakedQuadCollection.toDash(model.getQuads(null, null, random));
        Map<Direction, DashBakedQuadCollection> faces = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(null, face, random);
            faces.put(face, DashBakedQuadCollection.toDash(quads));
        }
        ResourceLocation particleId = model.getParticleTexture() == null
                ? null
                : model.getParticleTexture().getName();
        List<DashItemOverride> overrides = snapshotOverrides(model, modelIds);
        return new DashBasicBakedModel(general, faces,
                model.isAmbientOcclusion(), model.isGui3d(), model.isSideLit(),
                particleId, model.getItemCameraTransforms(), overrides);
    }

    private static List<DashItemOverride> snapshotOverrides(SimpleBakedModel model,
            Function<IBakedModel, String> modelIds) {
        List<DashItemOverride> out = new ArrayList<>();
        ItemOverrideList list;
        try {
            list = model.getOverrides();
        } catch (Throwable t) {
            return out;
        }
        if (list == null || list == ItemOverrideList.EMPTY) {
            return out;
        }
        List<ItemOverride> vanilla;
        try {
            vanilla = list.getOverrides();
        } catch (Throwable t) {
            return out;
        }
        if (vanilla == null || vanilla.isEmpty()) {
            return out;
        }
        for (int i = 0; i < vanilla.size(); i++) {
            ItemOverride override = vanilla.get(i);
            if (override == null || override.getLocation() == null) {
                continue;
            }
            // Target id comes straight from the override (verified via javap:
            // getLocation() returns the model field), no instance-identity
            // lookup: baked override targets are often copies, not registry
            // instances, so identity matching misses them.
            String targetId = override.getLocation().toString();
            Map<ResourceLocation, Float> predicates;
            try {
                predicates = ((ItemOverrideAccessor) override).getPredicateMap();
            } catch (Throwable t) {
                LOGGER.warn("Skipping override with unreadable predicates for {}.", override.getLocation());
                continue;
            }
            Map<String, Float> stored = new java.util.LinkedHashMap<>();
            if (predicates != null) {
                for (Map.Entry<ResourceLocation, Float> e : predicates.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        stored.put(e.getKey().toString(), e.getValue());
                    }
                }
            }
            out.add(new DashItemOverride(stored, targetId));
        }
        return out;
    }

    /** Legacy overload: overrides resolve to {@link ItemOverrideList#EMPTY} (targets unknown). */
    public SimpleBakedModel toVanilla(Function<ResourceLocation, TextureAtlasSprite> spriteLookup) {
        return toVanilla(spriteLookup, key -> {
            throw new IllegalArgumentException("Referenced model not restored: " + key);
        });
    }

    /**
     * Rebuild a vanilla model, resolving override targets through
     * {@code models}. Unresolvable targets are skipped with a warning; when
     * none resolve the overrides reset to {@link ItemOverrideList#EMPTY}.
     */
    public SimpleBakedModel toVanilla(Function<ResourceLocation, TextureAtlasSprite> spriteLookup,
            Function<String, IBakedModel> models) {
        List<BakedQuad> general = generalQuads.toVanilla(spriteLookup);
        Map<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, DashBakedQuadCollection> entry : faceQuads.entrySet()) {
            faces.put(entry.getKey(), entry.getValue().toVanilla(spriteLookup));
        }
        // Ensure every face is present; vanilla expects a full map.
        for (Direction face : Direction.values()) {
            if (!faces.containsKey(face)) {
                faces.put(face, new ArrayList<BakedQuad>());
            }
        }
        TextureAtlasSprite particle = particleSpriteId == null
                ? null
                : spriteLookup.apply(particleSpriteId);
        ItemCameraTransforms transforms = cameraTransforms == null
                ? ItemCameraTransforms.DEFAULT
                : cameraTransforms;
        ItemOverrideList overrides = buildOverrides(models);
        return new SimpleBakedModel(general, faces,
                ambientOcclusion, gui3d, sideLit,
                particle, transforms, overrides);
    }

    private ItemOverrideList buildOverrides(Function<String, IBakedModel> models) {
        if (itemOverrides == null || itemOverrides.isEmpty()) {
            return ItemOverrideList.EMPTY;
        }
        List<ItemOverride> rebuilt = new ArrayList<>(itemOverrides.size());
        List<IBakedModel> targets = new ArrayList<>(itemOverrides.size());
        for (DashItemOverride entry : itemOverrides) {
            if (entry == null || entry.model == null) {
                continue;
            }
            IBakedModel target;
            try {
                target = models.apply(entry.model);
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping unrestorable override target {}: {}", entry.model, e.getMessage());
                continue;
            }
            if (target == null) {
                LOGGER.warn("Skipping unrestorable override target {}: resolved null.", entry.model);
                continue;
            }
            Map<ResourceLocation, Float> predicates = new java.util.LinkedHashMap<>();
            if (entry.predicates != null) {
                for (Map.Entry<String, Float> e : entry.predicates.entrySet()) {
                    try {
                        if (e.getKey() != null && e.getValue() != null) {
                            predicates.put(new ResourceLocation(e.getKey()), e.getValue());
                        }
                    } catch (RuntimeException ex) {
                        LOGGER.warn("Skipping bad override predicate {} for {}.", e.getKey(), entry.model);
                    }
                }
            }
            try {
                rebuilt.add(new ItemOverride(new ResourceLocation(entry.model), predicates));
                targets.add(target);
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping unrestorable override for {}: {}", entry.model, e.getMessage());
            }
        }
        if (rebuilt.isEmpty()) {
            return ItemOverrideList.EMPTY;
        }
        int restored = rebuilt.size();
        int skipped = itemOverrides.size() - restored;
        if (skipped > 0) {
            LOGGER.warn("Restored {}/{} item overrides (skipped {} without cached targets).",
                    restored, itemOverrides.size(), skipped);
        } else {
            LOGGER.debug("Restored {} item overrides.", restored);
        }
        return new RestoredItemOverrideList(rebuilt, targets);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashBasicBakedModel that = (DashBasicBakedModel) o;
        return ambientOcclusion == that.ambientOcclusion
                && gui3d == that.gui3d
                && sideLit == that.sideLit
                && Objects.equals(generalQuads, that.generalQuads)
                && Objects.equals(faceQuads, that.faceQuads)
                && Objects.equals(particleSpriteId, that.particleSpriteId)
                && Objects.equals(itemOverrides, that.itemOverrides);
    }

    @Override
    public int hashCode() {
        int result = generalQuads == null ? 0 : generalQuads.hashCode();
        result = 31 * result + (faceQuads == null ? 0 : faceQuads.hashCode());
        result = 31 * result + (ambientOcclusion ? 1 : 0);
        result = 31 * result + (gui3d ? 1 : 0);
        result = 31 * result + (sideLit ? 1 : 0);
        result = 31 * result + (particleSpriteId == null ? 0 : particleSpriteId.hashCode());
        result = 31 * result + (itemOverrides == null ? 0 : itemOverrides.hashCode());
        return result;
    }
}
