package com.jetbrains.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.math.BigDecimal;

/**
 * Simple donut chart showing Income vs Expenses, with a large Net amount in the centre.
 * Theme-aware and uses the current accent colour.
 */
public class DonutChart extends JComponent {
    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal expense = BigDecimal.ZERO;
    private String centreText = "£0.00";

    public void setData(BigDecimal income, BigDecimal expense, String centreText) {
        this.income = income == null ? BigDecimal.ZERO : income;
        this.expense = expense == null ? BigDecimal.ZERO : expense;
        this.centreText = centreText == null ? "" : centreText;
        repaint();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(520, 280); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        Font base = getFont() != null ? getFont() : UIManager.getFont("Label.font");
        g2.setFont(base);
        int lh = g2.getFontMetrics().getHeight() + 12; // legend height + padding
        int usableH = Math.max(1, h - lh);

        int size = Math.min(w, usableH) - 24; // margin
        size = Math.max(80, size);
        int x = (w - size) / 2;
        int y = (usableH - size) / 2;

        // Colours based on theme
        Color ringBg = UIUtils.surfaceBg();
        Color incomeColor;
        Color expenseColor;
        if (UIUtils.isDark()) {
            if (UIUtils.accent().equals(new Color(80, 200, 140))) { // dark-green theme
                incomeColor = new Color(90, 210, 150);
                expenseColor = new Color(70, 170, 130);
            } else { // dark-blue theme
                incomeColor = UIUtils.accent(); // vibrant blue
                expenseColor = new Color(64, 128, 255); // analogous blue
            }
        } else {
            incomeColor = new Color(120, 210, 180);   // mint
            expenseColor = new Color(250, 200, 170);  // peach
        }

        // Ring background
        g2.setColor(ringBg);
        g2.fill(new Ellipse2D.Float(x, y, size, size));

        double inc = income.doubleValue();
        double exp = expense.doubleValue();
        double total = Math.max(inc + exp, 1.0); // avoid div by zero

        double incAng = 360.0 * (inc / total);
        double expAng = 360.0 * (exp / total);

        // Draw arcs
        float start = 90f; // start at top
        Arc2D.Double arcInc = new Arc2D.Double(x, y, size, size, -start, -incAng, Arc2D.PIE);
        g2.setColor(incomeColor);
        g2.fill(arcInc);
        Arc2D.Double arcExp = new Arc2D.Double(x, y, size, size, -start - incAng, -expAng, Arc2D.PIE);
        g2.setColor(expenseColor);
        g2.fill(arcExp);

        // Cut out the donut centre
        int hole = Math.round(size * 0.58f);
        int hx = x + (size - hole) / 2;
        int hy = y + (size - hole) / 2;
        g2.setColor(getParent() != null ? getParent().getBackground() : UIUtils.panelBg());
        g2.fill(new Ellipse2D.Float(hx, hy, hole, hole));

        // Centre text (Net)
        g2.setColor(UIUtils.labelFg());
        Font big = base.deriveFont(Font.BOLD, base.getSize2D() + 10f);
        g2.setFont(big);
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(centreText)) / 2;
        int ty = y + size/2 + fm.getAscent()/2 - 3;
        g2.drawString(centreText, tx, ty);

        // Legend (income/expense)
        g2.setFont(base);
        fm = g2.getFontMetrics();
        int legendY = usableH + (lh + fm.getAscent())/2 - 6;
        int lx = Math.max(16, (w - 220) / 2);
        int sw = 14; int sh = 14;
        g2.setColor(incomeColor); g2.fillRoundRect(lx, legendY - sh, sw, sh, 6, 6);
        g2.setColor(UIUtils.labelFg()); g2.drawString("Income", lx + sw + 8, legendY);
        lx += 120;
        g2.setColor(expenseColor); g2.fillRoundRect(lx, legendY - sh, sw, sh, 6, 6);
        g2.setColor(UIUtils.labelFg()); g2.drawString("Expenses", lx + sw + 8, legendY);

        g2.dispose();
    }
}
