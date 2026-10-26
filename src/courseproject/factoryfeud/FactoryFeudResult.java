package courseproject.factoryfeud;

/** Immutable result of a completed Factory Feud match. */
public final class FactoryFeudResult {
    public static final int TIE = -1;

    private final int cycles;
    private final int player0Score;
    private final int player1Score;
    private final int winner;

    public FactoryFeudResult(int cycles, int player0Score, int player1Score) {
        this.cycles = cycles;
        this.player0Score = player0Score;
        this.player1Score = player1Score;
        winner = player0Score == player1Score ? TIE : (player0Score > player1Score ? 0 : 1);
    }

    public int getCycles() {
        return cycles;
    }

    public int getScore(int player) {
        if (player == 0) return player0Score;
        if (player == 1) return player1Score;
        throw new IllegalArgumentException("Player must be 0 or 1");
    }

    public int getWinner() {
        return winner;
    }

    public boolean isTie() {
        return winner == TIE;
    }

    @Override
    public String toString() {
        return "FactoryFeudResult{cycles=" + cycles + ", score0=" + player0Score
                + ", score1=" + player1Score + ", winner=" + winner + "}";
    }
}
