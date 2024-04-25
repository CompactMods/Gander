package dev.compactmods.gander.network;

import dev.compactmods.gander.client.network.SceneDataClientHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public record SceneDataRequest(ResourceLocation templateID) implements CustomPacketPayload {

	public SceneDataRequest(FriendlyByteBuf buffer) {
		this(buffer.readResourceLocation());
	}

	public static final ResourceLocation ID = new ResourceLocation("gander", "scene_data");

	public static final IPlayPayloadHandler<SceneDataRequest> HANDLER = (req, ctx) -> {
		ctx.workHandler().submitAsync(() -> {
			final var templateManager = ServerLifecycleHooks.getCurrentServer().getStructureManager();
			templateManager.get(req.templateID).ifPresent(t -> {
				ctx.replyHandler().send(new SceneData(t));
			});
		});
	};

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(templateID);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public record SceneData(StructureTemplate data) implements CustomPacketPayload {

		public static SceneData fromBuffer(FriendlyByteBuf buffer) {
			final var templateData = new StructureTemplate();
			templateData.load(BuiltInRegistries.BLOCK.asLookup(), buffer.readNbt());
			return new SceneData(templateData);
		}

		public static final ResourceLocation ID = new ResourceLocation("gander", "scene_data_response");

		public static final IPlayPayloadHandler<SceneData> HANDLER = (pkt, ctx) -> {
			ctx.workHandler().submitAsync(() -> {
				SceneDataClientHandler.loadScene(pkt.data);
			});
		};

		@Override
		public void write(FriendlyByteBuf buf) {
			final var tag = data.save(new CompoundTag());
			buf.writeNbt(tag);
		}

		@Override
		public ResourceLocation id() {
			return ID;
		}
	}
}
