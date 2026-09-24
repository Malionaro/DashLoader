package dev.quantumfusion.dashloader.forge.model;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Forge 1.16.5 (MCP snapshot_20210309) port of modern
 * {@code DashBakedQuad} ({@code fabric-1.21.4}).
 *
 * <p>Yarn -&gt; MCP name mapping applied here:
 * <ul>
 *   <li>yarn {@code BakedQuad} -&gt; MCP {@code BakedQuad}
 *       ({@code net.minecraft.client.renderer.model.BakedQuad})</li>
 *   <li>yarn {@code Sprite} -&gt; MCP {@code TextureAtlasSprite}; only the
 *       sprite id ({@code ResourceLocation}) is stored, resolved lazily via
 *       the sprite lookup passed to {@link #toVanilla}.</li>
 *   <li>yarn {@code Direction} -&gt; MCP {@code Direction}
 *       ({@code net.minecraft.util.Direction}).</li>
 * </ul>
 *
 * <p>Intentional data loss vs modern (documented, not a bug):
 * <ul>
 *   <li>Modern {@code lightEmission} (added in 1.21) does not exist on the
 *       1.16.5 {@code BakedQuad} constructor
 *       {@code (int[], int, Direction, TextureAtlasSprite, boolean)} and is
 *       dropped.</li>
 *   <li>Modern {@code shade} maps to 1.16.5 {@code applyDiffuseLighting()}
 *       (same semantic: per-quad diffuse shading flag).</li>
 *   <li>Modern {@code colorIndex} maps to 1.16.5 {@code tintIndex}
 *       ({@code getTintIndex()}).</li>
 * </ul>
 *
 * <p>Serialization: plain fields, Hyphen-free. The cache backend
 * ({@code DashCacheBackend}) still has to pick these up — see its TODO.
 * @author Malionaro
 */public final class DashBakedQuad {
    public final int[] vertexData;
    public final int tintIndex;
    public final Direction face;
    public final ResourceLocation spriteId;
    public final boolean diffuseLighting;

    public DashBakedQuad(int[] vertexData, int tintIndex, Direction face,
            ResourceLocation spriteId, boolean diffuseLighting) {
        this.vertexData = vertexData;
        this.tintIndex = tintIndex;
        this.face = face;
        this.spriteId = spriteId;
        this.diffuseLighting = diffuseLighting;
    }

    /** Snapshot a vanilla quad. Sprite is stored by id only. */
    public static DashBakedQuad toDash(BakedQuad quad) {
        return new DashBakedQuad(
                quad.getVertexData().clone(),
                quad.getTintIndex(),
                quad.getFace(),
                quad.getSprite().getName(),
                quad.applyDiffuseLighting());
    }

    /** Rebuild a vanilla quad, resolving the sprite through {@code spriteLookup}. */
    public BakedQuad toVanilla(Function<ResourceLocation, TextureAtlasSprite> spriteLookup) {
        return new BakedQuad(
                vertexData.clone(),
                tintIndex,
                face,
                spriteLookup.apply(spriteId),
                diffuseLighting);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashBakedQuad that = (DashBakedQuad) o;
        return tintIndex == that.tintIndex
                && diffuseLighting == that.diffuseLighting
                && Arrays.equals(vertexData, that.vertexData)
                && face == that.face
                && Objects.equals(spriteId, that.spriteId);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(vertexData);
        result = 31 * result + tintIndex;
        result = 31 * result + (face == null ? 0 : face.hashCode());
        result = 31 * result + (spriteId == null ? 0 : spriteId.hashCode());
        result = 31 * result + (diffuseLighting ? 1 : 0);
        return result;
    }
}
