package com.digitalwm.killquest.events;
import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import cn.nukkit.Player;

// Event for when a user starts a jumping puzzle
public class JumpPuzzleStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String puzzleName;

    public JumpPuzzleStartEvent(Player player, String puzzleName) {
        this.player = player;
        this.puzzleName = puzzleName;
    }

    public Player getPlayer() {
        return player;
    }

    public String getPuzzleName() {
        return puzzleName;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }
}