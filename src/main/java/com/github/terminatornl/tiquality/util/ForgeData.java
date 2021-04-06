package com.github.terminatornl.tiquality.util;

import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.integration.bukkit.CustomGameProfilerProvider;
import com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;

/**
 * This is a mess. There should be something easier... Oh well.
 */
public class ForgeData {

    public static final GameProfile GAME_PROFILE_NOBODY = new GameProfile(new UUID(9223372036854775800L, Long.MAX_VALUE), "[Unknown]");

    public static final TreeSet<UUID> WEIRD_GAME_PROFILES = new TreeSet<>();

    public static final MinecraftServer SERVER = FMLCommonHandler.instance().getMinecraftServerInstance();

    public static CustomGameProfilerProvider CUSTOM_GAMEPROFILER_PROVIDER;

    /**
     * Gets the profile, but can reach out and contact mojang's servers if it failed.
     * "But Term! Networking on the main thread is wrong!"
     * I like to live dangerously. Besides, this beats a crash any day!
     *
     * @param uuid uuid
     * @return GameProfile
     */
    public static @Nonnull
    GameProfile getGameProfileByUUID(@Nonnull UUID uuid) {
        /*
                This works 99% of the time
         */
        GameProfile profile = SERVER.getPlayerProfileCache().getProfileByUUID(uuid);
        if (profile != null) {
            return profile;
        }

        /*
         * Our own cache, for funky game profiles.
         */
        if (WEIRD_GAME_PROFILES.contains(uuid)) {
            return GAME_PROFILE_NOBODY;
        }

        if (CUSTOM_GAMEPROFILER_PROVIDER != null) {
            /*
             *  We try to let bukkit get it for us.
             */
            profile = (GameProfile) CUSTOM_GAMEPROFILER_PROVIDER.provideGameProfile(uuid);
            if (profile != null){
                try {
                    SERVER.getPlayerProfileCache().addEntry(profile);
                }catch (Exception e){
                    Tiquality.LOGGER.warn("Fail adding GameProfile from: " + profile.toString());
                    e.printStackTrace();
                }
                return profile;
            }
        }

        Tiquality.LOGGER.warn("Player profile was not found in cache!");
        Tiquality.LOGGER.warn("I will add it, but it can cause some lag, as I may or may not contact the Mojang servers to get go and get it.");
        Tiquality.LOGGER.warn("UUID: " + uuid.toString());
        Tiquality.LOGGER.warn("Most significant bits:  " + uuid.getMostSignificantBits());
        Tiquality.LOGGER.warn("Least significant bits: " + uuid.getLeastSignificantBits());

        /*
         *  We try to let sponge get it for us.
         */
        if (MixinConfigPlugin.spongePresent) {
            profile = SpongeData.getProfileByUUID(uuid);
        }

        /*
            If sponge is not installed, or does not work, we reflectively attempt to get a fake player's profile.
         */
        try {
            Field fakePlayers = FakePlayerFactory.class.getDeclaredField("fakePlayers");
            fakePlayers.setAccessible(true);

            //noinspection unchecked
            Map<GameProfile, FakePlayer> map = (Map<GameProfile, FakePlayer>) fakePlayers.get(null);
            for (GameProfile fakePlayerProfile : map.keySet()) {
                if (fakePlayerProfile.getId() == uuid) {
                    SERVER.getPlayerProfileCache().addEntry(fakePlayerProfile);
                    return fakePlayerProfile;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        /*
                If that did not work, we try another way, which may or may not contact mojang.
         */
        if (profile == null) {
            profile = new GameProfile(uuid, null);
            try {
                SERVER.getMinecraftSessionService().fillProfileProperties(profile, false);
            } catch (Exception e) {
                Tiquality.LOGGER.warn("Failed attempt to fill profile (Stage 1)");
                e.printStackTrace();
            }
        }
        /*
                If it still did not, we make sure to contact mojang.
         */
        if (profile.getName() == null) {
            try {
                SERVER.getMinecraftSessionService().fillProfileProperties(profile, true);
            } catch (Exception e) {
                Tiquality.LOGGER.warn("Failed attempt to fill profile (Stage 2)");
                e.printStackTrace();
            }
        }


        /*
                If it still did not, we give up, and return a dummy GameProfile.
                We also save the UUID to our own cache, to make sure this weird thing doesn't contaminate the forge cache.
         */
        if (profile.getName() == null) {
            Tiquality.LOGGER.warn("This UUID cannot be resolved no matter what I do. (Fake player?)");
            Tiquality.LOGGER.warn("Temporarily storing this UUID for quick lookup. During this run, we're returning the following profile: " + GAME_PROFILE_NOBODY.toString());
            WEIRD_GAME_PROFILES.add(uuid);
            return GAME_PROFILE_NOBODY;
        }

        /*
                We save the result, making sure we don't have to ever do this again for this profile
         */
        SERVER.getPlayerProfileCache().addEntry(profile);
        return profile;
    }

    @Nullable
    public static GameProfile getGameProfileByName(String name) {
        return SERVER.getPlayerProfileCache().getGameProfileForUsername(name);
    }
}
