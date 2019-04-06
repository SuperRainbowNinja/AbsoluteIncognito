package com.superRainbowNinja.aincog.common.items;

import com.superRainbowNinja.aincog.util.LogHelper;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Created by SuperRainbowNinja on 7/12/2016.
 */
public class MakeshiftCore extends CoreItem {
    public MakeshiftCore() {
        super("makeshift_core");
    }

    @Override
    public int getMaxCoreDamage(ItemStack core) {
        return 2000;
    }

    @Override
    public int getCoreDamage(ItemStack core) {
        return core.getItemDamage();
    }

    @Override
    public boolean setCoreDamage(ItemStack core, int dmg) {
        core.setItemDamage(dmg);
        return dmg == getMaxCoreDamage(core);
    }

    @Override
    public ICore.CoreType getCoreType(ItemStack core) {
        return CoreType.PITIFUL;
    }

    @Override
    public int getPrimaryColor(ItemStack core) {
        return 0xD8D8D8;
    }

    @Override
    public int getSecondaryColor(ItemStack core) {
        return 0xFF5959;
    }

    @Override
    public int getOutlineColor(ItemStack core) {
        return 0xffffff;
    }
    //0x1e6e08
    //0x28f005
    //0x10800

    @Override
    public float getEfficiency(ItemStack core) {
        return 0.5f;
    }

    @Override
    public float getSpeed(ItemStack core) {
        return 0.5f;
    }

    @Override
    public float getStrength(ItemStack stack) {
        return 0.5f;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerModels() {
        LogHelper.infoLog("added makeshiftcore");
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation("aincog:makeshift_core", "inventory"));
    }
}
