package dev.quantumfusion.dashloader.forge.ui.toast;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.ToastGui;

/**
 * Forge 1.16.5 port of modern {@code DashToast}
 * ({@code fabric-1.21.4}).
 *
 * <p>Yarn -&gt; MCP mapping:
 * <ul>
 *   <li>Modern {@code Toast} -&gt; MCP {@code IToast}; render entry point is
 *       {@code func_230444_a_(MatrixStack, ToastGui, long)} returning
 *       {@link IToast.Visibility} (modern splits this into
 *       {@code update()} + {@code draw()}).</li>
 *   <li>Modern {@code DrawContext} drawing -&gt; 1.16.5
 *       {@link MatrixStack} + {@link AbstractGui#fill} +
 *       {@link FontRenderer#drawString}.</li>
 *   <li>Size: modern is 200x40; 1.16.5 {@code IToast} defaults are 160x32,
 *       so width/height are overridden to the modern values.</li>
 *   <li>Dedup token: default {@code getType()} ({@code NO_TOKEN}) is kept,
 *       so {@code ToastGui#getToast(DashToast.class, NO_TOKEN)} finds it —
 *       the 1.16.5 equivalent of modern's
 *       {@code getToast(DashToast.class, Toast.TYPE)} check.</li>
 * </ul>
 *
 * <p>Simplifications (documented): the animated background lines, glow,
 * progress-color ramp and fun-fact line from modern are dropped — this port
 * draws a flat background, status text, percentage and a progress bar. The
 * hide timing (DONE hides after 2s, CRASHED after 10s) matches modern.
 */
public final class DashToast implements IToast {
    private static final int WIDTH = 200;
    private static final int HEIGHT = 40;
    private static final int PROGRESS_BAR_HEIGHT = 3;

    private static final int BACKGROUND = 0xFF14141C;
    private static final int BAR_TRACK = 0xFF3A3A4A;
    private static final int BAR_FILL = 0xFF5AC85A;
    private static final int BAR_FAILED = 0xFFE05252;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int STATUS_COLOR = 0xFFB0B0C0;

    public final DashToastState state = new DashToastState();

    private Visibility visibility = Visibility.SHOW;

    @Override
    public int func_230445_a_() {
        return WIDTH;
    }

    @Override
    public int func_238540_d_() {
        return HEIGHT;
    }

    @Override
    public Visibility func_230444_a_(MatrixStack matrices, ToastGui toastGui, long time) {
        Minecraft minecraft = toastGui.getMinecraft();
        FontRenderer font = minecraft.fontRenderer;

        double progress = state.getProgress();
        boolean crashed = state.getStatus() == DashToastStatus.CRASHED;

        if (crashed && System.currentTimeMillis() - state.getTimeDone() > 10000) {
            visibility = Visibility.HIDE;
        } else if (state.getStatus() == DashToastStatus.DONE
                && System.currentTimeMillis() - state.getTimeDone() > 2000) {
            visibility = Visibility.HIDE;
        } else {
            visibility = Visibility.SHOW;
        }

        int barY = HEIGHT - PROGRESS_BAR_HEIGHT;
        int barColor = crashed ? BAR_FAILED : BAR_FILL;

        AbstractGui.fill(matrices, 0, 0, WIDTH, HEIGHT, BACKGROUND);
        font.drawString(matrices, "DashLoader", 8, 8, TEXT_COLOR);
        String statusLine = state.getText();
        font.drawString(matrices, statusLine, 8, 20, STATUS_COLOR);
        String percent = (int) Math.round(progress * 100.0) + "%";
        font.drawString(matrices, percent, WIDTH - 8 - font.getStringWidth(percent), 8, STATUS_COLOR);

        AbstractGui.fill(matrices, 0, barY, WIDTH, HEIGHT, BAR_TRACK);
        AbstractGui.fill(matrices, 0, barY, (int) (WIDTH * progress), HEIGHT, barColor);

        return visibility;
    }
}
