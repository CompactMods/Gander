package dev.compactmods.gander.ponder;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.ponder.element.PonderSceneElement;
import dev.compactmods.gander.render.DiffuseLightCalculator;
import dev.compactmods.gander.render.ForcedDiffuseState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

public class PonderSceneRenderer {
	public static void renderScene(PonderScene ponderScene, Camera camera, MultiBufferSource.BufferSource buffer, PoseStack ms, float pt) {
		ForcedDiffuseState.pushCalculator(DiffuseLightCalculator.DEFAULT);
		ms.pushPose();

		Minecraft mc = Minecraft.getInstance();

		final var level = ponderScene.getWorld();

		ponderScene.forEach(PonderSceneElement.class, e -> e.renderFirst(level, buffer, ms, pt));

		for (RenderType type : RenderType.chunkBufferLayers()) {
			var transTarget = mc.levelRenderer.getTranslucentTarget();
			boolean isTranslucent = type == RenderType.translucent();
			var chain = Minecraft.getInstance().levelRenderer.transparencyChain;

			if (isTranslucent) {
				if (transTarget != null)
					transTarget.clear(Minecraft.ON_OSX);

				if (chain != null) {
					transTarget.copyDepthFrom(mc.getMainRenderTarget());
				}
			}

			ponderScene.forEach(PonderSceneElement.class, e -> e.renderLayer(level, buffer, type, ms, pt));

			if (isTranslucent && chain != null)
				chain.process(pt);
		}

		ponderScene.forEach(PonderSceneElement.class, e -> e.renderLast(level, buffer, ms, pt));
		// ponderScene.camera.set(ponderScene.transform.xRotation.getValue(pt) + 90, ponderScene.transform.yRotation.getValue(pt) + 180);
		level.renderEntities(ms, buffer, camera, pt);
		level.renderParticles(ms, buffer, camera, pt);
		ponderScene.outliner.renderOutlines(ms, buffer, Vec3.ZERO, pt);

		ms.popPose();
		ForcedDiffuseState.popCalculator();
	}
}
