package dev.compactmods.gander.render.rendertypes;

import java.util.Map;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class RenderTypeStore {
    public static final ResourceLocation MAIN_TARGET = ResourceLocation.fromNamespaceAndPath("gander", "main");

    private static final Map<RenderStateShard.OutputStateShard, String> BLOCK_RENDER_TARGET_MAP = Map.of(
        RenderStateShard.OutputStateShard.MAIN_TARGET, "main",
        RenderStateShard.OutputStateShard.OUTLINE_TARGET, "entity",
        RenderStateShard.OutputStateShard.TRANSLUCENT_TARGET, "translucent",
        RenderStateShard.OutputStateShard.PARTICLES_TARGET, "particles",
        RenderStateShard.OutputStateShard.WEATHER_TARGET, "weather",
        RenderStateShard.OutputStateShard.CLOUDS_TARGET, "clouds",
        RenderStateShard.OutputStateShard.ITEM_ENTITY_TARGET, "item_entity"
    );

    private static final Map<RenderStateShard.OutputStateShard, String> FLUID_RENDER_TARGET_MAP = Map.of(
        RenderStateShard.OutputStateShard.MAIN_TARGET, "main",
        RenderStateShard.OutputStateShard.OUTLINE_TARGET, "entity",
        RenderStateShard.OutputStateShard.TRANSLUCENT_TARGET, "water",
        RenderStateShard.OutputStateShard.PARTICLES_TARGET, "particles",
        RenderStateShard.OutputStateShard.WEATHER_TARGET, "weather",
        RenderStateShard.OutputStateShard.CLOUDS_TARGET, "clouds",
        RenderStateShard.OutputStateShard.ITEM_ENTITY_TARGET, "item_entity"
    );

    private final TranslucencyChain translucencyChain;
    private final Map<RenderType, RenderType> REMAPPED_BLOCK_RENDER_TYPES = new Reference2ObjectOpenHashMap<>();
    private final Map<RenderType, RenderType> REMAPPED_FLUID_RENDER_TYPES = new Reference2ObjectOpenHashMap<>();

    public RenderTypeStore(TranslucencyChain translucencyChain) {
        this.translucencyChain = translucencyChain;
    }

    public RenderType redirectedRenderType(RenderType desiredType, Map<RenderStateShard.OutputStateShard, String> map, Map<RenderType, RenderType> renderTypeMap) {
        try {
            final var crying = GanderCompositeRenderType.of(desiredType);
            final var remappedQuestionMark = Gander.asResource(map.get(crying.state().outputState));

            final var AAAAAAAA = renderTypeMap.computeIfAbsent(desiredType, type -> GanderCompositeRenderType.of(type)
                .targetingTranslucentRenderTarget(
                    translucencyChain.getRenderTarget(remappedQuestionMark),
                    translucencyChain.getRenderTarget(MAIN_TARGET)));

            return AAAAAAAA;
        } catch (Exception ex) {
            return desiredType;
        }
    }

    public RenderType redirectedBlockRenderType(RenderType renderType) {
        return redirectedRenderType(renderType, BLOCK_RENDER_TARGET_MAP, REMAPPED_BLOCK_RENDER_TYPES);
    }

    public RenderType redirectedFluidRenderType(RenderType desiredType) {
        return redirectedRenderType(desiredType, FLUID_RENDER_TARGET_MAP, REMAPPED_FLUID_RENDER_TYPES);
    }

    public void dispose() {
        REMAPPED_BLOCK_RENDER_TYPES.clear();
        REMAPPED_FLUID_RENDER_TYPES.clear();
    }
}
