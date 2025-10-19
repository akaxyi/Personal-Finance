package com.jetbrains.ui;

import com.jetbrains.finance.model.TransactionType;
import com.jetbrains.finance.service.FinanceService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.YearMonth;

public class AppFrame extends JFrame {
    private final FinanceService service;
    private YearMonth currentMonth;

    private final JLabel monthLabel = new JLabel();
    private final SummaryPanel summaryPanel;
    private final TransactionsPanel transactionsPanel;
    private final BudgetsPanel budgetsPanel;
    private final JLabel statusLabel = new JLabel("Ready");
    private final AnalyticsPanel analyticsPanel;

    private JTabbedPane tabs;
    private JPanel topBar;
    private JPanel status;
    private JLabel pathLbl;

    public AppFrame(FinanceService service, YearMonth startMonth) {
        super("Personal Finance");
        this.service = service;
        this.currentMonth = startMonth;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        setJMenuBar(buildMenu());

        // Tabs (init panels early so listeners can reference them)
        tabs = new JTabbedPane();
        summaryPanel = new SummaryPanel(service, () -> currentMonth);
        transactionsPanel = new TransactionsPanel(service, () -> currentMonth, this::doSave, this::refreshAll);
        budgetsPanel = new BudgetsPanel(service, this::doSave, this::refreshAll);
        analyticsPanel = new AnalyticsPanel(service, () -> currentMonth, ym -> { this.currentMonth = ym; refreshAll(); });
        tabs.addTab("Summary", summaryPanel);
        tabs.addTab("Transactions", transactionsPanel);
        tabs.addTab("Budgets", budgetsPanel);
        tabs.addTab("Analytics", analyticsPanel);
        // glossy, compact tab style
        UIUtils.styleTabbedPane(tabs);
        // Optional tab icons if they exist (scaled)
        ImageIcon t1 = UIUtils.loadIconScaled("icons/summary.png", 18); if (t1 != null) tabs.setIconAt(0, t1);
        ImageIcon t2 = UIUtils.loadIconScaled("icons/transactions.png", 18); if (t2 != null) tabs.setIconAt(1, t2);
        ImageIcon t3 = UIUtils.loadIconScaled("icons/budgets.png", 18); if (t3 != null) tabs.setIconAt(2, t3);
        ImageIcon t4 = UIUtils.loadIconScaled("icons/analytics.png", 18); if (t4 != null) tabs.setIconAt(3, t4);

        // Top bar
        topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setOpaque(true);
        topBar.setBackground(UIUtils.topBarBackground());

        JButton prevBtn = new JButton("Prev"); prevBtn.setToolTipText("Go to previous month"); UIUtils.styleButton(prevBtn);
        JButton nextBtn = new JButton("Next"); nextBtn.setToolTipText("Go to next month"); UIUtils.styleButton(nextBtn);
        JButton selectBtn = new JButton("Select Month..."); selectBtn.setToolTipText("Jump to a specific month (YYYY-MM)"); UIUtils.styleButton(selectBtn);
        JButton exportBtn = new JButton("Export CSV"); exportBtn.setToolTipText("Export transactions for the current month to CSV"); UIUtils.styleButton(exportBtn);
        JButton addIncomeBtn = new JButton("Add Income"); addIncomeBtn.setToolTipText("Record an income for this month"); UIUtils.styleButton(addIncomeBtn);
        JButton addExpenseBtn = new JButton("Add Expense"); addExpenseBtn.setToolTipText("Record an expense for this month"); UIUtils.styleButton(addExpenseBtn);
        JButton saveBtn = new JButton("Save"); saveBtn.setToolTipText("Save data to " + service.getDataFile().toAbsolutePath()); UIUtils.styleButton(saveBtn);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 14f));

        // Toolbar icons from resources/icons if present (scaled to 18px)
        ImageIcon ic;
        ic = UIUtils.loadIconScaled("icons/prev.png", 18); if (ic != null) prevBtn.setIcon(ic);
        ic = UIUtils.loadIconScaled("icons/next.png", 18); if (ic != null) nextBtn.setIcon(ic);
        ic = UIUtils.loadIconScaled("icons/select.png", 18); if (ic != null) selectBtn.setIcon(ic);
        ic = UIUtils.loadIconScaled("icons/export.png", 18); if (ic != null) exportBtn.setIcon(ic);
        ic = UIUtils.loadIconScaled("icons/income.png", 18); if (ic != null) addIncomeBtn.setIcon(ic);
        ic = UIUtils.loadIconScaled("icons/expense.png", 18); if (ic != null) addExpenseBtn.setIcon(ic);
        ic = UIUtils.loadIconScaled("icons/save.png", 18); if (ic != null) saveBtn.setIcon(ic);
        prevBtn.setIconTextGap(6); nextBtn.setIconTextGap(6);

        topBar.add(prevBtn); topBar.add(monthLabel); topBar.add(nextBtn); topBar.add(selectBtn);
        topBar.add(Box.createHorizontalStrut(20));
        topBar.add(addExpenseBtn); topBar.add(addIncomeBtn);
        topBar.add(Box.createHorizontalStrut(20));
        topBar.add(exportBtn); topBar.add(saveBtn);

        prevBtn.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); refreshAll(); });
        nextBtn.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); refreshAll(); });
        selectBtn.addActionListener(this::onSelectMonth);
        exportBtn.addActionListener(this::onExportCsv);
        addIncomeBtn.addActionListener(e -> transactionsPanel.addTransactionDialog(TransactionType.INCOME));
        addExpenseBtn.addActionListener(e -> transactionsPanel.addTransactionDialog(TransactionType.EXPENSE));
        saveBtn.addActionListener(e -> doSave());

        // Status bar
        JPanel status = new JPanel(new BorderLayout());
        this.status = status;
        status.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        statusLabel.setForeground(UIUtils.labelFg());
        status.add(statusLabel, BorderLayout.WEST);
        JLabel pathLbl = new JLabel("Data: " + service.getDataFile().toAbsolutePath());
        this.pathLbl = pathLbl;
        pathLbl.setToolTipText("Data file path");
        pathLbl.setForeground(UIUtils.labelFg());
        status.add(pathLbl, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.SOUTH);

        refreshAll();
    }

    private JMenuBar buildMenu() {
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem miSave = new JMenuItem("Save"); miSave.addActionListener(e -> doSave());
        JMenuItem miExport = new JMenuItem("Export CSV..."); miExport.addActionListener(this::onExportCsv);
        JMenuItem miOpenFolder = new JMenuItem("Open Data Folder"); miOpenFolder.addActionListener(e -> openDataFolder());
        JMenuItem miExit = new JMenuItem("Exit"); miExit.addActionListener(e -> dispose());
        file.add(miSave); file.add(miExport); file.addSeparator(); file.add(miOpenFolder); file.addSeparator(); file.add(miExit);

        JMenu view = new JMenu("View");
        JMenuItem miResetTables = new JMenuItem("Reset Table Layouts");
        miResetTables.addActionListener(e -> {
            UIUtils.clearTableState("transactions");
            UIUtils.clearTableState("budgets");
            UIUtils.clearTableState("summary");
            refreshAll();
            JOptionPane.showMessageDialog(this, "Table layouts reset. Column widths and sorting will revert to defaults.");
        });
        view.add(miResetTables);
        view.addSeparator();
        JMenu theme = new JMenu("Theme");
        ButtonGroup grp = new ButtonGroup();
        JRadioButtonMenuItem light = new JRadioButtonMenuItem("Light", true);
        JRadioButtonMenuItem dark = new JRadioButtonMenuItem("Dark");
        JRadioButtonMenuItem akaxyi = new JRadioButtonMenuItem("Akaxyi");
        grp.add(light); grp.add(dark); grp.add(akaxyi);
        theme.add(light); theme.add(dark); theme.add(akaxyi);
        JMenuItem custom = new JMenuItem("Custom Palette...");
        theme.addSeparator();
        theme.add(custom);
        light.addActionListener(e -> applyTheme(UIUtils.Theme.LIGHT));
        dark.addActionListener(e -> applyTheme(UIUtils.Theme.DARK));
        akaxyi.addActionListener(e -> applyTheme(UIUtils.Theme.AKAXYI));
        custom.addActionListener(e -> openCustomPaletteDialog());
        view.add(theme);

        JMenu help = new JMenu("Help");
        JMenuItem miQuick = new JMenuItem("Quick Start"); miQuick.addActionListener(e -> showQuickStart());
        JMenuItem miAbout = new JMenuItem("About"); miAbout.addActionListener(e -> showAbout());
        help.add(miQuick); help.add(miAbout);

        mb.add(file); mb.add(view); mb.add(help);
        return mb;
    }

    private void applyTheme(UIUtils.Theme theme) {
        UIUtils.setTheme(theme);
        // re-style components that depend on helper colors
        topBar.setBackground(UIUtils.topBarBackground());
        UIUtils.styleTabbedPane(tabs);
        status.setBackground(UIUtils.panelBg());
        statusLabel.setForeground(UIUtils.labelFg());
        if (pathLbl != null) pathLbl.setForeground(UIUtils.labelFg());
        SwingUtilities.updateComponentTreeUI(this);
        revalidate(); repaint();
    }

    private static String toHex(Color c) { return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()); }

    private void openCustomPaletteDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(UIUtils.panelBg());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Background (#RRGGBB):"), gc);
        gc.gridy++;
        panel.add(new JLabel("Surface (#RRGGBB):"), gc);
        gc.gridy++;
        panel.add(new JLabel("Text (#RRGGBB):"), gc);
        gc.gridy++;
        panel.add(new JLabel("Accent 1 (#RRGGBB):"), gc);
        gc.gridy++;
        panel.add(new JLabel("Accent 2 (#RRGGBB):"), gc);

        JTextField bgF = new JTextField(10);
        JTextField surfF = new JTextField(10);
        JTextField textF = new JTextField(10);
        JTextField acc1F = new JTextField(10);
        JTextField acc2F = new JTextField(10);
        // Prefill with current effective colors
        bgF.setText(toHex(UIUtils.panelBg()));
        surfF.setText(toHex(UIUtils.surfaceBg()));
        textF.setText(toHex(UIUtils.labelFg()));
        acc1F.setText(toHex(UIUtils.accent()));
        // Use income color as a friendly default for accent2 if not provided
        acc2F.setText(toHex(UIUtils.incomeFg()));

        gc.gridx = 1; gc.gridy = 0;
        panel.add(bgF, gc);
        gc.gridy++; panel.add(surfF, gc);
        gc.gridy++; panel.add(textF, gc);
        gc.gridy++; panel.add(acc1F, gc);
        gc.gridy++; panel.add(acc2F, gc);

        int res = JOptionPane.showConfirmDialog(this, panel, "Custom Palette", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            Color bg = Color.decode(bgF.getText().trim());
            Color surface = Color.decode(surfF.getText().trim());
            Color text = Color.decode(textF.getText().trim());
            Color acc1 = Color.decode(acc1F.getText().trim());
            Color acc2 = Color.decode(acc2F.getText().trim());
            UIUtils.setCustomPalette(bg, surface, text, acc1, acc2);
            applyTheme(UIUtils.Theme.CUSTOM);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid color format. Please use #RRGGBB.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showQuickStart() {
        JOptionPane.showMessageDialog(this,
                "Quick Start:\n- Add Expense/Income from the top bar.\n- Set Budgets for categories in the Budgets tab.\n- Use Prev/Next to browse months, or Select Month.\n- Export CSV from File > Export CSV.",
                "Quick Start", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Personal Finance App\nData file: " + service.getDataFile().toAbsolutePath(),
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openDataFolder() {
        try {
            Path p = service.getDataFile();
            java.awt.Desktop.getDesktop().open(p.getParent() == null ? new java.io.File(".") : p.getParent().toFile());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot open folder: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSelectMonth(ActionEvent e) {
        String in = JOptionPane.showInputDialog(this, "Enter year-month (YYYY-MM):", currentMonth.toString());
        if (in == null || in.isBlank()) return;
        try {
            currentMonth = YearMonth.parse(in.trim());
            refreshAll();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid year-month.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onExportCsv(ActionEvent e) {
        String defaultName = "transactions-" + currentMonth + ".csv";
        Path defaultDir = getDefaultExportDir();
        String suggested = defaultDir.resolve(defaultName).toString();
        String name = JOptionPane.showInputDialog(this, "Export file path:", suggested);
        if (name == null || name.isBlank()) return;
        try {
            Path chosen = Path.of(name.trim());
            Path target = chosen.isAbsolute() ? chosen : defaultDir.resolve(chosen);
            var out = service.exportCsv(target, currentMonth);
            statusLabel.setText("Exported CSV: " + out.getFileName());
            JOptionPane.showMessageDialog(this, "Exported to\n" + out.toAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Path getDefaultExportDir() {
        try {
            String userHome = System.getProperty("user.home", ".");
            Path docs = Path.of(userHome, "Documents");
            if (Files.isDirectory(docs)) return docs;
            Path downloads = Path.of(userHome, "Downloads");
            if (Files.isDirectory(downloads)) return downloads;
            return Path.of(userHome);
        } catch (Exception ex) {
            return Path.of(".");
        }
    }

    private void doSave() {
        try {
            service.save();
            statusLabel.setText("Saved " + currentMonth + " ");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAll() {
        monthLabel.setText("  " + currentMonth + "  ");
        summaryPanel.refresh();
        transactionsPanel.refresh();
        budgetsPanel.refresh();
        analyticsPanel.refresh();
        int count = service.getMonthTransactionCount(currentMonth);
        statusLabel.setText("Showing " + currentMonth + " - " + count + " transaction" + (count==1?"":"s"));
    }
}
