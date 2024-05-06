package dev.compactmods.gander.level.util;

import com.google.common.base.Supplier;
import com.mojang.serialization.Lifecycle;

import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class VirtualLevelUtils {

	public static final ServerLevelData LEVEL_DATA = new PrimaryLevelData(
			new LevelSettings(
					"gander_level",
					GameType.SPECTATOR,
					false,
					Difficulty.PEACEFUL,
					false,
					new GameRules(),
					WorldDataConfiguration.DEFAULT
			),
			WorldOptions.defaultWithRandomSeed(),
			PrimaryLevelData.SpecialWorldProperty.NONE,
			Lifecycle.stable()
	);

	public static final Supplier<ProfilerFiller> PROFILER = () -> {
		if(FMLEnvironment.dist.isClient())
			return Minecraft.getInstance().getProfiler();

		var server = ServerLifecycleHooks.getCurrentServer();
		return server == null ? InactiveProfiler.INSTANCE : server.getProfiler();
	};
}
