# Brag Plan: MeowDuels

## What is this app?
MeowDuels is a modern duels plugin for Paper 1.21 — ranked 1v1s, queues, kit
and arena setup done entirely through in-game menus, party modes (FFA and
team Split), FFA events with a shrinking border, and arenas that rebuild
themselves after every fight.

## The angle
A product launch for the **plugin**, not for any server. Vertical, fast, and
built entirely out of the plugin's own screens: you watch someone type
`/duel`, configure the match, fight it, win it, and get taunted by the
plugin — then three cards tell you what else is in the box.

Nothing is mocked up in a generic style. Every string, colour and glyph on
screen is copied out of the source, because the plugin's look *is* the product:
the small-caps menu typography, the gradient buttons, the taunt lines.

**Hard constraint from the user: the server this is developed on is never
named, shown, or implied.** No server name, no IP, no scoreboard branding.
The only name in the video is MeowDuels.

## Hook (first 2-3 seconds)
`/duel GoodBoy` types itself into a Minecraft chat bar, character by
character, with key ticks — then ENTER. Short-form gives you one second; a
command being typed is instantly legible to anyone who has played Minecraft
and it sets up a question ("then what?") that the next 16 seconds answer.

## Key moments (the middle)
- **The duel request menu building itself.** The real `DuelConfirmMenu`: title
  `ᴅᴜᴇʟ ʀᴇǫᴜᴇꜱᴛ › GoodBoy`, then ᴀʀᴇɴᴀ, ᴋɪᴛ and ʀᴏᴜɴᴅꜱ landing one at a time,
  then a cursor pressing the green ꜱᴇɴᴅ ʀᴇǫᴜᴇꜱᴛ button.
- **3 · 2 · 1 · ꜰɪɢʜᴛ.** Fast cut, gold numbers, the green gradient on ꜰɪɢʜᴛ.
- **The taunt.** ᴠɪᴄᴛᴏʀʏ, the score, and one of the plugin's real random
  match-end broadcasts: `☄ Nafeh obliterated GoodBoy ☠ • 3-1`. This is the
  personality beat — it is a real feature, not a joke written for the video.

## Outro / punchline
Three feature cards, then the wordmark and one line:
**ᴍᴏᴅᴇʀɴ ᴅᴜᴇʟꜱ ꜰᴏʀ ᴘᴀᴘᴇʀ 1.21**, landing on the track's strong cue.

## User flow worth showing
The duel path, start to finish — it is the plugin's spine and it fits a short:

1. **Entry** — `/duel GoodBoy`.
2. **Key action** — the confirm menu: pick arena, kit, rounds; send.
3. **Result** — countdown, ꜰɪɢʜᴛ, ᴠɪᴄᴛᴏʀʏ, and the match-end taunt.

Scenes 1-4 are that flow in order. The feature cards come after, as the
"and it also does" beat, not as a substitute for the flow.

## Tone
- Preset: `app-store`
- Creative direction: a modern plugin launch cut for vertical short-form —
  clean feature reveals that land hard, no fluff, no irony
- Interpretation: smooth slides and wipes, no shake and no flash. Big
  centre-weighted type because it will be watched on a phone at arm's length.
  Every beat does one thing. The energy comes from cut rhythm, not from motion
  clutter — this is selling a tool to server owners, so it has to look
  competent before it looks exciting.

## Format: vertical — 1080x1920
## Duration: 18.52 seconds

## Visual identity (from the project)
There is no website and no CSS. These are the MiniMessage colours and the
Unicode small-caps strings the plugin renders in game, from `config.yml` and
the `gui/` sources.

- Background: `#0B0C0E`, panel `#15171A`, hairline `#24272C`, inset `#101215`
- Text: value `#E6E8EB`, label `#8E959D`, muted `#6B7079`
- Accent (brand / wordmark): gradient `#FF8AD0` → `#B04BD6`
- Accent (confirm / go / win): gradient `#7CFF6B` → `#1FA32F`
- Accent (countdown / taunt): `#FFD65C` → `#FFB02E`
- Accent (secondary): `#7DE2FF` → `#4B7BFF`
- Loss / danger: `#FF8A93`, `#FF3B57`
- Display font: **Unifont** (`assets/fonts/unifont.otf`, shipped locally) —
  Minecraft's own fallback face, a pixel font, and the only font available here
  with complete coverage of the Phonetic Extensions and Latin Extended-D glyphs
  the plugin's strings use. The small-caps glyphs are the signature; faking
  them with `text-transform` would be wrong.
- Body font: **DejaVu Sans** — used only where a glyph renders cleaner than
  Unifont's 16px bitmap at large sizes (the ☠ and ☄ in the taunt line).
- Strongest visual element: the duel confirm menu — stacked option cards read
  perfectly in a vertical frame.

## Share copy (draft)
MeowDuels — a modern duels plugin for Paper 1.21. Ranked 1v1s, queues, parties,
FFA with a shrinking border, and arenas that rebuild themselves. Every arena
and kit is set up from in-game menus; nothing is edited by hand.

## Audio direction
- Role: steady rhythmic bed with sparse motion-matched accents
- Music: `happy-beats-business-moves-vol-1-by-ende-dot-app.mp3` — 120.19 BPM.
  Picked for its tempo: 0.4993s beats give a 1.0s every-other-beat cadence that
  matches the reading floor exactly, and its strong cue sits at 17.02s, which
  is why the video is cut to 18.52s.
- Music treatment: in at 0.30 under the typing, up to 0.48 from the confirm
  menu, hold, fade to 0 across 17.9 → 18.52s.
- Music cue guidance: preset read from
  `assets/music/cues/happy-beats-business-moves-vol-1-...music-cues.json`.
  - **Strong cue 17.02s** — locked to the closing tagline under the wordmark.
  - Beat grid: the preset lists 3.02s onward; extrapolated back on the same
    0.4993s period for the opening scenes (0.02, 0.52, 1.02, 1.52, 2.02, 2.52).
  - Sequential text (menu rows, feature cards) lands on **every other beat**
    (~1.0s) so each line clears its reading floor. Countdown digits are single
    glyphs, not prose, so they may sit on consecutive beats.
- Audio-reactive treatment: subtle. Bass band drives a ≤4% breath and a soft
  glow on the wordmark only. No bars, no strobing, no pulsing panels.
- SFX posture: sparse, motion-matched — around eight cues in 18.5 seconds.
- Audio-coupled moments: key ticks while the command types; ENTER; each menu
  row landing; the cursor press on ꜱᴇɴᴅ ʀᴇǫᴜᴇꜱᴛ; the three countdown pips;
  ꜰɪɢʜᴛ; ᴠɪᴄᴛᴏʀʏ; each feature card; the wordmark on the strong cue.
- Restraint rule: no whooshes on text, no riser into the outro, no stacked
  impacts. Every cue is attached to something physically moving.

## Storyboard

### Scene 1 — Type the command — 2.52s (0 → 2.52)
Near-black. A Minecraft chat input bar sits low-centre in the vertical frame.
`/duel GoodBoy` types itself in from 0.52s, roughly one character per 0.06s,
with a blinking caret. At 2.02 the caret stops; at 2.52 the line flashes and
lifts out of frame as if sent.
Sequential/interaction: yes — simulated typing, character by character, then a simulated ENTER.
Audio intent: intimate and quiet; the bed is barely there, the keys carry it.
Audio-coupled idea: soft key tick per character (thinned so it does not machine-gun), one firmer cue on ENTER.
Music: low bed.
Transition mood: clean lift → Scene 2

### Scene 2 — Duel request menu — 3.51s (2.52 → 6.03)
The real `DuelConfirmMenu`, rebuilt as a vertical panel: header
`▏ ᴅᴜᴇʟ ʀᴇǫᴜᴇꜱᴛ › GoodBoy`, then three option rows arriving one per
every-other-beat, each with its icon, label and current value:
- **ᴀʀᴇɴᴀ** › ᴄᴏʟᴏꜱꜱᴇᴜᴍ  (3.02s)
- **ᴋɪᴛ** › ɴᴇᴛʜᴇʀɪᴛᴇ ᴏᴘ  (4.02s)
- **ʀᴏᴜɴᴅꜱ** › ꜰɪʀꜱᴛ ᴛᴏ 3  (5.03s)
Then the green ꜱᴇɴᴅ ʀᴇǫᴜᴇꜱᴛ button at the foot, with a cursor pressing it at 5.53s.
Sequential/interaction: yes — three rows one by one, then a simulated cursor press on the green button.
Audio intent: three clean arrivals then a decisive press; this is the scene that has to read as real software.
Audio-coupled idea: one light interface cue per row; a distinct, louder click on the press.
Transition mood: hard cut on the press → Scene 3

### Scene 3 — Countdown → FIGHT — 2.99s (6.03 → 9.02)
Arena-dark. Gold gradient digits **3 · 2 · 1** on consecutive beats (6.03,
6.52, 7.02) — single glyphs, so beat-speed is legible — each one scaling down
into place. At 7.52 the frame cuts to **ꜰɪɢʜᴛ** in the green gradient, held to
9.02s.
Sequential/interaction: yes — three digits on consecutive beats.
Audio intent: three pips rising in pitch, then one clean hit on ꜰɪɢʜᴛ.
Audio-coupled idea: warm pip per digit; a single medium impact on ꜰɪɢʜᴛ.
Transition mood: hard cut → Scene 4

### Scene 4 — Victory + the taunt — 3.0s (9.02 → 12.02)
**ᴠɪᴄᴛᴏʀʏ** in the green gradient at 9.02, the real score subtitle
`ꜱᴄᴏʀᴇ » 3 - 1` at 9.52, and at 10.52 the plugin's own random match-end
broadcast drops in on a chat row beneath it:
`☄ Nafeh obliterated GoodBoy ☠ • 3-1` — verbatim from `match.toxic.3`. Held to 12.02.
Sequential/interaction: yes — title, then score, then the broadcast line.
Audio intent: the payoff. One bright win cue, then nothing over the taunt — let it read.
Audio-coupled idea: a single announcement cue on ᴠɪᴄᴛᴏʀʏ; the taunt line arrives dry.
Transition mood: soft wipe → Scene 5

### Scene 5 — What else is in it — 3.5s (12.02 → 15.52)
Three feature cards stack up the vertical frame, one per every-other-beat,
all three still on screen at the end:
- ʀᴀɴᴋᴇᴅ ᴅᴜᴇʟꜱ · ǫᴜᴇᴜᴇꜱ · ᴋɪᴛꜱ  (12.02s)
- ᴘᴀʀᴛɪᴇꜱ · ꜰꜰᴀ · ᴛᴇᴀᴍ ꜱᴘʟɪᴛ  (13.01s)
- ᴀʀᴇɴᴀꜱ ᴛʜᴀᴛ ʀᴇʙᴜɪʟᴅ ᴛʜᴇᴍꜱᴇʟᴠᴇꜱ  (14.02s)
Sequential/interaction: yes — three cards one by one, each holding ≥1.0s, full set held 1.5s.
Audio intent: three matched arrivals, same cue each time, so the set reads as one list.
Audio-coupled idea: one card cue per arrival, beat-gridded.
Transition mood: clean → Scene 6

### Scene 6 — Wordmark — 3.0s (15.52 → 18.52)
The **MeowDuels** wordmark rises into the centre at 15.52 in the pink→purple
gradient, breathing very slightly with the bass. At the strong cue **17.02s**
the line **ᴍᴏᴅᴇʀɴ ᴅᴜᴇʟꜱ ꜰᴏʀ ᴘᴀᴘᴇʀ 1.21** snaps in beneath it. Both hold to
18.52 while the music fades. No server name, no URL, no handle.
Sequential/interaction: yes — wordmark, then the tagline on the strong cue.
Audio intent: let the track's own accent do the work; one soft impact at most.
Audio-coupled idea: the tagline rides the 17.02s strong cue.
Transition mood: hold to black

**Music mood for this video:** upbeat, clean, modern
**Audio summary:** A 120 BPM bed runs the whole 18.5 seconds — almost silent under the typing, opening up at the menu, carrying the fight and the feature list on a steady pulse, with about eight motion-matched cues and the track's own strong beat saved for the closing line.
