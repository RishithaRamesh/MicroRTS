package courseproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ai.PassiveAI;
import ai.abstraction.Harvest;
import courseproject.factoryfeud.AdaptiveRoleAI;
import courseproject.factoryfeud.CargoSafeAdaptiveRoleAI;
import courseproject.factoryfeud.FactoryFeudExperimentConfig;
import courseproject.factoryfeud.FactoryFeudMatch;
import courseproject.factoryfeud.FactoryFeudPhase2Match;
import courseproject.factoryfeud.FactoryFeudResult;
import courseproject.factoryfeud.FactoryFeudScenario;
import courseproject.factoryfeud.FactoryFeudVisualModel;
import courseproject.factoryfeud.FixedRoleAI;
import courseproject.factoryfeud.OldAdaptiveRoleAI;
import courseproject.factoryfeud.Role;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class FactoryFeudAITest {
    private UnitTypeTable utt;

    @Before
    public void setUp() {
        utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
    }

    @Test
    public void fixedAgentAssignsOneWorkerToEveryRole() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FixedRoleAI fixed = new FixedRoleAI(utt);
        fixed.getAction(0, gs);

        assertEquals(Role.COLLECTOR, fixed.getRole(20));
        assertEquals(Role.ESCORT, fixed.getRole(21));
        assertEquals(Role.ATTACKER, fixed.getRole(22));
        assertEquals(Role.DEFENDER, fixed.getRole(23));
        assertEquals(4, fixed.getRoleAssignments().size());
    }

    @Test
    public void fixedCollectorDeliversCocoa() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(
                gs, 250, new FixedRoleAI(utt), new PassiveAI(utt));
        FactoryFeudResult result = match.run();

        assertTrue(result.getScore(0) > 0);
        assertEquals(0, result.getWinner());
    }

    @Test
    public void everyFixedRoleProducesALegalNonIdleActionWhenNeeded() throws Exception {
        assertRoleProducesLegalAction(Role.COLLECTOR, -1, -1);
        assertRoleProducesLegalAction(Role.ESCORT, 2, 6);
        assertRoleProducesLegalAction(Role.ATTACKER, 3, 4);
        assertRoleProducesLegalAction(Role.DEFENDER, 2, 6);
    }

    private void assertRoleProducesLegalAction(Role role, int enemyX, int enemyY) throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        if (enemyX >= 0) {
            Unit enemy = gs.getUnit(24);
            enemy.setX(enemyX);
            enemy.setY(enemyY);
        }
        FixedRoleAI fixed = new FixedRoleAI(utt);
        PlayerAction action = fixed.getAction(0, gs);
        long unitId = role == Role.COLLECTOR ? 20
                : role == Role.ESCORT ? 21 : role == Role.ATTACKER ? 22 : 23;
        Unit worker = gs.getUnit(unitId);
        UnitAction unitAction = action.getAction(worker);

        assertNotNull("No action for " + role, unitAction);
        assertTrue("Expected active action for " + role, unitAction.getType() != UnitAction.TYPE_NONE);
        assertTrue("Illegal action for " + role + ": " + unitAction,
                worker.canExecuteAction(unitAction, gs));
    }

    @Test
    public void fixedAssignmentsRemainAfterCollectorIsEliminated() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FixedRoleAI fixed = new FixedRoleAI(utt);
        fixed.getAction(0, gs);
        Map<Long, Role> original = new LinkedHashMap<>(fixed.getRoleAssignments());

        gs.removeUnit(gs.getUnit(20));
        fixed.getAction(0, gs);

        assertEquals(original, fixed.getRoleAssignments());
        assertEquals(Role.COLLECTOR, fixed.getRole(20));
    }

    @Test
    public void adaptiveAgentChangesAssignmentForRealFactoryThreat() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        AdaptiveRoleAI adaptive = new AdaptiveRoleAI(utt, 100);
        adaptive.getAction(0, gs);
        Map<Long, Role> before = new LinkedHashMap<>(adaptive.getRoleAssignments());

        Unit enemy = gs.getUnit(24);
        Unit factory = gs.getUnit(10);
        enemy.setX(factory.getX() - 1);
        enemy.setY(factory.getY());
        adaptive.getAction(0, gs);

        assertFalse(before.equals(adaptive.getRoleAssignments()));
        assertEquals(2, roleCount(adaptive, Role.DEFENDER));
        assertEquals(1, adaptive.getRoleChangeCount());
        assertTrue(adaptive.getRoleChangeHistory().get(0).contains("emergency-factory-threat"));
    }

    @Test
    public void cargoSafeAdaptiveDoesNotDivertCarrierForEmergencyDefense() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        CargoSafeAdaptiveRoleAI adaptive = new CargoSafeAdaptiveRoleAI(utt, 100);
        adaptive.getAction(0, gs);

        Unit carrier = unitWithRole(gs, adaptive, 0, Role.COLLECTOR);
        carrier.setResources(1);
        Unit enemy = gs.getUnit(24);
        Unit factory = gs.getUnit(10);
        enemy.setX(factory.getX() - 1);
        enemy.setY(factory.getY());
        adaptive.getAction(0, gs);

        assertEquals(Role.COLLECTOR, adaptive.getRole(carrier.getID()));
        assertEquals(2, roleCount(adaptive, Role.DEFENDER));
        assertEquals(1, adaptive.getRoleChangeCount());
        assertFalse(adaptive.getRoleChangeHistory().get(0)
                .contains("unit=" + carrier.getID() + ","));
    }

    @Test
    public void cargoSafeAdaptivePreservesEveryCarrierDuringReevaluation() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        CargoSafeAdaptiveRoleAI adaptive = new CargoSafeAdaptiveRoleAI(utt, 100);
        adaptive.getAction(0, gs);

        gs.getUnit(20).setResources(1);
        gs.getUnit(21).setResources(1);
        for (int cycle = 0; cycle < AdaptiveRoleAI.REEVALUATION_INTERVAL; cycle++) gs.cycle();
        adaptive.getAction(0, gs);

        assertEquals(Role.COLLECTOR, adaptive.getRole(20));
        assertEquals(Role.COLLECTOR, adaptive.getRole(21));
    }

    @Test
    public void cargoSafeCloneAndResetHaveIndependentState() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        CargoSafeAdaptiveRoleAI adaptive = new CargoSafeAdaptiveRoleAI(utt, 100);
        adaptive.getAction(0, gs);

        CargoSafeAdaptiveRoleAI clone = (CargoSafeAdaptiveRoleAI) adaptive.clone();
        assertTrue(clone.getRoleAssignments().isEmpty());
        assertEquals(0, clone.getRoleChangeCount());

        adaptive.reset();
        assertTrue(adaptive.getRoleAssignments().isEmpty());
        assertEquals(0, adaptive.getRoleChangeCount());
    }

    @Test
    public void cargoSafeAdaptiveCompletesRefinedMapFromBothSidesWithoutDivertingCargo() throws Exception {
        for (int adaptivePlayer = 0; adaptivePlayer < 2; adaptivePlayer++) {
            GameState gs = FactoryFeudScenario.load(
                    FactoryFeudScenario.REFINED_NARROW_PASSAGE_MAP, utt);
            final CargoSafeAdaptiveRoleAI adaptive = new CargoSafeAdaptiveRoleAI(utt, 500);
            FixedRoleAI fixed = new FixedRoleAI(utt);
            FactoryFeudMatch match = adaptivePlayer == 0
                    ? new FactoryFeudMatch(gs, 500, adaptive, fixed)
                    : new FactoryFeudMatch(gs, 500, fixed, adaptive);
            final int observedPlayer = adaptivePlayer;

            FactoryFeudResult result = match.run(currentMatch -> {
                for (Unit worker : currentMatch.getGameState().getUnits()) {
                    if (worker.getPlayer() == observedPlayer && worker.getType().canHarvest
                            && worker.getResources() > 0) {
                        assertEquals(Role.COLLECTOR, adaptive.getRole(worker.getID()));
                    }
                }
            });

            assertEquals(500, result.getCycles());
            assertEquals(0, match.getIllegalActionCount());
        }
    }

    @Test
    public void adaptiveAssignmentsDoNotOscillateInStableOpening() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        AdaptiveRoleAI adaptive = new AdaptiveRoleAI(utt, 20);
        FactoryFeudMatch match = new FactoryFeudMatch(
                gs, 20, adaptive, new PassiveAI(utt));
        match.run();

        assertEquals(0, adaptive.getRoleChangeCount());
    }

    @Test
    public void adaptiveReplacesEliminatedCollectorWithoutCrashing() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        AdaptiveRoleAI adaptive = new AdaptiveRoleAI(utt, 100);
        adaptive.getAction(0, gs);
        Unit collector = unitWithRole(gs, adaptive, 0, Role.COLLECTOR);
        assertNotNull(collector);

        gs.removeUnit(collector);
        for (int cycle = 0; cycle < AdaptiveRoleAI.REEVALUATION_INTERVAL; cycle++) gs.cycle();
        adaptive.getAction(0, gs);

        boolean liveCollector = false;
        for (Unit unit : gs.getUnits()) {
            if (unit.getPlayer() == 0 && adaptive.getRole(unit.getID()) == Role.COLLECTOR) {
                liveCollector = true;
            }
        }
        assertTrue(liveCollector);
    }

    @Test
    public void cloneAndResetDoNotShareOrRetainAssignmentState() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FixedRoleAI fixed = new FixedRoleAI(utt);
        AdaptiveRoleAI adaptive = new AdaptiveRoleAI(utt, 100);
        fixed.getAction(0, gs);
        adaptive.getAction(0, gs);

        FixedRoleAI fixedClone = (FixedRoleAI) fixed.clone();
        AdaptiveRoleAI adaptiveClone = (AdaptiveRoleAI) adaptive.clone();
        assertTrue(fixedClone.getRoleAssignments().isEmpty());
        assertTrue(adaptiveClone.getRoleAssignments().isEmpty());
        assertEquals(0, adaptiveClone.getRoleChangeCount());

        fixed.reset();
        adaptive.reset();
        assertTrue(fixed.getRoleAssignments().isEmpty());
        assertTrue(adaptive.getRoleAssignments().isEmpty());
        assertEquals(0, adaptive.getRoleChangeCount());
    }

    @Test
    public void adaptiveAndFixedCompleteEndToEndMatch() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        AdaptiveRoleAI adaptive = new AdaptiveRoleAI(utt, 200);
        FixedRoleAI fixed = new FixedRoleAI(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(gs, 200, adaptive, fixed);

        FactoryFeudResult result = match.run();
        assertEquals(200, result.getCycles());
        assertTrue(result.getScore(0) >= 0);
        assertTrue(result.getScore(1) >= 0);
        assertEquals(0, match.getIllegalActionCount());
    }

    @Test
    public void collectorSkipsBlockedCocoaAndSelectsAReachableNode() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        Unit collector = gs.getUnit(20);
        collector.setX(0);
        collector.setY(0);
        Unit blocked = gs.getUnit(0);
        blocked.setX(3);
        blocked.setY(5);
        UnitType resourceType = utt.getUnitType("Resource");
        gs.getPhysicalGameState().addUnit(new Unit(1, -1, resourceType, 8, 5, 120));
        gs.getPhysicalGameState().setTerrain(2, 5, 1);
        gs.getPhysicalGameState().setTerrain(4, 5, 1);
        gs.getPhysicalGameState().setTerrain(3, 4, 1);
        gs.getPhysicalGameState().setTerrain(3, 6, 1);

        FixedRoleAI fixed = new FixedRoleAI(utt);
        fixed.getAction(0, gs);

        assertTrue(fixed.getAbstractAction(collector) instanceof Harvest);
        Harvest action = (Harvest) fixed.getAbstractAction(collector);
        assertEquals(1, action.getTarget().getID());
    }

    @Test
    public void visualModelReflectsLiveRolesAndAdaptiveEvents() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        AdaptiveRoleAI adaptive = new AdaptiveRoleAI(utt, 100);
        FixedRoleAI fixed = new FixedRoleAI(utt);
        adaptive.getAction(0, gs);
        fixed.getAction(1, gs);
        Unit collector = unitWithRole(gs, adaptive, 0, Role.COLLECTOR);
        FactoryFeudVisualModel model = new FactoryFeudVisualModel(
                adaptive, fixed, "P0 AdaptiveRoleAI vs P1 FixedRoleAI");

        String opening = model.rolePanelText(gs);
        assertTrue(opening.contains("P0 AdaptiveRoleAI"));
        assertTrue(opening.contains("P1 FixedRoleAI"));
        assertTrue(opening.contains("#" + collector.getID() + "  C  Collector"));
        assertTrue(opening.contains("No reassignment events yet"));

        Unit enemy = gs.getUnit(24);
        Unit factory = gs.getUnit(10);
        enemy.setX(factory.getX() - 1);
        enemy.setY(factory.getY());
        adaptive.getAction(0, gs);
        String threatened = model.rolePanelText(gs);

        assertTrue(threatened.contains("Adaptive changes: 1"));
        assertTrue(threatened.contains("Cycle 0 | Unit #"));
        assertTrue(threatened.contains(" -> DEFENDER"));
        assertTrue(threatened.contains("Reason: emergency-factory-threat"));
        long changedUnit = changedUnitId(adaptive);
        assertTrue(threatened.contains("#" + changedUnit + "  D  Defender"));

        gs.removeUnit(gs.getUnit(changedUnit));
        assertFalse(model.rolePanelText(gs).contains("#" + changedUnit + "  "));
    }

    @Test
    public void visualModelShowsDefaultFixedP0AndAdaptiveP1Assignments()
            throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FixedRoleAI fixed = new FixedRoleAI(utt);
        AdaptiveRoleAI adaptive = new AdaptiveRoleAI(utt, 500);
        fixed.getAction(0, gs);
        adaptive.getAction(1, gs);
        FactoryFeudVisualModel model = new FactoryFeudVisualModel(
                adaptive, fixed, 1,
                "P0 (Blue/Left) FixedRoleAI vs P1 (Pink/Right) "
                        + "AdaptiveRoleAI");

        String roles = model.rolePanelText(gs);
        assertTrue(roles.contains("P0 FixedRoleAI"));
        assertTrue(roles.contains("P1 AdaptiveRoleAI"));
        assertTrue(model.hudText(new FactoryFeudMatch(gs, 500, fixed, adaptive))
                .contains("P0 (Blue/Left) FixedRoleAI vs P1 (Pink/Right) "
                        + "AdaptiveRoleAI"));
    }

    @Test
    public void headlessRoleMatchDefaultsToFixedP0AndAdaptiveP1() {
        assertEquals(0, FactoryFeudPhase2Match.FIXED_PLAYER);
        assertEquals(1, FactoryFeudPhase2Match.ADAPTIVE_PLAYER);
        assertEquals(FactoryFeudExperimentConfig.AdaptiveStrategy.ADAPTIVE,
                FactoryFeudPhase2Match.DEFAULT_STRATEGY);
    }

    private static Unit unitWithRole(GameState gs, OldAdaptiveRoleAI adaptive,
            int player, Role role) {
        for (Unit unit : gs.getUnits()) {
            if (unit.getPlayer() == player && adaptive.getRole(unit.getID()) == role) return unit;
        }
        return null;
    }

    private static int roleCount(OldAdaptiveRoleAI adaptive, Role role) {
        int count = 0;
        for (Role assignment : adaptive.getRoleAssignments().values()) {
            if (assignment == role) count++;
        }
        return count;
    }

    private static long changedUnitId(OldAdaptiveRoleAI adaptive) {
        String event = adaptive.getRoleChangeHistory().get(0);
        int start = event.indexOf("unit=") + 5;
        return Long.parseLong(event.substring(start, event.indexOf(',', start)));
    }
}
