package dev.compactmods.gander.ponder.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.render.SuperRenderTypeBuffer;
import dev.compactmods.gander.utility.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.Direction;

public class CompassOverlay implements Renderable {

	private final PonderScene scene;
	private final Font font;

	public CompassOverlay(PonderScene scene) {
		this.scene = scene;
		this.font = Minecraft.getInstance().font;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

		SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();

		final var bounds = scene.getBounds();

		PoseStack ms = graphics.pose();

		ms.pushPose();

		ms.scale(-1, -1, 1);
		ms.scale(1 / 16f, 1 / 16f, 1 / 16f);
		ms.translate(1, -8, -1 / 64f);

		// X AXIS
		ms.pushPose();
		ms.translate(4, -3, 0);
		ms.translate(0, 0, -2 / 1024f);
		for (int x = 0; x <= bounds.getXSpan(); x++) {
			ms.translate(-16, 0, 0);
			graphics.drawString(font, x == bounds.getXSpan() ? "x" : "" + x, 0, 0, 0xFFFFFFFF, false);
		}
		ms.popPose();

		// Z AXIS
		ms.pushPose();
		ms.scale(-1, 1, 1);
		ms.translate(0, -3, -4);
		ms.mulPose(Axis.YP.rotationDegrees(-90));
		ms.translate(-8, -2, 2 / 64f);
		for (int z = 0; z <= bounds.getZSpan(); z++) {
			ms.translate(16, 0, 0);
			graphics.drawString(font, z == bounds.getZSpan() ? "z" : "" + z, 0, 0, 0xFFFFFFFF, false);
		}
		ms.popPose();

		// DIRECTIONS
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
		buffer.draw();

		ms.popPose();
	}
}
