package dev.compactmods.gander.ui.toolkit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

import java.util.function.BiConsumer;

public record GanderScreenRenderHelper(int width, int height) {

    public Matrix4f projectionMatrix() {
        return new Matrix4f().setPerspective(
            (float) Math.PI / 2f,
            (float) width / (float) height,
            0.05f,
            10000000);
    }

    public void renderInScreenSpace(GuiGraphics graphics, Camera camera, BiConsumer<Matrix4f, PoseStack> render) {
        final var projMatrix = projectionMatrix();

        RenderSystem.setProjectionMatrix(projMatrix, VertexSorting.byDistance(camera.getPosition().toVector3f()));

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        var poseStack2 = RenderSystem.getModelViewStack();
        poseStack2.pushMatrix();
        {
            poseStack2.identity();
            RenderSystem.applyModelViewMatrix();

            poseStack.setIdentity();
            poseStack.mulPose(camera.rotation());

            render.accept(projMatrix, poseStack);
        }

        poseStack2.popMatrix();
        RenderSystem.applyModelViewMatrix();

        poseStack.popPose();
    }
}
