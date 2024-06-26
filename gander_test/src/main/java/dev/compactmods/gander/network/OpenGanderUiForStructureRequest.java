package dev.compactmods.gander.network;

import dev.compactmods.gander.client.gui.GanderUI;
import dev.compactmods.gander.client.gui.ScreenOpener;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record OpenGanderUiForStructureRequest(Component sceneSource, StructureTemplate data) implements CustomPacketPayload
{
	public static final Type<OpenGanderUiForStructureRequest> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("gander", "scene_data_response"));

	private static final StreamCodec<RegistryFriendlyByteBuf, StructureTemplate> STRUCTURE_TEMPLATE_STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buf, StructureTemplate val) -> {
				buf.writeNbt(val.save(new CompoundTag()));
			},
			(RegistryFriendlyByteBuf buf) -> {
				var templateData = new StructureTemplate();
				templateData.load(buf.registryAccess().lookupOrThrow(Registries.BLOCK), (CompoundTag)buf.readNbt(NbtAccounter.unlimitedHeap()));
				return templateData;
			}
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenGanderUiForStructureRequest> STREAM_CODEC = StreamCodec.composite(
			ComponentSerialization.STREAM_CODEC, OpenGanderUiForStructureRequest::sceneSource,
			STRUCTURE_TEMPLATE_STREAM_CODEC, OpenGanderUiForStructureRequest::data,
			OpenGanderUiForStructureRequest::new
	);

	public static final IPayloadHandler<OpenGanderUiForStructureRequest> HANDLER = (pkt, ctx) -> {
		if(FMLEnvironment.dist.isClient())
			ctx.enqueueWork(() -> ScreenOpener.forStructureData(pkt.sceneSource, pkt.data));
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
