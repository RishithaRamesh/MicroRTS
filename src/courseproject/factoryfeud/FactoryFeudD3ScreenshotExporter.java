package courseproject.factoryfeud;

import ai.core.AI;
import gui.PhysicalGameStatePanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import rts.GameState;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/**
 * Replays audited D3 cases and exports unannotated frames through the native
 * microRTS board renderer. Captures are taken at the start of the named cycle,
 * immediately before that cycle's recorded controller decision.
 */
public final class FactoryFeudD3ScreenshotExporter {
    private static final Path DEFAULT_RESULTS = Paths.get(
            "results/factory-feud-d3/match-results.csv");
    private static final Path DEFAULT_OUTPUT = Paths.get(
            "results/factory-feud-d3/gameplay-captures");
    private static final int IMAGE_WIDTH = 1024;
    private static final int IMAGE_HEIGHT = 768;
    private static final int MATCH_CYCLES = 500;

    private static final CaptureSpec[] CAPTURES = {
        new CaptureSpec(6, 180, "match-006-open-cycle-180.png",
                "OPEN representative: simultaneous retreat and self-defense counterattack"),
        new CaptureSpec(66, 155, "match-066-chokepoint-cycle-155.png",
                "CHOKEPOINT representative: Collector retreat at the constrained center"),
        new CaptureSpec(50, 210, "match-050-main-cycle-210.png",
                "MAIN tradeoff case: fourth consecutive Collector retreat"),
        new CaptureSpec(52, 210, "match-052-ablation-cycle-210.png",
                "Matched tactical-off state at the same map, seed, side, and cycle"),
        new CaptureSpec(61, 320, "match-061-role-switch-cycle-320.png",
                "Optional context: three role changes occur at this cycle; roles are not drawn")
    };

    private FactoryFeudD3ScreenshotExporter() { }

    public static void main(String[] args) throws Exception {
        Path results = DEFAULT_RESULTS;
        Path output = DEFAULT_OUTPUT;
        for (int index = 0; index < args.length; index++) {
            String option = args[index];
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            String value = args[++index];
            if ("--results".equals(option)) results = Paths.get(value);
            else if ("--output-dir".equals(option)) output = Paths.get(value);
            else throw new IllegalArgumentException("Unknown option: " + option);
        }
        export(results, output);
    }

    static void export(Path results, Path output) throws Exception {
        Map<Integer, MatchRow> rows = readRows(results);
        Files.createDirectories(output);
        List<String> manifestRows = new ArrayList<>();
        manifestRows.add("match_id,event_cycle,capture_timing,experiment_type,map_id,family,"
                + "density_preset,adaptive_side,p0_ai,p1_ai,simulation_seed,p0_cocoa,p1_cocoa,"
                + "adaptive_surviving_workers,fixed_surviving_workers,role_changes,"
                + "tactical_counterattacks,tactical_retreats,image,rationale");

        for (CaptureSpec spec : CAPTURES) {
            MatchRow row = rows.get(spec.matchId);
            if (row == null) throw new IllegalArgumentException(
                    "Results do not contain match " + spec.matchId);
            Path image = output.resolve(spec.filename);
            replayAndCapture(row, spec, image);
            manifestRows.add(row.manifestRow(spec));
            System.out.println("captured match=" + spec.matchId + " cycle="
                    + spec.cycle + " image=" + image.toAbsolutePath());
        }

        Path manifest = output.resolve("capture-manifest.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(manifest, StandardCharsets.UTF_8)) {
            for (String line : manifestRows) {
                writer.write(line);
                writer.newLine();
            }
        }
        System.out.println("manifest=" + manifest.toAbsolutePath());
    }

    private static void replayAndCapture(MatchRow row, CaptureSpec spec, Path output)
            throws Exception {
        UnitAction.r.setSeed(row.simulationSeed);
        UnitTypeTable utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        GameState state = FactoryFeudScenario.load(row.mapPath, utt);
        FactoryFeudExperimentConfig.AdaptiveStrategy strategy = "MAIN".equals(row.experiment)
                ? FactoryFeudExperimentConfig.AdaptiveStrategy.ADAPTIVE
                : FactoryFeudExperimentConfig.AdaptiveStrategy.OLD_ADAPTIVE;
        OldAdaptiveRoleAI adaptive = strategy.create(utt, MATCH_CYCLES);
        FixedRoleAI fixed = new FixedRoleAI(utt);
        AI player0 = row.adaptiveSide == 0 ? adaptive : fixed;
        AI player1 = row.adaptiveSide == 1 ? adaptive : fixed;
        validateControllerNames(row, player0, player1);

        FactoryFeudMatch match = new FactoryFeudMatch(
                state, MATCH_CYCLES, player0, player1);
        final boolean[] captured = {false};
        FactoryFeudResult result = match.run(current -> {
            if (!captured[0] && current.getGameState().getTime() == spec.cycle) {
                render(output, current.getGameState());
                captured[0] = true;
            }
        });
        if (!captured[0]) throw new IllegalStateException(
                "Replay never reached capture cycle " + spec.cycle);
        validateReplay(row, result, state, adaptive);
    }

    private static void render(Path output, GameState state) throws Exception {
        BufferedImage image = new BufferedImage(
                IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(34, 39, 46));
        graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        PhysicalGameStatePanel.draw(graphics, null, IMAGE_WIDTH, IMAGE_HEIGHT,
                state, null, PhysicalGameStatePanel.COLORSCHEME_BLACK,
                true, -1, null);
        graphics.dispose();
        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IllegalStateException("No PNG writer is available");
        }
    }

    private static void validateControllerNames(MatchRow row, AI player0, AI player1) {
        String p0 = player0.getClass().getSimpleName();
        String p1 = player1.getClass().getSimpleName();
        if (!row.p0Ai.equals(p0) || !row.p1Ai.equals(p1)) {
            throw new IllegalStateException("Controller mismatch for match " + row.matchId
                    + ": expected " + row.p0Ai + "/" + row.p1Ai
                    + " but replay created " + p0 + "/" + p1);
        }
    }

    private static void validateReplay(MatchRow row, FactoryFeudResult result,
            GameState finalState, OldAdaptiveRoleAI adaptive) {
        int adaptiveWorkers = workerCount(finalState, row.adaptiveSide);
        int fixedWorkers = workerCount(finalState, 1 - row.adaptiveSide);
        AdaptiveRoleAI tactical = adaptive instanceof AdaptiveRoleAI
                ? (AdaptiveRoleAI) adaptive : null;
        int attacks = tactical == null ? 0 : tactical.getTacticalAttackOverrideCount();
        int retreats = tactical == null ? 0 : tactical.getTacticalRetreatOverrideCount();
        if (result.getScore(0) != row.p0Cocoa || result.getScore(1) != row.p1Cocoa
                || adaptiveWorkers != row.adaptiveWorkers
                || fixedWorkers != row.fixedWorkers
                || adaptive.getRoleChangeCount() != row.roleChanges
                || attacks != row.tacticalAttacks || retreats != row.tacticalRetreats) {
            throw new IllegalStateException("Replay validation failed for match " + row.matchId
                    + "; generated frame is not backed by the audited result");
        }
    }

    private static int workerCount(GameState state, int player) {
        int count = 0;
        for (Unit unit : state.getUnits()) {
            if (unit.getPlayer() == player && unit.getType().canHarvest) count++;
        }
        return count;
    }

    private static Map<Integer, MatchRow> readRows(Path results) throws Exception {
        if (!Files.isRegularFile(results)) {
            throw new IllegalArgumentException("D3 results do not exist: " + results);
        }
        Map<Integer, MatchRow> rows = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(results, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("D3 results are empty");
            List<String> header = FactoryFeudD3ExperimentRunner.parseCsvRow(headerLine);
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (int index = 0; index < header.size(); index++) {
                columns.put(header.get(index), index);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                MatchRow row = new MatchRow(
                        FactoryFeudD3ExperimentRunner.parseCsvRow(line), columns);
                rows.put(row.matchId, row);
            }
        }
        return rows;
    }

    private static String cell(List<String> cells, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (index == null || index >= cells.size()) {
            throw new IllegalArgumentException("D3 results missing column " + name);
        }
        return cells.get(index);
    }

    private static String csv(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static final class CaptureSpec {
        final int matchId;
        final int cycle;
        final String filename;
        final String rationale;

        CaptureSpec(int matchId, int cycle, String filename, String rationale) {
            this.matchId = matchId;
            this.cycle = cycle;
            this.filename = filename;
            this.rationale = rationale;
        }
    }

    private static final class MatchRow {
        final int matchId;
        final String experiment;
        final String mapId;
        final String family;
        final String density;
        final String mapPath;
        final long simulationSeed;
        final String p0Ai;
        final String p1Ai;
        final int adaptiveSide;
        final int p0Cocoa;
        final int p1Cocoa;
        final int adaptiveWorkers;
        final int fixedWorkers;
        final int roleChanges;
        final int tacticalAttacks;
        final int tacticalRetreats;

        MatchRow(List<String> cells, Map<String, Integer> columns) {
            matchId = Integer.parseInt(cell(cells, columns, "match_id"));
            experiment = cell(cells, columns, "experiment_type");
            mapId = cell(cells, columns, "map_id");
            family = cell(cells, columns, "family");
            density = cell(cells, columns, "density_preset");
            mapPath = cell(cells, columns, "map_path");
            simulationSeed = Long.parseLong(cell(cells, columns, "simulation_seed"));
            p0Ai = cell(cells, columns, "p0_ai");
            p1Ai = cell(cells, columns, "p1_ai");
            adaptiveSide = Integer.parseInt(cell(cells, columns, "adaptive_side"));
            p0Cocoa = Integer.parseInt(cell(cells, columns, "p0_cocoa"));
            p1Cocoa = Integer.parseInt(cell(cells, columns, "p1_cocoa"));
            adaptiveWorkers = Integer.parseInt(cell(
                    cells, columns, "adaptive_surviving_workers"));
            fixedWorkers = Integer.parseInt(cell(
                    cells, columns, "fixed_surviving_workers"));
            roleChanges = Integer.parseInt(cell(cells, columns, "role_changes"));
            tacticalAttacks = Integer.parseInt(cell(
                    cells, columns, "tactical_counterattacks"));
            tacticalRetreats = Integer.parseInt(cell(
                    cells, columns, "tactical_retreats"));
        }

        String manifestRow(CaptureSpec spec) {
            return matchId + "," + spec.cycle + ",start-of-cycle-before-controller-decision,"
                    + experiment + "," + mapId + "," + family + "," + density + ","
                    + adaptiveSide + "," + p0Ai + "," + p1Ai + "," + simulationSeed + ","
                    + p0Cocoa + "," + p1Cocoa + "," + adaptiveWorkers + "," + fixedWorkers
                    + "," + roleChanges + "," + tacticalAttacks + "," + tacticalRetreats
                    + "," + spec.filename + "," + csv(spec.rationale);
        }
    }
}
