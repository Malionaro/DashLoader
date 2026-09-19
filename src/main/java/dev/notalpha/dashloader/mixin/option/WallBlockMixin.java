package dev.notalpha.dashloader.mixin.option;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(WallBlock.class)
public abstract class WallBlockMixin extends Block implements SimpleWaterloggedBlock {
	@Unique
	private static final int LENGTH = WallSide.values().length;
	@Shadow
	@Final
	public static BooleanProperty UP;
	@Shadow
	@Final
	public static EnumProperty<WallSide> EAST_SHAPE;
	@Shadow
	@Final
	public static EnumProperty<WallSide> NORTH_SHAPE;
	@Shadow
	@Final
	public static EnumProperty<WallSide> WEST_SHAPE;
	@Shadow
	@Final
	public static EnumProperty<WallSide> SOUTH_SHAPE;
	@Shadow
	@Final
	public static BooleanProperty WATERLOGGED;
	@Unique
	private static VoxelShape[][][][][] SHAPE_CACHE;
	@Unique
	private static VoxelShape[][][][][] COLLISION_CACHE;

	public WallBlockMixin(Properties settings) {
		super(settings);
	}

	@Inject(method = "getShapeMap", at = @At(value = "HEAD"), cancellable = true)
	private void getShapeMapCache(float f, float g, float h, float i, float j, float k, CallbackInfoReturnable<Map<BlockState, VoxelShape>> cir) {
		if (this.isCommon(f, g, i)) {
			if (this.isShape(h, j, k)) {
				if (SHAPE_CACHE != null) {
					cir.setReturnValue(this.createFromCache(SHAPE_CACHE));
				}
			} else if (this.isCollision(h, j, k)) {
				if (COLLISION_CACHE != null) {
					cir.setReturnValue(this.createFromCache(COLLISION_CACHE));
				}
			}
		}
	}

	@Inject(method = "getShapeMap", at = @At(value = "RETURN"))
	private void getShapeMapCacheCreate(float f, float g, float h, float i, float j, float k, CallbackInfoReturnable<Map<BlockState, VoxelShape>> cir) {
		if (SHAPE_CACHE == null || COLLISION_CACHE == null) {
			if (this.isCommon(f, g, i)) {
				if (this.isShape(h, j, k)) {
					if (SHAPE_CACHE == null) {
						SHAPE_CACHE = new VoxelShape[2][LENGTH][LENGTH][LENGTH][LENGTH];
						this.createCache(SHAPE_CACHE, cir.getReturnValue());
					}
				} else if (this.isCollision(h, j, k)) {
					if (COLLISION_CACHE == null) {
						COLLISION_CACHE = new VoxelShape[2][LENGTH][LENGTH][LENGTH][LENGTH];
						this.createCache(COLLISION_CACHE, cir.getReturnValue());
					}
				}
			}
		}
	}

	@Unique
	private ImmutableMap<BlockState, VoxelShape> createFromCache(VoxelShape[][][][][] rawCache) {
		ImmutableMap.Builder<BlockState, VoxelShape> builder = ImmutableMap.builder();
		for (Boolean up : UP.getPossibleValues()) {
			VoxelShape[][][][] cache = up ? rawCache[1] : rawCache[0];
			for (WallSide east : EAST_SHAPE.getPossibleValues()) {
				for (WallSide north : NORTH_SHAPE.getPossibleValues()) {
					for (WallSide west : WEST_SHAPE.getPossibleValues()) {
						for (WallSide south : SOUTH_SHAPE.getPossibleValues()) {
							final VoxelShape cached = this.getCached(cache, east, north, west, south);

							BlockState blockState = this.defaultBlockState()
									.setValue(UP, up)
									.setValue(EAST_SHAPE, east)
									.setValue(WEST_SHAPE, west)
									.setValue(NORTH_SHAPE, north)
									.setValue(SOUTH_SHAPE, south);

							builder.put(blockState.setValue(WATERLOGGED, false), cached);
							builder.put(blockState.setValue(WATERLOGGED, true), cached);
						}
					}
				}
			}
		}
		return builder.build();
	}

	@Unique
	private void createCache(VoxelShape[][][][][] rawCache, Map<BlockState, VoxelShape> map) {
		for (Boolean up : UP.getPossibleValues()) {
			VoxelShape[][][][] cache = up ? rawCache[1] : rawCache[0];
			for (WallSide east : EAST_SHAPE.getPossibleValues()) {
				for (WallSide north : NORTH_SHAPE.getPossibleValues()) {
					for (WallSide west : WEST_SHAPE.getPossibleValues()) {
						for (WallSide south : SOUTH_SHAPE.getPossibleValues()) {

							BlockState blockState = this.defaultBlockState()
									.setValue(UP, up)
									.setValue(EAST_SHAPE, east)
									.setValue(WEST_SHAPE, west)
									.setValue(NORTH_SHAPE, north)
									.setValue(SOUTH_SHAPE, south)
									.setValue(WATERLOGGED, false);

							this.setCached(cache, east, north, west, south, map.get(blockState));
						}
					}
				}
			}
		}
	}

	//shape 4.0F, 3.0F, 16.0F, 0.0F, 14.0F, 16.0F
	//collision 4.0F, 3.0F, 24.0F, 0.0F, 24.0F, 24.0F
	@Unique
	private boolean isShape(float h, float j, float k) {
		return h == 16.0F && j == 14.0F && k == 16.0F;
	}

	@Unique
	private boolean isCollision(float h, float j, float k) {
		return h == 24.0F && j == 24.0F && k == 24.0F;
	}

	@Unique
	private boolean isCommon(float f, float g, float i) {
		return f == 4.0F && g == 3.0F && i == 0.0F;
	}

	@Unique
	private VoxelShape getCached(VoxelShape[][][][] cache, WallSide east, WallSide north, WallSide west, WallSide south) {
		return cache[east.ordinal()][north.ordinal()][west.ordinal()][south.ordinal()];
	}

	@Unique
	private void setCached(VoxelShape[][][][] cache, WallSide east, WallSide north, WallSide west, WallSide south, VoxelShape shape) {
		cache[east.ordinal()][north.ordinal()][west.ordinal()][south.ordinal()] = shape;
	}
}
