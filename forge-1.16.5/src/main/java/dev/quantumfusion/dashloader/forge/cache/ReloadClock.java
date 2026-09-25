package dev.quantumfusion.dashloader.forge.cache;

/**
 * Reload timing shared between mixins.
 *
 * <p>Plain helper class: Mixin 0.8.4 rejects non-private static members in
 * mixin classes, so shared mutable state lives here instead.
 */
public final class ReloadClock {
	private static volatile long reloadStart = System.currentTimeMillis();

	private ReloadClock() {
	}

	public static long getReloadStart() {
		return reloadStart;
	}

	public static void setReloadStart(long value) {
		reloadStart = value;
	}
}
