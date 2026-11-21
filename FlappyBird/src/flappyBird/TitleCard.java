package flappyBird;

/**
 * Where the title screen goes, in one unit that fits both a wide window and a narrow page (as the scoreboard's does):
 * the title above the hovering bird, and under it a card of two course tiles and the flight guide's switch, then the
 * prompt to flap. Each of the card's parts is a button for a click or a tap, as C and G are keys.
 */
final class TitleCard {
   static final int NONE = -1;
   static final int ENDLESS = 0;
   static final int TODAY = 1;
   static final int GUIDE = 2;

   final int u;
   final int x;
   final int y;
   final int w;
   final int h;
   final int titleY;
   final int tileY;
   final int tileW;
   final int tileH;
   final int gap;
   final int chipY;
   final int chipH;
   final int promptY;
   final int keysY;

   /** For a game of width by height whose hovering bird spans birdTop to birdBottom. */
   TitleCard(int width, int height, int birdTop, int birdBottom) {
      this.u = Math.max(8, Math.min(height / 24, width / 17));
      this.w = Math.min(width * 23 / 25, this.u * 17);
      this.x = (width - this.w) / 2;
      this.titleY = birdTop - this.u;
      this.y = birdBottom + this.u;
      this.gap = this.u / 2;
      this.tileY = this.y + this.u / 2;
      this.tileH = this.u * 3;
      this.tileW = (this.w - this.u - this.gap) / 2;
      this.chipY = this.tileY + this.tileH + this.u / 2;
      this.chipH = this.u * 3 / 2;
      this.h = this.chipY + this.chipH + this.u / 2 - this.y;
      this.promptY = this.y + this.h + this.u * 2;
      this.keysY = this.promptY + this.u * 6 / 5;
   }

   /** The left edge of a course tile (ENDLESS or TODAY). */
   int tileX(int tile) {
      return this.x + this.u / 2 + tile * (this.tileW + this.gap);
   }

   /** Which part of the card a click at (px, py) is on: ENDLESS, TODAY, GUIDE or NONE. */
   int hit(int px, int py) {
      if (px < this.x + this.u / 2 || px >= this.x + this.w - this.u / 2) {
         return NONE;
      }
      if (py >= this.tileY && py < this.tileY + this.tileH) {
         return px < this.tileX(TODAY) - this.gap / 2 ? ENDLESS : TODAY;
      }
      return py >= this.chipY && py < this.chipY + this.chipH ? GUIDE : NONE;
   }
}
