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
 * at the corner.
 */
public class EnderTabButton extends AbstractButton {
	public static final int ROW_W = 20;
	public static final int ROW_H = 18;
	public static final int CORNER_SIZE = 22;
	private static final ItemStack ICON = new ItemStack(Items.ENDER_CHEST);
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
		graphics.item(ICON,
				this.getX() + (this.getWidth() - 16) / 2, this.getY() + (this.getHeight() - 16) / 2);
		if (this.isHovered()) {
			// Deferred tooltips render outside the shrink transform — anchor at the
			// untransformed cursor.
			graphics.setTooltipForNextFrame(this.getMessage(), EnderPanelClient.rawMouseX, EnderPanelClient.rawMouseY);
		}
	}
}
