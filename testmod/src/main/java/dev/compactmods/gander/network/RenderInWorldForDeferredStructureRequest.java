package dev.compactmods.gander.network;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.client.gui.ScreenOpener;
import dev.compactmods.gander.world.InWorldRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import org.jetbrains.annotations.NotNull;

public record RenderInWorldForDeferredStructureRequest(ResourceLocation structureId) implements CustomPacketPayload
{
	public static final Type<RenderInWorldForDeferredStructureRequest> ID = new Type<>(GanderLib.asResource("in_world_open_scene"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RenderInWorldForDeferredStructureRequest> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, RenderInWorldForDeferredStructureRequest::structureId,
			RenderInWorldForDeferredStructureRequest::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}

	public static final IPayloadHandler<RenderInWorldForDeferredStructureRequest> HANDLER = (pkt, ctx) -> {
		if(FMLEnvironment.dist.isClient())
			ctx.enqueueWork(() -> InWorldRenderer.forStructure(pkt.structureId));
	};
}
