package dev.compactmods.gander;

import dev.compactmods.gander.core.GanderCommand;
import dev.compactmods.gander.core.TestCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class CommonEvents {

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		var dispatcher = event.getDispatcher();
		var root = GanderCommand.make();
		dispatcher.register(root);
		dispatcher.register(TestCommand.build(event.getBuildContext()));
	}
}
