package dev.compactmods.gander.ponder.instruction;

public class DelayInstruction extends TickingInstruction {

	public DelayInstruction(int ticks) {
		super(true, ticks);
	}

}
