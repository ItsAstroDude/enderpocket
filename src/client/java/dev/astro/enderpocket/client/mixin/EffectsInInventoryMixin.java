package dev.astro.enderpocket.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.astro.enderpocket.client.EnderPanelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The potion-effect display shares the space right of the inventory GUI with
 * EnderPocket: the stack starts below the EnderPocket button, and the panel
 * positions itself below the stack (see EnderPanelClient).
 */
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {
	@Shadow
	private AbstractContainerScreen<?> screen;

	@ModifyExpressionValue(method = "extractEffects",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;topPos:I"))
	private int enderpocket$startBelowButton(int topPos) {
		if (this.screen instanceof InventoryScreen && EnderPanelClient.available(Minecraft.getInstance().player)) {
			// No shift needed when the button found a home inside the GUI's row.
			return topPos + EnderPanelClient.effectsTop();
		}
		return topPos;
	}
}
