package courseproject.factoryfeud.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

/** Deterministic, retrying procedural map generator for Factory Feud D2. */
public final class FactoryFeudMapGenerator {
    private static final int[][] DIRECTIONS = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
    private static final long ATTEMPT_INCREMENT = 0x9E3779B97F4A7C15L;

    interface CandidateFactory {
        PhysicalGameState create(FactoryFeudGeneratorConfig config,
                UnitTypeTable utt, Random random);
    }

    private final FactoryFeudGeneratorConfig config;
    private final UnitTypeTable utt;
    private final CandidateFactory candidateFactory;

    public FactoryFeudMapGenerator(FactoryFeudGeneratorConfig config, UnitTypeTable utt) {
        this(config, utt, FactoryFeudMapGenerator::createCandidate);
    }

    FactoryFeudMapGenerator(FactoryFeudGeneratorConfig config, UnitTypeTable utt,
            CandidateFactory candidateFactory) {
        if (config == null) throw new IllegalArgumentException("config cannot be null");
        if (utt == null) throw new IllegalArgumentException("unit type table cannot be null");
        if (candidateFactory == null) {
            throw new IllegalArgumentException("candidate factory cannot be null");
        }
        this.config = config;
        this.utt = utt;
        this.candidateFactory = candidateFactory;
    }

    public FactoryFeudGeneratedMap generate() throws FactoryFeudGenerationException {
        List<FactoryFeudGenerationAttempt> attempts = new ArrayList<>();
        for (int attempt = 1; attempt <= config.getMaxAttempts(); attempt++) {
            long attemptSeed = seedForAttempt(config.getSeed(), attempt);
            PhysicalGameState candidate = candidateFactory.create(
                    config, utt, new Random(attemptSeed));
            FactoryFeudMapMetrics metrics = FactoryFeudMapMetrics.compute(candidate);
            try {
                FactoryFeudGeneratedMapValidator.validate(candidate);
                attempts.add(new FactoryFeudGenerationAttempt(
                        attempt, attemptSeed, metrics, true, null));
                return new FactoryFeudGeneratedMap(candidate, config, metrics, attempts);
            } catch (IllegalArgumentException invalid) {
                attempts.add(new FactoryFeudGenerationAttempt(attempt, attemptSeed,
                        metrics, false, invalid.getMessage()));
            }
        }
        throw new FactoryFeudGenerationException("Unable to generate a valid "
                + config.getFamily() + " Factory Feud map after "
                + config.getMaxAttempts() + " attempts", attempts);
    }

    private static long seedForAttempt(long seed, int attempt) {
        if (attempt == 1) return seed;
        long value = seed + ATTEMPT_INCREMENT * (attempt - 1L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static PhysicalGameState createCandidate(FactoryFeudGeneratorConfig config,
            UnitTypeTable utt, Random random) {
        PhysicalGameState pgs = new PhysicalGameState(config.getWidth(), config.getHeight());
        pgs.addPlayer(new Player(0, 0));
        pgs.addPlayer(new Player(1, 0));
        addStandardUnits(pgs, utt);

        Set<Integer> occupied = occupiedPositions(pgs);
        int target = config.getTargetObstacleCount();
        switch (config.getFamily()) {
            case OPEN:
                placeScatteredObstacles(pgs, occupied, target, random);
                break;
            case OBSTACLE_FIELD:
                placeClusteredObstacles(pgs, occupied, target, random);
                break;
            case CHOKEPOINT:
                placeChokepointObstacles(pgs, occupied, target, random);
                break;
            default:
                throw new IllegalArgumentException("Unsupported map family: "
                        + config.getFamily());
        }
        return pgs;
    }

    private static void addStandardUnits(PhysicalGameState pgs, UnitTypeTable utt) {
        UnitType resource = requiredType(utt, "Resource");
        UnitType base = requiredType(utt, "Base");
        UnitType worker = requiredType(utt, "Worker");
        int centerY = pgs.getHeight() / 2;
        int rightBaseX = pgs.getWidth() - 2;

        pgs.addUnit(new Unit(0, -1, resource,
                pgs.getWidth() / 2, centerY, FactoryFeudGeneratedMapValidator.REQUIRED_COCOA));
        pgs.addUnit(new Unit(10, 0, base, 1, centerY, 0));
        pgs.addUnit(new Unit(20, 0, worker, 2, centerY, 0));
        pgs.addUnit(new Unit(21, 0, worker, 1, centerY - 1, 0));
        pgs.addUnit(new Unit(22, 0, worker, 1, centerY + 1, 0));
        pgs.addUnit(new Unit(23, 0, worker, 2, centerY - 1, 0));

        pgs.addUnit(new Unit(11, 1, base, rightBaseX, centerY, 0));
        pgs.addUnit(new Unit(24, 1, worker, rightBaseX - 1, centerY, 0));
        pgs.addUnit(new Unit(25, 1, worker, rightBaseX, centerY - 1, 0));
        pgs.addUnit(new Unit(26, 1, worker, rightBaseX, centerY + 1, 0));
        pgs.addUnit(new Unit(27, 1, worker, rightBaseX - 1, centerY - 1, 0));
    }

    private static UnitType requiredType(UnitTypeTable utt, String name) {
        UnitType type = utt.getUnitType(name);
        if (type == null) throw new IllegalArgumentException("Missing unit type: " + name);
        return type;
    }

    private static Set<Integer> occupiedPositions(PhysicalGameState pgs) {
        Set<Integer> occupied = new LinkedHashSet<>();
        for (Unit unit : pgs.getUnits()) {
            occupied.add(position(unit.getX(), unit.getY(), pgs.getWidth()));
        }
        return occupied;
    }

    private static void placeScatteredObstacles(PhysicalGameState pgs,
            Set<Integer> occupied, int target, Random random) {
        List<Integer> eligible = eligiblePositions(pgs, occupied);
        Collections.shuffle(eligible, random);
        placeFirst(pgs, eligible, target);
    }

    private static void placeClusteredObstacles(PhysicalGameState pgs,
            Set<Integer> occupied, int target, Random random) {
        Set<Integer> available = new LinkedHashSet<>(eligiblePositions(pgs, occupied));
        int placed = 0;
        while (placed < target && !available.isEmpty()) {
            int seed = randomElement(available, random);
            List<Integer> frontier = new ArrayList<>();
            frontier.add(seed);
            int desiredClusterSize = 3 + random.nextInt(6);
            int clusterPlaced = 0;
            while (!frontier.isEmpty() && placed < target
                    && clusterPlaced < desiredClusterSize) {
                int index = random.nextInt(frontier.size());
                int current = frontier.remove(index);
                if (!available.remove(current)) continue;
                setWall(pgs, current);
                placed++;
                clusterPlaced++;
                addAvailableNeighbors(pgs, current, available, frontier);
            }
        }
    }

    private static void placeChokepointObstacles(PhysicalGameState pgs,
            Set<Integer> occupied, int target, Random random) {
        int barrierX = pgs.getWidth() / 2;
        int resourceY = pgs.getHeight() / 2;
        List<Integer> possiblePassages = new ArrayList<>();
        for (int y = 1; y < pgs.getHeight() - 1; y++) {
            if (Math.abs(y - resourceY) > 1) possiblePassages.add(y);
        }
        Collections.shuffle(possiblePassages, random);
        Set<Integer> passages = new LinkedHashSet<>();
        passages.add(resourceY);
        if (!possiblePassages.isEmpty()) passages.add(possiblePassages.get(0));

        int placed = 0;
        for (int y = 0; y < pgs.getHeight() && placed < target; y++) {
            int cell = position(barrierX, y, pgs.getWidth());
            if (passages.contains(y) || occupied.contains(cell)) continue;
            setWall(pgs, cell);
            placed++;
        }

        if (placed < target) {
            Set<Integer> unavailable = new LinkedHashSet<>(occupied);
            for (int y = 0; y < pgs.getHeight(); y++) {
                if (pgs.getTerrain(barrierX, y) != PhysicalGameState.TERRAIN_NONE) {
                    unavailable.add(position(barrierX, y, pgs.getWidth()));
                }
            }
            placeAdditionalClusters(pgs, unavailable, target - placed, random);
        }
    }

    private static void placeAdditionalClusters(PhysicalGameState pgs,
            Set<Integer> unavailable, int count, Random random) {
        Set<Integer> available = new LinkedHashSet<>(eligiblePositions(pgs, unavailable));
        int placed = 0;
        while (placed < count && !available.isEmpty()) {
            int start = randomElement(available, random);
            List<Integer> frontier = new ArrayList<>();
            frontier.add(start);
            while (!frontier.isEmpty() && placed < count) {
                int current = frontier.remove(random.nextInt(frontier.size()));
                if (!available.remove(current)) continue;
                setWall(pgs, current);
                placed++;
                addAvailableNeighbors(pgs, current, available, frontier);
                if (random.nextDouble() < 0.20) break;
            }
        }
    }

    private static List<Integer> eligiblePositions(PhysicalGameState pgs,
            Set<Integer> unavailable) {
        List<Integer> eligible = new ArrayList<>();
        for (int y = 0; y < pgs.getHeight(); y++) {
            for (int x = 0; x < pgs.getWidth(); x++) {
                int cell = position(x, y, pgs.getWidth());
                if (!unavailable.contains(cell)
                        && pgs.getTerrain(x, y) == PhysicalGameState.TERRAIN_NONE) {
                    eligible.add(cell);
                }
            }
        }
        return eligible;
    }

    private static void placeFirst(PhysicalGameState pgs, List<Integer> positions, int count) {
        if (positions.size() < count) {
            throw new IllegalArgumentException("obstacle density leaves insufficient open cells");
        }
        for (int index = 0; index < count; index++) setWall(pgs, positions.get(index));
    }

    private static int randomElement(Set<Integer> positions, Random random) {
        int selected = random.nextInt(positions.size());
        int index = 0;
        for (Integer position : positions) {
            if (index++ == selected) return position;
        }
        throw new IllegalStateException("random selection failed");
    }

    private static void addAvailableNeighbors(PhysicalGameState pgs, int cell,
            Set<Integer> available, List<Integer> frontier) {
        int x = cell % pgs.getWidth();
        int y = cell / pgs.getWidth();
        for (int[] direction : DIRECTIONS) {
            int nextX = x + direction[0];
            int nextY = y + direction[1];
            if (nextX < 0 || nextY < 0
                    || nextX >= pgs.getWidth() || nextY >= pgs.getHeight()) continue;
            int next = position(nextX, nextY, pgs.getWidth());
            if (available.contains(next) && !frontier.contains(next)) frontier.add(next);
        }
    }

    private static void setWall(PhysicalGameState pgs, int cell) {
        pgs.setTerrain(cell % pgs.getWidth(), cell / pgs.getWidth(),
                PhysicalGameState.TERRAIN_WALL);
    }

    private static int position(int x, int y, int width) {
        return x + y * width;
    }
}
