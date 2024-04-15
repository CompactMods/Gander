package dev.compactmods.gander.ponder.instruction;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.PonderLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AnimateBlockEntityInstruction extends TickingInstruction {

	protected double deltaPerTick;
	protected double totalDelta;
	protected double target;
	protected final BlockPos location;

	private final BiConsumer<PonderLevel, Float> setter;
	private final Function<PonderLevel, Float> getter;

	protected AnimateBlockEntityInstruction(BlockPos location, float totalDelta, int ticks,
                                            BiConsumer<PonderLevel, Float> setter, Function<PonderLevel, Float> getter) {
		super(false, ticks);
		this.location = location;
		this.setter = setter;
		this.getter = getter;
		this.deltaPerTick = totalDelta * (1d / ticks);
		this.totalDelta = totalDelta;
		this.target = totalDelta;
	}

	@Override
	protected final void firstTick(PonderScene scene) {
		super.firstTick(scene);
		target = getter.apply(scene.getWorld()) + totalDelta;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		PonderLevel world = scene.getWorld();
		float current = getter.apply(world);
		float next = (float) (remainingTicks == 0 ? target : current + deltaPerTick);
		setter.accept(world, next);
		if (remainingTicks == 0) // lock interpolation
			setter.accept(world, next);
	}

	private static <T> Optional<T> castIfPresent(PonderLevel world, BlockPos pos, Class<T> beType) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (beType.isInstance(blockEntity))
			return Optional.of(beType.cast(blockEntity));
		return Optional.empty();
	}

}
