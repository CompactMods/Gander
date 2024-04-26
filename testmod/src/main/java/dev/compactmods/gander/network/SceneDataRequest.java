package dev.compactmods.gander.network;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.client.network.SceneDataClientHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public record SceneDataRequest(ResourceLocation templateID) implements CustomPacketPayload {

	public static final Type<SceneDataRequest> ID = new Type<>(GanderLib.asResource("scene_data"));
	public static final StreamCodec<? super ByteBuf, SceneDataRequest> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(SceneDataRequest::new, SceneDataRequest::templateID);

	public void handle(IPayloadContext ctx) {
		final var templateManager = ServerLifecycleHooks.getCurrentServer().getStructureManager();
		templateManager.get(templateID).ifPresent(t -> ctx.reply(new SceneData(t)));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public record SceneData(StructureTemplate data) implements CustomPacketPayload {

		public static final Type<SceneData> ID = new Type<>(GanderLib.asResource("scene_data_response"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SceneData> STREAM_CODEC = StreamCodec.of(
				(buf, val) -> {
					buf.writeNbt(val.data.save(new CompoundTag()));
				},
				buf -> {
					var templateData = new StructureTemplate();
					templateData.load(buf.registryAccess().lookupOrThrow(Registries.BLOCK), buf.readNbt());
					return new SceneData(templateData);
				}
		);

		public void handle(IPayloadContext context) {
			SceneDataClientHandler.loadScene(data);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}
}
