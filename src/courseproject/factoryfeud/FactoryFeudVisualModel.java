package courseproject.factoryfeud;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import rts.GameState;
import rts.units.Unit;

/** Headless-testable text model used by the Factory Feud Swing HUD and role panel. */
public final class FactoryFeudVisualModel {
    private static final int EVENT_LINES = 4;

    private final AdaptiveRoleAI adaptive;
    private final FixedRoleAI fixed;
    private final String matchupLabel;

    public FactoryFeudVisualModel(
            AdaptiveRoleAI adaptive, FixedRoleAI fixed, String matchupLabel) {
        this.adaptive = adaptive;
        this.fixed = fixed;
        this.matchupLabel = matchupLabel;
    }

    public String hudText(FactoryFeudMatch match) {
        String status = match.isFinished() ? winnerText(match) : "Match running";
        return "<html><div style='text-align:center'>Factory Feud | Cocoa delivered: P0 "
                + match.getScore(0) + " - P1 " + match.getScore(1)
                + " | Cycles remaining: " + match.getRemainingCycles()
                + " | " + status + "<br>" + matchupLabel + "</div></html>";
    }

    public String rolePanelText(GameState gameState) {
        if (adaptive == null || fixed == null) {
            return "ROLE VIEW\n\nUnavailable for placeholder agents.\n\nLegend\n"
                    + "C Collector\nE Escort\nA Attacker\nD Defender";
        }

        StringBuilder text = new StringBuilder("LIVE ROLES\n\nP0 AdaptiveRoleAI\n");
        appendSurvivingRoles(text, gameState, 0, adaptive.getRoleAssignments());
        text.append("\nP1 FixedRoleAI\n");
        appendSurvivingRoles(text, gameState, 1, fixed.getRoleAssignments());
        text.append("\nLegend\nC Collector  E Escort\nA Attacker   D Defender\n");
        text.append("\nAdaptive changes: ").append(adaptive.getRoleChangeCount()).append('\n');

        List<String> history = adaptive.getRoleChangeHistory();
        if (history.isEmpty()) {
            text.append("No reassignment events yet.");
        } else {
            text.append("Recent events:\n");
            int first = Math.max(0, history.size() - EVENT_LINES);
            for (int index = first; index < history.size(); index++) {
                text.append(compactEvent(history.get(index))).append('\n');
            }
        }
        return text.toString();
    }

    private static void appendSurvivingRoles(
            StringBuilder text, GameState gameState, int player, Map<Long, Role> assignments) {
        List<Unit> workers = new ArrayList<>();
        for (Unit unit : gameState.getUnits()) {
            if (unit.getPlayer() == player && unit.getType().canHarvest) workers.add(unit);
        }
        FactoryFeudRoleAI.sortById(workers);
        if (workers.isEmpty()) {
            text.append("  (no surviving Workers)\n");
            return;
        }
        for (Unit worker : workers) {
            Role role = assignments.get(worker.getID());
            text.append("  #").append(worker.getID()).append("  ")
                    .append(abbreviation(role)).append("  ")
                    .append(role == null ? "Unassigned" : title(role)).append('\n');
        }
    }

    private static String abbreviation(Role role) {
        if (role == null) return "?";
        return role.name().substring(0, 1);
    }

    private static String title(Role role) {
        String lower = role.name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String compactEvent(String event) {
        return event.replace(",unit=", "  #")
                .replace(",from=", " ")
                .replace(",to=", "->")
                .replace(",reason=", "\n    ");
    }

    private static String winnerText(FactoryFeudMatch match) {
        int player0 = match.getScore(0);
        int player1 = match.getScore(1);
        if (player0 == player1) return "Final: tie";
        return "Final: Player " + (player0 > player1 ? 0 : 1) + " wins";
    }
}
