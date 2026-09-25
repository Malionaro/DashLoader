package dev.quantumfusion.dashloader.forge.ui;

/**
 * Forge 1.16.5 port of modern {@code Color} ({@code fabric-26.3}).
 *
 * <p>Verbatim: pure Java RGBA value object, no Minecraft dependencies.
 * Stored as {@code R,G,B,A} packed int; {@link #argb()} converts to the
 * packed ARGB int that 1.16.5 rendering ({@code AbstractGui#fill},
 * {@code FontRenderer#drawString}) expects.
 */
public final class Color {
    private final int rgba;

    public Color(int rgba) {
        this.rgba = rgba;
    }

    public Color(int red, int green, int blue, int alpha) {
        this.rgba = ((red & 0xFF) << 24) | ((green & 0xFF) << 16) | ((blue & 0xFF) << 8) | (alpha & 0xFF);
    }

    public Color(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    public int red() {
        return (rgba >>> 24) & 0xFF;
    }

    public int green() {
        return (rgba >>> 16) & 0xFF;
    }

    public int blue() {
        return (rgba >>> 8) & 0xFF;
    }

    public int alpha() {
        return rgba & 0xFF;
    }

    public int rgb() {
        return rgba >>> 8;
    }

    public int rgba() {
        return rgba;
    }

    public int argb() {
        return this.rgb() | (this.alpha() << 24);
    }
}
