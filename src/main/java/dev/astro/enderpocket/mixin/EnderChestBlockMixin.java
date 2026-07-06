package dev.astro.enderpocket.mixin;

import dev.astro.enderpocket.EnderPocketAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("RETURN"))
	private void enderpocket$unlock(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (!level.isClientSide()
				&& cir.getReturnValue().consumesAction()
				&& player instanceof ServerPlayer serverPlayer
				&& !serverPlayer.getAttachedOrElse(EnderPocketAttachments.UNLOCKED, false)) {
			serverPlayer.setAttached(EnderPocketAttachments.UNLOCKED, true);
		}
	}
}
