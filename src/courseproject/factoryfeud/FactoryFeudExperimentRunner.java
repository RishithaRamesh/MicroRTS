package courseproject.factoryfeud;

import ai.core.AI;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import rts.GameState;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/** Runs paired, side-swapped AdaptiveRoleAI versus FixedRoleAI experiments. */
public final class FactoryFeudExperimentRunner {
    private FactoryFeudExperimentRunner() {
    }

    public static void main(String[] args) throws Exception {
        FactoryFeudExperimentConfig config = FactoryFeudExperimentConfig.parse(args);
        FactoryFeudExperimentSummary summary = run(config);
        System.out.println("csv=" + config.getCsvPath().toAbsolutePath());
        System.out.println("baseSeed=" + config.getSeed()
                + " (one seed per two-match side-swapped pair)");
        System.out.println("adaptiveStrategy="
                + config.getAdaptiveStrategy().getCommandLineName());
        System.out.println(summary.format());
    }

    public static FactoryFeudExperimentSummary run(FactoryFeudExperimentConfig config)
            throws Exception {
        List<FactoryFeudExperimentRecord> records = new ArrayList<>();
        for (int index = 0; index < config.getMatchCount(); index++) {
            int adaptiveSide = index % 2;
            long pairSeed = config.getSeed() + index / 2;
            records.add(runMatch(index + 1, adaptiveSide, pairSeed, config));
        }
        writeCsv(config.getCsvPath(), records);
        return new FactoryFeudExperimentSummary(records);
    }

    private static FactoryFeudExperimentRecord runMatch(int matchId, int adaptiveSide,
            long seed, FactoryFeudExperimentConfig config) throws Exception {
        // Public engine RNG used for variable combat damage. The current deterministic
        // unit table has fixed damage, but recording and applying the seed is future-safe.
        UnitAction.r.setSeed(seed);
        UnitTypeTable utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        GameState gameState = FactoryFeudScenario.load(config.getMapPath(), utt);
        AdaptiveRoleAI adaptive = config.getAdaptiveStrategy()
                == FactoryFeudExperimentConfig.AdaptiveStrategy.CARGO_SAFE
                        ? new CargoSafeAdaptiveRoleAI(utt, config.getCycles())
                        : new AdaptiveRoleAI(utt, config.getCycles());
        FixedRoleAI fixed = new FixedRoleAI(utt);
        AI player0 = adaptiveSide == 0 ? adaptive : fixed;
        AI player1 = adaptiveSide == 1 ? adaptive : fixed;
        FactoryFeudMatch match = new FactoryFeudMatch(
                gameState, config.getCycles(), player0, player1);

        long start = System.nanoTime();
        FactoryFeudResult result = match.run();
        long runtimeMillis = (System.nanoTime() - start) / 1_000_000L;
        FactoryFeudMatchTelemetry telemetry = match.getTelemetry();
        int player0Survivors = survivorCount(gameState, 0);
        int player1Survivors = survivorCount(gameState, 1);
        int adaptiveScore = result.getScore(adaptiveSide);
        int fixedScore = result.getScore(1 - adaptiveSide);
        FactoryFeudExperimentRecord.Outcome outcome = adaptiveScore == fixedScore
                ? FactoryFeudExperimentRecord.Outcome.DRAW
                : adaptiveScore > fixedScore
                        ? FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN
                        : FactoryFeudExperimentRecord.Outcome.FIXED_WIN;

        FactoryFeudExperimentRecord.DeliveryMetrics player0Delivery = deliveryMetrics(
                telemetry.getDeliveryTiming(0));
        FactoryFeudExperimentRecord.DeliveryMetrics player1Delivery = deliveryMetrics(
                telemetry.getDeliveryTiming(1));
        FactoryFeudExperimentRecord.CarrierMetrics selectedCarrier = carrierMetrics(
                telemetry.getCarrierMetrics(adaptiveSide, gameState));
        FactoryFeudExperimentRecord.CarrierMetrics fixedCarrier = carrierMetrics(
                telemetry.getCarrierMetrics(1 - adaptiveSide, gameState));
        boolean cargoSafe = config.getAdaptiveStrategy()
                == FactoryFeudExperimentConfig.AdaptiveStrategy.CARGO_SAFE;

        return new FactoryFeudExperimentRecord(matchId, config.getMapPath(), seed,
                adaptiveSide, result.getScore(0), result.getScore(1), adaptiveScore,
                fixedScore, outcome, result.getCycles(), player0Survivors,
                player1Survivors, adaptiveSide == 0 ? player0Survivors : player1Survivors,
                adaptiveSide == 0 ? player1Survivors : player0Survivors,
                adaptive.getRoleChangeCount(), match.getIllegalActionCount(), runtimeMillis,
                config.getCycles(), telemetry.getCompletedCycles(), runtimeMillis,
                player0Delivery, player1Delivery,
                config.getAdaptiveStrategy().getCommandLineName(),
                cargoSafe ? null : selectedCarrier, fixedCarrier,
                cargoSafe ? selectedCarrier : null);
    }

    private static FactoryFeudExperimentRecord.DeliveryMetrics deliveryMetrics(
            FactoryFeudMatchTelemetry.DeliveryTiming timing) {
        return new FactoryFeudExperimentRecord.DeliveryMetrics(
                timing.getFirstDeliveryCycle(), timing.getLastDeliveryCycle(),
                timing.getMeanDeliveryIntervalCycles(), timing.getCompletedDeliveryEvents());
    }

    private static FactoryFeudExperimentRecord.CarrierMetrics carrierMetrics(
            FactoryFeudMatchTelemetry.CarrierMetrics metrics) {
        Integer roleInterruptions = metrics.areRoleMetricsAvailable()
                ? metrics.getRoleInterruptions() : null;
        Integer interruptedCycles = metrics.areRoleMetricsAvailable()
                ? metrics.getInterruptedCycles() : null;
        Integer corrections = metrics.areRoleMetricsAvailable()
                ? metrics.getCorrections() : null;
        return new FactoryFeudExperimentRecord.CarrierMetrics(
                roleInterruptions, interruptedCycles, metrics.getAttacksReceived(),
                metrics.getDeaths(), metrics.getUndeliveredCocoaAtEnd(), corrections);
    }

    private static int survivorCount(GameState gameState, int player) {
        int survivors = 0;
        for (Unit unit : gameState.getUnits()) if (unit.getPlayer() == player) survivors++;
        return survivors;
    }

    private static void writeCsv(Path csvPath, List<FactoryFeudExperimentRecord> records)
            throws Exception {
        Path absolute = csvPath.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (BufferedWriter writer = Files.newBufferedWriter(
                absolute, StandardCharsets.UTF_8)) {
            writer.write(FactoryFeudExperimentRecord.CSV_HEADER);
            writer.newLine();
            for (FactoryFeudExperimentRecord record : records) {
                writer.write(record.toCsvRow());
                writer.newLine();
            }
        }
    }
}
