package courseproject.factoryfeud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import courseproject.factoryfeud.FactoryFeudD3ExperimentRunner.Artifacts;
import courseproject.factoryfeud.FactoryFeudD3ExperimentRunner.Config;
import courseproject.factoryfeud.FactoryFeudD3ExperimentRunner.D3Record;
import courseproject.factoryfeud.FactoryFeudD3ExperimentRunner.MapMetadata;
import courseproject.factoryfeud.generation.FactoryFeudDensityPreset;
import courseproject.factoryfeud.generation.FactoryFeudMapFamily;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FactoryFeudD3ExperimentRunnerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void selectionIsDistinctBalancedAndDeterministic() throws Exception {
        List<MapMetadata> maps = FactoryFeudD3ExperimentRunner.readMetadata(
                FactoryFeudD3ExperimentRunner.DEFAULT_METADATA);
        List<MapMetadata> first = FactoryFeudD3ExperimentRunner.selectBalanced(maps, 2);
        List<MapMetadata> second = FactoryFeudD3ExperimentRunner.selectBalanced(maps, 2);
        assertEquals(24, first.size());
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < first.size(); index++) {
            MapMetadata map = first.get(index);
            assertEquals(map.getMapId(), second.get(index).getMapId());
            assertTrue(ids.add(map.getMapId()));
            int sameStratum = 0;
            for (MapMetadata candidate : first) {
                if (candidate.getFamily() == map.getFamily()
                        && candidate.getPreset() == map.getPreset()) sameStratum++;
            }
            assertEquals(2, sameStratum);
        }
    }

    @Test
    public void selectedXmlMapsLoadAndValidate() throws Exception {
        List<MapMetadata> selected = FactoryFeudD3ExperimentRunner.selectBalanced(
                FactoryFeudD3ExperimentRunner.readMetadata(
                        FactoryFeudD3ExperimentRunner.DEFAULT_METADATA), 2);
        FactoryFeudD3ExperimentRunner.validateSelectedMaps(selected);
    }

    @Test
    public void smokeRunWritesCompleteSchemaAndCorrectPairs() throws Exception {
        Path output = temporaryFolder.newFolder("d3-smoke").toPath();
        Artifacts artifacts = FactoryFeudD3ExperimentRunner.run(new Config(
                FactoryFeudD3ExperimentRunner.DEFAULT_METADATA, output, 1, 2, 900L));

        assertEquals(12, artifacts.selectedMaps.size());
        assertEquals(48, artifacts.records.size());
        assertEquals(0, artifacts.failedMatches);
        assertEquals(49, Files.readAllLines(artifacts.results, StandardCharsets.UTF_8).size());
        assertEquals(13, Files.readAllLines(artifacts.manifest, StandardCharsets.UTF_8).size());
        assertEquals(1, Files.readAllLines(artifacts.failures, StandardCharsets.UTF_8).size());
        assertEquals(FactoryFeudD3ExperimentRunner.RESULT_HEADER,
                Files.readAllLines(artifacts.results, StandardCharsets.UTF_8).get(0));
        List<String> resultLines = Files.readAllLines(
                artifacts.results, StandardCharsets.UTF_8);
        assertEquals(FactoryFeudD3ExperimentRunner.parseCsvRow(resultLines.get(0)).size(),
                FactoryFeudD3ExperimentRunner.parseCsvRow(resultLines.get(1)).size());

        double openMargin = 0;
        double chokepointMargin = 0;
        int openMatches = 0;
        int chokepointMatches = 0;
        double mainCocoa = 0;
        double ablationCocoa = 0;
        int mainMatches = 0;
        int ablationMatches = 0;
        for (int index = 0; index < artifacts.records.size(); index += 4) {
            D3Record mainA = artifacts.records.get(index);
            D3Record mainB = artifacts.records.get(index + 1);
            D3Record ablationA = artifacts.records.get(index + 2);
            D3Record ablationB = artifacts.records.get(index + 3);
            assertEquals("MAIN", mainA.getExperiment());
            assertEquals("MAIN", mainB.getExperiment());
            assertEquals("ABLATION", ablationA.getExperiment());
            assertEquals("ABLATION", ablationB.getExperiment());
            assertEquals(1, mainA.getMatch().getAdaptiveSide());
            assertEquals(0, mainB.getMatch().getAdaptiveSide());
            assertEquals(1, ablationA.getMatch().getAdaptiveSide());
            assertEquals(0, ablationB.getMatch().getAdaptiveSide());
            assertEquals("adaptive", mainA.getMatch().getAdaptiveStrategy());
            assertEquals("old-adaptive", ablationA.getMatch().getAdaptiveStrategy());
            assertEquals(mainA.getMatch().getAdaptiveScore()
                            - mainA.getMatch().getFixedScore(), mainA.getMargin());
        }
        for (D3Record record : artifacts.records) {
            int adaptive = record.getMatch().getAdaptiveScore();
            int fixed = record.getMatch().getFixedScore();
            if (adaptive > fixed) {
                assertEquals(FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN,
                        record.getMatch().getOutcome());
            } else if (adaptive < fixed) {
                assertEquals(FactoryFeudExperimentRecord.Outcome.FIXED_WIN,
                        record.getMatch().getOutcome());
            } else {
                assertEquals(FactoryFeudExperimentRecord.Outcome.DRAW,
                        record.getMatch().getOutcome());
            }
            if ("MAIN".equals(record.getExperiment())) {
                mainCocoa += adaptive;
                mainMatches++;
                if (record.getMap().getFamily() == FactoryFeudMapFamily.OPEN) {
                    openMargin += record.getMargin();
                    openMatches++;
                } else if (record.getMap().getFamily() == FactoryFeudMapFamily.CHOKEPOINT) {
                    chokepointMargin += record.getMargin();
                    chokepointMatches++;
                }
            } else {
                ablationCocoa += adaptive;
                ablationMatches++;
            }
        }
        assertEquals(chokepointMargin / chokepointMatches - openMargin / openMatches,
                artifacts.statistics.getH2Difference(), 0.000001);
        assertEquals(FactoryFeudD3ExperimentRunner.percentageImprovement(
                        mainCocoa / mainMatches, ablationCocoa / ablationMatches),
                artifacts.statistics.getH3Improvement());
        String csv = new String(Files.readAllBytes(artifacts.results), StandardCharsets.UTF_8);
        assertTrue(csv.contains("FixedRoleAI,AdaptiveRoleAI,1"));
        assertTrue(csv.contains("OldAdaptiveRoleAI,FixedRoleAI,0"));
        assertEquals(artifacts.statistics.getMain().strictWinRate() >= .70,
                artifacts.statistics.getMain().wins * 100
                        >= 70 * artifacts.statistics.getMain().count());

        Artifacts replay = FactoryFeudD3ExperimentRunner.run(new Config(
                FactoryFeudD3ExperimentRunner.DEFAULT_METADATA,
                temporaryFolder.newFolder("d3-replay").toPath(), 1, 2, 900L));
        for (int index = 0; index < artifacts.records.size(); index++) {
            assertTrue(artifacts.records.get(index).getMatch().sameSimulationResult(
                    replay.records.get(index).getMatch()));
        }
    }

    @Test
    public void formulasHandleThresholdsAndZeroDenominator() {
        assertEquals(20.0, FactoryFeudD3ExperimentRunner.percentageImprovement(12, 10),
                0.000001);
        assertEquals(-20.0, FactoryFeudD3ExperimentRunner.percentageImprovement(8, 10),
                0.000001);
        assertNull(FactoryFeudD3ExperimentRunner.percentageImprovement(4, 0));
    }

    @Test
    public void diagnosticHistoriesMatchRecordedCounters() throws Exception {
        FactoryFeudExperimentConfig config = new FactoryFeudExperimentConfig(
                2, 120, FactoryFeudScenario.DEFAULT_MAP, 22L,
                temporaryFolder.newFile("unused.csv").toPath(),
                FactoryFeudExperimentConfig.AdaptiveStrategy.ADAPTIVE);
        FactoryFeudExperimentRunner.MatchExecution execution =
                FactoryFeudExperimentRunner.executeMatch(1, 1, 22L, config);
        assertEquals(execution.record.getRoleReassignments(),
                execution.roleChangeHistory.size());
        assertEquals(execution.record.getTacticalOverrides(),
                execution.tacticalOverrideHistory.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingMetadataFailsClearly() throws Exception {
        FactoryFeudD3ExperimentRunner.readMetadata(
                temporaryFolder.getRoot().toPath().resolve("missing.csv"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedMetadataFailsClearly() throws Exception {
        Path csv = temporaryFolder.newFile("bad.csv").toPath();
        Files.write(csv, ("map_id,family\nonly,OPEN\n").getBytes(StandardCharsets.UTF_8));
        FactoryFeudD3ExperimentRunner.readMetadata(csv);
    }

    @Test(expected = IllegalArgumentException.class)
    public void incompleteStratumCannotBeSelected() {
        MapMetadata one = new MapMetadata("only", FactoryFeudMapFamily.OPEN,
                FactoryFeudDensityPreset.SPARSE, 1L, "missing.xml", 0.0, 1, 1, 0, 0);
        FactoryFeudD3ExperimentRunner.selectBalanced(java.util.Collections.singletonList(one), 2);
    }
}
