package flappyBird;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class GamePanel extends JPanel implements KeyListener {
   private static final int READY = 0;
   private static final int PLAYING = 1;
   private static final int GAME_OVER = 2;
   /** Between a crash and the game over screen: the bird drops to the ground. */
   private static final int DYING = 3;
   /** After a crash: the last seconds play backwards. */
   private static final int REWINDING = 4;
   /** A rewind has ended: the world waits, frozen, for the next flap. */
   private static final int REWOUND = 5;
   /** How far a rewind goes back, and how many times faster than real time it plays. */
   private static final double REWIND_SECONDS = 2.5;
   private static final int REWIND_SPEED = 3;
   private static final File HIGH_SCORE_FILE = new File(System.getProperty("user.home", "."), ".flappybird-highscore.txt");
   private boolean debugMode = false;
   private boolean paused = false;
   private int state = READY;
   private boolean collided = false;
   private int consoleFontSize = 25;
   private float redPhase;
   private float greenPhase;
   private float bluePhase;
   private List<Image> images = new ArrayList<>();
   private Background background;
   private Pipes pipes;
   private Bird bird;
   private int highScore = loadHighScore();
   /** Today's course (same pipes all day, with a ghost of the day's best run) instead of endless random pipes. */
   private boolean daily = false;
   private DailyCourse course = DailyCourse.load(DailyCourse.FILE, DailyCourse.today());
   /** Game steps played this run; the ghost is drawn where it was after as many steps. */
   private int steps = 0;
   /** This run's ghost samples (see DailyCourse), kept if it becomes today's best. */
   private final List<Integer> samples = new ArrayList<>();
   /** The score this run has to beat for a notice: the best overall, or today's best on today's course. */
   private int target;
   private String notice = "";
   private int noticeSteps = 0;
   /** Game steps per second; the game logic moves a fixed amount per step. */
   private final double stepsPerSecond;
   private double pendingSteps = 0.0;
   private long lastTickNanos = System.nanoTime();
   private final Sounds sounds = new Sounds();
   /** The crash's white flash and screen shake, 1 at the crash fading to 0. */
   private float flash;
   private float shake;
   /** 1 when a pipe is passed, fading to 0: the score is drawn bigger for it. */
   private float scorePop;
   private int lastScore;
   /** Game steps since the title screen opened (the bird hovers) or the game over screen did (it slides in). */
   private int stateSteps;
   /** Puffs of air under each flap and feathers from a crash. */
   private final List<Puff> puffs = new ArrayList<>();
   private final Random puffRandom = new Random();
   /** The last seconds of flight, for rewinding a crash. */
   private final Rewind rewind;
   /** Rewinds used this run; a run with any is practice and keeps no records. */
   private int rewinds = 0;

   public GamePanel(int stepsPerSecond) {
      this.stepsPerSecond = stepsPerSecond;
      this.rewind = new Rewind((int)Math.ceil(stepsPerSecond * REWIND_SECONDS));
      this.images.add(new ImageIcon("bg.jpg").getImage().getScaledInstance(Main.FRAME_WIDTH, Main.FRAME_HEIGHT, 4));
      this.images.add(new ImageIcon("ground.jpg").getImage().getScaledInstance(Main.FRAME_WIDTH, Main.FRAME_HEIGHT / 8, 4));
      this.images.add(new ImageIcon("topPipe.png").getImage().getScaledInstance(Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, 4));
      this.images.add(new ImageIcon("bottomPipe.png").getImage().getScaledInstance(Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, 4));
      this.images.add(new ImageIcon("bird.png").getImage().getScaledInstance(Bird.WIDTH, Bird.HEIGHT, 4));
      this.reset();
      // Click or tap anywhere in the game to flap, as Space does.
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            if (event.getButton() != MouseEvent.BUTTON1) {
               return;
            }
            int[] button = GamePanel.this.rewindButton();
            int x = event.getX();
            int y = event.getY();
            if (GamePanel.this.state == GAME_OVER && GamePanel.this.settled()
               && x >= button[0] && x < button[0] + button[2] && y >= button[1] && y < button[1] + button[3]) {
               GamePanel.this.startRewind();
            } else {
               GamePanel.this.press();
            }
         }
      });
      new Timer(10, event -> this.tick()).start();
   }

   /** Space, a click or a tap: start, flap, or (once the game over screen has settled) back to the title screen. */
   private void press() {
      if (this.state == GAME_OVER) {
         if (this.settled()) {
            this.reset();
         }
      } else if (this.state != DYING && this.state != REWINDING && !this.paused) {
         this.state = PLAYING;
         this.bird.flap();
         this.sounds.flap();
         this.puff(5, new Color(255, 255, 255), Bird.HEIGHT / 10.0F);
      }
   }

   /** The game over screen has been up long enough (half a second) that a press is not a flap from the crash. */
   private boolean settled() {
      return this.stateSteps > this.stepsPerSecond / 2;
   }

   /** Z, Backspace or the game over screen's button: play the last seconds backwards, then wait to fly on from there. */
   private void startRewind() {
      boolean crashed = this.state == DYING || this.state == GAME_OVER && this.settled();
      if (!crashed || this.rewind.size() == 0) {
         return;
      }
      this.state = REWINDING;
      this.stateSteps = 0;
      this.rewinds++;
      this.target = 0;
      this.collided = false;
      this.flash = 0.0F;
      this.shake = 0.0F;
      this.noticeSteps = 0;
      this.puffs.clear();
      this.rewind.start();
      this.sounds.rewind();
   }

   /** Puts the game back at a moment from the rewind tape. */
   private void recall(Rewind.Moment moment) {
      this.bird.recall(moment.bird);
      this.pipes.recall(moment.pipes);
      this.background.recall(moment.background);
      this.steps = moment.steps;
      this.lastScore = this.score();
   }

   /** Runs as many fixed game steps as the elapsed time calls for, then repaints. */
   private void tick() {
      long now = System.nanoTime();
      this.pendingSteps += (now - this.lastTickNanos) / 1.0E9 * this.stepsPerSecond;
      this.lastTickNanos = now;
      // Drop the backlog after a stall (window drag, sleep) instead of fast-forwarding.
      this.pendingSteps = Math.min(this.pendingSteps, 10.0);
      while (this.pendingSteps >= 1.0) {
         this.pendingSteps--;
         this.step();
      }
      this.repaint();
   }

   private void step() {
      if (this.noticeSteps > 0) {
         this.noticeSteps--;
      }
      if (this.paused) {
         return;
      }

      float fade = 4.0F / (float)this.stepsPerSecond;
      this.flash = Math.max(0.0F, this.flash - fade);
      this.shake = Math.max(0.0F, this.shake - fade);
      this.scorePop = Math.max(0.0F, this.scorePop - fade);
      this.stepPuffs();
      this.stateSteps++;
      if (this.state == GAME_OVER || this.state == REWOUND) {
         return;
      }
      if (this.state == REWINDING) {
         for (int i = 0; i < REWIND_SPEED; i++) {
            Rewind.Moment moment = this.rewind.back();
            if (moment == null) {
               this.state = REWOUND;
               this.stateSteps = 0;
               break;
            }
            this.recall(moment);
         }
         return;
      }
      if (this.state == DYING) {
         this.bird.update();
         if (this.onGround()) {
            this.land();
         }
         return;
      }

      this.background.scroll();
      if (this.state == READY) {
         // Hover gently on the title screen, as the original does: a bob every 0.8 seconds.
         double bob = Math.sin(this.stateSteps * 2.0 * Math.PI / (this.stepsPerSecond * 0.8)) * Bird.HEIGHT / 5.0;
         this.bird.position.set(new Position(this.bird.position.collisionX, (float)(Main.FRAME_HEIGHT / 3 + bob)));
         return;
      }

      this.rewind.record(new Rewind.Moment(this.steps, this.bird.remember(), this.pipes.remember(), this.background.remember()));
      this.bird.update();
      this.pipes.scroll();
      this.steps++;
      if (this.daily && this.steps % DailyCourse.SAMPLE_STEPS == 0) {
         this.samples.add(this.sample());
      }
      if (this.score() > this.lastScore) {
         this.lastScore = this.score();
         this.scorePop = 1.0F;
         this.sounds.point();
      }
      if (this.target > 0 && this.score() > this.target) {
         this.target = 0;
         this.notice = this.daily ? "You passed your ghost!" : "New best!";
         this.noticeSteps = (int)this.stepsPerSecond * 2;
      }
      this.collided = this.hitsPipe() || this.onGround();
      if (this.collided && !this.debugMode) {
         this.crash();
      }
   }

   private boolean onGround() {
      return this.bird.position.collisionY + (float)Bird.HEIGHT > (float)Background.groundY;
   }

   /** The run is over: keep the records, flash, shake, and let the bird drop to the ground before the game over screen. */
   private void crash() {
      // A run with a rewind in it is practice: it keeps no best score and no ghost.
      if (this.rewinds == 0 && this.score() > this.highScore) {
         this.highScore = this.score();
         saveHighScore(this.highScore);
      }
      if (this.rewinds == 0 && this.daily && this.course.offer(this.score(), this.samples)) {
         this.course.save(DailyCourse.FILE);
      }
      this.flash = 1.0F;
      this.shake = 1.0F;
      this.sounds.hit();
      this.puff(12, new Color(250, 200, 40), Bird.HEIGHT / 6.0F);
      this.bird.stall();
      if (this.onGround()) {
         this.land();
      } else {
         this.state = DYING;
         this.sounds.fall();
      }
   }

   private void land() {
      this.bird.position.set(new Position(this.bird.position.collisionX, (float)(Background.groundY - Bird.HEIGHT)));
      this.state = GAME_OVER;
      this.stateSteps = 0;
   }

   /** Little bits flung out from the bird: air under a flap, feathers from a crash. */
   private void puff(int count, Color color, float speed) {
      float x = this.bird.position.collisionX + Bird.WIDTH / 3.0F;
      float y = this.bird.position.collisionY + Bird.HEIGHT * 0.7F;
      for (int i = 0; i < count; i++) {
         double angle = this.puffRandom.nextDouble() * Math.PI * 2.0;
         float pace = speed * (0.4F + this.puffRandom.nextFloat());
         float vx = (float)Math.cos(angle) * pace - Pipes.scrollSpeed;
         float vy = (float)Math.sin(angle) * pace + speed * 0.5F;
         this.puffs.add(new Puff(x, y, vx, vy, color, (int)(this.stepsPerSecond * (0.25 + this.puffRandom.nextDouble() * 0.25))));
      }
   }

   private void stepPuffs() {
      for (int i = this.puffs.size() - 1; i >= 0; i--) {
         Puff puff = this.puffs.get(i);
         puff.x += puff.vx;
         puff.y += puff.vy;
         puff.vx *= 0.97F;
         puff.vy *= 0.97F;
         if (--puff.life <= 0) {
            this.puffs.remove(i);
         }
      }
   }

   /** The bird's height in thousandths of the window height. */
   private int sample() {
      return Math.round(this.bird.position.collisionY * 1000.0F / Main.FRAME_HEIGHT);
   }

   private int score() {
      return this.pipes.passed;
   }

   private boolean hitsPipe() {
      for (int pipeIndex = 0; pipeIndex < this.pipes.positions.size(); pipeIndex++) {
         Position pipe = this.pipes.positions.get(pipeIndex);
         int pipeLeft = (int)pipe.collisionX;
         int pipeRight = pipeLeft + Pipes.PIPE_WIDTH;
         int birdLeft = (int)this.bird.position.collisionX;
         int birdRight = birdLeft + Bird.WIDTH;
         boolean overlapsX = pipeLeft < birdRight && birdRight < pipeRight || pipeLeft < birdLeft && birdLeft < pipeRight;
         if (pipeIndex % 2 == 0) {
            int pipeBottom = (int)pipe.collisionY + Pipes.PIPE_HEIGHT;
            int birdTop = (int)this.bird.position.collisionY;
            if (overlapsX && pipeBottom > birdTop) {
               return true;
            }
         } else {
            int pipeTop = (int)pipe.collisionY;
            int birdBottom = (int)this.bird.position.collisionY + Bird.HEIGHT;
            if (overlapsX && pipeTop < birdBottom) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public final void paint(Graphics graphics) {
      Graphics2D g2 = (Graphics2D)graphics;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setColor(
         new Color(
            (int)(127.0 * (Math.cos((double)(this.redPhase += 0.02F)) + 1.0)),
            (int)(127.0 * (Math.cos((double)(this.greenPhase += 0.02F)) + 1.0)),
            (int)(127.0 * (Math.sin((double)(this.bluePhase += 0.02F)) + 1.0))
         )
      );
      g2.setStroke(new BasicStroke((float)(Main.FRAME_WIDTH / 250)));
      int shakeX = 0;
      int shakeY = 0;
      if (this.shake > 0.0F) {
         int reach = Math.max(2, Main.FRAME_HEIGHT / 40);
         shakeX = Math.round((this.puffRandom.nextFloat() * 2.0F - 1.0F) * reach * this.shake);
         shakeY = Math.round((this.puffRandom.nextFloat() * 2.0F - 1.0F) * reach * this.shake);
      }
      g2.translate(shakeX, shakeY);

      for (Position tile : this.background.layers.get(0)) {
         g2.drawImage(this.background.backgroundImage, (int)tile.x, (int)tile.y, null);
         if (this.debugMode) {
            g2.drawRect((int)tile.x, (int)tile.y, this.background.backgroundImage.getWidth(null), this.background.backgroundImage.getHeight(null));
         }
      }

      for (int pipeIndex = 0; pipeIndex < this.pipes.positions.size(); pipeIndex++) {
         Position pipe = this.pipes.positions.get(pipeIndex);
         Image image = pipeIndex % 2 == 0 ? this.pipes.topPipeImage : this.pipes.bottomPipeImage;
         g2.drawImage(image, (int)pipe.x, (int)pipe.y, null);
         if (this.debugMode) {
            g2.drawRect((int)pipe.x, (int)pipe.y, image.getWidth(null), image.getHeight(null));
         }
      }

      for (Position tile : this.background.layers.get(1)) {
         g2.drawImage(this.background.groundImage, (int)tile.x, (int)tile.y, null);
         if (this.debugMode) {
            g2.drawRect(0, Main.FRAME_HEIGHT * 7 / 8, Main.FRAME_WIDTH, Main.FRAME_HEIGHT / 8);
         }
      }

      boolean rewound = this.state == REWINDING || this.state == REWOUND;
      if (this.daily && (this.state == READY || this.state == PLAYING || rewound) && this.course.hasGhost()) {
         float ghostY = this.course.ghostY(this.steps);
         if (ghostY >= 0.0F) {
            this.drawGhost(g2, (int)this.bird.position.collisionX, (int)(ghostY * Main.FRAME_HEIGHT));
         }
      }

      if (this.state == PLAYING || rewound) {
         this.drawLastFlight(g2);
      }
      this.drawPuffs(g2);
      this.drawBird(g2);
      if (this.debugMode) {
         g2.drawRect((int)this.bird.position.collisionX, (int)this.bird.position.collisionY, this.bird.image.getWidth(null), this.bird.image.getHeight(null));
      }
      g2.translate(-shakeX, -shakeY);

      if (this.collided && this.debugMode) {
         g2.setColor(new Color(255, 0, 0));
         g2.setFont(new Font("Times New Roman", 1, Main.FRAME_HEIGHT / 10));
         g2.drawString("COLISION", Main.FRAME_WIDTH / 2, (Main.FRAME_HEIGHT << 1) / 10);
      }

      // The score shows in flight; the title and game over screens show it in their banners instead.
      boolean flying = this.state == PLAYING || this.state == DYING || rewound;
      if (flying) {
         this.drawScore(g2);
         if (this.rewinds > 0) {
            this.drawCaption(g2, "Practice: " + rewindCount(this.rewinds), Main.FRAME_HEIGHT * 5 / 24, new Color(200, 230, 255));
         }
      }
      if (this.state == REWINDING) {
         this.drawRewinding(g2);
      } else if (this.state == REWOUND) {
         this.drawCaption(g2, "Rewound. Space, click or tap to fly on", Main.FRAME_HEIGHT * 3 / 10, new Color(255, 255, 255));
      }

      if (this.noticeSteps > 0) {
         int noticeX = (Main.FRAME_WIDTH - fitFont(g2, this.notice, Main.FRAME_HEIGHT / 18, Main.FRAME_WIDTH * 19 / 20)) / 2;
         int noticeY = flying ? Main.FRAME_HEIGHT / 4 : Main.FRAME_HEIGHT / 8;
         g2.setColor(new Color(0, 0, 0, 140));
         g2.drawString(this.notice, noticeX + 2, noticeY + 2);
         g2.setColor(new Color(255, 220, 60));
         g2.drawString(this.notice, noticeX, noticeY);
      }

      String records = "Best " + this.highScore + (this.daily || this.course.bestScore() > 0 ? "     Today " + this.course.bestScore() : "");
      if (this.state == READY) {
         if (this.daily) {
            this.drawBanner(g2, "Today's Course", "The same pipes all day. Race your best run's ghost.", "Space, click or tap to flap; C for endless pipes", records);
         } else {
            this.drawBanner(g2, "Flappy Bird", "Endless: new pipes every run", "Space, click or tap to flap; C for today's course", records);
         }
      } else if (this.state == GAME_OVER) {
         String scores = this.rewinds > 0
            ? "Score " + this.score() + " with " + rewindCount(this.rewinds) + " (practice, not a record)"
            : "Score " + this.score() + "     " + records;
         // The game over screen rises into place over its first third of a second.
         float rise = Math.max(0.0F, 1.0F - this.stateSteps / (float)(this.stepsPerSecond / 3.0));
         int lift = Math.round(rise * rise * Main.FRAME_HEIGHT / 6.0F);
         g2.translate(0, lift);
         int titleWidth = this.drawBanner(g2, "Game Over", scores, medalLine(this.score()), "Space, click or tap for the title screen");
         Color medal = medalColor(this.score());
         if (medal != null) {
            this.drawMedal(g2, medal, titleWidth);
         }
         if (this.rewind.size() > 0) {
            this.drawRewindButton(g2);
         }
         g2.translate(0, -lift);
      } else if (this.paused) {
         this.drawBanner(g2, "Paused", "Press P to resume");
      }

      if (this.flash > 0.0F) {
         g2.setColor(new Color(255, 255, 255, Math.round(200 * this.flash)));
         g2.fillRect(0, 0, Main.FRAME_WIDTH, Main.FRAME_HEIGHT);
      }

      if (this.debugMode) {
         g2.setColor(new Color(0, 0, 0));
         g2.setFont(new Font("Times New Roman", 1, this.consoleFontSize));
         Position nextPipe = this.pipes.positions.get(0);
         String[] debugLines = new String[]{
            "bird x: " + this.bird.position.collisionX,
            "bird y: " + this.bird.position.collisionY,
            "next pipe x: " + nextPipe.collisionX,
            "next pipe y: " + nextPipe.collisionY,
            "paused: " + this.paused,
            "colision: " + this.collided,
            "consoleFontSize: " + this.consoleFontSize,
            "rendered pipes: " + this.pipes.positions.size(),
            ""
         };

         for (int line = 0; line < 9; line++) {
            g2.drawString(debugLines[line], Main.FRAME_WIDTH - 11 * this.consoleFontSize, Main.FRAME_HEIGHT - this.consoleFontSize * 10 + this.consoleFontSize * line);
         }
      }
   }

   static String rewindCount(int rewinds) {
      return rewinds + (rewinds == 1 ? " rewind" : " rewinds");
   }

   /** One line of text centered at height y on a dark strip, so it reads over the sky and the pipes. */
   private void drawCaption(Graphics2D g2, String text, int y, Color color) {
      int size = Main.FRAME_HEIGHT / 24;
      int width = fitFont(g2, text, size, Main.FRAME_WIDTH * 9 / 10);
      int x = (Main.FRAME_WIDTH - width) / 2;
      int height = g2.getFont().getSize();
      g2.setColor(new Color(0, 0, 0, 110));
      g2.fillRoundRect(x - height / 2, y - height, width + height, height * 3 / 2, height, height);
      g2.setColor(color);
      g2.drawString(text, x, y);
   }

   /** The rewind as an old video tape winding back: a blue tint, scan lines, a rolling band and a rewind sign. */
   private void drawRewinding(Graphics2D g2) {
      g2.setColor(new Color(40, 80, 200, 50));
      g2.fillRect(0, 0, Main.FRAME_WIDTH, Main.FRAME_HEIGHT);
      int gap = Math.max(4, Main.FRAME_HEIGHT / 120);
      g2.setColor(new Color(0, 0, 40, 35));
      for (int y = 0; y < Main.FRAME_HEIGHT; y += gap) {
         g2.fillRect(0, y, Main.FRAME_WIDTH, gap / 2);
      }
      int band = Main.FRAME_HEIGHT / 14;
      int bandY = (int)((long)this.stateSteps * Main.FRAME_HEIGHT / Math.max(1L, (long)(this.stepsPerSecond / 2.0)) % Main.FRAME_HEIGHT);
      g2.setColor(new Color(255, 255, 255, 40));
      g2.fillRect(0, bandY, Main.FRAME_WIDTH, band);
      int size = Main.FRAME_HEIGHT / 16;
      int x = Main.FRAME_WIDTH / 30;
      int y = Main.FRAME_HEIGHT / 12;
      // Blink the sign about three times a second.
      if (this.stateSteps * 6 / Math.max(1, (int)this.stepsPerSecond) % 2 == 0) {
         g2.setColor(new Color(255, 255, 255, 230));
         for (int i = 0; i < 2; i++) {
            int left = x + i * size * 3 / 4;
            g2.fillPolygon(new int[]{left, left + size * 3 / 4, left + size * 3 / 4}, new int[]{y + size / 2, y, y + size}, 3);
         }
         g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size * 3 / 4));
         g2.drawString("REWIND", x + size * 2, y + size * 4 / 5);
      }
   }

   /** The game over screen's rewind button as {x, y, width, height}, under the banner. */
   private int[] rewindButton() {
      int small = Main.FRAME_HEIGHT / 20;
      // Wide enough for its label on a narrow (portrait) browser page too.
      int width = Math.min(Main.FRAME_WIDTH * 9 / 10, Math.max(Main.FRAME_WIDTH * 2 / 5, Main.FRAME_HEIGHT * 3 / 4));
      return new int[]{(Main.FRAME_WIDTH - width) / 2, Main.FRAME_HEIGHT * 2 / 5 + small * 11 / 2, width, small * 2};
   }

   private void drawRewindButton(Graphics2D g2) {
      int[] button = this.rewindButton();
      int arc = button[3] / 2;
      g2.setColor(new Color(0, 0, 0, 90));
      g2.fillRoundRect(button[0] + 3, button[1] + 4, button[2], button[3], arc, arc);
      g2.setColor(new Color(60, 120, 220));
      g2.fillRoundRect(button[0], button[1], button[2], button[3], arc, arc);
      g2.setColor(new Color(255, 255, 255));
      g2.drawRoundRect(button[0], button[1], button[2], button[3], arc, arc);
      String label = "Rewind and fly on (Z)";
      int icon = button[3] / 2;
      int width = fitFont(g2, label, button[3] / 2, button[2] - icon * 3);
      int left = button[0] + (button[2] - width - icon * 2) / 2;
      int top = button[1] + (button[3] - icon) / 2;
      for (int i = 0; i < 2; i++) {
         int tip = left + i * icon * 3 / 5;
         g2.fillPolygon(new int[]{tip, tip + icon * 3 / 5, tip + icon * 3 / 5}, new int[]{top + icon / 2, top, top + icon}, 3);
      }
      g2.drawString(label, left + icon * 2, button[1] + button[3] * 2 / 3);
   }

   /**
    * The path the bird flew into its last crash, as dots that scroll with the pipes, and a red cross where it crashed,
    * so the next try can fly around it.
    */
   private void drawLastFlight(Graphics2D g2) {
      List<float[]> path = this.rewind.path();
      if (path.isEmpty()) {
         return;
      }
      float birdX = this.bird.position.collisionX + Bird.WIDTH / 2.0F;
      int dot = Math.max(3, Bird.HEIGHT / 8);
      g2.setColor(new Color(255, 255, 255, 170));
      for (int i = 0; i < path.size(); i += 8) {
         float[] point = path.get(i);
         int x = Math.round(birdX + (point[0] - this.steps) * Pipes.scrollSpeed);
         if (x > -dot && x < Main.FRAME_WIDTH + dot) {
            g2.fillOval(x - dot / 2, Math.round(point[1] + Bird.HEIGHT / 2.0F) - dot / 2, dot, dot);
         }
      }
      float[] crash = path.get(0);
      int x = Math.round(birdX + (crash[0] - this.steps) * Pipes.scrollSpeed);
      int y = Math.round(crash[1] + Bird.HEIGHT / 2.0F);
      int arm = Bird.HEIGHT / 3;
      Graphics2D g = (Graphics2D)g2.create();
      g.setStroke(new BasicStroke(Math.max(3.0F, Bird.HEIGHT / 10.0F), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.setColor(new Color(0, 0, 0, 120));
      g.drawLine(x - arm + 2, y - arm + 2, x + arm + 2, y + arm + 2);
      g.drawLine(x - arm + 2, y + arm + 2, x + arm + 2, y - arm + 2);
      g.setColor(new Color(230, 40, 40));
      g.drawLine(x - arm, y - arm, x + arm, y + arm);
      g.drawLine(x - arm, y + arm, x + arm, y - arm);
      g.dispose();
   }

   /** The bird, tilted with its flight and stretched for a moment by each flap. */
   private void drawBird(Graphics2D g2) {
      Graphics2D g = (Graphics2D)g2.create();
      g.translate(this.bird.position.collisionX + Bird.WIDTH / 2.0, this.bird.position.collisionY + Bird.HEIGHT / 2.0);
      g.rotate(Math.toRadians(this.bird.rotation()));
      float squash = this.bird.squash();
      g.scale(1.0 + 0.15 * squash, 1.0 - 0.2 * squash);
      g.drawImage(this.bird.image, -Bird.WIDTH / 2, -Bird.HEIGHT / 2, null);
      g.dispose();
   }

   private void drawPuffs(Graphics2D g2) {
      for (Puff puff : this.puffs) {
         int size = Math.max(2, Bird.HEIGHT / 5 * puff.life / puff.lifetime + 1);
         Color c = puff.color;
         g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 230 * puff.life / puff.lifetime));
         g2.fillOval(Math.round(puff.x) - size / 2, Math.round(puff.y) - size / 2, size, size);
      }
   }

   /** The score in big white digits outlined in black, centered at the top, popping bigger as each pipe is passed. */
   private void drawScore(Graphics2D g2) {
      String text = String.valueOf(this.score());
      int size = Math.round(Main.FRAME_HEIGHT / 9.0F * (1.0F + 0.3F * this.scorePop));
      g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
      int x = (Main.FRAME_WIDTH - g2.getFontMetrics().stringWidth(text)) / 2;
      int y = Main.FRAME_HEIGHT / 6 + Math.round((size - Main.FRAME_HEIGHT / 9.0F) / 3.0F);
      int edge = Math.max(2, size / 16);
      g2.setColor(new Color(0, 0, 0, 90));
      g2.drawString(text, x + edge, y + edge * 2);
      g2.setColor(new Color(40, 30, 20));
      for (int dx = -edge; dx <= edge; dx += edge) {
         for (int dy = -edge; dy <= edge; dy += edge) {
            if (dx != 0 || dy != 0) {
               g2.drawString(text, x + dx, y + dy);
            }
         }
      }
      g2.setColor(new Color(255, 255, 255));
      g2.drawString(text, x, y);
   }

   /** A title and lines of text centered on the screen. Returns the title's width. */
   private int drawBanner(Graphics2D g2, String title, String... lines) {
      int big = Main.FRAME_HEIGHT / 8;
      int small = Main.FRAME_HEIGHT / 20;
      int centerY = Main.FRAME_HEIGHT * 2 / 5;
      g2.setColor(new Color(0, 0, 0, 120));
      g2.fillRect(0, centerY - big - small, Main.FRAME_WIDTH, big + small * (lines.length * 3 / 2 + 2));
      g2.setColor(new Color(255, 255, 255));
      // The title keeps room beside it for the game over medal; the lines may use nearly the whole width.
      int titleWidth = fitFont(g2, title, big, Main.FRAME_WIDTH * 3 / 5);
      g2.drawString(title, (Main.FRAME_WIDTH - titleWidth) / 2, centerY);
      for (int line = 0; line < lines.length; line++) {
         int width = fitFont(g2, lines[line], small, Main.FRAME_WIDTH * 19 / 20);
         g2.drawString(lines[line], (Main.FRAME_WIDTH - width) / 2, centerY + small * 3 / 2 * (line + 1));
      }
      return titleWidth;
   }

   /** Sets a bold font of the given size, shrunk if the text is wider than maxWidth (a narrow browser page). Returns the text's width. */
   private static int fitFont(Graphics2D g2, String text, int size, int maxWidth) {
      g2.setFont(new Font("Times New Roman", 1, size));
      int width = g2.getFontMetrics().stringWidth(text);
      if (width > maxWidth) {
         g2.setFont(new Font("Times New Roman", 1, Math.max(8, size * maxWidth / width)));
         width = g2.getFontMetrics().stringWidth(text);
      }
      return width;
   }

   /** A medal beside the game over title. */
   private void drawMedal(Graphics2D g2, Color color, int titleWidth) {
      int size = Main.FRAME_HEIGHT / 10;
      int x = Math.max(4, (Main.FRAME_WIDTH - titleWidth) / 2 - size * 3 / 2);
      int y = Main.FRAME_HEIGHT * 2 / 5 - size;
      g2.setColor(new Color(0, 0, 0, 90));
      g2.fillOval(x + 3, y + 3, size, size);
      g2.setColor(color);
      g2.fillOval(x, y, size, size);
      g2.setColor(color.darker());
      g2.fillOval(x + size / 5, y + size / 5, size * 3 / 5, size * 3 / 5);
      g2.setColor(new Color(255, 255, 255, 170));
      g2.fillOval(x + size / 4, y + size / 4, size / 5, size / 5);
   }

   /** Medals at 10, 20, 30 and 40 pipes, as in the original Flappy Bird. Null below 10. */
   static String medalName(int score) {
      return score >= 40 ? "Platinum" : score >= 30 ? "Gold" : score >= 20 ? "Silver" : score >= 10 ? "Bronze" : null;
   }

   private static Color medalColor(int score) {
      String name = medalName(score);
      if (name == null) {
         return null;
      }
      switch (name) {
         case "Platinum":
            return new Color(214, 228, 236);
         case "Gold":
            return new Color(240, 190, 40);
         case "Silver":
            return new Color(190, 194, 204);
         default:
            return new Color(200, 120, 50);
      }
   }

   /** The medal won, and how far the next one is. */
   static String medalLine(int score) {
      if (score >= 40) {
         return "Platinum medal!";
      }
      String name = medalName(score);
      int next = (score / 10 + 1) * 10;
      int more = next - score;
      String goal = more + (more == 1 ? " more pipe for " : " more pipes for ") + medalName(next);
      return name == null ? "No medal yet: " + goal : name + " medal: " + goal;
   }

   /** Today's best run as a see-through bird with its score above it. */
   private void drawGhost(Graphics2D g2, int x, int y) {
      int size = Bird.WIDTH;
      g2.setColor(new Color(255, 255, 255, 100));
      g2.fillOval(x, y, size, size);
      g2.setColor(new Color(255, 255, 255, 200));
      g2.drawOval(x, y, size, size);
      g2.setColor(new Color(40, 40, 70, 170));
      g2.fillOval(x + size * 3 / 5, y + size / 4, size / 6 + 1, size / 6 + 1);
      g2.setColor(new Color(255, 160, 40, 180));
      g2.fillPolygon(new int[]{x + size - 2, x + size + size / 4, x + size - 2}, new int[]{y + size * 2 / 5, y + size / 2, y + size * 3 / 5}, 3);
      String label = "Today's best " + this.course.bestScore();
      g2.setFont(new Font("Times New Roman", 1, Math.max(10, Main.FRAME_HEIGHT / 40)));
      g2.setColor(new Color(255, 255, 255, 220));
      g2.drawString(label, x + (size - g2.getFontMetrics().stringWidth(label)) / 2, y - 4);
   }

   private void reset() {
      if (this.course.date != DailyCourse.today()) {
         this.course = DailyCourse.load(DailyCourse.FILE, DailyCourse.today());
      }
      this.background = new Background(this.images.get(0), this.images.get(1));
      this.pipes = new Pipes(this.images.get(2), this.images.get(3), this.daily ? this.course.pipeRandom() : new Random());
      this.bird = new Bird(this.images.get(4));
      this.bird.position.set(new Position(this.bird.position.collisionX, (float)(Main.FRAME_HEIGHT / 3)));
      this.collided = false;
      this.paused = false;
      this.state = READY;
      this.stateSteps = 0;
      this.lastScore = 0;
      this.scorePop = 0.0F;
      this.puffs.clear();
      this.steps = 0;
      this.rewind.clear();
      this.rewinds = 0;
      this.samples.clear();
      this.samples.add(this.sample());
      this.target = this.daily ? this.course.bestScore() : this.highScore;
      this.noticeSteps = 0;
   }

   private static int loadHighScore() {
      try (BufferedReader reader = new BufferedReader(new FileReader(HIGH_SCORE_FILE))) {
         return Integer.parseInt(reader.readLine().trim());
      } catch (IOException | RuntimeException e) {
         return 0;
      }
   }

   private static void saveHighScore(int score) {
      try (FileWriter writer = new FileWriter(HIGH_SCORE_FILE)) {
         writer.write(String.valueOf(score));
      } catch (IOException e) {
         System.err.println("Could not save the high score: " + e.getMessage());
      }
   }

   @Override
   public final void keyTyped(KeyEvent event) {
   }

   @Override
   public final void keyPressed(KeyEvent event) {
      switch (event.getKeyCode()) {
         case KeyEvent.VK_SPACE:
            this.press();
            return;
         case KeyEvent.VK_Z:
         case KeyEvent.VK_BACK_SPACE:
            this.startRewind();
            return;
         case KeyEvent.VK_M:
            this.notice = this.sounds.toggle() ? "Sound on" : "Sound off";
            this.noticeSteps = (int)this.stepsPerSecond;
            return;
         case KeyEvent.VK_UP:
            this.consoleFontSize += 5;
            return;
         case KeyEvent.VK_DOWN:
            this.consoleFontSize = Math.max(5, this.consoleFontSize - 5);
            return;
         case KeyEvent.VK_C:
            if (this.state == READY) {
               this.daily = !this.daily;
               this.reset();
            }
            return;
         case KeyEvent.VK_D:
            this.debugMode = !this.debugMode;
            return;
         case KeyEvent.VK_P:
            if (this.state == PLAYING) {
               this.paused = !this.paused;
            }
            return;
         case KeyEvent.VK_R:
            this.reset();
            return;
         case KeyEvent.VK_T:
            Image firstImage = this.images.get(0);

            for (int i = 0; i < this.images.size() - 1; i++) {
               this.images.set(i, this.images.get(i + 1).getScaledInstance(this.images.get(i).getWidth(null), this.images.get(i).getHeight(null), 4));
            }

            int last = this.images.size() - 1;
            this.images.set(last, firstImage.getScaledInstance(this.images.get(last).getWidth(null), this.images.get(last).getHeight(null), 4));
            System.out.println("test");
      }
   }

   @Override
   public final void keyReleased(KeyEvent event) {
   }

   /** One bit flung from the bird. */
   private static final class Puff {
      float x;
      float y;
      float vx;
      float vy;
      final Color color;
      final int lifetime;
      int life;

      Puff(float x, float y, float vx, float vy, Color color, int lifetime) {
         this.x = x;
         this.y = y;
         this.vx = vx;
         this.vy = vy;
         this.color = color;
         this.lifetime = Math.max(1, lifetime);
         this.life = this.lifetime;
      }
   }
}
