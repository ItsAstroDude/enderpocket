package dev.astro.enderpocket.client;

import dev.astro.enderpocket.EnderPocket;
import dev.astro.enderpocket.EnderPocketLayout;
import dev.astro.enderpocket.EnderPocketMenus;
import dev.astro.enderpocket.PanelActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the ender panel window (shared by the inventory and the chest screens)
 * and owns its title-bar buttons. All coordinates passed in for hit-testing are
 * panel-local: (0, 0) is the panel window's top-left corner.
 */
public final class EnderPanelPainter {
	private static final Identifier GENERIC_54 = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
	private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
	private static final Identifier SORT_ICON = EnderPocket.id("sort");
	private static final Identifier QUICK_STACK_ICON = EnderPocket.id("quick_stack");
	private static final Component TITLE = Component.translatable("container.enderchest");
	private static final Component SORT_TOOLTIP = Component.translatable("enderpocket.sort");
	private static final Component QUICK_STACK_TOOLTIP = Component.translatable("enderpocket.quick_stack");
	private static final int TEXT_COLOR = -12566464;

	public static final int BUTTON_SIZE = 11;
	public static final int BUTTON_Y = 3;
	public static final int QUICK_STACK_X = EnderPocketLayout.PANEL_W - 7 - BUTTON_SIZE;
	public static final int SORT_X = QUICK_STACK_X - BUTTON_SIZE - 2;

	private EnderPanelPainter() {
	}

	/**
	 * Window background, title, slot counter and buttons at (px, py) in the
	 * current pose. {@code localX/localY} is the mouse in panel-local coords
	 * (for button hover); {@code interactive} false draws the buttons idle.
	 */
	public static void drawWindow(GuiGraphicsExtractor graphics, Font font, int px, int py, AbstractContainerMenu menu,
			double localX, double localY, boolean interactive, int rawMouseX, int rawMouseY) {
		if (EnderPocketGuiLayout.hasPanelTextureOverride()) {
			// A resource pack supplied a dedicated 176x78 panel window.
			graphics.blit(RenderPipelines.GUI_TEXTURED, EnderPocketGuiLayout.panelTexture(),
					px, py, 0.0F, 0.0F, EnderPocketLayout.PANEL_W, EnderPocketLayout.PANEL_H,
					EnderPocketLayout.PANEL_W, EnderPocketLayout.PANEL_H);
		} else {
			// Assembled from the pack's own chest texture so it auto-themes:
			// title bar + 3 slot rows, then the bottom border strip.
			graphics.blit(RenderPipelines.GUI_TEXTURED, GENERIC_54,
					px, py, 0.0F, 0.0F, EnderPocketLayout.PANEL_W, 71, 256, 256);
			graphics.blit(RenderPipelines.GUI_TEXTURED, GENERIC_54,
					px, py + 71, 0.0F, 215.0F, EnderPocketLayout.PANEL_W, EnderPocketLayout.PANEL_H - 71, 256, 256);
		}
		graphics.text(font, TITLE, px + 8, py + 6, TEXT_COLOR, false);

		int filled = 0;
		int start = EnderPocketMenus.enderStart(menu);
		if (start >= 0) {
			for (int i = start; i < start + EnderPocketLayout.ENDER_COUNT; i++) {
				if (!menu.slots.get(i).getItem().isEmpty()) {
					filled++;
				}
			}
		}
		Component count = Component.translatable("enderpocket.slots", filled, EnderPocketLayout.ENDER_COUNT);
		graphics.text(font, count, px + SORT_X - 3 - font.width(count), py + 6, TEXT_COLOR, false);

		int hovered = interactive ? buttonAt(localX, localY) : -1;
		drawButton(graphics, px + SORT_X, py + BUTTON_Y, SORT_ICON, hovered == PanelActionPayload.SORT);
		drawButton(graphics, px + QUICK_STACK_X, py + BUTTON_Y, QUICK_STACK_ICON, hovered == PanelActionPayload.QUICK_STACK);
		if (hovered >= 0) {
			// Deferred tooltips render outside our pose transforms — anchor at the
			// untransformed cursor.
			graphics.setTooltipForNextFrame(hovered == PanelActionPayload.SORT ? SORT_TOOLTIP : QUICK_STACK_TOOLTIP,
					rawMouseX, rawMouseY);
		}
	}

	private static void drawButton(GuiGraphicsExtractor graphics, int x, int y, Identifier icon, boolean hovered) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? BUTTON_HIGHLIGHTED : BUTTON, x, y, BUTTON_SIZE, BUTTON_SIZE);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x + 2, y + 2, 7, 7);
	}

	/** Items drawn statically (no hover/interaction) while the panel slides. */
	public static void drawItems(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, AbstractContainerMenu menu) {
		int start = EnderPocketMenus.enderStart(menu);
		if (start < 0) {
			return;
		}
		for (int i = start; i < start + EnderPocketLayout.ENDER_COUNT; i++) {
			Slot slot = menu.slots.get(i);
			ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				graphics.item(stack, leftPos + slot.x, topPos + slot.y);
				graphics.itemDecorations(font, stack, leftPos + slot.x, topPos + slot.y);
			}
		}
	}

	/** Which title-bar button is under the panel-local point: an action id, or -1. */
	public static int buttonAt(double localX, double localY) {
		if (localY < BUTTON_Y || localY >= BUTTON_Y + BUTTON_SIZE) {
			return -1;
		}
		if (localX >= SORT_X && localX < SORT_X + BUTTON_SIZE) {
			return PanelActionPayload.SORT;
		}
		if (localX >= QUICK_STACK_X && localX < QUICK_STACK_X + BUTTON_SIZE) {
			return PanelActionPayload.QUICK_STACK;
		}
		return -1;
	}

	/** Handle a click at a panel-local point. True if a button consumed it. */
	public static boolean click(double localX, double localY) {
		int action = buttonAt(localX, localY);
		if (action < 0 || !EnderPanelClient.serverSupported()) {
			return false;
		}
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		ClientPlayNetworking.send(new PanelActionPayload(action));
		return true;
	}
}
