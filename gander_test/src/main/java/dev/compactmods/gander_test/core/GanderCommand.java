package dev.compactmods.gander_test.core;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.compactmods.gander.level.chunk.VirtualChunkGenerator;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander_test.network.OpenGanderUiForDeferredStructureRequest;
import dev.compactmods.gander_test.network.OpenGanderUiForStructureRequest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class GanderCommand {
	public static final SuggestionProvider<CommandSourceStack> ANY_STRUCTURE = (ctx, builder)
			-> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getStructureManager().listTemplates(), builder);

	public static LiteralArgumentBuilder<CommandSourceStack> make() {
		var root = Commands.literal("gander")
				.requires(cs -> cs.hasPermission(0));

		var scene = Commands.literal("scene")
				.then(Commands.argument("scene", ResourceLocationArgument.id())
						.suggests(ANY_STRUCTURE)
						.executes(ctx -> openTemplateScene(ResourceLocationArgument.getId(ctx, "scene"), ctx.getSource().getPlayerOrException()))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(cs -> cs.hasPermission(2))
								.executes(ctx -> openTemplateScene(ResourceLocationArgument.getId(ctx, "scene"), EntityArgument.getPlayers(ctx, "targets")))
						)
				);

		var structure = Commands.literal("structure")
				.then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
						.executes(ctx -> openStructureScene(ctx, ResourceKeyArgument.getStructure(ctx, "structure"), ctx.getSource().getPlayerOrException()))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(cs -> cs.hasPermission(2))
								.executes(ctx -> openTemplateScene(ResourceLocationArgument.getId(ctx, "scene"), EntityArgument.getPlayers(ctx, "targets")))
						)
				);

		var debug = Commands.literal("debug")
				.executes(ctx -> generateDebug(ctx, ctx.getSource().getPlayerOrException()))
				.then(Commands.argument("targets", EntityArgument.players())
						.requires(cs -> cs.hasPermission(2))
						.executes(ctx -> generateDebug(ctx, EntityArgument.getPlayers(ctx, "targets")))
				);

		root.then(scene);
		root.then(structure);
		root.then(debug);
		return root;
	}

	private static int openTemplateScene(ResourceLocation sceneId, ServerPlayer player) {
		return openTemplateScene(sceneId, ImmutableList.of(player));
	}

	private static int openTemplateScene(ResourceLocation sceneId, Collection<? extends ServerPlayer> players) {
		for (ServerPlayer player : players) {
			if (player instanceof FakePlayer)
				continue;

			PacketDistributor.sendToPlayer(player, new OpenGanderUiForDeferredStructureRequest(sceneId));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int openStructureScene(CommandContext<CommandSourceStack> ctx, Holder.Reference<Structure> key, ServerPlayer player) {
		return openStructureScene(ctx, key, ImmutableList.of(player));
	}

	private static int openStructureScene(CommandContext<CommandSourceStack> ctx, Holder.Reference<Structure> key, Collection<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			if (player instanceof FakePlayer)
				continue;

			final var server = ctx.getSource().getServer();

			RegistryAccess regAccess = ctx.getSource().registryAccess();

			VirtualLevel level = new VirtualLevel(regAccess);
			var seed = level.random.nextLong();

			final var biomeReg = regAccess.lookupOrThrow(Registries.BIOME);

			final var theVoid = key.value().biomes().getRandomElement(level.random).orElseGet(() -> biomeReg.getOrThrow(Biomes.PLAINS));
			final var biomeSource = new FixedBiomeSource(theVoid);
			final var chunkGenerator = new VirtualChunkGenerator(biomeSource);

			var pRandomState = RandomState.create(regAccess.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value(), regAccess.lookupOrThrow(Registries.NOISE), seed);
			final var structureStart = key.value().generate(
                    key,
                    Level.OVERWORLD,
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
											level.getMinY(),
											chunkPos.getMinBlockZ(),
											chunkPos.getMaxBlockX(),
											level.getMaxY(),
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

			PacketDistributor.sendToPlayer(player, new OpenGanderUiForStructureRequest(Component.literal("Generated: " + key.key().location()), finalStructure));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int generateDebug(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		return generateDebug(ctx, Collections.singleton(player));
	}

	private static int generateDebug(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
		var source = ctx.getSource();
		var registryAccess = source.registryAccess();
		var level = new VirtualLevel(registryAccess);

		var maxX = 323;
		var maxZ = 327;

		// DebugLevelSource.initValidStates();
		var bounds = new BoundingBox(0, 60, 0, maxX, 70, maxZ);
		level.setBounds(bounds);
		var generator = new DebugLevelSource(registryAccess.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.THE_VOID));

		var maxChunkX = maxX / 16;
		var maxChunkZ = maxZ / 16;

		for(var x = 0; x < maxChunkX; x++) {
			for(var z = 0; z < maxChunkZ; z++) {
				var chunk = level.getChunk(x, z);
				generator.applyBiomeDecoration(level, chunk, null);
			}
		}

		var structure = new StructureTemplate();
		structure.fillFromWorld(level, new BlockPos(0, 60, 0), new Vec3i(maxX, 70, maxZ), false, Blocks.AIR);

		var payload = new OpenGanderUiForStructureRequest(Component.literal("Generated: minecraft:debug"), structure);
		players.forEach(player -> PacketDistributor.sendToPlayer(player, payload));
		return Command.SINGLE_SUCCESS;
	}
}
