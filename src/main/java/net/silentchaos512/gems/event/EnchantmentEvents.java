package net.silentchaos512.gems.event;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silentchaos512.gems.SilentGems;
import net.silentchaos512.gems.enchantment.EnchantmentIceAspect;
import net.silentchaos512.gems.enchantment.EnchantmentLifeSteal;
import net.silentchaos512.gems.enchantment.EnchantmentLightningAspect;
import net.silentchaos512.gems.init.ModEnchantments;

@Mod.EventBusSubscriber(modid = SilentGems.MOD_ID)
public final class EnchantmentEvents {
    private EnchantmentEvents() {}

    @SubscribeEvent
    public static void onGetBreakSpeed(PlayerEvent.BreakSpeed event) {

        EntityPlayer player = event.getEntityPlayer();
        ItemStack mainHand = player.getHeldItem(EnumHand.MAIN_HAND);

        if (!mainHand.isEmpty()) {
            // Gravity enchantment.
            int gravityLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.gravity, mainHand);
            if (gravityLevel > 0) {
                ModEnchantments.gravity.onGetBreakSpeed(event, mainHand, gravityLevel);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            ItemStack mainHand = player.getHeldItemMainhand();
            ItemStack offHand = player.getHeldItemOffhand();

            int lifeStealLevel = 0;
            int iceAspectLevel = 0;
            int lightningAspectLevel = 0;

            // Get levels of relevant enchantments.
            if (!mainHand.isEmpty()) {
                lifeStealLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.lifeSteal, mainHand);
                iceAspectLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.iceAspect, mainHand);
                lightningAspectLevel = EnchantmentHelper
                        .getEnchantmentLevel(ModEnchantments.lightningAspect, mainHand);
            }
            // If not, is it on off hand?
            if (lifeStealLevel < 1 && !offHand.isEmpty()) {
                lifeStealLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.lifeSteal, offHand);
            }

            // Do life steal?
            if (lifeStealLevel > 0) {
                float amount = Math.min(event.getAmount(), event.getEntityLiving().getHealth());
                float healAmount = EnchantmentLifeSteal.getAmountHealed(lifeStealLevel, amount);
                player.heal(healAmount);
            }

            // Ice Aspect
            if (iceAspectLevel > 0) {
                EnchantmentIceAspect.applyTo(event.getEntityLiving(), iceAspectLevel);
            }

            // Lightning Aspect
            if (lightningAspectLevel > 0) {
                EnchantmentLightningAspect.applyTo(event.getEntityLiving(), lightningAspectLevel);
            }
        }
    }
}
