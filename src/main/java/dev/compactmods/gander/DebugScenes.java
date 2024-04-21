package dev.compactmods.gander;

import dev.compactmods.gander.ponder.PonderPalette;
import dev.compactmods.gander.ponder.PonderRegistry;
import dev.compactmods.gander.ponder.PonderStoryBoard;
import dev.compactmods.gander.ponder.PonderStoryBoardEntry;
import dev.compactmods.gander.ponder.SceneBuilder;
import dev.compactmods.gander.ponder.SceneBuildingUtil;
import dev.compactmods.gander.ponder.Selection;
import dev.compactmods.gander.ponder.instruction.EmitParticlesInstruction.Emitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DebugScenes {

	public static void registerAll() {
		// add(DebugScenes::blocksScene, "debug/scene_" + index);
		// add(DebugScenes::fluidsScene, "debug/scene_" + index);
		add(DebugScenes::renderers, "debug/renderers");
		add(DebugScenes::renderers, "debug/glass");
		add(DebugScenes::renderers, "debug/glass2");
		// add(DebugScenes::offScreenScene, "debug/scene_" + index);
		// add(DebugScenes::particleScene, "debug/scene_" + index);
		// add(DebugScenes::controlsScene, "debug/scene_" + index);
		add(DebugScenes::birbScene, "debug/scene_birbs");
		// add(DebugScenes::sectionsScene, "debug/scene_" + index);
		// add(DebugScenes::itemScene, "debug/scene_" + index);
	}

	private static void add(PonderStoryBoard sb, String schematicPath) {
		PonderStoryBoardEntry entry = PonderStoryBoardEntry.builder(sb)
				.schematicLocation(GanderLib.ID, schematicPath)
				.component(GanderLib.asResource(schematicPath))
				.build();

		PonderRegistry.addStoryBoard(entry);
	}

	public static void blocksScene(SceneBuilder scene, SceneBuildingUtil util) {
		scene.idle(20);
		scene.world.replaceBlocks(util.select.position(new BlockPos(3, 1, 1)), Blocks.GOLD_BLOCK.defaultBlockState(), true);
		scene.idle(5);
		scene.rotateCameraY(180);
		scene.markAsFinished();
	}

	public static void fluidsScene(SceneBuilder scene, SceneBuildingUtil util) {
		scene.idle(10);
		Vec3 parrotPos = util.vector.topOf(1, 0, 1);
		// scene.special.createBirb(parrotPos, FacePointOfInterestPose::new);
		scene.markAsFinished();

		Object outlineSlot = new Object();

		Vec3 vec1 = util.vector.topOf(1, 0, 0);
		Vec3 vec2 = util.vector.topOf(0, 0, 1);
		AABB boundingBox1 = new AABB(vec1, vec1).expandTowards(0, 2.5, 0)
				.inflate(.15, 0, .15);
		AABB boundingBox2 = new AABB(vec2, vec2).expandTowards(0, .125, 0)
				.inflate(.45, 0, .45);
		Vec3 poi1 = boundingBox1.getCenter();
		Vec3 poi2 = boundingBox2.getCenter();

		for (int i = 0; i < 10; i++) {
			scene.overlay.chaseBoundingBoxOutline(PonderPalette.RED, outlineSlot,
					i % 2 == 0 ? boundingBox1 : boundingBox2, 15);
			scene.idle(1);
		}
	}

	private static void renderers(SceneBuilder scene, SceneBuildingUtil util) {
		scene.markAsFinished();
	}

	public static void particleScene(SceneBuilder scene, SceneBuildingUtil util) {
		scene.idle(10);

		Vec3 emitterPos = util.vector.of(2.5, 2.25, 2.5);
		Emitter emitter = Emitter.simple(ParticleTypes.LAVA, util.vector.of(0, .1, 0));
		Emitter rotation = Emitter.simple(ParticleTypes.ASH, util.vector.of(0, .1, 0));

		scene.idle(30);
		scene.effects.emitParticles(emitterPos, emitter, 1, 60);
		scene.effects.emitParticles(emitterPos, rotation, 20, 1);
		scene.idle(30);
		scene.rotateCameraY(180);
	}

	public static void controlsScene(SceneBuilder scene, SceneBuildingUtil util) {
		scene.idle(10);

		BlockPos shaftPos = new BlockPos(3, 1, 1);
		Selection shaftSelection = util.select.position(shaftPos);
		scene.idle(20);
		scene.world.replaceBlocks(shaftSelection, Blocks.BAMBOO_FENCE.defaultBlockState(), true);

		scene.idle(40);

		BlockPos chassis = new BlockPos(1, 1, 3);
		Vec3 chassisSurface = util.vector.blockSurface(chassis, Direction.NORTH);

		Object chassisValueBoxHighlight = new Object();
		Object chassisEffectHighlight = new Object();

		AABB point = new AABB(chassisSurface, chassisSurface);
		AABB expanded = point.inflate(1 / 4f, 1 / 4f, 1 / 16f);

		Selection singleBlock = util.select.position(new BlockPos(1, 2, 3));
		Selection twoBlocks = util.select.fromTo(1, 2, 3, 1, 3, 3);
		Selection threeBlocks = util.select.fromTo(1, 2, 3, 1, 4, 3);

		Selection singleRow = util.select.fromTo(1, 2, 3, 3, 2, 3);
		Selection twoRows = util.select.fromTo(1, 2, 3, 3, 3, 3);
		Selection threeRows = twoRows.copy()
				.add(threeBlocks);

		scene.overlay.chaseBoundingBoxOutline(PonderPalette.GREEN, chassisValueBoxHighlight, point, 1);
		scene.idle(1);
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.GREEN, chassisValueBoxHighlight, expanded, 120);

		PonderPalette white = PonderPalette.WHITE;
		scene.overlay.showOutline(white, chassisEffectHighlight, singleBlock, 10);
		scene.idle(10);
		scene.overlay.showOutline(white, chassisEffectHighlight, twoBlocks, 10);
		scene.idle(10);
		scene.overlay.showOutline(white, chassisEffectHighlight, threeBlocks, 10);
		scene.idle(10);
		scene.overlay.showOutline(white, chassisEffectHighlight, twoBlocks, 10);
		scene.idle(10);
		scene.overlay.showOutline(white, chassisEffectHighlight, singleBlock, 10);
		scene.idle(10);

		scene.overlay.showOutline(white, chassisEffectHighlight, singleRow, 10);
		scene.idle(10);
		scene.overlay.showOutline(white, chassisEffectHighlight, twoRows, 10);
		scene.idle(10);
		scene.overlay.showOutline(white, chassisEffectHighlight, threeRows, 10);
		scene.idle(10);
		scene.overlay.showOutline(white, chassisEffectHighlight, twoRows, 10);
		scene.idle(10);
		scene.overlay.showOutline(white, chassisEffectHighlight, singleRow, 10);
		scene.idle(10);

		scene.markAsFinished();
	}

	public static void birbScene(SceneBuilder scene, SceneBuildingUtil util) {
		scene.idle(10);

		BlockPos pos = new BlockPos(1, 2, 3);

		scene.idle(10);
		// scene.special.createBirb(util.vector.topOf(0, 1, 2), DancePose::new);
		scene.idle(10);

		// scene.special.createBirb(util.vector.centerOf(3, 1, 3).add(0, 0.25f, 0), FacePointOfInterestPose::new);
		scene.idle(20);

		BlockPos poi1 = new BlockPos(4, 1, 0);
		BlockPos poi2 = new BlockPos(0, 1, 4);

		scene.world.setBlock(poi1, Blocks.GOLD_BLOCK.defaultBlockState(), true);
		scene.idle(20);

		scene.world.setBlock(poi2, Blocks.GOLD_BLOCK.defaultBlockState(), true);
		scene.idle(20);

		scene.world.destroyBlock(poi1);
		scene.idle(20);

		scene.world.destroyBlock(poi2);
	}

	public static void itemScene(SceneBuilder scene, SceneBuildingUtil util) {
		ItemStack brassItem = new ItemStack(Items.STICK);
		ItemStack copperItem = new ItemStack(Items.COPPER_INGOT);

		for (int z = 4; z >= 2; z--) {
			scene.world.createItemEntity(util.vector.centerOf(0, 4, z), Vec3.ZERO, brassItem.copy());
			scene.idle(10);
		}

		scene.world.modifyEntities(ItemEntity.class, entity -> {
			if (ItemHelper.sameItem(copperItem, entity.getItem()))
				entity.setNoGravity(true);
		});

		scene.idle(20);

		scene.world.modifyEntities(ItemEntity.class, entity -> {
			if (ItemHelper.sameItem(brassItem, entity.getItem()))
				entity.setDeltaMovement(util.vector.of(-.15f, .5f, 0));
		});

		scene.idle(25);

		scene.world.modifyEntities(ItemEntity.class, Entity::discard);
	}
}
