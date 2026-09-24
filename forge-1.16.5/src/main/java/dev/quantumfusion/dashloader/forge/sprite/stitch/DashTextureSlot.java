package dev.quantumfusion.dashloader.forge.sprite.stitch;

import java.util.Objects;

/**
 * Forge 1.16.5 port of modern {@code DashTextureSlot}
 * ({@code fabric-1.21.4}): cached atlas position + size of one sprite.
 * @author Malionaro
 */public final class DashTextureSlot {
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public DashTextureSlot(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashTextureSlot that = (DashTextureSlot) o;
        return x == that.x && y == that.y && width == that.width && height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height);
    }
}
