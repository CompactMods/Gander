package dev.compactmods.gander.render.screen;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
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

    public static boolean setupRenderTarget(PipelineState state) {
        final var mc = Minecraft.getInstance();

        final var renderTarget = new TextureTarget("gander", mc.getWindow().getWidth(), mc.getWindow().getHeight(), true);

        state.set(GanderRenderToolkit.RENDER_TARGET, renderTarget);
        return true;
    }

    public static boolean setupTranslucencyChain(PipelineState pipelineState) {
        final var renderTarget = pipelineState.get(GanderRenderToolkit.RENDER_TARGET);

//        final var translucencyChain = TranslucencyChain.builder()
//            .addLayer(Gander.asResource("main"))
//            .addLayer(Gander.asResource("entity"))
//            .addLayer(Gander.asResource("water"))
//            .addLayer(Gander.asResource("translucent"))
//            .addLayer(Gander.asResource("item_entity"))
//            .addLayer(Gander.asResource("particles"))
//            .addLayer(Gander.asResource("clouds"))
//            .addLayer(Gander.asResource("weather"))
//            .build(renderTarget);
//
//        pipelineState.set(GanderRenderToolkit.TRANSLUCENCY_CHAIN, translucencyChain);

//        final var renderTypeStore = new RenderTypeStore(translucencyChain);
//        pipelineState.set(GanderRenderToolkit.RENDER_TYPE_STORE, renderTypeStore);
        return true;
    }

    public static void backupProjectionMatrix(PipelineState state) {
        state.set(GanderRenderToolkit.ORIGINAL_CPU_BUFFER_SLICE, RenderSystem.getProjectionMatrixBuffer());
        state.set(GanderRenderToolkit.ORIGINAL_PROJECTION_TYPE, RenderSystem.getProjectionType());
    }

    public static void restoreProjectionMatrix(PipelineState state) {
        RenderSystem.setProjectionMatrix(
            state.get(GanderRenderToolkit.ORIGINAL_CPU_BUFFER_SLICE),
            state.get(GanderRenderToolkit.ORIGINAL_PROJECTION_TYPE)
        );
    }
}
