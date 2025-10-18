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

    public BudgetsPanel(FinanceService service, Runnable onSave, Runnable onDataChanged) {
        super(new BorderLayout(8,8));
        this.service = service;
        this.onSave = onSave;
        this.onDataChanged = onDataChanged;

        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        JButton add = new JButton("Add"); add.setToolTipText("Create a new budget category with a monthly limit");
        JButton edit = new JButton("Edit Limit"); edit.setToolTipText("Change the monthly limit for the selected category");
        JButton del = new JButton("Remove"); del.setToolTipText("Remove the selected budget category (does not delete transactions)");
        JButton refresh = new JButton("Refresh"); refresh.setToolTipText("Reload budgets");
        tb.add(add); tb.add(edit); tb.add(del); tb.add(Box.createHorizontalGlue()); tb.add(refresh);

        add(tb, BorderLayout.NORTH);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel hint = new JLabel("Tip: Budgets are monthly caps per category. Removing a budget does not delete past expenses.");
        hint.setForeground(new Color(100,100,100));
        hint.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        add(hint, BorderLayout.SOUTH);

        add.addActionListener(e -> onAdd());
        edit.addActionListener(e -> onEdit());
        del.addActionListener(e -> onDelete());
        refresh.addActionListener(e -> refresh());

        refresh();
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
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
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
