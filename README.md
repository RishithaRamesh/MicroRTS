# Factory Feud (CSC 584)

Factory Feud is a cocoa-delivery RTS project built on **microRTS**. It includes fixed-role and adaptive AI, five handcrafted maps (D1), procedurally generated maps (D2), and the integrated D3 evaluation.

## Requirements

- Java JDK and Apache Ant
- Python 3 with `matplotlib` for D2 plots
- A graphical desktop to run the visualizer

Run all commands from the repository root (the folder containing `build.xml`).

## Build and test

```bash
ant clean build
ant factory-feud-generation-tests
```

`ant test` runs the full suite, but the upstream `TestTracesIntegrity` test may fail if its BroodWar map fixture is missing.

## Deliverable 1: Handcrafted maps

```bash
ant factory-feud-visual
ant factory-feud-evaluate
```

The visualizer runs **FixedRoleAI (P0, blue)** against **AdaptiveRoleAI (P1, pink)**.

## Deliverable 2: Generate and analyze maps

```bash
# Generate 120 maps: 40 OPEN, 40 OBSTACLE_FIELD, 40 CHOKEPOINT
ant factory-feud-generate

# Produce distribution plots and summary statistics
ant factory-feud-plot

# Reproduce the rejected CHOKEPOINT example
ant factory-feud-invalid-map-diagnostic
```

Generated maps: `maps/factory-feud/generated/`  
Results and CSVs: `results/factory-feud-d2/`  
Plots: `results/factory-feud-d2/plots/`  
Rejected-map image: `results/factory-feud-d2/invalid-examples/`

### View generated maps

**OPEN**
```bash
ant -Dfactoryfeud.visual.map=maps/factory-feud/generated/factory-feud-pcg-v1-open-001-sparse-seed-584000-16x12.xml factory-feud-visual
```

**OBSTACLE_FIELD**
```bash
ant -Dfactoryfeud.visual.map=maps/factory-feud/generated/factory-feud-pcg-v1-obstacle-field-019-medium-seed-1584018-16x12.xml factory-feud-visual
```

**CHOKEPOINT**
```bash
ant -Dfactoryfeud.visual.map=maps/factory-feud/generated/factory-feud-pcg-v1-chokepoint-001-sparse-seed-2584000-16x12.xml factory-feud-visual
```

### Run a headless match on a generated map

```bash
ant -Dfactoryfeud.ai.map=maps/factory-feud/generated/factory-feud-pcg-v1-chokepoint-001-sparse-seed-2584000-16x12.xml factory-feud-ai-smoke
```

For generator parameters and CSV formats, see [`docs/FACTORY_FEUD_D2.md`](docs/FACTORY_FEUD_D2.md).

## Deliverable 3: Integrated evaluation

The D3 workflow deterministically selects 24 accepted D2 maps: two lexicographically
first map IDs from every family/density-preset stratum (8 maps per family). It runs
both side assignments for `AdaptiveRoleAI` and the tactical-off
`OldAdaptiveRoleAI`, each against `FixedRoleAI`, for 96 matches total.

```bash
# Build and run D3-specific tests (includes a 2-cycle balanced smoke run)
ant clean build
ant factory-feud-d3-tests

# Optional short command-line smoke experiment (48 matches × 25 cycles)
ant factory-feud-d3-smoke

# Full 24-map, 96-match, 500-cycle experiment
ant factory-feud-d3-experiment

# Reproducible descriptive analysis and figures (requires matplotlib)
ant factory-feud-d3-plot
```

The full run writes `selection-manifest.csv`, `match-results.csv`,
`match-failures.csv`, and `summary.md` under `results/factory-feud-d3/`. Plotting
adds six PNG figures, `terrain-associations.csv`, and `analysis.md` under its
`plots/` subdirectory. The result files are separate from D1/D2 outputs.

Configuration can be overridden with Ant properties such as
`factoryfeud.d3.seed`, `factoryfeud.d3.metadata`, and
`factoryfeud.d3.output.dir`. See [`docs/FACTORY_FEUD_D3.md`](docs/FACTORY_FEUD_D3.md)
for the design, metrics, hypotheses, and limitations.

## Credits

Our team implemented the Factory Feud scenario, AI strategies, maps, generator, validation, experiments, and visualization integration. The game engine, navigation, simulation, and rendering infrastructure come from [microRTS](https://github.com/Farama-Foundation/MicroRTS). Upstream engine source was not modified for Factory Feud.
