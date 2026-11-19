# Factory Feud D3 gameplay capture selection

These PNGs are native `PhysicalGameStatePanel` renders from deterministic
replays of the audited rows in `../match-results.csv`. No annotations or game
state edits were added. The footer (`T`, P0/P1 unit counts) is produced by the
existing microRTS renderer.

Each image is the state at the **start of the named event cycle**, immediately
before the controllers choose that cycle's actions. This is the exact board
geometry that caused the history event; it does not depict the action's later
completion or imply a Worker death time.

## Recommended gameplay images

| Priority | Match / cycle | Image | What the frame supports |
|---|---:|---|---|
| Primary OPEN | 6 / 180 | `match-006-open-cycle-180.png` | Sparse OPEN topology at the cycle recording both an Escort retreat (Worker 21) and Worker 22's self-defense counterattack. The audited match ended 4–1 with 3/1 surviving Workers. |
| Primary CHOKEPOINT | 66 / 155 | `match-066-chokepoint-cycle-155.png` | Constrained central corridor at Worker 22's Collector retreat. This match ended 4–4 with fewer tactical events and deaths than match 6. |
| Primary tradeoff pair | 50 / 210 | `match-050-main-cycle-210.png` | MAIN state during Worker 23's fourth consecutive Collector retreat (cycles 180, 190, 200, and 210; a fifth follows at 220). MAIN eventually retained four Adaptive Workers but delivered two cocoa. |
| Primary tradeoff pair | 52 / 210 | `match-052-ablation-cycle-210.png` | Same XML map, seed, side, and cycle with tactics disabled. ABLATION eventually retained three Adaptive Workers but delivered three cocoa. Use beside match 50, not alone. |
| Optional only | 61 / 320 | `match-061-role-switch-cycle-320.png` | Exact state preceding three simultaneous strategic role changes. The native board does not draw role labels, so this image alone does **not** demonstrate switching; use the event timeline for that claim. |

Matches 6 and 66 are the strongest video establishing shots. The side-by-side
match 50/52 frames are the strongest report gameplay pair because their map,
seed, side, and cycle are controlled. Match 61 is best presented as a replay
clip with the exported timeline, not as an unexplained still.

## Prioritized experimental figures

Use these as the report's three principal quantitative figures:

1. **H1 — match outcomes:** `../plots/01-outcomes-by-family.png`
2. **H2 — adaptive cocoa margin by family:** `../plots/02-margin-by-family.png`
3. **H3 — tactical ablation:** `../plots/05-tactical-vs-nontactical-cocoa.png`

If the H3 layout remains legible, use `../plots/06-tactical-vs-nontactical-survival.png`
as a second panel beside the cocoa plot. It should support, not replace, the H3
cocoa result.

## Reproduction

From the repository root:

```sh
ant factory-feud-d3-captures
```

The exporter reads the actual XML path, simulation seed, side assignment, and
controller names from `../match-results.csv`. It rejects a capture if the full
replay fails to reproduce the audited score, final Worker counts, role-change
count, counterattack count, or retreat count. Machine-readable capture metadata
is in `capture-manifest.csv`.
