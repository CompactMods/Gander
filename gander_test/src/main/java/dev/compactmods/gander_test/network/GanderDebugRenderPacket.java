package dev.compactmods.gander_test.network;

import dev.compactmods.gander.runtime.baked.model.ModelRebaker;
import dev.compactmods.gander.runtime.baked.section.SectionBaker;
import dev.compactmods.gander_test.GanderTestMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public record GanderDebugRenderPacket(BlockState state) implements CustomPacketPayload
{
    private static Logger LOGGER = LoggerFactory.getLogger(GanderDebugRenderPacket.class);

    public static final Type<GanderDebugRenderPacket> ID = new Type<>(GanderTestMod.asResource("debug"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GanderDebugRenderPacket> STREAM_CODEC
        = StreamCodec.composite(
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), GanderDebugRenderPacket::state,
            GanderDebugRenderPacket::new);

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }

    public static void handle(GanderDebugRenderPacket pkt, IPayloadContext ctx)
    {
        if (FMLEnvironment.dist.isClient())
        {
            Client.handle(pkt.state());
        }
    }

    private static class Client
    {
        public static void handle(BlockState state)
        {
            var mc = Minecraft.getInstance();
            SectionBaker.bake(mc.level, SectionPos.of(0, 0, 0), ModelRebaker.of(mc.getModelManager()));
        }
    }
}