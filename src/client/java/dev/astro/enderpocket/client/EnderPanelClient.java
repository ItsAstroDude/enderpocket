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

	/** Where the effect stack starts (below the button) and its per-effect step. */
	public static final int EFFECTS_TOP = 26;
	public static final int EFFECT_STEP = 33;

	private static boolean open;
	// Screen (ensemble) transform: scale + offset, pivot = screen centre.
	// Also used translate-only (scale 1) to slide the inventory left when the
	// full-size panel needs room on the right.
	private static float scrScale = 1.0f;
	private static float scrTx;
	private static float scrTy;
	// Panel-only transform: drop below the potion-effect stack, then scale
	// about the anchor point.
	private static float panScale = 1.0f;
	private static float panTy;
	private static long lastMs = -1L;
	// Recipe book handling: auto-closed once when the panel opens; if the user
	// re-opens it, respect that (fall back to the ensemble shrink) until the
	// screen closes.
	private static boolean bookRespected;

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
		panTy = 0.0f;
		lastMs = -1L;
		bookRespected = false;
	}

	public static boolean isBookRespected() {
		return bookRespected;
	}

	public static void respectBook() {
		bookRespected = true;
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
	public static void updateAnim(int width, int height, int leftPos, int topPos, boolean bookVisible, int effectsCount) {
		float tScrScale = 1.0f;
		float tScrTx = 0.0f;
		float tScrTy = 0.0f;
		float tPanTy = 0.0f;
		if (open) {
			// With active potion effects the panel drops below the effect stack
			// (which starts under the button and steps down per effect).
			if (effectsCount > 0) {
				int effectsHeight = effectsCount <= 5 ? effectsCount * EFFECT_STEP : 132 + 32;
				tPanTy = Math.max(0, EFFECTS_TOP + effectsHeight + 4 - EnderPocketLayout.PANEL_REL_Y);
			}

			int rightOverflow = leftPos + EnderPocketLayout.PANEL_REL_X + EnderPocketLayout.PANEL_W + 4 - width;
			float panelBottom = topPos + EnderPocketLayout.PANEL_REL_Y + tPanTy + EnderPocketLayout.PANEL_H;
			boolean verticalOk = panelBottom <= height - 2;
			if (!bookVisible && rightOverflow <= 0 && verticalOk) {
				// Fits detached at full size — nothing moves.
			} else if (!bookVisible && verticalOk && leftPos - rightOverflow >= 4) {
				// Slide the inventory left just enough for the full-size panel.
				tScrTx = -rightOverflow;
			} else {
				// Ensemble shrink around the screen centre: recipe book kept open
				// by the user, inventory can't slide far enough, or the panel
				// would run off the bottom. Everything scales in place.
				float cx = width / 2.0f;
				float cy = height / 2.0f;
				// Recipe book component is 147 wide plus a small gap when open.
				int leftEdge = (bookVisible ? leftPos - 155 : leftPos) - 4;
				int rightEdge = leftPos + EnderPocketLayout.PANEL_REL_X + EnderPocketLayout.PANEL_W + 4;
				float sRight = rightEdge > width - 2 ? (width - 2 - cx) / (rightEdge - cx) : 1.0f;
				float sLeft = leftEdge < 2 ? (cx - 2) / (cx - leftEdge) : 1.0f;
				float sVert = panelBottom + 2 > height - 2 ? (height - 2 - cy) / (panelBottom + 2 - cy) : 1.0f;
				float s = Math.clamp(Math.min(Math.min(sRight, sLeft), sVert), 0.4f, 1.0f);
				tScrScale = s;
				tScrTx = cx * (1.0f - s);
				tScrTy = cy * (1.0f - s);
			}
		}

		long now = System.nanoTime() / 1_000_000L;
		float dt = lastMs < 0 ? 1000.0f : now - lastMs;
		lastMs = now;
		float k = 1.0f - (float) Math.exp(-dt / EASE_MS);
		scrScale = ease(scrScale, tScrScale, k, 0.002f);
		scrTx = ease(scrTx, tScrTx, k, 0.35f);
		scrTy = ease(scrTy, tScrTy, k, 0.35f);
		panTy = ease(panTy, tPanTy, k, 0.35f);
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
		return panScale < 0.9995f || Math.abs(panTy) > 0.01f;
	}

	public static float panelScale() {
		return panScale;
	}

	public static float panelTy() {
		return panTy;
	}

	/**
	 * Push the panel-only transform (drop below the effect stack, then scale
	 * about the anchor) in a pose that is already translated to the GUI origin
	 * (the slot-drawing pose). No-op when the panel is at rest.
	 */
	public static void pushPanelRel(Matrix3x2fStack pose) {
		if (panelTransformActive()) {
			pose.pushMatrix();
			pose.translate(ANCHOR_X, ANCHOR_Y + panTy);
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
			pose.translate(ax, ay + panTy);
			pose.scale(panScale, panScale);
			pose.translate(-ax, -ay);
		}
	}

	public static void popPanelAbs(Matrix3x2fStack pose) {
		popPanelRel(pose);
	}

	/** Inverse panel mapping for GUI-relative coordinates. */
	public static double invPanelRelX(double xRel) {
		return ANCHOR_X + (xRel - ANCHOR_X) / panScale;
	}

	public static double invPanelRelY(double yRel) {
		return ANCHOR_Y + (yRel - panTy - ANCHOR_Y) / panScale;
	}
}
