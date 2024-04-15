package dev.compactmods.gander.ponder.instruction;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.element.InputWindowElement;

public class ShowInputInstruction extends FadeInOutInstruction {

	private final InputWindowElement element;

	public ShowInputInstruction(InputWindowElement element, int ticks) {
		super(ticks);
		this.element = element;
	}

	@Override
	protected void show(PonderScene scene) {
		scene.addElement(element);
		element.setVisible(true);
	}

	@Override
	protected void hide(PonderScene scene) {
		element.setVisible(false);
	}

	@Override
	protected void applyFade(PonderScene scene, float fade) {
		element.setFade(fade);
	}

}
