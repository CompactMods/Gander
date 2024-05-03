package dev.compactmods.gander.network;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.client.gui.ScreenOpener;
import dev.compactmods.gander.client.gui.GanderUI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import org.jetbrains.annotations.NotNull;

public record OpenUIPacket(ResourceLocation scene) implements CustomPacketPayload {

	public static final Type<OpenUIPacket> ID = new Type<>(GanderLib.asResource("open_scene"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenUIPacket> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, OpenUIPacket::scene,
			OpenUIPacket::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}

	public static final IPayloadHandler<OpenUIPacket> HANDLER = (pkt, ctx) -> {
		ctx.enqueueWork(() -> {
			if (FMLEnvironment.dist.isClient())
				handleOnClient(pkt.scene);
		});
	};

	private static void handleOnClient(ResourceLocation scene) {
		ScreenOpener.open(new GanderUI(scene));
	}
}
