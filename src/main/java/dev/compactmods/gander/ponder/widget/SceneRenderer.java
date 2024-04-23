package dev.compactmods.gander.ponder.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.client.gui.UIRenderHelper;
import dev.compactmods.gander.ponder.Scene;
import dev.compactmods.gander.ponder.ScreenSceneRenderer;
import dev.compactmods.gander.ponder.SceneCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

import net.minecraft.util.Mth;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class SceneRenderer implements Renderable {

	private @Nullable Scene scene;
	private final CompassOverlay compassOverlay;
	private final int width;
	private final int height;

	private final SceneCamera camera;
	private final Vector3f cameraTarget;

	private Vector3f lookFrom;
	private boolean shouldRenderCompass;
	private float scale;

	public SceneRenderer(int width, int height) {
		this.compassOverlay = new CompassOverlay();
		this.width = width;
		this.height = height;

		this.camera = new SceneCamera();
		this.cameraTarget = new Vector3f();
		this.lookFrom = new Vector3f();
		this.shouldRenderCompass = false;
		this.scale = 24f;
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

	public void setScene(@Nullable Scene scene) {
		this.scene = scene;
		if(scene != null) {
			this.shouldRenderCompass = true;
			this.compassOverlay.setBounds(scene.getBounds());
		} else {
			this.shouldRenderCompass = false;
			this.compassOverlay.setBounds(null);
		}
	}

	public void tick() {
		if(scene != null) {
			scene.tick();
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		if(this.scene == null)
			return;

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
			// Center (screen)
			ms.translate(width / 2f, height / 2f, 400);
			UIRenderHelper.flipForGuiRender(ms);

			ms.scale(scale, scale, scale);

			ms.pushPose();
			{
				camera.lookAt(this.lookFrom, cameraTarget, new Vector3f(0, 1, 0));

				ms.mulPose(camera.rotation());
				ms.translate(scene.getBounds().getXSpan() / -2f, -1f * (scene.getBounds().getYSpan() / 2f), scene.getBounds().getZSpan() / -2f);

				ScreenSceneRenderer.renderScene(scene, camera, buffer, ms, partialTicks);

				if(this.shouldRenderCompass)
					this.compassOverlay.render(graphics, mouseX, mouseY, partialTicks);
			}
			ms.popPose();
		}
		ms.popPose();

		RenderSystem.restoreProjectionMatrix();
	}

	public void scale(double scale) {
		this.scale += scale;
		this.scale = Mth.clamp(this.scale, 1, 50);
	}
}
