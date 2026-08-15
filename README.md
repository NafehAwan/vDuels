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
| `/editgui <menu>` | Customise a GUI's layout (`duelconfirm`, `kitmenu`, `mapselect`). |
| `/adminduel` | Shortcut for `/editgui kitmenu`. |
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
| `/duel <player>` | Opens the **kit picker**; choosing a kit opens **DUEL CONFIRM**. |
| `/duel accept [player]` | Accepts a pending challenge (or click the **[CLICK HERE]** message). |

`/duel <player>` first shows the **kit selection** grid (the layout you set with
`/adminduel`). Pick a kit and the **DUEL CONFIRM** menu opens with four controls:
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

## Customising the GUIs

Every duel-flow GUI has an editor: `/editgui <menu>` where `<menu>` is one of
`duelconfirm`, `kitmenu` or `mapselect` (`/adminduel` is a shortcut for
`kitmenu`).

The editor's top area is freely editable — drag the buttons / kit icons / arena
icons around, and click **Decoration** to grab black stained-glass panes to
arrange around them. The bottom row holds the controls: **Save Layout**,
**Cancel**, the decoration palette, and **Reset to Default**. Closing the menu
also saves. Players then see your arrangement, with each button showing its live
content (selected kit, arena, rounds, etc.).

- On **duelconfirm** the four buttons (map / kit / clock / confirm) can be moved;
  any button you drag out is restored at save so the menu can't break.
- On **kitmenu** / **mapselect** you arrange the kit / arena icons and decoration;
  the Back and Random buttons stay on the fixed bottom row. (Kits/arenas added
  after you save a custom layout won't appear until you edit again or Reset.)

---

## Data files

Everything is stored as readable YAML inside `plugins/vDuels/`:

- `arenas.yml` – arenas and their settings.
- `kits.yml` – saved kits.
- `gui-layouts.yml` – the custom GUI layouts from `/editgui`.
- `messages.yml` – every player-facing message and title (see below).
- `config.yml` – global options (scoreboard IP, timings).

### Editing messages

`messages.yml` is generated on first run and holds all player-facing chat
messages, the duel-request card and the duel titles. Edit any of them and
reload the server — deleting a key just falls back to the built-in default.

- `{prefix}` inserts the message prefix (default `&fⓘ `).
- Placeholders like `{target}`, `{kit}`, `{rounds}`, `{opponent}`, `{winner}`,
  `{yourScore}`, `{theirScore}`, `{seconds}`, `{name}` are filled in per message.
- Colours use `&` codes; text is rendered in the small-caps font automatically.

---

## Notes

- The arena **duplicator** and **auto-regeneration** use a built-in,
  version-safe block engine, so **FastAsyncWorldEdit is not required**. If FAWE
  is installed it is detected as a soft dependency for future integration.
- Auto-regeneration tracks the blocks that actually change during a fight, so it
  is cheap even on large arenas.
