# Factory Feud: Adaptive Role Assignment and Threat-Aware AI

**CSC 584: Building Game AI | Deliverable 1: Strategic Decision Making**

Factory Feud is a competitive cocoa-delivery real-time strategy scenario implemented using the Java **microRTS** engine. It compares a project-authored **AdaptiveRoleAI** against a project-authored **FixedRoleAI** baseline. The goal is to study whether dynamic role assignment and threat-aware actions improve cocoa deliveries.

## 1. Quick start (macOS / Linux)

**Prerequisites:** A JDK compatible with this microRTS checkout and Apache Ant (`java -version` and `ant -version` to check), plus a desktop/graphical session for the visual demo. Keep the repository's supplied `lib/` dependencies when packaging the project.

Open a terminal in the **repository root**, the directory containing `build.xml`:

```bash
cd ~/Documents/GitHub/MicroRTS
ant clean build
ant test
ant factory-feud-visual
```

The `cd` command above is an example for the development machine. On another machine, navigate to the extracted repository directory instead.

The visual demo runs **FixedRoleAI as P0 (blue/left)** and **AdaptiveRoleAI as P1 (pink/right)**. Close the Java game window to return to the terminal.

Other important commands:

```bash
# One headless match: FixedRoleAI vs AdaptiveRoleAI
ant factory-feud-ai-smoke

# Evaluate both player-side assignments across all Factory Feud XML maps
# and write timestamped CSV and Markdown summaries to results/
ant factory-feud-evaluate
```

## 2. Scenario and win condition

- **Teams:** Two players, each beginning with **four Workers** and one factory (`Base`).
- **Resource:** One shared neutral resource node with **120 cocoa** on each D1 map. Workers collect cocoa and return it to their own factory.
- **Match duration:** **500 simulation cycles** by default. Actions may span several cycles.
- **Winner:** The team that delivers the most cocoa wins; equal scores are a draw.
- **Combat:** Workers have **1 HP**, so one successful hit can eliminate a Worker.
- **Production:** New-unit production is disabled. Lost Workers are not replaced.

The scenario uses the existing microRTS game-state representation, movement, resource harvesting and returning, combat, and A* pathfinding. We did not modify upstream engine source for Factory Feud.

## 3. AI strategies

### FixedRoleAI: baseline opponent

Each Worker receives a predetermined role that remains unchanged during the match:

- **Collector:** Harvest and deliver cocoa.
- **Escort:** Protect friendly Collectors.
- **Attacker:** Engage opposing Workers.
- **Defender:** Protect the factory and teammates.

The baseline still reacts to the game within its assigned roles; it does **not** dynamically reassign those roles.

### AdaptiveRoleAI: proposed strategy

AdaptiveRoleAI reads the current microRTS `GameState` and evaluates resource availability, friendly and opposing unit positions, threats to Collectors, factory coverage, and the game situation. It can **reassign Workers among the same four roles** as circumstances change. Normal reassignment uses a cooldown, while urgent factory-defense conditions can override that stability mechanism.

Its **threat-aware tactical layer** can temporarily override a role-specific action with a counterattack or retreat when immediate combat conditions warrant it. The Worker otherwise continues its assigned objective. Role changes and tactical overrides are separate mechanisms.

**Strategy-to-engine interface:**

1. The engine provides the current `GameState` (units, positions, resources, factories, and relevant status).
2. Our strategy assigns a role/goal and selects a target for each Worker.
3. The adaptive tactical layer evaluates immediate enemy threats and may alter the immediate action.
4. The AI issues high-level movement, harvest/return, or attack orders through microRTS's abstraction layer.
5. microRTS carries out navigation, action validation and execution, resource transfer, and combat, producing the next observable state.

Both strategies and the cocoa-delivery match controller were authored for this project. **No open-source strategic tournament bot is used as the D1 strategist or baseline.**

## 4. Run the five D1 maps visually

Run these commands **one at a time** from the repository root. Close the current visual window before starting the next match.

### Map 1: Shared Cocoa (12×12, default)

```bash
ant factory-feud-visual
```

### Map 2: Contested Center (12×12)

```bash
ant -Dfactoryfeud.visual.map=maps/factory-feud/factory-feud-contested-center-12x12.xml factory-feud-visual
```

### Map 3: Factory Pressure (12×12)

```bash
ant -Dfactoryfeud.visual.map=maps/factory-feud/factory-feud-factory-pressure-12x12.xml factory-feud-visual
```

### Map 4: Narrow Passage (12×12)

```bash
ant -Dfactoryfeud.visual.map=maps/factory-feud/factory-feud-narrow-passage-12x12.xml factory-feud-visual
```

### Map 5: Obstacle Routes (14×12)

```bash
ant -Dfactoryfeud.visual.map=maps/factory-feud/factory-feud-obstacle-routes-14x12.xml factory-feud-visual
```

The visualization displays the map, cocoa-delivery scores, surviving Workers, the adaptive team's roles and recent role changes, and tactical override information. For a slower demo, adjust the delay in milliseconds per cycle:

```bash
ant -Dfactoryfeud.visual.delay=75 factory-feud-visual
```

To change the demonstration duration:

```bash
ant -Dfactoryfeud.cycles=1000 factory-feud-visual
```

The visual runner requires a graphical desktop session; it will not display a window in headless mode.

## 5. Reproduce the D1 evaluation

One command runs **FixedRoleAI vs AdaptiveRoleAI with both player-side assignments on every `.xml` map directly in `maps/factory-feud/`**:

```bash
ant factory-feud-evaluate
```

The evaluation runner:

- Discovers XML maps automatically and sorts them by filename.
- Validates the starting units, factories, shared cocoa supply, placements, and accessible collection/return routes.
- Runs **Fixed P0 vs Adaptive P1**, then **Adaptive P0 vs Fixed P1** on each map.
- Uses the default **500 cycles**, **seed 584**, the microRTS **`ORIGINAL_FINETUNED`** unit types, and **cancel-both** movement conflict handling.
- Writes new, timestamped **CSV** and **Markdown** reports under `results/` without overwriting historical outputs.

Override settings when needed:

```bash
ant -Dfactoryfeud.evaluate.seed=584 \
    -Dfactoryfeud.evaluate.cycles=500 \
    -Dfactoryfeud.evaluate.output.dir=results/evaluations \
    factory-feud-evaluate
```

**Important:** The two player-side assignments are paired comparisons, not independent random trials. Under the current deterministic rules and agents, repeating the same map and configuration with a different seed need not change the simulation outcome. Wall-clock runtimes may vary.

### D1 reference results (five maps, ten matches)

| Map | Mean Fixed cocoa | Mean Adaptive cocoa | Adaptive W/D/L |
|---|---:|---:|---:|
| Shared Cocoa | 2.00 | 3.00 | 2/0/0 |
| Contested Center | 5.00 | 5.50 | 1/1/0 |
| Factory Pressure | 0.00 | 1.50 | 1/1/0 |
| Narrow Passage | 5.00 | 5.50 | 1/1/0 |
| Obstacle Routes | 0.50 | 2.50 | 2/0/0 |
| **Overall (10 matches)** | **2.50** | **3.60** | **7/3/0** |

AdaptiveRoleAI recorded **138 role changes**, **27 tactical counterattacks**, **96 retreats**, and **15 Worker deaths** across the ten matches. It averaged **44% more cocoa delivered per match** than FixedRoleAI in this selected map set.

On Factory Pressure, Adaptive as P1 drew 0–0 with repeated role switching, whereas Adaptive as P0 won 3–0. This is a useful example of the limitations of role-priority stability and player-side asymmetry.

The CSV/Markdown generated by the runner are the source of truth if the maps or AI implementation change.

## 6. Tests and other useful commands

```bash
# Rebuild from sources and run the JUnit suite
ant clean build
ant test

# Headless final-AI smoke test
ant factory-feud-ai-smoke

# Original scenario smoke runner (Phase 1 regression, not D1 AI comparison)
ant factory-feud-smoke

# Reusable five-map evaluation (or any XML maps currently in the folder)
ant factory-feud-evaluate
```

A successful `ant clean build` is a clean compilation in the current checkout. For a true clean-checkout test, extract/clone the submission into a new directory and repeat the quick-start commands there.

## 7. Project-owned code and upstream attribution

| Location | Ownership / purpose |
|---|---|
| `src/courseproject/factoryfeud/` | Project-authored scenario, AI controllers, match runner, evaluation, and visualization integration |
| `test/courseproject/` | Project-authored tests |
| `maps/factory-feud/` | Project-authored scenario maps |
| `results/` | Generated evaluation data; may need to be generated after checkout |
| `src/rts/`, `src/ai/`, `src/gui/`, and related engine packages | Existing microRTS infrastructure |

The project is based on Farama Foundation microRTS upstream commit:

`72f2fd94039adf3323f4591947e98661a572f927`

The submitted source archive is intended to be used directly, rather than applied as a patch to a different checkout. Keep the project's build files, dependent libraries, source tree, tests, and maps together.

For engine documentation and upstream credit, see [Farama Foundation microRTS](https://github.com/Farama-Foundation/MicroRTS). The original microRTS work is attributed to Santiago Ontañón.

**Research inspiration:** Barriga, N., Stanescu, M., and Buro, M. (2017), *Combining Strategic Learning and Tactical Search in Real-Time Strategy Games*, AIIDE. [Paper](https://ojs.aaai.org/index.php/AIIDE/article/view/12922). Our work uses a rule-based strategy/tactics separation; it does not implement the paper's neural-network strategic learning or game-tree tactical search.

## 8. D1 submission contents

The Moodle submission should include a ZIP of the source repository with this README, the Factory Feud source and maps, necessary microRTS files/dependencies, the final PDF writeup (including the architecture diagram and appendix screenshots), and the **3–5-minute Mac-compatible demonstration video** according to the Moodle upload instructions.

**The video is mandatory under the D1 handout.** A common compatible format is MP4 with H.264 video.
