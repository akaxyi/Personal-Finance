package com.jetbrains.ui;

import com.jetbrains.finance.service.FinanceService;
import com.jetbrains.finance.service.MonthlySummary;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Analytics: charts and comparisons.
 * - Top: Donut (Income vs Expenses) for current month
 * - Middle: Month-over-month comparison text
 * - Bottom: Year view bar chart of monthly expenses with year selector
 */
public class AnalyticsPanel extends JPanel {
    private final FinanceService service;
    private final Supplier<YearMonth> monthSupplier;
    private final Consumer<YearMonth> onMonthSelected;

    private final DonutChart donut = new DonutChart();
    private final JLabel momLabel = new JLabel();
    private final YearBarChart yearChart = new YearBarChart();
    private final JComboBox<Integer> yearBox = new JComboBox<>();

    public AnalyticsPanel(FinanceService service, Supplier<YearMonth> monthSupplier, Consumer<YearMonth> onMonthSelected) {
        super(new BorderLayout(10,10));
        this.service = service;
        this.monthSupplier = monthSupplier;
        this.onMonthSelected = onMonthSelected == null ? ym -> {} : onMonthSelected;

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIUtils.titledLabel("Analytics"), BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Top: Donut inside a rounded card
        UIUtils.RoundedPanel donutCard = new UIUtils.RoundedPanel(new BorderLayout(), 24);
        donutCard.setBackground(UIUtils.surfaceBg());
        donutCard.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
        JLabel donutTitle = new JLabel("This Month");
        donutTitle.setForeground(UIUtils.labelFg());
        donutTitle.setBorder(BorderFactory.createEmptyBorder(4,6,8,6));
        donutTitle.setFont(donutTitle.getFont().deriveFont(Font.BOLD));
        donutCard.add(donutTitle, BorderLayout.NORTH);
        // make the donut slightly larger without affecting other panels
        donut.setPreferredSize(new Dimension(680, 340));
        donutCard.add(donut, BorderLayout.CENTER);

        // Middle: MoM comparison label (theme-aware)
        JPanel middle = new JPanel(new BorderLayout());
        middle.setOpaque(true);
        middle.setBackground(UIUtils.surfaceBg());
        momLabel.setForeground(UIUtils.labelFg());
        momLabel.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
        middle.add(momLabel, BorderLayout.CENTER);

        // Bottom: Year chart with selector
        JPanel yearPanel = new JPanel(new BorderLayout(6,6));
        yearPanel.setOpaque(false);
        JPanel barTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barTop.setOpaque(false);
        barTop.add(UIUtils.titledLabel("Year view"));
        barTop.add(new JLabel("Year:"));
        yearBox.setPrototypeDisplayValue(2099);
        // Slightly widen so the 4-digit year isn't clipped on some LAFs
        Dimension yps = yearBox.getPreferredSize();
        Dimension widened = new Dimension(Math.max(64, yps.width + 10), yps.height);
        yearBox.setPreferredSize(widened);
        yearBox.setMinimumSize(widened);
        barTop.add(yearBox);
        yearPanel.add(barTop, BorderLayout.NORTH);
        yearPanel.add(yearChart, BorderLayout.CENTER);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(donutCard);
        center.add(Box.createVerticalStrut(8));
        center.add(middle);
        center.add(Box.createVerticalStrut(8));
        center.add(yearPanel);

        add(center, BorderLayout.CENTER);

        yearBox.addActionListener(e -> updateYearChart());
        yearChart.setOnBarClicked(idx -> {
            Integer y = (Integer) yearBox.getSelectedItem();
            if (y == null) return;
            int month = Math.max(1, Math.min(12, idx + 1));
            onMonthSelected.accept(YearMonth.of(y, month));
        });

        refresh();
    }

    public void refresh() {
        YearMonth ym = monthSupplier.get();
        // Donut
        MonthlySummary ms = service.getMonthlySummary(ym);
        donut.setData(ms.totalIncome(), ms.totalExpense(), UIUtils.CURRENCY.format(ms.net()));
        // MoM comparison
        YearMonth prev = ym.minusMonths(1);
        MonthlySummary prevMs = service.getMonthlySummary(prev);
        BigDecimal currExp = ms.totalExpense();
        BigDecimal prevExp = prevMs.totalExpense();
        String momText;
        int cmp = currExp.compareTo(prevExp);
        BigDecimal diff = currExp.subtract(prevExp).abs();
        if (prevExp.compareTo(BigDecimal.ZERO) == 0) {
            momText = String.format("Compared to last month: spent %s (no prior baseline)", UIUtils.CURRENCY.format(currExp));
        } else if (cmp > 0) {
            double pct = safePct(currExp, prevExp);
            momText = String.format("You spent %s more than last month (%1.0f%% ↑)", UIUtils.CURRENCY.format(diff), pct);
        } else if (cmp < 0) {
            double pct = safePct(currExp, prevExp);
            momText = String.format("Nice! You spent %s less than last month (%1.0f%% ↓)", UIUtils.CURRENCY.format(diff), pct);
        } else {
            momText = "You spent exactly the same as last month.";
        }
        momLabel.setText(momText);

        // Year selector and chart
        populateYearBox(ym.getYear());
        updateYearChart();
        // highlight the current month if the same year is selected
        Integer y = (Integer) yearBox.getSelectedItem();
        if (y != null && y == ym.getYear()) {
            yearChart.setSelectedIndex(ym.getMonthValue() - 1);
        } else {
            yearChart.setSelectedIndex(-1);
        }
    }

    private static double safePct(BigDecimal a, BigDecimal b) {
        if (b.compareTo(BigDecimal.ZERO) == 0) return 100.0;
        try { return a.subtract(b).abs().multiply(new BigDecimal("100")).divide(b, 0, java.math.RoundingMode.HALF_UP).doubleValue(); }
        catch (Exception ex) { return 0.0; }
    }

    private void populateYearBox(int preferYear) {
        Set<YearMonth> months = service.getAvailableMonths();
        List<Integer> years = new ArrayList<>();
        for (YearMonth m : months) if (!years.contains(m.getYear())) years.add(m.getYear());
        years.sort(Comparator.naturalOrder());
        DefaultComboBoxModel<Integer> model = new DefaultComboBoxModel<>(years.toArray(new Integer[0]));
        yearBox.setModel(model);
        if (years.contains(preferYear)) yearBox.setSelectedItem(preferYear);
        else if (!years.isEmpty()) yearBox.setSelectedIndex(years.size()-1);
    }

    private void updateYearChart() {
        Integer y = (Integer) yearBox.getSelectedItem();
        if (y == null) return;
        String[] labels = new String[12];
        BigDecimal[] vals = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            labels[i] = Month.of(i+1).name().substring(0,3);
            YearMonth ym = YearMonth.of(y, i+1);
            MonthlySummary s = service.getMonthlySummary(ym);
            vals[i] = s.totalExpense();
        }
        yearChart.setData(labels, vals, "Expenses in " + y);
        repaint();
    }
}
