package dev.astro.enderpocket;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class EnderPocketAttachments {
	/**
	 * Whether this player has ever opened a physical ender chest in this world.
	 * Persistent, survives death, synced to the owning client.
	 */
	public static final AttachmentType<Boolean> UNLOCKED = AttachmentRegistry.<Boolean>builder()
			.initializer(() -> false)
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(EnderPocket.id("unlocked"));

	/**
	 * Whether the ender panel is currently open. Runtime-only; the client sets it
	 * locally and mirrors it to the server via {@link PanelOpenPayload} so both
	 * sides agree on which slots are active.
	 */
	public static final AttachmentType<Boolean> PANEL_OPEN = AttachmentRegistry.<Boolean>builder()
			.initializer(() -> false)
			.buildAndRegister(EnderPocket.id("panel_open"));

	private EnderPocketAttachments() {
	}

	static void init() {
	}
}
