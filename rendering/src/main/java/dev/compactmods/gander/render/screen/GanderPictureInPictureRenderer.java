package dev.compactmods.gander.render.screen;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

public class GanderPictureInPictureRenderer extends PictureInPictureRenderer<GanderPictureInPictureRenderState> {

    private static final float RENDER_SIZE = 8f;
    private static final ItemTransform DEFAULT_TRANSFORM = new ItemTransform(
        new Vector3f(30, 225, 0), new Vector3f(), new Vector3f(0.625F, 0.625F, 0.625F)
    );
    private static final Quaternionfc LIGHT_FIX_ROT = Axis.YP.rotationDegrees(285);
    private static final RandomSource RANDOM = RandomSource.create();

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
        final var blockRenderer = blocks.getModelRenderer();

        FeatureRenderDispatcher featureRenderer = mc.gameRenderer.getFeatureRenderDispatcher();
        SubmitNodeStorage nodeStorage = featureRenderer.getSubmitNodeStorage();

        mc.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);

        pose.pushPose();
        {
            pose.scale(RENDER_SIZE * renderState.scale(), -RENDER_SIZE * renderState.scale(), -RENDER_SIZE * renderState.scale());

//            graphics.renderItem(new ItemStack(Items.GOLD_INGOT), 0, 0);
            pose.pushPose();
            {
                DEFAULT_TRANSFORM.apply(false, pose.last());

                for (var pos : BlockPos.betweenClosed(bakedLevel.blockBoundaries())) {
//                    pose.translate(pos.getX() / RENDER_SIZE, pos.getY() / RENDER_SIZE, pos.getZ() / RENDER_SIZE);

                    boolean translate = RANDOM.nextBoolean();
                    pose.translate(0, 1 / 32f, 0);

                    final var state = bakedLevel.originalLevel().getBlockState(pos);
                    var model = blocks.getBlockModel(state);

                    // record BlockSubmit(PoseStack.Pose pose, BlockState state, int lightCoords, int overlayCoords, int outlineColor)
                    nodeStorage.submitBlock(pose, state, 15728880, OverlayTexture.NO_OVERLAY, 0);

//                    List<BlockModelPart> modelParts = model.collectParts(bakedLevel.originalLevel(), pos, state, RANDOM);
//                    RANDOM.setSeed(state.getSeed(BlockPos.ZERO));
//                    blockRenderer.tesselateBlock(
//                        bakedLevel.originalLevel(),
//                        modelParts,
//                        state,
//                        pos,
//                        pose,
//                        chunkLayer -> bufferSource.getBuffer(chunkLayer == ChunkSectionLayer.SOLID ? Sheets.solidBlockSheet() : RenderTypeHelper.getEntityRenderType(chunkLayer)),
//                        false,
//                        OverlayTexture.NO_OVERLAY
//                    );
                }
            }
            pose.popPose();
        }
        pose.popPose();

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
