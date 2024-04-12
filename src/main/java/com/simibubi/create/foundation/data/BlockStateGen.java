
package com.simibubi.create.foundation.data;

import java.util.function.Function;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;

public class BlockStateGen {

	public static <T extends Block> void axisBlock(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		Function<BlockState, ModelFile> modelFunc) {
		axisBlock(ctx, prov, modelFunc, false);
	}

	public static <T extends Block> void axisBlock(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		Function<BlockState, ModelFile> modelFunc, boolean uvLock) {
		prov.getVariantBuilder(ctx.getEntry())
			.forAllStatesExcept(state -> {
				Axis axis = state.getValue(BlockStateProperties.AXIS);
				return ConfiguredModel.builder()
					.modelFile(modelFunc.apply(state))
					.uvLock(uvLock)
					.rotationX(axis == Axis.Y ? 0 : 90)
					.rotationY(axis == Axis.X ? 90 : axis == Axis.Z ? 180 : 0)
					.build();
			}, BlockStateProperties.WATERLOGGED);
	}

	public static <T extends Block> void horizontalAxisBlock(DataGenContext<Block, T> ctx,
		RegistrateBlockstateProvider prov, Function<BlockState, ModelFile> modelFunc) {
		prov.getVariantBuilder(ctx.getEntry())
			.forAllStates(state -> {
				Axis axis = state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
				return ConfiguredModel.builder()
					.modelFile(modelFunc.apply(state))
					.rotationY(axis == Axis.X ? 90 : 0)
					.build();
			});
	}

	public static <T extends Block> void cubeAll(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
		String textureSubDir, String name) {
		String texturePath = "block/" + textureSubDir + name;
		prov.simpleBlock(ctx.get(), prov.models()
			.cubeAll(ctx.getName(), prov.modLoc(texturePath)));
	}

	public static <P extends TrapDoorBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> uvLockedTrapdoorBlock(
		P block, ModelFile bottom, ModelFile top, ModelFile open) {
		return (c, p) -> {
			p.getVariantBuilder(block)
				.forAllStatesExcept(state -> {
					int xRot = 0;
					int yRot = ((int) state.getValue(TrapDoorBlock.FACING)
						.toYRot()) + 180;
					boolean isOpen = state.getValue(TrapDoorBlock.OPEN);
					if (!isOpen)
						yRot = 0;
					yRot %= 360;
					return ConfiguredModel.builder()
						.modelFile(isOpen ? open : state.getValue(TrapDoorBlock.HALF) == Half.TOP ? top : bottom)
						.rotationX(xRot)
						.rotationY(yRot)
						.uvLock(!isOpen)
						.build();
				}, TrapDoorBlock.POWERED, TrapDoorBlock.WATERLOGGED);
		};
	}
}
