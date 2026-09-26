package dev.notalpha.dashloader.api.cache;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds the cache that is currently in use and notifies listeners about status changes.
 *
 * <p>This lives in the api package so that {@link dev.notalpha.dashloader.api.DashLoaderAPI}
 * can expose the cache state without depending on client internals.
 */
public final class CacheHolder {
	private static final Object LOCK = new Object();
	private static final List<Consumer<CacheStatus>> LISTENERS = new ArrayList<>();
	@Nullable
	private static Cache cache;

	private CacheHolder() {
	}

	/**
	 * Called by the cache implementation once it has been built.
	 */
	public static void set(@Nullable Cache newCache) {
		synchronized (LOCK) {
			cache = newCache;
		}
	}

	public static @Nullable Cache get() {
		synchronized (LOCK) {
			return cache;
		}
	}

	/**
	 * Registers a listener for status changes. Listeners may be called from the caching
	 * thread, so they must not assume they run on the main thread.
	 */
	public static void addListener(Consumer<CacheStatus> listener) {
		synchronized (LOCK) {
			LISTENERS.add(listener);
		}
	}

	/**
	 * Notifies all listeners. Called by the cache whenever its status changes.
	 */
	public static void fireStatus(CacheStatus status) {
		List<Consumer<CacheStatus>> copy;
		synchronized (LOCK) {
			copy = new ArrayList<>(LISTENERS);
		}
		for (Consumer<CacheStatus> listener : copy) {
			listener.accept(status);
		}
	}
}
