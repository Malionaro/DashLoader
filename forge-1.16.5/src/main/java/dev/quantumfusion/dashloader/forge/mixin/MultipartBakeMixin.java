package dev.quantumfusion.dashloader.forge.mixin;

import dev.quantumfusion.dashloader.forge.cache.CacheStatus;
import dev.quantumfusion.dashloader.forge.cache.DashCacheBackend;
import dev.quantumfusion.dashloader.forge.model.ModelModule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.model.multipart.Multipart;
import net.minecraft.client.renderer.model.multipart.Selector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Stages unbaked multipart selectors for SAVE caching.
 *
 * <p>Modern reference: {@code fabric-1.21.4}
 * {@code MultipartUnbakedModelMixin} captures the baked -&gt; unbaked
 * mapping at {@code bake} RETURN into {@code MULTIPART_PREDICATES}. The
 * 1.16.5 counterpart is the unbaked
 * {@link Multipart#bakeModel(ModelBakery, Function, IModelTransform, ResourceLocation)}
 * (javap-verified signature on the mapped snapshot jar): at RETURN the
 * baked {@link MultipartBakedModel} is paired with this instance's unbaked
 * {@code selectors} plus the owning block id (from
 * {@code stateContainer.getOwner()}, both javap-verified MCP field names).
 *
 * <p>Predicates ({@code Predicate<BlockState>}) are arbitrary lambdas and
 * cannot be serialized, so the unbaked {@link Selector} condition trees are
 * staged instead and predicates are rebuilt via
 * {@code Selector#getPredicate} on LOAD (see {@code ModelModule} and
 * {@code CacheGson}). Forge's {@code ModelLoader} extends vanilla
 * {@code ModelBakery} without overriding the multipart path, so this hook
 * sees every multipart bake; models replaced afterwards by
 * {@code onPostBakeEvent} simply miss the (identity-keyed) staging lookup
 * and fall back to vanilla, like any unstaged model.
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline (see {@code PORTING_NOTES.md}).
 */
@Mixin(value = Multipart.class, remap = false)
public abstract class MultipartBakeMixin {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-model");

    @Shadow(remap = false)
    private List<Selector> selectors;

    @Shadow(remap = false)
    private StateContainer<Block, BlockState> stateContainer;

    @Inject(method = "bakeModel", at = @At("RETURN"), remap = false)
    private void dashloader$captureSelectors(ModelBakery bakery,
            Function<RenderMaterial, TextureAtlasSprite> textureGetter, IModelTransform transform,
            ResourceLocation location, CallbackInfoReturnable<IBakedModel> cir) {
        if (DashCacheBackend.getStatus() != CacheStatus.SAVE || !ModelModule.isActive()) {
            return;
        }
        try {
            if (!(cir.getReturnValue() instanceof MultipartBakedModel)) {
                return;
            }
            Block owner = stateContainer.getOwner();
            if (owner == null) {
                return;
            }
            ResourceLocation ownerId = owner.getRegistryName();
            if (ownerId == null) {
                return;
            }
            ModelModule.stageMultipartSelectors((MultipartBakedModel) cir.getReturnValue(),
                    new ArrayList<>(selectors), ownerId);
        } catch (Throwable t) {
            LOGGER.warn("DashLoader multipart staging failed for {}.", location, t);
        }
    }
}
