/*
 * Silent's Gems -- UrnHelper
 * Copyright (C) 2018 SilentChaos512
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 3
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.silentchaos512.gems.lib.urn;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.silentchaos512.gems.lib.Gems;

import javax.annotation.Nullable;

public final class UrnHelper {
    private UrnHelper() {
        throw new IllegalAccessError("Utility class");
    }

    public static int getClayColor(ItemStack stack) {
        NBTTagCompound tags = getData(stack);
        return tags.contains(UrnConst.NBT_COLOR) ? tags.getInt(UrnConst.NBT_COLOR) : UrnConst.UNDYED_COLOR;
    }

    public static void setClayColor(ItemStack stack, int color) {
        getData(stack).putInt(UrnConst.NBT_COLOR, color);
    }

    @Nullable
    public static Gems getGem(ItemStack stack) {
        NBTTagCompound tags = getData(stack);
        if (tags.contains(UrnConst.NBT_GEM)) {
            String str = tags.getString(UrnConst.NBT_GEM);
            for (Gems gem : Gems.values())
                if (gem.getName().equals(str))
                    return gem;
        }
        return null;
    }

    public static void setGem(ItemStack stack, Gems gem) {
        getData(stack).putString(UrnConst.NBT_GEM, gem.getName());
    }

    public static boolean hasLid(ItemStack stack) {
        NBTTagCompound tag = getData(stack);
        return !tag.contains(UrnConst.NBT_LIDDED) || tag.getBoolean(UrnConst.NBT_LIDDED);
    }

    public static void setHasLid(ItemStack stack, boolean lidded) {
        getData(stack).putBoolean(UrnConst.NBT_LIDDED, lidded);
    }

    public static void toggleHasLid(ItemStack stack) {
        setHasLid(stack, !hasLid(stack));
    }

    private static NBTTagCompound getData(ItemStack stack) {
        return stack.getOrCreateChildTag(UrnConst.NBT_ROOT);
    }

//    public static StackList getContainedItems(ItemStack urn) {
//        NBTTagCompound tags = urn.getOrCreateSubCompound(UrnConst.NBT_ROOT);
//        return StackList.fromNBT(tags.getTagList("Items", 10));
//    }
}
