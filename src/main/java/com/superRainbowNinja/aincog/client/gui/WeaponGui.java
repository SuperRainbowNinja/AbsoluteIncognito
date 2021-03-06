package com.superRainbowNinja.aincog.client.gui;

import com.superRainbowNinja.aincog.common.containers.WeaponContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Created by SuperRainbowNinja on 10/10/2016.
 */
public class WeaponGui extends GuiContainer {
    private int xSize;
    private int ySize;

    public WeaponGui(EntityPlayer playerInv, int swordIn) {
        super(new WeaponContainer(playerInv, swordIn, true));

        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(new ModelResourceLocation("aincog:textures/gui/WeaponContainer.png"));
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
    }
}
