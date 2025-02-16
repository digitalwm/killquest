# KillQuestPlugin Custom Events

The `KillQuestPlugin` provides several custom events that other plugins can listen to and handle. These events are dispatched at various points during the execution of quests and jumping puzzles.

## Events Overview

### JumpPuzzleStartEvent

- **Description**: This event is triggered when a player starts a jumping puzzle.
- **Class**: `com.digitalwm.killquest.events.JumpPuzzleStartEvent`
- **Methods**:
  - `Player getPlayer()`: Returns the player who started the puzzle.
  - `String getPuzzleName()`: Returns the name of the puzzle.
- **Usage**:
  ```java
  @EventHandler
  public void onJumpPuzzleStart(JumpPuzzleStartEvent event) {
      Player player = event.getPlayer();
      String puzzleName = event.getPuzzleName();
      // Handle the event
  }
  ```

### JumpPuzzleEndEvent

- **Description**: This event is triggered when a player completes a jumping puzzle.
- **Class**: `com.digitalwm.killquest.events.JumpPuzzleEndEvent`
- **Methods**:
  - `Player getPlayer()`: Returns the player who completed the puzzle.
  - `String getPuzzleName()`: Returns the name of the puzzle.
- **Usage**:
  ```java
  @EventHandler
  public void onJumpPuzzleEnd(JumpPuzzleEndEvent event) {
      Player player = event.getPlayer();
      String puzzleName = event.getPuzzleName();
      // Handle the event
  }
  ```

### JumpPuzzleTimeoutEvent

- **Description**: This event is triggered when the timer for a jumping puzzle expires.
- **Class**: `com.digitalwm.killquest.events.JumpPuzzleTimeoutEvent`
- **Methods**:
  - `Player getPlayer()`: Returns the player whose puzzle timed out.
  - `String getPuzzleName()`: Returns the name of the puzzle.
- **Usage**:
  ```java
  @EventHandler
  public void onJumpPuzzleTimeout(JumpPuzzleTimeoutEvent event) {
      Player player = event.getPlayer();
      String puzzleName = event.getPuzzleName();
      // Handle the event
  }
  ```

### QuestStartEvent

- **Description**: This event is triggered when a player starts a quest.
- **Class**: `com.digitalwm.killquest.events.QuestStartEvent`
- **Methods**:
  - `Player getPlayer()`: Returns the player who started the quest.
  - `String getQuestName()`: Returns the name of the quest.
- **Usage**:
  ```java
  @EventHandler
  public void onQuestStart(QuestStartEvent event) {
      Player player = event.getPlayer();
      String questName = event.getQuestName();
      // Handle the event
  }
  ```

### QuestEndEvent

- **Description**: This event is triggered when a player completes a quest.
- **Class**: `com.digitalwm.killquest.events.QuestEndEvent`
- **Methods**:
  - `Player getPlayer()`: Returns the player who completed the quest.
  - `String getQuestName()`: Returns the name of the quest.
- **Usage**:
  ```java
  @EventHandler
  public void onQuestEnd(QuestEndEvent event) {
      Player player = event.getPlayer();
      String questName = event.getQuestName();
      // Handle the event
  }
  ```

## Registering Event Listeners

To listen for these events, you need to register an event listener in your plugin. Here is an example of how to do this:

```java
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.event.Listener;
import cn.nukkit.event.EventHandler;
import com.digitalwm.killquest.events.JumpPuzzleStartEvent;
import com.digitalwm.killquest.events.JumpPuzzleEndEvent;
import com.digitalwm.killquest.events.JumpPuzzleTimeoutEvent;
import com.digitalwm.killquest.events.QuestStartEvent;
import com.digitalwm.killquest.events.QuestEndEvent;

public class YourPlugin extends PluginBase implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onJumpPuzzleStart(JumpPuzzleStartEvent event) {
        // Handle the jump puzzle start event
    }

    @EventHandler
    public void onJumpPuzzleEnd(JumpPuzzleEndEvent event) {
        // Handle the jump puzzle end event
    }

    @EventHandler
    public void onJumpPuzzleTimeout(JumpPuzzleTimeoutEvent event) {
        // Handle the jump puzzle timeout event
    }

    @EventHandler
    public void onQuestStart(QuestStartEvent event) {
        // Handle the quest start event
    }

    @EventHandler
    public void onQuestEnd(QuestEndEvent event) {
        // Handle the quest end event
    }
}
```