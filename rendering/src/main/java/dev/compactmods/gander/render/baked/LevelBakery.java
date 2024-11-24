package dev.compactmods.gander.render.baked;

import java.lang.ref.WeakReference;
import java.util.Map;

import org.joml.Vector3f;

import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class LevelBakery {

	public static BakedLevel bakeVertices(Level level, BoundingBox blockBoundaries, Vector3f cameraPosition) {

		/*final Set<RenderType> visitedBlockRenderTypes = new HashSet<>();
		final Set<RenderType> visitedFluidRenderTypes = new HashSet<>();
		final RenderRegionCache regionCache = new RenderRegionCache();
		final ChunkBufferBuilderPack blockPack = new ChunkBufferBuilderPack();
		final ChunkBufferBuilderPack fluidPack = new ChunkBufferBuilderPack();

		PoseStack pose = new PoseStack();

		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		ModelBlockRenderer renderer = dispatcher.getModelRenderer();

		RandomSource random = RandomSource.createNewThreadLocalInstance();

		// pose.mulPoseMatrix(new Matrix4f().scaling(1, 1, -1));
		// pose.scale(1 / 24f, 1 / 24f, 1 / 24f);

		var center = blockBoundaries.getCenter().getCenter();

		ModelBlockRenderer.enableCaching();
		BlockPos.betweenClosedStream(blockBoundaries).forEach(pos -> {
			BlockState state = level.getBlockState(pos);
			FluidState fluidState = level.getFluidState(pos);

			pose.pushPose();
			pose.translate(pos.getX(), pos.getY(), pos.getZ());

			ModelData modelData;
			if (state.getRenderShape() == RenderShape.MODEL) {
				BakedModel model = dispatcher.getBlockModel(state);

				if (state.hasBlockEntity()) {
					BlockEntity blockEntity = level.getBlockEntity(pos);
					modelData = blockEntity != null ? blockEntity.getModelData() : ModelData.EMPTY;
					modelData = model.getModelData(level, pos, state, modelData);
				} else {
					modelData = ModelData.EMPTY;
				}

				long seed = state.getSeed(pos);
				random.setSeed(seed);

				ModelData finalModelData = modelData;
				model.getRenderTypes(state, random, modelData).forEach(type -> {
					var typedVC = blockPack.builder(type);
					if (visitedBlockRenderTypes.add(type)) {
						typedVC.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
					}

					renderer.tesselateBlock(level, model, state, pos, pose, typedVC, true, random, seed, OverlayTexture.NO_OVERLAY, finalModelData, type);
				});
			}

			if (!fluidState.isEmpty()) {
				final var fluidRenderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
				var typedVC = fluidPack.builder(fluidRenderType);
				if (visitedFluidRenderTypes.add(fluidRenderType)) {
					typedVC.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
				}

				dispatcher.getLiquidBlockRenderer().tesselate(level, pos, new FluidVertexConsumer(typedVC, pose, pos), state, fluidState);
			}

			 pose.popPose();
			ModelBlockRenderer.clearCache();
		});

		final var additionalRenderers = ClientHooks.gatherAdditionalRenderers(BlockPos.ZERO, level);

		// TODO - Hook for multiplatform?
		net.neoforged.neoforge.client.ClientHooks.addAdditionalGeometry(additionalRenderers, (type) -> {
			BufferBuilder builder = blockPack.builder(type);
			if (visitedBlockRenderTypes.add(type)) {
				builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
			}
			return builder;
		}, createRegion(regionCache, additionalRenderers, level, blockBoundaries), pose);

		BufferBuilder.SortState blockTransparencyState = null;
		if (visitedBlockRenderTypes.contains(RenderType.translucent())) {
			final var builder = blockPack.builder(RenderType.translucent());
			builder.setQuadSorting(VertexSorting.byDistance(cameraPosition.x, cameraPosition.y, cameraPosition.z));
			blockTransparencyState = builder.getSortState();
		}

		BufferBuilder.SortState fluidTransparencyState = null;
		if (visitedFluidRenderTypes.contains(RenderType.translucent())) {
			final var builder = fluidPack.builder(RenderType.translucent());
			builder.setQuadSorting(VertexSorting.byDistance(cameraPosition.x, cameraPosition.y, cameraPosition.z));
			fluidTransparencyState = builder.getSortState();
		}

		Reference2ObjectArrayMap<RenderType, VertexBuffer> blockGeometry = new Reference2ObjectArrayMap<>();
		Reference2ObjectArrayMap<RenderType, VertexBuffer> fluidGeometry = new Reference2ObjectArrayMap<>();
		visitedBlockRenderTypes.forEach(renderType -> {
			final var builder = blockPack.builder(renderType);
			final var buffer = builder.endOrDiscardIfEmpty();

			if(buffer != null) {
				var vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
				vb.bind();
				vb.upload(buffer);
				VertexBuffer.unbind();
				blockGeometry.put(renderType, vb);
			}
		});

		visitedFluidRenderTypes.forEach(renderType -> {
			final var builder = fluidPack.builder(renderType);
			final var buffer = builder.endOrDiscardIfEmpty();

			if(buffer != null) {
				var vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
				vb.bind();
				vb.upload(buffer);
				VertexBuffer.unbind();
				fluidGeometry.put(renderType, vb);
			}
		});

		return new BakedLevel(new WeakReference<>(level), blockPack, fluidPack, blockGeometry, fluidGeometry, blockTransparencyState, fluidTransparencyState, blockBoundaries);*/

		var blockBuilders = new ChunkBufferBuilderPack();
		var fluidBuilders = new ChunkBufferBuilderPack();
		return new BakedLevel(new WeakReference<>(level), blockBuilders, fluidBuilders, Map.of(), Map.of(), blockBuilders.builder(RenderType.cutout()).getSortState(), fluidBuilders.builder(RenderType.translucent()).getSortState(), BoundingBox.infinite());
	}

	/*private static RenderChunkRegion createRegion(RenderRegionCache cache, List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers, Level level, BoundingBox bounds) {
		return cache.createRegion(level,
				new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()),
				new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ()),
				0,
				additionalRenderers.isEmpty());
	}*/
}
