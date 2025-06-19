package dev.compactmods.gander.render.screen;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.impl.BakedLevelScreenRenderPipeline;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import org.jetbrains.annotations.NotNull;

public class GanderPictureInPictureRenderer extends PictureInPictureRenderer<GanderPictureInPictureRenderState> {
    public GanderPictureInPictureRenderer(MultiBufferSource.BufferSource buffers) {
        super(buffers);
    }

    @Override
    public Class<GanderPictureInPictureRenderState> getRenderStateClass() {
        return GanderPictureInPictureRenderState.class;
    }

    @Override
    protected void renderToTexture(GanderPictureInPictureRenderState renderState, PoseStack pose) {
        final var pipelineState = renderState.stateSupplier().get();
        BakedLevelScreenRenderPipeline.INSTANCE.render(pipelineState);
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "Gander: PIP Renderer";
    }
}
