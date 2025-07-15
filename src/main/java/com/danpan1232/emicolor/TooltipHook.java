package com.danpan1232.emicolor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EMIColor.MOD_ID, value = Dist.CLIENT)
public class TooltipHook {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Only apply to block items
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
            Block block = blockItem.getBlock();

            // If the block has a debug hex tooltip, add it
            if (AverageColor.debugTooltipMap.containsKey(block)) {
                String hex = AverageColor.debugTooltipMap.get(block);
                event.getToolTip().add(Component.literal(hex).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
