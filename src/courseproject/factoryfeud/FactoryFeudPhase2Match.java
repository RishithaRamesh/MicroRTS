package courseproject.factoryfeud;

import ai.abstraction.AbstractAction;
import ai.abstraction.Idle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rts.GameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/** Runs one complete headless AdaptiveRoleAI versus FixedRoleAI match. */
public final class FactoryFeudPhase2Match {
    public static final int DEFAULT_TIME_LIMIT = 500;
    private static final int STUCK_THRESHOLD = 50;

    private FactoryFeudPhase2Match() {
    }

    public static void main(String[] args) throws Exception {
        int timeLimit = args.length == 0 ? DEFAULT_TIME_LIMIT : Integer.parseInt(args[0]);
        UnitTypeTable utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        GameState gameState = FactoryFeudScenario.load(utt);
        AdaptiveRoleAI adaptive = new AdaptiveRoleAI(utt, timeLimit);
        FixedRoleAI fixed = new FixedRoleAI(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(gameState, timeLimit, adaptive, fixed);
        ProgressTracker progress = new ProgressTracker(adaptive, fixed);

        long start = System.nanoTime();
        FactoryFeudResult result = match.run(currentMatch ->
                progress.observe(currentMatch.getGameState()));
        long runtimeMillis = (System.nanoTime() - start) / 1_000_000L;

        System.out.println(result);
        System.out.println("survivingUnitsP0=" + survivingUnits(gameState, 0));
        System.out.println("survivingUnitsP1=" + survivingUnits(gameState, 1));
        System.out.println("adaptiveRoleChanges=" + adaptive.getRoleChangeCount());
        for (String change : adaptive.getRoleChangeHistory()) System.out.println("roleChange=" + change);
        System.out.println("illegalActionsProposed=" + match.getIllegalActionCount());
        System.out.println("productionActionsFiltered=" + match.getFilteredProductionActionCount());
        System.out.println("potentiallyStuckWorkerIds=" + progress.getPotentiallyStuckUnitIds()
                + " (threshold=" + STUCK_THRESHOLD + " unchanged active cycles)");
        System.out.println("runtimeMillis=" + runtimeMillis);
    }

    private static List<String> survivingUnits(GameState gameState, int player) {
        List<String> survivors = new ArrayList<>();
        for (Unit unit : gameState.getUnits()) {
            if (unit.getPlayer() == player) {
                survivors.add("#" + unit.getID() + ":" + unit.getType().name);
            }
        }
        return survivors;
    }

    private static final class ProgressTracker {
        private final FactoryFeudRoleAI[] agents;
        private final Map<Long, String> lastSignatures = new LinkedHashMap<>();
        private final Map<Long, Integer> unchangedActiveCycles = new LinkedHashMap<>();
        private final Set<Long> potentiallyStuckUnitIds = new LinkedHashSet<>();

        private ProgressTracker(FactoryFeudRoleAI player0, FactoryFeudRoleAI player1) {
            agents = new FactoryFeudRoleAI[]{player0, player1};
        }

        private void observe(GameState gameState) {
            for (Unit unit : gameState.getUnits()) {
                if (unit.getPlayer() < 0 || unit.getPlayer() > 1 || !unit.getType().canHarvest) {
                    continue;
                }
                AbstractAction action = agents[unit.getPlayer()].getAbstractAction(unit);
                boolean active = action != null && !(action instanceof Idle);
                String signature = unit.getX() + ":" + unit.getY() + ":"
                        + unit.getResources() + ":" + unit.getHitPoints();
                int unchanged = active && signature.equals(lastSignatures.get(unit.getID()))
                        ? unchangedActiveCycles.getOrDefault(unit.getID(), 0) + 1 : 0;
                lastSignatures.put(unit.getID(), signature);
                unchangedActiveCycles.put(unit.getID(), unchanged);
                if (unchanged >= STUCK_THRESHOLD) potentiallyStuckUnitIds.add(unit.getID());
            }
        }

        private Set<Long> getPotentiallyStuckUnitIds() {
            return new LinkedHashSet<>(potentiallyStuckUnitIds);
        }
    }
}
