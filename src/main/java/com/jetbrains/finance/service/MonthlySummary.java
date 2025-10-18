package com.jetbrains.finance.service;

import java.math.BigDecimal;

public record MonthlySummary(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal net) {}

