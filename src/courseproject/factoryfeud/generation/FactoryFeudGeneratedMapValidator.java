package courseproject.factoryfeud.generation;

import courseproject.factoryfeud.FactoryFeudScenario;
import rts.PhysicalGameState;
import rts.units.Unit;

/** D2-specific validation layered on the existing Factory Feud scenario checks. */
public final class FactoryFeudGeneratedMapValidator {
    public static final int REQUIRED_COCOA = 120;
    public static final int REQUIRED_UNIT_COUNT = 11;

    private FactoryFeudGeneratedMapValidator() {
    }

    public static void validate(PhysicalGameState pgs) {
        FactoryFeudScenario.validate(pgs, 1);
        if (pgs.getUnits().size() != REQUIRED_UNIT_COUNT) {
            throw new IllegalArgumentException("generated map must contain exactly "
                    + REQUIRED_UNIT_COUNT + " units");
        }

        int resources = 0;
        int cocoa = 0;
        int workers = 0;
        int factories = 0;
        for (Unit unit : pgs.getUnits()) {
            if (unit.getType().isResource) {
                resources++;
                cocoa += unit.getResources();
                if (unit.getPlayer() != -1) {
                    throw new IllegalArgumentException("shared cocoa resource must be neutral");
                }
            } else if ("Worker".equals(unit.getType().name)) {
                workers++;
                if (unit.getHitPoints() != 1) {
                    throw new IllegalArgumentException("generated Workers must start at 1 HP");
                }
            } else if (unit.getType().isStockpile) {
                factories++;
            } else {
                throw new IllegalArgumentException("unsupported generated unit type: "
                        + unit.getType().name);
            }
        }
        if (resources != 1 || cocoa != REQUIRED_COCOA) {
            throw new IllegalArgumentException(
                    "generated map must contain one neutral Resource with exactly 120 cocoa");
        }
        if (workers != 8 || factories != 2) {
            throw new IllegalArgumentException(
                    "generated map must contain eight Workers and two Bases");
        }

        FactoryFeudMapMetrics metrics = FactoryFeudMapMetrics.compute(pgs);
        if (metrics.getPlayer0FactoryToCocoaPath() < 0
                || metrics.getPlayer1FactoryToCocoaPath() < 0) {
            throw new IllegalArgumentException("both factories must have a terrain route to cocoa");
        }
    }
}
