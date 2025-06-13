package dev.compactmods.gander.render.screen;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass.Draw;

import dev.compactmods.gander.render.geometry.CombinedRenderTypeBufferPool;
import dev.compactmods.gander.render.geometry.CombinedRenderTypeBufferPool.UploadResult;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;

public class GanderScreenPipelinePhases {

//    public static final PipelineGeometryUploadPhase BLOCK_ENTITIES_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::blockEntitiesPass;
//    public static final PipelineGeometryUploadPhase TRANSLUCENT_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::translucentPass;

    private static final PipelineState.Item<Reference2ObjectOpenHashMap<RenderType, PooledGeometry>> POOLED_STATIC_GEOMETRY
        = new PipelineState.Item<>(clazz(Reference2ObjectOpenHashMap.class));
    private static <T> Class<T> clazz(Class<?> baseClass) { return (Class<T>)baseClass; }

    private record PooledGeometry(
        CombinedRenderTypeBufferPool pool,
        Long2ObjectOpenHashMap<CombinedRenderTypeBufferPool.UploadResult> sectionsToDraw,
        Long2ObjectOpenHashMap<Function<CommandEncoder, CombinedRenderTypeBufferPool.UploadResult>> uploadTasks)
    { }
    public static void uploadStaticGeometry(PipelineState state, CommandEncoder encoder) {
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        var pools = state.get(POOLED_STATIC_GEOMETRY);
        if (pools == null) {
            pools = new Reference2ObjectOpenHashMap<>();
            state.set(POOLED_STATIC_GEOMETRY, pools);
        }

        for (var entry : pools.entrySet()) {
            var poolInfo = entry.getValue();
            poolInfo.uploadTasks.clear();
            poolInfo.sectionsToDraw.clear();
        }

        // Check if we need to upload data
        for (var section : bakedLevel.sections().entrySet()) {
            // TODO: fustrum culling

            for (var renderMesh : section.getValue().meshData().entrySet()) {
                var poolInfo = pools.computeIfAbsent(renderMesh.getKey(),
                    $ -> new PooledGeometry(
                        new CombinedRenderTypeBufferPool(),
                        new Long2ObjectOpenHashMap<>(),
                        new Long2ObjectOpenHashMap<>()));

                // TODO: check if modified since last upload
                var offset = poolInfo.pool.getOffsetOf(section.getKey());
                if (offset >= 0) {
                    poolInfo.sectionsToDraw.put(
                        section.getKey().asLong(),
                        new UploadResult(offset, renderMesh.getValue().drawState()));
                } else {
                    var uploadTask = poolInfo.pool.queueUpload(section.getKey(), renderMesh.getValue());
                    poolInfo.uploadTasks.put(section.getKey().asLong(), uploadTask);
                }
            }
        }

        // Actually perform all of the uploads necessary
        for (var pool : pools.entrySet()) {
            var it = pool.getValue().uploadTasks.long2ObjectEntrySet().fastIterator();
            while (it.hasNext()) {
                var uploadTask = it.next();
                var result = uploadTask.getValue().apply(encoder);
                pool.getValue().sectionsToDraw.put(uploadTask.getLongKey(), result);
                it.remove();
            }
        }
    }

    public static void renderStaticGeometry(PipelineState state, CommandEncoder encoder, float partialTicks) {
        final var mc = Minecraft.getInstance();
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var pools = state.get(POOLED_STATIC_GEOMETRY);

        for (var layer : pools.entrySet()) {
            var renderType = layer.getKey();
            var pooled = layer.getValue();

            assert pooled.uploadTasks.isEmpty();

            // TODO: this should use our redirected targets
            var pass = encoder.createRenderPass(
                renderType.getRenderTarget().getColorTexture(),
                OptionalInt.empty(),
                renderType.getRenderTarget().getDepthTexture(),
                OptionalDouble.empty());

            pass.setPipeline(renderType.getRenderPipeline());

            // TODO: we very likely need to upload uniforms here

            var draws = new ArrayList<Draw>();
            for (var sectionToDraw : pooled.sectionsToDraw.long2ObjectEntrySet()) {
                var info = sectionToDraw.getValue();

                // We can't pass `offset` directly as it gets multiplied by the size of the index bytes...
                var offset = info.offset() / info.drawState().indexType().bytes;

                draws.add(new Draw(
                    0, pooled.pool.getVertexBuffer(),
                    pooled.pool.getIndexBuffer(),
                    info.drawState().indexType(),
                    offset,
                    info.drawState().indexCount()));
            }

            pass.drawMultipleIndexed(draws, null, null);
        }
    }

    private static void blockEntitiesPass(PipelineState state, GuiGraphics graphics, float partialTicks) {
        final var mc = Minecraft.getInstance();
        final var bufferSource = mc.renderBuffers().bufferSource();
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var lookFrom = camera.getPosition().toVector3f();
        // final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        final var blockEntityPositions = state.get(GanderRenderToolkit.BLOCK_ENTITY_POSITIONS);

        final var partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        final var blockEntities = Arrays.stream(blockEntityPositions)
            .map(bakedLevel.originalLevel()::getBlockEntity)
            .filter(Objects::nonNull);

//        chain.prepareLayer(Gander.asResource("entity"));

        final var dispatcher = mc.getBlockEntityRenderDispatcher();
        dispatcher.prepare(bakedLevel.originalLevel(), camera, null);

//        BlockEntityRender.render(bakedLevel.originalLevel(), blockEntities, graphics.pose(), lookFrom, renderTypeStore, bufferSource, partialTick);
    }

    private static void translucentPass(PipelineState state, GuiGraphics graphics, float partialTicks) {
//        final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var renderOrigin = state.getOrDefault(GanderRenderToolkit.RENDER_ORIGIN, new Vector3f());
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        final var projectionMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);

//        chain.prepareLayer(Gander.asResource("translucent"));

        final var camPos = camera.getPosition().toVector3f();

        for(var section : bakedLevel.sections().values()) {
//            BlockRenderer.renderSectionFluids(section, RenderType.translucent(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
//            BlockRenderer.renderSectionBlocks(section, RenderType.translucent(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
        }
    }
}
