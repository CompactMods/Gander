package dev.compactmods.gander.ponder;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import dev.compactmods.gander.utility.math.PoseTransformStack;

import net.minecraft.util.ArrayListDeque;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.compactmods.gander.gui.UIRenderHelper;
import dev.compactmods.gander.outliner.Outliner;
import dev.compactmods.gander.ponder.element.PonderElement;
import dev.compactmods.gander.ponder.element.PonderSceneElement;
import dev.compactmods.gander.ponder.element.WorldSectionElement;
import dev.compactmods.gander.ponder.instruction.contract.PonderInstruction;
import dev.compactmods.gander.render.DiffuseLightCalculator;
import dev.compactmods.gander.render.ForcedDiffuseState;
import dev.compactmods.gander.render.SuperRenderTypeBuffer;
import dev.compactmods.gander.utility.AnimationTickHolder;
import dev.compactmods.gander.utility.Pair;
import dev.compactmods.gander.utility.VecHelper;
import dev.compactmods.gander.utility.animation.LerpedFloat;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class PonderScene {

	private boolean finished;
	ResourceLocation sceneId;

	private final IntList keyframeTimes;

	List<PonderInstruction> schedule;
	private final Deque<PonderInstruction> activeSchedule;
	private final Map<UUID, PonderElement> linkedElements;
	private final Set<PonderElement> elements;

	private final PonderLevel world;
	private final ResourceLocation component;
	private SceneTransform transform;
	private final SceneCamera camera;
	private final Outliner outliner;

	private Vec3 pointOfInterest;
	private Vec3 chasingPointOfInterest;
	private final WorldSectionElement baseWorldSection;
	@Nullable
	private final Entity renderViewEntity;

	int basePlateOffsetX;
	int basePlateOffsetZ;
	int basePlateSize;
	float scaleFactor;
	float yOffset;

	private boolean stoppedCounting;
	private int totalTime;
	private int currentTime;

	public PonderScene(PonderLevel world, ResourceLocation component) {
		this.world = world;
		this.component = component;

		outliner = new Outliner();
		elements = new HashSet<>();
		linkedElements = new HashMap<>();
		schedule = new ArrayList<>();
		activeSchedule = new ArrayListDeque<>();
		transform = new SceneTransform();
		basePlateSize = getBounds().getXSpan();
		camera = new SceneCamera();
		baseWorldSection = new WorldSectionElement(Selection.of(world.getBounds()));
		renderViewEntity = new ArmorStand(world, 0, 0, 0);
		keyframeTimes = new IntArrayList(4);
		scaleFactor = 1;
		yOffset = 0;

		this.pointOfInterest = world.getBounds().getCenter().getCenter();
	}

	public SceneCamera camera() {
		return this.camera;
	}

	public void deselect() {
		forEach(WorldSectionElement.class, WorldSectionElement::resetSelectedBlock);
	}

	public Pair<ItemStack, BlockPos> rayTraceScene(double mouseX, double mouseY) {
		Vec3 vec1 = transform.screenToScene(mouseX, mouseY, 1000, 0);
		Vec3 vec2 = transform.screenToScene(mouseX, mouseY, -100, 0);
		return rayTraceScene(vec1, vec2);
	}

	public Pair<ItemStack, BlockPos> rayTraceScene(Vec3 from, Vec3 to) {
		MutableObject<Pair<WorldSectionElement, Pair<Vec3, BlockHitResult>>> nearestHit = new MutableObject<>();
		MutableDouble bestDistance = new MutableDouble(0);

		forEach(WorldSectionElement.class, wse -> {
			wse.resetSelectedBlock();
			Pair<Vec3, BlockHitResult> rayTrace = wse.rayTrace(world, from, to);
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

		BlockPos origin = new BlockPos(basePlateOffsetX, 0, basePlateOffsetZ);
		if (!world.getBounds()
			.isInside(selectedPos))
			return Pair.of(ItemStack.EMPTY, null);
		if (BoundingBox.fromCorners(origin, origin.offset(new Vec3i(basePlateSize - 1, 0, basePlateSize - 1)))
			.isInside(selectedPos)) {
			return Pair.of(ItemStack.EMPTY, selectedPos);
		}

		nearestHit.getValue()
			.getFirst()
			.selectBlock(selectedPos);
		BlockState blockState = world.getBlockState(selectedPos);

		Direction direction = selectedHit.getSecond()
			.getDirection();
		Vec3 location = selectedHit.getSecond()
			.getLocation();

		ItemStack pickBlock = blockState.getCloneItemStack(new BlockHitResult(location, direction, selectedPos, true),
			world, selectedPos, Minecraft.getInstance().player);

		return Pair.of(pickBlock, selectedPos);
	}

	public void reset() {
		currentTime = 0;
		activeSchedule.clear();
		schedule.forEach(mdi -> mdi.reset(this));
	}

	public void begin() {
		reset();
		forEach(pe -> pe.reset(this));

		world.restore();
		elements.clear();
		linkedElements.clear();
		keyframeTimes.clear();

		transform = new SceneTransform();
		finished = false;
		setPointOfInterest(new Vec3(0, 4, 0));

		baseWorldSection.forceApplyFade(1);
		elements.add(baseWorldSection);

		totalTime = 0;
		stoppedCounting = false;
		activeSchedule.addAll(schedule);
		activeSchedule.forEach(i -> i.onScheduled(this));
	}

	public WorldSectionElement getBaseWorldSection() {
		return baseWorldSection;
	}

	public void renderScene(SuperRenderTypeBuffer buffer, PoseStack ms, float pt) {
		ForcedDiffuseState.pushCalculator(DiffuseLightCalculator.DEFAULT);
		ms.pushPose();

		Minecraft mc = Minecraft.getInstance();
		Entity prevRVE = mc.cameraEntity;

		mc.cameraEntity = this.renderViewEntity;
		forEach(PonderSceneElement.class, e -> e.renderFirst(world, buffer, ms, pt));
		mc.cameraEntity = prevRVE;

		for (RenderType type : RenderType.chunkBufferLayers())
			forEach(PonderSceneElement.class, e -> e.renderLayer(world, buffer, type, ms, pt));

		forEach(PonderSceneElement.class, e -> e.renderLast(world, buffer, ms, pt));
		camera.set(transform.xRotation.getValue(pt) + 90, transform.yRotation.getValue(pt) + 180);
		world.renderEntities(ms, buffer, camera, pt);
		world.renderParticles(ms, buffer, camera, pt);
		outliner.renderOutlines(ms, buffer, Vec3.ZERO, pt);

		ms.popPose();
		ForcedDiffuseState.popCalculator();
	}

	public void setPointOfInterest(Vec3 poi) {
		if (chasingPointOfInterest == null)
			pointOfInterest = poi;
		chasingPointOfInterest = poi;
	}

	public Vec3 getPointOfInterest() {
		return pointOfInterest;
	}

	public void tick() {
		if (chasingPointOfInterest != null)
			pointOfInterest = VecHelper.lerp(.25f, pointOfInterest, chasingPointOfInterest);

		outliner.tickOutlines();
		world.tick();
		world.animateTick();
		transform.tick();
		forEach(e -> e.tick(this));

		if (currentTime < totalTime)
			currentTime++;

		if(activeSchedule.isEmpty()) {
			finished = true;
			return;
		}

		var current = activeSchedule.peek();
		current.tick(this);
		if(current.isComplete())
			activeSchedule.pop();
	}

	public void addToSceneTime(int time) {
		if (!stoppedCounting)
			totalTime += time;
	}

	public void stopCounting() {
		stoppedCounting = true;
	}

	public void markKeyframe(int offset) {
		if (!stoppedCounting)
			keyframeTimes.add(totalTime + offset);
	}

	public void addElement(PonderElement e) {
		elements.add(e);
	}

	public <E extends PonderElement> void linkElement(E e, ElementLink<E> link) {
		linkedElements.put(link.getId(), e);
	}

	public <E extends PonderElement> E resolve(ElementLink<E> link) {
		return link.cast(linkedElements.get(link.getId()));
	}

	public void forEach(Consumer<? super PonderElement> function) {
		for (PonderElement elemtent : elements)
			function.accept(elemtent);
	}

	public <T extends PonderElement> void forEach(Class<T> type, Consumer<T> function) {
		for (PonderElement element : elements)
			if (type.isInstance(element))
				function.accept(type.cast(element));
	}

	public <T extends Entity> void forEachWorldEntity(Class<T> type, Consumer<T> function) {
		world.getEntityStream()
			.filter(type::isInstance)
			.map(type::cast)
			.forEach(function);
	}

	public SceneBuilder builder() {
		return new SceneBuilder(this);
	}

	public SceneBuildingUtil getSceneBuildingUtil() {
		return new SceneBuildingUtil(getBounds());
	}

	public String getString(String key) {
		return PonderLocalization.getSpecific(sceneId, key);
	}

	public PonderLevel getWorld() {
		return world;
	}

	public ResourceLocation getComponent() {
		return component;
	}

	public Set<PonderElement> getElements() {
		return elements;
	}

	public BoundingBox getBounds() {
		return world == null ? new BoundingBox(BlockPos.ZERO) : world.getBounds();
	}

	public ResourceLocation getId() {
		return sceneId;
	}

	public SceneTransform getTransform() {
		return transform;
	}

	public Outliner getOutliner() {
		return outliner;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public int getBasePlateOffsetX() {
		return basePlateOffsetX;
	}

	public int getBasePlateOffsetZ() {
		return basePlateOffsetZ;
	}

	public int getBasePlateSize() {
		return basePlateSize;
	}

	public class SceneTransform {

		public LerpedFloat xRotation, yRotation;

		// Screen params
		private int width, height;
		private double offset;
		private Matrix4f cachedMat;

		public SceneTransform() {
			xRotation = LerpedFloat.angular()
				.disableSmartAngleChasing()
				.startWithValue(-35);
			yRotation = LerpedFloat.angular()
				.disableSmartAngleChasing()
				.startWithValue(55 + 90);
		}

		public void tick() {
			xRotation.tickChaser();
			yRotation.tickChaser();
		}

		public void updateScreenParams(int width, int height, double offset) {
			this.width = width;
			this.height = height;
			this.offset = offset;
			cachedMat = null;
		}

		public PoseStack apply(PoseStack ms) {
			return apply(ms, AnimationTickHolder.getPartialTicks(world));
		}

		public PoseStack apply(PoseStack ms, float pt) {
			ms.translate(width / 2, height / 2, 200 + offset);

			PoseTransformStack.of(ms)
				.rotateXDegrees(-35)
				.rotateYDegrees(55)
				.translate(offset, 0, 0)
				.rotateYDegrees(-55)
				.rotateXDegrees(35)
				.rotateXDegrees(xRotation.getValue(pt))
				.rotateYDegrees(yRotation.getValue(pt));

			UIRenderHelper.flipForGuiRender(ms);
			float f = 30 * scaleFactor;
			ms.scale(f, f, f);
			ms.translate((basePlateSize) / -2f - basePlateOffsetX, -1f + yOffset,
				(basePlateSize) / -2f - basePlateOffsetZ);

			return ms;
		}

		public void updateSceneRVE(float pt) {
			Vec3 v = screenToScene(width / 2, height / 2, 500, pt);
			if (renderViewEntity != null)
				renderViewEntity.setPos(v.x, v.y, v.z);
		}

		public Vec3 screenToScene(double x, double y, int depth, float pt) {
			refreshMatrix(pt);
			Vec3 vec = new Vec3(x, y, depth);

			vec = vec.subtract(width / 2, height / 2, 200 + offset);
			vec = VecHelper.rotate(vec, 35, Axis.X);
			vec = VecHelper.rotate(vec, -55, Axis.Y);
			vec = vec.subtract(offset, 0, 0);
			vec = VecHelper.rotate(vec, 55, Axis.Y);
			vec = VecHelper.rotate(vec, -35, Axis.X);
			vec = VecHelper.rotate(vec, -xRotation.getValue(pt), Axis.X);
			vec = VecHelper.rotate(vec, -yRotation.getValue(pt), Axis.Y);

			float f = 1f / (30 * scaleFactor);

			vec = vec.multiply(f, -f, f);
			vec = vec.subtract((basePlateSize) / -2f - basePlateOffsetX, -1f + yOffset,
				(basePlateSize) / -2f - basePlateOffsetZ);

			return vec;
		}

		public Vec2 sceneToScreen(Vec3 vec, float pt) {
			refreshMatrix(pt);
			Vector4f vec4 = new Vector4f((float) vec.x, (float) vec.y, (float) vec.z, 1);
			vec4.mul(cachedMat);
			return new Vec2(vec4.x(), vec4.y());
		}

		protected void refreshMatrix(float pt) {
			if (cachedMat != null)
				return;
			cachedMat = apply(new PoseStack(), pt).last()
				.pose();
		}

		public SceneTransform rotate(Axis axis, float amount) {
			return rotate(axis, amount, .1f);
		}

		public SceneTransform rotate(Axis axis, float amount, float speed) {
			float target;
			switch(axis) {
				case X:
					target = transform.xRotation.getChaseTarget() + amount;
					transform.xRotation.chase(target, speed, LerpedFloat.Chaser.EXP);
					break;

				case Y:
					target = transform.yRotation.getChaseTarget() + amount;
					transform.yRotation.chase(target, speed, LerpedFloat.Chaser.EXP);
					break;
			}

			return this;
		}
	}

	public class SceneCamera extends Camera {

		public void set(float xRotation, float yRotation) {
			setRotation(yRotation, xRotation);
		}

	}

}
