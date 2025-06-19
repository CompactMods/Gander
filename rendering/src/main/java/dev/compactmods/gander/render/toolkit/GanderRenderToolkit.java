package dev.compactmods.gander.render.toolkit;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;

import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

public class GanderRenderToolkit {
    public static final PipelineState.Item<GraphicsStatus> PREVIOUS_GRAPHICS_MODE = new PipelineState.Item<>(GraphicsStatus.class);
    public static final PipelineState.Item<GpuBufferSlice> ORIGINAL_CPU_BUFFER_SLICE = new PipelineState.Item<>(GpuBufferSlice.class);
    public static final PipelineState.Item<ProjectionType> ORIGINAL_PROJECTION_TYPE = new PipelineState.Item<>(ProjectionType.class);
    public static final PipelineState.Item<RenderTypeStore> RENDER_TYPE_STORE = new PipelineState.Item<>(RenderTypeStore.class);
    public static final PipelineState.Item<RenderTarget> RENDER_TARGET = new PipelineState.Item<>(RenderTarget.class);
    public static final PipelineState.Item<Vector3fc> RENDER_ORIGIN = new PipelineState.Item<>(Vector3fc.class);

    public static final PipelineState.Item<Matrix4f> PROJECTION_MATRIX = new PipelineState.Item<>(Matrix4f.class);
    public static final PipelineState.Item<Matrix4f> MODEL_VIEW_MATRIX = new PipelineState.Item<>(Matrix4f.class);

    public static final PipelineState.Item<BakedLevel> BAKED_LEVEL = new PipelineState.Item<>(BakedLevel.class);
    public static final PipelineState.Item<ScreenRectangle> RENDER_BOUNDS = new PipelineState.Item<>(ScreenRectangle.class);
    public static final PipelineState.Item<PictureInPictureRenderState> PIP_RENDER_STATE = new PipelineState.Item<>(PictureInPictureRenderState.class);
    public static final PipelineState.Item<Camera> CAMERA = new PipelineState.Item<>(Camera.class);
    public static final PipelineState.Item<BlockPos[]> BLOCK_ENTITY_POSITIONS = new PipelineState.Item<>(BlockPos[].class);
}
