# StockChecker

An editable list of ticker symbols with a price label beside each. The list starts from `myStocks.txt`, and "refresh" downloads quotes from the Yahoo Finance CSV API, which has since been retired. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Text fields**: one ticker each, seeded from the comma-separated `myStocks.txt` (`crm,lmt,tsn,nflx,swk`). Labels start as `$--`.
- **addStock**: adds an empty field at the bottom, as long as the window still fits on screen.
- **deleteStock**: removes the last field.
- **COLOR!**: random background color and random label colors.
- **refresh**: fetches quotes for all fields and sets each label to `$` plus the last price. Any ticker longer than 5 characters is replaced with `MORETHAN5` first.
- Edits to the list are not saved back to `myStocks.txt`.
- The window is 330 px wide, not resizable, and grows 25 px taller per field.
- No command-line arguments.

## Code layout

- `src/stockchecker/Main.java`: entry point; creates the `StockChecker` and the `Window`.
- `src/stockchecker/Window.java`: the UI; builds the fields and buttons with a `SpringLayout` and handles the buttons.
- `src/stockchecker/StockChecker.java`: builds the Yahoo URL, downloads it to `stockCheck.csv` and parses it.
- `src/stockchecker/WebpageDownloader.java`: copies a URL's response into a file.
- `src/stockchecker/CSVReader.java`: reads the first 100 lines of a CSV into a `String[][]`.
- `src/stockchecker/TextReader.java`: reads a whole `.txt` file into a string.

## Running

From the repo root: `./run.ps1 StockChecker` (or `./run.ps1 StockChecker -Build` to build a jar).

The working directory must be `StockChecker/`, since `myStocks.txt` and `stockCheck.csv` are opened by relative path. A successful download overwrites `stockCheck.csv`. If the request throws (for example on an HTTP error status), the exception is printed, `stockCheck.csv` is left as it was, and the labels are filled from it anyway. The committed file holds quotes dated 5/13/2016.

## Known quirks

- The symbol list sent to Yahoo always has 100 entries: unused slots are `null`, so the URL contains `null,null,...` and ends with a comma. The committed `stockCheck.csv` shows the resulting `"null",N/A,...` rows.
- `CSVReader` reads exactly 100 lines. A file with fewer lines throws `NullPointerException` and the labels are not updated.
- If `myStocks.txt` is missing, the error is printed and the list starts with one empty field.
- The request uses plain `http://` and runs on the Swing event thread, so the window freezes until it finishes.
- Lots of debug output on stdout (sizes, counts, every CSV line).
- The empty-field check compares strings with `!=`, so it never skips anything.
