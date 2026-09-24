package dev.quantumfusion.dashloader.forge.mixin;

import dev.quantumfusion.dashloader.forge.cache.CacheStatus;
import dev.quantumfusion.dashloader.forge.cache.DashCacheBackend;
import dev.quantumfusion.dashloader.forge.splash.SplashModule;
import net.minecraft.client.util.Splashes;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Splash-text cache hook.
 *
 * <p>Modern reference ({@code fabric-26.3}):
 * {@code SplashTextResourceSupplierMixin} serves {@code TEXTS} (LOAD) from
 * {@code prepare} HEAD and steals the vanilla list (SAVE) at RETURN. The
 * 1.16.5 counterpart is {@link Splashes}
 * ({@code net.minecraft.client.util.Splashes}, a
 * {@code ReloadListener<List<String>>} fed by {@code texts/splashes.txt}).
 *
 * <p>LOAD: at HEAD of {@code apply}, the prepared vanilla list is replaced
 * with the cached {@link SplashModule#LOADED} texts (served cache). SAVE: the
 * vanilla list is staged into {@link SplashModule#TEXTS} keep-first (only
 * when empty — same double-reload policy as modern sprite staging) so
 * {@link SplashModule#save()} snapshots real data. Never cancels; missing
 * cache entries fall back to vanilla loading.
 *
 * <p>The {@code possibleSplashes} field name was verified via {@code javap}
 * against the mapped snapshot jar. The {@code apply} descriptor below
 * ({@code apply(Ljava/util/List;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V})
 * matches the mapped snapshot jar. Dev workspace is MCP-named so
 * {@code remap = false}; production needs the refmap pipeline (see
 * {@code PORTING_NOTES.md}).
 */
@Mixin(value = Splashes.class, remap = false)
public abstract class SplashesCacheMixin {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-splash");

    @Shadow(remap = false)
    private List<String> possibleSplashes;

    @Inject(method = "apply", at = @At("HEAD"), remap = false)
    private void dashloader$serveAndStageSplashes(List<String> splashList,
            IResourceManager resourceManager, IProfiler profiler, CallbackInfo ci) {
        if (!SplashModule.isActive()) {
            return;
        }
        // LOAD-side: serve the cached texts (modern SplashTextResourceSupplierMixin parity).
        if (DashCacheBackend.getStatus() == CacheStatus.LOAD && !SplashModule.LOADED.isEmpty()
                && splashList != null) {
            try {
                splashList.clear();
                splashList.addAll(SplashModule.LOADED);
                LOGGER.debug("DashLoader: serving {} cached splash texts.", SplashModule.LOADED.size());
                return;
            } catch (Throwable t) {
                LOGGER.warn("DashLoader splash serve failed, using vanilla texts.", t);
            }
        }
        // SAVE-side: stage vanilla texts keep-first (only when empty).
        if (DashCacheBackend.getStatus() == CacheStatus.SAVE
                && splashList != null && !splashList.isEmpty() && SplashModule.TEXTS.isEmpty()) {
            try {
                SplashModule.TEXTS.addAll(splashList);
                LOGGER.debug("DashLoader: staged {} splash texts for caching.", splashList.size());
            } catch (Throwable t) {
                LOGGER.warn("DashLoader splash staging failed.", t);
            }
        }
    }
}
