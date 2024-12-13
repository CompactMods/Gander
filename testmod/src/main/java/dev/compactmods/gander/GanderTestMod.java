package dev.compactmods.gander;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.network.OpenGanderUiForDeferredStructureRequest;
import dev.compactmods.gander.network.OpenGanderUiForStructureRequest;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod("gander")
public class GanderTestMod {
	public static final String NET_VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			Gander.asResource("main"),
			() -> NET_VERSION,
			NET_VERSION::equals,
			NET_VERSION::equals
	);

	public GanderTestMod() {
		// Debug command, for showing the Gander UI
		MinecraftForge.EVENT_BUS.addListener(GanderCommand::register);

		// Open a UI for various scenes/structures
		CHANNEL.registerMessage(0, OpenGanderUiForDeferredStructureRequest.class, OpenGanderUiForDeferredStructureRequest::encode, OpenGanderUiForDeferredStructureRequest::new, OpenGanderUiForDeferredStructureRequest::handle);
		CHANNEL.registerMessage(1, OpenGanderUiForStructureRequest.class, OpenGanderUiForStructureRequest::encode, OpenGanderUiForStructureRequest::new, OpenGanderUiForStructureRequest::handle);

		// Actual scene data request - you will probably need a variant of this
		CHANNEL.registerMessage(2, StructureSceneDataRequest.class, StructureSceneDataRequest::encode, StructureSceneDataRequest::new, StructureSceneDataRequest::handle);
	}
}
