# Factory Feud Deliverable 3 results

## Reproducibility

- D2 metadata: `results/factory-feud-d2/factory-feud-generated-maps.csv`
- Selection rule: lexicographically first 1 map IDs in every family/preset stratum
- Selected maps: 12
- Simulation seed: base `3584` plus zero-based manifest index; paired sides and both experiments share it
- Match duration: 25 cycles
- Unit table: `ORIGINAL_FINETUNED`; movement conflicts: `cancel-both`
- Completed/expected matches: 48/48; failures: 0

The tactical ablation is controlled: `AdaptiveRoleAI` inherits all strategic allocation from `OldAdaptiveRoleAI` and overrides only role execution with immediate counterattack/retreat logic.

## H1 — overall performance

Main comparison W/D/L: **0/24/0**. Strict win percentage: **0.00%**; draw-adjusted score: 50.00%. Mean/median tactical cocoa: 0.000/0.000; mean/median fixed cocoa: 0.000/0.000; mean margin: 0.000. Threshold 70%: **NOT SUPPORTED**.

## H2 — map complexity

| Family | Main matches | Mean adaptive−fixed cocoa |
|---|---:|---:|
| OPEN | 8 | 0.000 |
| OBSTACLE_FIELD | 8 | 0.000 |
| CHOKEPOINT | 8 | 0.000 |

CHOKEPOINT minus OPEN mean margin: **0.000 cocoa**. Threshold 2 cocoa: **NOT SUPPORTED**.

These family comparisons are descriptive. OPEN uses systematically lower density presets, so family and obstacle density are confounded; the D2 path length is a center-to-center terrain proxy, not an exact Worker route. The Python analysis aggregates swapped sides to one observation per map before reporting terrain associations.

## H3 — tactical effectiveness

Mean tactical/non-tactical cocoa: **0.000/0.000**. Percentage improvement: **undefined (zero non-tactical mean)**. Threshold 15%: **NOT SUPPORTED**. Tactical W/D/L: 0/24/0; non-tactical W/D/L: 0/24/0. Mean surviving Workers: 4.000/4.000; mean deaths: 0.000/0.000; mean role changes: 0.000/0.000. Tactical counterattacks/retreats: 0/0.
