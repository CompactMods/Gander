package dev.compactmods.gander.network;

import java.util.function.Supplier;

import dev.compactmods.gander.client.gui.ScreenOpener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

public record OpenGanderUiForDeferredStructureRequest(ResourceLocation structureId) {
	public OpenGanderUiForDeferredStructureRequest(FriendlyByteBuf buffer) {
		this(buffer.readResourceLocation());
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeResourceLocation(structureId);
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> ScreenOpener.forStructure(structureId));
		context.get().setPacketHandled(true);
	}
}
