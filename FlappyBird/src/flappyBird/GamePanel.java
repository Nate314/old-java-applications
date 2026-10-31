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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class GamePanel extends JPanel implements KeyListener {
   private static final int READY = 0;
   private static final int PLAYING = 1;
   private static final int GAME_OVER = 2;
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
   /** Game steps per second; the game logic moves a fixed amount per step. */
   private final double stepsPerSecond;
   private double pendingSteps = 0.0;
   private long lastTickNanos = System.nanoTime();

   public GamePanel(int stepsPerSecond) {
      this.stepsPerSecond = stepsPerSecond;
      this.images.add(new ImageIcon("bg.jpg").getImage().getScaledInstance(Main.FRAME_WIDTH, Main.FRAME_HEIGHT, 4));
      this.images.add(new ImageIcon("ground.jpg").getImage().getScaledInstance(Main.FRAME_WIDTH, Main.FRAME_HEIGHT / 8, 4));
      this.images.add(new ImageIcon("topPipe.png").getImage().getScaledInstance(Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, 4));
      this.images.add(new ImageIcon("bottomPipe.png").getImage().getScaledInstance(Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, 4));
      this.images.add(new ImageIcon("bird.png").getImage().getScaledInstance(Bird.WIDTH, Bird.HEIGHT, 4));
      this.reset();
      new Timer(10, event -> this.tick()).start();
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
      if (this.paused || this.state == GAME_OVER) {
         return;
      }

      this.background.scroll();
      if (this.state == READY) {
         return;
      }

      this.bird.update();
      this.pipes.scroll();
      this.collided = this.hitsPipe() || this.bird.position.collisionY + (float)Bird.HEIGHT > (float)Background.groundY;
      if (this.collided && !this.debugMode) {
         this.state = GAME_OVER;
         if (this.score() > this.highScore) {
            this.highScore = this.score();
            saveHighScore(this.highScore);
         }
      }
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

      g2.drawImage(this.bird.image, (int)this.bird.position.collisionX, (int)this.bird.position.collisionY, null);
      if (this.debugMode) {
         g2.drawRect((int)this.bird.position.collisionX, (int)this.bird.position.collisionY, this.bird.image.getWidth(null), this.bird.image.getHeight(null));
      }

      if (this.collided && this.debugMode) {
         g2.setColor(new Color(255, 0, 0));
         g2.setFont(new Font("Times New Roman", 1, Main.FRAME_HEIGHT / 10));
         g2.drawString("COLISION", Main.FRAME_WIDTH / 2, (Main.FRAME_HEIGHT << 1) / 10);
      }

      g2.setColor(new Color(0, 0, 255));
      g2.setFont(new Font("Times New Roman", 1, Main.FRAME_HEIGHT / 10));
      g2.drawString(String.valueOf(this.score()), Main.FRAME_WIDTH / 2, Main.FRAME_HEIGHT / 10);

      if (this.state == READY) {
         this.drawBanner(g2, "Flappy Bird", "Press Space to start", "Best: " + this.highScore);
      } else if (this.state == GAME_OVER) {
         this.drawBanner(g2, "Game Over", "Score: " + this.score() + "   Best: " + this.highScore, "Press Space to play again");
      } else if (this.paused) {
         this.drawBanner(g2, "Paused", "Press P to resume", "");
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

   /** A title and two lines of text centered on the screen. */
   private void drawBanner(Graphics2D g2, String title, String line1, String line2) {
      int big = Main.FRAME_HEIGHT / 8;
      int small = Main.FRAME_HEIGHT / 20;
      int centerY = Main.FRAME_HEIGHT * 2 / 5;
      g2.setColor(new Color(0, 0, 0, 120));
      g2.fillRect(0, centerY - big - small, Main.FRAME_WIDTH, big + small * 4);
      g2.setColor(new Color(255, 255, 255));
      g2.setFont(new Font("Times New Roman", 1, big));
      g2.drawString(title, (Main.FRAME_WIDTH - g2.getFontMetrics().stringWidth(title)) / 2, centerY);
      g2.setFont(new Font("Times New Roman", 1, small));
      g2.drawString(line1, (Main.FRAME_WIDTH - g2.getFontMetrics().stringWidth(line1)) / 2, centerY + small * 3 / 2);
      g2.drawString(line2, (Main.FRAME_WIDTH - g2.getFontMetrics().stringWidth(line2)) / 2, centerY + small * 3);
   }

   private void reset() {
      this.background = new Background(this.images.get(0), this.images.get(1));
      this.pipes = new Pipes(this.images.get(2), this.images.get(3));
      this.bird = new Bird(this.images.get(4));
      this.bird.position.set(new Position(this.bird.position.collisionX, (float)(Main.FRAME_HEIGHT / 3)));
      this.collided = false;
      this.paused = false;
      this.state = READY;
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
            if (this.state == GAME_OVER) {
               this.reset();
            } else if (!this.paused) {
               this.state = PLAYING;
               this.bird.flap();
            }
            return;
         case KeyEvent.VK_UP:
            this.consoleFontSize += 5;
            return;
         case KeyEvent.VK_DOWN:
            this.consoleFontSize = Math.max(5, this.consoleFontSize - 5);
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
}
