package com.digitalwm.killquest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class Quest {
    private String name;
    private String description;
    private Map<String, Integer> killTargets;
    private Map<String, Integer> gatherItems;
    private int reward;

    public Quest(String name, String description, Map<String, Integer> killTargets,
                    Map<String, Integer> gatherItems, int reward) {
        this.name = name;
        this.description = description;
        this.killTargets = killTargets;
        this.gatherItems = gatherItems;
        this.reward = reward;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Map<String, Integer> getKillTargets() { return killTargets; }
    public Map<String, Integer> getGatherItems() { return gatherItems; }
    public int getReward() { return reward; }
}