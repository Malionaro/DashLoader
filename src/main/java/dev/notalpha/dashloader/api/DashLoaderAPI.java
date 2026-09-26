package dev.notalpha.dashloader.api;

import dev.notalpha.dashloader.api.cache.Cache;
import dev.notalpha.dashloader.api.cache.CacheHolder;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Public entry point for other mods that want to know what DashLoader is doing.
 *
 * <p>DashLoader does its work in the background while the game runs, so mods that
 * also touch resources can easily walk into a half-loaded state. Use this class to
 * check whether DashLoader currently serves cached data, and to react to changes
 * instead of polling the log.
 *
 * <p>Example: defer work until the cache finished loading.
 * <pre>{@code
 * DashLoaderAPI.whenLoaded(() -> {
 *     // every asset is now served from the cache
 * });
 * }</pre>
 *
 * <p>Example: ask once.
 * <pre>{@code
 * if (DashLoaderAPI.isLoaded()) {
 *     // ...
 * }
 * }</pre>
 */
public final class DashLoaderAPI {
	private DashLoaderAPI() {
	}

	/**
	 * @return The current cache status, or {@code null} if the cache is not set up yet.
	 */
	public static @Nullable CacheStatus getStatus() {
		Cache cache = getCache();
		return cache == null ? null : cache.getStatus();
	}

	/**
	 * @return {@code true} if a cache was found and is currently being served. In this
	 * state assets are read from the cache and no new cache is being written.
	 */
	public static boolean isLoaded() {
		return getStatus() == CacheStatus.LOAD;
	}

	/**
	 * @return {@code true} if DashLoader is currently creating a new cache. Assets are
	 * loaded the normal way until the cache is written.
	 */
	public static boolean isSaving() {
		return getStatus() == CacheStatus.SAVE;
	}

	/**
	 * @return {@code true} if DashLoader holds no temporary resources, which is the case
	 * between loading and saving.
	 */
	public static boolean isIdle() {
		return getStatus() == CacheStatus.IDLE;
	}

	/**
	 * @return The directory of the cache that is currently used, or {@code null} if the
	 * cache has not been named yet (which happens before the first resource reload).
	 */
	public static @Nullable Path getCacheDir() {
		Cache cache = getCache();
		if (cache == null) {
			return null;
		}
		try {
			return cache.getDir();
		} catch (RuntimeException notNamedYet) {
			// The cache is only named once the resource manager has reloaded once.
			return null;
		}
	}

	/**
	 * Runs the given action as soon as a cache is being served. If DashLoader is already
	 * loaded the action runs immediately on the calling thread. Otherwise it runs on the
	 * next status change to {@link CacheStatus#LOAD}.
	 *
	 * <p>Has no effect if DashLoader is set up to create a new cache, since that never
	 * reaches the loaded state.
	 *
	 * @param action The action to run.
	 */
	public static void whenLoaded(Runnable action) {
		Cache cache = getCache();
		if (cache == null) {
			return;
		}
		if (cache.getStatus() == CacheStatus.LOAD) {
			action.run();
			return;
		}
		CacheHolder.addListener(status -> {
			if (status == CacheStatus.LOAD) {
				action.run();
			}
		});
	}

	/**
	 * Runs the given action on every future status change. The current status is not
	 * reported. Listeners stay registered for the lifetime of the game and may be
	 * called from the caching thread.
	 *
	 * @param listener The listener which receives the new status.
	 */
	public static void addStatusListener(Consumer<CacheStatus> listener) {
		CacheHolder.addListener(listener);
	}

	private static @Nullable Cache getCache() {
		return CacheHolder.get();
	}
}
