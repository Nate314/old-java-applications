package flappyBird;

import java.awt.Image;

public final class Bird {
   public static final int WIDTH;
   public static final int HEIGHT = WIDTH = Main.FRAME_HEIGHT / 12;
   /** The tilt after a flap (nose up) and when diving (nose straight down), in degrees clockwise. */
   static final float NOSE_UP = -25.0F;
   static final float NOSE_DOWN = 90.0F;
   public Image image;
   public Position position;
   private float flapStrength;
   private float velocity;
   private float rotation;
   /** 1 right after a flap, easing back to 0: the bird is drawn stretched wide and squashed for it. */
   private float squash;

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
      this.rotation = NOSE_UP;
      this.squash = 1.0F;
   }

   /** A crash: the bird stops rising and drops, nose first. */
   public final void stall() {
      this.velocity = Math.min(this.velocity, 0.0F);
   }

   /** The tilt to draw the bird at, in degrees clockwise: nose up while it rises, turning to a dive as it falls. */
   public final float rotation() {
      return this.rotation;
   }

   public final float squash() {
      return this.squash;
   }

   public final void update() {
      this.velocity = this.velocity - this.flapStrength / 35.0F;
      this.squash = Math.max(0.0F, this.squash - 0.06F);
      // As in the original Flappy Bird: the nose stays up for a moment after a flap, then turns down into a dive.
      if (this.velocity < -this.flapStrength * 0.6F) {
         this.rotation = Math.min(NOSE_DOWN, this.rotation + 3.0F);
      }

      float rise = this.velocity;
      this.position.set(new Position(this.position.collisionX, this.position.collisionY - rise < (float)(-HEIGHT) ? (float)(-HEIGHT) : (this.position.collisionY - rise > (float)Main.FRAME_HEIGHT ? (float)Main.FRAME_HEIGHT : this.position.collisionY - rise)));
   }
}
