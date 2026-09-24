package dev.quantumfusion.dashloader.forge.mixin;

import dev.quantumfusion.dashloader.forge.DashLoaderConfig;
import dev.quantumfusion.dashloader.forge.cache.CacheStatus;
import dev.quantumfusion.dashloader.forge.cache.DashCacheBackend;
import dev.quantumfusion.dashloader.forge.model.ModelModule;
import dev.quantumfusion.dashloader.forge.ui.toast.DashToast;
import dev.quantumfusion.dashloader.forge.ui.toast.DashToastState;
import dev.quantumfusion.dashloader.forge.ui.toast.DashToastStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Function;

/**
 * Model-cache hook for the Forge 1.16.5 port.
 *
 * <p>Modern reference ({@code fabric-26.3}):
 * {@code ModelManagerMixin} (captures baked models into
 * {@code BLOCK_STATE_MODELS} on SAVE) + {@code BlockStatesLoaderMixin}
 * (shortcuts unbaked loading on LOAD). The 1.16.5 Forge equivalents,
 * verified via {@code javap} against the mapped snapshot jar, are:
 * <ul>
 *   <li>yarn {@code BakedModelManager} -&gt; MCP {@code ModelManager}</li>
 *   <li>yarn {@code ModelLoader} -&gt; MCP {@code ModelBakery}</li>
 *   <li>yarn {@code BakedModel} -&gt; MCP {@code IBakedModel}</li>
 *   <li>yarn {@code ResourceManager/Profiler/Identifier} -&gt; MCP
 *       {@code IResourceManager/IProfiler/ResourceLocation}</li>
 * </ul>
 *
 * <p>1.16.5 adaptation (documented): there is no unbaked shortcut point —
 * 1.16.5 bakes JSON models directly in {@code ModelBakery} with no
 * {@code BlockStatesLoader} equivalent — so LOAD installs <em>after</em>
 * vanilla baking: at TAIL of {@code apply}, restored models overwrite the
 * {@code modelRegistry} entries (vanilla result stays as the fallback for
 * anything skipped). SAVE stages the bakery's
 * {@link ModelBakery#getTopBakedModels()} at HEAD of {@code apply} and kicks
 * off the background Gson write (with toast) once per boot.
 *
 * <p>Descriptors below match the mapped snapshot jar:
 * {@code prepare(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)Lnet/minecraft/client/renderer/model/ModelBakery;}
 * {@code apply(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V}.
 * Dev workspace is MCP-named so {@code remap = false}; production needs the
 * refmap pipeline (see {@code PORTING_NOTES.md}).
 */
@Mixin(ModelManager.class)
public abstract class ModelManagerCacheMixin {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-model");

    @Shadow(remap = false)
    private Map<ResourceLocation, IBakedModel> field_174958_a;

    @Inject(method = "func_212854_a_", at = @At("HEAD"), remap = false)
    private void dashloader$ensureCache(IResourceManager resourceManager, IProfiler profiler,
            CallbackInfoReturnable<ModelBakery> cir) {
        try {
            DashCacheBackend.ensureLoaded();
        } catch (Throwable t) {
            LOGGER.warn("DashLoader cache init failed, continuing vanilla.", t);
        }
    }

    @Inject(method = "func_212853_a_", at = @At("HEAD"), remap = false)
    private void dashloader$stageModels(ModelBakery bakery, IResourceManager resourceManager,
            IProfiler profiler, CallbackInfo ci) {
        try {
            DashCacheBackend.ensureLoaded();
        } catch (Throwable t) {
            LOGGER.warn("DashLoader cache init failed, continuing vanilla.", t);
        }
        if (DashCacheBackend.getStatus() != CacheStatus.SAVE || !ModelModule.isActive() || bakery == null) {
            return;
        }
        try {
            ModelModule.SAVE_TOP_MODELS.clear();
            ModelModule.SAVE_TOP_MODELS.putAll(bakery.getTopBakedModels());
        } catch (Throwable t) {
            LOGGER.warn("DashLoader model staging failed, vanilla baking continues.", t);
        }
    }

    @Inject(method = "func_212853_a_", at = @At("TAIL"), remap = false)
    private void dashloader$installAndSave(ModelBakery bakery, IResourceManager resourceManager,
            IProfiler profiler, CallbackInfo ci) {
        if (DashCacheBackend.getStatus() == CacheStatus.LOAD && ModelModule.isActive() && ModelModule.hasLoad()) {
            try {
                Minecraft minecraft = Minecraft.getInstance();
                Function<ResourceLocation, TextureAtlasSprite> sprites = minecraft
                        .getAtlasSpriteGetter(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
                Map<ResourceLocation, IBakedModel> restored = ModelModule.buildLoadedModels(sprites);
                int replaced = 0;
                for (Map.Entry<ResourceLocation, IBakedModel> entry : restored.entrySet()) {
                    try {
                        field_174958_a.put(entry.getKey(), entry.getValue());
                        replaced++;
                    } catch (RuntimeException e) {
                        LOGGER.warn("Skipping cached model install for {}: {}", entry.getKey(), e.getMessage());
                    }
                }
                LOGGER.info("DashLoader installed {} cached models (vanilla fallback for the rest).", replaced);
            } catch (Throwable t) {
                LOGGER.warn("DashLoader model install failed, keeping vanilla models.", t);
            }
        }

        if (DashCacheBackend.getStatus() == CacheStatus.SAVE
                && DashLoaderConfig.ENABLE_CACHE.get()
                && !DashCacheBackend.isSaveStarted()) {
            DashCacheBackend.markSaveStarted();
            startBackgroundSave();
        }
    }

    /**
     * Modern {@code SplashScreenMixin} equivalent: writes the cache on a
     * background thread while a {@link DashToast} shows progress. Must be
     * called on the client thread (toast registration is not thread-safe).
     */
    private static void startBackgroundSave() {
        final DashToastState state;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (DashLoaderConfig.SHOW_CACHING_TOAST.get()
                    && minecraft.getToastGui().getToast(DashToast.class, net.minecraft.client.gui.toasts.IToast.NO_TOKEN) == null) {
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
