package dev.compactmods.gander.ponder.instruction.ticking;

import dev.compactmods.gander.ponder.instruction.contract.TickingInstruction;

public class DelayInstruction extends TickingInstruction {

	public DelayInstruction(int ticks) {
		super(true, ticks);
	}

}
