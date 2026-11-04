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
   static final int WIDTH = 720;
   static final int HEIGHT = 440;
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
   /** Whether the chart card shows the race against the rivals instead of the picked stock. */
   private boolean race;
   private final Rectangle[] rows = new Rectangle[Desk.MAX_SYMBOLS * 2];
   DeskPanel(Desk desk) {
      this.desk = desk;
      setPreferredSize(new Dimension(WIDTH, HEIGHT));
      setFocusable(true);
      BooleanSupplier open = () -> !desk.market.closed();
      BooleanSupplier closed = () -> desk.market.closed();
      BooleanSupplier always = () -> true;
      String[] speeds = {"Pause", "Play", "Fast"};
      char[] speedKeys = {' ', 'p', 'f'};
      for (int i = 0; i < speeds.length; i++) {
         int speed = i;
         Button button = new Button(speeds[i], speedKeys[i], null, new Rectangle(236 + i * 64, 10, 60, 26), always, () -> speed(speed));
         button.lit = () -> desk.speed == speed;
         buttons.add(button);
      }
      // The chart card's two tabs: the picked stock's chart, or the race against the rivals (R toggles).
      Button chartTab = new Button("Chart", 'c', null, new Rectangle(588, 60, 56, 22), always, () -> race = false);
      chartTab.lit = () -> !race;
      buttons.add(chartTab);
      Button raceTab = new Button("Race", 'r', null, new Rectangle(648, 60, 56, 22), always, () -> race = true);
      raceTab.lit = () -> race;
      buttons.add(raceTab);
      String[] labels = {"Buy 1", "Buy 10", "Buy max", "Sell 1", "Sell 10", "Sell all"};
      char[] keys = {'b', 'v', 'm', 's', 'd', 'a'};
      int[] counts = {1, 10, Integer.MAX_VALUE, -1, -10, Integer.MIN_VALUE};
      for (int i = 0; i < labels.length; i++) {
         int count = counts[i];
         Color fill = count > 0 ? new Color(30, 140, 80) : new Color(170, 50, 60);
         buttons.add(new Button(labels[i], keys[i], fill, new Rectangle(272 + i * 74, 340, 68, 30), open, () -> trade(count)));
      }
      buttons.add(new Button("Next day", '\n', new Color(30, 140, 80), new Rectangle(290, 330, 150, 32), closed, this::nextDay));
      buttons.add(new Button("Start over", 'x', null, new Rectangle(450, 330, 150, 32), closed, this::startOver));

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
      int every = desk.speed == Desk.FAST ? 2 : desk.speed == Desk.NORMAL ? 10 : 0;
      if (every > 0 && ticks % every == 0) {
         boolean opening = desk.market.minute == 0;
         if (desk.tick()) {
            bell.ring(3);
            startOverArmed = false;
         } else if (opening && !desk.market.closed()) {
            bell.ring(1);
         }
      }
      repaint();
   }

   private void speed(int speed) {
      desk.setSpeed(speed);
   }

   private String symbol() {
      List<String> symbols = desk.symbols();
      return symbols.isEmpty() ? null : symbols.get(Math.max(0, Math.min(selected, symbols.size() - 1)));
   }

   private void trade(int count) {
      String symbol = symbol();
      if (symbol == null) {
         say("Add a ticker above first.", WARN);
         return;
      }
      double price = desk.market.price(symbol);
      if (count > 0) {
         int done = desk.buy(symbol, count);
         if (done == 0) {
            say("Not enough cash for a share of " + symbol + " at " + Money.dollars(price) + ".", WARN);
         } else {
            say("Bought " + done + " " + symbol + " at " + Money.dollars(price) + ".", UP);
         }
      } else {
         int done = desk.sell(symbol, count == Integer.MIN_VALUE ? Integer.MAX_VALUE : -count);
         if (done == 0) {
            say("You hold no " + symbol + " to sell.", WARN);
         } else {
            say("Sold " + done + " " + symbol + " at " + Money.dollars(price) + ".", DOWN);
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
         race = !race;
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
      if (desk.market.closed()) {
         if (race) {
            paintStandings(g);
         } else {
            paintClose(g);
         }
      } else {
         if (race) {
            paintRace(g);
         } else {
            paintChart(g);
         }
         paintPosition(g);
      }
      paintButtons(g);
      paintNews(g);
   }

   private void paintHeader(Graphics2D g) {
      Market market = desk.market;
      g.setColor(new Color(0, 0, 0, 70));
      g.fillRect(0, 0, WIDTH, 46);
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

      double worth = desk.portfolio.worth(market);
      double opening = desk.portfolio.openingWorth();
      String value = Money.dollars(worth);
      int valueX = WIDTH - 14 - width(g, value, BIG);
      text(g, value, BIG, TEXT, valueX, 38);
      // Your place among the five traders by net worth.
      int rank = desk.rank(false);
      String place = ordinal(rank) + " of " + (desk.rivals.size() + 1);
      text(g, place, BOLD, rank == 1 ? UP : WARN, valueX - 12 - width(g, place, BOLD), 37);
      String today = Money.change(worth - opening) + " today";
      String cash = "Cash " + Money.dollars(desk.portfolio.cash) + "   ";
      int right = WIDTH - 14 - width(g, today, SMALL);
      text(g, today, SMALL, worth >= opening ? UP : DOWN, right, 17);
      text(g, cash, SMALL, MUTED, right - width(g, cash, SMALL), 17);
   }

   private void paintWatch(Graphics2D g) {
      List<String> symbols = desk.symbols();
      for (int i = 0; i < rows.length; i++) {
         rows[i] = null;
      }
      if (symbols.isEmpty()) {
         text(g, "Type tickers in the box above,", PLAIN, MUTED, 20, 80);
         text(g, "then press Enter.", PLAIN, MUTED, 20, 98);
         return;
      }
      int rowHeight = Math.min(40, 322 / symbols.size());
      for (int i = 0; i < symbols.size() && i < rows.length; i++) {
         String symbol = symbols.get(i);
         Market.Stock stock = desk.market.stock(symbol);
         if (stock == null) {
            continue;
         }
         Rectangle row = new Rectangle(10, 54 + i * rowHeight, 250, rowHeight - 4);
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
         int top = row.y + Math.min(17, row.height / 2 + 4);
         text(g, symbol, TITLE, TEXT, row.x + 12, top);
         String price1 = Money.dollars(price);
         text(g, price1, BOLD, TEXT, row.x + 160 - width(g, price1, BOLD), top);
         if (row.height >= 30) {
            int held = desk.portfolio.shares(symbol);
            String note = held > 0 ? held + " held" : "";
            if (held > 0 && !desk.watch().contains(symbol)) {
               note += ", off list";
            }
            text(g, note, SMALL, accent, row.x + 12, row.y + 31);
            String change = Money.percent(stock.open(), price);
            text(g, change, SMALL, up ? UP : DOWN, row.x + 160 - width(g, change, SMALL), row.y + 31);
         }
         paintLine(g, stock, new Rectangle(row.x + 170, row.y + 5, 72, row.height - 10), false);
      }
   }

   /** The stock's day so far inside {@code box}: the line, and for the big chart a fill, grid, labels and trades. */
   private void paintLine(Graphics2D g, Market.Stock stock, Rectangle box, boolean big) {
      int now = desk.market.minute;
      double low = stock.open();
      double high = stock.open();
      for (int m = 0; m <= now; m++) {
         low = Math.min(low, stock.path[m]);
         high = Math.max(high, stock.path[m]);
      }
      // At least a 1% range, so a quiet morning is not drawn as wild swings.
      double room = Math.max(high - low, stock.open() * 0.01);
      double mid = (high + low) / 2;
      low = mid - room * 0.6;
      high = mid + room * 0.6;
      double lowF = low;
      double span = high - low;
      java.util.function.DoubleToIntFunction yOf = price -> box.y + box.height - (int) Math.round((price - lowF) / span * box.height);
      java.util.function.IntUnaryOperator xOf = m -> box.x + (int) Math.round((double) m * box.width / Market.CLOSE);
      boolean up = stock.path[now] >= stock.open();
      Color line = up ? UP : DOWN;

      if (big) {
         g.setFont(SMALL);
         for (int k = 0; k <= 4; k++) {
            double price = low + span * k / 4;
            int y = yOf.applyAsInt(price);
            g.setColor(new Color(255, 255, 255, 22));
            g.drawLine(box.x, y, box.x + box.width, y);
            text(g, Money.dollars(price), SMALL, MUTED, box.x + box.width + 6, y + 4);
         }
         int[] hours = {30, 150, 270, 390};
         String[] names = {"10 AM", "12 PM", "2 PM", "4 PM"};
         for (int k = 0; k < hours.length; k++) {
            int x = xOf.applyAsInt(hours[k]);
            g.setColor(new Color(255, 255, 255, 22));
            g.drawLine(x, box.y, x, box.y + box.height);
            text(g, names[k], SMALL, MUTED, x - width(g, names[k], SMALL) / 2, box.y + box.height + 14);
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

      if (big) {
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
            }
         }
      }
   }

   private void paintChart(Graphics2D g) {
      Rectangle card = new Rectangle(272, 54, 438, 222);
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
      text(g, fit(g, change, PLAIN, 580 - x), PLAIN, up ? UP : DOWN, x, card.y + 24);
      paintLine(g, stock, new Rectangle(card.x + 14, card.y + 38, card.width - 84, card.height - 62), true);
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
      Rectangle card = new Rectangle(272, 54, 438, 222);
      g.setColor(new Color(255, 255, 255, 14));
      g.fillRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      text(g, "Race today", BIG, accent, card.x + 14, card.y + 25);
      Market market = desk.market;
      int now = market.minute;
      List<Desk.Standing> standings = desk.standings(true);

      // Each trader's change since the open, minute by minute, on one scale (at least +/-0.5%).
      Rectangle box = new Rectangle(card.x + 14, card.y + 44, 232, 140);
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
      java.util.function.IntUnaryOperator xOf = m -> box.x + (int) Math.round((double) m * box.width / Market.CLOSE);
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
      text(g, "4:00", SMALL, MUTED, box.x + box.width - width(g, "4:00", SMALL), box.y + box.height + 13);
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
      Rectangle card = new Rectangle(272, 54, 438, 322);
      g.setColor(new Color(0, 0, 0, 60));
      g.fillRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      g.setColor(withAlpha(accent, 120));
      g.drawRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      text(g, "Standings, day " + desk.market.day, BIG, accent, card.x + 18, card.y + 30);
      int todayRight = card.x + 340;
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
      }
      text(g, fit(g, first, PLAIN, 438), PLAIN, TEXT, 274, 298);
      text(g, fit(g, second, PLAIN, 438), PLAIN, color, 274, 318);
   }

   private void paintClose(Graphics2D g) {
      Rectangle card = new Rectangle(272, 54, 438, 322);
      g.setColor(new Color(0, 0, 0, 60));
      g.fillRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      g.setColor(withAlpha(accent, 120));
      g.drawRoundRect(card.x, card.y, card.width, card.height, 12, 12);
      Market market = desk.market;
      double worth = desk.portfolio.worth(market);
      double opening = desk.portfolio.openingWorth();
      int x = card.x + 18;
      text(g, "Closing bell, day " + market.day, BIG, accent, x, card.y + 30);
      text(g, "Net worth " + Money.dollars(worth), TITLE, TEXT, x, card.y + 56);
      text(g, "Today " + Money.change(worth - opening) + " (" + Money.percent(opening, worth) + ")", PLAIN, worth >= opening ? UP : DOWN, x, card.y + 78);
      text(g, "Since day 1 " + Money.change(worth - Portfolio.START) + " (" + Money.percent(Portfolio.START, worth) + ")", PLAIN,
         worth >= Portfolio.START ? UP : DOWN, x + 210, card.y + 78);
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
         text(g, movers, SMALL, MUTED, x, card.y + 120);
      }
      // How the race against the rivals went today.
      List<Desk.Standing> race = desk.standings(true);
      int place = desk.rank(true);
      Desk.Standing winner = race.get(0);
      String finish = place == 1 ? "You won today's race, " + (race.size() - 1) + " rivals behind you. R: standings"
         : "You came " + ordinal(place) + " of " + race.size() + " today; " + winner.name + " won with "
            + Money.percent(1, 1 + winner.today) + ". R: standings";
      text(g, fit(g, finish, PLAIN, card.width - 36), PLAIN, place == 1 ? UP : WARN, x, card.y + 100);

      // Net worth at every close so far (the last 14), today's bar last, against the starting cash.
      List<Double> closes = new ArrayList<>(desk.portfolio.closes);
      closes.add(worth);
      int from = Math.max(0, closes.size() - 14);
      Rectangle box = new Rectangle(x, card.y + 132, card.width - 36, 124);
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
         Color fill = button.fill != null ? button.fill : on ? withAlpha(accent, 150) : new Color(255, 255, 255, 30);
         g.setColor(hover ? fill.brighter() : fill);
         g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
         g.setColor(new Color(255, 255, 255, hover ? 160 : 70));
         g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
         String label = button.label.equals("Start over") && startOverArmed ? "Sure? Click again" : button.label;
         Font font = r.height >= 30 ? BOLD : SMALL;
         text(g, label, font, on ? new Color(10, 16, 40) : TEXT, r.x + (r.width - width(g, label, font)) / 2, r.y + r.height / 2 + 5);
      }
   }

   private void paintNews(Graphics2D g) {
      Market market = desk.market;
      List<Market.Headline> news = market.news(desk.symbols());
      if (news.isEmpty()) {
         text(g, "No news yet today.", PLAIN, MUTED, 14, 398);
      } else {
         Market.Headline last = news.get(news.size() - 1);
         int x = 14;
         g.setColor(last.up ? UP : DOWN);
         int[] tx = {x, x + 10, x + 5};
         int[] ty = last.up ? new int[] {396, 396, 387} : new int[] {387, 387, 396};
         g.fillPolygon(tx, ty, 3);
         x += 16;
         String time = Market.clock(last.minute);
         text(g, time, BOLD, accent, x, 398);
         text(g, last.text, PLAIN, TEXT, x + width(g, time, BOLD) + 8, 398);
      }

      // The ticker tape: every stock's price and change, sliding left.
      g.setColor(new Color(0, 0, 0, 110));
      g.fillRect(0, 406, WIDTH, 20);
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
         for (int copy = start; copy < WIDTH; copy += full) {
            for (int[] span : spans) {
               String part = text.substring(span[0], span[0] + span[1]);
               int px = copy + width(g, text.substring(0, span[0]), TAPE);
               if (px > WIDTH || px + width(g, part, TAPE) < 0) {
                  continue;
               }
               text(g, part, TAPE, span[2] == 1 ? UP : DOWN, px, 421);
            }
         }
      }
      text(g, "Practice market with simulated prices. Day 1 opens at the last real quotes saved in stockCheck.csv (May 13, 2016).",
         SMALL, MUTED, 14, 437);
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
