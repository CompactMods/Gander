package dev.compactmods.gander.render.toolkit;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import net.minecraft.client.GraphicsStatus;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

public class GanderRenderToolkit {
    public static final PipelineState.Item<GraphicsStatus> PREVIOUS_GRAPHICS_MODE = new PipelineState.Item<>(GraphicsStatus.class);
    public static final PipelineState.Item<Matrix4f> ORIGINAL_MATRIX = new PipelineState.Item<>(Matrix4f.class);
    public static final PipelineState.Item<VertexSorting> ORIGINAL_VERTEX_SORTING = new PipelineState.Item<>(VertexSorting.class);
    public static final PipelineState.Item<TranslucencyChain> TRANSLUCENCY_CHAIN = new PipelineState.Item<>(TranslucencyChain.class);
    public static final PipelineState.Item<RenderTypeStore> RENDER_TYPE_STORE = new PipelineState.Item<>(RenderTypeStore.class);
    public static final PipelineState.Item<RenderTarget> RENDER_TARGET = new PipelineState.Item<>(RenderTarget.class);
    public static final PipelineState.Item<Vector3fc> RENDER_ORIGIN = new PipelineState.Item<>(Vector3fc.class);
}
