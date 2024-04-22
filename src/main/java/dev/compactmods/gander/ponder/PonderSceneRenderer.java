package dev.compactmods.gander.ponder;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.client.render.ScreenLevelRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class PonderSceneRenderer {

	public static void renderScene(Scene scene, Camera camera, MultiBufferSource.BufferSource buffer, PoseStack pose, float pt) {
		var renderer = new ScreenLevelRenderer(scene.getLevel(), scene.getBounds());
		renderer.render(camera, buffer, pose, pt);
	}

}
