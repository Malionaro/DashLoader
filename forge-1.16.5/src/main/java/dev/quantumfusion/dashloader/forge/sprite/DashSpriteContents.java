package dev.quantumfusion.dashloader.forge.sprite;

import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Forge 1.16.5 port of modern {@code DashSpriteContents}
 * ({@code fabric-1.21.4}).
 *
 * <p>1.16.5 adaptation (no {@code SpriteContents}/{@code Sprite} split yet):
 * <ul>
 *   <li>Modern {@code SpriteContents} (pixels + size + animation) and
 *       {@code Sprite} (atlas position) are one class in 1.16.5:
 *       {@code TextureAtlasSprite}. This port snapshots the <em>content</em>
 *       side: sprite id, pixel data and dimensions, carried as
 *       {@link TextureAtlasSprite.Info} (id + size + animation metadata)
 *       plus an RGBA pixel copy.</li>
 *   <li>Pixels are stored as an RGBA {@code int[]} via
 *       {@link NativeImage#getPixelRGBA} /
 *       {@link NativeImage#setPixelRGBA} (both public in 1.16.5), so no
 *       accessor mixin is needed.</li>
 *   <li>Atlas <em>position</em> (x/y/width on the sheet) is the stitch
 *       module's job ({@code DashTextureStitcher}), same split as modern
 *       (content vs stitch modules).</li>
 * </ul>
 *
 * <p>Intentional simplifications (documented):
 * <ul>
 *   <li>Animation metadata round-trip is a stub: frame index/time pairs are
 *       stored, but rebuilding the vanilla
 *       {@code AnimationMetadataSection} for the {@link Info} constructor is
 *       a cache-backend TODO (vanilla parses it from {@code .mcmeta} JSON;
 *       section construction from parts is unwired). Restored sprites are
 *       non-animated until that lands.</li>
 *   <li>Mipmaps are not stored (vanilla regenerates them at upload).</li>
 * </ul>
 */
public final class DashSpriteContents {
    public final ResourceLocation id;
    public final int width;
    public final int height;
    /** Row-major RGBA pixels ({@code NativeImage} format), {@code width * height} entries. */
    public final int[] pixels;
    /** Animation frames (index + per-frame time); empty for static sprites. */
    public final List<Frame> frames;

    public DashSpriteContents(ResourceLocation id, int width, int height,
            int[] pixels, List<Frame> frames) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.pixels = pixels;
        this.frames = frames;
    }

    /** Snapshot content for {@code info} from a source image (e.g. the stitch input). */
    public static DashSpriteContents toDash(TextureAtlasSprite.Info info, NativeImage image) {
        int width = info.getSpriteWidth();
        int height = info.getSpriteHeight();
        int[] pixels = new int[width * height];
        int count = 0;
        for (int y = 0; y < height && y < image.getHeight(); y++) {
            for (int x = 0; x < width && x < image.getWidth(); x++) {
                pixels[count++] = image.getPixelRGBA(x, y);
            }
        }
        return new DashSpriteContents(info.getSpriteLocation(), width, height,
                pixels, new ArrayList<Frame>());
    }

    /** Rebuild the stitch input. Animation section is {@code null} (see class javadoc). */
    public TextureAtlasSprite.Info toInfo() {
        return new TextureAtlasSprite.Info(id, width, height, null);
    }

    /** Rebuild the pixel image for the stitch input. */
    public NativeImage toImage() {
        NativeImage image = new NativeImage(width, height, false);
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixelRGBA(x, y, pixels[count++]);
            }
        }
        return image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashSpriteContents that = (DashSpriteContents) o;
        return width == that.width
                && height == that.height
                && Objects.equals(id, that.id)
                && java.util.Arrays.equals(pixels, that.pixels)
                && Objects.equals(frames, that.frames);
    }

    @Override
    public int hashCode() {
        int result = id == null ? 0 : id.hashCode();
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + java.util.Arrays.hashCode(pixels);
        result = 31 * result + (frames == null ? 0 : frames.hashCode());
        return result;
    }

    /** Animation frame: sprite-sheet frame index + display time in ticks. */
    public static final class Frame {
        public final int index;
        public final int time;

        public Frame(int index, int time) {
            this.index = index;
            this.time = time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Frame frame = (Frame) o;
            return index == frame.index && time == frame.time;
        }

        @Override
        public int hashCode() {
            return 31 * index + time;
        }
    }
}
