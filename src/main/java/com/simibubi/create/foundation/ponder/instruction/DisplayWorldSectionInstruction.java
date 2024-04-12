package com.simibubi.create.foundation.ponder.instruction;

import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.Selection;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class DisplayWorldSectionInstruction extends FadeIntoSceneInstruction<WorldSectionElement> {

	private Selection initialSelection;
	private Optional<Supplier<WorldSectionElement>> mergeOnto;

	public DisplayWorldSectionInstruction(int fadeInTicks, Direction fadeInFrom, Selection selection,
		Optional<Supplier<WorldSectionElement>> mergeOnto) {
		super(fadeInTicks, fadeInFrom, new WorldSectionElement(selection));
		initialSelection = selection;
		this.mergeOnto = mergeOnto;
	}

	@Override
	protected void firstTick(PonderScene scene) {
		super.firstTick(scene);
		mergeOnto.ifPresent(wse -> element.setAnimatedOffset(wse.get()
			.getAnimatedOffset(), true));
		element.set(initialSelection);
		element.setVisible(true);
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		if (remainingTicks > 0)
			return;
		mergeOnto.ifPresent(c -> element.mergeOnto(c.get()));
	}

	@Override
	protected Class<WorldSectionElement> getElementClass() {
		return WorldSectionElement.class;
	}

}
