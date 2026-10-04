package dev.notalpha.dashloader.client;

import net.minecraft.client.render.model.ErrorCollectingSpriteGetter;
import org.jetbrains.annotations.Nullable;

// its lazy, but dash! Used for resolution of sprites.
public abstract class Dazy<V> {
	@Nullable
	private transient volatile V loaded;

	protected abstract V resolve(ErrorCollectingSpriteGetter spriteLoader);

	public V get(spriteLoader) {
		V local = this.loaded;
		if (local != null) {
			return local;
		}

		synchronized (this) {
			local = this.loaded;
			if (local == null) {
				local = resolve(spriteLoader);
				this.loaded = local;
			}
			return local;
		}
	}
}



