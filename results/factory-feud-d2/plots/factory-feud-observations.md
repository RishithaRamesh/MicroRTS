# Factory Feud generated-map observations

These statements are descriptive summaries of the generated batch; they do not claim statistical significance or causal effects.

- Actual obstacle density: highest family mean was tied between `OBSTACLE_FIELD` and `CHOKEPOINT` (0.160); lowest was `OPEN` (0.095).
- Mean terrain center-to-center shortest path length: highest family mean was `CHOKEPOINT` (8.400); lowest was `OPEN` (7.450).
- Route imbalance: highest family mean was `CHOKEPOINT` (3.950); lowest was `OPEN` (1.900).
- Dead-end count: highest family mean was `CHOKEPOINT` (3.350); lowest was `OPEN` (2.500).

Path length is a terrain-level center-to-center proxy from each Base tile to the Resource tile. It is not the exact Worker travel distance because both endpoint tiles are occupied. On the fixed 16x12 batch layout, the unobstructed center-to-center distances are 7 cells for player 0 and 6 for player 1, so route imbalance has a built-in one-cell baseline.

The family-and-preset boxplots summarize the distributions; they do not display every individual observation. OPEN also uses lower obstacle-density presets than OBSTACLE_FIELD and CHOKEPOINT, so differences between OPEN and the other families cannot be attributed solely to the family-generation algorithm.
