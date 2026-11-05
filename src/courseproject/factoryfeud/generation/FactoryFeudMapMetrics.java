package courseproject.factoryfeud.generation;

import java.util.ArrayDeque;
import java.util.Arrays;
import rts.PhysicalGameState;
import rts.units.Unit;

/** Terrain and route properties computed for every raw generated candidate. */
public final class FactoryFeudMapMetrics {
    private static final int[][] DIRECTIONS = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};

    private final int obstacleCount;
    private final double actualObstacleDensity;
    private final int player0FactoryToCocoaPath;
    private final int player1FactoryToCocoaPath;
    private final int deadEndCount;
    private final int routeDifference;

    private FactoryFeudMapMetrics(int obstacleCount, double actualObstacleDensity,
            int player0FactoryToCocoaPath, int player1FactoryToCocoaPath,
            int deadEndCount, int routeDifference) {
        this.obstacleCount = obstacleCount;
        this.actualObstacleDensity = actualObstacleDensity;
        this.player0FactoryToCocoaPath = player0FactoryToCocoaPath;
        this.player1FactoryToCocoaPath = player1FactoryToCocoaPath;
        this.deadEndCount = deadEndCount;
        this.routeDifference = routeDifference;
    }

    public static FactoryFeudMapMetrics compute(PhysicalGameState pgs) {
        if (pgs == null) throw new IllegalArgumentException("physical game state cannot be null");
        int obstacles = 0;
        int deadEnds = 0;
        for (int y = 0; y < pgs.getHeight(); y++) {
            for (int x = 0; x < pgs.getWidth(); x++) {
                if (pgs.getTerrain(x, y) != PhysicalGameState.TERRAIN_NONE) {
                    obstacles++;
                    continue;
                }
                int exits = 0;
                for (int[] direction : DIRECTIONS) {
                    int nextX = x + direction[0];
                    int nextY = y + direction[1];
                    if (inBounds(pgs, nextX, nextY)
                            && pgs.getTerrain(nextX, nextY)
                                    == PhysicalGameState.TERRAIN_NONE) exits++;
                }
                if (exits <= 1) deadEnds++;
            }
        }

        Unit cocoa = null;
        Unit[] factories = new Unit[2];
        for (Unit unit : pgs.getUnits()) {
            if (unit.getType().isResource && cocoa == null) cocoa = unit;
            if (unit.getPlayer() >= 0 && unit.getPlayer() < factories.length
                    && unit.getType().isStockpile) factories[unit.getPlayer()] = unit;
        }
        int player0Route = shortestTerrainPath(pgs, factories[0], cocoa);
        int player1Route = shortestTerrainPath(pgs, factories[1], cocoa);
        int difference = player0Route < 0 || player1Route < 0
                ? -1 : Math.abs(player0Route - player1Route);
        return new FactoryFeudMapMetrics(obstacles,
                obstacles / (double) (pgs.getWidth() * pgs.getHeight()),
                player0Route, player1Route, deadEnds, difference);
    }

    private static int shortestTerrainPath(PhysicalGameState pgs, Unit start, Unit target) {
        if (start == null || target == null) return -1;
        int width = pgs.getWidth();
        int[] distance = new int[width * pgs.getHeight()];
        Arrays.fill(distance, -1);
        int startPosition = start.getX() + start.getY() * width;
        int targetPosition = target.getX() + target.getY() * width;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        distance[startPosition] = 0;
        queue.add(startPosition);
        while (!queue.isEmpty()) {
            int position = queue.removeFirst();
            if (position == targetPosition) return distance[position];
            int x = position % width;
            int y = position / width;
            for (int[] direction : DIRECTIONS) {
                int nextX = x + direction[0];
                int nextY = y + direction[1];
                if (!inBounds(pgs, nextX, nextY)) continue;
                int next = nextX + nextY * width;
                if (distance[next] >= 0
                        || pgs.getTerrain(nextX, nextY)
                                != PhysicalGameState.TERRAIN_NONE) continue;
                distance[next] = distance[position] + 1;
                queue.add(next);
            }
        }
        return -1;
    }

    private static boolean inBounds(PhysicalGameState pgs, int x, int y) {
        return x >= 0 && y >= 0 && x < pgs.getWidth() && y < pgs.getHeight();
    }

    public int getObstacleCount() { return obstacleCount; }
    public double getActualObstacleDensity() { return actualObstacleDensity; }
    public int getPlayer0FactoryToCocoaPath() { return player0FactoryToCocoaPath; }
    public int getPlayer1FactoryToCocoaPath() { return player1FactoryToCocoaPath; }
    public int getDeadEndCount() { return deadEndCount; }
    public int getRouteDifference() { return routeDifference; }
}
