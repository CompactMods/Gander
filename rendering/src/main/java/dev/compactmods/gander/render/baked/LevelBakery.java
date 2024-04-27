package dev.compactmods.gander.render.baked;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.render.FluidVertexConsumer;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LevelBakery {

	private static final SectionBufferBuilderPack SECTION_BUILDER = new SectionBufferBuilderPack();

	public static BakedLevel bakeVertices(Level level, BoundingBox blockBoundaries, Vector3f cameraPosition) {

		final Set<RenderType> visitedRenderTypes = new HashSet<>();
		final RenderRegionCache regionCache = new RenderRegionCache();

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
					var typedVC = SECTION_BUILDER.builder(type);
					if (visitedRenderTypes.add(type)) {
						typedVC.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
					}

					renderer.tesselateBlock(level, model, state, pos, pose, typedVC, true, random, seed, OverlayTexture.NO_OVERLAY, finalModelData, type);
				});
			}

			if (!fluidState.isEmpty()) {
				final var fluidRenderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
				var typedVC = SECTION_BUILDER.builder(fluidRenderType);
				if (visitedRenderTypes.add(fluidRenderType)) {
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
			BufferBuilder builder = SECTION_BUILDER.builder(type);
			if (visitedRenderTypes.add(type)) {
				builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
			}
			return builder;
		}, createRegion(regionCache, additionalRenderers, level, blockBoundaries), pose);

		BufferBuilder.SortState transparencyState = sortTranslucency(cameraPosition, visitedRenderTypes);

		Reference2ObjectArrayMap<RenderType, VertexBuffer> renderers = new Reference2ObjectArrayMap<>();
		visitedRenderTypes.forEach(renderType -> {
			final var builder = SECTION_BUILDER.builder(renderType);
			final var buffer = builder.endOrDiscardIfEmpty();

			if(buffer != null) {
				var vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
				vb.bind();
				vb.upload(buffer);
				VertexBuffer.unbind();
				renderers.put(renderType, vb);
			}
		});

		return new BakedLevel(new WeakReference<>(level), renderers, transparencyState, blockBoundaries);
	}

	public static BufferBuilder.@Nullable SortState sortTranslucency(Vector3f cameraPosition, Set<RenderType> visitedRenderTypes) {
		if (visitedRenderTypes.contains(RenderType.translucent())) {
			var translucent = RenderType.translucent();
			final var builder = SECTION_BUILDER.builder(translucent);
			builder.setQuadSorting(VertexSorting.byDistance(cameraPosition.x, cameraPosition.y, cameraPosition.z));
			return builder.getSortState();
		}

		return null;
	}

	private static RenderChunkRegion createRegion(RenderRegionCache cache, List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers, Level level, BoundingBox bounds) {
		return cache.createRegion(level,
				new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()),
				new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ()),
				0,
				additionalRenderers.isEmpty());
	}
}
