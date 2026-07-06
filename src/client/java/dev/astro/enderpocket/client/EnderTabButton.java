package dev.astro.enderpocket.client;

import dev.astro.enderpocket.EnderPocket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Small button with an ender chest icon that toggles the panel. */
public class EnderTabButton extends AbstractButton {
	public static final int SIZE = 22;
	private static final Identifier ICON_SPRITE = EnderPocket.id("button/ender_pocket");
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
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON_SPRITE,
				this.getX() + (SIZE - 16) / 2, this.getY() + (SIZE - 16) / 2, 16, 16);
		if (this.isHovered()) {
			// Deferred tooltips render outside the shrink transform — anchor at the
			// untransformed cursor.
			graphics.setTooltipForNextFrame(this.getMessage(), EnderPanelClient.rawMouseX, EnderPanelClient.rawMouseY);
		}
	}
}
