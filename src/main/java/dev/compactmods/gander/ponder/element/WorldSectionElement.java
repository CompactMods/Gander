package dev.compactmods.gander.ponder.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.compactmods.gander.outliner.AABBOutline;
import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.PonderLevel;
import dev.compactmods.gander.ponder.Selection;
import dev.compactmods.gander.render.BlockEntityRenderHelper;
import dev.compactmods.gander.utility.AnimationTickHolder;
import dev.compactmods.gander.utility.Pair;
import dev.compactmods.gander.utility.VecHelper;

import dev.compactmods.gander.utility.math.PoseTransformStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.data.ModelData;

public class WorldSectionElement extends AnimatedSceneElement {

	List<BlockEntity> renderedBlockEntities;
	List<Pair<BlockEntity, Consumer<Level>>> tickableBlockEntities;
	Selection section;
	boolean redraw;

	Vec3 prevAnimatedOffset = Vec3.ZERO;
	Vec3 animatedOffset = Vec3.ZERO;
	Vec3 prevAnimatedRotation = Vec3.ZERO;
	Vec3 animatedRotation = Vec3.ZERO;
	Vec3 centerOfRotation = Vec3.ZERO;
	Vec3 stabilizationAnchor = null;

	BlockPos selectedBlock;

	public WorldSectionElement(Selection section) {
		this.section = section.copy();
		centerOfRotation = section.getCenter();
	}

	public void set(Selection selection) {
		applyNewSelection(selection.copy());
	}

	public void add(Selection toAdd) {
		applyNewSelection(this.section.add(toAdd));
	}

	public void erase(Selection toErase) {
		applyNewSelection(this.section.substract(toErase));
	}

	private void applyNewSelection(Selection selection) {
		this.section = selection;
		queueRedraw();
	}

	public void setCenterOfRotation(Vec3 center) {
		centerOfRotation = center;
	}

	public void stabilizeRotation(Vec3 anchor) {
		stabilizationAnchor = anchor;
	}

	@Override
	public void reset(PonderScene scene) {
		super.reset(scene);
		resetAnimatedTransform();
		resetSelectedBlock();
	}

	public void selectBlock(BlockPos pos) {
		selectedBlock = pos;
	}

	public void resetSelectedBlock() {
		selectedBlock = null;
	}

	public void resetAnimatedTransform() {
		prevAnimatedOffset = Vec3.ZERO;
		animatedOffset = Vec3.ZERO;
		prevAnimatedRotation = Vec3.ZERO;
		animatedRotation = Vec3.ZERO;
	}

	public void queueRedraw() {
		redraw = true;
	}

	public void setAnimatedRotation(Vec3 eulerAngles, boolean force) {
		this.animatedRotation = eulerAngles;
		if (force)
			prevAnimatedRotation = animatedRotation;
	}

	public Vec3 getAnimatedRotation() {
		return animatedRotation;
	}

	public void setAnimatedOffset(Vec3 offset, boolean force) {
		this.animatedOffset = offset;
		if (force)
			prevAnimatedOffset = animatedOffset;
	}

	public Vec3 getAnimatedOffset() {
		return animatedOffset;
	}

	public Pair<Vec3, BlockHitResult> rayTrace(PonderLevel world, Vec3 source, Vec3 target) {
		world.setMask(this.section);
		Vec3 transformedTarget = reverseTransformVec(target);
		BlockHitResult rayTraceBlocks = world.clip(new ClipContext(reverseTransformVec(source), transformedTarget,
				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));
		world.clearMask();

		double t = rayTraceBlocks.getLocation()
				.subtract(transformedTarget)
				.lengthSqr()
				/ source.subtract(target)
				.lengthSqr();
		Vec3 actualHit = VecHelper.lerp((float) t, target, source);
		return Pair.of(actualHit, rayTraceBlocks);
	}

	private Vec3 reverseTransformVec(Vec3 in) {
		float pt = AnimationTickHolder.getPartialTicks();
		in = in.subtract(VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset));
		if (!animatedRotation.equals(Vec3.ZERO) || !prevAnimatedRotation.equals(Vec3.ZERO)) {
			if (centerOfRotation == null)
				centerOfRotation = section.getCenter();
			double rotX = Mth.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
			double rotZ = Mth.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
			double rotY = Mth.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);
			in = in.subtract(centerOfRotation);
			in = VecHelper.rotate(in, -rotX, Axis.X);
			in = VecHelper.rotate(in, -rotZ, Axis.Z);
			in = VecHelper.rotate(in, -rotY, Axis.Y);
			in = in.add(centerOfRotation);
			if (stabilizationAnchor != null) {
				in = in.subtract(stabilizationAnchor);
				in = VecHelper.rotate(in, rotX, Axis.X);
				in = VecHelper.rotate(in, rotZ, Axis.Z);
				in = VecHelper.rotate(in, rotY, Axis.Y);
				in = in.add(stabilizationAnchor);
			}
		}
		return in;
	}

	public void transformMS(PoseStack ms, float pt) {
		PoseTransformStack.of(ms)
				.translate(VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset));
		if (!animatedRotation.equals(Vec3.ZERO) || !prevAnimatedRotation.equals(Vec3.ZERO)) {
			if (centerOfRotation == null)
				centerOfRotation = section.getCenter();
			double rotX = Mth.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
			double rotZ = Mth.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
			double rotY = Mth.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);
			PoseTransformStack.of(ms)
					.translate(centerOfRotation)
					.rotateX((float) rotX)
					.rotateZ((float) rotZ)
					.rotateY((float) rotY)
					.translateBack(centerOfRotation);
			if (stabilizationAnchor != null) {
				PoseTransformStack.of(ms)
						.translate(stabilizationAnchor)
						.rotateX((float) -rotX)
						.rotateZ((float) -rotZ)
						.rotateY((float) -rotY)
						.translateBack(stabilizationAnchor);
			}
		}
	}

	public void tick(PonderScene scene) {
		prevAnimatedOffset = animatedOffset;
		prevAnimatedRotation = animatedRotation;

		loadBEsIfMissing(scene.getWorld());
		renderedBlockEntities.removeIf(be -> scene.getWorld()
				.getBlockEntity(be.getBlockPos()) != be);
		tickableBlockEntities.removeIf(be -> scene.getWorld()
				.getBlockEntity(be.getFirst()
						.getBlockPos()) != be.getFirst());
		tickableBlockEntities.forEach(be -> be.getSecond()
				.accept(scene.getWorld()));
	}

	protected void loadBEsIfMissing(PonderLevel world) {
		if (renderedBlockEntities != null)
			return;
		tickableBlockEntities = new ArrayList<>();
		renderedBlockEntities = new ArrayList<>();
		section.forEach(pos -> {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			BlockState blockState = world.getBlockState(pos);
			Block block = blockState.getBlock();
			if (blockEntity == null)
				return;
			if (!(block instanceof EntityBlock))
				return;
			blockEntity.setBlockState(world.getBlockState(pos));
			BlockEntityTicker<?> ticker = ((EntityBlock) block).getTicker(world, blockState, blockEntity.getType());
			if (ticker != null)
				addTicker(blockEntity, ticker);
			renderedBlockEntities.add(blockEntity);
		});
	}

	@SuppressWarnings("unchecked")
	private <T extends BlockEntity> void addTicker(T blockEntity, BlockEntityTicker<?> ticker) {
		tickableBlockEntities.add(Pair.of(blockEntity, w -> ((BlockEntityTicker<T>) ticker).tick(w,
				blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity)));
	}

	@Override
	public void renderFirst(PonderLevel world, MultiBufferSource.BufferSource buffer, PoseStack ms, float fade, float pt) {
		int light = -1;
		if (fade != 1)
			light = (int) (Mth.lerp(fade, 5, 14));
		if (redraw) {
			renderedBlockEntities = null;
			tickableBlockEntities = null;
		}

		ms.pushPose();
		transformMS(ms, pt);
		world.pushFakeLight(light);
		renderBlockEntities(world, ms, buffer, pt);
		world.popLight();

		Map<BlockPos, Integer> blockBreakingProgressions = world.getBlockBreakingProgressions();
		PoseStack overlayMS = null;

		for (Entry<BlockPos, Integer> entry : blockBreakingProgressions.entrySet()) {
			BlockPos pos = entry.getKey();
			if (!section.test(pos))
				continue;

			if (overlayMS == null) {
				overlayMS = new PoseStack();
				overlayMS.last().pose().set(ms.last().pose());
				overlayMS.last().normal().set(ms.last().normal());
			}

			VertexConsumer builder = new SheetedDecalTextureGenerator(
					buffer.getBuffer(ModelBakery.DESTROY_TYPES.get(entry.getValue())),
					overlayMS.last().pose(),
					overlayMS.last().normal(),
					1);

			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());
			Minecraft.getInstance()
					.getBlockRenderer()
					.renderBreakingTexture(world.getBlockState(pos), pos, world, ms, builder, ModelData.EMPTY);

			ms.popPose();
		}

		ms.popPose();
	}

	@Override
	protected void renderLayer(PonderLevel world, MultiBufferSource.BufferSource buffer, RenderType type, PoseStack pose, float fade,
							   float pt) {
		var b = buffer.getBuffer(type);
		pose.pushPose();
		{
			transformMS(pose, pt);
			BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
			ModelBlockRenderer renderer = dispatcher.getModelRenderer();

			RandomSource random = RandomSource.createNewThreadLocalInstance();

			world.setMask(this.section);
			ModelBlockRenderer.enableCaching();
			section.forEach(pos -> {
				BlockState state = world.getBlockState(pos);
				FluidState fluidState = world.getFluidState(pos);

				pose.pushPose();
				pose.translate(pos.getX(), pos.getY(), pos.getZ());

				if (state.getRenderShape() == RenderShape.MODEL) {
					BakedModel model = dispatcher.getBlockModel(state);
					BlockEntity blockEntity = world.getBlockEntity(pos);

					long seed = state.getSeed(pos);
					random.setSeed(seed);

					ModelData modelData = blockEntity != null ? blockEntity.getModelData() : ModelData.EMPTY;
					modelData = model.getModelData(world, pos, state, modelData);
					if (model.getRenderTypes(state, random, modelData).contains(type)) {
						renderer.tesselateBlock(world, model, state, pos, pose, b, true,
								random, seed, OverlayTexture.NO_OVERLAY, modelData, type);
					}
				}

				if (!fluidState.isEmpty() && ItemBlockRenderTypes.getRenderLayer(fluidState) == type)
					dispatcher.renderLiquid(pos, world, b, state, fluidState);

				pose.popPose();
			});
			ModelBlockRenderer.clearCache();
			world.clearMask();
			buffer.endBatch();
		}
		pose.popPose();
	}

	@Override
	protected void renderLast(PonderLevel world, MultiBufferSource.BufferSource buffer, PoseStack ms, float fade, float pt) {
		redraw = false;
		if (selectedBlock == null)
			return;
		BlockState blockState = world.getBlockState(selectedBlock);
		if (blockState.isAir())
			return;
		VoxelShape shape =
				blockState.getShape(world, selectedBlock, CollisionContext.of(Minecraft.getInstance().player));
		if (shape.isEmpty())
			return;

		ms.pushPose();
		transformMS(ms, pt);
		ms.translate(selectedBlock.getX(), selectedBlock.getY(), selectedBlock.getZ());

		AABBOutline aabbOutline = new AABBOutline(shape.bounds());
		aabbOutline.getParams()
				.lineWidth(1 / 64f)
				.colored(0xefefef)
				.disableLineNormals();
		aabbOutline.render(ms, buffer, Vec3.ZERO, pt);

		ms.popPose();
	}

	private void renderBlockEntities(PonderLevel world, PoseStack ms, MultiBufferSource buffer, float pt) {
		loadBEsIfMissing(world);
		BlockEntityRenderHelper.renderBlockEntities(world, renderedBlockEntities, ms, buffer, pt);
	}
}
