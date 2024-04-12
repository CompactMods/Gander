package com.simibubi.create.datagen;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.AllTags.AllEntityTags;
import com.simibubi.create.AllTags.AllFluidTags;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.TagGen.CreateTagsProvider;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.Tags;

public class CreateRegistrateTags {
	public static void addGenerators() {
		Create.REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, CreateRegistrateTags::genBlockTags);
		Create.REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, CreateRegistrateTags::genItemTags);
		Create.REGISTRATE.addDataGenerator(ProviderType.FLUID_TAGS, CreateRegistrateTags::genFluidTags);
		Create.REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, CreateRegistrateTags::genEntityTags);
	}

	private static void genBlockTags(RegistrateTagsProvider<Block> provIn) {
		CreateTagsProvider<Block> prov = new CreateTagsProvider<>(provIn, Block::builtInRegistryHolder);

		prov.tag(AllBlockTags.BRITTLE.tag)
			.add(Blocks.BELL, Blocks.COCOA, Blocks.FLOWER_POT)
			.addTag(BlockTags.BEDS)
			.addTag(BlockTags.DOORS);

		prov.tag(AllBlockTags.COPYCAT_ALLOW.tag)
			.add(Blocks.BARREL);

		prov.tag(AllBlockTags.COPYCAT_DENY.tag)
			.addTag(BlockTags.CAULDRONS)
			.addTag(BlockTags.SAPLINGS)
			.addTag(BlockTags.CLIMBABLE);

		prov.tag(AllBlockTags.FAN_PROCESSING_CATALYSTS_HAUNTING.tag)
			.add(Blocks.SOUL_FIRE)
			.add(Blocks.SOUL_CAMPFIRE);

		prov.tag(AllBlockTags.FAN_PROCESSING_CATALYSTS_SMOKING.tag)
			.add(Blocks.FIRE)
			.add(Blocks.CAMPFIRE);

		prov.tag(AllBlockTags.FAN_TRANSPARENT.tag)
			.add(Blocks.IRON_BARS)
			.add(Blocks.MANGROVE_ROOTS)
			.addTag(BlockTags.CAMPFIRES)
			.addTag(BlockTags.FENCES)
			.addTag(BlockTags.LEAVES);

		prov.tag(AllBlockTags.MOVABLE_EMPTY_COLLIDER.tag)
			.add(Blocks.COBWEB, Blocks.POWDER_SNOW, Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK)
			.addTag(BlockTags.FENCE_GATES);

		prov.tag(AllBlockTags.ORE_OVERRIDE_STONE.tag)
			.addTag(BlockTags.STONE_ORE_REPLACEABLES);

		prov.tag(AllBlockTags.PASSIVE_BOILER_HEATERS.tag)
			.add(Blocks.MAGMA_BLOCK, Blocks.LAVA)
			.addTag(BlockTags.CAMPFIRES)
			.addTag(BlockTags.FIRE);

		prov.tag(AllBlockTags.SAFE_NBT.tag)
			.addTag(BlockTags.BANNERS)
			.addTag(BlockTags.SIGNS);

		prov.tag(AllBlockTags.TREE_ATTACHMENTS.tag)
			.add(Blocks.BEE_NEST, Blocks.COCOA, Blocks.MANGROVE_PROPAGULE, Blocks.MOSS_CARPET, Blocks.SHROOMLIGHT, Blocks.VINE);

		prov.tag(AllBlockTags.WINDMILL_SAILS.tag)
			.addTag(BlockTags.WOOL);

		prov.tag(AllBlockTags.WRENCH_PICKUP.tag)
			.add(Blocks.REDSTONE_WIRE, Blocks.REDSTONE_TORCH, Blocks.REPEATER, Blocks.LEVER,
					Blocks.COMPARATOR, Blocks.OBSERVER, Blocks.REDSTONE_WALL_TORCH, Blocks.PISTON, Blocks.STICKY_PISTON,
					Blocks.TRIPWIRE, Blocks.TRIPWIRE_HOOK, Blocks.DAYLIGHT_DETECTOR, Blocks.TARGET, Blocks.HOPPER)
			.addTag(BlockTags.BUTTONS)
			.addTag(BlockTags.PRESSURE_PLATES)
			.addTag(BlockTags.RAILS);

		// VALIDATE

		for (AllBlockTags tag : AllBlockTags.values()) {
			if (tag.alwaysDatagen) {
				prov.getOrCreateRawBuilder(tag.tag);
			}
		}
	}

	private static void genItemTags(RegistrateTagsProvider<Item> provIn) {
		CreateTagsProvider<Item> prov = new CreateTagsProvider<>(provIn, Item::builtInRegistryHolder);

		prov.tag(AllItemTags.SLEEPERS.tag)
			.add(Items.STONE_SLAB, Items.SMOOTH_STONE_SLAB, Items.ANDESITE_SLAB);

		prov.tag(AllItemTags.STRIPPED_LOGS.tag)
			.addTag(AllItemTags.VANILLA_STRIPPED_LOGS.tag)
			.addTag(AllItemTags.MODDED_STRIPPED_LOGS.tag);

		prov.tag(AllItemTags.STRIPPED_WOOD.tag)
			.addTag(AllItemTags.VANILLA_STRIPPED_WOOD.tag)
			.addTag(AllItemTags.MODDED_STRIPPED_WOOD.tag);

		prov.tag(AllItemTags.DEPLOYABLE_DRINK.tag)
			.add(Items.MILK_BUCKET, Items.POTION);

		prov.tag(AllItemTags.UPRIGHT_ON_BELT.tag)
			.add(Items.GLASS_BOTTLE, Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION,
					Items.HONEY_BOTTLE, Items.CAKE);

		prov.tag(AllItemTags.CONTRAPTION_CONTROLLED.tag)
			.add(Items.BELL, Items.CAMPFIRE, Items.SOUL_CAMPFIRE, Items.DISPENSER, Items.DROPPER);

		prov.tag(AllItemTags.VANILLA_STRIPPED_LOGS.tag)
			.add(Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_CRIMSON_STEM,
				Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_MANGROVE_LOG,
				Items.STRIPPED_OAK_LOG, Items.STRIPPED_SPRUCE_LOG, Items.STRIPPED_WARPED_STEM, Items.STRIPPED_CHERRY_LOG, Items.STRIPPED_BAMBOO_BLOCK);

		prov.tag(AllItemTags.VANILLA_STRIPPED_WOOD.tag)
			.add(Items.STRIPPED_ACACIA_WOOD, Items.STRIPPED_BIRCH_WOOD, Items.STRIPPED_CRIMSON_HYPHAE,
				Items.STRIPPED_DARK_OAK_WOOD, Items.STRIPPED_JUNGLE_WOOD, Items.STRIPPED_MANGROVE_WOOD,
				Items.STRIPPED_OAK_WOOD, Items.STRIPPED_SPRUCE_WOOD, Items.STRIPPED_WARPED_HYPHAE, Items.STRIPPED_CHERRY_WOOD);

		prov.tag(ItemTags.BEACON_PAYMENT_ITEMS)
			.addTag(AllItemTags.CREATE_INGOTS.tag);

		prov.tag(Tags.Items.INGOTS)
			.addTag(AllItemTags.CREATE_INGOTS.tag);

		// VALIDATE

		for (AllItemTags tag : AllItemTags.values()) {
			if (tag.alwaysDatagen) {
				prov.getOrCreateRawBuilder(tag.tag);
			}
		}
	}

	private static void genFluidTags(RegistrateTagsProvider<Fluid> provIn) {
		CreateTagsProvider<Fluid> prov = new CreateTagsProvider<>(provIn, Fluid::builtInRegistryHolder);

		prov.tag(AllFluidTags.BOTTOMLESS_ALLOW.tag)
			.add(Fluids.WATER, Fluids.LAVA);

		prov.tag(AllFluidTags.FAN_PROCESSING_CATALYSTS_BLASTING.tag)
			.add(Fluids.LAVA, Fluids.FLOWING_LAVA);

		prov.tag(AllFluidTags.FAN_PROCESSING_CATALYSTS_SPLASHING.tag)
			.add(Fluids.WATER, Fluids.FLOWING_WATER);

		// VALIDATE

		for (AllFluidTags tag : AllFluidTags.values()) {
			if (tag.alwaysDatagen) {
				prov.getOrCreateRawBuilder(tag.tag);
			}
		}
	}

	private static void genEntityTags(RegistrateTagsProvider<EntityType<?>> provIn) {
		CreateTagsProvider<EntityType<?>> prov = new CreateTagsProvider<>(provIn, EntityType::builtInRegistryHolder);

		prov.tag(AllEntityTags.BLAZE_BURNER_CAPTURABLE.tag)
			.add(EntityType.BLAZE);

		// VALIDATE

		for (AllEntityTags tag : AllEntityTags.values()) {
			if (tag.alwaysDatagen) {
				prov.getOrCreateRawBuilder(tag.tag);
			}
		}
	}
}
