package dev.astro.enderpocket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: a panel button was pressed ({@link #SORT} or {@link #QUICK_STACK}). */
public record PanelActionPayload(int action) implements CustomPacketPayload {
	public static final int SORT = 0;
	public static final int QUICK_STACK = 1;

	public static final CustomPacketPayload.Type<PanelActionPayload> TYPE = new CustomPacketPayload.Type<>(EnderPocket.id("panel_action"));
	public static final StreamCodec<FriendlyByteBuf, PanelActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PanelActionPayload::action,
			PanelActionPayload::new);

	@Override
	public CustomPacketPayload.Type<PanelActionPayload> type() {
		return TYPE;
	}
}
