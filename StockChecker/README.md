# StockChecker

An editable list of ticker symbols with a price label beside each. The list starts from `myStocks.txt`, and "refresh" downloads quotes from the Yahoo Finance CSV API, which has since been retired. Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Text fields**: one ticker each, seeded from the comma-separated `myStocks.txt` (`crm,lmt,tsn,nflx,swk`). Labels start as `$--`.
- **addStock**: adds an empty field at the bottom, as long as the window still fits on screen.
- **deleteStock**: removes the last field.
- **COLOR!**: random background color and random label colors.
- **refresh** (or **Enter** in a ticker field): fetches quotes for the non-empty fields in the background and sets each label to `$` plus the last price, matched by symbol, or `$--` if there is no quote for it. The button reads "loading..." while the request runs.
- The list is saved back to `myStocks.txt` on add, delete and refresh.
- If `myStocks.txt` is missing, the list starts with one empty field.
- The window is 330 px wide, not resizable, and grows 25 px taller per field.
- No command-line arguments.

## Code layout

- `src/stockchecker/Main.java`: entry point; creates the `StockChecker` and the `Window`.
- `src/stockchecker/Window.java`: the UI; builds the fields and buttons with a `SpringLayout` and handles the buttons.
- `src/stockchecker/StockChecker.java`: builds the Yahoo URL, downloads it to `stockCheck.csv`, parses it and looks up a symbol's price.
- `src/stockchecker/WebpageDownloader.java`: copies a URL's response into a file (10 second timeouts).
- `src/stockchecker/CSVReader.java`: reads every line of a CSV into a list of rows.
- `src/stockchecker/TextReader.java`: reads a whole `.txt` file into a string.

## Running

From the repo root: `./run.ps1 StockChecker` (or `./run.ps1 StockChecker -Build` to build a jar).

The working directory must be `StockChecker/`, since `myStocks.txt` and `stockCheck.csv` are opened by relative path. A successful download overwrites `stockCheck.csv`. If the request fails (for example on an HTTP error status), the error is printed, `stockCheck.csv` is left as it was, and the labels are filled from it anyway. The committed file holds quotes dated 5/13/2016. The request now uses `https://`; the API is retired either way.

## Self-check

`test/stockchecker/StockCheckerCheck.java` checks CSV parsing, symbol lookup and the quote URL. From `StockChecker/`:

```
javac -d ../out/check/StockChecker src/stockchecker/*.java test/stockchecker/*.java
java -ea -cp ../out/check/StockChecker stockchecker.StockCheckerCheck
```
