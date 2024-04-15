package com.simibubi.create;

import com.simibubi.create.infrastructure.command.ClearBufferCacheCommand;
import com.simibubi.create.infrastructure.command.HighlightCommand;
import com.simibubi.create.ponder.core.PonderCommand;
import com.simibubi.create.utility.Components;
import com.simibubi.create.utility.ServerSpeedProvider;
import com.simibubi.create.utility.WorldAttached;

import net.minecraft.commands.Commands;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.locating.IModFile;

@EventBusSubscriber
public class CommonEvents {

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent event) {
		if (event.phase == Phase.START)
			return;
		ServerSpeedProvider.serverTick();
	}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		var dispatcher = event.getDispatcher();
		var createRoot = Commands.literal("create")
				.requires(cs -> cs.hasPermission(0))
				.then(HighlightCommand.register())
				.then(ClearBufferCacheCommand.register());

		var ponderRoot = PonderCommand.make();

		dispatcher.register(createRoot);
		dispatcher.register(ponderRoot);
	}

	@SubscribeEvent
	public static void onUnloadWorld(LevelEvent.Unload event) {
		LevelAccessor world = event.getLevel();
		WorldAttached.invalidateWorld(world);
	}

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class ModBusEvents {

		@SubscribeEvent
		public static void addPackFinders(AddPackFindersEvent event) {
			if (event.getPackType() == PackType.CLIENT_RESOURCES) {
				IModFileInfo modFileInfo = ModList.get().getModFileById(Create.ID);
				if (modFileInfo == null) {
					Create.LOGGER.error("Could not find Create mod file info; built-in resource packs will be missing!");
					return;
				}
				IModFile modFile = modFileInfo.getFile();
				event.addRepositorySource(consumer -> {
					Pack pack = Pack.readMetaAndCreate(Create.asResource("legacy_copper").toString(), Components.literal("Create Legacy Copper"), false, id -> new ModFilePackResources(id, modFile, "resourcepacks/legacy_copper"), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
					if (pack != null) {
						consumer.accept(pack);
					}
				});
			}
		}
	}
}
