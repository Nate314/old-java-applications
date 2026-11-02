package stockchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The practice desk: the watch list, today's {@link Market}, the {@link Portfolio}, the clock's speed, and the file
 * ({@code ~/.stockchecker-desk.txt}) that keeps them, so a day carries on where it was left.
 */
final class Desk {
   static final int MAX_SYMBOLS = 8;
   /** Speeds: paused, normal (a day in about 2.5 minutes) and fast (about 30 seconds). */
   static final int PAUSED = 0;
   static final int NORMAL = 1;
   static final int FAST = 2;

   private final File file;
   private final Map<String, Double> firstPrices;
   /** Each symbol's last close (the price its next day opens from), or its starting price before any close. */
   private final Map<String, Double> bases = new LinkedHashMap<>();
   private List<String> watch = new ArrayList<>();
   final Portfolio portfolio = new Portfolio();
   Market market;
   int speed = NORMAL;

   private Desk(File file, Map<String, Double> firstPrices) {
      this.file = file;
      this.firstPrices = firstPrices;
   }

   /** The desk saved in {@code file}, or day 1 with the starting cash. {@code firstPrices} are real quotes. */
   static Desk load(File file, List<String> watch, Map<String, Double> firstPrices) {
      Desk desk = new Desk(file, firstPrices);
      int day = 1;
      int minute = 0;
      try {
         if (file != null && file.isFile()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            for (String line; (line = reader.readLine()) != null;) {
               int at = line.indexOf('=');
               if (at < 0) {
                  continue;
               }
               String key = line.substring(0, at).trim();
               String value = line.substring(at + 1).trim();
               String[] parts = value.split(":");
               switch (key) {
                  case "day": day = Math.max(1, Integer.parseInt(value)); break;
                  case "minute": minute = Math.max(0, Math.min(Market.CLOSE, Integer.parseInt(value))); break;
                  case "speed": desk.speed = Math.max(PAUSED, Math.min(FAST, Integer.parseInt(value))); break;
                  case "cash": desk.portfolio.cash = Double.parseDouble(value); break;
                  case "base": desk.bases.put(parts[0], Double.parseDouble(parts[1])); break;
                  case "hold":
                     Portfolio.Holding holding = new Portfolio.Holding();
                     holding.shares = Integer.parseInt(parts[1]);
                     holding.cost = Double.parseDouble(parts[2]);
                     if (holding.shares > 0) {
                        desk.portfolio.holdings.put(parts[0], holding);
                     }
                     break;
                  case "trade":
                     desk.portfolio.trades.add(new Portfolio.Trade(Integer.parseInt(parts[0]), parts[1],
                        Integer.parseInt(parts[2]), Double.parseDouble(parts[3])));
                     break;
                  case "close": desk.portfolio.closes.add(Double.parseDouble(value)); break;
                  default: break;
               }
            }
            reader.close();
         }
      } catch (IOException | RuntimeException e) {
         System.err.println("Could not read " + file + ", starting a new desk: " + e);
         desk = new Desk(file, firstPrices);
         day = 1;
         minute = 0;
      }
      desk.watch = clean(watch);
      desk.market = new Market(day, desk.symbols(), desk.bases, firstPrices);
      desk.market.minute = minute;
      return desk;
   }

   /** Upper case, no blanks or repeats, at most {@code MAX_SYMBOLS}. */
   static List<String> clean(List<String> symbols) {
      List<String> clean = new ArrayList<>();
      for (String symbol : symbols) {
         String s = symbol.trim().toUpperCase();
         if (!s.isEmpty() && s.length() <= 8 && !clean.contains(s) && clean.size() < MAX_SYMBOLS) {
            clean.add(s);
         }
      }
      return clean;
   }

   List<String> watch() {
      return watch;
   }

   /** The watch list, then any symbol still held that is not on it (it stays until it is sold). */
   List<String> symbols() {
      List<String> symbols = new ArrayList<>(watch);
      for (String held : portfolio.holdings.keySet()) {
         if (!symbols.contains(held)) {
            symbols.add(held);
         }
      }
      return symbols;
   }

   /** Replaces the watch list; new symbols join today's market at the current minute. */
   void setWatch(List<String> symbols) {
      watch = clean(symbols);
      for (String symbol : symbols()) {
         market.add(symbol, bases, firstPrices);
      }
      save();
   }

   /** Moves the clock on a minute. Returns true when that minute is the closing bell. */
   boolean tick() {
      if (market.closed()) {
         return false;
      }
      market.minute++;
      if (market.closed()) {
         save();
         return true;
      }
      if (market.minute % 15 == 0) {
         save();
      }
      return false;
   }

   /** After the close: notes the day's net worth and opens the next day from today's closing prices. */
   void nextDay() {
      if (!market.closed()) {
         return;
      }
      portfolio.closes.add(Math.round(portfolio.worth(market) * 100) / 100.0);
      for (Market.Stock stock : market.stocks()) {
         bases.put(stock.symbol, stock.close());
      }
      portfolio.trades.clear();
      market = new Market(market.day + 1, symbols(), bases, firstPrices);
      save();
   }

   /** Back to day 1 with the starting cash and nothing held. */
   void startOver() {
      portfolio.cash = Portfolio.START;
      portfolio.holdings.clear();
      portfolio.trades.clear();
      portfolio.closes.clear();
      bases.clear();
      market = new Market(1, symbols(), bases, firstPrices);
      save();
   }

   int buy(String symbol, int want) {
      int done = market.closed() ? 0 : portfolio.buy(symbol, want, market.price(symbol), market.minute);
      save();
      return done;
   }

   int sell(String symbol, int want) {
      int done = market.closed() ? 0 : portfolio.sell(symbol, want, market.price(symbol), market.minute);
      save();
      return done;
   }

   void setSpeed(int speed) {
      this.speed = speed;
      save();
   }

   void save() {
      if (file == null) {
         return;
      }
      StringBuilder out = new StringBuilder();
      out.append("day=").append(market.day).append('\n');
      out.append("minute=").append(market.minute).append('\n');
      out.append("speed=").append(speed).append('\n');
      out.append("cash=").append(portfolio.cash).append('\n');
      for (Map.Entry<String, Double> base : bases.entrySet()) {
         out.append("base=").append(base.getKey()).append(':').append(base.getValue()).append('\n');
      }
      for (Map.Entry<String, Portfolio.Holding> hold : portfolio.holdings.entrySet()) {
         out.append("hold=").append(hold.getKey()).append(':').append(hold.getValue().shares).append(':')
            .append(hold.getValue().cost).append('\n');
      }
      for (Portfolio.Trade trade : portfolio.trades) {
         out.append("trade=").append(trade.minute).append(':').append(trade.symbol).append(':').append(trade.shares)
            .append(':').append(trade.price).append('\n');
      }
      for (Double close : portfolio.closes) {
         out.append("close=").append(close).append('\n');
      }
      try (Writer writer = new FileWriter(file)) {
         writer.write(out.toString());
      } catch (IOException e) {
         System.err.println("Could not save " + file + ": " + e.getMessage());
      }
   }
}
