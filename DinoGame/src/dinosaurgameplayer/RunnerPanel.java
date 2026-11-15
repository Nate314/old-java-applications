package dinosaurgameplayer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
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
 * (Evolution, drawn by EvolveView) and back. A run has its sky (night every 700 points), dust at the dinosaur's feet,
 * a blinking score every 100 points, beeps (M) and a shake when it crashes. G races the ghost of your best run on its
 * own course. P races two players on one keyboard (Duel). L plays by ear (Ears): beats sound each obstacle, and the
 * blindfold hides them.
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
   private static final Color DUST = new Color(0xBDBDBD);
   /** The moon and the stars, as night shows them (they are not there by day). */
   private static final Color MOON = new Color(0xE6E6E6);
   /** The second player's dinosaur, in a two-player race. */
   static final Color P2 = new Color(0xB85A1B);
   static final Font SCORE = new Font(Font.MONOSPACED, Font.BOLD, 15);
   static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 16);
   static final Font TEXT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
   private static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
   static final Font TINY = new Font(Font.SANS_SERIF, Font.BOLD, 10);
   /** Evolution speeds, in steps per game step. */
   private static final int[] SPEEDS = { 1, 4, 16 };
   /** How long after a first N a second one starts the evolution over. */
   private static final long CONFIRM_NANOS = 3_000_000_000L;
   /** How long the score blinks after each 100 points, and the NEW BEST label shows. */
   private static final int FLASH_STEPS = 60;
   private static final int BEST_STEPS = 150;
   private static final int SHAKE_STEPS = 14;

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
   private final Sky sky = new Sky();
   private final Dust dust = new Dust();
   private final Sounds sounds;
   private int hundreds;
   private int flashSteps;
   private boolean passedBest;
   private int bestSteps;
   private int shakeSteps;
   /** G: every run is the saved ghost's course, with the ghost playing beside you (ghost, from the run's start). */
   private boolean racing;
   private Ghost ghost;
   /** P: non-null while two players race (P1 is runner); tapped is the runner a click or tap is holding. */
   private Duel duel;
   private Runner tapped;
   /** L: Play by ear's beats, and with the blindfold the obstacles hidden while a run is under way. */
   private Ears.Mode ears;

   /** A quiet panel, for the self-check. */
   RunnerPanel(Records records, long seed) {
      this(records, seed, Sounds.silent(records.sound));
   }

   RunnerPanel(Records records, long seed, Sounds sounds) {
      this.runner = new Runner(seed);
      this.records = records;
      this.sounds = sounds;
      this.ears = records.ears;
      this.bot.moveTo(records.markerX, records.markerY);
      this.setPreferredSize(new Dimension(Runner.WIDTH, HEIGHT));
      this.setFocusable(true);
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (RunnerPanel.this.evolution == null && k == KeyEvent.VK_M) {
               RunnerPanel.this.toggleSound();
            } else if (RunnerPanel.this.evolution != null || k == KeyEvent.VK_E) {
               RunnerPanel.this.evolveKey(k);
            } else if (k == KeyEvent.VK_P) {
               RunnerPanel.this.togglePlayers();
            } else if (k == KeyEvent.VK_G) {
               RunnerPanel.this.toggleRace();
            } else if (RunnerPanel.this.duel != null) {
               RunnerPanel.this.duelKey(k, true);
            } else if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
               RunnerPanel.this.jumpKey(true);
            } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
               RunnerPanel.this.duckKey(true);
            } else if (k == KeyEvent.VK_B) {
               RunnerPanel.this.toggleBot();
            } else if (k == KeyEvent.VK_X) {
               RunnerPanel.this.resetMarker();
            } else if (k == KeyEvent.VK_L) {
               RunnerPanel.this.toggleEars();
            }
         }

         @Override
         public void keyReleased(KeyEvent e) {
            int k = e.getKeyCode();
            if (RunnerPanel.this.evolution != null) {
               return;
            }
            if (RunnerPanel.this.duel != null) {
               RunnerPanel.this.duelKey(k, false);
            } else if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
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
            } else if (RunnerPanel.this.duel != null) {
               // Two players on one screen: the left half jumps P1, the right half P2.
               Duel d = RunnerPanel.this.duel;
               RunnerPanel.this.tapped = e.getX() < Runner.WIDTH / 2 ? d.p1 : d.p2;
               RunnerPanel.this.duelJump(RunnerPanel.this.tapped, true);
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
            } else if (RunnerPanel.this.tapped != null) {
               RunnerPanel.this.tapped.release();
               RunnerPanel.this.tapped = null;
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

   /** In a two-player race: Space, W and S are P1's jump and duck, the up and down arrows P2's. */
   private void duelKey(int k, boolean down) {
      Duel d = this.duel;
      Runner r = k == KeyEvent.VK_SPACE || k == KeyEvent.VK_W || k == KeyEvent.VK_S ? d.p1
         : k == KeyEvent.VK_UP || k == KeyEvent.VK_DOWN ? d.p2 : null;
      if (r == null) {
         return;
      }
      if (k == KeyEvent.VK_S || k == KeyEvent.VK_DOWN) {
         r.duck(down);
      } else {
         this.duelJump(r, down);
      }
   }

   /** A jump key in a two-player race: starts both at once (the start is not a jump), then jumps its own dinosaur. */
   private void duelJump(Runner r, boolean down) {
      Duel d = this.duel;
      if (!down) {
         r.release();
      } else if (d.running()) {
         r.press();
      } else if (this.runner.state() == Runner.State.READY
         || this.runner.idleSteps() >= RESTART_LOCK && d.p2.idleSteps() >= RESTART_LOCK) {
         d.start();
      }
   }

   /** P: two players race the same course, or back to one. Leaves the bot, the ghost and the evolution. */
   void togglePlayers() {
      boolean on = this.duel == null;
      this.setEvolving(false);
      this.setBot(false);
      this.duel = on ? new Duel(this.runner, System.nanoTime()) : null;
      this.repaint();
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
      this.toReady();
      this.racing = false;
      this.duel = null;
      this.dragging = false;
      this.confirmUntil = 0;
      this.evolution = on ? this.newEvolution() : null;
      this.setCursor(Cursor.getDefaultCursor());
   }

   /** G: race the ghost of your best run on its course, or back to a new course each run. */
   void toggleRace() {
      if (this.records.ghost == null && !this.racing) {
         return;
      }
      this.toReady();
      this.duel = null;
      this.racing = !this.racing;
      this.runner.setCourse(this.racing ? this.records.ghost.course : null);
      this.repaint();
   }

   /** A run under way when the mode changes is dropped, and counts for nobody. */
   private void toReady() {
      this.runner.duck(false);
      this.runner.ready();
      this.seen = Runner.State.READY;
      this.crashLines = new String[0];
      this.ghost = null;
      this.tapped = null;
      if (this.duel != null) {
         this.duel.ready();
      }
      this.runner.setCourse(null);
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
            if (c[2] == KeyEvent.VK_M) {
               this.toggleSound();
            } else if (c[2] == KeyEvent.VK_G) {
               this.toggleRace();
            } else if (c[2] == KeyEvent.VK_P) {
               this.togglePlayers();
            } else if (c[2] == KeyEvent.VK_L) {
               this.toggleEars();
            } else {
               this.evolveKey(c[2]);
            }
            return;
         }
      }
   }

   /** M: the beeps on or off, remembered. */
   private void toggleSound() {
      this.sounds.setOn(!this.sounds.isOn());
      this.records.sound = this.sounds.isOn();
      this.records.save();
      this.repaint();
   }

   /** L: off, beats, beats with the blindfold, and off again; remembered. */
   void toggleEars() {
      this.ears = Ears.next(this.ears);
      this.records.ears = this.ears;
      this.records.save();
      this.repaint();
   }

   /** Whether the screen hides the obstacles now: blindfolded, during a run of one player (shown again at a crash). */
   boolean blindfolded() {
      return this.ears == Ears.Mode.BLINDFOLD && this.duel == null && this.evolution == null
         && this.runner.state() == Runner.State.RUNNING;
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
      if (this.duel != null) {
         this.duelStep();
         return;
      }
      if (this.bot.isOn()) {
         if (this.frame == null) {
            this.frame = new BufferedImage(Runner.WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
         }
         Graphics2D g = this.frame.createGraphics();
         this.paintScene(g, false);   // the bot is never blindfolded
         g.dispose();
         this.bot.step(this.runner, (x, y) -> this.frame.getRGB(x, y) & 0xFFFFFF);
      }
      if (this.seen != Runner.State.RUNNING && this.runner.state() == Runner.State.RUNNING) {
         this.botWholeRun = this.bot.isOn();
         this.botEver = this.bot.isOn();
         this.humanHelped = false;
         this.crashLines = new String[0];
         this.bot.newRun();
         this.sky.reset();
         this.dust.clear();
         this.hundreds = 0;
         this.flashSteps = 0;
         this.passedBest = false;
         this.bestSteps = 0;
         this.ghost = this.racing ? this.records.ghost : null;
         if (this.ghost != null) {
            this.ghost.start();
         }
      }
      // The ghost runs beside you, and after you crash runs on until it is off the screen.
      if (this.ghost != null && this.ghost.runner().state() == Runner.State.RUNNING
         && (this.runner.state() == Runner.State.RUNNING || this.ghostOffset() < Runner.WIDTH)) {
         this.ghost.step();
      }
      boolean wasRunning = this.runner.state() == Runner.State.RUNNING;
      boolean wasJumping = this.runner.isJumping();
      this.runner.step();
      this.flashSteps = Math.max(0, this.flashSteps - 1);
      this.bestSteps = Math.max(0, this.bestSteps - 1);
      this.shakeSteps = Math.max(0, this.shakeSteps - 1);
      if (wasRunning && this.runner.state() == Runner.State.CRASHED) {
         this.shakeSteps = SHAKE_STEPS;
         this.sounds.crash();
         this.crashed();
      } else if (this.runner.state() == Runner.State.RUNNING) {
         this.runStep(wasJumping);
      }
      this.seen = this.runner.state();
   }

   /** After a running step: the sky, the dust, the jump blip, the chime every 100 points and a new best. */
   private void runStep(boolean wasJumping) {
      if (this.ears != Ears.Mode.OFF) {
         for (int[] b : Ears.beats(this.runner)) {
            this.sounds.beat(b[0], (int) Math.round(b[1] * Runner.STEP_MS));
         }
      }
      int score = this.runner.score();
      this.sky.step(score);
      this.dust.step(this.runner);
      if (this.runner.isJumping() && !wasJumping) {
         this.sounds.jump();
      }
      if (!this.passedBest && !this.botEver && this.records.best > 0 && score > this.records.best) {
         this.passedBest = true;
         this.bestSteps = BEST_STEPS;
         this.flashSteps = 0;
         this.sounds.newBest();
      } else if (score / 100 > this.hundreds) {
         this.flashSteps = FLASH_STEPS;
         this.sounds.milestone();
      }
      this.hundreds = score / 100;
   }

   /** One step of a two-player race: both dinosaurs, then the sky and dust of the one in front, and the result. */
   private void duelStep() {
      Duel d = this.duel;
      if (this.seen != Runner.State.RUNNING && d.running()) {
         this.sky.reset();
         this.dust.clear();
         this.hundreds = 0;
         this.flashSteps = 0;
         this.crashLines = new String[0];
      }
      this.flashSteps = Math.max(0, this.flashSteps - 1);
      this.shakeSteps = Math.max(0, this.shakeSteps - 1);
      for (Runner r : new Runner[] { d.p1, d.p2 }) {
         boolean wasRunning = r.state() == Runner.State.RUNNING;
         boolean wasJumping = r.isJumping();
         r.step();
         if (wasRunning && r.state() == Runner.State.CRASHED) {
            this.shakeSteps = SHAKE_STEPS;
            this.sounds.crash();
         } else if (r.isJumping() && !wasJumping) {
            this.sounds.jump();
         }
      }
      Runner v = d.view();
      if (v.state() == Runner.State.RUNNING) {
         this.sky.step(v.score());
         this.dust.step(v);
         if (v.score() / 100 > this.hundreds) {
            this.flashSteps = FLASH_STEPS;
            this.sounds.milestone();
         }
         this.hundreds = v.score() / 100;
      }
      d.score();
      if (d.over() && this.crashLines.length == 0) {
         this.crashLines = d.lines();
      }
      this.seen = d.running() ? Runner.State.RUNNING : this.runner.state();
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
            PixelBot.whyItCrashed(this.runner, this.sky.darkness() > 0) };
      } else if (this.botEver) {
         this.crashLines = new String[] { "You and the bot scored " + score + " together.",
            "Team runs do not count. Space starts a run of your own." };
      } else {
         boolean best = score > this.records.best;
         if (best) {
            this.records.best = score;
         }
         Ghost raced = this.ghost;
         boolean newGhost = this.records.ghost == null || score > this.records.ghost.score;
         if (newGhost) {
            this.records.ghost = Ghost.of(this.runner);
         }
         if (best || newGhost) {
            this.records.save();
         }
         String bot = raced != null
            ? score > raced.score ? "You beat your ghost by " + (score - raced.score) + ". This run is your ghost now."
               : score == raced.score ? "A dead heat with your ghost." : "Your ghost runs on to " + raced.score + ". Watch where it jumps."
            : this.records.botBest == 0 ? "Press B to see how far the original bot gets."
            : score > this.records.botBest ? "You beat the bot's best of " + this.records.botBest + "."
            : "The bot's best is " + this.records.botBest + ". Beat it?";
         this.crashLines = new String[] { (best && score > 0 ? "New best: " : "You scored ") + score + ".", bot };
      }
   }

   // ---- drawing ----

   @Override
   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      if (this.shakeSteps > 0 && this.evolution == null && !reducedMotion()) {
         // A crash shakes the picture (only on screen: the bot's frame stays still).
         int a = (this.shakeSteps + 1) / 3;
         int dx = this.shakeSteps % 2 == 0 ? a : -a;
         int dy = this.shakeSteps % 4 < 2 ? a / 2 : -a / 2;
         g2.setColor(Sky.tint(PAPER, this.sky.darkness()));
         g2.fillRect(0, 0, Runner.WIDTH, HEIGHT);
         g2.translate(dx, dy);
         this.paintScene(g2, this.blindfolded());
         g2.translate(-dx, -dy);
      } else {
         this.paintScene(g2, this.blindfolded());
      }
   }

   /** In the browser, when the system asks for reduced motion: the crash does not shake (null on the desktop JDK). */
   private static boolean reducedMotion() {
      return Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
   }

   private void paintScene(Graphics2D g, boolean blind) {
      double dark = this.evolution != null ? 0 : this.sky.darkness();
      if (this.evolution != null) {
         EvolveView.paint(g, this.evolution, SPEEDS[this.speed]);
      } else if (this.duel != null) {
         Runner v = this.duel.view();
         paintWorld(g, v, this.sky);
         this.dust.paint(g, Sky.tint(DUST, dark), Sky.tint(PAPER, dark));
         // Two that fell at the same obstacle stand together: P1 on top, P2's label lifted over P1's.
         Duel d = this.duel;
         boolean together = Math.abs(d.p2.distance() + d.p2.dinoX() - d.p1.distance() - d.p1.dinoX()) < 30;
         this.paintBeside(g, d.p2, v, Sky.tint(P2, dark), "P2", together ? 12 : 0, dark);
         this.paintBeside(g, d.p1, v, Sky.tint(INK, dark), "P1", 0, dark);
         this.paintDuelScore(g, dark);
         this.paintMessage(g, dark);
      } else {
         paintWorld(g, this.runner, this.sky, !blind);
         this.dust.paint(g, Sky.tint(DUST, dark), Sky.tint(PAPER, dark));
         if (this.racing && !this.bot.isOn()) {
            g.setFont(SCORE);
            g.setColor(Sky.tint(MUTED, dark));
            g.drawString("GHOST " + pad(this.records.ghost.score), 14, 24);
         }
         if (this.ears != Ears.Mode.OFF && !this.bot.isOn()) {
            g.setFont(TINY);
            g.setColor(Sky.tint(MUTED, dark));
            g.drawString(this.ears == Ears.Mode.BLINDFOLD ? "BLINDFOLD: LISTEN" : "PLAYING BY EAR", 14, this.racing ? 40 : 24);
         }
         if (this.ghost != null && !this.bot.isOn()) {
            this.paintGhost(g, dark);
         }
         paintDino(g, this.runner, Sky.tint(INK, dark), Sky.tint(PAPER, dark));
         this.paintScore(g, dark);
         if (this.bot.isOn()) {
            this.paintBot(g);
         }
         this.paintMessage(g, dark);
      }
      this.paintStrip(g, dark);
   }

   /** The strip along the bottom: what the keys do, and buttons a click or a tap presses. */
   private void paintStrip(Graphics2D g, double dark) {
      Color paper = Sky.tint(PAPER, dark);
      Color ink = Sky.tint(INK, dark);
      Color muted = Sky.tint(MUTED, dark);
      g.setColor(Sky.tint(STRIP_BG, dark));
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
      } else if (this.duel != null) {
         help = "P1: W or Space, S   P2: ↑ and ↓";
         labels = new String[] { this.sounds.isOn() ? "Sound on (M)" : "Sound off (M)", "One player (P)", "Evolve a bot (E)" };
         keys = new int[] { KeyEvent.VK_M, KeyEvent.VK_P, KeyEvent.VK_E };
      } else {
         help = this.bot.isOn()
            ? "B: bot off   Drag its red eye to change what it watches   X: put it back"
            : "Space or ↑: jump (hold for higher)   ↓: duck   B: the original bot plays";
         String ear = this.ears == Ears.Mode.OFF ? "By ear (L)" : this.ears == Ears.Mode.CUES ? "Beats on (L)" : "Blindfold (L)";
         if (this.records.ghost == null && !this.racing) {
            labels = new String[] { this.sounds.isOn() ? "Sound on (M)" : "Sound off (M)", ear, "Two players (P)", "Evolve a bot (E)" };
            keys = new int[] { KeyEvent.VK_M, KeyEvent.VK_L, KeyEvent.VK_P, KeyEvent.VK_E };
         } else {
            help = this.bot.isOn() ? "B: bot off   Drag its red eye" : "Space or ↑: jump   ↓: duck";
            labels = new String[] { this.sounds.isOn() ? "Sound on (M)" : "Sound off (M)", ear,
               this.racing ? "New courses (G)" : "Race your ghost (G)", "Two players (P)", "Evolve a bot (E)" };
            keys = new int[] { KeyEvent.VK_M, KeyEvent.VK_L, KeyEvent.VK_G, KeyEvent.VK_P, KeyEvent.VK_E };
         }
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
         boolean main = keys[i] == KeyEvent.VK_E || keys[i] == KeyEvent.VK_G && this.racing || keys[i] == KeyEvent.VK_P && this.duel != null
            || keys[i] == KeyEvent.VK_L && this.ears != Ears.Mode.OFF;
         g.setColor(main ? ink : paper);
         g.fillRoundRect(c[0], STRIP + 3, c[1], HEIGHT - STRIP - 6, 8, 8);
         g.setColor(main ? paper : muted);
         g.drawString(labels[i], c[0] + 8, STRIP + 16);
      }
      g.setColor(muted);
      if (fm.stringWidth(help) < x - 12) {
         g.drawString(help, 12, STRIP + 16);
      }
   }

   /** The background, the ground and the obstacles of runner's course, by day. */
   static void paintWorld(Graphics2D g, Runner runner) {
      paintWorld(g, runner, null);
   }

   /** The same under sky (or by day when it is null): at night inverted, with the moon and stars. */
   static void paintWorld(Graphics2D g, Runner runner, Sky sky) {
      paintWorld(g, runner, sky, true);
   }

   /** The same, leaving out the obstacles unless obstacles is set (the blindfold). */
   static void paintWorld(Graphics2D g, Runner runner, Sky sky, boolean obstacles) {
      double dark = sky == null ? 0 : sky.darkness();
      Color paper = Sky.tint(PAPER, dark);
      g.setColor(paper);
      g.fillRect(0, 0, Runner.WIDTH, HEIGHT);
      if (dark > 0) {
         paintNightSky(g, sky, paper, dark);
      }
      g.setColor(Sky.tint(CLOUD, dark));
      double[] clouds = runner.clouds();
      for (int i = 0; i < clouds.length; i++) {
         Sprites.draw(g, Sprites.CLOUD, (int) clouds[i], 46 + i % 2 * 24, 2);
      }

      // The ground: a line, with pebbles below it that scroll with the run.
      Color ink = Sky.tint(INK, dark);
      g.setColor(ink);
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

      for (Runner.Obstacle o : obstacles ? runner.obstacles() : new ArrayList<Runner.Obstacle>()) {
         int x = (int) Math.round(o.x);
         g.setColor(ink);
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

   /**
    * The moon and three stars, kept above the highest spot the bot's marker can sit (they are never blue 83 anyway,
    * but on the web the moon's soft edge blends colors). The moon drifts slowly left; its phase changes each night.
    */
   private static void paintNightSky(Graphics2D g, Sky sky, Color paper, double dark) {
      int step = Math.max(0, sky.nightStep());
      int n = sky.nights();
      for (int i = 0; i < 3; i++) {
         int x = Math.floorMod((int) Math.round(90 + i * 230 + n * 137 - step * 0.15), Runner.WIDTH);
         int y = 16 + (i * 17 + n * 11) % 30;
         double twinkle = 0.55 + 0.45 * Math.sin(step * 0.09 + i * 2.1);
         g.setColor(Sky.mix(paper, MOON, dark * twinkle));
         g.fillRect(x - 2, y, 5, 1);
         g.fillRect(x, y - 2, 1, 5);
      }
      int d = 20;
      int mx = (int) Math.round(560 - step * 0.25);
      int my = 18;
      g.setColor(Sky.mix(paper, MOON, dark));
      g.fillOval(mx, my, d, d);
      int k = Math.abs(sky.moonPhase() - 3);
      if (k > 0) {
         // The shadow: a disc of night over the moon, further off for a fuller moon.
         int[] offsets = { 0, 15, 10, 5 };
         int off = sky.moonPhase() < 3 ? -offsets[k] : offsets[k];
         g.setColor(paper);
         g.fillOval(mx + off, my - 1, d + 2, d + 2);
      }
   }

   /** runner's dinosaur, in color, by day. */
   static void paintDino(Graphics2D g, Runner runner, Color color) {
      paintDino(g, runner, color, PAPER);
   }

   /** runner's dinosaur in color on paper (the white of its eye after a crash). */
   static void paintDino(Graphics2D g, Runner runner, Color color, Color paper) {
      int top = runner.dinoTop();
      int x = runner.dinoX();
      g.setColor(color);
      if (runner.isDucking()) {
         Sprites.draw(g, runner.steps() / 6 % 2 == 0 ? Sprites.DUCK_A : Sprites.DUCK_B, x, top, 2);
         return;
      }
      boolean running = runner.state() == Runner.State.RUNNING && !runner.isJumping();
      String[] legs = !running ? Sprites.LEGS_STAND : runner.steps() / 6 % 2 == 0 ? Sprites.LEGS_A : Sprites.LEGS_B;
      Sprites.draw(g, Sprites.DINO, x, top, 2);
      Sprites.draw(g, legs, x, top + Sprites.DINO.length * 2, 2);
      if (runner.state() == Runner.State.CRASHED) {
         // A wide-open eye.
         g.setColor(paper);
         g.fillRect(x + 23, top + 1, 6, 6);
         g.setColor(color);
         g.fillRect(x + 25, top + 3, 2, 2);
      }
   }

   /** How far the ghost has run past you (less than 0 once it has crashed and you run on). */
   private double ghostOffset() {
      return this.ghost.runner().distance() - this.runner.distance();
   }

   /**
    * The ghost: your best run's dinosaur, see-through, where it is on this course. While you both run it is beside you;
    * once it crashes it is left behind, and once you crash it runs on over the obstacles still on the screen.
    */
   private void paintGhost(Graphics2D g, double dark) {
      Runner r = this.ghost.runner();
      Color ink = Sky.tint(INK, dark);
      boolean apart = Math.round(this.ghostOffset()) != 0 || r.state() == Runner.State.CRASHED || r.dinoTop() != this.runner.dinoTop();
      this.paintBeside(g, r, this.runner, new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 80), apart ? "ghost" : null, 0, dark);
   }

   /**
    * Runner r's dinosaur on view's course, as far ahead or behind as it has run, with a label above it (and its score
    * once it has crashed), lifted by lift pixels. A null label draws none.
    */
   private void paintBeside(Graphics2D g, Runner r, Runner view, Color color, String label, int lift, double dark) {
      int dx = (int) Math.round(r.distance() - view.distance());
      if (r.dinoX() + dx + Runner.DUCK_W < 0 || r.dinoX() + dx > Runner.WIDTH) {
         return;
      }
      g.translate(dx, 0);
      paintDino(g, r, color, Sky.tint(PAPER, dark));
      g.translate(-dx, 0);
      if (label != null) {
         g.setFont(TINY);
         g.setColor(Sky.tint(MUTED, dark));
         g.drawString(r.state() == Runner.State.CRASHED ? label + " " + r.score() : label, r.dinoX() + dx + 2, r.dinoTop() - 4 - lift);
      }
   }

   /** Top right in a two-player race: each player's score in its color; top left, the rounds each has won. */
   private void paintDuelScore(Graphics2D g, double dark) {
      g.setFont(SCORE);
      FontMetrics fm = g.getFontMetrics();
      String two = "P2 " + pad(this.duel.p2.score());
      String one = "P1 " + pad(this.duel.p1.score()) + "  ";
      int x = Runner.WIDTH - 14 - fm.stringWidth(two);
      g.setColor(Sky.tint(P2, dark));
      g.drawString(two, x, 24);
      g.setColor(Sky.tint(INK, dark));
      g.drawString(one, x - fm.stringWidth(one), 24);
      g.setColor(Sky.tint(MUTED, dark));
      g.drawString("WINS " + this.duel.wins(1) + " - " + this.duel.wins(2), 14, 24);
   }

   /** Top right, as in Chrome's game: the bot's best in red, your best, then this run. */
   private void paintScore(Graphics2D g, double dark) {
      g.setFont(SCORE);
      FontMetrics fm = g.getFontMetrics();
      // For a moment after each 100 points the score shows the round hundred and blinks.
      boolean flashing = this.flashSteps > 0 && this.runner.state() == Runner.State.RUNNING;
      String now = pad(flashing ? this.hundreds * 100 : this.runner.score());
      String hi = this.records.best > 0 ? "HI " + pad(this.records.best) + "  " : "";
      int x = Runner.WIDTH - 14 - fm.stringWidth(hi + now);
      g.setColor(Sky.tint(INK, dark));
      g.drawString(flashing && this.flashSteps / 8 % 2 == 1 ? hi : hi + now, x, 24);
      if (this.bestSteps > 0 && this.bestSteps / 10 % 2 == 0) {
         g.setFont(TINY);
         String label = "NEW BEST";
         g.drawString(label, Runner.WIDTH - 14 - g.getFontMetrics().stringWidth(label), 40);
         g.setFont(SCORE);
      }
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

   private void paintMessage(Graphics2D g, double dark) {
      Runner.State state = this.runner.state();
      if (state == Runner.State.RUNNING || this.duel != null && this.duel.running()) {
         return;
      }
      String title = state != Runner.State.READY ? "G A M E   O V E R" : this.duel != null ? "Two players: Space, W or ↑ to run" : "Space to run";
      String[] lines = state == Runner.State.READY && this.duel != null
         ? new String[] { "P1 jumps with W or Space and ducks with S. P2 has ↑ and ↓.",
            "One course for both, P2 a step ahead. Or tap your half of the game." }
         : state == Runner.State.READY && this.ears != Ears.Mode.OFF && !this.bot.isOn()
         ? new String[] { "Each cactus and low bird sounds two low beats: jump on the second, hold while it rings.",
            this.ears == Ears.Mode.BLINDFOLD ? "Blindfold: you will not see them. A middle bird beats high: duck."
               : "A middle bird beats high: duck on the second. High birds are silent." }
         : state == Runner.State.READY && this.racing
         ? new String[] { "Your ghost's course: the run where you scored " + this.records.ghost.score + ".",
            "It runs beside you, jump for jump. Get further and you are the ghost." }
         : state == Runner.State.READY
         ? new String[] { "Jump the cacti, duck the birds. Night falls every " + Sky.NIGHT_EVERY + ".",
            "Or press B and the original bot plays, watching one pixel." }
         : this.crashLines;
      Color ink = Sky.tint(INK, dark);
      g.setColor(ink);
      g.setFont(TITLE);
      centered(g, title, 82);
      g.setFont(TEXT);
      g.setColor(Sky.tint(MUTED, dark));
      for (int i = 0; i < lines.length; i++) {
         centered(g, lines[i], 104 + i * 18);
      }
      if (state == Runner.State.CRASHED && !this.bot.isOn() && this.runner.idleSteps() >= RESTART_LOCK
         && (this.duel == null || this.duel.p2.idleSteps() >= RESTART_LOCK)) {
         paintRestart(g, ink, Runner.WIDTH / 2, 147);
      }
   }

   /** A round arrow under GAME OVER: Space, a click or a tap runs again. */
   private static void paintRestart(Graphics2D g, Color ink, int cx, int cy) {
      g.setColor(ink);
      g.setStroke(new BasicStroke(3f));
      g.drawArc(cx - 9, cy - 9, 18, 18, 110, 290);
      g.setStroke(new BasicStroke(1f));
      // The arrowhead at the arc's open end, pointing round the circle.
      g.fillPolygon(new int[] { cx - 1, cx + 7, cx + 1 }, new int[] { cy - 14, cy - 9, cy - 3 }, 3);
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

   Ghost ghost() {
      return this.ghost;
   }

   Evolution evolution() {
      return this.evolution;
   }

   Duel duel() {
      return this.duel;
   }
}
