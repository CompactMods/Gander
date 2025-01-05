package dev.compactmods.gander.ui.widget;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.ui.pipeline.BakedVirtualLevelScreenPipeline;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
import dev.compactmods.gander.ui.toolkit.GanderScreenRenderHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.culling.Frustum;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.camera.SceneCamera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpatialRenderer implements Renderable {
    private final Supplier<BakedVirtualLevelScreenPipeline> pipeline = Suppliers.memoize(BakedVirtualLevelScreenPipeline::new);
    private final BakedLevelScreenRenderingContext renderingContext;
    private final GanderScreenRenderHelper renderHelper;

	private final CompassOverlay compassOverlay;
	private boolean shouldRenderCompass;

	private final SceneCamera camera;

	private boolean isDisposed = false;

	public SpatialRenderer(BakedLevel bakedLevel) {
        this.compassOverlay = new CompassOverlay();
        this.shouldRenderCompass = false;
        this.camera = new SceneCamera();
        this.renderingContext = new BakedLevelScreenRenderingContext(bakedLevel);

        final var mc = Minecraft.getInstance();
        this.renderHelper = new GanderScreenRenderHelper(mc.getWindow().getWidth(), mc.getWindow().getHeight());
	}

	public void dispose() {
		if (isDisposed) return;
		this.isDisposed = true;
		this.renderingContext.dispose();
	}

	public SceneCamera camera() {
		return camera;
	}

    // FIXME
//	public void recalculateTranslucency() {
//        renderingContext.bakedLevel.resortTranslucency(camera.getPosition().toVector3f());
//    }

	public void shouldRenderCompass(boolean render) {
		this.shouldRenderCompass = render;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        final var pipe = pipeline.get();

        final var frustum = new Frustum(new Matrix4f().rotate(camera.rotation().conjugate(new Quaternionf())),
            renderHelper.projectionMatrix());

        final var manager = pipe.createLifecycleManager();

        manager.setup(renderingContext, graphics, camera);

        renderHelper.renderInScreenSpace(graphics, camera, (projMatrix, poseStack) -> {
            poseStack.translate(
                renderingContext.bakedLevel.blockBoundaries().getXSpan() / -2f,
                renderingContext.bakedLevel.blockBoundaries().getYSpan() / -2f,
                renderingContext.bakedLevel.blockBoundaries().getZSpan() / -2f);

            // RENDER SCENE

            // TODO: Map layers somewhere - renderingContext.translucencyChain.layers()
            pipe.staticGeometryPass(renderingContext, graphics, partialTicks, poseStack, camera, projMatrix);
            pipe.blockEntitiesPass(renderingContext, graphics, partialTicks, poseStack, camera, frustum, graphics.bufferSource());
            pipe.translucentGeometryPass(renderingContext, graphics, partialTicks, poseStack, camera, projMatrix);

            renderingContext.translucencyChain.process();
            // END RENDER SCENE
        });

        manager.teardown(renderingContext, graphics);
	}

	private void renderCompass(GuiGraphics graphics, float partialTicks, PoseStack poseStack) {
		poseStack.pushPose();
		{
			poseStack.translate(
					renderingContext.blockBoundaries.getXSpan() / -2f,
					renderingContext.blockBoundaries.getYSpan() / -2f,
					renderingContext.blockBoundaries.getZSpan() / -2f);

			var position = camera.getLookFrom();
			poseStack.translate(-position.x, -position.y, -position.z);
			poseStack.last().pose().negateY();
			poseStack.scale(1 / 16f, 1 / 16f, 1 / 16f);

			compassOverlay.render(graphics, partialTicks);
		}
		poseStack.popPose();
	}

	public void zoom(double factor) {
		camera.zoom((float) factor);
	}
}
