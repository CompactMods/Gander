package dev.compactmods.gander.render.event;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class GanderRenderLevelStageEvent extends RenderLevelStageEvent {
    public GanderRenderLevelStageEvent(Stage stage, LevelRenderer levelRenderer, @Nullable PoseStack poseStack, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, int renderTick, DeltaTracker partialTick, Camera camera, Frustum frustum) {
        super(stage, levelRenderer, poseStack, modelViewMatrix, projectionMatrix, renderTick, partialTick, camera, frustum);
    }

    public static GanderRenderLevelStageEvent make(RenderLevelStageEvent.Stage stage, PipelineState state) {
        final var levelRenderer = state.get(GanderRenderToolkit.LEVEL_RENDERER);
        final var poseStack = state.getOrDefault(GanderRenderToolkit.POSE_STACK, new PoseStack());
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var timer = state.get(GanderRenderToolkit.DELTA_TRACKER);
        final var projMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);
        final var viewMatrix = state.get(GanderRenderToolkit.MODEL_VIEW_MATRIX);
        final var frustum = state.get(GanderRenderToolkit.CULLING_FRUSTUM);

        return new GanderRenderLevelStageEvent(stage, levelRenderer, new PoseStack(), viewMatrix, projMatrix,
            levelRenderer.getTicks(), timer, camera, frustum);
    }
}
