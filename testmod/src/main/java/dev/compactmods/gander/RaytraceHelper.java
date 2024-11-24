package dev.compactmods.gander;

import java.util.Optional;

import org.joml.Vector3f;

import dev.compactmods.gander.utility.VecHelper;
import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;

public class RaytraceHelper {

	public Optional<BlockHitResult> rayTraceBlock(BlockGetter blockGetter, Camera camera, Vector3f source, Vector3f target) {
		final var cameraLook = new Vector3f(camera.getLookVector());
		cameraLook.normalize().mul(-1);

		var src = reverseTransformVec(source);
		var targ = reverseTransformVec(target);

		BlockHitResult rayTraceBlocks = blockGetter.clip(new ClipContext(
				VecHelper.fromJoml(src),
				VecHelper.fromJoml(targ),
				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, null));

		return Optional.of(rayTraceBlocks);
	}

	private Vector3f reverseTransformVec(Vector3f in) {
		// float pt = AnimationTickHolder.getPartialTicks();

		Vector3f clone = new Vector3f(in).lerp(new Vector3f(), 0);
		in = in.sub(clone);
		return in;
	}

//	public static Vector3f screenToScene(Scene scene, int width, int height, Camera camera, double mouseX, double mouseY, int depth) {
//		var bullshit = new Matrix4f()
////				.translate((float) mouseX, (float) mouseY, depth)
//				.translate(width / 2f, height / 2f, 400)
//				.scale(24)
//				.rotate(camera.rotation())
//				.translate(scene.getBounds().getXSpan() / -2f, -1f, scene.getBounds().getZSpan() / -2f);
//
//		Vector3f vec = new Vector3f((float) mouseX, (float) mouseY, depth);
//
//		vec.mulPosition(bullshit);
//
////		float f = 1f / (24);
////
////		vec = vec.mul(f, -f, f);
////		vec = vec.sub(scene.getBounds().getXSpan() / -2f, -1f, scene.getBounds().getZSpan() / -2f);
//
//		return vec;
//	}

//	public static Pair<ItemStack, BlockPos> rayTraceScene(Scene scene, int width, int height, Camera camera, double mouseX, double mouseY) {
//
//		// TODO
////		Optional<ItemStack> item = RaytraceBuilder.for(scene)
////			.items()
////			.trace(mouseX, mouseY);
//
//		Vector3f vec1 = screenToScene(scene, width, height, camera, mouseX, mouseY, 1000);
//		Vector3f vec2 = screenToScene(scene, width, height, camera, mouseX, mouseY, -100);
//		return rayTraceScene(scene, vec1, vec2);
//	}
//
//	public static Pair<ItemStack, BlockPos> rayTraceScene(Scene scene, Vector3f from, Vector3f to) {
//		MutableObject<Pair<Vec3, BlockHitResult>> nearestHit = new MutableObject<>();
//		MutableDouble bestDistance = new MutableDouble(0);
//
//		final var level = scene.getLevel();
//
//		Pair<Vec3, BlockHitResult> rayTrace = scene.rayTrace(from, to);
//		if (rayTrace == null)
//			return Pair.of(ItemStack.EMPTY, null);
//
//		double distanceTo = rayTrace.getFirst().distanceTo(VecHelper.fromJoml(from));
//
//		if (nearestHit.getValue() != null && distanceTo >= bestDistance.getValue())
//			return Pair.of(ItemStack.EMPTY, null);
//
//		nearestHit.setValue(rayTrace);
//		bestDistance.setValue(distanceTo);
//
//		if (nearestHit.getValue() == null)
//			return Pair.of(ItemStack.EMPTY, null);
//
//		Pair<Vec3, BlockHitResult> selectedHit = nearestHit.getValue();
//		BlockPos selectedPos = selectedHit.getSecond().getBlockPos();
//
//		if (!scene.getBounds().isInside(selectedPos))
//			return Pair.of(ItemStack.EMPTY, null);
//
//		if (scene.getBounds().isInside(selectedPos)) {
//			return Pair.of(ItemStack.EMPTY, selectedPos);
//		}
//
//		BlockState blockState = level.getBlockState(selectedPos);
//
//		Direction direction = selectedHit.getSecond().getDirection();
//		Vec3 location = selectedHit.getSecond().getLocation();
//
//		ItemStack pickBlock = blockState.getCloneItemStack(new BlockHitResult(location, direction, selectedPos, true),
//				level, selectedPos, Minecraft.getInstance().player);
//
//		return Pair.of(pickBlock, selectedPos);
//	}


}
