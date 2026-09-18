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

MeowDuels also controls *visibility*, which no config can express: a duel's
players and spectators see only each other. That is why a duel tab list is short
- it is not TAB filtering, it is everyone else being hidden for the duration.

**Parties are not hidden.** They tried being, and it was wrong: forming a party
made the rest of the server vanish from your list. A duel hides the server
because the two fighters have nothing to do with it for the next minute; a party
is a group of people standing in the same lobby as everybody else.

Instead a party is a *section*, made by sorting:
`PLACEHOLDER_A_TO_Z:%meowduels_party_sort%` sits first in `sorting-types` and
answers `ZZZZ` for everyone not in a party - so they all tie on that level and
fall through to the rank order exactly as before, while party members answer
`A<leader>` and lift to the top, grouped by party.

Sorting is one order for everyone, so it cannot be "my party first"; TAB sorting
is not relational. The per-viewer view of your own party is the footer roster,
`%meowduels_party_members%`, which marks the leader with a star and, mid-match,
greys out whoever is out.
