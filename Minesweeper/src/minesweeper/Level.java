package minesweeper;

/** The board sizes. Classic is the original game's 15 x 15 board with 18 mines. */
public enum Level {
   BEGINNER("Beginner", 9, 10),
   CLASSIC("Classic", 15, 18),
   EXPERT("Expert", 16, 40);

   final String label;
   final int size;
   final int mines;

   Level(String label, int size, int mines) {
      this.label = label;
      this.size = size;
      this.mines = mines;
   }
}
