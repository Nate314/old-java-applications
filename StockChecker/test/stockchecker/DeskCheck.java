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
         }
      }
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
      check(desk.buy("CRM", 10) == 10, "desk buy");
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
      double close = again.market.price("CRM");
      double closingWorth = again.portfolio.worth(again.market);
      again.nextDay();
      check(again.market.day == 2 && again.market.minute == 0, "day 2 opens");
      check(again.portfolio.closes.size() == 1 && Math.abs(again.portfolio.closes.get(0) - closingWorth) < 0.01, "day 1's close noted");
      check(Math.abs(again.market.stock("CRM").open() / close - 1) < 0.06, "day 2 opens near day 1's close");
      check(again.portfolio.trades.isEmpty(), "trades are per day");
      Desk third = Desk.load(file, Arrays.asList("NFLX"), first);
      check(third.market.day == 2 && third.market.stock("CRM").open() == again.market.stock("CRM").open(), "day 2 reloads the same");
      third.startOver();
      check(third.market.day == 1 && third.portfolio.cash == Portfolio.START && third.portfolio.holdings.isEmpty(), "start over");
      check(worth > 0, "worth");
      file.delete();

      // Money text.
      check(Money.dollars(10234.5).equals("$10,234.50"), Money.dollars(10234.5));
      check(Money.dollars(-0.004).equals("$0.00") && Money.change(-12).equals("-$12.00") && Money.change(5).equals("+$5.00"), "change");
      check(Money.percent(100, 98.75).equals("-1.25%") && Money.percent(100, 100).equals("+0.00%"), Money.percent(100, 98.75));
      check(Money.dollars(1234567.891).equals("$1,234,567.89"), Money.dollars(1234567.891));

      System.out.println("DeskCheck passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
