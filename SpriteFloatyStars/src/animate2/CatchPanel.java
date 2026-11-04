package animate2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Star Catcher: a menu of the six nights, then the night itself (gold stars and black holes over one of the original
 * animation modes, with a clock and a catch count), then a result card with the stars won. Played with the mouse,
 * or with the arrow keys moving a net and Space catching under it.
 */
@SuppressWarnings("serial")
public final class CatchPanel extends JPanel {
   public static final int WIDTH = 640;
   public static final int HEIGHT = 480;
   private static final int HUD = 46;
   private static final double INTRO_SECONDS = 1.4;
   private static final double NET_SPEED = 380.0;

   private static final Color SKY = new Color(8, 12, 40);
   private static final Color SKY_SHADE = new Color(8, 12, 40, 110);
   private static final Color GOLD = new Color(255, 205, 40);
   private static final Color GOLD_EDGE = new Color(170, 110, 0);
   private static final Color HOLE_RING = new Color(170, 90, 255);
   private static final Color TEXT = new Color(235, 240, 255);
   private static final Color DIM = new Color(150, 160, 200);
   private static final Color CARD = new Color(20, 26, 70, 235);
   private static final Color BAD = new Color(255, 90, 90);

   private enum Screen { MENU, INTRO, PLAYING, RESULT }

   /** A short-lived ring and label where a click landed. */
   private static final class Effect {
      final double x;
      final double y;
      final String text;
      final Color color;
      double age;

      Effect(double x, double y, String text, Color color) {
         this.x = x;
         this.y = y;
         this.text = text;
         this.color = color;
      }
   }

   private final NightRecords records;
   private final Random random = new Random();
   private final List<Effect> effects = new ArrayList<>();
   private final Rectangle[] nightCards = new Rectangle[Night.values().length];
   private final Rectangle againButton = new Rectangle();
   private final Rectangle nextButton = new Rectangle();
   private final Rectangle menuButton = new Rectangle();
   private final boolean[] arrows = new boolean[4];
   private final Timer timer;
   private Screen screen = Screen.MENU;
   private Night selected;
   private CatchGame game;
   private StarField sky;
   private double screenSeconds;
   private double spin;
   private double netX = WIDTH / 2.0;
   private double netY = HUD + (HEIGHT - HUD) / 2.0;
   private boolean netShown;
   private boolean newBest;
   private long lastTick = System.nanoTime();

   public CatchPanel(NightRecords records) {
      this.records = records;
      this.selected = records.suggested();
      this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
      this.setFocusable(true);
      this.sky = new StarField(this.selected.mode(), new Dimension(WIDTH, HEIGHT - HUD), 90, 5, false);
      for (int i = 0; i < this.nightCards.length; i++) {
         this.nightCards[i] = new Rectangle(40 + (i % 3) * 190, 150 + (i / 3) * 130, 180, 118);
      }
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            CatchPanel.this.netShown = false;
            CatchPanel.this.press(event.getX(), event.getY());
            CatchPanel.this.requestFocusInWindow();
         }
      };
      this.addMouseListener(mouse);
      this.timer = new Timer(15, event -> this.tick());
      this.timer.start();
   }

   /** Key handling, shared by the panel and its window so the keys work whichever has focus. */
   public KeyAdapter keys() {
      return new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent event) {
            CatchPanel.this.keyPressed(event.getKeyCode());
         }

         @Override
         public void keyReleased(KeyEvent event) {
            CatchPanel.this.arrow(event.getKeyCode(), false);
         }
      };
   }

   public void stop() {
      this.timer.stop();
   }

   private boolean arrow(int code, boolean down) {
      int index = code == KeyEvent.VK_UP ? 0 : code == KeyEvent.VK_DOWN ? 1 : code == KeyEvent.VK_LEFT ? 2 : code == KeyEvent.VK_RIGHT ? 3 : -1;
      if (index >= 0) {
         this.arrows[index] = down;
      }
      return index >= 0;
   }

   private void keyPressed(int code) {
      boolean confirm = code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE;
      boolean toMenu = code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_M;
      if (this.screen == Screen.MENU) {
         int index = this.selected.ordinal();
         if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
            this.select(index + (code == KeyEvent.VK_LEFT ? -1 : 1));
         } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) {
            this.select(index + (code == KeyEvent.VK_UP ? -3 : 3));
         } else if (code >= KeyEvent.VK_1 && code < KeyEvent.VK_1 + Night.values().length) {
            this.select(code - KeyEvent.VK_1);
            this.start(this.selected);
         } else if (confirm || code == KeyEvent.VK_N) {
            this.start(this.selected);
         }
      } else if (this.screen == Screen.INTRO || this.screen == Screen.PLAYING) {
         if (this.arrow(code, true)) {
            this.netShown = true;
         } else if (confirm && this.screen == Screen.PLAYING) {
            this.netShown = true;
            this.catchAt(this.netX, this.netY);
         } else if (toMenu) {
            this.screen = Screen.MENU;
         } else if (code == KeyEvent.VK_R) {
            this.start(this.game.night());
         }
      } else if (this.screen == Screen.RESULT) {
         if (code == KeyEvent.VK_R) {
            this.start(this.game.night());
         } else if (confirm || code == KeyEvent.VK_N) {
            this.start(this.nextNight());
         } else if (toMenu) {
            this.screen = Screen.MENU;
         }
      }
      this.repaint();
   }

   /** Moves the menu's choice, skipping nights that are still locked. */
   private void select(int index) {
      if (index >= 0 && index < Night.values().length && this.records.isOpen(Night.values()[index])) {
         this.selected = Night.values()[index];
         // The menu's sky turns to the chosen night's animation mode.
         this.sky = new StarField(this.selected.mode(), new Dimension(WIDTH, HEIGHT - HUD), 90, 5, false);
      }
   }

   /** After a night: the next one when it is open, else the same one again. */
   private Night nextNight() {
      Night night = this.game.night();
      if (night.ordinal() + 1 < Night.values().length && this.records.isOpen(Night.values()[night.ordinal() + 1])) {
         return Night.values()[night.ordinal() + 1];
      }
      return night;
   }

   private void start(Night night) {
      this.selected = night;
      this.game = new CatchGame(night, WIDTH, HEIGHT - HUD, this.random);
      this.sky = new StarField(night.mode(), new Dimension(WIDTH, HEIGHT - HUD), 90, 5, false);
      this.effects.clear();
      this.newBest = false;
      this.screen = Screen.INTRO;
      this.screenSeconds = 0;
   }

   private void press(int x, int y) {
      if (this.screen == Screen.MENU) {
         for (int i = 0; i < this.nightCards.length; i++) {
            if (this.nightCards[i].contains(x, y) && this.records.isOpen(Night.values()[i])) {
               this.start(Night.values()[i]);
            }
         }
      } else if (this.screen == Screen.PLAYING) {
         this.catchAt(x, y);
      } else if (this.screen == Screen.RESULT) {
         if (this.againButton.contains(x, y)) {
            this.start(this.game.night());
         } else if (this.nextButton.contains(x, y)) {
            this.start(this.nextNight());
         } else if (this.menuButton.contains(x, y)) {
            this.screen = Screen.MENU;
         }
      }
      this.repaint();
   }

   private void catchAt(double x, double y) {
      if (y < HUD) {
         return;
      }
      int comboBefore = this.game.combo();
      CatchGame.Hit hit = this.game.click(x, y - HUD);
      if (hit == CatchGame.Hit.GOLD) {
         int combo = this.game.combo();
         String text = combo >= 3 ? "Combo x" + combo + "  +" + CatchGame.COMBO_BONUS + " s" : "+1";
         this.effects.add(new Effect(x, y, text, GOLD));
      } else if (hit == CatchGame.Hit.HOLE) {
         this.effects.add(new Effect(x, y, "-" + (int)CatchGame.HOLE_PENALTY + " s", BAD));
      } else if (hit == CatchGame.Hit.MISS && comboBefore >= 3) {
         this.effects.add(new Effect(x, y, "Combo over", DIM));
      }
      this.checkOver();
   }

   private void checkOver() {
      if (this.screen == Screen.PLAYING && this.game.state() != CatchGame.State.PLAYING) {
         this.newBest = this.records.add(this.game);
         this.screen = Screen.RESULT;
         this.screenSeconds = 0;
      }
   }

   private void tick() {
      long now = System.nanoTime();
      double seconds = Math.min(0.05, (now - this.lastTick) / 1.0E9);
      this.lastTick = now;
      this.spin += seconds;
      this.screenSeconds += seconds;
      this.sky.advance(seconds * 0.5, WIDTH, HEIGHT - HUD);
      if (this.screen == Screen.INTRO && this.screenSeconds >= INTRO_SECONDS) {
         this.screen = Screen.PLAYING;
      }
      if (this.screen == Screen.INTRO || this.screen == Screen.PLAYING) {
         double dx = (this.arrows[3] ? 1 : 0) - (this.arrows[2] ? 1 : 0);
         double dy = (this.arrows[1] ? 1 : 0) - (this.arrows[0] ? 1 : 0);
         this.netX = Math.max(0, Math.min(WIDTH, this.netX + dx * NET_SPEED * seconds));
         this.netY = Math.max(HUD, Math.min(HEIGHT, this.netY + dy * NET_SPEED * seconds));
      }
      if (this.screen == Screen.PLAYING) {
         this.game.step(seconds);
         this.checkOver();
      }
      for (Iterator<Effect> it = this.effects.iterator(); it.hasNext();) {
         Effect effect = it.next();
         effect.age += seconds;
         if (effect.age > 0.8) {
            it.remove();
         }
      }
      this.repaint();
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(SKY);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      g.translate(0, HUD);
      this.sky.drawStars(g);
      g.setColor(SKY_SHADE);
      g.fillRect(0, 0, WIDTH, HEIGHT - HUD);
      if (this.screen != Screen.MENU) {
         this.drawBodies(g);
      }
      g.translate(0, -HUD);
      this.drawEffects(g);
      if (this.screen == Screen.MENU) {
         this.drawMenu(g);
         return;
      }
      if (this.netShown && this.screen != Screen.RESULT) {
         this.drawNet(g);
      }
      this.drawHud(g);
      if (this.screen == Screen.INTRO) {
         this.drawIntro(g);
      } else if (this.screen == Screen.RESULT) {
         this.drawResult(g);
      }
   }

   private void drawBodies(Graphics2D g) {
      Night night = this.game.night();
      for (CatchGame.Body body : this.game.bodies()) {
         int x = (int)body.x;
         int y = (int)body.y;
         if (body.kind == CatchGame.Kind.HOLE) {
            int r = (int)CatchGame.HOLE_RADIUS;
            double pulse = 0.5 + 0.5 * Math.sin(this.spin * 3.0 + body.x * 0.01);
            g.setColor(new Color(170, 90, 255, 40 + (int)(50 * pulse)));
            g.fillOval(x - r - 9, y - r - 9, 2 * r + 18, 2 * r + 18);
            g.setColor(Color.black);
            g.fillOval(x - r, y - r, 2 * r, 2 * r);
            g.setStroke(new BasicStroke(3f));
            g.setColor(HOLE_RING);
            g.drawOval(x - r, y - r, 2 * r, 2 * r);
            g.setStroke(new BasicStroke(1f));
         } else {
            // Fades out over its last second when the night makes stars twinkle out.
            double fade = night.life() > 0 ? Math.max(0.15, Math.min(1.0, night.life() - body.age)) : 1.0;
            // And fades in over its first quarter second.
            fade = Math.min(fade, Math.min(1.0, 0.2 + body.age * 4.0));
            int r = (int)CatchGame.GOLD_RADIUS;
            // A twinkle: a thin cross of light that grows and shrinks behind the star.
            int ray = r + 6 + (int)(6 * Math.sin(this.spin * 5.0 + body.y * 0.05));
            g.setColor(new Color(255, 240, 170, (int)(170 * fade)));
            g.drawLine(x - ray, y, x + ray, y);
            g.drawLine(x, y - ray, x, y + ray);
            this.drawStar(g, x, y, r + 2, this.spin * 1.5 + body.x * 0.02, new Color(255, 205, 40, (int)(255 * fade)),
               new Color(170, 110, 0, (int)(255 * fade)));
         }
      }
   }

   private void drawStar(Graphics2D g, int x, int y, int r, double angle, Color fill, Color edge) {
      int[] xs = new int[10];
      int[] ys = new int[10];
      for (int i = 0; i < 10; i++) {
         double a = angle + i * Math.PI / 5.0 - Math.PI / 2.0;
         double radius = i % 2 == 0 ? r : r * 0.45;
         xs[i] = x + (int)Math.round(Math.cos(a) * radius);
         ys[i] = y + (int)Math.round(Math.sin(a) * radius);
      }
      g.setColor(fill);
      g.fillPolygon(xs, ys, 10);
      g.setColor(edge);
      g.drawPolygon(xs, ys, 10);
   }

   private void drawEffects(Graphics2D g) {
      g.setFont(new Font("SansSerif", Font.BOLD, 15));
      for (Effect effect : this.effects) {
         double t = effect.age / 0.8;
         int alpha = (int)(255 * (1.0 - t));
         Color c = effect.color;
         g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
         int r = (int)(18 + 30 * t);
         g.setStroke(new BasicStroke(2f));
         g.drawOval((int)effect.x - r, (int)effect.y - r, 2 * r, 2 * r);
         g.setStroke(new BasicStroke(1f));
         int width = g.getFontMetrics().stringWidth(effect.text);
         int textX = Math.max(4, Math.min(WIDTH - width - 4, (int)effect.x - width / 2));
         g.drawString(effect.text, textX, (int)Math.max(HUD + 16, effect.y - 26 - 24 * t));
      }
   }

   private void drawNet(Graphics2D g) {
      int x = (int)this.netX;
      int y = (int)this.netY;
      g.setStroke(new BasicStroke(2f));
      g.setColor(TEXT);
      g.drawOval(x - 14, y - 14, 28, 28);
      g.drawLine(x - 22, y, x - 8, y);
      g.drawLine(x + 8, y, x + 22, y);
      g.drawLine(x, y - 22, x, y - 8);
      g.drawLine(x, y + 8, x, y + 22);
      g.setStroke(new BasicStroke(1f));
   }

   private void drawHud(Graphics2D g) {
      g.setColor(new Color(14, 18, 52));
      g.fillRect(0, 0, WIDTH, HUD);
      g.setColor(new Color(60, 70, 130));
      g.drawLine(0, HUD - 1, WIDTH, HUD - 1);
      Night night = this.game.night();
      g.setFont(new Font("SansSerif", Font.BOLD, 14));
      g.setColor(TEXT);
      g.drawString("Night " + night.number() + ": " + night.label(), 12, 20);
      g.setFont(new Font("SansSerif", Font.PLAIN, 12));
      g.setColor(DIM);
      g.drawString("Esc: nights   R: restart", 12, 37);

      // Catch count: one small star per gold star to catch, lit once caught.
      int goal = night.goal();
      int step = Math.min(16, 200 / goal);
      int left = 200;
      for (int i = 0; i < goal; i++) {
         boolean lit = i < this.game.caught();
         this.drawStar(g, left + i * step + 7, 23, 7, 0, lit ? GOLD : new Color(50, 58, 110), lit ? GOLD_EDGE : new Color(90, 100, 160));
      }
      g.setFont(new Font("SansSerif", Font.BOLD, 13));
      g.setColor(TEXT);
      g.drawString(this.game.caught() + " / " + goal, left + goal * step + 8, 28);

      // The clock: a bar that empties, red in the last five seconds, with the stars thresholds marked.
      int barX = 470;
      int barW = 158;
      double left10 = Math.min(Night.SECONDS, this.game.secondsLeft());
      g.setColor(new Color(40, 46, 96));
      g.fillRoundRect(barX, 14, barW, 18, 9, 9);
      g.setColor(left10 <= Night.TWO_STARS ? BAD : left10 <= Night.THREE_STARS ? new Color(255, 170, 60) : new Color(90, 200, 255));
      g.fillRoundRect(barX, 14, (int)(barW * left10 / Night.SECONDS), 18, 9, 9);
      g.setColor(new Color(255, 255, 255, 120));
      for (double mark : new double[]{Night.THREE_STARS, Night.TWO_STARS}) {
         int mx = barX + (int)(barW * mark / Night.SECONDS);
         g.drawLine(mx, 12, mx, 34);
      }
      String clock = String.format("%.1f s", this.game.secondsLeft());
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      g.setColor(Color.white);
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(clock, barX + barW - metrics.stringWidth(clock) - 8, 28);
      if (this.game.combo() >= 3 && this.screen == Screen.PLAYING) {
         g.setColor(GOLD);
         g.drawString("Combo x" + this.game.combo(), left + goal * step + 58, 28);
      }
   }

   private void drawCentered(Graphics2D g, String text, int y) {
      g.drawString(text, (WIDTH - g.getFontMetrics().stringWidth(text)) / 2, y);
   }

   private void drawIntro(Graphics2D g) {
      Night night = this.game.night();
      g.setColor(CARD);
      g.fillRoundRect(WIDTH / 2 - 190, 170, 380, 120, 18, 18);
      g.setColor(GOLD);
      g.setFont(new Font("SansSerif", Font.BOLD, 26));
      this.drawCentered(g, "Night " + night.number() + ": " + night.label(), 212);
      g.setColor(TEXT);
      g.setFont(new Font("SansSerif", Font.PLAIN, 15));
      this.drawCentered(g, "Catch " + night.goal() + " gold stars in " + (int)Night.SECONDS + " seconds", 242);
      g.setColor(DIM);
      g.setFont(new Font("SansSerif", Font.PLAIN, 13));
      this.drawCentered(g, this.rule(night), 268);
   }

   /** What is new about a night, on its intro card and menu card. */
   private String rule(Night night) {
      if (night.pull() > 0) {
         return "Black holes pull the stars in";
      }
      if (night.life() > 0) {
         return "Stars twinkle out after " + (night.life() == (int)night.life() ? String.valueOf((int)night.life()) : String.valueOf(night.life())) + " s";
      }
      if (night.holes() > 0) {
         return "Black holes cost " + (int)CatchGame.HOLE_PENALTY + " seconds";
      }
      return "Click a star, or aim with the arrows and press Space";
   }

   private void drawResult(Graphics2D g) {
      Night night = this.game.night();
      boolean caught = this.game.state() == CatchGame.State.CAUGHT;
      int cardX = WIDTH / 2 - 200;
      int cardY = 120;
      g.setColor(CARD);
      g.fillRoundRect(cardX, cardY, 400, 250, 20, 20);
      g.setColor(new Color(90, 100, 180));
      g.drawRoundRect(cardX, cardY, 400, 250, 20, 20);
      g.setFont(new Font("SansSerif", Font.BOLD, 26));
      g.setColor(caught ? GOLD : BAD);
      this.drawCentered(g, caught ? "Night caught!" : "The clock ran out", cardY + 44);
      int stars = this.game.stars();
      for (int i = 0; i < 3; i++) {
         // The stars pop in one after another.
         boolean lit = i < stars && this.screenSeconds > 0.25 + i * 0.25;
         this.drawStar(g, WIDTH / 2 - 60 + i * 60, cardY + 95, 24, 0, lit ? GOLD : new Color(50, 58, 110), lit ? GOLD_EDGE : new Color(90, 100, 160));
      }
      g.setFont(new Font("SansSerif", Font.PLAIN, 14));
      g.setColor(TEXT);
      String line = caught
         ? String.format("%.1f seconds left", this.game.secondsLeft())
         : this.game.caught() + " of " + night.goal() + " gold stars caught";
      this.drawCentered(g, line, cardY + 145);
      g.setColor(newBest ? GOLD : DIM);
      g.setFont(new Font("SansSerif", newBest ? Font.BOLD : Font.PLAIN, 13));
      String sub;
      if (this.newBest) {
         sub = "New best!";
      } else if (!caught) {
         sub = "Catch them all before the bar empties";
      } else if (stars < 3) {
         sub = "3 stars: catch them with " + (int)Night.THREE_STARS + " seconds left";
      } else {
         sub = "Best: " + String.format("%.1f", this.records.bestTenths(night) / 10.0) + " seconds left";
      }
      this.drawCentered(g, sub, cardY + 168);
      int buttonY = cardY + 190;
      if (this.nextNight() != night) {
         this.button(g, this.againButton, cardX + 20, buttonY, 110, "Again (R)", false);
         this.button(g, this.nextButton, cardX + 145, buttonY, 110, "Next night (N)", true);
         this.button(g, this.menuButton, cardX + 270, buttonY, 110, "Nights (Esc)", false);
      } else {
         this.nextButton.setBounds(0, 0, 0, 0);
         this.button(g, this.againButton, cardX + 70, buttonY, 120, "Again (R)", true);
         this.button(g, this.menuButton, cardX + 210, buttonY, 120, "Nights (Esc)", false);
      }
   }

   private void button(Graphics2D g, Rectangle bounds, int x, int y, int width, String label, boolean primary) {
      bounds.setBounds(x, y, width, 36);
      g.setColor(primary ? GOLD : new Color(44, 52, 110));
      g.fillRoundRect(x, y, width, 36, 12, 12);
      g.setColor(primary ? GOLD_EDGE : new Color(90, 100, 180));
      g.drawRoundRect(x, y, width, 36, 12, 12);
      g.setFont(new Font("SansSerif", Font.BOLD, 13));
      g.setColor(primary ? new Color(40, 24, 0) : TEXT);
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(label, x + (width - metrics.stringWidth(label)) / 2, y + 23);
   }

   private void drawMenu(Graphics2D g) {
      g.setColor(GOLD);
      g.setFont(new Font("SansSerif", Font.BOLD, 34));
      this.drawCentered(g, "Star Catcher", 62);
      this.drawStar(g, WIDTH / 2 - 150, 50, 14, this.spin, GOLD, GOLD_EDGE);
      this.drawStar(g, WIDTH / 2 + 150, 50, 14, -this.spin, GOLD, GOLD_EDGE);
      g.setColor(TEXT);
      g.setFont(new Font("SansSerif", Font.PLAIN, 15));
      this.drawCentered(g, "Click the gold stars before the clock runs out. Stay off the black holes.", 96);
      g.setColor(DIM);
      g.setFont(new Font("SansSerif", Font.PLAIN, 13));
      this.drawCentered(g, this.records.totalStars() + " of " + Night.values().length * 3 + " stars    Arrows and Enter pick a night", 124);
      this.drawCentered(g, "Each night flies over one of the five animation modes (now: Mode " + (this.selected.mode() + 1) + ")", 440);
      for (Night night : Night.values()) {
         Rectangle card = this.nightCards[night.ordinal()];
         boolean open = this.records.isOpen(night);
         boolean chosen = night == this.selected;
         g.setColor(open ? new Color(24, 32, 84, 235) : new Color(18, 22, 50, 220));
         g.fillRoundRect(card.x, card.y, card.width, card.height, 16, 16);
         g.setStroke(new BasicStroke(chosen ? 3f : 1f));
         g.setColor(chosen ? GOLD : new Color(70, 80, 150));
         g.drawRoundRect(card.x, card.y, card.width, card.height, 16, 16);
         g.setStroke(new BasicStroke(1f));
         g.setFont(new Font("SansSerif", Font.BOLD, 16));
         g.setColor(open ? TEXT : DIM);
         g.drawString(night.number() + ". " + night.label(), card.x + 14, card.y + 26);
         g.setFont(new Font("SansSerif", Font.PLAIN, 12));
         g.setColor(DIM);
         if (open) {
            g.drawString("Catch " + night.goal() + (night.holes() > 0 ? ", " + night.holes() + (night.holes() == 1 ? " hole" : " holes") : ""), card.x + 14, card.y + 46);
            g.drawString(this.rule(night).replace("Click a star, or aim with the arrows and press Space", "No black holes"), card.x + 14, card.y + 63);
         } else {
            this.drawLock(g, card.x + card.width / 2, card.y + 56);
            g.drawString("Catch night " + night.ordinal() + " to open", card.x + 14, card.y + 104);
         }
         if (open) {
            int stars = this.records.stars(night);
            for (int i = 0; i < 3; i++) {
               boolean lit = i < stars;
               this.drawStar(g, card.x + 26 + i * 26, card.y + 92, 10, 0, lit ? GOLD : new Color(50, 58, 110), lit ? GOLD_EDGE : new Color(90, 100, 160));
            }
            int best = this.records.bestTenths(night);
            if (best >= 0) {
               g.setColor(DIM);
               g.drawString(String.format("%.1f s left", best / 10.0), card.x + 108, card.y + 97);
            }
         }
      }
   }

   private void drawLock(Graphics2D g, int x, int y) {
      g.setColor(DIM);
      g.setStroke(new BasicStroke(3f));
      g.drawOval(x - 8, y - 18, 16, 20);
      g.setStroke(new BasicStroke(1f));
      g.fillRoundRect(x - 12, y - 8, 24, 20, 5, 5);
   }
}
