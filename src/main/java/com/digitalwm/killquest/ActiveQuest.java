package com.digitalwm.killquest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class ActiveQuest {
    private Quest quest;
    private QuestProgress progress;

    public ActiveQuest(Quest quest, QuestProgress progress) {
        this.quest = quest;
        this.progress = progress;
    }

    public Quest getQuest() { return quest; }
    public QuestProgress getProgress() { return progress; }
    public void setQuest(Quest quest) { this.quest = quest; }
    public void setProgress(QuestProgress progress) { this.progress = progress; }
}