package courseproject.factoryfeud.generation;

import courseproject.factoryfeud.FactoryFeudScenario;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import util.XMLWriter;

/** Generates, audits, serializes, reloads, and reports a reproducible D2 map batch. */
public final class FactoryFeudBatchGenerator {
    public static final String ACCEPTED_CSV = "factory-feud-generated-maps.csv";
    public static final String ATTEMPTS_CSV = "factory-feud-generation-attempts.csv";
    public static final String VALIDITY_CSV = "factory-feud-validity-summary.csv";

    public static final String ACCEPTED_HEADER = "map_id,family,density_preset,map_seed,"
            + "width,height,requested_density,actual_density,player0_path_length,"
            + "player1_path_length,route_imbalance,dead_end_count,generation_attempts,"
            + "output_path";
    public static final String ATTEMPTS_HEADER = "map_id,family,density_preset,map_seed,"
            + "attempt_number,attempt_seed,valid,rejection_reason,actual_density,"
            + "player0_path_length,player1_path_length,route_imbalance,dead_end_count";
    public static final String VALIDITY_HEADER = "group,accepted_maps,total_attempts,"
            + "valid_attempts,invalid_attempts,raw_validity_rate";

    interface GeneratorService {
        FactoryFeudGeneratedMap generate(FactoryFeudGeneratorConfig config,
                UnitTypeTable utt) throws FactoryFeudGenerationException;
    }

    private FactoryFeudBatchGenerator() {
    }

    public static void main(String[] args) throws Exception {
        BatchResult result = run(FactoryFeudBatchConfig.parse(args));
        System.out.println("acceptedMaps=" + result.getAcceptedMaps().size());
        System.out.println("rawAttempts=" + result.getRawAttempts().size());
        System.out.println("validAttempts=" + result.getOverallStatistics().getValidAttempts());
        System.out.println("invalidAttempts="
                + result.getOverallStatistics().getInvalidAttempts());
        System.out.println(String.format(Locale.ROOT, "rawValidityRate=%.6f",
                result.getOverallStatistics().getRawValidityRate()));
        System.out.println("mapsCsv=" + result.getAcceptedCsv().toAbsolutePath());
        System.out.println("attemptsCsv=" + result.getAttemptsCsv().toAbsolutePath());
        System.out.println("validityCsv=" + result.getValidityCsv().toAbsolutePath());
    }

    public static BatchResult run(FactoryFeudBatchConfig config) throws Exception {
        return run(config, (generatorConfig, utt) ->
                new FactoryFeudMapGenerator(generatorConfig, utt).generate());
    }

    static BatchResult run(FactoryFeudBatchConfig config, GeneratorService generatorService)
            throws Exception {
        if (config == null) throw new IllegalArgumentException("batch config cannot be null");
        if (generatorService == null) {
            throw new IllegalArgumentException("generator service cannot be null");
        }
        Files.createDirectories(config.getMapDirectory());
        Files.createDirectories(config.getAnalysisDirectory());
        Path acceptedCsv = config.getAnalysisDirectory().resolve(ACCEPTED_CSV);
        Path attemptsCsv = config.getAnalysisDirectory().resolve(ATTEMPTS_CSV);
        Path validityCsv = config.getAnalysisDirectory().resolve(VALIDITY_CSV);

        UnitTypeTable utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        List<AcceptedMapRecord> accepted = new ArrayList<>();
        List<RawAttemptRecord> rawAttempts = new ArrayList<>();
        FactoryFeudDensityPreset[] presets = FactoryFeudDensityPreset.values();

        for (FactoryFeudMapFamily family : FactoryFeudMapFamily.values()) {
            for (int index = 0; index < config.getMapsPerFamily(); index++) {
                FactoryFeudDensityPreset preset = presets[index % presets.length];
                long mapSeed = mapSeed(config.getBaseSeed(), family, index);
                FactoryFeudGeneratorConfig generatorConfig = new FactoryFeudGeneratorConfig(
                        config.getWidth(), config.getHeight(), preset.densityFor(family),
                        family, mapSeed, config.getMaxAttempts());
                String mapId = mapId(family, index, preset, mapSeed);
                FactoryFeudGeneratedMap generated;
                try {
                    generated = generatorService.generate(generatorConfig, utt);
                } catch (FactoryFeudGenerationException failure) {
                    appendAttempts(rawAttempts, mapId, generatorConfig, preset,
                            failure.getAttempts());
                    writeCsvOutputs(acceptedCsv, attemptsCsv, validityCsv,
                            accepted, rawAttempts);
                    throw failure;
                }
                appendAttempts(rawAttempts, mapId, generatorConfig, preset,
                        generated.getAttempts());

                Path output = config.getMapDirectory().resolve(filename(
                        family, index, preset, mapSeed, config.getWidth(), config.getHeight()));
                exportXmlWithoutChangingExisting(output, generated.getPhysicalGameState());
                GameState reloaded = FactoryFeudScenario.load(output.toString(), utt);
                FactoryFeudGeneratedMapValidator.validate(reloaded.getPhysicalGameState());
                if (!sameMap(generated.getPhysicalGameState(),
                        reloaded.getPhysicalGameState())) {
                    throw new IllegalStateException("XML round trip changed generated map " + output);
                }
                accepted.add(new AcceptedMapRecord(mapId, family, preset, mapSeed,
                        generatorConfig, generated.getMetrics(), generated.getAttempts().size(),
                        output));
            }
        }

        writeCsvOutputs(acceptedCsv, attemptsCsv, validityCsv, accepted, rawAttempts);
        return new BatchResult(accepted, rawAttempts, statistics(accepted, rawAttempts),
                acceptedCsv, attemptsCsv, validityCsv);
    }

    private static long mapSeed(long baseSeed, FactoryFeudMapFamily family, int index) {
        return baseSeed + family.ordinal() * 1_000_000L + index;
    }

    private static String mapId(FactoryFeudMapFamily family, int index,
            FactoryFeudDensityPreset preset, long seed) {
        return String.format(Locale.ROOT, "%s-%03d-%s-seed-%s",
                family.name().toLowerCase(Locale.ROOT).replace('_', '-'), index + 1,
                preset.name().toLowerCase(Locale.ROOT), safeSeed(seed));
    }

    private static String filename(FactoryFeudMapFamily family, int index,
            FactoryFeudDensityPreset preset, long seed, int width, int height) {
        return "factory-feud-pcg-v1-" + mapId(family, index, preset, seed)
                + "-" + width + "x" + height + ".xml";
    }

    private static String safeSeed(long seed) {
        return seed < 0 ? "n" + Long.toString(seed).substring(1) : Long.toString(seed);
    }

    private static void appendAttempts(List<RawAttemptRecord> rows, String mapId,
            FactoryFeudGeneratorConfig config, FactoryFeudDensityPreset preset,
            List<FactoryFeudGenerationAttempt> attempts) {
        for (FactoryFeudGenerationAttempt attempt : attempts) {
            rows.add(new RawAttemptRecord(mapId, config.getFamily(), preset,
                    config.getSeed(), attempt));
        }
    }

    private static void exportXmlWithoutChangingExisting(Path output,
            PhysicalGameState pgs) throws Exception {
        StringWriter text = new StringWriter();
        XMLWriter writer = new XMLWriter(text);
        pgs.toxml(writer);
        writer.flush();
        byte[] xml = text.toString().getBytes(StandardCharsets.UTF_8);
        if (Files.exists(output)) {
            byte[] existing = Files.readAllBytes(output);
            if (!java.util.Arrays.equals(existing, xml)) {
                throw new IllegalStateException("Refusing to overwrite different generated map: "
                        + output);
            }
            return;
        }
        Files.write(output, xml);
    }

    static boolean sameMap(PhysicalGameState first, PhysicalGameState second) {
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()
                || first.getPlayers().size() != second.getPlayers().size()
                || first.getUnits().size() != second.getUnits().size()) return false;
        for (int player = 0; player < first.getPlayers().size(); player++) {
            if (first.getPlayer(player).getResources()
                    != second.getPlayer(player).getResources()) return false;
        }
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getTerrain(x, y) != second.getTerrain(x, y)) return false;
            }
        }
        Map<Long, Unit> secondById = new HashMap<>();
        for (Unit unit : second.getUnits()) secondById.put(unit.getID(), unit);
        for (Unit unit : first.getUnits()) {
            Unit other = secondById.get(unit.getID());
            if (other == null || unit.getPlayer() != other.getPlayer()
                    || !unit.getType().name.equals(other.getType().name)
                    || unit.getX() != other.getX() || unit.getY() != other.getY()
                    || unit.getResources() != other.getResources()
                    || unit.getHitPoints() != other.getHitPoints()) return false;
        }
        return true;
    }

    private static void writeCsvOutputs(Path acceptedCsv, Path attemptsCsv,
            Path validityCsv, List<AcceptedMapRecord> accepted,
            List<RawAttemptRecord> rawAttempts) throws Exception {
        List<String> acceptedLines = new ArrayList<>();
        acceptedLines.add(ACCEPTED_HEADER);
        for (AcceptedMapRecord record : accepted) acceptedLines.add(record.toCsv());
        Files.write(acceptedCsv, acceptedLines, StandardCharsets.UTF_8);

        List<String> attemptLines = new ArrayList<>();
        attemptLines.add(ATTEMPTS_HEADER);
        for (RawAttemptRecord attempt : rawAttempts) attemptLines.add(attempt.toCsv());
        Files.write(attemptsCsv, attemptLines, StandardCharsets.UTF_8);

        List<String> validityLines = new ArrayList<>();
        validityLines.add(VALIDITY_HEADER);
        Map<String, ValidityStatistics> summaries = statistics(accepted, rawAttempts);
        for (FactoryFeudMapFamily family : FactoryFeudMapFamily.values()) {
            validityLines.add(summaries.get(family.name()).toCsv(family.name()));
        }
        validityLines.add(summaries.get("OVERALL").toCsv("OVERALL"));
        Files.write(validityCsv, validityLines, StandardCharsets.UTF_8);
    }

    private static Map<String, ValidityStatistics> statistics(
            List<AcceptedMapRecord> accepted, List<RawAttemptRecord> attempts) {
        Map<String, ValidityStatistics> statistics = new java.util.LinkedHashMap<>();
        for (FactoryFeudMapFamily family : FactoryFeudMapFamily.values()) {
            int acceptedCount = 0;
            int valid = 0;
            int invalid = 0;
            for (AcceptedMapRecord record : accepted) {
                if (record.getFamily() == family) acceptedCount++;
            }
            for (RawAttemptRecord attempt : attempts) {
                if (attempt.getFamily() != family) continue;
                if (attempt.isValid()) valid++;
                else invalid++;
            }
            statistics.put(family.name(),
                    new ValidityStatistics(acceptedCount, valid, invalid));
        }
        int acceptedCount = accepted.size();
        int valid = 0;
        int invalid = 0;
        for (RawAttemptRecord attempt : attempts) {
            if (attempt.isValid()) valid++;
            else invalid++;
        }
        statistics.put("OVERALL", new ValidityStatistics(acceptedCount, valid, invalid));
        return Collections.unmodifiableMap(statistics);
    }

    private static String csv(String value) {
        if (value == null) return "";
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    public static final class AcceptedMapRecord {
        private final String mapId;
        private final FactoryFeudMapFamily family;
        private final FactoryFeudDensityPreset preset;
        private final long mapSeed;
        private final FactoryFeudGeneratorConfig config;
        private final FactoryFeudMapMetrics metrics;
        private final int generationAttempts;
        private final Path outputPath;

        AcceptedMapRecord(String mapId, FactoryFeudMapFamily family,
                FactoryFeudDensityPreset preset, long mapSeed,
                FactoryFeudGeneratorConfig config, FactoryFeudMapMetrics metrics,
                int generationAttempts, Path outputPath) {
            this.mapId = mapId;
            this.family = family;
            this.preset = preset;
            this.mapSeed = mapSeed;
            this.config = config;
            this.metrics = metrics;
            this.generationAttempts = generationAttempts;
            this.outputPath = outputPath;
        }

        String toCsv() {
            return csv(mapId) + ',' + family + ',' + preset + ',' + mapSeed + ','
                    + config.getWidth() + ',' + config.getHeight() + ','
                    + format(config.getObstacleDensity()) + ','
                    + format(metrics.getActualObstacleDensity()) + ','
                    + metrics.getPlayer0FactoryToCocoaPath() + ','
                    + metrics.getPlayer1FactoryToCocoaPath() + ','
                    + metrics.getRouteDifference() + ',' + metrics.getDeadEndCount() + ','
                    + generationAttempts + ',' + csv(outputPath.toString());
        }

        public String getMapId() { return mapId; }
        public FactoryFeudMapFamily getFamily() { return family; }
        public FactoryFeudDensityPreset getPreset() { return preset; }
        public long getMapSeed() { return mapSeed; }
        public FactoryFeudGeneratorConfig getConfig() { return config; }
        public FactoryFeudMapMetrics getMetrics() { return metrics; }
        public int getGenerationAttempts() { return generationAttempts; }
        public Path getOutputPath() { return outputPath; }
    }

    public static final class RawAttemptRecord {
        private final String mapId;
        private final FactoryFeudMapFamily family;
        private final FactoryFeudDensityPreset preset;
        private final long mapSeed;
        private final FactoryFeudGenerationAttempt attempt;

        RawAttemptRecord(String mapId, FactoryFeudMapFamily family,
                FactoryFeudDensityPreset preset, long mapSeed,
                FactoryFeudGenerationAttempt attempt) {
            this.mapId = mapId;
            this.family = family;
            this.preset = preset;
            this.mapSeed = mapSeed;
            this.attempt = attempt;
        }

        String toCsv() {
            FactoryFeudMapMetrics metrics = attempt.getMetrics();
            return csv(mapId) + ',' + family + ',' + preset + ',' + mapSeed + ','
                    + attempt.getAttemptNumber() + ',' + attempt.getAttemptSeed() + ','
                    + attempt.passedValidation() + ','
                    + csv(attempt.getValidationFailure()) + ','
                    + format(metrics.getActualObstacleDensity()) + ','
                    + metrics.getPlayer0FactoryToCocoaPath() + ','
                    + metrics.getPlayer1FactoryToCocoaPath() + ','
                    + metrics.getRouteDifference() + ',' + metrics.getDeadEndCount();
        }

        public String getMapId() { return mapId; }
        public FactoryFeudMapFamily getFamily() { return family; }
        public FactoryFeudDensityPreset getPreset() { return preset; }
        public long getMapSeed() { return mapSeed; }
        public FactoryFeudGenerationAttempt getAttempt() { return attempt; }
        public boolean isValid() { return attempt.passedValidation(); }
    }

    public static final class ValidityStatistics {
        private final int acceptedMaps;
        private final int validAttempts;
        private final int invalidAttempts;

        ValidityStatistics(int acceptedMaps, int validAttempts, int invalidAttempts) {
            this.acceptedMaps = acceptedMaps;
            this.validAttempts = validAttempts;
            this.invalidAttempts = invalidAttempts;
        }

        String toCsv(String group) {
            return group + ',' + acceptedMaps + ',' + getTotalAttempts() + ','
                    + validAttempts + ',' + invalidAttempts + ','
                    + format(getRawValidityRate());
        }

        public int getAcceptedMaps() { return acceptedMaps; }
        public int getValidAttempts() { return validAttempts; }
        public int getInvalidAttempts() { return invalidAttempts; }
        public int getTotalAttempts() { return validAttempts + invalidAttempts; }
        public double getRawValidityRate() {
            return getTotalAttempts() == 0 ? 0.0
                    : validAttempts / (double) getTotalAttempts();
        }
    }

    public static final class BatchResult {
        private final List<AcceptedMapRecord> acceptedMaps;
        private final List<RawAttemptRecord> rawAttempts;
        private final Map<String, ValidityStatistics> statistics;
        private final Path acceptedCsv;
        private final Path attemptsCsv;
        private final Path validityCsv;

        BatchResult(List<AcceptedMapRecord> acceptedMaps,
                List<RawAttemptRecord> rawAttempts,
                Map<String, ValidityStatistics> statistics,
                Path acceptedCsv, Path attemptsCsv, Path validityCsv) {
            this.acceptedMaps = Collections.unmodifiableList(new ArrayList<>(acceptedMaps));
            this.rawAttempts = Collections.unmodifiableList(new ArrayList<>(rawAttempts));
            this.statistics = statistics;
            this.acceptedCsv = acceptedCsv;
            this.attemptsCsv = attemptsCsv;
            this.validityCsv = validityCsv;
        }

        public List<AcceptedMapRecord> getAcceptedMaps() { return acceptedMaps; }
        public List<RawAttemptRecord> getRawAttempts() { return rawAttempts; }
        public Map<String, ValidityStatistics> getStatistics() { return statistics; }
        public ValidityStatistics getOverallStatistics() { return statistics.get("OVERALL"); }
        public Path getAcceptedCsv() { return acceptedCsv; }
        public Path getAttemptsCsv() { return attemptsCsv; }
        public Path getValidityCsv() { return validityCsv; }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.8f", value);
    }
}
