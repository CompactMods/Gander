package com.simibubi.create.foundation.data;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegistryObject;

public class CreateRegistrate extends AbstractRegistrate<CreateRegistrate> {
	private static final Map<RegistryEntry<?>, RegistryObject<CreativeModeTab>> TAB_LOOKUP = new IdentityHashMap<>();

	@Nullable
	protected Function<Item, TooltipModifier> currentTooltipModifierFactory;
	@Nullable
	protected RegistryObject<CreativeModeTab> currentTab;

	protected CreateRegistrate(String modid) {
		super(modid);
	}

	public static CreateRegistrate create(String modid) {
		return new CreateRegistrate(modid);
	}

	public CreateRegistrate setTooltipModifierFactory(@Nullable Function<Item, TooltipModifier> factory) {
		currentTooltipModifierFactory = factory;
		return self();
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
		if (type.equals(Registries.ITEM)) {
			if (currentTooltipModifierFactory != null) {
				TooltipModifier.REGISTRY.registerDeferred(entry.getId(), currentTooltipModifierFactory);
			}
		}
		if (currentTab != null) {
			TAB_LOOKUP.put(entry, currentTab);
		}
		return entry;
	}

	@Override
	public <T extends BlockEntity> CreateBlockEntityBuilder<T, CreateRegistrate> blockEntity(String name,
		BlockEntityFactory<T> factory) {
		return blockEntity(self(), name, factory);
	}

	@Override
	public <T extends BlockEntity, P> CreateBlockEntityBuilder<T, P> blockEntity(P parent, String name,
		BlockEntityFactory<T> factory) {
		return (CreateBlockEntityBuilder<T, P>) entry(name,
			(callback) -> CreateBlockEntityBuilder.create(this, parent, name, callback, factory));
	}

	@Override
	public <T extends Entity> CreateEntityBuilder<T, CreateRegistrate> entity(String name,
		EntityType.EntityFactory<T> factory, MobCategory classification) {
		return this.entity(self(), name, factory, classification);
	}

	@Override
	public <T extends Entity, P> CreateEntityBuilder<T, P> entity(P parent, String name,
		EntityType.EntityFactory<T> factory, MobCategory classification) {
		return (CreateEntityBuilder<T, P>) this.entry(name, (callback) -> {
			return CreateEntityBuilder.create(this, parent, name, callback, factory, classification);
		});
	}

	public <T extends ForgeFlowingFluid> FluidBuilder<T, CreateRegistrate> virtualFluid(String name,
		FluidBuilder.FluidTypeFactory typeFactory, NonNullFunction<ForgeFlowingFluid.Properties, T> factory) {
		return entry(name,
			c -> new VirtualFluidBuilder<>(self(), self(), name, c, new ResourceLocation(getModid(), "fluid/" + name + "_still"),
				new ResourceLocation(getModid(), "fluid/" + name + "_flow"), typeFactory, factory));
	}

	public FluidBuilder<ForgeFlowingFluid.Flowing, CreateRegistrate> standardFluid(String name,
		FluidBuilder.FluidTypeFactory typeFactory) {
		return fluid(name, new ResourceLocation(getModid(), "fluid/" + name + "_still"), new ResourceLocation(getModid(), "fluid/" + name + "_flow"),
			typeFactory);
	}

	/* Util */
	public static <T extends Block> NonNullConsumer<? super T> connectedTextures(
		Supplier<ConnectedTextureBehaviour> behavior) {
		return entry -> onClient(() -> () -> registerCTBehviour(entry, behavior));
	}

	protected static void onClient(Supplier<Runnable> toRun) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, toRun);
	}

	@OnlyIn(Dist.CLIENT)
	private static void registerCTBehviour(Block entry, Supplier<ConnectedTextureBehaviour> behaviorSupplier) {
		ConnectedTextureBehaviour behavior = behaviorSupplier.get();
		CreateClient.MODEL_SWAPPER.getCustomBlockModels()
			.register(RegisteredObjects.getKeyOrThrow(entry), model -> new CTModel(model, behavior));
	}
}
