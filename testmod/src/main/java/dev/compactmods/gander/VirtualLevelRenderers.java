package dev.compactmods.gander;


import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.ScreenBlockRenderer;
import dev.compactmods.gander.render.baked.BakedLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.neoforge.client.ClientHooks;

// TODO: put this in rendering
public final class VirtualLevelRenderers
{
	private static final ConcurrentHashMap<BakedLevel, VirtualLevel> REGISTERED_LEVELS = new ConcurrentHashMap<>();

	private VirtualLevelRenderers() { }

	public static void clearAll() {
		REGISTERED_LEVELS.clear();
	}

	public static void registerLevelToRender(BakedLevel level, VirtualLevel originalLevel) {
		if (REGISTERED_LEVELS.putIfAbsent(level, originalLevel) != null) {
			throw new IllegalStateException("Level was already previously registered");
		}
	}

	public static void unregisterLevelToRender(BakedLevel level) {
		if (REGISTERED_LEVELS.remove(level) == null) {
			throw new IllegalStateException("Level was not registered for rendering");
		}
	}

	public static void renderLayer(RenderType renderType, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix) {
		REGISTERED_LEVELS.keys().asIterator().forEachRemaining(it -> {
			ScreenBlockRenderer.renderSectionLayer(
				it.blockRenderBuffers(),
				Function.identity(),
				renderType,
				poseStack,
				camera.getPosition().toVector3f(),
				projectionMatrix);
			ScreenBlockRenderer.renderSectionLayer(
				it.fluidRenderBuffers(),
				Function.identity(),
				renderType,
				poseStack,
				camera.getPosition().toVector3f(),
				projectionMatrix);
		});
	}

	public static void renderBlockEntities(BlockEntityRenderDispatcher dispatcher, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource) {
		final var camPos = camera.getPosition();
		REGISTERED_LEVELS.keys().asIterator().forEachRemaining(it -> {
			var level = REGISTERED_LEVELS.get(it);
			if (level == null) return;

			level.blockSystem().blockAndFluidStorage().streamBlockEntities()
				.forEach(pos -> {
					var blockEnt = level.getBlockEntity(pos);
					if (!ClientHooks.isBlockEntityRendererVisible(dispatcher, blockEnt, frustum)) return;

					poseStack.pushPose();
					poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
					dispatcher.render(blockEnt, partialTick, poseStack, bufferSource);
					poseStack.popPose();
				});
		});
	}
}
