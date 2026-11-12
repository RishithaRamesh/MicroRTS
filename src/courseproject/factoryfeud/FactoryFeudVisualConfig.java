package courseproject.factoryfeud;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rts.GameState;
import rts.units.UnitTypeTable;

/** Immutable, headless-testable configuration for the Factory Feud visual runner. */
public final class FactoryFeudVisualConfig {
    private static final Pattern GENERATED_MAP_NAME = Pattern.compile(
            "^factory-feud-pcg-v[0-9]+-(open|obstacle-field|chokepoint)-"
                    + "([0-9]+)-(sparse|low|medium|high)-seed-[0-9]+-"
                    + "([0-9]+)x([0-9]+)\\.xml$",
            Pattern.CASE_INSENSITIVE);

    private final int timeLimit;
    private final int cycleDelayMs;
    private final String mapPath;
    private final boolean placeholderMode;
    private final FactoryFeudExperimentConfig.AdaptiveStrategy adaptiveStrategy;

    private FactoryFeudVisualConfig(
            int timeLimit, int cycleDelayMs, String mapPath, boolean placeholderMode,
            FactoryFeudExperimentConfig.AdaptiveStrategy adaptiveStrategy) {
        if (timeLimit <= 0) throw new IllegalArgumentException("time limit must be positive");
        if (cycleDelayMs < 0) {
            throw new IllegalArgumentException("cycle delay must be zero or greater");
        }
        if (mapPath == null || mapPath.trim().isEmpty()) {
            throw new IllegalArgumentException("map path cannot be blank");
        }
        this.timeLimit = timeLimit;
        this.cycleDelayMs = cycleDelayMs;
        this.mapPath = mapPath;
        this.placeholderMode = placeholderMode;
        this.adaptiveStrategy = adaptiveStrategy;
    }

    public static FactoryFeudVisualConfig parse(String[] args) {
        int timeLimit = args.length > 0
                ? Integer.parseInt(args[0]) : FactoryFeudVisual.DEFAULT_TIME_LIMIT;
        int cycleDelayMs = args.length > 1
                ? Integer.parseInt(args[1]) : FactoryFeudVisual.DEFAULT_CYCLE_DELAY_MS;
        String mapPath = FactoryFeudScenario.DEFAULT_MAP;
        boolean placeholderMode = false;
        FactoryFeudExperimentConfig.AdaptiveStrategy adaptiveStrategy =
                FactoryFeudExperimentConfig.AdaptiveStrategy.ADAPTIVE;

        if (args.length > 2) {
            // Retain compatibility with the former direct invocation whose third
            // argument was the placeholder-mode marker.
            if ("placeholder".equalsIgnoreCase(args[2])) placeholderMode = true;
            else mapPath = args[2];
        }
        if (args.length > 3) {
            if ("placeholder".equalsIgnoreCase(args[3])) placeholderMode = true;
            else adaptiveStrategy = FactoryFeudExperimentConfig.AdaptiveStrategy.parse(args[3]);
        }
        if (args.length > 4) {
            throw new IllegalArgumentException("Too many visual runner arguments");
        }
        return new FactoryFeudVisualConfig(
                timeLimit, cycleDelayMs, mapPath, placeholderMode, adaptiveStrategy);
    }

    /** Loads the selected path through the shared scenario validation API. */
    public GameState loadScenario(UnitTypeTable utt) throws Exception {
        return FactoryFeudScenario.load(mapPath, utt);
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getCycleDelayMs() {
        return cycleDelayMs;
    }

    public String getMapPath() {
        return mapPath;
    }

    public String getMapName() {
        return new File(mapPath).getName();
    }

    /** Returns a compact generated-map label or the existing handcrafted-map label. */
    public String getMapDisplayName(int mapWidth, int mapHeight) {
        String mapName = getMapName();
        Matcher generated = GENERATED_MAP_NAME.matcher(mapName);
        if (!generated.matches()) {
            return mapName + " (" + mapWidth + "x" + mapHeight + ")";
        }

        String family = generated.group(1).toUpperCase(Locale.ROOT).replace('-', '_');
        String density = generated.group(3).toLowerCase(Locale.ROOT);
        density = Character.toUpperCase(density.charAt(0)) + density.substring(1);
        return family + "-" + generated.group(2) + " | " + density + " | "
                + generated.group(4) + "\u00d7" + generated.group(5);
    }

    public boolean isPlaceholderMode() {
        return placeholderMode;
    }

    public FactoryFeudExperimentConfig.AdaptiveStrategy getAdaptiveStrategy() {
        return adaptiveStrategy;
    }
}
