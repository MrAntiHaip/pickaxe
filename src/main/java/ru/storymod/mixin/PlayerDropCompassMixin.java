package ru.storymod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.storymod.item.ModItems;

@Mixin(PlayerEntity.class)
public class PlayerDropCompassMixin {

    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void preventCompassDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<?> cir) {
        if (stack != null && stack.isOf(ModItems.STORY_COMPASS)) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (!player.getWorld().isClient) {
                player.sendMessage(Text.literal("§c✦ Компас Судьбы связан с душой Димы и Ульяны! Его нельзя выбросить из инвентаря."), true);
                player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.4f, 1.8f);
            }
            // Возвращаем предмет обратно в инвентарь
            player.getInventory().insertStack(stack.copy());
            cir.setReturnValue(null);
        }
    }
}
