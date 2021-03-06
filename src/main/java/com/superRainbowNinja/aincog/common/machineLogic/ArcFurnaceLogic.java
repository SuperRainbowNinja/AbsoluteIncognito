package com.superRainbowNinja.aincog.common.machineLogic;

import cofh.api.energy.EnergyStorage;
import com.superRainbowNinja.aincog.AIncogData;
import com.superRainbowNinja.aincog.client.models.tileEntityRenders.MachineFrameRender;
import com.superRainbowNinja.aincog.common.capabilites.ILockableTank;
import com.superRainbowNinja.aincog.common.capabilites.LockableTankImp;
import com.superRainbowNinja.aincog.common.items.ICore;
import com.superRainbowNinja.aincog.common.items.IMachineComponent;
import com.superRainbowNinja.aincog.common.items.TankComponent;
import com.superRainbowNinja.aincog.common.tileEntity.MachineFrameTile;
import com.superRainbowNinja.aincog.util.InvUtil;
import com.superRainbowNinja.aincog.util.LogHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * Created by SuperRainbowNinja on 7/12/2016.
 *
 * TODO partial recipy checks when an attempt to insert an item is performed so only valid items can be inserted and only if they form a recipy with the otehrs
 */
public class ArcFurnaceLogic extends FluidLogic {
    public static int RF_PER_TICK = 20;

    public static class RecipeRegistry {

        private static ArrayList<ArcFurnaceRecipe> recipes = new ArrayList<>(10);

        public static void add(ArcFurnaceRecipe recipe) {
            recipes.add(recipe);
        }

        public static ArcFurnaceRecipe get(IInventory components) {
            for (ArcFurnaceRecipe rep : recipes) {
                if (rep.check(components)) {
                    return rep;
                }
            }
            return null;
        }

        private RecipeRegistry() {};
    }

    public interface ArcFurnaceRecipe {
        boolean check(IInventory stacks);
        default int processTime() {return 80;}
        FluidStack getOutput();
        void consume(IInventory tile);
    }

    public static String NAME = "arc_furnace";
    private int progress;
    //not synced or saved
    private ArcFurnaceRecipe curRecipe = null;

    public static int getSlot(EnumFacing face) {
        return face.getIndex() - 2; //if face indexs change this will need 2 be changed
    }

    public int getRfPerTick(MachineFrameTile tile) {
        ICore core = tile.getCore();
        ItemStack coreStack = tile.getCoreStack();
        return (int) (RF_PER_TICK * (1f/core.getEfficiency(coreStack)));
    }

    public boolean progressCheck(MachineFrameTile tile) {
        ICore core = tile.getCore();
        ItemStack coreStack = tile.getCoreStack();
        return progress >= (int) (curRecipe.processTime() * (1f/core.getSpeed(coreStack)));
    }


    @Override
    public void tick(MachineFrameTile tile) {
        if (!tile.getWorld().isRemote) {
            if (progress != 0) {
                if (curRecipe != null) {
                    if (progressCheck(tile)) {
                        if (curRecipe.check(tile)) { //recipe finished
                            curRecipe.consume(tile);
                            tank.setState(LockableTankImp.OutputState.IO);
                            tank.fill(curRecipe.getOutput(), true);
                            tank.setState(LockableTankImp.OutputState.OUTPUT);
                            progress = 0;
                            if (!tryRecipe(tile, curRecipe)) {
                                tile.stopOp();
                            }
                        } else {
                            resetProgress(tile);
                        }
                    } else  {
                        //update recipe, every 20 ticks make sure its still valid
                        if (tile.getWorld().getWorldTime() % 20 != 0 || curRecipe.check(tile)) {
                            int rfPerTick = getRfPerTick(tile);
                            EnergyStorage storage = tile.getEnergy();
                            if (storage.extractEnergy(rfPerTick, true) == rfPerTick) {
                                storage.extractEnergy(rfPerTick, false);
                                progress++;
                                tile.damageCore(1);
                                tile.startOp();
                                tile.markRfUpdate();
                            } else {
                                tile.stopOp();
                            }
                        } else {
                            resetProgress(tile);
                        }
                    }
                } else {
                    resetProgress(tile);
                }
            } else if (tile.getWorld().getWorldTime() % 20 == 0) { //check every 20 ticks
                ArcFurnaceRecipe recipe = RecipeRegistry.get(tile);
                if (recipe != null) {
                    tryRecipe(tile, recipe);
                }
            }
        }
    }

    private boolean tryRecipe(MachineFrameTile tile, ArcFurnaceRecipe recipe) {
        int rfPerTick = getRfPerTick(tile);
        EnergyStorage storage = tile.getEnergy();
        tank.setState(LockableTankImp.OutputState.IO);
        FluidStack output = recipe.getOutput();
        if (storage.extractEnergy(rfPerTick, true) == rfPerTick && tank.fill(output, false) == output.amount) {
            storage.extractEnergy(rfPerTick, false);
            curRecipe = recipe;
            tile.startOp();
            progress++;
            tile.damageCore(1);
            tank.setState(LockableTankImp.OutputState.OUTPUT);
            return true;
        }
        tank.setState(LockableTankImp.OutputState.OUTPUT);
        return false;
    }

    private void resetProgress(MachineFrameTile tile) {
        progress = 0;
        tile.stopOp();
    }

    @Override
    public void initMachine(MachineFrameTile tile) {
        super.initMachine(tile);
        tile.resizeInv(4);
        tile.setBatteryBehaviour(MachineFrameTile.BatteryBehaviour.ACCEPT);
        tank.setState(LockableTankImp.OutputState.OUTPUT);
    }

    @Override
    public void postDeserialize(MachineFrameTile tile) {
        super.postDeserialize(tile);
        tank.setState(LockableTankImp.OutputState.OUTPUT);
        if (progress != 0) {
            curRecipe = RecipeRegistry.get(tile);
            if (curRecipe == null) {
                resetProgress(tile);
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void insertItem(MachineFrameTile te, EntityPlayer playerIn, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!FluidUtil.tryFillContainerAndStow(heldItem, tank, new InvWrapper(playerIn.inventory),Integer.MAX_VALUE,  playerIn) && side != EnumFacing.DOWN && side != EnumFacing.UP) {
            if (InvUtil.insertIntoInvFromPlayer(playerIn, te, getSlot(side), heldItem))
                te.markVisualDirty();
            //playerIn.inventory.setInventorySlotContents(playerIn.inventory.currentItem,
            //    ItemHandlerHelper.insertItemStacked(new InvWrapper(te), heldItem, false) //comparing items can be slow, so its just faster to assume it changed
            //);
        } else {
            if (InvUtil.insertIntoInvFromPlayer(playerIn, te, 0, heldItem))
                te.markVisualDirty();
        }
    }

    @Override
    public void removeItem(MachineFrameTile te, EntityPlayer playerIn, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (side != EnumFacing.UP && side != EnumFacing.DOWN) {
            ItemStack stack = te.getStackInSlot(getSlot(side));
            if (stack != null) {
                playerIn.inventory.setInventorySlotContents(playerIn.inventory.currentItem, stack);
                te.setInventorySlotContents(getSlot(side), null);
                te.markVisualDirty();
            }
        }
    }
//TODO clean up
    @Override
    public void renderTileEntityAt(MachineFrameRender r, MachineFrameTile teIn, double x, double y, double z, float partialTicks, int destroyStage) {
        ItemStack stack = teIn.getStackInSlot(getSlot(EnumFacing.NORTH));
        if (stack != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y + 12f/16f, z + 4f/16f);
            GlStateManager.scale(1f / 4, 1f / 4, 1f / 4);
            GlStateManager.rotate(90, 0, 1, 0);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.popMatrix();
        }

        stack = teIn.getStackInSlot(getSlot(EnumFacing.SOUTH));
        if (stack != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y + 12f/16f, z + 12f/16f);
            GlStateManager.scale(1f / 4, 1f / 4, 1f / 4);
            GlStateManager.rotate(90, 0, 1, 0);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.popMatrix();
        }

        stack = teIn.getStackInSlot(getSlot(EnumFacing.EAST));
        if (stack != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 12f/16f, y + 12f/16f, z + 0.5);
            GlStateManager.scale(1f / 4, 1f / 4, 1f / 4);
            GlStateManager.rotate(90, 0, 1, 0);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.popMatrix();
        }

        stack = teIn.getStackInSlot(getSlot(EnumFacing.WEST));
        if (stack != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 4f/16f, y + 12f/16f, z + 0.5);
            GlStateManager.scale(1f / 4, 1f / 4, 1f / 4);
            GlStateManager.rotate(90, 0, 1, 0);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void spawnParticles(MachineFrameTile tile, WorldServer worldServer, BlockPos pos) {

    }

    @Override
    public void coreRemoved(MachineFrameTile teIn) {
        progress = 0;
        curRecipe = null;
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        progress = buf.readInt();
    }

    @Override
    public IMachineLogic readFromNBT(NBTTagCompound compound) {
        progress = compound.getInteger("PROGRESS");

        return null;
    }

    @Nullable
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("PROGRESS", progress);
        return compound;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return side != EnumFacing.UP && side != EnumFacing.DOWN ? new int[]{getSlot(side)} : new int[0];
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return true;
    }

    public static final class Provider implements IMachineLogicProvider {

        @Override
        public IMachineLogic validMachine(MachineFrameTile tile) {
            int coilCount = 0;
            int count = 0;
            for (IMachineComponent component : tile.getComponents()) {
                if (component instanceof TankComponent) {
                    coilCount++;
                }
                if (component instanceof AIncogData.Piece) {
                    count++;
                }
            }
            if (coilCount == 1 && count == 4) {
                return new ArcFurnaceLogic();
            }
            return null;
        }

        @Override
        public String[] getLogics() {
            return new String[]{ArcFurnaceLogic.NAME};
        }

        @Override
        public IMachineLogic deserializeLogic(String name, ByteBuf buf) {
            ArcFurnaceLogic logic = new ArcFurnaceLogic();
            logic.deserialize(buf);
            return logic;
        }

        @Override
        public IMachineLogic getLogicByName(String name) {
            return new ArcFurnaceLogic();
        }
    }
}
