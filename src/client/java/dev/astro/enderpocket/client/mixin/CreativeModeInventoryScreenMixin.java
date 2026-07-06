package dev.astro.enderpocket.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.astro.enderpocket.EnderPocketLayout;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The creative screen wraps/clears every slot of the player's InventoryMenu by
 * index. Cap those loops at the vanilla slot count so our appended ender slots
 * are never wrapped into the creative GUI and — critically — never cleared by
 * the creative "destroy all" action.
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
	@ModifyExpressionValue(method = "slotClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I"))
	private int enderpocket$capDestroyAll(int size) {
		return Math.min(size, EnderPocketLayout.ENDER_SLOT_START);
	}

	@ModifyExpressionValue(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I"))
	private int enderpocket$capSlotWrapping(int size) {
		return Math.min(size, EnderPocketLayout.ENDER_SLOT_START);
	}
}
