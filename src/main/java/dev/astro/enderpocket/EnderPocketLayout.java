package dev.astro.enderpocket;

/**
 * Shared geometry for the ender panel. Slot coordinates are relative to the
 * inventory screen origin (leftPos/topPos), like every other menu slot, so the
 * panel follows the screen wherever vanilla or other mods move it.
 */
public final class EnderPocketLayout {
	public static final int INV_W = 176;
	public static final int INV_H = 166;
	/** Visual gap between the inventory window and the detached panel. */
	public static final int GAP = 8;
	public static final int PANEL_W = 176;
	public static final int PANEL_H = 78;
	public static final int PANEL_REL_X = INV_W + GAP;
	public static final int PANEL_REL_Y = (INV_H - PANEL_H) / 2;
	public static final int SLOT_X0 = PANEL_REL_X + 8;
	public static final int SLOT_Y0 = PANEL_REL_Y + 18;
	public static final int ENDER_COLS = 9;
	public static final int ENDER_ROWS = 3;
	/** First ender slot index in InventoryMenu (vanilla ends at 45 = shield). */
	public static final int ENDER_SLOT_START = 46;
	/** Exclusive end. */
	public static final int ENDER_SLOT_END = ENDER_SLOT_START + ENDER_COLS * ENDER_ROWS;

	private EnderPocketLayout() {
	}
}
