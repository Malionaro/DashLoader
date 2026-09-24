package dev.quantumfusion.dashloader.forge.cache;

/**
 * Forge 1.16.5 port of modern {@code CacheStatus}
 * ({@code fabric-26.3}, {@code api/cache/CacheStatus.java}).
 *
 * <p>Same three states: {@link #IDLE} (nothing staged), {@link #LOAD}
 * (a cache was found on disk and LOAD maps are populated),
 * {@link #SAVE} (no usable cache; SAVE maps are staged for writing).
 */
public enum CacheStatus {
    IDLE,
    LOAD,
    SAVE
}
