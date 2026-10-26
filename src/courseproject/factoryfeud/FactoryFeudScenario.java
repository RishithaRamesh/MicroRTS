package courseproject.factoryfeud;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/** Loads and validates the registered Factory Feud scenarios. */
public final class FactoryFeudScenario {
    public static final String CONTROL_MAP = "maps/factory-feud/factory-feud-12x12.xml";
    public static final String CONTESTED_CENTER_MAP =
            "maps/factory-feud/factory-feud-contested-center-12x12.xml";
    public static final String NARROW_PASSAGE_MAP =
            "maps/factory-feud/factory-feud-narrow-passage-12x12.xml";
    public static final String REFINED_NARROW_PASSAGE_MAP =
            "maps/factory-feud/factory-feud-narrow-passage-refined-12x12.xml";
    public static final String EXPOSED_ROUTES_MAP =
            "maps/factory-feud/factory-feud-exposed-routes-14x12.xml";
    public static final String DEFAULT_MAP = CONTROL_MAP;
    public static final int PLAYER_COUNT = 2;
    public static final int WORKERS_PER_TEAM = 4;
    public static final int FACTORIES_PER_TEAM = 1;
    /** Legacy control-map count; scenario validation now permits any positive count. */
    @Deprecated
    public static final int COCOA_RESOURCE_COUNT = 6;

    private FactoryFeudScenario() {
    }

    public static List<String> evaluationMaps() {
        return Collections.unmodifiableList(Arrays.asList(
                CONTROL_MAP, CONTESTED_CENTER_MAP, NARROW_PASSAGE_MAP,
                REFINED_NARROW_PASSAGE_MAP,
                EXPOSED_ROUTES_MAP));
    }

    public static GameState load(UnitTypeTable utt) throws Exception {
        return load(DEFAULT_MAP, utt);
    }

    public static GameState load(String mapPath, UnitTypeTable utt) throws Exception {
        if (mapPath == null || mapPath.trim().isEmpty()) {
            throw new IllegalArgumentException("mapPath cannot be blank");
        }
        PhysicalGameState pgs = PhysicalGameState.load(mapPath, utt);
        validate(pgs);
        return new GameState(pgs, utt);
    }

    /** Loads a map while also enforcing a caller-selected positive cocoa-node count. */
    public static GameState load(String mapPath, UnitTypeTable utt, int expectedCocoaCount)
            throws Exception {
        if (mapPath == null || mapPath.trim().isEmpty()) {
            throw new IllegalArgumentException("mapPath cannot be blank");
        }
        PhysicalGameState pgs = PhysicalGameState.load(mapPath, utt);
        validate(pgs, expectedCocoaCount);
        return new GameState(pgs, utt);
    }

    public static void validate(PhysicalGameState pgs) {
        validate(pgs, null);
    }

    /** Validates a map and optionally requires an exact, positive cocoa-node count. */
    public static void validate(PhysicalGameState pgs, int expectedCocoaCount) {
        if (expectedCocoaCount <= 0) {
            throw new IllegalArgumentException("Expected cocoa count must be positive");
        }
        validate(pgs, Integer.valueOf(expectedCocoaCount));
    }

    private static void validate(PhysicalGameState pgs, Integer expectedCocoaCount) {
        if (pgs.getPlayers().size() != PLAYER_COUNT) {
            throw new IllegalArgumentException("Factory Feud requires exactly two players");
        }

        int[] workers = new int[PLAYER_COUNT];
        int[] factories = new int[PLAYER_COUNT];
        int cocoa = 0;
        boolean[][] occupied = new boolean[pgs.getWidth()][pgs.getHeight()];

        for (Unit unit : pgs.getUnits()) {
            int x = unit.getX();
            int y = unit.getY();
            if (x < 0 || y < 0 || x >= pgs.getWidth() || y >= pgs.getHeight()) {
                throw new IllegalArgumentException("Unit " + unit.getID() + " is outside the map");
            }
            if (pgs.getTerrain(x, y) != PhysicalGameState.TERRAIN_NONE) {
                throw new IllegalArgumentException("Unit " + unit.getID() + " is placed on terrain");
            }
            if (occupied[x][y]) {
                throw new IllegalArgumentException("Multiple units occupy " + x + "," + y);
            }
            occupied[x][y] = true;
            if (unit.getType().isResource) {
                cocoa++;
            } else if (unit.getPlayer() >= 0 && unit.getPlayer() < PLAYER_COUNT) {
                if ("Worker".equals(unit.getType().name)) {
                    workers[unit.getPlayer()]++;
                }
                if (unit.getType().isStockpile) {
                    factories[unit.getPlayer()]++;
                }
            }
        }

        for (int player = 0; player < PLAYER_COUNT; player++) {
            if (workers[player] != WORKERS_PER_TEAM) {
                throw new IllegalArgumentException("Player " + player + " must have exactly four Workers");
            }
            if (factories[player] != FACTORIES_PER_TEAM) {
                throw new IllegalArgumentException("Player " + player + " must have exactly one factory/Base");
            }
            if (pgs.getPlayer(player).getResources() != 0) {
                throw new IllegalArgumentException("Factory Feud players must start with zero delivered cocoa");
            }
        }

        if (cocoa <= 0) {
            throw new IllegalArgumentException("Factory Feud map must contain cocoa resources");
        }
        if (expectedCocoaCount != null && cocoa != expectedCocoaCount.intValue()) {
            throw new IllegalArgumentException("Factory Feud map must contain exactly "
                    + expectedCocoaCount + " cocoa resources");
        }
        validateReachability(pgs);
    }

    /** Validates terrain-level access while ignoring temporary unit occupancy. */
    public static void validateReachability(PhysicalGameState pgs) {
        int width = pgs.getWidth();
        int height = pgs.getHeight();
        boolean[][][] reachable = new boolean[PLAYER_COUNT][width][height];
        for (int player = 0; player < PLAYER_COUNT; player++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            for (Unit unit : pgs.getUnits()) {
                if (unit.getPlayer() == player && unit.getType().canMove) {
                    reachable[player][unit.getX()][unit.getY()] = true;
                    queue.add(unit.getX() + unit.getY() * width);
                    break;
                }
            }
            floodTerrain(pgs, reachable[player], queue);
            for (Unit unit : pgs.getUnits()) {
                if (unit.getPlayer() == player && unit.getType().canMove
                        && !reachable[player][unit.getX()][unit.getY()]) {
                    throw new IllegalArgumentException("Player " + player + " worker "
                            + unit.getID() + " is isolated by terrain");
                }
            }
        }

        for (Unit unit : pgs.getUnits()) {
            if (!unit.getType().isResource) continue;
            if (unit.getResources() <= 0) {
                throw new IllegalArgumentException("Cocoa resource " + unit.getID()
                        + " must contain a positive quantity");
            }
            for (int player = 0; player < PLAYER_COUNT; player++) {
                if (!hasReachableAdjacent(unit, reachable[player], width, height)) {
                    throw new IllegalArgumentException("Player " + player
                            + " cannot reach cocoa resource " + unit.getID());
                }
            }
        }

        for (int player = 0; player < PLAYER_COUNT; player++) {
            Unit factory = null;
            for (Unit unit : pgs.getUnits()) {
                if (unit.getPlayer() == player && unit.getType().isStockpile) factory = unit;
            }
            if (factory == null || !hasReachableAdjacent(factory, reachable[player], width, height)) {
                throw new IllegalArgumentException("Player " + player
                        + " has no reachable return position next to its factory");
            }
        }
    }

    private static void floodTerrain(PhysicalGameState pgs, boolean[][] reachable,
            ArrayDeque<Integer> queue) {
        int width = pgs.getWidth();
        int height = pgs.getHeight();
        int[][] directions = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
        while (!queue.isEmpty()) {
            int position = queue.removeFirst();
            int x = position % width;
            int y = position / width;
            for (int[] direction : directions) {
                int nextX = x + direction[0];
                int nextY = y + direction[1];
                if (nextX < 0 || nextY < 0 || nextX >= width || nextY >= height
                        || reachable[nextX][nextY]
                        || pgs.getTerrain(nextX, nextY) != PhysicalGameState.TERRAIN_NONE) {
                    continue;
                }
                reachable[nextX][nextY] = true;
                queue.add(nextX + nextY * width);
            }
        }
    }

    private static boolean hasReachableAdjacent(Unit target, boolean[][] reachable,
            int width, int height) {
        int[][] directions = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
        for (int[] direction : directions) {
            int x = target.getX() + direction[0];
            int y = target.getY() + direction[1];
            if (x >= 0 && y >= 0 && x < width && y < height && reachable[x][y]) return true;
        }
        return false;
    }

    /** Accepts either vertical reflection or 180-degree rotational symmetry. */
    public static boolean hasBalancedSymmetry(PhysicalGameState pgs) {
        return hasSymmetry(pgs, false) || hasSymmetry(pgs, true);
    }

    private static boolean hasSymmetry(PhysicalGameState pgs, boolean rotateY) {
        int width = pgs.getWidth();
        int height = pgs.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int mirrorY = rotateY ? height - 1 - y : y;
                if (pgs.getTerrain(x, y) != pgs.getTerrain(width - 1 - x, mirrorY)) {
                    return false;
                }
            }
        }
        for (Unit unit : pgs.getUnits()) {
            int mirrorX = width - 1 - unit.getX();
            int mirrorY = rotateY ? height - 1 - unit.getY() : unit.getY();
            int mirrorPlayer = unit.getPlayer() < 0 ? unit.getPlayer() : 1 - unit.getPlayer();
            if (!containsEquivalentUnit(pgs, unit, mirrorX, mirrorY, mirrorPlayer)) return false;
        }
        return true;
    }

    private static boolean containsEquivalentUnit(PhysicalGameState pgs, Unit source,
            int x, int y, int player) {
        for (Unit candidate : pgs.getUnits()) {
            if (candidate.getX() == x && candidate.getY() == y
                    && candidate.getPlayer() == player
                    && candidate.getType().name.equals(source.getType().name)
                    && candidate.getResources() == source.getResources()
                    && candidate.getHitPoints() == source.getHitPoints()) return true;
        }
        return false;
    }
}
