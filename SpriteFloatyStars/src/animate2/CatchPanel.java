package animate2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
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
   private static final int NARROW_HUD = 62;
   /** The upright field is never shorter than this, so the menu's three rows of cards fit. */
   static final int NARROW_MIN_HEIGHT = 600;
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

   private enum Screen { MENU, INTRO, PLAYING, PAUSED, RESULT }

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

   /** Seconds a caught star takes to fly up into its place in the catch count, and its place then pops for. */
   static final double FLY = 0.45;
   static final double POP = 0.3;
   /** Seconds the lost part of the clock bar takes to drain away after a black hole. */
   static final double DRAIN = 0.7;

   /** A caught gold star flying from where it was caught to its place in the catch count. */
   private static final class Flyer {
      final double x;
      final double y;
      final int slot;
      double age;

      Flyer(double x, double y, int slot) {
         this.x = x;
         this.y = y;
         this.slot = slot;
      }
   }

   private final NightRecords records;
   final Random random = new Random(); // not private: CatchFeelCheck seeds it so its stars land the same every run
   private final List<Effect> effects = new ArrayList<>();
   private final List<Flyer> flyers = new ArrayList<>();
   // Not private: CatchFeelCheck checks the cards and buttons fit an upright phone.
   final Rectangle[] nightCards = new Rectangle[Night.values().length];
   final Rectangle againButton = new Rectangle();
   final Rectangle nextButton = new Rectangle();
   final Rectangle menuButton = new Rectangle();
   /** The pause button at the right of the bar, and the paused card's Resume. */
   final Rectangle pauseButton = new Rectangle();
   final Rectangle resumeButton = new Rectangle();
   private final boolean[] arrows = new boolean[4];
   private final Timer timer;
   private Screen screen = Screen.MENU;
   /** The screen a pause came from (the intro card or the night), so Resume goes back to it. */
   private Screen pausedFrom = Screen.PLAYING;
   private Night selected;
   CatchGame game;
   /** The clock just before the last black hole, and the seconds since it. */
   private double penaltyFrom;
   private double penaltyAge = Double.MAX_VALUE;
   private StarField sky;
   private double screenSeconds;
   private double spin;
   private double netX;
   private double netY;
   private boolean netShown;
   private boolean newBest;
   private long lastTick = System.nanoTime();
   /** The field's size: 640 x 480, or upright on a phone, where the HUD takes two rows and the cards two columns. */
   private final int w;
   private final int h;
   private final boolean narrow;
   private final int hud;
   private final int columns;

   public CatchPanel(NightRecords records) {
      this(records, new Dimension(WIDTH, HEIGHT));
   }

   public CatchPanel(NightRecords records, Dimension size) {
      this.records = records;
      this.selected = records.suggested();
      this.w = size.width;
      this.h = size.height;
      this.narrow = this.w < WIDTH;
      this.hud = this.narrow ? NARROW_HUD : HUD;
      this.columns = this.narrow ? 2 : 3;
      this.netX = this.w / 2.0;
      this.netY = this.hud + (this.h - this.hud) / 2.0;
      // A 44 px square on a phone, the size a thumb needs; 40 px in the 46 px bar on a wide screen.
      int pause = this.narrow ? 44 : 40;
      this.pauseButton.setBounds(this.w - pause - 8, (this.hud - pause) / 2, pause, pause);
      this.setPreferredSize(new Dimension(this.w, this.h));
      this.setFocusable(true);
      this.sky = new StarField(this.selected.mode(), new Dimension(this.w, this.h - this.hud), 90, 5, false);
      for (int i = 0; i < this.nightCards.length; i++) {
         if (this.narrow) {
            int cw = (this.w - 36) / 2;
            this.nightCards[i] = new Rectangle(12 + (i % 2) * (cw + 12), 150 + (i / 2) * 138, cw, 128);
         } else {
            this.nightCards[i] = new Rectangle(40 + (i % 3) * 190, 150 + (i / 3) * 130, 180, 118);
         }
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

   /**
    * The field for a screen, inside a window's borders: 640 x 480 as it always was, or on a screen narrower than the
    * settings window's phone width, the screen's width and as tall as it allows (600 to 760 px).
    */
   static Dimension sizeFor(Dimension screen, Insets insets) {
      if (screen.width >= SettingsFrame.NARROW) {
         return new Dimension(WIDTH, HEIGHT);
      }
      int width = Math.max(320, screen.width - insets.left - insets.right);
      int height = Math.max(NARROW_MIN_HEIGHT, Math.min(760, screen.height - insets.top - insets.bottom));
      return new Dimension(width, height);
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
            this.select(index + (code == KeyEvent.VK_UP ? -this.columns : this.columns));
         } else if (code >= KeyEvent.VK_1 && code < KeyEvent.VK_1 + Night.values().length) {
            this.select(code - KeyEvent.VK_1);
            this.start(this.selected);
         } else if (confirm || code == KeyEvent.VK_N) {
            this.start(this.selected);
         }
      } else if (this.screen == Screen.PAUSED) {
         if (confirm || code == KeyEvent.VK_P) {
            this.resume();
         } else if (code == KeyEvent.VK_R) {
            this.start(this.game.night());
         } else if (toMenu) {
            this.screen = Screen.MENU;
         }
      } else if (this.screen == Screen.INTRO || this.screen == Screen.PLAYING) {
         if (code == KeyEvent.VK_P) {
            this.pause();
         } else if (this.arrow(code, true)) {
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

   /** Stops the clock and shows the paused card, with Resume, Again and Nights. */
   void pause() {
      if (this.screen == Screen.INTRO || this.screen == Screen.PLAYING) {
         this.pausedFrom = this.screen;
         this.screen = Screen.PAUSED;
         java.util.Arrays.fill(this.arrows, false);
      }
   }

   private void resume() {
      if (this.screen == Screen.PAUSED) {
         this.screen = this.pausedFrom;
      }
   }

   /** Moves the menu's choice, skipping nights that are still locked. */
   private void select(int index) {
      if (index >= 0 && index < Night.values().length && this.records.isOpen(Night.values()[index])) {
         this.selected = Night.values()[index];
         // The menu's sky turns to the chosen night's animation mode.
         this.sky = new StarField(this.selected.mode(), new Dimension(this.w, this.h - this.hud), 90, 5, false);
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

   void start(Night night) {
      this.selected = night;
      this.game = new CatchGame(night, this.w, this.h - this.hud, this.random);
      this.sky = new StarField(night.mode(), new Dimension(this.w, this.h - this.hud), 90, 5, false);
      this.effects.clear();
      this.flyers.clear();
      this.penaltyAge = Double.MAX_VALUE;
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
      } else if ((this.screen == Screen.INTRO || this.screen == Screen.PLAYING) && this.pauseButton.contains(x, y)) {
         this.pause();
      } else if (this.screen == Screen.PLAYING) {
         this.catchAt(x, y);
      } else if (this.screen == Screen.PAUSED) {
         if (this.resumeButton.contains(x, y) || this.pauseButton.contains(x, y)) {
            this.resume();
         } else if (this.againButton.contains(x, y)) {
            this.start(this.game.night());
         } else if (this.menuButton.contains(x, y)) {
            this.screen = Screen.MENU;
         }
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

   void catchAt(double x, double y) {
      if (y < this.hud) {
         return;
      }
      int comboBefore = this.game.combo();
      double clockBefore = this.game.secondsLeft();
      CatchGame.Hit hit = this.game.click(x, y - this.hud);
      if (hit == CatchGame.Hit.GOLD) {
         int combo = this.game.combo();
         String text = combo >= 3 ? "Combo x" + combo + "  +" + CatchGame.COMBO_BONUS + " s" : "+1";
         this.effects.add(new Effect(x, y, text, GOLD));
         this.flyers.add(new Flyer(x, y, this.game.caught() - 1));
      } else if (hit == CatchGame.Hit.HOLE) {
         this.penaltyFrom = clockBefore;
         this.penaltyAge = 0;
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
      this.advance(seconds);
      this.repaint();
   }

   /** Moves everything on by a number of seconds. */
   void advance(double seconds) {
      if (this.screen == Screen.PAUSED) {
         // Everything holds still while paused: the clock, the stars and black holes, the effects and the sky.
         return;
      }
      this.spin += seconds;
      this.penaltyAge += seconds;
      this.screenSeconds += seconds;
      this.sky.advance(seconds * 0.5, this.w, this.h - this.hud);
      if (this.screen == Screen.INTRO && this.screenSeconds >= INTRO_SECONDS) {
         this.screen = Screen.PLAYING;
      }
      if (this.screen == Screen.INTRO || this.screen == Screen.PLAYING) {
         double dx = (this.arrows[3] ? 1 : 0) - (this.arrows[2] ? 1 : 0);
         double dy = (this.arrows[1] ? 1 : 0) - (this.arrows[0] ? 1 : 0);
         this.netX = Math.max(0, Math.min(this.w, this.netX + dx * NET_SPEED * seconds));
         this.netY = Math.max(this.hud, Math.min(this.h, this.netY + dy * NET_SPEED * seconds));
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
      for (Iterator<Flyer> it = this.flyers.iterator(); it.hasNext();) {
         Flyer flyer = it.next();
         flyer.age += seconds;
         if (flyer.age > FLY + POP) {
            it.remove();
         }
      }
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(SKY);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      g.translate(0, this.hud);
      this.sky.drawStars(g);
      g.setColor(SKY_SHADE);
      g.fillRect(0, 0, this.w, this.h - this.hud);
      if (this.screen != Screen.MENU) {
         this.drawBodies(g);
         if (this.penaltyAge < 0.3) {
            // A black hole's bite: the field blushes red for a moment.
            g.setColor(new Color(255, 40, 60, (int)(70 * (1.0 - this.penaltyAge / 0.3))));
            g.fillRect(0, 0, this.w, this.h - this.hud);
         }
      }
      g.translate(0, -this.hud);
      this.drawEffects(g);
      if (this.screen == Screen.MENU) {
         this.drawMenu(g);
         return;
      }
      if (this.netShown && this.screen != Screen.RESULT) {
         this.drawNet(g);
      }
      this.drawHud(g);
      this.drawFlyers(g);
      if (this.screen == Screen.INTRO) {
         this.drawIntro(g);
      } else if (this.screen == Screen.PAUSED) {
         this.drawPaused(g);
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
         int textX = Math.max(4, Math.min(this.w - width - 4, (int)effect.x - width / 2));
         g.drawString(effect.text, textX, (int)Math.max(this.hud + 16, effect.y - 26 - 24 * t));
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
      g.fillRect(0, 0, this.w, this.hud);
      g.setColor(new Color(60, 70, 130));
      g.drawLine(0, this.hud - 1, this.w, this.hud - 1);
      Night night = this.game.night();
      g.setFont(new Font("SansSerif", Font.BOLD, 14));
      g.setColor(TEXT);
      g.drawString("Night " + night.number() + ": " + night.label(), 12, this.narrow ? 22 : 20);
      if (!this.narrow) {
         // On a phone the HUD's second row is the catch count, and there are no keys to name.
         g.setFont(new Font("SansSerif", Font.PLAIN, 12));
         g.setColor(DIM);
         g.drawString("P: pause   Esc: nights   R: restart", 12, 37);
      }

      // Catch count: one small star per gold star to catch, lit once its caught star has flown in, popping as it lands.
      int goal = night.goal();
      int step = this.step(goal);
      int left = this.countLeft();
      int countY = this.countY();
      for (int i = 0; i < goal; i++) {
         Flyer flyer = this.flyerFor(i);
         boolean lit = i < this.game.caught() && (flyer == null || flyer.age >= FLY);
         int r = 7;
         if (lit && flyer != null) {
            double pop = 1.0 - (flyer.age - FLY) / POP;
            r += (int)Math.round(5 * pop);
            g.setColor(new Color(255, 240, 170, (int)(200 * pop)));
            int ring = 9 + (int)(10 * (1.0 - pop));
            g.drawOval(this.slotX(i) - ring, countY - ring, 2 * ring, 2 * ring);
         }
         this.drawStar(g, this.slotX(i), countY, r, 0, lit ? GOLD : new Color(50, 58, 110), lit ? GOLD_EDGE : new Color(90, 100, 160));
      }
      g.setFont(new Font("SansSerif", Font.BOLD, 13));
      g.setColor(TEXT);
      g.drawString(this.game.caught() + " / " + goal, left + goal * step + 8, countY + 5);

      // The clock: a bar that empties, red in the last five seconds, with the stars thresholds marked.
      // The bar ends left of the pause button.
      int barW = this.narrow ? Math.min(140, this.w - 220) : 150;
      int barX = this.pauseButton.x - 8 - barW;
      int barY = this.narrow ? 8 : 14;
      double left10 = Math.min(Night.SECONDS, this.game.secondsLeft());
      g.setColor(new Color(40, 46, 96));
      g.fillRoundRect(barX, barY, barW, 18, 9, 9);
      g.setColor(left10 <= Night.TWO_STARS ? BAD : left10 <= Night.THREE_STARS ? new Color(255, 170, 60) : new Color(90, 200, 255));
      int fill = (int)(barW * left10 / Night.SECONDS);
      if (this.penaltyAge < DRAIN) {
         // The seconds a black hole took stay on the bar in red, then drain down to the clock.
         double t = this.penaltyAge / DRAIN;
         int lost = (int)(barW * Math.min(Night.SECONDS, this.penaltyFrom) / Night.SECONDS) - fill;
         Color barColor = g.getColor();
         g.setColor(new Color(255, 70, 80, (int)(230 * (1.0 - t * 0.5))));
         g.fillRoundRect(barX, barY, fill + (int)(lost * (1.0 - t * t)), 18, 9, 9);
         g.setColor(barColor);
      }
      g.fillRoundRect(barX, barY, fill, 18, 9, 9);
      g.setColor(new Color(255, 255, 255, 120));
      for (double mark : new double[]{Night.THREE_STARS, Night.TWO_STARS}) {
         int mx = barX + (int)(barW * mark / Night.SECONDS);
         g.drawLine(mx, barY - 2, mx, barY + 20);
      }
      String clock = String.format("%.1f s", this.game.secondsLeft());
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      g.setColor(Color.white);
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(clock, barX + barW - metrics.stringWidth(clock) - 8, barY + 14);
      if (this.game.combo() >= 3 && this.screen == Screen.PLAYING) {
         g.setColor(GOLD);
         String combo = "Combo x" + this.game.combo();
         // On a phone right of the count, left of the pause button; on a wide screen under the count, clear of the bar.
         if (this.narrow) {
            g.drawString(combo, this.pauseButton.x - 8 - metrics.stringWidth(combo), countY + 5);
         } else {
            g.drawString(combo, left, 43);
         }
      }
      this.drawPauseButton(g);
   }

   /** Two bars while the night runs, a gold play triangle while paused; none on the result card. */
   private void drawPauseButton(Graphics2D g) {
      if (this.screen == Screen.RESULT) {
         return;
      }
      Rectangle b = this.pauseButton;
      boolean paused = this.screen == Screen.PAUSED;
      g.setColor(paused ? GOLD : new Color(44, 52, 110));
      g.fillRoundRect(b.x, b.y, b.width, b.height, 12, 12);
      g.setColor(paused ? GOLD_EDGE : new Color(90, 100, 180));
      g.drawRoundRect(b.x, b.y, b.width, b.height, 12, 12);
      int cx = b.x + b.width / 2;
      int cy = b.y + b.height / 2;
      if (paused) {
         g.setColor(new Color(40, 24, 0));
         g.fillPolygon(new int[]{cx - 6, cx - 6, cx + 9}, new int[]{cy - 9, cy + 9, cy}, 3);
      } else {
         g.setColor(TEXT);
         g.fillRoundRect(cx - 8, cy - 9, 6, 18, 2, 2);
         g.fillRoundRect(cx + 2, cy - 9, 6, 18, 2, 2);
      }
   }

   /** The catch count: where it starts, the height of its middle, and the room each place takes. */
   private int countLeft() {
      return this.narrow ? 12 : 200;
   }

   int countY() {
      return this.narrow ? 44 : 23;
   }

   private int step(int goal) {
      // The count leaves room on its right for "15 / 15" (and on a phone the combo) before the clock bar.
      return Math.min(16, (this.narrow ? this.w - 190 : 190) / goal);
   }

   /** The middle of a place in the catch count. */
   int slotX(int slot) {
      return this.countLeft() + slot * this.step(this.game.night().goal()) + 7;
   }

   private Flyer flyerFor(int slot) {
      for (Flyer flyer : this.flyers) {
         if (flyer.slot == slot) {
            return flyer;
         }
      }
      return null;
   }

   /** Where a caught star is a fraction t of the way along its flight: up from where it was caught, then across into its place. */
   double[] flightPoint(double x, double y, int slot, double t) {
      double e = t * t * (3 - 2 * t);
      double tx = this.slotX(slot);
      double u = 1 - e;
      double ty = this.countY();
      return new double[]{u * u * x + 2 * u * e * x + e * e * tx, u * u * y + 2 * u * e * ty + e * e * ty};
   }

   private void drawFlyers(Graphics2D g) {
      for (Flyer flyer : this.flyers) {
         if (flyer.age >= FLY) {
            continue;
         }
         // A short tail of fainter stars behind the one in flight.
         for (int k = 3; k >= 0; k--) {
            double t = Math.max(0, flyer.age / FLY - k * 0.06);
            double[] p = this.flightPoint(flyer.x, flyer.y, flyer.slot, t);
            int alpha = 255 - k * 60;
            int r = (int)Math.round(CatchGame.GOLD_RADIUS + 2 - (CatchGame.GOLD_RADIUS - 5) * t) - k * 2;
            this.drawStar(g, (int)p[0], (int)p[1], Math.max(3, r), this.spin * 6.0, new Color(255, 205, 40, alpha), new Color(170, 110, 0, alpha));
         }
      }
   }

   private void drawCentered(Graphics2D g, String text, int y) {
      g.drawString(text, (this.w - g.getFontMetrics().stringWidth(text)) / 2, y);
   }

   /** Draws text centered (x below 0) or from x, in lines no wider than {@code width}, the first at {@code y}. */
   private void drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight) {
      FontMetrics metrics = g.getFontMetrics();
      String line = "";
      for (String word : text.split(" ")) {
         String longer = line.isEmpty() ? word : line + " " + word;
         if (!line.isEmpty() && metrics.stringWidth(longer) > width) {
            this.drawLine(g, line, x, y);
            y += lineHeight;
            line = word;
         } else {
            line = longer;
         }
      }
      this.drawLine(g, line, x, y);
   }

   private void drawLine(Graphics2D g, String text, int x, int y) {
      if (x < 0) {
         this.drawCentered(g, text, y);
      } else {
         g.drawString(text, x, y);
      }
   }

   private void drawIntro(Graphics2D g) {
      Night night = this.game.night();
      int top = this.h / 2 - 70;
      int width = Math.min(380, this.w - 24);
      g.setColor(CARD);
      g.fillRoundRect((this.w - width) / 2, top, width, 120, 18, 18);
      g.setColor(GOLD);
      g.setFont(new Font("SansSerif", Font.BOLD, 26));
      this.drawCentered(g, "Night " + night.number() + ": " + night.label(), top + 42);
      g.setColor(TEXT);
      g.setFont(new Font("SansSerif", Font.PLAIN, 15));
      this.drawCentered(g, "Catch " + night.goal() + " gold stars in " + (int)Night.SECONDS + " seconds", top + 72);
      g.setColor(DIM);
      g.setFont(new Font("SansSerif", Font.PLAIN, 13));
      this.drawCentered(g, this.rule(night), top + 98);
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
      return this.narrow ? "Tap a gold star to catch it" : "Click a star, or aim with the arrows and press Space";
   }

   private void drawResult(Graphics2D g) {
      Night night = this.game.night();
      boolean caught = this.game.state() == CatchGame.State.CAUGHT;
      int cardW = Math.min(400, this.w - 24);
      int cardX = (this.w - cardW) / 2;
      int cardY = this.h / 2 - 120;
      g.setColor(CARD);
      g.fillRoundRect(cardX, cardY, cardW, 250, 20, 20);
      g.setColor(new Color(90, 100, 180));
      g.drawRoundRect(cardX, cardY, cardW, 250, 20, 20);
      g.setFont(new Font("SansSerif", Font.BOLD, 26));
      g.setColor(caught ? GOLD : BAD);
      this.drawCentered(g, caught ? "Night caught!" : "The clock ran out", cardY + 44);
      int stars = this.game.stars();
      for (int i = 0; i < 3; i++) {
         // The stars pop in one after another.
         boolean lit = i < stars && this.screenSeconds > 0.25 + i * 0.25;
         this.drawStar(g, this.w / 2 - 60 + i * 60, cardY + 95, 24, 0, lit ? GOLD : new Color(50, 58, 110), lit ? GOLD_EDGE : new Color(90, 100, 160));
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
      if (this.narrow) {
         // A phone has no R, N or Esc to name, and the three buttons share the narrower card.
         boolean next = this.nextNight() != night;
         int count = next ? 3 : 2;
         int bw = next ? (cardW - 40 - 20) / 3 : 120;
         int x = cardX + (cardW - count * bw - (count - 1) * 10) / 2;
         this.button(g, this.againButton, x, buttonY, bw, "Again", !next);
         if (next) {
            this.button(g, this.nextButton, x += bw + 10, buttonY, bw, "Next night", true);
         } else {
            this.nextButton.setBounds(0, 0, 0, 0);
         }
         this.button(g, this.menuButton, x + bw + 10, buttonY, bw, "Nights", false);
      } else if (this.nextNight() != night) {
         this.button(g, this.againButton, cardX + 20, buttonY, 110, "Again (R)", false);
         this.button(g, this.nextButton, cardX + 145, buttonY, 110, "Next night (N)", true);
         this.button(g, this.menuButton, cardX + 270, buttonY, 110, "Nights (Esc)", false);
      } else {
         this.nextButton.setBounds(0, 0, 0, 0);
         this.button(g, this.againButton, cardX + 70, buttonY, 120, "Again (R)", true);
         this.button(g, this.menuButton, cardX + 210, buttonY, 120, "Nights (Esc)", false);
      }
   }

   /** The paused card: what is left of the night, then Resume, Again and Nights. */
   private void drawPaused(Graphics2D g) {
      Night night = this.game.night();
      // The field goes dark, so a pause is no time to study where the stars are.
      g.setColor(new Color(8, 12, 40, 215));
      g.fillRect(0, this.hud, this.w, this.h - this.hud);
      int cardW = Math.min(400, this.w - 24);
      int cardX = (this.w - cardW) / 2;
      int cardY = this.h / 2 - 100;
      g.setColor(CARD);
      g.fillRoundRect(cardX, cardY, cardW, 190, 20, 20);
      g.setColor(new Color(90, 100, 180));
      g.drawRoundRect(cardX, cardY, cardW, 190, 20, 20);
      g.setFont(new Font("SansSerif", Font.BOLD, 26));
      g.setColor(GOLD);
      this.drawCentered(g, "Paused", cardY + 44);
      g.setFont(new Font("SansSerif", Font.PLAIN, 14));
      g.setColor(TEXT);
      int more = night.goal() - this.game.caught();
      this.drawCentered(g, more + (more == 1 ? " gold star" : " gold stars") + " to go, " + String.format("%.1f", this.game.secondsLeft()) + " s left", cardY + 80);
      g.setColor(DIM);
      g.setFont(new Font("SansSerif", Font.PLAIN, 13));
      this.drawCentered(g, "The clock waits until you resume", cardY + 103);
      int bw = this.narrow ? (cardW - 40 - 20) / 3 : 110;
      int x = cardX + (cardW - 3 * bw - 20) / 2;
      int y = cardY + 130;
      this.button(g, this.resumeButton, x, y, bw, this.narrow ? "Resume" : "Resume (P)", true);
      this.button(g, this.againButton, x + bw + 10, y, bw, this.narrow ? "Again" : "Again (R)", false);
      this.button(g, this.menuButton, x + 2 * (bw + 10), y, bw, this.narrow ? "Nights" : "Nights (Esc)", false);
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
      int beside = Math.min(150, this.w / 2 - 20);
      this.drawStar(g, this.w / 2 - beside, 50, 14, this.spin, GOLD, GOLD_EDGE);
      this.drawStar(g, this.w / 2 + beside, 50, 14, -this.spin, GOLD, GOLD_EDGE);
      g.setColor(TEXT);
      g.setFont(new Font("SansSerif", Font.PLAIN, 15));
      String total = this.records.totalStars() + " of " + Night.values().length * 3 + " stars";
      String sky = "Each night flies over one of the five animation modes (now: Mode " + (this.selected.mode() + 1) + ")";
      if (this.narrow) {
         this.drawWrapped(g, "Tap the gold stars before the clock runs out. Stay off the black holes.", -1, 92, this.w - 32, 19);
         g.setColor(DIM);
         g.setFont(new Font("SansSerif", Font.PLAIN, 13));
         this.drawCentered(g, total, 136);
         this.drawWrapped(g, sky, -1, this.nightCards[5].y + this.nightCards[5].height + 24, this.w - 32, 16);
      } else {
         this.drawCentered(g, "Click the gold stars before the clock runs out. Stay off the black holes.", 96);
         g.setColor(DIM);
         g.setFont(new Font("SansSerif", Font.PLAIN, 13));
         this.drawCentered(g, total + "    Arrows and Enter pick a night", 124);
         this.drawCentered(g, sky, 440);
      }
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
            String rule = night.holes() == 0 ? "No black holes" : this.rule(night);
            if (this.narrow) {
               this.drawWrapped(g, rule, card.x + 14, card.y + 63, card.width - 24, 15);
            } else {
               g.drawString(rule, card.x + 14, card.y + 63);
            }
         } else {
            this.drawLock(g, card.x + card.width / 2, card.y + 56);
            g.drawString("Catch night " + night.ordinal() + " to open", card.x + 14, card.y + card.height - 14);
         }
         if (open) {
            int stars = this.records.stars(night);
            for (int i = 0; i < 3; i++) {
               boolean lit = i < stars;
               this.drawStar(g, card.x + 26 + i * (this.narrow ? 22 : 26), card.y + card.height - 26, this.narrow ? 9 : 10, 0, lit ? GOLD : new Color(50, 58, 110), lit ? GOLD_EDGE : new Color(90, 100, 160));
            }
            int best = this.records.bestTenths(night);
            if (best >= 0) {
               g.setColor(DIM);
               String time = String.format("%.1f s left", best / 10.0);
               if (this.narrow) {
                  // Right-aligned in the narrower card, so it clears the stars down to a 320 px screen.
                  g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                  g.drawString(time, card.x + card.width - 10 - g.getFontMetrics().stringWidth(time), card.y + card.height - 21);
               } else {
                  g.drawString(time, card.x + 108, card.y + card.height - 21);
               }
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
