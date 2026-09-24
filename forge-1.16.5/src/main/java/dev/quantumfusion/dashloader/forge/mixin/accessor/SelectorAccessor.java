package dev.quantumfusion.dashloader.forge.mixin.accessor;

import net.minecraft.client.renderer.model.multipart.ICondition;
import net.minecraft.client.renderer.model.multipart.Selector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the unbaked condition tree of a multipart {@link Selector} for
 * SAVE staging/serialization.
 *
 * <p>1.16.5 MCP field name verified via {@code javap} against the mapped
 * Forge jar: {@code condition} (private final {@code ICondition}; only the
 * variant list has a public getter). The condition tree ({@code AndCondition},
 * {@code OrCondition}, {@code PropertyValueCondition}, {@code TRUE},
 * {@code FALSE}) is plain data and JSON-serializable (see
 * {@code CacheGson}); the predicate lambdas built from it are not, so they
 * are rebuilt via {@code getPredicate} on LOAD.
 *
 * <p>Dev workspace is MCP-named, so {@code remap = false}. Production
 * (SRG runtime) needs the refmap pipeline (see {@code PORTING_NOTES.md}).
 */
@Mixin(value = Selector.class, remap = false)
public interface SelectorAccessor {
    @Accessor("condition")
    ICondition getCondition();
}
