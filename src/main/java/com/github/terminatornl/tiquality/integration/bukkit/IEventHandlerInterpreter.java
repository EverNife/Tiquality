package com.github.terminatornl.tiquality.integration.bukkit;

import com.github.terminatornl.tiquality.api.event.TiqualityEvent;

public interface IEventHandlerInterpreter {
    void onSetTracker(TiqualityEvent.SetBlockTrackerEvent event);
    void onSetTracker(TiqualityEvent.SetChunkTrackerEvent event);
}
