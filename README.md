# MeowDuels

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

The finished plugin is `target/MeowDuels-1.0.0.jar`. Drop it into your server's
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
| `/editgui <menu> reset` | Clears that saved layout so `gui/<menu>.yml` is back in charge. |
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

### The `gui/` folder

Every one of the 27 menus is a file in `plugins/MeowDuels/gui/`, written out on
first run. Each holds that screen's title, its height, and every item's
material, name, lore, stack size, glow and slot:

```yaml
title: "Round Selection"
rows: 3
items:
  clock:
    slot: 11
    material: CLOCK
    name: "<gold>✦ <yellow>Rounds <gold>✦"
    lore:
      - ""
      - "<yellow>Current <white>{rounds} Rounds"
```

Each file documents its own placeholders at the top. A few conventions:

- **Delete anything to undo it.** Every value in these files is the one the
  plugin uses when the key is missing, so removing a line, an item, or a whole
  file puts that screen back exactly as it shipped. There is no reset command
  because deleting *is* the reset.
- **Two-state items** have `name-on`/`name-off`, `material-on`/`material-off`,
  `lore-on`/`lore-off` and `glow-on`/`glow-off` — a toggle that is on or off, a
  map that is picked or not. A plain `name:` or `material:` covers both states.
- **Theme placeholders** work in any name or lore: `{value}` `{label}` `{muted}`
  `{good}` `{bad}` `{warn}` `{tick}` `{cross}` `{sep}` `{hint}`, plus the
  gradient endpoints `{go-from}` `{go-to}` `{no-from}` `{no-to}` `{accent-from}`
  `{accent-to}`. These follow the `theme:` section of `config.yml`, so one edit
  there restyles all 27 screens at once. Write a colour out instead if you want
  one line to ignore the theme.
- **Colours** use `<yellow>`, `<#FF2E55>`, `<gradient:#7CFF6B:#1FA32F>text</gradient>`
  or legacy `&e` codes.
- **Five menus have no `slot:` or `rows:` keys** — `arena`, `guieditor`,
  `kititems`, `kitselect` and `trim`. Those screens work out what a click did
  from *which slot* was clicked, so moving an item would run the wrong action.
  Their materials, names and lore are still yours to change; each file says so
  at the top.

### `/editgui`, and how it interacts with the above

`/editgui <menu>` opens a drag-and-drop editor for `duelconfirm`, `kitmenu` and
`mapselect` (`/adminduel` is a shortcut for `kitmenu`).

A layout saved there **wins over the `slot:` values in `gui/<menu>.yml`**, and
the editor saves when you close it even if you moved nothing — so opening it
once is enough to pin every slot and make later file edits look ignored. Run
`/editgui <menu> reset` to clear the stored layout and hand control back to the
file.

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

Everything is stored as readable YAML inside `plugins/MeowDuels/`:

- `arenas.yml` – arenas and their settings.
- `kits.yml` – saved kits.
- `gui/` – one file per menu: titles, materials, names, lore, slots (see above).
- `gui-layouts.yml` – the custom GUI layouts from `/editgui`.
- `messages.yml` – every player-facing message and title (see below).
- `config.yml` – global options, the 18-key `theme:` palette, all 23 sound
  cues, golden-head values and the anticheat hooks. Clearing a sound's key
  silences that one cue.

### Editing messages

`messages.yml` is generated on first run and holds **every** player-facing line
— all 425 of them, including the arena setup wizard, so the plugin can be fully
translated. Edit any of them and reload — deleting a key falls back to the
built-in default.

- `{prefix}` inserts the message prefix (default `&fⓘ `).
- Placeholders like `{target}`, `{kit}`, `{rounds}`, `{opponent}`, `{winner}`,
  `{yourScore}`, `{theirScore}`, `{seconds}`, `{name}` are filled in per message.
- Colours use `&` codes.
- **Keep a key under its own prefix.** The prefix decides how a line is
  rendered: `request.`/`titles.` parse MiniMessage or apply small caps,
  `party.`/`event.`/`chat.` resolve hex and gradients, and `notice.`/`match.`/
  `round.`/`death.` take `&` codes as-is. Moving a line between them changes how
  it looks, or leaves a `<gradient:…>` tag showing as literal text.
- The `notice.*` keys are the command and wizard replies. They ship grey
  because the code that used to print them stripped every colour; add `&c` or
  `&a` and they will honour it.

---

## Notes

- The arena **duplicator** and **auto-regeneration** use a built-in,
  version-safe block engine, so **FastAsyncWorldEdit is not required**. If FAWE
  is installed it is detected as a soft dependency for future integration.
- Auto-regeneration tracks the blocks that actually change during a fight, so it
  is cheap even on large arenas.
