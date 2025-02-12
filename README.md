## **KillQuest - A Nukkit Quest System**
**KillQuest** is a **feature-rich quest system** for **Nukkit** servers, allowing players to complete **kill and gather quests** for rewards. It includes **multilingual support**, **scoreboard tracking**, and **EconomyAPI integration**.

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
✔ **Quest System:** Kill entities and gather items to complete quests.  
✔ **Dynamic Quest Selection:** Players can select quests using a UI (`/qk`).  
✔ **Scoreboard Tracking:** Displays active quest progress in the top-right.  
✔ **Auto-Saving:** Quest progress is saved to files per player.  
✔ **EconomyAPI Integration:** Players earn credits upon completion.  
✔ **Multilingual Support:** Uses Minecraft’s translations for entity/item names.  

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

---

## **📝 Configuration**
### **`quests.yml`**
Quests are defined in `plugins/KillQuestPlugin/quests.yml`:

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
```
- **Kill Targets:** Define entities to kill.
- **Gather Items:** Define items to collect.
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
| `/killquest reload` | Reloads quests and translations |

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
 [INFO ] [KillQuestPlugin]                            Version: 1.0.0
 [INFO ] [KillQuestPlugin]                            Developed by digitalwm
 [INFO ] [KillQuestPlugin]
 [INFO ] [KillQuestPlugin] Loaded language: en_US with 41 keys.
 [INFO ] [KillQuestPlugin] Loaded language: de_DE with 41 keys.
 [INFO ] [KillQuestPlugin] Total languages loaded: 2
 [INFO ] [KillQuestPlugin] Total translation keys loaded: 82
 [INFO ] [KillQuestPlugin] KillQuestPlugin enabled with 23 available quests.
 [INFO ] [KillQuestPlugin] EconomyAPI found. Rewards enabled.
  ```

---

## **💡 Future Improvements**
- ✅ **More Quest Types:** Fishing, Crafting, Mining
- ✅ **Permissions Support**
- ✅ **More Customization Options**
- ✅ **SQL Database support**
  
🚀 **Want to contribute? Fork the project and submit a pull request!** 🎯

---

## **📄 License**
KillQuest is an open-source Nukkit plugin, developed by **digitalwm**.  
Feel free to modify and distribute it! 🎮🔥  

---

### 🎉 **Enjoy Your Nukkit Quest System!** 🏆
