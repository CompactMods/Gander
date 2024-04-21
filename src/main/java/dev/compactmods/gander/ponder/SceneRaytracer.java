package dev.compactmods.gander.ponder;

import dev.compactmods.gander.ponder.element.WorldSectionElement;
import dev.compactmods.gander.utility.Pair;
import dev.compactmods.gander.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;

public class SceneRaytracer {

	public static Vec3 screenToScene(PonderScene ponderScene, double x, double y, int depth, float pt) {
		final var transform = ponderScene.transform;
		// transform.refreshMatrix(pt);

		Vec3 vec = new Vec3(x, y, depth);

		vec = vec.subtract(transform.width / 2f, transform.height / 2f, 200 + transform.offset);
		vec = VecHelper.rotate(vec, 35, Direction.Axis.X);
		vec = VecHelper.rotate(vec, -55, Direction.Axis.Y);
		vec = vec.subtract(transform.offset, 0, 0);
		vec = VecHelper.rotate(vec, 55, Direction.Axis.Y);
		vec = VecHelper.rotate(vec, -35, Direction.Axis.X);
		vec = VecHelper.rotate(vec, -transform.xRotation.getValue(pt), Direction.Axis.X);
		vec = VecHelper.rotate(vec, -transform.yRotation.getValue(pt), Direction.Axis.Y);

		float f = 1f / (24);

		vec = vec.multiply(f, -f, f);
		vec = vec.subtract(ponderScene.getBounds().getXSpan() / -2f, -1f, ponderScene.getBounds().getZSpan() / -2f);

		return vec;
	}

	public static Pair<ItemStack, BlockPos> rayTraceScene(PonderScene ponderScene, double mouseX, double mouseY) {

		// TODO
//		Optional<ItemStack> item = RaytraceBuilder.for(scene)
//			.items()
//			.trace(mouseX, mouseY);

		Vec3 vec1 = screenToScene(ponderScene, mouseX, mouseY, 1000, 0);
		Vec3 vec2 = screenToScene(ponderScene, mouseX, mouseY, -100, 0);
		return rayTraceScene(ponderScene, vec1, vec2);
	}

	public static Pair<ItemStack, BlockPos> rayTraceScene(PonderScene ponderScene, Vec3 from, Vec3 to) {
		MutableObject<Pair<WorldSectionElement, Pair<Vec3, BlockHitResult>>> nearestHit = new MutableObject<>();
		MutableDouble bestDistance = new MutableDouble(0);

		final var level = ponderScene.getWorld();

		ponderScene.forEach(WorldSectionElement.class, wse -> {
			wse.resetSelectedBlock();
			Pair<Vec3, BlockHitResult> rayTrace = wse.rayTrace(level, from, to);
			if (rayTrace == null)
				return;
			double distanceTo = rayTrace.getFirst()
					.distanceTo(from);
			if (nearestHit.getValue() != null && distanceTo >= bestDistance.getValue())
				return;

			nearestHit.setValue(Pair.of(wse, rayTrace));
			bestDistance.setValue(distanceTo);
		});

		if (nearestHit.getValue() == null)
			return Pair.of(ItemStack.EMPTY, null);

		Pair<Vec3, BlockHitResult> selectedHit = nearestHit.getValue()
				.getSecond();
		BlockPos selectedPos = selectedHit.getSecond()
				.getBlockPos();

		if (!ponderScene.getBounds().isInside(selectedPos))
			return Pair.of(ItemStack.EMPTY, null);

		if (ponderScene.getBounds().isInside(selectedPos)) {
			return Pair.of(ItemStack.EMPTY, selectedPos);
		}

		nearestHit.getValue()
				.getFirst()
				.selectBlock(selectedPos);

		BlockState blockState = level.getBlockState(selectedPos);

		Direction direction = selectedHit.getSecond().getDirection();
		Vec3 location = selectedHit.getSecond().getLocation();

		ItemStack pickBlock = blockState.getCloneItemStack(new BlockHitResult(location, direction, selectedPos, true),
				level, selectedPos, Minecraft.getInstance().player);

		return Pair.of(pickBlock, selectedPos);
	}
}
