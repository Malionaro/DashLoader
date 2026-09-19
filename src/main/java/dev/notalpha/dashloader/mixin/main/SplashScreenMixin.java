package dev.notalpha.dashloader.mixin.main;

import dev.notalpha.dashloader.DashLoader;
import dev.notalpha.dashloader.api.cache.Cache;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.DashLoaderClient;
import dev.notalpha.dashloader.client.ui.toast.DashToast;
import dev.notalpha.dashloader.client.ui.toast.DashToastState;
import dev.notalpha.dashloader.client.ui.toast.DashToastStatus;
import dev.notalpha.dashloader.config.ConfigHandler;
import dev.notalpha.dashloader.misc.ProfilerUtil;
import dev.notalpha.taski.builtin.StaticTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoadingOverlay.class, priority = 69420)
public class SplashScreenMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	private long fadeOutStart;
	@Shadow
	private long fadeInStart;
	@Shadow
	@Final
	private ReloadInstance reload;

	@Inject(
			method = "tick()V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMillis()J", shift = At.Shift.AFTER)
	)
	private void done(CallbackInfo ci) {
		this.minecraft.gui.setOverlay(null);
		if (this.minecraft.gui.screen() != null) {
			if (this.minecraft.gui.screen() instanceof TitleScreen) {
				this.minecraft.gui.setScreen(new TitleScreen(false));
			}
		}

		DashLoader.LOG.info("Minecraft reloaded in {}", ProfilerUtil.getTimeStringFromStart(ProfilerUtil.RELOAD_START));
		Cache cache = DashLoaderClient.CACHE;
		if (DashLoaderClient.CACHE.getStatus() == CacheStatus.SAVE && minecraft.gui.toastManager().getToast(DashToast.class, Toast.NO_TOKEN) == null) {
			DashToastState rawState;
			if (ConfigHandler.INSTANCE.config.showCachingToast) {
				DashToast toast = new DashToast();
				minecraft.gui.toastManager().addToast(toast);
				rawState = toast.state;
			} else {
				rawState = new DashToastState();
			}

			final Thread thread = new Thread(() -> {
				DashToastState state = rawState;
				DashToastState finalState = state;
				state.setStatus(DashToastStatus.PROGRESS);
				long start = System.currentTimeMillis();
				boolean save = cache.save(stepTask -> finalState.task = stepTask);
				if (save) {
					state.setOverwriteText("Created cache in " + ProfilerUtil.getTimeStringFromStart(start));
					state.setStatus(DashToastStatus.DONE);
				} else {
					// Only show toast on fail.
					if (!ConfigHandler.INSTANCE.config.showCachingToast) {
						DashToast toast = new DashToast();
						minecraft.gui.toastManager().addToast(toast);
						state = toast.state;
					}
					state.setOverwriteText("Internal error, Please check logs.");
					state.task = new StaticTask("Crash", 0);
					state.setStatus(DashToastStatus.CRASHED);
				}
				cache.reset();
				state.setDone();
			});
			thread.setName("dashloader-thread");
			thread.start();
		} else {
			cache.reset();
		}
	}
}
