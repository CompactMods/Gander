package dev.compactmods.gander.render;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RenderTypes extends RenderStateShard {

    public static final Map<RenderLevelStageEvent.Stage, RenderType> GEOMETRY_STAGES
        = RenderType.chunkBufferLayers()
        .stream()
        .collect(Collectors.toMap(RenderLevelStageEvent.Stage::fromRenderType, Function.identity()));

//	protected static final RenderStateShard BLOCK_SHADER =
//			new RenderStateShard.ShaderStateShard(GameRenderer:);
//
//	private static final RenderType FLUID = RenderType.create("gander:fluid",
//		DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
//			.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
//			.setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
//			.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
//			.setLightmapState(RenderStateShard.LIGHTMAP)
//			.setOverlayState(RenderStateShard.OVERLAY)
//			.createCompositeState(true));
//
//	public static final RenderType PHANTOM = RenderType.create("phantom", DefaultVertexFormat.BLOCK,
//			VertexFormat.Mode.QUADS, 2097152,
//			true, false,
//			RenderType.CompositeState.builder()
////					.setOutputState(BLOCK_SHADER)
//					.setLightmapState(RenderStateShard.LIGHTMAP)
//					.setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
////					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
//					.createCompositeState(true));
//
//	public static RenderType getFluid() {
//		return FLUID;
//	}

	// Mmm gimme those protected fields
	private RenderTypes() {
		super(null, null, null);
	}
}
