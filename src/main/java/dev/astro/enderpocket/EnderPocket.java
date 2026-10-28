package dev.astro.enderpocket;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnderPocket implements ModInitializer {
	public static final String MOD_ID = "enderpocket";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		EnderPocketConfig.load();
		EnderPocketAttachments.init();
		PayloadTypeRegistry.playC2S().register(PanelOpenPayload.TYPE, PanelOpenPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PanelActionPayload.TYPE, PanelActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RulesPayload.TYPE, RulesPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(PanelOpenPayload.TYPE, (payload, context) -> {
			var player = context.player();
			player.setAttached(EnderPocketAttachments.PANEL_OPEN, payload.open() && EnderPocketRules.canUse(player));
		});
		ServerPlayNetworking.registerGlobalReceiver(PanelActionPayload.TYPE,
				(payload, context) -> EnderPocketActions.handle(context.player(), payload.action()));

		// Tell modded clients the server's rules, so their button matches what
		// the server will actually allow.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler.player, RulesPayload.TYPE)) {
				EnderPocketConfig config = EnderPocketConfig.get();
				sender.sendPacket(new RulesPayload(config.requireUnlock, config.requireEnderChestItem));
			}
		});
	}
}
