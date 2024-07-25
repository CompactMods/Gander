package dev.compactmods.gander.render.baked.model.archetype;

import net.minecraft.client.resources.model.ModelResourceLocation;

public final class Archetypes
{
    private Archetypes() { }

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
}
