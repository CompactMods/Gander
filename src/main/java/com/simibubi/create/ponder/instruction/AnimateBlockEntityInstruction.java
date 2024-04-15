package com.simibubi.create.ponder.instruction;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.simibubi.create.ponder.PonderScene;
import com.simibubi.create.ponder.PonderWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AnimateBlockEntityInstruction extends TickingInstruction {

	protected double deltaPerTick;
	protected double totalDelta;
	protected double target;
	protected final BlockPos location;

	private final BiConsumer<PonderWorld, Float> setter;
	private final Function<PonderWorld, Float> getter;

	protected AnimateBlockEntityInstruction(BlockPos location, float totalDelta, int ticks,
		BiConsumer<PonderWorld, Float> setter, Function<PonderWorld, Float> getter) {
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
		PonderWorld world = scene.getWorld();
		float current = getter.apply(world);
		float next = (float) (remainingTicks == 0 ? target : current + deltaPerTick);
		setter.accept(world, next);
		if (remainingTicks == 0) // lock interpolation
			setter.accept(world, next);
	}

	private static <T> Optional<T> castIfPresent(PonderWorld world, BlockPos pos, Class<T> beType) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (beType.isInstance(blockEntity))
			return Optional.of(beType.cast(blockEntity));
		return Optional.empty();
	}

}
