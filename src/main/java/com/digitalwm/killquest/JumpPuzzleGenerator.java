package com.digitalwm.killquest;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;

import java.util.Random;

public class JumpPuzzleGenerator {
    
    private final Random random = new Random();
    private final Level level;
    private final Vector3 startPos;
    private final int length, width, maxHeight;
    private final Player player;
    private final KillQuestPlugin plugin;

    public JumpPuzzleGenerator(KillQuestPlugin plugin, Player player, int length, int width, int maxHeight) {
        this.level = player.getLevel();
        this.startPos = player.getPosition().floor();
        this.length = length;
        this.width = width;
        this.maxHeight = maxHeight;
        this.plugin = plugin;
        this.player = player;
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

        // ✅ Start at bottom-left corner but inset by 1 block (not directly on the edge)
        Vector3 lastBlock = new Vector3(
            startPos.x - width / 2 + 1,
            startPos.y,
            startPos.z - length / 2 + 1
        );

        level.setBlock(lastBlock, Block.get(Block.GOLD_BLOCK)); // Start block

        int currentHeight = 0;
        int blocksAtCurrentHeight = 0;

        while (currentHeight < maxHeight) {
            int newX, newZ;
            boolean validMove = false;

            do {
                int xOffset = random.nextInt(3) - 1; // -1, 0, or 1
                int zOffset = random.nextInt(3) - 1; // -1, 0, or 1

                // ✅ Ensure spacing of 1 to 2 blocks per move
                xOffset *= random.nextBoolean() ? 2 : 1;
                zOffset *= random.nextBoolean() ? 2 : 1;

                newX = (int) lastBlock.x + xOffset;
                newZ = (int) lastBlock.z + zOffset;

                // ✅ Prevent out-of-bounds movement
                if (newX <= startPos.x - width / 2 + 1 || newX >= startPos.x + width / 2 - 1) {
                    continue;
                }
                if (newZ <= startPos.z - length / 2 + 1 || newZ >= startPos.z + length / 2 - 1) {
                    continue;
                }

                // ✅ Ensure the block is not directly above the last block
                if (newX != (int) lastBlock.x || newZ != (int) lastBlock.z) {
                    // ✅ Ensure the block is NOT on the edge (except start block)
                    if (newX > startPos.x - width / 2 + 1 && newX < startPos.x + width / 2 - 1 &&
                        newZ > startPos.z - length / 2 + 1 && newZ < startPos.z + length / 2 - 1) {
                        
                        // ✅ Ensure second level and beyond have air below them
                        if (currentHeight >= 1) {
                            Vector3 belowBlock = new Vector3(newX, startPos.y + currentHeight - 1, newZ);
                            if (level.getBlock(belowBlock).getId() != Block.AIR) {
                                continue; // Skip if block is not air
                            }
                        }

                        // ✅ Ensure third level and beyond have TWO blocks of air below them
                        if (currentHeight >= 2) {
                            Vector3 belowBlock1 = new Vector3(newX, startPos.y + currentHeight - 1, newZ);
                            Vector3 belowBlock2 = new Vector3(newX, startPos.y + currentHeight - 2, newZ);
                            if (level.getBlock(belowBlock1).getId() != Block.AIR || level.getBlock(belowBlock2).getId() != Block.AIR) {
                                continue; // Skip if both blocks are not air
                            }
                        }

                        // ✅ Ensure fourth level and beyond have THREE blocks of air below them
                        if (currentHeight >= 3) {
                            Vector3 belowBlock1 = new Vector3(newX, startPos.y + currentHeight - 1, newZ);
                            Vector3 belowBlock2 = new Vector3(newX, startPos.y + currentHeight - 2, newZ);
                            Vector3 belowBlock3 = new Vector3(newX, startPos.y + currentHeight - 3, newZ);
                            if (level.getBlock(belowBlock1).getId() != Block.AIR || 
                                level.getBlock(belowBlock2).getId() != Block.AIR || 
                                level.getBlock(belowBlock3).getId() != Block.AIR) {
                                continue; // Skip if any of the three blocks below are not air
                            }
                        }

                        validMove = true;
                    }
                }

            } while (!validMove);

            Vector3 newBlock = new Vector3(newX, startPos.y + currentHeight, newZ);
            level.setBlock(newBlock, Block.get(Block.STONE)); // Place platform
            blocksAtCurrentHeight++;

            // ✅ Ensure at least 4 blocks are placed per height level
            if (blocksAtCurrentHeight >= 8) {
                currentHeight++; // Move upward only after placing 4 blocks
                blocksAtCurrentHeight = 0;
            }

            lastBlock = newBlock.clone();
        }

        // ✅ Place end block at max height
        Vector3 endBlock = new Vector3(lastBlock.x, lastBlock.y, lastBlock.z);
        level.setBlock(endBlock, Block.get(Block.DIAMOND_BLOCK));

        plugin.getLogger().info("Jump puzzle generated successfully!");
    }
}
