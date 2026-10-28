package dev.astro.enderpocket.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int enderpocket$leftPos();

	@Accessor("leftPos")
	void enderpocket$setLeftPos(int leftPos);

	@Accessor("topPos")
	int enderpocket$topPos();

	@Accessor("imageWidth")
	int enderpocket$imageWidth();
}
