package com.jetbrains.ui;

import com.jetbrains.finance.model.Transaction;
import com.jetbrains.finance.model.TransactionType;
import com.jetbrains.finance.service.FinanceService;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import javax.swing.table.DefaultTableCellRenderer;
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
    private final TableRowSorter<TransactionTableModel> sorter;

    public TransactionsPanel(FinanceService service, Supplier<YearMonth> monthSupplier, Runnable onSave, Runnable onDataChanged) {
        super(new BorderLayout(8,8));
        this.service = service;
        this.monthSupplier = monthSupplier;
        this.onSave = onSave;
        this.onDataChanged = onDataChanged;

        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setOpaque(true);
        tb.setBackground(UIUtils.topBarBackground());
        JButton addExp = new JButton("Add Expense"); addExp.setToolTipText("Record an expense for this month"); UIUtils.styleButton(addExp);
        JButton addInc = new JButton("Add Income"); addInc.setToolTipText("Record an income for this month"); UIUtils.styleButton(addInc);
        JButton edit = new JButton("Edit"); edit.setToolTipText("Edit the selected transaction"); UIUtils.styleButton(edit);
        JButton del = new JButton("Delete"); del.setToolTipText("Delete the selected transaction"); UIUtils.styleButton(del);
        JButton refresh = new JButton("Refresh"); refresh.setToolTipText("Reload this month's transactions"); UIUtils.styleButton(refresh);
        tb.add(addExp); tb.add(addInc); tb.add(edit); tb.add(del); tb.add(Box.createHorizontalGlue()); tb.add(refresh);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setOpaque(false);
        filterBar.add(new JLabel("Filter:"));
        filterField.setToolTipText("Type to filter by type/category/description/date");
        filterBar.add(filterField);
        JLabel hint = new JLabel("Tip: Double-click a row to edit.");
        hint.setForeground((Color)UIManager.get("Label.foreground"));
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
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setToolTipText("Transactions for the selected month");
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 6));
        // Avoid content getting squashed by enabling horizontal scrolling
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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
        // Set base delegates
        table.getColumnModel().getColumn(0).setCellRenderer(new UIUtils.DateRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new UIUtils.TypeBadgeRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new UIUtils.AmountRenderer());
        // Wrap each column to form a continuous rounded "card" per row
        var cm = table.getColumnModel();
        var r0 = cm.getColumn(0).getCellRenderer();
        var r1 = cm.getColumn(1).getCellRenderer();
        var r2 = cm.getColumn(2).getCellRenderer();
        var r3 = cm.getColumn(3).getCellRenderer();
        var r4 = cm.getColumn(4).getCellRenderer();
        cm.getColumn(0).setCellRenderer(new UIUtils.CardCellWrapperRenderer(r0, UIUtils.CardCellWrapperRenderer.FIRST));
        cm.getColumn(1).setCellRenderer(new UIUtils.CardCellWrapperRenderer(r1, UIUtils.CardCellWrapperRenderer.MIDDLE));
        cm.getColumn(2).setCellRenderer(new UIUtils.CardCellWrapperRenderer(r2, UIUtils.CardCellWrapperRenderer.MIDDLE));
        cm.getColumn(3).setCellRenderer(new UIUtils.CardCellWrapperRenderer(r3 != null ? r3 : new DefaultTableCellRenderer(), UIUtils.CardCellWrapperRenderer.MIDDLE));
        cm.getColumn(4).setCellRenderer(new UIUtils.CardCellWrapperRenderer(r4 != null ? r4 : new DefaultTableCellRenderer(), UIUtils.CardCellWrapperRenderer.LAST));

        // Sensible default widths so content is readable
        try {
            cm.getColumn(0).setPreferredWidth(120); // Date
            cm.getColumn(1).setPreferredWidth(110); // Type
            cm.getColumn(2).setPreferredWidth(140); // Amount
            cm.getColumn(3).setPreferredWidth(180); // Category
            cm.getColumn(4).setPreferredWidth(420); // Description
        } catch (Exception ignored) {}

        // Load persisted table state (widths and sort)
        SwingUtilities.invokeLater(() -> UIUtils.loadTableState(table, "transactions"));

        // Save state when user resizes/moves columns
        cm.addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) { SwingUtilities.invokeLater(() -> UIUtils.saveTableState(table, "transactions")); }
            @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) { SwingUtilities.invokeLater(() -> UIUtils.saveTableState(table, "transactions")); }
            @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) { }
            @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) { }
            @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) { }
        });
        // Save state when sorting changes
        sorter.addRowSorterListener(e -> SwingUtilities.invokeLater(() -> UIUtils.saveTableState(table, "transactions")));

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
