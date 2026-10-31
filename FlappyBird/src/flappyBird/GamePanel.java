package flappyBird;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public final class GamePanel extends JPanel implements KeyListener {
   private boolean debugMode = false;
   boolean running = false;
   private boolean collided = false;
   private int consoleFontSize = 25;
   private int launchArgument;
   private float redPhase;
   private float greenPhase;
   private float bluePhase;
   private ArrayList images = new ArrayList();
   private AffineTransform identityTransform = new AffineTransform();
   private AffineTransform transform = new AffineTransform();
   private Background background;
   private Pipes pipes;
   private Bird bird;

   public GamePanel(Main frame, int launchArgument) {
      this.launchArgument = launchArgument;
      this.images.add(new ImageIcon("bg.jpg").getImage().getScaledInstance(Main.FRAME_WIDTH, Main.FRAME_HEIGHT, 4));
      this.images.add(new ImageIcon("ground.jpg").getImage().getScaledInstance(Main.FRAME_WIDTH, Main.FRAME_HEIGHT / 8, 4));
      this.images.add(new ImageIcon("topPipe.png").getImage().getScaledInstance(Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, 4));
      this.images.add(new ImageIcon("bottomPipe.png").getImage().getScaledInstance(Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, 4));
      this.images.add(new ImageIcon("bird.png").getImage().getScaledInstance(Bird.WIDTH, Bird.HEIGHT, 4));
      this.background = new Background((Image)this.images.get(0), (Image)this.images.get(1));
      this.pipes = new Pipes((Image)this.images.get(2), (Image)this.images.get(3));
      this.bird = new Bird((Image)this.images.get(4));
      this.transform.setTransform(this.identityTransform);
      new Thread(new RenderLoop(this)).start();
   }

   private boolean hitsPipe() {
      for (int pipeIndex = Pipes.currentPipeIndex - 3; pipeIndex < Pipes.currentPipeIndex; pipeIndex++) {
         if (pipeIndex % 2 == 0) {
            int pipeLeft;
            int pipeRight = (pipeLeft = (int)((Position)this.pipes.positions.get(pipeIndex)).collisionX) + Pipes.PIPE_WIDTH;
            int pipeBottom = (int)((Position)this.pipes.positions.get(pipeIndex)).collisionY + Pipes.PIPE_HEIGHT;
            int birdLeft;
            int birdRight = (birdLeft = (int)this.bird.position.collisionX) + Bird.WIDTH;
            int birdTop = (int)this.bird.position.collisionY;
            if (pipeLeft < birdRight && birdRight < pipeRight && pipeBottom > birdTop || pipeLeft < birdLeft && birdLeft < pipeRight && pipeBottom > birdTop) {
               return true;
            }
         } else {
            int pipeLeft;
            int pipeRight = (pipeLeft = (int)((Position)this.pipes.positions.get(pipeIndex)).collisionX) + Pipes.PIPE_WIDTH;
            int pipeTop = (int)((Position)this.pipes.positions.get(pipeIndex)).collisionY;
            int birdLeft;
            int birdRight = (birdLeft = (int)this.bird.position.collisionX) + Bird.WIDTH;
            int birdBottom;
            birdBottom = (birdBottom = (int)this.bird.position.collisionY) + Bird.HEIGHT;
            if (pipeLeft < birdRight && birdRight < pipeRight && pipeTop < birdBottom || pipeLeft < birdLeft && birdLeft < pipeRight && pipeTop < birdBottom) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public final void paint(Graphics graphics) {
      if (this.running) {
         this.background.scroll();
         this.bird.update();
         this.pipes.scroll();
      }

      Graphics2D g2;
      (g2 = (Graphics2D)graphics)
         .setColor(
            new Color(
               (int)(127.0 * (Math.cos((double)(this.redPhase += 0.02F)) + 1.0)),
               (int)(127.0 * (Math.cos((double)(this.greenPhase += 0.02F)) + 1.0)),
               (int)(127.0 * (Math.sin((double)(this.bluePhase += 0.02F)) + 1.0))
            )
         );
      g2.setStroke(new BasicStroke((float)(Main.FRAME_WIDTH / 250)));

      for (int i = 0; i < ((ArrayList)this.background.layers.get(0)).size(); i++) {
         g2.drawImage(this.background.backgroundImage, (int)((Position)((ArrayList)this.background.layers.get(0)).get(i)).x, (int)((Position)((ArrayList)this.background.layers.get(0)).get(i)).y, null);
         if (this.debugMode) {
            g2.drawRect(
               (int)((Position)((ArrayList)this.background.layers.get(0)).get(i)).x,
               (int)((Position)((ArrayList)this.background.layers.get(0)).get(i)).y,
               this.background.backgroundImage.getWidth(null),
               this.background.backgroundImage.getHeight(null)
            );
         }
      }

      int firstRenderedPipe = Pipes.currentPipeIndex < 2 ? Pipes.currentPipeIndex - 1 : (Pipes.currentPipeIndex < 4 ? Pipes.currentPipeIndex - 3 : Pipes.currentPipeIndex - 5);
      int lastRenderedPipe = Pipes.currentPipeIndex + 5;

      for (int pipeIndex = firstRenderedPipe; pipeIndex < lastRenderedPipe; pipeIndex++) {
         if (pipeIndex % 2 == 0) {
            g2.drawImage(this.pipes.topPipeImage, (int)((Position)this.pipes.positions.get(pipeIndex)).x, (int)((Position)this.pipes.positions.get(pipeIndex)).y, null);
            if (this.debugMode) {
               g2.drawRect((int)((Position)this.pipes.positions.get(pipeIndex)).x, (int)((Position)this.pipes.positions.get(pipeIndex)).y, this.pipes.topPipeImage.getWidth(null), this.pipes.topPipeImage.getHeight(null));
            }
         } else {
            g2.drawImage(this.pipes.bottomPipeImage, (int)((Position)this.pipes.positions.get(pipeIndex)).x, (int)((Position)this.pipes.positions.get(pipeIndex)).y, null);
            if (this.debugMode) {
               g2.drawRect((int)((Position)this.pipes.positions.get(pipeIndex)).x, (int)((Position)this.pipes.positions.get(pipeIndex)).y, this.pipes.bottomPipeImage.getWidth(null), this.pipes.bottomPipeImage.getHeight(null));
            }
         }
      }

      for (int i = 0; i < ((ArrayList)this.background.layers.get(1)).size(); i++) {
         g2.drawImage(this.background.groundImage, (int)((Position)((ArrayList)this.background.layers.get(1)).get(i)).x, (int)((Position)((ArrayList)this.background.layers.get(1)).get(i)).y, null);
         if (this.debugMode) {
            g2.drawRect(0, Main.FRAME_HEIGHT * 7 / 8, Main.FRAME_WIDTH, Main.FRAME_HEIGHT / 8);
         }
      }

      g2.drawImage(this.bird.image, (int)this.bird.position.collisionX, (int)this.bird.position.collisionY, null);
      if (this.debugMode) {
         g2.drawRect((int)this.bird.position.collisionX, (int)this.bird.position.collisionY, this.bird.image.getWidth(null), this.bird.image.getHeight(null));
      }

      if (!this.hitsPipe() && !(this.bird.position.collisionY + (float)Bird.HEIGHT > (float)Background.groundY)) {
         this.collided = false;
      } else {
         this.collided = true;
      }

      if (this.collided) {
         if (this.debugMode) {
            g2.setColor(new Color(255, 0, 0));
            g2.setFont(new Font("Times New Roman", 1, Main.FRAME_HEIGHT / 10));
            g2.drawString("COLISION", Main.FRAME_WIDTH / 2, (Main.FRAME_HEIGHT << 1) / 10);
         } else {
            this.reset();
         }
      }

      g2.setColor(new Color(0, 0, 255));
      g2.setFont(new Font("Times New Roman", 1, Main.FRAME_HEIGHT / 10));
      int score = (Pipes.currentPipeIndex - 1) / 2 - 1;
      g2.drawString(String.valueOf(score), Main.FRAME_WIDTH / 2, Main.FRAME_HEIGHT / 10);
      if (this.debugMode) {
         g2.setColor(new Color(0, 0, 0));
         g2.setFont(new Font("Times New Roman", 1, this.consoleFontSize));
         String[] debugLines = new String[]{
            "bird x: " + this.bird.position.collisionX,
            "bird y: " + this.bird.position.collisionY,
            "current pipe x: " + ((Position)this.pipes.positions.get(Pipes.currentPipeIndex)).collisionX,
            "current pipe y: " + ((Position)this.pipes.positions.get(Pipes.currentPipeIndex)).collisionY,
            "paused: " + !this.running,
            "colision: " + this.collided,
            "consoleFontSize: " + this.consoleFontSize,
            "rendered pipes: " + firstRenderedPipe + " - " + lastRenderedPipe,
            ""
         };

         for (int line = 0; line < 9; line++) {
            g2.drawString(debugLines[line], Main.FRAME_WIDTH - 11 * this.consoleFontSize, Main.FRAME_HEIGHT - this.consoleFontSize * 10 + this.consoleFontSize * line);
         }
      }
   }

   private void reset() {
      this.background = new Background((Image)this.images.get(0), (Image)this.images.get(1));
      this.pipes = new Pipes((Image)this.images.get(2), (Image)this.images.get(3));
      this.bird = new Bird((Image)this.images.get(4));
   }

   @Override
   public final void keyTyped(KeyEvent event) {
   }

   @Override
   public final void keyPressed(KeyEvent event) {
      switch (event.getKeyCode()) {
         case 32:
            this.bird.flap();
            return;
         case 38:
            this.consoleFontSize += 5;
            return;
         case 40:
            this.consoleFontSize -= 5;
            return;
         case 68:
            this.debugMode = !this.debugMode;
            return;
         case 80:
            this.running = !this.running;
            return;
         case 82:
            this.reset();
            return;
         case 84:
            GamePanel panel = this;
            Image firstImage = (Image)this.images.get(0);

            for (int i = 0; i < panel.images.size() - 1; i++) {
               panel.images
                  .set(
                     i,
                     ((Image)panel.images.get(i + 1)).getScaledInstance(((Image)panel.images.get(i)).getWidth(null), ((Image)panel.images.get(i)).getHeight(null), 4)
                  );
            }

            panel.images
               .set(
                  panel.images.size() - 1,
                  firstImage.getScaledInstance(((Image)panel.images.get(panel.images.size() - 1)).getWidth(null), ((Image)panel.images.get(panel.images.size() - 1)).getHeight(null), 4)
               );
            System.out.println("test");
      }
   }

   @Override
   public final void keyReleased(KeyEvent event) {
   }
}
