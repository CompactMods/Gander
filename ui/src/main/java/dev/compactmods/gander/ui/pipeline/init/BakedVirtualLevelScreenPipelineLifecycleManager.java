package dev.compactmods.gander.ui.pipeline.init;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.render.pipeline.RenderPipelineLifecycleManager;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

public class BakedVirtualLevelScreenPipelineLifecycleManager implements RenderPipelineLifecycleManager<BakedLevelScreenRenderingContext> {

    private GraphicsStatus prevGraphics;
    private Matrix4f originalMatrix;
    private VertexSorting originalSorting;

    @Override
    public void setup(BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, Camera camera) {
        final var opts = Minecraft.getInstance().options;
        this.prevGraphics = opts.graphicsMode().get();
        opts.graphicsMode().set(GraphicsStatus.FABULOUS);

        // #blameMojang
        if (!canRender(ctx)) {
            opts.graphicsMode().set(prevGraphics);
            return;
        }

        final var mc = Minecraft.getInstance();

        setupRenderSizeAndPrepTranslucency(ctx, mc, camera);

        this.originalMatrix = RenderSystem.getProjectionMatrix();
        this.originalSorting = RenderSystem.getVertexSorting();

        // Setup Render Target
        var mainTarget = mc.getMainRenderTarget();
        ctx.translucencyChain.clear();
        ctx.translucencyChain.prepareBackgroundColor(mainTarget);
        ctx.renderTarget.bindWrite(true);
    }

    private static void setupRenderSizeAndPrepTranslucency(BakedLevelScreenRenderingContext ctx, Minecraft mc, Camera camera) {
        var width = mc.getWindow().getWidth();
        var height = mc.getWindow().getHeight();
        if (width != ctx.renderTarget.width || height != ctx.renderTarget.height) {
            ctx.renderTarget.resize(width, height, Minecraft.ON_OSX);
            ctx.translucencyChain.resize(ctx.renderTarget.width, ctx.renderTarget.height);

            ctx.recalculateTranslucency(camera);
        }
    }

    @Override
    public void teardown(BakedLevelScreenRenderingContext ctx, GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();

        mc.getMainRenderTarget().bindWrite(true);
        ctx.renderTarget.blitToScreen(ctx.renderTarget.width, ctx.renderTarget.height, false);
        RenderSystem.setProjectionMatrix(this.originalMatrix, this.originalSorting);

        // Restore Graphics Mode
        final var opts = Minecraft.getInstance().options;
        opts.graphicsMode().set(this.prevGraphics);
    }

    public boolean canRender(BakedLevelScreenRenderingContext ctx) {
        return ctx.blockAndTints != null && ctx.blockBoundaries != null;
    }
}
