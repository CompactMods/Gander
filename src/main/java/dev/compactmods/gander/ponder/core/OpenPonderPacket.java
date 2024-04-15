package dev.compactmods.gander.ponder.core;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.gui.ScreenOpener;
import dev.compactmods.gander.ponder.PonderRegistry;
import dev.compactmods.gander.ponder.ui.PonderUI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;

public record OpenPonderPacket(String scene) implements CustomPacketPayload {

	public static final ResourceLocation ID = GanderLib.asResource("open_scene");

	public OpenPonderPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(scene);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static final IPlayPayloadHandler<OpenPonderPacket> HANDLER = (pkt, ctx) -> {
		ctx.workHandler().submitAsync(() -> {
			if (FMLEnvironment.dist.isClient())
				handleOnClient(pkt.scene);
		});
	};

	private static void handleOnClient(String scene) {
		ResourceLocation id = new ResourceLocation(scene);
		if (!PonderRegistry.ALL.containsKey(id)) {
			GanderLib.LOGGER.error("Could not find ponder scenes for item: " + id);
			return;
		}

		ScreenOpener.transitionTo(PonderUI.of(id));
	}
}
