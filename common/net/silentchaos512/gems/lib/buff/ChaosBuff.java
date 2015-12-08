package net.silentchaos512.gems.lib.buff;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.silentchaos512.gems.SilentGems;
import net.silentchaos512.gems.core.handler.GemsExtendedPlayer;
import net.silentchaos512.gems.core.util.LocalizationHelper;
import net.silentchaos512.gems.core.util.LogHelper;
import net.silentchaos512.gems.item.ChaosGem;
import net.silentchaos512.gems.item.CraftingMaterial;
import net.silentchaos512.gems.item.ModItems;
import net.silentchaos512.gems.lib.Names;
import net.silentchaos512.gems.lib.Strings;
import net.silentchaos512.gems.network.MessageSetFlight;
import cpw.mods.fml.common.registry.GameRegistry;

public enum ChaosBuff {

  SPEED(0, "speed", 4, Potion.moveSpeed.id, 20, "ingotGold"),
  HASTE(1, "haste", 4, Potion.digSpeed.id, 20, "dustGlowstone"),
  JUMP(2, "jump", 4, Potion.jump.id, 10, CraftingMaterial.getStack(Names.PLUME)),
  FLIGHT(3, "flight", 1, -1, 80, CraftingMaterial.getStack(Names.GOLDEN_PLUME)),
  NIGHT_VISION(4, "nightVision", 1, Potion.nightVision.id, 10, Items.golden_carrot),
  REGENERATION(5, "regeneration", 2, Potion.regeneration.id, 40, Items.ghast_tear),
  RESISTANCE(6, "resistance", 2, Potion.resistance.id, 30, Items.leather_chestplate),
  FIRE_RESISTANCE(7, "fireResistance", 1, Potion.fireResistance.id, 30, Items.blaze_rod),
  WATER_BREATHING(8, "waterBreathing", 1, Potion.waterBreathing.id, 30, "blockLapis"),
  STRENGTH(9, "strength", 2, Potion.damageBoost.id, 30, "blockRedstone"),
  CAPACITY(10, "capacity", 4, -1, 0, null),
  BOOSTER(11, "booster", 4, -1, 0, null),
  ABSORPTION(12, "absorption", 1, -1, 50, null),
  INVISIBILITY(13, "invisibility", 1, Potion.invisibility.id, 40, Items.fermented_spider_eye);

  public static final int APPLY_DURATION_REGEN = 80;
  public static final int APPLY_DURATION_NIGHT_VISION = 400;
  public static final int APPLY_DURATION_DEFAULT = 20;

  public final int id;
  public final String name;
  public final int maxLevel;
  public final int potionId;
  public final int cost;
  public final Object material;

  private ChaosBuff(int id, String name, int maxLevel, int potionId, int cost, Object material) {

    this.id = id;
    this.name = name;
    this.maxLevel = maxLevel;
    this.potionId = potionId;
    this.cost = cost;
    this.material = material;
  }

  public static void initRecipes() {

    ItemStack refinedEssence = CraftingMaterial.getStack(Names.CHAOS_ESSENCE_PLUS);
    String redstone = "dustRedstone";
    for (ChaosBuff buff : values()) {
      if (buff.material != null) {
        ItemStack result = new ItemStack(ModItems.chaosRune, 1, buff.id);
        GameRegistry.addRecipe(new ShapedOreRecipe(result, "mcm", "cmc", "rcr", 'm', buff.material,
            'c', refinedEssence, 'r', redstone));
      }
    }
  }

  public int getCostPerTick(int level) {

    return (int) (this.cost * (1 + 0.20f * (level - 1)));
  }

  public int getApplyTime(EntityPlayer player, int level) {

    switch (this) {
      case REGENERATION:
        // Should apply every 2 seconds for regen I, every second for regen II.
        // Regen resets it timer when reapplied, so it won't work if applied too often.
        boolean shouldApply = false;

        PotionEffect activeRegen = player.getActivePotionEffect(Potion.regeneration);
        if (activeRegen == null) {
          shouldApply = true;
        } else {
          int remainingTime = activeRegen.getDuration();
          int healTime = level == 2 ? 20 : 40;
          if (remainingTime + healTime <= APPLY_DURATION_REGEN) {
            shouldApply = true;
          }
        }

        return shouldApply ? APPLY_DURATION_REGEN : 0;
      case NIGHT_VISION:
        return APPLY_DURATION_NIGHT_VISION;
      default:
        return APPLY_DURATION_DEFAULT;
    }
  }

  public void apply(EntityPlayer player, int level) {

    if (potionId > -1) {
      int time = getApplyTime(player, level);
      if (time > 0) {
        player.addPotionEffect(new PotionEffect(potionId, time, level - 1, true));
      }
    }

    // Apply other effects here.
    if (this.id == FLIGHT.id) {
      player.capabilities.allowFlying = true;
      player.fallDistance = 0.0f;
      // Prevents "lingering" flight effect, which allowed infinite flight.
      GemsExtendedPlayer properties = GemsExtendedPlayer.get(player);
      if (properties != null) {
        properties.refreshFlightTime();
      }
      // Send an "allow flight" message to the client, but only once per second.
      if (player.ticksExisted % 20 == 0 && player instanceof EntityPlayerMP) {
        SilentGems.instance.network.sendTo(new MessageSetFlight(true), (EntityPlayerMP) player);
      }
    }
  }

  public void remove(EntityPlayer player) {

    if (potionId > -1) {
      player.removePotionEffect(potionId);
    }

    // Apply other effects here.
    if (this.id == FLIGHT.id) {
      ChaosGem.removeFlight(player);
    }
  }

  public String getDisplayName(int level) {

    String s = LocalizationHelper.getLocalizedString(Strings.BUFF_RESOURCE_PREFIX + this.name);
    s += " ";

    if (level == 1) {
      s += "I";
    } else if (level == 2) {
      s += "II";
    } else if (level == 3) {
      s += "III";
    } else if (level == 4) {
      s += "IV";
    } else if (level == 5) {
      s += "V";
    } else {
      s += level;
    }

    return s;
  }
}
