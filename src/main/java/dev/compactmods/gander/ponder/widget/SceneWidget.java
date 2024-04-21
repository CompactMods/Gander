package dev.compactmods.gander.ponder.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.gui.UIRenderHelper;
import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.PonderSceneRenderer;
import dev.compactmods.gander.ponder.SceneCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class SceneWidget implements Renderable {

	private final PonderScene scene;
	private final CompassOverlay compassOverlay;
	private final int width;
	private final int height;

	public final SceneCamera camera;
	public Vector3f cameraTarget;
	private Vector3f lookFrom;
	private boolean shouldRenderCompass;

	public SceneWidget(PonderScene scene, int width, int height) {
		this.scene = scene;
		this.compassOverlay = new CompassOverlay(scene);
		this.width = width;
		this.height = height;

		this.camera = new SceneCamera();
		this.cameraTarget = new Vector3f();
		this.lookFrom = new Vector3f();
		this.shouldRenderCompass = false;
	}

	public void prepareCamera(Vector2f rotation) {
		var look = new Vector3f(0, 0, 1);
		look.rotateX(rotation.x);
		look.rotateY(rotation.y);
		look.mul(10);

		this.lookFrom = look;
	}

	public void shouldRenderCompass(boolean render) {
		this.shouldRenderCompass = render;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		var buffer = Minecraft.getInstance().renderBuffers().bufferSource();

		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.backupProjectionMatrix();

		// has to be outside of MS transforms, important for vertex sorting
		Matrix4f matrix4f = new Matrix4f(RenderSystem.getProjectionMatrix());
		RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

		PoseStack ms = graphics.pose();
		ms.pushPose();
		{
			scene.getTransform().updateScreenParams(width, height, 0);

			// Center (screen)
			ms.translate(width / 2f, height / 2f, 400);
			UIRenderHelper.flipForGuiRender(ms);

			ms.scale(24, 24, 24);

			ms.pushPose();
			{
				camera.lookAt(this.lookFrom, cameraTarget, new Vector3f(0, 1, 0));

				ms.mulPose(camera.rotation());
				ms.translate(scene.getBounds().getXSpan() / -2f, -1f, scene.getBounds().getZSpan() / -2f);

				PonderSceneRenderer.renderScene(scene, camera, buffer, ms, partialTicks);

				if(this.shouldRenderCompass)
					this.compassOverlay.render(graphics, mouseX, mouseY, partialTicks);
			}
			ms.popPose();
		}
		ms.popPose();

		RenderSystem.restoreProjectionMatrix();
	}
}
