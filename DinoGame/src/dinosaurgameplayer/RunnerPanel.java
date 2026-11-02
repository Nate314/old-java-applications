package dinosaurgameplayer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Draws the game and takes the keys. With the bot on, every step is also drawn into an image, and the bot reads
 * its one pixel from that image: it sees exactly what you see, red marker and all. E switches to evolving bots
 * (Evolution, drawn by EvolveView) and back.
 */
final class RunnerPanel extends JPanel {
   private static final long serialVersionUID = 1L;
   static final int HEIGHT = 220;
   static final int STRIP = 196;
   /** Steps after a crash before Space starts a new run, so a held key does not restart at once. */
   private static final int RESTART_LOCK = 24;

   static final Color INK = new Color(0x535353);
   static final Color PAPER = new Color(0xF7F7F7);
   private static final Color CLOUD = new Color(0xD6D6D6);
   static final Color MUTED = new Color(0x6E6E6E);
   private static final Color STRIP_BG = new Color(0xEBEBEB);
   static final Color MARKER = new Color(255, 0, 0);
   private static final Color BOT_TEXT = new Color(0xC62828);
   static final Font SCORE = new Font(Font.MONOSPACED, Font.BOLD, 15);
   static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 16);
   static final Font TEXT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
   private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
   static final Font TINY = new Font(Font.SANS_SERIF, Font.BOLD, 10);
   /** Evolution speeds, in steps per game step. */
   private static final int[] SPEEDS = { 1, 4, 16 };
   /** How long after a first N a second one starts the evolution over. */
   private static final long CONFIRM_NANOS = 3_000_000_000L;

   private final Runner runner;
   private final PixelBot bot = new PixelBot();
   private final Records records;
   private BufferedImage frame;
   private long lastNanos = System.nanoTime();
   private double pending;
   private Runner.State seen = Runner.State.READY;

   // This run: whether the bot played all of it, whether it ever played, whether you helped it.
   private boolean botWholeRun;
   private boolean botEver;
   private boolean humanHelped;
   private String[] crashLines = new String[0];
   private boolean dragging;
   /** Non-null while bots are evolving instead of the game being played. */
   private Evolution evolution;
   private int speed = 1;
   private long confirmUntil;
   /** The strip's buttons as last drawn: x, width and key code each. */
   private final List<int[]> chips = new ArrayList<>();

   RunnerPanel(Records records, long seed) {
      this.runner = new Runner(seed);
      this.records = records;
      this.bot.moveTo(records.markerX, records.markerY);
      this.setPreferredSize(new Dimension(Runner.WIDTH, HEIGHT));
      this.setFocusable(true);
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (RunnerPanel.this.evolution != null || k == KeyEvent.VK_E) {
               RunnerPanel.this.evolveKey(k);
            } else if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
               RunnerPanel.this.jumpKey(true);
            } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
               RunnerPanel.this.duckKey(true);
            } else if (k == KeyEvent.VK_B) {
               RunnerPanel.this.toggleBot();
            } else if (k == KeyEvent.VK_X) {
               RunnerPanel.this.resetMarker();
            }
         }

         @Override
         public void keyReleased(KeyEvent e) {
            int k = e.getKeyCode();
            if (RunnerPanel.this.evolution != null) {
               return;
            }
            if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
               RunnerPanel.this.jumpKey(false);
            } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
               RunnerPanel.this.duckKey(false);
            }
         }
      });
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            RunnerPanel.this.requestFocusInWindow();
            if (e.getY() >= STRIP) {
               RunnerPanel.this.clickStrip(e.getX());
            } else if (RunnerPanel.this.evolution != null) {
               return;
            } else if (RunnerPanel.this.bot.isOn() && RunnerPanel.this.bot.markerContains(e.getX(), e.getY())) {
               RunnerPanel.this.dragging = true;
            } else {
               RunnerPanel.this.jumpKey(true);   // a click or a tap jumps, for touch screens
            }
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            if (RunnerPanel.this.dragging) {
               RunnerPanel.this.dragging = false;
               RunnerPanel.this.saveMarker();
            } else if (RunnerPanel.this.evolution == null) {
               RunnerPanel.this.jumpKey(false);
            }
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            if (RunnerPanel.this.dragging) {
               RunnerPanel.this.bot.moveTo(e.getX() - PixelBot.MARKER / 2, e.getY() - PixelBot.MARKER / 2);
               RunnerPanel.this.repaint();
            }
         }

         @Override
         public void mouseMoved(MouseEvent e) {
            boolean over = RunnerPanel.this.evolution == null && RunnerPanel.this.bot.isOn() && RunnerPanel.this.bot.markerContains(e.getX(), e.getY());
            RunnerPanel.this.setCursor(Cursor.getPredefinedCursor(over ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
   }

   /** Starts the clock: about 60 steps a second, however often the timer fires. */
   void start() {
      this.lastNanos = System.nanoTime();
      new Timer(15, e -> this.tick()).start();
   }

   // ---- input ----

   private void jumpKey(boolean down) {
      if (!down) {
         this.runner.release();
         return;
      }
      if (this.runner.state() == Runner.State.CRASHED) {
         if (this.runner.idleSteps() >= RESTART_LOCK) {
            this.runner.restart();
         }
         return;
      }
      if (this.runner.state() == Runner.State.RUNNING && this.bot.isOn()) {
         this.humanHelped = true;
      }
      this.runner.press();
   }

   private void duckKey(boolean down) {
      if (down && this.runner.state() == Runner.State.RUNNING && this.bot.isOn()) {
         this.humanHelped = true;
      }
      this.runner.duck(down);
   }

   private void toggleBot() {
      this.bot.setOn(!this.bot.isOn(), this.runner);
      if (this.bot.isOn()) {
         this.botEver = true;
      } else {
         this.botWholeRun = false;
      }
      this.repaint();
   }

   /** E, and while evolving F (speed) and N (start over, pressed twice). */
   private void evolveKey(int k) {
      if (k == KeyEvent.VK_E) {
         this.setEvolving(this.evolution == null);
      } else if (k == KeyEvent.VK_F) {
         this.speed = (this.speed + 1) % SPEEDS.length;
      } else if (k == KeyEvent.VK_N) {
         if (System.nanoTime() < this.confirmUntil) {
            this.confirmUntil = 0;
            this.records.generations = 0;
            this.records.evolvedBest = 0;
            this.records.champion = null;
            this.records.chart.clear();
            this.records.save();
            this.evolution = this.newEvolution();
         } else {
            this.confirmUntil = System.nanoTime() + CONFIRM_NANOS;
         }
      }
      this.repaint();
   }

   /** Into the evolution (carrying on from the saved champion) or back to the game's ready screen. */
   void setEvolving(boolean on) {
      // A run under way when the mode changes is dropped, and counts for nobody.
      this.runner.duck(false);
      this.runner.ready();
      this.seen = Runner.State.READY;
      this.crashLines = new String[0];
      this.dragging = false;
      this.confirmUntil = 0;
      this.evolution = on ? this.newEvolution() : null;
      this.setCursor(Cursor.getDefaultCursor());
   }

   private Evolution newEvolution() {
      return new Evolution(System.nanoTime(), this.records.champion, this.records.generations, this.records.evolvedBest,
         this.records.chart);
   }

   private void saveEvolution() {
      this.records.generations = this.evolution.generation();
      this.records.evolvedBest = this.evolution.championScore();
      this.records.champion = this.evolution.champion();
      this.records.chart.clear();
      this.records.chart.addAll(this.evolution.history());
      this.records.save();
   }

   /** A click on the strip presses the key of the button under it. */
   private void clickStrip(int x) {
      for (int[] c : this.chips) {
         if (x >= c[0] && x < c[0] + c[1]) {
            this.evolveKey(c[2]);
            return;
         }
      }
   }

   private void resetMarker() {
      this.bot.moveTo(PixelBot.DEFAULT_X, PixelBot.DEFAULT_Y);
      this.saveMarker();
      this.repaint();
   }

   private void saveMarker() {
      this.records.markerX = this.bot.x();
      this.records.markerY = this.bot.y();
      this.records.save();
   }

   // ---- time ----

   private void tick() {
      long now = System.nanoTime();
      this.pending = Math.min(this.pending + (now - this.lastNanos) / 1e6, Runner.STEP_MS * 5);
      this.lastNanos = now;
      while (this.pending >= Runner.STEP_MS) {
         this.pending -= Runner.STEP_MS;
         this.step();
      }
      this.repaint();
   }

   /** One game step: the bot looks at the frame as drawn, then the game moves on. */
   void step() {
      if (this.evolution != null) {
         if (this.evolution.result() != null) {
            this.evolution.step();   // the pause between generations runs at normal speed
            return;
         }
         for (int i = 0; i < SPEEDS[this.speed]; i++) {
            if (this.evolution.step()) {
               this.saveEvolution();
               break;
            }
         }
         return;
      }
      if (this.bot.isOn()) {
         if (this.frame == null) {
            this.frame = new BufferedImage(Runner.WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
         }
         Graphics2D g = this.frame.createGraphics();
         this.paintScene(g);
         g.dispose();
         this.bot.step(this.runner, (x, y) -> this.frame.getRGB(x, y) & 0xFFFFFF);
      }
      if (this.seen != Runner.State.RUNNING && this.runner.state() == Runner.State.RUNNING) {
         this.botWholeRun = this.bot.isOn();
         this.botEver = this.bot.isOn();
         this.humanHelped = false;
         this.crashLines = new String[0];
         this.bot.newRun();
      }
      boolean wasRunning = this.runner.state() == Runner.State.RUNNING;
      this.runner.step();
      if (wasRunning && this.runner.state() == Runner.State.CRASHED) {
         this.crashed();
      }
      this.seen = this.runner.state();
   }

   private void crashed() {
      int score = this.runner.score();
      if (this.botWholeRun && !this.humanHelped) {
         boolean best = score > this.records.botBest;
         if (best) {
            this.records.botBest = score;
            this.records.save();
         }
         this.crashLines = new String[] { "The bot scored " + score + (best ? ", its new best." : "."),
            PixelBot.whyItCrashed(this.runner) };
      } else if (this.botEver) {
         this.crashLines = new String[] { "You and the bot scored " + score + " together.",
            "Team runs do not count. Space starts a run of your own." };
      } else {
         boolean best = score > this.records.best;
         if (best) {
            this.records.best = score;
            this.records.save();
         }
         String bot = this.records.botBest == 0 ? "Press B to see how far the original bot gets."
            : score > this.records.botBest ? "You beat the bot's best of " + this.records.botBest + "."
            : "The bot's best is " + this.records.botBest + ". Beat it?";
         this.crashLines = new String[] { (best && score > 0 ? "New best: " : "You scored ") + score + ".", bot };
      }
   }

   // ---- drawing ----

   @Override
   protected void paintComponent(Graphics g) {
      this.paintScene((Graphics2D) g);
   }

   private void paintScene(Graphics2D g) {
      if (this.evolution != null) {
         EvolveView.paint(g, this.evolution, SPEEDS[this.speed]);
      } else {
         paintWorld(g, this.runner);
         paintDino(g, this.runner, INK);
         this.paintScore(g);
         if (this.bot.isOn()) {
            this.paintBot(g);
         }
         this.paintMessage(g);
      }
      this.paintStrip(g);
   }

   /** The strip along the bottom: what the keys do, and buttons a click or a tap presses. */
   private void paintStrip(Graphics2D g) {
      g.setColor(STRIP_BG);
      g.fillRect(0, STRIP, Runner.WIDTH, HEIGHT - STRIP);
      g.setFont(SMALL);
      FontMetrics fm = g.getFontMetrics();
      String help;
      String[] labels;
      int[] keys;
      if (this.evolution != null) {
         help = "The furthest five of each 20 go on and breed the next.";
         labels = new String[] { "Speed " + SPEEDS[this.speed] + "x (F)",
            System.nanoTime() < this.confirmUntil ? "Sure? N again" : "Start over (N)", "Back to the game (E)" };
         keys = new int[] { KeyEvent.VK_F, KeyEvent.VK_N, KeyEvent.VK_E };
      } else {
         help = this.bot.isOn()
            ? "B: bot off   Drag its red eye to change what it watches   X: put it back"
            : "Space or ↑: jump (hold for higher)   ↓: duck   B: the original bot plays";
         labels = new String[] { "Evolve a bot (E)" };
         keys = new int[] { KeyEvent.VK_E };
      }
      this.chips.clear();
      int x = Runner.WIDTH - 6;
      for (int i = labels.length - 1; i >= 0; i--) {
         int w = fm.stringWidth(labels[i]) + 16;
         x -= w;
         this.chips.add(0, new int[] { x, w, keys[i] });
         x -= 6;
      }
      for (int i = 0; i < labels.length; i++) {
         int[] c = this.chips.get(i);
         boolean main = keys[i] == KeyEvent.VK_E;
         g.setColor(main ? INK : PAPER);
         g.fillRoundRect(c[0], STRIP + 3, c[1], HEIGHT - STRIP - 6, 8, 8);
         g.setColor(main ? PAPER : MUTED);
         g.drawString(labels[i], c[0] + 8, STRIP + 16);
      }
      g.setColor(MUTED);
      if (fm.stringWidth(help) < x - 12) {
         g.drawString(help, 12, STRIP + 16);
      }
   }

   /** The background, the ground and the obstacles of runner's course. */
   static void paintWorld(Graphics2D g, Runner runner) {
      g.setColor(PAPER);
      g.fillRect(0, 0, Runner.WIDTH, HEIGHT);
      g.setColor(CLOUD);
      double[] clouds = runner.clouds();
      for (int i = 0; i < clouds.length; i++) {
         Sprites.draw(g, Sprites.CLOUD, (int) clouds[i], 46 + i % 2 * 24, 2);
      }

      // The ground: a line, with pebbles below it that scroll with the run.
      g.setColor(INK);
      g.fillRect(0, Runner.GROUND, Runner.WIDTH, 1);
      double d = runner.distance();
      long first = (long) Math.floor(d / 23);
      for (long n = first; n * 23 - d < Runner.WIDTH; n++) {
         long h = (n * 2654435761L) >>> 7;
         if (h % 3 != 0) {
            int x = (int) Math.round(n * 23 - d);
            g.fillRect(x, Runner.GROUND + 3 + (int) (h % 7), 2 + (int) (h % 4), 1);
         }
      }

      for (Runner.Obstacle o : runner.obstacles()) {
         int x = (int) Math.round(o.x);
         if (o.kind == Runner.Kind.BIRD) {
            Sprites.draw(g, runner.birdWings(), x, o.top(), 2);
         } else {
            int unit = o.w / o.size;
            for (int j = 0; j < o.size; j++) {
               Sprites.cactus(g, x + j * unit, o.top(), o.kind == Runner.Kind.LARGE_CACTUS);
            }
         }
      }
   }

   /** runner's dinosaur, in color. */
   static void paintDino(Graphics2D g, Runner runner, Color color) {
      int top = runner.dinoTop();
      g.setColor(color);
      if (runner.isDucking()) {
         Sprites.draw(g, runner.steps() / 6 % 2 == 0 ? Sprites.DUCK_A : Sprites.DUCK_B, Runner.DINO_X, top, 2);
         return;
      }
      boolean running = runner.state() == Runner.State.RUNNING && !runner.isJumping();
      String[] legs = !running ? Sprites.LEGS_STAND : runner.steps() / 6 % 2 == 0 ? Sprites.LEGS_A : Sprites.LEGS_B;
      Sprites.draw(g, Sprites.DINO, Runner.DINO_X, top, 2);
      Sprites.draw(g, legs, Runner.DINO_X, top + Sprites.DINO.length * 2, 2);
      if (runner.state() == Runner.State.CRASHED) {
         // A wide-open eye.
         g.setColor(PAPER);
         g.fillRect(Runner.DINO_X + 23, top + 1, 6, 6);
         g.setColor(color);
         g.fillRect(Runner.DINO_X + 25, top + 3, 2, 2);
      }
   }

   /** Top right, as in Chrome's game: the bot's best in red, your best, then this run. */
   private void paintScore(Graphics2D g) {
      g.setFont(SCORE);
      FontMetrics fm = g.getFontMetrics();
      String now = pad(this.runner.score());
      String mine = (this.records.best > 0 ? "HI " + pad(this.records.best) + "  " : "") + now;
      int x = Runner.WIDTH - 14 - fm.stringWidth(mine);
      g.setColor(INK);
      g.drawString(mine, x, 24);
      if (this.records.botBest > 0 || this.bot.isOn()) {
         String bot = "BOT " + pad(this.records.botBest) + "  ";
         g.setColor(BOT_TEXT);
         g.drawString(bot, x - fm.stringWidth(bot), 24);
      }
   }

   private void paintBot(Graphics2D g) {
      int x = this.bot.x();
      int y = this.bot.y();
      g.setColor(MARKER);
      g.fillRect(x, y, PixelBot.MARKER, PixelBot.MARKER);
      g.setFont(TINY);
      g.drawString("bot's eye", x - 14, y - 5);
      // The badge, top left.
      g.fillRoundRect(12, 10, 150, 22, 10, 10);
      g.setColor(Color.white);
      g.drawString("BOT PLAYING  " + this.bot.jumps() + (this.bot.jumps() == 1 ? " jump" : " jumps"), 20, 25);
   }

   private void paintMessage(Graphics2D g) {
      Runner.State state = this.runner.state();
      if (state == Runner.State.RUNNING) {
         return;
      }
      String title = state == Runner.State.READY ? "Space to run" : "G A M E   O V E R";
      String[] lines = state == Runner.State.READY
         ? new String[] { "Jump the cacti, duck the birds.", "Or press B and the original bot plays, watching one pixel." }
         : this.crashLines;
      g.setColor(INK);
      g.setFont(TITLE);
      centered(g, title, 82);
      g.setFont(TEXT);
      g.setColor(MUTED);
      for (int i = 0; i < lines.length; i++) {
         centered(g, lines[i], 104 + i * 18);
      }
   }

   static void centered(Graphics2D g, String s, int y) {
      g.drawString(s, (Runner.WIDTH - g.getFontMetrics().stringWidth(s)) / 2, y);
   }

   static String pad(int n) {
      String s = "0000" + Math.min(n, 99999);
      return s.substring(s.length() - 5);
   }

   // ---- for the self-check ----

   Runner runner() {
      return this.runner;
   }

   PixelBot bot() {
      return this.bot;
   }

   void setBot(boolean on) {
      if (on != this.bot.isOn()) {
         this.toggleBot();
      }
   }

   String[] crashLines() {
      return this.crashLines;
   }

   Evolution evolution() {
      return this.evolution;
   }
}
