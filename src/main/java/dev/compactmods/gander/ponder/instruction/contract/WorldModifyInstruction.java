package dev.compactmods.gander.ponder.instruction.contract;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.Selection;
import dev.compactmods.gander.ponder.element.WorldSectionElement;

public abstract class WorldModifyInstruction extends PonderInstruction {

	private final Selection selection;

	public WorldModifyInstruction(Selection selection) {
		this.selection = selection;
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public void tick(PonderScene scene) {
		runModification(selection, scene);
		if (needsRedraw())
			scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
	}

	protected abstract void runModification(Selection selection, PonderScene scene);

	protected abstract boolean needsRedraw();

}
