package dev.compactmods.gander.ui.widget;

import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.ui.pipeline.BakedLevelScreenRenderPipeline;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
import dev.compactmods.gander.ui.toolkit.GanderScreenRenderHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.camera.SceneCamera;
import net.minecraft.client.gui.GuiGraphics;

import org.jetbrains.annotations.NotNull;

public class SpatialRenderer implements Renderable {
    private final BakedLevelScreenRenderingContext renderingContext;
    private final GanderScreenRenderHelper renderHelper;
    private PipelineState state;

	private final CompassOverlay compassOverlay;
	private boolean shouldRenderCompass;

	private final SceneCamera camera;

	public SpatialRenderer(BakedLevel bakedLevel) {
        this.compassOverlay = new CompassOverlay();
        this.shouldRenderCompass = false;
        this.camera = new SceneCamera();
        this.renderingContext = BakedLevelScreenRenderingContext.forBakedLevel(bakedLevel);

        final var mc = Minecraft.getInstance();
        this.renderHelper = new GanderScreenRenderHelper(mc.getWindow().getWidth(), mc.getWindow().getHeight());
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
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

        if(state == null)
            this.state = BakedLevelScreenRenderPipeline.INSTANCE.setup(renderingContext, camera);

        renderHelper.renderInScreenSpace(graphics, camera, (projMatrix, poseStack) -> {
            poseStack.translate(
                renderingContext.bakedLevel().blockBoundaries().getXSpan() / -2f,
                renderingContext.bakedLevel().blockBoundaries().getYSpan() / -2f,
                renderingContext.bakedLevel().blockBoundaries().getZSpan() / -2f);

            BakedLevelScreenRenderPipeline.INSTANCE.render(
                state, renderingContext, graphics, camera, poseStack, projMatrix
            );
        });
	}

	private void renderCompass(GuiGraphics graphics, float partialTicks, PoseStack poseStack) {
		poseStack.pushPose();
		{
			poseStack.translate(
					renderingContext.blockBoundaries().getXSpan() / -2f,
					renderingContext.blockBoundaries().getYSpan() / -2f,
					renderingContext.blockBoundaries().getZSpan() / -2f);

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
