package dev.compactmods.gander.ponder.instruction;

import dev.compactmods.gander.ponder.PonderScene;

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
