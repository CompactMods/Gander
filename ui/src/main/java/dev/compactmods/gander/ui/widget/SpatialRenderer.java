package dev.compactmods.gander.ui.widget;

import dev.compactmods.gander.core.camera.SceneCamera;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import dev.compactmods.gander.render.pipeline.impl.BakedLevelScreenRenderPipeline;
import dev.compactmods.gander.render.screen.GanderScreenRenderHelper;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class SpatialRenderer implements Renderable {
    private final GanderScreenRenderHelper renderHelper;
    private final ScreenRectangle renderArea;
    private final BakedLevel bakedLevel;
    private PipelineState state;

    private final CompassOverlay compassOverlay;
    private boolean shouldRenderCompass;

    private final SceneCamera camera;

    public SpatialRenderer(BakedLevel bakedLevel, int x, int y, int width, int height) {
        this.bakedLevel = bakedLevel;
        this.compassOverlay = new CompassOverlay();
        this.shouldRenderCompass = false;
        this.camera = new SceneCamera();
        this.renderArea = new ScreenRectangle(new ScreenPosition(x, y), width, height);
        this.renderHelper = new GanderScreenRenderHelper(width, height);
    }

    public SceneCamera camera() {
        return camera;
    }

    public void recalculateTranslucency() {
        // FIXME - Black Screen Issue
        //  renderingContext.recalculateTranslucency(camera);
    }

    public void shouldRenderCompass(boolean render) {
        this.shouldRenderCompass = render;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

        if (state == null) {
            this.state = BakedLevelScreenRenderPipeline.INSTANCE.setup(this::setupInitialState);
        }

        renderHelper.renderInScreenSpace(graphics, camera, (projMatrix) -> {
            final var poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(
                bakedLevel.blockBoundaries().getXSpan() / -2f,
                bakedLevel.blockBoundaries().getYSpan() / -2f,
                bakedLevel.blockBoundaries().getZSpan() / -2f);

            state.set(GanderRenderToolkit.PROJECTION_MATRIX, projMatrix);

            BakedLevelScreenRenderPipeline.INSTANCE.render(state, graphics, partialTicks);

            poseStack.popPose();
        });
    }

    private void setupInitialState(PipelineState state) {
        final var blockEntityPositions = BlockPos.betweenClosedStream(bakedLevel.blockBoundaries())
            .filter(p -> bakedLevel.originalLevel().getBlockState(p).hasBlockEntity())
            .map(BlockPos::immutable)
            .toArray(BlockPos[]::new);

        state.set(GanderRenderToolkit.BLOCK_ENTITY_POSITIONS, blockEntityPositions);
        state.set(GanderRenderToolkit.BAKED_LEVEL, bakedLevel);
        state.set(GanderRenderToolkit.RENDER_BOUNDS, renderArea);
        state.set(GanderRenderToolkit.CAMERA, this.camera);
    }

    private void renderCompass(GuiGraphics graphics, float partialTicks, PoseStack poseStack) {
        poseStack.pushPose();
        {
            poseStack.translate(
                bakedLevel.blockBoundaries().getXSpan() / -2f,
                bakedLevel.blockBoundaries().getYSpan() / -2f,
                bakedLevel.blockBoundaries().getZSpan() / -2f);

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
