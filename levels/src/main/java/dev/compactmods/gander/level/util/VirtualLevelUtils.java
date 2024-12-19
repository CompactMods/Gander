package dev.compactmods.gander.level.util;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Lifecycle;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
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
					new GameRules(FeatureFlags.VANILLA_SET),
					WorldDataConfiguration.DEFAULT
			),
			WorldOptions.defaultWithRandomSeed(),
			PrimaryLevelData.SpecialWorldProperty.NONE,
			Lifecycle.stable()
	);

	public static final Supplier<Holder<Biome>> PLAINS = Suppliers.memoize(() -> Minecraft.getInstance().level
			.registryAccess()
			.lookupOrThrow(Registries.BIOME)
			.getOrThrow(Biomes.PLAINS));

	public static final Supplier<ProfilerFiller> PROFILER = Profiler::get;
}
