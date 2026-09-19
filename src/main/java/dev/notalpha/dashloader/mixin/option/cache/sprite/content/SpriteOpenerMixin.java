package dev.notalpha.dashloader.mixin.option.cache.sprite.content;

import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.sprite.content.SpriteContentModule;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteOpener;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * In 1.21.11 {@code SpriteOpener} is a functional interface; the vanilla implementation
 * is the lambda returned by {@code SpriteOpener.create}. Wrapping that instance preserves
 * the LOAD (return cached contents) / SAVE (store loaded contents) behavior without
 * targeting the old {@code method_52851} class which no longer exists.
 */
@Mixin(SpriteOpener.class)
public interface SpriteOpenerMixin {
	@Inject(
			method = "create",
			at = @At(value = "RETURN"),
			cancellable = true
	)
	private static void dashloaderWrapOpener(Set<ResourceMetadataSerializer<?>> additionalMetadata, CallbackInfoReturnable<SpriteOpener> cir) {
		SpriteOpener original = cir.getReturnValue();
		cir.setReturnValue((id, resource) -> {
			var dashSpriteData = SpriteContentModule.SOURCE.get(CacheStatus.LOAD);
			if (dashSpriteData != null) {
				SpriteContents cached = dashSpriteData.get(id);
				if (cached != null) {
					return cached;
				}
			}

			SpriteContents result = original.loadSprite(id, resource);

			var saveData = SpriteContentModule.SOURCE.get(CacheStatus.SAVE);
			if (saveData != null && result != null) {
				if (saveData.containsKey(id)) { // filter out sprites with the same id
					saveData.put(id, null);
				} else {
					saveData.put(id, result);
				}
			}
			return result;
		});
	}
}
