package dev.compactmods.gander.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public record SceneDataRequest(ResourceLocation templateID) implements CustomPacketPayload {
	public static final Type<SceneDataRequest> ID = new Type<>(new ResourceLocation("gander", "scene_data"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SceneDataRequest> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, SceneDataRequest::templateID,
			SceneDataRequest::new
	);


	public static final IPayloadHandler<SceneDataRequest> HANDLER = (req, ctx) -> {
		ctx.enqueueWork(() -> {
			final var templateManager = ServerLifecycleHooks.getCurrentServer().getStructureManager();
			templateManager.get(req.templateID).ifPresent(t -> {
				ctx.reply(new SceneDataResponse(Component.literal(req.templateID.toString()), t));
			});
		});
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
