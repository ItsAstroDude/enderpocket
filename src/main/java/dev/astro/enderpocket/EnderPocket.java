package dev.astro.enderpocket;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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
		PayloadTypeRegistry.serverboundPlay().register(PanelOpenPayload.TYPE, PanelOpenPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PanelOpenPayload.TYPE, (payload, context) -> {
			var player = context.player();
			boolean allowed = !EnderPocketConfig.get().requireUnlock
					|| player.getAttachedOrElse(EnderPocketAttachments.UNLOCKED, false);
			player.setAttached(EnderPocketAttachments.PANEL_OPEN, payload.open() && allowed);
		});
	}
}
