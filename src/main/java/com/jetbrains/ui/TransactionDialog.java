package com.jetbrains.ui;

import com.jetbrains.finance.model.TransactionType;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDialog extends JDialog {
    private final JComboBox<TransactionType> typeCombo = new JComboBox<>(TransactionType.values());
    private final JTextField dateField = new JTextField(10); // YYYY-MM-DD
    private final JTextField amountField = new JTextField(10);
    private final JTextField categoryField = new JTextField(16);
    private final JTextField descField = new JTextField(24);

    private boolean ok = false;

    public TransactionDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4); gc.anchor = GridBagConstraints.WEST;
        int r = 0;
        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Type:"), gc);
        gc.gridx = 1; form.add(typeCombo, gc); r++;

        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Date (YYYY-MM-DD):"), gc);
        gc.gridx = 1; form.add(dateField, gc); r++;

        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Amount:"), gc);
        gc.gridx = 1; form.add(amountField, gc); r++;

        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Category (for Expense):"), gc);
        gc.gridx = 1; form.add(categoryField, gc); r++;

        gc.gridx = 0; gc.gridy = r; form.add(new JLabel("Description:"), gc);
        gc.gridx = 1; form.add(descField, gc); r++;

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");
        buttons.add(okBtn); buttons.add(cancelBtn);

        okBtn.addActionListener(e -> { if (validateInputs()) { ok = true; dispose(); } });
        cancelBtn.addActionListener(e -> dispose());
        typeCombo.addActionListener(e -> updateCategoryEnabled());
        updateCategoryEnabled();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private void updateCategoryEnabled() {
        boolean exp = getTxnType() == TransactionType.EXPENSE;
        categoryField.setEnabled(exp);
    }

    private boolean validateInputs() {
        try { LocalDate.parse(dateField.getText().trim()); } catch (Exception e) { showErr("Invalid date."); return false; }
        try {
            BigDecimal a = new BigDecimal(amountField.getText().trim());
            if (a.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException();
        } catch (Exception e) { showErr("Invalid amount."); return false; }
        if (getTxnType() == TransactionType.EXPENSE && (getCategory() == null || getCategory().isBlank())) {
            showErr("Category required for expense."); return false;
        }
        return true;
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void preset(TransactionType type, LocalDate date, BigDecimal amount, String category, String desc) {
        typeCombo.setSelectedItem(type);
        dateField.setText(date == null ? "" : date.toString());
        amountField.setText(amount == null ? "" : amount.toPlainString());
        categoryField.setText(category == null ? "" : category);
        descField.setText(desc == null ? "" : desc);
        updateCategoryEnabled();
    }

    public boolean isOk() { return ok; }
    public TransactionType getTxnType() { return (TransactionType) typeCombo.getSelectedItem(); }
    public LocalDate getDate() { return LocalDate.parse(dateField.getText().trim()); }
    public BigDecimal getAmount() { return new BigDecimal(amountField.getText().trim()); }
    public String getCategory() { return categoryField.getText().trim(); }
    public String getDescription() { return descField.getText().trim(); }
}
