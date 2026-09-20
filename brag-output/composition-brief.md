# Hyperframes Composition Brief: MeowDuels

## Objective
Create a short launch-style brag video for MeowDuels, the duels / party / FFA
engine behind the BLOODTHIRST Minecraft server.

## Output
- Composition directory: `brag-output/composition/`
- Rendered video: `brag-output/brag.mp4`
- Format: landscape — 1920x1080
- Duration: 21 seconds

## Source Material
- Project root: `/home/user/vDuels`
- Primary files read: `README.md`, `meowduels/src/main/resources/config.yml`,
  `meowduels/src/main/java/com/meowduels/managers/MessageManager.java`,
  `.../managers/ScoreboardService.java`, `.../model/PartyMode.java`,
  `.../gui/PartyModeMenu.java`, `.../managers/PartyManager.java`,
  `meowduels/tools/verify_kinds.sh`, `docs/PARTY-SPLIT.md`
- Product name: MeowDuels
- Tagline / strongest claim: the duels engine behind BLOODTHIRST — built
  against a hand-generated Paper API stub, with 416 API members verified on
  every build
- Key UI to recreate: the party mode picker menu, the Split countdown with its
  action-bar block meter, the sidebar scoreboard, and a kill-feed line
- **There is no website and no CSS.** All visual identity comes from the
  MiniMessage colour tags and Unicode small-caps strings the plugin renders
  in game. Treat those strings as the product's UI.
- Copy that must appear verbatim (exact glyphs, exact casing):
  - `☠ Nafeh was killed by GoodBoy • 3 left` (from `party.kill-pvp`)
  - `ᴘᴀʀᴛʏ ꜰꜰᴀ` / `ᴇᴠᴇʀʏᴏɴᴇ ꜰᴏʀ ᴛʜᴇᴍꜱᴇʟᴠᴇꜱ, ʟᴀꜱᴛ ᴏɴᴇ ꜱᴛᴀɴᴅɪɴɢ` (`PartyMode.FFA`)
  - `ᴘᴀʀᴛʏ ꜱᴘʟɪᴛ` / `ᴛᴡᴏ ᴛᴇᴀᴍꜱ, ᴀǫᴜᴀ ᴀɢᴀɪɴꜱᴛ ʀᴇᴅ` (`PartyMode.SPLIT`)
  - `ᴘᴀʀᴛʏ ᴅᴜᴇʟꜱ` + `ꜱᴏᴏɴ` (`PartyMode.DUELS`, `isReady() == false`)
  - `ᴛᴇᴀᴍ ᴠꜱ ᴛᴇᴀᴍ • ʏᴏᴜ'ʀᴇ ᴏɴ ᴛᴇᴀᴍ ᴀǫᴜᴀ` (`party.countdown-subtitle-split`)
  - `ꜰɪɢʜᴛ` (`party.countdown-go`)
  - `ʙʟᴏᴏᴅᴛʜɪʀꜱᴛ`, `⚔ ᴋɪʟʟꜱ`, `☠ ᴅᴇᴀᴛʜꜱ`, `⌚ ᴘʟᴀʏᴛɪᴍᴇ` (`scoreboard.global`)
  - `bloodthirstsmp.fun`

## Creative Direction
- Tone preset: `polished`
- Creative direction: a corporate product film for a Minecraft duels plugin,
  where every spec on screen is real
- Interpretation: few scenes, long settled holds, restrained motion, no shake
  and no flash. The comedy is entirely in the treatment meeting the subject —
  nothing on screen may signal that it knows it is funny.
- Angle: shoot a Minecraft PvP plugin like enterprise infrastructure. Real
  in-game UI, real hex values, real specs, corporate stock music. The punchline
  is that every claim is literally true: there really is a build step that
  verifies constant-pool reference kinds, for a plugin that helps people hit
  each other with swords.
- Hook: a real kill-feed line slams onto black in the plugin's exact per-token
  colours. No logo, no setup. `3 left` implies a match already in progress.
- Outro / punchline: three true spec lines, then
  **ꜰᴏʀ ᴀ ᴍɪɴᴇᴄʀᴀꜰᴛ ᴅᴜᴇʟꜱ ᴘʟᴜɢɪɴ.** delivered without a wink, then the wordmark.
- Avoid:
  - Generic SaaS language
  - Abstract filler visuals
  - Redesigning the plugin's look — the small-caps casing and the exact hex
    values ARE the brand. Do not substitute `text-transform: uppercase` for the
    Unicode small caps; the glyphs are the signature.

## Visual Identity
From `config.yml` and the `gui/` sources — these are rendered MiniMessage colours.
- Background: `#0B0C0E`, panel `#15171A`, hairline `#24272C`
- Text: value `#E6E8EB`, label `#8E959D`, muted `#6B7079`
- Accent (brand): `#D90707`
- Accent (party): `#FF8AD0` → `#B04BD6`
- Accent (split/aqua): `#7DE2FF` → `#4B7BFF`
- Accent (go): `#7CFF6B` → `#1FA32F`
- Accent (gold): `#FFD65C` → `#FFB02E`
- Kill feed: skull `#FF3B57`, victim `#FF8A93`, killer `#7CFF6B`
- Display font: **Unifont** (`assets/fonts/unifont.otf`, shipped locally).
  Chosen deliberately: it is Minecraft's own fallback font, it is a pixel face,
  and it is the only font on this machine with complete coverage of the
  Phonetic Extensions and Latin Extended-D glyphs the plugin's strings use
  (verified: zero missing glyphs for the full character set in this video).
- Body font: **DejaVu Sans** (`assets/fonts/DejaVuSans.ttf`) for the spec lines.
- Visual references from the project: sidebar scoreboard, party mode picker,
  Split countdown + action-bar block meter, kill feed.

## Storyboard
Use the storyboard in `brag-output/brag-plan.md` as the creative contract.

Scene summary:
1. Kill feed — 3.0s — `☠ Nafeh was killed by GoodBoy • 3 left`, per-token
   colours, survivor count ticks 3 → 2 on the beat.
2. Wordmark — 3.5s — MeowDuels in the party gradient, tagline
   ᴛʜᴇ ᴅᴜᴇʟꜱ ᴇɴɢɪɴᴇ ʙᴇʜɪɴᴅ ʙʟᴏᴏᴅᴛʜɪʀꜱᴛ, sidebar scoreboard fills row by row.
3. Party mode picker — 4.5s — three real menu slots one at a time, then a
   simulated cursor click on ᴘᴀʀᴛʏ ꜱᴘʟɪᴛ.
4. Split countdown → FIGHT — 4.5s — 3/2/1 in gold, the real Split subtitle,
   the block meter draining twice a second, then ꜰɪɢʜᴛ in green.
5. Spec slam + outro — 5.5s — three spec lines one at a time, held together,
   then the wordmark + `bloodthirstsmp.fun` on the track's strong cue.

## Audio
- Audio role: sparse professional accents over a steady corporate bed
- Audio arc: bed low under the cold open, lifts at the wordmark, stays up
  through the menu and the fight, fades out over the last 0.6s
- Music: `assets/music/happy-beats-business-moves-vol-10-by-ende-dot-app.mp3`
  (109.96 BPM). Chosen *because* it is upbeat corporate stock music — that is
  the tonal gag, and it must be played completely straight.
- Music treatment: `data-automation` volume lane — 0.30 at 0s, 0.46 from 3.5s,
  hold, ramp to 0 across 20.4→21.0s. No ducking for effect.
- Music cue guidance: preset read from
  `~/.claude/skills/brag/assets/music/cues/happy-beats-business-moves-vol-10-by-ende-dot-app.music-cues.json`
  - **Strong cue 20.19s** — locked to the closing wordmark. The video is built
    to 21s specifically so this lands on the logo.
  - Beat grid ~0.545s apart; sequential text uses **every other beat**
    (~1.09s) so each line clears the 0.8s reading floor.
- Audio-reactive treatment: subtle. Per-frame `AUDIO_DATA` from
  `assets/audio-data.json` (extracted with the hyperframes-creative
  `extract-audio-data.py`, 30fps, 16 bands). Bass band drives a small glow and
  ≤4% scale breath on the wordmark only. No waveform bars, no strobing.
- Audio-coupled moments:
  - Kill-feed line arriving — single warm accent
  - Survivor count flip — tiny rollover tick
  - Wordmark set — one soft heavy impact
  - Each menu slot landing — light click, beat-gridded
  - Cursor press on ꜱᴘʟɪᴛ — distinct click, louder
  - Countdown pips 3/2/1 — warm bong, pitch unchanged (the plugin's own rise is
    represented visually by the draining bar)
  - ꜰɪɢʜᴛ — one clean medium impact
  - Each spec line — quiet rollover tick
  - Closing wordmark — one soft heavy impact on the 20.19s strong cue
- SFX selection guidance: all from the brag SFX pack, all "low HF risk" picks
  per `sfx-analysis.md` — `interface/bong_001`, `ui/rollover2`, `ui/click2`,
  `interface/click_003`, `impact/impactSoft_heavy_000/002`,
  `impact/impactSoft_medium_001`.
- SFX analysis guidance: `~/.claude/skills/brag/assets/sfx/sfx-analysis.md`
- Audio files: copied into `brag-output/composition/assets/music`, `/sfx`.

## Local dependency notes (this machine)
- `cdn.jsdelivr.net` is blocked by the environment's network policy, so GSAP is
  vendored at `assets/vendor/gsap.min.js` (npm `gsap@3.14.2`) instead of the
  CDN `<script>` the scaffold ships.
- System `ffmpeg`/`ffprobe` were unavailable; real static builds were installed
  and put on PATH for `check`/`render`.
- Chrome Headless Shell fetched via `hyperframes browser ensure`.

## Hyperframes Instructions
Domain skills loaded: `hyperframes-core` (composition contract, `data-*`
timing, determinism), `hyperframes-animation`, `hyperframes-creative`
(audio-reactive + extraction script), `hyperframes-keyframes`,
`hyperframes-cli`. `/brag` owns product angle, copy and moments; Hyperframes
owns structure, timing mechanics and render.

Requirements:
- Show real UI/copy from the project — four separate surfaces are recreated.
- All text readable in the final render; sequential text on every other beat.
- 21 seconds, inside the 15–25s window.
- Music + SFX present; one strong-cue lock at 20.19s; audio-reactive on the
  wordmark only.
- `npx hyperframes check` must pass before render.
