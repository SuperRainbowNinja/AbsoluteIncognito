package com.superRainbowNinja.aincog.util.DataHandle;

import com.superRainbowNinja.aincog.util.BufferUtils;
import com.superRainbowNinja.aincog.util.NBTUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by SuperRainbowNinja on 25/12/2016.
 */
public class InvHandle<T> implements IFieldHandle<T> {

    protected String name;
    protected Function<T, IInventory> getter;
    protected BiConsumer<T, Integer> sizeHandle;

    public InvHandle(String nameIn, Function<T, IInventory> getterIn) {
        this(nameIn, getterIn, null);
    }

    public InvHandle(String nameIn, Function<T, IInventory> getterIn, BiConsumer<T, Integer> reSizer) {
        name = nameIn;
        getter = getterIn;
        sizeHandle = reSizer;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void writeNBT(NBTTagCompound compound, T object) {
        NBTUtils.writeInventory(getter.apply(object), compound, name);
    }

    @Override
    public void readNBT(NBTTagCompound compound, T object) {
        if (sizeHandle != null) {
            sizeHandle.accept(object, NBTUtils.readInvLength(compound, name));
        }
        NBTUtils.readInventory(getter.apply(object), compound, name);
    }

    @Override
    public void toBytes(ByteBuf buf, T object) {
        BufferUtils.writeInventory(buf, getter.apply(object), sizeHandle != null);
    }

    @Override
    public void fromBytes(ByteBuf buf, T object) {
        IInventory inv = getter.apply(object);
        if (sizeHandle != null) {
            sizeHandle.accept(object, BufferUtils.readInvLength(buf));
        }
        BufferUtils.readInvItems(buf, inv);
    }
}
