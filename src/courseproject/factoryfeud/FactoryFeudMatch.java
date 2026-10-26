package courseproject.factoryfeud;

import ai.core.AI;
import rts.GameState;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import util.Pair;

/**
 * Fixed-duration Factory Feud controller.
 *
 * The controller deliberately ignores microRTS elimination as an end condition.
 * It removes production commands before issuing actions and treats positive
 * player-resource deltas as delivered cocoa.
 */
public final class FactoryFeudMatch {
    /** Receives a notification after each completed cycle for rendering or recording. */
    public interface CycleObserver {
        void afterCycle(FactoryFeudMatch match) throws Exception;
    }

    private final GameState gameState;
    private final int timeLimit;
    private final AI[] agents;
    private final int[] deliveredCocoa = new int[2];
    private final int[] lastResourceBalance = new int[2];
    private int illegalActionCount;
    private int filteredProductionActionCount;

    public FactoryFeudMatch(GameState gameState, int timeLimit) {
        this(gameState, timeLimit, null, null);
    }

    public FactoryFeudMatch(GameState gameState, int timeLimit, AI player0, AI player1) {
        if (gameState == null) throw new IllegalArgumentException("gameState cannot be null");
        if (timeLimit <= gameState.getTime()) {
            throw new IllegalArgumentException("timeLimit must be greater than the current game time");
        }
        this.gameState = gameState;
        this.timeLimit = timeLimit;
        agents = new AI[]{player0, player1};
        lastResourceBalance[0] = gameState.getPlayer(0).getResources();
        lastResourceBalance[1] = gameState.getPlayer(1).getResources();
    }

    public FactoryFeudResult run() throws Exception {
        return run(null);
    }

    /**
     * Runs the match through this controller's normal scoring and action-filtering
     * path, optionally notifying an observer after each completed cycle.
     */
    public FactoryFeudResult run(CycleObserver observer) throws Exception {
        if (agents[0] == null || agents[1] == null) {
            throw new IllegalStateException("run() requires two AI controllers");
        }
        agents[0].reset();
        agents[1].reset();
        while (!isFinished()) {
            PlayerAction player0Action = agents[0].getAction(0, gameState);
            PlayerAction player1Action = agents[1].getAction(1, gameState);
            step(player0Action, player1Action);
            if (observer != null) observer.afterCycle(this);
        }
        FactoryFeudResult result = getResult();
        agents[0].gameOver(result.getWinner());
        agents[1].gameOver(result.getWinner());
        return result;
    }

    public void step(PlayerAction player0Action, PlayerAction player1Action) {
        if (isFinished()) throw new IllegalStateException("Factory Feud match has reached its time limit");

        PlayerAction allowedPlayer0 = withoutProduction(player0Action, 0);
        PlayerAction allowedPlayer1 = withoutProduction(player1Action, 1);
        illegalActionCount += countIllegalActions(allowedPlayer0);
        illegalActionCount += countIllegalActions(allowedPlayer1);
        gameState.issueSafe(allowedPlayer0);
        gameState.issueSafe(allowedPlayer1);
        gameState.cycle();
        updateDeliveryScores();
    }

    private PlayerAction withoutProduction(PlayerAction proposed, int player) {
        PlayerAction allowed = new PlayerAction();
        if (proposed == null) return allowed;

        for (Pair<Unit, UnitAction> pair : proposed.getActions()) {
            if (pair.m_a != null && pair.m_a.getPlayer() == player
                    && pair.m_b != null && pair.m_b.getType() != UnitAction.TYPE_PRODUCE) {
                allowed.addUnitAction(pair.m_a, pair.m_b);
            } else if (pair.m_b != null && pair.m_b.getType() == UnitAction.TYPE_PRODUCE) {
                filteredProductionActionCount++;
            }
        }
        return allowed;
    }

    private int countIllegalActions(PlayerAction action) {
        int count = 0;
        for (Pair<Unit, UnitAction> pair : action.getActions()) {
            if (pair.m_a != null && pair.m_b != null
                    && pair.m_b.getType() != UnitAction.TYPE_NONE
                    && !pair.m_a.canExecuteAction(pair.m_b, gameState)) count++;
        }
        return count;
    }

    private void updateDeliveryScores() {
        for (int player = 0; player < 2; player++) {
            int currentBalance = gameState.getPlayer(player).getResources();
            int delta = currentBalance - lastResourceBalance[player];
            if (delta < 0) {
                throw new IllegalStateException("Player resources decreased; spending is prohibited in Factory Feud");
            }
            deliveredCocoa[player] += delta;
            lastResourceBalance[player] = currentBalance;
        }
    }

    public boolean isFinished() {
        return gameState.getTime() >= timeLimit;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getRemainingCycles() {
        return Math.max(0, timeLimit - gameState.getTime());
    }

    public int getScore(int player) {
        if (player < 0 || player > 1) throw new IllegalArgumentException("Player must be 0 or 1");
        return deliveredCocoa[player];
    }

    public int getIllegalActionCount() {
        return illegalActionCount;
    }

    public int getFilteredProductionActionCount() {
        return filteredProductionActionCount;
    }

    public GameState getGameState() {
        return gameState;
    }

    public FactoryFeudResult getResult() {
        if (!isFinished()) throw new IllegalStateException("Match has not reached its time limit");
        return new FactoryFeudResult(gameState.getTime(), deliveredCocoa[0], deliveredCocoa[1]);
    }
}
