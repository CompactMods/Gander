package dev.compactmods.gander.ponder;

import dev.compactmods.gander.ponder.level.PonderLevel;
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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.joml.Vector3f;

public class Scene {

	private PonderLevel level;

	public Scene(StructureTemplate sceneTemplate) {
		loadTemplate(sceneTemplate);
	}

	public void begin() {
		level.restore();
	}

	public void tick() {
		level.tick();
		level.animateTick();
	}

	public PonderLevel getLevel() {
		return level;
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
