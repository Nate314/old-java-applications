# StockChecker

Originally an editable list of ticker symbols with a price label beside each, refreshed from the Yahoo Finance CSV API. That API has since been retired, so the app is now a practice trading desk: $10,000 of play money in a simulated market whose first day opens at the last real quotes the app saved (`stockCheck.csv`, dated 5/13/2016). Recovered from the git history of github.com/Nate314/site (`assets/javaApplications`).

## Controls / usage

- **Tickers** (top): the watch list, comma or space separated, seeded from `myStocks.txt` (`crm,lmt,tsn,nflx,swk`). **Enter** or **Watch** applies it and saves it back to `myStocks.txt`. Up to 8 tickers. A stock you still hold stays on the desk ("held, off list") until it is sold.
- **COLOR!**: a random dark background with a bright accent of the opposite hue, in the spirit of the original's random colors.
- **Header**: the day, the market clock (9:30 AM to 4:00 PM), OPEN/PAUSED/CLOSED, the speed buttons, cash, today's change and net worth.
  - **Pause** (Space toggles), **Play** (P, a trading day in about 2.5 minutes) and **Fast** (F, about 30 seconds).
- **Watch list** (left): each stock's price, change since the open, shares held and a sparkline of the day so far. Click a row, or use **Up**/**Down**, to pick it.
- **Chart** (right): the picked stock's day so far, with the open as a dashed line, price and time axes, and a green or red marker at each of your buys and sells.
- **Buy 1** (B), **Buy 10** (V), **Buy max** (M), **Sell 1** (S), **Sell 10** (D), **Sell all** (A): trade the picked stock at the current price, with no fees. You can only spend the cash you have. The line under the chart shows your position (shares, average cost, gain or loss) or how many shares your cash buys.
- **News** (bottom): the latest headline about the market or a stock on the desk. A headline comes with a push of 0.6% to 2.2% in that direction over the next 15 to 45 minutes. Under it, a ticker tape of every stock on the desk.
- **Closing bell**: at 4:00 PM a bell rings (tubular bells on the MIDI synthesizer) and the chart gives way to the day's results: net worth, the change today and since day 1, the best and worst stock, and a bar chart of net worth at every close so far (the last 14). **Next day** (Enter or N) opens the next day. **Start over** (X, press twice) goes back to day 1 with $10,000.
- The desk is saved in `~/.stockchecker-desk.txt` (on trades, speed changes, every 15 market minutes and at the close), so it carries on from the same minute next time.
- The window's content is 720 x 476 px and it is not resizable. No command-line arguments.

## The simulated market

- Each stock's whole day is a random walk worked out from the day number and its symbol, so a day replays the same after a reload, does not depend on which other stocks are on the desk, and is the same for everybody.
- Each stock has a daily volatility of 0.8% to 2% (from its symbol). The stocks share a market-wide move, and most days have one market-wide swing ("Stocks rally across the board").
- Day 1 opens at the price in `stockCheck.csv` for a symbol it has (CRM, LMT, TSN, NFLX, SWK), or a made-up steady price between $10 and $300 for any other. Each later day opens at the previous close plus an overnight gap of about 1%.
- The prices are not real and say nothing about the real companies; the desk says so along its bottom edge.

## Code layout

- `src/stockchecker/Main.java`: entry point; opens the `Window`.
- `src/stockchecker/Window.java`: the window: the ticker field, Watch and COLOR! across the top, the `DeskPanel` under them; reads `myStocks.txt` and the first prices from `stockCheck.csv`.
- `src/stockchecker/DeskPanel.java`: draws the desk and handles its clicks and keys; runs the market clock on a Swing timer.
- `src/stockchecker/Desk.java`: the watch list, today's market, the portfolio and the speed, and the `~/.stockchecker-desk.txt` file.
- `src/stockchecker/Market.java`: one simulated day of prices and headlines.
- `src/stockchecker/Portfolio.java`: cash, holdings with their cost, today's trades and the net worth at each close.
- `src/stockchecker/Money.java`: dollar and percent text.
- `src/stockchecker/Bell.java`: the opening and closing bell.
- `src/stockchecker/StockChecker.java`: from the original app: builds the retired Yahoo URL and downloads it to `stockCheck.csv` (no longer called), and looks up a symbol's price in the CSV rows (used for day 1).
- `src/stockchecker/WebpageDownloader.java`, `CSVReader.java`, `TextReader.java`: from the original app: copy a URL into a file, read a CSV into rows, read a `.txt` file.

## Running

From the repo root: `./run.ps1 StockChecker` (or `./run.ps1 StockChecker -Build` to build a jar).

The working directory must be `StockChecker/`, since `myStocks.txt` and `stockCheck.csv` are opened by relative path. If `myStocks.txt` is missing or empty, the desk watches the original five tickers. If `stockCheck.csv` is missing, every symbol starts at a made-up price.

## Self-check

`test/stockchecker/StockCheckerCheck.java` checks CSV parsing, symbol lookup and the quote URL. `test/stockchecker/DeskCheck.java` checks the market (replays the same, stays positive, news in time order and only about the stocks shown), buying and selling, the desk file across a reload and a close, Start over and the money text. From `StockChecker/`:

```
javac -d ../out/check/StockChecker src/stockchecker/*.java test/stockchecker/*.java
java -ea -cp ../out/check/StockChecker stockchecker.StockCheckerCheck
java -ea -cp ../out/check/StockChecker stockchecker.DeskCheck
```
