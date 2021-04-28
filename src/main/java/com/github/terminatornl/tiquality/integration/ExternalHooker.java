package com.github.terminatornl.tiquality.integration;

import com.github.terminatornl.tiquality.Tiquality;
import com.github.terminatornl.tiquality.integration.bukkit.BukkitToForgeEventHandler;
import com.github.terminatornl.tiquality.mixinhelper.MixinConfigPlugin;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashSet;

public class ExternalHooker {

    public static final HashSet<String> LOADED_HOOKS = new HashSet<>();

    public static void init() {
        //Do Nothing :V
    }

    public static void initBukkitHook() {
        try {
            Class.forName("org.bukkit.Bukkit");
            MixinConfigPlugin.bukkitPresent = true;
            Tiquality.LOGGER.info("Allowing bukkit plugins to Hook on Tiquality...");
            MinecraftForge.EVENT_BUS.register(BukkitToForgeEventHandler.INSTANCE);
        }catch (ClassNotFoundException ignored){

        }
    }


}
