package dev.compactmods.gander.network;

import dev.compactmods.gander.client.network.SceneDataClientHandler;
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
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record SceneDataResponse(Component sceneSource, StructureTemplate data) implements CustomPacketPayload
{
	public static final Type<SceneDataResponse> ID = new Type<>(new ResourceLocation("gander", "scene_data_response"));

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

	public static final StreamCodec<RegistryFriendlyByteBuf, SceneDataResponse> STREAM_CODEC = StreamCodec.composite(
			ComponentSerialization.STREAM_CODEC, SceneDataResponse::sceneSource,
			STRUCTURE_TEMPLATE_STREAM_CODEC, SceneDataResponse::data,
			SceneDataResponse::new
	);

	public static final IPayloadHandler<SceneDataResponse> HANDLER = (pkt, ctx) -> {
		ctx.enqueueWork(() -> {
			SceneDataClientHandler.loadScene(pkt.sceneSource, pkt.data);
		});
	};


	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
