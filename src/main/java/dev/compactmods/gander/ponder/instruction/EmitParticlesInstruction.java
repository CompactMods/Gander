package dev.compactmods.gander.ponder.instruction;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.PonderLevel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

public class EmitParticlesInstruction extends TickingInstruction {

	private final Vec3 anchor;
	private final Emitter emitter;
	private final float runsPerTick;

	@FunctionalInterface
	public interface Emitter {

		static <T extends ParticleOptions> Emitter simple(T data, Vec3 motion) {
			return (w, x, y, z) -> w.addParticle(data, x, y, z, motion.x, motion.y, motion.z);
		}

		static <T extends ParticleOptions> Emitter withinBlockSpace(T data, Vec3 motion) {
			return (w, x, y, z) -> w.addParticle(data, Math.floor(x) + GanderLib.RANDOM.nextFloat(),
					Math.floor(y) + GanderLib.RANDOM.nextFloat(), Math.floor(z) + GanderLib.RANDOM.nextFloat(), motion.x,
					motion.y, motion.z);
		}

		static ParticleEngine paticleManager() {
			return Minecraft.getInstance().particleEngine;
		}

		void create(PonderLevel world, double x, double y, double z);

	}

	public EmitParticlesInstruction(Vec3 anchor, Emitter emitter, float runsPerTick, int ticks) {
		super(false, ticks);
		this.anchor = anchor;
		this.emitter = emitter;
		this.runsPerTick = runsPerTick;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		int runs = (int) runsPerTick;
		if (GanderLib.RANDOM.nextFloat() < (runsPerTick - runs))
			runs++;
		for (int i = 0; i < runs; i++)
			emitter.create(scene.getWorld(), anchor.x, anchor.y, anchor.z);
	}

}
