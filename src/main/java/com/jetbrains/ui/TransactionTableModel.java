package com.jetbrains.ui;

import com.jetbrains.finance.model.Transaction;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class TransactionTableModel extends AbstractTableModel {
    private final String[] cols = {"Date", "Type", "Amount", "Category", "Description"};
    private final List<Transaction> rows = new ArrayList<>();

    public void setRows(List<Transaction> data) {
        rows.clear();
        if (data != null) rows.addAll(data);
        fireTableDataChanged();
    }

    public Transaction getAt(int row) {
        if (row < 0 || row >= rows.size()) return null;
        return rows.get(row);
    }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int column) { return cols[column]; }

    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        Transaction t = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> t.getDate();
            case 1 -> t.getType();
            case 2 -> t.getAmount();
            case 3 -> t.getCategory();
            case 4 -> t.getDescription();
            default -> "";
        };
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> java.time.LocalDate.class;
            case 1 -> com.jetbrains.finance.model.TransactionType.class;
            case 2 -> java.math.BigDecimal.class;
            default -> String.class;
        };
    }
}

