package dev.quantumfusion.dashloader.forge.model;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Forge 1.16.5 port of modern {@code DashBakedQuadCollection} /
 * {@code BakedQuadCollection} ({@code fabric-1.21.4}).
 *
 * <p>Modern stores registry ids ({@code List<Integer>}) pointing into the
 * Hyphen object registry; this port stores {@link DashBakedQuad} values
 * directly because the Hyphen registry is out of scope (see
 * {@code DashCacheBackend} TODO). The shape — one collection per
 * cull-face plus one unculled collection — is unchanged.
 * @author Malionaro
 */public final class DashBakedQuadCollection {
    public final List<DashBakedQuad> quads;

    public DashBakedQuadCollection(List<DashBakedQuad> quads) {
        this.quads = quads;
    }

    public static DashBakedQuadCollection toDash(List<BakedQuad> quads) {
        List<DashBakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            out.add(DashBakedQuad.toDash(quad));
        }
        return new DashBakedQuadCollection(out);
    }

    public List<BakedQuad> toVanilla(Function<ResourceLocation, TextureAtlasSprite> spriteLookup) {
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (DashBakedQuad quad : quads) {
            out.add(quad.toVanilla(spriteLookup));
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashBakedQuadCollection that = (DashBakedQuadCollection) o;
        return Objects.equals(quads, that.quads);
    }

    @Override
    public int hashCode() {
        return quads == null ? 0 : quads.hashCode();
    }
}
