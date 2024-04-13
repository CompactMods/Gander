package com.simibubi.create;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;

public class CreateRegistrate extends AbstractRegistrate<CreateRegistrate> {
	private static final Map<RegistryEntry<?>, RegistryObject<CreativeModeTab>> TAB_LOOKUP = new IdentityHashMap<>();

	@Nullable
	protected RegistryObject<CreativeModeTab> currentTab;

	protected CreateRegistrate(String modid) {
		super(modid);
	}

	public static CreateRegistrate create(String modid) {
		return new CreateRegistrate(modid);
	}

	@Override
	public CreateRegistrate registerEventListeners(IEventBus bus) {
		return super.registerEventListeners(bus);
	}

	@Override
	protected <R, T extends R> RegistryEntry<T> accept(String name, ResourceKey<? extends Registry<R>> type,
		Builder<R, T, ?, ?> builder, NonNullSupplier<? extends T> creator,
		NonNullFunction<RegistryObject<T>, ? extends RegistryEntry<T>> entryFactory) {
		RegistryEntry<T> entry = super.accept(name, type, builder, creator, entryFactory);

		if (currentTab != null) {
			TAB_LOOKUP.put(entry, currentTab);
		}
		return entry;
	}

	public FluidBuilder<ForgeFlowingFluid.Flowing, CreateRegistrate> standardFluid(String name,
		FluidBuilder.FluidTypeFactory typeFactory) {
		return fluid(name, new ResourceLocation(getModid(), "fluid/" + name + "_still"), new ResourceLocation(getModid(), "fluid/" + name + "_flow"),
			typeFactory);
	}
}
