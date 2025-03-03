package com.digitalwm.killquest;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.CommandExecutor;

import cn.nukkit.event.Listener;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerFishEvent;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerMoveEvent;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;

import cn.nukkit.entity.Entity;
import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;

import java.io.IOException;

// Import form API classes using the proper packages:
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.element.ElementButton;

// Scoreboard

import cn.nukkit.scoreboard.Scoreboard;

// EconomyAPI import:
import me.onebone.economyapi.EconomyAPI;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Own Events

import com.digitalwm.killquest.events.JumpPuzzleStartEvent;
import com.digitalwm.killquest.events.JumpPuzzleEndEvent;
import com.digitalwm.killquest.events.JumpPuzzleTimeoutEvent;
import com.digitalwm.killquest.events.QuestStartEvent;
import com.digitalwm.killquest.events.QuestEndEvent;

public class KillQuestPlugin extends PluginBase implements Listener, CommandExecutor {

    // List of all available quests (loaded from quests.yml)
    private List<Quest> questList = new ArrayList<>();

    // Mapping of player's name to their active quest (only one active quest at a time)
    public Map<String, ActiveQuest> activeQuests = new HashMap<>();
    private final Map<String, Quest> selectedQuests = new HashMap<>();

    public Map<Player, Scoreboard> scoreboards = new HashMap<>();
    public static final String SCOREBOARD_TITLE = "§6Active Quest";

    // Mapping for player quest selection and timestamps
    private final Map<String, List<Quest>> playerQuestSelections = new ConcurrentHashMap<>();
    private final Map<String, Long> playerQuestTimestamps = new ConcurrentHashMap<>();
    private static final int QUEST_REFRESH_TIME_MS = 5 * 60 * 1000; // 5 minutes

    // Translations
    private final Map<String, Map<String, String>> translations = new HashMap<>();

    // Folder to store per-player active quest data.
    private File playersFolder;

    // Reference to the EconomyAPI instance.
    private EconomyAPI economy = null;

    // Active Jump Puzzles
    private final Map<String, JumpPuzzleGenerator> activeJumpPuzzles = new HashMap<>();

    private File puzzlesFile;

    public Map<String, ActiveQuest> getActiveQuests() {
        return activeQuests;
    }

    @Override
    public void onEnable() {
        logBanner();
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        playersFolder = new File(getDataFolder(), "players");
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
        loadQuests();
        loadTranslations();
        getServer().getPluginManager().registerEvents(this, this);
        // Do not call setExecutor – Nukkit will use onCommand from your main plugin class.
        getLogger().info("KillQuestPlugin enabled with " + questList.size() + " available quests.");

        // Retrieve the EconomyAPI plugin.
        Plugin econPlugin = getServer().getPluginManager().getPlugin("EconomyAPI");
        if (econPlugin != null && econPlugin instanceof EconomyAPI) {
            economy = (EconomyAPI) econPlugin;
            getLogger().info("EconomyAPI found. Rewards enabled.");
        } else {
            getLogger().warning("EconomyAPI not found. Players will not receive credits.");
        }

        // Schedule a repeating task to save active quest progress every minute (1200 ticks).
        getServer().getScheduler().scheduleRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers().values()) {
                    saveActiveQuestProgress(player.getName());
                }
            }
        }, 1200);

        getServer().getScheduler().scheduleRepeatingTask(this, new QuestScoreboardUpdater(this), 40);
        getServer().getPluginManager().registerEvents(new ScoreboardListeners(this), this);
        getServer().getPluginManager().registerEvents(new JumpPuzzleListener(this), this);

        getLogger().info("Loading saved puzzles...");

        // Ensure data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize puzzle file if missing (but do NOT overwrite existing ones)
        puzzlesFile = new File(getDataFolder(), "puzzles.yml");
        if (!puzzlesFile.exists()) {
            try {
                puzzlesFile.createNewFile();
                getLogger().info("Created new puzzles.yml file.");
            } catch (IOException e) {
                getLogger().warning("Failed to create puzzles.yml: " + e.getMessage());
            }
        }

        loadJumpPuzzles(); // ✅ Load puzzles AFTER ensuring file exists
    }

    public void onJumpPuzzleStart(Player player, String puzzleName) {
        JumpPuzzleStartEvent event = new JumpPuzzleStartEvent(player, puzzleName);
        getServer().getInstance().getPluginManager().callEvent(event);
        getLogger().info("Jump puzzle started by " + player.getName() + " for puzzle: " + puzzleName);
    }

    public void onJumpPuzzleEnd(Player player, String puzzleName) {
        JumpPuzzleEndEvent event = new JumpPuzzleEndEvent(player, puzzleName);
        getServer().getInstance().getPluginManager().callEvent(event);
        getLogger().info("Jump puzzle ended by " + player.getName() + " for puzzle: " + puzzleName);
    }

    public void onJumpPuzzleTimeout(Player player, String puzzleName) {
        JumpPuzzleTimeoutEvent event = new JumpPuzzleTimeoutEvent(player, puzzleName);
        getServer().getInstance().getPluginManager().callEvent(event);
        getLogger().info("Jump puzzle timed out for player: " + player.getName() + " for puzzle: " + puzzleName);
    }

    public void onQuestStart(Player player, String questName) {
        QuestStartEvent event = new QuestStartEvent(player, questName);
        getServer().getInstance().getPluginManager().callEvent(event);
        getLogger().info("Quest started by " + player.getName() + ": " + questName);
    }

    public void onQuestEnd(Player player, String questName) {
        QuestEndEvent event = new QuestEndEvent(player, questName);
        getServer().getInstance().getPluginManager().callEvent(event);
        getLogger().info("Quest ended by " + player.getName() + ": " + questName);
    }

    public Map<String, JumpPuzzleGenerator> getActiveJumpPuzzles() {
        return activeJumpPuzzles;
    }

    @Override
    public void onDisable() {
        for (String playerName : activeQuests.keySet()) {
            saveActiveQuestProgress(playerName);
        }
        getLogger().info("KillQuestPlugin disabled and active quest progress saved.");
        saveJumpPuzzles();
        getLogger().info("Puzzles saved.");
    }

    private void logBanner() {
        String version = getDescription().getVersion(); // Fetch version from plugin.yml

        String[] banner = {
            "§2", // Green color
            "§2██╗  ██╗██╗██╗     ██╗      ██████╗ ██╗   ██╗███████╗███████╗████████╗",
            "§2██║ ██╔╝██║██║     ██║     ██╔═══██╗██║   ██║██╔════╝██╔════╝╚══██╔══╝",
            "§2█████╔╝ ██║██║     ██║     ██║   ██║██║   ██║█████╗  ███████╗   ██║   ",
            "§2██╔═██╗ ██║██║     ██║     ██║▄▄ ██║██║   ██║██╔══╝  ╚════██║   ██║   ",
            "§2██║  ██╗██║███████╗███████╗╚██████╔╝╚██████╔╝███████╗███████║   ██║   ",
            "§2╚═╝  ╚═╝╚═╝╚══════╝╚══════╝ ╚══▀▀═╝  ╚═════╝ ╚══════╝╚══════╝   ╚═╝   ",
            "§2",
            "                           Version: §e" + version,
            "                           Developed by §4digitalwm",
            "§2"
        };

        for (String line : banner) {
            getLogger().info(line);
        }
    }

    private void loadTranslations() {
        File file = new File(getDataFolder(), "translations.yml");
        if (!file.exists()) {
            saveResource("translations.yml", false);
        }

        Config config = new Config(file, Config.YAML);
        Map<String, Object> section = config.get("translations", new HashMap<>());

        int totalLanguages = 0;  // Counter for total languages
        int totalKeys = 0;       // Counter for total translation keys

        for (String lang : section.keySet()) {
            Map<String, Object> rawMap = config.getSection("translations." + lang).getAllMap();
            Map<String, String> translationsMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                translationsMap.put(entry.getKey(), entry.getValue().toString());
            }

            translations.put(lang, translationsMap);
            
            // Log loaded keys per language
            int keysCount = translationsMap.size();
            totalKeys += keysCount;
            getLogger().info("Loaded language: " + lang + " with " + keysCount + " keys.");

            totalLanguages++;
        }

        // Log summary
        getLogger().info("Total languages loaded: " + totalLanguages);
        getLogger().info("Total translation keys loaded: " + totalKeys);
    }


    public String translate(Player player, String key) {
        String lang = getPlayerLanguage(player);
        Map<String, String> langMap = translations.getOrDefault(lang, translations.get("en_US"));

        // ✅ Normalize key: Convert to lowercase and replace spaces with underscores
        String normalizedKey = key.toLowerCase().replace(" ", "_");

        return langMap.getOrDefault(normalizedKey, normalizedKey); // Default to key if not found
    }


    private List<Quest> getRandomQuestsForPlayer(String playerName) {
        long currentTime = System.currentTimeMillis();
        List<Quest> selectedQuests = playerQuestSelections.get(playerName);
        Long lastRefresh = playerQuestTimestamps.getOrDefault(playerName, 0L);

        // If the last selection was within the refresh period, return the same quests
        if (selectedQuests != null && currentTime - lastRefresh < QUEST_REFRESH_TIME_MS) {
            return selectedQuests;
        }

        // Keep active quest if the player has one
        ActiveQuest activeQuest = activeQuests.get(playerName);
        List<Quest> newQuests = new ArrayList<>();

        if (activeQuest != null) {
            newQuests.add(activeQuest.getQuest()); // Keep the active quest
        }

        // Pick remaining quests randomly
        List<Quest> availableQuests = new ArrayList<>(questList);
        availableQuests.removeAll(newQuests); // Remove the already active quest

        Random random = new Random();
        while (newQuests.size() < 5 && !availableQuests.isEmpty()) {
            newQuests.add(availableQuests.remove(random.nextInt(availableQuests.size())));
        }

        // Store the new selection and timestamp
        playerQuestSelections.put(playerName, newQuests);
        playerQuestTimestamps.put(playerName, currentTime);

        return newQuests;
    }

    public void createScoreboard(Player player) {
        ActiveQuest aq = activeQuests.get(player.getName());
        if (aq == null) {
            return; // No active quest, no scoreboard needed.
        }

        Scoreboard scoreboard = new Scoreboard(SCOREBOARD_TITLE, Scoreboard.SortOrder.ASCENDING, Scoreboard.DisplaySlot.SIDEBAR);
        int line = 0;
        
        Quest quest = aq.getQuest();
        QuestProgress progress = aq.getProgress();

        scoreboard.setScore("§e" + quest.getName(), line++);
        scoreboard.setScore("§9" + quest.getReward() + " credits", line++);
        
        // Add kill targets
        for (Map.Entry<String, Integer> entry : quest.getKillTargets().entrySet()) {
            String target = entry.getKey();
            int required = entry.getValue();
            int current = progress.getKills().getOrDefault(target, 0);
            String translatedTarget = translate(player, "entity.minecraft." + target.toLowerCase());
            scoreboard.setScore("§c" + translatedTarget, line++);
            scoreboard.setScore("   " + current + "/" + required, line++);
        }

        // Add gather items
        for (Map.Entry<String, Integer> entry : quest.getGatherItems().entrySet()) {
            String item = entry.getKey();
            int required = entry.getValue();
            int current = progress.getGather().getOrDefault(item, 0);
            String translatedItem = translate(player, "item.minecraft." + item.toLowerCase());
            scoreboard.setScore("§a" + translatedItem, line++);
            scoreboard.setScore("   " + current + "/" + required, line++);
        }

        // Add distance progress
        if (quest.getDistance() > 0) {
            int distanceTraveled = progress.getDistanceTraveled();
            scoreboard.setScore("§bDi: " + distanceTraveled + "/" + quest.getDistance(), line++);
        }

        // Add height progress
        if (quest.getHeight() > 0) {
            int heightClimbed = progress.getHeightClimbed();
            scoreboard.setScore("§bH: " + heightClimbed + "/" + quest.getHeight(), line++);
        }

        // Add depth progress
        if (quest.getDepth() > 0) {
            int depthDescended = progress.getDepthDescended();
            scoreboard.setScore("§bDe: " + depthDescended + "/" + quest.getDepth(), line++);
        }

        // Store and show the scoreboard
        scoreboards.put(player, scoreboard);
        scoreboard.showTo(player);
    }

    public void destroyScoreboard(Player player) {
        Scoreboard scoreboard = scoreboards.remove(player);
        if (scoreboard != null) {
            scoreboard.hideFor(player);
        }
    }

    /**
     * Loads available quests from quests.yml.
     */
    private void loadQuests() {
        File questFile = new File(getDataFolder(), "quests.yml");
        if (!questFile.exists()) {
            saveResource("quests.yml");
        }
        Config config = new Config(questFile, Config.YAML);
        List<Map<String, Object>> quests = config.get("quests", new ArrayList<>());
        for (Map<String, Object> questMap : quests) {
            String name = (String) questMap.get("name");
            String description = (String) questMap.get("description");
            int reward = 0;
            try {
                reward = Integer.parseInt(questMap.get("reward").toString());
            } catch (Exception e) {
                getLogger().warning("Invalid reward for quest '" + name + "': " + questMap.get("reward") + ". Using 0.");
            }
            Map<String, Integer> killTargets = new HashMap<>();
            if (questMap.get("killTargets") instanceof Map) {
                Map<?, ?> kt = (Map<?, ?>) questMap.get("killTargets");
                for (Map.Entry<?, ?> entry : kt.entrySet()) {
                    try {
                        killTargets.put(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()));
                    } catch (NumberFormatException e) {
                        getLogger().warning("Invalid kill target count for quest '" + name + "' for target '" 
                                + entry.getKey() + "': " + entry.getValue() + ". Skipping.");
                    }
                }
            }
            Map<String, Integer> gatherItems = new HashMap<>();
            if (questMap.get("gatherItems") instanceof Map) {
                Map<?, ?> gi = (Map<?, ?>) questMap.get("gatherItems");
                for (Map.Entry<?, ?> entry : gi.entrySet()) {
                    try {
                        gatherItems.put(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()));
                    } catch (NumberFormatException e) {
                        getLogger().warning("Invalid gather item count for quest '" + name + "' for item '" 
                                + entry.getKey() + "': " + entry.getValue() + ". Skipping.");
                    }
                }
            }
            int distance = questMap.containsKey("distance") ? Integer.parseInt(questMap.get("distance").toString()) : 0;
            int height = questMap.containsKey("height") ? Integer.parseInt(questMap.get("height").toString()) : 0;
            int depth = questMap.containsKey("depth") ? Integer.parseInt(questMap.get("depth").toString()) : 0;

            Quest quest = new Quest(name, description, killTargets, gatherItems, reward, distance, height, depth);
            questList.add(quest);
        }
    }

    /**
     * Loads a player's active quest progress from file.
     */
    private ActiveQuest loadActiveQuestProgress(String playerName) {
        File file = new File(playersFolder, playerName + ".yml");
        if (file.exists()) {
            Config config = new Config(file, Config.YAML);
            if (config.exists("activeQuest")) {
                Map<String, Object> activeData = config.get("activeQuest", new HashMap<>());
                String questName = (String) activeData.get("questName");
                for (Quest quest : questList) {
                    if (quest.getName().equalsIgnoreCase(questName)) {
                        QuestProgress progress = new QuestProgress();
                        if (activeData.containsKey("kills")) {
                            Map<?, ?> killsMap = (Map<?, ?>) activeData.get("kills");
                            for (Map.Entry<?, ?> entry : killsMap.entrySet()) {
                                try {
                                    progress.getKills().put(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()));
                                } catch (NumberFormatException e) {
                                    getLogger().warning("Invalid kill progress for active quest '" + questName + "' for player '" 
                                            + playerName + "': " + entry.getValue() + ". Skipping.");
                                }
                            }
                        }
                        if (activeData.containsKey("gather")) {
                            Map<?, ?> gatherMap = (Map<?, ?>) activeData.get("gather");
                            for (Map.Entry<?, ?> entry : gatherMap.entrySet()) {
                                try {
                                    progress.getGather().put(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()));
                                } catch (NumberFormatException e) {
                                    getLogger().warning("Invalid gather progress for active quest '" + questName + "' for player '" 
                                            + playerName + "': " + entry.getValue() + ". Skipping.");
                                }
                            }
                        }
                        if (activeData.containsKey("distanceTraveled")) {
                            progress.setDistanceTraveled(Integer.parseInt(activeData.get("distanceTraveled").toString()));
                        }
                        if (activeData.containsKey("heightClimbed")) {
                            progress.setHeightClimbed(Integer.parseInt(activeData.get("heightClimbed").toString()));
                        }
                        if (activeData.containsKey("depthDescended")) {
                            progress.setDepthDescended(Integer.parseInt(activeData.get("depthDescended").toString()));
                        }
                        String startPosString = (String) activeData.get("startPos");
                        String[] components = startPosString.substring(1, startPosString.length() - 1).split(",");
                        Vector3 startPos = new Vector3(
                            Double.parseDouble(components[0]),
                            Double.parseDouble(components[1]),
                            Double.parseDouble(components[2])
                        ); // Load the starting position
                        return new ActiveQuest(quest, progress, startPos);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Saves a player's active quest progress to file.
     */
    private void saveActiveQuestProgress(String playerName) {
        ActiveQuest aq = activeQuests.get(playerName);
        if (aq == null) return;
        Map<String, Object> activeData = new HashMap<>();
        activeData.put("questName", aq.getQuest().getName());
        activeData.put("kills", aq.getProgress().getKills());
        activeData.put("gather", aq.getProgress().getGather());
        activeData.put("distanceTraveled", aq.getProgress().getDistanceTraveled());
        activeData.put("heightClimbed", aq.getProgress().getHeightClimbed());
        activeData.put("depthDescended", aq.getProgress().getDepthDescended());
        activeData.put("startPos", aq.getStartPos().toString()); // Save the starting position

        File file = new File(playersFolder, playerName + ".yml");
        Config config = new Config(file, Config.YAML);
        config.set("activeQuest", activeData);
        config.save();
    }

    /**
     * Clears a player's active quest progress from file.
     */
    private void clearActiveQuestProgress(String playerName) {
        File file = new File(playersFolder, playerName + ".yml");
        if (file.exists()) {
            Config config = new Config(file, Config.YAML);
            config.remove("activeQuest");
            config.save();
        }
    }

    /**
     * Updates the active quest progress for the given player by checking their inventory.
     */
    private void updateQuestProgressForPlayer(Player player) {
        String playerName = player.getName();
        ActiveQuest aq = activeQuests.get(playerName);
        if (aq == null) {
            getLogger().debug("No active quest for player " + playerName);
            return;
        }
        Quest quest = aq.getQuest();
        QuestProgress progress = aq.getProgress();
        getLogger().debug("Updating active quest progress for " + playerName + " (Quest: " + quest.getName() + ")");

        // Update gather progress: recalc counts from player's inventory.
        for (Map.Entry<String, Integer> entry : quest.getGatherItems().entrySet()) {
            String requiredItem = entry.getKey();
            int inventoryCount = countItemInInventory(player, requiredItem);
            progress.getGather().put(requiredItem, inventoryCount);
            getLogger().debug("Updated gather count for " + requiredItem + " in quest '" + quest.getName() +
                    "': " + inventoryCount + "/" + entry.getValue());
        }

        // Update distance progress
        Vector3 startPos = aq.getStartPos();
        Vector3 currentPos = player.getPosition().floor();
        int distanceTraveled = (int) startPos.distance(currentPos);
        int heightClimbed = (int) (currentPos.getY() - startPos.getY());
        int depthDescended = (int) (startPos.getY() - currentPos.getY());

        progress.setDistanceTraveled(distanceTraveled);
        progress.setHeightClimbed(heightClimbed);
        progress.setDepthDescended(depthDescended);

        // Check for quest completion.
        if (checkQuestCompletion(quest, progress)) {
            // Remove required items from inventory.
            for (Map.Entry<String, Integer> entry : quest.getGatherItems().entrySet()) {
                removeItemsFromInventory(player, entry.getKey(), entry.getValue());
            }
            // Award credits.
            if (economy != null) {
                economy.addMoney(player, quest.getReward());
            } else {
                getLogger().warning("EconomyAPI not available; cannot award credits to " + playerName);
            }
            player.sendMessage("§aQuest complete: " + quest.getName() +
                    "§r! You've earned §e" + quest.getReward() + " coins§r.");
            getLogger().debug("Active quest '" + quest.getName() + "' completed for player " + playerName);

            // Trigger Quest End Event
            onQuestEnd(player, quest.getName());

            activeQuests.remove(playerName);
            clearActiveQuestProgress(playerName);
            destroyScoreboard(player);
        } else {
            saveActiveQuestProgress(playerName);
        }
    }

    /**
     * Counts the number of items in the player's inventory matching the given normalized item name.
     */
    private int countItemInInventory(Player player, String itemName) {
        int count = 0;
        for (Item item : player.getInventory().getContents().values()) {
            if (item != null && item.getName().toLowerCase().replace(" ", "_").equals(itemName)) {
                count += item.getCount();
            }
        }
        return count;
    }

    /**
     * Removes the specified number of items (matching the normalized item name) from the player's inventory.
     */
    private void removeItemsFromInventory(Player player, String itemName, int countToRemove) {
        Map<Integer, Item> contents = player.getInventory().getContents();
        for (Map.Entry<Integer, Item> entry : contents.entrySet()) {
            Item item = entry.getValue();
            if (item != null && item.getName().toLowerCase().replace(" ", "_").equals(itemName)) {
                int stackCount = item.getCount();
                if (stackCount <= countToRemove) {
                    countToRemove -= stackCount;
                    player.getInventory().clear(entry.getKey());
                } else {
                    item.setCount(stackCount - countToRemove);
                    player.getInventory().setItem(entry.getKey(), item);
                    countToRemove = 0;
                }
                if (countToRemove <= 0) {
                    break;
                }
            }
        }
    }

    /**
     * Checks whether a quest's requirements (both kills and gathers) are met.
     */
    private boolean checkQuestCompletion(Quest quest, QuestProgress progress) {
        for (Map.Entry<String, Integer> entry : quest.getKillTargets().entrySet()) {
            String target = entry.getKey();
            int required = entry.getValue();
            int current = progress.getKills().getOrDefault(target, 0);
            if (current < required) {
                return false;
            }
        }
        for (Map.Entry<String, Integer> entry : quest.getGatherItems().entrySet()) {
            String item = entry.getKey();
            int required = entry.getValue();
            int current = progress.getGather().getOrDefault(item, 0);
            if (current < required) {
                return false;
            }
        }

        // Check distance traveled
        if (quest.getDistance() > 0 && progress.getDistanceTraveled() < quest.getDistance()) {
            return false;
        }

        // Check height climbed
        if (quest.getHeight() > 0 && progress.getHeightClimbed() < quest.getHeight()) {
            return false;
        }

        // Check depth descended
        if (quest.getDepth() > 0 && progress.getDepthDescended() < quest.getDepth()) {
            return false;
        }
        return true;
    }

    /**
     * Displays the quest selection form when a player uses the /qk command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Handle Quest Selection
        if (command.getName().equalsIgnoreCase("quests")) {
            List<Quest> quests = getRandomQuestsForPlayer(player.getName());
            FormWindowSimple form = new FormWindowSimple(translate(player, "menu.questselector"), translate(player, "menu.questselector.description"));
            for (Quest quest : quests) {
                form.addButton(new ElementButton(quest.getName()));
            }
            player.showFormWindow(form);
            return true;
        }

        // Handle Quest Status Command
        if (command.getName().equalsIgnoreCase("queststatus")) {
            ActiveQuest activeQuest = activeQuests.get(player.getName());
            if (activeQuest == null) {
                player.sendMessage("§cYou do not have an active quest.");
                return true;
            }

            Quest quest = activeQuest.getQuest();
            FormWindowSimple form = new FormWindowSimple(translate(player, "menu.queststatus"), quest.getDescription());
            form.addButton(new ElementButton("OK"));
            form.addButton(new ElementButton("Cancel Quest"));
            player.showFormWindow(form);
            return true;
        }

        // Handle Jump Puzzle Generation
        if (command.getName().equalsIgnoreCase("jumpgen")) {
            if (args.length < 4) {
                sender.sendMessage("§cUsage: /jumpgen <name> <length> <width> <height>");
                return false;
            }

            int length, width, maxHeight;

            String puzzleName = args[0];

            // ✅ Check for duplicate names
            if (activeJumpPuzzles.containsKey(puzzleName)) {
                sender.sendMessage("§cA jump puzzle with this name already exists!");
                return true;
            }

            try {
                length = Math.max(20, Integer.parseInt(args[1]));  // Ensure minimum 20
                width = Math.max(20, Integer.parseInt(args[2]));   // Ensure minimum 20
                maxHeight = Math.max(20, Integer.parseInt(args[3])); // Ensure minimum 20
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number format. Use: /jumpgen <name> <length> <width> <height>");
                return true;
            }

            // ✅ Use the new class to generate the puzzle
            JumpPuzzleGenerator generator = new JumpPuzzleGenerator(this, player.getLevel(), player.getPosition().floor(), puzzleName, length, width, maxHeight);
            generator.generate();
            activeJumpPuzzles.put(puzzleName, generator);
            saveJumpPuzzles(); // ✅ Save immediately after generation

            sender.sendMessage("§aJump puzzle '" + puzzleName + "' generated!");
            return true;
        }

        // ✅ Handle Puzzle Area Clearing
        if (command.getName().equalsIgnoreCase("cleararea")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /cleararea <length> <width> <height>");
                return false;
            }

            int length, width, maxHeight;
            try {
                length = Math.max(20, Integer.parseInt(args[0]));  // Ensure minimum 20
                width = Math.max(20, Integer.parseInt(args[1]));   // Ensure minimum 20
                maxHeight = Math.max(20, Integer.parseInt(args[2])); // Ensure minimum 20
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number format. Use: /cleararea <length> <width> <height>");
                return true;
            }

            getLogger().info("Clearing puzzle area for player " + player.getName() + "...");
            JumpPuzzleGenerator generator = new JumpPuzzleGenerator(this, player.getLevel(), player.getPosition().floor(), "clear", length, width, maxHeight);
            generator.clearOnly();
            sender.sendMessage("§aJump puzzle area cleared!");
            getLogger().info("Puzzle area successfully cleared.");
            return true;
        }

        // Handle Puzzle clear
        if (command.getName().equalsIgnoreCase("clearpuzzle")) {
            if (args.length < 1) {
                sender.sendMessage("§cUsage: /clearpuzzle <name>");
                return false;
            }

            String puzzleName = args[0];
            JumpPuzzleGenerator puzzle = activeJumpPuzzles.get(puzzleName);

            if (puzzle == null) {
                sender.sendMessage("§cNo puzzle found with that name.");
                return true;
            }

            puzzle.removePuzzle(); // ✅ Call new remove function
            activeJumpPuzzles.remove(puzzleName);
            saveJumpPuzzles(); // ✅ Save changes

            sender.sendMessage("§aJump puzzle '" + puzzleName + "' cleared!");
            return true;
        }

        // Handle List puzzle
        if (command.getName().equalsIgnoreCase("listpuzzle")) {
            if (activeJumpPuzzles.isEmpty()) {
                sender.sendMessage("§cNo puzzles available.");
            } else {
                sender.sendMessage("§aActive Jump Puzzles:");
                for (String puzzleName : activeJumpPuzzles.keySet()) {
                    sender.sendMessage("§6- " + puzzleName);
                }
            }
            return true;
        }

        // Handle Puzzle Configuration
        if (command.getName().equalsIgnoreCase("jumpconfig")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /jumpconfig <name> <resetOnCompletion> <greenBlockResetTimeout>");
                return false;
            }

            String puzzleName = args[0];
            JumpPuzzleGenerator puzzle = activeJumpPuzzles.get(puzzleName);

            if (puzzle == null) {
                sender.sendMessage("§cNo puzzle found with that name.");
                return true;
            }

            boolean resetOnCompletion = Boolean.parseBoolean(args[1]);
            int greenBlockResetTimeout = Integer.parseInt(args[2]);

            puzzle.setResetOnCompletion(resetOnCompletion);
            puzzle.setGreenBlockResetTimeout(greenBlockResetTimeout);
            saveJumpPuzzles(); // Save changes

            sender.sendMessage("§aJump puzzle '" + puzzleName + "' configuration updated!");
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        ActiveQuest aq = activeQuests.get(playerName);
        if (aq == null) {
            return;
        } else {
            updateQuestProgressForPlayer(player);
        }
    }

    /**
     * Handles the player's form response for quest selection.
     */
    @EventHandler
    public void onPlayerFormResponse(PlayerFormRespondedEvent event) {
        if (!(event.getWindow() instanceof FormWindowSimple)) {
            return;
        }

        FormWindowSimple form = (FormWindowSimple) event.getWindow();
        FormResponseSimple response = (FormResponseSimple) event.getResponse();
        if (response == null) {
            return;
        }

        Player player = event.getPlayer();

        // Handle Quest Selector Form
        if (form.getTitle().equals(translate(player, "menu.questselector"))) {
            List<Quest> quests = playerQuestSelections.get(player.getName());
            if (quests == null) {
                return;
            }

            int selected = response.getClickedButtonId();
            if (selected < 0 || selected >= quests.size()) {
                return;
            }

            Quest selectedQuest = quests.get(selected);
            selectedQuests.put(player.getName(), selectedQuest); // Store the selected quest

            FormWindowSimple questDetailsForm = new FormWindowSimple(translate(player, "menu.questdetails"), selectedQuest.getName() + "\n" + selectedQuest.getDescription());
            questDetailsForm.addButton(new ElementButton("Accept"));
            questDetailsForm.addButton(new ElementButton("Cancel"));
            player.showFormWindow(questDetailsForm);
        }
        // Handle Quest Details Form
        else if (form.getTitle().equals(translate(player, "menu.questdetails"))) {
            Quest selectedQuest = selectedQuests.get(player.getName());
            if (selectedQuest == null) {
                return;
            }

            int clickedButtonId = response.getClickedButtonId();

            if (clickedButtonId == 0) { // Accept button
                // Check if the player already has an active quest
                boolean hasActiveQuest = activeQuests.containsKey(player.getName());

                // Update active quest
                Vector3 startPos = player.getPosition().floor(); // Store the starting position
                activeQuests.put(player.getName(), new ActiveQuest(selectedQuest, new QuestProgress(), startPos));
                saveActiveQuestProgress(player.getName());
                player.sendMessage("§aActive quest set to: " + selectedQuest.getName());

                // Trigger Quest Start Event
                onQuestStart(player, selectedQuest.getName());

                // Update the Scoreboard Immediately
                if (hasActiveQuest) {
                    destroyScoreboard(player);  // Remove old scoreboard
                }
                createScoreboard(player);  // Create the updated one
            } else if (clickedButtonId == 1) { // Cancel button
                // Show the quest selector form again
                List<Quest> quests = getRandomQuestsForPlayer(player.getName());
                FormWindowSimple questListForm = new FormWindowSimple(translate(player, "menu.questselector"), "Select a quest from the list below.");
                for (Quest quest : quests) {
                    questListForm.addButton(new ElementButton(quest.getName()));
                }
                player.showFormWindow(questListForm);
            }

            selectedQuests.remove(player.getName()); // Remove the selected quest from the map
        }
        // Handle Quest Status Form
        else if (form.getTitle().equals(translate(player, "menu.queststatus"))) {
            int clickedButtonId = response.getClickedButtonId();

            if (clickedButtonId == 1) { // Cancel Quest button
                ActiveQuest activeQuest = activeQuests.remove(player.getName());
                if (activeQuest != null) {
                    clearActiveQuestProgress(player.getName());
                    player.sendMessage("§cQuest cancelled: " + activeQuest.getQuest().getName());
                    destroyScoreboard(player); // Remove the scoreboard
                }
            }
        }
    }

    /**
     * Event handler for inventory pickup events.
     * Schedules a 1-tick delayed task to update the active quest progress based on the player's current inventory.
     */
    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof Player) {
            final Player player = (Player) event.getInventory().getHolder();
            getServer().getScheduler().scheduleDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    updateQuestProgressForPlayer(player);
                }
            }, 1);
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        Item loot = event.getLoot(); // ✅ Get the caught item

        if (loot != null) {
            String itemName = normalizeItemName(loot.getName());
            player.sendMessage("§aYou caught a " + itemName + "!");

            getLogger().info("Player " + player.getName() + " fished up: " + itemName);

            // ✅ Schedule a delayed task to update quest progress
            getServer().getScheduler().scheduleDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    updateQuestProgressForPlayer(player);
                }
            }, 1); // Delay 1 tick to ensure inventory updates
        }
    }

    private String normalizeItemName(String itemName) {
        return itemName.toLowerCase().replace(" ", "_");
    }

    /**
     * Event handler for entity death events.
     * Tracks kill progress for the player's active quest.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) entity.getLastDamageCause();
            Entity damager = damageEvent.getDamager();
            if (damager instanceof Player) {
                Player player = (Player) damager;
                String playerName = player.getName();
                String entityName = entity.getName();
                ActiveQuest aq = activeQuests.get(playerName);
                if (aq == null) return;
                Quest quest = aq.getQuest();
                QuestProgress progress = aq.getProgress();

                // Update kill progress if the active quest requires this kill target.
                if (quest.getKillTargets().containsKey(entityName)) {
                    int currentKills = progress.getKills().getOrDefault(entityName, 0) + 1;
                    progress.getKills().put(entityName, currentKills);
                    getLogger().info("Player " + playerName + " killed " + entityName +
                            " for active quest '" + quest.getName() + "': " +
                            currentKills + "/" + quest.getKillTargets().get(entityName));
                }

                // Check for quest completion.
                if (checkQuestCompletion(quest, progress)) {
                    // Remove required items from inventory.
                    for (Map.Entry<String, Integer> entry : quest.getGatherItems().entrySet()) {
                        removeItemsFromInventory(player, entry.getKey(), entry.getValue());
                    }
                    // Award credits.
                    if (economy != null) {
                        economy.addMoney(player, quest.getReward());
                    } else {
                        getLogger().warning("EconomyAPI not available; cannot award credits to " + playerName);
                    }
                    player.sendMessage("§aQuest complete: " + quest.getName() +
                            "§r! You've earned §e" + quest.getReward() + " coins§r.");
                    getLogger().info("Active quest '" + quest.getName() + "' completed for player " + playerName);
                    activeQuests.remove(playerName);
                    clearActiveQuestProgress(playerName);
                    destroyScoreboard(player);
                } else {
                    saveActiveQuestProgress(playerName);
                }
            }
        }
    }

    String getPlayerLanguage(Player player) {
        return player.getLoginChainData().getLanguageCode(); // ✅ This works!
    }

    public void saveJumpPuzzles() {
        List<Map<String, Object>> puzzleData = new ArrayList<>();
        for (JumpPuzzleGenerator puzzle : activeJumpPuzzles.values()) {
            puzzleData.add(puzzle.toMap());
        }
        Config config = new Config(puzzlesFile, Config.YAML);
        config.set("puzzles", puzzleData);
        config.save();
    }

    public void loadJumpPuzzles() {
        if (!puzzlesFile.exists()) {
            getLogger().info("No saved puzzles found.");
            return;
        }

        Config config = new Config(puzzlesFile, Config.YAML);

        // ✅ Explicitly cast to List<Map<String, Object>> to ensure type safety
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> puzzleData = (List<Map<String, Object>>) (List<?>) config.getMapList("puzzles");

        for (Map<String, Object> data : puzzleData) {
            JumpPuzzleGenerator puzzle = JumpPuzzleGenerator.fromMap(this, data);
            if (puzzle != null) { // ✅ Ensure puzzle is valid before adding
                activeJumpPuzzles.put(puzzle.getPuzzleName(), puzzle);
                getLogger().info("Loaded puzzle: " + puzzle.getPuzzleName());
            } else {
                getLogger().warning("Skipping invalid puzzle entry.");
            }
        }
    }
}