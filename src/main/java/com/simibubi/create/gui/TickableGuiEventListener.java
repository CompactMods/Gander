package com.simibubi.create.gui;

import net.minecraft.client.gui.components.events.GuiEventListener;

public interface TickableGuiEventListener extends GuiEventListener {
	void tick();
}
