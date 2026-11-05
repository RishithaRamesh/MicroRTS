package courseproject.factoryfeud.generation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import courseproject.factoryfeud.AdaptiveRoleAI;
import courseproject.factoryfeud.FactoryFeudMatch;
import courseproject.factoryfeud.FactoryFeudResult;
import courseproject.factoryfeud.FactoryFeudScenario;
import courseproject.factoryfeud.FixedRoleAI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rts.GameState;
import rts.units.UnitTypeTable;

public class FactoryFeudBatchGeneratorTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void batchProducesRequestedCountsUniqueNamesAndCompleteCsv() throws Exception {
        FactoryFeudBatchGenerator.BatchResult result = FactoryFeudBatchGenerator.run(
                config("counts", 2, 9000L));
        assertEquals(6, result.getAcceptedMaps().size());
        Set<String> names = new HashSet<>();
        int[] families = new int[FactoryFeudMapFamily.values().length];
        for (FactoryFeudBatchGenerator.AcceptedMapRecord record : result.getAcceptedMaps()) {
            families[record.getFamily().ordinal()]++;
            assertTrue(names.add(record.getOutputPath().getFileName().toString()));
            assertTrue(Files.isRegularFile(record.getOutputPath()));
        }
        assertEquals(2, families[FactoryFeudMapFamily.OPEN.ordinal()]);
        assertEquals(2, families[FactoryFeudMapFamily.OBSTACLE_FIELD.ordinal()]);
        assertEquals(2, families[FactoryFeudMapFamily.CHOKEPOINT.ordinal()]);

        List<String> accepted = Files.readAllLines(
                result.getAcceptedCsv(), StandardCharsets.UTF_8);
        assertEquals(7, accepted.size());
        assertEquals(FactoryFeudBatchGenerator.ACCEPTED_HEADER, accepted.get(0));
        assertEquals(14, accepted.get(1).split(",", -1).length);
        assertFalse(accepted.get(1).isEmpty());

        List<String> attempts = Files.readAllLines(
                result.getAttemptsCsv(), StandardCharsets.UTF_8);
        assertEquals(result.getRawAttempts().size() + 1, attempts.size());
        assertEquals(FactoryFeudBatchGenerator.ATTEMPTS_HEADER, attempts.get(0));
        assertEquals(13, attempts.get(1).split(",", -1).length);
    }

    @Test
    public void exportedXmlReloadsAndPreservesGeneratedState() throws Exception {
        FactoryFeudBatchGenerator.BatchResult result = FactoryFeudBatchGenerator.run(
                config("reload", 1, 9100L));
        UnitTypeTable utt = unitTypes();
        for (FactoryFeudBatchGenerator.AcceptedMapRecord record : result.getAcceptedMaps()) {
            GameState reloaded = FactoryFeudScenario.load(record.getOutputPath().toString(), utt);
            FactoryFeudGeneratedMapValidator.validate(reloaded.getPhysicalGameState());
            FactoryFeudMapMetrics metrics = FactoryFeudMapMetrics.compute(
                    reloaded.getPhysicalGameState());
            assertEquals(record.getMetrics().getObstacleCount(), metrics.getObstacleCount());
            assertEquals(record.getMetrics().getPlayer0FactoryToCocoaPath(),
                    metrics.getPlayer0FactoryToCocoaPath());
            assertEquals(record.getMetrics().getPlayer1FactoryToCocoaPath(),
                    metrics.getPlayer1FactoryToCocoaPath());
        }
    }

    @Test
    public void sameBatchConfigurationReproducesFilenamesAndXmlBytes() throws Exception {
        FactoryFeudBatchGenerator.BatchResult first = FactoryFeudBatchGenerator.run(
                config("repro-a", 1, 9200L));
        FactoryFeudBatchGenerator.BatchResult second = FactoryFeudBatchGenerator.run(
                config("repro-b", 1, 9200L));
        for (int index = 0; index < first.getAcceptedMaps().size(); index++) {
            Path firstMap = first.getAcceptedMaps().get(index).getOutputPath();
            Path secondMap = second.getAcceptedMaps().get(index).getOutputPath();
            assertEquals(firstMap.getFileName(), secondMap.getFileName());
            assertArrayEquals(Files.readAllBytes(firstMap), Files.readAllBytes(secondMap));
        }
    }

    @Test
    public void rawAttemptAccountingSeparatesInvalidCandidatesFromEventualSuccess()
            throws Exception {
        FactoryFeudBatchConfig config = config("attempts", 1, 9300L);
        FactoryFeudBatchGenerator.BatchResult result = FactoryFeudBatchGenerator.run(
                config, (generatorConfig, utt) -> {
                    FactoryFeudGeneratedMap generated = new FactoryFeudMapGenerator(
                            generatorConfig, utt).generate();
                    FactoryFeudMapMetrics metrics = generated.getMetrics();
                    List<FactoryFeudGenerationAttempt> attempts = new ArrayList<>();
                    attempts.add(new FactoryFeudGenerationAttempt(1,
                            generatorConfig.getSeed(), metrics, false,
                            "injected invalid candidate"));
                    attempts.add(new FactoryFeudGenerationAttempt(2,
                            generatorConfig.getSeed() + 1, metrics, true, null));
                    return new FactoryFeudGeneratedMap(generated.getPhysicalGameState(),
                            generatorConfig, metrics, attempts);
                });

        FactoryFeudBatchGenerator.ValidityStatistics overall =
                result.getOverallStatistics();
        assertEquals(3, result.getAcceptedMaps().size());
        assertEquals(6, overall.getTotalAttempts());
        assertEquals(3, overall.getValidAttempts());
        assertEquals(3, overall.getInvalidAttempts());
        assertEquals(0.5, overall.getRawValidityRate(), 0.0000001);
        assertFalse(result.getRawAttempts().get(0).isValid());
        assertTrue(result.getRawAttempts().get(1).isValid());

        List<String> summary = Files.readAllLines(
                result.getValidityCsv(), StandardCharsets.UTF_8);
        assertEquals(5, summary.size());
        assertTrue(summary.get(4).startsWith("OVERALL,3,6,3,3,0.50000000"));
        assertTrue(Files.readAllLines(result.getAttemptsCsv(), StandardCharsets.UTF_8)
                .get(1).contains("injected invalid candidate"));
    }

    @Test
    public void generatedMapCompletesExistingFiveHundredCycleRoleMatch() throws Exception {
        FactoryFeudBatchGenerator.BatchResult result = FactoryFeudBatchGenerator.run(
                config("match", 1, 9400L));
        String map = result.getAcceptedMaps().get(0).getOutputPath().toString();
        UnitTypeTable utt = unitTypes();
        GameState state = FactoryFeudScenario.load(map, utt);
        FactoryFeudMatch match = new FactoryFeudMatch(state, 500,
                new FixedRoleAI(utt), new AdaptiveRoleAI(utt, 500));
        FactoryFeudResult matchResult = match.run();
        assertEquals(500, matchResult.getCycles());
        assertEquals(0, match.getIllegalActionCount());
    }

    private FactoryFeudBatchConfig config(String name, int mapsPerFamily, long seed)
            throws Exception {
        Path root = temporary.newFolder(name).toPath();
        return new FactoryFeudBatchConfig(mapsPerFamily, 14, 12, seed, 100,
                root.resolve("maps"), root.resolve("analysis"));
    }

    private static UnitTypeTable unitTypes() {
        return new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
    }
}
