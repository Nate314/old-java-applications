package ultimateticktacktoe;

/** Who plays O: the computer at one of two strengths, a second person at the same screen, or a friend elsewhere, one move per link. */
public enum Mode {
   EASY("Computer: Easy", "Easy"),
   HARD("Computer: Hard", "Hard"),
   TWO_PLAYERS("Two players", "2 players"),
   FRIEND("Friend by link", "By link");

   public final String label;
   /** The button's label when the full ones do not fit across the board (a narrow screen). */
   public final String shortLabel;

   Mode(String label, String shortLabel) {
      this.label = label;
      this.shortLabel = shortLabel;
   }

   public boolean vsComputer() {
      return this == EASY || this == HARD;
   }
}
