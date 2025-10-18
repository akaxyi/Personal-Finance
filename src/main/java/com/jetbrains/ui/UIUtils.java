package com.jetbrains.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class UIUtils {
    private UIUtils() {}

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    public static void initLookAndFeel() {
        // Subtle UI tweaks
        UIManager.put("Table.alternateRowColor", new Color(250, 250, 250));
        UIManager.put("Panel.background", Color.white);
        UIManager.put("ToolTip.background", new Color(255, 255, 240));
        // Slightly larger default font for readability
        Font base = UIManager.getFont("Label.font");
        if (base != null) {
            Font bigger = base.deriveFont(base.getSize2D() + 1.0f);
            for (Object key : UIManager.getDefaults().keySet()) {
                if (key.toString().endsWith(".font")) {
                    UIManager.put(key, bigger);
                }
            }
        }
        ToolTipManager.sharedInstance().setInitialDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(12000);
    }

    public static JLabel titledLabel(String title) {
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, l.getFont().getSize2D() + 1f));
        return l;
    }

    public static JPanel card(String title, String value, Color bg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230,230,230)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        p.setBackground(bg);
        JLabel t = new JLabel(title);
        t.setForeground(new Color(60,60,60));
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.BOLD, v.getFont().getSize2D() + 4f));
        p.add(t, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    public static class CurrencyRenderer extends DefaultTableCellRenderer {
        @Override protected void setValue(Object value) {
            if (value instanceof BigDecimal bd) setText(CURRENCY.format(bd));
            else super.setValue(value);
        }
    }

    public static class DateRenderer extends DefaultTableCellRenderer {
        @Override protected void setValue(Object value) {
            if (value instanceof LocalDate d) setText(DATE_FMT.format(d));
            else super.setValue(value);
        }
    }
}

