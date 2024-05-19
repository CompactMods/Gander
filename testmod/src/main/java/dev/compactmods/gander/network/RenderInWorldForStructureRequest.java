package dev.compactmods.gander.network;

import dev.compactmods.gander.client.gui.ScreenOpener;
import dev.compactmods.gander.world.InWorldRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import org.joml.Vector3f;

public record RenderInWorldForStructureRequest(Component sceneSource, StructureTemplate data, Vector3f renderLocation)
		implements CustomPacketPayload
{
	public static final Type<RenderInWorldForStructureRequest> ID = new Type<>(new ResourceLocation("gander", "in_world_scene_data_response"));

	private static final StreamCodec<RegistryFriendlyByteBuf, StructureTemplate> STRUCTURE_TEMPLATE_STREAM_CODEC = StreamCodec.of(
			(RegistryFriendlyByteBuf buf, StructureTemplate val) -> {
				buf.writeNbt(val.save(new CompoundTag()));
			},
			(RegistryFriendlyByteBuf buf) -> {
				var templateData = new StructureTemplate();
				templateData.load(buf.registryAccess().lookupOrThrow(Registries.BLOCK), (CompoundTag)buf.readNbt(
						NbtAccounter.unlimitedHeap()));
				return templateData;
			}
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, RenderInWorldForStructureRequest> STREAM_CODEC = StreamCodec.composite(
			ComponentSerialization.STREAM_CODEC, RenderInWorldForStructureRequest::sceneSource,
			STRUCTURE_TEMPLATE_STREAM_CODEC, RenderInWorldForStructureRequest::data,
            ByteBufCodecs.VECTOR3F, RenderInWorldForStructureRequest::renderLocation,
			RenderInWorldForStructureRequest::new
	);

	public static final IPayloadHandler<RenderInWorldForStructureRequest> HANDLER = (pkt, ctx) -> {
		if(FMLEnvironment.dist.isClient())
			ctx.enqueueWork(() -> InWorldRenderer.forStructureData(pkt.sceneSource, pkt.data, pkt.renderLocation));
	};

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
