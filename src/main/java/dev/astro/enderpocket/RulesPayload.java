package dev.astro.enderpocket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client on join: the server's access rules (see {@link EnderPocketRules}). */
public record RulesPayload(boolean requireUnlock, boolean requireEnderChestItem) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RulesPayload> TYPE = new CustomPacketPayload.Type<>(EnderPocket.id("rules"));
	public static final StreamCodec<FriendlyByteBuf, RulesPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, RulesPayload::requireUnlock,
			ByteBufCodecs.BOOL, RulesPayload::requireEnderChestItem,
			RulesPayload::new);

	@Override
	public CustomPacketPayload.Type<RulesPayload> type() {
		return TYPE;
	}
}
