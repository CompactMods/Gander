package dev.compactmods.gander.render.screen;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.common.util.TransformationHelper;

public class GanderPictureInPictureRenderer extends PictureInPictureRenderer<GanderPictureInPictureRenderState> {

    private static final float RENDER_SIZE = 16f;
    private static final ItemTransform DEFAULT_TRANSFORM = new ItemTransform(
        new Vector3f(30, 225, 0), new Vector3f(), new Vector3f(0.625F, 0.625F, 0.625F)
    );

    public GanderPictureInPictureRenderer(MultiBufferSource.BufferSource buffers) {
        super(buffers);
    }

    @Override
    public @NotNull Class<GanderPictureInPictureRenderState> getRenderStateClass() {
        return GanderPictureInPictureRenderState.class;
    }

    @Override
    protected void renderToTexture(GanderPictureInPictureRenderState renderState, PoseStack pose) {
        final var pipelineState = renderState.stateSupplier().get();
        final var graphics = renderState.graphics();

        final var bakedLevel = pipelineState.get(GanderRenderToolkit.BAKED_LEVEL);
        final var mc = Minecraft.getInstance();
        final var blocks = mc.getBlockRenderer();
        final var camera = pipelineState.get(GanderRenderToolkit.CAMERA);
        final var blockRenderer = blocks.getModelRenderer();
        final var blockEntities = mc.getBlockEntityRenderDispatcher();

        FeatureRenderDispatcher featureRenderer = mc.gameRenderer.getFeatureRenderDispatcher();
        SubmitNodeStorage nodeStorage = featureRenderer.getSubmitNodeStorage();

        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        final var center = bakedLevel.blockBoundaries().getCenter();

        pose.mulPose(TransformationHelper.quatFromXYZ(new Vector3f(30, 45, 180), true));

        final var camState = new CameraRenderState();

        for (var pos : BlockPos.betweenClosed(bakedLevel.blockBoundaries())) {
            pose.pushPose();
            pose.translate(center.reverse());
            pose.translate(pos.getX(), pos.getY(), pos.getZ());

            // record BlockSubmit(PoseStack.Pose pose, BlockState state, int lightCoords, int overlayCoords, int outlineColor)
            final var state = bakedLevel.originalLevel().getBlockState(pos);
//            if(state.isAir())
//                continue;

            final var shape = state.getRenderShape();

            // TODO: Some entities (like dragon heads) are both model and get rendered via BER submission
            // This causes duplicate geometry
            if(shape == RenderShape.MODEL)
                nodeStorage.submitBlock(pose, state, 15728880, OverlayTexture.NO_OVERLAY, 0);

            if(state.hasBlockEntity()) {
                var ent = bakedLevel.originalLevel().getBlockEntity(pos);
                if(ent != null) {
                    var renderer = blockEntities.getRenderer(ent);
                    if (renderer != null) {
                        var renderEntState = renderer.createRenderState();
                        renderer.extractRenderState(ent, renderEntState, 0, camera.getPosition(), null);
                        renderer.submit(renderEntState, pose, nodeStorage, camState);
                    }
                }
            }
            pose.popPose();
        }

        featureRenderer.renderAllFeatures();

    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2F;
    }

    @Override
    protected @NotNull String getTextureLabel() {
        return "Gander: PIP Renderer";
    }

    private BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> buffers, SectionBufferBuilderPack builderPack, ChunkSectionLayer layer) {
        BufferBuilder bufferbuilder = buffers.get(layer);
        if (bufferbuilder == null) {
            ByteBufferBuilder bytebufferbuilder = builderPack.buffer(layer);
            bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            buffers.put(layer, bufferbuilder);
        }

        return bufferbuilder;
    }
}
