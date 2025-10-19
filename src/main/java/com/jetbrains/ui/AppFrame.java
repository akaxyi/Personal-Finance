package com.jetbrains.ui;

import com.jetbrains.finance.model.TransactionType;
import com.jetbrains.finance.service.FinanceService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;

public class AppFrame extends JFrame {
    private final FinanceService service;
    private YearMonth currentMonth;

    private final JLabel monthLabel = new JLabel();
    private final SummaryPanel summaryPanel;
    private final TransactionsPanel transactionsPanel;
    private final BudgetsPanel budgetsPanel;
    private final JLabel statusLabel = new JLabel("Ready");

    // keep a reference to the top bar so we can reapply styles when theme changes
    private final JPanel topBar;

    public AppFrame(FinanceService service, YearMonth startMonth) {
        super("Personal Finance");
        this.service = service;
        this.currentMonth = startMonth;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        setJMenuBar(buildMenu());

        // Tabs (init panels early so listeners can reference them)
        JTabbedPane tabs = new JTabbedPane();
        summaryPanel = new SummaryPanel(service, () -> currentMonth);
        transactionsPanel = new TransactionsPanel(service, () -> currentMonth, this::doSave, this::refreshAll);
        budgetsPanel = new BudgetsPanel(service, this::doSave, this::refreshAll);
        tabs.addTab("Summary", summaryPanel);
        tabs.addTab("Transactions", transactionsPanel);
        tabs.addTab("Budgets", budgetsPanel);

        // Top bar
        topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setOpaque(true);
        topBar.setBackground(UIUtils.topBarBackground());

        JButton prevBtn = new JButton("◀ Prev"); prevBtn.setToolTipText("Go to previous month"); UIUtils.styleButton(prevBtn);
        JButton nextBtn = new JButton("Next ▶"); nextBtn.setToolTipText("Go to next month"); UIUtils.styleButton(nextBtn);
        JButton selectBtn = new JButton("Select Month..."); selectBtn.setToolTipText("Jump to a specific month (YYYY-MM)"); UIUtils.styleButton(selectBtn);
        JButton exportBtn = new JButton("Export CSV"); exportBtn.setToolTipText("Export transactions for the current month to CSV"); UIUtils.styleButton(exportBtn);
        JButton addIncomeBtn = new JButton("Add Income"); addIncomeBtn.setToolTipText("Record an income for this month"); UIUtils.styleButton(addIncomeBtn);
        JButton addExpenseBtn = new JButton("Add Expense"); addExpenseBtn.setToolTipText("Record an expense for this month"); UIUtils.styleButton(addExpenseBtn);
        JButton saveBtn = new JButton("Save"); saveBtn.setToolTipText("Save data to " + service.getDataFile().toAbsolutePath()); UIUtils.styleButton(saveBtn);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 14f));

        // Optional icons (place PNG/SVG/etc under src/main/resources/icons/ with these names)
        ImageIcon ic;
        ic = UIUtils.loadIcon("icons/prev.png"); if (ic != null) prevBtn.setIcon(ic);
        ic = UIUtils.loadIcon("icons/next.png"); if (ic != null) nextBtn.setIcon(ic);
        ic = UIUtils.loadIcon("icons/select.png"); if (ic != null) selectBtn.setIcon(ic);
        ic = UIUtils.loadIcon("icons/export.png"); if (ic != null) exportBtn.setIcon(ic);
        ic = UIUtils.loadIcon("icons/income.png"); if (ic != null) addIncomeBtn.setIcon(ic);
        ic = UIUtils.loadIcon("icons/expense.png"); if (ic != null) addExpenseBtn.setIcon(ic);
        ic = UIUtils.loadIcon("icons/save.png"); if (ic != null) saveBtn.setIcon(ic);

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
        status.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        statusLabel.setForeground(new Color(70,70,70));
        status.add(statusLabel, BorderLayout.WEST);
        JLabel pathLbl = new JLabel("Data: " + service.getDataFile().toAbsolutePath());
        pathLbl.setToolTipText("Data file path");
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

        JMenu help = new JMenu("Help");
        JMenuItem miQuick = new JMenuItem("Quick Start"); miQuick.addActionListener(e -> showQuickStart());
        JMenuItem miAbout = new JMenuItem("About"); miAbout.addActionListener(e -> showAbout());
        help.add(miQuick); help.add(miAbout);

        mb.add(file); mb.add(help);
        return mb;
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
        String name = JOptionPane.showInputDialog(this, "Export file name:", defaultName);
        if (name == null || name.isBlank()) return;
        try {
            var out = service.exportCsv(Path.of(name.trim()), currentMonth);
            statusLabel.setText("Exported CSV: " + out.getFileName());
            JOptionPane.showMessageDialog(this, "Exported to\n" + out.toAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        int count = service.getMonthTransactionCount(currentMonth);
        statusLabel.setText("Showing " + currentMonth + " • " + count + " transaction" + (count==1?"":"s"));
    }
}
