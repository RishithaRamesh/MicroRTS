package courseproject.factoryfeud.generation;

import ai.evaluation.SimpleEvaluationFunction;
import gui.PhysicalGameStatePanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;
import util.XMLWriter;

/** Reproduces and renders one audited invalid raw candidate without retrying. */
public final class FactoryFeudInvalidMapDiagnostic {
    private static final int WIDTH = 16;
    private static final int HEIGHT = 12;
    private static final long ATTEMPT_SEED = 2584001L;
    private static final String EXPECTED_REJECTION =
            "Player 1 cannot reach cocoa resource 0";
    private static final Path OUTPUT_DIRECTORY = Paths.get(
            "results", "factory-feud-d2", "invalid-examples");
    private static final String OUTPUT_BASENAME =
            "chokepoint-low-attempt-seed-2584001-invalid";
    private static final int IMAGE_WIDTH = 1200;
    private static final int IMAGE_HEIGHT = 820;

    private FactoryFeudInvalidMapDiagnostic() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            throw new IllegalArgumentException("This diagnostic does not accept arguments");
        }

        UnitTypeTable utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        FactoryFeudGeneratorConfig config = new FactoryFeudGeneratorConfig(
                WIDTH, HEIGHT,
                FactoryFeudDensityPreset.LOW.densityFor(FactoryFeudMapFamily.CHOKEPOINT),
                FactoryFeudMapFamily.CHOKEPOINT, ATTEMPT_SEED, 1);

        // Deliberately construct attempt 1 directly. Do not invoke generate(), which retries.
        PhysicalGameState candidate = FactoryFeudMapGenerator.createCandidate(
                config, utt, new Random(ATTEMPT_SEED));
        FactoryFeudMapMetrics metrics = FactoryFeudMapMetrics.compute(candidate);
        verifyAuditMetrics(metrics);
        String rejection = rejectionReason(candidate);
        if (!EXPECTED_REJECTION.equals(rejection)) {
            throw new IllegalStateException("Unexpected validation result: " + rejection);
        }

        Files.createDirectories(OUTPUT_DIRECTORY);
        Path xmlOutput = OUTPUT_DIRECTORY.resolve(OUTPUT_BASENAME + ".xml");
        Path pngOutput = OUTPUT_DIRECTORY.resolve(OUTPUT_BASENAME + "-clean.png");
        exportXmlWithoutReplacingDifferentContent(xmlOutput, candidate);

        PhysicalGameState reloaded = PhysicalGameState.load(xmlOutput.toString(), utt);
        if (!FactoryFeudBatchGenerator.sameMap(candidate, reloaded)) {
            throw new IllegalStateException("Diagnostic XML round trip changed the candidate");
        }
        String reloadedRejection = rejectionReason(reloaded);
        if (!EXPECTED_REJECTION.equals(reloadedRejection)) {
            throw new IllegalStateException(
                    "Reloaded candidate had different validation result: " + reloadedRejection);
        }

        renderCleanPng(pngOutput, reloaded, utt);
        System.out.println("attemptSeed=" + ATTEMPT_SEED);
        System.out.println("family=" + config.getFamily());
        System.out.println("densityPreset=LOW");
        System.out.println("requestedDensity=" + config.getObstacleDensity());
        System.out.println("actualDensity=" + metrics.getActualObstacleDensity());
        System.out.println("player0PathLength=" + metrics.getPlayer0FactoryToCocoaPath());
        System.out.println("player1PathLength=" + metrics.getPlayer1FactoryToCocoaPath());
        System.out.println("validationRejection=" + rejection);
        System.out.println("xml=" + xmlOutput.toAbsolutePath());
        System.out.println("png=" + pngOutput.toAbsolutePath());
    }

    private static void verifyAuditMetrics(FactoryFeudMapMetrics metrics) {
        if (metrics.getObstacleCount() != 27
                || Double.compare(metrics.getActualObstacleDensity(), 0.140625) != 0
                || metrics.getPlayer0FactoryToCocoaPath() != 7
                || metrics.getPlayer1FactoryToCocoaPath() != -1
                || metrics.getRouteDifference() != -1
                || metrics.getDeadEndCount() != 4) {
            throw new IllegalStateException("Raw candidate does not match the attempts CSV");
        }
    }

    private static String rejectionReason(PhysicalGameState candidate) {
        try {
            FactoryFeudGeneratedMapValidator.validate(candidate);
            throw new IllegalStateException("Expected the raw candidate to be rejected");
        } catch (IllegalArgumentException rejected) {
            return rejected.getMessage();
        }
    }

    private static void exportXmlWithoutReplacingDifferentContent(
            Path output, PhysicalGameState candidate) throws Exception {
        StringWriter text = new StringWriter();
        XMLWriter writer = new XMLWriter(text);
        candidate.toxml(writer);
        writer.flush();
        byte[] xml = text.toString().getBytes(StandardCharsets.UTF_8);
        if (Files.exists(output)) {
            if (!Arrays.equals(Files.readAllBytes(output), xml)) {
                throw new IllegalStateException(
                        "Refusing to overwrite different diagnostic XML: " + output);
            }
            return;
        }
        Files.write(output, xml);
    }

    private static void renderCleanPng(
            Path output, PhysicalGameState candidate, UnitTypeTable utt) throws Exception {
        BufferedImage image = new BufferedImage(
                IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(34, 39, 46));
        graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        PhysicalGameStatePanel.draw(graphics, null, IMAGE_WIDTH, IMAGE_HEIGHT,
                new GameState(candidate, utt), null,
                PhysicalGameStatePanel.COLORSCHEME_BLACK, true, -1,
                new SimpleEvaluationFunction());
        graphics.dispose();

        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IllegalStateException("No PNG writer is available");
        }
    }
}
