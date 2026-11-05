package courseproject.factoryfeud.generation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import courseproject.factoryfeud.FactoryFeudScenario;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;

public class FactoryFeudMapGeneratorTest {
    private UnitTypeTable utt;

    @Before
    public void setUp() {
        utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
    }

    @Test
    public void sameConfigurationAndSeedReproduceIdenticalMap() throws Exception {
        FactoryFeudGeneratorConfig config = config(
                14, 12, 0.15, FactoryFeudMapFamily.OBSTACLE_FIELD, 584L, 100);
        FactoryFeudGeneratedMap first = generate(config);
        FactoryFeudGeneratedMap second = generate(config);

        assertEquals(fingerprint(first.getPhysicalGameState()),
                fingerprint(second.getPhysicalGameState()));
        assertEquals(first.getSuccessfulAttemptNumber(), second.getSuccessfulAttemptNumber());
        assertEquals(first.getMetrics().getObstacleCount(),
                second.getMetrics().getObstacleCount());
        assertEquals(first.getMetrics().getDeadEndCount(),
                second.getMetrics().getDeadEndCount());
    }

    @Test
    public void differentSeedsProduceTerrainVariety() throws Exception {
        Set<String> terrains = new HashSet<>();
        for (long seed = 1; seed <= 6; seed++) {
            terrains.add(terrainFingerprint(generate(config(
                    14, 12, 0.12, FactoryFeudMapFamily.OPEN, seed, 100))
                    .getPhysicalGameState()));
        }
        assertTrue("Expected meaningful terrain variety across seeds", terrains.size() >= 4);
    }

    @Test
    public void everyMapFamilyProducesValidConfiguredDimensionsAndMetrics()
            throws Exception {
        for (FactoryFeudMapFamily family : FactoryFeudMapFamily.values()) {
            double density = family == FactoryFeudMapFamily.CHOKEPOINT ? 0.15 : 0.12;
            FactoryFeudGeneratedMap generated = generate(config(
                    14, 12, density, family, 91L + family.ordinal(), 150));
            PhysicalGameState pgs = generated.getPhysicalGameState();
            FactoryFeudMapMetrics metrics = generated.getMetrics();

            assertEquals(14, pgs.getWidth());
            assertEquals(12, pgs.getHeight());
            assertEquals((int) Math.round(14 * 12 * density), metrics.getObstacleCount());
            assertEquals(metrics.getObstacleCount() / (double) (14 * 12),
                    metrics.getActualObstacleDensity(), 0.0000001);
            assertTrue(metrics.getPlayer0FactoryToCocoaPath() >= 0);
            assertTrue(metrics.getPlayer1FactoryToCocoaPath() >= 0);
            assertTrue(metrics.getRouteDifference() >= 0);
            assertTrue(metrics.getDeadEndCount() >= 0);
            assertTrue(generated.getAttempts().get(
                    generated.getAttempts().size() - 1).passedValidation());
        }
    }

    @Test
    public void generatedMapHasExactFactoryFeudUnitsAndCocoa() throws Exception {
        PhysicalGameState pgs = generate(config(
                16, 10, 0.18, FactoryFeudMapFamily.OBSTACLE_FIELD, 7L, 100))
                .getPhysicalGameState();
        int[] workers = new int[2];
        int[] bases = new int[2];
        int resourceCount = 0;
        int cocoa = 0;
        for (Unit unit : pgs.getUnits()) {
            if (unit.getType().isResource) {
                resourceCount++;
                cocoa += unit.getResources();
                assertEquals(-1, unit.getPlayer());
            } else if ("Worker".equals(unit.getType().name)) {
                workers[unit.getPlayer()]++;
            } else if (unit.getType().isStockpile) {
                bases[unit.getPlayer()]++;
            }
        }
        assertEquals(2, pgs.getPlayers().size());
        assertEquals(4, workers[0]);
        assertEquals(4, workers[1]);
        assertEquals(1, bases[0]);
        assertEquals(1, bases[1]);
        assertEquals(1, resourceCount);
        assertEquals(120, cocoa);
        assertEquals(11, pgs.getUnits().size());
    }

    @Test
    public void acceptedUnitsNeverOverlapOrOccupyWalls() throws Exception {
        PhysicalGameState pgs = generate(config(
                14, 12, 0.20, FactoryFeudMapFamily.OBSTACLE_FIELD, 44L, 150))
                .getPhysicalGameState();
        Set<Integer> occupied = new HashSet<>();
        for (Unit unit : pgs.getUnits()) {
            assertEquals(PhysicalGameState.TERRAIN_NONE,
                    pgs.getTerrain(unit.getX(), unit.getY()));
            assertTrue(occupied.add(unit.getX() + unit.getY() * pgs.getWidth()));
        }
    }

    @Test
    public void acceptedMapsPassSharedAndD2ReachabilityValidation() throws Exception {
        for (long seed = 20; seed < 25; seed++) {
            PhysicalGameState pgs = generate(config(
                    14, 12, 0.16, FactoryFeudMapFamily.OBSTACLE_FIELD, seed, 150))
                    .getPhysicalGameState();
            FactoryFeudScenario.validate(pgs, 1);
            FactoryFeudScenario.validateReachability(pgs);
            FactoryFeudGeneratedMapValidator.validate(pgs);
        }
    }

    @Test
    public void configurationAcceptsDocumentedBoundaries() {
        FactoryFeudGeneratorConfig minimum = config(
                FactoryFeudGeneratorConfig.MIN_WIDTH,
                FactoryFeudGeneratorConfig.MIN_HEIGHT, 0.0,
                FactoryFeudMapFamily.OPEN, Long.MIN_VALUE, 1);
        FactoryFeudGeneratorConfig maximum = config(
                FactoryFeudGeneratorConfig.MAX_WIDTH,
                FactoryFeudGeneratorConfig.MAX_HEIGHT,
                FactoryFeudGeneratorConfig.MAX_OBSTACLE_DENSITY,
                FactoryFeudMapFamily.OBSTACLE_FIELD, Long.MAX_VALUE, 1);
        assertEquals(0, minimum.getTargetObstacleCount());
        assertEquals(FactoryFeudGeneratorConfig.MAX_WIDTH, maximum.getWidth());
        assertEquals(1, minimum.getMaxAttempts());
    }

    @Test
    public void invalidConfigurationsAreRejected() {
        assertInvalid(9, 12, 0.1, FactoryFeudMapFamily.OPEN, 1);
        assertInvalid(12, 7, 0.1, FactoryFeudMapFamily.OPEN, 1);
        assertInvalid(12, 12, -0.01, FactoryFeudMapFamily.OPEN, 1);
        assertInvalid(12, 12, 0.36, FactoryFeudMapFamily.OPEN, 1);
        assertInvalid(12, 12, 0.1, null, 1);
        assertInvalid(12, 12, 0.1, FactoryFeudMapFamily.OPEN, 0);
        assertInvalid(12, 12, 0.01, FactoryFeudMapFamily.CHOKEPOINT, 1);
    }

    @Test
    public void maximumRetryFailurePreservesEveryAttemptAndNeverFallsBack()
            throws Exception {
        FactoryFeudGeneratorConfig config = config(
                12, 10, 0.10, FactoryFeudMapFamily.OPEN, 123L, 3);
        FactoryFeudMapGenerator generator = new FactoryFeudMapGenerator(config, utt,
                (ignoredConfig, ignoredUtt, ignoredRandom) ->
                        new PhysicalGameState(12, 10));
        try {
            generator.generate();
            fail("Expected generation to stop after the configured retry count");
        } catch (FactoryFeudGenerationException expected) {
            assertEquals(3, expected.getAttempts().size());
            for (int index = 0; index < expected.getAttempts().size(); index++) {
                FactoryFeudGenerationAttempt attempt = expected.getAttempts().get(index);
                assertEquals(index + 1, attempt.getAttemptNumber());
                assertFalse(attempt.passedValidation());
                assertNotNull(attempt.getMetrics());
                assertNotNull(attempt.getValidationFailure());
            }
        }
    }

    private FactoryFeudGeneratedMap generate(FactoryFeudGeneratorConfig config)
            throws Exception {
        return new FactoryFeudMapGenerator(config, utt).generate();
    }

    private static FactoryFeudGeneratorConfig config(int width, int height,
            double density, FactoryFeudMapFamily family, long seed, int attempts) {
        return new FactoryFeudGeneratorConfig(
                width, height, density, family, seed, attempts);
    }

    private static void assertInvalid(int width, int height, double density,
            FactoryFeudMapFamily family, int attempts) {
        try {
            config(width, height, density, family, 1L, attempts);
            fail("Expected invalid generator configuration");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static String fingerprint(PhysicalGameState pgs) {
        StringBuilder result = new StringBuilder(terrainFingerprint(pgs));
        for (Unit unit : pgs.getUnits()) {
            result.append('|').append(unit.getID()).append(':')
                    .append(unit.getPlayer()).append(':').append(unit.getType().name)
                    .append(':').append(unit.getX()).append(':').append(unit.getY())
                    .append(':').append(unit.getResources());
        }
        return result.toString();
    }

    private static String terrainFingerprint(PhysicalGameState pgs) {
        StringBuilder result = new StringBuilder(pgs.getWidth() * pgs.getHeight());
        for (int y = 0; y < pgs.getHeight(); y++) {
            for (int x = 0; x < pgs.getWidth(); x++) {
                result.append(pgs.getTerrain(x, y));
            }
        }
        return result.toString();
    }
}
