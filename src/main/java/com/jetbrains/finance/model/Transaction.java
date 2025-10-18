package com.jetbrains.finance.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private TransactionType type;
    private LocalDate date;
    private BigDecimal amount;
    private String category; // for EXPENSE only; INCOME uses "INCOME"
    private String description;

    public Transaction() {
    }

    public Transaction(TransactionType type, LocalDate date, BigDecimal amount, String category, String description) {
        this.type = type;
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.description = description;
    }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
