package dev.quantumfusion.dashloader.forge.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * SRG names (no refmap pipeline, {@code remap = false}):
 * {@code WeightedRandom$Item.itemWeight} is {@code field_76292_a}.
 */
@Mixin(targets = "net.minecraft.util.WeightedRandom$Item", remap = false)
public interface WeightedRandomItemAccessor {
    @Accessor("field_76292_a")
    int getItemWeight();
}
