package dev.compactmods.gander;

import com.mojang.serialization.Codec;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class GanderCodecs {

	public static final Codec<StructureTemplate> STRUCTURE = RecordCodecBuilder.create(i -> i.group(
			CompoundTag.CODEC.fieldOf("tag").forGetter(st -> st.save(new CompoundTag()))
	).apply(i, tag -> {
		final var t = new StructureTemplate();
		t.load(BuiltInRegistries.BLOCK.asLookup(), tag);
		return t;
	}));

}
