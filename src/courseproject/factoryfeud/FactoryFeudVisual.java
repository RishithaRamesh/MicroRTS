package courseproject.factoryfeud;

import ai.abstraction.WorkerRush;
import ai.core.AI;
import gui.PhysicalGameStateJFrame;
import gui.PhysicalGameStatePanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import rts.GameState;
import rts.units.UnitTypeTable;

/** Swing visualization for the project-authored Factory Feud strategies. */
public final class FactoryFeudVisual {
    public static final int DEFAULT_TIME_LIMIT = 500;
    public static final int DEFAULT_CYCLE_DELAY_MS = 50;
    private static final String PLACEHOLDER_MATCHUP =
            "PLACEHOLDER: P0 WorkerRush vs P1 WorkerRush";

    private FactoryFeudVisual() {
    }

    public static void main(String[] args) throws Exception {
        FactoryFeudVisualConfig config = FactoryFeudVisualConfig.parse(args);

        UnitTypeTable utt = new UnitTypeTable(
                UnitTypeTable.VERSION_ORIGINAL_FINETUNED,
                UnitTypeTable.MOVE_CONFLICT_RESOLUTION_CANCEL_BOTH);
        GameState gameState = config.loadScenario(utt);
        String mapLabel = config.getMapName() + " ("
                + gameState.getPhysicalGameState().getWidth() + "x"
                + gameState.getPhysicalGameState().getHeight() + ")";
        System.out.println("Factory Feud visual map: " + config.getMapPath());
        System.out.println("Factory Feud visual dimensions: "
                + gameState.getPhysicalGameState().getWidth() + "x"
                + gameState.getPhysicalGameState().getHeight());

        int timeLimit = config.getTimeLimit();
        int cycleDelayMs = config.getCycleDelayMs();
        final AdaptiveRoleAI adaptive;
        final FixedRoleAI fixed;
        final AI player0;
        final AI player1;
        final String matchupLabel;
        if (config.isPlaceholderMode()) {
            adaptive = null;
            fixed = null;
            player0 = new WorkerRush(utt);
            player1 = new WorkerRush(utt);
            matchupLabel = PLACEHOLDER_MATCHUP + " | Map: " + mapLabel;
        } else {
            adaptive = config.getAdaptiveStrategy().create(utt, timeLimit);
            fixed = new FixedRoleAI(utt);
            player0 = fixed;
            player1 = adaptive;
            matchupLabel = "P0 (Blue/Left) FixedRoleAI vs P1 (Pink/Right) "
                    + adaptive + " | Map: " + mapLabel;
        }
        System.out.println("P0 (Blue/Left): " + player0);
        System.out.println("P1 (Pink/Right): " + player1);
        System.out.println("Factory Feud matchup: " + matchupLabel);

        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                    "Factory Feud visual mode requires a graphical desktop environment");
        }

        FactoryFeudMatch match = new FactoryFeudMatch(gameState, timeLimit, player0, player1);
        FactoryFeudVisualModel visualModel =
                new FactoryFeudVisualModel(adaptive, fixed, 1, matchupLabel);

        WindowParts window = createWindow(match, visualModel, matchupLabel);
        match.run(currentMatch -> {
            refreshWindow(window, currentMatch, visualModel);
            if (cycleDelayMs > 0 && window.frame.isDisplayable()) {
                Thread.sleep(cycleDelayMs);
            }
        });
        refreshWindow(window, match, visualModel);

        FactoryFeudResult result = match.getResult();
        System.out.println(matchupLabel);
        System.out.println(result);
        if (adaptive != null) {
            System.out.println("adaptiveRoleChanges=" + adaptive.getRoleChangeCount());
            for (String event : adaptive.getRoleChangeHistory()) {
                System.out.println("roleChange=" + event);
            }
            if (adaptive instanceof ThreatAwareAdaptiveRoleAI) {
                ThreatAwareAdaptiveRoleAI threatAware = (ThreatAwareAdaptiveRoleAI) adaptive;
                System.out.println("tacticalOverrides="
                        + threatAware.getTacticalOverrideCount());
                for (String event : threatAware.getTacticalOverrideHistory()) {
                    System.out.println("tacticalOverride=" + event);
                }
            }
        }
        System.out.println("Close the Factory Feud window to exit.");
    }

    private static WindowParts createWindow(
            FactoryFeudMatch match, FactoryFeudVisualModel model, String matchupLabel)
            throws Exception {
        final WindowParts[] windowHolder = new WindowParts[1];
        SwingUtilities.invokeAndWait(() -> {
            PhysicalGameStateJFrame frame = PhysicalGameStatePanel.newVisualizer(
                    match.getGameState(), 980, 760, false,
                    PhysicalGameStatePanel.COLORSCHEME_BLACK);
            frame.setTitle("Factory Feud - " + matchupLabel);
            JLabel hud = createHud(model.hudText(match));
            JTextArea rolePanel = createRolePanel(model.rolePanelText(match.getGameState()));
            frame.getContentPane().add(hud, java.awt.BorderLayout.SOUTH);
            frame.getContentPane().add(rolePanel, java.awt.BorderLayout.EAST);
            frame.validate();
            windowHolder[0] = new WindowParts(frame, hud, rolePanel);
        });
        return windowHolder[0];
    }

    private static JLabel createHud(String text) {
        JLabel hud = new JLabel(text, SwingConstants.CENTER);
        hud.setOpaque(true);
        hud.setBackground(new Color(34, 39, 46));
        hud.setForeground(Color.WHITE);
        hud.setFont(hud.getFont().deriveFont(Font.BOLD, 15.0f));
        hud.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        return hud;
    }

    private static JTextArea createRolePanel(String text) {
        JTextArea roles = new JTextArea(text);
        roles.setEditable(false);
        roles.setFocusable(false);
        roles.setBackground(new Color(34, 39, 46));
        roles.setForeground(Color.WHITE);
        roles.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        roles.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        roles.setPreferredSize(new Dimension(280, 680));
        return roles;
    }

    private static void refreshWindow(
            WindowParts window, FactoryFeudMatch match, FactoryFeudVisualModel model)
            throws Exception {
        if (!window.frame.isDisplayable()) return;
        SwingUtilities.invokeAndWait(() -> {
            window.frame.setStateCloning(match.getGameState());
            window.hud.setText(model.hudText(match));
            window.rolePanel.setText(model.rolePanelText(match.getGameState()));
            window.rolePanel.setCaretPosition(0);
            window.frame.repaint();
        });
    }

    private static final class WindowParts {
        private final PhysicalGameStateJFrame frame;
        private final JLabel hud;
        private final JTextArea rolePanel;

        private WindowParts(
                PhysicalGameStateJFrame frame, JLabel hud, JTextArea rolePanel) {
            this.frame = frame;
            this.hud = hud;
            this.rolePanel = rolePanel;
        }
    }
}
