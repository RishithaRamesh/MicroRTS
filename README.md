# Factory Feud (CSC 584)

Factory Feud is a cocoa-delivery RTS project built on **microRTS**. It includes fixed-role and adaptive AI, five handcrafted maps (D1), and procedurally generated maps (D2).

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

## Credits

Our team implemented the Factory Feud scenario, AI strategies, maps, generator, validation, experiments, and visualization integration. The game engine, navigation, simulation, and rendering infrastructure come from [microRTS](https://github.com/Farama-Foundation/MicroRTS). Upstream engine source was not modified for Factory Feud.
