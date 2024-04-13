package com.simibubi.create.ponder.instruction;

import com.simibubi.create.ponder.PonderPalette;
import com.simibubi.create.ponder.PonderScene;
import com.simibubi.create.ponder.Selection;

public class OutlineSelectionInstruction extends TickingInstruction {

	private PonderPalette color;
	private Object slot;
	private Selection selection;

	public OutlineSelectionInstruction(PonderPalette color, Object slot, Selection selection, int ticks) {
		super(false, ticks);
		this.color = color;
		this.slot = slot;
		this.selection = selection;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		selection.makeOutline(scene.getOutliner(), slot)
			.lineWidth(1 / 16f)
			.colored(color.getColor());
	}

}
