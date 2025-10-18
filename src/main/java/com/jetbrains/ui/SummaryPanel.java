package com.jetbrains.ui;

import com.jetbrains.finance.service.FinanceService;
import com.jetbrains.finance.service.MonthlySummary;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.function.Supplier;

public class SummaryPanel extends JPanel {
    private final FinanceService service;
    private final Supplier<YearMonth> monthSupplier;

    private final JLabel incomeLbl = new JLabel();
    private final JLabel expenseLbl = new JLabel();
    private final JLabel netLbl = new JLabel();
    private final JTable budgetTable = new JTable();

    public SummaryPanel(FinanceService service, Supplier<YearMonth> monthSupplier) {
        super(new BorderLayout(8,8));
        this.service = service;
        this.monthSupplier = monthSupplier;

        JPanel cards = new JPanel(new GridLayout(1, 3, 8, 8));
        JPanel incomeCard = UIUtils.card("Income", "$0.00", new Color(240, 255, 245));
        JPanel expenseCard = UIUtils.card("Expenses", "$0.00", new Color(255, 245, 245));
        JPanel netCard = UIUtils.card("Net", "$0.00", new Color(245, 248, 255));
        incomeLbl.setHorizontalAlignment(SwingConstants.CENTER);
        expenseLbl.setHorizontalAlignment(SwingConstants.CENTER);
        netLbl.setHorizontalAlignment(SwingConstants.CENTER);
        incomeCard.add(incomeLbl, BorderLayout.SOUTH);
        expenseCard.add(expenseLbl, BorderLayout.SOUTH);
        netCard.add(netLbl, BorderLayout.SOUTH);
        cards.add(incomeCard); cards.add(expenseCard); cards.add(netCard);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIUtils.titledLabel("This month"), BorderLayout.WEST);
        top.add(cards, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        budgetTable.setFillsViewportHeight(true);
        budgetTable.setRowHeight(24);
        add(new JScrollPane(budgetTable), BorderLayout.CENTER);

        JLabel hint = new JLabel("Tip: Set budgets in the Budgets tab. Expenses roll up by category.");
        hint.setForeground(new Color(100,100,100));
        hint.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        add(hint, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        YearMonth ym = monthSupplier.get();
        MonthlySummary s = service.getMonthlySummary(ym);
        incomeLbl.setText(UIUtils.CURRENCY.format(s.totalIncome()));
        expenseLbl.setText(UIUtils.CURRENCY.format(s.totalExpense()));
        netLbl.setText(UIUtils.CURRENCY.format(s.net()));

        Map<String, BigDecimal> budgets = service.getBudgets();
        Map<String, BigDecimal> spent = service.getSpentByCategory(ym);
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Category", "Limit", "Spent", "Remaining"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 1,2,3 -> BigDecimal.class;
                    default -> String.class;
                };
            }
        };
        budgets.forEach((cat, limit) -> {
            BigDecimal spt = spent.getOrDefault(cat, BigDecimal.ZERO);
            model.addRow(new Object[]{cat, limit, spt, limit.subtract(spt)});
        });
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"(no budgets)", null, null, null});
        }
        budgetTable.setModel(model);
        budgetTable.getColumnModel().getColumn(1).setCellRenderer(new UIUtils.CurrencyRenderer());
        budgetTable.getColumnModel().getColumn(2).setCellRenderer(new UIUtils.CurrencyRenderer());
        budgetTable.getColumnModel().getColumn(3).setCellRenderer(new UIUtils.CurrencyRenderer());
        budgetTable.setAutoCreateRowSorter(true);
        budgetTable.getColumnModel().getColumn(0).setPreferredWidth(200);
    }
}
