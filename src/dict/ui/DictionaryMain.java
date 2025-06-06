package dict.ui;

import dict.net.DictConnectionException;
import dict.model.Database;
import dict.model.MatchingStrategy;
import dict.net.DictionaryConnection;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class DictionaryMain extends JFrame {

    private DictionaryConnection connection;
    private String serverName = "dict.org";

    private final DefaultComboBoxModel<Database> databaseModel;
    private final DefaultComboBoxModel<MatchingStrategy> strategyModel;
    private final DefinitionTableModel definitionModel;

    private final WordSearchField wordSearchField;
    private final JTable definitionTable;
    private final JEditorPane databaseDescription;
    private final JLabel statusLabel;
    private final JButton connectButton;
    private final JProgressBar progressBar;

    // Enhanced modern color scheme with gradients and shadows
    private static final Color PRIMARY_COLOR = new Color(30, 136, 229);
    private static final Color PRIMARY_DARK = new Color(25, 118, 210);
    private static final Color SECONDARY_COLOR = new Color(248, 249, 250);
    private static final Color ACCENT_COLOR = new Color(255, 111, 97);
    private static final Color ACCENT_HOVER = new Color(255, 87, 34);
    private static final Color TEXT_COLOR = new Color(33, 37, 41);
    private static final Color BORDER_COLOR = new Color(206, 212, 218);
    private static final Color CARD_SHADOW = new Color(0, 0, 0, 25);

    DictionaryMain() {
        super("Dictionary Explorer");

        // Initialize models and components in constructor
        databaseModel = new DefaultComboBoxModel<>();
        strategyModel = new DefaultComboBoxModel<>();
        definitionModel = new DefinitionTableModel();
        wordSearchField = new WordSearchField(this);
        definitionTable = new JTable(definitionModel);
        databaseDescription = new JEditorPane();
        statusLabel = new JLabel("Ready to connect");
        connectButton = new JButton("Connect to Server");
        progressBar = new JProgressBar();

        initializeUI();
        setupModernStyling();
        setupEventHandlers();
    }

    private void initializeUI() {
        this.setSize(1200, 720);
        this.setMinimumSize(new Dimension(900, 650));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        // Create main layout
        createHeaderPanel();
        createCenterPanel();
        createSidePanel();
        createStatusBar();
    }

    private void createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        // Add subtle gradient effect
        headerPanel = new GradientPanel(PRIMARY_COLOR, PRIMARY_DARK);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(18, 25, 18, 25));

        // Enhanced title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Dictionary Explorer");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setIcon(createIcon("📖"));

        titlePanel.add(titleLabel);

        // Enhanced search panel
        JPanel searchPanel = createSearchPanel();

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.CENTER);

        this.add(headerPanel, BorderLayout.NORTH);
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(12, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(new EmptyBorder(0, 40, 0, 0));

        styleComboBox(wordSearchField);
        wordSearchField.setPreferredSize(new Dimension(320, 40));

        JButton searchButton = new JButton("Search");
        styleButton(searchButton, true);
        searchButton.setPreferredSize(new Dimension(110, 40));
        searchButton.addActionListener(e -> showDefinitions());

        searchPanel.add(wordSearchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        this.getRootPane().setDefaultButton(searchButton);

        return searchPanel;
    }

    private void createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(25, 25, 5, 25));
        centerPanel.setBackground(Color.WHITE);

        // Enhanced results header with icons
        JPanel resultsHeader = new JPanel(new BorderLayout());
        resultsHeader.setBorder(new EmptyBorder(0, 0, 15, 0));
        resultsHeader.setOpaque(false);

        JPanel resultsLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resultsLabelPanel.setOpaque(false);

        JLabel resultsLabel = new JLabel("Definitions");
        resultsLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        resultsLabel.setForeground(TEXT_COLOR);
        resultsLabel.setIcon(createColoredIcon("📋", TEXT_COLOR));

        resultsLabelPanel.add(resultsLabel);

        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        styleProgressBar(progressBar);

        resultsHeader.add(resultsLabelPanel, BorderLayout.WEST);
        resultsHeader.add(progressBar, BorderLayout.EAST);

        // Enhanced definition table with shadow
        setupDefinitionTable();

        JScrollPane scrollPane = new JScrollPane(definitionTable);
        styleScrollPane(scrollPane);

        centerPanel.add(resultsHeader, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        this.add(centerPanel, BorderLayout.CENTER);
    }

    private void createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(new EmptyBorder(25, 5, 25, 25));
        sidePanel.setPreferredSize(new Dimension(300, 0));
        sidePanel.setBackground(SECONDARY_COLOR);

        // Enhanced panels with cards
        JPanel connectionPanel = createConnectionPanel();
        JPanel optionsPanel = createOptionsPanel();
        JPanel infoPanel = createInfoPanel();

        sidePanel.add(connectionPanel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(optionsPanel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(infoPanel);
        sidePanel.add(Box.createVerticalGlue());

        this.add(sidePanel, BorderLayout.EAST);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = createEnhancedStyledPanel("🔗 Connection", PRIMARY_COLOR);

        styleButton(connectButton, false);
        connectButton.setPreferredSize(new Dimension(220, 36));

        connectButton.addActionListener(e -> establishConnection());

        panel.add(connectButton);
        return panel;
    }

    private JPanel createOptionsPanel() {
        JPanel panel = createEnhancedStyledPanel("⚙️ Options", new Color(76, 175, 80));

        // Database selection
        JLabel dbLabel = new JLabel("Database:");
        dbLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbLabel.setForeground(TEXT_COLOR);

        JComboBox<Database> databaseSelection = new JComboBox<>(databaseModel);
        styleComboBox(databaseSelection);
        databaseSelection.setPreferredSize(new Dimension(220, 32));
        databaseSelection.addActionListener(this::onDatabaseSelectionChanged);

        // Strategy selection
        JLabel strategyLabel = new JLabel("Search Strategy:");
        strategyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        strategyLabel.setForeground(TEXT_COLOR);

        JComboBox<MatchingStrategy> strategySelection = new JComboBox<>(strategyModel);
        styleComboBox(strategySelection);
        strategySelection.setPreferredSize(new Dimension(220, 32));

        panel.add(dbLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(databaseSelection);
        panel.add(Box.createVerticalStrut(12));
        panel.add(strategyLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(strategySelection);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = createEnhancedStyledPanel("ℹ️ Database Info", new Color(156, 39, 176));

        databaseDescription.setContentType("text/html");
        databaseDescription.setText("<html><body style='font-family: Segoe UI; font-size: 12px; color: #6c757d; line-height: 1.4;'>Select a database to view detailed information</body></html>");
        databaseDescription.setEditable(false);
        databaseDescription.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(databaseDescription);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(250, 130));
        styleScrollPane(scrollPane);

        panel.add(scrollPane);
        return panel;
    }

    private void createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(10, 25, 10, 25)
        ));
        statusPanel.setBackground(new Color(252, 252, 253));

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(108, 117, 125));
        statusLabel.setIcon(createColoredIcon("●", new Color(40, 167, 69)));

        statusPanel.add(statusLabel, BorderLayout.WEST);
        this.add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createEnhancedStyledPanel(String title, Color accentColor) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Create custom border with accent color
        Border colorBorder = new CompoundBorder(
                new LineBorder(accentColor, 2, true),
                new EmptyBorder(1, 1, 1, 1)
        );

        Border titledBorder = new TitledBorder(
                colorBorder,
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                TEXT_COLOR
        );

        panel.setBorder(new CompoundBorder(
                titledBorder,
                new EmptyBorder(12, 12, 12, 12)
        ));

        panel.setBackground(Color.WHITE);
        return panel;
    }

    private void setupDefinitionTable() {
        definitionTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        definitionTable.setRowHeight(28);
        definitionTable.setShowGrid(false);
        definitionTable.setIntercellSpacing(new Dimension(0, 2));
        definitionTable.setSelectionBackground(new Color(227, 242, 253));
        definitionTable.setSelectionForeground(TEXT_COLOR);

        // Enhanced cell renderer
        definitionTable.getColumnModel().getColumn(2).setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
            JTextArea area = new JTextArea(value.toString());
            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            area.setBorder(new EmptyBorder(10, 10, 10, 10));

            if (isSelected) {
                area.setBackground(table.getSelectionBackground());
                area.setForeground(table.getSelectionForeground());
            } else {
                area.setBackground(table.getBackground());
                area.setForeground(table.getForeground());
            }

            return area;
        });

        // Adjusted column widths
        definitionTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        definitionTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        definitionTable.getColumnModel().getColumn(2).setPreferredWidth(520);

        // Enhanced header styling
        definitionTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        definitionTable.getTableHeader().setBackground(new Color(248, 249, 250));
        definitionTable.getTableHeader().setForeground(TEXT_COLOR);
        definitionTable.getTableHeader().setBorder(new MatteBorder(0, 0, 2, 0, new Color(206, 212, 218)));
        definitionTable.getTableHeader().setPreferredSize(new Dimension(0, 35));
    }

    private void setupModernStyling() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Enhanced global font settings
        Font segoeUI = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("defaultFont", segoeUI);
    }

    private void styleButton(JButton button, boolean isPrimary) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (isPrimary) {
            button.setBackground(ACCENT_COLOR);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(Color.WHITE);
            button.setForeground(PRIMARY_COLOR);
            button.setBorder(new CompoundBorder(
                    new LineBorder(PRIMARY_COLOR, 2, true),
                    new EmptyBorder(8, 16, 8, 16)
            ));
        }

        // Enhanced hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalBg = button.getBackground();

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(isPrimary ? ACCENT_HOVER : new Color(240, 248, 255));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalBg);
            }
        });
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboBox.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        comboBox.setBackground(Color.WHITE);
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        scrollPane.getVerticalScrollBar().setUI(new EnhancedScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new EnhancedScrollBarUI());
    }

    private void styleProgressBar(JProgressBar progressBar) {
        progressBar.setPreferredSize(new Dimension(120, 6));
        progressBar.setStringPainted(false);
        progressBar.setBackground(new Color(233, 233, 233));
        progressBar.setForeground(PRIMARY_COLOR);
        progressBar.setBorder(new LineBorder(new Color(200, 200, 200), 1, true));
    }

    private Icon createIcon(String emoji) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
                g.setColor(Color.WHITE);
                g.drawString(emoji, x, y + 16);
            }

            @Override
            public int getIconWidth() { return 28; }

            @Override
            public int getIconHeight() { return 22; }
        };
    }

    private Icon createColoredIcon(String symbol, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g.setColor(color);
                g.drawString(symbol, x, y + 10);
            }

            @Override
            public int getIconWidth() { return 16; }

            @Override
            public int getIconHeight() { return 14; }
        };
    }

    // Gradient panel for header
    private class GradientPanel extends JPanel {
        private Color color1, color2;

        public GradientPanel(Color color1, Color color2) {
            this.color1 = color1;
            this.color2 = color2;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void setupEventHandlers() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connection != null) {
                    connection.close();
                }
            }
        });
    }

    private void onDatabaseSelectionChanged(ActionEvent e) {
        Database d = (Database) databaseModel.getSelectedItem();
        if (d != null && connection != null) {
            try {
                String info = "<html><body style='font-family: Segoe UI; font-size: 12px; color: #495057; line-height: 1.5;'>" +
                        "<b style='color: #343a40;'>" + d.getDescription() + "</b><br><br>" +
                        connection.getDatabaseInfo(d).replace("\n", "<br>") +
                        "</body></html>";
                databaseDescription.setText(info);
            } catch (DictConnectionException ex) {
                databaseDescription.setText(
                        "<html><body style='font-family: Segoe UI; font-size: 12px; color: #6c757d;'>" +
                                "<b style='color: #343a40;'>" + d.getDescription() + "</b><br><br>" +
                                "<span style='color: #dc3545;'>⚠️ Error retrieving database information</span>" +
                                "</body></html>"
                );
            }
        }
    }

    public void handleException(Throwable ex) {
        setStatus("Connection error occurred", new Color(220, 53, 69));
        JOptionPane.showMessageDialog(this,
                "Connection error:\n" + ex.toString(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
        establishConnection();
    }

    public void showDefinitions() {
        progressBar.setVisible(true);
        setStatus("Searching definitions...", new Color(255, 193, 7));

        new SwingWorker<Void, Void>() {
            private final String word = Objects.requireNonNullElse(wordSearchField.getSelectedItem(), "").toString();

            @Override
            protected Void doInBackground() throws Exception {
                definitionModel.populateDefinitions(connection.getDefinitions(word,
                        (Database) databaseModel.getSelectedItem()));
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                try {
                    get();
                    // Auto-resize rows based on content
                    for (int i = 0; i < definitionModel.getRowCount(); i++) {
                        Component c = definitionTable.prepareRenderer(definitionTable.getCellRenderer(i, 2), i, 2);
                        definitionTable.setRowHeight(i, Math.max((int) c.getPreferredSize().getHeight(), 28));
                    }
                    setStatus("Found " + definitionModel.getRowCount() + " definitions", new Color(40, 167, 69));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    handleException(e.getCause());
                }
            }
        }.execute();
    }

    public void establishConnection() {
        if (connection != null) {
            connection.close();
        }

        setStatus("Connecting...", new Color(255, 193, 7));
        definitionModel.populateDefinitions(Collections.emptyList());
        databaseModel.removeAllElements();
        databaseModel.addElement(new Database("*", "All databases"));
        databaseModel.addElement(new Database("!", "Any database"));
        strategyModel.removeAllElements();
        wordSearchField.reset();

        try {
            serverName = JOptionPane.showInputDialog(this,
                    "Enter dictionary server address:",
                    "Dictionary Server",
                    JOptionPane.QUESTION_MESSAGE,
                    null, null, serverName).toString();

            if (serverName == null) {
                System.exit(0);
            }

            if (serverName.contains(":")) {
                String[] serverData = serverName.split(":", 2);
                connection = new DictionaryConnection(serverData[0], Integer.parseInt(serverData[1]));
            } else {
                connection = new DictionaryConnection(serverName);
            }

            for (Database db : connection.getDatabaseList().values()) {
                databaseModel.addElement(db);
            }

            for (MatchingStrategy strategy : connection.getStrategyList()) {
                strategyModel.addElement(strategy);
                if (strategy.getName().equals("prefix")) {
                    strategyModel.setSelectedItem(strategy);
                }
            }

            setStatus("Connected to " + serverName, new Color(40, 167, 69));
            connectButton.setText("Reconnect");
        } catch (DictConnectionException ex) {
            handleException(ex);
        }

        wordSearchField.grabFocus();
    }

    public Collection<String> getMatchList(String word) throws DictConnectionException {
        return connection.getMatchList(word,
                (MatchingStrategy) strategyModel.getSelectedItem(),
                (Database) databaseModel.getSelectedItem());
    }

    private void setStatus(String message) {
        setStatus(message, new Color(108, 117, 125));
    }

    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    // Enhanced scroll bar UI
    private static class EnhancedScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(180, 180, 180);
            this.trackColor = new Color(248, 249, 250);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                    thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DictionaryMain main = new DictionaryMain();
            main.setVisible(true);
            main.establishConnection();
        });
    }
}