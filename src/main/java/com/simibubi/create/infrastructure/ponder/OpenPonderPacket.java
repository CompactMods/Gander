package com.simibubi.create.infrastructure.ponder;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import com.simibubi.create.foundation.ponder.PonderRegistry;

import com.simibubi.create.foundation.ponder.ui.PonderUI;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public class OpenPonderPacket extends SimplePacketBase {

	private final String scene;

	public OpenPonderPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf());
	}

	public OpenPonderPacket(String scene) {
		this.scene = scene;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(scene);
	}

	@Override
	public boolean handle(NetworkEvent.Context context) {
		context.enqueueWork(() -> {
			if (FMLEnvironment.dist.isClient())
				handleOnClient();
		});

		return true;
	}

	private void handleOnClient() {
		ResourceLocation id = new ResourceLocation(scene);
		if (!PonderRegistry.ALL.containsKey(id)) {
			Create.LOGGER.error("Could not find ponder scenes for item: " + id);
			return;
		}

		ScreenOpener.transitionTo(PonderUI.of(id));
	}
}
