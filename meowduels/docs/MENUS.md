# Menu plan

## The problem

The plugin speaks two visual languages, and one of them is somebody else's.

`gui/Style.java` is ours: dark grey pane panels, small caps for the plugin's
own words, gradients spent only on titles and decision buttons, green and red
always carrying a symbol. Twelve menus use it.

`DuelConfirmMenu` is a **verbatim copy** of another server's Round Selection
screen — its title, its five slots, its `◆ Information ◆` headings, its
yellow-on-white lore. It was built that way from screenshots, on request. It
is the most-seen screen in the plugin and it has to go.

Fourteen more menus use neither: raw `&7&l` titles, black filler, ad-hoc
colours. They are not stolen, they are just old.

One language, ours, everywhere. Below is what it is, what the duel confirm
screen becomes, and the order to do it in.

---

## Which symbols are allowed

**Minecraft draws the Basic Multilingual Plane and nothing else.** The game
bundles GNU Unifont as a fallback for everything its own font misses, and
that fallback covers U+0000–U+FFFF. Miscellaneous Symbols, Dingbats, Arrows,
Box Drawing, Misc Technical — all fine.

Emoji proper live above U+FFFF and do **not** render. 🗡 🏆 🎯 🔥 ⚙️-with-a-
variation-selector: every one of those is a white box without a resource
pack. This is not theoretical — `MeowDuels.migrateDuelMarker()` exists
because `duel-marker` shipped as 🗡 (U+1F5E1) and had to be swapped for ⚔.

So the vocabulary below is emoji in spirit and BMP in fact. Every glyph is
one meaning, used in exactly that meaning, everywhere.

### Verbs — what a click does

| glyph | code | meaning |
|---|---|---|
| ✔ | U+2714 | confirm, send, yes, a toggle that is on |
| ✖ | U+2716 | cancel, no, a toggle that is off |
| ◀ | U+25C0 | back |
| ▶ | U+25B6 | next page, next category |
| ♻ | U+267B | shuffle, reroll, reset |
| ✎ | U+270E | edit |
| ▸ | U+25B8 | the hint line, and the marker on a live option |

### Nouns — what a thing is

| glyph | code | meaning |
|---|---|---|
| ❖ | U+2756 | the summary card — what this window is about |
| ⚔ | U+2694 | a kit, or a fight |
| ⚑ | U+2691 | a fighter (already the tab and sidebar marker) |
| ⛰ | U+26F0 | an arena / map |
| ⌛ | U+231B | rounds, duration |
| ⌚ | U+231A | elapsed time |
| ★ | U+2605 | party leader |
| ◆ | U+25C6 | party member |
| ☠ | U+2620 | knocked out |
| ❤ | U+2764 | health |
| ⚙ | U+2699 | settings |
| ✉ | U+2709 | chat |
| ◉ | U+25C9 | spectators |
| ⓘ | U+24D8 | the chat prefix |

Eleven of those already ship. **⛰, ⌛, ❖ and ✉ are the new ones — eyeball
them in game before committing to them.** They are all in blocks Unifont
covers, but ⛰ is the most pictographic of the four and so the most likely to
look wrong at 8px even if it draws.

If you ever do want real colour emoji, the route is a resource pack with a
`bitmap` font provider, and then the astral-plane characters work. That is a
server decision, not a plugin one — the plugin should keep shipping glyphs
that work on a vanilla client.

---

## The language

Three structural signatures. This is the part that makes a window
recognisable before you read a word of it, and none of the three is
something the copied menu does.

### 1. The header notch

Slot 4, in the **top border row**, holds one item: the thing this window is
about. The party card, the kit being edited, the request being built. Marked
`❖`. Never clickable.

A window with no state to report leaves the notch as a pane. A window that
needs a summary never puts it in the interior, where it would compete with
the choices.

### 2. The action rail

The **bottom border row** has fixed meanings in every menu, forever:

| col | slot (3 rows / 4 / 6) | glyph | meaning |
|-----|----------------------|-------|---------|
| 0   | 18 / 27 / 45         | ◀ | back |
| 3   | 21 / 30 / 48         | ✖ | cancel, discard |
| 5   | 23 / 32 / 50         | ✔ | confirm, commit |
| 8   | 26 / 35 / 53         | ▶ | next page, next category |

Unused rail slots stay panes. Nobody hunts for the way out, and muscle
memory transfers between screens.

### 3. The interior is only choices

Rows 1..n-2, columns 1..7. Everything in there is a thing you are picking
between. No decoration, no summary, no button that ends the flow — those
live in the notch and on the rail.

### Item grammar

```
❖ ɴᴀᴍᴇ          glyph, space, small caps, in the area's accent
(blank)
<state>          what this is right now, in normal font
(blank)
▸ <verb>         what a click does, dark grey
```

The notch is the one exception: `ʟᴀʙᴇʟ › value` rows, no verb line, because
it is not a button.

### Colour

- Accent per area, on the window title and the notch only: duels
  `#FF2E55 → #FF7FC4`, party `#7DE2FF → #4B7BFF`, events
  `#FFD65C → #FF8A2E`, admin `#B04BD6`.
- `#E6E8EB` content, `#8E959D` labels, `#6B7079` hints.
- Green `#7CFF6B` and red `#FF6B6B` **only** on the rail and on a toggle's
  state line. Never as a label colour.
- Gradient on the window title and the rail's two decision buttons. Nowhere
  else. Five gradients in a window is no gradient at all.

### Typography

Small caps for the plugin's own words. Normal font for anything that came
from a player or an admin: names, kit titles, arena names, numbers.
Re-casing somebody's name is a bug, not a style. No bold anywhere.

---

## Sketch: the duel confirm screen

Three rows. `▒` is a dark grey pane.

```
        c0    c1    c2    c3    c4    c5    c6    c7    c8
  r0    ▒     ▒     ▒     ▒     ❖     ▒     ▒     ▒     ▒
  r1    ▒     ▒     ⚔     ▒     ⛰     ▒     ⌛    ▒     ▒
  r2    ◀     ▒     ▒     ✖     ▒     ✔     ▒     ▒     ▒
```

Title: `▏ ᴅᴜᴇʟ ʀᴇǫᴜᴇꜱᴛ` in the duel gradient. Not the opponent's name — that
is the notch's job, and a title cannot be re-read once the window is open.

**❖ slot 4 — the request.** `PAPER`, not clickable.

```
❖ ʀᴇǫᴜᴇꜱᴛ
(blank)
ᴏᴘᴘᴏɴᴇɴᴛ  › Nafeh
ᴋɪᴛ       › Netherite OP
ᴀʀᴇɴᴀ     › Colosseum
ʀᴏᴜɴᴅꜱ    › ꜰɪʀꜱᴛ ᴛᴏ 3
```

**⚔ slot 11 — kit.** The kit's own icon, glowing once chosen.

```
⚔ ᴋɪᴛ
(blank)
Netherite OP
(blank)
▸ ᴄʟɪᴄᴋ ᴛᴏ ᴄʜᴀɴɢᴇ
```

**⛰ slot 13 — arena.** `FILLED_MAP`. Shows `ʀᴀɴᴅᴏᴍ` in grey when unset,
because random is a real choice and not a missing one.

**⌛ slot 15 — rounds.** `CLOCK`. Every option listed with the live one
marked — clicking a hidden value is the thing that made the old menu take
four clicks to find out what the choices even were.

```
⌛ ʀᴏᴜɴᴅꜱ
(blank)
  ꜰɪʀꜱᴛ ᴛᴏ 1
▸ ꜰɪʀꜱᴛ ᴛᴏ 3
  ꜰɪʀꜱᴛ ᴛᴏ 5
(blank)
▸ ʟᴇꜰᴛ ᴜᴘ · ʀɪɢʜᴛ ᴅᴏᴡɴ
```

**◀ 18** back to the kit picker · **✖ 21** close · **✔ 23** send.

Why this is not the screen it replaces: three rows instead of three-with-a-
gap, choices centred in the interior instead of strung along one row, the
summary in the border instead of the middle, state-then-verb lore instead of
`Label: value`, small caps instead of vanilla yellow-on-white, and a kit
button — which that menu could not have, because its gamemode was fixed
before the window opened and ours is not.

What is worth keeping from it, and is nobody's property: listing every round
option with the live one marked, and LMB/RMB to step through them.

---

## Order of work

**Phase 1 — the stolen one.** `DuelConfirmMenu`, alone, so it can be looked
at in game before anything else moves.

**Phase 2 — the duel flow around it.** `KitPickMenu` (first screen of
`/duel`, sets the tone; notch shows the opponent, rail col 8 cycles
category), `MapSelectMenu` (notch shows the kit, since maps are filtered by
it), `MatchSummaryMenu` (notch is the result, interior is the two cards).

**Phase 3 — the fourteen that never got `Style`.** Mechanical, one commit
each, no design decisions left: `QueuePickMenu`, `TrimKitMenu`, `TrimMenu`,
`EventMapMenu`, `ArenaMenu`, `KitSelectMenu`, `EditKitListMenu`,
`KitEditorMenu`, `KitItemsEditMenu`, `EditKitEffectsMenu`, `GuiEditorMenu`,
`TabConfigMenu`. Admin screens get the same frame and rail but stay wordier
— an admin needs to know what a button will do to their config, and brevity
there is a trap.

**Phase 4 — the party set.** Eleven menus that already use `Style` but
predate the notch and the rail. Slots move; nothing is rewritten.

---

## Things that will bite

Learned the hard way in this repo. Read before starting.

1. **`Items.rawName` / `rawLore` run `Colors.toSection`, which drops any
   `<tag>` it does not know.** A lore line containing `<player>` loses it
   silently. Write placeholders as `[player]`.

2. **`Text.color` small-caps every letter** — including inside a MiniMessage
   tag, which puts `<gradient:…>` on screen as literal text. Use `createRaw`
   / `rawName` for anything carrying tags or a player's name.

3. **Config values are never overwritten on update.** Changing a shipped
   default changes nothing on a live server. Text or layout that moves to
   config needs a migration replacing only the exact old shipped string —
   see `MeowDuels.replaceLine`.

4. **`GuiLayoutManager` lets admins move buttons.** Changing a menu's row
   count without changing `GuiLayoutManager.editableSize` lets someone place
   a button in a slot the menu never renders. Changing a button's id orphans
   their saved layout.

5. **Verify the jar, not the source.** `tools/verify_kinds.sh` catches a stub
   declared as a class where Paper has an interface. It does **not** check
   descriptors: a stub method with the wrong return type compiles clean and
   throws `NoSuchMethodError` at runtime. Anything new on a generic Adventure
   interface exists only in its erased form.

6. **A multi-edit script that asserts late writes nothing.** Verify with
   `grep` afterwards; never trust an "ok".

7. **Any glyph above U+FFFF is a white box.** See the symbol table.
