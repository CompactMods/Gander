package com.simibubi.create.ponder.element;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.gui.Theme;
import com.simibubi.create.gui.element.BoxElement;
import com.simibubi.create.ponder.PonderLocalization;
import com.simibubi.create.ponder.PonderPalette;
import com.simibubi.create.ponder.PonderScene;
import com.simibubi.create.ponder.PonderScene.SceneTransform;
import com.simibubi.create.ponder.ui.PonderUI;
import com.simibubi.create.utility.Color;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TextWindowElement extends AnimatedOverlayElement {

	Supplier<String> textGetter = () -> "(?) No text was provided";
	String bakedText;

	// from 0 to 200
	int y;

	Vec3 vec;

	boolean nearScene = false;
	int color = PonderPalette.WHITE.getColor();

	public class Builder {

		private PonderScene scene;

		public Builder(PonderScene scene) {
			this.scene = scene;
		}

		public Builder colored(PonderPalette color) {
			TextWindowElement.this.color = color.getColor();
			return this;
		}

		public Builder pointAt(Vec3 vec) {
			TextWindowElement.this.vec = vec;
			return this;
		}

		public Builder independent(int y) {
			TextWindowElement.this.y = y;
			return this;
		}

		public Builder independent() {
			return independent(0);
		}

		public Builder text(String defaultText) {
			textGetter = scene.registerText(defaultText);
			return this;
		}

		public Builder sharedText(ResourceLocation key) {
			textGetter = () -> PonderLocalization.getShared(key);
			return this;
		}

		public Builder sharedText(String key) {
			return sharedText(new ResourceLocation(scene.getNamespace(), key));
		}

		public Builder placeNearTarget() {
			TextWindowElement.this.nearScene = true;
			return this;
		}

		public Builder attachKeyFrame() {
			scene.builder()
				.addLazyKeyframe();
			return this;
		}

	}

	@Override
	protected void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks, float fade) {
		if (bakedText == null)
			bakedText = textGetter.get();
		if (fade < 1 / 16f)
			return;
		SceneTransform transform = scene.getTransform();
		Vec2 sceneToScreen = vec != null ? transform.sceneToScreen(vec, partialTicks)
			: new Vec2(screen.width / 2, (screen.height - 200) / 2 + y - 8);

		boolean settled = transform.xRotation.settled() && transform.yRotation.settled();
		float pY = settled ? (int) sceneToScreen.y : sceneToScreen.y;

		float yDiff = (screen.height / 2f - sceneToScreen.y - 10) / 100f;
		float targetX = (screen.width * Mth.lerp(yDiff * yDiff, 6f / 8, 5f / 8));

		if (nearScene)
			targetX = Math.min(targetX, sceneToScreen.x + 50);

		if (settled)
			targetX = (int) targetX;

		int textWidth = (int) Math.min(screen.width - targetX, 180);

		List<FormattedText> lines = screen.getFontRenderer()
			.getSplitter()
			.splitLines(bakedText, textWidth, Style.EMPTY);

		int boxWidth = 0;
		for (FormattedText line : lines)
			boxWidth = Math.max(boxWidth, screen.getFontRenderer()
				.width(line));

		int boxHeight = screen.getFontRenderer()
			.wordWrapHeight(bakedText, boxWidth);

		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(0, pY, 400);

		new BoxElement().withBackground(Theme.color(Theme.Key.PONDER_BACKGROUND_FLAT))
			.gradientBorder(Theme.pair(Theme.Key.TEXT_WINDOW_BORDER))
			.at(targetX - 10, 3, 100)
			.withBounds(boxWidth, boxHeight - 1)
			.render(graphics);

		//PonderUI.renderBox(ms, targetX - 10, 3, boxWidth, boxHeight - 1, 0xaa000000, 0x30eebb00, 0x10eebb00);

		int brighterColor = Color.mixColors(color, 0xFFffffdd, 1 / 2f);
		brighterColor = (0x00ffffff & brighterColor) | 0xff000000;
		if (vec != null) {
			ms.pushPose();
			ms.translate(sceneToScreen.x, 0, 0);
			double lineTarget = (targetX - sceneToScreen.x) * fade;
			ms.scale((float) lineTarget, 1, 1);
			graphics.fillGradient(0, 0, 1, 1, -100, brighterColor, brighterColor);
			graphics.fillGradient(0, 1, 1, 2, -100, 0xFF494949, 0xFF393939);
			ms.popPose();
		}

		ms.translate(0, 0, 400);
		for (int i = 0; i < lines.size(); i++) {
			graphics.drawString(screen.getFontRenderer(), lines.get(i)
				.getString(), targetX - 10, 3 + 9 * i,
				new Color(brighterColor).scaleAlpha(fade)
					.getRGB(),
				false);
		}
		ms.popPose();
	}

	public int getColor() {
		return color;
	}

}
