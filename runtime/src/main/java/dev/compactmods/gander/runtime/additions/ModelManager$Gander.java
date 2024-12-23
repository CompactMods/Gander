package dev.compactmods.gander.runtime.additions;

import dev.compactmods.gander.runtime.baked.AtlasIndex;
import net.minecraft.client.resources.model.ModelManager;

public interface ModelManager$Gander
{
    //ArchetypeBakery gander$getArchetypeBakery();
    AtlasIndex gander$getAtlasIndex();

    default BlockModelShaper$Gander gander$getBlockModelShaper()
    {
        // Cursed, but it should work :D
        return (BlockModelShaper$Gander)((ModelManager)this).getBlockModelShaper();
    }
}
