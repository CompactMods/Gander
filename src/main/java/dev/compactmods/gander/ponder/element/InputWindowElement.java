package dev.compactmods.gander.ponder.element;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.gui.AllIcons;
import dev.compactmods.gander.ponder.PonderLocalization;
import dev.compactmods.gander.ponder.PonderPalette;
import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.ui.PonderUI;
import dev.compactmods.gander.utility.Pointing;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class InputWindowElement extends AnimatedOverlayElement {

	private final Pointing direction;
	ResourceLocation key;
	AllIcons icon;
	private final Vec3 sceneSpace;

	public InputWindowElement clone() {
		InputWindowElement inputWindowElement = new InputWindowElement(sceneSpace, direction);
		inputWindowElement.key = key;
		inputWindowElement.icon = icon;
		return inputWindowElement;
	}

	public InputWindowElement(Vec3 sceneSpace, Pointing direction) {
		this.sceneSpace = sceneSpace;
		this.direction = direction;
	}

	public InputWindowElement scroll() {
		icon = AllIcons.I_SCROLL;
		return this;
	}

	public InputWindowElement rightClick() {
		icon = AllIcons.I_RMB;
		return this;
	}

	public InputWindowElement leftClick() {
		icon = AllIcons.I_LMB;
		return this;
	}

	public InputWindowElement whileSneaking() {
		key = GanderLib.asResource("sneak_and");
		return this;
	}

	public InputWindowElement whileCTRL() {
		key = GanderLib.asResource("ctrl_and");
		return this;
	}

	@Override
	protected void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks, float fade) {
		Font font = screen.getFontRenderer();
		int width = 0;
		int height = 0;

		float xFade = direction == Pointing.RIGHT ? -1 : direction == Pointing.LEFT ? 1 : 0;
		float yFade = direction == Pointing.DOWN ? -1 : direction == Pointing.UP ? 1 : 0;
		xFade *= 10 * (1 - fade);
		yFade *= 10 * (1 - fade);

		boolean hasText = key != null;
		boolean hasIcon = icon != null;
		int keyWidth = 0;
		String text = hasText ? PonderLocalization.getShared(key) : "";

		if (fade < 1 / 16f)
			return;
		Vec2 sceneToScreen = scene.getTransform()
			.sceneToScreen(sceneSpace, partialTicks);

		if (hasIcon) {
			width += 24;
			height = 24;
		}

		if (hasText) {
			keyWidth = font.width(text);
			width += keyWidth;
		}

		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(sceneToScreen.x + xFade, sceneToScreen.y + yFade, 400);

		PonderUI.renderSpeechBox(graphics, 0, 0, width, height, false, direction, true);

		ms.translate(0, 0, 100);

		if (hasText)
			graphics.drawString(font, text, 2, (height - font.lineHeight) / 2f + 2,
				PonderPalette.WHITE.getColorObject().scaleAlpha(fade).getRGB(), false);

		if (hasIcon) {
			ms.pushPose();
			ms.translate(keyWidth, 0, 0);
			ms.scale(1.5f, 1.5f, 1.5f);
			icon.render(graphics, 0, 0);
			ms.popPose();
		}

		ms.popPose();
	}

}
