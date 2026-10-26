package courseproject.factoryfeud;

/** Immutable data for one side-specific match in a paired experiment. */
public final class FactoryFeudExperimentRecord {
    public enum Outcome {
        ADAPTIVE_WIN,
        FIXED_WIN,
        DRAW
    }

    public static final String CSV_HEADER = "match_id,map,seed,adaptive_side,"
            + "player0_score,player1_score,adaptive_score,fixed_score,outcome,match_cycles,"
            + "player0_survivors,player1_survivors,adaptive_survivors,fixed_survivors,"
            + "role_reassignments,illegal_action_count,runtime_ms";

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

    public FactoryFeudExperimentRecord(int matchId, String map, long seed, int adaptiveSide,
            int player0Score, int player1Score, int adaptiveScore, int fixedScore,
            Outcome outcome, int matchCycles, int player0Survivors, int player1Survivors,
            int adaptiveSurvivors, int fixedSurvivors, int roleReassignments,
            int illegalActionCount, long runtimeMillis) {
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
    }

    public String toCsvRow() {
        return matchId + "," + csv(map) + "," + seed + "," + adaptiveSide + ","
                + player0Score + "," + player1Score + "," + adaptiveScore + ","
                + fixedScore + "," + outcome + "," + matchCycles + ","
                + player0Survivors + "," + player1Survivors + ","
                + adaptiveSurvivors + "," + fixedSurvivors + ","
                + roleReassignments + "," + illegalActionCount + "," + runtimeMillis;
    }

    private static String csv(String value) {
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

    /** Compares deterministic match outputs while intentionally excluding wall-clock runtime. */
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
                && illegalActionCount == other.illegalActionCount;
    }
}
