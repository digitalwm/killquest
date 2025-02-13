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

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;

import cn.nukkit.entity.Entity;
import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;

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

public class KillQuestPlugin extends PluginBase implements Listener, CommandExecutor {

    // List of all available quests (loaded from quests.yml)
    private List<Quest> questList = new ArrayList<>();

    // Mapping of player's name to their active quest (only one active quest at a time)
    public Map<String, ActiveQuest> activeQuests = new HashMap<>();

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
    }

    @Override
    public void onDisable() {
        for (String playerName : activeQuests.keySet()) {
            saveActiveQuestProgress(playerName);
        }
        getLogger().info("KillQuestPlugin disabled and active quest progress saved.");
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
            Quest quest = new Quest(name, description, killTargets, gatherItems, reward);
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
                        return new ActiveQuest(quest, progress);
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
            activeQuests.remove(playerName);
            clearActiveQuestProgress(playerName);
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
            FormWindowSimple form = new FormWindowSimple("Quest Selector", "Select one quest from the list below. Only one active quest is allowed at a time.");
            for (Quest quest : quests) {
                form.addButton(new ElementButton(quest.getName() + "\n" + quest.getDescription()));
            }
            player.showFormWindow(form);
            return true;
        }

        // Handle Jump Puzzle Generation
        if (command.getName().equalsIgnoreCase("jumpgen")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /jumpgen <length> <width> <height>");
                return false;
            }

            int length, width, maxHeight;
            try {
                length = Integer.parseInt(args[0]);
                width = Integer.parseInt(args[1]);
                maxHeight = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number format. Use: /jumpgen <length> <width> <height>");
                return true;
            }

            // ✅ Use the new class to generate the puzzle
            JumpPuzzleGenerator generator = new JumpPuzzleGenerator(this, player, length, width, maxHeight);
            generator.generate();

            sender.sendMessage("§aJumping puzzle generated inside a cage!");
            return true;
        }

        // ✅ Handle Puzzle Area Clearing
        if (command.getName().equalsIgnoreCase("clearpuzzle")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /clearpuzzle <length> <width> <height>");
                return false;
            }

            int length, width, maxHeight;
            try {
                length = Integer.parseInt(args[0]);
                width = Integer.parseInt(args[1]);
                maxHeight = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number format. Use: /clearpuzzle <length> <width> <height>");
                return true;
            }

            getLogger().info("Clearing puzzle area for player " + player.getName() + "...");
            JumpPuzzleGenerator generator = new JumpPuzzleGenerator(this, player, length, width, maxHeight);
            generator.clearOnly();
            sender.sendMessage("§aJump puzzle area cleared!");
            getLogger().info("Puzzle area successfully cleared.");
            return true;
        }

        return false;
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
        if (!form.getTitle().equals("Quest Selector")) {
            return;
        }

        FormResponseSimple response = (FormResponseSimple) event.getResponse();
        if (response == null) {
            return;
        }

        List<Quest> quests = playerQuestSelections.get(event.getPlayer().getName());
        if (quests == null) {
            return;
        }

        int selected = response.getClickedButtonId();
        if (selected < 0 || selected >= quests.size()) {
            return;
        }

        Player player = event.getPlayer();
        Quest selectedQuest = quests.get(selected);

        // Check if the player already has an active quest
        boolean hasActiveQuest = activeQuests.containsKey(player.getName());

        // Update active quest
        activeQuests.put(player.getName(), new ActiveQuest(selectedQuest, new QuestProgress()));
        saveActiveQuestProgress(player.getName());
        player.sendMessage("§aActive quest set to: " + selectedQuest.getName());

        // ✅ **Update the Scoreboard Immediately**
        if (hasActiveQuest) {
            destroyScoreboard(player);  // Remove old scoreboard
        }
        createScoreboard(player);  // Create the updated one
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
                } else {
                    saveActiveQuestProgress(playerName);
                }
            }
        }
    }

    String getPlayerLanguage(Player player) {
        return player.getLoginChainData().getLanguageCode(); // ✅ This works!
    }
}