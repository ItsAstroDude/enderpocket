package dev.astro.enderpocket.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;

public class EnderPocketClient implements ClientModInitializer {
	public static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.enderpocket.toggle",
				InputConstants.UNKNOWN.getValue(),
				KeyMapping.Category.INVENTORY));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EnderPanelClient.reset());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> EnderPanelClient.reset());
	}
}
