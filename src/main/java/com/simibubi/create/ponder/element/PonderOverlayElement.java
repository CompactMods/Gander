package com.simibubi.create.ponder.element;

import com.simibubi.create.ponder.PonderScene;
import com.simibubi.create.ponder.ui.PonderUI;

import net.minecraft.client.gui.GuiGraphics;

public abstract class PonderOverlayElement extends PonderElement {

	public void tick(PonderScene scene) {}

	public abstract void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks);

}
