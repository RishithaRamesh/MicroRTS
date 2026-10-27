package courseproject.factoryfeud;

import ai.core.AI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import rts.GameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/** Explainable utility/count-based role allocator with cooldown and emergency defense. */
public class OldAdaptiveRoleAI extends FactoryFeudRoleAI {
    public static final int DEFAULT_MATCH_DURATION = 500;
    public static final int REEVALUATION_INTERVAL = 10;
    public static final int REASSIGNMENT_COOLDOWN = 25;
    public static final int EMERGENCY_FACTORY_RADIUS = 3;

    private final int matchDuration;
    private final Map<Long, Integer> lastRoleChangeCycle = new HashMap<>();
    private final List<String> roleChangeHistory = new ArrayList<>();
    private int lastEvaluationCycle = Integer.MIN_VALUE;
    private int roleChangeCount;

    public OldAdaptiveRoleAI(UnitTypeTable utt) {
        this(utt, DEFAULT_MATCH_DURATION);
    }

    public OldAdaptiveRoleAI(UnitTypeTable utt, int matchDuration) {
        super(utt);
        if (matchDuration <= 0) throw new IllegalArgumentException("matchDuration must be positive");
        this.matchDuration = matchDuration;
    }

    @Override
    protected void resetAllocationState() {
        lastRoleChangeCycle.clear();
        roleChangeHistory.clear();
        lastEvaluationCycle = Integer.MIN_VALUE;
        roleChangeCount = 0;
    }

    @Override
    protected void initializeAssignments(List<Unit> workers, int player, GameState gs) {
        roleAssignments.putAll(planAssignments(workers, player, gs));
        for (Unit worker : workers) lastRoleChangeCycle.put(worker.getID(), gs.getTime());
        lastEvaluationCycle = gs.getTime();
    }

    @Override
    protected void updateAssignments(List<Unit> workers, int player, GameState gs) {
        if (workers.isEmpty()) return;
        if (isFactoryThreatened(player, gs)) {
            applyEmergencyDefense(workers, player, gs);
            return;
        }
        if (gs.getTime() - lastEvaluationCycle < REEVALUATION_INTERVAL) return;

        Map<Long, Role> proposed = planAssignments(workers, player, gs);
        for (Unit worker : workers) {
            Role desired = proposed.get(worker.getID());
            Role current = roleAssignments.get(worker.getID());
            if (desired == null || desired == current) continue;

            int lastChange = lastRoleChangeCycle.containsKey(worker.getID())
                    ? lastRoleChangeCycle.get(worker.getID()) : Integer.MIN_VALUE;
            boolean missingRequiredCollector = desired == Role.COLLECTOR
                    && !hasLiveRole(Role.COLLECTOR, workers, worker.getID());
            boolean carryingCocoa = preserveCarryingCollectors()
                    && desired == Role.COLLECTOR && worker.getResources() > 0;
            if (carryingCocoa || missingRequiredCollector
                    || gs.getTime() - lastChange >= REASSIGNMENT_COOLDOWN) {
                changeRole(worker.getID(), desired, gs.getTime(),
                        reasonFor(desired, workers, player, gs));
            }
        }
        lastEvaluationCycle = gs.getTime();
    }

    private Map<Long, Role> planAssignments(List<Unit> workers, int player, GameState gs) {
        Map<Long, Role> planned = new LinkedHashMap<>();
        List<Unit> available = new ArrayList<>(workers);
        int live = available.size();
        int remaining = Math.max(0, matchDuration - gs.getTime());
        boolean cocoaAvailable = !resources(gs).isEmpty();
        boolean behind = gs.getPlayer(player).getResources()
                < gs.getPlayer(1 - player).getResources();
        boolean factoryThreat = isFactoryThreatened(player, gs);
        boolean collectorEndangered = isAnyCollectorEndangered(workers, player, gs);

        int collectors = cocoaAvailable ? 1 : 0;
        if (cocoaAvailable && behind && live >= 3) collectors = 2;
        if (cocoaAvailable && behind && remaining <= Math.max(1, matchDuration / 4)) collectors = 2;
        int defenders = factoryThreat ? 2 : 1;
        int escorts = collectors > 0 ? 1 : 0;
        if (collectorEndangered && live >= 3) escorts = 2;

        int preservedCarriers = assignCarryingCollectors(planned, available);
        assignClosestToResources(planned, available,
                Math.max(0, collectors - preservedCarriers), gs);
        assignClosestToFactory(planned, available, defenders, player, gs);
        assignClosestToCollectors(planned, available, escorts, gs);
        for (Unit worker : available) planned.put(worker.getID(), Role.ATTACKER);
        return planned;
    }

    private void assignClosestToResources(Map<Long, Role> planned, List<Unit> available,
            int count, final GameState gs) {
        Collections.sort(available, new Comparator<Unit>() {
            @Override
            public int compare(Unit first, Unit second) {
                if ((first.getResources() > 0) != (second.getResources() > 0)) {
                    return first.getResources() > 0 ? -1 : 1;
                }
                int firstDistance = distanceToNearestResource(first, gs);
                int secondDistance = distanceToNearestResource(second, gs);
                if (firstDistance != secondDistance) return Integer.compare(firstDistance, secondDistance);
                return Long.compare(first.getID(), second.getID());
            }
        });
        take(planned, available, count, Role.COLLECTOR);
    }

    private int assignCarryingCollectors(Map<Long, Role> planned, List<Unit> available) {
        if (!preserveCarryingCollectors()) return 0;
        int assigned = 0;
        for (int index = available.size() - 1; index >= 0; index--) {
            Unit worker = available.get(index);
            if (worker.getResources() <= 0) continue;
            planned.put(worker.getID(), Role.COLLECTOR);
            available.remove(index);
            assigned++;
        }
        return assigned;
    }

    private void assignClosestToFactory(Map<Long, Role> planned, List<Unit> available,
            int count, int player, GameState gs) {
        final Unit factory = friendlyFactory(player, gs);
        Collections.sort(available, new Comparator<Unit>() {
            @Override
            public int compare(Unit first, Unit second) {
                int firstDistance = factory == null ? 0 : manhattan(first, factory);
                int secondDistance = factory == null ? 0 : manhattan(second, factory);
                if (firstDistance != secondDistance) return Integer.compare(firstDistance, secondDistance);
                return Long.compare(first.getID(), second.getID());
            }
        });
        take(planned, available, count, Role.DEFENDER);
    }

    private void assignClosestToCollectors(final Map<Long, Role> planned, List<Unit> available,
            int count, final GameState gs) {
        final List<Unit> collectors = new ArrayList<>();
        for (Unit unit : gs.getUnits()) {
            if (planned.get(unit.getID()) == Role.COLLECTOR) collectors.add(unit);
        }
        Collections.sort(available, new Comparator<Unit>() {
            @Override
            public int compare(Unit first, Unit second) {
                int firstDistance = distanceToAny(first, collectors);
                int secondDistance = distanceToAny(second, collectors);
                if (firstDistance != secondDistance) return Integer.compare(firstDistance, secondDistance);
                return Long.compare(first.getID(), second.getID());
            }
        });
        take(planned, available, count, Role.ESCORT);
    }

    private static void take(Map<Long, Role> planned, List<Unit> available, int count, Role role) {
        int assignments = Math.min(count, available.size());
        for (int index = 0; index < assignments; index++) {
            Unit chosen = available.remove(0);
            planned.put(chosen.getID(), role);
        }
    }

    private static int distanceToAny(Unit unit, List<Unit> targets) {
        int best = Integer.MAX_VALUE;
        for (Unit target : targets) best = Math.min(best, manhattan(unit, target));
        return best;
    }

    private boolean applyEmergencyDefense(List<Unit> workers, int player, GameState gs) {
        if (!isFactoryThreatened(player, gs)) return false;
        int defenders = 0;
        for (Unit worker : workers) if (roleAssignments.get(worker.getID()) == Role.DEFENDER) defenders++;
        if (defenders >= 2) return false;

        final Unit factory = friendlyFactory(player, gs);
        List<Unit> candidates = new ArrayList<>();
        for (Unit worker : workers) {
            if (roleAssignments.get(worker.getID()) != Role.DEFENDER
                    && !(preserveCarryingCollectors() && worker.getResources() > 0)) {
                candidates.add(worker);
            }
        }
        Collections.sort(candidates, new Comparator<Unit>() {
            @Override
            public int compare(Unit first, Unit second) {
                int firstDistance = factory == null ? 0 : manhattan(first, factory);
                int secondDistance = factory == null ? 0 : manhattan(second, factory);
                if (firstDistance != secondDistance) return Integer.compare(firstDistance, secondDistance);
                return Long.compare(first.getID(), second.getID());
            }
        });
        if (candidates.isEmpty()) return false;
        changeRole(candidates.get(0).getID(), Role.DEFENDER, gs.getTime(), "emergency-factory-threat");
        return true;
    }

    private boolean isFactoryThreatened(int player, GameState gs) {
        Unit factory = friendlyFactory(player, gs);
        if (factory == null) return false;
        for (Unit enemy : enemyWorkers(player, gs)) {
            if (manhattan(factory, enemy) <= EMERGENCY_FACTORY_RADIUS) return true;
        }
        return false;
    }

    private boolean isAnyCollectorEndangered(List<Unit> workers, int player, GameState gs) {
        for (Unit worker : workers) {
            if (roleAssignments.get(worker.getID()) != Role.COLLECTOR) continue;
            for (Unit enemy : enemyWorkers(player, gs)) {
                if (manhattan(worker, enemy) <= ESCORT_THREAT_RADIUS) return true;
            }
        }
        return false;
    }

    private boolean hasLiveRole(Role role, List<Unit> workers, long exceptUnit) {
        if (role == null) return false;
        for (Unit worker : workers) {
            if (worker.getID() != exceptUnit && roleAssignments.get(worker.getID()) == role) return true;
        }
        return false;
    }

    private String reasonFor(Role role, List<Unit> workers, int player, GameState gs) {
        switch (role) {
            case COLLECTOR:
                int ourScore = gs.getPlayer(player).getResources();
                int enemyScore = gs.getPlayer(1 - player).getResources();
                return "cocoa-available-score-" + (ourScore < enemyScore ? "behind" : "competitive");
            case ESCORT:
                return isAnyCollectorEndangered(workers, player, gs)
                        ? "collector-threat" : "collector-support";
            case DEFENDER:
                return "factory-coverage";
            case ATTACKER:
                for (Unit enemy : enemyWorkers(player, gs)) {
                    if (enemy.getResources() > 0) return "enemy-cocoa-carrier";
                }
                return "enemy-interception";
            default:
                return "role-utility";
        }
    }

    private void changeRole(long unitId, Role newRole, int cycle, String reason) {
        Role oldRole = roleAssignments.put(unitId, newRole);
        if (oldRole == newRole) return;
        lastRoleChangeCycle.put(unitId, cycle);
        roleChangeCount++;
        roleChangeHistory.add("cycle=" + cycle + ",unit=" + unitId + ",from=" + oldRole
                + ",to=" + newRole + ",reason=" + reason);
    }

    public int getRoleChangeCount() {
        return roleChangeCount;
    }

    public List<String> getRoleChangeHistory() {
        return Collections.unmodifiableList(new ArrayList<>(roleChangeHistory));
    }

    public int getMatchDuration() {
        return matchDuration;
    }

    /** Extension point for experimental policies; the control policy remains unchanged. */
    protected boolean preserveCarryingCollectors() {
        return false;
    }

    @Override
    public AI clone() {
        return new OldAdaptiveRoleAI(utt, matchDuration);
    }

    @Override
    public String toString() {
        return "OldAdaptiveRoleAI";
    }
}
