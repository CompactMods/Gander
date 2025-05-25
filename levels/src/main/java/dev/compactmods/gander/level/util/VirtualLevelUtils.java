package dev.compactmods.gander.level.util;

import com.mojang.serialization.Lifecycle;

import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;

public class VirtualLevelUtils {

	public static final ServerLevelData LEVEL_DATA = new PrimaryLevelData(
			new LevelSettings(
					"gander_level",
					GameType.SPECTATOR,
					false,
					Difficulty.PEACEFUL,
					false,
					new GameRules(FeatureFlagSet.of()),
					WorldDataConfiguration.DEFAULT
			),
			WorldOptions.defaultWithRandomSeed(),
			PrimaryLevelData.SpecialWorldProperty.NONE,
			Lifecycle.stable()
	);
}
