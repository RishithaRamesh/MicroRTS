package courseproject.factoryfeud;

import ai.core.AI;
import rts.units.UnitTypeTable;

/**
 * Experimental AdaptiveRoleAI variant that keeps cocoa carriers in the
 * Collector role until they return their cargo. The control agent is unchanged.
 */
public final class CargoSafeAdaptiveRoleAI extends AdaptiveRoleAI {
    public CargoSafeAdaptiveRoleAI(UnitTypeTable utt) {
        super(utt);
    }

    public CargoSafeAdaptiveRoleAI(UnitTypeTable utt, int matchDuration) {
        super(utt, matchDuration);
    }

    @Override
    protected boolean preserveCarryingCollectors() {
        return true;
    }

    @Override
    public AI clone() {
        return new CargoSafeAdaptiveRoleAI(utt, getMatchDuration());
    }

    @Override
    public String toString() {
        return "CargoSafeAdaptiveRoleAI (experimental)";
    }
}
