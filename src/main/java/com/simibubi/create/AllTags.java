package com.simibubi.create;

import java.util.Collections;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

public class AllTags {
	public static <T> TagKey<T> optionalTag(IForgeRegistry<T> registry,
											ResourceLocation id) {
		return registry.tags()
				.createOptionalTagKey(id, Collections.emptySet());
	}

	public static <T> TagKey<T> forgeTag(IForgeRegistry<T> registry, String path) {
		return optionalTag(registry, new ResourceLocation("forge", path));
	}

	public static TagKey<Item> forgeItemTag(String path) {
		return forgeTag(ForgeRegistries.ITEMS, path);
	}

	public enum AllFluidTags {
		HONEY(Create.ID, "honey", false, true);

		public final TagKey<Fluid> tag;
		public final boolean alwaysDatagen;

		AllFluidTags(String namespace, String path, boolean optional, boolean alwaysDatagen) {
			ResourceLocation id = new ResourceLocation(namespace, path == null ? name() : path);
			if (optional) {
				tag = optionalTag(ForgeRegistries.FLUIDS, id);
			} else {
				tag = FluidTags.create(id);
			}
			this.alwaysDatagen = alwaysDatagen;
		}

		private static void init() {
		}
	}

	public static void init() {
		AllFluidTags.init();
	}
}
