package courseproject.factoryfeud;

import courseproject.factoryfeud.generation.FactoryFeudGeneratedMapValidator;
import courseproject.factoryfeud.generation.FactoryFeudMapFamily;
import courseproject.factoryfeud.generation.FactoryFeudDensityPreset;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import rts.GameState;
import rts.units.UnitTypeTable;

/** Reproducible Deliverable 3 evaluation over the accepted D2 map dataset. */
public final class FactoryFeudD3ExperimentRunner {
    public static final Path DEFAULT_METADATA = Paths.get(
            "results/factory-feud-d2/factory-feud-generated-maps.csv");
    public static final Path DEFAULT_OUTPUT_DIRECTORY = Paths.get("results/factory-feud-d3");
    public static final int DEFAULT_SELECTIONS_PER_PRESET = 2;
    public static final int DEFAULT_CYCLES = 500;
    public static final long DEFAULT_SIMULATION_SEED = 3584L;

    public static final String MANIFEST_HEADER = "selection_order,map_id,family,"
            + "density_preset,map_seed,output_path,actual_density,mean_path_length,"
            + "route_imbalance,dead_end_count";
    public static final String RESULT_HEADER = "match_id,experiment_type,map_id,family,"
            + "density_preset,map_seed,simulation_seed,map_path,p0_ai,p1_ai,adaptive_side,"
            + "p0_cocoa,p1_cocoa,fixed_cocoa,adaptive_cocoa,adaptive_minus_fixed_margin,"
            + "outcome,cycles_executed,remaining_cocoa,adaptive_surviving_workers,"
            + "fixed_surviving_workers,adaptive_worker_deaths,fixed_worker_deaths,"
            + "role_changes,tactical_counterattacks,tactical_retreats,total_tactical_overrides,"
            + "adaptive_first_delivery_cycle,adaptive_last_delivery_cycle,"
            + "adaptive_mean_delivery_interval_cycles,adaptive_completed_delivery_events,"
            + "adaptive_carrier_role_interruptions,adaptive_carrier_interrupted_cycles,"
            + "adaptive_carrier_attacks_received,adaptive_carrier_deaths,"
            + "adaptive_undelivered_cocoa_at_end,adaptive_carrier_correction_events,"
            + "fixed_carrier_role_interruptions,fixed_carrier_interrupted_cycles,"
            + "fixed_carrier_attacks_received,fixed_carrier_deaths,"
            + "fixed_undelivered_cocoa_at_end,fixed_carrier_correction_events,"
            + "actual_obstacle_density,"
            + "mean_terrain_shortest_path_length,route_imbalance,dead_end_count,runtime_ms,"
            + "role_change_history,tactical_override_history";
    public static final String FAILURE_HEADER = "match_id,experiment_type,map_id,"
            + "adaptive_side,simulation_seed,error_type,error_message";

    private FactoryFeudD3ExperimentRunner() { }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        Artifacts artifacts = run(config);
        System.out.println("manifest=" + artifacts.manifest.toAbsolutePath());
        System.out.println("results=" + artifacts.results.toAbsolutePath());
        System.out.println("failures=" + artifacts.failures.toAbsolutePath());
        System.out.println("summary=" + artifacts.summary.toAbsolutePath());
        System.out.println("selectedMaps=" + artifacts.selectedMaps.size()
                + " completedMatches=" + artifacts.records.size()
                + " failedMatches=" + artifacts.failedMatches);
        if (artifacts.failedMatches > 0) {
            throw new IllegalStateException(artifacts.failedMatches
                    + " D3 matches failed; see " + artifacts.failures);
        }
    }

    public static Artifacts run(Config config) throws Exception {
        List<MapMetadata> allMaps = readMetadata(config.metadata);
        List<MapMetadata> selected = selectBalanced(
                allMaps, config.selectionsPerPreset);
        validateSelectedMaps(selected);
        Files.createDirectories(config.outputDirectory);
        Path manifest = config.outputDirectory.resolve("selection-manifest.csv");
        Path results = config.outputDirectory.resolve("match-results.csv");
        Path failures = config.outputDirectory.resolve("match-failures.csv");
        Path summary = config.outputDirectory.resolve("summary.md");
        writeManifest(manifest, selected);

        List<D3Record> records = new ArrayList<>();
        List<FailureRecord> failureRecords = new ArrayList<>();
        int matchId = 1;
        for (int mapIndex = 0; mapIndex < selected.size(); mapIndex++) {
            MapMetadata map = selected.get(mapIndex);
            long simulationSeed = config.simulationSeed + mapIndex;
            matchId = runPair(records, failureRecords, matchId, "MAIN", map,
                    simulationSeed, config.cycles,
                    FactoryFeudExperimentConfig.AdaptiveStrategy.ADAPTIVE);
            matchId = runPair(records, failureRecords, matchId, "ABLATION", map,
                    simulationSeed, config.cycles,
                    FactoryFeudExperimentConfig.AdaptiveStrategy.OLD_ADAPTIVE);
        }
        writeResults(results, records);
        writeFailures(failures, failureRecords);
        Summary statistics = Summary.compute(records, selected.size(), failureRecords.size());
        writeSummary(summary, config, selected, statistics);
        return new Artifacts(manifest, results, failures, summary, selected, records,
                failureRecords.size(), statistics);
    }

    private static int runPair(List<D3Record> records, List<FailureRecord> failures,
            int firstMatchId, String experiment, MapMetadata map, long seed, int cycles,
            FactoryFeudExperimentConfig.AdaptiveStrategy strategy) {
        int matchId = firstMatchId;
        // Required ordering: Fixed P0 / adaptive P1, followed by the side swap.
        int[] adaptiveSides = {1, 0};
        for (int adaptiveSide : adaptiveSides) {
            try {
                FactoryFeudExperimentConfig matchConfig = new FactoryFeudExperimentConfig(
                        2, cycles, map.outputPath, seed,
                        Paths.get("results/factory-feud-d3/.unused.csv"), strategy);
                FactoryFeudExperimentRunner.MatchExecution execution =
                        FactoryFeudExperimentRunner.executeMatch(
                                matchId, adaptiveSide, seed, matchConfig);
                records.add(new D3Record(matchId, experiment, map, execution));
            } catch (Exception failure) {
                failures.add(new FailureRecord(matchId, experiment, map.mapId,
                        adaptiveSide, seed, failure));
            }
            matchId++;
        }
        return matchId;
    }

    public static List<MapMetadata> readMetadata(Path csv) throws Exception {
        if (csv == null || !Files.isRegularFile(csv)) {
            throw new IllegalArgumentException("D2 accepted-map CSV does not exist: " + csv);
        }
        List<MapMetadata> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("D2 CSV is empty: " + csv);
            List<String> header = parseCsvRow(headerLine);
            String[] required = {"map_id", "family", "density_preset", "map_seed",
                "actual_density", "player0_path_length", "player1_path_length",
                "route_imbalance", "dead_end_count", "output_path"};
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (int index = 0; index < header.size(); index++) columns.put(header.get(index), index);
            for (String name : required) {
                if (!columns.containsKey(name)) {
                    throw new IllegalArgumentException("D2 CSV missing required column: " + name);
                }
            }
            String line;
            int lineNumber = 1;
            Set<String> ids = new HashSet<>();
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                List<String> cells = parseCsvRow(line);
                try {
                    MapMetadata record = new MapMetadata(
                            cell(cells, columns, "map_id"),
                            FactoryFeudMapFamily.valueOf(cell(cells, columns, "family")),
                            FactoryFeudDensityPreset.valueOf(
                                    cell(cells, columns, "density_preset")),
                            Long.parseLong(cell(cells, columns, "map_seed")),
                            cell(cells, columns, "output_path"),
                            Double.parseDouble(cell(cells, columns, "actual_density")),
                            Integer.parseInt(cell(cells, columns, "player0_path_length")),
                            Integer.parseInt(cell(cells, columns, "player1_path_length")),
                            Integer.parseInt(cell(cells, columns, "route_imbalance")),
                            Integer.parseInt(cell(cells, columns, "dead_end_count")));
                    if (!ids.add(record.mapId)) {
                        throw new IllegalArgumentException("duplicate map_id " + record.mapId);
                    }
                    records.add(record);
                } catch (RuntimeException invalid) {
                    throw new IllegalArgumentException("Invalid D2 CSV row " + lineNumber
                            + ": " + invalid.getMessage(), invalid);
                }
            }
        }
        if (records.isEmpty()) throw new IllegalArgumentException("D2 CSV contains no maps: " + csv);
        return Collections.unmodifiableList(records);
    }

    public static List<MapMetadata> selectBalanced(List<MapMetadata> maps,
            int selectionsPerPreset) {
        if (maps == null) throw new IllegalArgumentException("maps cannot be null");
        if (selectionsPerPreset <= 0) {
            throw new IllegalArgumentException("selections per preset must be positive");
        }
        List<MapMetadata> sorted = new ArrayList<>(maps);
        Collections.sort(sorted, Comparator.comparing(MapMetadata::getMapId));
        List<MapMetadata> selected = new ArrayList<>();
        for (FactoryFeudMapFamily family : FactoryFeudMapFamily.values()) {
            for (FactoryFeudDensityPreset preset : FactoryFeudDensityPreset.values()) {
                int found = 0;
                for (MapMetadata map : sorted) {
                    if (map.family == family && map.preset == preset
                            && found < selectionsPerPreset) {
                        selected.add(map);
                        found++;
                    }
                }
                if (found != selectionsPerPreset) {
                    throw new IllegalArgumentException("Need " + selectionsPerPreset + " maps for "
                            + family + "/" + preset + " but found " + found);
                }
            }
        }
        Set<String> distinct = new HashSet<>();
        for (MapMetadata map : selected) distinct.add(map.mapId);
        if (distinct.size() != selected.size()) {
            throw new IllegalStateException("Balanced selection contains duplicate maps");
        }
        return Collections.unmodifiableList(selected);
    }

    public static void validateSelectedMaps(List<MapMetadata> selected) throws Exception {
        UnitTypeTable utt = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        for (MapMetadata map : selected) {
            if (!Files.isRegularFile(Paths.get(map.outputPath))) {
                throw new IllegalArgumentException("Selected XML is missing: " + map.outputPath);
            }
            GameState state = FactoryFeudScenario.load(map.outputPath, utt);
            FactoryFeudGeneratedMapValidator.validate(state.getPhysicalGameState());
        }
    }

    private static String cell(List<String> cells, Map<String, Integer> columns, String name) {
        int index = columns.get(name);
        if (index >= cells.size() || cells.get(index).trim().isEmpty()) {
            throw new IllegalArgumentException("blank " + name);
        }
        return cells.get(index).trim();
    }

    static List<String> parseCsvRow(String row) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < row.length(); index++) {
            char current = row.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < row.length() && row.charAt(index + 1) == '"') {
                    cell.append('"');
                    index++;
                } else quoted = !quoted;
            } else if (current == ',' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else cell.append(current);
        }
        if (quoted) throw new IllegalArgumentException("unterminated CSV quote");
        cells.add(cell.toString());
        return cells;
    }

    private static void writeManifest(Path path, List<MapMetadata> maps) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(MANIFEST_HEADER);
            writer.newLine();
            for (int index = 0; index < maps.size(); index++) {
                writer.write(maps.get(index).toManifestRow(index + 1));
                writer.newLine();
            }
        }
    }

    private static void writeResults(Path path, List<D3Record> records) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(RESULT_HEADER);
            writer.newLine();
            for (D3Record record : records) {
                writer.write(record.toCsv());
                writer.newLine();
            }
        }
    }

    private static void writeFailures(Path path, List<FailureRecord> failures) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(FAILURE_HEADER);
            writer.newLine();
            for (FailureRecord failure : failures) {
                writer.write(failure.toCsv());
                writer.newLine();
            }
        }
    }

    private static void writeSummary(Path path, Config config, List<MapMetadata> selected,
            Summary summary) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("# Factory Feud Deliverable 3 results\n\n");
            writer.write("## Reproducibility\n\n");
            writer.write("- D2 metadata: `" + config.metadata + "`\n");
            writer.write("- Selection rule: lexicographically first "
                    + config.selectionsPerPreset + " map IDs in every family/preset stratum\n");
            writer.write("- Selected maps: " + selected.size() + "\n");
            writer.write("- Simulation seed: base `" + config.simulationSeed
                    + "` plus zero-based manifest index; paired sides and both experiments share it\n");
            writer.write("- Match duration: " + config.cycles + " cycles\n");
            writer.write("- Unit table: `ORIGINAL_FINETUNED`; movement conflicts: `cancel-both`\n");
            writer.write("- Completed/expected matches: " + summary.completedMatches + "/"
                    + summary.expectedMatches + "; failures: " + summary.failedMatches + "\n\n");
            writer.write("The tactical ablation is controlled: `AdaptiveRoleAI` inherits all "
                    + "strategic allocation from `OldAdaptiveRoleAI` and overrides only role "
                    + "execution with immediate counterattack/retreat logic.\n\n");

            writer.write("## H1 — overall performance\n\n");
            writer.write(String.format(Locale.ROOT,
                    "Main comparison W/D/L: **%d/%d/%d**. Strict win percentage: **%.2f%%**; "
                    + "draw-adjusted score: %.2f%%. Mean/median tactical cocoa: %.3f/%.3f; "
                    + "mean/median fixed cocoa: %.3f/%.3f; mean margin: %.3f. "
                    + "Threshold 70%%: **%s**.\n\n",
                    summary.main.wins, summary.main.draws, summary.main.losses,
                    100.0 * summary.main.strictWinRate(),
                    100.0 * summary.main.drawAdjustedRate(), summary.main.meanAdaptive(),
                    summary.main.medianAdaptive(), summary.main.meanFixed(),
                    summary.main.medianFixed(), summary.main.meanMargin(),
                    summary.main.strictWinRate() >= 0.70 ? "SUPPORTED" : "NOT SUPPORTED"));

            writer.write("## H2 — map complexity\n\n");
            writer.write("| Family | Main matches | Mean adaptive−fixed cocoa |\n");
            writer.write("|---|---:|---:|\n");
            for (FactoryFeudMapFamily family : FactoryFeudMapFamily.values()) {
                Aggregate aggregate = summary.mainByFamily.get(family);
                writer.write(String.format(Locale.ROOT, "| %s | %d | %.3f |%n",
                        family, aggregate.count(), aggregate.meanMargin()));
            }
            writer.write(String.format(Locale.ROOT,
                    "\nCHOKEPOINT minus OPEN mean margin: **%.3f cocoa**. Threshold 2 cocoa: **%s**.\n\n",
                    summary.h2Difference,
                    summary.h2Difference >= 2.0 ? "SUPPORTED" : "NOT SUPPORTED"));
            writer.write("These family comparisons are descriptive. OPEN uses systematically lower "
                    + "density presets, so family and obstacle density are confounded; the D2 path "
                    + "length is a center-to-center terrain proxy, not an exact Worker route. "
                    + "The Python analysis aggregates swapped sides to one observation per map "
                    + "before reporting terrain associations.\n\n");

            writer.write("## H3 — tactical effectiveness\n\n");
            writer.write(String.format(Locale.ROOT,
                    "Mean tactical/non-tactical cocoa: **%.3f/%.3f**. Percentage improvement: "
                    + "**%s**. Threshold 15%%: **%s**. Tactical W/D/L: %d/%d/%d; "
                    + "non-tactical W/D/L: %d/%d/%d. Mean surviving Workers: %.3f/%.3f; "
                    + "mean deaths: %.3f/%.3f; mean role changes: %.3f/%.3f. "
                    + "Tactical counterattacks/retreats: %d/%d.\n",
                    summary.main.meanAdaptive(), summary.ablation.meanAdaptive(),
                    summary.h3Improvement == null ? "undefined (zero non-tactical mean)"
                            : String.format(Locale.ROOT, "%.2f%%", summary.h3Improvement),
                    summary.h3Improvement != null && summary.h3Improvement >= 15.0
                            ? "SUPPORTED" : "NOT SUPPORTED",
                    summary.main.wins, summary.main.draws, summary.main.losses,
                    summary.ablation.wins, summary.ablation.draws, summary.ablation.losses,
                    summary.main.meanAdaptiveSurvivors(), summary.ablation.meanAdaptiveSurvivors(),
                    summary.main.meanAdaptiveDeaths(), summary.ablation.meanAdaptiveDeaths(),
                    summary.main.meanRoleChanges(), summary.ablation.meanRoleChanges(),
                    summary.main.totalCounterattacks, summary.main.totalRetreats));
        }
    }

    private static String csv(String value) {
        if (value == null) return "";
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    public static final class Config {
        final Path metadata;
        final Path outputDirectory;
        final int selectionsPerPreset;
        final int cycles;
        final long simulationSeed;

        public Config(Path metadata, Path outputDirectory, int selectionsPerPreset,
                int cycles, long simulationSeed) {
            if (metadata == null || outputDirectory == null) {
                throw new IllegalArgumentException("metadata and output directory are required");
            }
            if (selectionsPerPreset <= 0 || cycles <= 0) {
                throw new IllegalArgumentException("selection count and cycles must be positive");
            }
            this.metadata = metadata;
            this.outputDirectory = outputDirectory;
            this.selectionsPerPreset = selectionsPerPreset;
            this.cycles = cycles;
            this.simulationSeed = simulationSeed;
        }

        public static Config defaults() {
            return new Config(DEFAULT_METADATA, DEFAULT_OUTPUT_DIRECTORY,
                    DEFAULT_SELECTIONS_PER_PRESET, DEFAULT_CYCLES, DEFAULT_SIMULATION_SEED);
        }

        public static Config parse(String[] args) {
            Config defaults = defaults();
            Path metadata = defaults.metadata;
            Path output = defaults.outputDirectory;
            int selections = defaults.selectionsPerPreset;
            int cycles = defaults.cycles;
            long seed = defaults.simulationSeed;
            for (int index = 0; index < args.length; index++) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for " + args[index]);
                }
                String option = args[index];
                String value = args[++index];
                if ("--metadata".equals(option)) metadata = Paths.get(value);
                else if ("--output-dir".equals(option)) output = Paths.get(value);
                else if ("--selections-per-preset".equals(option)) selections = Integer.parseInt(value);
                else if ("--cycles".equals(option)) cycles = Integer.parseInt(value);
                else if ("--seed".equals(option)) seed = Long.parseLong(value);
                else throw new IllegalArgumentException("Unknown option: " + option);
            }
            return new Config(metadata, output, selections, cycles, seed);
        }
    }

    public static final class MapMetadata {
        final String mapId;
        final FactoryFeudMapFamily family;
        final FactoryFeudDensityPreset preset;
        final long mapSeed;
        final String outputPath;
        final double actualDensity;
        final int player0Path;
        final int player1Path;
        final int routeImbalance;
        final int deadEnds;

        MapMetadata(String mapId, FactoryFeudMapFamily family,
                FactoryFeudDensityPreset preset, long mapSeed, String outputPath,
                double actualDensity, int player0Path, int player1Path,
                int routeImbalance, int deadEnds) {
            this.mapId = mapId;
            this.family = family;
            this.preset = preset;
            this.mapSeed = mapSeed;
            this.outputPath = outputPath;
            this.actualDensity = actualDensity;
            this.player0Path = player0Path;
            this.player1Path = player1Path;
            this.routeImbalance = routeImbalance;
            this.deadEnds = deadEnds;
        }

        public String getMapId() { return mapId; }
        public FactoryFeudMapFamily getFamily() { return family; }
        public FactoryFeudDensityPreset getPreset() { return preset; }
        public long getMapSeed() { return mapSeed; }
        public String getOutputPath() { return outputPath; }
        public double getActualDensity() { return actualDensity; }
        public double getMeanPathLength() { return (player0Path + player1Path) / 2.0; }
        public int getRouteImbalance() { return routeImbalance; }
        public int getDeadEnds() { return deadEnds; }

        String toManifestRow(int order) {
            return order + "," + csv(mapId) + "," + family + "," + preset + ","
                    + mapSeed + "," + csv(outputPath) + ","
                    + String.format(Locale.ROOT, "%.8f,%.3f", actualDensity,
                            getMeanPathLength()) + "," + routeImbalance + "," + deadEnds;
        }
    }

    public static final class D3Record {
        final int matchId;
        final String experiment;
        final MapMetadata map;
        final FactoryFeudExperimentRecord match;
        final int remainingCocoa;
        final List<String> roleChangeHistory;
        final List<String> tacticalOverrideHistory;

        D3Record(int matchId, String experiment, MapMetadata map,
                FactoryFeudExperimentRunner.MatchExecution execution) {
            this.matchId = matchId;
            this.experiment = experiment;
            this.map = map;
            this.match = execution.record;
            this.remainingCocoa = execution.remainingCocoa;
            this.roleChangeHistory = execution.roleChangeHistory;
            this.tacticalOverrideHistory = execution.tacticalOverrideHistory;
        }

        public int getMargin() { return match.getAdaptiveScore() - match.getFixedScore(); }
        public String getExperiment() { return experiment; }
        public FactoryFeudExperimentRecord getMatch() { return match; }
        public MapMetadata getMap() { return map; }

        String toCsv() {
            String adaptiveName = "MAIN".equals(experiment)
                    ? "AdaptiveRoleAI" : "OldAdaptiveRoleAI";
            String p0 = match.getAdaptiveSide() == 0 ? adaptiveName : "FixedRoleAI";
            String p1 = match.getAdaptiveSide() == 1 ? adaptiveName : "FixedRoleAI";
            FactoryFeudExperimentRecord.CarrierMetrics carrier = match.getAdaptiveCarrier();
            FactoryFeudExperimentRecord.CarrierMetrics fixedCarrier = match.getFixedCarrier();
            FactoryFeudExperimentRecord.DeliveryMetrics delivery = match.getAdaptiveSide() == 0
                    ? match.getPlayer0Delivery() : match.getPlayer1Delivery();
            return matchId + "," + experiment + "," + csv(map.mapId) + "," + map.family
                    + "," + map.preset + "," + map.mapSeed + "," + match.getSeed()
                    + "," + csv(map.outputPath) + "," + p0 + "," + p1 + ","
                    + match.getAdaptiveSide() + "," + match.getPlayer0Score() + ","
                    + match.getPlayer1Score() + "," + match.getFixedScore() + ","
                    + match.getAdaptiveScore() + "," + getMargin() + ","
                    + outcomeLabel(match.getOutcome()) + "," + match.getCompletedCycles() + ","
                    + remainingCocoa + "," + match.getAdaptiveSurvivingWorkers() + ","
                    + match.getFixedSurvivingWorkers() + "," + match.getAdaptiveWorkerDeaths()
                    + "," + match.getFixedWorkerDeaths() + "," + match.getRoleReassignments()
                    + "," + match.getTacticalAttackOverrides() + ","
                    + match.getTacticalRetreatOverrides() + "," + match.getTacticalOverrides()
                    + "," + nullable(delivery.getFirstCycle())
                    + "," + nullable(delivery.getLastCycle())
                    + "," + nullable(delivery.getMeanInterval())
                    + "," + nullable(delivery.getCompletedEvents())
                    + "," + nullable(carrier == null ? null : carrier.getRoleInterruptions())
                    + "," + nullable(carrier == null ? null : carrier.getInterruptedCycles())
                    + "," + nullable(carrier == null ? null : carrier.getAttacksReceived())
                    + "," + nullable(carrier == null ? null : carrier.getDeaths())
                    + "," + nullable(carrier == null ? null : carrier.getUndeliveredCocoaAtEnd())
                    + "," + nullable(carrier == null ? null : carrier.getCorrections())
                    + "," + nullable(fixedCarrier == null ? null : fixedCarrier.getRoleInterruptions())
                    + "," + nullable(fixedCarrier == null ? null : fixedCarrier.getInterruptedCycles())
                    + "," + nullable(fixedCarrier == null ? null : fixedCarrier.getAttacksReceived())
                    + "," + nullable(fixedCarrier == null ? null : fixedCarrier.getDeaths())
                    + "," + nullable(fixedCarrier == null ? null : fixedCarrier.getUndeliveredCocoaAtEnd())
                    + "," + nullable(fixedCarrier == null ? null : fixedCarrier.getCorrections())
                    + "," + String.format(Locale.ROOT, "%.8f,%.3f", map.actualDensity,
                            map.getMeanPathLength())
                    + "," + map.routeImbalance + "," + map.deadEnds + ","
                    + match.getRuntimeMillis() + "," + csv(joinHistory(roleChangeHistory))
                    + "," + csv(joinHistory(tacticalOverrideHistory));
        }

        private static String nullable(Object value) { return value == null ? "" : value.toString(); }
        private static String joinHistory(List<String> events) {
            StringBuilder joined = new StringBuilder();
            for (String event : events) {
                if (joined.length() > 0) joined.append(';');
                joined.append(event);
            }
            return joined.toString();
        }
        private static String outcomeLabel(FactoryFeudExperimentRecord.Outcome outcome) {
            if (outcome == FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN) return "WIN";
            if (outcome == FactoryFeudExperimentRecord.Outcome.FIXED_WIN) return "LOSS";
            return "DRAW";
        }
    }

    static final class FailureRecord {
        final int matchId;
        final String experiment;
        final String mapId;
        final int side;
        final long seed;
        final Exception failure;

        FailureRecord(int matchId, String experiment, String mapId, int side,
                long seed, Exception failure) {
            this.matchId = matchId;
            this.experiment = experiment;
            this.mapId = mapId;
            this.side = side;
            this.seed = seed;
            this.failure = failure;
        }

        String toCsv() {
            return matchId + "," + experiment + "," + csv(mapId) + "," + side + ","
                    + seed + "," + csv(failure.getClass().getName()) + ","
                    + csv(failure.getMessage());
        }
    }

    public static final class Aggregate {
        final List<Integer> adaptiveScores = new ArrayList<>();
        final List<Integer> fixedScores = new ArrayList<>();
        int wins;
        int draws;
        int losses;
        int survivorTotal;
        int deathTotal;
        int roleChangeTotal;
        int totalCounterattacks;
        int totalRetreats;

        void add(D3Record record) {
            FactoryFeudExperimentRecord match = record.match;
            adaptiveScores.add(match.getAdaptiveScore());
            fixedScores.add(match.getFixedScore());
            if (match.getOutcome() == FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN) wins++;
            else if (match.getOutcome() == FactoryFeudExperimentRecord.Outcome.FIXED_WIN) losses++;
            else draws++;
            survivorTotal += match.getAdaptiveSurvivingWorkers();
            deathTotal += match.getAdaptiveWorkerDeaths();
            roleChangeTotal += match.getRoleReassignments();
            totalCounterattacks += match.getTacticalAttackOverrides();
            totalRetreats += match.getTacticalRetreatOverrides();
        }

        public int count() { return adaptiveScores.size(); }
        public double strictWinRate() { return count() == 0 ? 0 : wins / (double) count(); }
        public double drawAdjustedRate() {
            return count() == 0 ? 0 : (wins + 0.5 * draws) / count();
        }
        public double meanAdaptive() { return mean(adaptiveScores); }
        public double meanFixed() { return mean(fixedScores); }
        public double medianAdaptive() { return median(adaptiveScores); }
        public double medianFixed() { return median(fixedScores); }
        public double meanMargin() { return meanAdaptive() - meanFixed(); }
        public double meanAdaptiveSurvivors() { return ratio(survivorTotal, count()); }
        public double meanAdaptiveDeaths() { return ratio(deathTotal, count()); }
        public double meanRoleChanges() { return ratio(roleChangeTotal, count()); }
    }

    public static final class Summary {
        final int expectedMatches;
        final int completedMatches;
        final int failedMatches;
        final Aggregate main;
        final Aggregate ablation;
        final Map<FactoryFeudMapFamily, Aggregate> mainByFamily;
        final double h2Difference;
        final Double h3Improvement;

        Summary(int expectedMatches, int completedMatches, int failedMatches,
                Aggregate main, Aggregate ablation,
                Map<FactoryFeudMapFamily, Aggregate> mainByFamily,
                double h2Difference, Double h3Improvement) {
            this.expectedMatches = expectedMatches;
            this.completedMatches = completedMatches;
            this.failedMatches = failedMatches;
            this.main = main;
            this.ablation = ablation;
            this.mainByFamily = mainByFamily;
            this.h2Difference = h2Difference;
            this.h3Improvement = h3Improvement;
        }

        public static Summary compute(List<D3Record> records, int selectedMaps,
                int failures) {
            Aggregate main = new Aggregate();
            Aggregate ablation = new Aggregate();
            Map<FactoryFeudMapFamily, Aggregate> byFamily =
                    new EnumMap<>(FactoryFeudMapFamily.class);
            for (FactoryFeudMapFamily family : FactoryFeudMapFamily.values()) {
                byFamily.put(family, new Aggregate());
            }
            for (D3Record record : records) {
                if ("MAIN".equals(record.experiment)) {
                    main.add(record);
                    byFamily.get(record.map.family).add(record);
                } else if ("ABLATION".equals(record.experiment)) ablation.add(record);
            }
            double h2 = byFamily.get(FactoryFeudMapFamily.CHOKEPOINT).meanMargin()
                    - byFamily.get(FactoryFeudMapFamily.OPEN).meanMargin();
            Double h3 = percentageImprovement(main.meanAdaptive(), ablation.meanAdaptive());
            return new Summary(selectedMaps * 4, records.size(), failures, main,
                    ablation, Collections.unmodifiableMap(byFamily), h2, h3);
        }

        public Aggregate getMain() { return main; }
        public Aggregate getAblation() { return ablation; }
        public double getH2Difference() { return h2Difference; }
        public Double getH3Improvement() { return h3Improvement; }
    }

    public static Double percentageImprovement(double tacticalMean,
            double nonTacticalMean) {
        if (nonTacticalMean == 0.0) return null;
        return 100.0 * (tacticalMean - nonTacticalMean) / nonTacticalMean;
    }

    private static double mean(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        long total = 0;
        for (Integer value : values) total += value;
        return total / (double) values.size();
    }

    private static double median(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) return sorted.get(middle);
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private static double ratio(int total, int count) {
        return count == 0 ? 0.0 : total / (double) count;
    }

    public static final class Artifacts {
        public final Path manifest;
        public final Path results;
        public final Path failures;
        public final Path summary;
        public final List<MapMetadata> selectedMaps;
        public final List<D3Record> records;
        public final int failedMatches;
        public final Summary statistics;

        Artifacts(Path manifest, Path results, Path failures, Path summary,
                List<MapMetadata> selectedMaps, List<D3Record> records,
                int failedMatches, Summary statistics) {
            this.manifest = manifest;
            this.results = results;
            this.failures = failures;
            this.summary = summary;
            this.selectedMaps = Collections.unmodifiableList(new ArrayList<>(selectedMaps));
            this.records = Collections.unmodifiableList(new ArrayList<>(records));
            this.failedMatches = failedMatches;
            this.statistics = statistics;
        }
    }
}
