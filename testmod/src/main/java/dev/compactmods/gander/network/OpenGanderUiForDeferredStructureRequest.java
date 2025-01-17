package dev.compactmods.gander.network;

import dev.compactmods.gander.GanderTestMod;
import dev.compactmods.gander.client.gui.ScreenOpener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record OpenGanderUiForDeferredStructureRequest(ResourceLocation structureId) implements CustomPacketPayload {

	public static final Type<OpenGanderUiForDeferredStructureRequest> ID = new Type<>(GanderTestMod.asResource("open_scene"));

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenGanderUiForDeferredStructureRequest> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, OpenGanderUiForDeferredStructureRequest::structureId,
            OpenGanderUiForDeferredStructureRequest::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}

	public static final IPayloadHandler<OpenGanderUiForDeferredStructureRequest> HANDLER = (pkt, ctx) -> {
		if(FMLEnvironment.dist.isClient())
			ctx.enqueueWork(() -> ScreenOpener.forStructure(pkt.structureId));
	};
}
