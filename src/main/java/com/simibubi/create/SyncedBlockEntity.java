package com.simibubi.create;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.PacketDistributor;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class SyncedBlockEntity extends BlockEntity {

	public SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public CompoundTag getUpdateTag() {
		return writeClient(new CompoundTag());
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		readClient(tag);
	}

	@Override
	public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
		CompoundTag tag = packet.getTag();
		readClient(tag == null ? new CompoundTag() : tag);
	}

	// Special handling for client update packets
	public void readClient(CompoundTag tag) {
		load(tag);
	}

	// Special handling for client update packets
	public CompoundTag writeClient(CompoundTag tag) {
		saveAdditional(tag);
		return tag;
	}

}
