package com.github.terminatornl.tiquality.mixin;

import com.github.terminatornl.tiquality.profiling.interfaces.IBlockPos;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BlockPos.class, priority = 1001, remap = false)
public abstract class MixinBlockPos implements IBlockPos {

    @Override
    public int getX() {
        return ((BlockPos)(Object)this).getX();
    }

    @Override
    public int getY() {
        return ((BlockPos)(Object)this).getY();
    }

    @Override
    public int getZ() {
        return ((BlockPos)(Object)this).getZ();
    }
}
