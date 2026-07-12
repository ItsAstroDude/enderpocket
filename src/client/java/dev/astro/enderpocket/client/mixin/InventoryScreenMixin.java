package dev.astro.enderpocket.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.astro.enderpocket.EnderPocketLayout;
import dev.astro.enderpocket.client.EnderPanelClient;
import dev.astro.enderpocket.client.EnderPocketClient;
import dev.astro.enderpocket.client.EnderTabButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
	@Unique
	private static final Identifier ENDERPOCKET_GENERIC_54 = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	@Unique
	private static final Component ENDERPOCKET_PANEL_TITLE = Component.translatable("container.enderchest");

	@Unique
	private EnderTabButton enderpocket$button;

	public InventoryScreenMixin(InventoryMenu menu, RecipeBookComponent<?> recipeBookComponent, Inventory inventory, Component title) {
		super(menu, recipeBookComponent, inventory, title);
	}

	@Unique
	private boolean enderpocket$recipeSideBySide() {
		AbstractRecipeBookScreenAccessor accessor = (AbstractRecipeBookScreenAccessor) this;
		return accessor.enderpocket$recipeBook().isVisible() && !accessor.enderpocket$widthTooNarrow();
	}

	@Unique
	private void enderpocket$positionButton() {
		if (this.enderpocket$button != null) {
			// Top-right corner of the GUI, above where the potion-effect stack
			// starts (EffectsInInventoryMixin shifts the stack below this).
			this.enderpocket$button.setPosition(
					this.leftPos + EnderPocketLayout.INV_W + EnderPocketLayout.GAP,
					this.topPos);
		}
	}

	@Unique
	private boolean enderpocket$effectsActive() {
		return this.minecraft.player != null && !this.minecraft.player.getActiveEffects().isEmpty();
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void enderpocket$init(CallbackInfo ci) {
		if (EnderPanelClient.available(this.minecraft.player)) {
			this.enderpocket$button = this.addRenderableWidget(new EnderTabButton(0, 0, EnderPanelClient::toggle));
			this.enderpocket$positionButton();
		} else if (EnderPanelClient.isOpen()) {
			EnderPanelClient.setOpen(false);
		}
	}

	@Inject(method = "onRecipeBookButtonClick", at = @At("TAIL"))
	private void enderpocket$onRecipeBookToggled(CallbackInfo ci) {
		this.enderpocket$positionButton();
	}

	// ------------------------------------------------------------- rendering
	// The screen is extracted in two passes: extractBackground (full-screen dim
	// first, then the GUI window + player model), then extractRenderState
	// (widgets, slots, labels, tooltips). The ensemble shrink must cover the GUI
	// drawing of BOTH passes but must NOT scale the full-screen dim — so the
	// background push happens right after the super call that draws the dim.

	@Inject(method = "extractBackground",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
					shift = At.Shift.AFTER))
	private void enderpocket$afterDim(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		EnderPanelClient.updateAnim(this.width, this.height, this.leftPos, this.enderpocket$recipeSideBySide(), this.enderpocket$effectsActive());
		EnderPanelClient.pushScreen(graphics.pose());
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void enderpocket$panelBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (EnderPanelClient.isOpen()) {
			// Panel window assembled from the vanilla chest texture so resource
			// packs (e.g. dark GUI themes) restyle it automatically: title bar +
			// 3 slot rows, then the bottom border strip.
			EnderPanelClient.pushPanelAbs(graphics.pose(), this.leftPos, this.topPos);
			int px = this.leftPos + EnderPocketLayout.PANEL_REL_X;
			int py = this.topPos + EnderPocketLayout.PANEL_REL_Y;
			graphics.blit(RenderPipelines.GUI_TEXTURED, ENDERPOCKET_GENERIC_54,
					px, py, 0.0F, 0.0F, EnderPocketLayout.PANEL_W, 71, 256, 256);
			graphics.blit(RenderPipelines.GUI_TEXTURED, ENDERPOCKET_GENERIC_54,
					px, py + 71, 0.0F, 215.0F, EnderPocketLayout.PANEL_W, EnderPocketLayout.PANEL_H - 71, 256, 256);
			EnderPanelClient.popPanelAbs(graphics.pose());
		}
		EnderPanelClient.popScreen(graphics.pose());
	}

	// The player model is a real 3D render that ignores the 2D pose transform —
	// map its box and size manually, and feed it the real cursor position.
	@WrapOperation(method = "extractBackground",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"))
	private void enderpocket$scaleEntity(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int size,
			float offsetY, float mouseX, float mouseY, LivingEntity entity, Operation<Void> original) {
		if (EnderPanelClient.screenTransformActive()) {
			original.call(graphics,
					Math.round(EnderPanelClient.mapX(x0)), Math.round(EnderPanelClient.mapY(y0)),
					Math.round(EnderPanelClient.mapX(x1)), Math.round(EnderPanelClient.mapY(y1)),
					Math.round(size * EnderPanelClient.screenScale()), offsetY,
					(float) EnderPanelClient.rawMouseX, (float) EnderPanelClient.rawMouseY, entity);
		} else {
			original.call(graphics, x0, y0, x1, y1, size, offsetY, mouseX, mouseY, entity);
		}
	}

	@WrapMethod(method = "extractRenderState")
	private void enderpocket$wrapExtract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, Operation<Void> original) {
		EnderPanelClient.rawMouseX = mouseX;
		EnderPanelClient.rawMouseY = mouseY;
		EnderPanelClient.pushScreen(graphics.pose());
		if (EnderPanelClient.screenTransformActive()) {
			original.call(graphics, (int) EnderPanelClient.remapX(mouseX), (int) EnderPanelClient.remapY(mouseY), a);
		} else {
			original.call(graphics, mouseX, mouseY, a);
		}
		EnderPanelClient.popScreen(graphics.pose());
	}

	@Inject(method = "extractLabels", at = @At("TAIL"))
	private void enderpocket$panelLabel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		if (EnderPanelClient.isOpen()) {
			EnderPanelClient.pushPanelRel(graphics.pose());
			graphics.text(this.font, ENDERPOCKET_PANEL_TITLE,
					EnderPocketLayout.PANEL_REL_X + 8, EnderPocketLayout.PANEL_REL_Y + 6, -12566464, false);
			EnderPanelClient.popPanelRel(graphics.pose());
		}
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// The deferred tooltip renders outside our pose transforms, so anchor it
		// at the untransformed cursor position.
		super.extractTooltip(graphics, EnderPanelClient.rawMouseX, EnderPanelClient.rawMouseY);
	}

	// ------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return super.mouseClicked(EnderPanelClient.remap(event), doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return super.mouseReleased(EnderPanelClient.remap(event));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		float s = EnderPanelClient.screenScale();
		return super.mouseDragged(EnderPanelClient.remap(event), dx / s, dy / s);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		return super.mouseScrolled(EnderPanelClient.remapX(x), EnderPanelClient.remapY(y), scrollX, scrollY);
	}

	@Override
	protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
		if (EnderPanelClient.isOpen()) {
			float sp = EnderPanelClient.panelScale();
			double px = xo + EnderPocketLayout.PANEL_REL_X + EnderPanelClient.panelTx();
			double py = yo + EnderPanelClient.ANCHOR_Y - sp * EnderPocketLayout.PANEL_H / 2.0;
			if (mx >= px && mx < px + sp * EnderPocketLayout.PANEL_W && my >= py && my < py + sp * EnderPocketLayout.PANEL_H) {
				return false;
			}
		}
		if (this.enderpocket$button != null && this.enderpocket$button.isMouseOver(mx, my)) {
			return false;
		}
		return super.hasClickedOutside(mx, my, xo, yo);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (EnderPocketClient.toggleKey != null && !EnderPocketClient.toggleKey.isUnbound()
				&& EnderPocketClient.toggleKey.matches(event)
				&& EnderPanelClient.available(this.minecraft.player)) {
			EnderPanelClient.toggle();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		if (EnderPanelClient.isOpen()) {
			EnderPanelClient.setOpen(false);
		}
		super.removed();
	}
}
