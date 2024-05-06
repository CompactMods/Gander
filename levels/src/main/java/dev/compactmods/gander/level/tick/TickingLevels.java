package dev.compactmods.gander.level.tick;

import dev.compactmods.gander.level.LevelTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines a class which contains a registry of ticking levels.
 */
public final class TickingLevels
{
	private static final ConcurrentHashMap<Level, LevelTicker> REGISTERED_LEVEL_TICKERS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<LevelTicker, Unit> REGISTERED_TICKERS = new ConcurrentHashMap<>();

	private TickingLevels() { }

	/**
	 * Registers a level for receiving tick updates.
	 *
	 * @param ticker The ticker to register.
	 */
	public static void registerTicker(LevelTicker ticker) {
		// TODO: throwing exceptions: fine or?
		if (REGISTERED_TICKERS.putIfAbsent(ticker, Unit.INSTANCE) != null) {
			throw new IllegalStateException("Ticker was already previously registered");
		}
	}

	/**
	 * Registers a client level for receiving tick updates.
	 * <p>
	 * Passing in the level from {@link Minecraft#level} here produces
	 * undefined behavior.
	 *
	 * @param clientLevel The client level to tick.
	 */
	public static void registerTickingLevel(ClientLevel clientLevel) {
		var ticker = REGISTERED_LEVEL_TICKERS.put(clientLevel, dt -> clientLevel.tick(() -> true));
		registerTicker(ticker);
	}

	/**
	 * Registers a server level for receiving tick updates.
	 * <p>
	 * Passing in the level from {@link MinecraftServer#getLevel(ResourceKey)}
	 * here produces undefined behavior.
	 *
	 * @param serverLevel The client level to tick.
	 */
	public static void registerTickingLevel(ServerLevel serverLevel) {
		// TODO: find a decent value for () -> true
		var ticker = REGISTERED_LEVEL_TICKERS.put(serverLevel, dt -> serverLevel.tick(() -> true));
		registerTicker(ticker);
	}

	/**
	 * Unregisters a level ticker from receiving tick updates.
	 *
	 * @param ticker The ticker to unregister.
	 */
	public static void unregisterTicker(LevelTicker ticker) {
		var hadTicker = REGISTERED_TICKERS.remove(ticker);
		if (hadTicker == null) {
			throw new IllegalStateException("That ticker was not registered");
		}
	}

	/**
	 * Unregisters a level from receiving tick updates.
	 *
	 * @param level The level to unregister.
	 */
	public static void unregisterTickingLevel(Level level) {
		var ticker = REGISTERED_LEVEL_TICKERS.remove(level);
		if (ticker == null) {
			throw new IllegalStateException("Level was not registered for ticking");
		}

		unregisterTicker(ticker);
	}

	/**
	 * Ticks all of the registered tickers.
	 * <p>
	 * Calling this multiple times per tick produces undefined behavior.
	 *
	 * @param deltaTime The time since the last invocation of this method.
	 */
	public static void tickAll(float deltaTime) {
		// TODO: maybe this should track invocation times per level?
		REGISTERED_TICKERS.forEach((ticker, nothing) -> ticker.tick(deltaTime));
	}
}
