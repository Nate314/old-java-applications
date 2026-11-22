package stockchecker;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Self-check for the simulated market, the practice portfolio and the desk file. Run with java -ea. */
public class DeskCheck {
   public static void main(String[] args) throws Exception {
      Map<String, Double> first = new HashMap<>();
      first.put("CRM", 76.37);
      first.put("NFLX", 87.88);
      Map<String, Double> none = new HashMap<>();
      List<String> symbols = Arrays.asList("CRM", "NFLX", "ZZZ");

      // Day 1 opens at the real quotes (or a steady made-up price) and replays the same every time.
      Market a = new Market(1, symbols, none, first);
      Market b = new Market(1, Arrays.asList("ZZZ", "CRM"), none, first);
      check(a.stock("CRM").open() == 76.37, "CRM opens at its 2016 quote");
      check(a.stock("ZZZ").open() == Math.round(Market.madeUpPrice("ZZZ") * 100) / 100.0, "unknown symbol opens at its made-up price");
      check(Arrays.equals(a.stock("CRM").path, b.stock("CRM").path), "a stock's day does not depend on the other symbols");
      check(!Arrays.equals(a.stock("CRM").path, new Market(2, symbols, none, first).stock("CRM").path), "each day differs");
      double pushed = 0;
      int pushes = 0;
      for (int day = 1; day <= 200; day++) {
         Market market = new Market(day, symbols, none, first);
         for (Market.Stock stock : market.stocks()) {
            for (double price : stock.path) {
               check(price >= 0.5, "prices stay positive");
            }
            check(stock.close() / stock.open() > 0.7 && stock.close() / stock.open() < 1.4, "a day moves a stock a sane amount");
         }
         int last = -1;
         for (Market.Headline headline : market.news(symbols)) {
            check(false, "no news is shown before the open on day " + day + ": " + headline.text);
         }
         market.minute = Market.CLOSE;
         for (Market.Headline headline : market.news(symbols)) {
            check(headline.minute >= last, "headlines in time order");
            last = headline.minute;
            // The chart shades each headline's push over its length: 15 to 45 minutes for a stock, 30 to 60 for the market.
            check(headline.symbol == null ? headline.length >= 30 && headline.length <= 60 : headline.length >= 15 && headline.length <= 45,
               "a headline's push length: " + headline.length);
            if (headline.symbol != null) {
               double[] path = market.stock(headline.symbol).path;
               pushed += Math.log(path[Math.min(Market.CLOSE, headline.minute + headline.length)] / path[headline.minute - 1]) * (headline.up ? 1 : -1);
               pushes++;
            }
         }
      }
      check(pushes > 100 && pushed / pushes > 0.004, "news moves its stock its way over the shaded minutes, on average: " + pushed / pushes);
      Market quiet = new Market(3, symbols, none, first);
      quiet.minute = Market.CLOSE;
      for (Market.Headline headline : quiet.news(Arrays.asList("CRM"))) {
         check(headline.symbol == null || headline.symbol.equals("CRM"), "news only about the stocks shown");
      }
      check(Market.clock(0).equals("9:30 AM") && Market.clock(150).equals("12:00 PM") && Market.clock(390).equals("4:00 PM"), "clock");

      // Buying spends cash, selling returns it, and nobody spends more than they have.
      Portfolio portfolio = new Portfolio();
      check(portfolio.buy("CRM", 10, 76.37, 5) == 10, "buy 10");
      check(Math.abs(portfolio.cash - (10000 - 763.70)) < 1e-6, "cash after buying: " + portfolio.cash);
      check(portfolio.buy("NFLX", Integer.MAX_VALUE, 100, 6) == 92, "buy max spends what is left");
      check(portfolio.cash >= 0 && portfolio.cash < 100, "cash left under a share: " + portfolio.cash);
      check(portfolio.buy("NFLX", 1, 100, 7) == 0, "no buying on credit");
      check(portfolio.sell("CRM", 4, 80, 8) == 4, "sell 4");
      check(Math.abs(portfolio.holdings.get("CRM").cost - 6 * 76.37) < 1e-6, "average cost kept after a sale");
      check(portfolio.sell("CRM", Integer.MAX_VALUE, 80, 9) == 6 && portfolio.shares("CRM") == 0, "sell all");
      check(portfolio.sell("CRM", 1, 80, 9) == 0, "nothing left to sell");
      check(portfolio.trades.size() == 4, "trades logged: " + portfolio.trades.size());

      // The desk file carries the day on, mid-day and across a close.
      File file = File.createTempFile("stockchecker-desk", ".txt");
      file.delete();
      Desk desk = Desk.load(file, Arrays.asList("crm", "nflx", "crm", " "), first);
      check(desk.watch().equals(Arrays.asList("CRM", "NFLX")), "watch list cleaned: " + desk.watch());
      for (int i = 0; i < 100; i++) {
         desk.tick();
      }
      // The trade buttons' notes ask the desk what a trade would do; it must match what the trade then does.
      check(desk.canSell("CRM", 1) == 0 && desk.sell("CRM", 1) == 0, "can sell none before holding any");
      int covers = desk.portfolio.affordable(desk.market.price("CRM"));
      check(desk.canBuy("CRM", Integer.MAX_VALUE) == covers && desk.canBuy("CRM", 10) == 10, "can buy what the cash covers");
      check(desk.buy("CRM", 10) == 10, "desk buy");
      check(desk.canSell("CRM", Integer.MAX_VALUE) == 10 && desk.canSell("CRM", 4) == 4, "can sell what is held");
      check(desk.canBuy("CRM", Integer.MAX_VALUE) == desk.portfolio.affordable(desk.market.price("CRM")), "can buy shrinks with the cash");
      double paid = desk.market.price("CRM");
      desk.setWatch(Arrays.asList("NFLX"));
      check(desk.symbols().equals(Arrays.asList("NFLX", "CRM")), "a held stock stays on the desk: " + desk.symbols());
      desk.setSpeed(Desk.FAST);
      Desk again = Desk.load(file, Arrays.asList("NFLX"), first);
      check(again.market.day == 1 && again.market.minute == 100, "reload keeps the minute");
      check(again.speed == Desk.FAST, "reload keeps the speed");
      check(again.portfolio.shares("CRM") == 10 && again.portfolio.trades.size() == 1, "reload keeps holdings and trades");
      check(again.market.price("CRM") == paid, "reload replays the same prices");
      double worth = again.portfolio.worth(again.market);
      again.nextDay();
      check(again.market.day == 1, "no next day before the close");
      while (!again.tick()) {
         // run to the closing bell
      }
      check(again.market.closed() && again.buy("CRM", 1) == 0, "no trading after the close");
      check(again.canBuy("CRM", 1) == 0 && again.canSell("CRM", 1) == 0, "nothing to trade after the close");
      double close = again.market.price("CRM");
      double closingWorth = again.portfolio.worth(again.market);
      again.nextDay();
      check(again.market.day == 2 && again.market.minute == 0, "day 2 opens");
      check(again.portfolio.closes.size() == 1 && Math.abs(again.portfolio.closes.get(0) - closingWorth) < 0.01, "day 1's close noted");
      check(Math.abs(again.market.stock("CRM").open() / close - 1) < 0.06, "day 2 opens near day 1's close");
      check(again.portfolio.trades.isEmpty(), "trades are per day");
      Desk third = Desk.load(file, Arrays.asList("NFLX"), first);
      check(third.market.day == 2 && third.market.stock("CRM").open() == again.market.stock("CRM").open(), "day 2 reloads the same");
      // The Days chart: each close keeps a candle per stock, across a reload.
      double[] candle = third.days.get("CRM").get(0);
      check(third.days.get("CRM").size() == 1 && candle[0] == 1 && candle[4] == close, "day 1's CRM candle kept: " + Arrays.toString(candle));
      check(candle[2] >= Math.max(candle[1], candle[4]) && candle[3] <= Math.min(candle[1], candle[4]), "a candle's wick spans its body");
      double[] live = third.market.bar(third.market.stock("CRM"));
      check(live[0] == 2 && live[1] == live[4] && live[2] == live[1], "today's candle at the open is one price");
      third.startOver();
      check(third.market.day == 1 && third.portfolio.cash == Portfolio.START && third.portfolio.holdings.isEmpty(), "start over");
      check(third.days.isEmpty(), "start over clears the days");
      check(worth > 0, "worth");
      for (Rival rival : third.rivals) {
         check(rival.portfolio.cash == Portfolio.START && rival.portfolio.holdings.isEmpty() && rival.portfolio.closes.isEmpty(),
            "start over resets " + rival.name);
      }
      for (int day = 1; day <= Desk.HISTORY + 3; day++) {
         while (!third.tick()) {
            // run to the closing bell
         }
         third.nextDay();
      }
      List<double[]> kept = Desk.load(file, Arrays.asList("NFLX"), first).days.get("NFLX");
      check(kept.size() == Desk.HISTORY && kept.get(0)[0] == 4 && kept.get(kept.size() - 1)[0] == Desk.HISTORY + 3,
         "the last " + Desk.HISTORY + " days are kept: " + kept.size());
      file.delete();


      // The rivals: same trades for everybody, carried across a reload, no credit, day traders out by the close.
      List<String> five = Arrays.asList("crm", "lmt", "tsn", "nflx", "swk");
      Desk one = Desk.load(null, five, first);
      Desk two = Desk.load(null, five, first);
      int trades = 0;
      for (int day = 1; day <= 6; day++) {
         while (!one.tick()) {
            for (Rival rival : one.rivals) {
               check(rival.portfolio.cash >= 0, rival.name + " never spends more than he or she has");
            }
         }
         while (!two.tick()) {
            // the same day again
         }
         Rival hal = one.rivals.get(Rival.HAL);
         check(hal.portfolio.holdings.keySet().containsAll(one.watch()), "Hal holds the whole list: " + hal.portfolio.holdings.keySet());
         for (Rival rival : one.rivals) {
            Rival twin = two.rivals.get(rival.kind);
            check(rival.portfolio.trades.size() == twin.portfolio.trades.size()
               && rival.portfolio.worth(one.market) == twin.portfolio.worth(two.market), rival.name + " trades the same for everybody");
            if (rival.kind != Rival.HAL) {
               check(rival.portfolio.holdings.isEmpty(), rival.name + " holds nothing overnight");
               trades += rival.portfolio.trades.size();
            }
            double[] series = rival.portfolio.worthToday(one.market);
            check(series.length == Market.CLOSE + 1 && Math.abs(series[Market.CLOSE] - rival.portfolio.worth(one.market)) < 0.01,
               "worth through the day ends at the worth now");
            check(rival.kind == Rival.HAL || Math.abs(series[0] - rival.portfolio.openingWorth()) < 1, rival.name + " opens the day at the last close: "
               + series[0] + " vs " + rival.portfolio.openingWorth());
         }
         check(one.standings(false).size() == 5 && one.rank(false) >= 1 && one.rank(true) <= 5, "five in the standings");
         one.nextDay();
         two.nextDay();
      }
      check(trades > 20, "the day traders trade: " + trades);

      File saved = File.createTempFile("stockchecker-rivals", ".txt");
      saved.delete();
      Desk straight = Desk.load(saved, five, first);
      for (int i = 0; i < 210; i++) {
         straight.tick();
      }
      Desk reloaded = Desk.load(saved, five, first);
      for (Rival rival : straight.rivals) {
         Rival copy = reloaded.rivals.get(rival.kind);
         check(copy.portfolio.cash == rival.portfolio.cash && copy.portfolio.trades.size() == rival.portfolio.trades.size()
            && copy.portfolio.holdings.keySet().equals(rival.portfolio.holdings.keySet()), rival.name + " reloads mid-day");
      }
      while (!straight.tick()) {
         reloaded.tick();
      }
      reloaded.tick();
      for (Rival rival : straight.rivals) {
         check(Math.abs(reloaded.rivals.get(rival.kind).portfolio.worth(reloaded.market) - rival.portfolio.worth(straight.market)) < 0.01,
            rival.name + " ends the day the same after a reload");
      }
      // Hal sells a stock that left the list at the next open, and its price is still known until then.
      straight.setWatch(Arrays.asList("crm", "lmt"));
      check(straight.marketSymbols().contains("NFLX"), "a stock Hal holds stays in the market");
      straight.nextDay();
      straight.tick();
      check(straight.rivals.get(Rival.HAL).portfolio.holdings.keySet().equals(new java.util.HashSet<>(Arrays.asList("CRM", "LMT"))),
         "Hal sells what left the list: " + straight.rivals.get(Rival.HAL).portfolio.holdings.keySet());
      // A desk saved before the rivals existed gives them $10,000 each.
      try (java.io.Writer writer = new java.io.FileWriter(saved)) {
         writer.write("day=3\nminute=10\ncash=9000\nclose=10100\nclose=10200\n");
      }
      Desk old = Desk.load(saved, five, first);
      check(old.market.day == 3 && old.portfolio.cash == 9000, "old desk loads");
      for (Rival rival : old.rivals) {
         check(rival.portfolio.cash == Portfolio.START && rival.portfolio.openingWorth() == Portfolio.START, rival.name + " joins an old desk");
      }
      saved.delete();

      // Orders: a line below the price buys the dip, a stop sells all, the other line on the stock is cancelled.
      File ordered = File.createTempFile("stockchecker-orders", ".txt");
      ordered.delete();
      Desk lines = Desk.load(ordered, Arrays.asList("CRM"), first);
      lines.tick();
      double now = lines.market.price("CRM");
      check(lines.setOrder("CRM", now * 1.0005) == null, "no order right at the price");
      Desk.Order dip = lines.setOrder("CRM", now * 0.999);
      Desk.Order breakout = lines.setOrder("CRM", now * 1.5);
      check(dip.buy && !dip.above && breakout.buy && breakout.above && lines.orders.size() == 2, "a buy line each side");
      check(lines.setOrder("CRM", now * 1.4).above && lines.orders.size() == 2, "a new line replaces the one on its side");
      Desk.Order dipCopy = Desk.load(ordered, Arrays.asList("CRM"), first).order("CRM", false);
      check(dipCopy != null && dipCopy.buy && dipCopy.price == dip.price, "orders reload");
      Portfolio.Trade bought = null;
      while (bought == null && !lines.tick()) {
         if (!lines.fills.isEmpty()) {
            bought = lines.fills.get(0);
         }
      }
      check(bought != null && bought.shares > 0 && bought.price <= dip.price, "the dip line buys at or under it");
      check(lines.portfolio.shares("CRM") == bought.shares && lines.portfolio.cash < bought.price, "it buys max");
      check(lines.orders.isEmpty(), "a fill cancels the stock's other line");
      check(!lines.market.closed(), "the dip comes before the close");
      double held = lines.market.price("CRM");
      Desk.Order stop = lines.setOrder("CRM", held * 0.5);
      check(stop != null && !stop.buy && !stop.above, "holding, a line below is a stop");
      lines.sell("CRM", Integer.MAX_VALUE);
      check(lines.orders.isEmpty(), "selling everything drops the sell lines");
      lines.setOrder("CRM", held * 0.99);
      lines.setWatch(Arrays.asList("NFLX"));
      check(lines.orders.isEmpty(), "a stock that leaves the desk takes its lines");
      lines.setOrder("NFLX", lines.market.price("NFLX") * 0.5);
      check(lines.orders.size() == 1, "a line on NFLX");
      while (!lines.tick()) {
         // to the close
      }
      lines.nextDay();
      check(lines.orders.isEmpty(), "orders last the day");
      ordered.delete();

      // Money text.
      check(Money.dollars(10234.5).equals("$10,234.50"), Money.dollars(10234.5));
      check(Money.dollars(-0.004).equals("$0.00") && Money.change(-12).equals("-$12.00") && Money.change(5).equals("+$5.00"), "change");
      check(Money.percent(100, 98.75).equals("-1.25%") && Money.percent(100, 100).equals("+0.00%"), Money.percent(100, 98.75));
      check(Money.dollars(1234567.891).equals("$1,234,567.89"), Money.dollars(1234567.891));

      // Hearing the chart: the chart's bottom is the low note, its top the high one, and a hearing runs open to now.
      check(ChartEar.note(90, 90, 10) == ChartEar.LOW_NOTE && ChartEar.note(100, 90, 10) == ChartEar.HIGH_NOTE, "chart edges");
      check(ChartEar.note(95, 90, 10) == (ChartEar.LOW_NOTE + ChartEar.HIGH_NOTE) / 2, "middle of the chart");
      check(ChartEar.note(50, 90, 10) == ChartEar.LOW_NOTE && ChartEar.note(500, 90, 10) == ChartEar.HIGH_NOTE, "off the chart clamps");
      check(ChartEar.note(95, 95, 0) == (ChartEar.LOW_NOTE + ChartEar.HIGH_NOTE) / 2, "no span plays the middle");
      check(ChartEar.minuteAt(0, 200) == 0 && ChartEar.minuteAt(ChartEar.STEPS - 1, 200) == 200, "a hearing runs from the open to now");
      for (int step = 1; step < ChartEar.STEPS; step++) {
         check(ChartEar.minuteAt(step, 390) >= ChartEar.minuteAt(step - 1, 390), "the playhead only moves forward");
      }
      check(ChartEar.minuteAt(ChartEar.STEPS / 2, 0) == 0, "a hearing at the open stays on the open");

      // Hindsight: the best buy before a later sell, only up to the minute asked, and none on a day that only fell.
      double[] path = {10, 12, 9, 11, 14, 8, 12};
      check(Arrays.equals(Market.bestTrade(path, 6), new int[] {2, 4}), "best trade buys the low before the high");
      check(Arrays.equals(Market.bestTrade(path, 1), new int[] {0, 1}), "best trade so far looks only up to now");
      check(Market.bestTrade(new double[] {5, 4, 3, 3}, 3) == null, "no best trade when it never rose");
      check(Market.bestTrade(path, 0) == null, "no best trade at the open");
      for (int day = 1; day <= 50; day++) {
         for (Market.Stock stock : new Market(day, symbols, none, first).stocks()) {
            int[] best = Market.bestTrade(stock.path, Market.CLOSE);
            double most = 1;
            for (int i = 0; i <= Market.CLOSE; i++) {
               for (int j = i + 1; j <= Market.CLOSE; j++) {
                  most = Math.max(most, stock.path[j] / stock.path[i]);
               }
            }
            check(best == null ? most == 1 : best[0] < best[1] && stock.path[best[1]] / stock.path[best[0]] == most,
               "best trade matches every buy and sell pair on day " + day);
         }
      }

      // The day's charts reach a little past now (the first hour at least), never back, and the whole day by the close.
      for (int at = 0; at <= Market.CLOSE; at++) {
         int end = DeskPanel.chartEnd(at);
         check(end >= Math.min(Market.CLOSE, at + 10) && end >= 60 && end <= Market.CLOSE, "the chart reaches past now at minute " + at);
         check(at == 0 || end >= DeskPanel.chartEnd(at - 1), "the chart never shrinks at minute " + at);
      }
      check(DeskPanel.chartEnd(Market.CLOSE) == Market.CLOSE, "the whole day shows at the close");
      check(DeskPanel.timeLabel(30).equals("10 AM") && DeskPanel.timeLabel(60).equals("10:30") && DeskPanel.timeLabel(390).equals("4 PM"),
         "time labels: " + DeskPanel.timeLabel(30) + ", " + DeskPanel.timeLabel(60) + ", " + DeskPanel.timeLabel(390));

      System.out.println("DeskCheck passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
