package courseproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ai.PassiveAI;
import courseproject.factoryfeud.AdaptiveRoleAI;
import courseproject.factoryfeud.FactoryFeudMatch;
import courseproject.factoryfeud.FactoryFeudResult;
import courseproject.factoryfeud.FactoryFeudScenario;
import courseproject.factoryfeud.FixedRoleAI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class FactoryFeudTest {
    private UnitTypeTable utt;

    @Before
    public void setUp() {
        utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
    }

    @Test
    public void scenarioHasTwoTeamsFourWorkersAndOneFactoryEach() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        int[] workers = new int[2];
        int[] factories = new int[2];

        assertEquals(2, gs.getPhysicalGameState().getPlayers().size());
        for (Unit unit : gs.getUnits()) {
            if (unit.getPlayer() >= 0) {
                if ("Worker".equals(unit.getType().name)) workers[unit.getPlayer()]++;
                if (unit.getType().isStockpile) factories[unit.getPlayer()]++;
            }
        }

        assertEquals(4, workers[0]);
        assertEquals(4, workers[1]);
        assertEquals(1, factories[0]);
        assertEquals(1, factories[1]);
        assertEquals(0, gs.getPlayer(0).getResources());
        assertEquals(0, gs.getPlayer(1).getResources());
    }

    @Test
    public void cocoaResourcesAreNeutralAndAtExpectedSymmetricLocations() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        Set<String> actual = new HashSet<>();
        int initialCocoa = 0;
        for (Unit unit : gs.getUnits()) {
            if (unit.getType().isResource) {
                assertEquals(-1, unit.getPlayer());
                assertEquals(20, unit.getResources());
                initialCocoa += unit.getResources();
                actual.add(unit.getX() + "," + unit.getY());
            }
        }

        Set<String> expected = new HashSet<>(Arrays.asList(
                "3,5", "8,5", "5,3", "6,3", "5,7", "6,7"));
        assertEquals(expected, actual);
        assertEquals(120, initialCocoa);
    }

    @Test
    public void harvestingAndReturningIncrementsDeliveryScore() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(gs, 100);
        Unit worker = gs.getUnit(20);
        Unit cocoa = gs.getUnit(0);
        assertNotNull(worker);
        assertNotNull(cocoa);

        PlayerAction harvest = new PlayerAction();
        harvest.addUnitAction(worker, new UnitAction(UnitAction.TYPE_HARVEST, UnitAction.DIRECTION_RIGHT));
        match.step(harvest, new PlayerAction());
        while (worker.getResources() == 0) match.step(new PlayerAction(), new PlayerAction());

        assertEquals(worker.getHarvestAmount(), worker.getResources());
        assertEquals(20 - worker.getHarvestAmount(), cocoa.getResources());
        assertEquals(0, match.getScore(0));

        PlayerAction deliver = new PlayerAction();
        deliver.addUnitAction(worker, new UnitAction(UnitAction.TYPE_RETURN, UnitAction.DIRECTION_LEFT));
        match.step(deliver, new PlayerAction());
        while (gs.getPlayer(0).getResources() == 0) match.step(new PlayerAction(), new PlayerAction());

        assertEquals(0, worker.getResources());
        assertEquals(worker.getHarvestAmount(), gs.getPlayer(0).getResources());
        assertEquals(worker.getHarvestAmount(), match.getScore(0));

        while (!match.isFinished()) match.step(new PlayerAction(), new PlayerAction());
        FactoryFeudResult result = match.getResult();
        assertEquals(0, result.getWinner());
        assertEquals(worker.getHarvestAmount(), result.getScore(0));
        assertEquals(0, result.getScore(1));
    }

    @Test
    public void equalScoresResolveAsTie() {
        FactoryFeudResult result = new FactoryFeudResult(50, 3, 3);
        assertTrue(result.isTie());
        assertEquals(FactoryFeudResult.TIE, result.getWinner());
    }

    @Test
    public void timerEndsAtConfiguredCycleEvenWhenEngineReportsGameOver() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        for (Unit unit : new java.util.ArrayList<>(gs.getUnits())) {
            if (unit.getPlayer() == 1) gs.removeUnit(unit);
        }
        assertTrue(gs.gameover());

        FactoryFeudMatch match = new FactoryFeudMatch(
                gs, 7, new PassiveAI(utt), new PassiveAI(utt));
        FactoryFeudResult result = match.run();

        assertEquals(7, result.getCycles());
        assertTrue(result.isTie());
        assertTrue(match.isFinished());
    }

    @Test
    public void productionCommandsAreDiscardedAndCannotSpendScore() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        gs.getPlayer(0).setResources(10);
        FactoryFeudMatch match = new FactoryFeudMatch(gs, 60);
        int initialUnitCount = gs.getUnits().size();
        Unit factory = gs.getUnit(10);
        UnitType workerType = utt.getUnitType("Worker");

        PlayerAction produce = new PlayerAction();
        produce.addUnitAction(factory,
                new UnitAction(UnitAction.TYPE_PRODUCE, UnitAction.DIRECTION_LEFT, workerType));
        match.step(produce, new PlayerAction());
        while (!match.isFinished()) match.step(new PlayerAction(), new PlayerAction());

        assertEquals(initialUnitCount, gs.getUnits().size());
        assertEquals(10, gs.getPlayer(0).getResources());
        assertEquals(0, match.getScore(0));
        assertEquals(1, match.getFilteredProductionActionCount());
        assertFalse(gs.getUnitActions().containsKey(factory));
    }

    @Test
    public void runObserverUsesMatchCyclesAndReportsRemainingTime() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(
                gs, 5, new PassiveAI(utt), new PassiveAI(utt));
        final int[] observedCycles = {0};

        assertEquals(5, match.getTimeLimit());
        assertEquals(5, match.getRemainingCycles());
        match.run(currentMatch -> {
            observedCycles[0]++;
            assertEquals(5 - observedCycles[0], currentMatch.getRemainingCycles());
        });

        assertEquals(5, observedCycles[0]);
        assertEquals(0, match.getRemainingCycles());
    }

    @Test
    public void startingResourcesAreExcludedButSubsequentReturnIsScored() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        gs.getPlayer(0).setResources(10);
        gs.getPlayer(1).setResources(7);
        FactoryFeudMatch match = new FactoryFeudMatch(gs, 40);
        Unit worker = gs.getUnit(20);
        worker.setResources(1);

        assertEquals(0, match.getScore(0));
        assertEquals(0, match.getScore(1));

        PlayerAction deliver = new PlayerAction();
        deliver.addUnitAction(worker,
                new UnitAction(UnitAction.TYPE_RETURN, UnitAction.DIRECTION_LEFT));
        match.step(deliver, new PlayerAction());
        while (gs.getPlayer(0).getResources() == 10) {
            match.step(new PlayerAction(), new PlayerAction());
        }

        assertEquals(11, gs.getPlayer(0).getResources());
        assertEquals(1, match.getScore(0));
        assertEquals(0, match.getScore(1));
    }

    @Test
    public void tiedRoleMatchConservesCocoaAndDoesNotExhaustTheMap() throws Exception {
        GameState gs = FactoryFeudScenario.load(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(
                gs, 500, new AdaptiveRoleAI(utt, 500), new FixedRoleAI(utt));
        FactoryFeudResult result = match.run();
        int remaining = 0;
        int carried = 0;
        for (Unit unit : gs.getUnits()) {
            if (unit.getType().isResource) remaining += unit.getResources();
            else if (unit.getPlayer() >= 0) carried += unit.getResources();
        }

        assertEquals(result.getScore(0), result.getScore(1));
        assertTrue("The tie must not be caused by exhausting all cocoa", remaining > 0);
        assertEquals(120, result.getScore(0) + result.getScore(1) + remaining + carried);
    }
}
