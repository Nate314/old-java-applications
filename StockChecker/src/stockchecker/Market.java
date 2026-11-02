package stockchecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * One simulated trading day, 9:30 to 4:00, one price per minute. Every stock's whole day is worked out when it is
 * added, from a random walk seeded by the day number and its symbol, so a day replays the same after a reload (and the
 * same for everybody). The stocks share a market-wide move and now and then one gets news that pushes it for a while.
 */
final class Market {
   /** Minutes from the 9:30 open to the 4:00 close. */
   static final int CLOSE = 390;
   private static final String[] UP = {"Big buy orders lift %", "% climbs on heavy volume", "Traders pile into %"};
   private static final String[] DOWN = {"Big sell orders hit %", "% slips on heavy volume", "Traders dump %"};

   /** A news line, shown once the day's clock reaches its minute. */
   static final class Headline {
      final int minute;
      final String text;
      final boolean up;
      /** The stock it is about, or null for the whole market. */
      final String symbol;

      Headline(int minute, String text, boolean up, String symbol) {
         this.minute = minute;
         this.text = text;
         this.up = up;
         this.symbol = symbol;
      }
   }

   static final class Stock {
      final String symbol;
      /** Daily volatility, 0.8% to 2%. */
      final double sigma;
      final double[] path = new double[CLOSE + 1];

      Stock(String symbol, double sigma) {
         this.symbol = symbol;
         this.sigma = sigma;
      }

      double open() {
         return path[0];
      }

      double close() {
         return path[CLOSE];
      }
   }

   final int day;
   private final double[] market = new double[CLOSE + 1];
   private final double[] marketDrift = new double[CLOSE + 1];
   private final List<Stock> stocks = new ArrayList<>();
   private final List<Headline> headlines = new ArrayList<>();
   int minute;

   /**
    * The day's market for the symbols. {@code bases} holds each symbol's last close (or, on day 1, its starting
    * price); a later day opens at that close plus an overnight gap. A symbol missing from it starts at {@code
    * firstPrices}, or a made-up price.
    */
   Market(int day, List<String> symbols, Map<String, Double> bases, Map<String, Double> firstPrices) {
      this.day = day;
      Random random = new Random(seed(day, ""));
      for (int m = 1; m <= CLOSE; m++) {
         market[m] = random.nextGaussian();
      }
      // Most days have one market-wide swing of half a percent to a percent and a half.
      if (random.nextDouble() < 0.6) {
         boolean up = random.nextBoolean();
         int start = 30 + random.nextInt(270);
         int length = 30 + random.nextInt(31);
         double size = 0.005 + random.nextDouble() * 0.01;
         for (int m = start; m < start + length && m <= CLOSE; m++) {
            marketDrift[m] = (up ? size : -size) / length;
         }
         addHeadline(new Headline(start, up ? "Stocks rally across the board" : "Stocks slide across the board", up, null));
      }
      for (String symbol : symbols) {
         add(symbol, bases, firstPrices);
      }
   }

   /** Adds a symbol (if it is not here yet) with its whole day. */
   void add(String symbol, Map<String, Double> bases, Map<String, Double> firstPrices) {
      if (stock(symbol) != null) {
         return;
      }
      int hash = symbol.hashCode();
      Stock stock = new Stock(symbol, 0.008 + Math.floorMod(hash >>> 3, 13) / 1000.0);
      Random random = new Random(seed(day, symbol));
      Double base = bases.get(symbol);
      double start;
      if (base != null && base > 0) {
         // Overnight gap of about 1%, except on the first day, which opens at the starting prices.
         double gap = random.nextGaussian() * 0.01;
         start = day > 1 ? base * Math.exp(gap) : base;
      } else {
         Double first = firstPrices.get(symbol);
         start = first != null && first > 0 ? first : madeUpPrice(symbol);
      }
      stock.path[0] = round(start);
      double perMinute = stock.sigma / Math.sqrt(CLOSE);
      double drift = 0;
      int driftLeft = 0;
      double price = start;
      for (int m = 1; m <= CLOSE; m++) {
         double own = random.nextGaussian();
         double chance = random.nextDouble();
         double a = random.nextDouble();
         double b = random.nextDouble();
         // About one news push per stock per day: 0.6% to 2.2% spread over 15 to 45 minutes.
         if (driftLeft == 0 && m < CLOSE - 20 && chance < 1.0 / 420) {
            boolean up = a < 0.5;
            driftLeft = 15 + (int) (b * 31);
            double size = 0.006 + (a * 2 % 1) * 0.016;   // a picked the direction; its other bits pick the size
            drift = (up ? size : -size) / driftLeft;
            String[] lines = up ? UP : DOWN;
            addHeadline(new Headline(m, lines[(int) (b * lines.length) % lines.length].replace("%", symbol), up, symbol));
         }
         double move = perMinute * (0.55 * market[m] + 0.835 * own) + marketDrift[m] + (driftLeft > 0 ? drift : 0);
         if (driftLeft > 0) {
            driftLeft--;
         }
         price = Math.max(0.5, price * Math.exp(move));
         stock.path[m] = round(price);
      }
      stocks.add(stock);
   }

   List<Stock> stocks() {
      return stocks;
   }

   Stock stock(String symbol) {
      for (Stock stock : stocks) {
         if (stock.symbol.equals(symbol)) {
            return stock;
         }
      }
      return null;
   }

   double price(String symbol) {
      Stock stock = stock(symbol);
      return stock == null ? 0 : stock.path[minute];
   }

   boolean closed() {
      return minute >= CLOSE;
   }

   /** The headlines up to now about the market or one of {@code symbols}, newest last. */
   List<Headline> news(List<String> symbols) {
      List<Headline> seen = new ArrayList<>();
      for (Headline headline : headlines) {
         if (headline.minute <= minute && (headline.symbol == null || symbols.contains(headline.symbol))) {
            seen.add(headline);
         }
      }
      return seen;
   }

   /** "10:42 AM" for a minute of the day. */
   static String clock(int minute) {
      int total = 9 * 60 + 30 + minute;
      int hour = total / 60;
      int mins = total % 60;
      int shown = hour > 12 ? hour - 12 : hour;
      return shown + ":" + (mins < 10 ? "0" : "") + mins + (hour >= 12 ? " PM" : " AM");
   }

   /** A steady price between $10 and $300 for a symbol with no real quote. */
   static double madeUpPrice(String symbol) {
      return 10 + Math.floorMod(symbol.hashCode() * 31 + 7, 29000) / 100.0;
   }

   private void addHeadline(Headline headline) {
      int at = headlines.size();
      while (at > 0 && headlines.get(at - 1).minute > headline.minute) {
         at--;
      }
      headlines.add(at, headline);
   }

   private static long seed(int day, String symbol) {
      return day * 0x9E3779B97F4A7C15L + symbol.hashCode() * 1000003L + 2016;
   }

   private static double round(double price) {
      return Math.round(price * 100) / 100.0;
   }
}
