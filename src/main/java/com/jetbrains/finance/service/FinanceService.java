package com.jetbrains.finance.service;

import com.jetbrains.finance.model.FinanceData;
import com.jetbrains.finance.model.Transaction;
import com.jetbrains.finance.model.TransactionType;
import com.jetbrains.finance.store.PlainTextStore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class FinanceService {
    private final PlainTextStore store;
    private final FinanceData data;

    public FinanceService(Path file) {
        this.store = new PlainTextStore(file);
        this.data = store.load();
    }

    public void save() throws IOException { store.save(data); }

    public Map<String, BigDecimal> getBudgets() {
        return new TreeMap<>(data.getBudgets());
    }

    public void setBudget(String category, BigDecimal limit) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(limit, "limit");
        if (limit.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Budget must be >= 0");
        data.getBudgets().put(category, limit);
    }

    public void removeBudget(String category) {
        Objects.requireNonNull(category, "category");
        data.getBudgets().remove(category);
    }

    public java.nio.file.Path getDataFile() {
        return this.store.getFile();
    }

    public void addTransaction(TransactionType type, LocalDate date, BigDecimal amount, String category, String description) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(amount, "amount");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be > 0");
        if (type == TransactionType.EXPENSE && (category == null || category.isBlank())) {
            throw new IllegalArgumentException("Expense requires category");
        }
        if (type == TransactionType.INCOME) {
            category = "INCOME";
        }
        var t = new Transaction(type, date, amount, category, description);
        data.getTransactions().add(t);
        data.getTransactions().sort(Comparator.comparing(Transaction::getDate));
    }

    public List<Transaction> getTransactionsForMonth(YearMonth ym) {
        return data.getTransactions().stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(ym))
                .sorted(Comparator.comparing(Transaction::getDate))
                .collect(Collectors.toList());
    }

    public int getMonthTransactionCount(YearMonth ym) {
        return (int) data.getTransactions().stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(ym))
                .count();
    }

    public MonthlySummary getMonthlySummary(YearMonth ym) {
        var txns = getTransactionsForMonth(ym);
        BigDecimal income = txns.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = txns.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthlySummary(income, expense, income.subtract(expense));
    }

    public Map<String, BigDecimal> getSpentByCategory(YearMonth ym) {
        var txns = getTransactionsForMonth(ym);
        Map<String, BigDecimal> byCat = txns.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(Transaction::getCategory,
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        return new TreeMap<>(byCat);
    }

    public Set<YearMonth> getAvailableMonths() {
        return data.getTransactions().stream()
                .map(t -> YearMonth.from(t.getDate()))
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean deleteTransactionAt(YearMonth ym, int oneBasedIndex) {
        int idx = mapMonthIndexToGlobalIndex(ym, oneBasedIndex);
        if (idx < 0) return false;
        data.getTransactions().remove(idx);
        return true;
    }

    public boolean editTransactionAt(YearMonth ym, int oneBasedIndex, TransactionType newType, LocalDate newDate,
                                     BigDecimal newAmount, String newCategory, String newDescription) {
        int idx = mapMonthIndexToGlobalIndex(ym, oneBasedIndex);
        if (idx < 0) return false;
        Transaction t = data.getTransactions().get(idx);
        if (newType != null) t.setType(newType);
        if (newDate != null) t.setDate(newDate);
        if (newAmount != null) t.setAmount(newAmount);
        if (t.getType() == TransactionType.INCOME) {
            t.setCategory("INCOME");
        } else {
            // For EXPENSEs: allow updating category when provided; otherwise keep existing
            if (newCategory != null) {
                if (newCategory.isBlank()) throw new IllegalArgumentException("Category required for expenses");
                t.setCategory(newCategory);
            }
            // Validate we have a category for expenses
            if (t.getCategory() == null || t.getCategory().isBlank()) {
                throw new IllegalArgumentException("Category required for expenses");
            }
        }
        if (newDescription != null) t.setDescription(newDescription);
        data.getTransactions().sort(Comparator.comparing(Transaction::getDate));
        return true;
    }

    private int mapMonthIndexToGlobalIndex(YearMonth ym, int oneBasedIndex) {
        if (oneBasedIndex <= 0) return -1;
        List<Integer> globalIdxs = new ArrayList<>();
        for (int i = 0; i < data.getTransactions().size(); i++) {
            Transaction t = data.getTransactions().get(i);
            if (YearMonth.from(t.getDate()).equals(ym)) {
                globalIdxs.add(i);
            }
        }
        if (oneBasedIndex > globalIdxs.size()) return -1;
        return globalIdxs.get(oneBasedIndex - 1);
    }

    public Path exportCsv(Path file, YearMonth ym) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            bw.write("type,date,amount,category,description\n");
            for (Transaction t : getTransactionsForMonth(ym)) {
                bw.write(csv(t.getType().name())); bw.write(',');
                bw.write(csv(t.getDate().toString())); bw.write(',');
                bw.write(csv(t.getAmount().toPlainString())); bw.write(',');
                // Guard against CSV formula injection for user-entered fields
                bw.write(csv(formulaSafe(t.getCategory() == null ? "" : t.getCategory()))); bw.write(',');
                bw.write(csv(formulaSafe(t.getDescription() == null ? "" : t.getDescription()))); bw.write('\n');
            }
        }
        return file;
    }

    private static String csv(String s) {
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"")) {
            return '"' + v + '"';
        }
        return v;
    }

    private static String formulaSafe(String s) {
        if (s == null || s.isEmpty()) return "";
        String trimmed = trimLeadingSpaces(s);
        if (!trimmed.isEmpty()) {
            char c = trimmed.charAt(0);
            if (c == '=' || c == '+' || c == '-' || c == '@') {
                return "'" + s; // prefix apostrophe to render as literal in Excel/Sheets
            }
        }
        return s;
    }

    private static String trimLeadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(i);
    }
}
