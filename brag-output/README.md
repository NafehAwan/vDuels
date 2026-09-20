# brag-output

A 21-second launch video for MeowDuels, generated with the `/brag` Claude Code
skill and rendered by [Hyperframes](https://hyperframes.heygen.com).

| File | What it is |
| --- | --- |
| `brag.mp4` | The video. 1920x1080, 30fps, H.264 + AAC. Its first frame is the poster, so every platform's thumbnail grabber picks it up. |
| `brag.jpg` | The same poster as a still, for platforms that take a custom thumbnail upload. |
| `share-copy.txt` | One caption, postable as-is. |
| `brag-plan.md` | The creative contract: angle, hook, storyboard, beat plan, palette. |
| `composition-brief.md` | The handoff brief - what `/brag` decided vs. what Hyperframes decided. |
| `composition/index.html` | The composition source. One standalone Hyperframes root, one paused GSAP timeline. |

## Why the assets are not committed

`composition/assets/` is gitignored. It is 8.2 MB of files that are either
third-party or reproducible on any machine, and none of them are ours to vendor
into a plugin repository. Everything below restores it.

```bash
cd brag-output/composition
mkdir -p assets/fonts assets/music assets/sfx assets/vendor

# Fonts. Unifont is Minecraft's own fallback face and the only font that covers
# every Phonetic Extensions / Latin Extended-D glyph the plugin's strings use.
cp /usr/share/fonts/opentype/unifont/unifont.otf assets/fonts/
cp /usr/share/fonts/truetype/dejavu/DejaVuSans{,-Bold}.ttf assets/fonts/

# GSAP, from npm rather than the CDN the scaffold ships (see below).
npm i --no-save gsap@3.14.2 && cp node_modules/gsap/dist/gsap.min.js assets/vendor/

# Music and SFX ship with the /brag skill.
B=~/.claude/skills/brag/assets
cp "$B/music/happy-beats-business-moves-vol-10-by-ende-dot-app.mp3" assets/music/
cp "$B"/sfx/interface/bong_001.ogg "$B"/sfx/interface/click_003.ogg \
   "$B"/sfx/ui/rollover2.ogg "$B"/sfx/ui/click2.ogg \
   "$B"/sfx/impact/impactSoft_heavy_00{0,2}.ogg \
   "$B"/sfx/impact/impactSoft_medium_001.ogg assets/sfx/

# Per-frame audio data for the wordmark's audio-reactive glow.
uv run --with numpy python \
  ~/.claude/skills/hyperframes-creative/scripts/extract-audio-data.py \
  assets/music/happy-beats-business-moves-vol-10-by-ende-dot-app.mp3 \
  --fps 30 --bands 16 -o assets/audio-data.json
# then regenerate assets/audio-data.js from it (bass/low/rms arrays, 630 frames)
```

## Rebuilding

```bash
cd brag-output/composition
npx hyperframes check                      # lint + runtime + layout + motion + contrast
npx hyperframes render -o ../brag.mp4 --quality delivery
```

`check` passes with 0 errors and WCAG AA on every text element. It reports 7
`nested_structure_needs_subcomposition` warnings: they ask for each scene to be
split into its own file so Studio's timeline shows one row per scene. That is
display ergonomics only - splitting would break the cross-scene timing and the
audio-reactive wiring, so the single-file form is deliberate.

## Notes for whoever renders this next

- GSAP is vendored locally because `cdn.jsdelivr.net` is blocked on the machine
  this was built on. If your machine can reach the CDN, either form works.
- Hyperframes needs a real `ffmpeg`/`ffprobe` (the one Playwright bundles is
  VP8/WebM only and has no ffprobe) and its own Chrome:
  `npx hyperframes browser ensure`.
- The video is built to exactly 21s so the music's strong beat at 20.19s lands
  on the closing wordmark. Changing the duration breaks that lock.
