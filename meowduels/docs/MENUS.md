# Menu plan

## The problem

The plugin speaks two visual languages, and one of them is somebody else's.

`gui/Style.java` is ours: dark grey pane panels, small caps for the plugin's
own words, gradients spent only on titles and decision buttons, green and red
always carrying a symbol. Twelve menus use it.

`DuelConfirmMenu` is a **verbatim copy** of another server's Round Selection
screen — its title, its five slots, its `◆ Information ◆` headings, its
yellow-on-white lore. It was built that way on request, from screenshots. It
has to go, and it is the single most-seen screen in the plugin.

Fourteen more menus use neither: raw `&7&l` titles, black filler, ad-hoc
colours. They are not stolen, they are just old.

So: one language, ours, everywhere. Below is what that language is, and the
order to apply it in.

---

## The language

Three structural signatures. These are the part that makes a window
recognisable before you read a word of it — and none of them is a thing the
copied menu does.

### 1. The header notch

Slot 4, in the **top border row**, holds one item: the thing this window is
about. The party card, the kit being edited, the arena being configured, the
request being built. It is never clickable.

A window with no state to report leaves the notch as a pane. A window that
needs a summary never puts it in the interior, where it would compete with
the choices.

`PartyMenu` already does this. It becomes the rule.

### 2. The action rail

The **bottom border row** has fixed meanings, in every menu, forever:

| col | slot (3 rows / 4 / 6) | meaning |
|-----|----------------------|---------|
| 0   | 18 / 27 / 45         | back — always an arrow, never a coloured dye |
| 3   | 21 / 30 / 48         | cancel / discard — red, with `✖` |
| 5   | 23 / 32 / 50         | confirm / commit — green, with `✔` |
| 8   | 26 / 35 / 53         | next page, next category, cycle |

Empty slots stay panes. A menu that only goes back uses col 0 and nothing
else. Nobody ever has to hunt for the way out, and muscle memory transfers
between screens.

### 3. The interior is only choices

Rows 1..n-2, columns 1..7. Everything in there is a thing you are picking
between. No decoration, no summary, no button that ends the flow — those live
in the notch and the rail. This is what keeps a menu readable at a glance:
the middle of the window is the list of options and nothing else.

### Item grammar

Every item's lore, in this order, nothing else:

```
(blank)
<state>          what this is right now
(blank)
▸ <verb>         what clicking does, dark grey
```

A summary item (the notch) is the one exception: it is `label › value` rows,
no verb line, because it is not a button.

### Colour

- Accent per area, on the window title only: duels `#FF2E55 → #FF7FC4`,
  party `#7DE2FF → #4B7BFF`, events `#FFD65C → #FF8A2E`, admin `#B04BD6`.
- `Style.VALUE` `#E6E8EB` for content, `Style.LABEL` `#8E959D` for labels,
  `Style.MUTED` `#6B7079` for hints.
- Green `#7CFF6B` and red `#FF6B6B` **only** on the rail's confirm/cancel, and
  on a toggle's state line. Never as a label colour.
- Gradient on the window title and the rail's two decision buttons. Nowhere
  else. Five gradients in a window is no gradient at all.

### Typography

Small caps for the plugin's own words — `ᴀʀᴇɴᴀ`, `ʀᴏᴜɴᴅꜱ`, `ꜱᴇɴᴅ`. Normal
font for anything that came from a player or an admin: names, kit titles,
arena names, numbers. Re-casing somebody's name is a bug, not a style. No
bold anywhere.

---

## What each menu becomes

### Phase 1 — the stolen one

**`DuelConfirmMenu`** — 4 rows. Notch: the request summary (opponent, kit,
map, rounds). Interior row 1: `ᴍᴀᴘ`, `ʀᴏᴜɴᴅꜱ`. Rail: back, cancel, `ꜱᴇɴᴅ
ʀᴇǫᴜᴇꜱᴛ`. The `◆ ✦ ⇄` headings, the `Gamemode:` wording and the yellow body go.
Keep what was genuinely a good idea and is nobody's property: the rounds
button listing every option with the live one marked, and LMB/RMB to step it.

This is the one to do first and alone, so it can be looked at in game before
anything else moves.

### Phase 2 — the duel flow around it

- **`KitPickMenu`** — framed, notch shows the opponent, rail col 8 cycles
  category. It is the first screen of `/duel`, so it sets the tone.
- **`MapSelectMenu`** — same grid, notch shows the chosen kit (maps are
  filtered by it), rail back.
- **`MatchSummaryMenu`** — notch is the result. Interior is the two players'
  cards. No rail but back.

### Phase 3 — the fourteen that never got `Style`

Mechanical, one commit each, no design decisions left to make:

`QueuePickMenu`, `TrimKitMenu`, `TrimMenu`, `EventMapMenu`, `ArenaMenu`,
`KitSelectMenu`, `EditKitListMenu`, `KitEditorMenu`, `KitItemsEditMenu`,
`EditKitEffectsMenu`, `GuiEditorMenu`, `TabConfigMenu`.

Admin screens get the same frame and rail but stay wordier — an admin needs
to know what a button will do to their config, and brevity there is a trap.

### Phase 4 — the party set, already close

The eleven party menus use `Style` but predate the notch and the rail. They
need slots moved, not rewriting: summary items into slot 4, confirm/cancel
onto cols 3 and 5, page arrows onto col 8.

---

## Things that will bite

Learned the hard way in this repo. Read before starting.

1. **`Items.rawName` / `rawLore` use `Colors.toSection`, which drops any
   `<tag>` it does not know.** A lore line containing `<player>` loses it
   silently. Write placeholders as `[player]`.

2. **`Text.color` small-caps every letter** — including inside a MiniMessage
   tag, which turns `<gradient:…>` into literal text on screen. Use
   `createRaw` / `rawName` for anything carrying tags or a player's name.

3. **Config values are never overwritten on update.** Changing a shipped
   default changes nothing on a live server. If a menu's text or a layout
   moves to config, it needs a migration that replaces only the exact old
   shipped string — see `MeowDuels.replaceLine`.

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
   `grep` afterwards; do not trust an "ok".
