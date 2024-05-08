package dev.compactmods.gander.level.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor {
	@Accessor("palettes")
	List<StructureTemplate.Palette> getPalettes();
}
