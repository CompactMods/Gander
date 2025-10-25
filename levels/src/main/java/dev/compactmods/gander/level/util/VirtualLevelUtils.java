package dev.compactmods.gander.level.util;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.WritableLevelData;

public class VirtualLevelUtils {

    public static final WritableLevelData LEVEL_DATA = new ClientLevel.ClientLevelData(
        Difficulty.PEACEFUL,
        false,
        true
    );
}
