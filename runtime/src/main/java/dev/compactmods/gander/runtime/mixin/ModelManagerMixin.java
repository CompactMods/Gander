package dev.compactmods.gander.runtime.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import com.llamalad7.mixinextras.sugar.Local;

import com.llamalad7.mixinextras.sugar.Share;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import dev.compactmods.gander.render.baked.model.DisplayableMeshGroup;
import dev.compactmods.gander.runtime.additions.BlockModelShaper$Gander;
import dev.compactmods.gander.runtime.baked.ArchetypeBakery;
import dev.compactmods.gander.runtime.baked.ArchetypeDiscovery;
import dev.compactmods.gander.runtime.baked.AtlasIndex;
import dev.compactmods.gander.runtime.baked.AtlasIndex.IndexResult;
import dev.compactmods.gander.runtime.additions.ModelManager$ReloadState$Gander;
import dev.compactmods.gander.runtime.additions.ModelManager$Gander;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

import net.minecraft.world.level.block.state.BlockState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(ModelManager.class)
public class ModelManagerMixin implements ModelManager$Gander
{
    @Unique
    @Final
    private AtomicReference<ArchetypeBakery> gander$archetypeBakery;
    @Unique
    @Final
    private AtlasIndex gander$atlasIndex;

    @Unique
    @Final
    private Logger gander$LOGGER;

    @Unique
    private Map<ModelResourceLocation, DisplayableMeshGroup> gander$bakedBlockStateArchetypes;

    @Shadow
    @Final
    private BlockModelShaper blockModelShaper;

    @Inject(
        method = "<init>",
        at = @At("RETURN"))
    private void gander$initializeFields(
        TextureManager textureManager, BlockColors blockColors, int maxMipmapLevels,
        CallbackInfo ci,
        @Local Map<ResourceLocation, ResourceLocation> vanillaAtlases)
    {
        gander$archetypeBakery = new AtomicReference<>();
        gander$atlasIndex = new AtlasIndex(vanillaAtlases);

        gander$LOGGER = LoggerFactory.getLogger(ModelManager$Gander.class);
    }

    // After model discovery we need to discover their archetypes
    @Inject(
        require = 1,
        method = "reload",
        slice = @Slice(
            from = @At(
                value = "GANDER:INDY",
                target = "Lnet/minecraft/client/resources/model/ModelManager;lambda$reload$0(Lnet/minecraft/client/resources/model/UnbakedModel;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/lang/Void;)Lnet/minecraft/client/resources/model/ModelDiscovery;"),
            to = @At(
                value = "GANDER:INDY",
                target = "Lnet/minecraft/client/resources/model/ModelManager;lambda$reload$1(Lnet/minecraft/client/resources/model/BlockStateModelLoader$LoadedModels;)Lit/unimi/dsi/fastutil/objects/Object2IntMap;")),
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Ljava/util/concurrent/CompletableFuture;thenApplyAsync(Ljava/util/function/Function;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private void gander$invokeArchetypeDiscovery(
        PreparableReloadListener.PreparationBarrier p_249079_, ResourceManager p_251134_, Executor backgroundExecutor, Executor p_249221_,
        CallbackInfoReturnable<CompletableFuture<Void>> ci,
        @Local UnbakedModel missingModel,
        @Local(ordinal = 3) CompletableFuture<BlockStateModelLoader.LoadedModels> blockStates,
        @Local(ordinal = 5) CompletableFuture<ModelDiscovery> modelDiscovery,
        @Share("archetypeDiscovery") LocalRef<CompletableFuture<ArchetypeDiscovery>> archetypeDiscovery)
    {
        archetypeDiscovery.set(
            CompletableFuture.allOf(blockStates, modelDiscovery)
                .thenApplyAsync(discovery -> gander$collectModelArchetypes(
                    Profiler.get(),
                    missingModel,
                    modelDiscovery.join().getReferencedModels(),
                    blockStates.join()),
                    backgroundExecutor));
    }

    @Unique
    private ArchetypeDiscovery gander$collectModelArchetypes(
        ProfilerFiller profiler,
        UnbakedModel missingModel,
        Map<ResourceLocation, UnbakedModel> referencedModels,
        BlockStateModelLoader.LoadedModels loadedBlockStates)
    {
        var archetypeDiscovery = new ArchetypeDiscovery(
            referencedModels,
            missingModel,
            loadedBlockStates);
        archetypeDiscovery.discoverArchetypes(profiler);
        return archetypeDiscovery;
    }

    // After scheduling the load of atlases, we need to schedule building index maps for them.
    @Inject(
        require = 1,
        method = "reload",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/resources/model/AtlasSet;scheduleLoad(Lnet/minecraft/server/packs/resources/ResourceManager;ILjava/util/concurrent/Executor;)Ljava/util/Map;"))
    public void gander$invokeTextureBake(
        PreparableReloadListener.PreparationBarrier p_249079_, ResourceManager p_251134_, Executor backgroundExecutor, Executor p_249221_,
        CallbackInfoReturnable<CompletableFuture<Void>> ci,
        @Local Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> atlasMap,
        @Share("atlasIndexing") LocalRef<Map<ResourceLocation, CompletableFuture<IndexResult>>> atlasIndexing)
    {

        atlasIndexing.set(gander$atlasIndex.bakeAtlasIndices(atlasMap, backgroundExecutor));
    }

    // Ensure all of our additional tasks are complete before attempting to bake models.
    @ModifyExpressionValue(
        require = 1,
        method = "reload",
        slice = @Slice(
            from = @At(
                value = "GANDER:INDY",
                target = "Lnet/minecraft/client/resources/model/ModelManager;lambda$reload$0(Lnet/minecraft/client/resources/model/UnbakedModel;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/lang/Void;)Lnet/minecraft/client/resources/model/ModelDiscovery;"),
            to = @At(
                value = "GANDER:INDY",
                target = "Lnet/minecraft/client/resources/model/ModelManager;lambda$reload$5(Ljava/util/Map;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Lnet/minecraft/client/resources/model/UnbakedModel;Ljava/util/concurrent/CompletableFuture;Ljava/lang/Void;)Lnet/minecraft/client/resources/model/ModelManager$ReloadState;")),
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;concat(Ljava/util/stream/Stream;Ljava/util/stream/Stream;)Ljava/util/stream/Stream;"))
    private Stream<CompletableFuture<?>> gander$includeAtlasIndexingResults(
        Stream<CompletableFuture<?>> futures,
        @Share("archetypeDiscovery") LocalRef<CompletableFuture<ArchetypeDiscovery>> archetypeDiscovery,
        @Share("atlasIndexing") LocalRef<Map<ResourceLocation, CompletableFuture<IndexResult>>> atlasIndexing)
    {
        return Stream.concat(
            futures,
            Stream.concat(
                Stream.of(archetypeDiscovery.get()),
                atlasIndexing.get().values().stream()));
    }

    // Bake the discovered archetypes in parallel with vanilla's bakery.
    @ModifyExpressionValue(
        require = 1,
        method = "reload",
        slice = @Slice(
            from = @At(
                value = "GANDER:INDY",
                target = "Lnet/minecraft/client/resources/model/ModelManager;lambda$reload$0(Lnet/minecraft/client/resources/model/UnbakedModel;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/lang/Void;)Lnet/minecraft/client/resources/model/ModelDiscovery;"),
            to = @At(
                value = "GANDER:INDY",
                target = "Lnet/minecraft/client/resources/model/ModelManager;lambda$reload$5(Ljava/util/Map;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Ljava/util/concurrent/CompletableFuture;Lnet/minecraft/client/resources/model/UnbakedModel;Ljava/util/concurrent/CompletableFuture;Ljava/lang/Void;)Lnet/minecraft/client/resources/model/ModelManager$ReloadState;")),
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;allOf([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<?> gander$invokeBakeArchetypes(CompletableFuture<Void> future,
        @Local(argsOnly = true, ordinal = 0) Executor backgroundExecutor,
        @Local(argsOnly = true, ordinal = 1) Executor foregroundExecutor,
        @Local(argsOnly = true) PreparableReloadListener.PreparationBarrier preparationBarrier,
        @Local(ordinal = 3) CompletableFuture<BlockStateModelLoader.LoadedModels> blockStateModels,
        @Share("archetypeDiscovery") LocalRef<CompletableFuture<ArchetypeDiscovery>> archetypeDiscovery,
        @Share("atlasIndexing") LocalRef<Map<ResourceLocation, CompletableFuture<IndexResult>>> atlasIndexing,
        @Share("archetypeBakery") LocalRef<CompletableFuture<Void>> archetypeBakery)
    {
        var tsk = future.thenApplyAsync(Void -> {
            var indexedAtlases = atlasIndexing.get().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().join()));
            var discovery = archetypeDiscovery.get().join();

            var bakery = new ArchetypeBakery(
                discovery.getArchetypes(),
                discovery.getReferencedArchetypes(),
                discovery.getBlockStateArchetypes(),
                blockStateModels.join().models());
            gander$archetypeBakery.set(bakery);
            return gander$bakeArchetypes(Profiler.get(), indexedAtlases, bakery);
        }, backgroundExecutor)
            // TODO: do we need to wait for upload here?
            .thenCompose(preparationBarrier::wait)
            .thenAcceptAsync(state -> gander$apply(state, Profiler.get()), foregroundExecutor);

        archetypeBakery.set(tsk);

        return future;
    }

    @Unique
    private ModelManager$ReloadState$Gander gander$bakeArchetypes(ProfilerFiller profiler,
        Map<ResourceLocation, IndexResult> indexedAtlases,
        ArchetypeBakery bakery)
    {
        profiler.push("archetype_baking");
        var result = bakery.bakeArchetypes(profiler);
        profiler.popPush("archetype_dispatch");
        var dispatch = gander$createBlockStateToArchetypeDispatch(result.blockStateArchetypes());
        profiler.pop();
        return new ModelManager$ReloadState$Gander(result, indexedAtlases, dispatch);
    }

    @Unique
    private Map<BlockState, DisplayableMeshGroup> gander$createBlockStateToArchetypeDispatch(
        Map<ModelResourceLocation, DisplayableMeshGroup> map)
    {
        var result = new IdentityHashMap<BlockState, DisplayableMeshGroup>();

        for (var block : BuiltInRegistries.BLOCK)
        {
            block.getStateDefinition().getPossibleStates().forEach(state -> {
                var rl = state.getBlock().builtInRegistryHolder().key().location();
                var mrl = BlockModelShaper.stateToModelLocation(rl, state);
                var meshGroup = map.get(mrl);

                if (meshGroup == null)
                    gander$LOGGER.warn("Missing displayable mesh group for block state: '{}'", mrl);
                else
                    result.put(state, meshGroup);
            });
        }

        return result;
    }

    @Unique
    private void gander$apply(ModelManager$ReloadState$Gander reloadState, ProfilerFiller profiler)
    {
        profiler.push("archetype_upload");
        reloadState.atlasIndices().values().forEach(IndexResult::upload);
        var bakingResult = reloadState.archetypeBakingResult();
        gander$bakedBlockStateArchetypes = bakingResult.blockStateArchetypes();
        // TODO: standalone mesh groups
        //gander$bakedArchetypes = reloadState.archetypeBakingResult().bakedArchetypes();
        profiler.popPush("archetype_cache");
        ((BlockModelShaper$Gander)blockModelShaper).gander$replaceCache(reloadState.blockStateCache());

        profiler.pop();
    }

    // Ensure our baking returns as well as vanilla's.
    @ModifyReturnValue(
        require = 1,
        method = "reload",
        at = @At("RETURN"))
    private CompletableFuture<Void> gander$completeArchetypeBaking(
        CompletableFuture<Void> original,
        @Share("archetypeBakery") LocalRef<CompletableFuture<Void>> archetypeBakery)
    {
        return CompletableFuture.allOf(original, archetypeBakery.get());
    }

    @Override
    public AtlasIndex gander$getAtlasIndex()
    {
        return gander$atlasIndex;
    }
}
