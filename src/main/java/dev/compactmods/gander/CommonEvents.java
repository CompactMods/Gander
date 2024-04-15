package dev.compactmods.gander;

import dev.compactmods.gander.ponder.core.PonderCommand;
import dev.compactmods.gander.utility.WorldAttached;
import net.minecraft.commands.Commands;
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
		var createRoot = Commands.literal("create")
				.requires(cs -> cs.hasPermission(0));

		var ponderRoot = PonderCommand.make();

		dispatcher.register(createRoot);
		dispatcher.register(ponderRoot);
	}

	@SubscribeEvent
	public static void onUnloadWorld(LevelEvent.Unload event) {
		LevelAccessor world = event.getLevel();
		WorldAttached.invalidateWorld(world);
	}
}
