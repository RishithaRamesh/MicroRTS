<p align="center">
    <img src="microrts-text.png" width="500px"/>
</p>

ant clean build
ant test
ant factory-feud-ai-smoke
ant factory-feud-visual

**Aug 11, 2025: This project has been deprecated due to a lack of wide spread community use, and is no longer planned to receive any additional updates or support.**

[![Build Status](https://travis-ci.org/douglasrizzo/microrts.svg?branch=master)](https://travis-ci.org/douglasrizzo/microrts)

microRTS is a small implementation of an RTS game, designed to perform AI research. The advantage of using microRTS with respect to using a full-fledged game like Wargus or StarCraft (using BWAPI) is that microRTS is much simpler, and can be used to quickly test theoretical ideas, before moving on to full-fledged RTS games.

By default, microRTS is deterministic and real-time (i.e. players can issue actions simultaneously, and actions are durative). However, it is possible to experiment both with fully-observable and partially-observable games, as well as with deterministic and non-deterministic settings via configuration flags. As part of the implementation, I include a collection of hard-coded, and game-tree search techniques (such as variants of minimax, Monte Carlo search, and Monte Carlo Tree Search).

microRTS was developed by [Santiago Ontañón](https://sites.google.com/site/santiagoontanonvillar/Home). 

MicroRTS-Py will eventually be updated, maintained, and made compliant with the standards of the Farama Foundation (https://farama.org/project_standards). However, this is currently a lower priority than other projects we're working to maintain. If you'd like to contribute to development, you can join our discord server here- https://discord.gg/jfERDCSw.

For a video of how microRTS looks like when a human plays, see a [YouTube video](https://www.youtube.com/watch?v=ZsKKAoiD7B0)

If you are interested in testing your algorithms against other people's, **there is an annual microRTS competition**. For more information on the competition see the [competition website](https://sites.google.com/site/micrortsaicompetition/home). The previous competitions have been organized at IEEE-CIG2017 and IEEE-CIG2018, and this year it's organized at IEEE-COG2019 (notice the change of name of the conference).

To cite microRTS, please cite this paper:

Santiago Ontañón (2013) The Combinatorial Multi-Armed Bandit Problem and its Application to Real-Time Strategy Games, In AIIDE 2013. pp. 58 - 64.

## Setting up microRTS in an IDE

Watch [this YouTube video](https://www.youtube.com/watch?v=_jVOMNqw3Qs) to learn how to acquire microRTS and setup a project using Netbeans.

## Reinforcement Learning in microRTS

If you'd like to use reinforcement learning in microRTS please check this project: https://github.com/Farama-Foundation/MicroRTS-Py

## Executing microRTS through the terminal

If you want to build and run microRTS from source using the command line, clone or download this repository and run the following commands in the root folder of the project to compile the source code:

Linux or Mac OS:

```shell
javac -cp "lib/*:src" -d bin src/rts/MicroRTS.java # to build
```

Windows:

```shell
javac -cp "lib/*;src" -d bin src/rts/MicroRTS.java # to build
```

### Generating a JAR file

You can join all compiled source files and dependencies into a single JAR file, which can be executed on its own. In order to create a JAR file for microRTS:

```shell
javac -cp "lib/*:src" -d bin $(find . -name "*.java") # compile source files
cd bin
find ../lib -name "*.jar" | xargs -n 1 jar xvf # extract the contents of the JAR dependencies
jar cvf microrts.jar $(find . -name '*.class' -type f) # create a single JAR file with sources and dependencies
```

### Executing microRTS

To execute microRTS from compiled class files:

```shell
java -cp "lib/*:bin" rts.MicroRTS # on Linux/Mac
java -cp "lib/*;bin" rts.MicroRTS # on Windows
```

To execute microRTS from the JAR file:

```shell
java -cp microrts.jar rts.MicroRTS
```

#### Which class to execute

microRTS has multiple entry points, and for experimentation purposes you might eventually want to create your own class if none of the base ones suit your needs (see the "tests" folder for examples), but a default one is the `gui.frontend.FrontEnd` class, which opens the default GUI. To execute microRTS in this way, use the following command:

```shell
java -cp microrts.jar gui.frontend.FrontEnd
```

Another, more expansive entry point is the `rts.MicroRTS` class. It is capable of starting microRTS in multiple modes, such as in client mode (attempts to connect to a server which will provide commands to a bot), server mode (tries to connect to a client in order to control a bot), run a standalone game and exit or open the default GUI.

The `rts.MicroRTS` class accepts multiple initialization parameters, either from the command line or from a properties file. A list of all the acceptable command-line arguments can be accessed through the following command:

```shell
java -cp microrts.jar rts.MicroRTS -h
```

An example of a properties file is provided in the `resources` directory. microRTS can be started using a properties file with the following command:

```shell
java -cp microrts.jar rts.MicroRTS -f my_file.properties
```

## Instructions

![instructions image](https://raw.githubusercontent.com/santiontanon/microrts/master/help.png)

## CSC 584 Factory Feud

This repository also contains the project-authored Factory Feud implementation.
Two teams each receive four mobile Workers and one Base/factory.
Neutral Resources represent cocoa. Matches run for a fixed number of cycles and
are won by delivering the most cocoa; equal delivery totals are ties.

This submission is based on Farama Foundation microRTS upstream commit
`72f2fd94039adf3323f4591947e98661a572f927`. The archive is self-contained:
no patch needs to be applied to another checkout. It targets Java 8 bytecode,
builds with Apache Ant, and the commands below are run from the repository root.

### Code ownership

- The existing `src/rts`, `src/ai`, `src/gui`, and related packages are upstream
  microRTS code.
- `src/courseproject/factoryfeud` is project-authored Factory Feud code.
- `test/courseproject` contains project-authored tests.
- `maps/factory-feud` contains project-authored maps.
- Factory Feud reuses upstream movement, combat, harvest, return, map loading,
  and action-validation mechanics. No upstream engine source was changed for
  Factory Feud.

The Phase 1 smoke match temporarily uses two upstream `WorkerRush` controllers
only to exercise the scenario and match loop. These are not the future
Factory Feud baseline or adaptive strategy.

### Build and test

From the repository root:

```shell
ant clean build
ant test
```

The normal `ant test` target includes the focused Factory Feud JUnit suite.

### Run one headless match

Run the default 500-cycle smoke match:

```shell
ant factory-feud-smoke
```

Override the duration when needed:

```shell
ant -Dfactoryfeud.cycles=1000 factory-feud-smoke
```

After compiling, the equivalent direct Java command is:

```shell
java -cp "lib/*:bin" courseproject.factoryfeud.FactoryFeudSmoke 500
```

The command prints final cycles, both delivered-cocoa scores, and the winner;
winner `-1` denotes a tie.

### Run the visual match on macOS

From Terminal in the repository root, launch the Swing runner from a logged-in
macOS desktop session:

```shell
ant factory-feud-visual
```

By default, the window renders the project-authored 12x12 control map through
microRTS's existing Swing visualizer. Player 0 (blue/left) is `FixedRoleAI` and
Player 1 (pink/right) is `AdaptiveRoleAI`.
Its HUD shows delivered-cocoa scores and remaining cycles; the side panel shows
each surviving Worker's ID and role, the role legend, and recent adaptive
reassignment reasons. The runner uses `FactoryFeudScenario` and delegates every
cycle to `FactoryFeudMatch`, so delivery scoring and the production-action
filter are the same as in the headless match. The selected map name and dimensions
appear in the window title and HUD, and the full map path is printed in Terminal.
Close the window after the match to return to the shell.

To change the duration or animation speed (milliseconds per cycle):

```shell
ant -Dfactoryfeud.cycles=1000 -Dfactoryfeud.visual.delay=75 factory-feud-visual
```

Select any validated Factory Feud map with `factoryfeud.visual.map`. For example,
from macOS Terminal:

```shell
ant -Dfactoryfeud.visual.map=maps/factory-feud/factory-feud-narrow-passage-12x12.xml \
  factory-feud-visual

ant -Dfactoryfeud.visual.map=maps/factory-feud/factory-feud-exposed-routes-14x12.xml \
  factory-feud-visual
```

The default strategy can also be selected explicitly with:

```shell
ant -Dfactoryfeud.visual.strategy=adaptive factory-feud-visual
```

The runner requires a graphical session. It will not launch from a headless SSH
session or when Java is forced into headless mode.

The old upstream `WorkerRush` versus `WorkerRush` placeholder visualization is
retained only as a Phase 1 regression option:

```shell
ant factory-feud-visual-placeholder
```

### Phase 2 role agents

Phase 2 adds two project-authored strategies. `FixedRoleAI` assigns one Worker
each to Collector, Escort, Attacker, and Defender by stable unit ID and never
reallocates them. `OldAdaptiveRoleAI` contains the utility/count-based dynamic
allocation policy. `AdaptiveRoleAI` extends it with the successful threat-aware
tactical attack and retreat behavior while retaining dynamic reassignment. Both
use the same project-owned low-level role
behaviors but has an independent allocation policy based on cocoa availability,
score, remaining time, threats, endangered collectors, and surviving units. It
uses deterministic tie-breaking, a reassignment cooldown, and an immediate
factory-defense override.

Run the chosen headless matchup, P0 `FixedRoleAI` versus P1 `AdaptiveRoleAI`:

```shell
ant factory-feud-ai-smoke
```

Override its duration with the same property used by the Phase 1 smoke target:

```shell
ant -Dfactoryfeud.cycles=1000 factory-feud-ai-smoke
```

The default `AdaptiveRoleAI` output includes tactical attack and retreat events.
Use `-Dfactoryfeud.ai.strategy=old-adaptive` only to run the preserved utility-based
version on P1. The historical `threat-aware` selector remains an alias for the
renamed `AdaptiveRoleAI`.

The older `factory-feud-phase2` target remains as an alias. Output includes both
scores, winner, surviving units, adaptive role-change events, proposed illegal
action count, filtered production count, potential stuck-unit diagnostics, and
wall-clock runtime. A single match is a functional verification only and is not
statistical evidence.

### Phase 3A scoring validation and experiments

Run the default 20-match paired pilot (500 cycles per match):

```shell
ant factory-feud-experiments
```

Each seed is used for two consecutive matches, with `AdaptiveRoleAI` on player
0 and then player 1. The match count must therefore be a positive even number.
Override all experiment inputs with Ant properties:

```shell
ant -Dfactoryfeud.experiment.matches=20 \
  -Dfactoryfeud.cycles=500 \
  -Dfactoryfeud.experiment.map=maps/factory-feud/factory-feud-12x12.xml \
  -Dfactoryfeud.experiment.seed=584 \
  -Dfactoryfeud.experiment.csv=results/factory-feud-experiments.csv \
  factory-feud-experiments
```

The original 17 CSV columns remain first and unchanged. Appended telemetry
separates `configured_cycles` and `completed_cycles` (simulation time) from
`simulation_runtime_ms` (wall-clock time). Delivery timing is recorded per
player from observed positive score deltas: first and last delivery cycle, mean
interval between consecutive delivery-event cycles, and event count. First,
last, and mean interval cells are blank when unavailable; a team with no
deliveries has a genuine event count of zero.

Carrier fields are attributed by strategy rather than player number, so paired
side swaps remain comparable. A role interruption is a Collector-to-non-
Collector transition while carrying cocoa. Interrupted cycles count each
cycle each loaded Worker remains outside the Collector role. A correction is
the reverse transition while it is still carrying. Carrier attacks count only
observed HP decreases or removal during combat resolution, never attack orders
or selected targets. Deaths and surviving undelivered cargo are separate.
Blank strategy fields mean that strategy was not present or its role state was
not observable; observable zeroes are written as `0`.

The default remains `AdaptiveRoleAI` versus `FixedRoleAI`. To run the separate
carrier-continuity experiment, set the strategy property to `cargo-safe`:

```shell
ant -Dfactoryfeud.experiment.matches=20 \
  -Dfactoryfeud.cycles=500 \
  -Dfactoryfeud.experiment.map=maps/factory-feud/factory-feud-narrow-passage-refined-12x12.xml \
  -Dfactoryfeud.experiment.seed=584 \
  -Dfactoryfeud.experiment.strategy=cargo-safe \
  -Dfactoryfeud.experiment.csv=results/factory-feud-refined-cargo-safe.csv \
  factory-feud-experiments
```

Use `-Dfactoryfeud.experiment.strategy=adaptive` (the default) for the final
threat-aware `AdaptiveRoleAI`, or `-Dfactoryfeud.experiment.strategy=old-adaptive`
for the preserved utility-based class. The historical `threat-aware` selector is
an alias for `AdaptiveRoleAI`. Tactical CSV rows and the console summary include
the number of tactical overrides. The console summary reports win/draw/loss counts, adaptive-family win
rate (draws remain in the denominator), mean cocoa by strategy, mean score
differential, and mean runtime. Paired rows share a seed and are controlled
side swaps, not independent statistical samples.

The current 12x12 control map starts both players at zero delivered cocoa and
contains one central 120-unit cocoa node. Delivery scores are deltas from each
player's balance at match-controller construction, so preloaded resources are
not scored.

For controlled reproduction, the runner creates a fresh map, match controller,
and independent AI instances for every row and seeds the public microRTS
`UnitAction` random generator. With the current fixed-damage unit table,
cancel-both movement-conflict policy, and deterministic project agents, the
recorded simulation fields repeat in automated tests; wall-clock runtime is
intentionally excluded from equality checks. The engine also contains a private
static random generator for random movement-conflict policies, which this runner
cannot seed through a supported API. Switching to such a policy, variable
damage, or a stochastic AI would require additional reproducibility work.

This Phase 3A runner evaluates one configured map per invocation. Repeating the
same deterministic map and policies under different recorded seeds does not
create independent statistical trials. Procedural generation, multi-map
evaluation, and map-property analysis remain future work.

### Phase 3B controlled scenarios

The Phase 3B evaluation set retains the original map as its control and includes
four manually designed, project-authored variation maps:

- `factory-feud-contested-center-12x12.xml`: open approaches to a contested
  central resource.
- `factory-feud-narrow-passage-12x12.xml`: terrain funnels both teams through a
  central lane.
- `factory-feud-narrow-passage-refined-12x12.xml`: the corridor and factory
  placement create deliberately different route lengths by side.
- `factory-feud-exposed-routes-14x12.xml`: longer open routes increase exposure.

`FactoryFeudScenario` validates every loaded map's player, unit, factory, and
positive cocoa-node count, rejects overlapping or terrain-embedded units,
supports an optional caller-selected exact cocoa count, requires positive cocoa
quantities, and verifies that every Worker shares a terrain component with its
factory and the cocoa node. All five evaluation maps use exactly one neutral
120-cocoa resource and eight starting Workers at one hit point. The upstream
`Worker` type and combat engine are unchanged. Terrain-only route distances are
reported because the single-resource layouts are not all geometrically or
path-length symmetric; paired side swaps control for these effects.

Run 20 side-swapped matches on each map by changing only the existing runner's
map and CSV properties. For example:

```shell
ant -Dfactoryfeud.experiment.matches=20 \
  -Dfactoryfeud.experiment.map=maps/factory-feud/factory-feud-contested-center-12x12.xml \
  -Dfactoryfeud.experiment.csv=results/factory-feud-contested.csv \
  factory-feud-experiments
```

Repeat with these paths:

```text
maps/factory-feud/factory-feud-12x12.xml
maps/factory-feud/factory-feud-contested-center-12x12.xml
maps/factory-feud/factory-feud-narrow-passage-12x12.xml
maps/factory-feud/factory-feud-narrow-passage-refined-12x12.xml
maps/factory-feud/factory-feud-exposed-routes-14x12.xml
```

The aggregate output additionally reports mean casualties, total and mean
adaptive role reassignments, player-0/player-1 wins, and mean player-0 score
advantage. Side statistics must be examined alongside strategy statistics;
geometric symmetry does not guarantee identical deterministic engine outcomes.

Run the final 10-match, five-map `AdaptiveRoleAI` versus `FixedRoleAI`
reproducibility suite (both side assignments, seed 584, 500 cycles) with:

```shell
ant factory-feud-controlled-experiment
```

This writes `results/factory-feud-controlled-renamed-adaptive-seed-584.csv` and
`results/factory-feud-controlled-renamed-adaptive-seed-584.md`. The original
`results/factory-feud-controlled-seed-584.*` files remain preserved for comparison;
wall-clock runtime columns may differ between machines while deterministic
simulation fields remain identical.
