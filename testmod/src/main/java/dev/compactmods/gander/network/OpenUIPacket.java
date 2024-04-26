package dev.compactmods.gander.network;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.client.gui.GanderUI;
import dev.compactmods.gander.client.gui.ScreenOpener;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenUIPacket(ResourceLocation scene) implements CustomPacketPayload {

	public static final Type<OpenUIPacket> ID = new Type<>(GanderLib.asResource("open_scene"));

	public static final StreamCodec<? super ByteBuf, OpenUIPacket> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(OpenUIPacket::new, OpenUIPacket::scene);

	public void handle(IPayloadContext context) {
		if (FMLEnvironment.dist.isClient())
			ScreenOpener.open(new GanderUI(scene));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}
}
