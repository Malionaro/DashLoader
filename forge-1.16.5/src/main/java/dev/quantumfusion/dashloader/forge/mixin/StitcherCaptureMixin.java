package dev.quantumfusion.dashloader.forge.mixin;

import dev.quantumfusion.dashloader.forge.cache.CacheStatus;
import dev.quantumfusion.dashloader.forge.cache.DashCacheBackend;
import dev.quantumfusion.dashloader.forge.sprite.stitch.DashTextureStitcher;
import dev.quantumfusion.dashloader.forge.sprite.stitch.SpriteStitcherModule;
import dev.quantumfusion.dashloader.forge.sprite.stitch.StitchState;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures finished stitch runs for SAVE staging.
 *
 * <p>Modern reference ({@code fabric-26.3}): {@code StitchSpriteLoaderMixin}
 * records {@code (atlasId, stitcher)} pairs after {@code stitch()} returns.
 * In 1.16.5 the finished {@link Stitcher} never escapes
 * {@code AtlasTexture#stitch}, so this mixin captures it at TAIL of
 * {@link Stitcher#doStitch()} instead, keyed by the atlas id published by
 * {@code AtlasTextureStitchMixin} for the current thread.
 *
 * <p>Keep-first duplicates (modern parity): an atlas stitched twice in one
 * boot keeps the first capture. MCP field names below ({@code maxWidth} /
 * {@code maxHeight} / {@code mipmapLevelStitcher}) verified via {@code javap}
 * against the mapped snapshot jar; {@code remap = false} (dev workspace is
 * MCP-named).
 */

/**
 * @author Malionaro
 */
@Mixin(value = Stitcher.class, remap = false)
public abstract class StitcherCaptureMixin {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-stitch");

    @Shadow(remap = false)
    private int maxWidth;

    @Shadow(remap = false)
    private int maxHeight;

    @Shadow(remap = false)
    private int mipmapLevelStitcher;

    @Inject(method = "doStitch", at = @At("TAIL"), remap = false)
    private void dashloader$captureStitch(CallbackInfo ci) {
        if (DashCacheBackend.getStatus() != CacheStatus.SAVE || !SpriteStitcherModule.isActive()) {
            return;
        }
        ResourceLocation atlasId;
        try {
            atlasId = StitchState.CURRENT_ATLAS.get();
        } catch (Throwable t) {
            LOGGER.warn("DashLoader stitch capture failed (atlas lookup).", t);
            return;
        }
        if (atlasId == null || SpriteStitcherModule.STITCHERS_SAVE.containsKey(atlasId)) {
            return;
        }
        try {
            SpriteStitcherModule.STITCHERS_SAVE.put(atlasId, DashTextureStitcher.Data.capture(
                    (Stitcher) (Object) this, maxWidth, maxHeight, mipmapLevelStitcher));
        } catch (Throwable t) {
            LOGGER.warn("DashLoader stitch capture failed for {}.", atlasId, t);
        }
    }
}
