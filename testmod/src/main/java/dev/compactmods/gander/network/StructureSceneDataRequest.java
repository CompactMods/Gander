package dev.compactmods.gander.network;

import java.util.function.Supplier;

import dev.compactmods.gander.GanderLib;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

public record StructureSceneDataRequest(ResourceLocation templateID) {
	public StructureSceneDataRequest(FriendlyByteBuf buffer) {
		this(buffer.readResourceLocation());
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeResourceLocation(templateID);
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			var templateManager = ServerLifecycleHooks.getCurrentServer().getStructureManager();
			templateManager.get(templateID).ifPresent(template -> {
				GanderLib.CHANNEL.send(PacketDistributor.PLAYER.with(() -> context.get().getSender()), new OpenGanderUiForStructureRequest(Component.literal(templateID.toString()), template));
			});
		});
		context.get().setPacketHandled(true);
	}
}
