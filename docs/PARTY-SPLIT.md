# Party Split

**Built.** `PartyMode.SPLIT.isReady()` is `true` and the mode is live in the
party match picker. This is what it does; the "decisions" section at the end
records how the open questions were settled.

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

## Where it lives

| Where | What |
|---|---|
| `Party` | `Team` enum, `teams` map, `mode`, `shuffleTeams`, `aliveOn`, `aliveOrMembers` |
| `PartyManager.startMatch(leader, mode)` | assigns any team-less member, refuses a side with nobody online, spawns by team |
| `PartyManager.checkWin` | counts sides, not heads; `endSplit` announces the winning team |
| `DuelListener.onPartyFriendlyFire` | same-team hits cancelled while a Split match runs |
| `MeowDuelsPlaceholders.partyPrefix` | aqua/red by side, skull when out |
| `PartyTeamMenu` | the picker |

## How the open questions were settled

1. **Knocked-out members keep their team.** `teams` is membership, `alive` is
   who is still in - exactly the two-set shape FFA already used. So someone
   eliminated from aqua still reads as aqua everywhere except the alive count,
   and shows a skull instead of the bolt.
2. **Uneven teams are allowed.** 3v2 starts. Refusing an odd count would make a
   five-person party unable to play the mode at all, which is worse than an
   uneven match. The odd member goes to AQUA - predictable rather than
   whichever side the loop reached first.
3. **A side with nobody ONLINE refuses to start.** Distinct from an empty team:
   the picker will not let you start with an empty side, and `startMatch`
   re-checks against who is actually online, because members can log off between
   the picker being drawn and start being pressed.

## Party Duels

Still `isReady() == false`. It is a different shape again - it pairs members off
into real duels through `DuelManager` rather than running one match - but the
pairing UI is `PartyTeamMenu` with pairs instead of sides, so that screen is the
place to start.
