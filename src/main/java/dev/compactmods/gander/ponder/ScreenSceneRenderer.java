package dev.compactmods.gander.ponder;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.client.render.ScreenBlockEntityRender;
import dev.compactmods.gander.client.render.ScreenBlockRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;

public class ScreenSceneRenderer {

	public static void renderScene(Scene scene, Camera camera, MultiBufferSource.BufferSource buffer, PoseStack pose, float partialTicks) {
		ScreenBlockEntityRender.render(scene.getLevel(), () -> scene.getLevel().getBlockEntities().iterator(), pose, buffer, partialTicks);
		ScreenBlockRenderer.render(scene.getLevel(), scene.getBounds(), buffer, pose, partialTicks);
		// TODO Fix Particles

		scene.getLevel().renderParticles(pose, buffer, camera, partialTicks);
	}

}
