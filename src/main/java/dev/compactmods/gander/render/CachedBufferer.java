package dev.compactmods.gander.render;

import dev.compactmods.gander.CreateClient;
import dev.compactmods.gander.render.SuperByteBufferCache.Compartment;

import net.minecraft.world.level.block.state.BlockState;

public class CachedBufferer {

	public static final Compartment<BlockState> GENERIC_BLOCK = new Compartment<>();

	public static SuperByteBuffer block(BlockState toRender) {
		return block(GENERIC_BLOCK, toRender);
	}

	public static SuperByteBuffer block(Compartment<BlockState> compartment, BlockState toRender) {
		return CreateClient.BUFFER_CACHE.get(compartment, toRender, () -> VirtualRenderHelper.bufferBlock(toRender));
	}
}
