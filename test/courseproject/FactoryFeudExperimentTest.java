package courseproject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import courseproject.factoryfeud.FactoryFeudExperimentConfig;
import courseproject.factoryfeud.FactoryFeudExperimentRecord;
import courseproject.factoryfeud.FactoryFeudExperimentRunner;
import courseproject.factoryfeud.FactoryFeudExperimentSummary;
import courseproject.factoryfeud.FactoryFeudScenario;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            assertEquals(120, record.getConfiguredCycles());
            assertEquals(120, record.getCompletedCycles());
            assertEquals(record.getRuntimeMillis(), record.getSimulationRuntimeMillis());
            assertEquals(0, record.getIllegalActionCount());
            assertEquals("adaptive", record.getAdaptiveStrategy());
            assertTrue(record.getAdaptiveCarrier() != null);
            assertTrue(record.getFixedCarrier() != null);
            assertNull(record.getCargoSafeCarrier());
            assertEquals(Integer.valueOf(0),
                    record.getFixedCarrier().getRoleInterruptions());
        }

        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(5, lines.size());
        assertEquals(FactoryFeudExperimentRecord.CSV_HEADER, lines.get(0));
        assertEquals(58, FactoryFeudExperimentRecord.CSV_HEADER.split(",", -1).length);
        assertEquals(58, csvColumnCount(lines.get(1)));
        assertTrue(FactoryFeudExperimentRecord.CSV_HEADER.startsWith(
                FactoryFeudExperimentRecord.LEGACY_CSV_HEADER + ","));
        assertTrue(lines.get(1).contains(FactoryFeudScenario.DEFAULT_MAP));
    }

    @Test
    public void cargoSafeSelectionIsExplicitAndSideSwappedMetricsStayAttributed()
            throws Exception {
        Path csv = temporaryFolder.newFile("cargo-safe.csv").toPath();
        String map = existingMap(FactoryFeudScenario.NARROW_PASSAGE_MAP);
        FactoryFeudExperimentConfig config = new FactoryFeudExperimentConfig(
                2, 120, map, 7L, csv,
                FactoryFeudExperimentConfig.AdaptiveStrategy.CARGO_SAFE);
        List<FactoryFeudExperimentRecord> records =
                FactoryFeudExperimentRunner.run(config).getRecords();

        assertPair(records.get(0), records.get(1), 7L);
        for (FactoryFeudExperimentRecord record : records) {
            assertEquals("cargo-safe", record.getAdaptiveStrategy());
            assertNull(record.getAdaptiveCarrier());
            assertTrue(record.getCargoSafeCarrier() != null);
            assertTrue(record.getFixedCarrier() != null);
            assertEquals(Integer.valueOf(0),
                    record.getCargoSafeCarrier().getRoleInterruptions());
            assertEquals(Integer.valueOf(0),
                    record.getFixedCarrier().getRoleInterruptions());
        }
    }

    @Test
    public void threatAwareSelectionRecordsTacticalOverrides() throws Exception {
        Path csv = temporaryFolder.newFile("threat-aware.csv").toPath();
        FactoryFeudExperimentConfig config = new FactoryFeudExperimentConfig(
                2, 120, FactoryFeudScenario.DEFAULT_MAP, 7L, csv,
                FactoryFeudExperimentConfig.AdaptiveStrategy.THREAT_AWARE);
        List<FactoryFeudExperimentRecord> records =
                FactoryFeudExperimentRunner.run(config).getRecords();

        assertPair(records.get(0), records.get(1), 7L);
        int overrides = 0;
        int attacks = 0;
        int retreats = 0;
        for (FactoryFeudExperimentRecord record : records) {
            assertEquals("threat-aware", record.getAdaptiveStrategy());
            overrides += record.getTacticalOverrides();
            attacks += record.getTacticalAttackOverrides();
            retreats += record.getTacticalRetreatOverrides();
        }
        assertTrue(overrides > 0);
        assertEquals(overrides, attacks + retreats);
    }

    @Test
    public void zeroDeliveryCsvUsesBlankTimingCellsAndZeroEventCount() throws Exception {
        Path csv = temporaryFolder.newFile("no-deliveries.csv").toPath();
        FactoryFeudExperimentSummary summary = FactoryFeudExperimentRunner.run(
                new FactoryFeudExperimentConfig(2, 3, FactoryFeudScenario.DEFAULT_MAP,
                        4L, csv));
        FactoryFeudExperimentRecord record = summary.getRecords().get(0);

        assertNull(record.getPlayer0Delivery().getFirstCycle());
        assertNull(record.getPlayer0Delivery().getLastCycle());
        assertNull(record.getPlayer0Delivery().getMeanInterval());
        assertEquals(Integer.valueOf(0), record.getPlayer0Delivery().getCompletedEvents());

        String[] cells = Files.readAllLines(csv, StandardCharsets.UTF_8)
                .get(1).split(",", -1);
        assertEquals("", cells[20]);
        assertEquals("", cells[21]);
        assertEquals("", cells[22]);
        assertEquals("0", cells[23]);
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

    private static int csvColumnCount(String row) {
        int columns = 1;
        boolean quoted = false;
        for (int index = 0; index < row.length(); index++) {
            char current = row.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < row.length() && row.charAt(index + 1) == '"') {
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                columns++;
            }
        }
        return columns;
    }

    private static String existingMap(String map) {
        assertTrue("Factory Feud test map does not exist: " + map,
                Files.isRegularFile(Paths.get(map)));
        return map;
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
