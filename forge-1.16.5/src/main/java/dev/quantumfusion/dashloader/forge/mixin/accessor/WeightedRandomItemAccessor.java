package dev.quantumfusion.dashloader.forge.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * MCP name (dev workspace is MCP-named, {@code remap = false}):
 * {@code WeightedRandom$Item.itemWeight} ({@code public final int}).
 */
@Mixin(targets = "net.minecraft.util.WeightedRandom$Item", remap = false)
public interface WeightedRandomItemAccessor {
    @Accessor("itemWeight")
    int getItemWeight();
}
