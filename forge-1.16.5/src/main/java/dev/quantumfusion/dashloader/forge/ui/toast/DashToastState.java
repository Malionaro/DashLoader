package dev.quantumfusion.dashloader.forge.ui.toast;

/**
 * Forge 1.16.5 port of modern {@code DashToastState}
 * ({@code fabric-26.3}).
 *
 * <p>Yarn -&gt; MCP mapping: modern tracks a {@code taski} task tree
 * (nested progress + translated names). There is no taski equivalent wired
 * here, so the SAVE driver ({@code DashCacheBackend}) sets a plain
 * {@code 0..1} target progress and a plain status string; translation is
 * raw strings (i18n TODO).
 *
 * <p>Kept verbatim from modern: the progress smoothing ({@code tickProgress}
 * at ~100ups, fast attack / slow release via {@code divisionSpeed}) and the
 * DONE timestamp ({@link #setDone}/{@link #getTimeDone}) driving the
 * auto-hide timing in {@link DashToast}.
 * @author Malionaro
 */public final class DashToastState {
    // Volatile: the SAVE worker thread writes these while the render thread reads them.
    private volatile DashToastStatus status = DashToastStatus.IDLE;
    private volatile double targetProgress;
    private volatile String text = "Idle";
    private volatile long timeDone = System.currentTimeMillis();

    // Render-thread only easing state (mirrors modern currentProgress/lastUpdate).
    private double currentProgress = 0;
    private long lastUpdate = System.currentTimeMillis();

    public DashToastStatus getStatus() {
        return status;
    }

    public void setStatus(DashToastStatus status) {
        this.status = status;
    }

    /** Target progress in {@code 0..1}, set by the SAVE driver. */
    public void setProgress(double progress) {
        this.targetProgress = progress;
    }

    /**
     * Smoothed progress in {@code 0..1} (NaN-safe, clamped to 0 like
     * modern). Eases toward the target set by {@link #setProgress}.
     */
    public double getProgress() {
        final long currentTime = System.currentTimeMillis();
        while (currentTime > this.lastUpdate) {
            this.tickProgress();
            this.lastUpdate += 10; // ~100ups
        }
        return this.currentProgress;
    }

    private void tickProgress() {
        if (Double.isNaN(this.currentProgress)) {
            this.currentProgress = 0.0;
        }
        double actualProgress = this.targetProgress;
        if (Double.isNaN(actualProgress)) {
            actualProgress = 0.0;
        }
        final double divisionSpeed = (actualProgress < this.currentProgress) ? 3 : 30;
        double step = (actualProgress - this.currentProgress) / divisionSpeed;
        this.currentProgress += step;
    }

    /** Right-aligned progress text (modern shows the task progress text here). */
    public String getProgressText() {
        return (int) Math.round(getProgress() * 100.0) + "%";
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
