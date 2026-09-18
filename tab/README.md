# tab/

The TAB plugin's side of the tab list, kept here so it stays in step with the
MeowDuels placeholders it depends on.

| file | goes to | what it does |
|---|---|---|
| `groups.yml` | `plugins/TAB/groups.yml` | the name rows: rank prefix, tag/marker suffix, nametag |
| `config.yml` | `plugins/TAB/config.yml` | everything else, including the three header/footer designs |

`/tab reload` after either.

## Who draws what

TAB draws the tab list. MeowDuels cannot replace it per-player, so it answers
placeholders instead and TAB asks them once per refresh:

- **Header and footer** are three TAB *designs* - `fight`, `party`, `default` -
  each with a `display-condition`, filled in from ordinary MeowDuels
  placeholders. **Order matters**: TAB takes the first design whose condition
  matches, and `default` matches nearly everything, so it stays last.

  There is a second way to do the same thing, if you would rather keep the
  wording out of TAB's config: `%meowduels_tab_header_N%` /
  `%meowduels_tab_footer_N%` answer one line each and MeowDuels picks the
  layout - `global`, `duel` or `party` - from its own `config.yml` under `tab:`.
  Those layouts also drive the built-in header/footer when `external-tab` is
  off, so they are worth keeping correct either way. Use one approach or the
  other, not both.
- **Name rows** come from `%rel_meowduels_tabprefix%` / `...tabsuffix%`. The
  `rel_` matters: it hands MeowDuels the viewer as well as the target, which is
  what lets the duel bolt and the party star show only to the people in that
  duel or party. Everyone else sees an ordinary rank.

## Why the values are section codes

A value handed through PlaceholderAPI is not parsed again by whatever receives
it. A rank stored as `<gradient:...>` arrives at TAB as that literal text.
MeowDuels flattens everything to `§` codes first, which always render.

## Who is in whose list

MeowDuels controls *visibility*, which no config can express. A duel's players
and spectators see only each other; a party sees only the party. That is why a
duel or party tab list is short - it is not TAB filtering, it is everyone else
being hidden from those players for the duration.

It is **one-directional**, and that is the whole trick: the rest of the server
still sees the duellists and the party, with their real ranks, because they are
online and should look it. Hiding both ways is what makes people vanish from
everybody's list.

A party of one is never bubbled. There is nobody to share a list with, so the
bubble would empty the list instead of filtering it.

`%meowduels_party_sort%` exists but is not used here. It is for the other
arrangement - a party *section* in the shared list rather than a separate list.
Put `PLACEHOLDER_A_TO_Z:%meowduels_party_sort%` first in `sorting-types` and
non-party players all tie on it, falling through to the rank order below
unchanged, while party members lift to the top grouped by leader.
