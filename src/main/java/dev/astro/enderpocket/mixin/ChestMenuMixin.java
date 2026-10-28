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
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chests, barrels, minecart chests: ender slots appended after the player
 * inventory, laid out in a panel left of the container window.
 */
@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin extends AbstractContainerMenu implements EnderPocketMenu {
	@Shadow
	@Final
	private int containerRows;

	@Unique
	private int enderpocket$start = -1;

	protected ChestMenuMixin(MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	@Override
	public int enderpocket$enderStart() {
		return this.enderpocket$start;
	}

	@Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
			at = @At("TAIL"))
	private void enderpocket$addEnderSlots(MenuType<?> menuType, int containerId, Inventory inventory, Container container, int rows, CallbackInfo ci) {
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
		// The real ender chest opens through ChestMenu too. The server can tell
		// from the container; the client marks it from the screen title.
		if (container instanceof PlayerEnderChestContainer) {
			EnderPocketMenus.suppress(this);
		}
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void enderpocket$quickMove(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		int start = this.enderpocket$start;
		if (start < 0) {
			return;
		}
		int containerEnd = this.containerRows * 9;
		if (slotIndex >= start) {
			// Ender slot -> player inventory.
			ItemStack moved = EnderPocketMenus.quickMoveTo(this, player, slotIndex, containerEnd, start, true);
			cir.setReturnValue(moved == null ? ItemStack.EMPTY : moved);
		} else if (slotIndex < containerEnd && EnderPocketMenus.hasLivePanel(this)) {
			// Panel open: looting goes straight into the ender chest; if it's
			// full, vanilla moves the rest into the player inventory.
			ItemStack moved = EnderPocketMenus.quickMoveTo(this, player, slotIndex, start, start + EnderPocketLayout.ENDER_COUNT, false);
			if (moved != null) {
				cir.setReturnValue(moved);
			}
		}
	}

	// Vanilla moves chest -> "everything after the chest"; keep that range to
	// the player inventory so the ender slots aren't filled behind your back.
	@ModifyExpressionValue(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I"))
	private int enderpocket$capRange(int size) {
		return this.enderpocket$start >= 0 ? this.enderpocket$start : size;
	}
}
