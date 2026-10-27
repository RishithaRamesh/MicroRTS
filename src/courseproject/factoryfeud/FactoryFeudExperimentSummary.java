package courseproject.factoryfeud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Aggregate results for a completed Factory Feud experiment. */
public final class FactoryFeudExperimentSummary {
    private final List<FactoryFeudExperimentRecord> records;
    private final int adaptiveWins;
    private final int fixedWins;
    private final int draws;
    private final double meanAdaptiveCocoa;
    private final double meanFixedCocoa;
    private final double meanScoreDifferential;
    private final double meanAdaptiveCasualties;
    private final double meanFixedCasualties;
    private final int totalRoleReassignments;
    private final int player0Wins;
    private final int player1Wins;
    private final double meanPlayer0ScoreAdvantage;
    private final double meanRuntimeMillis;
    private final int totalTacticalOverrides;

    public FactoryFeudExperimentSummary(List<FactoryFeudExperimentRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("records cannot be empty");
        }
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
        int adaptiveWinCount = 0;
        int fixedWinCount = 0;
        int drawCount = 0;
        long adaptiveCocoa = 0;
        long fixedCocoa = 0;
        long adaptiveSurvivors = 0;
        long fixedSurvivors = 0;
        int roleReassignments = 0;
        int player0WinCount = 0;
        int player1WinCount = 0;
        long player0Score = 0;
        long player1Score = 0;
        long runtime = 0;
        int tacticalOverrides = 0;
        for (FactoryFeudExperimentRecord record : records) {
            if (record.getOutcome() == FactoryFeudExperimentRecord.Outcome.ADAPTIVE_WIN) {
                adaptiveWinCount++;
            } else if (record.getOutcome() == FactoryFeudExperimentRecord.Outcome.FIXED_WIN) {
                fixedWinCount++;
            } else {
                drawCount++;
            }
            adaptiveCocoa += record.getAdaptiveScore();
            fixedCocoa += record.getFixedScore();
            adaptiveSurvivors += record.getAdaptiveSurvivors();
            fixedSurvivors += record.getFixedSurvivors();
            roleReassignments += record.getRoleReassignments();
            player0Score += record.getPlayer0Score();
            player1Score += record.getPlayer1Score();
            if (record.getPlayer0Score() > record.getPlayer1Score()) player0WinCount++;
            else if (record.getPlayer1Score() > record.getPlayer0Score()) player1WinCount++;
            runtime += record.getRuntimeMillis();
            tacticalOverrides += record.getTacticalOverrides();
        }
        adaptiveWins = adaptiveWinCount;
        fixedWins = fixedWinCount;
        draws = drawCount;
        meanAdaptiveCocoa = adaptiveCocoa / (double) records.size();
        meanFixedCocoa = fixedCocoa / (double) records.size();
        meanScoreDifferential = (adaptiveCocoa - fixedCocoa) / (double) records.size();
        int initialUnits = FactoryFeudScenario.WORKERS_PER_TEAM
                + FactoryFeudScenario.FACTORIES_PER_TEAM;
        meanAdaptiveCasualties = initialUnits - adaptiveSurvivors / (double) records.size();
        meanFixedCasualties = initialUnits - fixedSurvivors / (double) records.size();
        totalRoleReassignments = roleReassignments;
        player0Wins = player0WinCount;
        player1Wins = player1WinCount;
        meanPlayer0ScoreAdvantage = (player0Score - player1Score) / (double) records.size();
        meanRuntimeMillis = runtime / (double) records.size();
        totalTacticalOverrides = tacticalOverrides;
    }

    public List<FactoryFeudExperimentRecord> getRecords() { return records; }
    public int getMatchCount() { return records.size(); }
    public int getAdaptiveWins() { return adaptiveWins; }
    public int getFixedWins() { return fixedWins; }
    public int getDraws() { return draws; }
    public double getAdaptiveWinRate() { return adaptiveWins / (double) records.size(); }
    public double getMeanAdaptiveCocoa() { return meanAdaptiveCocoa; }
    public double getMeanFixedCocoa() { return meanFixedCocoa; }
    public double getMeanScoreDifferential() { return meanScoreDifferential; }
    public double getMeanAdaptiveCasualties() { return meanAdaptiveCasualties; }
    public double getMeanFixedCasualties() { return meanFixedCasualties; }
    public int getTotalRoleReassignments() { return totalRoleReassignments; }
    public double getMeanRoleReassignments() {
        return totalRoleReassignments / (double) records.size();
    }
    public int getPlayer0Wins() { return player0Wins; }
    public int getPlayer1Wins() { return player1Wins; }
    public double getMeanPlayer0ScoreAdvantage() { return meanPlayer0ScoreAdvantage; }
    public double getMeanRuntimeMillis() { return meanRuntimeMillis; }
    public int getTotalTacticalOverrides() { return totalTacticalOverrides; }

    public String format() {
        return String.format(Locale.ROOT,
                "matches=%d adaptiveWins=%d draws=%d fixedWins=%d "
                + "adaptiveWinRate=%.4f meanAdaptiveCocoa=%.3f meanFixedCocoa=%.3f "
                + "meanScoreDifferential=%.3f meanAdaptiveCasualties=%.3f "
                + "meanFixedCasualties=%.3f totalRoleReassignments=%d "
                + "meanRoleReassignments=%.3f player0Wins=%d player1Wins=%d "
                + "meanPlayer0ScoreAdvantage=%.3f tacticalOverrides=%d meanRuntimeMillis=%.3f",
                getMatchCount(), adaptiveWins, draws, fixedWins, getAdaptiveWinRate(),
                meanAdaptiveCocoa, meanFixedCocoa, meanScoreDifferential,
                meanAdaptiveCasualties, meanFixedCasualties, totalRoleReassignments,
                getMeanRoleReassignments(), player0Wins, player1Wins,
                meanPlayer0ScoreAdvantage, totalTacticalOverrides, meanRuntimeMillis);
    }
}
