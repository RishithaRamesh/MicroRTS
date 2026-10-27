package courseproject.factoryfeud;

import ai.core.AI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import rts.GameState;
import rts.units.Unit;

/**
 * Project-owned, state-difference telemetry for one Factory Feud match.
 *
 * Carrier hits are confirmed from an HP decrease or a carrying Worker's removal
 * during a cycle. Attack commands and target selections are deliberately ignored.
 */
public final class FactoryFeudMatchTelemetry {
    private final int configuredCycles;
    private final FactoryFeudRoleAI[] roleAgents = new FactoryFeudRoleAI[2];
    private final SideAccumulator[] sides = {new SideAccumulator(), new SideAccumulator()};
    private final List<Integer>[] deliveryCycles;
    private Map<Long, WorkerSnapshot> beforeCycle = Collections.emptyMap();
    private int completedCycles;

    @SuppressWarnings("unchecked")
    FactoryFeudMatchTelemetry(int configuredCycles, AI[] agents) {
        this.configuredCycles = configuredCycles;
        deliveryCycles = new List[]{new ArrayList<Integer>(), new ArrayList<Integer>()};
        for (int player = 0; player < 2; player++) {
            if (agents[player] instanceof FactoryFeudRoleAI) {
                roleAgents[player] = (FactoryFeudRoleAI) agents[player];
                sides[player].roleMetricsAvailable = true;
            }
        }
    }

    void beforeCycle(GameState gameState) {
        Map<Long, WorkerSnapshot> snapshots = new LinkedHashMap<>();
        for (Unit unit : gameState.getUnits()) {
            int player = unit.getPlayer();
            if (player < 0 || player > 1 || !unit.getType().canHarvest) continue;
            Role currentRole = roleAgents[player] == null
                    ? null : roleAgents[player].getRole(unit.getID());
            Role previousRole = sides[player].previousRoles.get(unit.getID());
            if (unit.getResources() > 0 && currentRole != null) {
                if (previousRole == Role.COLLECTOR && currentRole != Role.COLLECTOR) {
                    sides[player].carrierRoleInterruptions++;
                } else if (previousRole != null && previousRole != Role.COLLECTOR
                        && currentRole == Role.COLLECTOR) {
                    sides[player].carrierCorrections++;
                }
                if (currentRole != Role.COLLECTOR) {
                    sides[player].carrierInterruptedCycles++;
                }
            }
            if (currentRole != null) sides[player].previousRoles.put(unit.getID(), currentRole);
            snapshots.put(unit.getID(), new WorkerSnapshot(unit));
        }
        beforeCycle = snapshots;
    }

    void afterCycle(GameState gameState, int[] deliveryDeltas) {
        completedCycles = gameState.getTime();
        for (int player = 0; player < 2; player++) {
            if (deliveryDeltas[player] > 0) deliveryCycles[player].add(gameState.getTime());
        }

        Map<Long, Unit> survivingWorkers = new HashMap<>();
        for (Unit unit : gameState.getUnits()) {
            if (unit.getPlayer() >= 0 && unit.getPlayer() <= 1 && unit.getType().canHarvest) {
                survivingWorkers.put(unit.getID(), unit);
            }
        }
        for (Map.Entry<Long, WorkerSnapshot> entry : beforeCycle.entrySet()) {
            WorkerSnapshot before = entry.getValue();
            if (before.carriedCocoa <= 0) continue;
            Unit after = survivingWorkers.get(entry.getKey());
            if (after == null) {
                sides[before.player].carrierDeaths++;
                sides[before.player].carrierAttacksReceived++;
            } else if (after.getHitPoints() < before.hitPoints) {
                sides[before.player].carrierAttacksReceived++;
            }
        }
    }

    public int getConfiguredCycles() {
        return configuredCycles;
    }

    public int getCompletedCycles() {
        return completedCycles;
    }

    public DeliveryTiming getDeliveryTiming(int player) {
        validatePlayer(player);
        return new DeliveryTiming(deliveryCycles[player]);
    }

    public CarrierMetrics getCarrierMetrics(int player, GameState finalState) {
        validatePlayer(player);
        int undelivered = 0;
        for (Unit unit : finalState.getUnits()) {
            if (unit.getPlayer() == player && unit.getType().canHarvest) {
                undelivered += unit.getResources();
            }
        }
        SideAccumulator side = sides[player];
        return new CarrierMetrics(side.roleMetricsAvailable,
                side.carrierRoleInterruptions, side.carrierInterruptedCycles,
                side.carrierAttacksReceived, side.carrierDeaths, undelivered,
                side.carrierCorrections);
    }

    private static void validatePlayer(int player) {
        if (player < 0 || player > 1) throw new IllegalArgumentException("Player must be 0 or 1");
    }

    /** Immutable delivery-event timing for one team. */
    public static final class DeliveryTiming {
        private final List<Integer> cycles;

        private DeliveryTiming(List<Integer> cycles) {
            this.cycles = Collections.unmodifiableList(new ArrayList<>(cycles));
        }

        public List<Integer> getCycles() { return cycles; }
        public Integer getFirstDeliveryCycle() {
            return cycles.isEmpty() ? null : cycles.get(0);
        }
        public Integer getLastDeliveryCycle() {
            return cycles.isEmpty() ? null : cycles.get(cycles.size() - 1);
        }
        public Double getMeanDeliveryIntervalCycles() {
            if (cycles.size() < 2) return null;
            long total = 0;
            for (int index = 1; index < cycles.size(); index++) {
                total += cycles.get(index) - cycles.get(index - 1);
            }
            return total / (double) (cycles.size() - 1);
        }
        public int getCompletedDeliveryEvents() { return cycles.size(); }
    }

    /** Immutable carrier telemetry for one player/strategy. */
    public static final class CarrierMetrics {
        private final boolean roleMetricsAvailable;
        private final int roleInterruptions;
        private final int interruptedCycles;
        private final int attacksReceived;
        private final int deaths;
        private final int undeliveredCocoaAtEnd;
        private final int corrections;

        private CarrierMetrics(boolean roleMetricsAvailable, int roleInterruptions,
                int interruptedCycles, int attacksReceived, int deaths,
                int undeliveredCocoaAtEnd, int corrections) {
            this.roleMetricsAvailable = roleMetricsAvailable;
            this.roleInterruptions = roleInterruptions;
            this.interruptedCycles = interruptedCycles;
            this.attacksReceived = attacksReceived;
            this.deaths = deaths;
            this.undeliveredCocoaAtEnd = undeliveredCocoaAtEnd;
            this.corrections = corrections;
        }

        public boolean areRoleMetricsAvailable() { return roleMetricsAvailable; }
        public int getRoleInterruptions() { return roleInterruptions; }
        public int getInterruptedCycles() { return interruptedCycles; }
        public int getAttacksReceived() { return attacksReceived; }
        public int getDeaths() { return deaths; }
        public int getUndeliveredCocoaAtEnd() { return undeliveredCocoaAtEnd; }
        public int getCorrections() { return corrections; }
    }

    private static final class SideAccumulator {
        private final Map<Long, Role> previousRoles = new HashMap<>();
        private boolean roleMetricsAvailable;
        private int carrierRoleInterruptions;
        private int carrierInterruptedCycles;
        private int carrierAttacksReceived;
        private int carrierDeaths;
        private int carrierCorrections;
    }

    private static final class WorkerSnapshot {
        private final int player;
        private final int hitPoints;
        private final int carriedCocoa;

        private WorkerSnapshot(Unit unit) {
            player = unit.getPlayer();
            hitPoints = unit.getHitPoints();
            carriedCocoa = unit.getResources();
        }
    }
}
