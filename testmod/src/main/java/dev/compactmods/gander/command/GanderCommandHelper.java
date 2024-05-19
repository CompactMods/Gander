package dev.compactmods.gander.command;

import com.mojang.brigadier.context.CommandContext;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.level.chunk.VirtualChunkGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class GanderCommandHelper {

    static @Nullable StructureTemplate generateStructureWithFloor(CommandContext<CommandSourceStack> ctx, BlockState floor, Holder.Reference<Structure> structure, long seed) {

        final var server = ctx.getSource().getServer();

        RegistryAccess regAccess = ctx.getSource().registryAccess();

        VirtualLevel level = new VirtualLevel(regAccess, false);
        final var biomeReg = regAccess
            .registry(Registries.BIOME)
            .get();

        final var theVoid = structure.value().biomes().getRandomElement(level.random).orElseGet(() -> biomeReg.getHolderOrThrow(Biomes.PLAINS));
        final var biomeSource = new FixedBiomeSource(theVoid);
        final var chunkGenerator = new VirtualChunkGenerator(biomeSource);

        var pRandomState = RandomState.create(regAccess.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value(), regAccess.lookupOrThrow(Registries.NOISE), seed);
        final var structureStart = structure.value().generate(
            regAccess,
            chunkGenerator,
            biomeSource,
            pRandomState,
            server.getStructureManager(),
            seed,
            ChunkPos.ZERO,
            0,
            level,
            p_214580_ -> true
        );

        if (!structureStart.isValid()) {
            return null;
        }

        // GENERATE
        BoundingBox boundingbox = structureStart.getBoundingBox();
        level.setBounds(boundingbox);

        var lowestY = boundingbox.minY();

        for (var y = boundingbox.minY(); y < boundingbox.maxY(); y++) {
            if (level.setBlock(new BlockPos(0, y, 0), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)) {
                lowestY = y;
                break;
            }
        }

        for (var x = boundingbox.minX(); x < boundingbox.maxX(); x++) {
            for (var z = boundingbox.minZ(); z < boundingbox.maxZ(); z++) {
                level.setBlock(new BlockPos(x, lowestY, z), floor, Block.UPDATE_CLIENTS);
            }
        }

        ChunkPos startChunk = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.minX()), SectionPos.blockToSectionCoord(boundingbox.minZ()));
        ChunkPos endChunk = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.maxX()), SectionPos.blockToSectionCoord(boundingbox.maxZ()));

        var structureCheck = new StructureCheck(
            (pChunkPos, pVisitor) -> CompletableFuture.completedFuture(null),
            regAccess,
            server.getStructureManager(),
            Level.OVERWORLD,
            chunkGenerator,
            pRandomState,
            level,
            biomeSource,
            seed,
            server.getFixerUpper()
        );
        var structureManager = new StructureManager(level, new WorldOptions(seed, true, false), structureCheck);

        ChunkPos.rangeClosed(startChunk, endChunk)
            .forEach(
                chunkPos -> structureStart.placeInChunk(
                    level,
                    structureManager,
                    chunkGenerator,
                    level.getRandom(),
                    new BoundingBox(
                        chunkPos.getMinBlockX(),
                        boundingbox.minY(),
                        chunkPos.getMinBlockZ(),
                        chunkPos.getMaxBlockX(),
                        boundingbox.maxY(),
                        chunkPos.getMaxBlockZ()
                    ),
                    chunkPos
                )
            );

        final var finalStructure = new StructureTemplate();
        finalStructure.fillFromWorld(level, new BlockPos(
            boundingbox.minX(),
            boundingbox.minY(),
            boundingbox.minZ()
        ), boundingbox.getLength(), false, Blocks.AIR);
        return finalStructure;
    }

    static @NotNull StructureTemplate buildDebugStructure(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();

        var registryAccess = source.registryAccess();
        var level = new VirtualLevel(registryAccess, false) {
            @Override
            public int getMinBuildHeight() {
                return this.dimensionType().minY();
            }

            @Override
            public int getMaxBuildHeight() {
                return this.getMinBuildHeight() + this.getHeight();
            }
        };

        var maxX = 323;
        var maxZ = 327;
        var minY = DebugLevelSource.BARRIER_HEIGHT;
        var maxY = DebugLevelSource.HEIGHT;

        // DebugLevelSource.initValidStates();
        var bounds = new BoundingBox(0, minY, 0, maxX, maxY, maxZ);
        level.setBounds(bounds);
        var generator = new DebugLevelSource(registryAccess.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.THE_VOID));

        var maxChunkX = maxX / 16;
        var maxChunkZ = maxZ / 16;

        for (var x = 0; x < maxChunkX; x++) {
            for (var z = 0; z < maxChunkZ; z++) {
                var chunk = level.getChunk(x, z);
                generator.applyBiomeDecoration(level, chunk, null);
            }
        }

        var structure = new StructureTemplate();
        structure.fillFromWorld(level, new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()), new Vec3i(bounds.maxX(), bounds.maxY(), bounds.maxZ()), false, Blocks.AIR);
        return structure;
    }
}
