package com.digitalwm.killquest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import cn.nukkit.math.Vector3;

public class ActiveQuest {
    private Quest quest;
    private QuestProgress progress;
    private Vector3 startPos;

    public ActiveQuest(Quest quest, QuestProgress progress, Vector3 startPos) {
        this.quest = quest;
        this.progress = progress;
        this.startPos = startPos;
    }

    public Quest getQuest() { return quest; }
    public QuestProgress getProgress() { return progress; }
    public void setQuest(Quest quest) { this.quest = quest; }
    public void setProgress(QuestProgress progress) { this.progress = progress; }
    public Vector3 getStartPos() { return startPos; }
}