package dev.compactmods.gander.ui.toolkit;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;

public class GanderScreenToolkit {

    public static boolean switchToFabulous(PipelineState state) {
        final var opts = Minecraft.getInstance().options;
        state.set(GanderRenderToolkit.PREVIOUS_GRAPHICS_MODE, opts.graphicsMode().get());
        opts.graphicsMode().set(GraphicsStatus.FABULOUS);
        return true;
    }

    public static boolean revertGraphicsMode(PipelineState pipelineState) {
        final var opts = Minecraft.getInstance().options;
        opts.graphicsMode().set(pipelineState.get(GanderRenderToolkit.PREVIOUS_GRAPHICS_MODE));
        return true;
    }

    public static boolean setupBasicRenderRequirements(PipelineState pipelineState) {
        final var mc = Minecraft.getInstance();
        final var renderTarget = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
        renderTarget.setClearColor(0, 0, 0, 0);

        pipelineState.set(GanderRenderToolkit.RENDER_TARGET, renderTarget);

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

        pipelineState.set(GanderRenderToolkit.TRANSLUCENCY_CHAIN, translucencyChain);

        final var renderTypeStore = new RenderTypeStore(translucencyChain);
        pipelineState.set(GanderRenderToolkit.RENDER_TYPE_STORE, renderTypeStore);
        return true;
    }

    public static void backupProjectionMatrix(PipelineState state) {
        state.set(GanderRenderToolkit.ORIGINAL_MATRIX, RenderSystem.getProjectionMatrix());
        state.set(GanderRenderToolkit.ORIGINAL_VERTEX_SORTING, RenderSystem.getVertexSorting());
    }

    public static void restoreProjectionMatrix(PipelineState state) {
        RenderSystem.setProjectionMatrix(state.get(GanderRenderToolkit.ORIGINAL_MATRIX), state.get(GanderRenderToolkit.ORIGINAL_VERTEX_SORTING));
    }
}
