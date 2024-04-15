package dev.compactmods.gander.ponder.element;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.ui.PonderUI;

import net.minecraft.client.gui.GuiGraphics;

public abstract class PonderOverlayElement extends PonderElement {

	public void tick(PonderScene scene) {}

	public abstract void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks);

}
