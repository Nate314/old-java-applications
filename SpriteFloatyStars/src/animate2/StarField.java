package animate2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public final class StarField extends Component {
   public int starCount = 1000;
   private int starSize = 20;
   private int fieldWidth;
   private int fieldHeight;
   private double[] x = new double[this.starCount];
   private double[] y = new double[this.starCount];
   private double[] velocityX = new double[this.starCount];
   private double[] velocityY = new double[this.starCount];
   private int[] randomColorValues = new int[this.starCount * 3];
   private int mode = 2;

   public StarField(int mode, Dimension size, int starCount, int starSize) {
      this.mode = mode;
      this.starCount = starCount;
      this.starSize = starSize;

      for (int star = 0; star < this.starCount; star++) {
         if (this.mode == 0 || this.mode == 4) {
            this.x[star] = Math.random() * size.getWidth();
            this.y[star] = Math.random() * size.getHeight();
            this.velocityX[star] = Math.random();
            this.velocityY[star] = Math.random();
         } else if (this.mode == 1 || this.mode == 2) {
            this.x[star] = size.getWidth() / 2.0 - (double)(this.starSize / 2);
            this.y[star] = size.getHeight() / 2.0 - (double)(this.starSize / 2);
            this.velocityX[star] = 2.0 * (Math.random() - 0.5);
            this.velocityY[star] = 2.0 * (Math.random() - 0.5);
         }
      }

      for (int colorIndex = 0; colorIndex < this.starCount * 3; colorIndex++) {
         this.randomColorValues[colorIndex] = (int)(Math.random() * 255.0);
      }
   }

   @Override
   public final void paint(Graphics g) {
      super.paint(g);
      this.fieldWidth = this.getWidth();
      this.fieldHeight = this.getHeight();
      g.setColor(Color.blue);
      g.fillRect(0, 0, this.fieldWidth, this.fieldHeight);
      if (this.mode == 0) {
         Graphics driftGraphics = g;
         StarField driftField = this;

         for (int star = 0; star < driftField.starCount; star++) {
            driftField.setStarColor(driftGraphics, star);
            driftField.x[star] = driftField.x[star] - driftField.velocityX[star];
            driftField.y[star] = driftField.y[star] + driftField.velocityY[star];
            if (driftField.x[star] < (double)(0 - driftField.starSize)) {
               driftField.x[star] = driftField.x[star] + (double)(driftField.fieldWidth + 2 * driftField.starSize);
            } else if (driftField.y[star] > (double)(driftField.fieldHeight + driftField.starSize)) {
               driftField.y[star] = driftField.y[star] - (double)(driftField.fieldHeight + 2 * driftField.starSize);
            }

            driftGraphics.fillOval((int)driftField.x[star], (int)driftField.y[star], driftField.starSize, driftField.starSize);
         }
      } else if (this.mode == 1) {
         this.paintBouncingStars(g);
      } else if (this.mode == 2) {
         this.paintBouncingStars(g);
      } else if (this.mode == 3) {
         Graphics bounceGraphics = g;
         StarField bounceField = this;

         for (int star = 0; star < bounceField.starCount; star++) {
            bounceField.setStarColor(bounceGraphics, star);
            bounceField.x[star] = bounceField.x[star] - bounceField.velocityX[star];
            bounceField.y[star] = bounceField.y[star] - bounceField.velocityY[star];
            int starX = (int)bounceField.x[star];
            int starY = (int)bounceField.y[star];
            if (starX < 0) {
               bounceField.velocityX[star] = -bounceField.velocityX[star];
            } else if (starX > bounceField.fieldWidth - bounceField.starSize) {
               bounceField.velocityX[star] = -bounceField.velocityX[star];
            } else if (starY > bounceField.fieldHeight - bounceField.starSize) {
               bounceField.velocityY[star] = -bounceField.velocityY[star];
            } else if (starY < 0) {
               bounceField.velocityY[star] = -bounceField.velocityY[star];
            }

            bounceGraphics.fillOval(starX, starY, bounceField.starSize, bounceField.starSize);
         }
      } else if (this.mode == 4) {
         Graphics flockGraphics = g;
         StarField flockField = this;

         for (int star = 0; star < flockField.starCount; star++) {
            flockField.setStarColor(flockGraphics, star);

            for (int other = 0; other < flockField.starCount; other++) {
               if (flockField.x[star] < flockField.x[other]
                  && flockField.x[star] + (double)flockField.starSize > flockField.x[other]
                  && flockField.y[star] < flockField.y[other]
                  && flockField.y[star] + (double)flockField.starSize > flockField.y[other]) {
                  double averageVelocityX = (flockField.velocityX[star] + flockField.velocityX[other]) / 2.0;
                  double averageVelocityY = (flockField.velocityY[star] + flockField.velocityY[other]) / 2.0;
                  flockField.velocityX[star] = averageVelocityX;
                  flockField.velocityX[other] = averageVelocityX;
                  flockField.velocityY[star] = averageVelocityY;
                  flockField.velocityY[other] = averageVelocityY;
               }
            }

            flockField.x[star] = flockField.x[star] - flockField.velocityX[star];
            flockField.y[star] = flockField.y[star] + flockField.velocityY[star];
            if (flockField.x[star] < (double)(0 - flockField.starSize)) {
               flockField.x[star] = flockField.x[star] + (double)(flockField.fieldWidth + 2 * flockField.starSize);
            } else if (flockField.y[star] > (double)(flockField.fieldHeight + flockField.starSize)) {
               flockField.y[star] = flockField.y[star] - (double)(flockField.fieldHeight + 2 * flockField.starSize);
            }

            flockGraphics.fillOval((int)flockField.x[star], (int)flockField.y[star], flockField.starSize, flockField.starSize);
         }
      }

      this.repaint();
   }

   private void paintBouncingStars(Graphics g) {
      for (int star = 0; star < this.starCount; star++) {
         this.setStarColor(g, star);
         this.x[star] = this.x[star] - this.velocityX[star];
         this.y[star] = this.y[star] - this.velocityY[star];
         int starX = (int)this.x[star];
         int starY = (int)this.y[star];
         if (starX < 0) {
            this.velocityX[star] = -this.velocityX[star];
         } else if (starX > this.fieldWidth - this.starSize) {
            this.velocityX[star] = -this.velocityX[star];
         } else if (starY > this.fieldHeight - this.starSize) {
            this.velocityY[star] = -this.velocityY[star];
         } else if (starY < 0) {
            this.velocityY[star] = -this.velocityY[star];
         }

         g.fillOval(starX, starY, this.starSize, this.starSize);
      }
   }

   private void setStarColor(Graphics g, int star) {
      if (this.mode == 0) {
         g.setColor(Color.white);
      } else if (this.mode == 1) {
         int red = (int)(this.x[star] / (double)this.fieldWidth * 250.0);
         star = (int)(this.y[star] / (double)this.fieldWidth * 250.0);
         g.setColor(new Color(red, star, star));
      } else {
         if (this.mode == 2) {
            int directionX = (int)(this.velocityX[star] / Math.abs(this.velocityX[star]));
            star = (int)(this.velocityY[star] / Math.abs(this.velocityY[star]));
            if (directionX == 1 && star == 1) {
               g.setColor(new Color(255, 0, 0));
               return;
            }

            if (directionX == 1 && star == -1) {
               g.setColor(new Color(0, 255, 0));
               return;
            }

            if (directionX == -1 && star == -1) {
               g.setColor(new Color(255, 0, 255));
               return;
            }

            if (directionX == -1 && star == 1) {
               g.setColor(new Color(0, 255, 255));
               return;
            }
         } else if (this.mode != 3 && this.mode == 4) {
            g.setColor(Color.white);
         }
      }
   }
}
