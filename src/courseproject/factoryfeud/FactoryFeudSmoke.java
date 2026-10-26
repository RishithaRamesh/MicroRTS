package courseproject.factoryfeud;

import ai.abstraction.WorkerRush;
import rts.GameState;
import rts.units.UnitTypeTable;

/** Runs one headless Phase 1 smoke match using upstream WorkerRush controllers. */
public final class FactoryFeudSmoke {
    public static final int DEFAULT_TIME_LIMIT = 500;

    private FactoryFeudSmoke() {
    }

    public static void main(String[] args) throws Exception {
        int timeLimit = args.length == 0 ? DEFAULT_TIME_LIMIT : Integer.parseInt(args[0]);
        UnitTypeTable utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        GameState gameState = FactoryFeudScenario.load(utt);
        FactoryFeudMatch match = new FactoryFeudMatch(
                gameState, timeLimit, new WorkerRush(utt), new WorkerRush(utt));
        FactoryFeudResult result = match.run();
        System.out.println(result);
    }
}
