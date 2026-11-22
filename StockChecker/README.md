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
- Each trade button says under its name what it would do right now: the cost or the money back ("$763.20"), how many shares Buy max and Sell all would trade ("131 shares"), "only 3" when the cash or the shares held cover fewer, or "no cash" and "none held", faded, when it would trade nothing. Pointing at one spells it out on the line under the chart: "Buys 10 CRM for $763.20, leaving $9,236.80 cash." or, for a sale, the gain or loss on what those shares cost ("Sells 10 CRM for $790.00: +$26.80 (+3.51%) on what you paid."). After a trade that line says what it came to in all.
- **Chart** / **Days** / **Race** (tabs at the top right of the chart, C, W and R; R toggles): the Days tab shows the picked stock day by day (see below), the Race tab how you are doing against four computer traders (see below).
- **Header rank**: "2nd of 5" beside your net worth is your place among you and the four rivals by net worth.
- **News** (bottom): the latest headline about the market or a stock on the desk. A headline comes with a push of 0.6% to 2.2% in that direction over the next 15 to 45 minutes. Under it, a ticker tape of every stock on the desk.
- **Closing bell**: at 4:00 PM a bell rings (tubular bells on the MIDI synthesizer) and the chart gives way to the day's results: net worth, the change today and since day 1, the best and worst stock, and a bar chart of net worth at every close so far (the last 14). **Next day** (Enter or N) opens the next day. **Start over** (X, press twice) goes back to day 1 with $10,000.
- The desk is saved in `~/.stockchecker-desk.txt` (on trades, speed changes, every 15 market minutes and at the close), so it carries on from the same minute next time. The rivals are saved there too (`rival=`, `rhold=`, `rtrade=` and `rclose=` lines), and so are the Days tab's candles (`bar=` lines).

## Order lines

You do not have to watch every minute: draw a line on the chart and the desk trades when the price gets there.

- **Click the chart** at a price to put a line there. While you hold none of the stock, a line is a buy: below the price it buys the dip, above it buys the breakout. While you hold some, a line is a sell: below the price it is a stop, above it a target. Pointing at the chart previews the line and what it would do ("Click: Buy max if CRM falls to $76.14").
- Each stock has at most one line above the price and one below; a new click on that side moves it. Click a line (or point within 5 px of it) to cancel it. **-** puts the line below the price 1% under it (or moves it 1% further down), **=** does the same above, and **O** cancels the picked stock's lines.
- A buy line spends all the cash it can and a sell line sells every share held, at the price of the first minute that reaches the line (so a fast drop can fill under a buy line or a stop). The bell rings twice, the line under the chart says what was traded, and the stock's other line is cancelled, so a stop and a target work as a pair.
- Lines are green for a buy and red for a sell, each with a tag saying what it does, and the chart's scale stretches to keep them in view. Selling every share by hand drops the stock's sell lines, a stock that leaves the desk takes its lines with it, and lines last only the day: they are gone at the next open. They are saved in the desk file as `order=` lines.

## Reading the chart

The chart says why a stock moved and what it cost at any minute, not only where it is now.

- **News on the chart**: every headline that moved the picked stock so far today, its own or the whole market's ("Stocks slide across the board"), puts a small arrow at the foot of the chart at its minute (green pointing up, red pointing down) and shades the minutes it has pushed the price so far, in the same color. So a climb that starts at a green arrow was the news, and a red band over a green one shows the market dragging a stock down against its own good news. The band grows only up to now, so it does not give away how long the push will last.
- **Point at the chart** to read it: a line down the chart at the minute under the pointer, a dot on the price, and a tag along the top with the time, the price and the change since the open ("12:49 PM  $79.99  -1.00%"). Within 4 minutes of a news arrow it jumps to it and the tag reads the headline instead ("10:43 AM  CRM climbs on heavy volume"). Clicking still puts an order line, as before.

## Hear the chart

The chart can be listened to as well as looked at, so you can follow a stock without watching it, or without seeing the screen at all.

- **Hear** (H, at the chart's bottom right corner) plays the picked stock's day so far, from the open to now, in about three seconds: one note at a time, higher for a higher price on the chart's own scale (the bottom of the chart is C3, the top C6), so the tune rises and falls with the line. A chime sounds at each of your trades in it, high for a buy and low for a sell. A playhead runs across the chart with the time and price it is playing. H again stops it, as does picking another stock, the Race tab or the closing bell.
- **L** listens live: while the market runs, a short note each market minute for the picked stock's price, on the same scale. L again stops.
- For screen readers, the desk's accessible name says the day, the time, whether the market is open, the picked stock's price and change, the shares you hold, your net worth and your place, and that H and L listen.
- An order line's tag slides right past any trade marker it would cover, such as a rival's dot at 9:30 at the chart's left edge.

## Hindsight

Every chart is easy to trade after the fact. **Hindsight** (I, beside Hear under the chart) shows it: the best single buy and later sell of the picked stock from the open to now, so you can see what the day offered and how close your own trades came.

- On the Chart tab, a gold ring at the buy and at the sell, a dashed line between them and the price span between them shaded, with a tag at the chart's bottom left: "Hindsight: 10:21 AM $75.67 to 12:14 PM $76.76, +1.44%". It follows the market as it runs, so a new low or high moves it. "No rise to catch yet" while the stock has only fallen since the open.
- Turning it on says what that trade would have made with all your money in it (your net worth at the open). I again hides it.
- At the closing bell, the results show the day's best trade of all the stocks on the desk: "In hindsight, NFLX's best trade today: buy at 10:28 AM for $147.14, sell at 3:58 PM for $152.67, +3.76%, about +$375.83 with all your money in it."
- While it is on, the desk's screen reader name says the same for the picked stock.

## Days

The Chart tab shows one day; the **Days** tab (W) shows the picked stock across the days you have played, so you can see whether a dip today is part of a slide or a blip in a climb.

- One candle per day, oldest at the left, today's at the right, outlined and still moving while the market is open: the body runs from the day's open to its close (green when it closed higher, red when lower) and the thin wick from its low to its high. Up to 20 days show (today and the 19 before it), labelled D1, D2 and so on underneath (every other day when they would not fit).
- Beside the price, the change from the open of the first day shown to now ("+4.15% since day 1"; just today's change while today is the only day).
- While you hold the stock, a dashed line marks your average cost ("You paid $79.66 a share"), so you can see at a glance how far the price has run from it.
- Point at a candle for that day's change, open, high, low and close (for today: "Today so far", and the price now). The Days tab takes no clicks; order lines are put on the Chart tab.
- It works after the closing bell too, with the finished day as the last candle. The screen reader name of the desk adds the change shown while the tab is open.
- Each closing bell keeps every stock on the desk's candle in the desk file, as `bar=` lines (the last 19 per stock). The candles start with the first close after this change: a desk saved before it shows only the days closed since. Start over clears them. A stock that was off the desk for some days simply has no candles for them, so its D labels skip.

## The rivals

Four computer traders start with $10,000 each and trade the same market, the same minutes, at the same prices as you. Each follows one plain rule, decided only from the prices and headlines up to that minute, so their trades are the same for everybody and the rule is all there is to beat:

- **Hal** (holds): at the first minute of each day (and whenever he holds nothing, as when he joins), sells any stock that has left your watch list and splits his cash evenly over the listed stocks he does not hold yet; then he holds. He is the "just buy the list" benchmark.
- **Mo** (momentum): every 10 minutes from 10:00 AM, buys as much as he can of the stock up most over the last half hour, if it is up at least 0.4% and he has not sold it in the last half hour; sells when it falls 0.6% below its high since he bought.
- **Val** (bargains): every 5 minutes from 9:50 AM, buys as much as she can of the stock down most since the open, if it is down at least 1.2% and she has not sold it in the last half hour; sells when it is back at its open, or when it falls 1.5% below what she paid.
- **Nell** (news): 10 minutes after a good headline about a stock on the list, buys as much as she can of it; sells 30 minutes after buying, or at once on a bad headline about it.

Mo, Val and Nell hold one stock at a time, buy nothing new after 3:30 PM and sell what they hold at 3:59 PM, so only Hal holds overnight.

- **Race** tab, while the market is open: everyone's net worth today as a line (change since the open, you in white and thicker), the order today with each trader's style, and the latest rival trade. Point at a name to read that trader's rule.
- **Chart** tab: the rivals' trades in the picked stock show on its chart as a dot with their initial in their color (Hal blue, Mo orange, Val purple, Nell pink), filled for a buy and a ring for a sell.
- At the closing bell, the results say where you came in today's race and who won it, and the Race tab becomes the standings: all five by net worth, with today's change, the change since day 1 and each rule.
- **Start over** starts the rivals over with you. A desk saved before the rivals existed gives them $10,000 each on the day it is on.
- The window's content is 720 x 476 px and it is not resizable. No command-line arguments.

## On a phone held upright

On a screen narrower than 600 px (a phone held upright, in the browser) the desk is laid out in one column, 360 px wide, at full size instead of shrunk to half:

- The header takes two rows: the day, the clock and OPEN/PAUSED/CLOSED with your net worth on the first; Pause, Play and Fast with your place, today's change and your cash on the second.
- The chart card goes across the whole width, with its Chart, Days and Race tabs. Hear and Hindsight sit on a row of their own under the chart's time labels, and the title shows just the percent change when the whole line does not fit.
- The two lines about your position, then the trade buttons in two rows of three, buys over sells, each 112 x 40 px.
- The watch list comes under the buttons, each row as wide as the screen with a longer sparkline; then the news, the ticker tape and a shorter note that the prices are simulated.
- At the closing bell the results card covers the chart and the buttons: "Since day 1" and the day's best trade in hindsight get lines of their own.
- The ticker box at the top is shorter. The layout is chosen when the app starts, so turning the phone does not switch it.

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
- `src/stockchecker/Portfolio.java`: cash, holdings with their cost, today's trades, the net worth at each close and through today (worked back from the trades).
- `src/stockchecker/Rival.java`: the four computer traders and their rules.
- `src/stockchecker/Money.java`: dollar and percent text.
- `src/stockchecker/Bell.java`: the opening and closing bell, and the one MIDI synthesizer the desk opens.
- `src/stockchecker/ChartEar.java`: hearing the chart (Hear and listening live).
- `src/stockchecker/StockChecker.java`: from the original app: builds the retired Yahoo URL and downloads it to `stockCheck.csv` (no longer called), and looks up a symbol's price in the CSV rows (used for day 1).
- `src/stockchecker/WebpageDownloader.java`, `CSVReader.java`, `TextReader.java`: from the original app: copy a URL into a file, read a CSV into rows, read a `.txt` file.

## Running

From the repo root: `./run.ps1 old StockChecker` (or `./run.ps1 old StockChecker -Build` to build a jar).

The working directory must be `StockChecker/`, since `myStocks.txt` and `stockCheck.csv` are opened by relative path. If `myStocks.txt` is missing or empty, the desk watches the original five tickers. If `stockCheck.csv` is missing, every symbol starts at a made-up price.

## Self-check

`test/stockchecker/StockCheckerCheck.java` checks CSV parsing, symbol lookup and the quote URL. `test/stockchecker/DeskCheck.java` checks the market (replays the same, stays positive, news in time order and only about the stocks shown, each headline pushing for 15 to 45 minutes, or 30 to 60 for the market, and moving its stock its way on average), buying and selling, the desk file across a reload and a close, Start over, the rivals (the same trades on two desks, never on credit, day traders out by the close, Hal holding the list and selling what left it, the same day's end after a mid-day reload, an old desk without them), the Days candles (kept at each close and across a reload, the wick spanning the body, the last 19 kept, cleared by Start over), the money text, what a trade would buy or sell matching what it then does (and nothing after the close), Hindsight's best trade (the low before the high, only up to now, none on a day that only fell, and the same as trying every buy and sell pair over 50 days) and the notes Hear plays (the chart's bottom and top, clamping, the playhead running from the open to now). `test/stockchecker/LayoutCheck.java` checks both layouts, wide and upright phone: the desk's size, and that every button showing lies inside the desk with no two overlapping, with the market open and at the close, and that each paints. From `StockChecker/`:

```
javac -d ../out/check/StockChecker src/stockchecker/*.java test/stockchecker/*.java
java -ea -cp ../out/check/StockChecker stockchecker.StockCheckerCheck
java -ea -cp ../out/check/StockChecker stockchecker.DeskCheck
java -ea -cp ../out/check/StockChecker stockchecker.LayoutCheck
```
