package flappyBird;

public final class Position {
   public float x;
   public float y;
   public float collisionX;
   public float collisionY;

   public Position(float x, float y) {
      this.x = x;
      this.y = y;
      this.collisionX = x;
      this.collisionY = y;
   }

   public final void set(Position other) {
      this.x = other.x;
      this.y = other.y;
      this.collisionX = other.x;
      this.collisionY = other.y;
   }
}
