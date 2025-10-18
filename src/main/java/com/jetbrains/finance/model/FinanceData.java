package com.jetbrains.finance.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinanceData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, BigDecimal> budgets = new HashMap<>();
    private List<Transaction> transactions = new ArrayList<>();

    public FinanceData() {}

    public Map<String, BigDecimal> getBudgets() { return budgets; }
    public void setBudgets(Map<String, BigDecimal> budgets) { this.budgets = budgets; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}

