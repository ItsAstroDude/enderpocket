package dev.astro.enderpocket.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.astro.enderpocket.EnderPocketConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.RecipeBookType;

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

		// Open-from-anywhere: pressing the keybind in-world opens the inventory
		// with the panel already out.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				if (client.gui.screen() == null && client.player != null && EnderPanelClient.available(client.player)) {
					if (EnderPocketConfig.get().autoCloseRecipeBook && !EnderPanelClient.isBookRespected()) {
						// Pre-close the recipe book in its persisted settings so the
						// screen inits cleanly with the book shut.
						client.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, false);
					}
					EnderPanelClient.setOpen(true);
					client.gui.setScreen(new InventoryScreen(client.player));
				}
			}
		});
	}
}
