# Factory Feud controlled experiment — seed 584

These are 20 deterministic, single-seed match outcomes, not independent randomized trials. Every match used 500 cycles, microRTS ORIGINAL_FINETUNED unit types, seed 584, and cancel-both movement conflict resolution. Each map/strategy pairing was side-swapped.

## Map validation and route audit

Distances are terrain-only shortest movement steps. Worker arrays are ordered by unit ID; return distance is the shortest cocoa-adjacent cell to factory-adjacent cell. Temporary unit occupancy is ignored.

| Map | Size | Cocoa | P0 worker→cocoa | P1 worker→cocoa | P0 return | P1 return | Geometric symmetry |
|---|---:|---:|---|---|---:|---:|---|
| factory-feud-12x12.xml | 12×12 | 120 @ 5,6 | [7, 7, 6, 6] | [7, 7, 6, 6] | 7 | 7 | no |
| factory-feud-contested-center-12x12.xml | 12×12 | 120 @ 5,5 | [2, 3, 3, 4] | [3, 4, 4, 5] | 2 | 3 | no |
| factory-feud-exposed-routes-14x12.xml | 14×12 | 120 @ 7,5 | [4, 5, 5, 6] | [4, 5, 3, 6] | 4 | 4 | no |
| factory-feud-narrow-passage-12x12.xml | 12×12 | 120 @ 5,5 | [2, 3, 3, 4] | [3, 4, 4, 5] | 2 | 3 | no |
| factory-feud-narrow-passage-refined-12x12.xml | 12×12 | 120 @ 5,5 | [2, 3, 3, 4] | [4, 5, 3, 6] | 2 | 4 | no |

## Match results

| ID | Map | Strategy side | Cocoa P0–P1 | Result | Strategy workers alive/dead | Fixed workers alive/dead | Role changes | Overrides A/R | Cycles | Runtime ms |
|---:|---|---|---:|---|---:|---:|---:|---:|---:|---:|
| 1 | factory-feud-12x12.xml | Adaptive P0 | 2–2 | D | 1/3 | 3/1 | 5 | 0/0 | 500 | 27 |
| 2 | factory-feud-12x12.xml | Adaptive P1 | 2–3 | W | 2/2 | 1/3 | 8 | 0/0 | 500 | 8 |
| 3 | factory-feud-12x12.xml | ThreatAware P0 | 3–2 | W | 3/1 | 2/2 | 12 | 2/15 | 500 | 7 |
| 4 | factory-feud-12x12.xml | ThreatAware P1 | 2–3 | W | 2/2 | 2/2 | 10 | 2/15 | 500 | 3 |
| 5 | factory-feud-contested-center-12x12.xml | Adaptive P0 | 7–5 | W | 2/2 | 2/2 | 1 | 0/0 | 500 | 2 |
| 6 | factory-feud-contested-center-12x12.xml | Adaptive P1 | 7–6 | L | 2/2 | 1/3 | 12 | 0/0 | 500 | 2 |
| 7 | factory-feud-contested-center-12x12.xml | ThreatAware P0 | 6–5 | W | 2/2 | 2/2 | 1 | 2/4 | 500 | 2 |
| 8 | factory-feud-contested-center-12x12.xml | ThreatAware P1 | 5–5 | D | 3/1 | 1/3 | 15 | 3/6 | 500 | 3 |
| 9 | factory-feud-exposed-routes-14x12.xml | Adaptive P0 | 4–0 | W | 3/1 | 1/3 | 10 | 0/0 | 500 | 2 |
| 10 | factory-feud-exposed-routes-14x12.xml | Adaptive P1 | 4–4 | D | 3/1 | 2/2 | 15 | 0/0 | 500 | 3 |
| 11 | factory-feud-exposed-routes-14x12.xml | ThreatAware P0 | 4–0 | W | 2/2 | 1/3 | 1 | 4/6 | 500 | 2 |
| 12 | factory-feud-exposed-routes-14x12.xml | ThreatAware P1 | 3–4 | W | 3/1 | 2/2 | 13 | 2/6 | 500 | 3 |
| 13 | factory-feud-narrow-passage-12x12.xml | Adaptive P0 | 6–5 | W | 2/2 | 2/2 | 7 | 0/0 | 500 | 3 |
| 14 | factory-feud-narrow-passage-12x12.xml | Adaptive P1 | 5–5 | D | 2/2 | 2/2 | 9 | 0/0 | 500 | 5 |
| 15 | factory-feud-narrow-passage-12x12.xml | ThreatAware P0 | 6–5 | W | 2/2 | 2/2 | 5 | 2/3 | 500 | 3 |
| 16 | factory-feud-narrow-passage-12x12.xml | ThreatAware P1 | 5–5 | D | 3/1 | 2/2 | 15 | 2/7 | 500 | 4 |
| 17 | factory-feud-narrow-passage-refined-12x12.xml | Adaptive P0 | 6–0 | W | 3/1 | 2/2 | 16 | 0/0 | 500 | 5 |
| 18 | factory-feud-narrow-passage-refined-12x12.xml | Adaptive P1 | 2–3 | W | 1/3 | 3/1 | 6 | 0/0 | 500 | 4 |
| 19 | factory-feud-narrow-passage-refined-12x12.xml | ThreatAware P0 | 5–0 | W | 3/1 | 2/2 | 22 | 3/7 | 500 | 4 |
| 20 | factory-feud-narrow-passage-refined-12x12.xml | ThreatAware P1 | 2–4 | W | 2/2 | 1/3 | 10 | 4/9 | 500 | 3 |

## Summary by map and strategy

| Map | Strategy | Avg strategy cocoa | Avg Fixed cocoa | W/D/L | Worker deaths | Role changes | Overrides A/R |
|---|---|---:|---:|---|---:|---:|---:|
| factory-feud-12x12.xml | Adaptive | 2.50 | 2.00 | 1/1/0 | 5 | 13 | 0/0 |
| factory-feud-12x12.xml | ThreatAware | 3.00 | 2.00 | 2/0/0 | 3 | 22 | 4/30 |
| factory-feud-contested-center-12x12.xml | Adaptive | 6.50 | 6.00 | 1/0/1 | 4 | 13 | 0/0 |
| factory-feud-contested-center-12x12.xml | ThreatAware | 5.50 | 5.00 | 1/1/0 | 3 | 16 | 5/10 |
| factory-feud-exposed-routes-14x12.xml | Adaptive | 4.00 | 2.00 | 1/1/0 | 2 | 25 | 0/0 |
| factory-feud-exposed-routes-14x12.xml | ThreatAware | 4.00 | 1.50 | 2/0/0 | 3 | 14 | 6/12 |
| factory-feud-narrow-passage-12x12.xml | Adaptive | 5.50 | 5.00 | 1/1/0 | 4 | 16 | 0/0 |
| factory-feud-narrow-passage-12x12.xml | ThreatAware | 5.50 | 5.00 | 1/1/0 | 3 | 20 | 4/10 |
| factory-feud-narrow-passage-refined-12x12.xml | Adaptive | 4.50 | 1.00 | 2/0/0 | 4 | 22 | 0/0 |
| factory-feud-narrow-passage-refined-12x12.xml | ThreatAware | 4.50 | 1.00 | 2/0/0 | 3 | 32 | 7/16 |

## Overall comparison

| Strategy | Matches | Avg cocoa | Avg Fixed cocoa | W/D/L | Worker deaths | Avg worker deaths | Overrides A/R |
|---|---:|---:|---:|---|---:|---:|---:|
| Adaptive | 10 | 4.60 | 3.20 | 6/3/1 | 19 | 1.90 | 0/0 |
| ThreatAware | 10 | 4.50 | 2.90 | 8/2/0 | 15 | 1.50 | 26/78 |

## ThreatAware change by map

Positive cocoa delta and negative death delta favor ThreatAware.

| Map | Δ avg cocoa | Δ worker deaths | Δ wins | Δ losses | Assessment |
|---|---:|---:|---:|---:|---|
| factory-feud-12x12.xml | +0.50 | -2 | +1 | +0 | improves |
| factory-feud-contested-center-12x12.xml | -1.00 | -1 | +0 | -1 | mixed |
| factory-feud-exposed-routes-14x12.xml | +0.00 | +1 | +1 | +0 | mixed |
| factory-feud-narrow-passage-12x12.xml | +0.00 | -1 | +0 | +0 | improves |
| factory-feud-narrow-passage-refined-12x12.xml | +0.00 | -1 | +0 | +0 | improves |

## Observed tactical examples

- factory-feud-12x12.xml, P0 attack: `cycle=160,unit=21,type=attack,target=26,reason=self-defense`
- factory-feud-12x12.xml, P0 retreat: `cycle=80,unit=22,type=retreat,to=4:5,role=COLLECTOR`
- factory-feud-12x12.xml, P1 attack: `cycle=80,unit=27,type=attack,target=21,reason=self-defense`
- factory-feud-12x12.xml, P1 retreat: `cycle=70,unit=25,type=retreat,to=6:8,role=ESCORT`
- factory-feud-contested-center-12x12.xml, P0 attack: `cycle=30,unit=22,type=attack,target=26,reason=self-defense`
- factory-feud-contested-center-12x12.xml, P0 retreat: `cycle=40,unit=20,type=retreat,to=3:5,role=COLLECTOR`
- factory-feud-contested-center-12x12.xml, P1 attack: `cycle=30,unit=26,type=attack,target=22,reason=self-defense`
- factory-feud-contested-center-12x12.xml, P1 retreat: `cycle=30,unit=24,type=retreat,to=7:5,role=COLLECTOR`
- factory-feud-exposed-routes-14x12.xml, P0 attack: `cycle=40,unit=22,type=attack,target=24,reason=self-defense`
- factory-feud-exposed-routes-14x12.xml, P0 retreat: `cycle=40,unit=20,type=retreat,to=5:5,role=COLLECTOR`
- factory-feud-exposed-routes-14x12.xml, P1 attack: `cycle=60,unit=24,type=attack,target=22,reason=self-defense`
- factory-feud-exposed-routes-14x12.xml, P1 retreat: `cycle=50,unit=26,type=retreat,to=9:5,role=COLLECTOR`
- factory-feud-narrow-passage-12x12.xml, P0 attack: `cycle=30,unit=22,type=attack,target=26,reason=self-defense`
- factory-feud-narrow-passage-12x12.xml, P0 retreat: `cycle=40,unit=20,type=retreat,to=3:5,role=COLLECTOR`
- factory-feud-narrow-passage-12x12.xml, P1 attack: `cycle=30,unit=26,type=attack,target=22,reason=self-defense`
- factory-feud-narrow-passage-12x12.xml, P1 retreat: `cycle=30,unit=24,type=retreat,to=7:5,role=COLLECTOR`
- factory-feud-narrow-passage-refined-12x12.xml, P0 attack: `cycle=30,unit=22,type=attack,target=24,reason=self-defense`
- factory-feud-narrow-passage-refined-12x12.xml, P0 retreat: `cycle=45,unit=22,type=retreat,to=5:6,role=ESCORT`
- factory-feud-narrow-passage-refined-12x12.xml, P1 attack: `cycle=40,unit=26,type=attack,target=22,reason=self-defense`
- factory-feud-narrow-passage-refined-12x12.xml, P1 retreat: `cycle=30,unit=26,type=retreat,to=7:5,role=COLLECTOR`
