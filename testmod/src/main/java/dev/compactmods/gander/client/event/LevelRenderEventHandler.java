package dev.compactmods.gander.client.event;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.VirtualLevelRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LevelRenderEventHandler {

	private static Map<RenderLevelStageEvent.Stage, RenderType> GEOMETRY_STAGES
		= RenderType.chunkBufferLayers()
			.stream()
			.collect(Collectors.toMap(RenderLevelStageEvent.Stage::fromRenderType, Function.identity()));

	public static void onRenderStage(final RenderLevelStageEvent evt) {
		var chunkRenderType = GEOMETRY_STAGES.get(evt.getStage());
		if (chunkRenderType != null) {
			var stack = new PoseStack();
			stack.mulPose(evt.getModelViewMatrix());
			VirtualLevelRenderers.renderLayer(
				chunkRenderType,
				stack,
				evt.getCamera(),
				evt.getProjectionMatrix());
		}
		else if (evt.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			VirtualLevelRenderers.renderBlockEntities(
				Minecraft.getInstance().getBlockEntityRenderDispatcher(),
				evt.getPartialTick(),
				evt.getPoseStack(),
				evt.getCamera(),
				evt.getFrustum(),
				Minecraft.getInstance().renderBuffers().bufferSource());
		}
	}
}
