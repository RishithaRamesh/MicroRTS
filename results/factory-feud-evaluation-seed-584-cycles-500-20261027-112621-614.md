# Factory Feud evaluation

## Configuration

- Seed: `584`
- Match duration: `500` cycles
- Unit types: `ORIGINAL_FINETUNED`
- Movement conflict policy: `cancel-both`
- Pair order: FixedRoleAI P0 vs AdaptiveRoleAI P1, then side swap

## Maps tested

- `factory-feud-12x12.xml`
- `factory-feud-contested-center-12x12.xml`
- `factory-feud-factory-pressure-12x12.xml`
- `factory-feud-narrow-passage-12x12.xml`
- `factory-feud-obstacle-routes-14x12.xml`

## Map validation and route audit

| Map | Size | Cocoa | P0 worker→cocoa | P1 worker→cocoa | P0 return | P1 return | Geometric symmetry |
|---|---:|---:|---|---|---:|---:|---|
| factory-feud-12x12.xml | 12×12 | 120 @ 5,6 | [7, 7, 6, 6] | [7, 7, 6, 6] | 7 | 7 | no |
| factory-feud-contested-center-12x12.xml | 12×12 | 120 @ 5,5 | [2, 3, 3, 4] | [3, 4, 4, 5] | 2 | 3 | no |
| factory-feud-factory-pressure-12x12.xml | 12×12 | 120 @ 5,2 | [6, 5, 7, 5] | [7, 6, 8, 6] | 6 | 7 | no |
| factory-feud-narrow-passage-12x12.xml | 12×12 | 120 @ 5,5 | [2, 3, 3, 4] | [3, 4, 4, 5] | 2 | 3 | no |
| factory-feud-obstacle-routes-14x12.xml | 14×12 | 120 @ 7,5 | [4, 5, 5, 6] | [4, 5, 3, 6] | 4 | 4 | no |

## Match results

| ID | Map | P0 AI | P1 AI | Fixed cocoa | Adaptive cocoa | Adaptive result | Adaptive workers alive/dead | Fixed workers alive/dead | Role changes | Counterattacks/retreats | Completed/configured cycles | Runtime ms |
|---:|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|
| 1 | factory-feud-12x12.xml | FixedRoleAI | AdaptiveRoleAI | 2 | 3 | WIN | 2/2 | 2/2 | 10 | 2/15 | 500/500 | 24 |
| 2 | factory-feud-12x12.xml | AdaptiveRoleAI | FixedRoleAI | 2 | 3 | WIN | 3/1 | 2/2 | 12 | 2/15 | 500/500 | 13 |
| 3 | factory-feud-contested-center-12x12.xml | FixedRoleAI | AdaptiveRoleAI | 5 | 5 | DRAW | 3/1 | 1/3 | 15 | 3/6 | 500/500 | 7 |
| 4 | factory-feud-contested-center-12x12.xml | AdaptiveRoleAI | FixedRoleAI | 5 | 6 | WIN | 2/2 | 2/2 | 1 | 2/4 | 500/500 | 2 |
| 5 | factory-feud-factory-pressure-12x12.xml | FixedRoleAI | AdaptiveRoleAI | 0 | 0 | DRAW | 2/2 | 2/2 | 45 | 2/26 | 500/500 | 4 |
| 6 | factory-feud-factory-pressure-12x12.xml | AdaptiveRoleAI | FixedRoleAI | 0 | 3 | WIN | 3/1 | 0/4 | 16 | 6/3 | 500/500 | 2 |
| 7 | factory-feud-narrow-passage-12x12.xml | FixedRoleAI | AdaptiveRoleAI | 5 | 5 | DRAW | 3/1 | 2/2 | 15 | 2/7 | 500/500 | 7 |
| 8 | factory-feud-narrow-passage-12x12.xml | AdaptiveRoleAI | FixedRoleAI | 5 | 6 | WIN | 2/2 | 2/2 | 5 | 2/3 | 500/500 | 3 |
| 9 | factory-feud-obstacle-routes-14x12.xml | FixedRoleAI | AdaptiveRoleAI | 1 | 3 | WIN | 2/2 | 1/3 | 10 | 4/15 | 500/500 | 6 |
| 10 | factory-feud-obstacle-routes-14x12.xml | AdaptiveRoleAI | FixedRoleAI | 0 | 2 | WIN | 3/1 | 2/2 | 9 | 2/2 | 500/500 | 3 |

## Summary by map

| Map | Matches | Avg Adaptive cocoa | Avg Fixed cocoa | Adaptive W/D/L | Adaptive worker deaths | Role changes | Counterattacks/retreats |
|---|---:|---:|---:|---|---:|---:|---:|
| factory-feud-12x12.xml | 2 | 3.00 | 2.00 | 2/0/0 | 3 | 22 | 4/30 |
| factory-feud-contested-center-12x12.xml | 2 | 5.50 | 5.00 | 1/1/0 | 3 | 16 | 5/10 |
| factory-feud-factory-pressure-12x12.xml | 2 | 1.50 | 0.00 | 1/1/0 | 3 | 61 | 8/29 |
| factory-feud-narrow-passage-12x12.xml | 2 | 5.50 | 5.00 | 1/1/0 | 3 | 20 | 4/10 |
| factory-feud-obstacle-routes-14x12.xml | 2 | 2.50 | 0.50 | 2/0/0 | 3 | 19 | 6/17 |

## Overall

| Matches | Avg Adaptive cocoa | Avg Fixed cocoa | Adaptive W/D/L | Adaptive worker deaths | Role changes | Counterattacks/retreats |
|---:|---:|---:|---|---:|---:|---:|
| 10 | 3.60 | 2.50 | 7/3/0 | 15 | 138 | 27/96 |
