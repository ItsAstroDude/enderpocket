package dev.astro.enderpocket.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.astro.enderpocket.client.EnderPanelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The potion-effect display shares the space right of the inventory GUI with
 * EnderPocket. Two adjustments keep them from overlapping:
 * - the stack starts below the EnderPocket button, and
 * - while the panel is open, effects render in their compact 32px mode (the
 *   panel steps right past the strip — see EnderPanelClient).
 */
@Mixin(EffectsInInventory.class)
public abstract class EffectsInInventoryMixin {
	@Shadow
	private AbstractContainerScreen<?> screen;

	@Unique
	private boolean enderpocket$onInventoryScreen() {
		return this.screen instanceof InventoryScreen;
	}

	// availableWidth >= 120 chooses the wide boxes; treating the threshold as
	// unreachable forces the compact strip while the panel is open.
	@ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 120))
	private int enderpocket$forceCompactWhenPanelOpen(int threshold) {
		return this.enderpocket$onInventoryScreen() && EnderPanelClient.isOpen() ? Integer.MAX_VALUE : threshold;
	}

	@ModifyExpressionValue(method = "extractEffects",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;topPos:I"))
	private int enderpocket$startBelowButton(int topPos) {
		if (this.enderpocket$onInventoryScreen() && EnderPanelClient.available(Minecraft.getInstance().player)) {
			return topPos + 26;
		}
		return topPos;
	}
}
