package dev.compactmods.gander.render.screen;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

import java.util.function.Consumer;

public record GanderScreenRenderHelper(int width, int height) {

    public Matrix4f projectionMatrix() {
        return new Matrix4f().setPerspective(
            (float) Math.PI / 2f,
            (float) width / (float) height,
            0.05f,
            10000000);
    }

    /**
     * Prepares the graphics pose stack and projection matrices for rendering to the screen.
     *
     * @param graphics GuiGraphics instance from the screen/render method
     * @param camera The camera instance used to view the scene from
     * @param render A consumer for receiving the set-up projection matrix
     */
    public void renderInScreenSpace(GuiGraphics graphics, Camera camera, Consumer<Matrix4f> render) {
        final var projMatrix = projectionMatrix();

        RenderSystem.setProjectionMatrix(projMatrix, ProjectionType.ORTHOGRAPHIC);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        {
            modelViewStack.identity();
//            RenderSystem.applyModelViewMatrix();

            poseStack.setIdentity();
            poseStack.mulPose(camera.rotation());

            render.accept(projMatrix);
        }

        modelViewStack.popMatrix();
//        RenderSystem.applyModelViewMatrix();

        poseStack.popPose();
    }
}
