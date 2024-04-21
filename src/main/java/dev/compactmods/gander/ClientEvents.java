package dev.compactmods.gander;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.ponder.level.WrappedClientWorld;
import dev.compactmods.gander.render.SuperRenderTypeBuffer;
import dev.compactmods.gander.utility.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEvents {

	@SubscribeEvent
	public static void onTick(TickEvent.ClientTickEvent event) {
		if (!isGameActive())
			return;

		if (event.phase == TickEvent.Phase.START) {
			return;
		}

		AnimationTickHolder.tick();
		CreateClient.OUTLINER.tickOutlines();
	}

	@SubscribeEvent
	public static void onLoadWorld(LevelEvent.Load event) {
		LevelAccessor world = event.getLevel();
		if (world.isClientSide() && world instanceof ClientLevel && !(world instanceof WrappedClientWorld)) {
			CreateClient.invalidateRenderers();
			AnimationTickHolder.reset();
		}
	}

	@SubscribeEvent
	public static void onUnloadWorld(LevelEvent.Unload event) {
		if (!event.getLevel()
			.isClientSide())
			return;
		CreateClient.invalidateRenderers();
		AnimationTickHolder.reset();
	}

	@SubscribeEvent
	public static void onRenderWorld(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		PoseStack ms = event.getPoseStack();
		ms.pushPose();
		SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
		float partialTicks = AnimationTickHolder.getPartialTicks();
		Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera()
			.getPosition();

		CreateClient.OUTLINER.renderOutlines(ms, buffer, camera, partialTicks);

		buffer.draw();
		RenderSystem.enableCull();
		ms.popPose();
	}

	protected static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}

	@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ModBusEvents {

		@SubscribeEvent
		public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
			event.registerReloadListener(CreateClient.RESOURCE_RELOAD_LISTENER);
		}
	}

}
