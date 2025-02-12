package com.hvm24.killquest;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerLocallyInitializedEvent;
import cn.nukkit.event.player.PlayerQuitEvent;

public class ScoreboardListeners implements Listener {
    private final KillQuestPlugin plugin;

    public ScoreboardListeners(KillQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerLocallyInitializedEvent event) {
        Player player = event.getPlayer();
        plugin.createScoreboard(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.destroyScoreboard(event.getPlayer());
    }
}
