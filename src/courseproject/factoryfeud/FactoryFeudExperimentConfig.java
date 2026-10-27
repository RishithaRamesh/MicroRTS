package courseproject.factoryfeud;

import java.nio.file.Path;
import java.nio.file.Paths;
import rts.units.UnitTypeTable;

/** Immutable command-line configuration for paired Factory Feud experiments. */
public final class FactoryFeudExperimentConfig {
    public enum AdaptiveStrategy {
        ADAPTIVE("adaptive"),
        OLD_ADAPTIVE("old-adaptive"),
        CARGO_SAFE("cargo-safe"),
        THREAT_AWARE("threat-aware");

        private final String commandLineName;

        AdaptiveStrategy(String commandLineName) {
            this.commandLineName = commandLineName;
        }

        public String getCommandLineName() { return commandLineName; }

        OldAdaptiveRoleAI create(UnitTypeTable utt, int matchDuration) {
            if (this == OLD_ADAPTIVE) return new OldAdaptiveRoleAI(utt, matchDuration);
            if (this == CARGO_SAFE) return new CargoSafeAdaptiveRoleAI(utt, matchDuration);
            return new AdaptiveRoleAI(utt, matchDuration);
        }

        static AdaptiveStrategy parse(String value) {
            for (AdaptiveStrategy strategy : values()) {
                if (strategy.commandLineName.equals(value)) return strategy;
            }
            throw new IllegalArgumentException(
                    "adaptive strategy must be 'adaptive', 'old-adaptive', "
                            + "'cargo-safe', or 'threat-aware'");
        }
    }

    public static final int DEFAULT_MATCH_COUNT = 20;
    public static final int DEFAULT_CYCLES = 500;
    public static final long DEFAULT_SEED = 584L;
    public static final String DEFAULT_CSV = "results/factory-feud-experiments.csv";

    private final int matchCount;
    private final int cycles;
    private final String mapPath;
    private final long seed;
    private final Path csvPath;
    private final AdaptiveStrategy adaptiveStrategy;

    public FactoryFeudExperimentConfig(
            int matchCount, int cycles, String mapPath, long seed, Path csvPath) {
        this(matchCount, cycles, mapPath, seed, csvPath, AdaptiveStrategy.ADAPTIVE);
    }

    public FactoryFeudExperimentConfig(int matchCount, int cycles, String mapPath,
            long seed, Path csvPath, AdaptiveStrategy adaptiveStrategy) {
        if (matchCount <= 0 || matchCount % 2 != 0) {
            throw new IllegalArgumentException("match count must be a positive even number");
        }
        if (cycles <= 0) throw new IllegalArgumentException("cycles must be positive");
        if (mapPath == null || mapPath.trim().isEmpty()) {
            throw new IllegalArgumentException("map path cannot be blank");
        }
        if (csvPath == null) throw new IllegalArgumentException("CSV path cannot be null");
        if (adaptiveStrategy == null) {
            throw new IllegalArgumentException("adaptive strategy cannot be null");
        }
        this.matchCount = matchCount;
        this.cycles = cycles;
        this.mapPath = mapPath;
        this.seed = seed;
        this.csvPath = csvPath;
        this.adaptiveStrategy = adaptiveStrategy;
    }

    public static FactoryFeudExperimentConfig defaults() {
        return new FactoryFeudExperimentConfig(DEFAULT_MATCH_COUNT, DEFAULT_CYCLES,
                FactoryFeudScenario.DEFAULT_MAP, DEFAULT_SEED, Paths.get(DEFAULT_CSV));
    }

    public static FactoryFeudExperimentConfig parse(String[] args) {
        FactoryFeudExperimentConfig defaults = defaults();
        int matches = defaults.matchCount;
        int cycles = defaults.cycles;
        String map = defaults.mapPath;
        long seed = defaults.seed;
        Path csv = defaults.csvPath;
        AdaptiveStrategy strategy = defaults.adaptiveStrategy;

        for (int index = 0; index < args.length; index++) {
            String option = args[index];
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            String value = args[++index];
            if ("--matches".equals(option)) matches = Integer.parseInt(value);
            else if ("--cycles".equals(option)) cycles = Integer.parseInt(value);
            else if ("--map".equals(option)) map = value;
            else if ("--seed".equals(option)) seed = Long.parseLong(value);
            else if ("--csv".equals(option)) csv = Paths.get(value);
            else if ("--adaptive-strategy".equals(option)) strategy = AdaptiveStrategy.parse(value);
            else throw new IllegalArgumentException("Unknown option: " + option);
        }
        return new FactoryFeudExperimentConfig(matches, cycles, map, seed, csv, strategy);
    }

    public int getMatchCount() {
        return matchCount;
    }

    public int getCycles() {
        return cycles;
    }

    public String getMapPath() {
        return mapPath;
    }

    public long getSeed() {
        return seed;
    }

    public Path getCsvPath() {
        return csvPath;
    }

    public AdaptiveStrategy getAdaptiveStrategy() {
        return adaptiveStrategy;
    }
}
