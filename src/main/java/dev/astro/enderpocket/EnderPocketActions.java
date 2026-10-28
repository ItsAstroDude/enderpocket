package dev.astro.enderpocket;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-side handlers for the panel's Sort and Quick-stack buttons. */
public final class EnderPocketActions {
	private static final Comparator<ItemStack> ORDER = Comparator
			.comparingInt((ItemStack stack) -> BuiltInRegistries.ITEM.getId(stack.getItem()))
			.thenComparing(stack -> stack.getHoverName().getString())
			.thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());

	private EnderPocketActions() {
	}

	public static void handle(ServerPlayer player, int action) {
		// Only while the panel is genuinely live in the menu the player has open.
		if (!EnderPocketMenus.hasLivePanel(player.containerMenu)) {
			return;
		}
		Container ender = player.getEnderChestInventory();
		boolean changed = switch (action) {
			case PanelActionPayload.SORT -> sort(ender);
			case PanelActionPayload.QUICK_STACK -> quickStack(player, ender);
			default -> false;
		};
		if (changed) {
			player.containerMenu.broadcastChanges();
		}
	}

	/** Merge partial stacks, then order by item (registry order groups related items), name, count. */
	static boolean sort(Container ender) {
		List<ItemStack> merged = new ArrayList<>();
		for (int i = 0; i < ender.getContainerSize(); i++) {
			ItemStack stack = ender.getItem(i).copy();
			if (stack.isEmpty()) {
				continue;
			}
			for (ItemStack target : merged) {
				if (stack.isEmpty()) {
					break;
				}
				int room = Math.min(target.getMaxStackSize(), ender.getMaxStackSize()) - target.getCount();
				if (room > 0 && ItemStack.isSameItemSameComponents(target, stack)) {
					int moved = Math.min(room, stack.getCount());
					target.grow(moved);
					stack.shrink(moved);
				}
			}
			if (!stack.isEmpty()) {
				merged.add(stack);
			}
		}
		merged.sort(ORDER);
		boolean changed = false;
		for (int i = 0; i < ender.getContainerSize(); i++) {
			ItemStack next = i < merged.size() ? merged.get(i) : ItemStack.EMPTY;
			if (!ItemStack.matches(ender.getItem(i), next)) {
				ender.setItem(i, next);
				changed = true;
			}
		}
		if (changed) {
			ender.setChanged();
		}
		return changed;
	}

	/**
	 * Deposit every main-inventory stack (hotbar excluded) whose item already
	 * has a stack in the ender chest: top up partial stacks first, then fill
	 * empty slots.
	 */
	static boolean quickStack(ServerPlayer player, Container ender) {
		Inventory inventory = player.getInventory();
		boolean keepEnderChests = EnderPocketRules.requireEnderChestItem(player);
		boolean changed = false;
		for (int i = Inventory.SELECTION_SIZE; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty() || (keepEnderChests && stack.is(Items.ENDER_CHEST)) || !contains(ender, stack)) {
				continue;
			}
			int before = stack.getCount();
			int max = Math.min(stack.getMaxStackSize(), ender.getMaxStackSize());
			for (int j = 0; j < ender.getContainerSize() && !stack.isEmpty(); j++) {
				ItemStack target = ender.getItem(j);
				if (!target.isEmpty() && target.getCount() < max && ItemStack.isSameItemSameComponents(target, stack)) {
					int moved = Math.min(max - target.getCount(), stack.getCount());
					target.grow(moved);
					stack.shrink(moved);
				}
			}
			for (int j = 0; j < ender.getContainerSize() && !stack.isEmpty(); j++) {
				if (ender.getItem(j).isEmpty()) {
					ender.setItem(j, stack.split(max));
				}
			}
			if (stack.getCount() != before) {
				inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
				changed = true;
			}
		}
		if (changed) {
			inventory.setChanged();
			ender.setChanged();
		}
		return changed;
	}

	private static boolean contains(Container ender, ItemStack stack) {
		for (int i = 0; i < ender.getContainerSize(); i++) {
			if (ItemStack.isSameItemSameComponents(ender.getItem(i), stack)) {
				return true;
			}
		}
		return false;
	}
}
