package dev.compactmods.gander.render.toolkit;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.level.GanderLevelRenderer;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.phase.PipelineLifecyclePhase;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.GraphicsStatus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.function.Supplier;

public class GanderRenderToolkit {
    public static final PipelineState.Item<GraphicsStatus> PREVIOUS_GRAPHICS_MODE = new PipelineState.Item<>(GraphicsStatus.class);
    public static final PipelineState.Item<Matrix4f> ORIGINAL_MATRIX = new PipelineState.Item<>(Matrix4f.class);
    public static final PipelineState.Item<VertexSorting> ORIGINAL_VERTEX_SORTING = new PipelineState.Item<>(VertexSorting.class);
    public static final PipelineState.Item<TranslucencyChain> TRANSLUCENCY_CHAIN = new PipelineState.Item<>(TranslucencyChain.class);
    public static final PipelineState.Item<RenderTypeStore> RENDER_TYPE_STORE = new PipelineState.Item<>(RenderTypeStore.class);
    public static final PipelineState.Item<LevelRenderer> LEVEL_RENDERER = new PipelineState.Item<>(LevelRenderer.class);
    public static final PipelineState.Item<RenderTarget> RENDER_TARGET = new PipelineState.Item<>(RenderTarget.class);
    public static final PipelineState.Item<Vector3fc> RENDER_ORIGIN = new PipelineState.Item<>(Vector3fc.class);

    public static final PipelineState.Item<Matrix4f> PROJECTION_MATRIX = new PipelineState.Item<>(Matrix4f.class);
    public static final PipelineState.Item<Matrix4f> MODEL_VIEW_MATRIX = new PipelineState.Item<>(Matrix4f.class);

    public static final PipelineState.Item<BakedLevel> BAKED_LEVEL = new PipelineState.Item<>(BakedLevel.class);
    public static final PipelineState.Item<ScreenRectangle> RENDER_BOUNDS = new PipelineState.Item<>(ScreenRectangle.class);
    public static final PipelineState.Item<Camera> CAMERA = new PipelineState.Item<>(Camera.class);
    public static final PipelineState.Item<BlockPos[]> BLOCK_ENTITY_POSITIONS = new PipelineState.Item<>(BlockPos[].class);
    public static final PipelineState.Item<Frustum> CULLING_FRUSTUM = new PipelineState.Item<>(Frustum.class);
    public static final PipelineState.Item<DeltaTracker> DELTA_TRACKER = new PipelineState.Item<>(DeltaTracker.class);
    public static final PipelineState.Item<PoseStack> POSE_STACK = new PipelineState.Item<>(PoseStack.class);


    public static boolean makeDeltaTracker(PipelineState state) {
        final var tracker = new DeltaTracker.Timer(20, 0, FloatUnaryOperator.identity());
        state.set(DELTA_TRACKER, tracker);
        return true;
    }
}
