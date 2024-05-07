package dev.compactmods.gander.level.util;

import dev.compactmods.gander.level.mixin.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public interface StructureTemplateUtils {
	static <T extends LevelWriter & BlockGetter> void place(StructureTemplate template, T level, RegistryAccess registryAccess, BlockPos origin, int flags) {
		var palettes = accessor(template).getPalettes();

		if(palettes.isEmpty())
			return;

		var palette = palettes.getFirst();
		var blockInfos = palette.blocks();

		for(var blockInfo : blockInfos) {
			var pos = origin.offset(blockInfo.pos());
			var blockState = blockInfo.state();

			var nbt = blockInfo.nbt();

			if(level.setBlock(pos, blockState, flags)) {
				if(nbt != null) {
					var blockEntity = level.getBlockEntity(pos);

					if(blockEntity != null)
						blockEntity.loadWithComponents(nbt, registryAccess);
				}
			}
		}
	}

	static void place(StructureTemplate template, Level level, BlockPos origin, int flags) {
		place(template, level, level.registryAccess(), origin, flags);
	}

	static StructureTemplateAccessor accessor(StructureTemplate template) {
		return StructureTemplateAccessor.class.cast(template);
	}
}
