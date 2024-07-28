package dev.compactmods.gander_test;

import dev.compactmods.gander_test.core.GanderCommand;
import dev.compactmods.gander_test.core.TestCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
