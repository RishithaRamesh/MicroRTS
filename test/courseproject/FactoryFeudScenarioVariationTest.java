package courseproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ai.PassiveAI;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import courseproject.factoryfeud.AdaptiveRoleAI;
import courseproject.factoryfeud.FactoryFeudMatch;
import courseproject.factoryfeud.FactoryFeudExperimentConfig;
import courseproject.factoryfeud.FactoryFeudResult;
import courseproject.factoryfeud.FactoryFeudScenario;
import courseproject.factoryfeud.FactoryFeudVisualConfig;
import courseproject.factoryfeud.FixedRoleAI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

public class FactoryFeudScenarioVariationTest {
    private UnitTypeTable utt;

    @Before
    public void setUp() {
        utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
    }

    @Test
    public void everyEvaluationMapIsStandardizedValidAndReachable() throws Exception {
        assertEquals(5, FactoryFeudScenario.evaluationMaps().size());
        for (String map : FactoryFeudScenario.evaluationMaps()) {
            GameState gs = FactoryFeudScenario.load(map, utt);
            PhysicalGameState pgs = gs.getPhysicalGameState();
            int[] workers = new int[2];
            int[] factories = new int[2];
            int cocoa = 0;
            int cocoaTotal = 0;
            for (Unit unit : gs.getUnits()) {
                if (unit.getType().isResource) {
                    cocoa++;
                    cocoaTotal += unit.getResources();
                } else if (unit.getPlayer() >= 0) {
                    if (unit.getType().canHarvest) {
                        workers[unit.getPlayer()]++;
                        assertEquals(1, unit.getHitPoints());
                    }
                    if (unit.getType().isStockpile) factories[unit.getPlayer()]++;
                }
            }
            assertEquals(4, workers[0]);
            assertEquals(4, workers[1]);
            assertEquals(1, factories[0]);
            assertEquals(1, factories[1]);
            assertEquals(1, cocoa);
            assertEquals(120, cocoaTotal);
            FactoryFeudScenario.validate(pgs, 1);
            FactoryFeudScenario.validateReachability(pgs);
        }
    }

    @Test
    public void refinedNarrowPassageUsesTerrainCorridorAndStandardizedNode()
            throws Exception {
        GameState gs = FactoryFeudScenario.load(
                FactoryFeudScenario.REFINED_NARROW_PASSAGE_MAP, utt, 1);
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int cocoaTotal = 0;
        Set<String> resourceLocations = new HashSet<>();
        for (Unit unit : gs.getUnits()) {
            if (!unit.getType().isResource) continue;
            cocoaTotal += unit.getResources();
            resourceLocations.add(unit.getX() + "," + unit.getY());
        }

        assertEquals(new HashSet<>(Arrays.asList("5,5")), resourceLocations);
        assertEquals(120, cocoaTotal);
        for (int y = 0; y < pgs.getHeight(); y++) {
            if (y == 5 || y == 6) {
                assertEquals(PhysicalGameState.TERRAIN_NONE, pgs.getTerrain(5, y));
                assertEquals(PhysicalGameState.TERRAIN_NONE, pgs.getTerrain(6, y));
                if (y == 5) assertNotNull(pgs.getUnitAt(5, y));
                else assertNull(pgs.getUnitAt(5, y));
                assertNull(pgs.getUnitAt(6, y));
            } else if (y <= 3 || y >= 8) {
                assertTrue(pgs.getTerrain(5, y) != PhysicalGameState.TERRAIN_NONE);
                assertTrue(pgs.getTerrain(6, y) != PhysicalGameState.TERRAIN_NONE);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void configurableCocoaCountRejectsMismatch() throws Exception {
        FactoryFeudScenario.load(FactoryFeudScenario.REFINED_NARROW_PASSAGE_MAP, utt, 6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configurableCocoaCountMustBePositive() throws Exception {
        FactoryFeudScenario.load(FactoryFeudScenario.REFINED_NARROW_PASSAGE_MAP, utt, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void genericValidationRejectsMapWithoutCocoa() throws Exception {
        GameState gs = FactoryFeudScenario.load(
                FactoryFeudScenario.REFINED_NARROW_PASSAGE_MAP, utt);
        for (Unit unit : new ArrayList<>(gs.getUnits())) {
            if (unit.getType().isResource) gs.removeUnit(unit);
        }
        FactoryFeudScenario.validate(gs.getPhysicalGameState());
    }

    @Test
    public void refinedNarrowPassageSupportsDeliveryAndCombatForBothRoleAgents()
            throws Exception {
        GameState gs = FactoryFeudScenario.load(
                FactoryFeudScenario.REFINED_NARROW_PASSAGE_MAP, utt);
        // Preserve this legacy two-sided delivery/combat fixture independently of the
        // evaluation maps' standardized one-hit Workers.
        for (Unit unit : gs.getUnits()) {
            if (unit.getType().canHarvest) unit.setHitPoints(2);
        }
        gs.removeUnit(gs.getUnit(0));
        int[][] cocoaPositions = {{5, 4}, {6, 7}, {3, 7}, {8, 4}};
        for (int id = 0; id < cocoaPositions.length; id++) {
            gs.getPhysicalGameState().addUnit(new Unit(id, -1, utt.getUnitType("Resource"),
                    cocoaPositions[id][0], cocoaPositions[id][1], 30));
        }
        FactoryFeudMatch match = new FactoryFeudMatch(
                gs, 500, new AdaptiveRoleAI(utt, 500), new FixedRoleAI(utt));
        FactoryFeudResult result = match.run();
        int survivors = 0;
        for (Unit unit : gs.getUnits()) if (unit.getPlayer() >= 0) survivors++;

        assertTrue(result.getScore(0) > 0);
        assertTrue(result.getScore(1) > 0);
        assertTrue("The corridor should permit a combat casualty", survivors < 10);
        assertEquals(0, match.getIllegalActionCount());
    }

    @Test
    public void tacticalMapsUseMinimumEscortEffectiveDurability() throws Exception {
        for (String map : FactoryFeudScenario.evaluationMaps()) {
            GameState gs = FactoryFeudScenario.load(map, utt);
            for (Unit unit : gs.getUnits()) {
                if (unit.getPlayer() >= 0 && unit.getType().canHarvest) {
                    assertEquals(1, unit.getHitPoints());
                }
            }
        }
    }

    @Test
    public void visualConfigurationLoadsSelectedCustomMap() throws Exception {
        FactoryFeudVisualConfig config = FactoryFeudVisualConfig.parse(new String[]{
            "500", "0", FactoryFeudScenario.NARROW_PASSAGE_MAP
        });
        GameState gs = config.loadScenario(utt);

        assertEquals(FactoryFeudScenario.NARROW_PASSAGE_MAP, config.getMapPath());
        assertEquals("factory-feud-narrow-passage-12x12.xml", config.getMapName());
        assertEquals(12, gs.getPhysicalGameState().getWidth());
        assertEquals(12, gs.getPhysicalGameState().getHeight());
        assertTrue(gs.getPhysicalGameState().getTerrain(5, 0) != 0);
    }

    @Test
    public void visualConfigurationDefaultsToControlAndSupportsRectangularMap()
            throws Exception {
        FactoryFeudVisualConfig defaults = FactoryFeudVisualConfig.parse(new String[0]);
        FactoryFeudVisualConfig exposed = FactoryFeudVisualConfig.parse(new String[]{
            "500", "0", FactoryFeudScenario.EXPOSED_ROUTES_MAP
        });
        GameState exposedState = exposed.loadScenario(utt);

        assertEquals(FactoryFeudScenario.DEFAULT_MAP, defaults.getMapPath());
        assertEquals(FactoryFeudExperimentConfig.AdaptiveStrategy.THREAT_AWARE,
                defaults.getAdaptiveStrategy());
        assertEquals(14, exposedState.getPhysicalGameState().getWidth());
        assertEquals(12, exposedState.getPhysicalGameState().getHeight());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reachabilityValidationRejectsSealedCocoa() throws Exception {
        PhysicalGameState pgs = FactoryFeudScenario.load(utt).getPhysicalGameState();
        Unit cocoa = null;
        for (Unit unit : pgs.getUnits()) {
            if (unit.getType().isResource) cocoa = unit;
        }
        assertNotNull(cocoa);
        pgs.setTerrain(cocoa.getX(), cocoa.getY() - 1, 1);
        pgs.setTerrain(cocoa.getX() - 1, cocoa.getY(), 1);
        pgs.setTerrain(cocoa.getX() + 1, cocoa.getY(), 1);
        pgs.setTerrain(cocoa.getX(), cocoa.getY() + 1, 1);
        FactoryFeudScenario.validateReachability(pgs);
    }

    @Test
    public void attackerBlocksDeliveryAndEscortRestoresIt() throws Exception {
        FactoryFeudResult unopposed = runCourierInteraction(false, false);
        InteractionResult attacked = runCourierInteractionWithState(true, false);
        InteractionResult guarded = runCourierInteractionWithState(true, true);
        InteractionResult oneHitPointGuarded =
                runCourierInteractionWithState(true, true, 1);

        assertEquals(1, unopposed.getScore(0));
        assertEquals(0, attacked.result.getScore(0));
        assertNotNull(attacked.gameState.getUnit(26));
        assertEquals(1, guarded.result.getScore(0));
        assertNull("The escort should remove the intercepting attacker",
                guarded.gameState.getUnit(26));
        assertEquals("One-hit Workers provide no protection reaction window",
                0, oneHitPointGuarded.result.getScore(0));
        assertNull(oneHitPointGuarded.gameState.getUnit(20));
    }

    private FactoryFeudResult runCourierInteraction(boolean hunter, boolean escort)
            throws Exception {
        return runCourierInteractionWithState(hunter, escort).result;
    }

    private InteractionResult runCourierInteractionWithState(boolean hunter, boolean escort)
            throws Exception {
        return runCourierInteractionWithState(hunter, escort, 2);
    }

    private InteractionResult runCourierInteractionWithState(
            boolean hunter, boolean escort, int hitPoints) throws Exception {
        GameState gs = tacticalInteractionState(hunter, escort);
        for (Unit unit : gs.getUnits()) {
            if (unit.getType().canHarvest) unit.setHitPoints(hitPoints);
        }
        AI player0 = new CourierTeamAI(escort);
        AI player1 = hunter ? new HunterAI() : new PassiveAI(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(gs, 100, player0, player1);
        return new InteractionResult(match.run(), gs);
    }

    private GameState tacticalInteractionState(boolean hunterPresent, boolean escort)
            throws Exception {
        GameState gs = FactoryFeudScenario.load(FactoryFeudScenario.CONTESTED_CENTER_MAP, utt);
        for (Unit unit : new ArrayList<>(gs.getUnits())) {
            long id = unit.getID();
            boolean keep = id == 10 || id == 11 || id == 20
                    || (hunterPresent && id == 26)
                    || (escort && id == 21);
            if (unit.getType().isResource || (unit.getPlayer() >= 0 && !keep)) {
                gs.removeUnit(unit);
            }
        }
        Unit collector = gs.getUnit(20);
        collector.setX(6);
        collector.setY(5);
        collector.setResources(1);
        Unit hunter = gs.getUnit(26);
        if (hunter != null) {
            hunter.setX(5);
            hunter.setY(5);
        }
        if (escort) {
            Unit guard = gs.getUnit(21);
            guard.setX(5);
            guard.setY(4);
        }
        return gs;
    }

    private static final class InteractionResult {
        private final FactoryFeudResult result;
        private final GameState gameState;

        private InteractionResult(FactoryFeudResult result, GameState gameState) {
            this.result = result;
            this.gameState = gameState;
        }
    }

    private static final class CourierTeamAI extends AbstractionLayerAI {
        private final boolean escort;

        private CourierTeamAI(boolean escort) {
            super(new AStarPathFinding());
            this.escort = escort;
        }

        @Override
        public PlayerAction getAction(int player, GameState gs) {
            Unit collector = gs.getUnit(20);
            Unit factory = gs.getUnit(10);
            Unit guard = gs.getUnit(21);
            Unit hunter = gs.getUnit(26);
            if (collector != null && factory != null) harvest(collector, null, factory);
            if (escort && guard != null && hunter != null) attack(guard, hunter);
            return translateActions(player, gs);
        }

        @Override
        public AI clone() {
            return new CourierTeamAI(escort);
        }

        @Override
        public List<ParameterSpecification> getParameters() {
            return new ArrayList<>();
        }
    }

    private static final class HunterAI extends AbstractionLayerAI {
        private HunterAI() {
            super(new AStarPathFinding());
        }

        @Override
        public PlayerAction getAction(int player, GameState gs) {
            Unit hunter = gs.getUnit(26);
            Unit collector = gs.getUnit(20);
            if (hunter != null && collector != null) attack(hunter, collector);
            return translateActions(player, gs);
        }

        @Override
        public AI clone() {
            return new HunterAI();
        }

        @Override
        public List<ParameterSpecification> getParameters() {
            return new ArrayList<>();
        }
    }
}
