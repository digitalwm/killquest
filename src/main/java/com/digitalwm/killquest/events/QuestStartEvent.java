package com.digitalwm.killquest.events;
import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import cn.nukkit.Player;

// Event for when a user starts a quest
public class QuestStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String questName;

    public QuestStartEvent(Player player, String questName) {
        this.player = player;
        this.questName = questName;
    }

    public Player getPlayer() {
        return player;
    }

    public String getQuestName() {
        return questName;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }
}