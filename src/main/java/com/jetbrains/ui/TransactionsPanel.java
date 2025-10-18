package com.jetbrains.ui;

import com.jetbrains.finance.model.Transaction;
import com.jetbrains.finance.model.TransactionType;
import com.jetbrains.finance.service.FinanceService;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Supplier;

public class TransactionsPanel extends JPanel {
    private final FinanceService service;
    private final Supplier<YearMonth> monthSupplier;
    private final Runnable onSave;
    private final Runnable onDataChanged;

    private final TransactionTableModel model = new TransactionTableModel();
    private final JTable table = new JTable(model);
    private final JTextField filterField = new JTextField(18);
    private TableRowSorter<TransactionTableModel> sorter;

    public TransactionsPanel(FinanceService service, Supplier<YearMonth> monthSupplier, Runnable onSave, Runnable onDataChanged) {
        super(new BorderLayout(8,8));
        this.service = service;
        this.monthSupplier = monthSupplier;
        this.onSave = onSave;
        this.onDataChanged = onDataChanged;

        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        JButton addExp = new JButton("Add Expense"); addExp.setToolTipText("Record an expense for this month");
        JButton addInc = new JButton("Add Income"); addInc.setToolTipText("Record an income for this month");
        JButton edit = new JButton("Edit"); edit.setToolTipText("Edit the selected transaction");
        JButton del = new JButton("Delete"); del.setToolTipText("Delete the selected transaction");
        JButton refresh = new JButton("Refresh"); refresh.setToolTipText("Reload this month's transactions");
        tb.add(addExp); tb.add(addInc); tb.add(edit); tb.add(del); tb.add(Box.createHorizontalGlue()); tb.add(refresh);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.add(new JLabel("Filter:"));
        filterField.setToolTipText("Type to filter by type/category/description/date");
        filterBar.add(filterField);
        JLabel hint = new JLabel("Tip: Double-click a row to edit.");
        hint.setForeground(new Color(100,100,100));
        filterBar.add(hint);

        add(tb, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(filterBar, BorderLayout.SOUTH);

        addExp.addActionListener(e -> addTransactionDialog(TransactionType.EXPENSE));
        addInc.addActionListener(e -> addTransactionDialog(TransactionType.INCOME));
        edit.addActionListener(e -> editSelected());
        del.addActionListener(e -> deleteSelected());
        refresh.addActionListener(e -> refresh());

        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setToolTipText("Transactions for the selected month");
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editSelected();
                }
            }
        });

        // Renderers and sorter
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setCellRenderer(new UIUtils.DateRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new UIUtils.CurrencyRenderer());

        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                String txt = filterField.getText().trim();
                if (txt.isEmpty()) { sorter.setRowFilter(null); return; }
                sorter.setRowFilter(new RowFilter<>() {
                    @Override
                    public boolean include(Entry<? extends TransactionTableModel, ? extends Integer> entry) {
                        for (int i = 0; i < entry.getValueCount(); i++) {
                            Object v = entry.getValue(i);
                            if (v == null) continue;
                            if (v.toString().toLowerCase().contains(txt.toLowerCase())) return true;
                        }
                        return false;
                    }
                });
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        refresh();
    }

    public void refresh() {
        model.setRows(service.getTransactionsForMonth(monthSupplier.get()));
    }

    public void addTransactionDialog(TransactionType type) {
        YearMonth ym = monthSupplier.get();
        TransactionDialog dlg = new TransactionDialog(SwingUtilities.getWindowAncestor(this), "Add " + type);
        int day = Math.min(LocalDate.now().getDayOfMonth(), ym.lengthOfMonth());
        dlg.preset(type, ym.atDay(day), new java.math.BigDecimal("0.01"), type == TransactionType.EXPENSE ? "" : "INCOME", "");
        dlg.setVisible(true);
        if (!dlg.isOk()) return;
        try {
            service.addTransaction(dlg.getTxnType(), dlg.getDate(), dlg.getAmount(),
                    dlg.getTxnType() == TransactionType.EXPENSE ? dlg.getCategory() : "INCOME",
                    dlg.getDescription());
            onSave.run();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Add failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { JOptionPane.showMessageDialog(this, "Select a row to edit."); return; }
        int row = table.convertRowIndexToModel(viewRow);
        Transaction t = model.getAt(row);
        YearMonth ym = monthSupplier.get();
        TransactionDialog dlg = new TransactionDialog(SwingUtilities.getWindowAncestor(this), "Edit Transaction");
        dlg.preset(t.getType(), t.getDate(), t.getAmount(), t.getCategory(), t.getDescription());
        dlg.setVisible(true);
        if (!dlg.isOk()) return;
        try {
            int oneBased = row + 1; // within current month ordering
            service.editTransactionAt(ym, oneBased, dlg.getTxnType(), dlg.getDate(), dlg.getAmount(),
                    dlg.getTxnType() == TransactionType.EXPENSE ? dlg.getCategory() : null,
                    dlg.getDescription());
            onSave.run();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Edit failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { JOptionPane.showMessageDialog(this, "Select a row to delete."); return; }
        int row = table.convertRowIndexToModel(viewRow);
        int res = JOptionPane.showConfirmDialog(this, "Delete selected transaction?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (res != JOptionPane.YES_OPTION) return;
        try {
            int oneBased = row + 1;
            if (!service.deleteTransactionAt(monthSupplier.get(), oneBased)) {
                JOptionPane.showMessageDialog(this, "Delete failed: index not found.");
                return;
            }
            onSave.run();
            onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
