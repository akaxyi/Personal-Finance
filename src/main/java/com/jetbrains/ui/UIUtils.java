package com.jetbrains.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

public final class UIUtils {
    private UIUtils() {}

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // Use UK pounds formatting
    public static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.UK);

    // Typographic constants
    public static final String[] PREFERRED_FAMILIES = new String[]{"Segoe UI", "Tahoma", "Helvetica", "Arial", "SansSerif"};
    public static float BASE_FONT_SIZE = 13f;

    public static void initLookAndFeel() {
        // Try to load bundled fonts (optional). Place TTFs under src/main/resources/fonts/ if you want to use them.
        Font base = tryLoadFontFromResources("/fonts/Inter-Regular.ttf", 0, 13f);
        Font baseBold = tryLoadFontFromResources("/fonts/Inter-Bold.ttf", Font.BOLD, 13f);

        if (base == null) {
            String chosen = findAvailableFamily(PREFERRED_FAMILIES);
            if (chosen == null) chosen = "SansSerif";
            BASE_FONT_SIZE = UIManager.getFont("Label.font").getSize2D() + 1.5f;
            base = new Font(chosen, Font.PLAIN, Math.round(BASE_FONT_SIZE));
            baseBold = base.deriveFont(Font.BOLD);
        } else {
            BASE_FONT_SIZE = base.getSize2D();
            if (baseBold == null) baseBold = base.deriveFont(Font.BOLD);
        }

        // Apply base font to common UI keys
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
        UIManager.put("Table.font", base);
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("TextField.font", base);
        UIManager.put("Menu.font", base);

        // Apply light palette
        applyTheme();

        // Bigger tab look
        UIManager.put("TabbedPane.tabInsets", new Insets(16, 28, 16, 28));
        UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(16, 28, 16, 28));
        UIManager.put("TabbedPane.font", UIManager.getFont("Label.font").deriveFont(Font.BOLD, UIManager.getFont("Label.font").getSize2D() + 3f));

        ToolTipManager.sharedInstance().setInitialDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(12000);
    }

    private static Font tryLoadFontFromResources(String path, int style, float size) {
        try (InputStream in = UIUtils.class.getResourceAsStream(path)) {
            if (in == null) return null;
            Font f = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f.deriveFont(style, size);
        } catch (Exception ignored) { return null; }
    }

    private static String findAvailableFamily(String[] candidates) {
        try {
            String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (String pref : candidates) {
                for (String a : available) if (a.equalsIgnoreCase(pref)) return a;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Light-only palette application
    public static void applyTheme() {
        UIManager.put("Panel.background", Color.white);
        UIManager.put("Label.foreground", new Color(30,30,30));
        UIManager.put("Table.alternateRowColor", new Color(250, 250, 250));
        UIManager.put("ToolTip.background", new Color(255, 255, 240));
        UIManager.put("Button.background", new Color(245,245,245));
        UIManager.put("Menu.background", Color.white);
    }

    // Pastel card colours (light-only)
    public static Color cardBgIncome() { return new Color(240, 255, 245); }
    public static Color cardBgExpense() { return new Color(255, 245, 245); }
    public static Color cardBgNet() { return new Color(245, 248, 255); }

    public static Color topBarBackground() { return new Color(250,250,250); }

    // Rounded border for buttons/fields
    public static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final int radius;
        private final Color line;
        private final Insets insets;
        public RoundedBorder(int radius, Color line) { this(radius, line, new Insets(8,12,8,12)); }
        public RoundedBorder(int radius, Color line, Insets insets) { this.radius = radius; this.line = line; this.insets = insets == null ? new Insets(8,12,8,12) : insets; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(line != null ? line : new Color(230,230,230));
            g2.draw(new RoundRectangle2D.Float(x+0.5f, y+0.5f, width-1f, height-1f, radius, radius));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(insets.top, insets.left, insets.bottom, insets.right); }
        @Override public Insets getBorderInsets(Component c, Insets insets) { var i=getBorderInsets(c); insets.top=i.top; insets.left=i.left; insets.bottom=i.bottom; insets.right=i.right; return insets; }
    }

    // Panel with rounded corners and subtle shadow
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color shadow = new Color(0,0,0,28);
        public RoundedPanel(LayoutManager lm, int radius) { super(lm); this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // shadow
            g2.setColor(shadow);
            g2.fill(new RoundRectangle2D.Float(3, 5, w-6, h-6, radius+6, radius+6));
            // card
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 2, w-6, h-8, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void styleButton(AbstractButton b) {
        b.setFocusPainted(false);
        b.setBorderPainted(true);
        b.setOpaque(true);
        b.setBackground(UIManager.getColor("Button.background"));
        b.setForeground((Color)UIManager.get("Label.foreground"));
        b.setBorder(new RoundedBorder(20, new Color(220,220,220)));
    }

    // Try to load an icon from classpath (/icons/...) or from a local file path. Returns null if not found.
    public static ImageIcon loadIcon(String path) {
        try {
            java.net.URL r = UIUtils.class.getResource(path.startsWith("/") ? path : "/" + path);
            if (r != null) return new ImageIcon(r);
            java.io.File f = new java.io.File(path);
            if (f.exists()) return new ImageIcon(path);
        } catch (Exception ignored) {}
        return null;
    }

    public static JLabel titledLabel(String title) {
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, l.getFont().getSize2D() + 1f));
        return l;
    }

    // Rounded, shadowed card container
    public static JPanel card(String title, Color bg) {
        JPanel p = new RoundedPanel(new BorderLayout(), 20);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        p.setBackground(bg);
        JLabel t = new JLabel(title);
        t.setForeground((Color)UIManager.get("Label.foreground"));
        t.setFont(t.getFont().deriveFont(Font.BOLD));
        p.add(t, BorderLayout.NORTH);
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

    // Renderer that paints a JProgressBar inside a table cell. Expects value to be Number (percentage).
    public static class ProgressRenderer implements TableCellRenderer {
        private final JProgressBar bar = new JProgressBar(0, 100);
        public ProgressRenderer() {
            bar.setStringPainted(true);
            bar.setBorderPainted(false);
            bar.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            double pct = 0.0;
            if (value instanceof Number n) pct = n.doubleValue();
            int ipct = (int) Math.round(pct);
            bar.setValue(Math.max(0, Math.min(100, ipct)));

            // Determine model row in case the table is sorted/filtered
            int modelRow = row;
            try { modelRow = table.convertRowIndexToModel(row); } catch (Exception ignored) {}

            // Attempt to read limit and spent from model columns if present
            Object limitObj = null;
            Object spentObj = null;
            try {
                if (table.getModel().getColumnCount() > 2) {
                    limitObj = table.getModel().getValueAt(modelRow, 1);
                    spentObj = table.getModel().getValueAt(modelRow, 2);
                }
            } catch (Exception ignored) {}

            BigDecimal limit = null;
            BigDecimal spent = null;
            try {
                if (limitObj instanceof BigDecimal lb) limit = lb;
                else if (limitObj instanceof Number num) limit = new BigDecimal(num.toString());
            } catch (Exception ignored) {}
            try {
                if (spentObj instanceof BigDecimal sb) spent = sb;
                else if (spentObj instanceof Number num) spent = new BigDecimal(num.toString());
            } catch (Exception ignored) {}

            String label;
            if (spent != null && limit != null) {
                String sAmt = CURRENCY.format(spent);
                String lAmt = CURRENCY.format(limit);
                label = String.format("%s / %s (%d%%)", sAmt, lAmt, ipct);
                BigDecimal remaining = limit.subtract(spent);
                String remAmt = CURRENCY.format(remaining);
                bar.setToolTipText(String.format("Spent %s of %s — Remaining %s", sAmt, lAmt, remAmt));
            } else {
                label = ipct + "%";
                bar.setToolTipText(null);
            }
            bar.setString(label);

            // Color based on over/under 100%
            if (pct > 100.0) {
                bar.setForeground(Color.decode("#d64545")); // reddish
            } else {
                bar.setForeground(new Color(64, 160, 64)); // greenish
            }

            // Light theme background
            bar.setBackground(table.getBackground());
            return bar;
        }
    }

    // Badge renderer for TransactionType (Income/Expense)
    public static class TypeBadgeRenderer extends DefaultTableCellRenderer {
        private final Color incomeBg = new Color(210, 240, 225);
        private final Color incomeFg = new Color(24, 128, 56);
        private final Color expenseBg = new Color(250, 220, 220);
        private final Color expenseFg = new Color(178, 40, 48);
        public TypeBadgeRenderer() { setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String txt = String.valueOf(value);
            boolean income = txt != null && txt.toUpperCase().contains("INCOME");
            setText(income ? "Income" : "Expense");
            setBackground(income ? incomeBg : expenseBg);
            setForeground(income ? incomeFg : expenseFg);
            setFont(getFont().deriveFont(Font.BOLD));
            setBorder(new RoundedBorder(16, new Color(230,230,230)));
            return this;
        }
    }

    // Amount renderer that colours positive/negative (type-aware)
    public static class AmountRenderer extends DefaultTableCellRenderer {
        private final Color incomeFg = new Color(24, 128, 56);
        private final Color expenseFg = new Color(178, 40, 48);
        public AmountRenderer() { setHorizontalAlignment(SwingConstants.RIGHT); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String text = value instanceof BigDecimal bd ? CURRENCY.format(bd) : String.valueOf(value);
            setText(text);
            int modelRow = row;
            try { modelRow = table.convertRowIndexToModel(row); } catch (Exception ignored) {}
            Object typeObj = null;
            try { typeObj = table.getModel().getValueAt(modelRow, 1); } catch (Exception ignored) {}
            boolean income = String.valueOf(typeObj).toUpperCase().contains("INCOME");
            setForeground(income ? incomeFg : expenseFg);
            setFont(getFont().deriveFont(Font.BOLD));
            return this;
        }
    }

    public static class CardCellWrapperRenderer implements TableCellRenderer {
        public static final int FIRST = 0, MIDDLE = 1, LAST = 2;
        private final TableCellRenderer delegate;
        private final int position;
        private final Color cardBg = new Color(248, 250, 252);
        private final Color selBg = new Color(242, 248, 255);
        public CardCellWrapperRenderer(TableCellRenderer delegate, int position) {
            this.delegate = delegate; this.position = position;
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component inner = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(true);
            wrapper.setBackground(isSelected ? selBg : cardBg);
            if (inner instanceof JComponent jc) {
                // Keep chip background for Type badge only; otherwise let wrapper paint the background
                if (!(jc instanceof UIUtils.TypeBadgeRenderer)) jc.setOpaque(false);
            }
            wrapper.add(inner, BorderLayout.CENTER);

            int vpad = 4;
            int left = position == FIRST ? 8 : 4;
            int right = position == LAST ? 8 : 4;
            Border pad = BorderFactory.createEmptyBorder(vpad, left, vpad, right);
            if (position == FIRST || position == LAST) {
                // Use a rounded border with smaller insets so content isn't squashed
                Border rounded = new RoundedBorder(12, new Color(230,230,230), new Insets(3,6,3,6));
                pad = BorderFactory.createCompoundBorder(rounded, pad);
            }
            wrapper.setBorder(pad);
            return wrapper;
        }
    }

    // Persist/restore table column widths and sort keys using Preferences
    public static void saveTableState(JTable table, String key) {
        Preferences p = Preferences.userNodeForPackage(UIUtils.class).node("table." + key);
        var cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            p.putInt("w." + i, cm.getColumn(i).getWidth());
        }
        if (table.getRowSorter() != null) {
            List<? extends RowSorter.SortKey> keys = table.getRowSorter().getSortKeys();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                var k = keys.get(i);
                sb.append(k.getColumn()).append(":" ).append(k.getSortOrder().name());
                if (i < keys.size() - 1) sb.append(",");
            }
            p.put("sort", sb.toString());
        }
    }

    public static void loadTableState(JTable table, String key) {
        Preferences p = Preferences.userNodeForPackage(UIUtils.class).node("table." + key);
        var cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            int w = p.getInt("w." + i, -1);
            if (w > 10) cm.getColumn(i).setPreferredWidth(w);
        }
        if (table.getRowSorter() != null) {
            String s = p.get("sort", "");
            if (!s.isBlank()) {
                java.util.ArrayList<RowSorter.SortKey> keys = new java.util.ArrayList<>();
                for (String part : s.split(",")) {
                    String[] kv = part.split(":");
                    if (kv.length == 2) {
                        try {
                            int col = Integer.parseInt(kv[0]);
                            SortOrder order = SortOrder.valueOf(kv[1]);
                            keys.add(new RowSorter.SortKey(col, order));
                        } catch (Exception ignored) {}
                    }
                }
                if (!keys.isEmpty()) table.getRowSorter().setSortKeys(keys);
            }
        }
    }
}
