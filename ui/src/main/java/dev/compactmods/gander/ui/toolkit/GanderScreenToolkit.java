package dev.compactmods.gander.ui.toolkit;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

public class GanderScreenToolkit {

    public static final PipelineState.Item<GraphicsStatus> PREVIOUS_GRAPHICS_MODE = new PipelineState.Item<>(GraphicsStatus.class);
    public static final PipelineState.Item<Matrix4f> ORIGINAL_MATRIX = new PipelineState.Item<>(Matrix4f.class);
    public static final PipelineState.Item<VertexSorting> ORIGINAL_VERTEX_SORTING = new PipelineState.Item<>(VertexSorting.class);

    public static final PipelineState.Item<TranslucencyChain> TRANSLUCENCY_CHAIN = new PipelineState.Item<>(TranslucencyChain.class);
    public static final PipelineState.Item<RenderTypeStore> RENDER_TYPE_STORE = new PipelineState.Item<>(RenderTypeStore.class);
    public static final PipelineState.Item<RenderTarget> RENDER_TARGET = new PipelineState.Item<>(RenderTarget.class);

    public static boolean switchToFabulous(PipelineState state, LevelRenderingContext context, GuiGraphics graphics, Camera camera) {
        final var opts = Minecraft.getInstance().options;
        state.set(PREVIOUS_GRAPHICS_MODE, opts.graphicsMode().get());
        opts.graphicsMode().set(GraphicsStatus.FABULOUS);
        return true;
    }

    public static boolean revertGraphicsMode(PipelineState pipelineState, LevelRenderingContext context, GuiGraphics graphics, Camera camera) {
        final var opts = Minecraft.getInstance().options;
        opts.graphicsMode().set(pipelineState.get(PREVIOUS_GRAPHICS_MODE));
        return true;
    }

    public static boolean setupRenderRequirements(PipelineState pipelineState, LevelRenderingContext context, GuiGraphics graphics, Camera camera) {
        final var mc = Minecraft.getInstance();
        final var renderTarget = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
        renderTarget.setClearColor(0, 0, 0, 0);

        pipelineState.set(RENDER_TARGET, renderTarget);

        final var translucencyChain = TranslucencyChain.builder()
            .addLayer(Gander.asResource("main"))
            .addLayer(Gander.asResource("entity"))
            .addLayer(Gander.asResource("water"))
            .addLayer(Gander.asResource("translucent"))
            .addLayer(Gander.asResource("item_entity"))
            .addLayer(Gander.asResource("particles"))
            .addLayer(Gander.asResource("clouds"))
            .addLayer(Gander.asResource("weather"))
            .build(renderTarget);

        pipelineState.set(TRANSLUCENCY_CHAIN, translucencyChain);

        final var renderTypeStore = new RenderTypeStore(translucencyChain);
        pipelineState.set(RENDER_TYPE_STORE, renderTypeStore);
        return true;
    }

    public static void backupProjectionMatrix(PipelineState state) {
        state.set(GanderScreenToolkit.ORIGINAL_MATRIX, RenderSystem.getProjectionMatrix());
        state.set(GanderScreenToolkit.ORIGINAL_VERTEX_SORTING, RenderSystem.getVertexSorting());
    }

    public static void restoreProjectionMatrix(PipelineState state) {
        RenderSystem.setProjectionMatrix(state.get(GanderScreenToolkit.ORIGINAL_MATRIX), state.get(GanderScreenToolkit.ORIGINAL_VERTEX_SORTING));
    }
}
