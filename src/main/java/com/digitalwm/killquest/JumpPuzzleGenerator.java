package com.digitalwm.killquest;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockDoor;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.block.BlockDoorWood;
import me.onebone.economyapi.EconomyAPI;
import java.util.HashMap;
import java.util.Map;

import java.util.*;

public class JumpPuzzleGenerator {
    
    private final Random random = new Random();
    private final Level level;
    private final Vector3 startPos;
    private final int length, width, maxHeight;
//    private final Player player;
    private final KillQuestPlugin plugin;
    
    private Vector3 puzzleMin;
    private Vector3 puzzleMax;
    private Vector3 startBlock;
    private Vector3 endBlock;

    private final Map<Player, Long> playerStartTimes = new HashMap<>();
    private final Map<Player, Long> playerGreenBlockTimes = new HashMap<>();

    // List to track all blocks generated in the puzzle
    
    private final Map<Vector3, String> puzzleBlocks = new HashMap<>(); // ✅ Stores block type

    private final String puzzleName; // ✅ Declare puzzle name

    public JumpPuzzleGenerator(KillQuestPlugin plugin, Level level, Vector3 startPos, String puzzleName, int length, int width, int maxHeight) {
        this.level = level;
        this.startPos = startPos;
        this.length = length;
        this.width = width;
        this.maxHeight = maxHeight;
        this.plugin = plugin;
        this.puzzleName = puzzleName; // ✅ Store name
        this.setPuzzleBoundaries(this.startPos, this.width, this.length, this.maxHeight);
    }

    public void setPuzzleBoundaries(Vector3 startPos, int width, int length, int maxHeight) {
        maxHeight++;
        puzzleMin = new Vector3(startPos.x - width / 2, startPos.y, startPos.z - length / 2);
        puzzleMax = new Vector3(startPos.x + width / 2, startPos.y + maxHeight, startPos.z + length / 2);
        plugin.getLogger().info("Puzzle boundaries set: " + puzzleMin + " to " + puzzleMax);
    }

    public String getPuzzleName() {
        return puzzleName;
    }

    public void generate() {
        clearArea();
        generateBase();
        generateWalls();
        generatePuzzle();
    }

    private void clearArea() {
        plugin.getLogger().info("Clearing area for jump puzzle...");

        // Get adjusted start position
        Vector3 adjustedStart = new Vector3(
            startPos.x - width / 2,
            startPos.y,
            startPos.z - length / 2
        );

        // ✅ Remove all blocks in the defined area
        for (int x = 0; x <= width; x++) {
            for (int z = 0; z <= length; z++) {
                for (int y = 0; y <= maxHeight; y++) { // Go up to maxHeight
                    level.setBlock(new Vector3(adjustedStart.x + x, adjustedStart.y + y, adjustedStart.z + z), Block.get(Block.AIR));
                }
            }
        }

        plugin.getLogger().info("Area cleared successfully!");
    }

    private void generateBase() {
        plugin.getLogger().info("Generating puzzle base with light-emitting blocks...");

        Vector3 baseStart = new Vector3(
            startPos.x - width / 2,
            startPos.y - 1,
            startPos.z - length / 2
        );

        Vector3 centerBlock = new Vector3(
            startPos.x,
            startPos.y - 1,
            startPos.z
        );

        for (int x = 0; x <= width; x++) {
            for (int z = 0; z <= length; z++) {
                Vector3 currentPos = new Vector3(baseStart.x + x, baseStart.y, baseStart.z + z);
                trackBlock(currentPos, "BASE");
                // ✅ Place a Red Block in the exact center
                if (currentPos.x == centerBlock.x && currentPos.z == centerBlock.z) {
                    level.setBlock(currentPos, Block.get(Block.REDSTONE_BLOCK));
                } else {
                    level.setBlock(currentPos, Block.get(Block.SEA_LANTERN)); // Light-emitting base
                }
            }
        }

        plugin.getLogger().info("Puzzle base generated with lighting and center marker!");
    }

    private void generateWalls() {
        plugin.getLogger().info("Generating walls around the puzzle...");

        Vector3 baseStart = new Vector3(
            startPos.x - width / 2,
            startPos.y - 1,
            startPos.z - length / 2
        );

        for (int y = 0; y <= maxHeight; y++) {
            for (int x = 0; x <= width; x++) {
                Vector3 leftWall = new Vector3(baseStart.x + x, baseStart.y + y, baseStart.z);
                Vector3 rightWall = new Vector3(baseStart.x + x, baseStart.y + y, baseStart.z + length);
                level.setBlock(leftWall, Block.get(Block.GLASS));
                level.setBlock(rightWall, Block.get(Block.GLASS));
                trackBlock(leftWall, "WALL");
                trackBlock(rightWall, "WALL");
            }
            for (int z = 0; z <= length; z++) {
                Vector3 frontWall = new Vector3(baseStart.x, baseStart.y + y, baseStart.z + z);
                Vector3 backWall = new Vector3(baseStart.x + width, baseStart.y + y, baseStart.z + z);
                level.setBlock(frontWall, Block.get(Block.GLASS));
                level.setBlock(backWall, Block.get(Block.GLASS));
                trackBlock(frontWall, "WALL");
                trackBlock(backWall, "WALL");
            }
        }

        plugin.getLogger().info("Walls generated successfully!");
    }

/*    private Vector3 getForwardDirection(Vector3 position) {
        float yaw = (float) this.player.getYaw(); // Get player's yaw (rotation)

        if (yaw >= -45 && yaw <= 45) { // Facing Z+
            return new Vector3(0, 0, 1);
        } else if (yaw > 45 && yaw < 135) { // Facing X-
            return new Vector3(-1, 0, 0);
        } else if (yaw >= 135 || yaw <= -135) { // Facing Z-
            return new Vector3(0, 0, -1);
        } else { // Facing X+
            return new Vector3(1, 0, 0);
        }
    }*/

    public void clearOnly() {
        clearArea();
    }

    private void generatePuzzle() {
        plugin.getLogger().info("Generating jump puzzle...");

        Vector3 lastBlock = new Vector3(
            startPos.x - width / 2 + 1,
            startPos.y,
            startPos.z - length / 2 + 1
        );

        startBlock = lastBlock.clone();
        level.setBlock(startBlock, Block.get(Block.GOLD_BLOCK));
        trackBlock(startBlock, "START"); // ✅ Track start block

        // ✅ Create wooden doors
        BlockDoorWood doorBottom1 = new BlockDoorWood();
        BlockDoorWood doorTop1 = new BlockDoorWood();
        doorTop1.setDamage(8);

        BlockDoorWood doorBottom2 = new BlockDoorWood();
        BlockDoorWood doorTop2 = new BlockDoorWood();
        doorTop2.setDamage(8); // ✅ Set as upper part of the door

        // Door base positions (1 block above start block, placed at the corner)
        Vector3 doorBase1 = new Vector3(startBlock.x - 1, startBlock.y + 1, startBlock.z);
        Vector3 doorBase2 = new Vector3(startBlock.x, startBlock.y + 1, startBlock.z - 1);

        // Door top positions (1 block above door bases)
        Vector3 doorTop1Pos = new Vector3(doorBase1.x, doorBase1.y + 1, doorBase1.z);
        Vector3 doorTop2Pos = new Vector3(doorBase2.x, doorBase2.y + 1, doorBase2.z);

        // ✅ Place bottom and top door parts
        level.setBlock(doorBase1, doorBottom1);
        level.setBlock(doorTop1Pos, doorTop1);

        level.setBlock(doorBase2, doorBottom2);
        level.setBlock(doorTop2Pos, doorTop2);

        // ✅ Track doors for removal
        trackBlock(doorBase1, "DOOR_BOTTOM");
        trackBlock(doorTop1Pos, "DOOR_TOP");

        trackBlock(doorBase2, "DOOR_BOTTOM");
        trackBlock(doorTop2Pos, "DOOR_TOP");

         // ✅ Add a single green block diagonally opposite to the start block on the same height plane
        Vector3 greenBlock = new Vector3(puzzleMax.x - 1, startBlock.y, puzzleMax.z - 1);
        level.setBlock(greenBlock, Block.get(Block.EMERALD_BLOCK));
        trackBlock(greenBlock, "GREEN"); // ✅ Track green block

        int currentHeight = 0;
        int blocksAtCurrentHeight = 0;

        while (currentHeight < maxHeight) {
            boolean validMove = false;
            int retryCount = 0; // ✅ Prevent infinite loops
            int newX = (int) lastBlock.x, newZ = (int) lastBlock.z;

            do {
                retryCount++;
                if (retryCount > 50) { // ✅ If we can't find a position after 50 tries, force move
                    plugin.getLogger().warning("Stuck in loop! Forcing move...");
                    break;
                }

                int xOffset = random.nextInt(3) - 1;
                int zOffset = random.nextInt(3) - 1;

                xOffset *= random.nextBoolean() ? 2 : 1;
                zOffset *= random.nextBoolean() ? 2 : 1;

                newX = (int) lastBlock.x + xOffset;
                newZ = (int) lastBlock.z + zOffset;

                // ✅ Ensure the new block is within bounds
                if (newX <= startPos.x - width / 2 + 1 || newX >= startPos.x + width / 2 - 1) {
                    continue;
                }
                if (newZ <= startPos.z - length / 2 + 1 || newZ >= startPos.z + length / 2 - 1) {
                    continue;
                }

                // ✅ Ensure blocks below are air starting from level 3
                if (currentHeight >= 3) {
                    Vector3 below1 = new Vector3(newX, startPos.y + currentHeight - 1, newZ);
                    Vector3 below2 = new Vector3(newX, startPos.y + currentHeight - 2, newZ);

                    if (level.getBlock(below1).getId() != Block.AIR || level.getBlock(below2).getId() != Block.AIR) {
                        continue; // Skip this position if blocks are below
                    }
                }

                // ✅ Ensure blocks below are air starting from level 4
                if (currentHeight >= 4) {
                    Vector3 below1 = new Vector3(newX, startPos.y + currentHeight - 1, newZ);
                    Vector3 below2 = new Vector3(newX, startPos.y + currentHeight - 2, newZ);
                    Vector3 below3 = new Vector3(newX, startPos.y + currentHeight - 3, newZ);

                    if (level.getBlock(below1).getId() != Block.AIR || level.getBlock(below2).getId() != Block.AIR || level.getBlock(below3).getId() != Block.AIR) {
                        continue; // Skip this position if blocks are below
                    }
                }

                validMove = true;

            } while (!validMove);

            // ✅ Place the jump block
            Vector3 newBlock = new Vector3(newX, startPos.y + currentHeight, newZ);
            trackBlock(newBlock, "JUMP"); // ✅ Track jump blocks
            level.setBlock(newBlock, Block.get(Block.STONE));

            plugin.getLogger().debug("Placed block at x: " + newBlock.x + " z: " + newBlock.z);

            blocksAtCurrentHeight++;

            // ✅ Ensure at least 4 blocks are placed per height level before increasing height
            if (blocksAtCurrentHeight >= 8) {
                currentHeight++;
                blocksAtCurrentHeight = 0;
            }

            lastBlock = newBlock.clone();
        }

        // ✅ Place end block
        endBlock = lastBlock.clone();
        level.setBlock(endBlock, Block.get(Block.DIAMOND_BLOCK));
        trackBlock(endBlock, "END"); // ✅ Track end block
        plugin.getLogger().info("Jump puzzle generated successfully!");
    }

    private boolean isInsidePuzzle(Vector3 pos) {
        return (pos.x >= puzzleMin.x && pos.x <= puzzleMax.x) &&
               (pos.y >= puzzleMin.y && pos.y <= puzzleMax.y) &&
               (pos.z >= puzzleMin.z && pos.z <= puzzleMax.z);
    }

    public boolean isPlayerInside(Player player) {
        return isInsidePuzzle(player.getPosition()); // ✅ Reuse `isInsidePuzzle`
    }

    public void handlePlayerMovement(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Vector3 pos = player.getPosition().floor();
        pos.y = pos.y - 1;

        if (pos.equals(startBlock) && !playerStartTimes.containsKey(player)) {
            player.sendMessage("§eYou started the jump puzzle! Reach the end within 15 minutes!");
            playerStartTimes.put(player, System.currentTimeMillis());
        }

        // Check if the player is on the green block
        if (puzzleBlocks.containsKey(pos) && "GREEN".equals(puzzleBlocks.get(pos))) {
            long currentTime = System.currentTimeMillis();
            if (!playerGreenBlockTimes.containsKey(player)) {
                playerGreenBlockTimes.put(player, currentTime);
            } else {
                long timeOnGreenBlock = currentTime - playerGreenBlockTimes.get(player);
                if (timeOnGreenBlock > 5000) { // 5 seconds
                    player.sendMessage("§cYou stayed on the green block for too long! The puzzle has been reset.");
                    resetPuzzleForPlayer(player);
                }
            }
        } else {
            playerGreenBlockTimes.remove(player);
        }

        if (playerStartTimes.containsKey(player)) {
            long startTime = playerStartTimes.get(player);
            if (System.currentTimeMillis() - startTime > 15 * 60 * 1000) {
                player.sendMessage("§cYou ran out of time for the jump puzzle!");
                playerStartTimes.remove(player);
                return;
            }

            if (pos.equals(endBlock)) {
                player.sendMessage("§aCongratulations! You completed the jump puzzle!");
                EconomyAPI.getInstance().addMoney(player, 100);
                playerStartTimes.remove(player);

                resetPuzzleForPlayer(player);
            }
        }
    }

    public void removePuzzle() {
        plugin.getLogger().info("Removing puzzle '" + puzzleName + "'...");

        // ✅ Iterate over the saved block positions and remove them
        for (Map.Entry<Vector3, String> entry : puzzleBlocks.entrySet()) {
            Vector3 pos = entry.getKey();
            level.setBlock(pos, Block.get(Block.AIR));
        }

        // ✅ Clear the block tracking map
        puzzleBlocks.clear();

        plugin.getLogger().info("Puzzle '" + puzzleName + "' removed successfully!");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", puzzleName);
        data.put("startX", startPos.x);
        data.put("startY", startPos.y);
        data.put("startZ", startPos.z);
        data.put("length", length);
        data.put("width", width);
        data.put("maxHeight", maxHeight);

        List<Map<String, Object>> blockList = new ArrayList<>();
        for (Map.Entry<Vector3, String> entry : puzzleBlocks.entrySet()) {
            Map<String, Object> blockData = new HashMap<>();
            blockData.put("x", entry.getKey().x);
            blockData.put("y", entry.getKey().y);
            blockData.put("z", entry.getKey().z);
            blockData.put("type", entry.getValue()); // ✅ Save block type
            blockList.add(blockData);
        }
        data.put("puzzleBlocks", blockList);

        return data;
    }

    public static JumpPuzzleGenerator fromMap(KillQuestPlugin plugin, Map<String, Object> data) {
        if (!data.containsKey("name") || !data.containsKey("length") || !data.containsKey("width") || 
            !data.containsKey("maxHeight") || !data.containsKey("startX") || 
            !data.containsKey("startY") || !data.containsKey("startZ")) {
            plugin.getLogger().warning("Skipping puzzle load: Missing required fields.");
            return null; // ✅ Skip invalid puzzles
        }

        String name = (String) data.get("name");
        int length = ((Number) data.get("length")).intValue();
        int width = ((Number) data.get("width")).intValue();
        int maxHeight = ((Number) data.get("maxHeight")).intValue();

        int startX = ((Number) data.get("startX")).intValue();
        int startY = ((Number) data.get("startY")).intValue();
        int startZ = ((Number) data.get("startZ")).intValue();
        Vector3 startPos = new Vector3(startX, startY, startZ); // ✅ Load correct position

        // ✅ Get the default level
        Level defaultLevel = plugin.getServer().getDefaultLevel();
        if (defaultLevel == null) {
            plugin.getLogger().warning("No default level found. Cannot load puzzle: " + name);
            return null;
        }

        JumpPuzzleGenerator puzzle = new JumpPuzzleGenerator(plugin, defaultLevel, startPos, name, length, width, maxHeight);

        // ✅ Ensure saved blocks exist before accessing them
        if (data.containsKey("puzzleBlocks")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blocksData = (List<Map<String, Object>>) data.get("puzzleBlocks");
            Map<Vector3, String> puzzleBlocks = new HashMap<>(); // Create a modifiable map
            for (Map<String, Object> blockData : blocksData) {
                if (blockData.containsKey("x") && blockData.containsKey("y") && blockData.containsKey("z") && blockData.containsKey("type")) {
                    int x = ((Number) blockData.get("x")).intValue();
                    int y = ((Number) blockData.get("y")).intValue();
                    int z = ((Number) blockData.get("z")).intValue();
                    String type = (String) blockData.get("type");
                    Vector3 blockPos = new Vector3(x, y, z);
                    puzzleBlocks.put(blockPos, type); // Use the modifiable map

                    // ✅ Check if the block is the start or end block
                    if ("START".equals(type)) {
                        puzzle.startBlock = blockPos;
                    } else if ("END".equals(type)) {
                        puzzle.endBlock = blockPos;
                    }
                } else {
                    plugin.getLogger().warning("Skipping invalid block entry in puzzle: " + name);
                }
            }
            // Update the puzzle with the modified blocks
            puzzle.setPuzzleBlocks(puzzleBlocks);
        } else {
            plugin.getLogger().warning("Puzzle " + name + " has no saved blocks.");
        }

        return puzzle;
    }

    private void trackBlock(Vector3 pos, String type) {
        puzzleBlocks.put(pos, type);
    }

    public Map<Vector3, String> getPuzzleBlocks() {
        return Collections.unmodifiableMap(this.puzzleBlocks);
    }

    // Method to update the internal map with new entries
    public void updatePuzzleBlocks(Map<Vector3, String> newBlocks) {
        this.puzzleBlocks.putAll(newBlocks);
    }

    // Method to clear and update the internal map with new entries
    public void setPuzzleBlocks(Map<Vector3, String> newBlocks) {
        this.puzzleBlocks.clear();
        this.puzzleBlocks.putAll(newBlocks);
    }

    private void resetPuzzleForPlayer(Player player) {
        // Teleport the player to the center of the puzzle
        Vector3 centerBlock = new Vector3(
            startPos.x,
            startPos.y,
            startPos.z
        );
        player.teleport(centerBlock);

        // Regenerate the puzzle
        removePuzzle();
        generate();
    }
}
