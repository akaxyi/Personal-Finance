# Personal Finance - Budgeting & Expense Tracker (Windows Desktop)

A tiny, dependency-free desktop app to track income and expenses. No installation or Java required.

## Download
- Direct download (current build): [dist/PersonalFinance.zip](dist/PersonalFinance.zip)
- If the link above doesn’t work, open this repository’s Releases page and download `PersonalFinance.zip`.

## Run it
1) Download the ZIP above.
2) Right‑click the ZIP and choose “Extract All…”.
3) Open the extracted folder and double‑click: `PersonalFinance\PersonalFinance.exe`.

Tips:
- On first run, Windows SmartScreen may warn you. Click “More info” > “Run anyway”.
- The app includes its own private Java runtime, so you don’t need to install anything else.

## What you can do
- Create and update budget categories with monthly limits
- Record expenses and incomes
- Browse months: previous/next/select or list available months
- Edit and delete transactions
- View summary (income, expenses, net) and budget usage for the selected month
- Export the selected month to CSV (transactions-YYYY-MM.csv by default)
- Data is saved between runs in a simple text file

## UI notes
- Light and dark themes, plus a custom palette.
- Donut overview in Summary visualises Income vs Expenses with Net in the centre.
- Currency and formatting use UK conventions (pound symbol £ and UK number formatting).
- Toolbar and buttons have a flat, pill-shaped look for easier scanning.

## Where your data is stored
Your data is kept locally on your computer:
- Windows: `%LOCALAPPDATA%\PersonalFinance\finance-data.txt`

You can open the data folder from the app via: File > Open Data Folder. CSV exports default to your Documents (or Downloads) folder and you can change the path when prompted.

If you used an older version that saved `finance-data.txt` in the project folder and want to re-use it, copy that file into the folder above.

## Data format (for reference)
The app uses a simple plain-text format:

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

## Troubleshooting
- SmartScreen blocked it: click “More info” > “Run anyway”.
- CSV not created: use Export CSV and choose a folder you can write to (Documents works well).
- Date/amount format: dates as `YYYY-MM-DD` and amounts like `123.45`.

## Privacy
- Your data stays on your machine in the file listed above. No cloud or external services are used.
