package dev.compactmods.gander.ponder.instruction;

import dev.compactmods.gander.ponder.element.MinecartElement;

import net.minecraft.core.Direction;

public class CreateMinecartInstruction extends FadeIntoSceneInstruction<MinecartElement> {

	public CreateMinecartInstruction(int fadeInTicks, Direction fadeInFrom, MinecartElement element) {
		super(fadeInTicks, fadeInFrom, element);
	}

	@Override
	protected Class<MinecartElement> getElementClass() {
		return MinecartElement.class;
	}

}
