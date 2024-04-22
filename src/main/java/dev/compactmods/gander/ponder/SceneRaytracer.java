package dev.compactmods.gander.ponder;

import dev.compactmods.gander.client.gui.UIRenderHelper;
import dev.compactmods.gander.utility.Pair;
import dev.compactmods.gander.utility.VecHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class SceneRaytracer {

	public static Vector3f screenToScene(Scene scene, int width, int height, Camera camera, double mouseX, double mouseY, int depth) {
		var bullshit = new Matrix4f()
//				.translate((float) mouseX, (float) mouseY, depth)
				.translate(width / 2f, height / 2f, 400)
				.scale(24)
				.rotate(camera.rotation())
				.translate(scene.getBounds().getXSpan() / -2f, -1f, scene.getBounds().getZSpan() / -2f);

		Vector3f vec = new Vector3f((float) mouseX, (float) mouseY, depth);

		vec.mulPosition(bullshit);

//		float f = 1f / (24);
//
//		vec = vec.mul(f, -f, f);
//		vec = vec.sub(scene.getBounds().getXSpan() / -2f, -1f, scene.getBounds().getZSpan() / -2f);

		return vec;
	}

	public static Pair<ItemStack, BlockPos> rayTraceScene(Scene scene, int width, int height, Camera camera, double mouseX, double mouseY) {

		// TODO
//		Optional<ItemStack> item = RaytraceBuilder.for(scene)
//			.items()
//			.trace(mouseX, mouseY);

		Vector3f vec1 = screenToScene(scene, width, height, camera, mouseX, mouseY, 1000);
		Vector3f vec2 = screenToScene(scene, width, height, camera, mouseX, mouseY, -100);
		return rayTraceScene(scene, vec1, vec2);
	}

	public static Pair<ItemStack, BlockPos> rayTraceScene(Scene scene, Vector3f from, Vector3f to) {
		MutableObject<Pair<Vec3, BlockHitResult>> nearestHit = new MutableObject<>();
		MutableDouble bestDistance = new MutableDouble(0);

		final var level = scene.getLevel();

		Pair<Vec3, BlockHitResult> rayTrace = scene.rayTrace(from, to);
		if (rayTrace == null)
			return Pair.of(ItemStack.EMPTY, null);

		double distanceTo = rayTrace.getFirst().distanceTo(VecHelper.fromJoml(from));

		if (nearestHit.getValue() != null && distanceTo >= bestDistance.getValue())
			return Pair.of(ItemStack.EMPTY, null);

		nearestHit.setValue(rayTrace);
		bestDistance.setValue(distanceTo);

		if (nearestHit.getValue() == null)
			return Pair.of(ItemStack.EMPTY, null);

		Pair<Vec3, BlockHitResult> selectedHit = nearestHit.getValue();
		BlockPos selectedPos = selectedHit.getSecond().getBlockPos();

		if (!scene.getBounds().isInside(selectedPos))
			return Pair.of(ItemStack.EMPTY, null);

		if (scene.getBounds().isInside(selectedPos)) {
			return Pair.of(ItemStack.EMPTY, selectedPos);
		}

		BlockState blockState = level.getBlockState(selectedPos);

		Direction direction = selectedHit.getSecond().getDirection();
		Vec3 location = selectedHit.getSecond().getLocation();

		ItemStack pickBlock = blockState.getCloneItemStack(new BlockHitResult(location, direction, selectedPos, true),
				level, selectedPos, Minecraft.getInstance().player);

		return Pair.of(pickBlock, selectedPos);
	}


}
