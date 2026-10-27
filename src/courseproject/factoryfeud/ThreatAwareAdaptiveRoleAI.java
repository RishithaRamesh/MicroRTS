package courseproject.factoryfeud;

import ai.core.AI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/** AdaptiveRoleAI with a local, one-move tactical safety check. */
public final class ThreatAwareAdaptiveRoleAI extends AdaptiveRoleAI {
    private final List<String> tacticalOverrideHistory = new ArrayList<>();
    private int tacticalAttackOverrides;
    private int tacticalRetreatOverrides;

    public ThreatAwareAdaptiveRoleAI(UnitTypeTable utt) {
        super(utt);
    }

    public ThreatAwareAdaptiveRoleAI(UnitTypeTable utt, int matchDuration) {
        super(utt, matchDuration);
    }

    @Override
    protected void resetAllocationState() {
        super.resetAllocationState();
        tacticalOverrideHistory.clear();
        tacticalAttackOverrides = 0;
        tacticalRetreatOverrides = 0;
    }

    @Override
    protected void executeRole(Unit worker, Role role, int player, GameState gs) {
        if (!applyImmediateThreatOverride(worker, role, player, gs)) {
            super.executeRole(worker, role, player, gs);
        }
    }

    private boolean applyImmediateThreatOverride(
            Unit worker, Role role, int player, GameState gs) {
        if (gs.getActionAssignment(worker) != null) return false;

        Unit immediateThreat = bestImmediateThreat(worker, player, gs);
        if (immediateThreat != null && canAttackNow(worker, immediateThreat)
                && worker.getMaxDamage() >= immediateThreat.getHitPoints()
                && worker.getAttackTime() <= immediateThreat.getAttackTime()) {
            attack(worker, immediateThreat);
            recordAttack(gs, worker, immediateThreat, "self-defense");
            return true;
        }

        if ((role == Role.COLLECTOR || role == Role.ESCORT)
                && hasOneMoveThreat(worker, player, gs)) {
            int[] retreat = safestAdjacentPosition(worker, player, gs);
            if (retreat != null) {
                move(worker, retreat[0], retreat[1]);
                tacticalRetreatOverrides++;
                tacticalOverrideHistory.add("cycle=" + gs.getTime() + ",unit="
                        + worker.getID() + ",type=retreat,to=" + retreat[0] + ":"
                        + retreat[1] + ",role=" + role);
                return true;
            }
        }

        Unit protectedThreat = bestThreatToPriorityFriendly(worker, player, gs);
        if (protectedThreat != null && canAttackNow(worker, protectedThreat)
                && worker.getMaxDamage() >= protectedThreat.getHitPoints()
                && worker.getAttackTime() <= protectedThreat.getMoveTime()) {
            attack(worker, protectedThreat);
            recordAttack(gs, worker, protectedThreat, "protect-friendly");
            return true;
        }
        return false;
    }

    private Unit bestImmediateThreat(Unit worker, int player, GameState gs) {
        Unit best = null;
        int bestEta = Integer.MAX_VALUE;
        for (Unit enemy : enemyWorkers(player, gs)) {
            if (!canAttackNow(enemy, worker)) continue;
            int eta = enemy.getAttackTime();
            if (eta < bestEta || (eta == bestEta && enemy.getID() < best.getID())) {
                best = enemy;
                bestEta = eta;
            }
        }
        return best;
    }

    private boolean hasOneMoveThreat(Unit worker, int player, GameState gs) {
        int currentEta = minimumEnemyAttackEta(worker.getX(), worker.getY(), player, gs);
        return currentEta > worker.getAttackTime()
                && currentEta <= worker.getMoveTime() + worker.getAttackTime();
    }

    private Unit bestThreatToPriorityFriendly(Unit worker, int player, GameState gs) {
        Unit best = null;
        int bestPriority = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        for (Unit friendly : friendlyWorkers(player, gs)) {
            Role friendlyRole = roleAssignments.get(friendly.getID());
            if (friendlyRole != Role.COLLECTOR && friendlyRole != Role.ESCORT) continue;
            for (Unit enemy : enemyWorkers(player, gs)) {
                if (!canAttackNow(enemy, friendly) || !canAttackNow(worker, enemy)) continue;
                int priority = friendlyRole == Role.COLLECTOR ? 0 : 1;
                int distance = manhattan(worker, enemy);
                if (priority < bestPriority
                        || (priority == bestPriority && distance < bestDistance)
                        || (priority == bestPriority && distance == bestDistance
                                && (best == null || enemy.getID() < best.getID()))) {
                    best = enemy;
                    bestPriority = priority;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private int[] safestAdjacentPosition(Unit worker, int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit factory = friendlyFactory(player, gs);
        int currentEta = minimumEnemyAttackEta(worker.getX(), worker.getY(), player, gs);
        int[][] offsets = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
        int[] best = null;
        int bestEta = currentEta;
        int bestFactoryDistance = Integer.MAX_VALUE;
        int bestPosition = Integer.MAX_VALUE;
        for (int[] offset : offsets) {
            int x = worker.getX() + offset[0];
            int y = worker.getY() + offset[1];
            if (x < 0 || y < 0 || x >= pgs.getWidth() || y >= pgs.getHeight()) continue;
            if (!gs.free(x, y)) continue;
            int eta = minimumEnemyAttackEta(x, y, player, gs);
            if (eta <= currentEta) continue;
            int factoryDistance = factory == null ? 0
                    : Math.abs(factory.getX() - x) + Math.abs(factory.getY() - y);
            int position = x + y * pgs.getWidth();
            if (eta > bestEta
                    || (eta == bestEta && factoryDistance < bestFactoryDistance)
                    || (eta == bestEta && factoryDistance == bestFactoryDistance
                            && position < bestPosition)) {
                best = new int[]{x, y};
                bestEta = eta;
                bestFactoryDistance = factoryDistance;
                bestPosition = position;
            }
        }
        return best;
    }

    private int minimumEnemyAttackEta(int x, int y, int player, GameState gs) {
        int best = Integer.MAX_VALUE;
        for (Unit enemy : enemyWorkers(player, gs)) {
            int distance = Math.abs(enemy.getX() - x) + Math.abs(enemy.getY() - y);
            int movementSteps = Math.max(0, distance - enemy.getAttackRange());
            int eta = movementSteps * enemy.getMoveTime() + enemy.getAttackTime();
            best = Math.min(best, eta);
        }
        return best;
    }

    private static boolean canAttackNow(Unit attacker, Unit target) {
        int dx = attacker.getX() - target.getX();
        int dy = attacker.getY() - target.getY();
        return dx * dx + dy * dy <= attacker.getAttackRange() * attacker.getAttackRange();
    }

    private void recordAttack(GameState gs, Unit worker, Unit threat, String reason) {
        tacticalAttackOverrides++;
        tacticalOverrideHistory.add("cycle=" + gs.getTime() + ",unit=" + worker.getID()
                + ",type=attack,target=" + threat.getID() + ",reason=" + reason);
    }

    public int getTacticalOverrideCount() {
        return tacticalAttackOverrides + tacticalRetreatOverrides;
    }

    public int getTacticalAttackOverrideCount() {
        return tacticalAttackOverrides;
    }

    public int getTacticalRetreatOverrideCount() {
        return tacticalRetreatOverrides;
    }

    public List<String> getTacticalOverrideHistory() {
        return Collections.unmodifiableList(new ArrayList<>(tacticalOverrideHistory));
    }

    @Override
    public AI clone() {
        return new ThreatAwareAdaptiveRoleAI(utt, getMatchDuration());
    }

    @Override
    public String toString() {
        return "ThreatAwareAdaptiveRoleAI";
    }
}
