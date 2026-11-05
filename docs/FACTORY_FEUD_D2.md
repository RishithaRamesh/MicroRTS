# Factory Feud Deliverable 2: Procedural Content Generation

## Reproducible workflow

Run commands from the repository root:

```bash
ant clean build
ant factory-feud-generation-tests
ant factory-feud-generate
ant factory-feud-plot
```

The standard batch contains 120 accepted maps: 40 `OPEN`, 40
`OBSTACLE_FIELD`, and 40 `CHOKEPOINT`. The Java generator owns all terrain and
unit placement. Python is used only to read the accepted-map CSV and render
descriptive plots.

The full upstream and project Ant suite remains available through `ant test`.
In this checkout, the upstream trace-integrity test may fail because its derived
BroodWar map fixture is absent. `factory-feud-generation-tests` runs the project
PCG tests independently; it does not remove, skip, or modify the upstream test.

## Standard batch presets

The defaults are a 16×12 map, base seed `584000`, and at most 100 raw attempts
per accepted map. The map seed is
`baseSeed + familyOrdinal * 1,000,000 + index`, where the zero-based family
order is `OPEN`, `OBSTACLE_FIELD`, `CHOKEPOINT`. Presets cycle evenly through
each 40-map family.

| Preset | OPEN density | OBSTACLE_FIELD density | CHOKEPOINT density |
|---|---:|---:|---:|
| SPARSE | 0.05 | 0.10 | 0.10 |
| LOW | 0.08 | 0.14 | 0.14 |
| MEDIUM | 0.11 | 0.18 | 0.18 |
| HIGH | 0.14 | 0.22 | 0.22 |

Override batch parameters with Ant properties, for example:

```bash
ant -Dfactoryfeud.pcg.maps.per.family=40 \
    -Dfactoryfeud.pcg.width=16 \
    -Dfactoryfeud.pcg.height=12 \
    -Dfactoryfeud.pcg.base.seed=584000 \
    -Dfactoryfeud.pcg.max.attempts=100 \
    -Dfactoryfeud.pcg.map.dir=maps/factory-feud/generated \
    -Dfactoryfeud.pcg.analysis.dir=results/factory-feud-d2 \
    factory-feud-generate
```

## Families and parameters

- `OPEN` scatters individual wall cells over otherwise open terrain.
- `OBSTACLE_FIELD` grows randomized wall clusters, producing local barriers and
  detours.
- `CHOKEPOINT` constructs a separating barrier with seeded narrow passages,
  then adds clustered walls until the requested obstacle count is reached.

Every accepted map contains exactly two players, four Workers and one Base per
player, and one shared neutral Resource containing 120 cocoa. Unit production
remains disabled by the unchanged `FactoryFeudMatch` controller.

## Validity oracle and retry accounting

Each raw candidate is measured before validation. Validation reuses
`FactoryFeudScenario.validate(pgs, 1)` and then enforces the exact D2 unit and
cocoa configuration, one-hit Workers, supported unit types, and a terrain route
from both factories to cocoa. The shared scenario validation also checks unit
placement, worker connectivity, resource access, and reachable return positions.

Invalid candidates are recorded with their attempt seed, metrics, and rejection
reason. Generation deterministically advances to a derived attempt seed until a
candidate passes or the maximum attempt count is exhausted. There is no
handcrafted-map fallback. A map that succeeds on attempt 3 contributes two
invalid raw candidates and one valid raw candidate; it is not counted as a 100%
raw validity result.

## Output files

Accepted maps use reproducible names such as:

`factory-feud-pcg-v1-open-001-sparse-seed-584000-16x12.xml`

If a generated filename already exists with identical bytes, the exporter leaves
it unchanged. If the bytes differ, generation stops rather than overwriting it.
Handcrafted XML files are outside the generated directory.

`results/factory-feud-d2/factory-feud-generated-maps.csv` contains one row per
accepted map with family, density preset, map seed, dimensions, requested and
actual obstacle density, both factory-to-cocoa paths, route imbalance, dead-end
count, attempts required, and XML path.

`factory-feud-generation-attempts.csv` contains every raw attempt, including
invalid candidates and rejection reasons. `factory-feud-validity-summary.csv`
reports accepted maps, raw valid and invalid attempts, and raw validity rate for
each family and overall.

`ant factory-feud-plot` creates four PNG distributions comparing family and
family/preset groups, plus `factory-feud-property-summary.csv` and
`factory-feud-observations.md`. The observations are direct numerical summaries;
they do not assert statistical significance or causality.

## Playability and visualization

Every exported XML is immediately reloaded through `FactoryFeudScenario.load()`
and compared with its in-memory source before its accepted CSV row is written.

Run a generated map headlessly:

```bash
ant -Dfactoryfeud.ai.map=maps/factory-feud/generated/factory-feud-pcg-v1-open-001-sparse-seed-584000-16x12.xml \
    factory-feud-ai-smoke
```

Visualize it:

```bash
ant -Dfactoryfeud.visual.map=maps/factory-feud/generated/factory-feud-pcg-v1-open-001-sparse-seed-584000-16x12.xml \
    factory-feud-visual
```

## Limitations and manual work

- Validity guarantees connectivity and rules compliance, not balanced tactical
  difficulty or equal path lengths. Route imbalance is measured for analysis.
- Dead ends are terrain-topology measurements and ignore temporary unit
  occupancy.
- The batch is descriptive; formal significance testing is outside this phase.
- Screenshots and video require a graphical desktop and must be captured
  manually while running `factory-feud-visual`.
- Matplotlib must be installed for plotting. Generation and XML export require
  only Java/Ant and the repository libraries.
