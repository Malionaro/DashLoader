package dev.quantumfusion.dashloader.forge.model;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;

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
 *   <li>{@code ItemCameraTransforms} is carried as a vanilla runtime object
 *       (not serialized field-by-field yet) — cache-backend TODO.</li>
 *   <li>{@code ItemOverrideList} is reset to {@link ItemOverrideList#EMPTY}
 *       on restore. Item overrides depend on {@code ModelBakery} context and
 *       are out of scope for this slice.</li>
 * </ul>
 */
public final class DashBasicBakedModel {
    public final DashBakedQuadCollection generalQuads;
    public final Map<Direction, DashBakedQuadCollection> faceQuads;
    public final boolean ambientOcclusion;
    public final boolean gui3d;
    public final boolean sideLit;
    public final ResourceLocation particleSpriteId;
    /** Runtime object, not yet serialized — see class javadoc. */
    public final ItemCameraTransforms cameraTransforms;

    public DashBasicBakedModel(DashBakedQuadCollection generalQuads,
            Map<Direction, DashBakedQuadCollection> faceQuads,
            boolean ambientOcclusion, boolean gui3d, boolean sideLit,
            ResourceLocation particleSpriteId,
            ItemCameraTransforms cameraTransforms) {
        this.generalQuads = generalQuads;
        this.faceQuads = faceQuads;
        this.ambientOcclusion = ambientOcclusion;
        this.gui3d = gui3d;
        this.sideLit = sideLit;
        this.particleSpriteId = particleSpriteId;
        this.cameraTransforms = cameraTransforms;
    }

    public static DashBasicBakedModel toDash(SimpleBakedModel model) {
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
        return new DashBasicBakedModel(general, faces,
                model.isAmbientOcclusion(), model.isGui3d(), model.isSideLit(),
                particleId, model.getItemCameraTransforms());
    }

    public SimpleBakedModel toVanilla(Function<ResourceLocation, TextureAtlasSprite> spriteLookup) {
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
        return new SimpleBakedModel(general, faces,
                ambientOcclusion, gui3d, sideLit,
                particle, transforms, ItemOverrideList.EMPTY);
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
                && Objects.equals(particleSpriteId, that.particleSpriteId);
    }

    @Override
    public int hashCode() {
        int result = generalQuads == null ? 0 : generalQuads.hashCode();
        result = 31 * result + (faceQuads == null ? 0 : faceQuads.hashCode());
        result = 31 * result + (ambientOcclusion ? 1 : 0);
        result = 31 * result + (gui3d ? 1 : 0);
        result = 31 * result + (sideLit ? 1 : 0);
        result = 31 * result + (particleSpriteId == null ? 0 : particleSpriteId.hashCode());
        return result;
    }
}
