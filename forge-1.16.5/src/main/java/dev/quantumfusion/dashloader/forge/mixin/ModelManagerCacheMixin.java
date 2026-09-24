package dev.quantumfusion.dashloader.forge.mixin;

import dev.quantumfusion.dashloader.forge.cache.CacheStatus;
import dev.quantumfusion.dashloader.forge.cache.DashCacheBackend;
import dev.quantumfusion.dashloader.forge.model.ModelModule;
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
 * anything skipped). SAVE stages the finished {@code modelRegistry} at TAIL
 * of {@code apply}; the background Gson write (with toast) is kicked off
 * once per boot by {@code ResourceLoadProgressGuiMixin} at reload-complete,
 * after every staging hook has finished.
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

    /** Reload start (approximation): set at {@code prepare} HEAD, read by the loading-screen hook for timing logs. */
    private static volatile long reloadStart = System.currentTimeMillis();

    public static long getReloadStart() {
        return reloadStart;
    }

    @Shadow(remap = false)
    private Map<ResourceLocation, IBakedModel> modelRegistry;

    @Inject(method = "prepare(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)Lnet/minecraft/client/renderer/model/ModelBakery;", at = @At("HEAD"), remap = false)
    private void dashloader$ensureCache(IResourceManager resourceManager, IProfiler profiler,
            CallbackInfoReturnable<ModelBakery> cir) {
        reloadStart = System.currentTimeMillis();
        try {
            DashCacheBackend.ensureLoaded();
        } catch (Throwable t) {
            LOGGER.warn("DashLoader cache init failed, continuing vanilla.", t);
        }
    }

    @Inject(method = "apply(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V", at = @At("TAIL"), remap = false)
    private void dashloader$stageModels(ModelBakery bakery, IResourceManager resourceManager,
            IProfiler profiler, CallbackInfo ci) {
        try {
            DashCacheBackend.ensureLoaded();
        } catch (Throwable t) {
            LOGGER.warn("DashLoader cache init failed, continuing vanilla.", t);
        }
        if (DashCacheBackend.getStatus() != CacheStatus.SAVE || !ModelModule.isActive()) {
            return;
        }
        // Stage from the finished model registry: Forge's ModelLoader does not
        // fill ModelBakery top models (always empty at apply HEAD), but the
        // registry is complete here at TAIL.
        try {
            ModelModule.SAVE_TOP_MODELS.clear();
            ModelModule.SAVE_TOP_MODELS.putAll(modelRegistry);
            LOGGER.info("DashLoader staged {} baked models.", ModelModule.SAVE_TOP_MODELS.size());
        } catch (Throwable t) {
            LOGGER.warn("DashLoader model staging failed, vanilla baking continues.", t);
        }
    }

    @Inject(method = "apply(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V", at = @At("TAIL"), remap = false)
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
                    // Synthetic inline part ids only exist for reference
                    // resolution (multipart components); they are not real
                    // registry entries.
                    if (ModelModule.isSyntheticKey(entry.getKey())) {
                        continue;
                    }
                    try {
                        modelRegistry.put(entry.getKey(), entry.getValue());
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
        // Background SAVE with toast is driven by ResourceLoadProgressGuiMixin
        // at reload-complete (modern SplashScreenMixin parity), once every
        // staging hook has finished — not here.
    }
}
