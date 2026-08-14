# vDuels — roadmap

A running backlog. Items marked ✅ are now implemented; the rest are pending.

- ✅ Per-player duel scoreboard + `/vduels:scoreboardip`
- ✅ Duel-confirm GUI redesign (map / clock / kit / green-glass confirm)
- ✅ Map selector GUI + challenger-chosen arena
- ⏳ Parties + party matches (needs a spec — see bottom)

---

## ✅ Per-player duel scoreboard

A sidebar scoreboard shown **only to players who are currently in a duel**.
Each player sees **their own** view of the match (own team, own ping, score and
timer), so two duellists in the same match see different boards, and everyone
not in a duel sees no vDuels board at all.

**Layout (from the reference screenshot):**

```
                DUELS
  ⓘ Score:  <you> - <them>
  🟦 Team:  BLUE            (BLUE for player 1, RED for player 2, coloured)
  ✦ Ping:  <n>ms           (that player's own ping)
  🕐 Time:  mm:ss           (elapsed match time, updates live)
            <server-ip>     (configurable footer, e.g. desertianc.fun)
```

**Behaviour**
- Attached when a duel starts, torn down when it ends / the player leaves the
  duel; the player's previous scoreboard is restored afterward.
- Live-updating (ping + timer refresh on a short repeating task).
- `Score` is oriented to the viewer (your score first).
- `Team` colour is decided per duel (player 1 = BLUE, player 2 = RED).

**Config / command**
- Server IP shown in the footer is configurable and stored in `config.yml`
  (e.g. `scoreboard-ip: "desertianc.fun"`).
- Command `/vduels:scoreboardip <ip>` (admin) sets it, saves config, and
  refreshes any live boards.

**Implementation notes (for later)**
- Use a per-player scoreboard from `ScoreboardManager#getNewScoreboard()` so
  each duellist gets an independent board.
- A repeating task (≈ every 20 ticks) updates ping/time; team/score come
  straight from the `ActiveDuel`.
- Hook into `DuelManager`: `startDuel`/`startRound` → attach & start updating;
  `endMatch`/disconnect → detach & restore.
- Player ping via `Player#getPing()`.

---

## ✅ Duel confirm GUI redesign

Rework the `/duel <player>` menu into a **DUEL CONFIRM: <opponent>** GUI.

**Icons (from the screenshots):**
- **Map** (`FILLED_MAP`) → **arena / map selector** — opens the Map Selector GUI
  below so the challenger picks which map to play on (instead of auto-choosing).
- **Clock** (`CLOCK`) → **rounds selector** — click to cycle the round count.
- **Kit** icon → kit selector (keep; the gold icon in the screenshot).
- **Green stained glass** → **confirm & send** the challenge.
- **Remove the book** item that's there now.

## ✅ Map selector GUI

A **DUEL MAP: <name>** GUI opened from the map icon above.
- Lists the available arenas as icons (map / representative block, e.g. grass).
- Click an arena to pick it as the duel's map.
- **Arrow** = back to the confirm GUI.
- Only show arenas that are configured and compatible with the chosen kit.

**Implementation note:** the duel request must then carry the chosen arena
(or "random"); `DuelManager.acceptRequest` uses that specific arena instead of
`findFreeArena`, falling back to a free compatible arena if it's taken.

## ⏳ Parties + party matches (needs spec)

Not built yet — the flow is open-ended. To implement, we need:
- Commands: create/disband, invite/accept, kick, leave, list.
- How party-vs-party matches are started and teams are formed.
- Whether party matches reuse the arena/kit selection or a new "PARTY MATCH" UI.

## Bug fixes done alongside this batch

- Players who quit mid-duel now get their real inventory restored (previously
  they could keep the kit on rejoin).
- Duel state restore no longer tries to teleport a leaving player.
