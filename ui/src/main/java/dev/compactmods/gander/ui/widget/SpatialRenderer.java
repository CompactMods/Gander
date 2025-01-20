package dev.compactmods.gander.ui.widget;

import dev.compactmods.gander.core.camera.SceneCamera;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.ui.pipeline.BakedLevelScreenRenderPipeline;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
import dev.compactmods.gander.ui.toolkit.GanderScreenRenderHelper;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.layouts.LayoutElement;

import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import net.minecraft.util.CommonColors;

import org.jetbrains.annotations.NotNull;

public class SpatialRenderer implements Renderable {
    private final BakedLevelScreenRenderingContext renderingContext;
    private final GanderScreenRenderHelper renderHelper;
    private final ScreenRectangle renderArea;
    private PipelineState state;

    private final CompassOverlay compassOverlay;
    private boolean shouldRenderCompass;

    private final SceneCamera camera;

    public SpatialRenderer(BakedLevel bakedLevel, int x, int y, int width, int height) {
        this.compassOverlay = new CompassOverlay();
        this.shouldRenderCompass = false;
        this.camera = new SceneCamera();
        this.renderArea = new ScreenRectangle(new ScreenPosition(x, y), width, height);
        this.renderingContext = BakedLevelScreenRenderingContext.forBakedLevel(bakedLevel);
        this.renderHelper = new GanderScreenRenderHelper(width, height);
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

        if (state == null) {
            this.state = BakedLevelScreenRenderPipeline.INSTANCE.setup();
            BakedLevelScreenRenderPipeline.INSTANCE.setupContext(this.state, renderingContext, camera);
        }

        graphics.enableScissor(renderArea.left(), renderArea.top(), renderArea.right(), renderArea.bottom());
        renderHelper.renderInScreenSpace(graphics, camera, (projMatrix, poseStack) -> {
            poseStack.translate(
                renderingContext.bakedLevel().blockBoundaries().getXSpan() / -2f,
                renderingContext.bakedLevel().blockBoundaries().getYSpan() / -2f,
                renderingContext.bakedLevel().blockBoundaries().getZSpan() / -2f);

            BakedLevelScreenRenderPipeline.INSTANCE.render(
                state, renderingContext, graphics, camera, poseStack, projMatrix
            );
        });
        graphics.disableScissor();
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

    public ScreenRectangle getRenderArea() {
        return renderArea;
    }
}
