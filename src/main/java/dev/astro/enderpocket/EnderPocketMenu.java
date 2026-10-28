package dev.astro.enderpocket;

/**
 * Implemented (via mixin) by every menu that carries ender slots: the player
 * inventory, chests/barrels and shulker boxes.
 */
public interface EnderPocketMenu {
	/** Index of the first ender slot, or -1 if this menu instance has none. */
	int enderpocket$enderStart();
}
