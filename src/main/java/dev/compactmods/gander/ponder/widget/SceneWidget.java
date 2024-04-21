package dev.compactmods.gander.ponder.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.render.SuperRenderTypeBuffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

import org.joml.Matrix4f;

public class SceneWidget implements Renderable {

	private final PonderScene scene;
	private final CompassOverlay compassOverlay;
	private final int width;
	private final int height;

	public SceneWidget(PonderScene scene, int width, int height) {
		this.scene = scene;
		this.compassOverlay = new CompassOverlay(scene);
		this.width = width;
		this.height = height;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();

		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.backupProjectionMatrix();

		// has to be outside of MS transforms, important for vertex sorting
		Matrix4f matrix4f = new Matrix4f(RenderSystem.getProjectionMatrix());
		matrix4f.translate(0, 0, 800);
		RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);

		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(0, 0, -800);

		scene.getTransform().updateScreenParams(width, height, 0);
		scene.getTransform().apply(ms, partialTicks);
		scene.getTransform().updateSceneRVE(partialTicks);

		// RenderSystem.runAsFancy(() -> {
		scene.renderScene(buffer, ms, partialTicks);
		buffer.draw();
		// });

		this.compassOverlay.render(graphics, mouseX, mouseY, partialTicks);

		ms.popPose();
		RenderSystem.restoreProjectionMatrix();
	}
}
