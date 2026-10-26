package courseproject.factoryfeud;

import ai.core.AI;
import java.util.List;
import rts.GameState;
import rts.units.Unit;
import rts.units.UnitTypeTable;

/** Independent baseline: assigns roles once by stable Worker ID and never reallocates them. */
public final class FixedRoleAI extends FactoryFeudRoleAI {
    private static final Role[] INITIAL_ROLES = {
        Role.COLLECTOR, Role.ESCORT, Role.ATTACKER, Role.DEFENDER
    };

    public FixedRoleAI(UnitTypeTable utt) {
        super(utt);
    }

    @Override
    protected void initializeAssignments(List<Unit> workers, int player, GameState gs) {
        for (int index = 0; index < workers.size(); index++) {
            Role role = index < INITIAL_ROLES.length ? INITIAL_ROLES[index] : Role.ATTACKER;
            roleAssignments.put(workers.get(index).getID(), role);
        }
    }

    @Override
    protected void updateAssignments(List<Unit> workers, int player, GameState gs) {
        // Fixed baseline intentionally retains assignments, including entries for dead units.
    }

    @Override
    public AI clone() {
        return new FixedRoleAI(utt);
    }

    @Override
    public String toString() {
        return "FixedRoleAI";
    }
}
