package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.core.math.WorldMath;
import dev.compactmods.gander.render.vertex.FluidVertexConsumer;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import net.minecraft.world.phys.AABB;

import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LevelBakery {

    public static BakedLevel bakeVertices(Level level, AABB blockBoundaries, Vector3f cameraPosition) {

        final var allSections = WorldMath.sectionPositions(level, blockBoundaries)
            .collect(Collectors.toSet());

        Minecraft mc = Minecraft.getInstance();

        if (!GanderGeometryHelper.initialized())
            GanderGeometryHelper.setup();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        Map<SectionPos, BakedLevelSection> bakedSections = new Reference2ObjectArrayMap<>();
        try {
            final var sorting = VertexSorting.byDistance(cameraPosition.x, cameraPosition.y, cameraPosition.z);

            RenderRegionCache regionCache = new RenderRegionCache();
            for (var sectionPos : allSections) {
                final AABB sectionAABB = WorldMath.sectionAABB(sectionPos);

                final var renderChunk = regionCache.createRegion(level, sectionPos);
                if (renderChunk == null) {
                    // Empty section - see createRegion
                    continue;
                }

                final SectionBufferBuilderPack bufferPack = new SectionBufferBuilderPack();
                final var compileResults = GanderGeometryHelper.SECTION_COMPILER
                    .compile(sectionPos, renderChunk, sorting, bufferPack);

                final var bakedSection = new BakedLevelSection(bufferPack,
                    compileResults.renderedLayers,
                    sectionAABB);

                bakedSections.put(sectionPos, bakedSection);

//
//                final SectionBufferBuilderPack fluidPack = new SectionBufferBuilderPack();
//
//                final Map<RenderType, BufferBuilder> blockBufferBuilders = new HashMap<>();
//                final Map<RenderType, BufferBuilder> fluidBufferBuilders = new HashMap<>();
//
//                PoseStack pose = new PoseStack();
//                RandomSource random = RandomSource.createNewThreadLocalInstance();
//                BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
//
////                ModelBlockRenderer.enableCaching();
//                BlockPos.betweenClosedStream(blockBoundaries).forEach(pos -> {
//                    createBlockGeometry(level, pos, pose, buffers, dispatcher, random, bufferPack, fluidBufferBuilders);
//                });
//
//
//                final var blockVertices = buildSortedGeometryBuffers(bufferPack, sorting, blockBufferBuilders);
//                final var fluidVertices = buildSortedGeometryBuffers(fluidPack, sorting, fluidBufferBuilders);
//
//                final var bakedSection = new BakedLevelSection(bufferPack, fluidPack,
//                    blockVertices.buffers(), fluidVertices.buffers(),
//                    blockVertices.meshData(), fluidVertices.meshData(),
//                    chunkArea);
//
//                bakedSections.put(sectionPos, bakedSection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new BakedLevel(level, blockBoundaries, bakedSections);
    }

    private static void createBlockGeometry(Level level, BlockPos pos, PoseStack pose, MultiBufferSource.BufferSource buffers, BlockRenderDispatcher dispatcher,
                                            RandomSource random, SectionBufferBuilderPack blockPack, Map<RenderType, BufferBuilder> fluidBufferBuilders) {

        BlockState state = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);

        pose.pushPose();
        pose.translate(pos.getX(), pos.getY(), pos.getZ());

        if (state.getRenderShape() == RenderShape.MODEL) {
            BlockStateModel model = dispatcher.getBlockModel(state);

            long seed = state.getSeed(pos);
            random.setSeed(seed);

            // renderModel(PoseStack.Pose p_111068_, MultiBufferSource bufferSource, BlockStateModel p_405848_,
            // float red, float green, float blue, int light, int overlay, BlockAndTintGetter level, BlockPos pos, BlockState state)

            int light = Math.max(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));

            try {
                for (BlockModelPart blockmodelpart : model.collectParts(level, pos, state, RandomSource.create(42L))) {
                    final var renderType = blockmodelpart.getRenderType(state);
                    if (renderType == null)
                        continue;

//                    final var entRenderType = net.neoforged.neoforge.client.RenderTypeHelper.getEntityRenderType(renderType);
//                    if(entRenderType == null)
//                        continue;

                    VertexConsumer vertices = buffers.getBuffer(renderType);

                    for (Direction direction : Direction.values()) {
                        renderQuadList(pose.last(), vertices, 1, 1, 1, blockmodelpart.getQuads(direction), light, OverlayTexture.NO_OVERLAY);
                    }

                    renderQuadList(pose.last(), vertices, 1, 1, 1, blockmodelpart.getQuads(null), light, OverlayTexture.NO_OVERLAY);
                }
            } catch (Exception e) {

            }

            // 21.1 Code
//            model.getRenderTypes(state, random, modelData).forEach(type -> {
//                var vertexBuilder = blockBufferBuilders.computeIfAbsent(type, t -> {
//                    var typedVC = blockPack.buffer(t);
//                    return new BufferBuilder(typedVC, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
//                });
//
//                renderer.tesselateBlock(level, model, state, pos, pose, vertexBuilder, true, random, seed, OverlayTexture.NO_OVERLAY, finalModelData, type);
//            });
        }

        if (!fluidState.isEmpty()) {
            final var fluidRenderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
            var vertexBuilder = fluidBufferBuilders.computeIfAbsent(fluidRenderType, t -> {
                var typedFluidVC = blockPack.buffer(t);
                return new BufferBuilder(typedFluidVC, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            });

            dispatcher.getLiquidBlockRenderer().tesselate(level, pos, new FluidVertexConsumer(vertexBuilder, pose, pos), state, fluidState);
        }

        pose.popPose();
//        ModelBlockRenderer.clearCache();

//        TODO: Make Result Record so this can be immutable
    }

    private static void renderQuadList(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float red,
        float green,
        float blue,
        List<BakedQuad> quads,
        int packedLight,
        int packedOverlay
    ) {
        for (BakedQuad bakedquad : quads) {
            float f;
            float f1;
            float f2;
            if (bakedquad.isTinted()) {
                f = Mth.clamp(red, 0.0F, 1.0F);
                f1 = Mth.clamp(green, 0.0F, 1.0F);
                f2 = Mth.clamp(blue, 0.0F, 1.0F);
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
            }

            consumer.putBulkData(pose, bakedquad, f, f1, f2, 1.0F, packedLight, packedOverlay);
        }
    }

//    private static SortedGeometryBufferResult buildSortedGeometryBuffers(SectionBufferBuilderPack blockPack,
//                                                                         VertexSorting sorting,
//                                                                         Map<RenderType, BufferBuilder> bufferBuilders
//    ) {
//        final var renderSortStates = new Reference2ObjectArrayMap<RenderType, MeshData.SortState>();
//        final var vertexBuffers = new Reference2ObjectArrayMap<RenderType, GpuBuffer>();
//
//        bufferBuilders.forEach((renderType, builder) -> {
//            final var buffer = builder.build();
//            if (buffer == null) return;
//
//            if (renderType.sortOnUpload()) {
//                var state = buffer.sortQuads(blockPack.buffer(renderType), sorting);
//                renderSortStates.put(renderType, state);
//            }
//
//            // TODO 21.5 Port
////            VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
////            vb.bind();
////            vb.upload(buffer);
////            VertexBuffer.unbind();
////            vertexBuffers.put(renderType, vb);
//        });
//
//        return new SortedGeometryBufferResult(vertexBuffers, renderSortStates);
//    }
}
