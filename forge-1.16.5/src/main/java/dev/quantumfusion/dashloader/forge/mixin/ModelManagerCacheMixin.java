package dev.quantumfusion.dashloader.forge.mixin;

import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WIP vertical slice: model-cache hook point.
 *
 * <p>Era reference: {@code def-fabric-1.17/.../mixin/feature/cache/BakedModelManagerOverride.java}
 * (lines 31-114) cancels {@code BakedModelManager.prepare/apply} to skip vanilla
 * baking when a cache is present. The 1.16.5 Forge equivalents, verified against
 * the mapped snapshot jar ({@code javap} on
 * {@code forge-1.16.5-36.2.42_mapped_snapshot_20210309-1.16.5}), are:
 * <ul>
 *   <li>yarn {@code BakedModelManager} -&gt; MCP {@code ModelManager}</li>
 *   <li>yarn {@code ModelLoader} -&gt; MCP {@code ModelBakery}</li>
 *   <li>yarn {@code BakedModel} -&gt; MCP {@code IBakedModel}</li>
 *   <li>yarn {@code ResourceManager/Profiler/Identifier} -&gt; MCP
 *       {@code IResourceManager/IProfiler/ResourceLocation}</li>
 * </ul>
 *
 * <p>This mixin is intentionally behaviour-neutral (log only, never cancels):
 * the actual skip/cache logic needs {@code dashloader-core} (Hyphen
 * serialisation, {@code VanillaData}, {@code DashMappings}), which has no
 * resolvable 1.16.5-era artifact — see {@code DashModelCache} and the spike
 * report. Compiling this mixin proves the Mixin toolchain works and the
 * target method descriptors
 * ({@code prepare(Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)Lnet/minecraft/client/renderer/model/ModelBakery;},
 * {@code apply(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/resources/IResourceManager;Lnet/minecraft/profiler/IProfiler;)V})
 * exist in 1.16.5.
 *
 * <p>Runtime blocker: no refmap is generated yet (needs MixinGradle on the
 * FG4 build); production use also needs the config registered via the
 * {@code MixinConfigs} jar manifest attribute (done in build.gradle).
 */
@Mixin(ModelManager.class)
public abstract class ModelManagerCacheMixin {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-slice");

    @Inject(
            method = "prepare",
            at = @At("HEAD"),
            cancellable = true,
            // WIP slice: dev workspace is already MCP-named, so no remapping
            // is needed to compile/run here. Production (SRG runtime) needs
            // remap=true plus a working refmap — the AP's searge mapping
            // lookup is still broken (see build.gradle), so that is pending.
            remap = false
    )
    private void dashloader$onPrepare(IResourceManager resourceManager, IProfiler profiler,
            CallbackInfoReturnable<ModelBakery> cir) {
        // WIP: when DashModelCache has data, return null here (mirrors the
        // era mixin setting ModelLoader to null) and serve models in apply.
        LOGGER.debug("DashLoader slice: ModelManager.prepare hook reached (no-op)");
    }

    @Inject(
            method = "apply",
            at = @At("HEAD"),
            cancellable = true,
            // Same as above: remap=false is dev-correct, prod needs the
            // refmap pipeline fixed first.
            remap = false
    )
    private void dashloader$onApply(ModelBakery bakery, IResourceManager resourceManager,
            IProfiler profiler, CallbackInfo ci) {
        // WIP: when DashModelCache has data, install cached models/atlases
        // here (mirrors era lines 90-111) and cancel.
        LOGGER.debug("DashLoader slice: ModelManager.apply hook reached (no-op)");
    }
}
