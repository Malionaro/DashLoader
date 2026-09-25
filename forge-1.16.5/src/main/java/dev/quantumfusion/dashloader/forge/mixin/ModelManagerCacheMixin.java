package dev.quantumfusion.dashloader.forge.mixin;

import dev.quantumfusion.dashloader.forge.cache.CacheStatus;
import dev.quantumfusion.dashloader.forge.cache.DashCacheBackend;
import dev.quantumfusion.dashloader.forge.cache.ReloadClock;
import dev.quantumfusion.dashloader.forge.mixin.accessor.MultipartAccessor;
import dev.quantumfusion.dashloader.forge.model.ModelModule;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import net.minecraft.client.renderer.model.multipart.Multipart;
import net.minecraft.client.renderer.model.multipart.Selector;
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

    /** Reload start tracking lives in {@link ReloadClock} (no statics allowed in mixins). */
    @Shadow(remap = false)
    private Map<ResourceLocation, IBakedModel> field_174958_a;

    @Inject(method = "func_212854_a_(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)Lnet/minecraft/client/renderer/model/ModelBakery;", at = @At("HEAD"), remap = false)
    private void dashloader$ensureCache(IResourceManager resourceManager, IProfiler profiler,
            CallbackInfoReturnable<ModelBakery> cir) {
        ReloadClock.setReloadStart(System.currentTimeMillis());
        try {
            DashCacheBackend.ensureLoaded();
        } catch (Throwable t) {
            LOGGER.warn("DashLoader cache init failed, continuing vanilla.", t);
        }
    }

    @Inject(method = "func_212853_a_(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V", at = @At("TAIL"), remap = false)
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
            ModelModule.SAVE_TOP_MODELS.putAll(field_174958_a);
            LOGGER.info("DashLoader staged {} baked models.", ModelModule.SAVE_TOP_MODELS.size());
            stageMultipartFallback(bakery);
        } catch (Throwable t) {
            LOGGER.warn("DashLoader model staging failed, vanilla baking continues.", t);
        }
    }

    /**
     * Fallback multipart staging: the bake-time hook
     * ({@code MultipartBakeMixin}) misses when the cache was IDLE during
     * baking (pack hash not ready at {@code prepare} HEAD). At apply TAIL the
     * bakery still holds the unbaked {@link Multipart} per id, so re-stage
     * any baked multipart model without staged selectors from
     * {@code bakery.getUnbakedModel}. Identity-keyed like the bake hook; never
     * overwrites existing staging.
     */
    private static void stageMultipartFallback(ModelBakery bakery) {
        if (bakery == null) {
            return;
        }
        int restaged = 0;
        for (Map.Entry<ResourceLocation, IBakedModel> entry : ModelModule.SAVE_TOP_MODELS.entrySet()) {
            IBakedModel baked = entry.getValue();
            if (!(baked instanceof MultipartBakedModel)) {
                continue;
            }
            if (ModelModule.SAVE_MULTIPART.containsKey(baked)) {
                continue;
            }
            try {
                IUnbakedModel unbaked = bakery.getUnbakedModel(entry.getKey());
                if (!(unbaked instanceof Multipart)) {
                    continue;
                }
                Multipart multipart = (Multipart) unbaked;
                java.util.List<Selector> selectors = multipart.getSelectors();
                Block owner;
                try {
                    owner = ((MultipartAccessor) multipart).getStateContainer().getOwner();
                } catch (Throwable t) {
                    LOGGER.debug("Multipart fallback: no state container for {}.", entry.getKey());
                    continue;
                }
                if (owner == null || owner.getRegistryName() == null || selectors == null) {
                    continue;
                }
                ModelModule.stageMultipartSelectors((MultipartBakedModel) baked,
                        new java.util.ArrayList<>(selectors), owner.getRegistryName());
                restaged++;
            } catch (Throwable t) {
                LOGGER.debug("Multipart fallback staging failed for {}.", entry.getKey(), t);
            }
        }
        if (restaged > 0) {
            LOGGER.info("DashLoader restaged {} multipart selectors from bakery (bake-hook miss).", restaged);
        }
    }

    @Inject(method = "func_212853_a_(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V", at = @At("TAIL"), remap = false)
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
        // Background SAVE with toast is driven by ResourceLoadProgressGuiMixin
        // at reload-complete (modern SplashScreenMixin parity), once every
        // staging hook has finished — not here.
    }
}
