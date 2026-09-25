package dev.quantumfusion.dashloader.forge.sprite.stitch;

import java.util.Objects;

/**
 * Forge 1.16.5 port of modern {@code DashTextureSlot}
 * ({@code fabric-1.21.4}): cached atlas position + size of one sprite.
 *
 * <p>{@code width}/{@code height} are the PACKED slot dims (what
 * {@code Stitcher} allocated, passed to {@code ISpriteLoader#load}).
 * {@code sourceWidth}/{@code sourceHeight} are the SOURCE sprite dims
 * ({@code Info#getSpriteWidth/Height}) used for the
 * changed-dimensions check in {@code DashTextureStitcher#addSprite}.
 * Animated sprites pack as a vertical strip (packed height = source height
 * x frame count), so comparing packed vs source always falls back; source
 * vs source only falls back when the pack really changed.
 *
 * <p>Old caches (4-field JSON) deserialize with source dims 0, which the
 * stitcher treats as "unknown source, use packed" (legacy behavior).
 */
public final class DashTextureSlot {
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    public final int sourceWidth;
    public final int sourceHeight;

    public DashTextureSlot(int x, int y, int width, int height) {
        this(x, y, width, height, 0, 0);
    }

    public DashTextureSlot(int x, int y, int width, int height, int sourceWidth, int sourceHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashTextureSlot that = (DashTextureSlot) o;
        return x == that.x && y == that.y && width == that.width && height == that.height
                && sourceWidth == that.sourceWidth && sourceHeight == that.sourceHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height, sourceWidth, sourceHeight);
    }
}
