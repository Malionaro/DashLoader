package dev.quantumfusion.dashloader.forge.ui.toast;

/**
 * Forge 1.16.5 port of modern {@code DashToastStatus}
 * ({@code fabric-1.21.4}): lifecycle of the caching toast.
 */
public enum DashToastStatus {
    IDLE,
    PROGRESS,
    DONE,
    CRASHED
}
