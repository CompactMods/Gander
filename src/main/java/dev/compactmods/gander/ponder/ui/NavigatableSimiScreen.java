package dev.compactmods.gander.ponder.ui;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.compactmods.gander.gui.AbstractSimiScreen;
import dev.compactmods.gander.gui.ScreenOpener;
import dev.compactmods.gander.gui.Theme;
import dev.compactmods.gander.gui.UIRenderHelper;
import dev.compactmods.gander.ponder.PonderLocalization;
import dev.compactmods.gander.utility.animation.LerpedFloat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public abstract class NavigatableSimiScreen extends AbstractSimiScreen {

	public static final String THINK_BACK = PonderLocalization.LANG_PREFIX + "think_back";

	protected int depthPointX, depthPointY;
	public final LerpedFloat transition = LerpedFloat.linear()
			.startWithValue(0)
			.chase(0, .1f, LerpedFloat.Chaser.LINEAR);
	protected final LerpedFloat arrowAnimation = LerpedFloat.linear()
			.startWithValue(0)
			.chase(0, 0.075f, LerpedFloat.Chaser.LINEAR);
	protected PonderButton backTrack;

	public NavigatableSimiScreen() {
		Window window = Minecraft.getInstance().getWindow();
		depthPointX = window.getGuiScaledWidth() / 2;
		depthPointY = window.getGuiScaledHeight() / 2;
	}

	@Override
	public void onClose() {
		ScreenOpener.clearStack();
		super.onClose();
	}

	@Override
	public void tick() {
		super.tick();
		transition.tickChaser();
		arrowAnimation.tickChaser();
	}

	@Override
	protected void init() {
		super.init();

		backTrack = null;
		List<Screen> screenHistory = ScreenOpener.getScreenHistory();
		if (screenHistory.isEmpty())
			return;
		if (!(screenHistory.get(0) instanceof NavigatableSimiScreen))
			return;

        addRenderableWidget(backTrack = new PonderButton(31, height - 31 - 20).enableFade(0, 5)
			.withCallback(() -> ScreenOpener.openPreviousScreen(this, Optional.empty())));
		backTrack.fade(1);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);

		if (backTrack == null)
			return;

		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(0, 0, 500);
		if (backTrack.isHoveredOrFocused()) {
			MutableComponent translate = Component.translatable(THINK_BACK);
			graphics.drawString(font, translate, 41 - font.width(translate) / 2, height - 16,
					Theme.color(Theme.Key.TEXT_DARKER).getRGB(), false);
			if (Mth.equal(arrowAnimation.getValue(), arrowAnimation.getChaseTarget())) {
				arrowAnimation.setValue(1);
				arrowAnimation.setValue(1);// called twice to also set the previous value to 1
			}
		}
		ms.popPose();
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		if (backTrack != null) {
			int x = (int) Mth.lerp(arrowAnimation.getValue(partialTicks), -9, 21);
			int maxX = backTrack.getX() + backTrack.getWidth();

			if (x + 30 < backTrack.getX())
				UIRenderHelper.breadcrumbArrow(graphics, x + 30, height - 51, 0, maxX - (x + 30), 20, 5,
						Theme.pair(Theme.Key.PONDER_BACK_ARROW));

			UIRenderHelper.breadcrumbArrow(graphics, x, height - 51, 0, 30, 20, 5, Theme.pair(Theme.Key.PONDER_BACK_ARROW));
			UIRenderHelper.breadcrumbArrow(graphics, x - 30, height - 51, 0, 30, 20, 5, Theme.pair(Theme.Key.PONDER_BACK_ARROW));
		}

		if (transition.getChaseTarget() == 0 || transition.settled()) {
			super.renderBackground(graphics, mouseX, mouseY, partialTicks);
			return;
		}

		super.renderBackground(graphics, mouseX, mouseY, partialTicks);

		PoseStack ms = graphics.pose();

		float transitionValue = transition.getValue(partialTicks);

        // modify current screen as well
        float scale = transitionValue > 0 ? 1 - 0.5f * (1 - transitionValue) : 1 + .5f * (1 + transitionValue);
		ms.translate(depthPointX, depthPointY, 0);
		ms.scale(scale, scale, 1);
		ms.translate(-depthPointX, -depthPointY, 0);
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		if (code == GLFW.GLFW_KEY_BACKSPACE) {
			ScreenOpener.openPreviousScreen(this, Optional.empty());
			return true;
		}
		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
	}

	public boolean isEquivalentTo(NavigatableSimiScreen other) {
		return false;
	}

	public void shareContextWith(NavigatableSimiScreen other) {
	}
}
