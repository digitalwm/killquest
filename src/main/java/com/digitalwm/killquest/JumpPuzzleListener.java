package com.digitalwm.killquest;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.math.Vector3;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerMoveEvent;

public class JumpPuzzleListener implements Listener {

    private final KillQuestPlugin plugin;

    public JumpPuzzleListener(KillQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // ✅ Cycle through all active puzzles and trigger their event handling
        for (JumpPuzzleGenerator puzzle : plugin.getActiveJumpPuzzles()) {
            if (puzzle.isPlayerInside(player)) {
                puzzle.handlePlayerMovement(event);
                break; // Stop checking after finding one matching puzzle
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Vector3 pos = event.getBlock().getLocation();

        for (JumpPuzzleGenerator puzzle : plugin.getActiveJumpPuzzles()) {
            if (puzzle.isPlayerInside(player)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot place blocks inside the jumping puzzle!");
                plugin.getLogger().info("Blocked " + player.getName() + " from placing a block in the puzzle area.");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Vector3 pos = event.getBlock().getLocation();

        for (JumpPuzzleGenerator puzzle : plugin.getActiveJumpPuzzles()) {
            if (puzzle.isPlayerInside(player)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot break blocks inside the jumping puzzle!");
                plugin.getLogger().info("Blocked " + player.getName() + " from breaking a block in the puzzle area.");
            }
        }
    }
}
