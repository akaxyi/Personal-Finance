package com.jetbrains.ui;

import com.jetbrains.finance.service.FinanceService;
import com.jetbrains.finance.service.MonthlySummary;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final DonutChart donut = new DonutChart();

    public SummaryPanel(FinanceService service, Supplier<YearMonth> monthSupplier) {
        super(new BorderLayout(10,10));
        this.service = service;
        this.monthSupplier = monthSupplier;

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 12));
        // Use theme-aware card backgrounds
        JPanel incomeCard = UIUtils.card("Income", UIUtils.cardBgIncome());
        JPanel expenseCard = UIUtils.card("Expenses", UIUtils.cardBgExpense());
        JPanel netCard = UIUtils.card("Net", UIUtils.cardBgNet());

        // Configure the large value labels and add them to the CENTER of the cards (one visible value)
        incomeLbl.setHorizontalAlignment(SwingConstants.CENTER);
        expenseLbl.setHorizontalAlignment(SwingConstants.CENTER);
        netLbl.setHorizontalAlignment(SwingConstants.CENTER);
        // Bigger font for the main numbers (emphasis)
        incomeLbl.setFont(incomeLbl.getFont().deriveFont(Font.BOLD, incomeLbl.getFont().getSize2D() + 8f));
        expenseLbl.setFont(expenseLbl.getFont().deriveFont(Font.BOLD, expenseLbl.getFont().getSize2D() + 8f));
        netLbl.setFont(netLbl.getFont().deriveFont(Font.BOLD, netLbl.getFont().getSize2D() + 8f));
        incomeCard.add(incomeLbl, BorderLayout.CENTER);
        expenseCard.add(expenseLbl, BorderLayout.CENTER);
        netCard.add(netLbl, BorderLayout.CENTER);
        cards.add(incomeCard); cards.add(expenseCard); cards.add(netCard);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UIUtils.titledLabel("This month"), BorderLayout.WEST);
        top.add(cards, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Middle area: Donut chart above budgets table
        JPanel center = new JPanel(new BorderLayout(10,10));
        UIUtils.RoundedPanel donutWrap = new UIUtils.RoundedPanel(new BorderLayout(), 24);
        donutWrap.setBackground(new Color(248, 250, 252));
        donutWrap.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
        donutWrap.add(donut, BorderLayout.CENTER);
        center.add(donutWrap, BorderLayout.NORTH);

        budgetTable.setFillsViewportHeight(true);
        budgetTable.setRowHeight(30);
        budgetTable.setShowGrid(false);
        budgetTable.setIntercellSpacing(new java.awt.Dimension(0, 6));
        budgetTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane sp = new JScrollPane(budgetTable);
        sp.setBorder(BorderFactory.createEmptyBorder());
        center.add(sp, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JLabel hint = new JLabel("Tip: Set budgets in the Budgets tab. Expenses roll up by category.");
        hint.setForeground((Color)UIManager.get("Label.foreground"));
        hint.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        add(hint, BorderLayout.SOUTH);

        // Persist table layout
        budgetTable.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) { SwingUtilities.invokeLater(() -> UIUtils.saveTableState(budgetTable, "summary")); }
            @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) { SwingUtilities.invokeLater(() -> UIUtils.saveTableState(budgetTable, "summary")); }
            @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) { }
            @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) { }
            @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) { }
        });

        refresh();
        // Ensure sorter created and then restore state
        budgetTable.setAutoCreateRowSorter(true);
        budgetTable.getRowSorter().addRowSorterListener(e -> SwingUtilities.invokeLater(() -> UIUtils.saveTableState(budgetTable, "summary")));
        SwingUtilities.invokeLater(() -> UIUtils.loadTableState(budgetTable, "summary"));
    }

    public void refresh() {
        YearMonth ym = monthSupplier.get();
        MonthlySummary s = service.getMonthlySummary(ym);
        incomeLbl.setText(UIUtils.CURRENCY.format(s.totalIncome()));
        expenseLbl.setText(UIUtils.CURRENCY.format(s.totalExpense()));
        netLbl.setText(UIUtils.CURRENCY.format(s.net()));
        // Donut represents income vs expenses, centre shows net
        donut.setData(s.totalIncome(), s.totalExpense(), UIUtils.CURRENCY.format(s.net()));

        Map<String, BigDecimal> budgets = service.getBudgets();
        Map<String, BigDecimal> spent = service.getSpentByCategory(ym);
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Category", "Limit", "Spent", "Remaining", "Used"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 1,2,3 -> BigDecimal.class;
                    case 4 -> Double.class;
                    default -> String.class;
                };
            }
        };
        budgets.forEach((cat, limit) -> {
            BigDecimal spt = spent.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal remaining = limit.subtract(spt);
            double pct;
            if (limit == null || limit.compareTo(BigDecimal.ZERO) == 0) {
                pct = spt.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
            } else {
                pct = spt.multiply(new BigDecimal("100")).divide(limit, 2, RoundingMode.HALF_UP).doubleValue();
            }
            model.addRow(new Object[]{cat, limit, spt, remaining, pct});
        });
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"(no budgets)", null, null, null, 0.0});
        }
        budgetTable.setModel(model);
        budgetTable.getColumnModel().getColumn(1).setCellRenderer(new UIUtils.CurrencyRenderer());
        budgetTable.getColumnModel().getColumn(2).setCellRenderer(new UIUtils.CurrencyRenderer());
        budgetTable.getColumnModel().getColumn(3).setCellRenderer(new UIUtils.CurrencyRenderer());
        budgetTable.getColumnModel().getColumn(4).setCellRenderer(new UIUtils.ProgressRenderer());
        budgetTable.setAutoCreateRowSorter(true);
        // Default widths to avoid squashing
        budgetTable.getColumnModel().getColumn(0).setPreferredWidth(260);
        budgetTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        budgetTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        budgetTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        budgetTable.getColumnModel().getColumn(4).setPreferredWidth(180);
    }
}
