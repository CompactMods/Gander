package dev.compactmods.gander.network;

import dev.compactmods.gander.GanderTestMod;
import dev.compactmods.gander.world.InWorldRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record RenderInWorldForDeferredStructureRequest(ResourceLocation structureId, Vector3f renderLocation) implements CustomPacketPayload
{
	public static final Type<RenderInWorldForDeferredStructureRequest> ID = new Type<>(GanderTestMod.asResource("in_world_open_scene"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RenderInWorldForDeferredStructureRequest> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, RenderInWorldForDeferredStructureRequest::structureId,
            ByteBufCodecs.VECTOR3F, RenderInWorldForDeferredStructureRequest::renderLocation,
            RenderInWorldForDeferredStructureRequest::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}

	public static final IPayloadHandler<RenderInWorldForDeferredStructureRequest> HANDLER = (pkt, ctx) -> {
		if(FMLEnvironment.dist.isClient())
			ctx.enqueueWork(() -> InWorldRenderer.forStructure(pkt.structureId, pkt.renderLocation));
	};
}
