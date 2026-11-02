package stockchecker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Practice money: cash, the shares held with what they cost, today's trades and the net worth at each day's close. */
final class Portfolio {
   static final double START = 10000;

   static final class Holding {
      int shares;
      /** What the shares held cost in all. */
      double cost;
   }

   /** A buy (shares above 0) or sell (below 0) at a minute of today. */
   static final class Trade {
      final int minute;
      final String symbol;
      final int shares;
      final double price;

      Trade(int minute, String symbol, int shares, double price) {
         this.minute = minute;
         this.symbol = symbol;
         this.shares = shares;
         this.price = price;
      }
   }

   double cash = START;
   final Map<String, Holding> holdings = new LinkedHashMap<>();
   final List<Trade> trades = new ArrayList<>();
   /** Net worth when each finished day closed, day 1 first. */
   final List<Double> closes = new ArrayList<>();

   int shares(String symbol) {
      Holding holding = holdings.get(symbol);
      return holding == null ? 0 : holding.shares;
   }

   int affordable(double price) {
      return price <= 0 ? 0 : (int) Math.floor((cash + 1e-9) / price);
   }

   /** Buys up to {@code want} shares, as many as the cash covers. Returns how many were bought. */
   int buy(String symbol, int want, double price, int minute) {
      int count = Math.min(want, affordable(price));
      if (count <= 0) {
         return 0;
      }
      Holding holding = holdings.get(symbol);
      if (holding == null) {
         holding = new Holding();
         holdings.put(symbol, holding);
      }
      cash = cents(cash - count * price);
      holding.shares += count;
      holding.cost += count * price;
      trades.add(new Trade(minute, symbol, count, price));
      return count;
   }

   /** Sells up to {@code want} of the shares held. Returns how many were sold. */
   int sell(String symbol, int want, double price, int minute) {
      Holding holding = holdings.get(symbol);
      int count = holding == null ? 0 : Math.min(want, holding.shares);
      if (count <= 0) {
         return 0;
      }
      cash = cents(cash + count * price);
      holding.cost -= holding.cost * count / holding.shares;
      holding.shares -= count;
      if (holding.shares == 0) {
         holdings.remove(symbol);
      }
      trades.add(new Trade(minute, symbol, -count, price));
      return count;
   }

   /** Cash plus every holding at the market's price now. */
   double worth(Market market) {
      double total = cash;
      for (Map.Entry<String, Holding> entry : holdings.entrySet()) {
         total += entry.getValue().shares * market.price(entry.getKey());
      }
      return total;
   }

   /** Net worth when today opened: the last close, or the starting cash. */
   double openingWorth() {
      return closes.isEmpty() ? START : closes.get(closes.size() - 1);
   }

   private static double cents(double value) {
      return Math.round(value * 100) / 100.0;
   }
}
