package dev.compactmods.gander.ponder;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import dev.compactmods.gander.GanderLib;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class PonderRegistry {

	// Map from item IDs to storyboard entries
	public static final Map<ResourceLocation, PonderStoryBoardEntry> ALL = new HashMap<>();

	public static void addStoryBoard(PonderStoryBoardEntry entry) {
		synchronized (ALL) {
			ALL.putIfAbsent(entry.id(), entry);
		}
	}

	public static PonderScene compile(ResourceLocation id) {
		var storyboard = ALL.get(id);
		return compile(storyboard);
	}

	public static PonderScene compile(PonderStoryBoardEntry sb) {
		StructureTemplate activeTemplate = loadSchematic(sb.getSchematicLocation());
		PonderLevel world = new PonderLevel(BlockPos.ZERO, Minecraft.getInstance().level);
		activeTemplate.placeInWorld(world, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(), RandomSource.create(), Block.UPDATE_CLIENTS);
		world.createBackup();
		PonderScene scene = compileScene(sb, world);
		scene.begin();

		return scene;
	}

	public static PonderScene compileScene(PonderStoryBoardEntry sb, PonderLevel world) {
		PonderScene scene = new PonderScene(world, sb.id());
		SceneBuilder builder = scene.builder();
		sb.getBoard().program(builder, scene.getSceneBuildingUtil());
		return scene;
	}

	public static StructureTemplate loadSchematic(ResourceLocation location) {
		return loadSchematic(Minecraft.getInstance().getResourceManager(), location);
	}

	public static StructureTemplate loadSchematic(ResourceManager resourceManager, ResourceLocation location) {
		String namespace = location.getNamespace();
		String path = "ponder/" + location.getPath() + ".nbt";
		ResourceLocation location1 = new ResourceLocation(namespace, path);

		Optional<Resource> optionalResource = resourceManager.getResource(location1);
		if (optionalResource.isPresent()) {
			Resource resource = optionalResource.get();
			try (InputStream inputStream = resource.open()) {
				return loadSchematic(inputStream);
			} catch (IOException e) {
				GanderLib.LOGGER.error("Failed to read ponder schematic: " + location1, e);
			}
		} else {
			GanderLib.LOGGER.error("Ponder schematic missing: " + location1);
		}
		return new StructureTemplate();
	}

	public static StructureTemplate loadSchematic(InputStream resourceStream) throws IOException {
		StructureTemplate t = new StructureTemplate();
		DataInputStream stream =
			new DataInputStream(new BufferedInputStream(new GZIPInputStream(resourceStream)));
		CompoundTag nbt = NbtIo.read(stream, NbtAccounter.create(0x20000000L));
		t.load(Minecraft.getInstance().level.holderLookup(Registries.BLOCK), nbt);
		return t;
	}
}
