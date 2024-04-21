package dev.compactmods.gander.ponder.instruction;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.instruction.contract.PonderInstruction;

public class KeyframeInstruction extends PonderInstruction {

    public static final KeyframeInstruction IMMEDIATE = new KeyframeInstruction(false);
    public static final KeyframeInstruction DELAYED = new KeyframeInstruction(true);

	private final boolean delayed;

    private KeyframeInstruction(boolean delayed) {
		this.delayed = delayed;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void tick(PonderScene scene) { }

    @Override
    public void onScheduled(PonderScene scene) {
        scene.markKeyframe(delayed ? 6 : 0);
    }
}
