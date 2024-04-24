package dev.compactmods.gander;

import dev.compactmods.gander.core.GanderCommand;
import dev.compactmods.gander.utility.WorldAttached;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod.EventBusSubscriber
public class CommonEvents {

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		var dispatcher = event.getDispatcher();
		var root = GanderCommand.make();
		dispatcher.register(root);
	}

	@SubscribeEvent
	public static void onUnloadWorld(LevelEvent.Unload event) {
		LevelAccessor world = event.getLevel();
		WorldAttached.invalidateWorld(world);
	}
}
