package dev.compactmods.gander.ui.pipeline.context;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Set;
import java.util.stream.Collectors;

public class BakedLevelScreenRenderingContext implements ScreenLevelRenderingContext {
    public BakedLevel bakedLevel;
    public BlockAndTintGetter blockAndTints;
    public BoundingBox blockBoundaries;
    public Set<BlockPos> blockEntityPositions;

    public final RenderTarget renderTarget;
    public final TranslucencyChain translucencyChain;
    public final RenderTypeStore renderTypeStore;
    private boolean isDisposed;

    public BakedLevelScreenRenderingContext(BakedLevel bakedLevel) {
        this.bakedLevel = bakedLevel;
        this.blockAndTints = bakedLevel.originalLevel();
        this.blockBoundaries = bakedLevel.blockBoundaries();

        this.blockEntityPositions = BlockPos.betweenClosedStream(blockBoundaries)
            .filter(p -> blockAndTints.getBlockState(p).hasBlockEntity())
            .map(BlockPos::immutable)
            .collect(Collectors.toUnmodifiableSet());

        final var mc = Minecraft.getInstance();
        this.renderTarget = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
        renderTarget.setClearColor(0, 0, 0, 0);

        this.translucencyChain = TranslucencyChain.builder()
            .addLayer(Gander.asResource("main"))
            .addLayer(Gander.asResource("entity"))
            .addLayer(Gander.asResource("water"))
            .addLayer(Gander.asResource("translucent"))
            .addLayer(Gander.asResource("item_entity"))
            .addLayer(Gander.asResource("particles"))
            .addLayer(Gander.asResource("clouds"))
            .addLayer(Gander.asResource("weather"))
            .build(renderTarget);

        this.renderTypeStore = new RenderTypeStore(this.translucencyChain);
        this.isDisposed = false;
    }

    public void dispose() {
        if (isDisposed) return;
        this.isDisposed = true;
        renderTarget.destroyBuffers();
        renderTypeStore.dispose();
    }

    public void recalculateTranslucency(Camera camera) {
        if(bakedLevel != null) {
            bakedLevel.resortTranslucency(camera.getPosition().toVector3f());
        }
    }
}
