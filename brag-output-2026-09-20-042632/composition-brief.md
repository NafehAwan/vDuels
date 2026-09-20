# Hyperframes Composition Brief: MeowDuels

## Objective
A vertical launch video for MeowDuels as a **product** — a modern duels plugin
for Paper 1.21 — cut for TikTok, Reels and YouTube Shorts.

## Output
- Composition directory: `brag-output-2026-09-20-042632/composition/`
- Rendered video: `brag-output-2026-09-20-042632/brag.mp4`
- Format: vertical — 1080x1920
- Duration: 18.52 seconds

## Source Material
- Project root: `/home/user/vDuels`
- Primary files read: `meowduels/src/main/java/com/meowduels/managers/MessageManager.java`
  (`duel.*`, `request.*`, `titles.*`, `match.toxic.*`),
  `.../gui/DuelConfirmMenu.java`, `.../model/PartyMode.java`,
  `meowduels/src/main/resources/config.yml`, `README.md`
- Product name: MeowDuels
- Strongest claim: a duels plugin where arenas, kits, queues, parties and FFA
  events are all configured from in-game menus, and arenas rebuild themselves
  after every fight
- Key UI to recreate: the `DuelConfirmMenu` (header, ᴀʀᴇɴᴀ / ᴋɪᴛ / ʀᴏᴜɴᴅꜱ rows,
  green ꜱᴇɴᴅ ʀᴇǫᴜᴇꜱᴛ button), the countdown and ꜰɪɢʜᴛ titles, the ᴠɪᴄᴛᴏʀʏ title
  with its score subtitle, and a real match-end broadcast line
- **There is no website and no CSS.** The MiniMessage colour tags and the
  Unicode small-caps strings the plugin renders in game are the visual identity.
- Copy that must appear verbatim (exact glyphs, exact casing):
  - `ᴅᴜᴇʟ ʀᴇǫᴜᴇꜱᴛ › GoodBoy` — `DuelConfirmMenu` title
  - `ᴀʀᴇɴᴀ`, `ᴋɪᴛ`, `ʀᴏᴜɴᴅꜱ`, `ꜱᴇɴᴅ ʀᴇǫᴜᴇꜱᴛ` — its slot names
  - `ꜰɪɢʜᴛ` — the go signal
  - `ᴠɪᴄᴛᴏʀʏ` and `ꜱᴄᴏʀᴇ » 3 - 1` — `titles.round-won.*`
  - `☄ Nafeh obliterated GoodBoy ☠ • 3-1` — `match.toxic.3`, verbatim

## Hard constraint
The server this plugin is developed on is **never named, shown or implied**.
No server name, no IP, no scoreboard branding, no handle. The only name on
screen is MeowDuels. This rules out the sidebar scoreboard, whose title and
footer both carry server branding.

## Creative Direction
- Tone preset: `app-store`
- Creative direction: a modern plugin launch cut for vertical short-form —
  clean feature reveals that land hard, no fluff, no irony
- Interpretation: smooth slides, no shake, no flash. Big centre-weighted type
  because it is watched on a phone. Energy comes from cut rhythm, not motion
  clutter — this sells a tool to server owners, so it has to look competent
  before it looks exciting.
- Angle: the whole duel flow in order — type the command, configure the match,
  fight it, win it, get taunted by the plugin — then three cards for the rest.
- Hook: `/duel GoodBoy` typing itself into a chat bar with key ticks.
- Outro / punchline: **ᴍᴏᴅᴇʀɴ ᴅᴜᴇʟꜱ ꜰᴏʀ ᴘᴀᴘᴇʀ 1.21** under the wordmark.
- Avoid:
  - Generic SaaS language
  - Abstract filler visuals
  - Any server identity
  - Substituting `text-transform: uppercase` for the real small-caps glyphs

## Visual Identity
- Background `#0B0C0E`, panel `#15171A`, inset `#101215`, hairline `#24272C`
- Text: value `#E6E8EB`, label `#8E959D`, muted `#6B7079`
- Wordmark gradient `#FF8AD0` → `#B04BD6`
- Confirm / go / win gradient `#7CFF6B` → `#1FA32F`
- Countdown gradient `#FFD65C` → `#FFB02E`
- Secondary gradient `#7DE2FF` → `#4B7BFF`
- Loss `#FF8A93`
- Display font: **Unifont** (`assets/fonts/unifont.otf`) — Minecraft's own
  fallback face and the only font available here with complete coverage of the
  Phonetic Extensions and Latin Extended-D glyphs in use. Verified: zero
  missing glyphs across this video's full character set.
- Body font: **DejaVu Sans**, used only for `☄` and `☠`, which are cleaner than
  Unifont's 16px bitmaps at display size.

## Storyboard
`brag-plan.md` is the creative contract.

1. Type the command — 2.52s — `/duel GoodBoy` types in, then ENTER.
2. Duel request menu — 3.51s — three option rows one at a time, then a cursor
   pressing ꜱᴇɴᴅ ʀᴇǫᴜᴇꜱᴛ.
3. Countdown → FIGHT — 2.99s — 3 · 2 · 1 on consecutive beats, then ꜰɪɢʜᴛ.
4. Victory + the taunt — 3.0s — ᴠɪᴄᴛᴏʀʏ, the score, then the real broadcast.
5. Feature cards — 3.5s — three cards, one per every-other-beat, held together.
6. Wordmark — 3.0s — MeowDuels, then the tagline on the strong cue.

A small `ᴍᴇᴏᴡᴅᴜᴇʟꜱ` mark sits at the top of the frame for scenes 1-5 and ends
where the real wordmark begins, so the brand is present for a viewer who
scrolls away at second four.

## Audio
- Audio role: steady rhythmic bed with sparse motion-matched accents
- Audio arc: almost silent under the typing, opens at the menu, carries the
  fight and the feature list, fades out over the last 0.9s
- Music: `assets/music/happy-beats-business-moves-vol-1-by-ende-dot-app.mp3`,
  120.19 BPM — chosen for its tempo (0.4993s beats give an exact 1.0s
  every-other-beat cadence) and for its strong cue at 17.02s
- Music treatment: `data-automation` volume lane — 0.30 to 2.5s, 0.48 from
  3.0s, ramp to 0 across 17.6 → 18.52s
- Music cue guidance: preset cue file for this track.
  - **Strong cue 17.02s** locked to the closing tagline; the 18.52s runtime
    exists so that beat lands there.
  - The preset's grid starts at 3.02s; extrapolated back on the same 0.4993s
    period for the opening scenes.
  - Sequential text on every other beat (~1.0s). Countdown digits are single
    glyphs, not prose, so they sit on consecutive beats.
- Audio-reactive treatment: subtle — bass drives ≤4% scale and a soft glow on
  the wordmark only, per-frame from `assets/audio-data.js`.
- Audio-coupled moments: key ticks (thinned to five) while typing; ENTER; each
  menu row; the cursor press; three countdown pips; ꜰɪɢʜᴛ; ᴠɪᴄᴛᴏʀʏ; each
  feature card; the wordmark; the tagline on the strong cue.
- SFX guidance: all low-HF-risk picks per the brag pack's `sfx-analysis.md` —
  `keyboard/keypress-001`, `interface/click_003`, `ui/click2`, `ui/rollover2`,
  `interface/bong_001`, `impact/impactSoft_medium_001`,
  `impact/impactSoft_heavy_000/002`.

## Local dependency notes (this machine)
- GSAP vendored at `assets/vendor/gsap.min.js`; `cdn.jsdelivr.net` is blocked.
- `ffmpeg`/`ffprobe` static builds on PATH; Chrome via `hyperframes browser ensure`.

## Result
`npx hyperframes check` passes: 0 errors, 0 layout issues across 9 samples,
29/29 text checks at WCAG AA. Six `nested_structure_needs_subcomposition`
warnings remain — Studio timeline ergonomics only; splitting the scenes into
separate files would break the cross-scene timing and the audio-reactive
wiring, so the single-file form is deliberate.
