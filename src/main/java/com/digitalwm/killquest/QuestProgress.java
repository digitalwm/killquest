package com.digitalwm.killquest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class QuestProgress {
    private Map<String, Integer> kills = new HashMap<>();
    private Map<String, Integer> gather = new HashMap<>();
    private boolean completed = false;

    public Map<String, Integer> getKills() { return kills; }
    public Map<String, Integer> getGather() { return gather; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}