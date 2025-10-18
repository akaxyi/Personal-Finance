package com.jetbrains.finance.store;

import com.jetbrains.finance.model.FinanceData;
import com.jetbrains.finance.model.Transaction;
import com.jetbrains.finance.model.TransactionType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dependency-free plain-text store (INI-like) for FinanceData.
 * Format v1:
 *   [budgets]
 *   category|amount
 *   ...
 *   [transactions]
 *   type|date|amount|category|description
 *   ...
 * Strings escape: '\\' -> '\\\\', '|' -> '\\|'
 */
public class PlainTextStore {
    private final Path file;

    public PlainTextStore(Path file) {
        this.file = file;
    }

    public FinanceData load() {
        FinanceData data = new FinanceData();
        if (file == null || !Files.exists(file)) return data;
        String section = "";
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line;
                    continue;
                }
                if ("[budgets]".equals(section)) {
                    String[] parts = splitEscaped(line, '|', 2);
                    if (parts.length == 2) {
                        String cat = unescape(parts[0]);
                        BigDecimal amt = new BigDecimal(parts[1]);
                        data.getBudgets().put(cat, amt);
                    }
                } else if ("[transactions]".equals(section)) {
                    String[] parts = splitEscaped(line, '|', 5);
                    if (parts.length >= 5) {
                        TransactionType type = TransactionType.valueOf(parts[0]);
                        LocalDate date = LocalDate.parse(parts[1]);
                        BigDecimal amount = new BigDecimal(parts[2]);
                        String category = unescape(parts[3]);
                        String description = unescape(parts[4]);
                        data.getTransactions().add(new Transaction(type, date, amount, category, description));
                    }
                }
            }
        } catch (Exception ignored) {
            // On any parse error, return what we have (best effort)
        }
        return data;
    }

    public void save(FinanceData data) throws IOException {
        if (file == null) return;
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            bw.write("# finance-data v1\n");
            bw.write("[budgets]\n");
            for (Map.Entry<String, BigDecimal> e : data.getBudgets().entrySet()) {
                bw.write(escape(e.getKey()));
                bw.write('|');
                bw.write(e.getValue().toPlainString());
                bw.write('\n');
            }
            bw.write("[transactions]\n");
            for (Transaction t : data.getTransactions()) {
                bw.write(t.getType().name()); bw.write('|');
                bw.write(t.getDate().toString()); bw.write('|');
                bw.write(t.getAmount().toPlainString()); bw.write('|');
                bw.write(escape(nullToEmpty(t.getCategory()))); bw.write('|');
                bw.write(escape(nullToEmpty(t.getDescription()))); bw.write('\n');
            }
        }
    }

    public Path getFile() { return file; }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("|", "\\|");
    }

    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                out.append(c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String[] splitEscaped(String s, char sep, int expectedParts) {
        List<String> parts = new ArrayList<>(expectedParts);
        StringBuilder cur = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                cur.append(c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == sep) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }
}

