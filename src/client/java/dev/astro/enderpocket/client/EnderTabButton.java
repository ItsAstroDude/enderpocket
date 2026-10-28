package dev.astro.enderpocket.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Small button that toggles the panel. Draws the vanilla ender chest item as
 * its icon, so it always matches the player's resource pack automatically.
 * 20x18 when it sits in the vanilla button row inside the GUI, 22x22 outside
 * at the corner, 13x13 in a chest's title bar (icon scaled down to fit).
 */
public class EnderTabButton extends AbstractButton {
	public static final int ROW_W = 20;
	public static final int ROW_H = 18;
	public static final int CORNER_SIZE = 22;
	public static final int TITLE_BAR_SIZE = 13;
	private static final ItemStack ICON = new ItemStack(Items.ENDER_CHEST);
	private static final Component NEEDS_ITEM = Component.translatable("enderpocket.button.needs_item");
	private final Runnable onToggle;

	public EnderTabButton(int x, int y, Runnable onToggle) {
		super(x, y, CORNER_SIZE, CORNER_SIZE, Component.translatable("enderpocket.button"));
		this.onToggle = onToggle;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.onToggle.run();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.extractDefaultSprite(graphics);
		int size = Math.min(this.getWidth(), this.getHeight());
		if (size >= 18) {
			graphics.item(ICON,
					this.getX() + (this.getWidth() - 16) / 2, this.getY() + (this.getHeight() - 16) / 2);
		} else {
			float scale = (size - 2) / 16.0f;
			graphics.pose().pushMatrix();
			graphics.pose().translate(this.getX() + (this.getWidth() - 16 * scale) / 2.0f, this.getY() + (this.getHeight() - 16 * scale) / 2.0f);
			graphics.pose().scale(scale, scale);
			graphics.item(ICON, 0, 0);
			graphics.pose().popMatrix();
		}
		if (this.isHovered()) {
			// Deferred tooltips render outside the shrink transform — anchor at the
			// untransformed cursor.
			graphics.setTooltipForNextFrame(this.active ? this.getMessage() : NEEDS_ITEM,
					EnderPanelClient.rawMouseX, EnderPanelClient.rawMouseY);
		}
	}
}
