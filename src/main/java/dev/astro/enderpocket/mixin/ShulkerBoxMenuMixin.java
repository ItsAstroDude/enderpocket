package dev.astro.enderpocket.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.astro.enderpocket.EnderPocketLayout;
import dev.astro.enderpocket.EnderPocketMenu;
import dev.astro.enderpocket.EnderPocketMenus;
import dev.astro.enderpocket.EnderSlot;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shulker boxes: same panel as chests (see ChestMenuMixin). */
@Mixin(ShulkerBoxMenu.class)
public abstract class ShulkerBoxMenuMixin extends AbstractContainerMenu implements EnderPocketMenu {
	@Unique
	private static final int ENDERPOCKET_CONTAINER_SIZE = 27;

	@Unique
	private int enderpocket$start = -1;

	protected ShulkerBoxMenuMixin(MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	@Override
	public int enderpocket$enderStart() {
		return this.enderpocket$start;
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V", at = @At("TAIL"))
	private void enderpocket$addEnderSlots(int containerId, Inventory inventory, Container container, CallbackInfo ci) {
		Player owner = inventory.player;
		if (!EnderPocketMenus.shouldAttach(owner)) {
			return;
		}
		this.enderpocket$start = this.slots.size();
		Container ender = owner.getEnderChestInventory();
		for (int row = 0; row < EnderPocketLayout.ENDER_ROWS; row++) {
			for (int col = 0; col < EnderPocketLayout.ENDER_COLS; col++) {
				this.addSlot(new EnderSlot(ender, owner, col + row * EnderPocketLayout.ENDER_COLS,
						EnderPocketLayout.CHEST_SLOT_X0 + col * 18,
						EnderPocketLayout.CHEST_SLOT_Y0 + row * 18));
			}
		}
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void enderpocket$quickMove(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		int start = this.enderpocket$start;
		if (start < 0) {
			return;
		}
		if (slotIndex >= start) {
			ItemStack moved = EnderPocketMenus.quickMoveTo(this, player, slotIndex, ENDERPOCKET_CONTAINER_SIZE, start, true);
			cir.setReturnValue(moved == null ? ItemStack.EMPTY : moved);
		} else if (slotIndex < ENDERPOCKET_CONTAINER_SIZE && EnderPocketMenus.hasLivePanel(this)) {
			ItemStack moved = EnderPocketMenus.quickMoveTo(this, player, slotIndex, start, start + EnderPocketLayout.ENDER_COUNT, false);
			if (moved != null) {
				cir.setReturnValue(moved);
			}
		}
	}

	@ModifyExpressionValue(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I"))
	private int enderpocket$capRange(int size) {
		return this.enderpocket$start >= 0 ? this.enderpocket$start : size;
	}
}
