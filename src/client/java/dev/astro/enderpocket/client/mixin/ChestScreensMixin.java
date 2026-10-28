package dev.astro.enderpocket.client.mixin;

import dev.astro.enderpocket.client.ChestPanelClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

	@Inject(method = "extractBackground",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
					shift = At.Shift.AFTER))
	private void enderpocket$afterDim(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		int dx = ChestPanelClient.beforeBackground((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY);
		if (dx != 0) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(dx, 0.0f);
			this.enderpocket$pushed = true;
		}
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void enderpocket$popShift(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (this.enderpocket$pushed) {
			this.enderpocket$pushed = false;
			graphics.pose().popMatrix();
		}
	}
}
