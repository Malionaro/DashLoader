package dev.notalpha.dashloader.mixin.option.cache.sprite.stitch;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.notalpha.dashloader.DashLoader;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.sprite.stitch.DashTextureStitcher;
import dev.notalpha.dashloader.client.sprite.stitch.SpriteStitcherModule;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.client.texture.TextureStitcher;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(SpriteLoader.class)
public final class StitchSpriteLoaderMixin {
	@Shadow
	@Final
	private Identifier id;

	@Shadow
	@Mutable
	private int width;

	@Shadow
	@Mutable
	private int height;

	/**
	 * {@code SpriteLoader.fromAtlas} passes the previous atlas dimensions and
	 * vanilla then uses {@code max(stitched, previous)} as the atlas size. When a
	 * cached packing is served, that keeps the atlas at the size of the pack
	 * that was active before (e.g. 4096x2048 after unloading a 64x pack) while
	 * only the small cached image is uploaded, so the untouched part of the
	 * texture still holds the old pack (and the mip levels are never rewritten).
	 * Pin the atlas to the cached packing so texture and image always match.
	 */
	@Inject(method = "<init>", at = @At("RETURN"))
	private void dashloaderPinToCachedSize(Identifier id, int maxTextureSize, int width, int height, CallbackInfo ci) {
		var map = SpriteStitcherModule.STITCHERS_LOAD.get(CacheStatus.LOAD);
		var data = map == null ? null : map.get(id);
		if (data != null) {
			this.width = data.width;
			this.height = data.height;
		} else {
			// No cached packing (fresh cache): do not inherit the size of the
			// atlas that is being replaced, otherwise a 64x pack would keep its
			// 4096x2048 texture and the old pack stays visible in it.
			this.width = 0;
			this.height = 0;
		}
	}

	@WrapOperation(
			method = "stitch",
			at = @At(value = "NEW", target = "(III)Lnet/minecraft/client/texture/TextureStitcher;")
	)
	private TextureStitcher<?> dashloaderStitcherLoad(int maxWidth, int maxHeight, int mipLevel, Operation<TextureStitcher<?>> original) {
		var map = SpriteStitcherModule.STITCHERS_LOAD.get(CacheStatus.LOAD);
		if (map != null) {
			var data = map.get(id);
			if (data != null) {
				if (data.matches(maxWidth, maxHeight, mipLevel)) {
					return new DashTextureStitcher<>(maxWidth, maxHeight, mipLevel, data);
				}
				// Stitch parameters changed (e.g. mipmap video settings):
				// cached packing is stale, stitch vanilla instead of corrupting the atlas.
				DashLoader.LOG.info("Stitch parameters changed for {}, re-stitching vanilla.", id);
			}
		}

		return original.call(maxWidth, maxHeight, mipLevel);
	}

	@Inject(
			method = "stitch",
			at = @At(value = "RETURN"),
			locals = LocalCapture.CAPTURE_FAILSOFT
	)
	private void dashloaderStitcherSave(List<SpriteContents> sprites, int mipLevel, Executor executor, CallbackInfoReturnable<SpriteLoader.StitchResult> cir, int i, TextureStitcher<SpriteContents> textureStitcher) {
		SpriteStitcherModule.STITCHERS_SAVE.visit(CacheStatus.SAVE, map -> map.add(Pair.of(id, textureStitcher)));
	}
}
