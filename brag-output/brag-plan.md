# Brag Plan: MeowDuels

## What is this app?
MeowDuels is the duels, party and FFA-event engine behind the BLOODTHIRST
Minecraft server — 18,296 lines of Java that turn arena setup, kit creation,
ranked matchmaking, team fights and a shrinking world border into in-game menus,
and which is built against a hand-generated fake Paper API because the build
machine cannot reach `repo.papermc.io`.

## The angle
Play it completely straight. Shoot a Minecraft PvP plugin like enterprise
infrastructure — real in-game UI, real hex values, real specs, corporate
"business moves" music — and let the punchline be that every claim on screen is
literally true. The joke is not a joke; it's a build verifier that checks
constant-pool reference kinds, for a plugin that helps people hit each other
with swords.

Nothing in the video is invented. Every colour, every glyph, every number comes
out of the repository.

## Hook (first 2-3 seconds)
A real kill-feed line slams onto black, in the plugin's exact colours:

`☠ Nafeh was killed by GoodBoy • 3 left`

Skull `#FF3B57`, victim `#FF8A93`, killer `#7CFF6B`, survivor count `#E6E8EB`,
separator `#6B7079`. No logo, no setup. Anyone who has played on a PvP server
knows instantly what they are looking at, and "3 left" implies a match already
in progress.

## Key moments (the middle)
- **The party mode picker, built one slot at a time.** The real 3-option menu:
  ᴘᴀʀᴛʏ ꜰꜰᴀ, ᴘᴀʀᴛʏ ꜱᴘʟɪᴛ, ᴘᴀʀᴛʏ ᴅᴜᴇʟꜱ (greyed, "soon") under the actual
  ice-blue→indigo gradient title. Cursor clicks ꜱᴘʟɪᴛ.
- **The Split countdown, live.** The countdown number, the real subtitle
  "ᴛᴇᴀᴍ ᴠꜱ ᴛᴇᴀᴍ • ʏᴏᴜ'ʀᴇ ᴏɴ ᴛᴇᴀᴍ ᴀǫᴜᴀ", and the draining block bar
  (█████░░░░) that ticks twice a second — then ꜰɪɢʜᴛ in the green gradient.
- **The spec slam.** Three true engineering lines, deadpan, one at a time.

## Outro / punchline
Three lines land, hold together, then cut to the wordmark:

> 416 ᴀᴘɪ ᴍᴇᴍʙᴇʀꜱ ᴠᴇʀɪꜰɪᴇᴅ ᴇᴠᴇʀʏ ʙᴜɪʟᴅ
> ᴄᴏᴍᴘɪʟᴇꜱ ᴡɪᴛʜ ɴᴏ ɪɴᴛᴇʀɴᴇᴛ
> **ꜰᴏʀ ᴀ ᴍɪɴᴇᴄʀᴀꜰᴛ ᴅᴜᴇʟꜱ ᴘʟᴜɢɪɴ.**

The third line is the joke and it is delivered without a wink. Then MeowDuels /
bloodthirstsmp.fun on the track's strong cue.

## User flow worth showing
The party flow, which is the newest and most demonstrable path in the plugin:

1. **Entry** — right-click ᴘᴀʀᴛʏ ᴍᴀᴛᴄʜ, the mode picker opens.
2. **Key action** — pick ᴘᴀʀᴛʏ ꜱᴘʟɪᴛ; teams are assigned, both sides spawn.
3. **Result** — the countdown runs with your side named, then ꜰɪɢʜᴛ, then a
   kill lands on the feed.

The hook is deliberately the *end* of that flow (a kill), so the video opens at
the payoff and then shows how you get there.

## Tone
- Preset: `polished`
- Creative direction: a corporate product film for a Minecraft duels plugin,
  where every spec on screen is real
- Interpretation: few scenes, long settled holds, restrained motion, no shake
  and no flash. Confidence through restraint — the comedy comes entirely from
  the seriousness of the treatment meeting the subject matter, so nothing on
  screen is allowed to signal that it knows it is funny.

## Format: landscape — 1920x1080
## Duration: 21 seconds

## Visual identity (from the project)
Pulled from `meowduels/src/main/resources/config.yml` and the `gui/` sources.
There is no website and no CSS — these are the MiniMessage colours the plugin
actually renders in game.

- Background: `#0B0C0E` (near-black; the game's chat/menu void, not pure #000)
- Panel: `#15171A` with `#24272C` hairline borders (inventory-slot feel)
- Accent (brand): `#D90707` — the BLOODTHIRST scoreboard title red
- Accent (party): gradient `#FF8AD0` → `#B04BD6`
- Accent (split/aqua): gradient `#7DE2FF` → `#4B7BFF`
- Accent (go/win): gradient `#7CFF6B` → `#1FA32F`
- Accent (gold/victory): `#FFD65C` → `#FFB02E`
- Text: value `#E6E8EB`, label `#8E959D`, muted `#6B7079`
- Display font: a pixel/geometric face for the wordmark; **all in-game strings
  must render as Unicode small caps exactly as written in the source**
  (ᴘᴀʀᴛʏ ꜱᴘʟɪᴛ, not PARTY SPLIT) — that casing is the plugin's signature and
  faking it with CSS `text-transform` would be wrong.
- Body font: a clean grotesque for the spec lines (Inter or similar)
- Strongest visual element: the sidebar scoreboard and the kill feed — both are
  instantly readable as "a real PvP server" to the target audience.

## Share copy (draft)
Built the duels engine for my own Minecraft server: parties, team splits, FFA
with a shrinking border — and a build step that verifies 416 API references
every compile, because one wrong constant-pool entry crashes the server every
tick. 21 seconds.

## Audio direction
- Role: sparse professional accents over a steady corporate bed
- Music: `happy-beats-business-moves-vol-10-by-ende-dot-app.mp3` — 109.96 BPM,
  60s. Chosen *because* it is upbeat corporate stock music; that is the whole
  tonal gag, so it must be played completely sincerely.
- Music treatment: start at 0.00s, sit low (≈0.35) under the hook, lift to ≈0.5
  from the mode picker, hold, and fade out over the final 0.6s after the
  wordmark lands.
- Music cue guidance: preset cue file read from
  `assets/music/cues/happy-beats-business-moves-vol-10-by-ende-dot-app.music-cues.json`.
  - Strong cue: **20.19s** — reserve this for the wordmark slam. The video is
    built to 21s specifically so this lands on the logo.
  - Beat grid (~0.545s apart): 0.27, 0.82, 1.37, 1.90, 2.46, 3.01, 3.55, 4.10,
    4.64, 5.19, 5.74, 6.28, 6.82, 7.35, 7.79, 8.22, 8.73, 9.29, 9.83, 10.38,
    10.93, 11.47, 12.02, 12.56, 13.11, 13.64, 14.20, 14.73, 15.28, 15.82,
    16.38, 16.93, 17.47, 18.01, 18.55, 19.10, 19.64, 20.19, 20.74
  - Sequential reveals: menu slots on **every other beat** (~1.09s apart) so
    each label clears the 0.8s reading floor. Spec lines likewise.
- Audio-reactive treatment: subtle. The wordmark may breathe slightly with
  low-band energy. No waveform bars, no pumping panels.
- SFX posture: sparse, motion-matched. Roughly six cues in 21 seconds.
- Audio-coupled moments: the kill-feed line arriving, each menu slot landing,
  the cursor click on ꜱᴘʟɪᴛ, each countdown bar step, the ꜰɪɢʜᴛ reveal, the
  wordmark on the 20.19s cue.
- Restraint rule: no whooshes on text, no riser into the outro, no stacked
  impacts. If a cue is not matched to something physically moving on screen, it
  does not go in. The music never ducks for effect.

## Storyboard

### Scene 1 — Kill feed — 3.0s
Black `#0B0C0E`. A single kill-feed line fades up centre-left at chat scale and
holds: `☠ Nafeh was killed by GoodBoy • 3 left`, in the exact per-token colours
listed above. Nothing else on screen. At ~2.1s the "3 left" ticks to "2 left"
with a small counter flip — the only motion in the scene.
Sequential/interaction: yes — the survivor count decrements once, on the beat at 2.46s.
Audio intent: cold open, low bed, one dry accent on the line's arrival.
Audio-coupled idea: single soft interface cue as the line lands; a quieter tick on the count flip.
Music: low corporate bed, already running.
Transition mood: clean → Scene 2

### Scene 2 — Wordmark — 3.5s
Cut to the wordmark **MeowDuels** centred, filled with the `#FF8AD0`→`#B04BD6`
party gradient, with the line below in small caps and label grey:
ᴛʜᴇ ᴅᴜᴇʟꜱ ᴇɴɢɪɴᴇ ʙᴇʜɪɴᴅ ʙʟᴏᴏᴅᴛʜɪʀꜱᴛ. The sidebar scoreboard (BLOODTHIRST title,
⚔ ᴋɪʟʟꜱ, ☠ ᴅᴇᴀᴛʜꜱ, ⌚ ᴘʟᴀʏᴛɪᴍᴇ, strikethrough dividers, bloodthirstsmp.fun)
slides in at the right edge and stays as a parked element.
Sequential/interaction: yes — scoreboard rows fill top-to-bottom on the beat grid, then settle.
Audio intent: the bed opens up; this is the "product film" moment.
Audio-coupled idea: one soft riserless swell as the wordmark sets; light ticks per scoreboard row.
Transition mood: soft crossfade → Scene 3

### Scene 3 — Party mode picker — 4.5s
The real menu, recreated as a dark inventory panel: gradient title
ᴘᴀʀᴛʏ ᴍᴀᴛᴄʜ in `#7DE2FF`→`#4B7BFF`, three slots appearing **one at a time on
every other beat** and each holding ≥0.9s once landed:
1. ᴘᴀʀᴛʏ ꜰꜰᴀ — "ᴇᴠᴇʀʏᴏɴᴇ ꜰᴏʀ ᴛʜᴇᴍꜱᴇʟᴠᴇꜱ, ʟᴀꜱᴛ ᴏɴᴇ ꜱᴛᴀɴᴅɪɴɢ"
2. ᴘᴀʀᴛʏ ꜱᴘʟɪᴛ — "ᴛᴡᴏ ᴛᴇᴀᴍꜱ, ᴀǫᴜᴀ ᴀɢᴀɪɴꜱᴛ ʀᴇᴅ"
3. ᴘᴀʀᴛʏ ᴅᴜᴇʟꜱ — greyed, "ꜱᴏᴏɴ"
Then a cursor moves to slot 2 and clicks; the slot flashes its selected state.
Sequential/interaction: yes — three slots one by one, then a simulated cursor click on ꜱᴘʟɪᴛ.
Audio intent: three clean arrivals, then a decisive click. This is the scene that must feel like software.
Audio-coupled idea: one interface cue per slot; a distinct click for the cursor press.
Transition mood: clean → Scene 4

### Scene 4 — Split countdown → FIGHT — 4.5s
Arena-dark frame. Centred countdown number (3 → 2 → 1) in the gold gradient,
under it the real Split subtitle ᴛᴇᴀᴍ ᴠꜱ ᴛᴇᴀᴍ • ʏᴏᴜ'ʀᴇ ᴏɴ ᴛᴇᴀᴍ ᴀǫᴜᴀ (team name
in `#7DE2FF`), and below that the action-bar block meter draining in half-second
steps: `████████████████████` → `░░░░░░░░░░░░░░░░░░░░`. On the last step, cut to
ꜰɪɢʜᴛ in the `#7CFF6B`→`#1FA32F` gradient, held.
Sequential/interaction: yes — the bar loses cells twice a second, matching the plugin's real 10-tick step.
Audio intent: rising pips that climb in pitch as the number falls, exactly as the plugin does it, then one clean hit on ꜰɪɢʜᴛ.
Audio-coupled idea: one short tick per bar step, pitch climbing; single impact on ꜰɪɢʜᴛ.
Transition mood: hard cut → Scene 5

### Scene 5 — Spec slam + outro — 5.5s
Back to near-black. Three lines arrive one at a time, each holding ≥0.9s, all
three on screen together for ~1.2s:
- 416 ᴀᴘɪ ᴍᴇᴍʙᴇʀꜱ ᴠᴇʀɪꜰɪᴇᴅ ᴇᴠᴇʀʏ ʙᴜɪʟᴅ
- ᴄᴏᴍᴘɪʟᴇꜱ ᴡɪᴛʜ ɴᴏ ɪɴᴛᴇʀɴᴇᴛ
- **ꜰᴏʀ ᴀ ᴍɪɴᴇᴄʀᴀꜰᴛ ᴅᴜᴇʟꜱ ᴘʟᴜɢɪɴ.** (white, the only bold-weight line)
Then a clean cut to the **MeowDuels** wordmark with `bloodthirstsmp.fun` in
`#D90707` beneath it, landing on the strong cue at **20.19s** and holding to
21.0s while the music fades.
Sequential/interaction: yes — three spec lines one by one on every other beat, then the wordmark.
Audio intent: dry, unhurried, no build. The wordmark gets the track's own strong beat and nothing added on top of it.
Audio-coupled idea: one quiet cue per spec line; the wordmark rides the 20.19s strong cue with at most one soft impact.
Transition mood: hold to black

**Music mood for this video:** upbeat corporate, played sincerely
**Audio summary:** A steady business-stock bed runs the whole 21 seconds, lifting once at the wordmark and once into the fight, with about six motion-matched interface cues and a single strong beat reserved for the closing logo — the soundtrack of a SaaS explainer, applied without irony to a Minecraft PvP plugin.
