package ultimateticktacktoe;

/** Who plays O: the computer at one of two strengths, or a second person at the same screen. */
public enum Mode {
   EASY("Computer: Easy"),
   HARD("Computer: Hard"),
   TWO_PLAYERS("Two players");

   public final String label;

   Mode(String label) {
      this.label = label;
   }

   public boolean vsComputer() {
      return this != TWO_PLAYERS;
   }
}
