package courseproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import courseproject.factoryfeud.FactoryFeudExperimentConfig;
import courseproject.factoryfeud.FactoryFeudExperimentRecord;
import courseproject.factoryfeud.FactoryFeudExperimentRunner;
import courseproject.factoryfeud.FactoryFeudExperimentSummary;
import courseproject.factoryfeud.FactoryFeudScenario;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FactoryFeudExperimentTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = IllegalArgumentException.class)
    public void pairedExperimentsRejectOddMatchCounts() {
        new FactoryFeudExperimentConfig(3, 100, FactoryFeudScenario.DEFAULT_MAP,
                1L, temporaryFolder.getRoot().toPath().resolve("odd.csv"));
    }

    @Test
    public void runnerWritesCompleteCsvAndPairedSideSwaps() throws Exception {
        Path csv = temporaryFolder.newFile("paired.csv").toPath();
        FactoryFeudExperimentConfig config = new FactoryFeudExperimentConfig(
                4, 120, FactoryFeudScenario.DEFAULT_MAP, 91L, csv);
        FactoryFeudExperimentSummary summary = FactoryFeudExperimentRunner.run(config);

        assertEquals(4, summary.getMatchCount());
        List<FactoryFeudExperimentRecord> records = summary.getRecords();
        assertPair(records.get(0), records.get(1), 91L);
        assertPair(records.get(2), records.get(3), 92L);
        for (FactoryFeudExperimentRecord record : records) {
            assertEquals(record.getAdaptiveSide() == 0
                            ? record.getPlayer0Score() : record.getPlayer1Score(),
                    record.getAdaptiveScore());
            assertEquals(record.getAdaptiveSide() == 0
                            ? record.getPlayer1Score() : record.getPlayer0Score(),
                    record.getFixedScore());
            assertEquals(120, record.getMatchCycles());
            assertEquals(0, record.getIllegalActionCount());
        }

        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(5, lines.size());
        assertEquals(FactoryFeudExperimentRecord.CSV_HEADER, lines.get(0));
        assertEquals(17, lines.get(1).split(",", -1).length);
        assertTrue(lines.get(1).contains(FactoryFeudScenario.DEFAULT_MAP));
    }

    @Test
    public void controlledRunsRepeatAndAgentsDoNotAccumulateState() throws Exception {
        Path firstCsv = temporaryFolder.newFile("repeat-a.csv").toPath();
        Path secondCsv = temporaryFolder.newFile("repeat-b.csv").toPath();
        FactoryFeudExperimentSummary first = FactoryFeudExperimentRunner.run(
                new FactoryFeudExperimentConfig(4, 200, FactoryFeudScenario.DEFAULT_MAP,
                        584L, firstCsv));
        FactoryFeudExperimentSummary second = FactoryFeudExperimentRunner.run(
                new FactoryFeudExperimentConfig(4, 200, FactoryFeudScenario.DEFAULT_MAP,
                        584L, secondCsv));

        for (int index = 0; index < first.getMatchCount(); index++) {
            assertTrue(first.getRecords().get(index)
                    .sameSimulationResult(second.getRecords().get(index)));
        }
        assertSameSideFresh(first.getRecords().get(0), first.getRecords().get(2));
        assertSameSideFresh(first.getRecords().get(1), first.getRecords().get(3));
    }

    @Test
    public void aggregateStatisticsCountDrawsSeparately() {
        FactoryFeudExperimentRecord win = record(
                1, FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN, 3, 1, 10);
        FactoryFeudExperimentRecord draw = record(
                2, FactoryFeudExperimentRecord.Outcome.DRAW, 2, 2, 20);
        FactoryFeudExperimentRecord loss = record(
                3, FactoryFeudExperimentRecord.Outcome.FIXED_WIN, 0, 4, 30);
        FactoryFeudExperimentSummary summary =
                new FactoryFeudExperimentSummary(Arrays.asList(win, draw, loss));

        assertEquals(1, summary.getAdaptiveWins());
        assertEquals(1, summary.getDraws());
        assertEquals(1, summary.getFixedWins());
        assertEquals(1.0 / 3.0, summary.getAdaptiveWinRate(), 0.000001);
        assertEquals(5.0 / 3.0, summary.getMeanAdaptiveCocoa(), 0.000001);
        assertEquals(7.0 / 3.0, summary.getMeanFixedCocoa(), 0.000001);
        assertEquals(-2.0 / 3.0, summary.getMeanScoreDifferential(), 0.000001);
        assertEquals(0.0, summary.getMeanAdaptiveCasualties(), 0.000001);
        assertEquals(0.0, summary.getMeanFixedCasualties(), 0.000001);
        assertEquals(0, summary.getTotalRoleReassignments());
        assertEquals(1, summary.getPlayer0Wins());
        assertEquals(1, summary.getPlayer1Wins());
        assertEquals(-2.0 / 3.0, summary.getMeanPlayer0ScoreAdvantage(), 0.000001);
        assertEquals(20.0, summary.getMeanRuntimeMillis(), 0.000001);
    }

    @Test
    public void csvEscapesMapNamesContainingCommasAndQuotes() {
        FactoryFeudExperimentRecord record = new FactoryFeudExperimentRecord(
                1, "maps/a,\"quoted\".xml", 3L, 0, 1, 0, 1, 0,
                FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN, 50,
                5, 4, 5, 4, 2, 0, 7);
        assertTrue(record.toCsvRow().contains("\"maps/a,\"\"quoted\"\".xml\""));
    }

    private static void assertPair(FactoryFeudExperimentRecord first,
            FactoryFeudExperimentRecord second, long expectedSeed) {
        assertEquals(expectedSeed, first.getSeed());
        assertEquals(expectedSeed, second.getSeed());
        assertEquals(0, first.getAdaptiveSide());
        assertEquals(1, second.getAdaptiveSide());
    }

    private static void assertSameSideFresh(FactoryFeudExperimentRecord first,
            FactoryFeudExperimentRecord later) {
        assertEquals(first.getAdaptiveSide(), later.getAdaptiveSide());
        assertEquals(first.getAdaptiveScore(), later.getAdaptiveScore());
        assertEquals(first.getFixedScore(), later.getFixedScore());
        assertEquals(first.getRoleReassignments(), later.getRoleReassignments());
        assertEquals(first.getOutcome(), later.getOutcome());
    }

    private static FactoryFeudExperimentRecord record(int id,
            FactoryFeudExperimentRecord.Outcome outcome, int adaptiveScore,
            int fixedScore, long runtime) {
        return new FactoryFeudExperimentRecord(id, FactoryFeudScenario.DEFAULT_MAP,
                1L, 0, adaptiveScore, fixedScore, adaptiveScore, fixedScore,
                outcome, 100, 5, 5, 5, 5, 0, 0, runtime);
    }
}
