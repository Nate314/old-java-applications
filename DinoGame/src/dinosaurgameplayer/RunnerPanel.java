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
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Draws the game and takes the keys. With the bot on, every step is also drawn into an image, and the bot reads
 * its one pixel from that image: it sees exactly what you see, red marker and all.
 */
final class RunnerPanel extends JPanel {
   private static final long serialVersionUID = 1L;
   static final int HEIGHT = 220;
   private static final int STRIP = 196;
   /** Steps after a crash before Space starts a new run, so a held key does not restart at once. */
   private static final int RESTART_LOCK = 24;

   static final Color INK = new Color(0x535353);
   static final Color PAPER = new Color(0xF7F7F7);
   private static final Color CLOUD = new Color(0xD6D6D6);
   private static final Color MUTED = new Color(0x6E6E6E);
   private static final Color STRIP_BG = new Color(0xEBEBEB);
   static final Color MARKER = new Color(255, 0, 0);
   private static final Color BOT_TEXT = new Color(0xC62828);
   private static final Font SCORE = new Font(Font.MONOSPACED, Font.BOLD, 15);
   private static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 16);
   private static final Font TEXT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
   private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
   private static final Font TINY = new Font(Font.SANS_SERIF, Font.BOLD, 10);

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
            if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
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
            if (RunnerPanel.this.bot.isOn() && RunnerPanel.this.bot.markerContains(e.getX(), e.getY())) {
               RunnerPanel.this.dragging = true;
            } else if (e.getY() < STRIP) {
               RunnerPanel.this.jumpKey(true);   // a click or a tap jumps, for touch screens
            }
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            if (RunnerPanel.this.dragging) {
               RunnerPanel.this.dragging = false;
               RunnerPanel.this.saveMarker();
            } else {
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
            boolean over = RunnerPanel.this.bot.isOn() && RunnerPanel.this.bot.markerContains(e.getX(), e.getY());
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
      g.setColor(PAPER);
      g.fillRect(0, 0, Runner.WIDTH, HEIGHT);
      g.setColor(CLOUD);
      double[] clouds = this.runner.clouds();
      for (int i = 0; i < clouds.length; i++) {
         Sprites.draw(g, Sprites.CLOUD, (int) clouds[i], 46 + i % 2 * 24, 2);
      }

      // The ground: a line, with pebbles below it that scroll with the run.
      g.setColor(INK);
      g.fillRect(0, Runner.GROUND, Runner.WIDTH, 1);
      double d = this.runner.distance();
      long first = (long) Math.floor(d / 23);
      for (long n = first; n * 23 - d < Runner.WIDTH; n++) {
         long h = (n * 2654435761L) >>> 7;
         if (h % 3 != 0) {
            int x = (int) Math.round(n * 23 - d);
            g.fillRect(x, Runner.GROUND + 3 + (int) (h % 7), 2 + (int) (h % 4), 1);
         }
      }

      for (Runner.Obstacle o : this.runner.obstacles()) {
         int x = (int) Math.round(o.x);
         if (o.kind == Runner.Kind.BIRD) {
            Sprites.draw(g, this.runner.steps() / 10 % 2 == 0 ? Sprites.BIRD_UP : Sprites.BIRD_DOWN, x, o.top(), 2);
         } else {
            int unit = o.w / o.size;
            for (int j = 0; j < o.size; j++) {
               Sprites.cactus(g, x + j * unit, o.top(), o.kind == Runner.Kind.LARGE_CACTUS);
            }
         }
      }
      this.paintDino(g);
      this.paintScore(g);
      if (this.bot.isOn()) {
         this.paintBot(g);
      }
      this.paintMessage(g);

      g.setColor(STRIP_BG);
      g.fillRect(0, STRIP, Runner.WIDTH, HEIGHT - STRIP);
      g.setColor(MUTED);
      g.setFont(SMALL);
      String help = this.bot.isOn()
         ? "B: bot off   Drag the red marker to change what the bot watches   X: put it back"
         : "Space or ↑: jump (hold for higher)   ↓: duck   B: let the original bot play";
      centered(g, help, STRIP + 16);
   }

   private void paintDino(Graphics2D g) {
      int top = this.runner.dinoTop();
      g.setColor(INK);
      if (this.runner.isDucking()) {
         Sprites.draw(g, this.runner.steps() / 6 % 2 == 0 ? Sprites.DUCK_A : Sprites.DUCK_B, Runner.DINO_X, top, 2);
         return;
      }
      boolean running = this.runner.state() == Runner.State.RUNNING && !this.runner.isJumping();
      String[] legs = !running ? Sprites.LEGS_STAND : this.runner.steps() / 6 % 2 == 0 ? Sprites.LEGS_A : Sprites.LEGS_B;
      Sprites.draw(g, Sprites.DINO, Runner.DINO_X, top, 2);
      Sprites.draw(g, legs, Runner.DINO_X, top + Sprites.DINO.length * 2, 2);
      if (this.runner.state() == Runner.State.CRASHED) {
         // A wide-open eye.
         g.setColor(PAPER);
         g.fillRect(Runner.DINO_X + 23, top + 1, 6, 6);
         g.setColor(INK);
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

   private static void centered(Graphics2D g, String s, int y) {
      g.drawString(s, (Runner.WIDTH - g.getFontMetrics().stringWidth(s)) / 2, y);
   }

   private static String pad(int n) {
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
}
