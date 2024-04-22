package dev.compactmods.gander.ponder;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class SceneBuilder {

	final StructureTemplate template;

	private SceneBuilder(StructureTemplate template) {
		this.template = template;
	}

	public static SceneBuilder empty() {
		return new SceneBuilder(new StructureTemplate());
	}

	public static SceneBuilder forTemplate(ResourceLocation templateID) {
		final var templateManager = ServerLifecycleHooks.getCurrentServer().getStructureManager();
		return templateManager.get(templateID)
				.map(SceneBuilder::forTemplate)
				.orElse(SceneBuilder.empty());
	}

	public static SceneBuilder forTemplate(StructureTemplate template) {
		return new SceneBuilder(template);
	}

	public Scene build() {
		return new Scene(template);
	}
}
//
//	/**
//	 * Instructions for manipulating the schematic and its currently visible areas.
//	 * Allows to show, hide and modify blocks as the scene plays out.
//	 */
//	public final WorldInstructions world;
//
//	/**
//	 * Special effects to embellish and communicate with
//	 */
//	public final EffectInstructions effects;
//
//
//	private final PonderScene scene;
//
//	public SceneBuilder(PonderScene ponderScene) {
//		scene = ponderScene;
//		world = new WorldInstructions();
//		effects = new EffectInstructions();
//	}
//
//	// General
//	/**
//	 * Adds an instruction to the scene. It is recommended to only use this method
//	 * if another method in this class or its subclasses does not already allow
//	 * adding a certain instruction.
//	 */
//	public void addInstruction(PonderInstruction instruction) {
//		scene.schedule.add(instruction);
//	}
//
//	/**
//	 * Adds a simple instruction to the scene. It is recommended to only use this
//	 * method if another method in this class or its subclasses does not already
//	 * allow adding a certain instruction.
//	 */
//	public void addInstruction(Consumer<PonderScene> callback) {
//		addInstruction(PonderInstruction.simple(callback));
//	}
//
//
//	/**
//	 * Pans the scene's camera view around the vertical axis by the given amount
//	 *
//	 * @param degrees
//	 */
//	public void rotateCameraY(float degrees) {
//		addInstruction(PonderInstruction.simple(scene -> scene.getTransform().rotate(Axis.Y, degrees)));
//	}
//
//	/**
//	 * Adds a Key Frame at the end of the last delay() instruction for the users to
//	 * skip to
//	 */
//	public void addKeyframe() {
//		addInstruction(KeyframeInstruction.IMMEDIATE);
//	}
//
//	/**
//	 * Adds a Key Frame a couple ticks after the last delay() instruction for the
//	 * users to skip to
//	 */
//	public void addLazyKeyframe() {
//		addInstruction(KeyframeInstruction.DELAYED);
//	}
//
//	public class EffectInstructions {
//
//		public void emitParticles(Vec3 location, Emitter emitter, float amountPerCycle, int cycles) {
//			addInstruction(new EmitParticlesInstruction(location, emitter, amountPerCycle, cycles));
//		}
//
//		public void indicateRedstone(BlockPos pos) {
//			createRedstoneParticles(pos, 0xFF0000, 10);
//		}
//
//		public void indicateSuccess(BlockPos pos) {
//			createRedstoneParticles(pos, 0x80FFaa, 10);
//		}
//
//		public void createRedstoneParticles(BlockPos pos, int color, int amount) {
//			Vector3f rgb = new Color(color).asVectorF();
//			addInstruction(new EmitParticlesInstruction(VecHelper.getCenterOf(pos),
//				Emitter.withinBlockSpace(new DustParticleOptions(rgb, 1), Vec3.ZERO), amount, 2));
//		}
//
//	}
//
//	public class WorldInstructions {
//
//		public void incrementBlockBreakingProgress(BlockPos pos) {
//			addInstruction(scene -> {
//				PonderLevel world = scene.getWorld();
//				int progress = world.getBlockBreakingProgressions()
//					.getOrDefault(pos, -1) + 1;
//				if (progress == 9) {
//					world.addBlockDestroyEffects(pos, world.getBlockState(pos));
//					world.destroyBlock(pos, false);
//					world.setBlockBreakingProgress(pos, 0);
//					scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
//				} else
//					world.setBlockBreakingProgress(pos, progress + 1);
//			});
//		}
//
//		public void rotateSection(ElementLink<WorldSectionElement> link, double xRotation, double yRotation,
//			double zRotation, int duration) {
//			addInstruction(
//				AnimateWorldSectionInstruction.rotate(link, new Vec3(xRotation, yRotation, zRotation), duration));
//		}
//
//		public void configureCenterOfRotation(ElementLink<WorldSectionElement> link, Vec3 anchor) {
//			addInstruction(scene -> scene.resolve(link)
//				.setCenterOfRotation(anchor));
//		}
//
//		public void configureStabilization(ElementLink<WorldSectionElement> link, Vec3 anchor) {
//			addInstruction(scene -> scene.resolve(link)
//				.stabilizeRotation(anchor));
//		}
//
//		public void moveSection(ElementLink<WorldSectionElement> link, Vec3 offset, int duration) {
//			addInstruction(AnimateWorldSectionInstruction.move(link, offset, duration));
//		}
//
//		public void destroyBlock(BlockPos pos) {
//			setBlock(pos, Blocks.AIR.defaultBlockState(), true);
//		}
//
//		public void modifyBlock(BlockPos pos, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
//			modifyBlocks(scene.getSceneBuildingUtil().select.position(pos), stateFunc, spawnParticles);
//		}
//
//		public void cycleBlockProperty(BlockPos pos, Property<?> property) {
//			modifyBlocks(scene.getSceneBuildingUtil().select.position(pos),
//				s -> s.hasProperty(property) ? s.cycle(property) : s, false);
//		}
//
//		public void modifyBlocks(Selection selection, UnaryOperator<BlockState> stateFunc, boolean spawnParticles) {
//			addInstruction(new ReplaceBlocksInstruction(selection, stateFunc, false, spawnParticles));
//		}
//
//		public void toggleRedstonePower(Selection selection) {
//			modifyBlocks(selection, s -> {
//				if (s.hasProperty(BlockStateProperties.POWER))
//					s = s.setValue(BlockStateProperties.POWER, s.getValue(BlockStateProperties.POWER) == 0 ? 15 : 0);
//				if (s.hasProperty(BlockStateProperties.POWERED))
//					s = s.cycle(BlockStateProperties.POWERED);
//				if (s.hasProperty(RedstoneTorchBlock.LIT))
//					s = s.cycle(RedstoneTorchBlock.LIT);
//				return s;
//			}, false);
//		}
//
//		public <T extends Entity> void modifyEntities(Class<T> entityClass, Consumer<T> entityCallBack) {
//			addInstruction(scene -> scene.forEachWorldEntity(entityClass, entityCallBack));
//		}
//
//		public <T extends Entity> void modifyEntitiesInside(Class<T> entityClass, Selection area,
//			Consumer<T> entityCallBack) {
//			addInstruction(scene -> scene.forEachWorldEntity(entityClass, e -> {
//				if (area.test(e.blockPosition()))
//					entityCallBack.accept(e);
//			}));
//		}
//
//		public void modifyEntity(ElementLink<EntityElement> link, Consumer<Entity> entityCallBack) {
//			addInstruction(scene -> {
//				EntityElement resolve = scene.resolve(link);
//				if (resolve != null)
//					resolve.ifPresent(entityCallBack::accept);
//			});
//		}
//
//		public ElementLink<EntityElement> createEntity(Function<Level, Entity> factory) {
//			ElementLink<EntityElement> link = new ElementLink<>(EntityElement.class, UUID.randomUUID());
//			addInstruction(scene -> {
//				PonderLevel world = scene.getWorld();
//				Entity entity = factory.apply(world);
//				EntityElement handle = new EntityElement(entity);
//				scene.addElement(handle);
//				scene.linkElement(handle, link);
//				world.addFreshEntity(entity);
//			});
//			return link;
//		}
//
//		public ElementLink<EntityElement> createItemEntity(Vec3 location, Vec3 motion, ItemStack stack) {
//			return createEntity(world -> {
//				ItemEntity itemEntity = new ItemEntity(world, location.x, location.y, location.z, stack);
//				itemEntity.setDeltaMovement(motion);
//				return itemEntity;
//			});
//		}
//
//		public void setFilterData(Selection selection, Class<? extends BlockEntity> teType, ItemStack filter) {
//			modifyBlockEntityNBT(selection, teType, nbt -> {
//				nbt.put("Filter", filter.save(new CompoundTag()));
//			});
//		}
//
//		public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> beType,
//			Consumer<CompoundTag> consumer) {
//			modifyBlockEntityNBT(selection, beType, consumer, false);
//		}
//
//		public <T extends BlockEntity> void modifyBlockEntity(BlockPos position, Class<T> beType,
//			Consumer<T> consumer) {
//			addInstruction(scene -> {
//				BlockEntity blockEntity = scene.getWorld()
//					.getBlockEntity(position);
//				if (beType.isInstance(blockEntity))
//					consumer.accept(beType.cast(blockEntity));
//			});
//		}
//
//		public void modifyBlockEntityNBT(Selection selection, Class<? extends BlockEntity> teType,
//			Consumer<CompoundTag> consumer, boolean reDrawBlocks) {
//			addInstruction(new BlockEntityDataInstruction(selection, teType, nbt -> {
//				consumer.accept(nbt);
//				return nbt;
//			}, reDrawBlocks));
//		}
//	}
//}
