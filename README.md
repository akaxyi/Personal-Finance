# Personal Finance - Budgeting & Expense Tracker (Desktop GUI)

A tiny, dependency-free personal finance desktop app (Swing). It runs with just a JDK and stores data in a plain-text file (`finance-data.txt`) in your project folder.

What you can do:
- Create and update budget categories with monthly limits
- Record expenses and incomes
- Browse months: previous/next/select or list available months
- Edit and delete transactions
- View summary (income, expenses, net) and budget usage for the selected month
- Export the selected month to CSV (transactions-YYYY-MM.csv by default)
- Data is saved between runs in an easy-to-read text format

UI notes (new)
- Light-only, rounded, pastel aesthetic with clearer layout and larger primary numbers.
- A donut overview in Summary visualises Income vs Expenses with Net in the centre.
- Currency and formatting use UK conventions (pound symbol £ and UK number formatting).
- Toolbar and buttons have a flat, pill-shaped look for easier scanning.

## Requirements
- Windows
- JDK 17+ (javac/java available in PATH). Check with:

```bat
javac -version
java -version
```

## Run the Desktop App
Use the included batch script to compile and launch the GUI:

```bat
run.bat
```

GUI quick tour:
- Top bar: Previous/Next month, Select Month, Add Expense/Income, Export CSV, Save
- Summary tab: cards for Income/Expenses/Net + a donut chart and a budgets table with progress bars
- Transactions: list, add, edit, delete
- Budgets: manage categories and monthly limits

All actions save to `finance-data.txt` (in the project root). CSV export writes a file like `transactions-YYYY-MM.csv`.

## Data format
The app uses a simple, dependency-free plain-text format:

```
# finance-data v1
[budgets]
Food|250.00
Transport|100.00
[transactions]
EXPENSE|2025-10-18|15.75|Food|Lunch
INCOME|2025-10-15|2000.00|INCOME|Salary
```

- `|` and `\` are escaped inside text.
- You can back up or edit this file by hand if needed.

## Troubleshooting
- "Compile failed" in run.bat: ensure a JDK is installed and `javac -version` works.
- If you see date/number format errors, check you typed the date as `YYYY-MM-DD` and amounts like `123.45`.
- Data not saving: use the Save button in the GUI.
- CSV not created: use Export CSV (top bar) and provide a valid filename or accept the default.

## Notes
- The app entry point is `com.jetbrains.ui.FinanceApp`. Running `com.jetbrains.Main` also starts the GUI.
- No external libraries are used. All persistence is plain-text for easy portability.

## Next steps (optional)
- Export summaries (budgets and totals) across multiple months
- Category deletions and renames affecting historical data
- Packaging into a runnable JAR or installer
