package dev.compactmods.gander.ponder.instruction;

import java.util.function.UnaryOperator;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.PonderLevel;
import dev.compactmods.gander.ponder.Selection;

import dev.compactmods.gander.ponder.instruction.contract.WorldModifyInstruction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ReplaceBlocksInstruction extends WorldModifyInstruction {

	private final UnaryOperator<BlockState> stateToUse;
	private final boolean replaceAir;
	private final boolean spawnParticles;

	public ReplaceBlocksInstruction(Selection selection, UnaryOperator<BlockState> stateToUse, boolean replaceAir,
		boolean spawnParticles) {
		super(selection);
		this.stateToUse = stateToUse;
		this.replaceAir = replaceAir;
		this.spawnParticles = spawnParticles;
	}

	@Override
	protected void runModification(Selection selection, PonderScene scene) {
		PonderLevel world = scene.getWorld();
		selection.forEach(pos -> {
			if (!world.getBounds()
				.isInside(pos))
				return;
			BlockState prevState = world.getBlockState(pos);
			if (!replaceAir && prevState == Blocks.AIR.defaultBlockState())
				return;
			if (spawnParticles)
				world.addBlockDestroyEffects(pos, prevState);
			world.setBlockAndUpdate(pos, stateToUse.apply(prevState));
		});
	}

	@Override
	protected boolean needsRedraw() {
		return true;
	}

}
