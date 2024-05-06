package dev.compactmods.gander.level.util;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import com.mojang.serialization.Lifecycle;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class VirtualLevelUtils {

	public static final WritableLevelData LEVEL_DATA = Util.make(() -> {
		if(FMLEnvironment.dist.isClient())
			return new ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, false);

		return new PrimaryLevelData(
				new LevelSettings(
						"",
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
	});

	public static final Supplier<ProfilerFiller> PROFILER = () -> {
		if(FMLEnvironment.dist.isClient())
			return Minecraft.getInstance().getProfiler();

		var server = ServerLifecycleHooks.getCurrentServer();
		return server == null ? InactiveProfiler.INSTANCE : server.getProfiler();
	};
}
