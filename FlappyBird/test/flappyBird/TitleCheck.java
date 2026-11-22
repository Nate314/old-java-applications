package flappyBird;

/** Checks the title screen's card and the game over screen's buttons at any shape of window, and where clicks land. */
public final class TitleCheck {
   public static void main(String[] args) {
      int[][] sizes = {{1440, 810}, {960, 600}, {300, 520}, {600, 600}, {1600, 400}};
      for (int[] size : sizes) {
         int width = size[0];
         int height = size[1];
         int bird = height / 12;
         int top = height / 3 - bird / 5;
         int bottom = height / 3 + bird + bird / 5;
         TitleCard card = new TitleCard(width, height, top, bottom);
         String at = width + "x" + height + ": ";
         assert card.y > bottom : at + "the card starts under the hovering bird";
         assert card.titleY < top : at + "the title is above the bird";
         assert card.keysY < height * 7 / 8 : at + "the key line is above the ground, " + card.keysY;
         assert card.x >= 0 && card.x + card.w <= width : at + "the card fits across";
         assert card.tileX(TitleCard.TODAY) + card.tileW <= card.x + card.w - card.u / 2 : at + "both tiles fit in the card";
         int middle = card.tileY + card.tileH / 2;
         assert card.hit(card.tileX(TitleCard.ENDLESS) + 1, middle) == TitleCard.ENDLESS : at + "endless tile";
         assert card.hit(card.tileX(TitleCard.TODAY) + card.tileW - 1, middle) == TitleCard.TODAY : at + "today tile";
         assert card.hit(width / 2, card.chipY + card.chipH / 2) == TitleCard.GUIDE : at + "guide switch";
         assert card.hit(width / 2, card.y - 1) == TitleCard.NONE : at + "above the card flaps";
         assert card.hit(width / 2, card.promptY) == TitleCard.NONE : at + "the prompt flaps";
         assert card.hit(card.x, middle) == TitleCard.NONE : at + "the card's rim flaps";

         // The game over screen's buttons, at the scoreboard's unit and width: in a row inside it, Fly again the widest.
         int u = Math.max(8, Math.min(height / 24, width / 17));
         int boardW = Math.min(width * 23 / 25, u * 15);
         int boardX = (width - boardW) / 2;
         for (boolean rewind : new boolean[]{true, false}) {
            int[][] row = GamePanel.overButtons(boardX, 100, boardW, u, rewind);
            int[] title = row[GamePanel.TITLE_BUTTON];
            int[] again = row[GamePanel.AGAIN_BUTTON];
            assert (row[GamePanel.REWIND_BUTTON] != null) == rewind : at + "rewind only with a tape";
            assert title[0] == boardX && again[0] + again[2] == boardX + boardW : at + "the row spans the scoreboard";
            int right = title[0] + title[2];
            for (int[] b : row) {
               if (b != null && b != title) {
                  assert b[0] > right : at + "buttons do not overlap";
                  right = b[0] + b[2];
                  assert again[2] >= b[2] : at + "Fly again is the widest";
               }
            }
            for (int i = 0; i < row.length; i++) {
               if (row[i] != null) {
                  assert GamePanel.overHit(row, row[i][0] + 1, row[i][1] + row[i][3] / 2) == i : at + "button " + i + " is hit";
               }
            }
            assert GamePanel.overHit(row, title[0] + title[2] + 1, title[1] + 1) == -1 : at + "the gap is no button";
            assert GamePanel.overHit(row, width / 2, title[1] - 1) == -1 : at + "above the row is no button";
         }
      }
      System.out.println("TitleCheck passed");
   }
}
