# Party Split — plan

Not built. The mode appears in the party match picker greyed and labelled
"coming soon"; `PartyMode.SPLIT.isReady()` is `false` and the picker refuses the
click. This is the design to build against.

## Flow

Identical to Party FFA up to the kit, then one extra screen:

```
Party Match item  →  mode picker  →  kit picker  →  TEAM PICKER  →  start
                                     (header: ᴘᴀʀᴛʏ ꜱᴘʟɪᴛ)         (new)
```

FFA goes kit → start confirm. Split goes kit → team picker, and the team picker
carries its own start button, so the hopper confirm is skipped: the team picker
already shows everything the confirm would have said.

## The team picker

Two sections, aqua and red, in one window. Heads, small-caps names, no bold.

```
        ᴛᴇᴀᴍ ᴀQᴜᴀ                    ᴛᴇᴀᴍ ʀᴇᴅ
 .  H  H  H  H  .  H  H  H          rows 1-2, aqua left of centre,
 .  H  H  .  .  .  H  H  .          red right, divider column down the middle
 .  .  ⇄ ꜱʜᴜꜰꜰʟᴇ  .  ꜱᴛᴀʀᴛ  .  .
```

- **Pre-shuffled on open.** Members are split evenly at random; an odd member
  goes to whichever side is smaller (i.e. the first team in the tie-break, so it
  is deterministic rather than "whichever the loop reached first").
- **Click a head to move it** to the other team. Left or right click, same
  result - two ways to do one thing is two ways to be surprised.
- **Shuffle** re-randomises both teams from scratch.
- **Start** begins the match. Greyed while either team is empty.

Only the leader can open it or click anything in it, like every other party
management screen.

## Starting

- Aqua spawns at the arena's **player-1** spawn, red at **player-2**. This is
  why Split can use ordinary duel arenas rather than needing an FFA spawn: those
  two points already exist on every configured arena.
- Nametag colour by team, reusing the duel colours (`§b` aqua, `§c` red) through
  the same relational `tagprefix` the duel bolt uses - see
  `MeowDuelsPlaceholders.partyPrefix`. No second colouring mechanism.
- Friendly fire **off within a team** for the duration, regardless of the
  party's friendly-fire setting: a team mode where you can kill your own team by
  accident is a bug report waiting to happen.
- Win condition: a team is out when all its members are eliminated. Last team
  standing wins.

## What has to change

| Where | What |
|---|---|
| `Party` | `Map<UUID, Team> teams`, `Team` enum (AQUA/RED), and `mode` so the match knows which shape it is |
| `PartyManager.startMatch` | split spawns by team; win check counts teams, not heads |
| `PartyManager.onDeath` | eliminate, then check whether that emptied a team |
| `DuelListener.onPartyFriendlyFire` | same-team hits cancelled while a Split match runs |
| `MeowDuelsPlaceholders.partyPrefix` | team colour when the party is mid-Split |
| new `PartyTeamMenu` | the screen above |

## Decisions worth making before building

1. **Rejoining a Split match.** FFA lets a knocked-out member watch. Split
   should too, but a spectator who was on aqua is not "on aqua" any more - the
   team map needs to keep them for the scoreboard while excluding them from the
   alive count. Keeping two sets (`teams` for membership, `alive` for who is
   still in) is the same shape FFA already uses.
2. **Uneven teams.** 3v2 is allowed above. The alternative - refusing to start
   on an odd count - is worse: it makes a five-person party unable to play the
   mode at all.
3. **Party Duels**, the third mode, is a different shape again: it pairs members
   off into real duels through `DuelManager` rather than running one match.
   Worth building after Split, because the pairing UI is the team picker with
   pairs instead of sides.
