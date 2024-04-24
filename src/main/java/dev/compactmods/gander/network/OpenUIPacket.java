package dev.compactmods.gander.network;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.client.gui.ScreenOpener;
import dev.compactmods.gander.client.gui.PonderUI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;

public record OpenUIPacket(ResourceLocation scene) implements CustomPacketPayload {

	public static final ResourceLocation ID = GanderLib.asResource("open_scene");

	public OpenUIPacket(FriendlyByteBuf buffer) {
		this(buffer.readResourceLocation());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeResourceLocation(scene);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static final IPlayPayloadHandler<OpenUIPacket> HANDLER = (pkt, ctx) -> {
		ctx.workHandler().submitAsync(() -> {
			if (FMLEnvironment.dist.isClient())
				handleOnClient(pkt.scene);
		});
	};

	private static void handleOnClient(ResourceLocation scene) {
		ScreenOpener.open(new PonderUI(scene));
	}
}
