package silent.gems.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.oredict.OreDictionary;
import silent.gems.lib.EnumGem;
import silent.gems.lib.Names;
import silent.gems.lib.Strings;
import cpw.mods.fml.common.registry.GameRegistry;

public class GemRod extends ItemSG {

    public GemRod() {

        icons = new IIcon[EnumGem.all().length];
        setMaxStackSize(64);
        setHasSubtypes(true);
        setHasGemSubtypes(true);
        setUnlocalizedName(Names.GEM_ROD);
        setMaxDamage(0);
    }

    @Override
    public void addRecipes() {

        for (int i = 0; i < EnumGem.all().length; ++i) {
            GameRegistry.addShapelessRecipe(new ItemStack(this, 1, i), CraftingMaterial.getStack(Names.ORNATE_STICK),
                    EnumGem.all()[i].getItem());
        }
    }
    
    @Override
    public void addOreDict() {
        
        for (int i = 0; i < EnumGem.all().length; ++i) {
            OreDictionary.registerOre(Strings.ORE_DICT_STICK_FANCY, new ItemStack(this, 1, i));
        }
    }
}