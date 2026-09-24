package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.client.renderer.model.multipart.AndCondition;
import net.minecraft.client.renderer.model.multipart.ICondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the sub-conditions of an {@link AndCondition} for
 * SAVE staging/serialization.
 *
 * <p>1.16.5 MCP field name verified via {@code javap} against the mapped
 * Forge jar: {@code conditions} (private final {@code Iterable}).
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline (see {@code PORTING_NOTES.md}).
 */

/**
 * @author Malionaro
 */
@Mixin(value = AndCondition.class, remap = false)
public interface AndConditionAccessor {
    @Accessor("conditions")
    Iterable<? extends ICondition> getConditions();
}
