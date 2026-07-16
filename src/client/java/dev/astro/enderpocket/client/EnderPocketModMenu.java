package dev.astro.enderpocket.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.astro.enderpocket.EnderPocketConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public class EnderPocketModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
			return parent -> null;
		}
		return parent -> {
			EnderPocketConfig config = EnderPocketConfig.get();
			ConfigBuilder builder = ConfigBuilder.create()
					.setParentScreen(parent)
					.setTitle(Component.translatable("enderpocket.config.title"))
					.setSavingRunnable(EnderPocketConfig::save);
			ConfigEntryBuilder entries = builder.entryBuilder();
			ConfigCategory general = builder.getOrCreateCategory(Component.translatable("enderpocket.config.category"));
			general.addEntry(entries.startBooleanToggle(Component.translatable("enderpocket.config.requireUnlock"), config.requireUnlock)
					.setDefaultValue(true)
					.setTooltip(Component.translatable("enderpocket.config.requireUnlock.tooltip"))
					.setSaveConsumer(v -> config.requireUnlock = v)
					.build());
			general.addEntry(entries.startBooleanToggle(Component.translatable("enderpocket.config.rememberOpen"), config.rememberOpen)
					.setDefaultValue(false)
					.setTooltip(Component.translatable("enderpocket.config.rememberOpen.tooltip"))
					.setSaveConsumer(v -> config.rememberOpen = v)
					.build());
			general.addEntry(entries.startBooleanToggle(Component.translatable("enderpocket.config.autoCloseRecipeBook"), config.autoCloseRecipeBook)
					.setDefaultValue(true)
					.setTooltip(Component.translatable("enderpocket.config.autoCloseRecipeBook.tooltip"))
					.setSaveConsumer(v -> config.autoCloseRecipeBook = v)
					.build());
			general.addEntry(entries.startBooleanToggle(Component.translatable("enderpocket.config.sounds"), config.sounds)
					.setDefaultValue(true)
					.setSaveConsumer(v -> config.sounds = v)
					.build());
			general.addEntry(entries.startFloatField(Component.translatable("enderpocket.config.animationSpeed"), config.animationSpeed)
					.setDefaultValue(1.0f)
					.setMin(0.25f)
					.setMax(4.0f)
					.setTooltip(Component.translatable("enderpocket.config.animationSpeed.tooltip"))
					.setSaveConsumer(v -> config.animationSpeed = v)
					.build());
			return builder.build();
		};
	}
}
