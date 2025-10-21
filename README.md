# Personal Finance - Budgeting & Expense Tracker (Desktop GUI)

A tiny, dependency-free personal finance desktop app (Swing). You can package it as a native Windows installer or as a portable folder that includes a private JRE—so end users don't need Java or Maven.

## Download
- Direct download (current build in this repo): [dist/PersonalFinance.zip](dist/PersonalFinance.zip)
- Windows installer (recommended): see this repository’s Releases page and download the latest `PersonalFinance-Setup-<version>.exe`.
- Portable ZIP (no install): on the Releases page, download `PersonalFinance.zip`, extract it, and run `PersonalFinance\PersonalFinance.exe`.

Maintainers: to produce and upload the ZIP locally, run:

```bat
zip-portable.bat
```

This creates `dist\PersonalFinance.zip`. Attach that ZIP (and the installer from `dist\`) to a new GitHub Release, then these download instructions will apply to end users.

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

If you received a portable ZIP:
- Extract it and run `PersonalFinance\PersonalFinance.exe` inside the extracted folder.
- This bundle already includes a JRE.

## Builders: create an installer/portable app (Windows)
Prerequisites to build locally:
- Windows 10/11
- JDK 17+ that includes `jpackage` in `bin` (for example, Temurin 17+ from adoptium.net)
- Optional: Maven 3.x (the scripts try Maven first and fall back to a simple packager)

Build a native installer (.exe, bundled JRE):

```bat
build-installer.bat
```

Build a portable app image (folder with .exe + bundled JRE):

```bat
build-portable.bat
```

Zip the portable app (creates `dist\PersonalFinance.zip`):

```bat
zip-portable.bat
```

Output appears under `dist/`.

Notes:
- If Maven is present, we build the app JAR via `mvn package`. If not, the scripts fall back to a simple packager.
- If you run into issues in PowerShell, run the scripts from Command Prompt (cmd.exe).

## Developers: run from source
Requirements:
- JDK 17+

Run the app from source with the included batch script:

```bat
run.bat
```

Or with Maven:

```bat
mvn -q -DskipTests package
javaw -jar target\app-1.0-SNAPSHOT.jar
```

## Data storage location
By default, data is stored in a per-user app data folder (created automatically):
- Windows: `%LOCALAPPDATA%\PersonalFinance\finance-data.txt`
- macOS: `~/Library/Application Support/PersonalFinance/finance-data.txt`
- Linux: `~/.local/share/personal-finance/finance-data.txt`

You can open the data folder from the app: File > Open Data Folder. CSV exports default to your Documents (or Downloads) folder and you can change the path when prompted.

If you used an older version that saved `finance-data.txt` in the project folder and want to re-use it, copy that file into the new data folder above.

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
- If packaging scripts fail in PowerShell, try from Command Prompt (cmd.exe).
- To build an installer, ensure your JDK includes `jpackage` and it's first in PATH.
- If you see date/number format errors, type dates as `YYYY-MM-DD` and amounts like `123.45`.
- CSV not created: use Export CSV and provide a valid path; the default is in your Documents/Downloads.

## Notes
- The app entry point is `com.jetbrains.ui.FinanceApp`. Running `com.jetbrains.Main` also starts the GUI.
- No external libraries are used. All persistence is plain-text for easy portability.

## Next steps (optional)
- Automate Releases (CI) to build and attach the installer and ZIP on tag
- Export summaries (budgets and totals) across multiple months
- Category deletions and renames affecting historical data
