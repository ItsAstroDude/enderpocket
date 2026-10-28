package dev.astro.enderpocket.client.mixin;

import dev.astro.enderpocket.client.ChestPanelClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chest and shulker screens draw their window art at the centred position
 * rather than leftPos. While the panel pushes leftPos right, shift the art
 * along with it (after the full-screen dim, which must stay put).
 */
@Mixin({ContainerScreen.class, ShulkerBoxScreen.class})
public abstract class ChestScreensMixin {
	@Unique
	private boolean enderpocket$pushed;

	// 1.21.x: renderBg runs right after the dim (AbstractContainerScreen.renderBackground).
	@Inject(method = "renderBg", at = @At("HEAD"))
	private void enderpocket$afterDim(GuiGraphics graphics, float a, int mouseX, int mouseY, CallbackInfo ci) {
		int dx = ChestPanelClient.beforeBackground((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY);
		if (dx != 0) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(dx, 0.0f);
			this.enderpocket$pushed = true;
		}
	}

	@Inject(method = "renderBg", at = @At("TAIL"))
	private void enderpocket$popShift(GuiGraphics graphics, float a, int mouseX, int mouseY, CallbackInfo ci) {
		if (this.enderpocket$pushed) {
			this.enderpocket$pushed = false;
			graphics.pose().popMatrix();
		}
	}
}
