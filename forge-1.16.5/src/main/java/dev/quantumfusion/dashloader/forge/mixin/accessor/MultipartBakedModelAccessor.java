package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.function.Predicate;

/**
 * Mirrors modern {@code MultipartBakedModelAccessor}
 * ({@code fabric-1.21.4}): exposes the predicate&rarr;model selector list.
 *
 * <p>1.16.5 MCP field name verified via {@code javap} against the mapped
 * Forge jar: {@code selectors} on {@code MultipartBakedModel}.
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline — same pending item as
 * {@code ModelManagerCacheMixin}.
 */
@Mixin(value = MultipartBakedModel.class, remap = false)
public interface MultipartBakedModelAccessor {
    @Accessor("selectors")
    List<Pair<Predicate<BlockState>, IBakedModel>> getSelectors();
}
