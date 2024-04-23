package dev.compactmods.gander.ponder.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.mixin.accessor.ParticleEngineAccessor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PonderLevel extends SchematicLevel {

	protected Map<BlockPos, BlockState> originalBlocks;
	protected Map<BlockPos, CompoundTag> originalBlockEntities;
	protected Map<BlockPos, Integer> blockBreakingProgressions;
	protected List<Entity> originalEntities;
	private final Supplier<ClientLevel> asClientWorld = Suppliers.memoize(() -> WrappedClientWorld.of(this));

	protected PonderWorldParticles particles;

	int overrideLight;
	boolean currentlyTickingEntities;

	public PonderLevel(BlockPos anchor, Level original) {
		super(anchor, original);
		originalBlocks = new HashMap<>();
		originalBlockEntities = new HashMap<>();
		blockBreakingProgressions = new HashMap<>();
		originalEntities = new ArrayList<>();
		particles = new PonderWorldParticles(this);
	}

	public void restore() {
		entities.clear();
		blocks.clear();
		blockEntities.clear();
		blockBreakingProgressions.clear();
		renderedBlockEntities.clear();
		originalBlocks.forEach((k, v) -> blocks.put(k, v));
		originalBlockEntities.forEach((k, v) -> {
			BlockEntity blockEntity = BlockEntity.loadStatic(k, originalBlocks.get(k), v);
			onBEadded(blockEntity, blockEntity.getBlockPos());
			blockEntities.put(k, blockEntity);
			renderedBlockEntities.add(blockEntity);
		});
		originalEntities.forEach(e -> EntityType.create(e.serializeNBT(), this)
				.ifPresent(entities::add));
		particles.clearEffects();
	}

	@Override
	public int getBrightness(LightLayer p_226658_1_, BlockPos p_226658_2_) {
		return 15;
	}

	@Override
	public BlockState getBlockState(BlockPos globalPos) {
		if (currentlyTickingEntities && globalPos.getY() < 0)
			return Blocks.AIR.defaultBlockState();

		return super.getBlockState(globalPos);
	}

	@Override // For particle collision
	public BlockGetter getChunkForCollisions(int p_225522_1_, int p_225522_2_) {
		return this;
	}


	public void renderParticles(PoseStack ms, MultiBufferSource.BufferSource buffer, Camera camera, float partialTicks) {
		particles.render(ms, buffer, Minecraft.getInstance().gameRenderer.lightTexture(), camera, partialTicks, null);
	}

	public void animateTick() {
		blocks.keySet()
				.stream()
				.filter(p -> random.nextIntBetweenInclusive(1, 10) <= 3)
				.forEach(this::animateBlockTick);
	}

	protected void animateBlockTick(BlockPos pBlockPos) {
		BlockState blockstate = this.getBlockState(pBlockPos);
		blockstate.getBlock().animateTick(blockstate, this, pBlockPos, random);
		FluidState fluidstate = this.getFluidState(pBlockPos);
		if (!fluidstate.isEmpty()) {
			fluidstate.animateTick(this, pBlockPos, random);
		}

		if (!blockstate.isCollisionShapeFullBlock(this, pBlockPos)) {
			this.getBiome(pBlockPos)
					.value()
					.getAmbientParticle()
					.filter(aps -> aps.canSpawn(random))
					.ifPresent((p_264703_) -> {
						this.addParticle(p_264703_.getOptions(), (double) pBlockPos.getX() + this.random.nextDouble(), (double) pBlockPos.getY() + this.random.nextDouble(), (double) pBlockPos.getZ() + this.random.nextDouble(), 0.0D, 0.0D, 0.0D);
					});
		}
	}

	public void tick() {
		currentlyTickingEntities = true;

		blockEntities.forEach((pos, ent) -> {
			final var state = ent.getBlockState();
			@SuppressWarnings("unchecked") var possibleTicker = (BlockEntityTicker<BlockEntity>) state.getTicker(this, ent.getType());
			if(possibleTicker != null)
				possibleTicker.tick(this, pos, state, ent);

		});

		particles.tick();

		for (Iterator<Entity> iterator = entities.iterator(); iterator.hasNext(); ) {
			Entity entity = iterator.next();

			entity.tickCount++;
			entity.xOld = entity.getX();
			entity.yOld = entity.getY();
			entity.zOld = entity.getZ();
			entity.tick();

			if (entity.getY() <= -.5f)
				entity.discard();

			if (!entity.isAlive())
				iterator.remove();
		}

		currentlyTickingEntities = false;
	}

	@Override
	public void addParticle(ParticleOptions data, double x, double y, double z, double mx, double my, double mz) {
		addParticle(makeParticle(data, x, y, z, mx, my, mz));
	}

	@Override
	public void addAlwaysVisibleParticle(ParticleOptions data, double x, double y, double z, double mx, double my, double mz) {
		addParticle(data, x, y, z, mx, my, mz);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <T extends ParticleOptions> Particle makeParticle(T data, double x, double y, double z, double mx, double my,
															  double mz) {

		ResourceLocation key = BuiltInRegistries.PARTICLE_TYPE.getKey(data.getType());
		ParticleProvider<T> particleProvider = (ParticleProvider<T>) particles.getProvider(key);
		return particleProvider == null ? null
				: particleProvider.createParticle(data, asClientWorld.get(), x, y, z, mx, my, mz);
	}

	@Override
	public boolean setBlock(BlockPos pos, BlockState arg1, int arg2) {
		return super.setBlock(pos, arg1, arg2);
	}

	public void addParticle(Particle p) {
		if (p != null)
			particles.addParticle(p);
	}

	public void setBlockBreakingProgress(BlockPos pos, int damage) {
		if (damage == 0)
			blockBreakingProgressions.remove(pos);
		else
			blockBreakingProgressions.put(pos, damage - 1);
	}

	public Map<BlockPos, Integer> getBlockBreakingProgressions() {
		return blockBreakingProgressions;
	}

	public void addBlockDestroyEffects(BlockPos pos, BlockState state) {
		VoxelShape voxelshape = state.getShape(this, pos);
		if (voxelshape.isEmpty())
			return;

		AABB bb = voxelshape.bounds();
		double d1 = Math.min(1.0D, bb.maxX - bb.minX);
		double d2 = Math.min(1.0D, bb.maxY - bb.minY);
		double d3 = Math.min(1.0D, bb.maxZ - bb.minZ);
		int i = Math.max(2, Mth.ceil(d1 / 0.25D));
		int j = Math.max(2, Mth.ceil(d2 / 0.25D));
		int k = Math.max(2, Mth.ceil(d3 / 0.25D));

		for (int l = 0; l < i; ++l) {
			for (int i1 = 0; i1 < j; ++i1) {
				for (int j1 = 0; j1 < k; ++j1) {
					double d4 = (l + 0.5D) / i;
					double d5 = (i1 + 0.5D) / j;
					double d6 = (j1 + 0.5D) / k;
					double d7 = d4 * d1 + bb.minX;
					double d8 = d5 * d2 + bb.minY;
					double d9 = d6 * d3 + bb.minZ;
					addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + d7, pos.getY() + d8,
							pos.getZ() + d9, d4 - 0.5D, d5 - 0.5D, d6 - 0.5D);
				}
			}
		}
	}

	@Override
	protected BlockState processBlockStateForPrinting(BlockState state) {
		return state;
	}

	@Override
	public boolean hasChunkAt(BlockPos pos) {
		return true; // fix particle lighting
	}

	@Override
	public boolean hasChunk(int x, int y) {
		return true; // fix particle lighting
	}

	@Override
	public boolean isLoaded(BlockPos pos) {
		return true; // fix particle lighting
	}

	@Override
	public boolean hasNearbyAlivePlayer(double p_217358_1_, double p_217358_3_, double p_217358_5_, double p_217358_7_) {
		return true; // always enable spawner animations
	}

	public Vec3i getDimensions() {
		return new Vec3i(bounds.getXSpan(), bounds.getYSpan(), bounds.getZSpan());
	}
}
