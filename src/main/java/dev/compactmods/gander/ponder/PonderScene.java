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

import dev.compactmods.gander.utility.math.PoseTransformStack;

import net.minecraft.util.ArrayListDeque;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.compactmods.gander.gui.UIRenderHelper;
import dev.compactmods.gander.outliner.Outliner;
import dev.compactmods.gander.ponder.element.PonderElement;
import dev.compactmods.gander.ponder.element.WorldSectionElement;
import dev.compactmods.gander.ponder.instruction.contract.PonderInstruction;
import dev.compactmods.gander.utility.AnimationTickHolder;
import dev.compactmods.gander.utility.animation.LerpedFloat;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class PonderScene {

	private boolean finished;
	ResourceLocation sceneId;

	private final IntList keyframeTimes;

	List<PonderInstruction> schedule;
	private final Deque<PonderInstruction> activeSchedule;
	private final Map<UUID, PonderElement> linkedElements;
	private final Set<PonderElement> elements;

	private final PonderLevel world;
	public final ResourceLocation component;
	public SceneTransform transform;
	public final Outliner outliner;

	private final WorldSectionElement baseWorldSection;

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
		baseWorldSection = new WorldSectionElement(Selection.of(world.getBounds()));
		keyframeTimes = new IntArrayList(4);
	}

	public void deselect() {
		forEach(WorldSectionElement.class, WorldSectionElement::resetSelectedBlock);
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

	public void tick() {
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

	public class SceneTransform {

		public LerpedFloat xRotation, yRotation;

		// Screen params
		public int width, height;
		public double offset;
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

}
