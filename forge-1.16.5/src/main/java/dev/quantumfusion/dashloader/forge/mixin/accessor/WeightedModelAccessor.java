package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.WeightedBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads a single {@code WeightedBakedModel$WeightedModel} entry.
 *
 * <p>1.16.5 MCP names verified via {@code javap}: {@code model} (declared on
 * the entry class, {@code protected final IBakedModel}) and
 * {@code itemWeight} (inherited from {@code WeightedRandom$Item},
 * {@code public final int}).
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline — same pending item as
 * {@code ModelManagerCacheMixin}.
 */
@Mixin(targets = "net.minecraft.client.renderer.model.WeightedBakedModel$WeightedModel", remap = false)
public interface WeightedModelAccessor {
    @Accessor("model")
    IBakedModel getModel();
}
