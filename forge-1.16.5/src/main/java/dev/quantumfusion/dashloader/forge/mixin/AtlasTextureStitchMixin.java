package dev.quantumfusion.dashloader.forge.mixin;

import dev.quantumfusion.dashloader.forge.cache.CacheStatus;
import dev.quantumfusion.dashloader.forge.cache.DashCacheBackend;
import dev.quantumfusion.dashloader.forge.mixin.accessor.AtlasSheetDataAccessor;
import dev.quantumfusion.dashloader.forge.sprite.DashSpriteContents;
import dev.quantumfusion.dashloader.forge.sprite.SpriteContentModule;
import dev.quantumfusion.dashloader.forge.sprite.stitch.DashTextureStitcher;
import dev.quantumfusion.dashloader.forge.sprite.stitch.SpriteStitcherModule;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Atlas stitching hook for the Forge 1.16.5 port.
 *
 * <p>Modern reference ({@code fabric-26.3}): {@code StitchSpriteLoaderMixin}
 * wraps {@code SpriteLoader.stitch} — swapping in a
 * {@code DashTextureStitcher} when cached packing matches
 * ({@code ExportedData#matches}, including the stitch-parameter validation)
 * and recording the finished stitcher on SAVE. In 1.16.5 stitching lives in
 * {@link AtlasTexture#stitch}, which builds a concrete {@link Stitcher}
 * internally (verified via {@code javap -c}: a single
 * {@code new Stitcher(III)} inside {@code stitch}).
 *
 * <p>This mixin therefore:
 * <ul>
 *   <li>{@link Redirect} on that {@code new Stitcher} — returns a
 *       {@link DashTextureStitcher} when LOAD packing for this atlas exists
 *       and stitch parameters match, vanilla {@link Stitcher} otherwise
 *       (stitch-parameter validation equivalent, including the
 *       "parameters changed, re-stitch vanilla" log).</li>
 *   <li>SAVE sprite-content staging at {@code stitch} RETURN from the
 *       finished {@code SheetData} sprites (per-sprite skip resilience +
 *       keep-first via {@code putIfAbsent} semantics — modern
 *       {@code SpriteOpenerMixin} parity).</li>
 * </ul>
 *
 * <p>Pixel serving limitation (documented, not silent): cached packing
 * (positions) is reused on LOAD, but pixel decoding still runs vanilla —
 * per-sprite image replacement inside {@code getStitchedSprites} has no
 * clean 1.16.5 hook. The sprite-content JSON round-trips (backend parity)
 * and feeds future work; see {@code PORTING_NOTES.md}.
 *
 * <p>Dev workspace is MCP-named so {@code remap = false}; production needs
 * the refmap pipeline (see {@code PORTING_NOTES.md}).
 */
@Mixin(value = AtlasTexture.class, remap = false)
public abstract class AtlasTextureStitchMixin {
    private static final Logger LOGGER = LogManager.getLogger("dashloader-stitch");

    /** Atlas currently being stitched on this thread (read by {@code StitcherCaptureMixin}). */
    @Unique
    static final ThreadLocal<ResourceLocation> CURRENT_ATLAS = new ThreadLocal<>();

    /** Atlas id captured from the constructor (no reliable name-based shadow exists on 1.16.5). */
    @Unique
    private ResourceLocation dashloader$atlasId;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void dashloader$captureId(ResourceLocation location, CallbackInfo ci) {
        this.dashloader$atlasId = location;
    }

    @Redirect(method = "stitch", at = @At(value = "NEW", target = "net/minecraft/client/renderer/texture/Stitcher"), remap = false)
    private Stitcher dashloader$newStitcher(int maxWidth, int maxHeight, int mipLevel) {
        ResourceLocation atlasId = this.dashloader$atlasId;
        if (SpriteStitcherModule.isActive() && DashCacheBackend.getStatus() == CacheStatus.LOAD) {
            DashTextureStitcher.ExportedData data = SpriteStitcherModule.STITCHERS_LOAD.get(atlasId);
            if (data != null) {
                if (data.matches(maxWidth, maxHeight, mipLevel)) {
                    LOGGER.debug("DashLoader: reusing cached stitching for {}.", atlasId);
                    return new DashTextureStitcher(maxWidth, maxHeight, mipLevel, data);
                }
                // Stitch parameters changed (e.g. mipmap video setting):
                // cached packing is stale, stitch vanilla instead of corrupting the atlas.
                LOGGER.info("Stitch parameters changed for {}, re-stitching vanilla.", atlasId);
            }
        }
        return new Stitcher(maxWidth, maxHeight, mipLevel);
    }

    @Inject(method = "stitch", at = @At("HEAD"), remap = false)
    private void dashloader$trackAtlas(IResourceManager resourceManager,
            Stream<ResourceLocation> sprites, IProfiler profiler, int mipLevel,
            CallbackInfoReturnable<AtlasTexture.SheetData> cir) {
        try {
            CURRENT_ATLAS.set(this.dashloader$atlasId);
        } catch (Throwable t) {
            LOGGER.warn("DashLoader atlas tracking failed.", t);
        }
    }

    @Inject(method = "stitch", at = @At("RETURN"), remap = false)
    private void dashloader$stageSpriteContents(IResourceManager resourceManager,
            Stream<ResourceLocation> sprites, IProfiler profiler, int mipLevel,
            CallbackInfoReturnable<AtlasTexture.SheetData> cir) {
        try {
            CURRENT_ATLAS.remove();
        } catch (Throwable t) {
            LOGGER.warn("DashLoader atlas tracking cleanup failed.", t);
        }
        if (DashCacheBackend.getStatus() != CacheStatus.SAVE || !SpriteContentModule.isActive()) {
            return;
        }
        AtlasTexture.SheetData data = cir.getReturnValue();
        if (data == null) {
            return;
        }
        List<TextureAtlasSprite> stitched;
        try {
            stitched = ((AtlasSheetDataAccessor) data).getSprites();
        } catch (Throwable t) {
            LOGGER.warn("DashLoader sprite staging failed (sheet accessor).", t);
            return;
        }
        if (stitched == null) {
            return;
        }
        int staged = 0;
        for (TextureAtlasSprite sprite : stitched) {
            if (sprite == null) {
                continue;
            }
            try {
                ResourceLocation id = sprite.getName();
                // Keep-first duplicates (modern parity): double reloads must
                // not overwrite (or null-poison) the first result.
                if (SpriteContentModule.SAVE.containsKey(id)) {
                    continue;
                }
                SpriteContentModule.SAVE.put(id, DashSpriteContents.fromSprite(sprite));
                staged++;
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping uncacheable sprite {}: {}", sprite, e.getMessage());
            } catch (Throwable t) {
                LOGGER.warn("Skipping uncacheable sprite (unexpected).", t);
            }
        }
        LOGGER.debug("DashLoader: staged {} sprite contents for caching.", staged);
    }
}
