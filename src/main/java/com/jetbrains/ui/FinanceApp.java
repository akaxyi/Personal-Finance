package com.jetbrains.ui;

import com.jetbrains.finance.service.FinanceService;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Locale;

public class FinanceApp {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            UIUtils.initLookAndFeel();
            var dataPath = Path.of(System.getProperty("user.dir"), "finance-data.txt");
            var service = new FinanceService(dataPath);
            AppFrame frame = new AppFrame(service, YearMonth.now());
            frame.setVisible(true);
            JOptionPane.showMessageDialog(frame,
                    "Welcome!\n\nQuick Start:\n- Use Add Expense/Income on the top bar.\n- Set budgets in the Budgets tab.\n- Switch months with Prev/Next or Select Month.\n- Export CSV from the top bar.",
                    "Quick Start",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
