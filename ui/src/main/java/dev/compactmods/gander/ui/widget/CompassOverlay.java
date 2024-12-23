package dev.compactmods.gander.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class CompassOverlay implements Renderable {

	private @Nullable BoundingBox sceneBounds;
	private final Font font;

	public CompassOverlay() {
		this.font = Minecraft.getInstance().font;
	}

	public CompassOverlay(BoundingBox bounds) {
		this.sceneBounds = bounds;
		this.font = Minecraft.getInstance().font;
	}

	public void setBounds(BoundingBox sceneBounds) {
		this.sceneBounds = sceneBounds;
	}

	public void render(GuiGraphics graphics, float partialTicks) {
		render(graphics, 0, 0, partialTicks);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		if(sceneBounds == null)
			return;

		PoseStack poseStack = graphics.pose();

		poseStack.pushPose();

		int color = DyeColor.WHITE.getTextColor();

		renderXAxis(graphics, poseStack, sceneBounds, color);
		renderZAxis(graphics, poseStack, sceneBounds, color);
		renderCompassDirections(graphics, poseStack, sceneBounds);
		renderAxisWidget(graphics, poseStack);

		poseStack.popPose();
	}

	private void renderCompassDirections(GuiGraphics graphics, PoseStack ms, BoundingBox bounds) {
		ms.pushPose();
		ms.scale(-1, -1, 1);

		ms.translate(bounds.getXSpan() * -8, 0, bounds.getZSpan() * 8);
		ms.mulPose(Axis.YP.rotationDegrees(-90));

		final var horizontals = Direction.Plane.HORIZONTAL.iterator();
		final var distance = Math.max(
				sceneBounds.getXSpan(),
				sceneBounds.getZSpan());
		horizontals.forEachRemaining(d -> {
			ms.mulPose(Axis.YP.rotationDegrees(90));
			ms.pushPose();
			ms.translate(0, 0, distance * 16);
			ms.mulPose(Axis.XP.rotationDegrees(-90));
			graphics.drawString(font, d.name()
					.substring(0, 1), 0, 0, 0x66FFFFFF, false);
			graphics.drawString(font, "|", 2, 10, 0x44FFFFFF, false);
			graphics.drawString(font, ".", 2, 14, 0x22FFFFFF, false);
			ms.popPose();
		});
		ms.popPose();
	}

	private void renderXAxis(GuiGraphics graphics, PoseStack ms, BoundingBox bounds, int color) {
		ms.pushPose();
		ms.translate((bounds.getXSpan() + 2) * 16, -8, 0);
		ms.mulPose(Axis.YP.rotation((float)Math.PI));
		ms.translate(4, -4, 0);
		for (int x = 0; x <= bounds.getXSpan(); x++) {
			ms.translate(16, 0, 0);
			graphics.drawString(font, x == 0 ? "x" : Integer.toString(bounds.getXSpan() - x), 0, 0, color, true);
		}
		ms.popPose();
	}

	private void renderZAxis(GuiGraphics graphics, PoseStack ms, BoundingBox bounds, int color) {
		ms.pushPose();
		ms.translate(0, -8, -16);
		ms.mulPose(Axis.YP.rotation(-(float)Math.PI / 2f));
		ms.translate(4, -4, 0);
		for (int z = 0; z <= bounds.getZSpan(); z++) {
			ms.translate(16, 0, 0);
			graphics.drawString(font, z == bounds.getZSpan() ? "z" : "" + z, 0, 0, color, true);
		}
		ms.popPose();
	}

	private void renderAxisWidget(GuiGraphics graphics, PoseStack poseStack) {
		float pLineLength = Math.max(
				Math.max(
						sceneBounds.getXSpan(),
						sceneBounds.getYSpan()),
				sceneBounds.getZSpan()) * 16;

		var pose = graphics.pose().last().pose();

		var tesselator = RenderSystem.renderThreadTesselator();
		var buffer = tesselator.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        // RED
		buffer.addVertex(pose, 0.0f, 0.0f, 0.0f)
            .setColor(255, 0, 0, 255)
            .setNormal(1.0F, 0.0F, 0.0F);

		buffer.addVertex(pose, pLineLength, 0.0f, 0.0f)
            .setColor(255, 0, 0, 255)
            .setNormal(1.0F, 0.0F, 0.0F);

        // GREEN
		buffer.addVertex(pose, 0.0f, 0.0f, 0.0f)
            .setColor(0, 255, 0, 255)
            .setNormal(0.0F, 1.0F, 0.0F);

		buffer.addVertex(pose, 0.0f, -pLineLength, 0.0f)
            .setColor(0, 255, 0, 255)
            .setNormal(0.0F, 1.0F, 0.0F);

        // BLUE
		buffer.addVertex(pose, 0.0f, 0.0f, 0.0f)
            .setColor(127, 127, 255, 255)
            .setNormal(0.0F, 0.0F, 1.0F);

		buffer.addVertex(pose, 0.0f, 0.0f, pLineLength)
            .setColor(127, 127, 255, 255)
            .setNormal(0.0F, 0.0F, 1.0F);

		//RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        BufferUploader.drawWithShader(buffer.build());
		RenderSystem.enableCull();
		//RenderSystem.depthMask(true);
	}
}
