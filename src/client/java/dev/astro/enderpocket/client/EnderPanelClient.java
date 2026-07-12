package dev.astro.enderpocket.client;

import dev.astro.enderpocket.EnderPocketAttachments;
import dev.astro.enderpocket.EnderPocketLayout;
import dev.astro.enderpocket.PanelOpenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Matrix3x2fStack;

/**
 * Client-side panel state plus the two fitting transforms (Astro's rules):
 *
 * - Recipe book CLOSED: the inventory must not move or scale at all. If the
 *   panel doesn't fit in the free space to the right, only the PANEL shrinks,
 *   anchored at its left-centre edge ("panel transform").
 * - Recipe book OPEN (side-by-side): the whole ensemble (book + inventory +
 *   full-size panel) shrinks around the screen centre ("screen transform").
 *
 * The two are mutually exclusive; both animate with a frame-rate independent
 * exponential ease. Mouse input is remapped through the inverse transforms.
 */
public final class EnderPanelClient {
	private static final float EASE_MS = 60.0f;

	/** Panel scale anchor, relative to the GUI origin (left edge, vertical centre of the panel). */
	public static final float ANCHOR_X = EnderPocketLayout.PANEL_REL_X;
	public static final float ANCHOR_Y = EnderPocketLayout.PANEL_REL_Y + EnderPocketLayout.PANEL_H / 2.0f;

	/** Horizontal room reserved for the compact potion-effect strip (32px + gap). */
	public static final int EFFECTS_STRIP_OFFSET = 34;

	private static boolean open;
	// Screen (ensemble) transform: scale + offset, pivot = screen centre.
	private static float scrScale = 1.0f;
	private static float scrTx;
	private static float scrTy;
	// Panel-only transform: translate right (to clear the effect strip), then
	// scale about the anchor point.
	private static float panScale = 1.0f;
	private static float panTx;
	private static long lastMs = -1L;

	/** Untransformed mouse position, kept for tooltip anchoring and the entity renderer. */
	public static int rawMouseX;
	public static int rawMouseY;

	private EnderPanelClient() {
	}

	public static boolean isOpen() {
		return open;
	}

	public static void reset() {
		open = false;
		scrScale = 1.0f;
		scrTx = 0.0f;
		scrTy = 0.0f;
		panScale = 1.0f;
		panTx = 0.0f;
		lastMs = -1L;
	}

	public static boolean serverSupported() {
		try {
			return ClientPlayNetworking.canSend(PanelOpenPayload.TYPE);
		} catch (IllegalStateException e) {
			return false;
		}
	}

	public static boolean available(LocalPlayer player) {
		return player != null && serverSupported() && player.getAttachedOrElse(EnderPocketAttachments.UNLOCKED, false);
	}

	public static void setOpen(boolean o) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		if (o && !available(player)) {
			return;
		}
		open = o;
		player.setAttached(EnderPocketAttachments.PANEL_OPEN, o);
		if (serverSupported()) {
			ClientPlayNetworking.send(new PanelOpenPayload(o));
		}
	}

	public static void toggle() {
		setOpen(!open);
	}

	/**
	 * Advance the animation one frame and compute the target transforms for the
	 * current layout. Called once per frame, from the background extract pass.
	 * {@code recipeSideBySide} must be false when the recipe book is closed OR
	 * overlaying the GUI in width-too-narrow mode.
	 */
	public static void updateAnim(int width, int height, int leftPos, boolean recipeSideBySide, boolean effectsActive) {
		float tScrScale = 1.0f;
		float tScrTx = 0.0f;
		float tScrTy = 0.0f;
		float tPanScale = 1.0f;
		float tPanTx = 0.0f;
		if (open) {
			// With active potion effects the panel steps right so the compact
			// effect strip keeps its spot next to the GUI.
			int panelRelX = EnderPocketLayout.PANEL_REL_X + (effectsActive ? EFFECTS_STRIP_OFFSET : 0);
			tPanTx = effectsActive ? EFFECTS_STRIP_OFFSET : 0.0f;
			if (recipeSideBySide) {
				// Ensemble shrink around the screen centre; panel stays full-size
				// within the ensemble.
				float cx = width / 2.0f;
				float cy = height / 2.0f;
				// Recipe book component is 147 wide plus a small gap when open.
				int leftEdge = leftPos - 155 - 4;
				int rightEdge = leftPos + panelRelX + EnderPocketLayout.PANEL_W + 4;
				float sRight = rightEdge > width - 2 ? (width - 2 - cx) / (rightEdge - cx) : 1.0f;
				float sLeft = leftEdge < 2 ? (cx - 2) / (cx - leftEdge) : 1.0f;
				float s = Math.clamp(Math.min(sRight, sLeft), 0.4f, 1.0f);
				tScrScale = s;
				tScrTx = cx * (1.0f - s);
				tScrTy = cy * (1.0f - s);
			} else {
				// Inventory untouched; shrink only the panel into the free space.
				float available = width - 2 - (leftPos + panelRelX);
				tPanScale = Math.clamp(available / EnderPocketLayout.PANEL_W, 0.5f, 1.0f);
			}
		}

		long now = System.nanoTime() / 1_000_000L;
		float dt = lastMs < 0 ? 1000.0f : now - lastMs;
		lastMs = now;
		float k = 1.0f - (float) Math.exp(-dt / EASE_MS);
		scrScale = ease(scrScale, tScrScale, k, 0.002f);
		scrTx = ease(scrTx, tScrTx, k, 0.35f);
		scrTy = ease(scrTy, tScrTy, k, 0.35f);
		panScale = ease(panScale, tPanScale, k, 0.002f);
		panTx = ease(panTx, tPanTx, k, 0.35f);
	}

	private static float ease(float cur, float target, float k, float snap) {
		cur += (target - cur) * k;
		return Math.abs(target - cur) < snap ? target : cur;
	}

	// ---------------------------------------------------------------- screen tf

	public static boolean screenTransformActive() {
		return scrScale < 0.9995f || Math.abs(scrTx) > 0.01f || Math.abs(scrTy) > 0.01f;
	}

	// The screen render state is extracted in two separate passes (background,
	// then contents), and the recipe-book "width too narrow" path re-enters
	// extractBackground from inside extractRenderState — so the transform push
	// is depth-guarded to apply exactly once.
	private static int pushDepth;
	private static boolean pushed;

	public static void pushScreen(Matrix3x2fStack pose) {
		if (pushDepth++ == 0 && screenTransformActive()) {
			pushed = true;
			pose.pushMatrix();
			pose.translate(scrTx, scrTy);
			pose.scale(scrScale, scrScale);
		}
	}

	public static void popScreen(Matrix3x2fStack pose) {
		if (--pushDepth == 0 && pushed) {
			pushed = false;
			pose.popMatrix();
		}
	}

	public static float screenScale() {
		return scrScale;
	}

	/** Map virtual GUI coords to real screen coords (screen transform). */
	public static float mapX(float x) {
		return scrTx + scrScale * x;
	}

	public static float mapY(float y) {
		return scrTy + scrScale * y;
	}

	public static double remapX(double x) {
		return (x - scrTx) / scrScale;
	}

	public static double remapY(double y) {
		return (y - scrTy) / scrScale;
	}

	public static MouseButtonEvent remap(MouseButtonEvent event) {
		if (!screenTransformActive()) {
			return event;
		}
		return new MouseButtonEvent(remapX(event.x()), remapY(event.y()), event.buttonInfo());
	}

	// ---------------------------------------------------------------- panel tf

	public static boolean panelTransformActive() {
		return panScale < 0.9995f || Math.abs(panTx) > 0.01f;
	}

	public static float panelScale() {
		return panScale;
	}

	public static float panelTx() {
		return panTx;
	}

	/**
	 * Push the panel-only transform (rightward step past the effect strip, then
	 * scale about the anchor) in a pose that is already translated to the GUI
	 * origin (the slot-drawing pose). No-op when the panel is at rest.
	 */
	public static void pushPanelRel(Matrix3x2fStack pose) {
		if (panelTransformActive()) {
			pose.pushMatrix();
			pose.translate(ANCHOR_X + panTx, ANCHOR_Y);
			pose.scale(panScale, panScale);
			pose.translate(-ANCHOR_X, -ANCHOR_Y);
		}
	}

	public static void popPanelRel(Matrix3x2fStack pose) {
		if (panelTransformActive()) {
			pose.popMatrix();
		}
	}

	/** Same as {@link #pushPanelRel} but for a pose in absolute screen space. */
	public static void pushPanelAbs(Matrix3x2fStack pose, int leftPos, int topPos) {
		if (panelTransformActive()) {
			float ax = leftPos + ANCHOR_X;
			float ay = topPos + ANCHOR_Y;
			pose.pushMatrix();
			pose.translate(ax + panTx, ay);
			pose.scale(panScale, panScale);
			pose.translate(-ax, -ay);
		}
	}

	public static void popPanelAbs(Matrix3x2fStack pose) {
		popPanelRel(pose);
	}

	/** Inverse panel mapping for GUI-relative coordinates. */
	public static double invPanelRelX(double xRel) {
		return ANCHOR_X + (xRel - panTx - ANCHOR_X) / panScale;
	}

	public static double invPanelRelY(double yRel) {
		return ANCHOR_Y + (yRel - ANCHOR_Y) / panScale;
	}
}
