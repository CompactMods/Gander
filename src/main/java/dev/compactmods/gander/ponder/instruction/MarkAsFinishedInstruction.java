package dev.compactmods.gander.ponder.instruction;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.instruction.contract.PonderInstruction;

public class MarkAsFinishedInstruction extends PonderInstruction {

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public void tick(PonderScene scene) {
		scene.setFinished(true);
	}

	@Override
	public void onScheduled(PonderScene scene) {
		scene.stopCounting();
	}

}
