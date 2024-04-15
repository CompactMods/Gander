package dev.compactmods.gander.ponder.element;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.ui.PonderUI;
import dev.compactmods.gander.utility.animation.LerpedFloat;

import net.minecraft.client.gui.GuiGraphics;

public abstract class AnimatedOverlayElement extends PonderOverlayElement {

	protected LerpedFloat fade;

	public AnimatedOverlayElement() {
		fade = LerpedFloat.linear()
			.startWithValue(0);
	}

	public void setFade(float fade) {
		this.fade.setValue(fade);
	}

	@Override
	public final void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks) {
		float currentFade = fade.getValue(partialTicks);
		render(scene, screen, graphics, partialTicks, currentFade);
	}

	protected abstract void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks, float fade);

}
