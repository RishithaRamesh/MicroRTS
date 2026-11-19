# Factory Feud D3 supplementary analysis

All terrain associations use one side-paired observation per map (n=24), not the two swapped matches as independent observations. Pearson coefficients are descriptive; no causal or significance claim is made.

## Terrain associations

| Predictor | Response | Pearson r |
|---|---|---:|
| actual_obstacle_density | adaptive_margin | -0.718 |
| mean_path_length | adaptive_margin | -0.278 |
| route_imbalance | adaptive_margin | -0.369 |
| dead_end_count | adaptive_margin | -0.143 |
| mean_path_length | combined_delivery_throughput | -0.597 |
| route_imbalance | absolute_side_advantage | 0.762 |
| actual_obstacle_density | carrier_hits_received | -0.054 |

Leave-one-map-out ranges for the three headline correlations were density/margin -0.754 to -0.670, path/throughput -0.673 to -0.540, and imbalance/side-advantage 0.525 to 0.802. No single map reverses a headline association, but n=24 remains small. Within-family coefficients are provided in terrain-associations.csv; each uses only eight maps and should be treated as exploratory.

## Player-side check

- MAIN mean adaptive margin as P0/P1: 0.792/0.875 cocoa
- ABLATION mean adaptive margin as P0/P1: 0.625/-0.333 cocoa
- MAIN is nearly side-neutral in aggregate. The non-tactical comparison has a noticeable P0 advantage, despite exact side pairing; this limits interpretation of unpaired ablation summaries but does not invalidate the matched H3 calculation.

## Tactical matched-pair diagnostics

- Matched map/side observations: 48
- Mean tactical cocoa delta: 0.625
- Mean tactical Worker-survival delta: 0.812
- Better survival but lower cocoa: 1 pairs
- More cocoa but lower survival: 1 pairs
- Main-comparison 0–0 draws: 1
- Mean tactical overrides per match by family: OPEN=8.625, OBSTACLE_FIELD=7.312, CHOKEPOINT=4.375
- MAIN W/D/L by family: OPEN=12/4/0; OBSTACLE_FIELD=9/5/2; CHOKEPOINT=8/6/2
- Highest role-switch count: 29 (obstacle-field-008-high-seed-1584007, adaptive side 1, MAIN)
- Its most frequent transitions were DEFENDER→ESCORT=7, COLLECTOR→DEFENDER=6, ESCORT→COLLECTOR=6, ESCORT→DEFENDER=4. The MAIN and ABLATION histories are identical on this map/side, and MAIN recorded 0 tactical overrides, so the repeated Collector/Defender/Escort rotation comes from the shared strategic allocator rather than the tactical layer.
- MAIN matches with an adaptive cocoa-carrier death: 5; the telemetry does not timestamp deaths, so delivery specifically after a Collector loss cannot be inferred accurately.
- The 0–0 case is match 13 on open-006-low-seed-584005: one cocoa left the resource node, none was delivered or carried at the end, and seven Workers died. The telemetry does not establish the exact loss event.
- Better survival/lower delivery case: obstacle-field-003-medium-seed-1584002, adaptive side 0 (cocoa 3→2, survivors 3→4).
- Higher delivery/lower survival case: obstacle-field-005-sparse-seed-1584004, adaptive side 0 (cocoa 1→3, survivors 3→2).

## CHOKEPOINT comparison

- Mean adaptive/fixed cocoa: OPEN=2.750/1.625; OBSTACLE_FIELD=2.250/1.438; CHOKEPOINT=2.875/2.312
- Mean adaptive/fixed Worker deaths: OPEN=1.062/2.062; OBSTACLE_FIELD=1.188/2.062; CHOKEPOINT=0.812/1.188
- CHOKEPOINT lowered the relative margin primarily because FixedRoleAI delivered more there, not because AdaptiveRoleAI delivered less than on OPEN. CHOKEPOINT also had fewer deaths, carrier hits, and tactical overrides. These observations are consistent with less direct engagement, but they do not establish constrained movement as the cause.

Carrier-hit telemetry counts confirmed hits/deaths while carrying cocoa; it is not a complete count of all combat. Route length is the D2 center-to-center terrain proxy, not the exact route traveled by Workers. Family/density comparisons remain confounded because OPEN uses lower density presets.
