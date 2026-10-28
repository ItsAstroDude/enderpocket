package dev.astro.enderpocket;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A slot backed by the player's real {@code PlayerEnderChestContainer}.
 * Only interactable while the panel is open on both sides and the server's
 * rules allow it ({@link EnderPocketRules}).
 */
public class EnderSlot extends Slot {
	private final Player owner;
	// Set on the copies inside a real ender chest's own menu, where a second
	// view of the same container would only be confusing.
	private boolean suppressed;

	public EnderSlot(Container container, Player owner, int index, int x, int y) {
		super(container, index, x, y);
		this.owner = owner;
	}

	public void setSuppressed(boolean suppressed) {
		this.suppressed = suppressed;
	}

	private boolean live() {
		return !this.suppressed && EnderPocketRules.panelActive(this.owner);
	}

	@Override
	public boolean isActive() {
		return this.live();
	}

	@Override
	public boolean mayPickup(Player player) {
		return this.live() && super.mayPickup(player);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		// With the carry rule on, stashing your ender chest inside itself would
		// lock you out of the panel, so it's refused outright.
		if (stack.is(Items.ENDER_CHEST) && EnderPocketRules.requireEnderChestItem(this.owner)) {
			return false;
		}
		return this.live() && super.mayPlace(stack);
	}
}
