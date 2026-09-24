package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Mirrors modern {@code WeightedBakedModelAccessor}
 * ({@code fabric-1.21.4}): exposes the weighted entry list.
 *
 * <p>1.16.5 MCP field name verified via {@code javap} against the mapped
 * Forge jar: {@code models} on {@code WeightedBakedModel}. The entry type
 * ({@code WeightedBakedModel$WeightedModel}) is package-private there, so
 * this accessor returns a raw list and
 * {@link WeightedModelAccessor} reads the entries.
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline — same pending item as
 * {@code ModelManagerCacheMixin}.
 */
@Mixin(value = WeightedBakedModel.class, remap = false)
public interface WeightedBakedModelAccessor {
    @Accessor("models")
    List<?> getModels();
}
