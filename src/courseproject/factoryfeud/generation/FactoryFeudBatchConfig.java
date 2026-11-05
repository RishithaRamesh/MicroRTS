package courseproject.factoryfeud.generation;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Immutable command-line configuration for the reproducible D2 map batch. */
public final class FactoryFeudBatchConfig {
    public static final int DEFAULT_MAPS_PER_FAMILY = 40;
    public static final int DEFAULT_WIDTH = 16;
    public static final int DEFAULT_HEIGHT = 12;
    public static final long DEFAULT_BASE_SEED = 584000L;
    public static final int DEFAULT_MAX_ATTEMPTS = 100;
    public static final Path DEFAULT_MAP_DIRECTORY =
            Paths.get("maps/factory-feud/generated");
    public static final Path DEFAULT_ANALYSIS_DIRECTORY =
            Paths.get("results/factory-feud-d2");

    private final int mapsPerFamily;
    private final int width;
    private final int height;
    private final long baseSeed;
    private final int maxAttempts;
    private final Path mapDirectory;
    private final Path analysisDirectory;

    public FactoryFeudBatchConfig(int mapsPerFamily, int width, int height,
            long baseSeed, int maxAttempts, Path mapDirectory,
            Path analysisDirectory) {
        if (mapsPerFamily <= 0) {
            throw new IllegalArgumentException("maps per family must be positive");
        }
        if (width < FactoryFeudGeneratorConfig.MIN_WIDTH
                || width > FactoryFeudGeneratorConfig.MAX_WIDTH) {
            throw new IllegalArgumentException("batch width is outside generator bounds");
        }
        if (height < FactoryFeudGeneratorConfig.MIN_HEIGHT
                || height > FactoryFeudGeneratorConfig.MAX_HEIGHT) {
            throw new IllegalArgumentException("batch height is outside generator bounds");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maximum attempt count must be positive");
        }
        if (mapDirectory == null || analysisDirectory == null) {
            throw new IllegalArgumentException("output directories cannot be null");
        }
        this.mapsPerFamily = mapsPerFamily;
        this.width = width;
        this.height = height;
        this.baseSeed = baseSeed;
        this.maxAttempts = maxAttempts;
        this.mapDirectory = mapDirectory;
        this.analysisDirectory = analysisDirectory;
    }

    public static FactoryFeudBatchConfig defaults() {
        return new FactoryFeudBatchConfig(DEFAULT_MAPS_PER_FAMILY,
                DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_BASE_SEED,
                DEFAULT_MAX_ATTEMPTS, DEFAULT_MAP_DIRECTORY,
                DEFAULT_ANALYSIS_DIRECTORY);
    }

    public static FactoryFeudBatchConfig parse(String[] args) {
        FactoryFeudBatchConfig defaults = defaults();
        int maps = defaults.mapsPerFamily;
        int width = defaults.width;
        int height = defaults.height;
        long seed = defaults.baseSeed;
        int attempts = defaults.maxAttempts;
        Path mapDirectory = defaults.mapDirectory;
        Path analysisDirectory = defaults.analysisDirectory;
        for (int index = 0; index < args.length; index++) {
            String option = args[index];
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            String value = args[++index];
            if ("--maps-per-family".equals(option)) maps = Integer.parseInt(value);
            else if ("--width".equals(option)) width = Integer.parseInt(value);
            else if ("--height".equals(option)) height = Integer.parseInt(value);
            else if ("--base-seed".equals(option)) seed = Long.parseLong(value);
            else if ("--max-attempts".equals(option)) attempts = Integer.parseInt(value);
            else if ("--map-dir".equals(option)) mapDirectory = Paths.get(value);
            else if ("--analysis-dir".equals(option)) analysisDirectory = Paths.get(value);
            else throw new IllegalArgumentException("Unknown option: " + option);
        }
        return new FactoryFeudBatchConfig(maps, width, height, seed, attempts,
                mapDirectory, analysisDirectory);
    }

    public int getMapsPerFamily() { return mapsPerFamily; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getBaseSeed() { return baseSeed; }
    public int getMaxAttempts() { return maxAttempts; }
    public Path getMapDirectory() { return mapDirectory; }
    public Path getAnalysisDirectory() { return analysisDirectory; }

    public int getTotalAcceptedMapCount() {
        return mapsPerFamily * FactoryFeudMapFamily.values().length;
    }
}
