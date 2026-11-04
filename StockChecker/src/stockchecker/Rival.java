package stockchecker;

import java.util.ArrayList;
import java.util.List;

/**
 * A computer trader with its own $10,000 on the same desk. Each follows one plain rule, written out in {@link #plan},
 * and decides only from the prices and headlines up to the current minute, so a day plays out the same for everybody
 * and the rule is all there is to beat. Hal holds; Mo, Val and Nell trade one stock at a time and are out by the close.
 */
final class Rival {
   static final int HAL = 0;
   static final int MO = 1;
   static final int VAL = 2;
   static final int NELL = 3;
   static final String[] NAMES = {"Hal", "Mo", "Val", "Nell"};
   /** One word for the kind of trader. */
   static final String[] STYLES = {"holds", "momentum", "bargains", "news"};
   static final String[] PLANS = {
      "Splits his cash over your list and holds.",
      "Buys the top riser, sells 0.6% off its high.",
      "Buys a 1.2% drop, sells when it is back.",
      "Buys 10 min after good news, sells 30 later.",
   };
   /** The last minute anyone trades: the day traders sell what they hold then. */
   static final int LAST = Market.CLOSE - 1;
   /** Nell reads the news this many minutes after it breaks. */
   static final int NEWS_DELAY = 10;

   final int kind;
   final String name;
   final String plan;
   final Portfolio portfolio = new Portfolio();

   Rival(int kind) {
      this.kind = kind;
      this.name = NAMES[kind];
      this.plan = PLANS[kind];
   }

   static List<Rival> all() {
      List<Rival> rivals = new ArrayList<>();
      for (int kind = 0; kind < NAMES.length; kind++) {
         rivals.add(new Rival(kind));
      }
      return rivals;
   }

   /** Trades at the market's current minute, if the rule says so. Only symbols on {@code watch} are bought. */
   void act(Market market, List<String> watch) {
      int m = market.minute;
      if (m <= 0 || m > LAST) {
         return;
      }
      if (kind == HAL) {
         hold(market, watch, m);
         return;
      }
      String held = held();
      if (held != null) {
         if (m == LAST || sellNow(market, held, m)) {
            portfolio.sell(held, Integer.MAX_VALUE, market.price(held), m);
         }
         return;
      }
      if (m > 360) {
         return;   // nothing new in the last half hour
      }
      String pick = kind == MO ? riser(market, watch, m) : kind == VAL ? bargain(market, watch, m) : goodNews(market, watch, m);
      if (pick != null) {
         portfolio.buy(pick, Integer.MAX_VALUE, market.price(pick), m);
      }
   }

   /** Hal: at the first minute of a day (or whenever he holds nothing), sells what left the list and buys what joined. */
   private void hold(Market market, List<String> watch, int m) {
      if (m != 1 && !portfolio.holdings.isEmpty()) {
         return;
      }
      for (String symbol : new ArrayList<>(portfolio.holdings.keySet())) {
         if (!watch.contains(symbol) && market.price(symbol) > 0) {
            portfolio.sell(symbol, Integer.MAX_VALUE, market.price(symbol), m);
         }
      }
      List<String> missing = new ArrayList<>();
      for (String symbol : watch) {
         if (portfolio.shares(symbol) == 0 && market.price(symbol) > 0) {
            missing.add(symbol);
         }
      }
      double each = portfolio.cash / Math.max(1, missing.size());
      for (String symbol : missing) {
         double price = market.price(symbol);
         portfolio.buy(symbol, (int) Math.floor(each / price), price, m);
      }
   }

   private boolean sellNow(Market market, String symbol, int m) {
      Market.Stock stock = market.stock(symbol);
      if (stock == null) {
         return true;
      }
      double price = stock.path[m];
      int bought = lastBuy(symbol);
      switch (kind) {
         case MO:
            double peak = 0;
            for (int t = Math.max(0, bought); t <= m; t++) {
               peak = Math.max(peak, stock.path[t]);
            }
            return price <= peak * 0.994;
         case VAL:
            Portfolio.Holding holding = portfolio.holdings.get(symbol);
            double paid = holding.cost / holding.shares;
            return price >= stock.open() || price <= paid * 0.985;
         default:
            return m - bought >= 30 || newsAt(market, symbol, m, false);
      }
   }

   /** Mo, every 10 minutes from 10:00: the stock up most over the last half hour, if it is up at least 0.4%. */
   private String riser(Market market, List<String> watch, int m) {
      if (m < 30 || m % 10 != 0) {
         return null;
      }
      String best = null;
      double bestRise = 0.004;
      for (String symbol : watch) {
         Market.Stock stock = market.stock(symbol);
         if (stock != null && !soldSince(symbol, m - 30)) {
            double rise = stock.path[m] / stock.path[m - 30] - 1;
            if (rise > bestRise) {
               best = symbol;
               bestRise = rise;
            }
         }
      }
      return best;
   }

   /** Val, every 5 minutes from 9:50: the stock down most since the open, if it is down at least 1.2%. */
   private String bargain(Market market, List<String> watch, int m) {
      if (m < 20 || m % 5 != 0) {
         return null;
      }
      String worst = null;
      double worstDrop = -0.012;
      for (String symbol : watch) {
         Market.Stock stock = market.stock(symbol);
         if (stock != null && !soldSince(symbol, m - 30)) {
            double drop = stock.path[m] / stock.open() - 1;
            if (drop < worstDrop) {
               worst = symbol;
               worstDrop = drop;
            }
         }
      }
      return worst;
   }

   /** Nell: a stock on the list that has a good headline this minute. */
   private String goodNews(Market market, List<String> watch, int m) {
      for (String symbol : watch) {
         if (market.stock(symbol) != null && newsAt(market, symbol, m - NEWS_DELAY, true)) {
            return symbol;
         }
      }
      return null;
   }

   private static boolean newsAt(Market market, String symbol, int m, boolean up) {
      for (Market.Headline headline : market.news(java.util.Collections.singletonList(symbol))) {
         if (headline.minute == m && symbol.equals(headline.symbol) && headline.up == up) {
            return true;
         }
      }
      return false;
   }

   /** The one stock a day trader holds, or null. */
   private String held() {
      for (String symbol : portfolio.holdings.keySet()) {
         return symbol;
      }
      return null;
   }

   private int lastBuy(String symbol) {
      for (int i = portfolio.trades.size() - 1; i >= 0; i--) {
         Portfolio.Trade trade = portfolio.trades.get(i);
         if (trade.symbol.equals(symbol) && trade.shares > 0) {
            return trade.minute;
         }
      }
      return 0;
   }

   private boolean soldSince(String symbol, int since) {
      for (Portfolio.Trade trade : portfolio.trades) {
         if (trade.symbol.equals(symbol) && trade.shares < 0 && trade.minute > since) {
            return true;
         }
      }
      return false;
   }
}
