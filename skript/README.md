# skript/

`ffa.sk` - the server's FFA arena script, kept here so its changes are tracked
alongside the plugins it shares a server with. Drop it in `plugins/Skript/scripts/`
and run `/sk reload ffa`.

## Where the line is drawn between this and MeowDuels

Both act on join, on death and on respawn, so they have to agree on who owns what:

| | owner | why |
|---|---|---|
| join -> spawn + spawn items | both | MeowDuels moves first, the script moves last; join teleports are contested and the last mover wins |
| death outside an arena -> spawn + spawn items | MeowDuels | only it can tell a lobby death from a duel death, and a second teleport here would drag people out of duels |
| death inside an FFA arena -> arena + kit | this script | MeowDuels sends them to spawn on the respawn tick, this pulls them back ten ticks later |
| combat tag | this script | MeowDuels has no notion of one |

## Options

`spawn_command` and `spawn_items_command` at the top name the two commands the
join handler runs. `meowduelsspawnitems` is MeowDuels' own; change `spawn` if
this server calls it something else.
