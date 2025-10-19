package com.jetbrains.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.geom.Path2D;
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
    public static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.UK);

    // Track base font size resolved at runtime for consistent sizing
    private static float BASE_FONT_SIZE = 13f;

    // Theme system
    public enum Theme { LIGHT, DARK, AKAXYI, CUSTOM }
    private static Theme CURRENT_THEME = Theme.LIGHT;

    private static Color customBg, customSurface, customText, customAccent, customAccent2;

    // Akaxyi palette
    private static final Color AK_BG = new Color(0x03, 0x19, 0x26);       // #031926 rich black
    private static final Color AK_SURFACE = new Color(0x9D, 0xBE, 0xBB);  // #9DBEBB ash gray
    private static final Color AK_ACCENT1 = new Color(0x46, 0x81, 0x89);  // #468189 teal
    private static final Color AK_ACCENT2 = new Color(0x77, 0xAC, 0xA2);  // #77ACA2 cambridge blue

    // Contrast helpers
    private static boolean isColorDark(Color c) {
        if (c == null) return false;
        double r = c.getRed() / 255.0;
        double g = c.getGreen() / 255.0;
        double b = c.getBlue() / 255.0;
        // linearize
        r = (r <= 0.04045) ? r/12.92 : Math.pow((r+0.055)/1.055, 2.4);
        g = (g <= 0.04045) ? g/12.92 : Math.pow((g+0.055)/1.055, 2.4);
        b = (b <= 0.04045) ? b/12.92 : Math.pow((b+0.055)/1.055, 2.4);
        double lum = 0.2126*r + 0.7152*g + 0.0722*b; // 0..1
        return lum < 0.5; // simple cutoff works well enough here
    }
    public static Color onColor(Color bg) {
        boolean dark = isColorDark(bg);
        return dark ? new Color(0xF2,0xF4,0xF8) : new Color(0x1E,0x1E,0x1E);
    }

    // Accent colors
    public static Color accent() {
        if (CURRENT_THEME == Theme.CUSTOM && customAccent != null) return customAccent;
        return switch (CURRENT_THEME) {
            case DARK -> new Color(88, 166, 255);
            case AKAXYI -> AK_ACCENT1;
            default -> new Color(66, 133, 244);
        };
    }
    public static boolean isDark() {
        if (CURRENT_THEME == Theme.DARK || CURRENT_THEME == Theme.AKAXYI) return true;
        if (CURRENT_THEME == Theme.CUSTOM && customBg != null) return isColorDark(customBg);
        return false;
    }

    // Palette helpers depending on theme (CUSTOM uses user-supplied values)
    public static Color panelBg() {
        if (CURRENT_THEME == Theme.CUSTOM && customBg != null) return customBg;
        return switch (CURRENT_THEME) {
            case DARK -> new Color(30, 33, 36);
            case AKAXYI -> AK_BG;
            default -> Color.white;
        };
    }
    public static Color surfaceBg() {
        if (CURRENT_THEME == Theme.CUSTOM && customSurface != null) return customSurface;
        return switch (CURRENT_THEME) {
            case DARK -> new Color(36, 39, 43);
            case AKAXYI -> AK_SURFACE;
            default -> new Color(248, 250, 252);
        };
    }
    public static Color labelFg() {
        if (CURRENT_THEME == Theme.CUSTOM && customText != null) return customText;
        // auto contrast based on panel background for general labels
        return onColor(panelBg());
    }
    public static Color subtleBorder() { return isDark() ? new Color(70, 74, 79) : new Color(230, 230, 230); }
    public static Color altRow() { return isDark() ? new Color(42, 46, 50) : new Color(250, 250, 250); }
    public static Color topBarBackground() { return surfaceBg(); }

    public static Color incomeFg() {
        if (CURRENT_THEME == Theme.CUSTOM && customAccent2 != null) return blend(customAccent2, Color.white, 0.15f);
        if (CURRENT_THEME == Theme.AKAXYI) return blend(AK_ACCENT2, Color.white, 0.15f);
        return isDark() ? new Color(144, 238, 144) : new Color(24, 128, 56);
    }
    public static Color expenseFg() { return isDark() ? new Color(255, 120, 120) : new Color(178, 40, 48); }
    public static Color chipIncomeBg() {
        if (CURRENT_THEME == Theme.CUSTOM && customAccent2 != null) return applyAlpha(customAccent2, 60);
        if (CURRENT_THEME == Theme.AKAXYI) return applyAlpha(AK_ACCENT2, 60);
        return isDark() ? new Color(30, 90, 60) : new Color(210, 240, 225);
    }
    public static Color chipExpenseBg() { return isDark() ? new Color(90, 45, 45) : new Color(250, 220, 220); }

    public static Color cardBgIncome() { return tint(surfaceBg(), CURRENT_THEME == Theme.CUSTOM && customAccent2 != null ? customAccent2 : (CURRENT_THEME == Theme.AKAXYI ? AK_ACCENT2 : new Color(120,210,180)), 0.08f); }
    public static Color cardBgExpense() { return tint(surfaceBg(), new Color(250, 200, 170), 0.08f); }
    public static Color cardBgNet() { return tint(surfaceBg(), accent(), 0.06f); }

    private static Color tint(Color base, Color tint, float amount) {
        float a = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(base.getRed() * (1-a) + tint.getRed() * a);
        int g = Math.round(base.getGreen() * (1-a) + tint.getGreen() * a);
        int b = Math.round(base.getBlue() * (1-a) + tint.getBlue() * a);
        return new Color(r,g,b);
    }
    private static Color blend(Color c1, Color c2, float amount) { return tint(c1, c2, amount); }
    private static Color applyAlpha(Color c, int alpha) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha))); }

    public static void setTheme(Theme t) {
        CURRENT_THEME = t == null ? Theme.LIGHT : t;
        // persist selection
        try {
            Preferences.userNodeForPackage(UIUtils.class).put("theme", CURRENT_THEME.name());
        } catch (Exception ignored) {}
        applyTheme();
    }

    public static void setCustomPalette(Color bg, Color surface, Color text, Color accent1, Color accent2) {
        customBg = bg; customSurface = surface; customText = text; customAccent = accent1; customAccent2 = accent2;
        try {
            Preferences p = Preferences.userNodeForPackage(UIUtils.class).node("palette");
            p.put("bg", toHex(bg)); p.put("surface", toHex(surface)); p.put("text", text != null ? toHex(text) : ""); p.put("accent1", toHex(accent1)); p.put("accent2", toHex(accent2));
        } catch (Exception ignored) {}
        setTheme(Theme.CUSTOM);
    }

    private static String toHex(Color c) { return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()); }
    private static Color parseHex(String s, Color def) {
        try {
            if (s == null || s.isBlank()) return def;
            s = s.trim();
            if (s.startsWith("#")) s = s.substring(1);
            if (s.length() == 6) {
                int r = Integer.parseInt(s.substring(0,2), 16);
                int g = Integer.parseInt(s.substring(2,4), 16);
                int b = Integer.parseInt(s.substring(4,6), 16);
                return new Color(r,g,b);
            }
        } catch (Exception ignored) {}
        return def;
    }

    public static Theme getSavedTheme() {
        try {
            String s = Preferences.userNodeForPackage(UIUtils.class).get("theme", Theme.LIGHT.name());
            Theme t = Theme.valueOf(s);
            if (t == Theme.CUSTOM) {
                Preferences p = Preferences.userNodeForPackage(UIUtils.class).node("palette");
                customBg = parseHex(p.get("bg", "#FFFFFF"), Color.white);
                customSurface = parseHex(p.get("surface", "#F6F7F9"), new Color(246,247,249));
                String text = p.get("text", "");
                customText = text.isBlank() ? null : parseHex(text, new Color(30,30,30));
                customAccent = parseHex(p.get("accent1", "#4285F4"), new Color(66,133,244));
                customAccent2 = parseHex(p.get("accent2", "#50C88C"), new Color(80,200,140));
            }
            return t;
        } catch (Exception ex) {
            return Theme.LIGHT;
        }
    }

    public static void initLookAndFeel() {
        Font base = tryLoadFontFromResources("/fonts/Inter-Regular.ttf", 0, 13f);
        Font baseBold = tryLoadFontFromResources("/fonts/Inter-Bold.ttf", Font.BOLD, 13f);

        if (base == null) {
            String chosen = findAvailableFamily(new String[]{"Segoe UI", "Tahoma", "Helvetica", "Arial", "SansSerif"});
            if (chosen == null) chosen = "SansSerif";
            BASE_FONT_SIZE = UIManager.getFont("Label.font").getSize2D() + 1.5f;
            base = new Font(chosen, Font.PLAIN, Math.round(BASE_FONT_SIZE));
            baseBold = base.deriveFont(Font.BOLD);
        } else {
            BASE_FONT_SIZE = base.getSize2D();
            if (baseBold == null) baseBold = base.deriveFont(Font.BOLD);
        }

        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
        UIManager.put("Table.font", base);
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("TextField.font", base);
        UIManager.put("Menu.font", base);

        // Ensure scroll and viewport match the panel background (avoid gray gaps)
        UIManager.put("ScrollPane.background", panelBg());
        UIManager.put("Viewport.background", panelBg());
        UIManager.put("TabbedPane.contentAreaColor", panelBg());
        UIManager.put("TabbedPane.background", surfaceBg());

        // Apply theme palette
        applyTheme();

        // Compact tab look
        UIManager.put("TabbedPane.tabInsets", new Insets(12, 20, 12, 20));
        UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(12, 20, 12, 20));
        UIManager.put("TabbedPane.font", UIManager.getFont("Label.font").deriveFont(Font.BOLD, UIManager.getFont("Label.font").getSize2D() + 1.5f));
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0,0,0,0));

        ToolTipManager.sharedInstance().setInitialDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(12000);

        // apply last saved theme if any (overrides defaults above)
        setTheme(getSavedTheme());
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

    // Apply palette to UIManager
    public static void applyTheme() {
        UIManager.put("Panel.background", panelBg());
        UIManager.put("Label.foreground", labelFg());
        UIManager.put("Table.alternateRowColor", altRow());
        UIManager.put("ToolTip.background", isDark() ? new Color(48, 50, 54) : new Color(255, 255, 240));
        UIManager.put("Button.background", surfaceBg());
        UIManager.put("Menu.background", panelBg());
        UIManager.put("Menu.foreground", labelFg());
        UIManager.put("ScrollPane.background", panelBg());
        UIManager.put("Viewport.background", panelBg());
        UIManager.put("TabbedPane.background", surfaceBg());
        UIManager.put("TabbedPane.contentAreaColor", panelBg());
        UIManager.put("TableHeader.background", surfaceBg());
        UIManager.put("TableHeader.foreground", onColor(surfaceBg()));
    }

    public static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final int radius;
        private final Color line;
        private final Insets insets;
        public RoundedBorder(int radius, Color line) { this(radius, line, new Insets(8,12,8,12)); }
        public RoundedBorder(int radius, Color line, Insets insets) { this.radius = radius; this.line = line; this.insets = insets == null ? new Insets(8,12,8,12) : insets; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(line != null ? line : subtleBorder());
            g2.draw(new RoundRectangle2D.Float(x+0.5f, y+0.5f, width-1f, height-1f, radius, radius));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(insets.top, insets.left, insets.bottom, insets.right); }
        @Override public Insets getBorderInsets(Component c, Insets insets) { var i=getBorderInsets(c); insets.top=i.top; insets.left=i.left; insets.bottom=i.bottom; insets.right=i.right; return insets; }
    }

    public static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color shadowLight = new Color(0,0,0,28);
        private final Color shadowDark = new Color(0,0,0,80);
        public RoundedPanel(LayoutManager lm, int radius) { super(lm); this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDark() ? shadowDark : shadowLight);
            g2.fill(new RoundRectangle2D.Float(3, 5, w-6, h-6, radius+6, radius+6));
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
        b.setForeground(onColor(b.getBackground()));
        b.setBorder(new RoundedBorder(20, subtleBorder()));
    }

    public static ImageIcon loadIcon(String path) {
        try {
            java.net.URL r = UIUtils.class.getResource(path.startsWith("/") ? path : "/" + path);
            if (r != null) return new ImageIcon(r);
            java.io.File f = new java.io.File(path);
            if (f.exists()) return new ImageIcon(path);
        } catch (Exception ignored) {}
        return null;
    }

    public static ImageIcon loadIconScaled(String path, int size) {
        ImageIcon base = loadIcon(path);
        if (base == null || base.getIconWidth() <= 0 || base.getIconHeight() <= 0) return base;
        int w = base.getIconWidth();
        int h = base.getIconHeight();
        float scale = (float) size / Math.max(w, h);
        int tw = Math.max(1, Math.round(w * scale));
        int th = Math.max(1, Math.round(h * scale));
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(tw, th, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(base.getImage(), 0, 0, tw, th, null);
        g2.dispose();
        return new ImageIcon(out);
    }

    public static JLabel titledLabel(String title) {
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, l.getFont().getSize2D() + 1f));
        l.setForeground(labelFg());
        return l;
    }

    public static JPanel card(String title, Color bg) {
        JPanel p = new RoundedPanel(new BorderLayout(), 20);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        p.setBackground(bg);
        JLabel t = new JLabel(title);
        t.setForeground(labelFg());
        t.setFont(t.getFont().deriveFont(Font.BOLD));
        p.add(t, BorderLayout.NORTH);
        return p;
    }

    public static class CurrencyRenderer extends DefaultTableCellRenderer {
        @Override protected void setValue(Object value) {
            if (value instanceof BigDecimal bd) setText(CURRENCY.format(bd));
            else super.setValue(value);
            setForeground(labelFg());
        }
    }

    public static class DateRenderer extends DefaultTableCellRenderer {
        @Override protected void setValue(Object value) {
            if (value instanceof LocalDate d) setText(DATE_FMT.format(d));
            else super.setValue(value);
            setForeground(labelFg());
        }
    }

    public static class ProgressRenderer implements TableCellRenderer {
        private final JProgressBar bar = new JProgressBar(0, 100);
        public ProgressRenderer() { bar.setStringPainted(true); bar.setBorderPainted(false); bar.setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            double pct = 0.0;
            if (value instanceof Number n) pct = n.doubleValue();
            int ipct = (int) Math.round(pct);
            bar.setValue(Math.max(0, Math.min(100, ipct)));
            bar.setForeground(pct > 100.0 ? new Color(214, 69, 69) : accent());
            bar.setBackground(isDark() ? surfaceBg() : table.getBackground());
            // label
            String label = ipct + "%";
            bar.setString(label);
            // tooltip
            BigDecimal spent = null, limit = null;
            try { spent = new BigDecimal(table.getValueAt(row, 2).toString()); } catch (Exception ignored) {}
            try { limit = new BigDecimal(table.getValueAt(row, 3).toString()); } catch (Exception ignored) {}
            if (spent != null && limit != null) {
                String sAmt = CURRENCY.format(spent);
                String lAmt = CURRENCY.format(limit);
                label = String.format("%s / %s (%d%%)", sAmt, lAmt, ipct);
                BigDecimal remaining = limit.subtract(spent);
                String remAmt = CURRENCY.format(remaining);
                // avoid unicode em-dash to prevent tofu boxes
                bar.setToolTipText(String.format("Spent %s of %s - Remaining %s", sAmt, lAmt, remAmt));
            } else {
                bar.setToolTipText(null);
            }
            return bar;
        }
    }

    public static class TypeBadgeRenderer extends DefaultTableCellRenderer {
        public TypeBadgeRenderer() { setHorizontalAlignment(SwingConstants.CENTER); setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String txt = String.valueOf(value);
            boolean income = txt != null && txt.toUpperCase().contains("INCOME");
            setText(income ? "Income" : "Expense");
            setBackground(income ? chipIncomeBg() : chipExpenseBg());
            setForeground(income ? incomeFg() : expenseFg());
            setFont(getFont().deriveFont(Font.BOLD));
            setBorder(new RoundedBorder(16, subtleBorder()));
            return this;
        }
    }

    public static class AmountRenderer extends DefaultTableCellRenderer {
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
            setForeground(income ? incomeFg() : expenseFg());
            setFont(getFont().deriveFont(Font.BOLD));
            return this;
        }
    }

    public static class CardCellWrapperRenderer implements TableCellRenderer {
        public static final int FIRST = 0, MIDDLE = 1, LAST = 2;
        private final TableCellRenderer delegate;
        private final int position;
        private final Color selBgLight = new Color(242, 248, 255);
        public CardCellWrapperRenderer(TableCellRenderer delegate, int position) { this.delegate = delegate; this.position = position; }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component inner = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(true);
            wrapper.setBackground(isSelected ? (isDark() ? new Color(50, 60, 72) : selBgLight) : surfaceBg());
            if (inner instanceof JComponent jc) { if (!(jc instanceof UIUtils.TypeBadgeRenderer)) jc.setOpaque(false); }
            wrapper.add(inner, BorderLayout.CENTER);
            int vpad = 4; int left = position == FIRST ? 8 : 4; int right = position == LAST ? 8 : 4;
            Border pad = BorderFactory.createEmptyBorder(vpad, left, vpad, right);
            if (position == FIRST || position == LAST) {
                Border rounded = new RoundedBorder(12, subtleBorder(), new Insets(3,6,3,6));
                pad = BorderFactory.createCompoundBorder(rounded, pad);
            }
            wrapper.setBorder(pad);
            return wrapper;
        }
    }

    // Glossy TabbedPane UI with proper clipping and separators
    public static class GlassTabbedPaneUI extends BasicTabbedPaneUI {
        @Override protected void installDefaults() { super.installDefaults(); tabAreaInsets = new Insets(6, 8, 6, 8); contentBorderInsets = new Insets(0,0,0,0); }
        @Override protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setClip(x, y, w, h);
            Color top = isSelected ? (isDark() ? new Color(46, 50, 56) : Color.white) : (isDark() ? new Color(40, 44, 50) : new Color(245, 246, 248));
            Color bottom = isSelected ? (isDark() ? new Color(36, 40, 46) : new Color(243, 246, 255)) : (isDark() ? new Color(34, 38, 43) : new Color(236, 238, 241));
            for (int i = 0; i < h; i++) {
                float t = i / (float) Math.max(1, h-1);
                int r = (int) (top.getRed() * (1-t) + bottom.getRed() * t);
                int b = (int) (top.getBlue() * (1-t) + bottom.getBlue() * t);
                int gr = (int) (top.getGreen() * (1-t) + bottom.getGreen() * t);
                g2.setColor(new Color(r, gr, b));
                g2.drawLine(x, y+i, x+w-1, y+i);
            }
            g2.setColor(new Color(255,255,255, isDark()?40:120));
            g2.fillRect(x+1, y+1, w-2, Math.max(1, h/3));
            g2.dispose();
        }
        @Override protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setClip(x, y, w, h);
            g2.setColor(subtleBorder());
            Path2D path = new Path2D.Double();
            path.moveTo(x, y+h);
            path.lineTo(x, y+6);
            path.quadTo(x, y, x+6, y);
            path.lineTo(x+w-6, y);
            path.quadTo(x+w, y, x+w, y+6);
            path.lineTo(x+w, y+h);
            g2.draw(path);
            if (isSelected) {
                g2.setClip(null);
                g2.setColor(accent());
                g2.fillRect(x+10, y+h-3, w-20, 3);
            }
            g2.dispose();
        }
        @Override protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) { }
        @Override protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(surfaceBg());
            g2.fillRect(0, 0, tabPane.getWidth(), tabPane.getHeight());
            g2.setColor(subtleBorder());
            // draw separators between tabs to avoid visual overlap
            for (int i = 0; i < rects.length - 1; i++) {
                int sx = rects[i].x + rects[i].width; int y = rects[i].y + 6; int y2 = rects[i].y + rects[i].height - 1;
                g2.drawLine(sx, y, sx, y2);
            }
            g2.dispose();
            super.paintTabArea(g, tabPlacement, selectedIndex);
        }
    }

    public static void styleTabbedPane(JTabbedPane tabs) {
        tabs.setUI(new GlassTabbedPaneUI());
        tabs.setOpaque(true);
        tabs.setBackground(surfaceBg());
        tabs.setForeground(labelFg());
        tabs.setBorder(BorderFactory.createEmptyBorder());
    }

    public static void styleTable(JTable table) {
        JTableHeaderRenderer headerRenderer = new JTableHeaderRenderer();
        java.awt.Component c = headerRenderer.getTableCellRendererComponent(table, "Sample", false, false, 0, 0);
        table.getTableHeader().setDefaultRenderer(headerRenderer);
        table.getTableHeader().setOpaque(true);
        table.getTableHeader().setBackground(surfaceBg());
        table.getTableHeader().setForeground(labelFg());
        table.setGridColor(subtleBorder());
    }

    private static class JTableHeaderRenderer extends DefaultTableCellRenderer {
        JTableHeaderRenderer() { setHorizontalAlignment(CENTER); setOpaque(true); }
        @Override public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setBackground(surfaceBg());
            setForeground(onColor(surfaceBg()));
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, subtleBorder()));
            return this;
        }
    }

    public static JScrollPane wrapTable(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(panelBg());
        sp.setBackground(panelBg());
        // Header corners to match header background (avoid gray strips)
        JComponent rightCorner = new JPanel();
        rightCorner.setOpaque(true);
        rightCorner.setBackground(surfaceBg());
        sp.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, rightCorner);
        JComponent leftCorner = new JPanel();
        leftCorner.setOpaque(true);
        leftCorner.setBackground(surfaceBg());
        sp.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, leftCorner);
        return sp;
    }

    // Persist table state helpers remain unchanged below
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

    public static void clearTableState(String key) {
        try {
            Preferences p = Preferences.userNodeForPackage(UIUtils.class);
            p.node("table." + key).removeNode();
            p.flush();
        } catch (Exception ignored) {}
    }
}
