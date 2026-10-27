package courseproject.factoryfeud;

import java.util.Objects;

/** Immutable data for one side-specific match in a paired experiment. */
public final class FactoryFeudExperimentRecord {
    public enum Outcome { ADAPTIVE_WIN, FIXED_WIN, DRAW }

    /** The original 17 columns are retained first, in their original order. */
    public static final String LEGACY_CSV_HEADER = "match_id,map,seed,adaptive_side,"
            + "player0_score,player1_score,adaptive_score,fixed_score,outcome,match_cycles,"
            + "player0_survivors,player1_survivors,adaptive_survivors,fixed_survivors,"
            + "role_reassignments,illegal_action_count,runtime_ms";
    public static final String CSV_HEADER = LEGACY_CSV_HEADER
            + ",configured_cycles,completed_cycles,simulation_runtime_ms"
            + ",player0_first_delivery_cycle,player0_last_delivery_cycle,"
            + "player0_mean_delivery_interval_cycles,player0_completed_delivery_events"
            + ",player1_first_delivery_cycle,player1_last_delivery_cycle,"
            + "player1_mean_delivery_interval_cycles,player1_completed_delivery_events"
            + ",adaptive_strategy,tactical_overrides,tactical_attack_overrides,"
            + "tactical_retreat_overrides"
            + ",adaptive_carrier_role_interruptions,adaptive_carrier_interrupted_cycles,"
            + "adaptive_carrier_attacks_received,adaptive_carrier_deaths,"
            + "adaptive_undelivered_cocoa_at_end,adaptive_carrier_correction_events"
            + ",fixed_carrier_role_interruptions,fixed_carrier_interrupted_cycles,"
            + "fixed_carrier_attacks_received,fixed_carrier_deaths,"
            + "fixed_undelivered_cocoa_at_end,fixed_carrier_correction_events"
            + ",cargo_safe_carrier_role_interruptions,cargo_safe_carrier_interrupted_cycles,"
            + "cargo_safe_carrier_attacks_received,cargo_safe_carrier_deaths,"
            + "cargo_safe_undelivered_cocoa_at_end,cargo_safe_carrier_correction_events"
            + ",player0_surviving_workers,player1_surviving_workers,"
            + "adaptive_surviving_workers,fixed_surviving_workers,"
            + "example_attack_override,example_retreat_override";

    private final int matchId;
    private final String map;
    private final long seed;
    private final int adaptiveSide;
    private final int player0Score;
    private final int player1Score;
    private final int adaptiveScore;
    private final int fixedScore;
    private final Outcome outcome;
    private final int matchCycles;
    private final int player0Survivors;
    private final int player1Survivors;
    private final int adaptiveSurvivors;
    private final int fixedSurvivors;
    private final int roleReassignments;
    private final int illegalActionCount;
    private final long runtimeMillis;
    private final int configuredCycles;
    private final int completedCycles;
    private final long simulationRuntimeMillis;
    private final DeliveryMetrics player0Delivery;
    private final DeliveryMetrics player1Delivery;
    private final String adaptiveStrategy;
    private final CarrierMetrics adaptiveCarrier;
    private final CarrierMetrics fixedCarrier;
    private final CarrierMetrics cargoSafeCarrier;
    private final int tacticalOverrides;
    private final int tacticalAttackOverrides;
    private final int tacticalRetreatOverrides;
    private final int player0SurvivingWorkers;
    private final int player1SurvivingWorkers;
    private final int adaptiveSurvivingWorkers;
    private final int fixedSurvivingWorkers;
    private final String exampleAttackOverride;
    private final String exampleRetreatOverride;

    /** Backward-compatible constructor for existing callers and aggregate tests. */
    public FactoryFeudExperimentRecord(int matchId, String map, long seed, int adaptiveSide,
            int player0Score, int player1Score, int adaptiveScore, int fixedScore,
            Outcome outcome, int matchCycles, int player0Survivors, int player1Survivors,
            int adaptiveSurvivors, int fixedSurvivors, int roleReassignments,
            int illegalActionCount, long runtimeMillis) {
        this(matchId, map, seed, adaptiveSide, player0Score, player1Score,
                adaptiveScore, fixedScore, outcome, matchCycles, player0Survivors,
                player1Survivors, adaptiveSurvivors, fixedSurvivors,
                roleReassignments, illegalActionCount, runtimeMillis,
                matchCycles, matchCycles, runtimeMillis, DeliveryMetrics.unavailable(),
                DeliveryMetrics.unavailable(), "adaptive", null, null, null, 0, 0, 0,
                Math.max(0, player0Survivors - FactoryFeudScenario.FACTORIES_PER_TEAM),
                Math.max(0, player1Survivors - FactoryFeudScenario.FACTORIES_PER_TEAM),
                Math.max(0, adaptiveSurvivors - FactoryFeudScenario.FACTORIES_PER_TEAM),
                Math.max(0, fixedSurvivors - FactoryFeudScenario.FACTORIES_PER_TEAM),
                null, null);
    }

    public FactoryFeudExperimentRecord(int matchId, String map, long seed, int adaptiveSide,
            int player0Score, int player1Score, int adaptiveScore, int fixedScore,
            Outcome outcome, int matchCycles, int player0Survivors, int player1Survivors,
            int adaptiveSurvivors, int fixedSurvivors, int roleReassignments,
            int illegalActionCount, long runtimeMillis, int configuredCycles,
            int completedCycles, long simulationRuntimeMillis,
            DeliveryMetrics player0Delivery, DeliveryMetrics player1Delivery,
            String adaptiveStrategy, CarrierMetrics adaptiveCarrier,
            CarrierMetrics fixedCarrier, CarrierMetrics cargoSafeCarrier,
            int tacticalOverrides, int tacticalAttackOverrides,
            int tacticalRetreatOverrides, int player0SurvivingWorkers,
            int player1SurvivingWorkers, int adaptiveSurvivingWorkers,
            int fixedSurvivingWorkers, String exampleAttackOverride,
            String exampleRetreatOverride) {
        this.matchId = matchId;
        this.map = map;
        this.seed = seed;
        this.adaptiveSide = adaptiveSide;
        this.player0Score = player0Score;
        this.player1Score = player1Score;
        this.adaptiveScore = adaptiveScore;
        this.fixedScore = fixedScore;
        this.outcome = outcome;
        this.matchCycles = matchCycles;
        this.player0Survivors = player0Survivors;
        this.player1Survivors = player1Survivors;
        this.adaptiveSurvivors = adaptiveSurvivors;
        this.fixedSurvivors = fixedSurvivors;
        this.roleReassignments = roleReassignments;
        this.illegalActionCount = illegalActionCount;
        this.runtimeMillis = runtimeMillis;
        this.configuredCycles = configuredCycles;
        this.completedCycles = completedCycles;
        this.simulationRuntimeMillis = simulationRuntimeMillis;
        this.player0Delivery = player0Delivery;
        this.player1Delivery = player1Delivery;
        this.adaptiveStrategy = adaptiveStrategy;
        this.adaptiveCarrier = adaptiveCarrier;
        this.fixedCarrier = fixedCarrier;
        this.cargoSafeCarrier = cargoSafeCarrier;
        this.tacticalOverrides = tacticalOverrides;
        this.tacticalAttackOverrides = tacticalAttackOverrides;
        this.tacticalRetreatOverrides = tacticalRetreatOverrides;
        this.player0SurvivingWorkers = player0SurvivingWorkers;
        this.player1SurvivingWorkers = player1SurvivingWorkers;
        this.adaptiveSurvivingWorkers = adaptiveSurvivingWorkers;
        this.fixedSurvivingWorkers = fixedSurvivingWorkers;
        this.exampleAttackOverride = exampleAttackOverride;
        this.exampleRetreatOverride = exampleRetreatOverride;
    }

    public String toCsvRow() {
        StringBuilder row = new StringBuilder();
        row.append(matchId).append(',').append(csv(map)).append(',').append(seed).append(',')
                .append(adaptiveSide).append(',').append(player0Score).append(',')
                .append(player1Score).append(',').append(adaptiveScore).append(',')
                .append(fixedScore).append(',').append(outcome).append(',')
                .append(matchCycles).append(',').append(player0Survivors).append(',')
                .append(player1Survivors).append(',').append(adaptiveSurvivors).append(',')
                .append(fixedSurvivors).append(',').append(roleReassignments).append(',')
                .append(illegalActionCount).append(',').append(runtimeMillis).append(',')
                .append(configuredCycles).append(',').append(completedCycles).append(',')
                .append(simulationRuntimeMillis);
        appendDelivery(row, player0Delivery);
        appendDelivery(row, player1Delivery);
        row.append(',').append(csv(adaptiveStrategy)).append(',').append(tacticalOverrides)
                .append(',').append(tacticalAttackOverrides)
                .append(',').append(tacticalRetreatOverrides);
        appendCarrier(row, adaptiveCarrier);
        appendCarrier(row, fixedCarrier);
        appendCarrier(row, cargoSafeCarrier);
        row.append(',').append(player0SurvivingWorkers)
                .append(',').append(player1SurvivingWorkers)
                .append(',').append(adaptiveSurvivingWorkers)
                .append(',').append(fixedSurvivingWorkers)
                .append(',').append(csv(exampleAttackOverride))
                .append(',').append(csv(exampleRetreatOverride));
        return row.toString();
    }

    private static void appendDelivery(StringBuilder row, DeliveryMetrics timing) {
        row.append(',').append(value(timing == null ? null : timing.firstCycle));
        row.append(',').append(value(timing == null ? null : timing.lastCycle));
        row.append(',').append(value(timing == null ? null : timing.meanInterval));
        row.append(',').append(value(timing == null ? null : timing.completedEvents));
    }

    private static void appendCarrier(StringBuilder row, CarrierMetrics metrics) {
        row.append(',').append(value(metrics == null ? null : metrics.roleInterruptions));
        row.append(',').append(value(metrics == null ? null : metrics.interruptedCycles));
        row.append(',').append(value(metrics == null ? null : metrics.attacksReceived));
        row.append(',').append(value(metrics == null ? null : metrics.deaths));
        row.append(',').append(value(metrics == null ? null : metrics.undeliveredCocoaAtEnd));
        row.append(',').append(value(metrics == null ? null : metrics.corrections));
    }

    private static String value(Object value) { return value == null ? "" : value.toString(); }

    private static String csv(String value) {
        if (value == null) return "";
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    public int getMatchId() { return matchId; }
    public String getMap() { return map; }
    public long getSeed() { return seed; }
    public int getAdaptiveSide() { return adaptiveSide; }
    public int getPlayer0Score() { return player0Score; }
    public int getPlayer1Score() { return player1Score; }
    public int getAdaptiveScore() { return adaptiveScore; }
    public int getFixedScore() { return fixedScore; }
    public Outcome getOutcome() { return outcome; }
    public int getMatchCycles() { return matchCycles; }
    public int getPlayer0Survivors() { return player0Survivors; }
    public int getPlayer1Survivors() { return player1Survivors; }
    public int getAdaptiveSurvivors() { return adaptiveSurvivors; }
    public int getFixedSurvivors() { return fixedSurvivors; }
    public int getRoleReassignments() { return roleReassignments; }
    public int getIllegalActionCount() { return illegalActionCount; }
    public long getRuntimeMillis() { return runtimeMillis; }
    public int getConfiguredCycles() { return configuredCycles; }
    public int getCompletedCycles() { return completedCycles; }
    public long getSimulationRuntimeMillis() { return simulationRuntimeMillis; }
    public DeliveryMetrics getPlayer0Delivery() { return player0Delivery; }
    public DeliveryMetrics getPlayer1Delivery() { return player1Delivery; }
    public String getAdaptiveStrategy() { return adaptiveStrategy; }
    public CarrierMetrics getAdaptiveCarrier() { return adaptiveCarrier; }
    public CarrierMetrics getFixedCarrier() { return fixedCarrier; }
    public CarrierMetrics getCargoSafeCarrier() { return cargoSafeCarrier; }
    public int getTacticalOverrides() { return tacticalOverrides; }
    public int getTacticalAttackOverrides() { return tacticalAttackOverrides; }
    public int getTacticalRetreatOverrides() { return tacticalRetreatOverrides; }
    public int getPlayer0SurvivingWorkers() { return player0SurvivingWorkers; }
    public int getPlayer1SurvivingWorkers() { return player1SurvivingWorkers; }
    public int getAdaptiveSurvivingWorkers() { return adaptiveSurvivingWorkers; }
    public int getFixedSurvivingWorkers() { return fixedSurvivingWorkers; }
    public int getAdaptiveWorkerDeaths() {
        return FactoryFeudScenario.WORKERS_PER_TEAM - adaptiveSurvivingWorkers;
    }
    public int getFixedWorkerDeaths() {
        return FactoryFeudScenario.WORKERS_PER_TEAM - fixedSurvivingWorkers;
    }
    public String getExampleAttackOverride() { return exampleAttackOverride; }
    public String getExampleRetreatOverride() { return exampleRetreatOverride; }

    /** Compares deterministic outputs while intentionally excluding wall-clock runtime fields. */
    public boolean sameSimulationResult(FactoryFeudExperimentRecord other) {
        return other != null && matchId == other.matchId && map.equals(other.map)
                && seed == other.seed && adaptiveSide == other.adaptiveSide
                && player0Score == other.player0Score && player1Score == other.player1Score
                && adaptiveScore == other.adaptiveScore && fixedScore == other.fixedScore
                && outcome == other.outcome && matchCycles == other.matchCycles
                && player0Survivors == other.player0Survivors
                && player1Survivors == other.player1Survivors
                && adaptiveSurvivors == other.adaptiveSurvivors
                && fixedSurvivors == other.fixedSurvivors
                && roleReassignments == other.roleReassignments
                && illegalActionCount == other.illegalActionCount
                && configuredCycles == other.configuredCycles
                && completedCycles == other.completedCycles
                && Objects.equals(player0Delivery, other.player0Delivery)
                && Objects.equals(player1Delivery, other.player1Delivery)
                && Objects.equals(adaptiveStrategy, other.adaptiveStrategy)
                && Objects.equals(adaptiveCarrier, other.adaptiveCarrier)
                && Objects.equals(fixedCarrier, other.fixedCarrier)
                && Objects.equals(cargoSafeCarrier, other.cargoSafeCarrier)
                && tacticalOverrides == other.tacticalOverrides
                && tacticalAttackOverrides == other.tacticalAttackOverrides
                && tacticalRetreatOverrides == other.tacticalRetreatOverrides
                && player0SurvivingWorkers == other.player0SurvivingWorkers
                && player1SurvivingWorkers == other.player1SurvivingWorkers
                && adaptiveSurvivingWorkers == other.adaptiveSurvivingWorkers
                && fixedSurvivingWorkers == other.fixedSurvivingWorkers
                && Objects.equals(exampleAttackOverride, other.exampleAttackOverride)
                && Objects.equals(exampleRetreatOverride, other.exampleRetreatOverride);
    }

    /** Nullable timing fields use blank CSV cells when unavailable. */
    public static final class DeliveryMetrics {
        private final Integer firstCycle;
        private final Integer lastCycle;
        private final Double meanInterval;
        private final Integer completedEvents;

        public DeliveryMetrics(Integer firstCycle, Integer lastCycle,
                Double meanInterval, Integer completedEvents) {
            this.firstCycle = firstCycle;
            this.lastCycle = lastCycle;
            this.meanInterval = meanInterval;
            this.completedEvents = completedEvents;
        }

        static DeliveryMetrics unavailable() {
            return new DeliveryMetrics(null, null, null, null);
        }

        public Integer getFirstCycle() { return firstCycle; }
        public Integer getLastCycle() { return lastCycle; }
        public Double getMeanInterval() { return meanInterval; }
        public Integer getCompletedEvents() { return completedEvents; }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof DeliveryMetrics)) return false;
            DeliveryMetrics other = (DeliveryMetrics) object;
            return Objects.equals(firstCycle, other.firstCycle)
                    && Objects.equals(lastCycle, other.lastCycle)
                    && Objects.equals(meanInterval, other.meanInterval)
                    && Objects.equals(completedEvents, other.completedEvents);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstCycle, lastCycle, meanInterval, completedEvents);
        }
    }

    /** Role fields may be null when an agent does not expose role assignments. */
    public static final class CarrierMetrics {
        private final Integer roleInterruptions;
        private final Integer interruptedCycles;
        private final Integer attacksReceived;
        private final Integer deaths;
        private final Integer undeliveredCocoaAtEnd;
        private final Integer corrections;

        public CarrierMetrics(Integer roleInterruptions, Integer interruptedCycles,
                Integer attacksReceived, Integer deaths, Integer undeliveredCocoaAtEnd,
                Integer corrections) {
            this.roleInterruptions = roleInterruptions;
            this.interruptedCycles = interruptedCycles;
            this.attacksReceived = attacksReceived;
            this.deaths = deaths;
            this.undeliveredCocoaAtEnd = undeliveredCocoaAtEnd;
            this.corrections = corrections;
        }

        public Integer getRoleInterruptions() { return roleInterruptions; }
        public Integer getInterruptedCycles() { return interruptedCycles; }
        public Integer getAttacksReceived() { return attacksReceived; }
        public Integer getDeaths() { return deaths; }
        public Integer getUndeliveredCocoaAtEnd() { return undeliveredCocoaAtEnd; }
        public Integer getCorrections() { return corrections; }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CarrierMetrics)) return false;
            CarrierMetrics other = (CarrierMetrics) object;
            return Objects.equals(roleInterruptions, other.roleInterruptions)
                    && Objects.equals(interruptedCycles, other.interruptedCycles)
                    && Objects.equals(attacksReceived, other.attacksReceived)
                    && Objects.equals(deaths, other.deaths)
                    && Objects.equals(undeliveredCocoaAtEnd, other.undeliveredCocoaAtEnd)
                    && Objects.equals(corrections, other.corrections);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleInterruptions, interruptedCycles, attacksReceived,
                    deaths, undeliveredCocoaAtEnd, corrections);
        }
    }
}
