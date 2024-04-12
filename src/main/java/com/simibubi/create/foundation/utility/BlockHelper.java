package com.simibubi.create.foundation.utility;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

public class BlockHelper {

	public static void destroyBlockAs(Level world, BlockPos pos, @Nullable Player player, ItemStack usedTool,
		float effectChance, Consumer<ItemStack> droppedItemCallback) {
		FluidState fluidState = world.getFluidState(pos);
		BlockState state = world.getBlockState(pos);

		if (world.random.nextFloat() < effectChance)
			world.levelEvent(2001, pos, Block.getId(state));
		BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;

		if (player != null) {
			BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, state, player);
			MinecraftForge.EVENT_BUS.post(event);
			if (event.isCanceled())
				return;

			if (event.getExpToDrop() > 0 && world instanceof ServerLevel)
				state.getBlock()
					.popExperience((ServerLevel) world, pos, event.getExpToDrop());

			usedTool.mineBlock(world, state, pos, player);
			player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
		}

		if (world instanceof ServerLevel && world.getGameRules()
			.getBoolean(GameRules.RULE_DOBLOCKDROPS) && !world.restoringBlockSnapshots
			&& (player == null || !player.isCreative())) {
			for (ItemStack itemStack : Block.getDrops(state, (ServerLevel) world, pos, blockEntity, player, usedTool))
				droppedItemCallback.accept(itemStack);

			// Simulating IceBlock#playerDestroy. Not calling method directly as it would drop item
			// entities as a side-effect
			if (state.getBlock() instanceof IceBlock && usedTool.getEnchantmentLevel(Enchantments.SILK_TOUCH) == 0) {
				if (world.dimensionType()
					.ultraWarm())
					return;

				 BlockState blockstate = world.getBlockState(pos.below());
		         if (blockstate.blocksMotion() || blockstate.liquid())
					world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
				return;
			}

			state.spawnAfterBreak((ServerLevel) world, pos, ItemStack.EMPTY, true);
		}

		world.setBlockAndUpdate(pos, fluidState.createLegacyBlock());
	}
}
