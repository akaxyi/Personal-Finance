package com.jetbrains.ui;

import com.jetbrains.finance.service.FinanceService;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.YearMonth;
import java.util.Locale;

public class FinanceApp {
    public static void main(String[] args) {
        // Use UK locale so currency displays as £ and follows UK formatting
        Locale.setDefault(Locale.UK);
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            UIUtils.initLookAndFeel();
            var dataPath = defaultDataPath();
            var service = new FinanceService(dataPath);
            AppFrame frame = new AppFrame(service, YearMonth.now());
            frame.setVisible(true);
            JOptionPane.showMessageDialog(frame,
                    "Welcome!\n\nQuick Start:\n- Use Add Expense/Income on the top bar.\n- Set budgets in the Budgets tab.\n- Switch months with Prev/Next or Select Month.\n- Export CSV from the top bar.",
                    "Quick Start",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private static Path defaultDataPath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path base;
        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                base = Path.of(localAppData, "PersonalFinance");
            } else {
                base = Path.of(System.getProperty("user.home", "."), "AppData", "Local", "PersonalFinance");
            }
        } else if (os.contains("mac")) {
            base = Path.of(System.getProperty("user.home", "."), "Library", "Application Support", "PersonalFinance");
        } else {
            base = Path.of(System.getProperty("user.home", "."), ".local", "share", "personal-finance");
        }
        try {
            Files.createDirectories(base);
        } catch (Exception ignored) {}
        return base.resolve("finance-data.txt");
    }
}
