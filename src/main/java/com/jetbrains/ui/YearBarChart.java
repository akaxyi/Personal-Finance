package com.jetbrains.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Minimal, theme-aware bar chart for 12 months.
 */
public class YearBarChart extends JComponent {
    private String[] monthLabels = new String[12];
    private BigDecimal[] values = new BigDecimal[12]; // usually expenses per month
    private String yLegend = "Expenses";

    private int selectedIndex = -1;
    private int hoverIndex = -1;
    private Consumer<Integer> onBarClicked;

    public YearBarChart() {
        for (int i = 0; i < 12; i++) { monthLabels[i] = String.valueOf(i+1); values[i] = BigDecimal.ZERO; }
        setOpaque(false);
        // Mouse interaction for selecting a month by clicking a bar
        MouseAdapter click = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int idx = barIndexAtPoint(e.getX(), e.getY());
                if (idx >= 0) {
                    setSelectedIndex(idx);
                    if (onBarClicked != null) onBarClicked.accept(idx);
                }
            }
        }; addMouseListener(click);
        // Hover feedback: hand cursor and tooltip
        MouseMotionAdapter mm = new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                hoverIndex = barIndexAtPoint(e.getX(), e.getY());
                setCursor(hoverIndex >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                setToolTipText(hoverIndex >= 0 ? tooltipFor(hoverIndex) : null);
            }
        }; addMouseMotionListener(mm);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    private String tooltipFor(int idx) {
        NumberFormat c = NumberFormat.getCurrencyInstance(Locale.UK);
        BigDecimal v = (idx >= 0 && idx < values.length && values[idx] != null) ? values[idx] : BigDecimal.ZERO;
        String lab = (idx >= 0 && idx < monthLabels.length) ? monthLabels[idx] : String.valueOf(idx+1);
        return lab + ": " + c.format(v);
    }

    public void setOnBarClicked(Consumer<Integer> listener) { this.onBarClicked = listener; }
    public void setSelectedIndex(int idx) { this.selectedIndex = (idx >= 0 && idx < 12) ? idx : -1; repaint(); }

    public void setData(String[] labels, BigDecimal[] vals, String legend) {
        if (labels != null && labels.length == 12) this.monthLabels = labels.clone();
        if (vals != null && vals.length == 12) this.values = vals.clone();
        if (legend != null) this.yLegend = legend;
        repaint();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(820, 280); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int left = 46, right = 16, top = 18, bottom = 40;
        int cw = w - left - right; int ch = h - top - bottom;

        // background
        g2.setColor(UIUtils.surfaceBg());
        g2.fillRoundRect(0, 0, w, h, 16, 16);

        // find max
        double max = 0.0;
        for (var v : values) max = Math.max(max, v == null ? 0.0 : v.doubleValue());
        if (max <= 0) max = 1.0;

        // axes
        g2.setColor(UIUtils.subtleBorder());
        g2.drawLine(left, h - bottom, left + cw, h - bottom);
        g2.drawLine(left, top, left, h - bottom);

        // legend
        g2.setColor(UIUtils.labelFg());
        g2.setFont(getFont().deriveFont(Font.BOLD));
        g2.drawString(yLegend, left, top - 2);

        // bars
        int barSpace = Math.max(1, cw / 12);
        int barW = Math.max(8, barSpace - 8);
        int x = left + (barSpace - barW) / 2;
        Color bar = UIUtils.accent();
        Color barAlt = new Color(bar.getRed(), bar.getGreen(), bar.getBlue(), 140);
        for (int i = 0; i < 12; i++) {
            double v = values[i] == null ? 0.0 : values[i].doubleValue();
            int bh = (int) Math.round((v / max) * (ch - 4));
            int by = h - bottom - bh;
            g2.setColor(i % 2 == 0 ? bar : barAlt);
            g2.fillRoundRect(x, by, barW, bh, 8, 8);
            // selection highlight
            if (i == selectedIndex) {
                Stroke old = g2.getStroke();
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(UIUtils.onColor(UIUtils.surfaceBg()));
                g2.drawRoundRect(x, by, barW, bh, 8, 8);
                g2.setStroke(old);
            }
            // month tick/label
            g2.setColor(UIUtils.labelFg());
            String lab = monthLabels[i];
            int sw = g2.getFontMetrics().stringWidth(lab);
            int lx = x + (barW - sw) / 2;
            g2.drawString(lab, lx, h - bottom + g2.getFontMetrics().getAscent() + 6);
            x += barSpace;
        }

        g2.dispose();
    }

    private int barIndexAtPoint(int px, int py) {
        int w = getWidth(), h = getHeight();
        int left = 46, right = 16, top = 18, bottom = 40;
        int cw = w - left - right; int ch = h - top - bottom;
        if (px < left || px > left + cw || py < top || py > h - bottom) return -1;
        int barSpace = Math.max(1, cw / 12);
        int barW = Math.max(8, barSpace - 8);
        int idx = (px - left) / barSpace;
        int xStart = left + idx * barSpace + (barSpace - barW) / 2;
        if (px >= xStart && px <= xStart + barW) return Math.max(0, Math.min(11, idx));
        return -1;
    }
}
