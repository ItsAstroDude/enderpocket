package dev.astro.enderpocket;

import dev.astro.enderpocket.mixin.AbstractContainerMenuInvoker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

/** Shared plumbing for the menus that carry ender slots. */
public final class EnderPocketMenus {
	/**
	 * Client side: whether the connected server runs the mod. Set by the client
	 * initializer. Both sides' slot layouts must match exactly, or the client
	 * crashes on the server's slot updates.
	 */
	public static BooleanSupplier clientServerSupported = () -> false;

	private EnderPocketMenus() {
	}

	/**
	 * Whether a chest/shulker menu for this player gets ender slots. Each side
	 * only adds them when the other side has the mod too.
	 */
	public static boolean shouldAttach(Player player) {
		if (player.level().isClientSide()) {
			return clientServerSupported.getAsBoolean();
		}
		if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
			try {
				return ServerPlayNetworking.canSend(serverPlayer, RulesPayload.TYPE);
			} catch (RuntimeException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Shift-click the stack in {@code slotIndex} into [start, end). Returns the
	 * quickMoveStack result, or null when nothing could move (so the caller can
	 * fall back to vanilla).
	 */
	public static @Nullable ItemStack quickMoveTo(AbstractContainerMenu menu, Player player, int slotIndex, int start, int end, boolean backwards) {
		Slot slot = menu.slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (!((AbstractContainerMenuInvoker) menu).enderpocket$moveItemStackTo(stack, start, end, backwards)) {
			return null;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY, copy);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == copy.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return copy;
	}

	/** First ender slot of a menu, or -1. */
	public static int enderStart(AbstractContainerMenu menu) {
		return menu instanceof EnderPocketMenu m ? m.enderpocket$enderStart() : -1;
	}

	/** True when the menu has ender slots and they're live (open, allowed, not a real ender chest). */
	public static boolean hasLivePanel(AbstractContainerMenu menu) {
		int start = enderStart(menu);
		return start >= 0 && menu.slots.get(start) instanceof EnderSlot slot && slot.isActive();
	}

	/** Mark a menu's ender slots as suppressed (the menu IS the player's ender chest). */
	public static void suppress(AbstractContainerMenu menu) {
		int start = enderStart(menu);
		if (start < 0) {
			return;
		}
		for (int i = start; i < start + EnderPocketLayout.ENDER_COUNT; i++) {
			if (menu.slots.get(i) instanceof EnderSlot slot) {
				slot.setSuppressed(true);
			}
		}
	}
}
