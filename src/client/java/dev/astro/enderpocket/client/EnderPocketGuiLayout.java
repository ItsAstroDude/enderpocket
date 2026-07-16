package dev.astro.enderpocket.client;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import dev.astro.enderpocket.EnderPocket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;

/**
 * Resource-pack-overridable GUI layout. Packs that restyle the inventory can
 * ship {@code assets/enderpocket/gui_layout.json} to reposition the button and
 * panel to match their art, and optionally provide a full panel window texture
 * at {@code assets/enderpocket/textures/gui/ender_panel.png} (176x78) instead
 * of the default window assembled from the pack's own generic_54.png.
 *
 * All coordinates are relative to the inventory GUI origin (leftPos/topPos).
 * Defaults reproduce the built-in behaviour exactly.
 */
public final class EnderPocketGuiLayout {
	private static final Gson GSON = new Gson();
	private static final Identifier LAYOUT_ID = EnderPocket.id("gui_layout.json");
	private static final Identifier PANEL_TEXTURE_ID = EnderPocket.id("textures/gui/ender_panel.png");

	private static EnderPocketGuiLayout current = new EnderPocketGuiLayout();
	private static boolean panelTextureOverride;

	@SerializedName("button_row_spots")
	public int[][] buttonRowSpots = {{128, 61}, {152, 61}};
	@SerializedName("button_corner")
	public int[] buttonCorner = {184, 0};
	@SerializedName("panel_offset")
	public int[] panelOffset = {0, 0};
	@SerializedName("effects_clearance")
	public int effectsClearance = 26;

	public static EnderPocketGuiLayout get() {
		return current;
	}

	public static boolean hasPanelTextureOverride() {
		return panelTextureOverride;
	}

	public static Identifier panelTexture() {
		return PANEL_TEXTURE_ID;
	}

	public static void reload(ResourceManager resourceManager) {
		EnderPocketGuiLayout loaded = null;
		var resource = resourceManager.getResource(LAYOUT_ID);
		if (resource.isPresent()) {
			try (BufferedReader reader = resource.get().openAsReader()) {
				loaded = GSON.fromJson(reader, EnderPocketGuiLayout.class);
			} catch (Exception e) {
				EnderPocket.LOGGER.warn("Invalid enderpocket:gui_layout.json in a resource pack, using defaults", e);
			}
		}
		current = loaded != null ? loaded.sanitized() : new EnderPocketGuiLayout();
		panelTextureOverride = resourceManager.getResource(PANEL_TEXTURE_ID).isPresent();
	}

	private EnderPocketGuiLayout sanitized() {
		EnderPocketGuiLayout defaults = new EnderPocketGuiLayout();
		if (this.buttonRowSpots == null) {
			this.buttonRowSpots = defaults.buttonRowSpots;
		}
		if (this.buttonCorner == null || this.buttonCorner.length != 2) {
			this.buttonCorner = defaults.buttonCorner;
		}
		if (this.panelOffset == null || this.panelOffset.length != 2) {
			this.panelOffset = defaults.panelOffset;
		}
		return this;
	}
}
