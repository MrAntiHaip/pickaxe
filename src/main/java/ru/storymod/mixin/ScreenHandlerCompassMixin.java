package ru.storymod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.storymod.item.ModItems;

@Mixin(ScreenHandler.class)
public class ScreenHandlerCompassMixin {

    @Inject(method = "internalOnSlotClick", at = @At("HEAD"), cancellable = true)
    private void lockStoryCompassInInventory(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        ScreenHandler handler = (ScreenHandler) (Object) this;
        if (actionType == SlotActionType.THROW && slotIndex >= 0 && slotIndex < handler.slots.size()) {
            ItemStack stackInSlot = handler.slots.get(slotIndex).getStack();
            if (stackInSlot.isOf(ModItems.STORY_COMPASS)) {
                player.sendMessage(Text.literal("§c✦ Компас Судьбы нельзя выкинуть!"), true);
                ci.cancel();
            }
        }
        if (slotIndex == -999 && handler.getCursorStack().isOf(ModItems.STORY_COMPASS)) {
            player.sendMessage(Text.literal("§c✦ Компас Судьбы неразрывно привязан к вашему инвентарю!"), true);
            ci.cancel();
        }
    }
}
