package courseproject.factoryfeud.generation;

/** Immutable parameters for one deterministic Factory Feud map generation run. */
public final class FactoryFeudGeneratorConfig {
    public static final int MIN_WIDTH = 10;
    public static final int MAX_WIDTH = 64;
    public static final int MIN_HEIGHT = 8;
    public static final int MAX_HEIGHT = 64;
    public static final double MAX_OBSTACLE_DENSITY = 0.35;
    public static final int DEFAULT_MAX_ATTEMPTS = 50;

    private final int width;
    private final int height;
    private final double obstacleDensity;
    private final FactoryFeudMapFamily family;
    private final long seed;
    private final int maxAttempts;

    public FactoryFeudGeneratorConfig(int width, int height, double obstacleDensity,
            FactoryFeudMapFamily family, long seed) {
        this(width, height, obstacleDensity, family, seed, DEFAULT_MAX_ATTEMPTS);
    }

    public FactoryFeudGeneratorConfig(int width, int height, double obstacleDensity,
            FactoryFeudMapFamily family, long seed, int maxAttempts) {
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            throw new IllegalArgumentException("width must be between " + MIN_WIDTH
                    + " and " + MAX_WIDTH);
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            throw new IllegalArgumentException("height must be between " + MIN_HEIGHT
                    + " and " + MAX_HEIGHT);
        }
        if (Double.isNaN(obstacleDensity) || Double.isInfinite(obstacleDensity)
                || obstacleDensity < 0.0 || obstacleDensity > MAX_OBSTACLE_DENSITY) {
            throw new IllegalArgumentException("obstacle density must be between 0.0 and "
                    + MAX_OBSTACLE_DENSITY);
        }
        if (family == null) throw new IllegalArgumentException("map family cannot be null");
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maximum attempt count must be positive");
        }
        if (family == null) throw new IllegalArgumentException("map family cannot be null");
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maximum attempt count must be positive");
        }
        int targetObstacles = (int) Math.round(width * height * obstacleDensity);
        if (family == FactoryFeudMapFamily.CHOKEPOINT
                && targetObstacles < height - 2) {
            throw new IllegalArgumentException("CHOKEPOINT density must provide at least "
                    + (height - 2) + " wall cells for the separating barrier");
        }
        this.width = width;
        this.height = height;
        this.obstacleDensity = obstacleDensity;
        this.family = family;
        this.seed = seed;
        this.maxAttempts = maxAttempts;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public double getObstacleDensity() { return obstacleDensity; }
    public FactoryFeudMapFamily getFamily() { return family; }
    public long getSeed() { return seed; }
    public int getMaxAttempts() { return maxAttempts; }

    public int getTargetObstacleCount() {
        return (int) Math.round(width * height * obstacleDensity);
    }
}
