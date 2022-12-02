package edebe.mana_box.common;

import edebe.mana_box.ManaBox;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaItem;
import vazkii.botania.api.mana.IManaTooltipDisplay;
import vazkii.botania.common.core.helper.ItemNBTHelper;
import vazkii.botania.common.item.ModItems;

import javax.annotation.Nonnull;

public class BlockItemManaBox extends BlockItem implements IManaItem, IManaTooltipDisplay {
    public static final int MAX_MANA = 1000000;

    private static final String TAG_MANA = "mana";
    private static final String TAG_COLOR = "color";
    private static final String TAG_ONE_USE = "oneUse";

    public BlockItemManaBox() {
        super(ManaBox.MANA_BOX_BLOCK, ModItems.defaultBuilder().maxStackSize(1).rarity(Rarity.RARE));
    }

    @Override
    public void fillItemGroup(@Nonnull ItemGroup tab, @Nonnull NonNullList<ItemStack> stacks) {
        if (this.isInGroup(tab)) {
            stacks.add(new ItemStack(this));

            final ItemStack fullPower = new ItemStack(this);
            setMana(fullPower, MAX_MANA);
            stacks.add(fullPower);
        }
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, World world) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMana(ItemStack stack) {
        return ItemNBTHelper.getInt(stack, TAG_MANA, 0) * stack.getCount();
    }

    @Override
    public int getMaxMana(ItemStack stack) {
        return MAX_MANA * stack.getCount();
    }

    @Override
    public void addMana(ItemStack stack, int mana) {
        setMana(stack, Math.min(getMana(stack) + mana, getMaxMana(stack)) / stack.getCount());
    }

    @Override
    public boolean canReceiveManaFromPool(ItemStack stack, TileEntity pool) {
        return !ItemNBTHelper.getBoolean(stack, TAG_ONE_USE, false);
    }

    @Override
    public boolean canReceiveManaFromItem(ItemStack stack, ItemStack otherStack) {
        return true;
    }

    @Override
    public boolean canExportManaToPool(ItemStack stack, TileEntity pool) {
        return true;
    }

    @Override
    public boolean canExportManaToItem(ItemStack stack, ItemStack otherStack) {
        return true;
    }

    @Override
    public boolean isNoExport(ItemStack stack) {
        return false;
    }

    @Override
    public float getManaFractionForDisplay(ItemStack stack) {
        return (float) getMana(stack) / (float) getMaxMana(stack);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - getManaFractionForDisplay(stack);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return MathHelper.hsvToRGB(getManaFractionForDisplay(stack) / 3.0F, 1.0F, 1.0F);
    }

    public static int getBlockMana(ItemStack stack) {
        return ItemNBTHelper.getInt(stack, TAG_MANA, 0);
    }

    public static void setMana(ItemStack stack, int mana) {
        ItemNBTHelper.setInt(stack, TAG_MANA, mana);
    }

    public static DyeColor getColor(ItemStack stack) {
        return DyeColor.byId(ItemNBTHelper.getInt(stack, TAG_COLOR, 0));
    }

    public static void setColor(ItemStack stack, DyeColor color) {
        ItemNBTHelper.setInt(stack, TAG_COLOR, color.getId());
    }
}
