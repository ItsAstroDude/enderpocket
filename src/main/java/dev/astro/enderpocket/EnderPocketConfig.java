package dev.astro.enderpocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain JSON config at {@code config/enderpocket.json}. Read by both sides:
 * {@link #requireUnlock} governs server behaviour (the server's own file wins
 * on dedicated servers); the rest are client-side preferences.
 */
public class EnderPocketConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static EnderPocketConfig instance;

	/** Button/panel only after the player has opened a physical ender chest in this world. */
	public boolean requireUnlock = true;
	/** Keep the panel open across inventory opens (per game session). */
	public boolean rememberOpen = false;
	/** Auto-close the recipe book (once) when the panel opens. */
	public boolean autoCloseRecipeBook = true;
	/** Ender-chest open/close sounds on toggle. */
	public boolean sounds = true;
	/** Animation speed multiplier (higher = snappier). */
	public float animationSpeed = 1.0f;

	public static EnderPocketConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("enderpocket.json");
	}

	public static void load() {
		try {
			if (Files.exists(path())) {
				instance = GSON.fromJson(Files.readString(path()), EnderPocketConfig.class);
			}
		} catch (IOException | RuntimeException e) {
			EnderPocket.LOGGER.warn("Could not read enderpocket.json, using defaults", e);
		}
		if (instance == null) {
			instance = new EnderPocketConfig();
			save();
		}
		instance.animationSpeed = Math.clamp(instance.animationSpeed, 0.25f, 4.0f);
	}

	public static void save() {
		try {
			Files.writeString(path(), GSON.toJson(get()));
		} catch (IOException e) {
			EnderPocket.LOGGER.warn("Could not write enderpocket.json", e);
		}
	}
}
