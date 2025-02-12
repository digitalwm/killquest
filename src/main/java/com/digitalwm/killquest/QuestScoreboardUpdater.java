package com.hvm24.killquest;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.scoreboard.Scoreboard;
import cn.nukkit.scoreboard.Scoreboard.SortOrder;
import cn.nukkit.scoreboard.Scoreboard.DisplaySlot;
import com.hvm24.killquest.ActiveQuest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestScoreboardUpdater implements Runnable {

    private final KillQuestPlugin plugin;

    public QuestScoreboardUpdater(KillQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (!player.spawned) {
                continue; // Skip players who haven't fully loaded.
            }

            ActiveQuest aq = plugin.getActiveQuests().get(player.getName());

            if (aq == null) {
                // Remove scoreboard if no active quest.
                if (plugin.scoreboards.containsKey(player)) {
                    plugin.scoreboards.get(player).clear();
                    plugin.scoreboards.remove(player);
                }
                continue;
            }

            Quest quest = aq.getQuest();
            QuestProgress progress = aq.getProgress();

            Scoreboard scoreboard = plugin.scoreboards.getOrDefault(player, new Scoreboard("QuestTracker", SortOrder.ASCENDING, DisplaySlot.SIDEBAR));
            scoreboard.holdUpdates();

            boolean needsUpdate = false;
            List<String> lines = new ArrayList<>();

            lines.add("§6U:" + quest.getName());
            lines.add("§9" + quest.getReward() + " credits");

            // Add kill progress
            for (Map.Entry<String, Integer> entry : quest.getKillTargets().entrySet()) {
                String target = entry.getKey();
                int required = entry.getValue();
                int current = progress.getKills().getOrDefault(target, 0);
                String translatedTarget = plugin.translate(player, "entity.minecraft." + target.toLowerCase());
                String line = "§c" + translatedTarget;
                lines.add(line);
                line = "   " + current + "/" + required;
                lines.add(line);

                if (!scoreboard.getScores().containsKey(line)) {
                    needsUpdate = true;
                }
            }

            // Add gather progress
            for (Map.Entry<String, Integer> entry : quest.getGatherItems().entrySet()) {
                String item = entry.getKey();
                int required = entry.getValue();
                int current = progress.getGather().getOrDefault(item, 0);
                String translatedItem = plugin.translate(player, "item.minecraft." + item.toLowerCase());
                String line = "§a" + translatedItem;
                lines.add(line);
                line = "   " + current + "/" + required;
                lines.add(line);

                if (!scoreboard.getScores().containsKey(line)) {
                    needsUpdate = true;
                }
            }

            if (needsUpdate) {
                scoreboard.clear();
                int lineNumber = 0;
                for (String text : lines) {
                    scoreboard.setScore(text, lineNumber++);
                }
            }

            scoreboard.unholdUpdates();
            plugin.scoreboards.put(player, scoreboard);
        }
    }
}