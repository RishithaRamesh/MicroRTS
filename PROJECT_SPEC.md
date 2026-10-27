# CSC 584 Factory Feud Project Specification

## Engine and ownership boundary

Factory Feud is implemented on top of microRTS. Upstream microRTS owns the
game-state representation, map loading, unit actions, movement, pathfinding,
combat, harvesting, and resource-return mechanics. Project-authored code lives
under `src/courseproject/`, project tests live under `test/courseproject/`, and
project maps live under `maps/factory-feud/`.

Upstream engine source should not be modified unless the project cannot be
implemented through the existing APIs.

## Finalized game design

- There are exactly two competing teams: player 0 and player 1.
- Each team starts with four mobile `Worker` units and one `Base`. In Factory
  Feud terminology, the `Base` is that team's factory.
- Neutral microRTS `Resource` units represent collectable cocoa.
- Workers use the standard microRTS harvest action to pick up cocoa and the
  standard return action to deliver it to their own factory.
- A match lasts for a configured, fixed number of game cycles. Normal microRTS
  elimination does not decide a Factory Feud match early.
- A team's score is the total cocoa delivered during the match, not its combat
  survival and not an engine elimination result.
- At the time limit, the team with more delivered cocoa wins. Equal scores,
  including 0-0, are ties.
- Unit production and all resource spending are prohibited. This keeps the
  engine player-resource balance monotonic and allows delivery deltas to be
  accounted for without changing the upstream return mechanic.

## Deliverable 1: strategic decision making

The eventual project comparison uses two independent project-authored agents:

- `AdaptiveRoleAI`, which reassigns roles in response to game state.
- `FixedRoleAI`, which keeps a fixed assignment and serves as the baseline.

Both agents use four roles:

- **Collector**: harvests cocoa and returns it to the factory.
- **Escort**: protects a collector or cocoa carrier.
- **Attacker**: pressures enemy workers and disrupts deliveries.
- **Defender**: protects the team's factory and nearby approaches.

The adaptive policy will use current score, remaining time, cocoa availability,
carriers, threats, and unit positions. Reassignment must use a cooldown,
minimum tenure, hysteresis, or an equivalent mechanism to prevent role
thrashing. The fixed-role baseline must not call, inherit, or share the
adaptive role allocator; only low-level movement/action helpers may be shared.

The final Deliverable 1 demonstration uses `FixedRoleAI` as P0 and
`AdaptiveRoleAI` as P1. `AdaptiveRoleAI` extends the preserved utility-based
`OldAdaptiveRoleAI` allocator with the successful immediate-threat tactical
override. The controlled evaluation reports `AdaptiveRoleAI` against Fixed with
paired side swaps.

Phase 1 implements the scenario, delivery scoring, fixed-duration match
controller, smoke runner, and correctness tests. Phase 2 implements both
strategic agents, shared low-level role behaviors, deterministic allocation,
adaptive reassignment cooldown, emergency defense, and focused AI tests.

## Deliverable 2: procedural content generation

After Deliverable 1 is functional, a project-authored, parameterized generator
will produce and validate at least 100 playable Factory Feud maps and measure at
least three map properties. It must not use the upstream hard-coded
`tests.MapGenerator` as the project contribution.

## Deliverable 3: integration and experimentation

After Deliverables 1 and 2 are functional, a headless experiment runner will
compare the adaptive and fixed-role agents over at least 20 generated maps. It
will use explicit seeds, paired side swaps, per-match CSV records, and map
metrics suitable for analyzing performance by environment.

Phase 3A validates delivery scoring and supplies the reproducible evaluation
infrastructure before procedural generation. It runs paired, side-swapped
matches on a configurable existing Factory Feud map, records one CSV row per
match, and reports aggregate wins, draws, losses, cocoa scores, score
differential, and runtime. Phase 3A is a fixed-map pilot and does not by itself
satisfy the later generated-map and map-metric requirements of Deliverable 3.

Phase 3B adds four project-authored, manually controlled evaluation scenarios:
contested central cocoa, two narrow-passage variants, and exposed delivery
routes. The original 12x12 scenario remains the control. All five evaluation
maps use one 120-cocoa node and one-hit Workers, and must pass team-count,
positive-cocoa, and terrain-reachability validation. Route asymmetries are
measured explicitly. Side-swapped evaluation remains mandatory because map
geometry, deterministic action ordering, and path selection can create
player-position effects.

## Implementation order

1. Scenario, rules, scoring, timer, tests, and one-match runner.
2. Independent fixed-role baseline and adaptive role strategy.
3. Fixed-map scoring validation and paired experiment infrastructure.
4. Controlled manual scenario variation and tactical evaluation.
5. Procedural map generation and validation.
6. Multi-map, multi-seed experiments and analysis.
