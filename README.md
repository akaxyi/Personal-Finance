# Personal Finance - Budgeting & Expense Tracker (Desktop GUI)

A tiny, dependency-free personal finance desktop app (Swing). You can package it as a native Windows installer or as a portable folder that includes a private JRE—so end users don't need Java or Maven.

What you can do:
- Create and update budget categories with monthly limits
- Record expenses and incomes
- Browse months: previous/next/select or list available months
- Edit and delete transactions
- View summary (income, expenses, net) and budget usage for the selected month
- Export the selected month to CSV (transactions-YYYY-MM.csv by default)
- Data is saved between runs in a simple text file

UI notes
- Light and dark themes, plus a custom palette.
- Donut overview in Summary visualises Income vs Expenses with Net in the centre.
- Currency and formatting use UK conventions (pound symbol £ and UK number formatting).
- Toolbar and buttons have a flat, pill-shaped look for easier scanning.

## End users: install and run (no Java required)
If you have an installer (.exe) built for you:
- Double-click the installer and follow the prompts.
- Launch "PersonalFinance" from the Start Menu or desktop shortcut.
- Your data file is stored here on Windows: `%LOCALAPPDATA%\PersonalFinance\finance-data.txt`.

