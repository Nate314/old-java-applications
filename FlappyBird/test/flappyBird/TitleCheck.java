package flappyBird;

/** Checks the title screen's card: under the bird and above the ground at any shape of window, and where clicks land. */
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
      }
      System.out.println("TitleCheck passed");
   }
}
