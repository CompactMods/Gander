package dev.compactmods.gander.render.baked.section;

import com.mojang.math.Transformation;
import dev.compactmods.gander.render.baked.model.BakedMesh;
import dev.compactmods.gander.render.baked.model.DisplayableMesh;
//import dev.compactmods.gander.render.baked.model.ModelRebaker;
import dev.compactmods.gander.render.baked.model.material.MaterialInstance;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*public final class SectionBaker
{
    private record InstanceInfo(Transformation transform, List<MaterialInstance> materials)
    { }

    private SectionBaker() { }

    public static void bake(Level level, SectionPos section)
    {
        // The map of render passes, to the map of meshes
        var renderPasses = section.blocksInside()
            .flatMap(pos -> modelsAt(level, pos))
            .collect(Collectors.groupingBy(DisplayableMesh::renderType,
                Collectors.groupingBy(DisplayableMesh::mesh)));

        for (var renderPass : renderPasses.entrySet())
        {
            for (var mesh : renderPass.getValue().entrySet())
            {
                buildBuffers(mesh.getKey(), mesh.getValue());
            }
        }
    }

    private static void buildBuffers(
        BakedMesh mesh,
        Collection<DisplayableMesh> instances)
    {
        instances.stream()
            .forEach(it -> {
                var materials = new ArrayList<MaterialInstance>(mesh.materialIndexes().length);
                for (var index : it.mesh().materialIndexes())
                {
                    var parent = it.mesh().materials().get(index);
                    var potentials = it.materialInstances().get(parent);
                    if (potentials.isEmpty())
                    {
                        materials.add(MaterialInstance.MISSING);
                    }

                    //potentials.stream().findFirst().get()
                }
            });
    }

    // TODO: should this return a thing
    private static Stream<DisplayableMesh> modelsAt(
        Level level,
        BlockPos pos)
    {
        var modelPos = BlockModelShaper.stateToModelLocation(level.getBlockState(pos));
        return ModelRebaker.getArchetypeMeshes(modelPos)
            .stream()
            .map(it -> new DisplayableMesh(
                it.name(),
                it.mesh(),
                it.renderType(),
                translate(it.transform(), pos),
                it.weight(),
                it.materialInstanceSupplier()));
    }

    // Instead of using compose() we do this to preserve accuracy
    private static Transformation translate(
        Transformation original,
        BlockPos position)
    {
        // Annoyingly, there's no way to just directly do this...
        var packed = SectionPos.sectionRelativePos(position);
        var relX = SectionPos.sectionRelativeX(packed);
        var relY = SectionPos.sectionRelativeY(packed);
        var relZ = SectionPos.sectionRelativeZ(packed);

        return new Transformation(
            original.getTranslation()
                .add(relX, relY, relZ),
            original.getLeftRotation(),
            original.getScale(),
            original.getRightRotation());
    }
}*/