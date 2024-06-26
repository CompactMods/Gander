package dev.compactmods.gander.render.baked.model.archetype;

import dev.compactmods.gander.render.baked.BakedMesh;
import dev.compactmods.gander.render.baked.model.block.BlockModelBaker;
import dev.compactmods.gander.render.baked.model.composite.CompositeModelBaker;
import dev.compactmods.gander.render.baked.model.obj.ObjModelBaker;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.obj.ObjModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public final class ArchetypeBaker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchetypeBaker.class);

    private ArchetypeBaker() { }

    public static ModelResourceLocation computeMeshName(
        ModelResourceLocation original)
    {
        if (original.getVariant().isEmpty())
            return new ModelResourceLocation(
                original.id(),
                "gander_archetype");
        else if (original.getVariant().startsWith("gander_archetype"))
            return original;
        else
            return new ModelResourceLocation(
                original.id(),
                "gander_archetype/" + original.getVariant());
    }

    public static ModelResourceLocation computeMeshName(
        ModelResourceLocation original,
        String variant)
    {
        if (original.getVariant().isEmpty())
            return new ModelResourceLocation(
                original.id(),
                "gander_archetype/" + variant);
        else if (original.getVariant().startsWith("gander_archetype"))
            return new ModelResourceLocation(
                original.id(),
                original.getVariant() + "/" + variant);
        else
            return new ModelResourceLocation(
                original.id(),
                "gander_archetype/" + original.getVariant() + "/" + variant);
    }

    // TODO: this should support mods registering their own geometry types
    public static Map<ModelResourceLocation, BakedMesh> bakeArchetypes(
        ModelResourceLocation originalName,
        UnbakedModel model)
    {
        var result = switch (model)
        {
            case BlockModel block ->
                block.customData.hasCustomGeometry()
                    ? dispatchBakeCustomGeometry(
                        originalName,
                        block,
                        Objects.requireNonNull(
                            block.customData.getCustomGeometry()))
                    : BlockModelBaker.bakeBlockModel(originalName, block);
            default -> throw new IllegalStateException("Unexpected value: " + model);
        };

        if (result == null && LOGGER.isTraceEnabled())
            LOGGER.trace("Could not bake mesh {}", originalName);

        return result;
    }

    // TODO: this should support mods registering their own geometry types
    private static Map<ModelResourceLocation, BakedMesh> dispatchBakeCustomGeometry(
        ModelResourceLocation originalName,
        BlockModel model,
        IUnbakedGeometry<?> geometry)
    {
        var result = switch (geometry)
        {
            case CompositeModel composite ->
                CompositeModelBaker.bakeCompositeModel(originalName, model, composite);
            case ObjModel obj ->
                ObjModelBaker.bakeObjModel(originalName, model, obj);
            default ->
            {
                LOGGER.error("Unsupported custom geometry type {}", geometry.getClass());
                yield null;
            }
        };

        if (result == null && LOGGER.isDebugEnabled())
            LOGGER.debug("Could not bake custom geometry {}", originalName);

        return result;
    }
}
