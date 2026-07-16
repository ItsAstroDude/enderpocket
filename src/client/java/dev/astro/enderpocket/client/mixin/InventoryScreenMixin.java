package dev.astro.enderpocket.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.astro.enderpocket.EnderPocketConfig;
import dev.astro.enderpocket.EnderPocketLayout;
import dev.astro.enderpocket.client.EnderPanelClient;
import dev.astro.enderpocket.client.EnderPocketClient;
import dev.astro.enderpocket.client.EnderTabButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
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
	private void enderpocket$positionButton() {
		if (this.enderpocket$button == null) {
			return;
		}
		// Preferred: a free 20x18 spot in the vanilla button row next to the
		// recipe book toggle (x 128/152 rel; occupied by e.g. Terrastorage when
		// present). Scanned live so it adapts to whatever other mods add.
		int rowY = this.height / 2 - 22;
		for (int relX : new int[]{128, 152}) {
			int bx = this.leftPos + relX;
			if (this.enderpocket$rowSpotFree(bx, rowY, EnderTabButton.ROW_W, EnderTabButton.ROW_H)) {
				this.enderpocket$button.setPosition(bx, rowY);
				this.enderpocket$button.setWidth(EnderTabButton.ROW_W);
				this.enderpocket$button.setHeight(EnderTabButton.ROW_H);
				EnderPanelClient.setButtonInRow(true);
				return;
			}
		}
		// Fallback: top-right corner outside the GUI, above where the
		// potion-effect stack starts (EffectsInInventoryMixin shifts it down).
		this.enderpocket$button.setPosition(
				this.leftPos + EnderPocketLayout.INV_W + EnderPocketLayout.GAP,
				this.topPos);
		this.enderpocket$button.setWidth(EnderTabButton.CORNER_SIZE);
		this.enderpocket$button.setHeight(EnderTabButton.CORNER_SIZE);
		EnderPanelClient.setButtonInRow(false);
	}

	@Unique
	private boolean enderpocket$rowSpotFree(int x, int y, int w, int h) {
		for (var child : this.children()) {
			if (child != this.enderpocket$button && child instanceof AbstractWidget widget && widget.visible
					&& x < widget.getX() + widget.getWidth() && x + w > widget.getX()
					&& y < widget.getY() + widget.getHeight() && y + h > widget.getY()) {
				return false;
			}
		}
		return true;
	}

	@Unique
	private int enderpocket$effectsCount() {
		return this.minecraft.player == null ? 0 : this.minecraft.player.getActiveEffects().size();
	}

	@Unique
	private void enderpocket$onPanelToggled() {
		if (!EnderPanelClient.isOpen() || EnderPanelClient.isBookRespected()
				|| !EnderPocketConfig.get().autoCloseRecipeBook) {
			return;
		}
		// Auto-close the recipe book to make room for the full-size panel —
		// once. If the user re-opens it, we respect that and shrink instead.
		var recipeBook = ((AbstractRecipeBookScreenAccessor) this).enderpocket$recipeBook();
		if (recipeBook.isVisible()) {
			recipeBook.toggleVisibility();
			this.leftPos = recipeBook.updateScreenPosition(this.width, this.imageWidth);
			this.rebuildWidgets();
		}
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void enderpocket$init(CallbackInfo ci) {
		if (EnderPanelClient.available(this.minecraft.player)) {
			this.enderpocket$button = this.addRenderableWidget(new EnderTabButton(0, 0, () -> {
				EnderPanelClient.toggle();
				this.enderpocket$onPanelToggled();
			}));
			this.enderpocket$positionButton();
			// Panel already open (remembered state or the from-anywhere keybind):
			// give the recipe book auto-close a chance to run.
			if (EnderPanelClient.isOpen()) {
				this.enderpocket$onPanelToggled();
			}
		} else if (EnderPanelClient.isOpen()) {
			EnderPanelClient.setOpen(false);
		}
	}

	@Inject(method = "onRecipeBookButtonClick", at = @At("TAIL"))
	private void enderpocket$onRecipeBookToggled(CallbackInfo ci) {
		this.enderpocket$positionButton();
		// The user re-opened the recipe book while the panel is open — stop
		// auto-closing it and let the ensemble shrink handle the width.
		if (EnderPanelClient.isOpen() && ((AbstractRecipeBookScreenAccessor) this).enderpocket$recipeBook().isVisible()) {
			EnderPanelClient.respectBook();
		}
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
		// Re-scan placement each frame: all mods' widgets exist by now (init
		// hook ordering is undefined), and it adapts to recipe-book toggles.
		this.enderpocket$positionButton();
		EnderPanelClient.updateAnim(this.width, this.height, this.leftPos, this.topPos,
				((AbstractRecipeBookScreenAccessor) this).enderpocket$recipeBook().isVisible(), this.enderpocket$effectsCount());
		EnderPanelClient.pushScreen(graphics.pose());
		// Panel window drawn here — BEFORE the vanilla GUI texture — so it slides
		// out from behind the inventory. Assembled from the vanilla chest texture
		// so resource packs (e.g. dark GUI themes) restyle it automatically:
		// title bar + 3 slot rows, then the bottom border strip.
		if (EnderPanelClient.visualOpen()) {
			EnderPanelClient.pushPanelAbs(graphics.pose(), this.leftPos, this.topPos);
			int px = this.leftPos + EnderPocketLayout.PANEL_REL_X;
			int py = this.topPos + EnderPocketLayout.PANEL_REL_Y;
			graphics.blit(RenderPipelines.GUI_TEXTURED, ENDERPOCKET_GENERIC_54,
					px, py, 0.0F, 0.0F, EnderPocketLayout.PANEL_W, 71, 256, 256);
			graphics.blit(RenderPipelines.GUI_TEXTURED, ENDERPOCKET_GENERIC_54,
					px, py + 71, 0.0F, 215.0F, EnderPocketLayout.PANEL_W, EnderPocketLayout.PANEL_H - 71, 256, 256);
			EnderPanelClient.popPanelAbs(graphics.pose());
		}
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void enderpocket$panelBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
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
		if (EnderPanelClient.slotsInteractive()) {
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
			double px = xo + EnderPocketLayout.PANEL_REL_X;
			double py = yo + EnderPanelClient.ANCHOR_Y + EnderPanelClient.panelTy() - sp * EnderPocketLayout.PANEL_H / 2.0;
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
			this.enderpocket$onPanelToggled();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		if (EnderPanelClient.isOpen() && !EnderPocketConfig.get().rememberOpen) {
			EnderPanelClient.setOpen(false);
		}
		super.removed();
	}
}
