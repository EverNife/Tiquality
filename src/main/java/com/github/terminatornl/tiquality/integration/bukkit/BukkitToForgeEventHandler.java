package com.github.terminatornl.tiquality.integration.bukkit;

import com.github.terminatornl.tiquality.api.event.TiqualityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BukkitToForgeEventHandler {

    public static BukkitToForgeEventHandler INSTANCE = new BukkitToForgeEventHandler();

    public IEventHandlerInterpreter INTERPRETER_INSTANCE;

    private BukkitToForgeEventHandler() {
    }

    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetBlockTrackerEvent e) {
        if (this.INTERPRETER_INSTANCE != null) this.INTERPRETER_INSTANCE.onSetTracker(e);
    }
    @SubscribeEvent
    public void onSetTracker(TiqualityEvent.SetChunkTrackerEvent e) {
        if (this.INTERPRETER_INSTANCE != null) this.INTERPRETER_INSTANCE.onSetTracker(e);
    }
}
