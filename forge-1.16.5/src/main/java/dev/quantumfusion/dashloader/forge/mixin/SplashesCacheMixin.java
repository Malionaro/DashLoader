package dev.quantumfusion.dashloader.forge.mixin;

import dev.quantumfusion.dashloader.forge.splash.SplashModule;
import net.minecraft.client.util.Splashes;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Splash-text cache hook.
 *
 * <p>Modern reference: {@code SplashTextResourceSupplierMixin}
 * ({@code fabric-1.21.4}) feeds cached splash texts into the title screen.
 * The 1.16.5 counterpart is {@link Splashes}
 * ({@code net.minecraft.client.util.Splashes}, a
 * {@code ReloadListener<List<String>>} fed by {@code texts/splashes.txt}).
 *
 * <p>This slice is SAVE-side only and behaviour-neutral: on
 * {@code apply} it stages the vanilla splash list into
 * {@link SplashModule#TEXTS} so {@link SplashModule#save()} snapshots real
 * data. It never cancels and never replaces the list.
 *
 * <p>LOAD-side TODO: serving {@link SplashModule#LOADED} needs the splash
 * list field on {@code Splashes} (name unverified in this MCP snapshot —
 * intentionally not guessed here) plus a cancellable apply hook. The mixin
 * descriptor below
 * ({@code apply(Ljava/util/List;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V})
 * matches the mapped snapshot jar.
 */
@Mixin(value = Splashes.class, remap = false)
public abstract class SplashesCacheMixin {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-splash");

    @Inject(method = "apply", at = @At("HEAD"))
    private void dashloader$stageSplashes(List<String> splashList,
            IResourceManager resourceManager, IProfiler profiler, CallbackInfo ci) {
        if (SplashModule.isActive() && splashList != null && !splashList.isEmpty()
                && SplashModule.TEXTS.isEmpty()) {
            SplashModule.TEXTS.addAll(splashList);
            LOGGER.debug("DashLoader: staged {} splash texts for caching.", splashList.size());
        }
    }
}
