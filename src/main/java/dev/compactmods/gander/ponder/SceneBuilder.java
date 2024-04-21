package dev.compactmods.gander.ponder;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.joml.Vector3f;

import dev.compactmods.gander.ponder.element.EntityElement;
import dev.compactmods.gander.ponder.element.WorldSectionElement;
import dev.compactmods.gander.ponder.instruction.animation.AnimateWorldSectionInstruction;
import dev.compactmods.gander.ponder.instruction.BlockEntityDataInstruction;
import dev.compactmods.gander.ponder.instruction.ChaseAABBInstruction;
import dev.compactmods.gander.ponder.instruction.ticking.DelayInstruction;
import dev.compactmods.gander.ponder.instruction.EmitParticlesInstruction;
import dev.compactmods.gander.ponder.instruction.EmitParticlesInstruction.Emitter;
import dev.compactmods.gander.ponder.instruction.HighlightValueBoxInstruction;
import dev.compactmods.gander.ponder.instruction.KeyframeInstruction;
import dev.compactmods.gander.ponder.instruction.MarkAsFinishedInstruction;
import dev.compactmods.gander.ponder.instruction.MovePoiInstruction;
import dev.compactmods.gander.ponder.instruction.OutlineSelectionInstruction;
import dev.compactmods.gander.ponder.instruction.contract.PonderInstruction;
import dev.compactmods.gander.ponder.instruction.ReplaceBlocksInstruction;
import dev.compactmods.gander.utility.Color;
import dev.compactmods.gander.utility.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Enqueue instructions to the schedule via this object's methods.
 */
@SuppressWarnings("unused")
public class SceneBuilder {

	/**
	 * Ponder's toolkit for showing information on top of the scene world, such as
	 * highlighted bounding boxes, texts, icons and keybindings.
	 */
	public final OverlayInstructions overlay;

	/**
	 * Instructions for manipulating the schematic and its currently visible areas.
	 * Allows to show, hide and modify blocks as the scene plays out.
	 */
	public final WorldInstructions world;

	/**
	 * Special effects to embellish and communicate with
	 */
	public final EffectInstructions effects;

	/**
	 * Random other instructions that might come in handy
	 */
	public final SpecialInstructions special;

	private final PonderScene scene;

	public SceneBuilder(PonderScene ponderScene) {
		scene = ponderScene;
		overlay = new OverlayInstructions();
		special = new SpecialInstructions();
		world = new WorldInstructions();
		effects = new EffectInstructions();
	}

	// General
	/**
	 * Communicates to the ponder UI which parts of the schematic make up the base
	 * horizontally. Use of this is encouraged whenever there are components outside
	 * the the base plate. <br>
	 * As a result, showBasePlate() will only show the configured size, and the
	 * scene's scaling inside the UI will be consistent with its base size.
	 *
	 * @param xOffset       Block spaces between the base plate and the schematic
	 *                      boundary on the Western side.
	 * @param zOffset       Block spaces between the base plate and the schematic
	 *                      boundary on the Northern side.
	 * @param basePlateSize Length in blocks of the base plate itself. Ponder
	 *                      assumes it to be square
	 */
	public void configureBasePlate(int xOffset, int zOffset, int basePlateSize) {
		scene.basePlateOffsetX = xOffset;
		scene.basePlateOffsetZ = zOffset;
		scene.basePlateSize = basePlateSize;
	}

	/**
	 * Use this in case you are not happy with the scale of the scene relative to
	 * the overlay
	 *
	 * @param factor {@literal >}1 will make the scene appear larger, smaller
	 *               otherwise
	 */
	public void scaleSceneView(float factor) {
		scene.scaleFactor = factor;
	}

	/**
	 * Use this in case you are not happy with the vertical alignment of the scene
	 * relative to the overlay
	 *
	 * @param yOffset {@literal >}0 moves the scene up, down otherwise
	 */
	public void setSceneOffsetY(float yOffset) {
		scene.yOffset = yOffset;
	}

	/**
	 * Adds an instruction to the scene. It is recommended to only use this method
	 * if another method in this class or its subclasses does not already allow
	 * adding a certain instruction.
	 */
	public void addInstruction(PonderInstruction instruction) {
		scene.schedule.add(instruction);
	}

	/**
	 * Adds a simple instruction to the scene. It is recommended to only use this
	 * method if another method in this class or its subclasses does not already
	 * allow adding a certain instruction.
	 */
	public void addInstruction(Consumer<PonderScene> callback) {
		addInstruction(PonderInstruction.simple(callback));
	}

	/**
	 * Before running the upcoming instructions, wait for a duration to let previous
	 * actions play out. <br>
	 * Idle does not stall any animations, only schedules a time gap between
	 * instructions.
	 *
	 * @param ticks Duration to wait for
	 */
	public void idle(int ticks) {
		addInstruction(new DelayInstruction(ticks));
	}

	/**
	 * Before running the upcoming instructions, wait for a duration to let previous
	 * actions play out. <br>
	 * Idle does not stall any animations, only schedules a time gap between
	 * instructions.
	 *
	 * @param seconds Duration to wait for
	 */
	public void idleSeconds(int seconds) {
		idle(seconds * 20);
	}

	/**
	 * Once the scene reaches this instruction in the timeline, mark it as
	 * "finished". This happens automatically when the end of a storyboard is
	 * reached, but can be desirable to do earlier, in order to bypass the wait for
	 * any residual text windows to time out. <br>
	 * So far this event only affects the "next scene" button in the UI to flash.
	 */
	public void markAsFinished() {
		addInstruction(new MarkAsFinishedInstruction());
	}

	/**
	 * Pans the scene's camera view around the vertical axis by the given amount
	 *
	 * @param degrees
	 */
	public void rotateCameraY(float degrees) {
		addInstruction(PonderInstruction.simple(scene -> scene.getTransform().rotate(Axis.Y, degrees)));
	}

	/**
	 * Adds a Key Frame at the end of the last delay() instruction for the users to
	 * skip to
	 */
	public void addKeyframe() {
		addInstruction(KeyframeInstruction.IMMEDIATE);
	}

	/**
	 * Adds a Key Frame a couple ticks after the last delay() instruction for the
	 * users to skip to
	 */
	public void addLazyKeyframe() {
		addInstruction(KeyframeInstruction.DELAYED);
	}

	public class EffectInstructions {

		public void emitParticles(Vec3 location, Emitter emitter, float amountPerCycle, int cycles) {
			addInstruction(new EmitParticlesInstruction(location, emitter, amountPerCycle, cycles));
		}

		public void indicateRedstone(BlockPos pos) {
			createRedstoneParticles(pos, 0xFF0000, 10);
		}

		public void indicateSuccess(BlockPos pos) {
			createRedstoneParticles(pos, 0x80FFaa, 10);
		}

		public void createRedstoneParticles(BlockPos pos, int color, int amount) {
			Vector3f rgb = new Color(color).asVectorF();
			addInstruction(new EmitParticlesInstruction(VecHelper.getCenterOf(pos),
				Emitter.withinBlockSpace(new DustParticleOptions(rgb, 1), Vec3.ZERO), amount, 2));
		}

	}

	public class OverlayInstructions {

		public void chaseBoundingBoxOutline(PonderPalette color, Object slot, AABB boundingBox, int duration) {
			addInstruction(new ChaseAABBInstruction(color, slot, boundingBox, duration));
		}

		public void showCenteredScrollInput(BlockPos pos, Direction side, int duration) {
			showFilterSlotInput(scene.getSceneBuildingUtil().vector.blockSurface(pos, side), side, duration);
		}

		public void showScrollInput(Vec3 location, Direction side, int duration) {
			Axis axis = side.getAxis();
			float s = 1 / 16f;
			float q = 1 / 4f;
			Vec3 expands = new Vec3(axis == Axis.X ? s : q, axis == Axis.Y ? s : q, axis == Axis.Z ? s : q);
			addInstruction(new HighlightValueBoxInstruction(location, expands, duration));
		}

		public void showRepeaterScrollInput(BlockPos pos, int duration) {
			showFilterSlotInput(scene.getSceneBuildingUtil().vector.blockSurface(pos, Direction.DOWN)
				.add(0, 3 / 16f, 0), Direction.UP, duration);
		}

		public void showFilterSlotInput(Vec3 location, Direction side, int duration) {
			location = location.add(Vec3.atLowerCornerOf(side.getNormal())
				.scale(-3 / 128f));
			Vec3 expands = VecHelper.axisAlingedPlaneOf(side)
				.scale(11 / 128f);
			addInstruction(new HighlightValueBoxInstruction(location, expands, duration));
		}

		public void showOutline(PonderPalette color, Object slot, Selection selection, int duration) {
			addInstruction(new OutlineSelectionInstruction(color, slot, selection, duration));
		}

	}

	public class SpecialInstructions {
		public void movePointOfInterest(Vec3 location) {
			addInstruction(new MovePoiInstruction(location));
		}

		public void movePointOfInterest(BlockPos location) {
			movePointOfInterest(VecHelper.getCenterOf(location));
		}
	}

	public class WorldInstructions {

		public void incrementBlockBreakingProgress(BlockPos pos) {
			addInstruction(scene -> {
				PonderLevel world = scene.getWorld();
				int progress = world.getBlockBreakingProgressions()
					.getOrDefault(pos, -1) + 1;
				if (progress == 9) {
					world.addBlockDestroyEffects(pos, world.getBlockState(pos));
					world.destroyBlock(pos, false);
					world.setBlockBreakingProgress(pos, 0);
					scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
				} else
					world.setBlockBreakingProgress(pos, progress + 1);
			});
		}

		public void restoreBlocks(Selection selection) {
			addInstruction(scene -> {
				scene.getWorld().restoreBlocks(selection);
				scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
			});
		}

		public ElementLink<WorldSectionElement> makeSectionIndependent(Selection selection) {
			WorldSectionElement worldSectionElement = new WorldSectionElement(selection);
			ElementLink<WorldSectionElement> elementLink = new ElementLink<>(WorldSectionElement.class);

			addInstruction(scene -> {
				scene.getBaseWorldSection().erase(selection);
				scene.linkElement(worldSectionElement, elementLink);
				scene.addElement(worldSectionElement);
				worldSectionElement.queueRedraw();
				worldSectionElement.resetAnimatedTransform();
				worldSectionElement.forceApplyFade(1);
			});

			return elementLink;
		}

		public void rotateSection(ElementLink<WorldSectionElement> link, double xRotation, double yRotation,
			double zRotation, int duration) {
			addInstruction(
				AnimateWorldSectionInstruction.rotate(link, new Vec3(xRotation, yRotation, zRotation), duration));
		}

		public void configureCenterOfRotation(ElementLink<WorldSectionElement> link, Vec3 anchor) {
			addInstruction(scene -> scene.resolve(link)
				.setCenterOfRotation(anchor));
		}

		public void configureStabilization(ElementLink<WorldSectionElement> link, Vec3 anchor) {
			addInstruction(scene -> scene.resolve(link)
				.stabilizeRotation(anchor));
		}

		public void moveSection(ElementLink<WorldSectionElement> link, Vec3 offset, int duration) {
			addInstruction(AnimateWorldSectionInstruction.move(link, offset, duration));
		}

		public void setBlocks(Selection selection, BlockState state, boolean spawnParticles) {
			addInstruction(new ReplaceBlocksInstruction(selection, $ -> state, true, spawnParticles));
		}

		public void destroyBlock(BlockPos pos) {
			setBlock(pos, Blocks.AIR.defaultBlockState(), true);
		}

		public void setBlock(BlockPos pos, BlockState state, boolean spawnParticles) {
			setBlocks(scene.getSceneBuildingUtil().select.position(pos), state, spawnParticles);
		}

		public void replaceBlocks(Selection selection, BlockState state, boolean spawnParticles) {
			modifyBlocks(selection, $ -> state, spawnParticles);
		}

		public void modifyBlock(BlockPos pos, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
			modifyBlocks(scene.getSceneBuildingUtil().select.position(pos), stateFunc, spawnParticles);
		}

		public void cycleBlockProperty(BlockPos pos, Property<?> property) {
			modifyBlocks(scene.getSceneBuildingUtil().select.position(pos),
				s -> s.hasProperty(property) ? s.cycle(property) : s, false);
		}

		public void modifyBlocks(Selection selection, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
			addInstruction(new ReplaceBlocksInstruction(selection, stateFunc, false, spawnParticles));
		}

		public void toggleRedstonePower(Selection selection) {
			modifyBlocks(selection, s -> {
				if (s.hasProperty(BlockStateProperties.POWER))
					s = s.setValue(BlockStateProperties.POWER, s.getValue(BlockStateProperties.POWER) == 0 ? 15 : 0);
				if (s.hasProperty(BlockStateProperties.POWERED))
					s = s.cycle(BlockStateProperties.POWERED);
				if (s.hasProperty(RedstoneTorchBlock.LIT))
					s = s.cycle(RedstoneTorchBlock.LIT);
				return s;
			}, false);
		}

		public <T extends Entity> void modifyEntities(Class<T> entityClass, Consumer<T> entityCallBack) {
			addInstruction(scene -> scene.forEachWorldEntity(entityClass, entityCallBack));
		}

		public <T extends Entity> void modifyEntitiesInside(Class<T> entityClass, Selection area,
			Consumer<T> entityCallBack) {
			addInstruction(scene -> scene.forEachWorldEntity(entityClass, e -> {
				if (area.test(e.blockPosition()))
					entityCallBack.accept(e);
			}));
		}

		public void modifyEntity(ElementLink<EntityElement> link, Consumer<Entity> entityCallBack) {
			addInstruction(scene -> {
				EntityElement resolve = scene.resolve(link);
				if (resolve != null)
					resolve.ifPresent(entityCallBack::accept);
			});
		}

		public ElementLink<EntityElement> createEntity(Function<Level, Entity> factory) {
			ElementLink<EntityElement> link = new ElementLink<>(EntityElement.class, UUID.randomUUID());
			addInstruction(scene -> {
				PonderLevel world = scene.getWorld();
				Entity entity = factory.apply(world);
				EntityElement handle = new EntityElement(entity);
				scene.addElement(handle);
				scene.linkElement(handle, link);
				world.addFreshEntity(entity);
			});
			return link;
		}

		public ElementLink<EntityElement> createItemEntity(Vec3 location, Vec3 motion, ItemStack stack) {
			return createEntity(world -> {
				ItemEntity itemEntity = new ItemEntity(world, location.x, location.y, location.z, stack);
				itemEntity.setDeltaMovement(motion);
				return itemEntity;
			});
		}

		public void setFilterData(Selection selection, Class<? extends BlockEntity> teType, ItemStack filter) {
			modifyBlockEntityNBT(selection, teType, nbt -> {
				nbt.put("Filter", filter.save(new CompoundTag()));
			});
		}

		public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> beType,
			Consumer<CompoundTag> consumer) {
			modifyBlockEntityNBT(selection, beType, consumer, false);
		}

		public <T extends BlockEntity> void modifyBlockEntity(BlockPos position, Class<T> beType,
			Consumer<T> consumer) {
			addInstruction(scene -> {
				BlockEntity blockEntity = scene.getWorld()
					.getBlockEntity(position);
				if (beType.isInstance(blockEntity))
					consumer.accept(beType.cast(blockEntity));
			});
		}

		public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> teType,
			Consumer<CompoundTag> consumer, boolean reDrawBlocks) {
			addInstruction(new BlockEntityDataInstruction(selection, teType, nbt -> {
				consumer.accept(nbt);
				return nbt;
			}, reDrawBlocks));
		}
	}
}
