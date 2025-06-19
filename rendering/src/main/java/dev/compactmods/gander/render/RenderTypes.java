package dev.compactmods.gander.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import com.mojang.blaze3d.vertex.VertexFormat;

import dev.compactmods.gander.core.Gander;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RenderTypes {

    public static final Map<RenderLevelStageEvent.Stage, ChunkSectionLayerGroup> GEOMETRY_STAGES
        = Stream.of(ChunkSectionLayerGroup.values())
            .collect(Collectors.toMap(RenderLevelStageEvent.Stage::fromChunkLayerGroup, Function.identity()));

    //	protected static final RenderStateShard BLOCK_SHADER =
//			new RenderStateShard.ShaderStateShard(GameRenderer:);
//
    private static final RenderType FLUID = Util.make(() -> {
        final var builder = RenderType.CompositeState.builder()
//			.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
            .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
//			.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setOverlayState(RenderStateShard.OVERLAY)
            .createCompositeState(true);

        return RenderType.create(Gander.asResource("fluid").toString(), 256,
            false, true, RenderPipelines.TRANSLUCENT_MOVING_BLOCK, builder);
    });
//
//	public static final RenderType PHANTOM = RenderType.create("phantom", DefaultVertexFormat.BLOCK,
//			VertexFormat.Mode.QUADS, 2097152,
//			true, false,
//			RenderType.CompositeState.builder()
////					.setOutputState(BLOCK_SHADER)
//					.setLightmapState(RenderStateShard.LIGHTMAP)
//					.setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)

    /// /					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
//					.createCompositeState(true));

	public static RenderType getFluid() {
		return FLUID;
	}
}
