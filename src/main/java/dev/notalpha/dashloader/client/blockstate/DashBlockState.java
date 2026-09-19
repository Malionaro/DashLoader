package dev.notalpha.dashloader.client.blockstate;

import dev.notalpha.dashloader.api.DashObject;
import dev.notalpha.dashloader.api.registry.RegistryReader;
import dev.notalpha.dashloader.api.registry.RegistryWriter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class DashBlockState implements DashObject<BlockState, BlockState> {
	public final int owner;
	public final int pos;

	public DashBlockState(int owner, int pos) {
		this.owner = owner;
		this.pos = pos;
	}

	public DashBlockState(BlockState blockState, RegistryWriter writer) {
		Block block = blockState.getBlock();
		int pos = -1;
		Identifier owner = null;

		var states = block.getStateManager().getStates();
		for (int i = 0; i < states.size(); i++) {
			BlockState state = states.get(i);
			if (state.equals(blockState)) {
				pos = i;
				owner = Registries.BLOCK.getId(block);
				break;
			}
		}

		if (owner == null) {
			throw new RuntimeException("Could not find a blockstate for " + blockState);
		}

		this.owner = writer.add(owner);
		this.pos = pos;
	}

	@Override
	public BlockState export(final RegistryReader reader) {
		final Identifier id = reader.get(this.owner);
		return Registries.BLOCK.get(id).getStateManager().getStates().get(this.pos);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DashBlockState that = (DashBlockState) o;

		if (owner != that.owner) return false;
		return pos == that.pos;
	}

	@Override
	public int hashCode() {
		int result = owner;
		result = 31 * result + pos;
		return result;
	}
}
