# Factory Feud D3 unusual-match analysis

## Scope and evidence

This analysis uses `match-results.csv`, `selection-manifest.csv`, the D3 summary
and terrain analysis, and the exported `role_change_history` and
`tactical_override_history` columns. The histories are embedded in the match CSV;
they are not separate log files.

The histories record role assignments and tactical commands, but not Worker death
cycles, exact movement paths, or complete per-cycle game states. Interpretations
below distinguish direct observations from plausible explanations.

## Compact comparison

| Case | Match | Map | Side / comparison | Cocoa (adaptive–fixed) | Outcome | Workers alive (adaptive/fixed) | Roles | Attacks / retreats |
|---|---:|---|---|---:|---|---:|---:|---:|
| 0–0 draw | 13 | `open-006-low-seed-584005` | P1 Adaptive vs Fixed | 0–0 | Draw | 2 / 1 | 17 | 4 / 30 |
| Maximum switching | 61 | `obstacle-field-008-high-seed-1584007` | P1 Adaptive vs Fixed | 4–0 | Win | 4 / 4 | 29 | 0 / 0 |
| Survival↑, cocoa↓ | 50 | `obstacle-field-003-medium-seed-1584002` | P0 Adaptive vs Fixed | 2–0 | Win | 4 / 2 | 17 | 2 / 7 |
| Matched tactical-off | 52 | same map/side | P0 OldAdaptive vs Fixed | 3–0 | Win | 3 / 2 | 14 | 0 / 0 |
| Cocoa↑, survival↓ | 38 | `obstacle-field-005-sparse-seed-1584004` | P0 Adaptive vs Fixed | 3–0 | Win | 2 / 1 | 11 | 3 / 11 |
| Matched tactical-off | 40 | same map/side | P0 OldAdaptive vs Fixed | 1–1 | Draw | 3 / 1 | 8 | 0 / 0 |
| Representative OPEN | 6 | `open-005-sparse-seed-584004` | P0 Adaptive vs Fixed | 4–1 | Win | 3 / 1 | 20 | 3 / 5 |
| Representative CHOKEPOINT | 66 | `chokepoint-001-sparse-seed-2584000` | P0 Adaptive vs Fixed | 4–4 | Draw | 4 / 3 | 21 | 1 / 3 |
| Long-route CHOKEPOINT | 77 | `chokepoint-006-low-seed-2584005` | P1 Adaptive vs Fixed | 0–2 | Loss | 2 / 2 | 7 | 2 / 0 |

Worker survival is final-state survival. Death timing is not recorded.

## Case 1: MAIN 0–0 draw

### Match and map

- Match ID: **13**
- Map: `open-006-low-seed-584005` (`OPEN`, `LOW`)
- Adaptive side: P1
- Assignment: P0 `FixedRoleAI`, P1 `AdaptiveRoleAI`
- Terrain: density 0.078125, mean terrain-proxy path 8.5 cells, route
  imbalance 1, two dead ends
- Result: 0–0 draw
- Final Workers: Adaptive 2/4; Fixed 1/4
- Final resource node: 119 cocoa
- End-of-match carried cocoa: 0 for both teams
- Adaptive events: 17 role changes, 4 counterattacks, 30 retreats
- Delivery telemetry: zero delivery events; no first or last delivery cycle

### Chronology

- Cycles 30–60: Workers 26 and 27 exchange Attacker/Escort duties; at cycle
  60 Worker 24 performs a self-defense counterattack.
- Cycles 90–180: collector assignments move among Workers 25, 26, and 27.
  Numerous retreats occur while those Workers are Collectors or Escorts.
- Cycles 140, 170, and 220: three additional self-defense counterattacks are
  recorded.
- Cycles 195 and 235: emergency factory-threat logic moves a Collector to
  Defender.
- Cycles 245–485: Worker 27 receives repeated Collector retreat overrides,
  including repeated commands toward position `12:5` at 20-cycle intervals.
- Cycle 500: neither team has delivered; three Fixed and two Adaptive Workers
  have died.

### Evidence-based interpretation

Confirmed: combat pressure and repeated tactical avoidance coincided with a
complete delivery stall. Exactly one cocoa was removed from the resource node,
but it was neither delivered nor carried by a surviving Worker at the end. The
history also confirms extensive Collector/Defender reassignment and 30 tactical
retreat commands.

Not established: the logs do not identify the exact cycle or mechanism by which
the missing cocoa ceased to be represented. In particular, carrier-death
telemetry does not timestamp deaths and can miss a harvest-and-death transition
within one state-difference interval. Therefore, the data support “harvested but
not delivered,” not a more specific causal claim.

## Case 2: 29 role changes

### Match and map

- Match ID: **61**
- Map: `obstacle-field-008-high-seed-1584007`
  (`OBSTACLE_FIELD`, `HIGH`)
- Adaptive side: P1
- Assignment: P0 `FixedRoleAI`, P1 `AdaptiveRoleAI`
- Terrain: density 0.21875, mean path 12.5, route imbalance 13, three dead ends
- Result: Adaptive 4–0 win
- Final Workers: 4/4 for each team
- Deliveries: four Adaptive events, first at cycle 130 and last at 430;
  mean interval 100 cycles
- Tactical events: none

The initial P1 roles were Worker 24 Collector, 25 Defender, 26 Attacker, and
27 Escort. All 29 changes are reconstructed below.

| Cycle | Role changes |
|---:|---|
| 30 | W26 Attacker→Escort; W27 Escort→Attacker |
| 130 | W24 Collector→Defender; W25 Defender→Escort; W26 Escort→Collector |
| 190 | W25 Escort→Collector; W26 Collector→Escort |
| 230 | W25 Collector→Escort; W26 Escort→Collector |
| 250 | W24 Defender→Escort |
| 260 | W25 Escort→Collector; W26 Collector→Defender |
| 290 | W24 Escort→Defender; W26 Defender→Escort |
| 320 | W24 Defender→Escort; W25 Collector→Defender; W26 Escort→Collector |
| 350 | W25 Defender→Collector; W26 Collector→Defender |
| 390 | W24 Escort→Defender; W26 Defender→Escort |
| 420 | W24 Defender→Escort; W26 Escort→Defender |
| 430 | W25 Collector→Defender |
| 440 | W24 Escort→Collector |
| 450 | W26 Defender→Escort |
| 460 | W25 Defender→Collector |
| 470 | W24 Collector→Defender |
| 480 | W26 Escort→Defender |

The most frequent transitions were Defender→Escort (7), Collector→Defender
(6), Escort→Collector (6), and Escort→Defender (4). Match 63—the identical map,
side, and seed with `OldAdaptiveRoleAI`—has the same score, delivery timing, and
all 29 role changes. Match 61 recorded zero tactical overrides.

Confirmed: the allocator repeatedly rotated Workers 24–26 among Collector,
Defender, and Escort responsibilities even though no Worker died. This is
repeated switching between competing collection, factory coverage, and collector
support objectives. The 25-cycle cooldown prevents cycle-to-cycle oscillation,
but does not prevent repeated longer-period rotations over 500 cycles.

Possible explanation: the long, highly imbalanced route changes which live
Worker is closest to the cocoa, factory, and current Collector as units move.
That can change the utility-based assignment at successive reevaluations. The
histories establish the switching pattern, but not the precise pathfinding state
that caused each proximity ranking.

## Case 3: tactics preserved a Worker but reduced delivery

### Matched configuration

- Map: `obstacle-field-003-medium-seed-1584002`
  (`OBSTACLE_FIELD`, `MEDIUM`), Adaptive side P0
- Terrain: density 0.182292, mean path 8.5, route imbalance 3, two dead ends
- MAIN match 50: `AdaptiveRoleAI` won 2–0, four Adaptive Workers survived,
  17 role changes, two attacks, seven retreats
- ABLATION match 52: `OldAdaptiveRoleAI` won 3–0, three Adaptive Workers
  survived, 14 role changes, no tactical events

### Chronology

- MAIN cycle 80: Worker 20 counterattacks in self-defense while Worker 22
  retreats as Escort.
- MAIN cycles 100–180: Escort/Attacker responsibilities rotate among Workers
  21–23; at 180 Worker 20 changes Collector→Defender and Worker 23 changes
  Attacker→Collector.
- MAIN cycle 175: first cocoa delivery. From cycles 180–220 Worker 23 receives
  five Collector retreat commands.
- MAIN cycle 230: Worker 22 counterattacks and Worker 23 receives a sixth retreat.
- MAIN cycle 385: second and final delivery. One cocoa remains carried at the
  end.
- ABLATION cycle 150: first delivery and a three-Worker role rotation assigns
  Worker 23 as Collector. It completes three deliveries by cycle 420, with a
  135-cycle mean interval. One Adaptive Worker is dead at the end.

Confirmed: the tactical controller finished with one additional Worker but one
fewer delivered cocoa. Its first delivery was 25 cycles later, it completed only
two events versus three, and its mean delivery interval was 210 versus 135
cycles. The seven retreat overrides concentrated around a Collector between
cycles 180 and 230.

Possible explanation: the retreats plausibly exchanged collection progress for
safety, which is consistent with the survival/delivery tradeoff. Death cycles and
exact movement paths are not recorded, so the extra surviving Worker cannot be
causally attributed to any specific retreat.

## Case 4: tactics increased delivery despite lower survival

### Matched configuration

- Map: `obstacle-field-005-sparse-seed-1584004`
  (`OBSTACLE_FIELD`, `SPARSE`), Adaptive side P0
- Terrain: density 0.098958, mean path 8.5, route imbalance 3, no dead ends
- MAIN match 38: `AdaptiveRoleAI` won 3–0, two Adaptive Workers survived,
  11 role changes, three attacks, 11 retreats
- ABLATION match 40: `OldAdaptiveRoleAI` drew 1–1, three Adaptive Workers
  survived, eight role changes, no tactical events

### Chronology

- MAIN cycle 80: Worker 20 counterattacks; Worker 22 retreats. Retreats continue
  for Escorts through cycles 100–170, reaching 11 retreat events.
- MAIN cycles 130–185: collector support/defense roles rotate among all four
  Workers; at 180 Worker 20 changes Collector→Defender for an emergency factory
  threat, and at 185 Worker 22 becomes Collector.
- MAIN cycle 175: first delivery.
- MAIN cycles 180 and 190: two more self-defense counterattacks. Deliveries
  continue through cycle 440, reaching three.
- ABLATION cycles 120–150: Worker 22 changes Escort→Attacker, followed by a
  four-Worker rotation at 150 that makes Worker 22 the Collector. Cycle 150 is
  its only delivery. It ends with one cocoa still carried.

Confirmed: MAIN delivered two more cocoa and changed the outcome from draw to
win, despite losing one more Worker. MAIN recorded three completed deliveries
from cycles 175–440; ABLATION recorded only one at cycle 150. MAIN also killed
three Fixed Workers, leaving only one Fixed survivor in both matched games.

Possible explanation: tactical engagements and role changes may have created
collection opportunities after early combat, but the exported histories do not
link a particular attack to a later delivery. The data demonstrate the tradeoff,
not the causal chain.

## Case 5: representative OPEN versus CHOKEPOINT behavior

Across all MAIN matches, mean adaptive-minus-fixed margins were +1.125 on OPEN
and +0.563 on CHOKEPOINT. Mean tactical overrides were 8.625 versus 4.375.
Adaptive delivery itself was similar (OPEN 2.750, CHOKEPOINT 2.875); the smaller
CHOKEPOINT margin primarily came from higher Fixed delivery (1.625 versus
2.312). CHOKEPOINT also had fewer Adaptive/Fixed deaths (0.812/1.188 versus
1.062/2.062 on OPEN).

### OPEN representative: match 6

- Map: `open-005-sparse-seed-584004`, P0 Adaptive
- Density 0.052083; mean path 7.5; route imbalance 1; no dead ends
- Adaptive won 4–1; final Workers 3–1
- 20 role changes; three counterattacks; five retreats
- First/last deliveries: cycles 130/490
- Key sequence: Escort/Attacker changes at cycles 30–90; four retreat commands
  at cycles 70–80; first delivery and a new Collector at 130; counterattacks at
  180, 225, and 230; collection/defense rotations continue through 490.

### CHOKEPOINT representative: match 66

- Map: `chokepoint-001-sparse-seed-2584000`, P0 Adaptive
- Density 0.098958; mean path 7.5; route imbalance 3; no dead ends
- 4–4 draw; final Workers 4–3
- 21 role changes; one counterattack; three retreats
- First/last deliveries: cycles 210/475
- Key sequence: early role changes at 30–120; one counterattack at 80; an extra
  Collector is assigned while behind at 110; retreats occur at 155, 280, and
  300; both strategies finish with four deliveries.

These two maps have the same mean path proxy and no dead ends, but the
CHOKEPOINT map has almost twice the obstacle density and greater route imbalance.
The OPEN match produced twice as many tactical interventions, more deaths, and a
three-cocoa adaptive margin; the CHOKEPOINT match produced fewer interventions,
fewer deaths, and no margin.

Match 77 supplies a more difficult CHOKEPOINT example: on
`chokepoint-006-low-seed-2584005`, mean path length is 12.5 and route imbalance
11. Adaptive P1 lost 0–2, completed no deliveries, ended with one carried cocoa,
and recorded only two counterattacks and no retreats. This is consistent with
the broader observation that high CHOKEPOINT complexity can reduce direct
tactical interaction and delivery throughput.

Confirmed: selected CHOKEPOINT matches have fewer recorded interventions and
deaths, and FixedRoleAI is more competitive there. Possible explanation:
barriers and constrained routes may reduce opportunities for the local
one-move threat override to activate. The event histories do not record blocked
movement attempts or counterfactual paths, so constrained movement is a
plausible mechanism rather than a demonstrated cause.

## Recommended report and video cases

1. **Match 61 (29 role changes)** is the strongest final-report example. It
   directly demonstrates recurring strategic reassignment, has a complete
   auditable timeline, and the identical tactical-off history isolates the
   behavior to the shared strategic allocator.
2. **Matches 50 and 52 (survival versus delivery)** are the strongest matched
   tactical example. They show the intended tradeoff cleanly: +1 survivor but
   −1 cocoa with seven recorded retreats and later/slower deliveries.

For video, match 6 versus match 66 is a useful visual OPEN/CHOKEPOINT contrast.
The 0–0 match is visually striking, but its exact cocoa-loss event is not fully
instrumented, so it should be presented only with that limitation.
