package com.simibubi.create.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface ScreenElement {

	@OnlyIn(Dist.CLIENT)
	void render(GuiGraphics graphics, int x, int y);

}
