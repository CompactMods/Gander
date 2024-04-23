package dev.compactmods.gander.ponder.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.compactmods.gander.client.gui.UIRenderHelper;
import dev.compactmods.gander.ponder.Scene;
import dev.compactmods.gander.utility.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.jetbrains.annotations.Nullable;

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

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		if(sceneBounds == null)
			return;

		PoseStack poseStack = graphics.pose();

		poseStack.pushPose();

		int color = DyeColor.WHITE.getTextColor();

		UIRenderHelper.flipForGuiRender(poseStack);
		poseStack.scale(1 / 16f, 1 / 16f, 1 / 16f);
		poseStack.translate(0, -8, 0);
		// poseStack.translate(1, -8, -1 / 64f);

		renderXAxis(graphics, poseStack, sceneBounds, color);
		renderZAxis(graphics, poseStack, sceneBounds, color);
		renderCompassDirections(graphics, poseStack, sceneBounds);

		graphics.bufferSource().endBatch();

		// renderAxisWidget(graphics, poseStack);

		poseStack.popPose();
	}

	private void renderCompassDirections(GuiGraphics graphics, PoseStack ms, BoundingBox bounds) {
		ms.pushPose();
		ms.translate(bounds.getXSpan() * -8, 0, bounds.getZSpan() * 8);
		ms.mulPose(Axis.YP.rotationDegrees(-90));
		for (Direction d : Iterate.horizontalDirections) {
			ms.mulPose(Axis.YP.rotationDegrees(90));
			ms.pushPose();
			ms.translate(0, 0, bounds.getZSpan() * 16);
			ms.mulPose(Axis.XP.rotationDegrees(-90));
			graphics.drawString(font, d.name()
					.substring(0, 1), 0, 0, 0x66FFFFFF, false);
			graphics.drawString(font, "|", 2, 10, 0x44FFFFFF, false);
			graphics.drawString(font, ".", 2, 14, 0x22FFFFFF, false);
			ms.popPose();
		}
		ms.popPose();
	}

	private void renderXAxis(GuiGraphics graphics, PoseStack ms, BoundingBox bounds, int color) {
		ms.pushPose();
		ms.translate(-5, -4, 0);
		ms.mulPose(Axis.YP.rotationDegrees(180));
		ms.translate(0, 0, -2 / 1024f);
		for (int x = 0; x <= bounds.getXSpan(); x++) {
			ms.translate(-16, 0, 0);
			graphics.drawString(font, x == bounds.getXSpan() ? "x" : "" + x, 0, 0, color, true);
		}
		ms.popPose();
	}

	private void renderZAxis(GuiGraphics graphics, PoseStack ms, BoundingBox bounds, int color) {
		ms.pushPose();
		ms.translate(0, -2, -3);
		ms.mulPose(Axis.YP.rotationDegrees(-90));
		ms.translate(-8, -2, 2 / 64f);
		for (int z = 0; z <= bounds.getZSpan(); z++) {
			ms.translate(16, 0, 0);
			graphics.drawString(font, z == bounds.getZSpan() ? "z" : "" + z, 0, 0, color, true);
		}
		ms.popPose();
	}

	private void renderAxisWidget(GuiGraphics graphics, PoseStack poseStack) {

		// TODO - Figure out a less cursed system, this won't work

//		double pLineLength = 1000;
//
//		RenderSystem.disableDepthTest();
//		RenderSystem.disableCull();
//		RenderSystem.lineWidth(4.0F);
//
//		poseStack.pushPose();
//		poseStack.scale(100, 100, 100);
//
//		var bufferbuilder = graphics.bufferSource().getBuffer(RenderType.LINES);
//		bufferbuilder.vertex(0.0, 0.0, 0.0).color(255, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
//		bufferbuilder.vertex(pLineLength, 0.0, 0.0).color(255, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
//
//		bufferbuilder.vertex(0.0, 0.0, 0.0).color(0, 255, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
//		bufferbuilder.vertex(0.0, pLineLength, 0.0).color(0, 255, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
//
//		bufferbuilder.vertex(0.0, 0.0, 0.0).color(127, 127, 255, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
//		bufferbuilder.vertex(0.0, 0.0, pLineLength).color(127, 127, 255, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
//
//		graphics.bufferSource().endBatch(RenderType.LINES);
//
//		poseStack.popPose();
//
//		RenderSystem.lineWidth(1.0F);
//		RenderSystem.enableCull();
//		RenderSystem.enableDepthTest();
	}
}
