# brag-output-2026-09-20-042632

An 18.5-second **vertical** launch video for MeowDuels — cut for TikTok, Reels
and YouTube Shorts. Generated with the `/brag` Claude Code skill and rendered by
[Hyperframes](https://hyperframes.heygen.com).

This is a separate run from `brag-output/`, which is the landscape cut. That one
is framed around the server the plugin runs on; **this one deliberately names no
server at all.** The only name on screen is MeowDuels.

| File | What it is |
| --- | --- |
| `brag.mp4` | The video. 1080x1920, 30fps, H.264 + AAC, 18.53s. Frame 0 is the poster. |
| `brag.jpg` | The poster as a still, for platforms that take a thumbnail upload. |
| `share-copy.txt` | One caption, postable as-is. |
| `brag-plan.md` | Angle, hook, storyboard, beat plan, palette. |
| `composition-brief.md` | The handoff brief, including the no-server-identity constraint. |
| `composition/index.html` | The composition source. |

## What it shows

The duel flow in order, built entirely from the plugin's own screens:
`/duel GoodBoy` typed into a chat bar → the real `DuelConfirmMenu` assembling
itself (ᴀʀᴇɴᴀ, ᴋɪᴛ, ʀᴏᴜɴᴅꜱ, then a cursor pressing ꜱᴇɴᴅ ʀᴇǫᴜᴇꜱᴛ) → 3·2·1·ꜰɪɢʜᴛ
→ ᴠɪᴄᴛᴏʀʏ with a real `match.toxic` broadcast → three feature cards → wordmark.

Every string is copied verbatim from `MessageManager` and `DuelConfirmMenu`.

## Assets are not committed

`composition/assets/` is gitignored — system fonts, the brag skill's bundled
audio, npm output and derived data, none of it ours to vendor into a plugin
repo. Restore it all:

```bash
cd brag-output-2026-09-20-042632/composition
mkdir -p assets/fonts assets/music assets/sfx assets/vendor

cp /usr/share/fonts/opentype/unifont/unifont.otf assets/fonts/
cp /usr/share/fonts/truetype/dejavu/DejaVuSans{,-Bold}.ttf assets/fonts/

npm i --no-save gsap@3.14.2 && cp node_modules/gsap/dist/gsap.min.js assets/vendor/

B=~/.claude/skills/brag/assets
cp "$B/music/happy-beats-business-moves-vol-1-by-ende-dot-app.mp3" assets/music/
cp "$B"/sfx/interface/{bong_001,click_003}.ogg \
   "$B"/sfx/ui/{rollover2,click2}.ogg \
   "$B"/sfx/impact/impactSoft_heavy_00{0,2}.ogg \
   "$B"/sfx/impact/impactSoft_medium_001.ogg assets/sfx/
cp "$B/sfx/keyboard/keypress-001.wav" assets/sfx/keypress.wav

uv run --with numpy python \
  ~/.claude/skills/hyperframes-creative/scripts/extract-audio-data.py \
  assets/music/happy-beats-business-moves-vol-1-by-ende-dot-app.mp3 \
  --fps 30 --bands 16 -o assets/audio-data.json
# then write assets/audio-data.js from it: {fps,totalFrames,bass[],low[]}, 570 frames
```

## Rebuilding

```bash
cd brag-output-2026-09-20-042632/composition
npx hyperframes check
npx hyperframes render -o ../brag.mp4 --quality delivery
```

`check` passes with 0 errors, 0 layout issues and 29/29 text checks at WCAG AA.
The six `nested_structure_needs_subcomposition` warnings are Studio timeline
ergonomics only — splitting the scenes into separate files would break the
cross-scene timing and the audio-reactive wiring.

## Notes for whoever renders this next

- The runtime is exactly 18.52s so the track's strong beat at **17.02s** lands
  on the closing tagline. Changing the duration breaks that lock.
- GSAP is vendored locally because `cdn.jsdelivr.net` is blocked here.
- Hyperframes needs a real `ffmpeg`/`ffprobe` (Playwright's bundled build is
  VP8/WebM only, with no ffprobe) and its own Chrome: `npx hyperframes browser ensure`.
