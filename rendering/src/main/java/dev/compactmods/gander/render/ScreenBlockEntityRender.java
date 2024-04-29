package dev.compactmods.gander.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.fml.loading.FMLEnvironment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ScreenBlockEntityRender {
	private static final Logger LOGS = LogManager.getLogger();

	public static void render(BlockAndTintGetter world, @NotNull BlockEntity blockEntity, PoseStack ms, Vector3f cameraPosition,
							  PostChain translucencyChain, MultiBufferSource.BufferSource buffer, float pt) {
		render(world, Stream.of(blockEntity), ms, cameraPosition, null, translucencyChain, buffer, pt);
	}

	public static void render(BlockAndTintGetter world, Stream<BlockEntity> resolver, PoseStack ms, Vector3f cameraPosition,
							  PostChain translucencyChain, MultiBufferSource.BufferSource buffer, float pt) {
		render(world, resolver, ms, cameraPosition, null, translucencyChain, buffer, pt);
	}

	public static void render(BlockAndTintGetter world, Stream<BlockEntity> resolver, PoseStack ms, Vector3f cameraPosition,
							  @Nullable Matrix4f lightTransform, PostChain translucencyChain, MultiBufferSource.BufferSource buffer, float pt) {

		final var wrappedBuffer = WrappedBufferSource.from(translucencyChain, buffer);

		resolver.filter(Objects::nonNull).forEach(ent -> render(world, ent, ms, cameraPosition, lightTransform, wrappedBuffer, pt));
		wrappedBuffer.endBatch(RenderType.solid());
		wrappedBuffer.endBatch(RenderType.endPortal());
		wrappedBuffer.endBatch(RenderType.endGateway());
		wrappedBuffer.endBatch(Sheets.solidBlockSheet());
		wrappedBuffer.endBatch(Sheets.cutoutBlockSheet());
		wrappedBuffer.endBatch(Sheets.bedSheet());
		wrappedBuffer.endBatch(Sheets.shulkerBoxSheet());
		wrappedBuffer.endBatch(Sheets.signSheet());
		wrappedBuffer.endBatch(Sheets.hangingSignSheet());
		wrappedBuffer.endBatch(Sheets.chestSheet());
		wrappedBuffer.endBatch();
	}

	public static void render(BlockAndTintGetter world, @NotNull BlockEntity blockEntity, PoseStack ms, Vector3f cameraPosition,
							  @Nullable Matrix4f lightTransform, MultiBufferSource.BufferSource buffer, float pt) {

		BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
		if (renderer == null)
			return;

		BlockPos pos = blockEntity.getBlockPos();
		ms.pushPose();
		ms.translate(pos.getX() - cameraPosition.x, pos.getY() - cameraPosition.y, pos.getZ() - cameraPosition.z);

		try {
			BlockPos worldPos = getLightPos(lightTransform, pos);
			int worldLight = LevelRenderer.getLightColor(world, worldPos);

			renderer.render(blockEntity, pt, ms, buffer, worldLight, OverlayTexture.NO_OVERLAY);

		} catch (Exception e) {
			String message = "BlockEntity " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()) + " could not be rendered virtually.";
			if (FMLEnvironment.production)
				LOGS.error(message, e);
			else
				LOGS.error(message);
		}

		ms.popPose();
	}

	private static BlockPos getLightPos(@Nullable Matrix4f lightTransform, BlockPos pos) {
		if (lightTransform != null) {
			Vector4f lightVec = new Vector4f(pos.getX() + .5f, pos.getY() + .5f, pos.getZ() + .5f, 1);
			lightVec.mul(lightTransform);
			return BlockPos.containing(lightVec.x(), lightVec.y(), lightVec.z());
		} else {
			return pos;
		}
	}
}
