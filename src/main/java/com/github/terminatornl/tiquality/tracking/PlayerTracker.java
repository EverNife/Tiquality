package com.github.terminatornl.tiquality.tracking;

import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.TiqualityConfig;
import com.github.terminatornl.tiquality.interfaces.TiqualityWorld;
import com.github.terminatornl.tiquality.interfaces.Tracker;
import com.github.terminatornl.tiquality.util.ForgeData;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import java.util.*;

import static com.github.terminatornl.tiquality.Tiquality.PREFIX;
import static com.github.terminatornl.tiquality.util.Utils.TWO_DECIMAL_FORMATTER;

@SuppressWarnings({"WeakerAccess"})
public class PlayerTracker extends TrackerBase {

    private final GameProfile profile;
    private final Set<Long> sharedTo = new HashSet<>();
    private final Set<Long> sharedFrom = new HashSet<>();
    private final HashMap<String, ClaimOverrideRequester> pendingRequests = new HashMap<>();
    private boolean notifyUser = true;
    private long nextMessageMillis = 0L;
    private TickWallet wallet = new TickWallet();

    /**
     * Required.
     */
    public PlayerTracker() {
        profile = ForgeData.GAME_PROFILE_NOBODY;
    }

    /**
     * Creates the tracker
     *
     * @param profile a given game profile
     */
    public PlayerTracker(@Nonnull GameProfile profile) {
        super();
        this.profile = profile;
    }

    /*
     * Gets the tracker for a player, if no one exists yet, it will create one. Never returns null.
     * @param profile the profile to bind this tracker to the profile MUST contain an UUID!
     * @return the associated PlayerTracker
     */
    @Nonnull
    public static PlayerTracker getOrCreatePlayerTrackerByProfile(TiqualityWorld world, @Nonnull final GameProfile profile) {
        UUID id = profile.getId();
        if (id == null) {
            throw new IllegalArgumentException("GameProfile must have an UUID");
        }

        PlayerTracker tracker = TrackerManager.foreach(new TrackerManager.Action<PlayerTracker>() {
            @Override
            public void each(Tracker tracker) {
                if (tracker instanceof PlayerTracker) {
                    PlayerTracker playerTracker = (PlayerTracker) tracker;
                    if (playerTracker.getOwner().getId().equals(id)) {
                        stop(playerTracker);
                    }
                }
            }
        });

        return tracker != null ? tracker : TrackerManager.createNewTrackerHolder(world, new PlayerTracker(profile)).getTracker();
    }

    public List<TextComponentString> getSharedToTextual(TiqualityWorld world) {
        LinkedList<TextComponentString> list = new LinkedList<>();
        for (long id : sharedTo) {
            TrackerHolder holder = TrackerHolder.getTrackerHolder(world, id);
            if (holder == null || holder.getTracker() instanceof PlayerTracker == false) {
                switchSharedTo(id);
                list.add(new TextComponentString(TextFormatting.RED + "Tracker ID: " + id + " removed!"));
            } else {
                list.add(new TextComponentString(TextFormatting.WHITE + ((PlayerTracker) holder.getTracker()).getOwner().getName()));
            }
        }
        return list;
    }

    public String getOwnerName(){
        return profile.getName();
    }

    public UUID getOwnerUUID(){
        return profile.getId();
    }

    public boolean switchNotify() {
        notifyUser = notifyUser == false;
        return notifyUser;
    }

    public boolean switchSharedTo(long id) {
        if (sharedTo.contains(id) == false) {
            sharedTo.add(id);
            getHolder().update();
            return true;
        } else {
            sharedTo.remove(id);
            getHolder().update();
            return false;
        }
    }

    /**
     * Requests a claim override.
     *
     * @param world    The world
     * @param leastPos .
     * @param mostPos  .
     * @param requester the callback to run upon success or failure.
     * @throws CommandException If the user already has a pending request.
     */
    public void requestClaimOverride(ClaimOverrideRequester requester, World world, BlockPos leastPos, BlockPos mostPos) throws CommandException {
        EntityPlayerMP thisPlayer = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(this.getOwner().getId());
        //noinspection ConstantConditions (Player == null can be true.)
        if (thisPlayer == null || thisPlayer.hasDisconnected()) {
            requester.onOfflineOwner(this);
            return;
        }
        String requesterName = requester.getProfile().getName();

        String acceptString = "/tiquality acceptoverride " + requesterName;
        String denyString = "/tiquality denyoverride " + requesterName;

        if (pendingRequests.containsKey(requesterName.toLowerCase())) {
            throw new CommandException("You already have another pending request. Inform the owner to run: '" + acceptString + "' to accept your last request, or '" + denyString + "' to reject it.");
        } else {
            pendingRequests.put(requesterName.toLowerCase(), requester);
        }
        thisPlayer.sendMessage(new TextComponentString(PREFIX + requesterName + " wishes to claim an area that overlaps or is near your claim:"));
        thisPlayer.sendMessage(new TextComponentString(TextFormatting.GRAY + "  Affected area: X=" + leastPos.getX() + " Z=" + leastPos.getZ() + " to X=" + mostPos.getX() + " Z=" + mostPos.getZ()));
        thisPlayer.sendMessage(new TextComponentString(TextFormatting.GRAY + "  Affected dimension: " + world.provider.getDimension()));
        thisPlayer.sendMessage(new TextComponentString(TextFormatting.GRAY + "  Surface area of total claim: " + (mostPos.getX() - leastPos.getX()) * (mostPos.getZ() - leastPos.getZ())));
        thisPlayer.sendMessage(new TextComponentString(TextFormatting.GRAY + "To accept this request use: " + TextFormatting.GREEN + acceptString));
        thisPlayer.sendMessage(new TextComponentString(TextFormatting.GRAY + "To deny this request use: " + TextFormatting.RED + denyString));
    }

    public List<String> getOverrideRequests() {
        return new LinkedList<>(pendingRequests.keySet());
    }

    public void acceptOverride(ICommandSender sender, String name) throws CommandException {
        ClaimOverrideRequester request = pendingRequests.remove(name.toLowerCase());
        if (request != null) {
            sender.sendMessage(new TextComponentString(PREFIX + "Accepted!"));
            request.onAccept(this);
        } else {
            throw new CommandException("Request for: '" + name + "' was not found.");
        }
    }

    public void denyOverride(ICommandSender sender, String name) throws CommandException {
        ClaimOverrideRequester request = pendingRequests.remove(name.toLowerCase());
        if (request != null) {
            sender.sendMessage(new TextComponentString(PREFIX + "Denied override!"));
            request.onDeny(this);
        } else {
            throw new CommandException("Request for: '" + name + "' was not found.");
        }
    }

    public void addWallet(TickWallet wallet, Long from) {
        this.wallet.addWallet(wallet);
        this.sharedFrom.add(from);
    }

    public TickWallet getWallet() {
        return this.wallet;
    }

    @Override
    public void setNextTickTime(long time) {
        super.setNextTickTime(time);
        wallet.clearWallets();
        wallet.setRemainingTime(time);
        sharedFrom.clear();
    }

    @Override
    public void tick() {
        super.tick();
        for (Long id : sharedTo) {
            Tracker tracker = TrackerManager.getTrackerByID(id);
            if (tracker instanceof PlayerTracker) {
                ((PlayerTracker) tracker).addWallet(this.wallet, this.getHolder().getId());
            }
        }
    }

    /**
     * Notify this tracker about it's performance falling behind.
     *
     * @param ratio the tracker's speed compared to the server tick time.
     */
    public void notifyFallingBehind(double ratio) {
        if (TiqualityConfig.DEFAULT_THROTTLE_WARNING_INTERVAL_SECONDS > 1 && notifyUser && System.currentTimeMillis() > nextMessageMillis) {
            nextMessageMillis = System.currentTimeMillis() + (TiqualityConfig.DEFAULT_THROTTLE_WARNING_INTERVAL_SECONDS * 1000);
            Entity e = FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(getOwner().getId());
            if (e instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) e;
                player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Atenção: " + TextFormatting.GRAY + "Os seus blocos estão funcionando a apenas  " + (Math.round(ratio * 10000D) / 100D) + "% da velocidade maxima." + TextFormatting.DARK_GRAY + " (/mylag notify off)"), true);
                double serverTPS_raw = Tiquality.TPS_MONITOR.getAverageTPS();

                String serverTPS = TWO_DECIMAL_FORMATTER.format(Math.round(serverTPS_raw * 100D) / 100D);
                String playerTPS = TWO_DECIMAL_FORMATTER.format(Math.round(serverTPS_raw * ratio * 100D) / 100D);


                player.sendMessage(new TextComponentString(PREFIX + "Seu TPS: " + TextFormatting.WHITE + playerTPS + TextFormatting.GRAY + " (" + TextFormatting.WHITE + Math.round(ratio * 100D) + "%" + TextFormatting.GRAY + ")" + TextFormatting.DARK_GRAY + " (/mylag notify off)"));
            }
        }
    }

    /**
     * Decreases the remaining tick time for a tracker.
     *
     * @param time in nanoseconds
     */
    @Override
    public void consume(long time) {
        wallet.consume(time);
    }

    @Override
    public long getRemainingTime() {
        return wallet.getTimeLeft();
    }

    @Override
    public Tracker load(TiqualityWorld world, NBTTagCompound trackerTag) {
        PlayerTracker tracker = new PlayerTracker(ForgeData.getGameProfileByUUID(new UUID(trackerTag.getLong("uuidMost"), trackerTag.getLong("uuidLeast"))));
        if (trackerTag.hasKey("shared")) {
            NBTTagList shared = trackerTag.getTagList("shared", 4);
            for (NBTBase base : shared) {
                tracker.sharedTo.add(((NBTTagLong) base).getLong());
            }
        }
        tracker.notifyUser = trackerTag.getBoolean("notify");
        return tracker;
    }

    /**
     * Checks if the owner of this tracker is online or not.
     *
     * @param onlinePlayerProfiles an array of online players
     * @return true if online
     */
    public boolean isPlayerOnline(final GameProfile[] onlinePlayerProfiles) {
        for (GameProfile profile : onlinePlayerProfiles) {
            if (this.profile.getId().equals(profile.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the NBT data from this object, is called when the tracker is saved to disk.
     */
    @Nonnull
    @Override
    public NBTTagCompound getNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("uuidMost", profile.getId().getMostSignificantBits());
        tag.setLong("uuidLeast", profile.getId().getLeastSignificantBits());
        tag.setBoolean("notify", notifyUser);
        if (sharedTo.size() > 0) {
            NBTTagList sharedToTag = new NBTTagList();
            for (long id : sharedTo) {
                sharedToTag.appendTag(new NBTTagLong(id));
            }
            tag.setTag("shared", sharedToTag);
        }
        /* Human readable names and UUID's. These are not accessed.*/
        tag.setString("name", profile.getName());
        tag.setString("uuid", profile.getId().toString());
        return tag;
    }

    /**
     * Gets the tick time multiplier for the PlayerTracker.
     * This is used to distribute tick time in a more controlled manner.
     *
     * @param cache The current online player cache
     * @return the multiplier
     */
    public double getMultiplier(final GameProfile[] cache) {
        return isPlayerOnline(cache) ? 1 : TiqualityConfig.OFFLINE_PLAYER_TICK_TIME_MULTIPLIER;
    }

    /**
     * Gets the associated player for this tracker
     *
     * @return a list containing just 1 player.
     */
    @Override
    @Nonnull
    public List<GameProfile> getAssociatedPlayers() {
        List<GameProfile> list = new ArrayList<>();
        list.add(profile);
        return list;
    }

    /**
     * Gets the owner corresponding to this PlayerTracker.
     *
     * @return the owner's profile
     */
    public GameProfile getOwner() {
        return profile;
    }

    /**
     * Gets the ids of other Trackers this Tracker
     * is sharing with
     *
     * @return all shared trackers's ids
     */
    public Set<Long> getSharedTo() {
        return sharedTo;
    }

    /**
     * Gets the ids of other Trackers that are
     * sharing with this Tracker
     *
     * @return all sharedFrom trackers's ids
     */
    public Set<Long> getSharedFrom() {
        return sharedFrom;
    }

    /**
     * Gets the GameProfile from this PlayerTracker
     *
     * @return player's GameProfile
     */
    public GameProfile getProfile() {
        return profile;
    }

    /**
     * Get if this tracker is notifying its user
     *
     * @return nofity state
     */
    public boolean isNotifyUser() {
        return notifyUser;
    }

    /**
     * Debugging method. Do not use in production environments.
     *
     * @return description
     */
    @Override
    public String toString() {
        return "PlayerTracker:{Owner: '" + getOwner().getName() + "', nsleft: " + tick_time_remaining_ns + ", unticked: " + tickQueue.size() + ", hashCode: " + System.identityHashCode(this) + "}";
    }

    /**
     * @return the info describing this TrackerBase (Like the owner)
     */
    @Nonnull
    @Override
    public TextComponentString getInfo() {
        return new TextComponentString(TextFormatting.GREEN + "Tracked by: " + TextFormatting.AQUA + getOwner().getName());
    }

    /**
     * @return an unique identifier for this TrackerBase type, used to re-instantiate the tracker later on.
     */
    @Nonnull
    public String getIdentifier() {
        return "PlayerTracker";
    }

    /**
     * Required to check for colission with unloaded trackers.
     *
     * @return int the hash code, just like Object#hashCode().
     */
    @Override
    public int getHashCode() {
        return getOwner().getId().hashCode();
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
        UUID ownerUUID = new UUID(tag.getLong("uuidMost"), tag.getLong("uuidLeast"));
        return ownerUUID.equals(getOwner().getId());
    }

    @SuppressWarnings("IfMayBeConditional") /* Too confusing */
    @Override
    public boolean equals(Object o) {
        if (o instanceof PlayerTracker == false) {
            return false;
        } else {
            return o == this || this.getOwner().getId().equals(((PlayerTracker) o).getOwner().getId());
        }
    }

    @Override
    public int hashCode() {
        return getOwner().getId().hashCode();
    }

    public interface ClaimOverrideRequester {
        void onDeny(PlayerTracker tracker);

        void onOfflineOwner(PlayerTracker tracker);

        void onAccept(PlayerTracker tracker);

        GameProfile getProfile();
    }
}
