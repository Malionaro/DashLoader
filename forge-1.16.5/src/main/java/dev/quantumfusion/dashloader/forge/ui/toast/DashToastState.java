package dev.quantumfusion.dashloader.forge.ui.toast;

/**
 * Forge 1.16.5 port of modern {@code DashToastState}
 * ({@code fabric-1.21.4}).
 *
 * <p>Simplifications (documented):
 * <ul>
 *   <li>Modern tracks a {@code taski} task tree (nested progress + translated
 *       names). There is no taski equivalent wired here, so progress is a
 *       plain {@code 0..1} double and text is a plain string set by the
 *       caching driver. The smoothing tick from modern is kept in spirit:
 *       none — {@link #getProgress()} returns the last set value.</li>
 *   <li>Modern translates text via {@code TranslationHelper}; this port
 *       uses raw strings (i18n TODO).</li>
 * </ul>
 */
public final class DashToastState {
    // Volatile: the SAVE worker thread writes these while the render thread reads them.
    private volatile DashToastStatus status = DashToastStatus.IDLE;
    private volatile double progress;
    private volatile String text = "Idle";
    private volatile long timeDone = System.currentTimeMillis();

    public DashToastStatus getStatus() {
        return status;
    }

    public void setStatus(DashToastStatus status) {
        this.status = status;
    }

    /** Progress in {@code 0..1}. NaN-safe (clamped to 0 like modern). */
    public double getProgress() {
        if (Double.isNaN(progress)) {
            return 0.0;
        }
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimeDone() {
        return timeDone;
    }

    public void setDone() {
        this.timeDone = System.currentTimeMillis();
    }
}
