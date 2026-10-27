package courseproject.factoryfeud;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

/** Discovers, validates, and evaluates every Factory Feud map with paired sides. */
public final class FactoryFeudControlledExperimentRunner {
    public static final int DEFAULT_CYCLES = 500;
    public static final long DEFAULT_SEED = 584L;
    public static final Path MAP_DIRECTORY = Paths.get("maps/factory-feud");
    public static final Path DEFAULT_OUTPUT_DIRECTORY = Paths.get("results");

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS", Locale.ROOT);
    private static final int[][] DIRECTIONS = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};

    private FactoryFeudControlledExperimentRunner() {
    }

    public static void main(String[] args) throws Exception {
        EvaluationArtifacts artifacts = run(EvaluationConfig.parse(args));
        System.out.println("csv=" + artifacts.csv.toAbsolutePath());
        System.out.println("report=" + artifacts.report.toAbsolutePath());
        System.out.println("maps=" + artifacts.maps.size()
                + " matches=" + artifacts.records.size()
                + " cycles=" + artifacts.cycles + " seed=" + artifacts.seed
                + " unitTypes=ORIGINAL_FINETUNED conflictPolicy=cancel-both");
    }

    static EvaluationArtifacts run(EvaluationConfig config) throws Exception {
        List<String> maps = discoverMaps(MAP_DIRECTORY);
        List<MapAudit> audits = auditMaps(maps);
        Files.createDirectories(config.outputDirectory);
        Path[] outputs = uniqueOutputPaths(config.outputDirectory, config.seed, config.cycles,
                LocalDateTime.now().format(TIMESTAMP));
        List<FactoryFeudExperimentRecord> records = runMatches(
                outputs[0], maps, config.cycles, config.seed);
        writeReport(outputs[1], audits, records, config.cycles, config.seed);
        return new EvaluationArtifacts(outputs[0], outputs[1], maps, records,
                config.cycles, config.seed);
    }

    /** Returns direct .xml children only, sorted by filename for stable execution. */
    public static List<String> discoverMaps(Path directory) throws Exception {
        if (directory == null || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException(
                    "Factory Feud map directory does not exist: " + directory);
        }
        List<Path> discovered = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                if (Files.isRegularFile(entry)
                        && entry.getFileName().toString().endsWith(".xml")) {
                    discovered.add(entry);
                }
            }
        }
        Collections.sort(discovered, Comparator.comparing(
                path -> path.getFileName().toString()));
        if (discovered.isEmpty()) {
            throw new IllegalArgumentException(
                    "No .xml maps found directly inside " + directory);
        }
        List<String> maps = new ArrayList<>();
        for (Path path : discovered) maps.add(path.toString());
        return Collections.unmodifiableList(maps);
    }

    static List<FactoryFeudExperimentRecord> runMatches(Path csv, List<String> maps,
            int cycles, long seed) throws Exception {
        if (cycles <= 0) throw new IllegalArgumentException("cycles must be positive");
        List<FactoryFeudExperimentRecord> records = new ArrayList<>();
        int matchId = 1;
        for (String map : maps) {
            FactoryFeudExperimentConfig config = new FactoryFeudExperimentConfig(
                    2, cycles, map, seed, csv,
                    FactoryFeudExperimentConfig.AdaptiveStrategy.ADAPTIVE);
            // Required ordering: Fixed P0 vs Adaptive P1, then the side swap.
            records.add(FactoryFeudExperimentRunner.runMatch(
                    matchId++, 1, seed, config));
            records.add(FactoryFeudExperimentRunner.runMatch(
                    matchId++, 0, seed, config));
        }
        FactoryFeudExperimentRunner.writeCsv(csv, records);
        return Collections.unmodifiableList(records);
    }

    static List<MapAudit> auditMaps(List<String> maps) throws Exception {
        UnitTypeTable utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        List<MapAudit> audits = new ArrayList<>();
        for (String map : maps) {
            try {
                audits.add(auditMap(map, FactoryFeudScenario.load(map, utt)));
            } catch (Exception failure) {
                throw new IllegalArgumentException("Invalid Factory Feud map " + map
                        + ": " + failure.getMessage(), failure);
            }
        }
        return Collections.unmodifiableList(audits);
    }

    private static MapAudit auditMap(String map, GameState state) {
        PhysicalGameState pgs = state.getPhysicalGameState();
        Unit resource = null;
        int resourceCount = 0;
        int cocoa = 0;
        int totalWorkers = 0;
        int totalFactories = 0;
        int[] workers = new int[FactoryFeudScenario.PLAYER_COUNT];
        int[] factories = new int[FactoryFeudScenario.PLAYER_COUNT];
        for (Unit unit : pgs.getUnits()) {
            if (unit.getType().isResource) {
                resource = unit;
                resourceCount++;
                cocoa += unit.getResources();
            } else {
                if ("Worker".equals(unit.getType().name)) {
                    totalWorkers++;
                    if (unit.getPlayer() < 0
                            || unit.getPlayer() >= FactoryFeudScenario.PLAYER_COUNT) {
                        throw new IllegalArgumentException(
                                "every Worker must belong to player 0 or player 1");
                    }
                    workers[unit.getPlayer()]++;
                    if (unit.getHitPoints() != 1) {
                        throw new IllegalArgumentException("Worker " + unit.getID()
                                + " must start at exactly 1 HP");
                    }
                }
                if (unit.getType().isStockpile) {
                    totalFactories++;
                    if (unit.getPlayer() < 0
                            || unit.getPlayer() >= FactoryFeudScenario.PLAYER_COUNT) {
                        throw new IllegalArgumentException(
                                "every factory must belong to player 0 or player 1");
                    }
                    factories[unit.getPlayer()]++;
                }
            }
        }
        if (resourceCount != 1 || cocoa != 120) {
            throw new IllegalArgumentException(
                    "expected one shared resource containing exactly 120 cocoa");
        }
        if (totalFactories != 2 || totalWorkers != 8) {
            throw new IllegalArgumentException(
                    "expected exactly two factories and eight Workers total");
        }
        for (int player = 0; player < FactoryFeudScenario.PLAYER_COUNT; player++) {
            if (workers[player] != 4 || factories[player] != 1) {
                throw new IllegalArgumentException("player " + player
                        + " must have exactly four Workers and one factory");
            }
        }

        int[][] workerDistances = new int[2][];
        int[] returnDistances = new int[2];
        List<Integer> resourceApproaches = adjacentTerrain(pgs, resource);
        for (int player = 0; player < 2; player++) {
            List<Unit> playerWorkers = units(pgs, player, true);
            workerDistances[player] = new int[playerWorkers.size()];
            for (int index = 0; index < playerWorkers.size(); index++) {
                Unit worker = playerWorkers.get(index);
                workerDistances[player][index] = shortestDistance(
                        pgs, worker.getX() + worker.getY() * pgs.getWidth(),
                        resourceApproaches);
                if (workerDistances[player][index] < 0) {
                    throw new IllegalArgumentException("player " + player + " worker "
                            + worker.getID() + " cannot reach cocoa");
                }
            }
            Unit factory = units(pgs, player, false).get(0);
            returnDistances[player] = shortestBetween(
                    pgs, resourceApproaches, adjacentTerrain(pgs, factory));
            if (returnDistances[player] < 0) {
                throw new IllegalArgumentException(
                        "player " + player + " cannot return cocoa to its factory");
            }
        }
        return new MapAudit(map, pgs.getWidth(), pgs.getHeight(),
                resource.getX(), resource.getY(), workerDistances,
                returnDistances, FactoryFeudScenario.hasBalancedSymmetry(pgs));
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
                        || pgs.getTerrain(nextX, nextY)
                                != PhysicalGameState.TERRAIN_NONE) continue;
                distance[next] = distance[position] + 1;
                queue.add(next);
            }
        }
        return -1;
    }

    private static Path[] uniqueOutputPaths(Path directory, long seed, int cycles,
            String timestamp) {
        String prefix = "factory-feud-evaluation-seed-" + seed
                + "-cycles-" + cycles + "-" + timestamp;
        int suffix = 0;
        while (true) {
            String base = suffix == 0 ? prefix : prefix + "-" + suffix;
            Path csv = directory.resolve(base + ".csv");
            Path report = directory.resolve(base + ".md");
            if (!Files.exists(csv) && !Files.exists(report)) return new Path[]{csv, report};
            suffix++;
        }
    }

    private static void writeReport(Path report, List<MapAudit> audits,
            List<FactoryFeudExperimentRecord> records, int cycles, long seed)
            throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(
                report, StandardCharsets.UTF_8)) {
            writer.write("# Factory Feud evaluation\n\n## Configuration\n\n");
            writer.write("- Seed: `" + seed + "`\n");
            writer.write("- Match duration: `" + cycles + "` cycles\n");
            writer.write("- Unit types: `ORIGINAL_FINETUNED`\n");
            writer.write("- Movement conflict policy: `cancel-both`\n");
            writer.write("- Pair order: FixedRoleAI P0 vs AdaptiveRoleAI P1, then side swap\n");
            writer.write("\n## Maps tested\n\n");
            for (MapAudit audit : audits) writer.write("- `" + mapName(audit.map) + "`\n");

            writer.write("\n## Map validation and route audit\n\n");
            writer.write("| Map | Size | Cocoa | P0 worker→cocoa | P1 worker→cocoa | "
                    + "P0 return | P1 return | Geometric symmetry |\n");
            writer.write("|---|---:|---:|---|---|---:|---:|---|\n");
            for (MapAudit audit : audits) writer.write(audit.tableRow());

            writer.write("\n## Match results\n\n");
            writer.write("| ID | Map | P0 AI | P1 AI | Fixed cocoa | Adaptive cocoa | "
                    + "Adaptive result | Adaptive workers alive/dead | Fixed workers "
                    + "alive/dead | Role changes | Counterattacks/retreats | "
                    + "Completed/configured cycles | Runtime ms |\n");
            writer.write("|---:|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|\n");
            for (FactoryFeudExperimentRecord record : records) {
                String player0 = record.getAdaptiveSide() == 0
                        ? "AdaptiveRoleAI" : "FixedRoleAI";
                String player1 = record.getAdaptiveSide() == 1
                        ? "AdaptiveRoleAI" : "FixedRoleAI";
                writer.write(String.format(Locale.ROOT,
                        "| %d | %s | %s | %s | %d | %d | %s | %d/%d | %d/%d | %d | %d/%d | %d/%d | %d |%n",
                        record.getMatchId(), mapName(record.getMap()), player0, player1,
                        record.getFixedScore(), record.getAdaptiveScore(),
                        adaptiveResultLabel(record), record.getAdaptiveSurvivingWorkers(),
                        record.getAdaptiveWorkerDeaths(), record.getFixedSurvivingWorkers(),
                        record.getFixedWorkerDeaths(), record.getRoleReassignments(),
                        record.getTacticalAttackOverrides(),
                        record.getTacticalRetreatOverrides(), record.getCompletedCycles(),
                        record.getConfiguredCycles(), record.getRuntimeMillis()));
            }

            Map<String, Aggregate> byMap = aggregate(records);
            writer.write("\n## Summary by map\n\n");
            writer.write("| Map | Matches | Avg Adaptive cocoa | Avg Fixed cocoa | "
                    + "Adaptive W/D/L | Adaptive worker deaths | Role changes | "
                    + "Counterattacks/retreats |\n");
            writer.write("|---|---:|---:|---:|---|---:|---:|---:|\n");
            Aggregate overall = new Aggregate();
            for (MapAudit audit : audits) {
                writer.write(byMap.get(audit.map).tableRow(mapName(audit.map)));
            }
            for (FactoryFeudExperimentRecord record : records) overall.add(record);
            writer.write("\n## Overall\n\n");
            writer.write("| Matches | Avg Adaptive cocoa | Avg Fixed cocoa | Adaptive W/D/L | "
                    + "Adaptive worker deaths | Role changes | Counterattacks/retreats |\n");
            writer.write("|---:|---:|---:|---|---:|---:|---:|\n");
            writer.write(overall.overallRow());
        }
    }

    private static Map<String, Aggregate> aggregate(
            List<FactoryFeudExperimentRecord> records) {
        Map<String, Aggregate> aggregates = new LinkedHashMap<>();
        for (FactoryFeudExperimentRecord record : records) {
            Aggregate aggregate = aggregates.get(record.getMap());
            if (aggregate == null) {
                aggregate = new Aggregate();
                aggregates.put(record.getMap(), aggregate);
            }
            aggregate.add(record);
        }
        return aggregates;
    }

    static String adaptiveResultLabel(FactoryFeudExperimentRecord record) {
        if (record.getOutcome() == FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN) {
            return "WIN";
        }
        if (record.getOutcome() == FactoryFeudExperimentRecord.Outcome.FIXED_WIN) {
            return "LOSS";
        }
        return "DRAW";
    }

    private static String mapName(String path) {
        return Paths.get(path).getFileName().toString();
    }

    static final class EvaluationConfig {
        final int cycles;
        final long seed;
        final Path outputDirectory;

        EvaluationConfig(int cycles, long seed, Path outputDirectory) {
            if (cycles <= 0) throw new IllegalArgumentException("cycles must be positive");
            if (outputDirectory == null) {
                throw new IllegalArgumentException("output directory cannot be null");
            }
            this.cycles = cycles;
            this.seed = seed;
            this.outputDirectory = outputDirectory;
        }

        static EvaluationConfig parse(String[] args) {
            int cycles = DEFAULT_CYCLES;
            long seed = DEFAULT_SEED;
            Path output = DEFAULT_OUTPUT_DIRECTORY;
            for (int index = 0; index < args.length; index++) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for " + args[index]);
                }
                String option = args[index];
                String value = args[++index];
                if ("--cycles".equals(option)) cycles = Integer.parseInt(value);
                else if ("--seed".equals(option)) seed = Long.parseLong(value);
                else if ("--output-dir".equals(option)) output = Paths.get(value);
                else throw new IllegalArgumentException("Unknown option: " + option);
            }
            return new EvaluationConfig(cycles, seed, output);
        }
    }

    static final class EvaluationArtifacts {
        final Path csv;
        final Path report;
        final List<String> maps;
        final List<FactoryFeudExperimentRecord> records;
        final int cycles;
        final long seed;

        EvaluationArtifacts(Path csv, Path report, List<String> maps,
                List<FactoryFeudExperimentRecord> records, int cycles, long seed) {
            this.csv = csv;
            this.report = report;
            this.maps = maps;
            this.records = records;
            this.cycles = cycles;
            this.seed = seed;
        }
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
            return String.format(Locale.ROOT,
                    "| %s | %d×%d | 120 @ %d,%d | %s | %s | %d | %d | %s |%n",
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
            else if (record.getOutcome() == FactoryFeudExperimentRecord.Outcome.FIXED_WIN) {
                losses++;
            } else draws++;
        }

        private double averageAdaptiveCocoa() { return cocoa / (double) matches; }
        private double averageFixedCocoa() { return fixedCocoa / (double) matches; }

        String tableRow(String map) {
            return String.format(Locale.ROOT,
                    "| %s | %d | %.2f | %.2f | %d/%d/%d | %d | %d | %d/%d |%n",
                    map, matches, averageAdaptiveCocoa(), averageFixedCocoa(),
                    wins, draws, losses, workerDeaths, roleChanges, attacks, retreats);
        }

        String overallRow() {
            return String.format(Locale.ROOT,
                    "| %d | %.2f | %.2f | %d/%d/%d | %d | %d | %d/%d |%n",
                    matches, averageAdaptiveCocoa(), averageFixedCocoa(),
                    wins, draws, losses, workerDeaths, roleChanges, attacks, retreats);
        }
    }
}
