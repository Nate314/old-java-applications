package flappyBird;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
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
   /** Where the bird's body met a pipe in the last crash, marked until the next run or rewind; null for the ground. */
   private float[] contact;
   /** The flight guide ({@link Guide}): on by itself until the best score reaches Guide.UNTIL, unless G chose. */
   private boolean guide;
   private boolean guideChosen = false;
   /** This run flies with the guide that G brought back after the best passed Guide.UNTIL: practice, no records. */
   private boolean guidedPractice = false;
   /** The time of day ({@link Sky}), easing toward the score's. */
   private float skyPhase;
   /** Whether the run just ended set a new best score, or today's best run, for the scoreboard's NEW tags. */
   private boolean newBest;
   private boolean newToday;

   /**
    * A picture from the classpath (a jar of the app), or else from the folder the game is started in (as in the
    * browser, where there is no classpath). ImageIcon waits for it to load either way.
    */
   private static Image image(String name) {
      java.net.URL url = GamePanel.class.getResource("/" + name);
      return (url != null ? new ImageIcon(Toolkit.getDefaultToolkit().getImage(url)) : new ImageIcon(name)).getImage();
   }

   public GamePanel(int stepsPerSecond) {
      this.stepsPerSecond = stepsPerSecond;
      this.rewind = new Rewind((int)Math.ceil(stepsPerSecond * REWIND_SECONDS));
      this.images.add(image("ground.jpg").getScaledInstance(Main.FRAME_WIDTH, Main.FRAME_HEIGHT / 8, 4));
      this.images.add(image("topPipe.png").getScaledInstance(Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, 4));
      this.images.add(image("bottomPipe.png").getScaledInstance(Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, 4));
      this.images.add(image("bird.png").getScaledInstance(Bird.WIDTH, Bird.HEIGHT, 4));
      this.reset();
      // Click or tap anywhere in the game to flap, as Space does.
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            if (event.getButton() != MouseEvent.BUTTON1) {
               return;
            }
            int x = event.getX();
            int y = event.getY();
            int over = GamePanel.this.state == GAME_OVER && GamePanel.this.settled() ? overHit(GamePanel.this.overButtons(), x, y) : -1;
            if (over == REWIND_BUTTON) {
               GamePanel.this.startRewind();
            } else if (over == TITLE_BUTTON) {
               GamePanel.this.reset();
            } else if (GamePanel.this.state == READY && GamePanel.this.titleCard().hit(x, y) != TitleCard.NONE) {
               GamePanel.this.choose(GamePanel.this.titleCard().hit(x, y));
            } else {
               GamePanel.this.press();
            }
         }
      });
      new Timer(10, event -> this.tick()).start();
   }

   /** Space, a click or a tap: start, flap, or (once the game over screen has settled) fly again on the same course. */
   private void press() {
      if (this.state == GAME_OVER) {
         if (!this.settled()) {
            return;
         }
         this.reset();   // then straight into the new run with this press's flap, no title screen between
      }
      if (this.state != DYING && this.state != REWINDING && !this.paused) {
         if (this.state == READY) {
            this.guidedPractice = this.guide && this.highScore >= Guide.UNTIL;
         }
         this.state = PLAYING;
         this.bird.flap();
         this.sounds.flap();
         this.puff(5, new Color(255, 255, 255), Bird.HEIGHT / 10.0F);
      }
   }

   /** A click or tap on the title card: a course tile picks that course (the chosen one starts it), the switch turns the guide. */
   private void choose(int part) {
      if (part == TitleCard.GUIDE) {
         this.toggleGuide();
      } else if (part == TitleCard.TODAY == this.daily) {
         this.press();
      } else {
         this.switchCourse();
      }
   }

   /** C on the title screen, or its other course tile: endless pipes or today's course. */
   private void switchCourse() {
      this.daily = !this.daily;
      this.reset();
   }

   /** G, or the title screen's switch: the flight guide on or off. */
   private void toggleGuide() {
      this.guide = !this.guide;
      this.guideChosen = true;
      if (this.state != READY) {
         // Brought back in flight once the best has passed Guide.UNTIL, it makes this run practice, as at the start.
         this.guidedPractice |= this.guide && this.highScore >= Guide.UNTIL;
         this.notice = this.guide ? "Flight guide on" : "Flight guide off";
         this.noticeSteps = (int)this.stepsPerSecond;
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
      this.contact = null;
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
      // The sky turns a tenth of a phase per pipe, over about half a second (and back again in a rewind).
      float skyPace = 0.2F / (float)this.stepsPerSecond;
      this.skyPhase += Math.max(-skyPace, Math.min(skyPace, this.score() / (float)Sky.PIPES_PER_PHASE - this.skyPhase));
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
      this.contact = null;
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
      // A run with a rewind in it, or the guide brought back, is practice: it keeps no best score and no ghost.
      this.newBest = this.practiceNote() == null && this.score() > this.highScore;
      if (this.newBest) {
         this.highScore = this.score();
         saveHighScore(this.highScore);
      }
      this.newToday = this.practiceNote() == null && this.daily && this.course.offer(this.score(), this.samples);
      if (this.newToday) {
         this.course.save(DailyCourse.FILE);
      }
      // With reduced motion the crash keeps its thud and feathers but neither flashes nor shakes.
      this.flash = calm() ? 0.0F : 1.0F;
      this.shake = this.flash;
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

   /** Whether the bird's body touches a pipe; if so, where is kept in {@link #contact} for the crash mark. */
   private boolean hitsPipe() {
      float[] body = this.body();
      for (int pipeIndex = 0; pipeIndex < this.pipes.positions.size(); pipeIndex++) {
         float[] touch = Hitbox.touch(body, this.pipeShape(pipeIndex));
         if (touch != null) {
            this.contact = touch;
            return true;
         }
      }
      return false;
   }

   private float[] body() {
      return Hitbox.body(this.bird.position.collisionX, this.bird.position.collisionY, Bird.WIDTH, this.bird.rotation());
   }

   private float[][] pipeShape(int pipeIndex) {
      Position pipe = this.pipes.positions.get(pipeIndex);
      return Hitbox.pipe(pipe.collisionX, pipe.collisionY, Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, pipeIndex % 2 == 0);
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
      Color debugColor = g2.getColor();   // the puffs and the last flight change the color before the body's circle
      int shakeX = 0;
      int shakeY = 0;
      if (this.shake > 0.0F) {
         int reach = Math.max(2, Main.FRAME_HEIGHT / 40);
         shakeX = Math.round((this.puffRandom.nextFloat() * 2.0F - 1.0F) * reach * this.shake);
         shakeY = Math.round((this.puffRandom.nextFloat() * 2.0F - 1.0F) * reach * this.shake);
      }
      g2.translate(shakeX, shakeY);

      this.background.paintBack(g2);
      Sky.paintSky(g2, this.skyPhase, Main.FRAME_WIDTH, Main.FRAME_HEIGHT, Background.groundY, calm());

      for (int pipeIndex = 0; pipeIndex < this.pipes.positions.size(); pipeIndex++) {
         Position pipe = this.pipes.positions.get(pipeIndex);
         Image image = pipeIndex % 2 == 0 ? this.pipes.topPipeImage : this.pipes.bottomPipeImage;
         g2.drawImage(image, (int)pipe.x, (int)pipe.y, null);
         if (this.debugMode) {
            for (float[] r : this.pipeShape(pipeIndex)) {
               g2.drawRect(Math.round(r[0]), Math.round(r[1]), Math.round(r[2]), Math.round(r[3]));
            }
         }
      }

      this.background.paintGround(g2);
      if (this.debugMode) {
         g2.drawRect(0, Background.groundY, Main.FRAME_WIDTH, Main.FRAME_HEIGHT / 8);
      }
      Sky.paintDim(g2, this.skyPhase, Main.FRAME_WIDTH, Main.FRAME_HEIGHT);

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
      if (this.guide && (this.state == PLAYING || this.state == REWOUND)) {
         this.drawGuide(g2);
      }
      this.drawPuffs(g2);
      this.drawBird(g2);
      if (this.debugMode) {
         float[] body = this.body();
         g2.setColor(debugColor);
         g2.drawOval(Math.round(body[0] - body[2]), Math.round(body[1] - body[2]), Math.round(body[2] * 2.0F), Math.round(body[2] * 2.0F));
      }
      if (this.contact != null && (this.state == DYING || this.state == GAME_OVER)) {
         this.drawContact(g2);
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
         if (this.practiceNote() != null) {
            this.drawCaption(g2, "Practice: " + this.practiceNote(), Main.FRAME_HEIGHT * 5 / 24, new Color(200, 230, 255));
         } else if (this.guide && this.state == PLAYING && this.score() == 0 && this.noticeSteps == 0) {   // not under a notice
            this.drawCaption(g2, "Yellow dots: a flap now. White: no flap. Red ring: a crash", Main.FRAME_HEIGHT * 5 / 24, new Color(255, 255, 255));
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

      if (this.state == READY) {
         this.drawTitle(g2);
      } else if (this.state == GAME_OVER) {
         // The game over screen rises into place over its first third of a second.
         float rise = Math.max(0.0F, 1.0F - this.stateSteps / (float)(this.stepsPerSecond / 3.0));
         int lift = Math.round(rise * rise * Main.FRAME_HEIGHT / 6.0F);
         g2.translate(0, lift);
         this.drawScoreboard(g2);
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

   /** Whether the visitor asked for reduced motion (the web page's shim answers; the desktop JDK gives null, so false). */
   private static boolean calm() {
      return Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
   }

   /** What made this run practice ("the flight guide, 1 rewind"), or null for a run that keeps its records. */
   private String practiceNote() {
      String guided = this.guidedPractice ? "the flight guide" : null;
      if (this.rewinds == 0) {
         return guided;
      }
      return guided == null ? rewindCount(this.rewinds) : guided + ", " + rewindCount(this.rewinds);
   }

   private String guideLine() {
      if (this.guide && this.highScore >= Guide.UNTIL) {
         return "Flight guide on: practice, not a record";
      }
      return (this.guide ? "Flight guide on" : "Flight guide off") + ": shows where a flap takes you";
   }

   /**
    * The flight guide: the next second of flight as dots, yellow if the bird flaps now and white if it does not, each
    * ending in a red ring where it would crash.
    */
   private void drawGuide(Graphics2D g2) {
      List<float[][]> shapes = new ArrayList<>();
      for (int pipeIndex = 0; pipeIndex < this.pipes.positions.size(); pipeIndex++) {
         shapes.add(this.pipeShape(pipeIndex));
      }
      int steps = (int)this.stepsPerSecond;
      int every = Math.max(1, steps / 14);
      // Yellow kept apart from the copper pipes and the teal bird that web/clips.ps1's autopilot looks for.
      this.drawPath(g2, Guide.path(this.bird, false, steps, every, shapes, Background.groundY), new Color(255, 255, 255));
      this.drawPath(g2, Guide.path(this.bird, true, steps, every, shapes, Background.groundY), new Color(255, 235, 90));
   }

   private void drawPath(Graphics2D g2, List<float[]> path, Color color) {
      int dot = Math.max(4, Bird.HEIGHT / 6);
      Graphics2D g = (Graphics2D)g2.create();
      for (int i = 0; i < path.size(); i++) {
         float[] point = path.get(i);
         int x = Math.round(point[0]);
         int y = Math.round(point[1]);
         if (point.length == 3) {
            int ring = dot * 2;
            g.setStroke(new BasicStroke(Math.max(2.0F, dot / 2.0F)));
            g.setColor(new Color(0, 0, 0, 120));
            g.drawOval(x - ring + 1, y - ring + 2, ring * 2, ring * 2);
            g.setColor(new Color(235, 50, 50));
            g.drawOval(x - ring, y - ring, ring * 2, ring * 2);
         } else {
            // Fainter further ahead, where the guess counts for less; a thin dark edge keeps it seen over a pale sky.
            int alpha = 240 - 120 * i / Math.max(1, path.size());
            g.setStroke(new BasicStroke(Math.max(1.0F, dot / 6.0F)));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.fillOval(x - dot / 2, y - dot / 2, dot, dot);
            g.setColor(new Color(40, 30, 20, alpha * 2 / 3));
            g.drawOval(x - dot / 2, y - dot / 2, dot, dot);
         }
      }
      g.dispose();
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

   static final int TITLE_BUTTON = 0;
   static final int REWIND_BUTTON = 1;
   static final int AGAIN_BUTTON = 2;

   /**
    * The game over screen's row of buttons, as wide as the scoreboard at x with width w, each {x, y, width, height}:
    * Title, Rewind (null with nothing to rewind) and Fly again, the widest.
    */
   static int[][] overButtons(int x, int y, int w, int u, boolean rewind) {
      int gap = u / 2;
      int h = u * 2;
      int titleW = (w - gap * 2) * 4 / 22;
      int rewindW = (w - gap * 2) * 8 / 22;   // Fly again gets the other 10
      int againX = x + titleW + gap + (rewind ? rewindW + gap : 0);
      return new int[][]{{x, y, titleW, h}, rewind ? new int[]{x + titleW + gap, y, rewindW, h} : null, {againX, y, x + w - againX, h}};
   }

   /** Which of the buttons a click at (px, py) is on, or -1. */
   static int overHit(int[][] buttons, int px, int py) {
      for (int i = 0; i < buttons.length; i++) {
         int[] b = buttons[i];
         if (b != null && px >= b[0] && px < b[0] + b[2] && py >= b[1] && py < b[1] + b[3]) {
            return i;
         }
      }
      return -1;
   }

   private int[][] overButtons() {
      int[] layout = this.scoreboardLayout();
      return overButtons(layout[1], layout[5], layout[3], layout[0], this.rewind.size() > 0);
   }

   /** A rounded button with a label and an icon: 0 none, 1 rewind (two triangles pointing back), 2 fly (one pointing on). */
   private static void drawButton(Graphics2D g2, int[] button, Color fill, Color ink, String label, int iconKind) {
      int arc = button[3] / 2;
      g2.setColor(new Color(0, 0, 0, 90));
      g2.fillRoundRect(button[0] + 3, button[1] + 4, button[2], button[3], arc, arc);
      g2.setColor(fill);
      g2.fillRoundRect(button[0], button[1], button[2], button[3], arc, arc);
      g2.setColor(new Color(255, 255, 255));
      g2.drawRoundRect(button[0], button[1], button[2], button[3], arc, arc);
      int icon = iconKind == 0 ? 0 : button[3] / 2;
      int width = sans(g2, label, button[3] * 9 / 20, button[2] - icon * 3 / 2 - button[3] / 3);
      int left = button[0] + (button[2] - width - icon * 3 / 2) / 2;
      int top = button[1] + (button[3] - icon) / 2;
      g2.setColor(ink);
      if (iconKind == 1) {
         for (int i = 0; i < 2; i++) {
            int tip = left + i * icon * 3 / 5;
            g2.fillPolygon(new int[]{tip, tip + icon * 3 / 5, tip + icon * 3 / 5}, new int[]{top + icon / 2, top, top + icon}, 3);
         }
      } else if (iconKind == 2) {
         g2.fillPolygon(new int[]{left, left + icon * 4 / 5, left}, new int[]{top, top + icon / 2, top + icon}, 3);
      }
      g2.drawString(label, left + icon * 3 / 2, button[1] + button[3] * 2 / 3);
   }

   /**
    * Title, Rewind and Fly again under the scoreboard. Fly again is copper like the pipes and glows a little (held
    * still with reduced motion), since a press anywhere else does the same.
    */
   private void drawOverButtons(Graphics2D g2) {
      int[][] buttons = this.overButtons();
      int[] again = buttons[AGAIN_BUTTON];
      float glow = calm() ? 0.5F : 0.5F + 0.5F * (float)Math.sin(this.stateSteps * 2.0 * Math.PI / (this.stepsPerSecond * 1.6));
      int reach = Math.max(2, again[3] / 6);
      g2.setColor(new Color(255, 210, 150, Math.round(40 + 80 * glow)));
      g2.fillRoundRect(again[0] - reach, again[1] - reach, again[2] + reach * 2, again[3] + reach * 2, again[3] / 2 + reach * 2, again[3] / 2 + reach * 2);
      drawButton(g2, buttons[TITLE_BUTTON], new Color(12, 64, 60, 225), new Color(255, 255, 255), "Title", 0);
      if (buttons[REWIND_BUTTON] != null) {
         drawButton(g2, buttons[REWIND_BUTTON], new Color(60, 120, 220), new Color(255, 255, 255), "Rewind", 1);
      }
      drawButton(g2, again, new Color(232, 149, 90), new Color(40, 30, 20), "Fly again", 2);
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

   /**
    * A spark where the bird's body met the pipe: the spot it touched, so a crash shows it was a real one. It stays
    * through the fall and the game over screen, pulsing gently.
    */
   private void drawContact(Graphics2D g2) {
      int x = Math.round(this.contact[0]);
      int y = Math.round(this.contact[1]);
      float pulse = 0.85F + 0.15F * (float)Math.sin(this.stateSteps * 2.0 * Math.PI / this.stepsPerSecond);
      int inner = Math.max(3, Math.round(Bird.HEIGHT / 7.0F * pulse));
      int outer = Math.max(8, Math.round(Bird.HEIGHT / 2.6F * pulse));
      Graphics2D g = (Graphics2D)g2.create();
      g.setStroke(new BasicStroke(Math.max(2.0F, Bird.HEIGHT / 16.0F), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      for (int pass = 0; pass < 2; pass++) {
         g.setColor(pass == 0 ? new Color(60, 20, 0, 150) : new Color(255, 236, 120));
         int shift = pass == 0 ? 2 : 0;
         for (int ray = 0; ray < 8; ray++) {
            double a = ray * Math.PI / 4.0 + Math.PI / 8.0;
            int length = ray % 2 == 0 ? outer : outer * 2 / 3;
            g.drawLine(x + shift + (int)Math.round(Math.cos(a) * inner * 1.6), y + shift + (int)Math.round(Math.sin(a) * inner * 1.6),
               x + shift + (int)Math.round(Math.cos(a) * length), y + shift + (int)Math.round(Math.sin(a) * length));
         }
         g.drawOval(x + shift - inner, y + shift - inner, inner * 2, inner * 2);
      }
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
      float tilt;
      if (this.state == READY) {
         tilt = Wing.hover(this.stateSteps / (this.stepsPerSecond * 0.8), calm());
      } else if (this.state == DYING || this.state == GAME_OVER) {
         tilt = Wing.LIMP;
      } else {
         tilt = Wing.flight(squash, this.bird.rotation());
      }
      Wing.draw(g, tilt);
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
      outlined(g2, text, x, y, new Color(255, 255, 255));
   }

   /** Text in the current font, in a color outlined in dark brown with a soft shadow, like the score. */
   private static void outlined(Graphics2D g2, String text, int x, int y, Color color) {
      int edge = Math.max(1, g2.getFont().getSize() / 16);
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
      g2.setColor(color);
      g2.drawString(text, x, y);
   }

   /** Sets a bold sans-serif font of the given size, shrunk to fit maxWidth. Returns the text's width. */
   private static int sans(Graphics2D g2, String text, int size, int maxWidth) {
      g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
      int width = g2.getFontMetrics().stringWidth(text);
      if (width > maxWidth) {
         g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, size * maxWidth / width)));
         width = g2.getFontMetrics().stringWidth(text);
      }
      return width;
   }

   /** A title and lines of text centered on the screen. */
   private void drawBanner(Graphics2D g2, String title, String... lines) {
      int big = Main.FRAME_HEIGHT / 8;
      int small = Main.FRAME_HEIGHT / 20;
      int centerY = Main.FRAME_HEIGHT * 2 / 5;
      g2.setColor(new Color(0, 0, 0, 120));
      g2.fillRect(0, centerY - big - small, Main.FRAME_WIDTH, big + small * (lines.length * 3 / 2 + 2));
      g2.setColor(new Color(255, 255, 255));
      int titleWidth = fitFont(g2, title, big, Main.FRAME_WIDTH * 3 / 5);
      g2.drawString(title, (Main.FRAME_WIDTH - titleWidth) / 2, centerY);
      for (int line = 0; line < lines.length; line++) {
         int width = fitFont(g2, lines[line], small, Main.FRAME_WIDTH * 19 / 20);
         g2.drawString(lines[line], (Main.FRAME_WIDTH - width) / 2, centerY + small * 3 / 2 * (line + 1));
      }
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

   /** The scoreboard's tiles: the score, the best, and today's best once there is one. */
   private int scoreboardTiles() {
      return this.daily || this.course.bestScore() > 0 ? 3 : 2;
   }

   /**
    * Where the game over screen goes, top to bottom, in units that fit both a wide window and a narrow (portrait)
    * page: {unit, panel x, panel y, panel width, panel height, button y, title baseline, first line baseline}.
    */
   private int[] scoreboardLayout() {
      int u = Math.max(8, Math.min(Main.FRAME_HEIGHT / 24, Main.FRAME_WIDTH / 17));
      int cardW = Math.min(Main.FRAME_WIDTH * 23 / 25, u * 15);
      // The medal on top, then a row of tiles.
      int cardH = u * 9 / 10 + medalSize(u) + u * 7 / 2;
      int lines = this.practiceNote() != null ? 2 : 1;
      int total = u * 3 + cardH + lines * u * 3 / 2 + u * 5;
      int top = Math.max(u / 2, (Background.groundY - total) / 2);
      int cardY = top + u * 3;
      int linesY = cardY + cardH + u * 3 / 2;
      int buttonY = linesY + (lines - 1) * u * 3 / 2 + u;
      return new int[]{u, (Main.FRAME_WIDTH - cardW) / 2, cardY, cardW, cardH, buttonY, cardY - u * 3 / 5, linesY};
   }

   /** The game over medal's diameter for a layout unit. */
   private static int medalSize(int u) {
      return u * 17 / 5;
   }

   /**
    * The game over screen: "Game Over", then a panel of deep teal glass in a copper rim (the bird's and the pipes'
    * colors) with the medal centered on top (or an empty ring with the pipes the first medal needs) and a row of tiles
    * for the score, which counts up, the best and today's best, a beaten one lit in copper and labelled NEW; under it
    * the medal line and the Title, Rewind and Fly again buttons. With reduced motion the numbers and medal show at once.
    */
   private void drawScoreboard(Graphics2D g2) {
      int[] layout = this.scoreboardLayout();
      int u = layout[0];
      int x = layout[1];
      int y = layout[2];
      int w = layout[3];
      int h = layout[4];
      int center = Main.FRAME_WIDTH / 2;
      Color lightCopper = new Color(255, 199, 154);
      int titleWidth = sans(g2, "Game Over", u * 12 / 5, Main.FRAME_WIDTH * 9 / 10);
      outlined(g2, "Game Over", center - titleWidth / 2, layout[6], new Color(255, 226, 196));

      drawGlass(g2, x, y, w, h, u);

      // The score counts up once the screen has risen (up to 0.8 s for a long run); the medal and NEW labels follow it.
      int score = this.score();
      float sps = (float)this.stepsPerSecond;
      float countSteps = sps * Math.min(0.8F, 0.1F + 0.04F * score);
      float since = this.stateSteps - sps / 3.0F;
      float counted = calm() ? 1.0F : Math.max(0.0F, Math.min(1.0F, since / countSteps));
      float pop = calm() ? 1.0F : Math.max(0.0F, Math.min(1.0F, (since - countSteps) / (sps * 0.2F)));

      int d = medalSize(u);
      int medalX = center;
      int medalY = y + u * 7 / 10 + d / 2;
      Color medal = medalColor(score);
      if (medal != null && pop > 0.0F) {
         // Lands with a little bounce: big, then settling to its size.
         float scale = 1.0F + 0.5F * (1.0F - pop) * (1.0F - pop);
         drawMedal(g2, medal, medalX, medalY, Math.round(d * scale));
         if (pop >= 1.0F && !calm()) {
            this.drawTwinkle(g2, medalX, medalY, d);
         }
      } else {
         // An empty ring until the medal lands; with no medal won it holds the pipes the first one needs.
         Graphics2D ring = (Graphics2D)g2.create();
         ring.setColor(new Color(255, 255, 255, 25));
         ring.fillOval(medalX - d / 2, medalY - d / 2, d, d);
         ring.setStroke(new BasicStroke(Math.max(1.0F, u / 8.0F)));
         ring.setColor(new Color(255, 255, 255, 90));
         ring.drawOval(medalX - d / 2, medalY - d / 2, d, d);
         ring.dispose();
         if (medal == null) {
            String next = String.valueOf((score / 10 + 1) * 10);
            int nextWidth = sans(g2, next, d * 2 / 5, d * 3 / 4);
            g2.setColor(lightCopper);
            g2.drawString(next, medalX - nextWidth / 2, medalY + d * 3 / 20);
         }
      }

      int tiles = this.scoreboardTiles();
      int gap = u / 2;
      int tileY = y + u * 9 / 10 + d;
      int tileH = u * 3;
      int tileW = (w - u - gap * (tiles - 1)) / tiles;
      String[] names = {"SCORE", "BEST", "TODAY"};
      String[] values = {String.valueOf(Math.round(score * counted)), String.valueOf(this.highScore), String.valueOf(this.course.bestScore())};
      boolean[] fresh = {false, this.newBest, this.newToday};
      for (int tile = 0; tile < tiles; tile++) {
         int tileX = x + u / 2 + tile * (tileW + gap);
         boolean lit = fresh[tile] && pop > 0.0F;
         drawTile(g2, tileX, tileY, tileW, tileH, u, lit ? pop : 0.0F);
         String name = lit ? "NEW " + names[tile] : names[tile];
         int nameWidth = sans(g2, name, u * 13 / 20, tileW - u / 2);
         g2.setColor(lit ? new Color(255, 226, 120) : lightCopper);
         g2.drawString(name, tileX + (tileW - nameWidth) / 2, tileY + u);
         int valueWidth = sans(g2, values[tile], u * 3 / 2, tileW - u / 2);
         outlined(g2, values[tile], tileX + (tileW - valueWidth) / 2, tileY + u * 13 / 5, new Color(255, 255, 255));
      }

      int lineY = layout[7];
      if (this.practiceNote() != null) {
         String note = "Practice with " + this.practiceNote() + ": not a record";
         int noteWidth = sans(g2, note, u, Main.FRAME_WIDTH * 19 / 20);
         outlined(g2, note, center - noteWidth / 2, lineY, new Color(200, 230, 255));
         lineY += u * 3 / 2;
      }
      String line = medalLine(score);
      int lineWidth = sans(g2, line, u, Main.FRAME_WIDTH * 19 / 20);
      outlined(g2, line, center - lineWidth / 2, lineY, new Color(255, 255, 255));

      this.drawOverButtons(g2);
      int hintY = layout[5] + u * 7 / 2;
      String hint = this.rewind.size() > 0 ? "Space or a tap flies again, Z rewinds, R for the title" : "Space or a tap flies again, R for the title";
      int hintWidth = sans(g2, hint, u * 4 / 5, Main.FRAME_WIDTH * 19 / 20);
      outlined(g2, hint, center - hintWidth / 2, hintY, new Color(255, 255, 255));
   }

   /** The game's panel: teal glass the sky shows through (the bird's color), in a copper rim with a lighter copper inner line (the pipes'). */
   private static void drawGlass(Graphics2D g2, int x, int y, int w, int h, int u) {
      g2.setColor(new Color(0, 0, 0, 70));
      g2.fillRoundRect(x + u / 6, y + u / 4, w, h, u * 3 / 2, u * 3 / 2);
      g2.setColor(new Color(12, 64, 60, 215));
      g2.fillRoundRect(x, y, w, h, u * 3 / 2, u * 3 / 2);
      Graphics2D edge = (Graphics2D)g2.create();
      edge.setStroke(new BasicStroke(Math.max(2.0F, u / 5.0F)));
      edge.setColor(new Color(196, 106, 53));
      edge.drawRoundRect(x, y, w, h, u * 3 / 2, u * 3 / 2);
      edge.setStroke(new BasicStroke(Math.max(1.0F, u / 14.0F)));
      edge.setColor(new Color(232, 149, 90, 160));
      int inner = Math.max(3, u / 3);
      edge.drawRoundRect(x + inner, y + inner, w - inner * 2, h - inner * 2, u, u);
      edge.dispose();
   }

   /** A darker tile on the glass, its frame lit in copper as lit goes from 0 to 1. */
   private static void drawTile(Graphics2D g2, int x, int y, int w, int h, int u, float lit) {
      g2.setColor(new Color(0, 0, 0, 70));
      g2.fillRoundRect(x, y, w, h, u * 3 / 4, u * 3 / 4);
      Graphics2D frame = (Graphics2D)g2.create();
      frame.setStroke(new BasicStroke(Math.max(1.0F, lit > 0.0F ? u / 6.0F * lit : u / 14.0F)));
      frame.setColor(lit > 0.0F ? new Color(232, 149, 90) : new Color(255, 255, 255, 60));
      frame.drawRoundRect(x, y, w, h, u * 3 / 4, u * 3 / 4);
      frame.dispose();
   }

   private TitleCard titleCard() {
      int bob = Bird.HEIGHT / 5;
      return new TitleCard(Main.FRAME_WIDTH, Main.FRAME_HEIGHT, Main.FRAME_HEIGHT / 3 - bob, Main.FRAME_HEIGHT / 3 + Bird.HEIGHT + bob);
   }

   /**
    * The title screen: the name over the hovering bird, then a card of the two courses (the chosen one lit in copper,
    * with its record) and the flight guide's switch, each a button, and a gently pulsing prompt to flap.
    */
   private void drawTitle(Graphics2D g2) {
      TitleCard card = this.titleCard();
      int u = card.u;
      int center = Main.FRAME_WIDTH / 2;
      Color lightCopper = new Color(255, 199, 154);
      int titleWidth = sans(g2, "Flappy Bird", u * 12 / 5, Main.FRAME_WIDTH * 9 / 10);
      outlined(g2, "Flappy Bird", center - titleWidth / 2, card.titleY, new Color(255, 226, 196));
      drawGlass(g2, card.x, card.y, card.w, card.h, u);

      String[] names = {"ENDLESS", "TODAY'S COURSE"};
      String[] about = {"New pipes every run", "Same pipes all day"};
      int today = this.course.bestScore();
      String[] records = {"Best " + this.highScore, today > 0 ? "Race your ghost: " + today : "Race your ghost"};
      for (int tile = TitleCard.ENDLESS; tile <= TitleCard.TODAY; tile++) {
         boolean chosen = tile == TitleCard.TODAY == this.daily;
         int tileX = card.tileX(tile);
         int inside = card.tileW - u / 2;
         drawTile(g2, tileX, card.tileY, card.tileW, card.tileH, u, chosen ? 1.0F : 0.0F);
         int nameWidth = sans(g2, names[tile], u * 13 / 20, inside);
         g2.setColor(chosen ? new Color(255, 226, 120) : lightCopper);
         g2.drawString(names[tile], tileX + (card.tileW - nameWidth) / 2, card.tileY + u);
         int aboutWidth = sans(g2, about[tile], u * 7 / 10, inside);
         g2.setColor(new Color(255, 255, 255, chosen ? 255 : 190));
         g2.drawString(about[tile], tileX + (card.tileW - aboutWidth) / 2, card.tileY + u * 9 / 5);
         int recordWidth = sans(g2, records[tile], u * 7 / 10, inside);
         g2.setColor(chosen ? lightCopper : new Color(255, 199, 154, 170));
         g2.drawString(records[tile], tileX + (card.tileW - recordWidth) / 2, card.tileY + u * 13 / 5);
      }

      // The guide's switch: a track lit copper with its knob to the right when on, dark with it to the left when off.
      int chipX = card.tileX(TitleCard.ENDLESS);
      int chipW = card.w - u;
      drawTile(g2, chipX, card.chipY, chipW, card.chipH, u, 0.0F);
      int trackH = card.chipH - u / 2;
      int trackW = trackH * 9 / 5;
      int trackX = chipX + u / 2;
      int trackY = card.chipY + u / 4;
      g2.setColor(this.guide ? new Color(196, 106, 53) : new Color(255, 255, 255, 50));
      g2.fillRoundRect(trackX, trackY, trackW, trackH, trackH, trackH);
      int knob = trackH - Math.max(2, u / 6) * 2;
      g2.setColor(new Color(255, 255, 255));
      g2.fillOval(this.guide ? trackX + trackW - trackH + (trackH - knob) / 2 : trackX + (trackH - knob) / 2, trackY + (trackH - knob) / 2, knob, knob);
      String label = this.guideLine();
      sans(g2, label, u * 7 / 10, chipW - trackW - u * 3 / 2);
      g2.setColor(new Color(255, 255, 255, this.guide ? 255 : 200));
      g2.drawString(label, trackX + trackW + u / 2, card.chipY + card.chipH / 2 + g2.getFont().getSize() * 7 / 20);

      // The prompt breathes a little (held still with reduced motion), so the first thing to do stands out.
      double breath = calm() ? 0.0 : Math.sin(this.stateSteps * 2.0 * Math.PI / (this.stepsPerSecond * 1.6));
      String prompt = "Space, click or tap to flap";
      int promptWidth = sans(g2, prompt, Math.round(u * (1.1F + 0.06F * (float)breath)), Main.FRAME_WIDTH * 19 / 20);
      outlined(g2, prompt, center - promptWidth / 2, card.promptY, new Color(255, 255, 255));
      String keys = "C switches the course, G the guide, M the sound";
      int keysWidth = sans(g2, keys, u * 3 / 5, Main.FRAME_WIDTH * 19 / 20);
      outlined(g2, keys, center - keysWidth / 2, card.keysY, lightCopper);
   }

   /** A medal of diameter d centered at (x, y): a rim, a ring and a star, with a shine. */
   private static void drawMedal(Graphics2D g2, Color color, int x, int y, int d) {
      int r = d / 2;
      g2.setColor(new Color(0, 0, 0, 90));
      g2.fillOval(x - r + d / 25 + 1, y - r + d / 20 + 1, d, d);
      g2.setColor(color.darker());
      g2.fillOval(x - r, y - r, d, d);
      int inset = Math.max(2, d / 14);
      g2.setColor(color);
      g2.fillOval(x - r + inset, y - r + inset, d - inset * 2, d - inset * 2);
      Graphics2D g = (Graphics2D)g2.create();
      g.setStroke(new BasicStroke(Math.max(1.0F, d / 30.0F)));
      g.setColor(color.darker());
      int ring = d * 36 / 100;
      g.drawOval(x - ring, y - ring, ring * 2, ring * 2);
      int[] xs = new int[10];
      int[] ys = new int[10];
      for (int i = 0; i < 10; i++) {
         double a = -Math.PI / 2.0 + i * Math.PI / 5.0;
         double reach = (i % 2 == 0 ? 0.25 : 0.1) * d;
         xs[i] = x + (int)Math.round(Math.cos(a) * reach);
         ys[i] = y + (int)Math.round(Math.sin(a) * reach);
      }
      g.fillPolygon(xs, ys, 10);
      g.setColor(new Color(255, 255, 255, 170));
      g.fillOval(x - r * 3 / 5, y - r * 3 / 5, d / 6, d / 6);
      g.dispose();
   }

   /** A sparkle that grows and fades at a new spot on the medal every 0.7 s. */
   private void drawTwinkle(Graphics2D g2, int x, int y, int d) {
      int period = Math.max(1, (int)(this.stepsPerSecond * 0.7));
      Random spot = new Random(this.stateSteps / period);
      double angle = spot.nextDouble() * Math.PI * 2.0;
      double reach = spot.nextDouble() * d * 0.4;
      int sx = x + (int)Math.round(Math.cos(angle) * reach);
      int sy = y + (int)Math.round(Math.sin(angle) * reach);
      float life = this.stateSteps % period / (float)period;
      int arm = Math.round(d / 6.0F * (float)Math.sin(life * Math.PI));
      if (arm < 2) {
         return;
      }
      g2.setColor(new Color(255, 255, 255, 230));
      g2.fillPolygon(new int[]{sx, sx + arm / 4, sx + arm, sx + arm / 4, sx, sx - arm / 4, sx - arm, sx - arm / 4},
         new int[]{sy - arm, sy - arm / 4, sy, sy + arm / 4, sy + arm, sy + arm / 4, sy, sy - arm / 4}, 8);
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
            return new Color(175, 225, 242);   // icy blue, so it does not pass for Silver
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
      this.background = new Background(this.images.get(0));
      this.pipes = new Pipes(this.images.get(1), this.images.get(2), this.daily ? this.course.pipeRandom() : new Random());
      this.bird = new Bird(this.images.get(3));
      this.bird.position.set(new Position(this.bird.position.collisionX, (float)(Main.FRAME_HEIGHT / 3)));
      this.collided = false;
      this.contact = null;
      this.paused = false;
      this.state = READY;
      this.stateSteps = 0;
      this.lastScore = 0;
      this.scorePop = 0.0F;
      this.skyPhase = 0.0F;   // every run starts at sunrise
      this.puffs.clear();
      this.steps = 0;
      this.rewind.clear();
      this.rewinds = 0;
      this.samples.clear();
      this.samples.add(this.sample());
      this.target = this.daily ? this.course.bestScore() : this.highScore;
      this.noticeSteps = 0;
      this.guidedPractice = false;
      if (!this.guideChosen) {
         boolean was = this.guide;
         this.guide = this.highScore < Guide.UNTIL;
         if (was && !this.guide) {
            this.notice = "Best of " + Guide.UNTIL + "! The flight guide steps aside (G brings it back)";
            this.noticeSteps = (int)this.stepsPerSecond * 4;
         }
      }
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
               this.switchCourse();
            }
            return;
         case KeyEvent.VK_G:
            if (this.state == GAME_OVER || this.state == DYING) {
               return;
            }
            this.toggleGuide();
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
