package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

public interface RenderPipelineLifecycleManager<TCtx extends LevelRenderingContext> {

    default void setup(TCtx ctx, GuiGraphics graphics, Camera camera) {}

    default void teardown(TCtx ctx, GuiGraphics graphics) {}
}
