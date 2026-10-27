package courseproject.factoryfeud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FactoryFeudControlledExperimentRunnerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void discoveryUsesOnlySortedDirectXmlChildren() throws Exception {
        Path directory = temporaryFolder.newFolder("maps").toPath();
        Files.write(directory.resolve("z.xml"), Arrays.asList("z"), StandardCharsets.UTF_8);
        Files.write(directory.resolve("a.xml"), Arrays.asList("a"), StandardCharsets.UTF_8);
        Files.write(directory.resolve("ignored.txt"), Arrays.asList("x"), StandardCharsets.UTF_8);
        Files.write(directory.resolve("upper.XML"), Arrays.asList("x"), StandardCharsets.UTF_8);
        Path nested = Files.createDirectory(directory.resolve("nested"));
        Files.write(nested.resolve("hidden.xml"), Arrays.asList("x"), StandardCharsets.UTF_8);

        List<String> maps = FactoryFeudControlledExperimentRunner.discoverMaps(directory);

        assertEquals(Arrays.asList(directory.resolve("a.xml").toString(),
                directory.resolve("z.xml").toString()), maps);
    }

    @Test
    public void discoveredProjectMapsAllPassStrictValidation() throws Exception {
        List<String> maps = FactoryFeudControlledExperimentRunner.discoverMaps(
                FactoryFeudControlledExperimentRunner.MAP_DIRECTORY);
        assertEquals(maps.size(),
                FactoryFeudControlledExperimentRunner.auditMaps(maps).size());
    }

    @Test
    public void pairedSidesCsvAndReplayAreDeterministic() throws Exception {
        String map = FactoryFeudControlledExperimentRunner.discoverMaps(
                FactoryFeudControlledExperimentRunner.MAP_DIRECTORY).get(0);
        Path firstCsv = temporaryFolder.newFile("first.csv").toPath();
        Path secondCsv = temporaryFolder.newFile("second.csv").toPath();
        List<FactoryFeudExperimentRecord> first =
                FactoryFeudControlledExperimentRunner.runMatches(
                        firstCsv, Arrays.asList(map), 40, 584L);
        List<FactoryFeudExperimentRecord> second =
                FactoryFeudControlledExperimentRunner.runMatches(
                        secondCsv, Arrays.asList(map), 40, 584L);

        assertEquals(2, first.size());
        assertEquals(1, first.get(0).getAdaptiveSide());
        assertEquals(0, first.get(1).getAdaptiveSide());
        assertEquals(first.get(0).getPlayer1Score(), first.get(0).getAdaptiveScore());
        assertEquals(first.get(0).getPlayer0Score(), first.get(0).getFixedScore());
        assertEquals(first.get(1).getPlayer0Score(), first.get(1).getAdaptiveScore());
        assertEquals(first.get(1).getPlayer1Score(), first.get(1).getFixedScore());
        assertEquals(3, Files.readAllLines(firstCsv, StandardCharsets.UTF_8).size());
        assertTrue(Files.readAllLines(firstCsv, StandardCharsets.UTF_8).get(0)
                .equals(FactoryFeudExperimentRecord.CSV_HEADER));
        for (int index = 0; index < first.size(); index++) {
            assertTrue(first.get(index).sameSimulationResult(second.get(index)));
        }
    }

    @Test
    public void winnerLabelsUseAdaptivePerspectiveRegardlessOfSide() {
        assertEquals("WIN", FactoryFeudControlledExperimentRunner.adaptiveResultLabel(
                record(FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN)));
        assertEquals("DRAW", FactoryFeudControlledExperimentRunner.adaptiveResultLabel(
                record(FactoryFeudExperimentRecord.Outcome.DRAW)));
        assertEquals("LOSS", FactoryFeudControlledExperimentRunner.adaptiveResultLabel(
                record(FactoryFeudExperimentRecord.Outcome.FIXED_WIN)));
    }

    private static FactoryFeudExperimentRecord record(
            FactoryFeudExperimentRecord.Outcome outcome) {
        return new FactoryFeudExperimentRecord(1, "map.xml", 584L, 1,
                1, 2, 2, 1, outcome, 500, 5, 5, 5, 5, 0, 0, 1);
    }
}
