# Factory Feud Deliverable 3 results

## Reproducibility

- D2 metadata: `results/factory-feud-d2/factory-feud-generated-maps.csv`
- Selection rule: lexicographically first 2 map IDs in every family/preset stratum
- Selected maps: 24
- Simulation seed: base `3584` plus zero-based manifest index; paired sides and both experiments share it
- Match duration: 500 cycles
- Unit table: `ORIGINAL_FINETUNED`; movement conflicts: `cancel-both`
- Completed/expected matches: 96/96; failures: 0

The tactical ablation is controlled: `AdaptiveRoleAI` inherits all strategic allocation from `OldAdaptiveRoleAI` and overrides only role execution with immediate counterattack/retreat logic.

## H1 — overall performance

Main comparison W/D/L: **29/15/4**. Strict win percentage: **60.42%**; draw-adjusted score: 76.04%. Mean/median tactical cocoa: 2.625/3.000; mean/median fixed cocoa: 1.792/2.000; mean margin: 0.833. Threshold 70%: **NOT SUPPORTED**.

## H2 — map complexity

| Family | Main matches | Mean adaptive−fixed cocoa |
|---|---:|---:|
| OPEN | 16 | 1.125 |
| OBSTACLE_FIELD | 16 | 0.813 |
| CHOKEPOINT | 16 | 0.563 |

CHOKEPOINT minus OPEN mean margin: **-0.563 cocoa**. Threshold 2 cocoa: **NOT SUPPORTED**.

These family comparisons are descriptive. OPEN uses systematically lower density presets, so family and obstacle density are confounded; the D2 path length is a center-to-center terrain proxy, not an exact Worker route. The Python analysis aggregates swapped sides to one observation per map before reporting terrain associations.

## H3 — tactical effectiveness

Mean tactical/non-tactical cocoa: **2.625/2.000**. Percentage improvement: **31.25%**. Threshold 15%: **SUPPORTED**. Tactical W/D/L: 29/15/4; non-tactical W/D/L: 20/14/14. Mean surviving Workers: 2.979/2.167; mean deaths: 1.021/1.833; mean role changes: 14.250/12.500. Tactical counterattacks/retreats: 96/229.
