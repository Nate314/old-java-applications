package flappyBird;

import java.awt.Image;

public final class Bird {
   public static final int WIDTH;
   public static final int HEIGHT = WIDTH = Main.FRAME_HEIGHT / 12;
   public Image image;
   public Position position;
   private float flapStrength;
   private float velocity;
   private float rotation;

   public Bird(Image image) {
      this.position = new Position((float)(Main.FRAME_WIDTH / 4), 0.0F);
      this.flapStrength = (float)(HEIGHT / 10);
      this.velocity = 0.0F;
      this.rotation = 0.0F;
      this.image = image;
      this.update();
   }

   public final void flap() {
      this.velocity = this.flapStrength;
   }

   public final void update() {
      this.velocity = this.velocity - this.flapStrength / 35.0F;
      if (this.velocity < 0.0F) {
         this.rotation += 3.0F;
      } else {
         this.rotation -= 3.0F;
      }

      float rise = this.velocity;
      this.position.set(new Position(this.position.collisionX, this.position.collisionY - rise < (float)(-HEIGHT) ? (float)(-HEIGHT) : (this.position.collisionY - rise > (float)Main.FRAME_HEIGHT ? (float)Main.FRAME_HEIGHT : this.position.collisionY - rise)));
   }
}
