package com.jetbrains.ui;

import com.jetbrains.finance.service.FinanceService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Map;

public class BudgetsPanel extends JPanel {
    private final FinanceService service;
    private final Runnable onSave;
    private final Runnable onDataChanged;

    private final JTable table = new JTable();
    private boolean sorterListenerInstalled = false;

    public BudgetsPanel(FinanceService service, Runnable onSave, Runnable onDataChanged) {
        super(new BorderLayout(8,8));
        this.service = service;
        this.onSave = onSave;
        this.onDataChanged = onDataChanged;

        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setOpaque(true);
        tb.setBackground(UIUtils.topBarBackground());
        JButton add = new JButton("Add"); add.setToolTipText("Create a new budget category with a monthly limit"); UIUtils.styleButton(add);
        JButton edit = new JButton("Edit Limit"); edit.setToolTipText("Change the monthly limit for the selected category"); UIUtils.styleButton(edit);
        JButton del = new JButton("Remove"); del.setToolTipText("Remove the selected budget category (does not delete transactions)"); UIUtils.styleButton(del);
        JButton refresh = new JButton("Refresh"); refresh.setToolTipText("Reload budgets"); UIUtils.styleButton(refresh);
        tb.add(add); tb.add(edit); tb.add(del); tb.add(Box.createHorizontalGlue()); tb.add(refresh);

        add(tb, BorderLayout.NORTH);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel hint = new JLabel("Tip: Budgets are monthly caps per category. Removing a budget does not delete past expenses.");
        hint.setForeground((Color)UIManager.get("Label.foreground"));
        hint.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        add(hint, BorderLayout.SOUTH);

        add.addActionListener(e -> onAdd());
        edit.addActionListener(e -> onEdit());
        del.addActionListener(e -> onDelete());
        refresh.addActionListener(e -> refresh());

        refresh();
        // load persisted state after initial model installed
        SwingUtilities.invokeLater(() -> UIUtils.loadTableState(table, "budgets"));
        maybeInstallSorterListener();
    }

    private void attachColumnModelPersistence() {
        var cm = table.getColumnModel();
        cm.addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) { SwingUtilities.invokeLater(() -> UIUtils.saveTableState(table, "budgets")); }
            @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) { SwingUtilities.invokeLater(() -> UIUtils.saveTableState(table, "budgets")); }
            @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) { }
            @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) { }
            @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) { }
        });
    }

    private void maybeInstallSorterListener() {
        if (!sorterListenerInstalled && table.getRowSorter() != null) {
            table.getRowSorter().addRowSorterListener(e -> SwingUtilities.invokeLater(() -> UIUtils.saveTableState(table, "budgets")));
            sorterListenerInstalled = true;
        }
    }

    public void refresh() {
        Map<String, BigDecimal> budgets = service.getBudgets();
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Category", "Monthly Limit"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? BigDecimal.class : String.class;
            }
        };
        budgets.forEach((cat, limit) -> model.addRow(new Object[]{cat, limit}));
        if (model.getRowCount() == 0) model.addRow(new Object[]{"(no budgets)", null});
        table.setModel(model);
        table.getColumnModel().getColumn(1).setCellRenderer(new UIUtils.CurrencyRenderer());
        // sensible defaults
        try {
            table.getColumnModel().getColumn(0).setPreferredWidth(260);
            table.getColumnModel().getColumn(1).setPreferredWidth(160);
        } catch (Exception ignored) {}
        // reattach persistence listeners to the (possibly) new column model and reapply saved state
        attachColumnModelPersistence();
        SwingUtilities.invokeLater(() -> UIUtils.loadTableState(table, "budgets"));
        maybeInstallSorterListener();
    }

    private void onAdd() {
        String cat = JOptionPane.showInputDialog(this, "Category name:");
        if (cat == null || cat.isBlank()) return;
        String amt = JOptionPane.showInputDialog(this, "Monthly limit (e.g., 250.00):");
        if (amt == null || amt.isBlank()) return;
        try {
            BigDecimal lim = new BigDecimal(amt.trim());
            service.setBudget(cat.trim(), lim);
            onSave.run();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Add failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a row to edit."); return; }
        Object catObj = table.getValueAt(row, 0);
        if (catObj == null || "(no budgets)".equals(catObj.toString())) return;
        String cat = catObj.toString();
        String amt = JOptionPane.showInputDialog(this, "New monthly limit for '" + cat + "':", table.getValueAt(row, 1));
        if (amt == null || amt.isBlank()) return;
        try {
            BigDecimal lim = new BigDecimal(amt.trim());
            service.setBudget(cat, lim);
            onSave.run();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Edit failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a row to remove."); return; }
        Object catObj = table.getValueAt(row, 0);
        if (catObj == null || "(no budgets)".equals(catObj.toString())) return;
        String cat = catObj.toString();
        int res = JOptionPane.showConfirmDialog(this, "Remove budget '" + cat + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (res != JOptionPane.YES_OPTION) return;
        try {
            service.removeBudget(cat);
            onSave.run();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Remove failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
