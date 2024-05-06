package dev.compactmods.gander.level.util;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.WritableLevelData;

public class VirtualLevelUtils {

	public static final WritableLevelData LEVEL_DATA = new ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, true);

	public static final Supplier<Holder<Biome>> PLAINS = Suppliers.memoize(() -> Minecraft.getInstance().level
			.registryAccess()
			.registryOrThrow(Registries.BIOME)
			.getHolderOrThrow(Biomes.PLAINS));
}
