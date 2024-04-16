package dev.compactmods.gander;

import dev.compactmods.gander.gui.UIRenderHelper;
import dev.compactmods.gander.outliner.Outliner;
import dev.compactmods.gander.ponder.element.WorldSectionElement;
import dev.compactmods.gander.render.CachedBufferer;
import dev.compactmods.gander.render.SuperByteBufferCache;
import dev.compactmods.gander.utility.Components;

import net.minecraft.ChatFormatting;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class CreateClient {

	public static final SuperByteBufferCache BUFFER_CACHE = new SuperByteBufferCache();
	public static final Outliner OUTLINER = new Outliner();
	public static final ClientResourceReloadListener RESOURCE_RELOAD_LISTENER = new ClientResourceReloadListener();

	public static void onCtorClient(IEventBus modEventBus) {
		modEventBus.addListener(CreateClient::clientInit);
	}

	public static void clientInit(final FMLClientSetupEvent event) {
		BUFFER_CACHE.registerCompartment(CachedBufferer.GENERIC_BLOCK);
		BUFFER_CACHE.registerCompartment(WorldSectionElement.DOC_WORLD_SECTION, 20);

		DebugScenes.registerAll();
		UIRenderHelper.init();
	}

	public static void invalidateRenderers() {
		BUFFER_CACHE.invalidate();
	}

	public static void checkGraphicsFanciness() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;

		if (mc.options.graphicsMode().get() != GraphicsStatus.FABULOUS)
			return;

		MutableComponent text = ComponentUtils.wrapInSquareBrackets(Components.literal("WARN"))
			.withStyle(ChatFormatting.GOLD)
			.append(Components.literal(
				" Some of Create's visual features will not be available while Fabulous graphics are enabled!"))
			.withStyle(style -> style
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/create dismissFabulousWarning"))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
					Components.literal("Click here to disable this warning"))));

		mc.player.displayClientMessage(text, false);
	}

}
