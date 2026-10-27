package courseproject.factoryfeud;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/** Runs the fixed 20-match, five-map Factory Feud comparison requested for seed 584. */
public final class FactoryFeudControlledExperimentRunner {
    public static final int CYCLES = 500;
    public static final long SEED = 584L;
    public static final Path DEFAULT_CSV = Paths.get(
            "results/factory-feud-controlled-seed-584.csv");
    public static final Path DEFAULT_REPORT = Paths.get(
            "results/factory-feud-controlled-seed-584.md");

    private static final List<String> MAPS = Collections.unmodifiableList(Arrays.asList(
            FactoryFeudScenario.CONTROL_MAP,
            FactoryFeudScenario.CONTESTED_CENTER_MAP,
            FactoryFeudScenario.EXPOSED_ROUTES_MAP,
            FactoryFeudScenario.NARROW_PASSAGE_MAP,
            FactoryFeudScenario.REFINED_NARROW_PASSAGE_MAP));
    private static final List<FactoryFeudExperimentConfig.AdaptiveStrategy> STRATEGIES =
            Collections.unmodifiableList(Arrays.asList(
                    FactoryFeudExperimentConfig.AdaptiveStrategy.ADAPTIVE,
                    FactoryFeudExperimentConfig.AdaptiveStrategy.THREAT_AWARE));
    private static final int[][] DIRECTIONS = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};

    private FactoryFeudControlledExperimentRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 2) {
            throw new IllegalArgumentException("Usage: [csv-output] [markdown-output]");
        }
        Path csv = args.length >= 1 ? Paths.get(args[0]) : DEFAULT_CSV;
        Path report = args.length >= 2 ? Paths.get(args[1]) : DEFAULT_REPORT;
        List<MapAudit> audits = auditMaps();
        List<FactoryFeudExperimentRecord> records = runMatches(csv);
        writeReport(report, audits, records);
        System.out.println("csv=" + csv.toAbsolutePath());
        System.out.println("report=" + report.toAbsolutePath());
        System.out.println("matches=" + records.size() + " cycles=" + CYCLES
                + " seed=" + SEED + " conflictPolicy=cancel-both");
    }

    static List<FactoryFeudExperimentRecord> runMatches(Path csv) throws Exception {
        List<FactoryFeudExperimentRecord> records = new ArrayList<>();
        int matchId = 1;
        for (String map : MAPS) {
            for (FactoryFeudExperimentConfig.AdaptiveStrategy strategy : STRATEGIES) {
                FactoryFeudExperimentConfig config = new FactoryFeudExperimentConfig(
                        2, CYCLES, map, SEED, csv, strategy);
                for (int strategySide = 0; strategySide < 2; strategySide++) {
                    records.add(FactoryFeudExperimentRunner.runMatch(
                            matchId++, strategySide, SEED, config));
                }
            }
        }
        FactoryFeudExperimentRunner.writeCsv(csv, records);
        return records;
    }

    static List<MapAudit> auditMaps() throws Exception {
        UnitTypeTable utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        List<MapAudit> audits = new ArrayList<>();
        for (String map : MAPS) {
            GameState state = FactoryFeudScenario.load(map, utt);
            PhysicalGameState pgs = state.getPhysicalGameState();
            Unit resource = null;
            int resourceCount = 0;
            int cocoa = 0;
            int workers = 0;
            for (Unit unit : pgs.getUnits()) {
                if (unit.getType().isResource) {
                    resource = unit;
                    resourceCount++;
                    cocoa += unit.getResources();
                } else if (unit.getPlayer() >= 0 && unit.getType().canHarvest) {
                    workers++;
                    if (unit.getHitPoints() != 1) {
                        throw new IllegalArgumentException(map + " worker " + unit.getID()
                                + " does not start at 1 HP");
                    }
                }
            }
            if (resourceCount != 1 || cocoa != 120 || workers != 8) {
                throw new IllegalArgumentException(map
                        + " must contain eight Workers and one 120-cocoa node");
            }
            int[][] workerDistances = new int[2][];
            int[] returnDistances = new int[2];
            for (int player = 0; player < 2; player++) {
                List<Unit> playerWorkers = units(pgs, player, true);
                workerDistances[player] = new int[playerWorkers.size()];
                List<Integer> resourceApproaches = adjacentTerrain(pgs, resource);
                for (int index = 0; index < playerWorkers.size(); index++) {
                    Unit worker = playerWorkers.get(index);
                    workerDistances[player][index] = shortestDistance(
                            pgs, worker.getX() + worker.getY() * pgs.getWidth(),
                            resourceApproaches);
                    if (workerDistances[player][index] < 0) {
                        throw new IllegalArgumentException(map + " player " + player
                                + " worker " + worker.getID() + " cannot reach cocoa");
                    }
                }
                Unit factory = units(pgs, player, false).get(0);
                returnDistances[player] = shortestBetween(
                        pgs, resourceApproaches, adjacentTerrain(pgs, factory));
                if (returnDistances[player] < 0) {
                    throw new IllegalArgumentException(map + " player " + player
                            + " cannot return cocoa to its factory");
                }
            }
            audits.add(new MapAudit(map, pgs.getWidth(), pgs.getHeight(),
                    resource.getX(), resource.getY(), workerDistances,
                    returnDistances, FactoryFeudScenario.hasBalancedSymmetry(pgs)));
        }
        return audits;
    }

    private static List<Unit> units(PhysicalGameState pgs, int player, boolean workers) {
        List<Unit> selected = new ArrayList<>();
        for (Unit unit : pgs.getUnits()) {
            if (unit.getPlayer() != player) continue;
            if (workers && unit.getType().canHarvest) selected.add(unit);
            if (!workers && unit.getType().isStockpile) selected.add(unit);
        }
        Collections.sort(selected, Comparator.comparingLong(Unit::getID));
        return selected;
    }

    private static List<Integer> adjacentTerrain(PhysicalGameState pgs, Unit target) {
        List<Integer> positions = new ArrayList<>();
        for (int[] direction : DIRECTIONS) {
            int x = target.getX() + direction[0];
            int y = target.getY() + direction[1];
            if (x >= 0 && y >= 0 && x < pgs.getWidth() && y < pgs.getHeight()
                    && pgs.getTerrain(x, y) == PhysicalGameState.TERRAIN_NONE) {
                positions.add(x + y * pgs.getWidth());
            }
        }
        return positions;
    }

    private static int shortestBetween(PhysicalGameState pgs, List<Integer> starts,
            List<Integer> targets) {
        int best = Integer.MAX_VALUE;
        for (Integer start : starts) {
            int distance = shortestDistance(pgs, start, targets);
            if (distance >= 0) best = Math.min(best, distance);
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static int shortestDistance(PhysicalGameState pgs, int start,
            List<Integer> targets) {
        int size = pgs.getWidth() * pgs.getHeight();
        int[] distance = new int[size];
        Arrays.fill(distance, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        distance[start] = 0;
        queue.add(start);
        while (!queue.isEmpty()) {
            int position = queue.removeFirst();
            if (targets.contains(position)) return distance[position];
            int x = position % pgs.getWidth();
            int y = position / pgs.getWidth();
            for (int[] direction : DIRECTIONS) {
                int nextX = x + direction[0];
                int nextY = y + direction[1];
                if (nextX < 0 || nextY < 0 || nextX >= pgs.getWidth()
                        || nextY >= pgs.getHeight()) continue;
                int next = nextX + nextY * pgs.getWidth();
                if (distance[next] >= 0
                        || pgs.getTerrain(nextX, nextY) != PhysicalGameState.TERRAIN_NONE) {
                    continue;
                }
                distance[next] = distance[position] + 1;
                queue.add(next);
            }
        }
        return -1;
    }

    private static void writeReport(Path report, List<MapAudit> audits,
            List<FactoryFeudExperimentRecord> records) throws Exception {
        Path absolute = report.toAbsolutePath();
        if (absolute.getParent() != null) Files.createDirectories(absolute.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(absolute, StandardCharsets.UTF_8)) {
            writer.write("# Factory Feud controlled experiment — seed 584\n\n");
            writer.write("These are 20 deterministic, single-seed match outcomes, not "
                    + "independent randomized trials. Every match used 500 cycles, microRTS "
                    + "ORIGINAL_FINETUNED unit types, seed 584, and cancel-both movement "
                    + "conflict resolution. Each map/strategy pairing was side-swapped.\n\n");
            writer.write("## Map validation and route audit\n\n");
            writer.write("Distances are terrain-only shortest movement steps. Worker arrays "
                    + "are ordered by unit ID; return distance is the shortest cocoa-adjacent "
                    + "cell to factory-adjacent cell. Temporary unit occupancy is ignored.\n\n");
            writer.write("| Map | Size | Cocoa | P0 worker→cocoa | P1 worker→cocoa | "
                    + "P0 return | P1 return | Geometric symmetry |\n");
            writer.write("|---|---:|---:|---|---|---:|---:|---|\n");
            for (MapAudit audit : audits) writer.write(audit.tableRow());

            writer.write("\n## Match results\n\n");
            writer.write("| ID | Map | Strategy side | Cocoa P0–P1 | Result | Strategy "
                    + "workers alive/dead | Fixed workers alive/dead | Role changes | "
                    + "Overrides A/R | Cycles | Runtime ms |\n");
            writer.write("|---:|---|---|---:|---|---:|---:|---:|---:|---:|---:|\n");
            for (FactoryFeudExperimentRecord record : records) {
                writer.write(String.format(Locale.ROOT,
                        "| %d | %s | %s P%d | %d–%d | %s | %d/%d | %d/%d | %d | %d/%d | %d | %d |%n",
                        record.getMatchId(), mapName(record.getMap()),
                        displayStrategy(record.getAdaptiveStrategy()), record.getAdaptiveSide(),
                        record.getPlayer0Score(), record.getPlayer1Score(),
                        resultLetter(record), record.getAdaptiveSurvivingWorkers(),
                        record.getAdaptiveWorkerDeaths(), record.getFixedSurvivingWorkers(),
                        record.getFixedWorkerDeaths(), record.getRoleReassignments(),
                        record.getTacticalAttackOverrides(),
                        record.getTacticalRetreatOverrides(), record.getMatchCycles(),
                        record.getRuntimeMillis()));
            }

            writer.write("\n## Summary by map and strategy\n\n");
            writer.write("| Map | Strategy | Avg strategy cocoa | Avg Fixed cocoa | W/D/L | "
                    + "Worker deaths | Role changes | Overrides A/R |\n");
            writer.write("|---|---|---:|---:|---|---:|---:|---:|\n");
            Map<String, Aggregate> aggregates = aggregate(records);
            for (String map : MAPS) {
                for (FactoryFeudExperimentConfig.AdaptiveStrategy strategy : STRATEGIES) {
                    Aggregate aggregate = aggregates.get(key(map, strategy.getCommandLineName()));
                    writer.write(aggregate.tableRow(mapName(map),
                            displayStrategy(strategy.getCommandLineName())));
                }
            }

            writer.write("\n## Overall comparison\n\n");
            writer.write("| Strategy | Matches | Avg cocoa | Avg Fixed cocoa | W/D/L | "
                    + "Worker deaths | Avg worker deaths | Overrides A/R |\n");
            writer.write("|---|---:|---:|---:|---|---:|---:|---:|\n");
            for (FactoryFeudExperimentConfig.AdaptiveStrategy strategy : STRATEGIES) {
                Aggregate aggregate = new Aggregate();
                for (FactoryFeudExperimentRecord record : records) {
                    if (strategy.getCommandLineName().equals(record.getAdaptiveStrategy())) {
                        aggregate.add(record);
                    }
                }
                writer.write(aggregate.overallRow(displayStrategy(
                        strategy.getCommandLineName())));
            }

            writer.write("\n## ThreatAware change by map\n\n");
            writer.write("Positive cocoa delta and negative death delta favor ThreatAware.\n\n");
            writer.write("| Map | Δ avg cocoa | Δ worker deaths | Δ wins | Δ losses | Assessment |\n");
            writer.write("|---|---:|---:|---:|---:|---|\n");
            for (String map : MAPS) {
                Aggregate adaptive = aggregates.get(key(map, "adaptive"));
                Aggregate threat = aggregates.get(key(map, "threat-aware"));
                double cocoaDelta = threat.averageCocoa() - adaptive.averageCocoa();
                int deathDelta = threat.workerDeaths - adaptive.workerDeaths;
                int winDelta = threat.wins - adaptive.wins;
                int lossDelta = threat.losses - adaptive.losses;
                writer.write(String.format(Locale.ROOT,
                        "| %s | %+.2f | %+d | %+d | %+d | %s |%n",
                        mapName(map), cocoaDelta, deathDelta, winDelta, lossDelta,
                        assessment(cocoaDelta, deathDelta, winDelta, lossDelta)));
            }

            writer.write("\n## Observed tactical examples\n\n");
            boolean exampleFound = false;
            for (FactoryFeudExperimentRecord record : records) {
                if (!"threat-aware".equals(record.getAdaptiveStrategy())) continue;
                if (record.getExampleAttackOverride() != null) {
                    writer.write("- " + mapName(record.getMap()) + ", P"
                            + record.getAdaptiveSide() + " attack: `"
                            + record.getExampleAttackOverride() + "`\n");
                    exampleFound = true;
                }
                if (record.getExampleRetreatOverride() != null) {
                    writer.write("- " + mapName(record.getMap()) + ", P"
                            + record.getAdaptiveSide() + " retreat: `"
                            + record.getExampleRetreatOverride() + "`\n");
                    exampleFound = true;
                }
            }
            if (!exampleFound) writer.write("- No tactical override triggered.\n");
        }
    }

    private static Map<String, Aggregate> aggregate(
            List<FactoryFeudExperimentRecord> records) {
        Map<String, Aggregate> aggregates = new LinkedHashMap<>();
        for (FactoryFeudExperimentRecord record : records) {
            String key = key(record.getMap(), record.getAdaptiveStrategy());
            Aggregate aggregate = aggregates.get(key);
            if (aggregate == null) {
                aggregate = new Aggregate();
                aggregates.put(key, aggregate);
            }
            aggregate.add(record);
        }
        return aggregates;
    }

    private static String key(String map, String strategy) { return map + "\n" + strategy; }

    private static String resultLetter(FactoryFeudExperimentRecord record) {
        if (record.getOutcome() == FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN) return "W";
        if (record.getOutcome() == FactoryFeudExperimentRecord.Outcome.FIXED_WIN) return "L";
        return "D";
    }

    private static String displayStrategy(String strategy) {
        return "threat-aware".equals(strategy) ? "ThreatAware" : "Adaptive";
    }

    private static String mapName(String path) {
        return Paths.get(path).getFileName().toString();
    }

    private static String assessment(double cocoaDelta, int deathDelta, int winDelta,
            int lossDelta) {
        boolean noWorse = cocoaDelta >= 0 && deathDelta <= 0
                && winDelta >= 0 && lossDelta <= 0;
        boolean noBetter = cocoaDelta <= 0 && deathDelta >= 0
                && winDelta <= 0 && lossDelta >= 0;
        if (noWorse && (cocoaDelta > 0 || deathDelta < 0
                || winDelta > 0 || lossDelta < 0)) return "improves";
        if (noBetter && (cocoaDelta < 0 || deathDelta > 0
                || winDelta < 0 || lossDelta > 0)) return "regresses";
        if (!noWorse && !noBetter) return "mixed";
        return "unchanged";
    }

    static final class MapAudit {
        final String map;
        final int width;
        final int height;
        final int resourceX;
        final int resourceY;
        final int[][] workerDistances;
        final int[] returnDistances;
        final boolean symmetric;

        MapAudit(String map, int width, int height, int resourceX, int resourceY,
                int[][] workerDistances, int[] returnDistances, boolean symmetric) {
            this.map = map;
            this.width = width;
            this.height = height;
            this.resourceX = resourceX;
            this.resourceY = resourceY;
            this.workerDistances = workerDistances;
            this.returnDistances = returnDistances;
            this.symmetric = symmetric;
        }

        String tableRow() {
            return String.format(Locale.ROOT, "| %s | %d×%d | 120 @ %d,%d | %s | %s | %d | %d | %s |%n",
                    mapName(map), width, height, resourceX, resourceY,
                    Arrays.toString(workerDistances[0]), Arrays.toString(workerDistances[1]),
                    returnDistances[0], returnDistances[1], symmetric ? "yes" : "no");
        }
    }

    private static final class Aggregate {
        int matches;
        int cocoa;
        int fixedCocoa;
        int wins;
        int draws;
        int losses;
        int workerDeaths;
        int roleChanges;
        int attacks;
        int retreats;

        void add(FactoryFeudExperimentRecord record) {
            matches++;
            cocoa += record.getAdaptiveScore();
            fixedCocoa += record.getFixedScore();
            workerDeaths += record.getAdaptiveWorkerDeaths();
            roleChanges += record.getRoleReassignments();
            attacks += record.getTacticalAttackOverrides();
            retreats += record.getTacticalRetreatOverrides();
            if (record.getOutcome() == FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN) wins++;
            else if (record.getOutcome() == FactoryFeudExperimentRecord.Outcome.FIXED_WIN) losses++;
            else draws++;
        }

        double averageCocoa() { return cocoa / (double) matches; }

        String tableRow(String map, String strategy) {
            return String.format(Locale.ROOT,
                    "| %s | %s | %.2f | %.2f | %d/%d/%d | %d | %d | %d/%d |%n",
                    map, strategy, averageCocoa(), fixedCocoa / (double) matches,
                    wins, draws, losses, workerDeaths, roleChanges, attacks, retreats);
        }

        String overallRow(String strategy) {
            return String.format(Locale.ROOT,
                    "| %s | %d | %.2f | %.2f | %d/%d/%d | %d | %.2f | %d/%d |%n",
                    strategy, matches, averageCocoa(), fixedCocoa / (double) matches,
                    wins, draws, losses, workerDeaths, workerDeaths / (double) matches,
                    attacks, retreats);
        }
    }
}
