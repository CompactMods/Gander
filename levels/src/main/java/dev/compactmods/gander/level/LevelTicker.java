package dev.compactmods.gander.level;

/**
 * Defines an interface for a level which can tick.
 */
@FunctionalInterface
public interface LevelTicker
{
	/**
	 * Ticks the level, performing update operations such as:
	 * <ul>
	 *     <li>Updating lighting</li>
	 *     <li>Ticking block entities</li>
	 *     <li>Performing random block updates</li>
	 *     <li>Executing entity logic</li>
	 * </ul>
	 * @param deltaTime
	 */
	void tick(float deltaTime);
}
