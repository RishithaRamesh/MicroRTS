package courseproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ai.abstraction.Harvest;
import courseproject.factoryfeud.AdaptiveRoleAI;
import courseproject.factoryfeud.FactoryFeudScenario;
import courseproject.factoryfeud.FactoryFeudExperimentConfig;
import courseproject.factoryfeud.FactoryFeudVisualConfig;
import courseproject.factoryfeud.Role;
import courseproject.factoryfeud.ThreatAwareAdaptiveRoleAI;
import org.junit.Before;
import org.junit.Test;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

public class ThreatAwareAdaptiveRoleAITest {
    private UnitTypeTable utt;

    @Before
    public void setUp() {
        utt = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
    }

    @Test
    public void collectorRetreatsFromOneMoveThreat() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        ThreatAwareAdaptiveRoleAI adaptive = new ThreatAwareAdaptiveRoleAI(utt, 100);
        adaptive.getAction(0, gs);
        Unit collector = workerWithRole(gs, adaptive, Role.COLLECTOR);
        Unit enemy = gs.getUnit(24);
        collector.setX(4);
        collector.setY(5);
        enemy.setX(4);
        enemy.setY(7);

        PlayerAction action = adaptive.getAction(0, gs);
        UnitAction retreat = action.getAction(collector);

        assertNotNull(retreat);
        assertEquals(UnitAction.TYPE_MOVE, retreat.getType());
        int destinationX = collector.getX()
                + UnitAction.DIRECTION_OFFSET_X[retreat.getDirection()];
        int destinationY = collector.getY()
                + UnitAction.DIRECTION_OFFSET_Y[retreat.getDirection()];
        int oldDistance = Math.abs(collector.getX() - enemy.getX())
                + Math.abs(collector.getY() - enemy.getY());
        int newDistance = Math.abs(destinationX - enemy.getX())
                + Math.abs(destinationY - enemy.getY());
        assertTrue(newDistance > oldDistance);
        assertEquals(1, adaptive.getTacticalOverrideCount());
        assertEquals(1, adaptive.getTacticalRetreatOverrideCount());
        assertTrue(adaptive.getTacticalOverrideHistory().get(0).contains("type=retreat"));
    }

    @Test
    public void escortCounterattacksImmediateLethalThreat() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        ThreatAwareAdaptiveRoleAI adaptive = new ThreatAwareAdaptiveRoleAI(utt, 100);
        adaptive.getAction(0, gs);
        Unit escort = workerWithRole(gs, adaptive, Role.ESCORT);
        Unit enemy = gs.getUnit(24);
        escort.setX(4);
        escort.setY(5);
        enemy.setX(4);
        enemy.setY(6);

        PlayerAction action = adaptive.getAction(0, gs);
        UnitAction counterattack = action.getAction(escort);

        assertNotNull(counterattack);
        assertEquals(UnitAction.TYPE_ATTACK_LOCATION, counterattack.getType());
        assertEquals(enemy.getX(), counterattack.getLocationX());
        assertEquals(enemy.getY(), counterattack.getLocationY());
        assertEquals(1, adaptive.getTacticalAttackOverrideCount());
        assertTrue(adaptive.getTacticalOverrideHistory().get(0).contains("self-defense"));
    }

    @Test
    public void enemyMoreThanOneMoveAwayDoesNotTriggerOverride() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        ThreatAwareAdaptiveRoleAI adaptive = new ThreatAwareAdaptiveRoleAI(utt, 100);
        adaptive.getAction(0, gs);
        Unit collector = workerWithRole(gs, adaptive, Role.COLLECTOR);
        Unit enemy = gs.getUnit(24);
        collector.setX(4);
        collector.setY(4);
        enemy.setX(4);
        enemy.setY(7);

        adaptive.getAction(0, gs);

        assertEquals(0, adaptive.getTacticalOverrideCount());
        assertTrue(adaptive.getAbstractAction(collector) instanceof Harvest);
    }

    @Test
    public void cloneAndResetClearTacticalState() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        ThreatAwareAdaptiveRoleAI adaptive = new ThreatAwareAdaptiveRoleAI(utt, 100);
        adaptive.getAction(0, gs);
        Unit escort = workerWithRole(gs, adaptive, Role.ESCORT);
        Unit enemy = gs.getUnit(24);
        enemy.setX(escort.getX() + 1);
        enemy.setY(escort.getY());
        adaptive.getAction(0, gs);
        assertTrue(adaptive.getTacticalOverrideCount() > 0);

        ThreatAwareAdaptiveRoleAI clone = (ThreatAwareAdaptiveRoleAI) adaptive.clone();
        assertEquals(0, clone.getTacticalOverrideCount());
        adaptive.reset();
        assertEquals(0, adaptive.getTacticalOverrideCount());
        assertTrue(adaptive.getTacticalOverrideHistory().isEmpty());
    }

    @Test
    public void visualConfigurationSelectsThreatAwareStrategy() {
        FactoryFeudVisualConfig config = FactoryFeudVisualConfig.parse(new String[]{
            "500", "0", FactoryFeudScenario.DEFAULT_MAP, "threat-aware"
        });

        assertEquals(FactoryFeudExperimentConfig.AdaptiveStrategy.THREAT_AWARE,
                config.getAdaptiveStrategy());
    }

    private static Unit workerWithRole(
            GameState gs, AdaptiveRoleAI adaptive, Role expectedRole) {
        for (Unit unit : gs.getUnits()) {
            if (unit.getPlayer() == 0 && unit.getType().canHarvest
                    && adaptive.getRole(unit.getID()) == expectedRole) return unit;
        }
        throw new AssertionError("No worker assigned role " + expectedRole);
    }
}
