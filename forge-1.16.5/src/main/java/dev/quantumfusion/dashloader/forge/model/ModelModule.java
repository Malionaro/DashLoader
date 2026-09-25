package dev.quantumfusion.dashloader.forge.model;

import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import dev.quantumfusion.dashloader.forge.mixin.accessor.MultipartBakedModelAccessor;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import net.minecraft.client.renderer.model.SimpleBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import net.minecraft.client.renderer.model.multipart.Selector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Forge 1.16.5 port of modern {@code ModelModule}
 * ({@code fabric-1.21.4}).
 *
 * <p>What is kept from modern:
 * <ul>
  *   <li>Staging maps for the SAVE pass (populated from
  *       {@link ModelBakery#getTopBakedModels()}, the 1.16.5 counterpart of
  *       modern {@code ModelLoader} baked-model maps).</li>
 *   <li>{@code instanceof} dispatch over the three cacheable
 *       implementations (basic / multipart / weighted); anything else
 *       (modded models) lands on the missing list with a warning and falls
 *       back to vanilla — same policy as modern.</li>
 *   <li>A {@link Data} snapshot object holding per-kind Dash models plus
 *       missing-model ids.</li>
 * </ul>
 *
 * <p>What is simplified (Hyphen-out, documented):
 * <ul>
 *   <li>Modern splits unbaked parts vs block/item models with bake-setting
 *       variants; this port snapshots the <em>baked top models</em> keyed by
 *       model id, which is the 1.16.5-visible unit
 *       ({@code ModelBakery#getTopBakedModels()}).</li>
 *   <li>Model cross-references use plain {@code String} ids (registry id
 *       strings) instead of Hyphen int pointers. Multipart part models that
 *       are not staged top models themselves are snapshotted inline under
 *       synthetic ids ({@link #SYNTHETIC_PART_MARKER}); nested non-top-level
 *       models that cannot be snapshotted are reported missing (vanilla
 *       fallback).</li>
 *   <li>Multipart unbaked {@code Selector} lists are staged by
 *       {@code MultipartBakeMixin} (modern {@code MULTIPART_PREDICATES}
 *       parity) into {@link #SAVE_MULTIPART}; predicates are rebuilt from
 *       the staged condition trees on LOAD.</li>
 *   <li>Installing restored models back into the bakery happens in
 *       {@code ModelManagerCacheMixin} (TAIL of {@code apply} overwrites
 *       {@code modelRegistry} entries with {@link #buildLoadedModels});
 *       synthetic part entries are restored for reference resolution but
 *       skipped at install (see {@link #isSyntheticKey}).</li>
 * </ul>
 */
public final class ModelModule {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-model");

    /**
     * Marker in synthetic part-model ids (see {@link #save}): part models of
     * a multipart model that are not staged top models are snapshotted
     * inline as {@code <multipartId>/__dashloader_part_<n>}. Uses only
     * {@code ResourceLocation}-legal characters and can never collide with a
     * real model id (plus an explicit collision loop).
     */
    public static final String SYNTHETIC_PART_MARKER = "/__dashloader_part_";

    /** SAVE-stage: top baked models keyed by model id. Mirrors modern {@code BAKED_MODEL_PARTS}. */
    public static final Map<ResourceLocation, IBakedModel> SAVE_TOP_MODELS = new LinkedHashMap<>();

    /**
     * SAVE-stage: unbaked selectors + owning block per baked multipart
     * model, identity-keyed (a replaced model instance simply misses the
     * lookup and falls back to vanilla). Mirrors modern
     * {@code MULTIPART_PREDICATES}; populated by
     * {@code MultipartBakeMixin} during baking.
     */
    public static final Map<IBakedModel, StagedMultipart> SAVE_MULTIPART =
            Collections.synchronizedMap(new IdentityHashMap<IBakedModel, StagedMultipart>());

    /** LOAD-stage: deserialized snapshot waiting to be installed (see {@link #buildLoadedModels}). */
    private static volatile Data LOAD_DATA;

    private ModelModule() {
    }

    /** Gated on the Forge config equivalent of modern {@code Option.CACHE_MODEL_LOADER}. */
    public static boolean isActive() {
        return DashLoaderConfig.CACHE_MODELS.get();
    }

    public static void reset() {
        SAVE_TOP_MODELS.clear();
        SAVE_MULTIPART.clear();
    }

    /** Clears the LOAD snapshot to free memory (called on cache reset). */
    public static void clearLoad() {
        LOAD_DATA = null;
    }

    public static boolean hasLoad() {
        Data data = LOAD_DATA;
        return data != null
                && data.basicModels != null
                && (!data.basicModels.isEmpty()
                        || (data.weightedModels != null && !data.weightedModels.isEmpty())
                        || (data.multipartModels != null && !data.multipartModels.isEmpty()));
    }

    /** Empty snapshot used when the module is disabled (keeps the JSON shape stable). */
    public static Data emptyData() {
        return new Data(new LinkedHashMap<String, DashBasicBakedModel>(),
                new LinkedHashMap<String, DashMultipartBakedModel>(),
                new LinkedHashMap<String, DashWeightedBakedModel>(),
                new ArrayList<String>());
    }

    /**
     * Snapshot staged top models into a {@link Data} object.
     * Multipart part models resolve through the staged top models by
     * identity, or are snapshotted inline under synthetic ids when they are
     * not top models themselves (see {@link #resolvePartId}).
     */
    public static Data save() {
        Map<String, DashBasicBakedModel> basic = new LinkedHashMap<>();
        Map<String, DashMultipartBakedModel> multipart = new LinkedHashMap<>();
        Map<String, DashWeightedBakedModel> weighted = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        // Nested model-id strings for weighted/multipart part references:
        // every staged top model is addressable by its id string.
        final Map<IBakedModel, String> idsByModel = new java.util.IdentityHashMap<>();
        for (Map.Entry<ResourceLocation, IBakedModel> entry : SAVE_TOP_MODELS.entrySet()) {
            idsByModel.put(entry.getValue(), entry.getKey().toString());
        }
        java.util.function.Function<IBakedModel, String> modelIds = part -> {
            String id = idsByModel.get(part);
            if (id == null) {
                throw new IllegalArgumentException("Nested model is not a staged top model: "
                        + part.getClass().getName());
            }
            return id;
        };

        for (Map.Entry<ResourceLocation, IBakedModel> entry : SAVE_TOP_MODELS.entrySet()) {
            String key = entry.getKey().toString();
            IBakedModel model = entry.getValue();
            if (model == null) {
                continue;
            }
            try {
                if (model instanceof SimpleBakedModel) {
                    basic.put(key, DashBasicBakedModel.toDash((SimpleBakedModel) model, modelIds));
                } else if (model instanceof WeightedBakedModel) {
                    weighted.put(key, DashWeightedBakedModel.toDash((WeightedBakedModel) model, modelIds));
                } else if (model instanceof MultipartBakedModel) {
                    StagedMultipart staged = SAVE_MULTIPART.get(model);
                    int bakedCount = ((MultipartBakedModelAccessor) model).getSelectors().size();
                    if (staged == null || staged.selectors.size() != bakedCount) {
                        // No (or stale) unbaked selectors — e.g. replaced by
                        // onPostBakeEvent after baking. Vanilla fallback.
                        missing.add(key);
                        LOGGER.debug("Multipart model {} has no staged selectors (baked={}), using vanilla fallback.",
                                key, bakedCount);
                    } else {
                        try {
                            multipart.put(key, DashMultipartBakedModel.toDash(
                                    (MultipartBakedModel) model, staged.selectors, staged.owner,
                                    part -> resolvePartId(part, key, basic, weighted, idsByModel)));
                        } catch (RuntimeException e) {
                            missing.add(key);
                            if (missing.size() <= 3) {
                                LOGGER.warn("Skipping uncacheable model {} ({}): {}", key,
                                        model.getClass().getName(), e.getMessage());
                            }
                        }
                    }
                } else {
                    missing.add(key);
                    if (missing.size() <= 3) {
                        LOGGER.warn("Skipping uncacheable model {} ({}).", key, model.getClass().getName());
                    }
                }
            } catch (RuntimeException e) {
                missing.add(key);
                if (missing.size() <= 3) {
                    LOGGER.warn("Skipping uncacheable model {} ({}): {}", key, model.getClass().getName(), e.getMessage());
                }
            }
        }

        LOGGER.info("Model snapshot: {} basic, {} weighted, {} multipart, {} missing.",
                basic.size(), weighted.size(), multipart.size(), missing.size());
        if (weighted.isEmpty()) {
            // Vanilla 1.16.5 has no weighted top models with default packs
            // (weights live inside VariantList baking, not as top-level
            // WeightedBakedModel entries): round-trip path (WeightedModelAccessor
            // + WeightedRandomItemAccessor, reflective WeightedModel rebuild) is
            // retained and verified statically, nothing to snapshot this boot.
            LOGGER.info("No weighted models found (vanilla has none as top models) — weighted round-trip retained.");
        }
        return new Data(basic, multipart, weighted, missing);
    }

    /**
     * Resolves a multipart part model to a model-id string: staged top
     * models by identity, otherwise snapshotted inline under a synthetic id
     * (see {@link #SYNTHETIC_PART_MARKER}). Uncacheable parts throw, which
     * the caller turns into a missing entry (vanilla fallback).
     */
    private static String resolvePartId(IBakedModel part, String ownerKey,
            Map<String, DashBasicBakedModel> basic, Map<String, DashWeightedBakedModel> weighted,
            Map<IBakedModel, String> idsByModel) {
        String id = idsByModel.get(part);
        if (id != null) {
            return id;
        }
        if (part instanceof SimpleBakedModel) {
            String synthKey = syntheticPartKey(ownerKey, basic, weighted);
            basic.put(synthKey, DashBasicBakedModel.toDash((SimpleBakedModel) part, nested -> {
                String nestedId = idsByModel.get(nested);
                if (nestedId == null) {
                    throw new IllegalArgumentException("Nested model is not a staged top model: "
                            + nested.getClass().getName());
                }
                return nestedId;
            }));
            idsByModel.put(part, synthKey);
            return synthKey;
        }
        if (part instanceof WeightedBakedModel) {
            String synthKey = syntheticPartKey(ownerKey, basic, weighted);
            weighted.put(synthKey, DashWeightedBakedModel.toDash((WeightedBakedModel) part, nested -> {
                String nestedId = idsByModel.get(nested);
                if (nestedId == null) {
                    throw new IllegalArgumentException("Nested model is not a staged top model: "
                            + nested.getClass().getName());
                }
                return nestedId;
            }));
            idsByModel.put(part, synthKey);
            return synthKey;
        }
        throw new IllegalArgumentException("Uncacheable multipart part model: " + part.getClass().getName());
    }

    /** Synthetic part key that cannot collide with a real model id. */
    private static String syntheticPartKey(String ownerKey,
            Map<String, DashBasicBakedModel> basic, Map<String, DashWeightedBakedModel> weighted) {
        int index = 0;
        String candidate = ownerKey + SYNTHETIC_PART_MARKER + index;
        while (basic.containsKey(candidate) || weighted.containsKey(candidate)) {
            index++;
            candidate = ownerKey + SYNTHETIC_PART_MARKER + index;
        }
        return candidate;
    }

    /** Whether a snapshot id is a synthetic inline part (skipped at registry install). */
    public static boolean isSyntheticKey(ResourceLocation id) {
        return id != null && id.getPath().contains(SYNTHETIC_PART_MARKER);
    }

    /** Stages unbaked selectors for a baked multipart model (called by {@code MultipartBakeMixin}). */
    public static void stageMultipartSelectors(MultipartBakedModel baked,
            List<Selector> selectors, ResourceLocation owner) {
        if (baked == null || selectors == null || owner == null) {
            return;
        }
        SAVE_MULTIPART.put(baked, new StagedMultipart(selectors, owner));
    }

    /**
     * Stores the deserialized snapshot for later installation.
     * Mirrors modern {@code ModelModule#load} (which fills
     * {@code BLOCK_STATE_UNBAKED}); installation happens in
     * {@code ModelManagerCacheMixin} once a sprite lookup is available.
     */
    public static void load(Data data) {
        if (data == null) {
            return;
        }
        LOAD_DATA = data;
        int basic = data.basicModels == null ? 0 : data.basicModels.size();
        int weighted = data.weightedModels == null ? 0 : data.weightedModels.size();
        int multipart = data.multipartModels == null ? 0 : data.multipartModels.size();
        LOGGER.info("Model restore staged: {} basic, {} weighted, {} multipart.", basic, weighted, multipart);
    }

    /**
     * Rebuilds vanilla models from the LOAD snapshot.
     *
     * <p>Per-entry skip resilience (modern parity): any model that fails to
     * restore is skipped with a warning and left for vanilla baking instead
     * of aborting the whole install. Basic models are restored first so
     * weighted/multipart entries referencing them (including synthetic
     * inline part ids) resolve; unresolvable references are skipped.
     * Basic overrides resolve to already-restored models, so basics build in
     * two passes (bare first, then with overrides — forward references like
     * bow pulling variants resolve). Synthetic part entries are built for
     * reference resolution but filtered out of the returned map by the caller
     * via {@link #isSyntheticKey} so the model registry keeps only real ids.
     */
    public static Map<ResourceLocation, IBakedModel> buildLoadedModels(
            Function<ResourceLocation, TextureAtlasSprite> spriteLookup) {
        Data data = LOAD_DATA;
        if (data == null) {
            return Collections.emptyMap();
        }
        Map<ResourceLocation, IBakedModel> out = new LinkedHashMap<>();
        if (data.basicModels != null) {
            // Pass 1: bare models (EMPTY overrides) so every id resolves.
            for (Map.Entry<String, DashBasicBakedModel> entry : data.basicModels.entrySet()) {
                try {
                    DashBasicBakedModel dash = entry.getValue();
                    DashBasicBakedModel bare = new DashBasicBakedModel(
                            dash.generalQuads, dash.faceQuads,
                            dash.ambientOcclusion, dash.gui3d, dash.sideLit,
                            dash.particleSpriteId, dash.cameraTransforms,
                            Collections.<DashItemOverride>emptyList());
                    out.put(new ResourceLocation(entry.getKey()),
                            bare.toVanilla(spriteLookup, key -> {
                                throw new IllegalArgumentException("Overrides deferred to pass 2: " + key);
                            }));
                } catch (RuntimeException e) {
                    LOGGER.warn("Skipping unrestorable cached model {}: {}", entry.getKey(), e.getMessage());
                }
            }
            // Pass 2: rebuild with overrides resolved against pass-1 models.
            // Unresolvable override targets are skipped per-override with a
            // warning (vanilla fallback for that override); models whose own
            // quads fail stay on their pass-1 bare version.
            final Map<ResourceLocation, IBakedModel> built = out;
            for (Map.Entry<String, DashBasicBakedModel> entry : data.basicModels.entrySet()) {
                ResourceLocation id = new ResourceLocation(entry.getKey());
                if (!built.containsKey(id)) {
                    continue;
                }
                DashBasicBakedModel dash = entry.getValue();
                if (dash == null || dash.itemOverrides == null || dash.itemOverrides.isEmpty()) {
                    continue;
                }
                try {
                    built.put(id, dash.toVanilla(spriteLookup, key -> {
                        IBakedModel part = built.get(new ResourceLocation(key));
                        if (part == null) {
                            throw new IllegalArgumentException("Referenced model not restored: " + key);
                        }
                        return part;
                    }));
                } catch (RuntimeException e) {
                    LOGGER.warn("Keeping bare cached model {} (override rebuild failed): {}",
                            entry.getKey(), e.getMessage());
                }
            }
        }
        if (data.weightedModels != null) {
            for (Map.Entry<String, DashWeightedBakedModel> entry : data.weightedModels.entrySet()) {
                try {
                    final Map<ResourceLocation, IBakedModel> built = out;
                    WeightedBakedModel model = entry.getValue().toVanilla(key -> {
                        IBakedModel part = built.get(new ResourceLocation(key));
                        if (part == null) {
                            throw new IllegalArgumentException("Referenced model not restored: " + key);
                        }
                        return part;
                    });
                    out.put(new ResourceLocation(entry.getKey()), model);
                } catch (RuntimeException e) {
                    LOGGER.warn("Skipping unrestorable cached model {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
        if (data.multipartModels != null && !data.multipartModels.isEmpty()) {
            for (Map.Entry<String, DashMultipartBakedModel> entry : data.multipartModels.entrySet()) {
                try {
                    final Map<ResourceLocation, IBakedModel> built = out;
                    MultipartBakedModel model = entry.getValue().toVanilla(key -> {
                        IBakedModel part = built.get(new ResourceLocation(key));
                        if (part == null) {
                            throw new IllegalArgumentException("Referenced model not restored: " + key);
                        }
                        return part;
                    }, owner -> {
                        Block block = ForgeRegistries.BLOCKS.getValue(owner);
                        if (block == null) {
                            throw new IllegalArgumentException("Unknown block: " + owner);
                        }
                        return block.getStateContainer();
                    });
                    out.put(new ResourceLocation(entry.getKey()), model);
                } catch (RuntimeException e) {
                    LOGGER.warn("Skipping unrestorable cached model {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
        LOGGER.info("Model restore built: {} models.", out.size());
        return out;
    }

    /** SAVE-stage unbaked data for one baked multipart model (see {@link #SAVE_MULTIPART}). */
    public static final class StagedMultipart {
        public final List<Selector> selectors;
        public final ResourceLocation owner;

        public StagedMultipart(List<Selector> selectors, ResourceLocation owner) {
            this.selectors = selectors;
            this.owner = owner;
        }
    }

    /** Snapshot data. Keys are model id strings; values are Dash models. */
    public static final class Data {
        public final Map<String, DashBasicBakedModel> basicModels;
        public final Map<String, DashMultipartBakedModel> multipartModels;
        public final Map<String, DashWeightedBakedModel> weightedModels;
        public final List<String> missingModels;

        public Data(Map<String, DashBasicBakedModel> basicModels,
                Map<String, DashMultipartBakedModel> multipartModels,
                Map<String, DashWeightedBakedModel> weightedModels,
                List<String> missingModels) {
            this.basicModels = basicModels;
            this.multipartModels = multipartModels;
            this.weightedModels = weightedModels;
            this.missingModels = missingModels;
        }
    }
}
