package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.multipart.Multipart;
import net.minecraft.state.StateContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the owning state container of an unbaked {@link Multipart} model.
 *
 * <p>1.16.5 MCP field name verified via {@code javap} against the mapped
 * snapshot jar: {@code stateContainer} (private final
 * {@code StateContainer<Block, BlockState>}, no public getter). Used by the
 * apply-TAIL fallback in {@code ModelManagerCacheMixin} which re-stages
 * unbaked selectors from {@code ModelBakery#getUnbakedModel} when the
 * bake-time hook missed (e.g. cache was IDLE during baking).
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline (see {@code PORTING_NOTES.md}).
 */
@Mixin(value = Multipart.class, remap = false)
public interface MultipartAccessor {
    @Accessor("field_188140_b")
    StateContainer<Block, BlockState> getStateContainer();
}
