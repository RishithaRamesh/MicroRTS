# Factory Feud seed-sensitivity check

## Question and method

This diagnostic checks whether changing the recorded microRTS seed changes the
simulation under the current Factory Feud configuration. It uses:

- `factory-feud-12x12.xml`
- `ThreatAwareAdaptiveRoleAI` versus `FixedRoleAI`
- seeds 584, 585, 586, 587, and 588
- both player-side assignments for every seed
- 500 cycles
- `VERSION_ORIGINAL_FINETUNED` unit types
- cancel-both movement conflict resolution

The comparison ignores only `match_id`, `seed`, `runtime_ms`, and
`simulation_runtime_ms`. Every other CSV field—including scores, survivors,
delivery timing, carrier telemetry, role changes, tactical counts, and example
override events—is compared.

## Actual results

| Seed | ThreatAware side | Cocoa P0–P1 | ThreatAware result | ThreatAware workers alive/dead | Fixed workers alive/dead | Role changes | Tactical attacks | Tactical retreats | Cycles |
|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|
| 584 | P0 | 3–2 | Win | 3/1 | 2/2 | 12 | 2 | 15 | 500 |
| 584 | P1 | 2–3 | Win | 2/2 | 2/2 | 10 | 2 | 15 | 500 |
| 585 | P0 | 3–2 | Win | 3/1 | 2/2 | 12 | 2 | 15 | 500 |
| 585 | P1 | 2–3 | Win | 2/2 | 2/2 | 10 | 2 | 15 | 500 |
| 586 | P0 | 3–2 | Win | 3/1 | 2/2 | 12 | 2 | 15 | 500 |
| 586 | P1 | 2–3 | Win | 2/2 | 2/2 | 10 | 2 | 15 | 500 |
| 587 | P0 | 3–2 | Win | 3/1 | 2/2 | 12 | 2 | 15 | 500 |
| 587 | P1 | 2–3 | Win | 2/2 | 2/2 | 10 | 2 | 15 | 500 |
| 588 | P0 | 3–2 | Win | 3/1 | 2/2 | 12 | 2 | 15 | 500 |
| 588 | P1 | 2–3 | Win | 2/2 | 2/2 | 10 | 2 | 15 | 500 |

Automated comparison result:

- P0 ThreatAware: all five rows have identical non-runtime simulation fields.
- P1 ThreatAware: all five rows have identical non-runtime simulation fields.
- Unique simulation trajectories: two, one for each side assignment.

The raw ten-row diagnostic aggregate is 10 wins, 0 draws, and 0 losses for
ThreatAware, with 30 cocoa delivered versus Fixed's 20, 15 ThreatAware worker
deaths, 20 Fixed worker deaths, 110 role changes, 20 tactical attacks, and 150
tactical retreats. These totals repeat the same two deterministic trajectories
five times and must not be interpreted as ten independent performance samples.

## Why the seed has no effect here

The experiment runner seeds microRTS's public action random generator, but the
current unit table gives Workers fixed one-damage attacks. The engine therefore
does not draw a random damage value. The cancel-both movement-conflict policy is
also deterministic; it does not use the engine's random conflict resolver.
Both project AIs use deterministic tie-breaking. Consequently, changing the
seed records a different experimental label but does not change gameplay.

## Decision and recommendation

The proposed 50-match matrix across five maps and five seeds was not run.
Doing so would produce five copies of each map/side trajectory and would not
provide independent evidence.

A meaningful expansion using the existing maps is a deterministic factorial
evaluation over genuinely different conditions. Keep paired side swaps and run
all five maps at several declared match horizons—for example 250, 500, 750, and
1000 cycles. Match duration changes delivery opportunities and the adaptive
allocator's remaining-time decisions, so each map/side/horizon combination is
a distinct test condition. Report results as coverage of those conditions, not
as randomized trials. Event-level delivery timing and tactical-override timing
from the existing CSV telemetry can further explain why outcomes change.

Alternatively, retain the current 500-cycle results as the complete evaluation
for this fixed deterministic configuration: five distinct maps times two side
assignments. The existing side-swapped map conditions—not seed repetitions—are
the independent sources of environmental variation available without changing
maps or AI behavior.
