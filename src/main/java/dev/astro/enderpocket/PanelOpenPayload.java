package dev.astro.enderpocket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PanelOpenPayload(boolean open) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PanelOpenPayload> TYPE = new CustomPacketPayload.Type<>(EnderPocket.id("panel_open"));
	public static final StreamCodec<FriendlyByteBuf, PanelOpenPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, PanelOpenPayload::open,
			PanelOpenPayload::new);

	@Override
	public CustomPacketPayload.Type<PanelOpenPayload> type() {
		return TYPE;
	}
}
