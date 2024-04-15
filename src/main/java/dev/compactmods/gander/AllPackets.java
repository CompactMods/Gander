//package dev.compactmods.gander;
//
//import java.util.function.BiConsumer;
//import java.util.function.Function;
//import java.util.function.Supplier;
//
//import dev.compactmods.gander.infrastructure.command.HighlightCommand;
//import dev.compactmods.gander.networking.SimplePacketBase;
//import dev.compactmods.gander.ponder.core.OpenPonderPacket;
//
//import dev.compactmods.gander.utility.ServerSpeedProvider;
//
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraft.resources.ResourceLocation;
//
//public enum AllPackets {
//
//	SERVER_SPEED(ServerSpeedProvider.Packet.class, ServerSpeedProvider.Packet::new, PLAY_TO_CLIENT),
//	BLOCK_HIGHLIGHT(HighlightCommand.HighlightPacket.class, HighlightCommand.HighlightPacket::new, PLAY_TO_CLIENT),
//	OPEN_PONDER_SCENE(OpenPonderPacket.class, OpenPonderPacket::new, PLAY_TO_CLIENT)
//
//	;
//
//	public static final ResourceLocation CHANNEL_NAME = Create.asResource("main");
//	public static final int NETWORK_VERSION = 3;
//	public static final String NETWORK_VERSION_STR = String.valueOf(NETWORK_VERSION);
//	private static SimpleChannel channel;
//
//	private final PacketType<?> packetType;
//
//	<T extends SimplePacketBase> AllPackets(Class<T> type, Function<FriendlyByteBuf, T> factory,
//		NetworkDirection direction) {
//		packetType = new PacketType<>(type, factory, direction);
//	}
//
//	public static void registerPackets() {
//		channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_NAME)
//			.serverAcceptedVersions(NETWORK_VERSION_STR::equals)
//			.clientAcceptedVersions(NETWORK_VERSION_STR::equals)
//			.networkProtocolVersion(() -> NETWORK_VERSION_STR)
//			.simpleChannel();
//
//		for (AllPackets packet : values())
//			packet.packetType.register();
//	}
//
//	public static SimpleChannel getChannel() {
//		return channel;
//	}
//
//	private static class PacketType<T extends SimplePacketBase> {
//		private static int index = 0;
//
//		private final BiConsumer<T, FriendlyByteBuf> encoder;
//		private final Function<FriendlyByteBuf, T> decoder;
//		private final BiConsumer<T, Supplier<Context>> handler;
//		private final Class<T> type;
//		private final NetworkDirection direction;
//
//		private PacketType(Class<T> type, Function<FriendlyByteBuf, T> factory, NetworkDirection direction) {
//			encoder = T::write;
//			decoder = factory;
//			handler = (packet, contextSupplier) -> {
//				Context context = contextSupplier.get();
//				if (packet.handle(context)) {
//					context.setPacketHandled(true);
//				}
//			};
//			this.type = type;
//			this.direction = direction;
//		}
//
//		private void register() {
//			getChannel().messageBuilder(type, index++, direction)
//				.encoder(encoder)
//				.decoder(decoder)
//				.consumerNetworkThread(handler)
//				.add();
//		}
//	}
//
//}
