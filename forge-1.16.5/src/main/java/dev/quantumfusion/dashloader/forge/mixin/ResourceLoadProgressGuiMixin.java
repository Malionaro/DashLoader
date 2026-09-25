package dev.quantumfusion.dashloader.forge.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import dev.quantumfusion.dashloader.forge.cache.CacheStatus;
import dev.quantumfusion.dashloader.forge.cache.DashCacheBackend;
import dev.quantumfusion.dashloader.forge.ui.toast.DashToast;
import dev.quantumfusion.dashloader.forge.ui.toast.DashToastState;
import dev.quantumfusion.dashloader.forge.ui.toast.DashToastStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ResourceLoadProgressGui;
import net.minecraft.resources.IAsyncReloader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Loading-screen hook for the Forge 1.16.5 port.
 *
 * <p>Modern reference ({@code fabric-26.3}): {@code SplashScreenMixin} on
 * {@code LoadingOverlay#tick} — dismisses the overlay at reload-complete and
 * writes the cache on a background thread while a {@link DashToast} shows
 * progress. The 1.16.5 LoadingOverlay equivalent is
 * {@link ResourceLoadProgressGui} (verified via {@code javap}: there is no
 * {@code LoadingOverlay} class on 1.16.5; the reload screen is
 * {@code ResourceLoadProgressGui extends LoadingGui} with
 * {@code render(MatrixStack, int, int, float)}).
 *
 * <p>1.16.5 adaptation (all names verified via {@code javap} against the
 * mapped snapshot jar, {@code remap = false}):
 * <ul>
 *   <li>Modern {@code tick()} -&gt; 1.16.5
 *       {@code render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V} TAIL
 *       (there is no per-tick method; 1.16.5 drives the screen from
 *       {@code render}).</li>
 *   <li>Modern {@code Util#getMillis()} reload check -&gt; 1.16.5
 *       {@code Util#milliTime()} is internal to {@code render}; the
 *       equivalent completion signal is
 *       {@code IAsyncReloader#fullyDone()} (also javap-verified).</li>
 *   <li>Modern {@code gui#setOverlay(null)} -&gt; 1.16.5
 *       {@code Minecraft#setLoadingGui(null)} (javap-verified). This only
 *       skips the ~1s vanilla fade: it runs at TAIL, after vanilla already
 *       executed {@code join()} + {@code completedCallback} + screen
 *       {@code init} earlier in the same call (javap-verified order), and
 *       only once vanilla has marked completion ({@code fadeOutStart != -1}).
 *       Dismissing earlier would abort the reload, so unlike modern the
 *       overlay is NOT cleared before completion.</li>
 *   <li>Background SAVE with toast: same driver as the old
 *       {@code ModelManagerCacheMixin} hook (moved here for modern parity),
 *       kicked off once per boot. Running it at reload-complete instead of
 *       {@code ModelManager#apply} TAIL guarantees every staging hook
 *       (models, sprites, stitch captures, splashes) has finished.</li>
 * </ul>
 */
@Mixin(value = ResourceLoadProgressGui.class, remap = false)
public abstract class ResourceLoadProgressGuiMixin {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-cache");

    @Shadow(remap = false)
    private Minecraft field_212974_b;

    @Shadow(remap = false)
    private IAsyncReloader field_212975_c;

    @Shadow(remap = false)
    private long field_212979_g;

    @Inject(method = "func_230430_a_(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V",
            at = @At("TAIL"), remap = false)
    private void dashloader$onReloadComplete(MatrixStack matrices, int mouseX, int mouseY, float partialTicks,
            CallbackInfo ci) {
        try {
            if (!field_212975_c.fullyDone()) {
                return;
            }
            // Dismiss the overlay at reload-complete (modern parity). Vanilla
            // would do this ~1s later once its fade finishes; the reload
            // itself (join + completedCallback + screen init) already ran.
            if (field_212979_g != -1L && field_212974_b.getLoadingGui() != null) {
                field_212974_b.setLoadingGui(null);
            }
            LOGGER.info("Minecraft reloaded in {}ms.",
                    System.currentTimeMillis() - ModelManagerCacheMixin.getReloadStart());
            if (DashCacheBackend.getStatus() == CacheStatus.SAVE
                    && DashLoaderConfig.ENABLE_CACHE.get()
                    && !DashCacheBackend.isSaveStarted()) {
                DashCacheBackend.markSaveStarted();
                startBackgroundSave();
            }
        } catch (Throwable t) {
            LOGGER.warn("DashLoader reload-complete hook failed, continuing vanilla.", t);
        }
    }

    /**
     * Writes the cache on a background thread while a {@link DashToast}
     * shows progress. Must be called on the client thread (toast
     * registration is not thread-safe).
     */
    private static void startBackgroundSave() {
        final DashToastState state;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (DashLoaderConfig.SHOW_CACHING_TOAST.get()
                    && minecraft.getToastGui().getToast(DashToast.class,
                            net.minecraft.client.gui.toasts.IToast.NO_TOKEN) == null) {
                DashToast toast = new DashToast();
                minecraft.getToastGui().add(toast);
                state = toast.state;
            } else {
                state = new DashToastState();
            }
        } catch (Throwable t) {
            LOGGER.warn("DashLoader toast setup failed, saving without toast.", t);
            saveWithoutToast();
            return;
        }
        state.setStatus(DashToastStatus.PROGRESS);
        state.setText("caching");
        final Thread thread = new Thread(() -> {
            long start = System.currentTimeMillis();
            boolean ok;
            try {
                ok = DashCacheBackend.save(state);
            } catch (Throwable t) {
                LOGGER.error("DashLoader background save crashed.", t);
                ok = false;
            }
            if (ok) {
                state.setText("Created cache in " + (System.currentTimeMillis() - start) + "ms");
                state.setStatus(DashToastStatus.DONE);
            } else {
                state.setText("Internal error, please check logs.");
                state.setStatus(DashToastStatus.CRASHED);
            }
            state.setDone();
            DashCacheBackend.reset();
        });
        thread.setName("dashloader-save");
        thread.setDaemon(true);
        thread.start();
    }

    private static void saveWithoutToast() {
        final Thread thread = new Thread(() -> {
            try {
                DashCacheBackend.save(new DashToastState());
            } catch (Throwable t) {
                LOGGER.error("DashLoader background save crashed.", t);
            }
            DashCacheBackend.reset();
        });
        thread.setName("dashloader-save");
        thread.setDaemon(true);
        thread.start();
    }
}
