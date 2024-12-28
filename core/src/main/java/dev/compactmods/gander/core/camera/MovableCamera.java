package dev.compactmods.gander.core.camera;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;

// These following classes exist because we need to re-base the world based on a passed-in origin, rather than the typical (0, 0, 0) origin.
public class MovableCamera extends Camera {
    @Override
    public void setup(
        final BlockGetter pLevel,
        final Entity pEntity,
        final boolean pDetached,
        final boolean pThirdPersonReverse,
        final float pPartialTick) {
        tick();
        super.setup(pLevel, pEntity, pDetached, pThirdPersonReverse, pPartialTick);
    }

    public void moveWorldSpace(double x, double y, double z) {
        super.setPosition(super.getPosition().x + x, super.getPosition().y + y, super.getPosition().z + z);
    }
}
