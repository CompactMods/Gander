package dev.compactmods.gander.ponder;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import dev.compactmods.gander.utility.Pair;
import dev.compactmods.gander.utility.VecHelper;
import net.minecraft.client.Minecraft;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.phys.shapes.CollisionContext;

import dev.compactmods.gander.ponder.element.PonderElement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.joml.Vector3f;

public class Scene {

	private final Set<PonderElement> elements;
	private PonderLevel level;

	public Scene(StructureTemplate sceneTemplate) {
		this.elements = new HashSet<>();

		loadTemplate(sceneTemplate);
	}

	public void begin() {
		forEach(pe -> pe.reset(this));

		level.restore();
		elements.clear();
	}

	public void tick() {
		level.tick();
		level.animateTick();
		forEach(e -> e.tick(this));
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
		level.getEntityStream()
			.filter(type::isInstance)
			.map(type::cast)
			.forEach(function);
	}

	public Pair<Vec3, BlockHitResult> rayTrace(Vector3f source, Vector3f target) {
		var src = reverseTransformVec(source);
		var srcV = VecHelper.fromJoml(source);

		var targ = reverseTransformVec(target);
		var targ2 = VecHelper.fromJoml(targ);
		var targ3 = VecHelper.fromJoml(target);

		BlockHitResult rayTraceBlocks = level.clip(new ClipContext(
				VecHelper.fromJoml(src),
				VecHelper.fromJoml(targ),
				ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty()));

		double t = rayTraceBlocks.getLocation().subtract(targ2).lengthSqr() / source.sub(target).lengthSquared();

		Vec3 actualHit = VecHelper.lerp((float) t, targ3, srcV);
		return Pair.of(actualHit, rayTraceBlocks);
	}

	private Vector3f reverseTransformVec(Vector3f in) {
		// float pt = AnimationTickHolder.getPartialTicks();

		Vector3f clone = new Vector3f(in).lerp(new Vector3f(), 0);
		in = in.sub(clone);
		return in;
	}

	public PonderLevel getLevel() {
		return level;
	}

	public Set<PonderElement> getElements() {
		return elements;
	}

	public BoundingBox getBounds() {
		return level == null ? new BoundingBox(BlockPos.ZERO) : level.getBounds();
	}

	public void loadTemplate(StructureTemplate data) {
		this.level = new PonderLevel(BlockPos.ZERO, Minecraft.getInstance().level);
		data.placeInWorld(level, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(), RandomSource.create(), Block.UPDATE_CLIENTS);
	}

	public StructureTemplate makeTemplate() {
		var template = new StructureTemplate();
		template.fillFromWorld(level, BlockPos.ZERO, level.getDimensions(), true, null);
		return template;
	}
}
