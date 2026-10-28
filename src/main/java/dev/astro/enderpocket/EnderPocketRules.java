package dev.astro.enderpocket;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * Who may use the panel. The server applies its own config; clients connected
 * to a remote server apply the rules that server sent ({@link RulesPayload}),
 * so the button and the server-side slot checks always agree. Singleplayer and
 * LAN hosts read the live config on both sides.
 */
public final class EnderPocketRules {
	private static boolean remoteSet;
	private static boolean remoteRequireUnlock;
	private static boolean remoteRequireEnderChestItem;

	private EnderPocketRules() {
	}

	/** Client: rules received from the connected server. */
	public static void setRemote(boolean requireUnlock, boolean requireEnderChestItem) {
		remoteSet = true;
		remoteRequireUnlock = requireUnlock;
		remoteRequireEnderChestItem = requireEnderChestItem;
	}

	/** Client: back to the local config (disconnect, or a server that doesn't send rules). */
	public static void clearRemote() {
		remoteSet = false;
	}

	public static boolean requireUnlock(Player player) {
		return player.level().isClientSide() && remoteSet ? remoteRequireUnlock : EnderPocketConfig.get().requireUnlock;
	}

	public static boolean requireEnderChestItem(Player player) {
		return player.level().isClientSide() && remoteSet ? remoteRequireEnderChestItem : EnderPocketConfig.get().requireEnderChestItem;
	}

	/** The button exists for this player (unlock rule satisfied). */
	public static boolean unlocked(Player player) {
		return !requireUnlock(player) || player.getAttachedOrElse(EnderPocketAttachments.UNLOCKED, false);
	}

	public static boolean carryingEnderChest(Player player) {
		return player.getInventory().contains(stack -> stack.is(Items.ENDER_CHEST))
				|| player.containerMenu.getCarried().is(Items.ENDER_CHEST);
	}

	/** The panel may actually be used right now. */
	public static boolean canUse(Player player) {
		return unlocked(player) && (!requireEnderChestItem(player) || carryingEnderChest(player));
	}

	/** Panel open on this side and allowed by the rules: ender slots are live. */
	public static boolean panelActive(Player player) {
		return player.getAttachedOrElse(EnderPocketAttachments.PANEL_OPEN, false) && canUse(player);
	}
}
