# Factory Feud Deliverable 3: Putting It All Together

## Design and reproducibility

D3 consumes the accepted D2 metadata rather than regenerating or modifying maps.
For each of the three families and four density presets, it sorts accepted map IDs
lexicographically and selects the first two. This outcome-blind rule yields 24
distinct maps and is recorded in `results/factory-feud-d3/selection-manifest.csv`.

Each selected map receives four 500-cycle matches:

1. Fixed P0 versus `AdaptiveRoleAI` P1.
2. `AdaptiveRoleAI` P0 versus Fixed P1.
3. Fixed P0 versus `OldAdaptiveRoleAI` P1.
4. `OldAdaptiveRoleAI` P0 versus Fixed P1.

The first two are the MAIN comparison (H1/H2); the latter two are the ABLATION
comparison (H3). A map uses simulation seed `3584 + manifestIndex` for both side
assignments and both experiments. The engine unit table remains
`ORIGINAL_FINETUNED`, movement conflicts remain cancel-both, production remains
filtered, and matches retain the Factory Feud 500-cycle scoring controller.

The ablation is controlled in the implementation: `AdaptiveRoleAI` extends
`OldAdaptiveRoleAI`, inherits its complete strategic assignment policy, and only
overrides role execution to add immediate threat counterattacks and retreats.

## Commands

```bash
ant clean build
ant factory-feud-d3-tests
ant factory-feud-d3-experiment
ant factory-feud-d3-plot
```

For a short integration check:

```bash
ant factory-feud-d3-smoke
```

The smoke command uses one map per family/preset and 25 cycles. It writes to
`results/factory-feud-d3-smoke/`, leaving the full D3 outputs untouched.

## Outputs and metrics

`match-results.csv` records experiment/map/seed identity, both AI assignments,
cocoa scores and adaptive margin, W/D/L outcome, cycles, cocoa remaining on the
resource node, Worker survival/deaths, role changes, tactical interventions,
delivery and carrier telemetry, full role-change/tactical event histories, D2
terrain properties, and runtime. The histories are diagnostic output only and do
not alter controller behavior. `match-failures.csv`
always exists and contains a header plus a row for every failed match; the full
Ant target fails after reporting if any match failed.

`summary.md` evaluates the original thresholds without retuning them:

- H1: strict MAIN win percentage is at least 70%.
- H2: CHOKEPOINT mean margin minus OPEN mean margin is at least 2 cocoa.
- H3: mean tactical cocoa improves on matched non-tactical cocoa by at least 15%.

The H3 denominator is checked explicitly. The Python analysis validates complete
map/side matching, reduces swapped MAIN matches to one observation per map for
terrain associations, and produces six explanatory figures plus a table of
descriptive Pearson correlations.

## Interpretation limits

- Side-swapped observations on the same deterministic map are paired, not
  independent replications.
- D2 path length is a Base-center to cocoa-center terrain proxy. It is not the
  exact route traversed by Workers, whose interactions occur from adjacent cells.
- OPEN density presets are lower than OBSTACLE_FIELD/CHOKEPOINT presets, so family
  and density are confounded. Family differences are descriptive, not causal.
- Carrier-hit telemetry confirms hits or deaths while a Worker carries cocoa; it
  is not a complete count of every combat interaction.
- The experiment evaluates 24 deterministically selected maps from this D2 batch;
  it does not establish generalization to every possible generated map.
