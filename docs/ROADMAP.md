# vDuels — planned features (not yet implemented)

A running backlog of features to build together. Nothing here is implemented
yet; it's captured so the design is agreed before we batch the work.

---

## Per-player duel scoreboard

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

## Other requested features (to be detailed)

- Parties + party matches.
- (More coming — user is batching requests.)
