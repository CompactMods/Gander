package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.render.vertex.FluidVertexConsumer;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;

import net.neoforged.neoforge.client.model.data.ModelData;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class LevelBakery {

    public static BakedLevel bakeVertices(Level level, BoundingBox blockBoundaries, Vector3f cameraPosition) {
        final SectionBufferBuilderPack blockPack = new SectionBufferBuilderPack();
        final SectionBufferBuilderPack fluidPack = new SectionBufferBuilderPack();

        final Map<RenderType, BufferBuilder> blockBufferBuilders = new HashMap<>();
        final Map<RenderType, BufferBuilder> fluidBufferBuilders = new HashMap<>();

        final Reference2ObjectArrayMap<RenderType, VertexBuffer> blockGeometry = new Reference2ObjectArrayMap<>();
        final Reference2ObjectArrayMap<RenderType, VertexBuffer> fluidGeometry = new Reference2ObjectArrayMap<>();

        final Reference2ObjectArrayMap<RenderType, MeshData.SortState> blockRenderSortStates = new Reference2ObjectArrayMap<>();
        final Reference2ObjectArrayMap<RenderType, MeshData.SortState> fluidRenderSortStates = new Reference2ObjectArrayMap<>();

        final var SORTING = VertexSorting.byDistance(cameraPosition.x, cameraPosition.y, cameraPosition.z);

        PoseStack pose = new PoseStack();
        RandomSource random = RandomSource.createNewThreadLocalInstance();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        ModelBlockRenderer renderer = dispatcher.getModelRenderer();

        ModelBlockRenderer.enableCaching();
        BlockPos.betweenClosedStream(blockBoundaries).forEach(pos -> {
            createBlockGeometry(level, pos, pose, dispatcher, random, blockBufferBuilders, blockPack, renderer, fluidBufferBuilders);
        });

        sortGeometry(blockBufferBuilders, blockPack, SORTING, blockRenderSortStates, blockGeometry, fluidBufferBuilders,
            fluidRenderSortStates, fluidGeometry);

        return new BakedLevel(level, blockPack, fluidPack, blockGeometry, fluidGeometry,
            blockRenderSortStates, fluidRenderSortStates, blockBoundaries);
    }

    private static void createBlockGeometry(Level level, BlockPos pos, PoseStack pose, BlockRenderDispatcher dispatcher,
                                            RandomSource random, Map<RenderType, BufferBuilder> blockBufferBuilders,
                                            SectionBufferBuilderPack blockPack, ModelBlockRenderer renderer,
                                            Map<RenderType, BufferBuilder> fluidBufferBuilders) {

        BlockState state = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);

        pose.pushPose();
        pose.translate(pos.getX(), pos.getY(), pos.getZ());

        ModelData modelData = ModelData.EMPTY;
        if (state.getRenderShape() == RenderShape.MODEL) {
            BakedModel model = dispatcher.getBlockModel(state);

            if (state.hasBlockEntity()) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                modelData = blockEntity != null ? blockEntity.getModelData() : ModelData.EMPTY;
            }

            modelData = model.getModelData(level, pos, state, modelData);

            long seed = state.getSeed(pos);
            random.setSeed(seed);

            ModelData finalModelData = modelData;
            model.getRenderTypes(state, random, modelData).forEach(type -> {
                var vertexBuilder = blockBufferBuilders.computeIfAbsent(type, t -> {
                    var typedVC = blockPack.buffer(t);
                    return new BufferBuilder(typedVC, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                });

                renderer.tesselateBlock(level, model, state, pos, pose, vertexBuilder, true, random, seed, OverlayTexture.NO_OVERLAY, finalModelData, type);
            });
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
        ModelBlockRenderer.clearCache();
    }

    private static void sortGeometry(Map<RenderType, BufferBuilder> blockBufferBuilders, SectionBufferBuilderPack blockPack, VertexSorting SORTING, Reference2ObjectArrayMap<RenderType, MeshData.SortState> blockRenderSortStates, Reference2ObjectArrayMap<RenderType, VertexBuffer> blockGeometry, Map<RenderType, BufferBuilder> fluidBufferBuilders, Reference2ObjectArrayMap<RenderType, MeshData.SortState> fluidRenderSortStates, Reference2ObjectArrayMap<RenderType, VertexBuffer> fluidGeometry) {
        blockBufferBuilders.forEach((renderType, builder) -> {
            final var buffer = builder.build();
            if(buffer == null) return;

            if (renderType.sortOnUpload()) {
                var state = buffer.sortQuads(blockPack.buffer(renderType), SORTING);
                blockRenderSortStates.put(renderType, state);
            }

            var vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vb.bind();
            vb.upload(buffer);
            VertexBuffer.unbind();
            blockGeometry.put(renderType, vb);
        });

        fluidBufferBuilders.forEach((renderType, builder) -> {
            final var buffer = builder.build();
            if(buffer == null) return;

            if (renderType.sortOnUpload()) {
                var state = buffer.sortQuads(blockPack.buffer(renderType), SORTING);
                fluidRenderSortStates.put(renderType, state);
            }

            var vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vb.bind();
            vb.upload(buffer);
            VertexBuffer.unbind();
            fluidGeometry.put(renderType, vb);
        });
    }
}
