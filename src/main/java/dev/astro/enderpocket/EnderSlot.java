package dev.astro.enderpocket;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot backed by the player's real {@code PlayerEnderChestContainer}.
 * Only interactable while the panel is open on both sides.
 */
public class EnderSlot extends Slot {
	private final Player owner;

	public EnderSlot(Container container, Player owner, int index, int x, int y) {
		super(container, index, x, y);
		this.owner = owner;
	}

	private boolean panelOpen() {
		return this.owner.getAttachedOrElse(EnderPocketAttachments.PANEL_OPEN, false);
	}

	@Override
	public boolean isActive() {
		return this.panelOpen();
	}

	@Override
	public boolean mayPickup(Player player) {
		return this.panelOpen() && super.mayPickup(player);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return this.panelOpen() && super.mayPlace(stack);
	}
}
