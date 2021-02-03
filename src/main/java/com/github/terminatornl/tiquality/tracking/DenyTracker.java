package com.github.terminatornl.tiquality.tracking;

import com.github.terminatornl.tiquality.api.TiqualityException;
import com.github.terminatornl.tiquality.interfaces.*;
import com.github.terminatornl.tiquality.profiling.ProfilingKey;
import com.github.terminatornl.tiquality.profiling.TickLogger;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Used to remember when not to tick blocks, this prevents scanning the block list to improve performance at cost of ram.
 * <p>
 * This does not persist, and will 'slowly' repopulate when chunks are reloaded.
 */
public class DenyTracker implements Tracker {

    public static final DenyTracker INSTANCE = new DenyTracker();

    @Override
    public boolean isLoaded() {
        return true;
    }

    /**
     * Checks if the tracker is equal to one already in the database.
     * Allows for flexibility for loading.
     *
     * @param tag tag
     * @return equals
     */
    @Override
    public boolean equalsSaved(NBTTagCompound tag) {
        return true;
    }

    @Override
    public Tracker load(TiqualityWorld world, NBTTagCompound trackerTag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldSaveToDisk() {
        return false;
    }

    @Nonnull
    @Override
    public NBTTagCompound getNBT() {
        return new NBTTagCompound();
    }

    @Nonnull
    @Override
    public ProfilingKey startProfiler(long endTime) throws TiqualityException.TrackerCannotProfileException {
        throw new TiqualityException.TrackerCannotProfileException(this);
    }

    @Nonnull
    @Override
    public TickLogger stopProfiler(ProfilingKey key) throws TiqualityException.TrackerWasNotProfilingException {
        throw new TiqualityException.TrackerWasNotProfilingException(this);
    }

    /**
     * Gets the current TickLogger. Will throw an exception if access was attempted when the tracker wasn't profiling
     *
     * @return the TickLogger.
     */
    @Nonnull
    @Override
    public TickLogger getTickLogger() throws TiqualityException.TrackerWasNotProfilingException {
        throw new TiqualityException.TrackerWasNotProfilingException(this);
    }

    @Override
    public void setNextTickTime(long granted_ns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getMultiplier(GameProfile[] cache) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getRemainingTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canProfile() {
        return false;
    }

    @Override
    public boolean isProfiling() {
        return false;
    }

    @Override
    public long getProfileEndTime() {
        return 0;
    }

    @Override
    public boolean needsTick() {
        return false;
    }

    /**
     * Void the tick
     *
     * @param tileEntity t
     */
    @Override
    public void tickSimpleTickable(TiqualitySimpleTickable tileEntity) {

    }

    /**
     * Void the tick
     *
     * @param entity e
     */
    @Override
    public void tickEntity(TiqualityEntity entity) {

    }

    /**
     * Void the tick
     *
     * @param tileEntity t
     * @param runnable r
     */
    @Override
    public void tickRunnable(TiqualitySimpleTickable tileEntity, Runnable runnable) {

    }

    /**
     * Void the tick
     *
     * @param block b
     * @param world w
     * @param pos   p
     * @param state s
     * @param rand  r
     */
    @Override
    public void doBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {

    }

    /**
     * Void the tick
     *
     * @param block b
     * @param world w
     * @param pos   p
     * @param state s
     * @param rand  r
     */
    @Override
    public void doRandomBlockTick(Block block, World world, BlockPos pos, IBlockState state, Random rand) {

    }

    @Override
    public void grantTick() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTickableToQueue(TiqualitySimpleTickable tickable) {

    }

    @Override
    public void associateChunk(TiqualityChunk chunk) {

    }

    @Override
    public void associateDelegatingTracker(Tracker tracker) {

    }

    @Override
    public void removeDelegatingTracker(Tracker tracker) {

    }

    @Nonnull
    @Override
    public List<GameProfile> getAssociatedPlayers() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public TextComponentString getInfo() {
        return new TextComponentString(TextFormatting.RED + "TICK-DENIED");
    }

    @Nonnull
    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException();
    }

    /**
     * Required to check for colission with unloaded trackers.
     *
     * @return int the hash code, just like Object#hashCode().
     */
    @Override
    public int getHashCode() {
        return 0;
    }

    @Override
    public boolean shouldUnload() {
        return false;
    }

    @Override
    public void onUnload() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TrackerHolder getHolder() {
        return null;
    }

    @Override
    public void setHolder(TrackerHolder holder) {

    }
}
