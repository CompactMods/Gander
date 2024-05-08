package dev.compactmods.gander.level.block;

import dev.compactmods.gander.level.light.VirtualLightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;

import org.jetbrains.annotations.Nullable;

public class VirtualBlockSystem {

	private final VirtualLevelHeightAccessor heightAccessor;
	private final VirtualBlockAndFluidStorage blockAndFluidStorage;

	private @Nullable VirtualBlockGetter blockGetter;
	private @Nullable VirtualBlockAndTintGetter blockAndTintGetter;
	private @Nullable VirtualLightEngine lightEngine;

	public VirtualBlockSystem() {
		this.heightAccessor = new VirtualLevelHeightAccessor();
		this.blockAndFluidStorage = new VirtualBlockAndFluidStorage();
	}

	public VirtualLevelHeightAccessor heightAccessor() {
		return heightAccessor;
	}

	public VirtualBlockAndFluidStorage blockAndFluidStorage() {
		return blockAndFluidStorage;
	}

	public LevelLightEngine lightEngine() {
		if(this.lightEngine == null) {
			this.lightEngine = new VirtualLightEngine(pos -> 15, skyPos -> 15, this::blockGetter);
		}

		return lightEngine;
	}

	public VirtualBlockGetter blockGetter() {
		if(blockGetter == null) {
			blockGetter = new VirtualBlockGetter(heightAccessor, blockAndFluidStorage);
		}

		return blockGetter;
	}

	public VirtualBlockAndTintGetter blockAndTintGetter() {
		if(blockAndTintGetter == null) {
			blockAndTintGetter = new VirtualBlockAndTintGetter(heightAccessor, blockAndFluidStorage, lightEngine());
		}

		return blockAndTintGetter;
	}
}
