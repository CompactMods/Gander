package dev.compactmods.gander.core;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.level.chunk.VirtualChunkGenerator;
import dev.compactmods.gander.network.OpenGanderUiForDeferredStructureRequest;
import dev.compactmods.gander.network.OpenGanderUiForStructureRequest;
import dev.compactmods.gander.network.RenderInWorldForDeferredStructureRequest;
import dev.compactmods.gander.network.RenderInWorldForStructureRequest;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
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
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.command.EnumArgument;

public class GanderCommand {
	public static final SuggestionProvider<CommandSourceStack> ANY_STRUCTURE = (ctx, builder)
			-> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getStructureManager().listTemplates(), builder);

	public enum GanderTarget {
		screen, world
	}

	public static LiteralArgumentBuilder<CommandSourceStack> make(CommandBuildContext buildContext) {
		var scene = Commands.literal("scene")
            .then(Commands.argument("scene", ResourceLocationArgument.id())
                .suggests(ANY_STRUCTURE)
                .executes(GanderCommand::openTemplateScene)
            );

		var structure = Commands.literal("structure")
            .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                .executes(ctx -> openStructureSceneWithFloor(ctx, Blocks.AIR.defaultBlockState()))
                .then(Commands.argument("floor", BlockStateArgument.block(buildContext))
                    .executes(GanderCommand::openStructureScene)
                )
            );

		var debug = Commands.literal("debug")
            .executes(GanderCommand::generateDebug);

		var target = Commands.argument("target", EnumArgument.enumArgument(GanderTarget.class))
            .then(scene)
            .then(structure)
            .then(debug);

        return Commands.literal("gander")
			.requires(cs -> cs.hasPermission(Commands.LEVEL_ALL))
			.then(target);
	}

	private static int openTemplateScene(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var target = ctx.getArgument("target", GanderTarget.class);
        var sceneId = ResourceLocationArgument.getId(ctx, "scene");
        var player = ctx.getSource().getPlayerOrException();

        if (player instanceof FakePlayer)
            return -1;

        PacketDistributor.sendToPlayer(player,
            switch (target)
            {
                case screen -> new OpenGanderUiForDeferredStructureRequest(sceneId);
                case world -> new RenderInWorldForDeferredStructureRequest(sceneId);
            });

        return Command.SINGLE_SUCCESS;
    }

    private static int openStructureScene(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var floor = BlockStateArgument.getBlock(ctx, "floor").getState();
        return openStructureSceneWithFloor(ctx, floor);
    }

	private static int openStructureSceneWithFloor(CommandContext<CommandSourceStack> ctx, BlockState floor) throws CommandSyntaxException {
        var target = ctx.getArgument("target", GanderTarget.class);
        var structure = ResourceKeyArgument.getStructure(ctx, "structure");
        var player = ctx.getSource().getPlayerOrException();

		if (player instanceof FakePlayer)
			return -1;

		final var server = ctx.getSource().getServer();

		RegistryAccess regAccess = ctx.getSource().registryAccess();

		VirtualLevel level = new VirtualLevel(regAccess, false);
		var seed = level.random.nextLong();

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
			return -1;
		}

		// GENERATE
		BoundingBox boundingbox = structureStart.getBoundingBox();
		level.setBounds(boundingbox);

        var lowestY = boundingbox.minY();

        for(var y = boundingbox.minY(); y < boundingbox.maxY(); y++) {
            if(level.setBlock(new BlockPos(0, y, 0), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)) {
                lowestY = y;
                break;
            }
        }

        for(var x = boundingbox.minX(); x < boundingbox.maxX(); x++) {
            for(var z = boundingbox.minZ(); z < boundingbox.maxZ(); z++) {
                level.setBlock(new BlockPos(x, lowestY, z), floor, Block.UPDATE_CLIENTS);

                /*for(var y = lowestY; y < boundingbox.maxY(); y++) {
                    level.setBlock(new BlockPos(x, y, z), floor, Block.UPDATE_CLIENTS);
                }*/
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


		final var title = Component.literal("Generated: %s, Seed: %d".formatted(structure.key().location(), seed));
		PacketDistributor.sendToPlayer(player,
				switch (target)
				{
					case screen -> new OpenGanderUiForStructureRequest(title, finalStructure);
					case world -> {
						player.displayClientMessage(Component.literal("Seed: %d".formatted(seed))
								.withStyle(style -> style
										.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy Seed to Clipboard")))
										.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, seed + ""))
								), false);
						yield new RenderInWorldForStructureRequest(title, finalStructure);
					}
				});

		return Command.SINGLE_SUCCESS;
	}

    private static int generateDebug(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var target = ctx.getArgument("target", GanderTarget.class);
        var player = ctx.getSource().getPlayerOrException();

        if (player instanceof FakePlayer)
            return -1;

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

        for(var x = 0; x < maxChunkX; x++) {
            for(var z = 0; z < maxChunkZ; z++) {
                var chunk = level.getChunk(x, z);
                generator.applyBiomeDecoration(level, chunk, null);
            }
        }

        var structure = new StructureTemplate();
        structure.fillFromWorld(level, new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()), new Vec3i(bounds.maxX(), bounds.maxY(), bounds.maxZ()), false, Blocks.AIR);

        PacketDistributor.sendToPlayer(player,
            switch (target)
            {
                case screen -> new OpenGanderUiForStructureRequest(Component.literal("Generated: minecraft:debug"), structure);
                case world -> new RenderInWorldForStructureRequest(Component.literal("Generated: minecraft:debug"), structure);
            });

        return Command.SINGLE_SUCCESS;
    }
}
