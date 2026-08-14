# vDuels

A GUI-based **Duels** plugin for Paper **1.21**. Admins create arenas and kits
entirely through in-game menus and a chat-guided setup wizard; players challenge
each other with `/duel`.

> This is the first pass of the plugin. Parties and the extra features you
> mentioned can be layered on top of this foundation later.

---

## Building

Requires **JDK 21** and **Maven**.

```bash
mvn package
```

The finished plugin is `target/vDuels-1.0.0.jar`. Drop it into your server's
`plugins/` folder and restart. (The build downloads the Paper API from
`repo.papermc.io`, so the machine that builds it needs internet access.)

---

## Admin commands

All admin commands require the `vduels.admin` permission (OP by default). They
also work with the `vduels:` prefix, e.g. `/vduels:createarena`.

| Command | What it does |
| --- | --- |
| `/createarena <name>` | Creates a new (empty) arena. |
| `/arena <name>` | Opens the arena **GUI** (setup + settings). |
| `/deletearena <name>` | Deletes an arena. |
| `/kitcreate <name>` | Saves your **current inventory + armor** as a kit. |
| `/deletekit <name>` | Deletes a kit. |
| `/adminduel` | Opens the **kit-menu editor** (decorate the kit picker). |
| `/vduels <arena> copy` | Copies the arena region to your clipboard (duplicator). |
| `/vduels <arena> paste` | Pastes your clipboard at your feet (duplicator). |
| `/vduels:scoreboardip <ip>` | Sets the server IP shown on the duel scoreboard. |

### Setting up an arena

1. `/createarena myarena`
2. `/arena myarena` – the GUI opens with a green **Setup** pane in the middle.
3. Click **Setup**. The GUI closes and the chat wizard walks you through:
   - *Go to the Player 1 spawn and type `done`*
   - *Go to the Player 2 spawn and type `done`*
   - *Go to corner 1 (the lowest corner) and type `done`*
   - *Go to corner 2 (the highest corner) and type `done`*
   - (Type `cancel` at any time to abort.)
4. When you finish, the arena is saved and the GUI reopens with all the
   **settings**.

### Arena settings (all toggled in the GUI)

- **Auto-Regenerate** – restores every block that changed after each fight.
- **Allow Block Break** – can players break arena blocks?
- **Allow Block Place** – can players place blocks?
- **Remove Own Blocks** – can players break blocks they placed this round even
  when breaking is otherwise off?
- **Duplicator** – enables the `/vduels <arena> copy` / `paste` clone tool.
- **Compatible Kits** – a sub-GUI of every kit; click to allow/disallow it in
  this arena. *Selecting none means every kit is allowed.*

---

## Player commands

| Command | What it does |
| --- | --- |
| `/duel <player>` | Opens the **DUEL CONFIRM** GUI. |
| `/duel accept [player]` | Accepts a pending challenge (or click the **[CLICK HERE]** message). |

**The DUEL CONFIRM menu** has four controls:
- 🗺️ **Map** – opens the map/arena selector (or leave it on **Random**).
- 🎒 **Kit** – opens the kit picker (the layout you set with `/adminduel`).
- ⏰ **Clock** – cycles the number of rounds (first to 1/2/3/5).
- 🟩 **Green pane** – confirms and sends the challenge.

The opponent gets the request card with a clickable **[CLICK HERE]**. Once
accepted, both players go into the chosen arena (or a free compatible one),
get the kit, and a countdown starts. First to the selected round wins takes the
match, after which everyone is restored to exactly where and how they were.

### In-duel scoreboard

While a duel is running each player sees their own sidebar: score, their team
colour (BLUE / RED), ping, elapsed time and the server IP. Set the IP with
`/vduels:scoreboardip <ip>`.

---

## Customising the /duel menu

Run `/adminduel` to open the layout editor. The top five rows are freely
editable — drag the kit icons around, and click **Decoration** to grab black
stained-glass panes to arrange around them. The bottom row holds the controls.
Click **Save Layout** (or just close the menu) and players will see your layout
when they run `/duel`.

---

## Data files

Everything is stored as readable YAML inside `plugins/vDuels/`:

- `arenas.yml` – arenas and their settings.
- `kits.yml` – saved kits.
- `duel-menu.yml` – the custom /duel layout.
- `config.yml` – reserved for future global options.

---

## Notes

- The arena **duplicator** and **auto-regeneration** use a built-in,
  version-safe block engine, so **FastAsyncWorldEdit is not required**. If FAWE
  is installed it is detected as a soft dependency for future integration.
- Auto-regeneration tracks the blocks that actually change during a fight, so it
  is cheap even on large arenas.
