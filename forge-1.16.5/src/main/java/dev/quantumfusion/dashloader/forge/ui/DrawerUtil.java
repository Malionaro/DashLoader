package dev.quantumfusion.dashloader.forge.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;

/**
 * Forge 1.16.5 port of modern {@code DrawerUtil} ({@code fabric-26.3}).
 *
 * <p>Yarn -&gt; MCP mapping: modern draws through
 * {@code GuiGraphicsExtractor} ({@code fill}, {@code text}); 1.16.5 draws
 * through {@link MatrixStack} + {@link AbstractGui#fill} +
 * {@link FontRenderer#drawString}. Colors, the progress-color ramp, the glow
 * approximation (nested expanding rects with decreasing opacity) and the
 * text baseline adjustment ({@code y - lineHeight}) are kept verbatim.
 * @author Malionaro
 */public final class DrawerUtil {
    public static final float GLOW_SIZE = 30f;
    public static final float GLOW_STRENGTH = 0.1f;
    public static final Color FAILED_COLOR = new Color(250, 68, 51);
    public static final Color BACKGROUND_COLOR = new Color(34, 31, 34);
    public static final Color FOREGROUND_COLOR = new Color(252, 252, 250);
    public static final Color STATUS_COLOR = new Color(180, 180, 180);
    public static final Color NEUTRAL_LINE = new Color(45, 42, 46);
    public static final Color PROGRESS_TRACK = new Color(25, 25, 25);
    private static final Color[] PROGRESS_COLORS = new Color[]{
            new Color(0xff, 0x61, 0x88),
            new Color(0xfc, 0x98, 0x67),
            new Color(0xff, 0xd8, 0x66),
            new Color(0xa9, 0xdc, 0x76)
    };

    private DrawerUtil() {
    }

    public static void drawRect(MatrixStack matrices, int x, int y, int width, int height, Color color) {
        final int x2 = width + x;
        final int y2 = height + y;
        AbstractGui.fill(matrices, x, y, x2, y2, color.argb());
    }

    public static void drawText(MatrixStack matrices, FontRenderer font, int x, int y, String text, Color color) {
        font.drawString(matrices, text == null ? "" : text, (float) x, (float) (y - font.FONT_HEIGHT), color.argb());
    }

    /**
     * Flat approximation of the old vertex glow: nested expanding rects with
     * decreasing opacity.
     */
    public static void drawGlow(MatrixStack matrices, float x, float y, float width, float height, float strength,
            Color color, boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        if (!topLeft && !topRight && !bottomLeft && !bottomRight) {
            return;
        }
        drawGlowClipped(matrices, x, y, width, height, strength, color, topLeft, topRight, bottomLeft, bottomRight,
                (int) x - (int) GLOW_SIZE - 1, (int) y - (int) GLOW_SIZE - 1,
                (int) width + (int) (GLOW_SIZE * 2) + 2, (int) height + (int) (GLOW_SIZE * 2) + 2);
    }

    public static void drawGlowClipped(MatrixStack matrices, float x, float y, float width, float height,
            float strength, Color color, boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight,
            int clipX, int clipY, int clipWidth, int clipHeight) {
        Color glow = withOpacity(color, GLOW_STRENGTH * strength);
        int layers = 3;
        for (int i = layers; i >= 1; i--) {
            float spread = (GLOW_SIZE / layers) * i;
            Color layer = withOpacity(glow, 1f - ((float) i / (layers + 1)));
            int rx1 = Math.max(clipX, (int) (x - spread));
            int ry1 = Math.max(clipY, (int) (y - spread));
            int rx2 = Math.min(clipX + clipWidth, (int) (x + width + spread));
            int ry2 = Math.min(clipY + clipHeight, (int) (y + height + spread));
            if (rx2 > rx1 && ry2 > ry1) {
                AbstractGui.fill(matrices, rx1, ry1, rx2, ry2, layer.argb());
            }
        }
    }

    public static int convertColor(Color color) {
        return color.rgb() | color.alpha() << 24;
    }

    public static Color withOpacity(Color color, float opacity) {
        float currentOpacity = color.alpha() / 255f;
        return new Color(color.red(), color.green(), color.blue(), (int) ((opacity * currentOpacity) * 255));
    }

    public static Color getProgressColor(double progress) {
        return mix(progress, PROGRESS_COLORS);
    }

    private static Color mix(double pos, Color... colors) {
        if (colors.length == 1) {
            return colors[0];
        }
        pos = Math.min(1, Math.max(0, pos));
        int breaks = colors.length - 1;
        if (pos == 1) {
            return colors[breaks];
        }
        int colorPos = (int) Math.floor(pos * (breaks));
        final double step = 1d / (breaks);
        double localRatio = (pos % step) * breaks;
        return blend(colors[colorPos], colors[colorPos + 1], localRatio);
    }

    private static Color blend(Color i1, Color i2, double ratio) {
        if (ratio > 1f) {
            ratio = 1f;
        } else if (ratio < 0f) {
            ratio = 0f;
        }
        double iRatio = 1.0f - ratio;

        int a = (int) ((i1.alpha() * iRatio) + (i2.alpha() * ratio));
        int r = (int) ((i1.red() * iRatio) + (i2.red() * ratio));
        int g = (int) ((i1.green() * iRatio) + (i2.green() * ratio));
        int b = (int) ((i1.blue() * iRatio) + (i2.blue() * ratio));

        return new Color(r, g, b, a);
    }
}
