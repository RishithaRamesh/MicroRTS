package courseproject.factoryfeud;

import ai.abstraction.WorkerRush;
import ai.core.AI;
import gui.PhysicalGameStateJFrame;
import gui.PhysicalGameStatePanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import rts.GameState;
import rts.units.UnitTypeTable;

/** Swing visualization for the project-authored Factory Feud strategies. */
public final class FactoryFeudVisual {
    public static final int DEFAULT_TIME_LIMIT = 500;
    public static final int DEFAULT_CYCLE_DELAY_MS = 50;
    private static final int WINDOW_WIDTH = 980;
    private static final int WINDOW_HEIGHT = 760;
    private static final int HUD_HEIGHT = 58;
    private static final double BOARD_RESIZE_WEIGHT = 0.70;
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
        String mapLabel = config.getMapDisplayName(
                gameState.getPhysicalGameState().getWidth(),
                gameState.getPhysicalGameState().getHeight());
        System.out.println("Factory Feud visual map: " + config.getMapPath());
        System.out.println("Factory Feud visual dimensions: "
                + gameState.getPhysicalGameState().getWidth() + "x"
                + gameState.getPhysicalGameState().getHeight());

        int timeLimit = config.getTimeLimit();
        int cycleDelayMs = config.getCycleDelayMs();
        final OldAdaptiveRoleAI adaptive;
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
            boolean windowOpen = refreshWindow(window, currentMatch, visualModel);
            if (cycleDelayMs > 0 && windowOpen) {
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
            if (adaptive instanceof AdaptiveRoleAI) {
                AdaptiveRoleAI threatAware = (AdaptiveRoleAI) adaptive;
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
                    match.getGameState(), WINDOW_WIDTH, WINDOW_HEIGHT, false,
                    PhysicalGameStatePanel.COLORSCHEME_BLACK);
            frame.setTitle("Factory Feud - " + matchupLabel);
            HudParts hud = createHud(model.hudStatusText(match), matchupLabel);
            JTextArea rolePanel = createRolePanel(model.rolePanelText(match.getGameState()));
            JScrollPane roleScroller = createRoleScroller(rolePanel);
            frame.getContentPane().remove(frame.getPanel());
            JSplitPane mainSplit = createMainSplit(frame, roleScroller);
            frame.getContentPane().add(hud.panel, java.awt.BorderLayout.SOUTH);
            frame.getContentPane().add(mainSplit, java.awt.BorderLayout.CENTER);
            frame.getPanel().setMinimumSize(new Dimension(360, 360));
            frame.validate();
            mainSplit.setDividerLocation(BOARD_RESIZE_WEIGHT);
            frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            frame.setResizable(false);
            frame.repaint();
            windowHolder[0] = new WindowParts(frame, hud.status, rolePanel);
        });
        return windowHolder[0];
    }

    private static HudParts createHud(String statusText, String matchupText) {
        JPanel hud = new JPanel(new GridLayout(2, 1));
        hud.setOpaque(true);
        hud.setBackground(new Color(34, 39, 46));
        hud.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        Dimension fixedSize = new Dimension(0, HUD_HEIGHT);
        hud.setPreferredSize(fixedSize);
        hud.setMinimumSize(fixedSize);
        hud.setMaximumSize(new Dimension(Integer.MAX_VALUE, HUD_HEIGHT));

        JLabel status = createHudLine(statusText);
        JLabel matchup = createHudLine(matchupText);
        hud.add(status);
        hud.add(matchup);
        return new HudParts(hud, status);
    }

    private static JLabel createHudLine(String text) {
        JLabel line = new JLabel(text, SwingConstants.CENTER);
        line.setForeground(Color.WHITE);
        line.setFont(line.getFont().deriveFont(Font.BOLD, 15.0f));
        return line;
    }

    private static JTextArea createRolePanel(String text) {
        JTextArea roles = new JTextArea(text);
        roles.setEditable(false);
        roles.setFocusable(false);
        roles.setBackground(new Color(34, 39, 46));
        roles.setForeground(Color.WHITE);
        roles.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        roles.setLineWrap(true);
        roles.setWrapStyleWord(true);
        roles.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return roles;
    }

    private static JScrollPane createRoleScroller(JTextArea rolePanel) {
        JScrollPane scroller = new JScrollPane(rolePanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setPreferredSize(new Dimension(280, 680));
        scroller.setMinimumSize(new Dimension(230, 0));
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setBackground(rolePanel.getBackground());
        return scroller;
    }

    private static JSplitPane createMainSplit(
            PhysicalGameStateJFrame frame, JScrollPane roleScroller) {
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, frame.getPanel(), roleScroller);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        split.setResizeWeight(BOARD_RESIZE_WEIGHT);
        return split;
    }

    private static boolean refreshWindow(
            WindowParts window, FactoryFeudMatch match, FactoryFeudVisualModel model)
            throws Exception {
        final boolean[] displayable = new boolean[1];
        SwingUtilities.invokeAndWait(() -> {
            displayable[0] = window.frame.isDisplayable();
            if (!displayable[0]) return;
            window.frame.setStateCloning(match.getGameState());
            String hudText = model.hudStatusText(match);
            if (!hudText.equals(window.hud.getText())) window.hud.setText(hudText);
            String roleText = model.rolePanelText(match.getGameState());
            if (!roleText.equals(window.rolePanel.getText())) {
                window.rolePanel.setText(roleText);
                window.rolePanel.setCaretPosition(0);
            }
            window.frame.getPanel().repaint();
        });
        return displayable[0];
    }

    private static final class HudParts {
        private final JPanel panel;
        private final JLabel status;

        private HudParts(JPanel panel, JLabel status) {
            this.panel = panel;
            this.status = status;
        }
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
