package stockchecker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The trading desk, drawn by hand: the clock and net worth across the top, the watch list with sparklines on the
 * left, the chosen stock's day chart with its buy and sell buttons on the right, and the news and a ticker tape along
 * the bottom. At the closing bell the chart gives way to the day's results and a chart of every close so far.
 */
@SuppressWarnings("serial")
final class DeskPanel extends JPanel {
   /** A screen narrower than this (a phone held upright, in the browser) gets the desk in one column. */
   static final int NARROW = 600;
   private static final int TICK_MS = 40;
   private static final int MESSAGE_MS = 3000;

   private static final Color UP = new Color(70, 224, 128);
   private static final Color DOWN = new Color(255, 104, 104);
   private static final Color WARN = new Color(255, 200, 70);
   private static final Color TEXT = new Color(240, 244, 255);
   private static final Color MUTED = new Color(170, 184, 220);
   /** Hal, Mo, Val and Nell (in {@link Rival} order); you are drawn in {@link #TEXT}. */
   private static final Color[] RIVALS = {new Color(120, 196, 255), new Color(255, 172, 64), new Color(196, 150, 255), new Color(255, 128, 200)};
   private static final Font BIG = new Font("SansSerif", Font.BOLD, 18);
   private static final Font TITLE = new Font("SansSerif", Font.BOLD, 15);
   private static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);
   private static final Font PLAIN = new Font("SansSerif", Font.PLAIN, 13);
   private static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);
   private static final Font TAPE = new Font("Monospaced", Font.BOLD, 13);

   private final class Button {
      final String label;
      final char key;
      final Runnable action;
      final BooleanSupplier shown;
      final Color fill;
      final Rectangle bounds;
      /** Whether a button with no fill of its own shows as on (the current speed, the open tab). */
      BooleanSupplier lit = () -> false;
      /** A trade button's shares (+ to buy, - to sell, MAX_VALUE and MIN_VALUE for all), or 0 for any other button. */
      int count;

      Button(String label, char key, Color fill, Rectangle bounds, BooleanSupplier shown, Runnable action) {
         this.label = label;
         this.key = key;
         this.fill = fill;
         this.bounds = bounds;
         this.shown = shown;
         this.action = action;
      }
   }

   private final Desk desk;
   private final Bell bell = new Bell();
   private final ChartEar ear = new ChartEar();
   private final List<Button> buttons = new ArrayList<>();
   private final Timer timer;
   private Color background = new Color(14, 24, 96);
   private Color accent = new Color(0, 255, 255);
   private int selected;
   private int ticks;
   private int mouseX = -1;
   private int mouseY = -1;
   private String message;
   private Color messageColor = TEXT;
   private long messageUntil;
   private boolean startOverArmed;
   /** Whether the chart shows, in hindsight, the best single buy and sell of the picked stock so far today (I). */
   private boolean hindsight;
   /** The chart card's tabs: the picked stock's day, its days so far as candles, or the race against the rivals. */
   private static final int CHART = 0;
   private static final int DAYS = 1;
   private static final int RACE = 2;
   private int tab = CHART;
   private final Rectangle[] rows = new Rectangle[Desk.MAX_SYMBOLS * 2];
   /** The big chart's plot box and price range as last drawn (null when no chart shows), for placing order lines. */
   private Rectangle plot;
   /** The trade markers on the big chart as last drawn, which order line tags keep clear of. */
   private final List<Rectangle> marks = new ArrayList<>();
   private double plotLow;
   private double plotSpan;

   /** A trade on its way from its button (or the chart, for an order line) to the stock's row: "+10 CRM". */
   private static final class Flight {
      final String symbol;
      final String label;
      final boolean buy;
      final int fromX;
      final int fromY;
      int age;

      Flight(String symbol, int shares, int fromX, int fromY) {
         this.symbol = symbol;
         this.label = (shares > 0 ? "+" : "-") + Math.abs(shares) + " " + symbol;
         this.buy = shares > 0;
         this.fromX = fromX;
         this.fromY = fromY;
      }
   }

   /** Ticks a trade takes to fly to its row (about half a second), then ticks its row glows after it lands. */
   private static final int FLY = 12;
   private static final int LAND = 12;
   /** Ticks a watch row's price stays tinted after it moves. */
   private static final int FLASH = 9;
   private final List<Flight> flights = new ArrayList<>();
   /** The tick of the last market minute, which the prices' flashes fade from. */
   private int minuteTick = -FLASH;
   /** The cash and net worth as shown: they roll to the real figures over about a second, so a trade's cost shows. */
   private double shownCash = Double.NaN;
   private double shownWorth = Double.NaN;

   /**
    * Where the parts of the desk go. Wide (720 x 440): the watch list on the left, the chart card with the trade buttons
    * under it on the right. Narrow (360 x 680, a phone held upright): a two-row header, the chart card across the
    * width, the trade buttons in two rows of three under it, then the watch list, all at full size instead of shrunk.
    */
   private final boolean narrow;
   private final int width;
   private final int height;
   private final int header;
   /** The chart card (Chart, Days and Race while the market is open). */
   private final Rectangle card;
   /** The results card at the closing bell (and the standings): the chart card and the trade buttons' room. */
   private final Rectangle closeCard;
   private final Rectangle watchBox;
   /** The baseline of the first of the two lines under the chart card. */
   private final int positionY;
   /** The baseline of the headline; the ticker tape and the note about the prices are under it. */
   private final int newsY;

   DeskPanel(Desk desk, boolean narrow) {
      this.desk = desk;
      this.narrow = narrow;
      width = narrow ? 360 : 720;
      height = narrow ? 680 : 440;
      header = narrow ? 70 : 46;
      card = narrow ? new Rectangle(8, 76, 344, 222) : new Rectangle(272, 54, 438, 222);
      closeCard = narrow ? new Rectangle(8, 76, 344, 350) : new Rectangle(272, 54, 438, 322);
      watchBox = narrow ? new Rectangle(8, 434, 344, 180) : new Rectangle(10, 54, 250, 322);
      positionY = card.y + card.height + (narrow ? 18 : 22);
      newsY = narrow ? 634 : 398;
      setPreferredSize(new Dimension(width, height));
      setFocusable(true);
      BooleanSupplier open = () -> !desk.market.closed();
      BooleanSupplier closed = () -> desk.market.closed();
      BooleanSupplier always = () -> true;
      String[] speeds = {"Pause", "Play", "Fast"};
      char[] speedKeys = {' ', 'p', 'f'};
      for (int i = 0; i < speeds.length; i++) {
         int speed = i;
         Button button = new Button(speeds[i], speedKeys[i], null, narrow ? new Rectangle(10 + i * 52, 38, 48, 26) : new Rectangle(236 + i * 64, 10, 60, 26), always, () -> speed(speed));
         button.lit = () -> desk.speed == speed;
         buttons.add(button);
      }
      // The chart card's tabs: Chart (C), Days (W) and Race (R, which toggles).
      String[] tabs = {"Chart", "Days", "Race"};
      char[] tabKeys = {'c', 'w', 'r'};
      for (int i = 0; i < tabs.length; i++) {
         int which = i;
         Button button = new Button(tabs[i], tabKeys[i], null, new Rectangle(card.x + card.width - 138 + i * 46, card.y + 6, 42, 22), always, () -> tab = which);
         button.lit = () -> tab == which;
         buttons.add(button);
      }
      // At the chart card's bottom right corner, under the price labels.
      int under = card.y + card.height - 17;
      Button hear = new Button("Hear", 'h', null, new Rectangle(card.x + card.width - 50, under, 46, 16), () -> open.getAsBoolean() && tab == CHART, this::hear);
      hear.lit = () -> ear.hearing() != null;
      buttons.add(hear);
      // Between the 2 PM and 4 PM labels under the chart (narrow: beside Hear, on a row of their own under the labels).
      Button look = new Button("Hindsight", 'i', null, new Rectangle(card.x + card.width - (narrow ? 118 : 154), under, 64, 16), () -> open.getAsBoolean() && tab == CHART, this::toggleHindsight);
      look.lit = () -> hindsight;
      buttons.add(look);
      String[] labels = {"Buy 1", "Buy 10", "Buy max", "Sell 1", "Sell 10", "Sell all"};
      char[] keys = {'b', 'v', 'm', 's', 'd', 'a'};
      int[] counts = {1, 10, Integer.MAX_VALUE, -1, -10, Integer.MIN_VALUE};
      for (int i = 0; i < labels.length; i++) {
         int count = counts[i];
         Color fill = count > 0 ? new Color(30, 140, 80) : new Color(170, 50, 60);
         Button button = new Button(labels[i], keys[i], fill, narrow
            ? new Rectangle(card.x + i % 3 * 116, positionY + 26 + i / 3 * 44, 112, 40) : new Rectangle(272 + i * 74, 336, 68, 36), open, () -> trade(count));
         button.count = count;
         buttons.add(button);
      }
      buttons.add(new Button("Next day", '\n', new Color(30, 140, 80), narrow ? new Rectangle(25, 380, 150, 32) : new Rectangle(290, 330, 150, 32), closed, this::nextDay));
      buttons.add(new Button("Start over", 'x', null, narrow ? new Rectangle(185, 380, 150, 32) : new Rectangle(450, 330, 150, 32), closed, this::startOver));

      timer = new Timer(TICK_MS, event -> tick());
      timer.start();

      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            requestFocusInWindow();
            press(event.getX(), event.getY());
            repaint();
         }

         @Override
         public void mouseMoved(MouseEvent event) {
            mouseX = event.getX();
            mouseY = event.getY();
         }

         @Override
         public void mouseExited(MouseEvent event) {
            mouseX = -1;
            mouseY = -1;
         }
      };
      addMouseListener(mouse);
      addMouseMotionListener(mouse);
      addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent event) {
            key(event);
            repaint();
         }
      });
   }

   /** COLOR!: a random dark background and a bright accent of the opposite hue, like the original's random colors. */
   void recolor() {
      float hue = (float) Math.random();
      background = Color.getHSBColor(hue, 0.75f, 0.36f);
      accent = Color.getHSBColor((hue + 0.5f) % 1f, 0.55f, 1f);
      repaint();
   }

   Color background() {
      return background;
   }

   /** Called after the watch list changed, to keep the same stock picked when it is still there. */
   void watchChanged(String note) {
      selected = Math.max(0, Math.min(selected, desk.symbols().size() - 1));
      if (note != null) {
         say(note, WARN);
      }
      repaint();
   }

   private void tick() {
      ticks++;
      String symbol = symbol();
      if (ear.hearing() != null && (!ear.hearing().equals(symbol) || tab != CHART || desk.market.closed())) {
         ear.stop();
      }
      ear.tick();
      int every = desk.speed == Desk.FAST ? 2 : desk.speed == Desk.NORMAL ? 10 : 0;
      if (every > 0 && ticks % every == 0) {
         boolean opening = desk.market.minute == 0;
         boolean closing = desk.tick();
         minuteTick = ticks;
         announceFills();
         if (closing) {
            bell.ring(3);
            startOverArmed = false;
         } else if (opening && !desk.market.closed()) {
            bell.ring(1);
         }
         if (!closing && symbol != null) {
            Market.Stock stock = desk.market.stock(symbol);
            double[] range = range(stock, true);
            ear.minute(stock.path[desk.market.minute], range[0], range[1]);
         }
      }
      animate();
      String name = summary();
      if (!name.equals(getAccessibleContext().getAccessibleName())) {
         getAccessibleContext().setAccessibleName(name);
      }
      repaint();
   }

   /** One tick of the trades in flight and the rolling cash and net worth (all at once with reduced motion). */
   void animate() {
      boolean still = still();
      flights.removeIf(flight -> (flight.age = still ? Math.max(flight.age, FLY) + 1 : flight.age + 1) >= FLY + LAND);
      shownCash = roll(shownCash, desk.portfolio.cash, still);
      shownWorth = roll(shownWorth, desk.portfolio.worth(desk.market), still);
   }

   private static double roll(double shown, double real, boolean still) {
      return still || Double.isNaN(shown) || Math.abs(real - shown) < 0.01 ? real : shown + (real - shown) * 0.3;
   }

   private static boolean still() {
      return Boolean.TRUE.equals(java.awt.Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
   }

   /** The trades still flying or glowing (for LayoutCheck). */
   int flying() {
      return flights.size();
   }

   /** The cash as the header shows it now (for LayoutCheck). */
   double shownCash() {
      return Double.isNaN(shownCash) ? desk.portfolio.cash : shownCash;
   }

   /** H: plays the picked stock's day so far as notes, or stops the one playing. */
   private void hear() {
      String symbol = symbol();
      if (ear.hearing() != null) {
         ear.stop();
      } else if (symbol != null) {
         Market.Stock stock = desk.market.stock(symbol);
         double[] range = range(stock, true);
         ear.hear(stock, desk.market.minute, range[0], range[1], desk.portfolio.trades);
         say("Hearing " + symbol + ": high notes, high prices; chimes at trades. L: live.", MUTED);
      }
   }

   /** I: shows or hides the best trade so far today on the chart, and says what it would have made. */
   private void toggleHindsight() {
      hindsight = !hindsight;
      String symbol = symbol();
      String[] parts = symbol == null ? null : hindsight(symbol, desk.market.minute);
      say(!hindsight ? "Hindsight off." : parts == null ? symbol + " has not risen yet today: no trade to catch. I hides it."
         : parts[2] + " I hides it.", hindsight ? WARN : MUTED);
   }

   /**
    * The best single trade in {@code symbol} from the open to {@code upTo}, in three pieces: "CRM's best trade so far:
    * buy at 10:41 AM for $75.20", "sell at 2:58 PM for $78.10, +3.86%, about +$386 with all your money in it" and the
    * short "In hindsight, CRM: +3.86%, about +$386 with all your money." Null when it has not risen.
    */
   private String[] hindsight(String symbol, int upTo) {
      Market.Stock stock = desk.market.stock(symbol);
      int[] best = Market.bestTrade(stock.path, upTo);
      if (best == null) {
         return null;
      }
      double buy = stock.path[best[0]];
      double sell = stock.path[best[1]];
      String gain = Money.percent(buy, sell) + ", about " + Money.change(desk.portfolio.openingWorth() * (sell / buy - 1)) + " with all your money";
      return new String[] {symbol + "'s best trade " + (upTo >= Market.CLOSE ? "today" : "so far") + ": buy at " + Market.clock(best[0])
         + " for " + Money.dollars(buy), "sell at " + Market.clock(best[1]) + " for " + Money.dollars(sell) + ", " + gain + " in it",
         "In hindsight, " + symbol + ": " + gain + "."};
   }

   /** What a screen reader says for the desk: the time, the picked stock and your money. */
   private String summary() {
      Market market = desk.market;
      StringBuilder out = new StringBuilder("Trading desk, day " + market.day + ", " + Market.clock(market.minute)
         + (market.closed() ? ", closed. " : desk.speed == Desk.PAUSED ? ", paused. " : ", open. "));
      String symbol = symbol();
      if (symbol != null) {
         Market.Stock stock = market.stock(symbol);
         double price = stock.path[market.minute];
         out.append(symbol).append(' ').append(Money.dollars(price)).append(", ").append(Money.percent(stock.open(), price))
            .append(" today").append(desk.portfolio.shares(symbol) > 0 ? ", you hold " + desk.portfolio.shares(symbol) + ". " : ". ");
         if (tab == DAYS) {
            out.append("Days tab: ").append(daysChange(bars(symbol))).append(". ");
         } else if (tab == CHART && hindsight && !market.closed()) {
            String[] parts = hindsight(symbol, market.minute);
            out.append(parts == null ? "In hindsight, no rise to catch yet. " : "In hindsight, " + parts[0] + ", " + parts[1] + ". ");
         }
      }
      out.append("Net worth ").append(Money.dollars(desk.portfolio.worth(market))).append(", ").append(ordinal(desk.rank(false)))
         .append(" of ").append(desk.rivals.size() + 1).append(". H hears the chart, L listens live.");
      return out.toString();
   }

   /** Says what the orders the last tick reached did, and rings the bell twice when one traded. */
   private void announceFills() {
      for (Portfolio.Trade fill : desk.fills) {
         if (fill.shares != 0) {
            // From the chart's right edge, where the line was reached.
            flights.add(new Flight(fill.symbol, fill.shares, card.x + card.width - 60, card.y + card.height / 2));
         }
         String at = Market.clock(fill.minute) + ": your " + fill.symbol + " line ";
         if (fill.shares > 0) {
            say(at + "bought " + fill.shares + " at " + Money.dollars(fill.price) + ".", UP);
         } else if (fill.shares < 0) {
            say(at + "sold " + -fill.shares + " at " + Money.dollars(fill.price) + ".", DOWN);
         } else {
            say(at + "was reached at " + Money.dollars(fill.price) + ", with nothing to trade.", WARN);
         }
      }
      if (!desk.fills.isEmpty()) {
         bell.ring(2);
      }
   }

   /** The order a click at {@code y} on the chart would cancel (its line within 5 px), or null. */
   private Desk.Order orderAt(String symbol, int y) {
      for (Desk.Order order : desk.orders) {
         if (order.symbol.equals(symbol) && Math.abs(yOfPlot(order.price) - y) <= 5) {
            return order;
         }
      }
      return null;
   }

   private int yOfPlot(double price) {
      return plot.y + plot.height - (int) Math.round((price - plotLow) / plotSpan * plot.height);
   }

   private double priceOfPlot(int y) {
      return plotLow + (double) (plot.y + plot.height - y) / plot.height * plotSpan;
   }

   /** Whether the pointer is over the chart where a click places or cancels an order. */
   private boolean onPlot(int x, int y) {
      return plot != null && tab == CHART && !desk.market.closed() && symbol() != null && plot.contains(x, y);
   }

   /** A click on the chart: cancels the order line under it, or puts one at that price. */
   private void clickPlot(int y) {
      String symbol = symbol();
      Desk.Order order = orderAt(symbol, y);
      if (order != null) {
         desk.cancel(order);
         say("Cancelled the " + symbol + " line at " + Money.dollars(order.price) + ".", MUTED);
      } else {
         placed(desk.setOrder(symbol, priceOfPlot(y)));
      }
   }

   /** - and =: a line 1% below or above the price, or the line on that side moved 1% further out. */
   private void nudgeOrder(boolean above) {
      String symbol = symbol();
      if (symbol == null || desk.market.closed()) {
         return;
      }
      Desk.Order order = desk.order(symbol, above);
      double from = order != null ? order.price : desk.market.price(symbol);
      placed(desk.setOrder(symbol, from * (above ? 1.01 : 0.99)));
   }

   private void placed(Desk.Order order) {
      if (order == null) {
         say("Put the line a little further from the price.", WARN);
      } else {
         say(describe(order) + ". Click the line to cancel it.", order.buy ? UP : DOWN);
      }
   }

   /** "Buy max if CRM falls to $75.00", "Sell all if CRM rises to $80.00". */
   private static String describe(Desk.Order order) {
      return (order.buy ? "Buy max" : "Sell all") + " if " + order.symbol + (order.above ? " rises to " : " falls to ")
         + Money.dollars(order.price);
   }

   private void speed(int speed) {
      desk.setSpeed(speed);
   }

   private String symbol() {
      List<String> symbols = desk.symbols();
      return symbols.isEmpty() ? null : symbols.get(Math.max(0, Math.min(selected, symbols.size() - 1)));
   }

   /** How many shares a trade button would want: Buy max and Sell all as many as there are. */
   private static int want(int count) {
      return count == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(count);
   }

   /** How many shares the trade button for {@code count} would trade in the picked stock now (0 for none). */
   private int wouldTrade(int count) {
      String symbol = symbol();
      return symbol == null ? 0 : count > 0 ? desk.canBuy(symbol, want(count)) : desk.canSell(symbol, want(count));
   }

   /** Why a trade button would trade nothing. */
   private String cannot(int count, String symbol) {
      return count > 0 ? "Not enough cash for a share of " + symbol + " at " + Money.dollars(desk.market.price(symbol)) + "."
         : "You hold no " + symbol + " to sell.";
   }

   /** The second line on a trade button: what it would cost or bring now ("$761.40", "131 shares", "only 3", "no cash"). */
   private String buttonNote(int count) {
      int n = wouldTrade(count);
      if (n == 0) {
         return count > 0 ? "no cash" : "none held";
      }
      if (want(count) == Integer.MAX_VALUE) {
         return n + (n == 1 ? " share" : " shares");
      }
      return n < want(count) ? "only " + n : Money.dollars(n * desk.market.price(symbol()));
   }

   /** Pointing at a trade button: "Buys 10 CRM for $761.40, leaving $9,238.60 cash." or the sale's gain on what was paid. */
   private String tradePreview(int count) {
      String symbol = symbol();
      int n = wouldTrade(count);
      if (n == 0) {
         return cannot(count, symbol);
      }
      double total = n * desk.market.price(symbol);
      if (count > 0) {
         return "Buys " + n + " " + symbol + " for " + Money.dollars(total) + ", leaving " + Money.dollars(desk.portfolio.cash - total) + " cash.";
      }
      Portfolio.Holding holding = desk.portfolio.holdings.get(symbol);
      double cost = holding.cost * n / holding.shares;
      return "Sells " + n + " " + symbol + " for " + Money.dollars(total) + ": " + Money.change(total - cost) + " ("
         + Money.percent(cost, total) + ") on what you paid.";
   }

   void trade(int count) {
      String symbol = symbol();
      if (symbol == null) {
         say("Add a ticker above first.", WARN);
         return;
      }
      double price = desk.market.price(symbol);
      int done = count > 0 ? desk.buy(symbol, want(count)) : desk.sell(symbol, want(count));
      if (done == 0) {
         say(cannot(count, symbol), WARN);
      } else {
         say((count > 0 ? "Bought " : "Sold ") + done + " " + symbol + " at " + Money.dollars(price) + ", "
            + Money.dollars(done * price) + (count > 0 ? " in all." : " back."), count > 0 ? UP : DOWN);
         for (Button button : buttons) {
            if (button.count == count) {
               flights.add(new Flight(symbol, count > 0 ? done : -done, (int) button.bounds.getCenterX(), (int) button.bounds.getCenterY()));
            }
         }
      }
      // A stock sold off the watch list leaves the desk, so keep the pick in range.
      selected = Math.max(0, Math.min(selected, desk.symbols().size() - 1));
   }

   private void nextDay() {
      desk.nextDay();
      startOverArmed = false;
      say("Day " + desk.market.day + ": the market opens at 9:30.", TEXT);
   }

   private void startOver() {
      if (!startOverArmed) {
         startOverArmed = true;
         return;
      }
      startOverArmed = false;
      desk.startOver();
      say("A fresh start: day 1 with " + Money.dollars(Portfolio.START) + ".", TEXT);
   }

   private void say(String text, Color color) {
      message = text;
      messageColor = color;
      messageUntil = System.currentTimeMillis() + MESSAGE_MS;
   }

   private void press(int x, int y) {
      for (Button button : buttons) {
         if (button.shown.getAsBoolean() && button.bounds.contains(x, y)) {
            if (!button.label.equals("Start over")) {
               startOverArmed = false;
            }
            button.action.run();
            return;
         }
      }
      for (int i = 0; i < rows.length; i++) {
         if (rows[i] != null && rows[i].contains(x, y)) {
            selected = i;
            return;
         }
      }
      if (onPlot(x, y)) {
         startOverArmed = false;
         clickPlot(y);
      }
   }

   private void key(KeyEvent event) {
      int code = event.getKeyCode();
      int count = desk.symbols().size();
      if (code == KeyEvent.VK_UP && count > 0) {
         selected = (selected + count - 1) % count;
         return;
      }
      if (code == KeyEvent.VK_DOWN && count > 0) {
         selected = (selected + 1) % count;
         return;
      }
      char key = code == KeyEvent.VK_ENTER ? '\n' : Character.toLowerCase(event.getKeyChar());
      if (key == 'n') {
         key = '\n';
      }
      if (key == ' ') {
         // Space pauses, or plays again when paused.
         speed(desk.speed == Desk.PAUSED ? Desk.NORMAL : Desk.PAUSED);
         return;
      }
      if (key == 'r') {
         tab = tab == RACE ? CHART : RACE;
         return;
      }
      if (key == '-' || key == '=' || key == '+') {
         nudgeOrder(key != '-');
         return;
      }
      if (key == 'l') {
         ear.live = !ear.live;
         say(ear.live ? "Listening live: a note each minute for the picked stock, higher when it is higher on the chart. L stops."
            : "Stopped listening live.", MUTED);
         return;
      }
      if (key == 'o') {
         String symbol = symbol();
         if (symbol != null && desk.orders.removeIf(order -> order.symbol.equals(symbol))) {
            desk.save();
            say("Cancelled the " + symbol + " lines.", MUTED);
         }
         return;
      }
      for (Button button : buttons) {
         if (button.key == key && button.shown.getAsBoolean()) {
            if (!button.label.equals("Start over")) {
               startOverArmed = false;
            }
            button.action.run();
            return;
         }
      }
   }

   // ---- painting ----

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(background);
      g.fillRect(0, 0, getWidth(), getHeight());
      paintHeader(g);
      paintWatch(g);
      plot = null;
      if (desk.market.closed()) {
         if (tab == RACE) {
            paintStandings(g);
         } else if (tab == DAYS) {
            paintDays(g, new Rectangle(closeCard.x, closeCard.y, closeCard.width, closeCard.height - 54));
         } else {
            paintClose(g);
         }
      } else {
         if (tab == RACE) {
            paintRace(g);
         } else if (tab == DAYS) {
            paintDays(g, card);
         } else {
            paintChart(g);
         }
         paintPosition(g);
      }
      paintButtons(g);
      paintNews(g);
      paintFlights(g);
   }

   /** Each trade in flight: a chip with its shares and symbol, arcing from its button to the stock's row. */
   private void paintFlights(Graphics2D g) {
      List<String> symbols = desk.symbols();
      for (Flight flight : flights) {
         int i = symbols.indexOf(flight.symbol);
         if (flight.age >= FLY || i < 0 || i >= rows.length || rows[i] == null) {
            continue;
         }
         double t = (double) flight.age / FLY;
         double p = 1 - Math.pow(1 - t, 3);
         int toX = rows[i].x + 120;
         int toY = rows[i].y + rows[i].height / 2;
         int w = width(g, flight.label, BOLD) + 14;
         int x = (int) Math.round(flight.fromX + (toX - flight.fromX) * p) - w / 2;
         int y = (int) Math.round(flight.fromY + (toY - flight.fromY) * p - Math.sin(Math.PI * p) * 40) - 10;
         g.setColor(flight.buy ? UP : DOWN);
         g.fillRoundRect(x, y, w, 20, 20, 20);
         g.setColor(new Color(255, 255, 255, 200));
         g.drawRoundRect(x, y, w, 20, 20, 20);
         text(g, flight.label, BOLD, new Color(10, 16, 40), x + 7, y + 15);
      }
   }

   private void paintHeader(Graphics2D g) {
      Market market = desk.market;
      g.setColor(new Color(0, 0, 0, 70));
      g.fillRect(0, 0, width, header);
      text(g, "Day " + market.day, BIG, accent, 14, 30);
      int x = 14 + width(g, "Day " + market.day, BIG) + 12;
      text(g, Market.clock(market.minute), PLAIN, TEXT, x, 29);
      x += width(g, "12:00 PM", PLAIN) + 10;
      String state = market.closed() ? "CLOSED" : desk.speed == Desk.PAUSED ? "PAUSED" : "OPEN";
      Color color = market.closed() ? DOWN : desk.speed == Desk.PAUSED ? WARN : UP;
      int pill = width(g, state, SMALL) + 14;
      g.setColor(color);
      g.fillRoundRect(x, 14, pill, 18, 18, 18);
      text(g, state, SMALL, new Color(10, 16, 40), x + 7, 27);

      double worth = Double.isNaN(shownWorth) ? desk.portfolio.worth(market) : shownWorth;
      double opening = desk.portfolio.openingWorth();
      String value = Money.dollars(worth);
      int valueX = width - 14 - width(g, value, BIG);
      text(g, value, BIG, TEXT, valueX, narrow ? 29 : 38);
      // Your place among the five traders by net worth.
      int rank = desk.rank(false);
      String place = ordinal(rank) + " of " + (desk.rivals.size() + 1);
      String today = Money.change(worth - opening) + " today";
      double cashNow = shownCash();
      String cash = "Cash " + Money.dollars(cashNow);
      // While the cash rolls after a trade: red as it is spent, green as a sale pays.
      Color cashColor = cashNow > desk.portfolio.cash ? DOWN : cashNow < desk.portfolio.cash ? UP : MUTED;
      int right = width - 14 - width(g, today, SMALL);
      if (narrow) {
         // Under the net worth, beside the speed buttons: the place and today's change, then the cash.
         text(g, today, SMALL, worth >= opening ? UP : DOWN, right, 49);
         text(g, place, BOLD, rank == 1 ? UP : WARN, right - 10 - width(g, place, BOLD), 49);
         text(g, cash, SMALL, cashColor, width - 14 - width(g, cash, SMALL), 64);
         return;
      }
      text(g, place, BOLD, rank == 1 ? UP : WARN, valueX - 12 - width(g, place, BOLD), 37);
      text(g, today, SMALL, worth >= opening ? UP : DOWN, right, 17);
      cash += "   ";
      text(g, cash, SMALL, cashColor, right - width(g, cash, SMALL), 17);
   }

   private void paintWatch(Graphics2D g) {
      List<String> symbols = desk.symbols();
      for (int i = 0; i < rows.length; i++) {
         rows[i] = null;
      }
      if (symbols.isEmpty()) {
         text(g, "Type tickers in the box above,", PLAIN, MUTED, watchBox.x + 10, watchBox.y + 26);
         text(g, "then press Enter.", PLAIN, MUTED, watchBox.x + 10, watchBox.y + 44);
         return;
      }
      // Wide, a short list's rows grow to fill the column (up to 60 px), with longer sparklines.
      int rowHeight = Math.min(narrow ? 36 : 60, watchBox.height / symbols.size());
      for (int i = 0; i < symbols.size() && i < rows.length; i++) {
         String symbol = symbols.get(i);
         Market.Stock stock = desk.market.stock(symbol);
         if (stock == null) {
            continue;
         }
         Rectangle row = new Rectangle(watchBox.x, watchBox.y + i * rowHeight, watchBox.width, rowHeight - 4);
         rows[i] = row;
         boolean picked = i == selected;
         g.setColor(picked ? withAlpha(accent, 60) : row.contains(mouseX, mouseY) ? new Color(255, 255, 255, 30) : new Color(255, 255, 255, 14));
         g.fillRoundRect(row.x, row.y, row.width, row.height, 10, 10);
         if (picked) {
            g.setColor(accent);
            g.fillRoundRect(row.x, row.y + 4, 4, row.height - 8, 4, 4);
         }
         double price = stock.path[desk.market.minute];
         boolean up = price >= stock.open();
         // The text sits in the middle of a tall row.
         int dy = Math.max(0, (row.height - 36) / 2);
         int top = row.y + dy + Math.min(17, row.height / 2 + 4);
         text(g, symbol, TITLE, TEXT, row.x + 12, top);
         String price1 = Money.dollars(price);
         int priceW = width(g, price1, BOLD);
         // A price that moved this minute lights up green or red for a moment, like a live board.
         int fade = FLASH - (ticks - minuteTick);
         int minute = desk.market.minute;
         if (fade > 0 && minute > 0 && !price1.equals(Money.dollars(stock.path[minute - 1]))) {
            g.setColor(withAlpha(price > stock.path[minute - 1] ? UP : DOWN, 110 * fade / FLASH));
            g.fillRoundRect(row.x + 156 - priceW, top - 13, priceW + 8, 17, 8, 8);
         }
         text(g, price1, BOLD, TEXT, row.x + 160 - priceW, top);
         // A trade that has just landed in this row: the row glows in the trade's color.
         for (Flight flight : flights) {
            if (flight.symbol.equals(symbol) && flight.age >= FLY) {
               g.setColor(withAlpha(flight.buy ? UP : DOWN, 230 * (FLY + LAND - flight.age) / LAND));
               g.setStroke(new BasicStroke(2f));
               g.drawRoundRect(row.x, row.y, row.width, row.height, 10, 10);
               g.setStroke(new BasicStroke(1f));
            }
         }
         if (row.height >= 30) {
            int held = desk.portfolio.shares(symbol);
            String note = held > 0 ? held + " held" : "";
            if (held > 0 && !desk.watch().contains(symbol)) {
               note += ", off list";
            }
            text(g, note, SMALL, accent, row.x + 12, row.y + dy + 31);
            String change = Money.percent(stock.open(), price);
            text(g, change, SMALL, up ? UP : DOWN, row.x + 160 - width(g, change, SMALL), row.y + dy + 31);
         }
         paintLine(g, stock, new Rectangle(row.x + 170, row.y + 5, row.width - 178, row.height - 10), false);
      }
   }

   /** The stock's day so far inside {@code box}: the line, and for the big chart a fill, grid, labels and trades. */
   private void paintLine(Graphics2D g, Market.Stock stock, Rectangle box, boolean big) {
      int now = desk.market.minute;
      double[] range = range(stock, big);
      double low = range[0];
      double lowF = low;
      double span = range[1];
      if (big) {
         plot = box;
         plotLow = low;
         plotSpan = span;
      }
      java.util.function.DoubleToIntFunction yOf = price -> box.y + box.height - (int) Math.round((price - lowF) / span * box.height);
      int end = chartEnd(now);
      java.util.function.IntUnaryOperator xOf = m -> box.x + (int) Math.round((double) m * box.width / end);
      boolean up = stock.path[now] >= stock.open();
      Color line = up ? UP : DOWN;

      if (big) {
         paintScale(g, box, low, span);
         // Time lines on the clock's quarter, half or whole hours, or every two hours, at least 48 px apart.
         int step = 15;
         while (step < 120 && step * box.width < 48 * end) {
            step *= 2;
         }
         int first = (step - OPEN_CLOCK % step) % step;
         for (int m = first == 0 ? step : first; m <= end; m += step) {
            int x = xOf.applyAsInt(m);
            g.setColor(new Color(255, 255, 255, 22));
            g.drawLine(x, box.y, x, box.y + box.height);
            String name = timeLabel(m);
            int w = width(g, name, SMALL);
            // A label that would run under Hindsight or Hear is left out (its line still shows).
            Rectangle room = new Rectangle(x - w / 2 - 2, box.y + box.height + 3, w + 4, 14);
            if (buttons.stream().noneMatch(b -> b.shown.getAsBoolean() && b.bounds.intersects(room))) {
               text(g, name, SMALL, MUTED, x - w / 2, box.y + box.height + 14);
            }
         }
         // The fill under the line.
         int[] xs = new int[now + 3];
         int[] ys = new int[now + 3];
         for (int m = 0; m <= now; m++) {
            xs[m] = xOf.applyAsInt(m);
            ys[m] = yOf.applyAsInt(stock.path[m]);
         }
         xs[now + 1] = xOf.applyAsInt(now);
         ys[now + 1] = box.y + box.height;
         xs[now + 2] = box.x;
         ys[now + 2] = box.y + box.height;
         g.setColor(withAlpha(line, 40));
         g.fillPolygon(xs, ys, now + 3);
         // Each headline that moved this stock (its own or the market's): a faint band over the minutes it has pushed so far.
         for (Market.Headline news : newsOf(stock.symbol)) {
            int x1 = xOf.applyAsInt(news.minute);
            g.setColor(withAlpha(news.up ? UP : DOWN, 28));
            g.fillRect(x1, box.y, Math.max(2, xOf.applyAsInt(Math.min(now, news.minute + news.length)) - x1), box.height);
         }
      }

      // The open, dashed.
      int openY = yOf.applyAsInt(stock.open());
      g.setColor(new Color(255, 255, 255, big ? 90 : 60));
      for (int x = box.x; x < box.x + box.width; x += 6) {
         g.drawLine(x, openY, Math.min(x + 2, box.x + box.width), openY);
      }

      g.setColor(line);
      g.setStroke(new BasicStroke(big ? 2f : 1.4f));
      for (int m = 1; m <= now; m++) {
         g.drawLine(xOf.applyAsInt(m - 1), yOf.applyAsInt(stock.path[m - 1]), xOf.applyAsInt(m), yOf.applyAsInt(stock.path[m]));
      }
      g.setStroke(new BasicStroke(1f));
      int dot = big ? 8 : 5;
      g.fillOval(xOf.applyAsInt(now) - dot / 2, yOf.applyAsInt(stock.path[now]) - dot / 2, dot, dot);
      if (big && desk.speed != Desk.PAUSED && !desk.market.closed() && !still()) {
         // While the market runs, a ring keeps spreading from the price now, once a second.
         int beat = ticks % 25;
         int r = dot / 2 + beat / 2;
         g.setColor(withAlpha(line, 170 * (25 - beat) / 25));
         g.drawOval(xOf.applyAsInt(now) - r, yOf.applyAsInt(stock.path[now]) - r, r * 2, r * 2);
      }

      if (big) {
         marks.clear();
         // A pin at the foot of the chart at each headline's minute, pointing the way it pushed.
         for (Market.Headline news : newsOf(stock.symbol)) {
            int x = xOf.applyAsInt(news.minute);
            int bottom = box.y + box.height - 1;
            g.setColor(news.up ? UP : DOWN);
            g.fillPolygon(new int[] {x - 5, x + 5, x}, news.up ? new int[] {bottom, bottom, bottom - 9} : new int[] {bottom - 9, bottom - 9, bottom}, 3);
            marks.add(new Rectangle(x - 6, bottom - 10, 12, 11));
         }
         // The rivals' trades in this stock: a dot in their color with their initial, filled for a buy, a ring for a sell.
         for (Rival rival : desk.rivals) {
            for (Portfolio.Trade trade : rival.portfolio.trades) {
               if (trade.symbol.equals(stock.symbol) && trade.minute <= now) {
                  int x = xOf.applyAsInt(trade.minute);
                  int y = yOf.applyAsInt(trade.price);
                  Color color = RIVALS[rival.kind];
                  boolean buy = trade.shares > 0;
                  g.setColor(buy ? color : new Color(10, 16, 40, 160));
                  g.fillOval(x - 7, y - 7, 14, 14);
                  marks.add(new Rectangle(x - 8, y - 8, 16, 16));
                  if (!buy) {
                     g.setColor(color);
                     g.setStroke(new BasicStroke(2f));
                     g.drawOval(x - 7, y - 7, 14, 14);
                     g.setStroke(new BasicStroke(1f));
                  }
                  String initial = rival.name.substring(0, 1);
                  text(g, initial, SMALL, buy ? new Color(10, 16, 40) : color, x - width(g, initial, SMALL) / 2, y + 4);
               }
            }
         }
         for (Portfolio.Trade trade : desk.portfolio.trades) {
            if (trade.symbol.equals(stock.symbol) && trade.minute <= now) {
               int x = xOf.applyAsInt(trade.minute);
               int y = yOf.applyAsInt(trade.price);
               boolean buy = trade.shares > 0;
               g.setColor(buy ? UP : DOWN);
               int[] tx = {x - 6, x + 6, x};
               int[] ty = buy ? new int[] {y + 14, y + 14, y + 4} : new int[] {y - 14, y - 14, y - 4};
               g.fillPolygon(tx, ty, 3);
               g.setColor(Color.WHITE);
               g.drawPolygon(tx, ty, 3);
               marks.add(new Rectangle(x - 7, Math.min(ty[0], ty[2]) - 1, 14, 12));
            }
         }
         if (hindsight) {
            paintHindsight(g, stock, now, xOf, yOf, box);
         }
         paintOrders(g, stock.symbol, box);
         // While the chart is heard, a playhead at the minute playing, with its time and price.
         int heard = ear.minute();
         if (stock.symbol.equals(ear.hearing()) && heard <= now) {
            cursor(g, box, xOf.applyAsInt(heard), yOf.applyAsInt(stock.path[heard]), Market.clock(heard) + "  " + Money.dollars(stock.path[heard]), withAlpha(accent, 200));
         } else if (box.contains(mouseX, mouseY)) {
            // Under the pointer: that minute's time, price and change, or the headline when a pin is near.
            int pointed = (int) Math.round((double) (mouseX - box.x) * end / box.width);
            Market.Headline near = newsNear(stock.symbol, pointed);
            int m = near != null ? near.minute : pointed;
            if (m <= now) {
               String tag = Market.clock(m) + "  " + (near != null ? near.text
                  : Money.dollars(stock.path[m]) + "  " + Money.percent(stock.open(), stock.path[m]));
               cursor(g, box, xOf.applyAsInt(m), yOf.applyAsInt(stock.path[m]), tag, near != null ? (near.up ? UP : DOWN) : withAlpha(TEXT, 150));
            }
         }
      }
   }

   /** The headlines so far today that moved {@code symbol}: its own and the market-wide ones, oldest first. */
   private List<Market.Headline> newsOf(String symbol) {
      return desk.market.news(java.util.Collections.singletonList(symbol));
   }

   /** The headline about {@code symbol} within 4 minutes of {@code minute} (the nearest), so a pin is easy to point at; or null. */
   private Market.Headline newsNear(String symbol, int minute) {
      Market.Headline near = null;
      for (Market.Headline news : newsOf(symbol)) {
         if (Math.abs(news.minute - minute) <= 4 && (near == null || Math.abs(news.minute - minute) < Math.abs(near.minute - minute))) {
            near = news;
         }
      }
      return near;
   }

   /** A vertical line down the chart at {@code x}, a dot on the price at {@code y}, and {@code tag} along the top. */
   private void cursor(Graphics2D g, Rectangle box, int x, int y, String tag, Color color) {
      g.setColor(color);
      g.setStroke(new BasicStroke(2f));
      g.drawLine(x, box.y, x, box.y + box.height);
      g.setStroke(new BasicStroke(1f));
      g.fillOval(x - 6, y - 6, 12, 12);
      tag = fit(g, tag, SMALL, box.width - 10);
      int w = width(g, tag, SMALL) + 10;
      int left = Math.max(box.x, Math.min(x + 6, box.x + box.width - w));
      g.setColor(new Color(10, 16, 40, 230));
      g.fillRoundRect(left, box.y + 2, w, 15, 8, 8);
      g.setColor(color);
      g.drawRoundRect(left, box.y + 2, w, 15, 8, 8);
      text(g, tag, SMALL, TEXT, left + 5, box.y + 13);
   }

   /**
    * The best single buy and later sell so far today, in gold: a ring at each end, a dashed line between them, the
    * span between their prices shaded, and a tag with the times, prices and gain.
    */
   private void paintHindsight(Graphics2D g, Market.Stock stock, int now, java.util.function.IntUnaryOperator xOf,
         java.util.function.DoubleToIntFunction yOf, Rectangle box) {
      int[] best = Market.bestTrade(stock.path, now);
      String tag;
      if (best == null) {
         tag = "Hindsight: no rise to catch yet";
      } else {
         double buy = stock.path[best[0]];
         double sell = stock.path[best[1]];
         int x1 = xOf.applyAsInt(best[0]);
         int y1 = yOf.applyAsInt(buy);
         int x2 = xOf.applyAsInt(best[1]);
         int y2 = yOf.applyAsInt(sell);
         g.setColor(withAlpha(WARN, 34));
         g.fillRect(x1, y2, Math.max(1, x2 - x1), y1 - y2);
         g.setColor(WARN);
         g.setStroke(new BasicStroke(2f));
         // Dashed by hand: the web build's BasicStroke has no dashes.
         double length = Math.max(1, Math.hypot(x2 - x1, y2 - y1));
         for (double d = 0; d < length; d += 9) {
            double e = Math.min(length, d + 5);
            g.drawLine((int) Math.round(x1 + (x2 - x1) * d / length), (int) Math.round(y1 + (y2 - y1) * d / length),
               (int) Math.round(x1 + (x2 - x1) * e / length), (int) Math.round(y1 + (y2 - y1) * e / length));
         }
         g.drawOval(x1 - 6, y1 - 6, 12, 12);
         g.drawOval(x2 - 6, y2 - 6, 12, 12);
         g.setStroke(new BasicStroke(1f));
         tag = "Hindsight: " + Market.clock(best[0]) + " " + Money.dollars(buy) + " to " + Market.clock(best[1]) + " "
            + Money.dollars(sell) + ", " + Money.percent(buy, sell);
      }
      tag = fitOr(g, tag, tag.substring("Hindsight: ".length()), SMALL, box.width - 12);
      int w = width(g, tag, SMALL) + 10;
      int top = box.y + box.height - 17;
      g.setColor(new Color(10, 16, 40, 220));
      g.fillRoundRect(box.x + 2, top, w, 15, 8, 8);
      g.setColor(WARN);
      g.drawRoundRect(box.x + 2, top, w, 15, 8, 8);
      text(g, tag, SMALL, TEXT, box.x + 7, top + 11);
   }

   /**
    * {@code stock}'s price scale so far as {low, span}; with {@code orders}, the big chart's (stretched to keep its
    * order lines in view), which the ear plays on too.
    */
   private double[] range(Market.Stock stock, boolean orders) {
      double low = stock.open();
      double high = stock.open();
      for (int m = 0; m <= desk.market.minute; m++) {
         low = Math.min(low, stock.path[m]);
         high = Math.max(high, stock.path[m]);
      }
      if (orders) {
         // Keep every order line on the chart.
         for (Desk.Order order : desk.orders) {
            if (order.symbol.equals(stock.symbol)) {
               low = Math.min(low, order.price);
               high = Math.max(high, order.price);
            }
         }
      }
      // At least a 1% range, so a quiet morning is not drawn as wild swings.
      double room = Math.max(high - low, stock.open() * 0.01);
      double mid = (high + low) / 2;
      return new double[] {mid - room * 0.6, room * 1.2};
   }

   /**
    * The picked stock's order lines (green to buy, red to sell, with a tag saying what each does), and under the
    * pointer a preview of the line a click would put, or the hint that the chart takes clicks.
    */
   private void paintOrders(Graphics2D g, String symbol, Rectangle box) {
      for (Desk.Order order : desk.orders) {
         if (order.symbol.equals(symbol)) {
            orderLine(g, box, yOfPlot(order.price), order.buy ? UP : DOWN, describe(order), 230, false);
         }
      }
      if (!onPlot(mouseX, mouseY)) {
         if (desk.order(symbol, true) == null && desk.order(symbol, false) == null) {
            text(g, "Click the chart to set a buy or sell line (- and = keys)", SMALL, withAlpha(MUTED, 170), box.x + 4, box.y + 12);
         }
         return;
      }
      Desk.Order under = orderAt(symbol, mouseY);
      if (under != null) {
         orderLine(g, box, yOfPlot(under.price), WARN, "Click to cancel: " + describe(under), 255, false);
         return;
      }
      double price = Math.round(priceOfPlot(mouseY) * 100) / 100.0;
      double now = desk.market.price(symbol);
      if (Math.abs(price / now - 1) < 0.001) {
         return;
      }
      boolean buy = desk.portfolio.shares(symbol) == 0;
      Desk.Order preview = new Desk.Order(symbol, buy, price > now, price);
      orderLine(g, box, mouseY, withAlpha(buy ? UP : DOWN, 150), "Click: " + describe(preview), 150, true);
   }

   /**
    * A dashed line across {@code box} at {@code y}, with a tag holding {@code label} at its left end (or its right end, for the preview, so it never covers the tag of a line beside it).
    */
   private void orderLine(Graphics2D g, Rectangle box, int y, Color color, String label, int alpha, boolean right) {
      g.setColor(color);
      g.setStroke(new BasicStroke(1.5f));
      for (int x = box.x; x < box.x + box.width; x += 11) {
         g.drawLine(x, y, Math.min(x + 6, box.x + box.width), y);
      }
      g.setStroke(new BasicStroke(1f));
      String tag = fit(g, label, SMALL, box.width - 12);
      int w = width(g, tag, SMALL) + 10;
      int left = right ? box.x + box.width - 2 - w : box.x + 2;
      int top = y - 17 < box.y ? y + 2 : y - 17;
      // Slide a left tag along past any trade marker it would cover (a rival's 9:30 dot sits right at the edge).
      for (boolean moved = !right; moved; ) {
         moved = false;
         for (Rectangle mark : marks) {
            if (mark.x < left + w && left < mark.x + mark.width && mark.y < top + 15 && top < mark.y + mark.height
               && mark.x + mark.width + 2 + w <= box.x + box.width) {
               left = mark.x + mark.width + 2;
               moved = true;
            }
         }
      }
      g.setColor(new Color(10, 16, 40, alpha));
      g.fillRoundRect(left, top, w, 15, 8, 8);
      g.setColor(color);
      g.drawRoundRect(left, top, w, 15, 8, 8);
      text(g, tag, SMALL, TEXT, left + 5, top + 11);
   }

   private void paintChart(Graphics2D g) {
      g.setColor(new Color(255, 255, 255, 14));
      g.fillRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      String symbol = symbol();
      if (symbol == null) {
         text(g, "No stock picked.", PLAIN, MUTED, card.x + 14, card.y + 24);
         return;
      }
      Market.Stock stock = desk.market.stock(symbol);
      double price = stock.path[desk.market.minute];
      int x = card.x + 14;
      text(g, symbol, BIG, accent, x, card.y + 25);
      x += width(g, symbol, BIG) + 10;
      text(g, Money.dollars(price), BIG, TEXT, x, card.y + 25);
      x += width(g, Money.dollars(price), BIG) + 10;
      boolean up = price >= stock.open();
      String change = Money.change(price - stock.open()) + " (" + Money.percent(stock.open(), price) + ") today";
      text(g, fitOr(g, change, Money.percent(stock.open(), price), PLAIN, tabsX() - 8 - x), PLAIN, up ? UP : DOWN, x, card.y + 24);
      // Narrow, the chart is a row shorter, so Hear and Hindsight sit under its time labels.
      paintLine(g, stock, new Rectangle(card.x + 14, card.y + 38, card.width - 84, card.height - (narrow ? 80 : 62)), true);
   }

   /** Five price lines across {@code box}, from {@code low} up by {@code span}, labelled at the right. */
   private void paintScale(Graphics2D g, Rectangle box, double low, double span) {
      for (int k = 0; k <= 4; k++) {
         int y = box.y + box.height - (int) Math.round(box.height * k / 4.0);
         g.setColor(new Color(255, 255, 255, 22));
         g.drawLine(box.x, y, box.x + box.width, y);
         text(g, Money.dollars(low + span * k / 4), SMALL, MUTED, box.x + box.width + 6, y + 4);
      }
   }

   /** The stock's finished days kept on the desk and today so far, oldest first, as {day, open, high, low, close}. */
   private List<double[]> bars(String symbol) {
      List<double[]> bars = new ArrayList<>(desk.days.getOrDefault(symbol, new ArrayList<>()));
      bars.add(desk.market.bar(desk.market.stock(symbol)));
      return bars;
   }

   /** "+3.20% since day 4", or "+0.40% today" while today is the only day. */
   private static String daysChange(List<double[]> bars) {
      double[] last = bars.get(bars.size() - 1);
      return Money.percent(bars.get(0)[1], last[4]) + (bars.size() == 1 ? " today" : " since day " + (int) bars.get(0)[0]);
   }

   /**
    * The Days tab: the picked stock's days on the desk as candles (a body from the open to the close, a wick from the
    * low to the high), today's outlined and still moving, your average cost as a line while you hold some, and the
    * prices of the day under the pointer.
    */
   private void paintDays(Graphics2D g, Rectangle card) {
      g.setColor(new Color(255, 255, 255, 14));
      g.fillRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      String symbol = symbol();
      if (symbol == null) {
         text(g, "No stock picked.", PLAIN, MUTED, card.x + 14, card.y + 24);
         return;
      }
      List<double[]> bars = bars(symbol);
      double first = bars.get(0)[1];
      double price = bars.get(bars.size() - 1)[4];
      int x = card.x + 14;
      text(g, symbol, BIG, accent, x, card.y + 25);
      x += width(g, symbol, BIG) + 10;
      text(g, Money.dollars(price), BIG, TEXT, x, card.y + 25);
      x += width(g, Money.dollars(price), BIG) + 10;
      text(g, fitOr(g, daysChange(bars), daysChange(bars).split(" ")[0], PLAIN, tabsX() - 8 - x), PLAIN, price >= first ? UP : DOWN, x, card.y + 24);

      int held = desk.portfolio.shares(symbol);
      double paid = held > 0 ? desk.portfolio.holdings.get(symbol).cost / held : 0;
      double low = paid > 0 ? Math.min(first, paid) : first;
      double high = paid > 0 ? Math.max(first, paid) : first;
      for (double[] bar : bars) {
         low = Math.min(low, bar[3]);
         high = Math.max(high, bar[2]);
      }
      // At least a 2% range, so a quiet day is not drawn as a wild one.
      double room = Math.max(high - low, first * 0.02);
      double lowF = (high + low) / 2 - room * 0.6;
      double span = room * 1.2;
      Rectangle box = new Rectangle(card.x + 14, card.y + 38, card.width - 84, card.height - 62);
      paintScale(g, box, lowF, span);
      java.util.function.DoubleToIntFunction yOf = p -> box.y + box.height - (int) Math.round((p - lowF) / span * box.height);
      int slot = box.width / Math.max(bars.size(), 10);
      int hover = box.contains(mouseX, mouseY) ? (mouseX - box.x) / slot : -1;
      // Label every day when the labels fit, or every other one (today always).
      int every = (24 + slot - 1) / slot;
      for (int i = 0; i < bars.size(); i++) {
         double[] bar = bars.get(i);
         boolean today = i == bars.size() - 1;
         int cx = box.x + i * slot + slot / 2;
         if (i == hover) {
            g.setColor(new Color(255, 255, 255, 26));
            g.fillRect(box.x + i * slot, box.y, slot, box.height);
         }
         Color color = bar[4] >= bar[1] ? UP : DOWN;
         g.setColor(today ? color : withAlpha(color, 190));
         g.setStroke(new BasicStroke(1.5f));
         g.drawLine(cx, yOf.applyAsInt(bar[2]), cx, yOf.applyAsInt(bar[3]));
         g.setStroke(new BasicStroke(1f));
         int w = Math.max(3, slot - 6);
         int top = yOf.applyAsInt(Math.max(bar[1], bar[4]));
         int tall = Math.max(2, yOf.applyAsInt(Math.min(bar[1], bar[4])) - top);
         g.fillRect(cx - w / 2, top, w, tall);
         if (today) {
            g.setColor(accent);
            g.drawRect(cx - w / 2 - 2, top - 2, w + 3, tall + 3);
         }
         if ((bars.size() - 1 - i) % every == 0) {
            String label = "D" + (int) bar[0];
            text(g, label, SMALL, today ? accent : MUTED, cx - width(g, label, SMALL) / 2, box.y + box.height + 14);
         }
      }
      marks.clear();
      if (paid > 0) {
         orderLine(g, box, yOf.applyAsInt(paid), accent, "You paid " + Money.dollars(paid) + " a share", 230, false);
      }
      if (hover >= 0 && hover < bars.size()) {
         double[] bar = bars.get(hover);
         boolean now = hover == bars.size() - 1 && !desk.market.closed();
         String head = (now ? "Today so far" : "Day " + (int) bar[0]) + ": " + Money.percent(bar[1], bar[4]);
         String body = "Open " + Money.dollars(bar[1]) + ", high " + Money.dollars(bar[2]) + ", low " + Money.dollars(bar[3])
            + (now ? ", now " : ", close ") + Money.dollars(bar[4]);
         int w = Math.max(width(g, head, BOLD), width(g, body, SMALL)) + 12;
         // On the side away from the pointer, so the candle stays in sight.
         int left = mouseX < box.x + box.width / 2 ? box.x + box.width - w : box.x;
         g.setColor(new Color(10, 16, 40, 235));
         g.fillRoundRect(left, box.y + 2, w, 34, 8, 8);
         g.setColor(withAlpha(accent, 200));
         g.drawRoundRect(left, box.y + 2, w, 34, 8, 8);
         text(g, head, BOLD, bar[4] >= bar[1] ? UP : DOWN, left + 6, box.y + 16);
         text(g, body, SMALL, TEXT, left + 6, box.y + 31);
      } else if (bars.size() == 1) {
         text(g, "Each closing bell adds a day here.", SMALL, withAlpha(MUTED, 170), box.x + slot + 8, box.y + 12);
      } else {
         text(g, "Point at a day for its prices", SMALL, withAlpha(MUTED, 170), box.x + box.width - 160, box.y + 12);
      }
   }

   private static Color colorOf(Desk.Standing standing) {

      return standing.rival == null ? TEXT : RIVALS[standing.rival.kind];
   }

   private static String styleOf(Desk.Standing standing) {
      return standing.rival == null ? "your trades" : Rival.STYLES[standing.rival.kind];
   }

   private static String planOf(Desk.Standing standing) {
      return standing.rival == null ? "Your own trades." : standing.rival.plan;
   }

   /** The race tab while the market is open: everyone's net worth today as a line, the order, the latest rival trade. */
   private void paintRace(Graphics2D g) {
      g.setColor(new Color(255, 255, 255, 14));
      g.fillRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      text(g, "Race today", BIG, accent, card.x + 14, card.y + 25);
      Market market = desk.market;
      int now = market.minute;
      List<Desk.Standing> standings = desk.standings(true);

      // Each trader's change since the open, minute by minute, on one scale (at least +/-0.5%).
      Rectangle box = new Rectangle(card.x + 14, card.y + 44, card.width - 206, 140);
      List<double[]> series = new ArrayList<>();
      double reach = 0.005;
      for (Desk.Standing standing : standings) {
         double[] worth = standing.portfolio.worthToday(market);
         double opening = standing.portfolio.openingWorth();
         for (int m = 0; m < worth.length; m++) {
            worth[m] = worth[m] / opening - 1;
            reach = Math.max(reach, Math.abs(worth[m]));
         }
         series.add(worth);
      }
      double top = reach * 1.15;
      java.util.function.DoubleToIntFunction yOf = change -> box.y + box.height / 2 - (int) Math.round(change / top * box.height / 2);
      int end = chartEnd(now);
      java.util.function.IntUnaryOperator xOf = m -> box.x + (int) Math.round((double) m * box.width / end);
      g.setColor(new Color(0, 0, 0, 50));
      g.fillRect(box.x, box.y, box.width, box.height);
      int zero = yOf.applyAsInt(0);
      g.setColor(new Color(255, 255, 255, 90));
      for (int x = box.x; x < box.x + box.width; x += 6) {
         g.drawLine(x, zero, Math.min(x + 2, box.x + box.width), zero);
      }
      String high = "+" + percentText(top);
      text(g, high, SMALL, MUTED, box.x + box.width + 4, box.y + 10);
      text(g, "0%", SMALL, MUTED, box.x + box.width + 4, zero + 4);
      text(g, "-" + percentText(top), SMALL, MUTED, box.x + box.width + 4, box.y + box.height);
      text(g, "9:30", SMALL, MUTED, box.x, box.y + box.height + 13);
      String endLabel = Market.clock(end).replace(" AM", "").replace(" PM", "");
      text(g, endLabel, SMALL, MUTED, box.x + box.width - width(g, endLabel, SMALL), box.y + box.height + 13);
      // Rivals first, you on top, thicker.
      for (int pass = 0; pass < 2; pass++) {
         for (int i = 0; i < standings.size(); i++) {
            Desk.Standing standing = standings.get(i);
            if ((standing.rival == null) != (pass == 1)) {
               continue;
            }
            double[] line = series.get(i);
            g.setColor(colorOf(standing));
            g.setStroke(new BasicStroke(standing.rival == null ? 2.4f : 1.5f));
            for (int m = 1; m < line.length; m++) {
               g.drawLine(xOf.applyAsInt(m - 1), yOf.applyAsInt(line[m - 1]), xOf.applyAsInt(m), yOf.applyAsInt(line[m]));
            }
            g.setStroke(new BasicStroke(1f));
            g.fillOval(xOf.applyAsInt(now) - 3, yOf.applyAsInt(line[now]) - 3, 7, 7);
         }
      }

      // The order today, with each trader's style; pointing at one shows their rule below.
      Desk.Standing pointed = null;
      int lx = box.x + box.width + 44;
      for (int i = 0; i < standings.size(); i++) {
         Desk.Standing standing = standings.get(i);
         Rectangle row = new Rectangle(lx - 4, card.y + 36 + i * 31, card.x + card.width - lx - 2, 29);
         boolean hover = row.contains(mouseX, mouseY);
         if (hover) {
            pointed = standing;
         }
         if (standing.rival == null || hover) {
            g.setColor(new Color(255, 255, 255, hover ? 34 : 20));
            g.fillRoundRect(row.x, row.y, row.width, row.height, 8, 8);
         }
         text(g, Integer.toString(i + 1), BOLD, MUTED, lx, row.y + 15);
         text(g, standing.name, BOLD, colorOf(standing), lx + 14, row.y + 14);
         text(g, styleOf(standing), SMALL, MUTED, lx + 14, row.y + 26);
         String change = Money.percent(1, 1 + standing.today);
         text(g, change, BOLD, standing.today >= 0 ? UP : DOWN, row.x + row.width - 4 - width(g, change, BOLD), row.y + 14);
      }

      String line;
      Color color;
      if (pointed != null) {
         line = pointed.name + ": " + planOf(pointed);
         color = colorOf(pointed);
      } else {
         Rival who = null;
         Portfolio.Trade last = null;
         for (Rival rival : desk.rivals) {
            for (Portfolio.Trade trade : rival.portfolio.trades) {
               if (last == null || trade.minute >= last.minute) {
                  last = trade;
                  who = rival;
               }
            }
         }
         if (last == null) {
            line = "No rival has traded yet today. Point at a name to read their rule.";
            color = MUTED;
         } else {
            int count = Math.abs(last.shares);
            line = Market.clock(last.minute) + "  " + who.name + (last.shares > 0 ? " bought " : " sold ") + count + " "
               + last.symbol + " at " + Money.dollars(last.price) + ".";
            color = RIVALS[who.kind];
         }
      }
      text(g, fit(g, line, SMALL, card.width - 28), SMALL, color, card.x + 14, card.y + card.height - 8);
   }

   /** "0.5%" or "1.2%": a scale label. */
   private static String percentText(double fraction) {
      long tenths = Math.round(fraction * 1000);
      return tenths / 10 + "." + tenths % 10 + "%";
   }

   /** The race tab after the closing bell: the five traders by net worth, with today's change and their rules. */
   private void paintStandings(Graphics2D g) {
      Rectangle card = closeCard;
      g.setColor(new Color(0, 0, 0, 60));
      g.fillRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      g.setColor(withAlpha(accent, 120));
      g.drawRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      text(g, "Standings, day " + desk.market.day, BIG, accent, card.x + 18, card.y + 30);
      int todayRight = card.x + card.width - 98;
      int worthRight = card.x + card.width - 12;
      text(g, "Today", SMALL, MUTED, todayRight - width(g, "Today", SMALL), card.y + 52);
      text(g, "Net worth", SMALL, MUTED, worthRight - width(g, "Net worth", SMALL), card.y + 52);
      List<Desk.Standing> standings = desk.standings(false);
      for (int i = 0; i < standings.size(); i++) {
         Desk.Standing standing = standings.get(i);
         int y = card.y + 58 + i * 40;
         if (standing.rival == null) {
            g.setColor(withAlpha(accent, 40));
            g.fillRoundRect(card.x + 8, y, card.width - 16, 37, 10, 10);
         }
         text(g, Integer.toString(i + 1), BIG, MUTED, card.x + 18, y + 25);
         text(g, standing.name, BOLD, colorOf(standing), card.x + 42, y + 16);
         text(g, fit(g, planOf(standing), SMALL, todayRight - 60 - card.x - 42), SMALL, MUTED, card.x + 42, y + 31);
         String today = Money.percent(1, 1 + standing.today);
         text(g, today, BOLD, standing.today >= 0 ? UP : DOWN, todayRight - width(g, today, BOLD), y + 22);
         String worth = Money.dollars(standing.worth);
         text(g, worth, BOLD, TEXT, worthRight - width(g, worth, BOLD), y + 16);
         String all = Money.percent(Portfolio.START, standing.worth) + " in all";
         text(g, all, SMALL, standing.worth >= Portfolio.START ? UP : DOWN, worthRight - width(g, all, SMALL), y + 31);
      }
   }

   /** Minutes from midnight to the open, 9:30 AM. */
   private static final int OPEN_CLOCK = 9 * 60 + 30;

   /**
    * The last minute the day's charts reach at minute {@code now}: the day so far and a fifth more (at least the first
    * hour), up to the close. So the line fills the chart from the opening bell instead of a sliver at its left edge.
    */
   static int chartEnd(int now) {
      return Math.min(Market.CLOSE, Math.max(60, now + now / 5 + 12));
   }

   /** A time line's label: "10 AM" on the hour, else "10:30". */
   static String timeLabel(int minute) {
      String clock = Market.clock(minute);
      return clock.contains(":00") ? clock.replace(":00", "") : clock.substring(0, clock.length() - 3);
   }

   private static String ordinal(int n) {
      return n + (n == 1 ? "st" : n == 2 ? "nd" : n == 3 ? "rd" : "th");
   }

   private void paintPosition(Graphics2D g) {
      String symbol = symbol();
      if (symbol == null) {
         return;
      }
      double price = desk.market.price(symbol);
      int held = desk.portfolio.shares(symbol);
      String first;
      String second;
      Color color = MUTED;
      if (held > 0) {
         Portfolio.Holding holding = desk.portfolio.holdings.get(symbol);
         double worth = held * price;
         first = "You hold " + held + " " + symbol + ", bought at " + Money.dollars(holding.cost / held) + " a share on average.";
         second = "Worth " + Money.dollars(worth) + " now: " + Money.change(worth - holding.cost) + " (" + Money.percent(holding.cost, worth) + ").";
         color = worth >= holding.cost ? UP : DOWN;
      } else {
         first = "You hold no " + symbol + ".";
         int can = desk.portfolio.affordable(price);
         second = "Your cash buys up to " + can + (can == 1 ? " share" : " shares") + " at " + Money.dollars(price) + ".";
      }
      if (message != null && System.currentTimeMillis() < messageUntil) {
         second = message;
         color = messageColor;
      } else {
         for (Button button : buttons) {
            if (button.count != 0 && button.bounds.contains(mouseX, mouseY)) {
               boolean can = wouldTrade(button.count) > 0;
               second = tradePreview(button.count);
               color = !can ? WARN : button.count > 0 ? UP : DOWN;
            }
         }
      }
      Font font = narrow ? SMALL : PLAIN;
      text(g, fit(g, first, font, card.width), font, TEXT, card.x + 2, positionY);
      text(g, fit(g, second, font, card.width), font, color, card.x + 2, positionY + (narrow ? 16 : 20));
   }

   private void paintClose(Graphics2D g) {
      Rectangle card = closeCard;
      g.setColor(new Color(0, 0, 0, 60));
      g.fillRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      g.setColor(withAlpha(accent, 120));
      g.drawRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      Market market = desk.market;
      double worth = desk.portfolio.worth(market);
      double opening = desk.portfolio.openingWorth();
      int x = card.x + 18;
      // Narrow, "Since day 1" takes a line of its own under "Today", and everything under it moves down one line.
      int drop = narrow ? 18 : 0;
      text(g, "Closing bell, day " + market.day, BIG, accent, x, card.y + 30);
      text(g, "Net worth " + Money.dollars(worth), TITLE, TEXT, x, card.y + 56);
      text(g, "Today " + Money.change(worth - opening) + " (" + Money.percent(opening, worth) + ")", PLAIN, worth >= opening ? UP : DOWN, x, card.y + 78);
      text(g, "Since day 1 " + Money.change(worth - Portfolio.START) + " (" + Money.percent(Portfolio.START, worth) + ")", PLAIN,
         worth >= Portfolio.START ? UP : DOWN, x + (narrow ? 0 : 210), card.y + 78 + drop);
      Market.Stock best = null;
      Market.Stock worst = null;
      for (Market.Stock stock : market.stocks()) {
         if (!desk.symbols().contains(stock.symbol)) {
            continue;
         }
         double move = stock.close() / stock.open();
         if (best == null || move > best.close() / best.open()) {
            best = stock;
         }
         if (worst == null || move < worst.close() / worst.open()) {
            worst = stock;
         }
      }
      if (best != null) {
         String movers = "Best " + best.symbol + " " + Money.percent(best.open(), best.close())
            + "    Worst " + worst.symbol + " " + Money.percent(worst.open(), worst.close());
         text(g, movers, SMALL, MUTED, x, card.y + 120 + drop);
      }
      // In hindsight, the day's best single trade among the stocks on the desk.
      String bestSymbol = null;
      double bestGain = 1;
      for (Market.Stock stock : market.stocks()) {
         int[] trade = desk.symbols().contains(stock.symbol) ? Market.bestTrade(stock.path, Market.CLOSE) : null;
         if (trade != null && stock.path[trade[1]] / stock.path[trade[0]] > bestGain) {
            bestGain = stock.path[trade[1]] / stock.path[trade[0]];
            bestSymbol = stock.symbol;
         }
      }
      if (bestSymbol != null) {
         String[] parts = hindsight(bestSymbol, Market.CLOSE);
         String[] lines = {"In hindsight, " + parts[0] + ",", parts[1] + "."};
         if (narrow) {
            // Four short lines: "In hindsight, NFLX's best trade today:", "buy at ...,", "sell at ..., +3.76%,", "about ...".
            int colon = parts[0].indexOf(": ");
            int about = parts[1].indexOf(", about");
            lines = new String[] {"In hindsight, " + parts[0].substring(0, colon + 1), parts[0].substring(colon + 2) + ",",
               parts[1].substring(0, about + 1), parts[1].substring(about + 2) + "."};
         }
         for (int i = 0; i < lines.length; i++) {
            text(g, fit(g, lines[i], SMALL, card.width - 36), SMALL, WARN, x, card.y + 138 + drop + i * 14);
         }
      }
      // How the race against the rivals went today.
      List<Desk.Standing> race = desk.standings(true);
      int place = desk.rank(true);
      Desk.Standing winner = race.get(0);
      String finish = place == 1 ? "You won today's race, " + (race.size() - 1) + " rivals behind you. R: standings"
         : "You came " + ordinal(place) + " of " + race.size() + " today; " + winner.name + " won with "
            + Money.percent(1, 1 + winner.today) + ". R: standings";
      Font font = narrow ? SMALL : PLAIN;
      text(g, fit(g, finish, font, card.width - 36), font, place == 1 ? UP : WARN, x, card.y + 100 + drop);

      // Net worth at every close so far (the last 14), today's bar last, against the starting cash.
      List<Double> closes = new ArrayList<>(desk.portfolio.closes);
      closes.add(worth);
      int from = Math.max(0, closes.size() - 14);
      Rectangle box = narrow ? new Rectangle(x, card.y + 210, card.width - 36, 60) : new Rectangle(x, card.y + 164, card.width - 36, 92);
      double low = Portfolio.START;
      double high = Portfolio.START;
      for (int i = from; i < closes.size(); i++) {
         low = Math.min(low, closes.get(i));
         high = Math.max(high, closes.get(i));
      }
      double room = Math.max(high - low, Portfolio.START * 0.02);
      low = Math.min(low, Portfolio.START) - room * 0.1;
      high = Math.max(high, Portfolio.START) + room * 0.1;
      int baseY = box.y + box.height - (int) Math.round((Portfolio.START - low) / (high - low) * box.height);
      int count = closes.size() - from;
      int slot = box.width / 14;
      for (int i = 0; i < count; i++) {
         double value = closes.get(from + i);
         int y = box.y + box.height - (int) Math.round((value - low) / (high - low) * box.height);
         int bx = box.x + i * slot + 3;
         g.setColor(value >= Portfolio.START ? UP : DOWN);
         if (i < count - 1) {
            g.setColor(withAlpha(g.getColor(), 150));
         }
         g.fillRect(bx, Math.min(y, baseY), slot - 6, Math.max(2, Math.abs(baseY - y)));
         String label = "D" + (from + i + 1);
         text(g, label, SMALL, MUTED, bx + (slot - 6 - width(g, label, SMALL)) / 2, box.y + box.height + 14);
      }
      g.setColor(new Color(255, 255, 255, 120));
      g.drawLine(box.x, baseY, box.x + box.width, baseY);
      text(g, Money.dollars(Portfolio.START), SMALL, MUTED, box.x + box.width - width(g, "$10,000.00", SMALL), baseY - 4);
      if (startOverArmed) {
         text(g, "Start over wipes every day so far. Click it again to be sure.", SMALL, WARN, x, card.y + card.height - 6);
      }
   }

   private void paintButtons(Graphics2D g) {
      for (Button button : buttons) {
         if (!button.shown.getAsBoolean()) {
            continue;
         }
         Rectangle r = button.bounds;
         boolean hover = r.contains(mouseX, mouseY);
         boolean on = button.fill == null && button.lit.getAsBoolean();
         // A trade button that would trade nothing now is faded (a press still says why).
         boolean faded = button.count != 0 && wouldTrade(button.count) == 0;
         Color fill = button.fill != null ? button.fill : on ? withAlpha(accent, 150) : new Color(255, 255, 255, 30);
         if (faded) {
            fill = withAlpha(fill, 70);
         }
         g.setColor(hover ? fill.brighter() : fill);
         g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
         g.setColor(new Color(255, 255, 255, hover ? 160 : 70));
         g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
         String label = button.label.equals("Start over") && startOverArmed ? "Sure? Click again" : button.label;
         Font font = r.height >= 30 ? BOLD : SMALL;
         Color ink = on ? new Color(10, 16, 40) : faded ? MUTED : TEXT;
         if (button.count != 0) {
            String note = fit(g, buttonNote(button.count), SMALL, r.width - 6);
            text(g, label, font, ink, r.x + (r.width - width(g, label, font)) / 2, r.y + r.height / 2 - 2);
            text(g, note, SMALL, faded ? MUTED : withAlpha(TEXT, 210), r.x + (r.width - width(g, note, SMALL)) / 2, r.y + r.height / 2 + 12);
         } else {
            text(g, label, font, ink, r.x + (r.width - width(g, label, font)) / 2, r.y + r.height / 2 + 5);
         }
      }
   }

   private void paintNews(Graphics2D g) {
      Market market = desk.market;
      List<Market.Headline> news = market.news(desk.symbols());
      if (news.isEmpty()) {
         text(g, "No news yet today.", PLAIN, MUTED, 14, newsY);
      } else {
         Market.Headline last = news.get(news.size() - 1);
         int x = 14;
         g.setColor(last.up ? UP : DOWN);
         int[] tx = {x, x + 10, x + 5};
         int[] ty = last.up ? new int[] {newsY - 2, newsY - 2, newsY - 11} : new int[] {newsY - 11, newsY - 11, newsY - 2};
         g.fillPolygon(tx, ty, 3);
         x += 16;
         String time = Market.clock(last.minute);
         text(g, time, BOLD, accent, x, newsY);
         x += width(g, time, BOLD) + 8;
         text(g, fit(g, last.text, PLAIN, width - 10 - x), PLAIN, TEXT, x, newsY);
      }

      // The ticker tape: every stock's price and change, sliding left.
      g.setColor(new Color(0, 0, 0, 110));
      g.fillRect(0, newsY + 8, width, 20);
      StringBuilder tape = new StringBuilder();
      List<String> symbols = desk.symbols();
      List<int[]> spans = new ArrayList<>();
      g.setFont(TAPE);
      for (String symbol : symbols) {
         Market.Stock stock = market.stock(symbol);
         if (stock == null) {
            continue;
         }
         String part = symbol + " " + Money.dollars(stock.path[market.minute]).substring(1) + " " + Money.percent(stock.open(), stock.path[market.minute]) + "     ";
         spans.add(new int[] {tape.length(), part.length(), stock.path[market.minute] >= stock.open() ? 1 : 0});
         tape.append(part);
      }
      if (tape.length() > 0) {
         String text = tape.toString();
         int full = width(g, text, TAPE);
         int start = -(ticks % Math.max(1, full));
         for (int copy = start; copy < width; copy += full) {
            for (int[] span : spans) {
               String part = text.substring(span[0], span[0] + span[1]);
               int px = copy + width(g, text.substring(0, span[0]), TAPE);
               if (px > width || px + width(g, part, TAPE) < 0) {
                  continue;
               }
               text(g, part, TAPE, span[2] == 1 ? UP : DOWN, px, newsY + 23);
            }
         }
      }
      text(g, narrow ? "Practice market with simulated prices, not real quotes."
         : "Practice market with simulated prices. Day 1 opens at the last real quotes saved in stockCheck.csv (May 13, 2016).",
         SMALL, MUTED, 14, newsY + 39);
   }

   // ---- helpers ----

   private static Color withAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   private static void text(Graphics2D g, String text, Font font, Color color, int x, int y) {
      g.setFont(font);
      g.setColor(color);
      g.drawString(text, x, y);
   }

   private static int width(Graphics2D g, String text, Font font) {
      return g.getFontMetrics(font).stringWidth(text);
   }

   /** The bounds of the buttons showing now (for LayoutCheck). */
   List<Rectangle> shownButtons() {
      List<Rectangle> shown = new ArrayList<>();
      for (Button button : buttons) {
         if (button.shown.getAsBoolean()) {
            shown.add(button.bounds);
         }
      }
      return shown;
   }

   /** Where the chart card's tabs start: the title row's text keeps left of them. */
   private int tabsX() {
      return card.x + card.width - 138;
   }

   /** The text when it fits {@code room} pixels, else the shorter one (cut short if even that does not fit). */
   private static String fitOr(Graphics2D g, String text, String shorter, Font font, int room) {
      return fit(g, width(g, text, font) <= room ? text : shorter, font, room);
   }

   /** The text, cut short with "..." to fit {@code room} pixels. */
   private static String fit(Graphics2D g, String text, Font font, int room) {
      if (width(g, text, font) <= room) {
         return text;
      }
      String cut = text;
      while (cut.length() > 1 && width(g, cut + "...", font) > room) {
         cut = cut.substring(0, cut.length() - 1);
      }
      return cut + "...";
   }
}
