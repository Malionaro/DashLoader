package dev.notalpha.dashloader.mixin.option.cache.sprite.content;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.notalpha.dashloader.api.cache.CacheStatus;
import dev.notalpha.dashloader.client.sprite.content.SpriteContentModule;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.client.texture.SpriteOpener;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

/**
 * 1.21.9: SpriteOpener is a functional interface (loadSprite + static create).
 * The old method_52851 target no longer exists, so instead wrap the
 * SpriteOpener.create call in SpriteLoader.load with a caching delegate:
 * LOAD returns the cached SpriteContents without touching disk, SAVE stores it.
 */
@Mixin(SpriteLoader.class)
public class SpriteOpenerMixin {
	@WrapOperation(
			method = "load",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/SpriteOpener;create(Ljava/util/Set;)Lnet/minecraft/client/texture/SpriteOpener;")
	)
	private SpriteOpener dashloader$wrapOpener(Set<ResourceMetadataSerializer<?>> additionalMetadata, Operation<SpriteOpener> original) {
		SpriteOpener delegate = original.call(additionalMetadata);
		return (id, resource) -> {
			var loadData = SpriteContentModule.SOURCE.get(CacheStatus.LOAD);
			if (loadData != null) {
				SpriteContents cached = loadData.get(id);
				if (cached != null) {
					return cached;
				}
			}
			SpriteContents result = delegate.loadSprite(id, resource);
			var saveData = SpriteContentModule.SOURCE.get(CacheStatus.SAVE);
			if (saveData != null && result != null) {
				if (saveData.containsKey(id)) { // filter out sprites with the same id
					saveData.put(id, null);
				} else {
					saveData.put(id, result);
				}
			}
			return result;
		};
	}
}
