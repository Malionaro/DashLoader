package dev.quantumfusion.dashloader.forge.sprite.stitch;

import net.minecraft.util.ResourceLocation;

/**
 * Tracks which atlas is currently being stitched on this thread.
 *
 * <p>Plain helper class (deliberately NOT inside a mixin: Mixin 0.8.4 rejects
 * non-private static fields in mixin classes, even with {@code @Unique}).
 * @author Malionaro
 */public final class StitchState {
	public static final ThreadLocal<ResourceLocation> CURRENT_ATLAS = new ThreadLocal<>();

	private StitchState() {
	}
}
