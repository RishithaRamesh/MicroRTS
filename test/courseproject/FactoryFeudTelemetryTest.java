package courseproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import ai.PassiveAI;
import ai.core.AI;
import courseproject.factoryfeud.FactoryFeudMatch;
import courseproject.factoryfeud.FactoryFeudMatchTelemetry;
import courseproject.factoryfeud.FactoryFeudRoleAI;
import courseproject.factoryfeud.FactoryFeudScenario;
import courseproject.factoryfeud.Role;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

public class FactoryFeudTelemetryTest {
    private UnitTypeTable utt;

    @Before
    public void setUp() {
        utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
    }

    @Test
    public void deliveryTimingUsesObservedPositiveDeltas() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(gs, 25);
        Unit worker = gs.getUnit(20);
        worker.setResources(1);

        issueReturnAndAdvance(match, worker, 10);
        worker.setResources(1);
        issueReturnAndAdvance(match, worker, 20);
        while (!match.isFinished()) match.step(new PlayerAction(), new PlayerAction());

        FactoryFeudMatchTelemetry.DeliveryTiming timing =
                match.getTelemetry().getDeliveryTiming(0);
        assertEquals(Integer.valueOf(10), timing.getFirstDeliveryCycle());
        assertEquals(Integer.valueOf(20), timing.getLastDeliveryCycle());
        assertEquals(10.0, timing.getMeanDeliveryIntervalCycles(), 0.000001);
        assertEquals(2, timing.getCompletedDeliveryEvents());
        assertEquals(2, match.getScore(0));
    }

    @Test
    public void zeroDeliveriesHaveUnavailableTimingRatherThanZeroCycles() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(
                gs, 3, new PassiveAI(utt), new PassiveAI(utt));
        match.run();

        FactoryFeudMatchTelemetry.DeliveryTiming timing =
                match.getTelemetry().getDeliveryTiming(0);
        assertNull(timing.getFirstDeliveryCycle());
        assertNull(timing.getLastDeliveryCycle());
        assertNull(timing.getMeanDeliveryIntervalCycles());
        assertEquals(0, timing.getCompletedDeliveryEvents());
    }

    @Test
    public void carrierInterruptionCountsUnitCyclesOnceAndStopsOnCorrection() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        gs.getUnit(20).setResources(1);
        ScriptedRoleAI scripted = new ScriptedRoleAI(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(
                gs, 5, scripted, new PassiveAI(utt));
        match.run();

        FactoryFeudMatchTelemetry.CarrierMetrics metrics =
                match.getTelemetry().getCarrierMetrics(0, gs);
        assertEquals(1, metrics.getRoleInterruptions());
        assertEquals(3, metrics.getInterruptedCycles());
        assertEquals(1, metrics.getCorrections());
        assertEquals(0, metrics.getDeaths());
        assertEquals(1, metrics.getUndeliveredCocoaAtEnd());
    }

    @Test
    public void confirmedHitCountsButMissedAttackDoesNot() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        Unit carrier = gs.getUnit(20);
        Unit attacker = gs.getUnit(24);
        carrier.setX(5);
        carrier.setY(5);
        carrier.setResources(1);
        carrier.setHitPoints(2);
        attacker.setX(6);
        attacker.setY(5);
        FactoryFeudMatch match = new FactoryFeudMatch(gs, 11);

        issueAttackAndAdvance(match, attacker, 6, 4, 5);
        assertEquals(0, match.getTelemetry().getCarrierMetrics(0, gs).getAttacksReceived());

        issueAttackAndAdvance(match, attacker, 5, 5, 10);
        FactoryFeudMatchTelemetry.CarrierMetrics metrics =
                match.getTelemetry().getCarrierMetrics(0, gs);
        assertEquals(1, metrics.getAttacksReceived());
        assertEquals(0, metrics.getDeaths());
        assertEquals(1, metrics.getUndeliveredCocoaAtEnd());
    }

    @Test
    public void carrierDeathIsSeparateFromSurvivingCargo() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        Unit carrier = gs.getUnit(20);
        Unit attacker = gs.getUnit(24);
        carrier.setX(5);
        carrier.setY(5);
        carrier.setResources(1);
        carrier.setHitPoints(1);
        attacker.setX(6);
        attacker.setY(5);
        FactoryFeudMatch match = new FactoryFeudMatch(gs, 5);

        issueAttackAndAdvance(match, attacker, 5, 5, 5);

        FactoryFeudMatchTelemetry.CarrierMetrics metrics =
                match.getTelemetry().getCarrierMetrics(0, gs);
        assertEquals(1, metrics.getAttacksReceived());
        assertEquals(1, metrics.getDeaths());
        assertEquals(0, metrics.getUndeliveredCocoaAtEnd());
    }

    private static void issueReturnAndAdvance(FactoryFeudMatch match, Unit worker,
            int targetCycle) {
        PlayerAction action = new PlayerAction();
        action.addUnitAction(worker,
                new UnitAction(UnitAction.TYPE_RETURN, UnitAction.DIRECTION_LEFT));
        match.step(action, new PlayerAction());
        while (match.getGameState().getTime() < targetCycle) {
            match.step(new PlayerAction(), new PlayerAction());
        }
    }

    private static void issueAttackAndAdvance(FactoryFeudMatch match, Unit attacker,
            int targetX, int targetY, int targetCycle) {
        PlayerAction action = new PlayerAction();
        action.addUnitAction(attacker,
                new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, targetX, targetY));
        match.step(new PlayerAction(), action);
        while (match.getGameState().getTime() < targetCycle) {
            match.step(new PlayerAction(), new PlayerAction());
        }
    }

    private static final class ScriptedRoleAI extends FactoryFeudRoleAI {
        private ScriptedRoleAI(UnitTypeTable utt) { super(utt); }

        @Override
        public PlayerAction getAction(int player, GameState gs) {
            roleAssignments.put(20L, gs.getTime() == 0 || gs.getTime() >= 4
                    ? Role.COLLECTOR : Role.DEFENDER);
            return new PlayerAction();
        }

        @Override
        protected void initializeAssignments(List<Unit> workers, int player, GameState gs) { }

        @Override
        protected void updateAssignments(List<Unit> workers, int player, GameState gs) { }

        @Override
        public AI clone() { return new ScriptedRoleAI(utt); }
    }
}
