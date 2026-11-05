package courseproject.factoryfeud.generation;

/** Four documented obstacle-density levels, interpreted per map family. */
public enum FactoryFeudDensityPreset {
    SPARSE(0.05, 0.10, 0.10),
    LOW(0.08, 0.14, 0.14),
    MEDIUM(0.11, 0.18, 0.18),
    HIGH(0.14, 0.22, 0.22);

    private final double openDensity;
    private final double obstacleFieldDensity;
    private final double chokepointDensity;

    FactoryFeudDensityPreset(double openDensity, double obstacleFieldDensity,
            double chokepointDensity) {
        this.openDensity = openDensity;
        this.obstacleFieldDensity = obstacleFieldDensity;
        this.chokepointDensity = chokepointDensity;
    }

    public double densityFor(FactoryFeudMapFamily family) {
        if (family == null) throw new IllegalArgumentException("map family cannot be null");
        switch (family) {
            case OPEN:
                return openDensity;
            case OBSTACLE_FIELD:
                return obstacleFieldDensity;
            case CHOKEPOINT:
                return chokepointDensity;
            default:
                throw new IllegalArgumentException("Unsupported map family: " + family);
        }
    }
}
