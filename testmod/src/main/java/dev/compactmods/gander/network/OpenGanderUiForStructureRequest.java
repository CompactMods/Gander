package dev.compactmods.gander.network;

import java.util.function.Supplier;

import dev.compactmods.gander.client.ClientPacketHandler;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.network.NetworkEvent;

public record OpenGanderUiForStructureRequest(Component sceneSource, StructureTemplate data)
{
	public OpenGanderUiForStructureRequest(FriendlyByteBuf buffer) {
		this(buffer.readComponent(), Util.make(new StructureTemplate(), template -> {
			template.load(BuiltInRegistries.BLOCK.asLookup(), buffer.readAnySizeNbt());
		}));
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeComponent(sceneSource);
		buffer.writeNbt(data.save(new CompoundTag()));
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> ClientPacketHandler.structureDataReceived(sceneSource, data));
		context.get().setPacketHandled(true);
	}
}
