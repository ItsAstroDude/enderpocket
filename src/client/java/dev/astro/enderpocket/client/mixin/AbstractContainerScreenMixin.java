package dev.astro.enderpocket.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.astro.enderpocket.EnderSlot;
import dev.astro.enderpocket.client.ChestPanelClient;
import dev.astro.enderpocket.client.EnderPanelClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When the panel-only transform is active (recipe book closed, tight space),
 * ender slots render, highlight and hit-test through the panel scale while the
 * rest of the inventory screen stays untouched. Chest screens only need the
 * panel excluded from the "clicked outside the window" check.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected Slot hoveredSlot;
	@Shadow
	protected int leftPos;
	@Shadow
	protected int topPos;

	@Unique
	private boolean enderpocket$panelSlot(Slot slot) {
		return (Object) this instanceof InventoryScreen && slot instanceof EnderSlot && EnderPanelClient.panelTransformActive();
	}

	@WrapMethod(method = "renderSlot")
	private void enderpocket$scaleSlotRender(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, Operation<Void> original) {
		if ((Object) this instanceof InventoryScreen && slot instanceof EnderSlot) {
			// Slot contents stay hidden until the panel has slid out; they'd
			// otherwise float over the inventory during the animation.
			if (!EnderPanelClient.slotsInteractive()) {
				return;
			}
			EnderPanelClient.pushPanelRel(graphics.pose());
			original.call(graphics, slot, mouseX, mouseY);
			EnderPanelClient.popPanelRel(graphics.pose());
		} else {
			original.call(graphics, slot, mouseX, mouseY);
		}
	}

	@WrapMethod(method = "renderSlotHighlightBack")
	private void enderpocket$scaleHighlightBack(GuiGraphics graphics, Operation<Void> original) {
		if (this.hoveredSlot != null && this.enderpocket$panelSlot(this.hoveredSlot)) {
			EnderPanelClient.pushPanelRel(graphics.pose());
			original.call(graphics);
			EnderPanelClient.popPanelRel(graphics.pose());
		} else {
			original.call(graphics);
		}
	}

	@WrapMethod(method = "renderSlotHighlightFront")
	private void enderpocket$scaleHighlightFront(GuiGraphics graphics, Operation<Void> original) {
		if (this.hoveredSlot != null && this.enderpocket$panelSlot(this.hoveredSlot)) {
			EnderPanelClient.pushPanelRel(graphics.pose());
			original.call(graphics);
			EnderPanelClient.popPanelRel(graphics.pose());
		} else {
			original.call(graphics);
		}
	}

	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void enderpocket$chestPanelInside(double mx, double my, int xo, int yo, CallbackInfoReturnable<Boolean> cir) {
		if (ChestPanelClient.isChestScreen(this) && ChestPanelClient.covers((AbstractContainerScreen<?>) (Object) this, mx, my)) {
			cir.setReturnValue(false);
		}
	}

	@WrapMethod(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z")
	private boolean enderpocket$panelSlotHover(Slot slot, double mx, double my, Operation<Boolean> original) {
		if ((Object) this instanceof InventoryScreen && slot instanceof EnderSlot) {
			if (!EnderPanelClient.slotsInteractive()) {
				return false;
			}
			if (EnderPanelClient.panelTransformActive()) {
				double relX = EnderPanelClient.invPanelRelX(mx - this.leftPos);
				double relY = EnderPanelClient.invPanelRelY(my - this.topPos);
				return relX >= slot.x - 1 && relX < slot.x + 17 && relY >= slot.y - 1 && relY < slot.y + 17;
			}
		}
		return original.call(slot, mx, my);
	}
}
