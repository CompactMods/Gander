package dev.compactmods.gander.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO 1.17: use custom shaders instead of vanilla ones
public class RenderTypes extends RenderStateShard {

    public static final Map<RenderLevelStageEvent.Stage, RenderType> GEOMETRY_STAGES
        = RenderType.chunkBufferLayers()
        .stream()
        .collect(Collectors.toMap(RenderLevelStageEvent.Stage::fromRenderType, Function.identity()));

    public static final Set<RenderType> STATIC_GEOMETRY_RENDER_TYPES = Set.of(
        RenderType.SOLID,
        RenderType.CUTOUT_MIPPED,
        RenderType.CUTOUT
    );

	protected static final RenderStateShard.ShaderStateShard BLOCK_SHADER =
			new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTranslucentMovingBlockShader);

	private static final RenderType FLUID = RenderType.create("gander:fluid",
		DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
			.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
			.setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
			.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
			.setLightmapState(RenderStateShard.LIGHTMAP)
			.setOverlayState(RenderStateShard.OVERLAY)
			.createCompositeState(true));

	public static final RenderType PHANTOM = RenderType.create("phantom", DefaultVertexFormat.BLOCK,
			VertexFormat.Mode.QUADS, 2097152,
			true, false,
			RenderType.CompositeState.builder()
					.setShaderState(BLOCK_SHADER)
					.setLightmapState(RenderStateShard.LIGHTMAP)
					.setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.createCompositeState(true));

	public static RenderType getFluid() {
		return FLUID;
	}

	// Mmm gimme those protected fields
	private RenderTypes() {
		super(null, null, null);
	}

    public static boolean isStaticGeometryRenderType(RenderType renderType) {
        return STATIC_GEOMETRY_RENDER_TYPES.contains(renderType);
    }
}
