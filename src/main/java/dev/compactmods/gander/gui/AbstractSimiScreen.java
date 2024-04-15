package dev.compactmods.gander.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.compactmods.gander.gui.widget.AbstractSimiWidget;
import dev.compactmods.gander.utility.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public abstract class AbstractSimiScreen extends Screen {

	protected int windowWidth, windowHeight;
	protected int windowXOffset, windowYOffset;
	protected int guiLeft, guiTop;

	protected AbstractSimiScreen(Component title) {
		super(title);
	}

	protected AbstractSimiScreen() {
		this(Components.immutableEmpty());
	}

	@Override
	protected void init() {
		guiLeft = (width - windowWidth) / 2;
		guiTop = (height - windowHeight) / 2;
		guiLeft += windowXOffset;
		guiTop += windowYOffset;
	}

	@Override
	public void tick() {
		for (GuiEventListener listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}
	}

	@Override
	public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		if (getFocused() != null && !getFocused().isMouseOver(pMouseX, pMouseY))
			setFocused(null);
		return super.mouseClicked(pMouseX, pMouseY, pButton);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		partialTicks = minecraft.getFrameTime();
		PoseStack ms = graphics.pose();

		ms.pushPose();

		prepareFrame();

		renderBackground(graphics, mouseX, mouseY, partialTicks);
		renderWindow(graphics, mouseX, mouseY, partialTicks);
		super.render(graphics, mouseX, mouseY, partialTicks);
		renderWindowForeground(graphics, mouseX, mouseY, partialTicks);

		endFrame();

		ms.popPose();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
		if (keyPressed || getFocused() instanceof EditBox)
			return keyPressed;

		InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
		if (this.minecraft.options.keyInventory.isActiveAndMatches(mouseKey)) {
			this.onClose();
			return true;
		}

		return false;
	}

	protected void prepareFrame() {}

	protected abstract void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);

	protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		for (Renderable widget : renderables) {
			if (widget instanceof AbstractSimiWidget simiWidget && simiWidget.isMouseOver(mouseX, mouseY)
				&& simiWidget.visible) {
				List<Component> tooltip = simiWidget.getToolTip();
				if (tooltip.isEmpty())
					continue;
				int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
				int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
				graphics.renderComponentTooltip(font, tooltip, ttx, tty);
			}
		}
	}

	protected void endFrame() {}

	@Deprecated
	protected void debugWindowArea(GuiGraphics graphics) {
		graphics.fill(guiLeft + windowWidth, guiTop + windowHeight, guiLeft, guiTop, 0xD3D3D3D3);
	}

	@Override
	public GuiEventListener getFocused() {
		GuiEventListener focused = super.getFocused();
		if (focused instanceof AbstractWidget && !focused.isFocused())
			focused = null;
		setFocused(focused);
		return focused;
	}

}
