package dev.compactmods.gander.runtime.mixin;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import com.llamalad7.mixinextras.sugar.Local;

import com.llamalad7.mixinextras.sugar.Share;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import dev.compactmods.gander.runtime.baked.model.ArchetypeBakery;
import dev.compactmods.gander.runtime.baked.model.ArchetypeDiscovery;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin
{
    @Unique
    @Final
    public AtomicReference<ArchetypeBakery> _archetypeBakery;

    @Inject(
        method = "<init>",
        at = @At("RETURN"))
    private void gander$initializeFields(TextureManager textureManager, BlockColors blockColors, int maxMipmapLevels, CallbackInfo ci)
    {
        _archetypeBakery = new AtomicReference<>();
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
        PreparableReloadListener.PreparationBarrier p_249079_, ResourceManager p_251134_, Executor p_250550_, Executor p_249221_,
        CallbackInfoReturnable<CompletableFuture<Void>> ci,
        @Local UnbakedModel missingModel,
        @Local(ordinal = 3) CompletableFuture<BlockStateModelLoader.LoadedModels> blockStates,
        @Local(ordinal = 5) CompletableFuture<ModelDiscovery> modelDiscovery,
        @Share("archetypeDiscovery") LocalRef<CompletableFuture<ArchetypeDiscovery>> archetypeDiscovery)
    {
        archetypeDiscovery.set(
            CompletableFuture.allOf(blockStates, modelDiscovery)
                .thenApply(discovery -> gander$collectModelArchetypes(
                    Profiler.get(),
                    missingModel,
                    modelDiscovery.join().getReferencedModels(),
                    blockStates.join())));
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
    // TODO: maybe move this to an AtlasSet mixin?
    @Inject(
        require = 1,
        method = "reload",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/resources/model/AtlasSet;scheduleLoad(Lnet/minecraft/server/packs/resources/ResourceManager;ILjava/util/concurrent/Executor;)Ljava/util/Map;"))
    public void gander$invokeTextureBake(
        PreparableReloadListener.PreparationBarrier p_249079_, ResourceManager p_251134_, Executor p_250550_, Executor p_249221_,
        CallbackInfoReturnable<CompletableFuture<Void>> ci,
        @Local Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> atlasMap,
        @Share("atlasIndexing") LocalRef<Map<ResourceLocation, CompletableFuture<?>>> atlasIndexing)
    {
        atlasIndexing.set(Map.of());
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
        @Share("atlasIndexing") LocalRef<Map<ResourceLocation, CompletableFuture<?>>> atlasIndexing)
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
        @Local(ordinal = 3) CompletableFuture<BlockStateModelLoader.LoadedModels> blockStateModels,
        @Share("archetypeDiscovery") LocalRef<CompletableFuture<ArchetypeDiscovery>> archetypeDiscovery,
        @Share("atlasIndexing") LocalRef<Map<ResourceLocation, CompletableFuture<?>>> atlasIndexing,
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
            _archetypeBakery.set(bakery);
            return gander$bakeArchetypes(Profiler.get(), indexedAtlases, bakery);
        }, backgroundExecutor)
            .thenAcceptAsync(p -> {}, foregroundExecutor);

        archetypeBakery.set(tsk);

        return future;
    }

    @Unique
    private int gander$bakeArchetypes(ProfilerFiller profiler,
        Map<ResourceLocation, ?> indexedAtlases,
        ArchetypeBakery bakery)
    {
        var result = bakery.bakeArchetypes(profiler);

        return 0;
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
}
