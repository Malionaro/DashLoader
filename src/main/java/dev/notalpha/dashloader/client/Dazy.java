package dev.notalpha.dashloader.client;

import net.minecraft.client.render.model.ErrorCollectingSpriteGetter;
import org.jetbrains.annotations.Nullable;

// its lazy, but dash! Used for resolution of sprites.
public abstract class Dazy<V> {
	@Nullable
	private transient V loaded;

	protected abstract V resolve(ErrorCollectingSpriteGetter spriteLoader);

	public V get(ErrorCollectingSpriteGetter spriteLoader) {
		if (loaded != null) {
			return loaded;
		}

		loaded = resolve(spriteLoader);
		return loaded;
	}
}
