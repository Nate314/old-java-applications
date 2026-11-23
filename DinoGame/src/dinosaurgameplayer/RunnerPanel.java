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
 * blindfold hides them. T after a crash shows the whole run in one picture (RunPicture).
 */
final class RunnerPanel extends JPanel {
   private static final long serialVersionUID = 1L;
   static final int HEIGHT = 220;
   static final int STRIP = 196;
   /** Steps after a crash before Space starts a new run, so a held key does not restart at once. */
   private static final int RESTART_LOCK = 24;
   /** Until your best reaches this (the first night), runs show the mark that says when to jump or duck. */
   static final int GUIDE_UNTIL = Sky.NIGHT_EVERY;
   /** The mark stays lit at least this many steps (a fifth of a second), so a tap's moment can be seen. */
   private static final int GUIDE_LIT = 12;
   /** Jumps held up to this many steps are shown as TAP (a short hop clears them best), longer ones as HOLD. */
   static final int GUIDE_TAP = 7;
   /** On a screen narrower than the game (a phone held upright): the thumb pads' height, under the strip. */
   static final int PADS = 190;
   private static final Font PAD = new Font(Font.SANS_SERIF, Font.BOLD, 36);
   private static final Font PAD_TEXT = new Font(Font.SANS_SERIF, Font.PLAIN, 19);

   /** Words, the score, the ground line and the marks: a dark warm brown. */
   static final Color INK = new Color(0x4A3728);
   /** A warm desert sky; under the ground line, sand. */
   static final Color PAPER = new Color(0xFBEFD9);
   static final Color SAND = new Color(0xF1DDB6);
   static final Color CLOUD = new Color(0xE6CFAE);
   static final Color SUN = new Color(0xFFE0A3);
   static final Color MUTED = new Color(0x7A6650);
   static final Color STRIP_BG = new Color(0xEFDDBF);
   /** The dinosaur: deep teal, so it never reads as an obstacle. */
   static final Color DINO = new Color(0x2E6E7E);
   /**
    * The obstacles keep blue at exactly 83 (0x53), the one number the original bot knows, whatever their red and
    * green: sage cacti with a lighter rib, plum birds. Nothing else the game draws has blue 83.
    */
   static final Color CACTUS = new Color(0x4E8553);
   static final Color CACTUS_LIGHT = new Color(0x74A653);
   static final Color BIRD = new Color(0x8E4F53);
   static final Color MARKER = new Color(255, 0, 0);
   private static final Color BOT_TEXT = new Color(0xC62828);
   static final Color DUST = new Color(0xD8BF97);
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
   /** The slow replay after a coached crash: steps it waits (past the shake), starts before the press and runs past the crash, then rests. */
   private static final int REPLAY_WAIT = 40;
   private static final int REPLAY_LEAD = 20;
   private static final int REPLAY_AFTER = 24;
   private static final int REPLAY_HOLD = 30;

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
   /** After a run of your own crashes: what would have cleared it, or null. */
   private Coach coach;
   /** The slow replay of the crash: your run (see-through) and the coach's fix (solid), and its frame, or -1 before it starts. */
   private Ghost replayYou;
   private Ghost replayFix;
   private int replayAt = -1;
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
   /** T after a crash: the run just ended, in one picture, shown instead of the game until a key goes on. */
   private RunPicture picture;
   /** Whether the thumb pads are under the game, and the one a finger holds (Ears.JUMP or Ears.DUCK, else 0). */
   private final boolean pads;
   private int padHeld;

   /** A quiet panel, for the self-check. */
   RunnerPanel(Records records, long seed) {
      this(records, seed, Sounds.silent(records.sound), false);
   }

   RunnerPanel(Records records, long seed, Sounds sounds, boolean pads) {
      this.pads = pads;
      this.runner = new Runner(seed);
      this.records = records;
      this.sounds = sounds;
      this.ears = records.ears;
      this.bot.moveTo(records.markerX, records.markerY);
      this.setPreferredSize(new Dimension(Runner.WIDTH, HEIGHT + (pads ? PADS : 0)));
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
            } else if (k == KeyEvent.VK_T) {
               RunnerPanel.this.togglePicture();
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
            if (e.getY() >= HEIGHT && RunnerPanel.this.evolution == null && RunnerPanel.this.duel == null) {
               RunnerPanel.this.padPress(e.getX() < Runner.WIDTH / 2 ? Ears.DUCK : Ears.JUMP);
            } else if (e.getY() >= STRIP && e.getY() < HEIGHT) {
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
            if (RunnerPanel.this.padHeld != 0) {
               RunnerPanel.this.padRelease();
            } else if (RunnerPanel.this.dragging) {
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
      if (down) {
         this.picture = null;
      }
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

   /** A finger on a thumb pad: DUCK holds Down, JUMP is Space (it starts a run, and runs again after a crash). */
   void padPress(int need) {
      this.padHeld = need;
      if (need == Ears.DUCK) {
         this.duckKey(true);
      } else {
         this.jumpKey(true);
      }
      this.repaint();
   }

   void padRelease() {
      if (this.padHeld == Ears.DUCK) {
         this.duckKey(false);
      } else {
         this.jumpKey(false);
      }
      this.padHeld = 0;
      this.repaint();
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
      this.picture = null;
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
      this.picture = null;
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
            } else if (c[2] == KeyEvent.VK_T) {
               this.togglePicture();
            } else {
               this.evolveKey(c[2]);
            }
            return;
         }
      }
   }

   /** Whether T can show the run just ended: one player, the bot off, after a crash. */
   private boolean canPicture() {
      return this.evolution == null && this.duel == null && !this.bot.isOn() && this.runner.state() == Runner.State.CRASHED;
   }

   /** T: the whole run just ended in one picture (with the ghost raced, if any), or back to the crash screen. */
   void togglePicture() {
      this.picture = this.picture == null && this.canPicture()
         ? new RunPicture(Ghost.of(this.runner), this.ghost == null ? null : this.ghost.copy()) : null;
      this.repaint();
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
      } else if (this.runner.state() == Runner.State.CRASHED) {
         this.replayStep();
      }
      this.seen = this.runner.state();
   }

   /** Whether the slow replay shows: after your coached crash, past the shake, unless the system asks for reduced motion. */
   boolean replaying() {
      return this.coach != null && this.runner.state() == Runner.State.CRASHED && !this.bot.isOn() && this.picture == null
         && this.runner.idleSteps() >= REPLAY_WAIT && !reducedMotion();
   }

   /** One step of the slow replay, at half speed: from just before the press to just past the crash, a rest, and again. */
   private void replayStep() {
      Coach c = this.coach;
      if (!this.replaying() || (this.runner.idleSteps() - REPLAY_WAIT) % 2 != 0) {
         return;
      }
      int begin = Math.max(0, Math.min(c.step, c.press >= 0 ? c.press : c.step) - REPLAY_LEAD);
      int length = (int) this.runner.steps() + REPLAY_AFTER - begin;
      this.replayAt = (this.replayAt + 1) % (length + REPLAY_HOLD);
      if (this.replayAt == 0) {
         // shortcut: each loop plays the run again from its start (a few ms after a long run); keep a copy at begin if that ever shows.
         this.replayYou = Ghost.of(this.runner);
         this.replayFix = c.fixed();
         this.replayYou.start();
         this.replayFix.start();
         while (this.replayFix.runner().steps() < begin) {
            this.replayYou.step();
            this.replayFix.step();
         }
      } else if (this.replayAt <= length) {
         this.replayYou.step();
         this.replayFix.step();
      }
   }

   /** The fix's run while the slow replay shows, else null. */
   Runner replayView() {
      return this.replayFix != null && this.replayAt >= 0 && this.replaying() ? this.replayFix.runner() : null;
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
      this.coach = null;
      this.replayAt = -1;
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
         this.coach = Coach.of(this.runner, this.pads);
         String you = (best && score > 0 ? "New best: " : "You scored ") + score + ".";
         this.crashLines = new String[] { this.coach == null ? you : you + " " + this.coach.line, bot };
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
      if (this.pads) {
         this.paintPads(g2, this.darkness());
      }
   }

   /** How dark the sky is on screen: never while evolving or showing the whole run. */
   private double darkness() {
      return this.evolution != null || this.picture != null ? 0 : this.sky.darkness();
   }

   /**
    * The thumb pads under the game: DUCK on the left, JUMP on the right, filled while a finger holds one and, on your
    * first runs, while the mark under the ground is lit (with its TAP, HOLD or DUCK). In two players each is a player's jump.
    */
   private void paintPads(Graphics2D g, double dark) {
      Color paper = Sky.tint(PAPER, dark);
      Color ink = Sky.tint(INK, dark);
      Color muted = Sky.tint(MUTED, dark);
      g.setColor(Sky.tint(STRIP_BG, dark));
      g.fillRect(0, HEIGHT, Runner.WIDTH, PADS);
      if (this.evolution != null) {
         g.setFont(PAD_TEXT);
         g.setColor(muted);
         centered(g, "The bots are racing. Back to the game (E) is just above.", HEIGHT + PADS / 2);
         return;
      }
      Runner.State state = this.runner.state();
      int[] lit = this.guiding() && state == Runner.State.RUNNING ? this.guide() : null;
      int w = Runner.WIDTH / 2 - 18;
      int y = HEIGHT + 6;
      int h = PADS - 18;
      for (int need : new int[] { Ears.DUCK, Ears.JUMP }) {
         int x = need == Ears.DUCK ? 12 : Runner.WIDTH / 2 + 6;
         String word;
         String hint;
         boolean on;
         if (this.duel != null) {
            word = need == Ears.DUCK ? "P1 JUMP" : "P2 JUMP";
            hint = this.duel.running() ? "hold to jump higher" : "tap to start both";
            on = this.tapped == (need == Ears.DUCK ? this.duel.p1 : this.duel.p2);
         } else {
            boolean marked = lit != null && lit[1] == 1 && lit[0] == need;
            on = this.padHeld == need || marked;
            if (need == Ears.DUCK) {
               word = "DUCK";
               hint = "hold under the middle birds";
            } else {
               word = marked ? lit[2] == 1 ? "HOLD" : "TAP" : state == Runner.State.READY ? "RUN"
                  : state == Runner.State.CRASHED ? "RUN AGAIN" : "JUMP";
               hint = state == Runner.State.RUNNING ? "hold to jump higher" : "then tap to jump";
            }
         }
         g.setColor(on ? ink : paper);
         g.fillRoundRect(x, y, w, h, 28, 28);
         g.setColor(on ? paper : muted);
         g.setStroke(new BasicStroke(2f));
         g.drawRoundRect(x, y, w, h, 28, 28);
         g.setStroke(new BasicStroke(1f));
         int cx = x + w / 2;
         // An arrow: up for a jump, down for the duck.
         int ay = y + 40;
         int dir = need == Ears.DUCK && this.duel == null ? 1 : -1;
         g.setColor(on ? paper : ink);
         g.fillPolygon(new int[] { cx - 18, cx, cx + 18 }, new int[] { ay - dir * 10, ay + dir * 10, ay - dir * 10 }, 3);
         g.setFont(PAD);
         g.drawString(word, cx - g.getFontMetrics().stringWidth(word) / 2, y + 98);
         g.setFont(PAD_TEXT);
         g.setColor(on ? paper : muted);
         g.drawString(hint, cx - g.getFontMetrics().stringWidth(hint) / 2, y + 134);
      }
   }

   /** In the browser, when the system asks for reduced motion: the crash does not shake (null on the desktop JDK). */
   private static boolean reducedMotion() {
      return Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
   }

   private void paintScene(Graphics2D g, boolean blind) {
      double dark = this.darkness();
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
         this.paintBeside(g, d.p1, v, Sky.tint(DINO, dark), "P1", 0, dark);
         this.paintDuelScore(g, dark);
         this.paintMessage(g, dark);
      } else if (this.picture != null) {
         this.picture.paint(g);
      } else {
         Runner replay = this.replayView();
         paintWorld(g, replay != null ? replay : this.runner, this.sky, !blind);
         if (replay == null) {
            this.dust.paint(g, Sky.tint(DUST, dark), Sky.tint(PAPER, dark));
         }
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
         if (this.ghost != null && !this.bot.isOn() && replay == null) {
            this.paintGhost(g, dark);
         }
         if (this.coach != null && this.runner.state() == Runner.State.CRASHED && !this.bot.isOn()) {
            this.paintCoach(g, dark, replay);
         }
         if (this.guiding()) {
            this.paintGuide(g, dark);
         }
         if (replay == null) {
            paintDino(g, this.runner, Sky.tint(DINO, dark), Sky.tint(PAPER, dark));
         }
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
         String whole = this.picture != null ? "Back (T)" : this.canPicture() ? "Whole run (T)" : null;
         if (this.records.ghost == null && !this.racing) {
            labels = new String[] { this.sounds.isOn() ? "Sound on (M)" : "Sound off (M)", ear, "Two players (P)", "Evolve a bot (E)" };
            keys = new int[] { KeyEvent.VK_M, KeyEvent.VK_L, KeyEvent.VK_P, KeyEvent.VK_E };
         } else {
            help = this.bot.isOn() ? "B: bot off   Drag its red eye" : "Space or ↑: jump   ↓: duck";
            labels = new String[] { this.sounds.isOn() ? "Sound on (M)" : "Sound off (M)", ear,
               this.racing ? "New courses (G)" : "Race your ghost (G)", "Two players (P)", "Evolve a bot (E)" };
            keys = new int[] { KeyEvent.VK_M, KeyEvent.VK_L, KeyEvent.VK_G, KeyEvent.VK_P, KeyEvent.VK_E };
         }
         if (whole != null) {
            String[] l = new String[labels.length + 1];
            int[] k = new int[keys.length + 1];
            l[0] = whole;
            k[0] = KeyEvent.VK_T;
            System.arraycopy(labels, 0, l, 1, labels.length);
            System.arraycopy(keys, 0, k, 1, keys.length);
            labels = l;
            keys = k;
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
            || keys[i] == KeyEvent.VK_L && this.ears != Ears.Mode.OFF || keys[i] == KeyEvent.VK_T;
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
      // A low sun behind the mesas, fading into the sky as night falls.
      g.setColor(Sky.mix(SUN, paper, dark));
      g.fillOval(574, 92, 64, 64);
      Land.paint(g, runner.distance(), dark);
      g.setColor(Sky.tint(CLOUD, dark));
      double[] clouds = runner.clouds();
      for (int i = 0; i < clouds.length; i++) {
         Sprites.draw(g, Sprites.CLOUD, (int) clouds[i], 46 + i % 2 * 24, 2);
      }

      // The ground: sand under a line, with pebbles that scroll with the run.
      g.setColor(Sky.tint(SAND, dark));
      g.fillRect(0, Runner.GROUND, Runner.WIDTH, STRIP - Runner.GROUND);
      g.setColor(Sky.tint(INK, dark));
      g.fillRect(0, Runner.GROUND, Runner.WIDTH, 1);
      g.setColor(Sky.tint(MUTED, dark));
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
         paintObstacle(g, runner, o, (int) Math.round(o.x), dark, 255);
      }
   }

   /** Obstacle o with its left edge at x, in its own colors as night turns them, at alpha (255 is solid). */
   private static void paintObstacle(Graphics2D g, Runner runner, Runner.Obstacle o, int x, double dark, int alpha) {
      if (o.kind == Runner.Kind.BIRD) {
         g.setColor(see(Sky.tint(BIRD, dark), alpha));
         Sprites.draw(g, runner.birdWings(), x, o.top(), 2);
      } else {
         boolean large = o.kind == Runner.Kind.LARGE_CACTUS;
         Color body = see(Sky.tint(CACTUS, dark), alpha);
         Color rib = see(Sky.tint(CACTUS_LIGHT, dark), alpha);
         int unit = o.w / o.size;
         for (int j = 0; j < o.size; j++) {
            g.setColor(body);
            Sprites.cactus(g, x + j * unit, o.top(), large);
            g.setColor(rib);
            Sprites.ribs(g, x + j * unit, o.top(), large);
         }
      }
   }

   /** c at alpha (255 is c itself). */
   static Color see(Color c, int alpha) {
      return alpha >= 255 ? c : new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
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

   /**
    * After a crash: the obstacle see-through and a filled mark under the ground where it was when the press that clears
    * it should have come, and a hollow mark where it was when you pressed. In the slow replay (fix, the fix's run) the
    * real obstacle comes to the mark instead: it lights with TAP, HOLD or DUCK when the fix presses, the fix's dinosaur
    * runs solid and yours see-through until it crashes.
    */
   private void paintCoach(Graphics2D g, double dark, Runner fix) {
      Coach c = this.coach;
      g.setFont(TINY);
      FontMetrics fm = g.getFontMetrics();
      int y = Runner.GROUND + 3;
      Color ink = Sky.tint(INK, dark);
      Color dino = Sky.tint(DINO, dark);
      Color seeThrough = see(dino, 70);
      if (fix != null) {
         g.setColor(Sky.tint(MUTED, dark));
         g.drawString("SLOW REPLAY: HOW TO GET PAST IT", 14, 24 + (this.racing ? 16 : 0) + (this.ears != Ears.Mode.OFF ? 16 : 0));
      }
      if (c.pressX >= 0 && c.pressX < Runner.WIDTH && (fix == null || fix.steps() >= c.press)) {
         g.setColor(Sky.tint(MUTED, dark));
         g.drawPolygon(new int[] { c.pressX - 5, c.pressX, c.pressX + 5 }, new int[] { y + 8, y, y + 8 }, 3);
         String you = c.fix == Coach.Fix.STAY ? "you jumped" : "you";
         g.drawString(you, c.pressX - fm.stringWidth(you) / 2, y + 19);
      }
      if (c.fix != Coach.Fix.STAY) {
         boolean lit = fix == null || fix.steps() >= c.step;
         if (fix == null) {
            // The obstacle you hit, see-through, where it was when the press should have come.
            paintObstacle(g, this.runner, this.runner.crashedInto(), c.markX, dark, 70);
         }
         if (fix != null) {
            g.setColor(Sky.tint(SAND, dark));
            g.fillRect(c.markX - 7, y - 1, 15, 10);   // clear of the pebbles, as on your first runs
         }
         g.setColor(ink);
         int[] xs = { c.markX - 5, c.markX, c.markX + 5 };
         int[] ys = { y + 8, y, y + 8 };
         if (lit) {
            g.fillPolygon(xs, ys, 3);
         } else {
            g.drawPolygon(xs, ys, 3);
         }
         String here = fix != null && lit ? c.fix == Coach.Fix.DUCK ? "DUCK" : c.hold > GUIDE_TAP ? "HOLD" : "TAP"
            : c.fix == Coach.Fix.DUCK ? "duck here" : "jump here";
         // Beside your mark when they are close, so the two labels do not overlap.
         int lx = Math.abs(c.markX - c.pressX) < 40 && c.pressX >= 0
            ? c.markX < c.pressX ? c.markX - 4 - fm.stringWidth(here) : c.markX + 8 : c.markX - fm.stringWidth(here) / 2;
         g.drawString(here, lx, Math.abs(c.markX - c.pressX) < 40 && c.pressX >= 0 ? y + 8 : y + 19);
      }
      if (fix != null) {
         Runner you = this.replayYou.runner();
         boolean apart = you.state() == Runner.State.CRASHED || you.dinoTop() != fix.dinoTop() || you.isDucking() != fix.isDucking();
         this.paintBeside(g, you, fix, seeThrough, apart ? "you" : null, 0, dark);
         paintDino(g, fix, dino, Sky.tint(PAPER, dark));
         if (apart) {
            // Above the fix's dinosaur, lifted clear of yours when yours is just above it.
            boolean near = Math.abs(you.distance() - fix.distance()) < 60 && Math.abs(you.dinoTop() - fix.dinoTop()) < 14;
            g.setColor(ink);
            g.drawString("like this", fix.dinoX() + 2, near ? Math.min(fix.dinoTop(), you.dinoTop()) - 16 : fix.dinoTop() - 4);
         }
      }
   }

   /** Whether the mark shows: your own runs (not the bot's, not blindfolded) until your best reaches GUIDE_UNTIL. */
   boolean guiding() {
      return this.records.best < GUIDE_UNTIL && !this.bot.isOn() && this.ears != Ears.Mode.BLINDFOLD
         && this.runner.state() != Runner.State.CRASHED;
   }

   /**
    * What the mark says now: { Ears.JUMP or Ears.DUCK for the next cactus or bird to get past, 1 while lit else 0,
    * 1 when the key should be held rather than tapped }. It lights when the obstacle reaches the mark (Play by ear's beat
    * to act on) and stays lit as long as the key should be held, but at least GUIDE_LIT steps so a tap's moment is seen.
    */
   int[] guide() {
      Runner r = this.runner;
      for (Runner.Obstacle o : r.obstacles()) {
         double t = Ears.stepsToAct(r, o);
         int hold = Ears.holdSteps(r, o);
         if (Ears.need(o) != 0 && t > -Math.max(GUIDE_LIT, hold)) {
            return new int[] { Ears.need(o), t <= 0 ? 1 : 0, Ears.need(o) == Ears.DUCK || hold > GUIDE_TAP ? 1 : 0 };
         }
      }
      return new int[] { Ears.JUMP, 0, 0 };
   }

   /** The mark under the ground: hollow where the next obstacle should be when you act, filled with TAP, HOLD or DUCK while you should. */
   private void paintGuide(Graphics2D g, double dark) {
      int[] guide = this.guide();
      boolean now = guide[1] == 1;
      int x = guideX(this.runner, guide[0]);
      int y = Runner.GROUND + 3;
      int[] xs = { x - 5, x, x + 5 };
      int[] ys = { y + 8, y, y + 8 };
      g.setColor(Sky.tint(SAND, dark));
      g.fillRect(x - 7, y - 1, 15, 10);   // clear of the pebbles
      g.setFont(TINY);
      String label = guide[0] == Ears.DUCK ? "duck" : "jump";
      if (now) {
         g.setColor(Sky.tint(INK, dark));
         g.fillPolygon(xs, ys, 3);
         label = guide[0] == Ears.DUCK ? "DUCK" : guide[2] == 1 ? "HOLD" : "TAP";
      } else {
         g.setColor(Sky.tint(MUTED, dark));
         g.drawPolygon(xs, ys, 3);
      }
      g.drawString(label, x - g.getFontMetrics().stringWidth(label) / 2, y + 19);
   }

   static int guideX(Runner r, int need) {
      return (int) Math.round(Ears.markX(r, need));
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
      Color ink = Sky.tint(DINO, dark);
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
      g.setColor(Sky.tint(DINO, dark));
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
      String title = state != Runner.State.READY ? "G A M E   O V E R" : this.duel != null ? this.pads ? "Two players: tap your pad to run" : "Two players: Space, W or ↑ to run"
         : this.pads ? "Tap RUN to start" : "Space to run";
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
         : state == Runner.State.READY && this.guiding()
         ? new String[] { "Jump when a cactus reaches the mark under the ground: a quick tap, or hold on HOLD.",
            "Or press B and the original bot plays, watching one pixel." }
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

   Coach coach() {
      return this.coach;
   }

   String[] crashLines() {
      return this.crashLines;
   }

   Ghost ghost() {
      return this.ghost;
   }

   RunPicture picture() {
      return this.picture;
   }

   Evolution evolution() {
      return this.evolution;
   }

   Duel duel() {
      return this.duel;
   }
}
