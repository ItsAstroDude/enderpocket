package dev.astro.enderpocket.client;

import dev.astro.enderpocket.EnderPocketConfig;
import dev.astro.enderpocket.EnderPocketLayout;
import dev.astro.enderpocket.EnderPocketMenus;
import dev.astro.enderpocket.client.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jspecify.annotations.Nullable;

/**
 * The ender panel on chest, barrel and shulker box screens. Simpler than the
 * inventory version: the panel sits left of the container window and the
 * whole screen glides right to make room (via leftPos, so vanilla slot
 * hit-testing just works — no transforms to invert).
 */
public final class ChestPanelClient {
	private static final float EASE_MS = 60.0f;

	private static @Nullable EnderTabButton button;
	private static float shift;
	private static long lastMs = -1L;

	private ChestPanelClient() {
	}

	public static boolean isChestScreen(Object screen) {
		return screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen;
	}

	private static boolean isEnderChest(Component title) {
		return title.getContents() instanceof TranslatableContents contents && "container.enderchest".equals(contents.getKey());
	}

	private static AbstractContainerScreenAccessor acc(AbstractContainerScreen<?> screen) {
		return (AbstractContainerScreenAccessor) screen;
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!isChestScreen(screen)) {
				return;
			}
			button = null;
			AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
			if (EnderPocketMenus.enderStart(containerScreen.getMenu()) < 0) {
				return;
			}
			if (isEnderChest(screen.getTitle())) {
				// The real ender chest: it already shows the contents.
				EnderPocketMenus.suppress(containerScreen.getMenu());
				return;
			}
			if (!EnderPanelClient.unlocked(client.player)) {
				return;
			}
			EnderPanelClient.enforceRules(client.player);
			EnderTabButton chestButton = new EnderTabButton(0, 0, EnderPanelClient::toggle);
			button = chestButton;
			Screens.getWidgets(screen).add(chestButton);
			// (Re)init: jump straight to the settled layout.
			shift = targetShift(containerScreen);
			lastMs = -1L;
			applyShift(containerScreen);
			positionButton(containerScreen, chestButton);

			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
				if (!EnderPanelClient.isOpen()) {
					return true;
				}
				int px = acc(containerScreen).enderpocket$leftPos() + EnderPocketLayout.CHEST_PANEL_REL_X;
				int py = acc(containerScreen).enderpocket$topPos() + EnderPocketLayout.CHEST_PANEL_REL_Y;
				return !EnderPanelPainter.click(event.x() - px, event.y() - py);
			});
			ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
				if (EnderPocketClient.toggleKey != null && !EnderPocketClient.toggleKey.isUnbound()
						&& EnderPocketClient.toggleKey.matches(event)
						&& EnderPanelClient.usable(Minecraft.getInstance().player)) {
					EnderPanelClient.toggle();
					return false;
				}
				return true;
			});
			ScreenEvents.remove(screen).register(s -> {
				button = null;
				if (EnderPanelClient.isOpen() && !EnderPocketConfig.get().rememberOpen) {
					EnderPanelClient.setOpen(false);
				}
			});
		});
	}

	private static int centeredLeft(AbstractContainerScreen<?> screen) {
		return (screen.width - acc(screen).enderpocket$imageWidth()) / 2;
	}

	/** How far right of centre the container window should sit. */
	private static float targetShift(AbstractContainerScreen<?> screen) {
		if (!EnderPanelClient.isOpen()) {
			return 0.0f;
		}
		int centered = centeredLeft(screen);
		// Centre the panel + window pair, but never push the panel off the left edge...
		int left = Math.max(centered + (EnderPocketLayout.PANEL_W + EnderPocketLayout.GAP) / 2,
				-EnderPocketLayout.CHEST_PANEL_REL_X + 2);
		// ...nor the window off the right edge.
		left = Math.min(left, Math.max(centered, screen.width - acc(screen).enderpocket$imageWidth() - 2));
		return left - centered;
	}

	private static void applyShift(AbstractContainerScreen<?> screen) {
		acc(screen).enderpocket$setLeftPos(centeredLeft(screen) + Math.round(shift));
	}

	private static void positionButton(AbstractContainerScreen<?> screen, EnderTabButton chestButton) {
		int left = acc(screen).enderpocket$leftPos();
		int top = acc(screen).enderpocket$topPos();
		// Preferred: the right end of the container's title bar. Other mods
		// (sort buttons...) sometimes live there, so it's re-checked live.
		int bx = left + acc(screen).enderpocket$imageWidth() - 7 - EnderTabButton.TITLE_BAR_SIZE;
		int by = top + 3;
		if (spotFree(screen, chestButton, bx, by, EnderTabButton.TITLE_BAR_SIZE, EnderTabButton.TITLE_BAR_SIZE)) {
			chestButton.setPosition(bx, by);
			chestButton.setWidth(EnderTabButton.TITLE_BAR_SIZE);
			chestButton.setHeight(EnderTabButton.TITLE_BAR_SIZE);
			return;
		}
		// Fallback: outside on the left, just below where the panel opens.
		chestButton.setPosition(left - EnderPocketLayout.GAP - EnderTabButton.CORNER_SIZE,
				top + EnderPocketLayout.CHEST_PANEL_REL_Y + EnderPocketLayout.PANEL_H + 4);
		chestButton.setWidth(EnderTabButton.CORNER_SIZE);
		chestButton.setHeight(EnderTabButton.CORNER_SIZE);
	}

	private static boolean spotFree(AbstractContainerScreen<?> screen, EnderTabButton self, int x, int y, int w, int h) {
		for (AbstractWidget widget : Screens.getWidgets(screen)) {
			if (widget != self && widget.visible
					&& x < widget.getX() + widget.getWidth() && x + w > widget.getX()
					&& y < widget.getY() + widget.getHeight() && y + h > widget.getY()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Per frame, right after the screen dims the world: animate the glide, place
	 * the button and draw the panel. Returns how far the vanilla window art must
	 * be shifted (vanilla draws it at the centred position, ignoring leftPos).
	 */
	public static int beforeBackground(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		EnderTabButton chestButton = button;
		if (chestButton == null) {
			return 0;
		}
		LocalPlayer player = Minecraft.getInstance().player;
		EnderPanelClient.enforceRules(player);
		chestButton.active = EnderPanelClient.usable(player);

		long now = System.nanoTime() / 1_000_000L;
		float dt = lastMs < 0 ? 1000.0f : now - lastMs;
		lastMs = now;
		float k = 1.0f - (float) Math.exp(-dt * EnderPocketConfig.get().animationSpeed / EASE_MS);
		float target = targetShift(screen);
		shift += (target - shift) * k;
		if (Math.abs(target - shift) < 0.35f) {
			shift = target;
		}
		applyShift(screen);
		positionButton(screen, chestButton);

		int left = acc(screen).enderpocket$leftPos();
		int top = acc(screen).enderpocket$topPos();
		if (EnderPanelClient.isOpen()) {
			int px = left + EnderPocketLayout.CHEST_PANEL_REL_X;
			int py = top + EnderPocketLayout.CHEST_PANEL_REL_Y;
			EnderPanelPainter.drawWindow(graphics, Minecraft.getInstance().font, px, py, screen.getMenu(),
					mouseX - px, mouseY - py, true, mouseX, mouseY);
		}
		return left - centeredLeft(screen);
	}

	/** Clicks on the panel or button must not count as "outside the window" (item drop). */
	public static boolean covers(AbstractContainerScreen<?> screen, double mx, double my) {
		EnderTabButton chestButton = button;
		if (chestButton == null) {
			return false;
		}
		if (chestButton.isMouseOver(mx, my)) {
			return true;
		}
		if (!EnderPanelClient.isOpen()) {
			return false;
		}
		int px = acc(screen).enderpocket$leftPos() + EnderPocketLayout.CHEST_PANEL_REL_X;
		int py = acc(screen).enderpocket$topPos() + EnderPocketLayout.CHEST_PANEL_REL_Y;
		return mx >= px && mx < px + EnderPocketLayout.PANEL_W && my >= py && my < py + EnderPocketLayout.PANEL_H;
	}
}
