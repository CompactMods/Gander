package dev.compactmods.gander.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.joml.Vector3f;

public record StructureSceneDataRequest(ResourceLocation templateID, boolean inWorld,
                                        Vector3f renderLocation) implements CustomPacketPayload {
    public static final Type<StructureSceneDataRequest> ID = new Type<>(new ResourceLocation("gander", "scene_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureSceneDataRequest> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, StructureSceneDataRequest::templateID,
        ByteBufCodecs.BOOL, StructureSceneDataRequest::inWorld,
        ByteBufCodecs.VECTOR3F, StructureSceneDataRequest::renderLocation,
        StructureSceneDataRequest::new
    );

    public StructureSceneDataRequest(ResourceLocation templateID, boolean inWorld) {
        this(templateID, inWorld, Vec3.ZERO.toVector3f());
    }


    public static final IPayloadHandler<StructureSceneDataRequest> HANDLER = (req, ctx) -> {
        ctx.enqueueWork(() -> {
            final var templateManager = ServerLifecycleHooks.getCurrentServer().getStructureManager();
            templateManager.get(req.templateID).ifPresent(t -> {
                if (req.inWorld)
                    ctx.reply(new RenderInWorldForStructureRequest(Component.literal(req.templateID.toString()), t, req.renderLocation));
                else
                    ctx.reply(new OpenGanderUiForStructureRequest(Component.literal(req.templateID.toString()), t));
            });
        });
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
