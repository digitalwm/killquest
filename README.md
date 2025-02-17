## **KillQuest - A Nukkit Quest System** v1.0.3
**KillQuest** is a **feature-rich quest system** for **Nukkit** servers, allowing players to complete **kill and gather quests** for rewards, and challenge themselves with procedurally generated jumping puzzles. It includes **multilingual support**, **scoreboard tracking**, and **EconomyAPI integration**.

---

## Changes in 1.0.3
- Added quest tracking for distance traveled (in height, depth or any direction)
  - see example quests.yml
- Added better Forms handling of quest selection
- Added /queststatus command to show the detailed view of the active quest
- Added the ability to cancel the active quest in the /queststatus command
- Added the configuration possibility for generated kumpign puzzles
  - /jumpconfig <name> <resetOnCompletion> <greenBlockResetTimeout>
  - resetOnCompletion - Boolean - To regenerate the puzzle when a player finishes the puzzle
  - greenBlockResetTimeout - Int - How many seconds a player have to sit on the green block to triger jumping puzzle regeneration
- Added translation keys for Quest Menus
  - menu.questselector
  - menu.questdetails
  - menu.queststatus
  - menu.questselector.description

## Changes in 1.0.2
- Added regenaration of puzzle when a user finishes it
- Added regeneration of puzzle, using a reset block. You must stay at least 5 seconds on it
- Added 2 Doors around the start block so the players can enter and exit the puzzle
- Added 5 Events to be triggered from within the plugin
  - JumpPuzzleEndEvent
  - JumpPuzzleStartEvent
  - JumpPuzzleTimeoutEvent
  - QuestEndEvent
  - QuestStartEvent

## Changes in 1.0.1

- Added Handling of onPlayerFish event so fishing quests can be created
- Added complete new loging of generating jumping puzzles

---

## **📥 Installation**
1. **Download the Plugin**
   - Compile the plugin using Maven (`mvn clean package`)
   - Find the generated `.jar` file in the `target/` directory.
   - Move the `.jar` file to your `plugins/` folder.

2. **Restart Your Nukkit Server**
   - Run:
     ```sh
     java -jar nukkit.jar
     ```
   - Check logs to confirm the plugin is loaded successfully.

---

## **📜 Features**
- **Quest System:** Kill entities, gather items or travel distances to complete quests.  
- **Dynamic Quest Selection:** Players can select quests using a UI (`/quests`).
- **Active Quest Status:** Players can view active quests using a UI (`/questsstatus`). 
- **Scoreboard Tracking:** Displays active quest progress in the top-right.  
- **Auto-Saving:** Quest progress is saved to files per player.  
- **EconomyAPI Integration:** Players earn credits upon completion.  
- **Multilingual Support:** Uses Minecraft’s translations for entity/item names.  
- **Jumping Puzzles:** Procedurally generate jumping puzzles with varying heights and difficulties.
- **Puzzle Persistence:** Puzzles are saved and can be reloaded after server restarts.
- **Player Movement Tracking:** Detect when players start and complete puzzles.
- **Block Restrictions:** Prevent players from modifying puzzle areas.
- **Puzzle Management Commands:** Create, list, and remove puzzles dynamically.
- **Puzzle Regen Logic:** Regenerate the given puzzle, when the user completes it or using reset block.
- **Server Events:** The plugin sends events when changes are on Quests and Jumping Puzzles.

---

## **🎮 How to Use**
### **1️⃣ Viewing Available Quests**
- Run the command:
  ```sh
  /quests
  ```
- A **selection window** appears, showing **5 random quests** (rotating every 5 minutes).
- If you have an active quest, it **remains selected** while 4 others change.

### **2️⃣ Completing a Quest**
- Track progress via the **scoreboard** (updates live).
- Fulfill the quest objectives:
  - **Kill Targets:** Kill the required entities.
  - **Gather Items:** Pick up or collect required materials.
- Once completed:
  - **Reward is granted**
  - **Quest progress resets**
  - **Scoreboard updates**

### How the Jump Puzzle Works
1. A player generates a puzzle using `/jumpgen`.
2. The puzzle is enclosed with walls and a light-emitting base.
3. Players must jump across blocks that are generated to be challenging but solvable.
4. A tracking system monitors when a player starts and completes the puzzle.
5. Upon completion, the player receives 100 credits via EconomyAPI.
6. Once completed, the user is teleported to the center and the puzzle regenerates
7. If puzzle is imposible, use the green block to regenerate it. Stay on it more than 5 seconds

---

## **For coders**

For more information about the custom events provided by this plugin, see the [Events Documentation](Events.md).

---

## **📝 Configuration**
### **`quests.yml`**
Quests are defined in `plugins/KillQuestPlugin/quests.yml`:

### `puzzles.yml`
Stores all active puzzles, including blocks, start, and end positions, ensuring persistence after restarts.

```yaml
quests:
  - name: "Zombie Slayer"
    description: "Kill 10 Zombies"
    killTargets:
      zombie: 10
    gatherItems: {}
    reward: 100

  - name: "Resource Collector"
    description: "Gather 20 Diamonds and 50 Iron Ore"
    killTargets: {}   # No kills required
    gatherItems:
      diamond: 20
      iron_ore: 50
    reward: 200

  - name: "Long Journey"
    description: "Travel over 500 blocks from your starting position"
    distance: 500
    reward: 50

  - name: "High Climb"
    description: "Climb 200 blocks in height"
    height: 200
    reward: 30

  - name: "Deep Dive"
    description: "Descend 100 blocks in depth"
    depth: 100
    reward: 40
```
- **Kill Targets:** Define entities to kill.
- **Gather Items:** Define items to collect.
- **Distance Traveled:** Define distance to travel (in range, height or depth).
- **Reward:** Credits given via EconomyAPI.

### **`translations.yml`**
Custom language translations can be configured in:
```yaml
translations:
  en_US:
    entity.minecraft.zombie: "Zombie"
    item.minecraft.diamond: "Diamond"
  de_DE:
    entity.minecraft.zombie: "Zombie"
    item.minecraft.diamond: "Diamant"
```
- Supports multiple **Minecraft locales**.
- Used in the **scoreboard UI**.

---

## **🔧 Admin Commands**
| Command | Description |
|---------|------------|
| `/quests` | Opens the quest selection UI |
| `/questsstatus` | Opens the active quest status UI |
| `/killquest reload` | Reloads quests and translations |
| `/jumpgen <name> <length> <width> <height>` | Generates a jumping puzzle with a unique name. |
| `/clearpuzzle <name>` | Clears a specific puzzle. |
| `/listpuzzles` | Lists all active puzzles. |
| `/jumpconfig <name> <resetOnCompletion> <greenBlockResetTimeout>` | Adjust values of generated jumping puzzle |

---

## **📌 Debugging & Logs**
- **Startup Log:**
  ```
  [INFO ] [KillQuestPlugin]
  [INFO ] [KillQuestPlugin] ██╗  ██╗██╗██╗     ██╗      ██████╗ ██╗   ██╗███████╗███████╗████████╗
  [INFO ] [KillQuestPlugin] ██║ ██╔╝██║██║     ██║     ██╔═══██╗██║   ██║██╔════╝██╔════╝╚══██╔══╝
  [INFO ] [KillQuestPlugin] █████╔╝ ██║██║     ██║     ██║   ██║██║   ██║█████╗  ███████╗   ██║
  [INFO ] [KillQuestPlugin] ██╔═██╗ ██║██║     ██║     ██║▄▄ ██║██║   ██║██╔══╝  ╚════██║   ██║
  [INFO ] [KillQuestPlugin] ██║  ██╗██║███████╗███████╗╚██████╔╝╚██████╔╝███████╗███████║   ██║
  [INFO ] [KillQuestPlugin] ╚═╝  ╚═╝╚═╝╚══════╝╚══════╝ ╚══▀▀═╝  ╚═════╝ ╚══════╝╚══════╝   ╚═╝
  [INFO ] [KillQuestPlugin]
  [INFO ] [KillQuestPlugin]                            Version: 1.0.3
  [INFO ] [KillQuestPlugin]                            Developed by digitalwm
  [INFO ] [KillQuestPlugin]
  [INFO ] [KillQuestPlugin] Loaded language: en_US with 41 keys.
  [INFO ] [KillQuestPlugin] Loaded language: de_DE with 41 keys.
  [INFO ] [KillQuestPlugin] Total languages loaded: 2
  [INFO ] [KillQuestPlugin] Total translation keys loaded: 82
  [INFO ] [KillQuestPlugin] KillQuestPlugin enabled with 23 available quests.
  [INFO ] [KillQuestPlugin] EconomyAPI found. Rewards enabled.
  [INFO ] [KillQuestPlugin] Loading saved puzzles...
  [INFO ] [KillQuestPlugin] Loaded puzzle: test
  ```

---

## **💡 Future Improvements**
- ✅ **More Quest Types:** Crafting, Shooting
- ✅ **More Customization Options**
- ✅ **SQL Database support**
  
🚀 **Want to contribute? Fork the project and submit a pull request!** 🎯

---

## **📄 License**
KillQuest is an open-source Nukkit plugin, developed by **digitalwm**.  
Feel free to modify and distribute it! 🎮🔥  

---

### 🎉 **Enjoy Your Nukkit Quest System!** 🏆
