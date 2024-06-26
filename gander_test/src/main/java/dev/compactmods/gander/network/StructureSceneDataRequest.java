package dev.compactmods.gander.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public record StructureSceneDataRequest(ResourceLocation templateID) implements CustomPacketPayload {
	public static final Type<StructureSceneDataRequest> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("gander", "scene_data"));

	public static final StreamCodec<RegistryFriendlyByteBuf, StructureSceneDataRequest> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, StructureSceneDataRequest::templateID,
			StructureSceneDataRequest::new
	);


	public static final IPayloadHandler<StructureSceneDataRequest> HANDLER = (req, ctx) -> {
		ctx.enqueueWork(() -> {
			final var templateManager = ServerLifecycleHooks.getCurrentServer().getStructureManager();
			templateManager.get(req.templateID).ifPresent(t -> {
				ctx.reply(new OpenGanderUiForStructureRequest(Component.literal(req.templateID.toString()), t));
			});
		});
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
