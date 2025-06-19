package dev.compactmods.gander.level;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

public final class VirtualLevels {

    public static VirtualLevel containingStructure(StructureTemplate structure, RegistryAccess registryAccess) {
        var bounds = AABB.of(structure.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO));
        var virtualLevel = new VirtualLevel(registryAccess, true);

        virtualLevel.setBounds(bounds);
        structure.placeInWorld(virtualLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings().setKnownShape(true), RandomSource.create(), Block.UPDATE_KNOWN_SHAPE);

        virtualLevel.refreshBlockEntityModels();

        return virtualLevel;
    }

    public static VirtualLevel empty(RegistryAccess registryAccess) {
        return new VirtualLevel(registryAccess, false);
    }

    public static VirtualLevel emptyServer(RegistryAccess registryAccess) {
        return new VirtualLevel(registryAccess, true);
    }
}
