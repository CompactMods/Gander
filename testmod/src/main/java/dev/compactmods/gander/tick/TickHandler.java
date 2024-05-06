package dev.compactmods.gander.tick;

import dev.compactmods.gander.level.tick.TickingLevels;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class TickHandler
{
	private static long timeOfLastTick = 0;

	private TickHandler() { }

	public static void register(IEventBus bus)
	{
		bus.addListener(ClientTickEvent.Pre.class, TickHandler::onClientTick);
	}

	// TODO: pre or post?
	private static void onClientTick(ClientTickEvent.Pre preClientTick)
	{
		var now = System.nanoTime();
		var delta = timeOfLastTick - now;
		timeOfLastTick = now;

		// 1ms = 1/1_000, 1µs = 1/100_000, 1ns = 1/100_000_000
		TickingLevels.tickAll(delta / 1_000_000_000f);
	}
}
