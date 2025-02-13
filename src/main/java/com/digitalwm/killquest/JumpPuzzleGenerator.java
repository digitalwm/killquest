package com.digitalwm.killquest;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.event.player.PlayerMoveEvent;
import me.onebone.economyapi.EconomyAPI;

import java.util.*;

public class JumpPuzzleGenerator {
    
    private final Random random = new Random();
    private final Level level;
    private final Vector3 startPos;
    private final int length, width, maxHeight;
    private final Player player;
    private final KillQuestPlugin plugin;
    
    private Vector3 puzzleMin;
    private Vector3 puzzleMax;
    private Vector3 startBlock;
    private Vector3 endBlock;

    private final Set<Vector3> jumpBlocks = new HashSet<>();
    private final Map<Player, Long> playerStartTimes = new HashMap<>();

    public JumpPuzzleGenerator(KillQuestPlugin plugin, Player player, int length, int width, int maxHeight) {
        this.level = player.getLevel();
        this.startPos = player.getPosition().floor();
        this.length = length;
        this.width = width;
        this.maxHeight = maxHeight;
        this.plugin = plugin;
        this.player = player;
        this.setPuzzleBoundaries(this.startPos, this.width, this.length, this.maxHeight);
    }

    public void setPuzzleBoundaries(Vector3 startPos, int width, int length, int maxHeight) {
        maxHeight++;
        puzzleMin = new Vector3(startPos.x - width / 2, startPos.y, startPos.z - length / 2);
        puzzleMax = new Vector3(startPos.x + width / 2, startPos.y + maxHeight, startPos.z + length / 2);
        plugin.getLogger().info("Puzzle boundaries set: " + puzzleMin + " to " + puzzleMax);
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
                // Left Wall
                level.setBlock(new Vector3(baseStart.x + x, baseStart.y + y, baseStart.z), Block.get(Block.GLASS));
                // Right Wall
                level.setBlock(new Vector3(baseStart.x + x, baseStart.y + y, baseStart.z + length), Block.get(Block.GLASS));
            }
            for (int z = 0; z <= length; z++) {
                // Front Wall
                level.setBlock(new Vector3(baseStart.x, baseStart.y + y, baseStart.z + z), Block.get(Block.GLASS));
                // Back Wall
                level.setBlock(new Vector3(baseStart.x + width, baseStart.y + y, baseStart.z + z), Block.get(Block.GLASS));
            }
        }

        plugin.getLogger().info("Walls generated successfully!");
    }

    private Vector3 getForwardDirection(Vector3 position) {
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
    }

    public void clearOnly() {
        plugin.getLogger().info("Starting area cleanup for jump puzzle...");
        clearArea();
        plugin.getLogger().info("Jump puzzle area cleared successfully!");
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
            level.setBlock(newBlock, Block.get(Block.STONE));
            jumpBlocks.add(newBlock);

            plugin.getLogger().info("Placed block at x: " + newBlock.x + " z: " + newBlock.z);

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
            }
        }
    }
}
