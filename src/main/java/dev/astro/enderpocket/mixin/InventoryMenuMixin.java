package dev.astro.enderpocket.mixin;

import dev.astro.enderpocket.EnderPocketAttachments;
import dev.astro.enderpocket.EnderPocketLayout;
import dev.astro.enderpocket.EnderSlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu {
	protected InventoryMenuMixin(MenuType<?> menuType, int containerId, int width, int height) {
		super(menuType, containerId, width, height);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void enderpocket$addEnderSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
		Container ender = owner.getEnderChestInventory();
		for (int row = 0; row < EnderPocketLayout.ENDER_ROWS; row++) {
			for (int col = 0; col < EnderPocketLayout.ENDER_COLS; col++) {
				this.addSlot(new EnderSlot(ender, owner, col + row * EnderPocketLayout.ENDER_COLS,
						EnderPocketLayout.SLOT_X0 + col * 18,
						EnderPocketLayout.SLOT_Y0 + row * 18));
			}
		}
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void enderpocket$quickMove(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		int start = EnderPocketLayout.ENDER_SLOT_START;
		int end = EnderPocketLayout.ENDER_SLOT_END;
		if (slotIndex >= start && slotIndex < end) {
			// Ender slot -> player inventory (main + hotbar), like a chest.
			Slot slot = this.slots.get(slotIndex);
			if (!slot.hasItem()) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
			ItemStack stack = slot.getItem();
			ItemStack copy = stack.copy();
			if (!this.moveItemStackTo(stack, 9, 45, true)) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY, copy);
			} else {
				slot.setChanged();
			}
			if (stack.getCount() == copy.getCount()) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
			slot.onTake(player, stack);
			cir.setReturnValue(copy);
			return;
		}

		boolean panelOpen = player.getAttachedOrElse(EnderPocketAttachments.PANEL_OPEN, false);
		if (panelOpen && slotIndex >= 9 && slotIndex < 45) {
			// Panel open: shift-click from main inventory/hotbar deposits into the
			// ender chest, chest-GUI style. If nothing fits, fall through to vanilla.
			Slot slot = this.slots.get(slotIndex);
			if (!slot.hasItem()) {
				return;
			}
			ItemStack stack = slot.getItem();
			ItemStack copy = stack.copy();
			if (!this.moveItemStackTo(stack, start, end, false)) {
				return;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY, copy);
			} else {
				slot.setChanged();
			}
			if (stack.getCount() == copy.getCount()) {
				cir.setReturnValue(ItemStack.EMPTY);
				return;
			}
			slot.onTake(player, stack);
			cir.setReturnValue(copy);
		}
	}
}
