package courseproject.factoryfeud;

import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/** Shared project-owned role behaviors; subclasses own their allocation policy. */
public abstract class FactoryFeudRoleAI extends AbstractionLayerAI {
    protected static final int ESCORT_THREAT_RADIUS = 3;
    protected static final int FACTORY_DEFENSE_RADIUS = 4;

    protected final UnitTypeTable utt;
    protected final Map<Long, Role> roleAssignments = new LinkedHashMap<>();
    protected int controlledPlayer = -1;

    protected FactoryFeudRoleAI(UnitTypeTable utt) {
        super(new AStarPathFinding());
        this.utt = utt;
    }

    @Override
    public void reset() {
        super.reset();
        roleAssignments.clear();
        controlledPlayer = -1;
        resetAllocationState();
    }

    protected void resetAllocationState() {
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) {
        controlledPlayer = player;
        List<Unit> workers = friendlyWorkers(player, gs);
        if (roleAssignments.isEmpty()) initializeAssignments(workers, player, gs);
        updateAssignments(workers, player, gs);

        for (Unit worker : workers) {
            Role role = roleAssignments.get(worker.getID());
            if (role == null) {
                role = Role.ATTACKER;
                roleAssignments.put(worker.getID(), role);
            }
            executeRole(worker, role, player, gs);
        }
        return translateActions(player, gs);
    }

    protected abstract void initializeAssignments(List<Unit> workers, int player, GameState gs);

    protected abstract void updateAssignments(List<Unit> workers, int player, GameState gs);

    protected void executeRole(Unit worker, Role role, int player, GameState gs) {
        switch (role) {
            case COLLECTOR:
                collect(worker, player, gs);
                break;
            case ESCORT:
                escort(worker, player, gs);
                break;
            case ATTACKER:
                attackCollector(worker, player, gs);
                break;
            case DEFENDER:
                defend(worker, player, gs);
                break;
            default:
                idle(worker);
        }
    }

    private void collect(Unit worker, int player, GameState gs) {
        Unit factory = friendlyFactory(player, gs);
        if (worker.getResources() > 0) {
            if (factory != null) harvest(worker, null, factory);
            else idle(worker);
            return;
        }

        Unit cocoa = nearestReachableResource(worker, gs);
        if (cocoa != null && factory != null) harvest(worker, cocoa, factory);
        else if (factory != null) moveNear(worker, factory, gs);
        else idle(worker);
    }

    private void escort(Unit worker, int player, GameState gs) {
        Unit collector = nearestLiveRole(worker, player, Role.COLLECTOR, gs);
        if (collector == null) {
            defend(worker, player, gs);
            return;
        }

        Unit threat = nearestEnemyToProtectedUnit(worker, collector, player, ESCORT_THREAT_RADIUS, gs);
        if (threat != null && reachableForAttack(worker, threat, gs)) attack(worker, threat);
        else moveNear(worker, collector, gs);
    }

    private void attackCollector(Unit worker, int player, GameState gs) {
        for (Unit candidate : inferredEnemyCollectors(worker, player, gs)) {
            if (reachableForAttack(worker, candidate, gs)) {
                attack(worker, candidate);
                return;
            }
        }
        idle(worker);
    }

    private void defend(Unit worker, int player, GameState gs) {
        Unit factory = friendlyFactory(player, gs);
        Unit threat = factory == null ? null
                : nearestEnemyToProtectedUnit(worker, factory, player, FACTORY_DEFENSE_RADIUS, gs);
        if (threat == null) threat = nearestEnemyToFriendlyUnit(worker, player, 2, gs);

        if (threat != null && reachableForAttack(worker, threat, gs)) attack(worker, threat);
        else if (factory != null) moveNear(worker, factory, gs);
        else idle(worker);
    }

    protected List<Unit> friendlyWorkers(int player, GameState gs) {
        List<Unit> workers = new ArrayList<>();
        for (Unit unit : gs.getUnits()) {
            if (unit.getPlayer() == player && unit.getType().canHarvest) workers.add(unit);
        }
        sortById(workers);
        return workers;
    }

    protected List<Unit> enemyWorkers(int player, GameState gs) {
        List<Unit> workers = new ArrayList<>();
        for (Unit unit : gs.getUnits()) {
            if (unit.getPlayer() >= 0 && unit.getPlayer() != player && unit.getType().canHarvest) {
                workers.add(unit);
            }
        }
        sortById(workers);
        return workers;
    }

    protected List<Unit> resources(GameState gs) {
        List<Unit> resources = new ArrayList<>();
        for (Unit unit : gs.getUnits()) if (unit.getType().isResource) resources.add(unit);
        sortById(resources);
        return resources;
    }

    protected Unit friendlyFactory(int player, GameState gs) {
        Unit result = null;
        for (Unit unit : gs.getUnits()) {
            if (unit.getPlayer() == player && unit.getType().isStockpile
                    && (result == null || unit.getID() < result.getID())) result = unit;
        }
        return result;
    }

    protected Unit nearestResource(Unit worker, GameState gs) {
        Unit best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Unit resource : resources(gs)) {
            int distance = manhattan(worker, resource);
            if (distance < bestDistance || (distance == bestDistance && resource.getID() < best.getID())) {
                best = resource;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Returns the nearest cocoa node that A* can currently reach adjacent to. */
    protected Unit nearestReachableResource(Unit worker, GameState gs) {
        Unit best = null;
        int bestDistance = Integer.MAX_VALUE;
        int width = gs.getPhysicalGameState().getWidth();
        for (Unit resource : resources(gs)) {
            int targetPosition = resource.getX() + resource.getY() * width;
            if (!pf.pathToPositionInRangeExists(worker, targetPosition, 1, gs, null)) continue;
            int distance = manhattan(worker, resource);
            if (distance < bestDistance || (distance == bestDistance && resource.getID() < best.getID())) {
                best = resource;
                bestDistance = distance;
            }
        }
        return best;
    }

    protected Unit nearestLiveRole(Unit from, int player, Role role, GameState gs) {
        Unit best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Unit unit : friendlyWorkers(player, gs)) {
            if (unit.getID() == from.getID() || roleAssignments.get(unit.getID()) != role) continue;
            int distance = manhattan(from, unit);
            if (distance < bestDistance || (distance == bestDistance && unit.getID() < best.getID())) {
                best = unit;
                bestDistance = distance;
            }
        }
        return best;
    }

    private Unit nearestEnemyToProtectedUnit(Unit actor, Unit protectedUnit, int player,
            int protectionRadius, GameState gs) {
        Unit best = null;
        int bestActorDistance = Integer.MAX_VALUE;
        for (Unit enemy : enemyWorkers(player, gs)) {
            if (manhattan(enemy, protectedUnit) > protectionRadius) continue;
            int actorDistance = manhattan(actor, enemy);
            if (actorDistance < bestActorDistance
                    || (actorDistance == bestActorDistance && enemy.getID() < best.getID())) {
                best = enemy;
                bestActorDistance = actorDistance;
            }
        }
        return best;
    }

    private Unit nearestEnemyToFriendlyUnit(Unit actor, int player, int protectionRadius, GameState gs) {
        Unit best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Unit enemy : enemyWorkers(player, gs)) {
            boolean threatensFriendly = false;
            for (Unit friendly : friendlyWorkers(player, gs)) {
                if (manhattan(enemy, friendly) <= protectionRadius) {
                    threatensFriendly = true;
                    break;
                }
            }
            if (!threatensFriendly) continue;
            int distance = manhattan(actor, enemy);
            if (distance < bestDistance || (distance == bestDistance && enemy.getID() < best.getID())) {
                best = enemy;
                bestDistance = distance;
            }
        }
        return best;
    }

    protected List<Unit> inferredEnemyCollectors(final Unit actor, int player, final GameState gs) {
        List<Unit> candidates = enemyWorkers(player, gs);
        Collections.sort(candidates, new Comparator<Unit>() {
            @Override
            public int compare(Unit first, Unit second) {
                if ((first.getResources() > 0) != (second.getResources() > 0)) {
                    return first.getResources() > 0 ? -1 : 1;
                }
                int firstResourceDistance = distanceToNearestResource(first, gs);
                int secondResourceDistance = distanceToNearestResource(second, gs);
                if (firstResourceDistance != secondResourceDistance) {
                    return Integer.compare(firstResourceDistance, secondResourceDistance);
                }
                int firstActorDistance = manhattan(actor, first);
                int secondActorDistance = manhattan(actor, second);
                if (firstActorDistance != secondActorDistance) {
                    return Integer.compare(firstActorDistance, secondActorDistance);
                }
                return Long.compare(first.getID(), second.getID());
            }
        });
        return candidates;
    }

    protected int distanceToNearestResource(Unit unit, GameState gs) {
        int best = Integer.MAX_VALUE;
        for (Unit resource : resources(gs)) best = Math.min(best, manhattan(unit, resource));
        return best;
    }

    protected boolean reachableForAttack(Unit actor, Unit target, GameState gs) {
        int targetPosition = target.getX() + target.getY() * gs.getPhysicalGameState().getWidth();
        return pf.pathToPositionInRangeExists(actor, targetPosition, actor.getAttackRange(), gs, null);
    }

    protected void moveNear(Unit actor, Unit target, GameState gs) {
        if (manhattan(actor, target) <= 1) {
            idle(actor);
            return;
        }

        PhysicalGameState pgs = gs.getPhysicalGameState();
        int[][] offsets = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
        int bestX = -1;
        int bestY = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int[] offset : offsets) {
            int x = target.getX() + offset[0];
            int y = target.getY() + offset[1];
            if (x < 0 || y < 0 || x >= pgs.getWidth() || y >= pgs.getHeight()) continue;
            if (pgs.getTerrain(x, y) != PhysicalGameState.TERRAIN_NONE) continue;
            Unit occupant = pgs.getUnitAt(x, y);
            if (occupant != null && occupant.getID() != actor.getID()) continue;
            int distance = Math.abs(actor.getX() - x) + Math.abs(actor.getY() - y);
            int position = x + y * pgs.getWidth();
            int bestPosition = bestX < 0 ? Integer.MAX_VALUE : bestX + bestY * pgs.getWidth();
            if (distance < bestDistance || (distance == bestDistance && position < bestPosition)) {
                bestX = x;
                bestY = y;
                bestDistance = distance;
            }
        }
        if (bestX >= 0) move(actor, bestX, bestY);
        else idle(actor);
    }

    protected static int manhattan(Unit first, Unit second) {
        return Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY());
    }

    protected static void sortById(List<Unit> units) {
        Collections.sort(units, new Comparator<Unit>() {
            @Override
            public int compare(Unit first, Unit second) {
                return Long.compare(first.getID(), second.getID());
            }
        });
    }

    public Map<Long, Role> getRoleAssignments() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(roleAssignments));
    }

    public Role getRole(long unitId) {
        return roleAssignments.get(unitId);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }
}
