package dev.astro.enderpocket.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Small button with an ender chest icon that toggles the panel. */
public class EnderTabButton extends AbstractButton {
	public static final int SIZE = 22;
	private static final ItemStack ICON = new ItemStack(Items.ENDER_CHEST);
	private final Runnable onToggle;

	public EnderTabButton(int x, int y, Runnable onToggle) {
		super(x, y, SIZE, SIZE, Component.translatable("enderpocket.button"));
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
		graphics.item(ICON, this.getX() + (SIZE - 16) / 2, this.getY() + (SIZE - 16) / 2);
		if (this.isHovered()) {
			// Deferred tooltips render outside the shrink transform — anchor at the
			// untransformed cursor.
			graphics.setTooltipForNextFrame(this.getMessage(), EnderPanelClient.rawMouseX, EnderPanelClient.rawMouseY);
		}
	}
}
