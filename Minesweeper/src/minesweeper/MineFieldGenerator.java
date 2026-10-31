package minesweeper;

public final class MineFieldGenerator {
   private int mineCount;
   private String[][] grid;

   public MineFieldGenerator(int rows, int columns) {
      this.mineCount = (int)((double)columns * 1.25);
      this.grid = new String[rows][columns];
      String[][] cells = new String[rows][columns];
      int[] minePositions = new int[this.mineCount];
      int lastRow = rows - 1;
      int lastColumn = columns - 1;

      for (int mineIndex = 0; mineIndex < this.mineCount; mineIndex++) {
         System.out.println(rows);
         minePositions[mineIndex] = (int)(Math.random() * (double)(rows * columns));
      }

      for (int cellIndex = 0; cellIndex < rows * columns - 1; cellIndex++) {
         for (int mineIndex = 0; mineIndex < this.mineCount; mineIndex++) {
            if (cellIndex == minePositions[mineIndex]) {
               cells[cellIndex / rows][cellIndex % rows] = "b";
            }
         }

         if (cells[cellIndex / rows][cellIndex % rows] != "b") {
            cells[cellIndex / rows][cellIndex % rows] = " ";
         }
      }

      for (int row = 0; row < rows; row++) {
         for (int column = 0; column < columns; column++) {
            if (cells[row][column] == "b") {
               cells[row][column] = "b";
               if (row == 0 && column == 0) {
                  cells[row][column + 1] = cells[row][column + 1] == " "
                     ? "1"
                     : (cells[row][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column + 1]) + 1));
                  cells[row + 1][column + 1] = cells[row + 1][column + 1] == " "
                     ? "1"
                     : (cells[row + 1][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column + 1]) + 1));
                  cells[row + 1][column] = cells[row + 1][column] == " "
                     ? "1"
                     : (cells[row + 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column]) + 1));
               } else if (row == 0 && column == lastColumn) {
                  cells[row + 1][column] = cells[row + 1][column] == " "
                     ? "1"
                     : (cells[row + 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column]) + 1));
                  cells[row + 1][column - 1] = cells[row + 1][column - 1] == " "
                     ? "1"
                     : (cells[row + 1][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column - 1]) + 1));
                  cells[row][column - 1] = cells[row][column - 1] == " "
                     ? "1"
                     : (cells[row][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column - 1]) + 1));
               } else if (row == lastRow && column == lastColumn) {
                  cells[row - 1][column] = cells[row - 1][column] == " "
                     ? "1"
                     : (cells[row - 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column]) + 1));
                  cells[row][column - 1] = cells[row][column - 1] == " "
                     ? "1"
                     : (cells[row][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column - 1]) + 1));
                  cells[row - 1][column - 1] = cells[row - 1][column - 1] == " "
                     ? "1"
                     : (cells[row - 1][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column - 1]) + 1));
               } else if (row == lastRow && column == 0) {
                  cells[row - 1][column] = cells[row - 1][column] == " "
                     ? "1"
                     : (cells[row - 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column]) + 1));
                  cells[row - 1][column + 1] = cells[row - 1][column + 1] == " "
                     ? "1"
                     : (cells[row - 1][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column + 1]) + 1));
                  cells[row][column + 1] = cells[row][column + 1] == " "
                     ? "1"
                     : (cells[row][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column + 1]) + 1));
               } else if (row == 0 && column != 0 && column != lastColumn) {
                  cells[row][column + 1] = cells[row][column + 1] == " "
                     ? "1"
                     : (cells[row][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column + 1]) + 1));
                  cells[row + 1][column + 1] = cells[row + 1][column + 1] == " "
                     ? "1"
                     : (cells[row + 1][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column + 1]) + 1));
                  cells[row + 1][column] = cells[row + 1][column] == " "
                     ? "1"
                     : (cells[row + 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column]) + 1));
                  cells[row + 1][column - 1] = cells[row + 1][column - 1] == " "
                     ? "1"
                     : (cells[row + 1][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column - 1]) + 1));
                  cells[row][column - 1] = cells[row][column - 1] == " "
                     ? "1"
                     : (cells[row][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column - 1]) + 1));
               } else if (column == lastColumn && row != 0 && row != lastRow) {
                  cells[row - 1][column] = cells[row - 1][column] == " "
                     ? "1"
                     : (cells[row - 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column]) + 1));
                  cells[row + 1][column] = cells[row + 1][column] == " "
                     ? "1"
                     : (cells[row + 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column]) + 1));
                  cells[row + 1][column - 1] = cells[row + 1][column - 1] == " "
                     ? "1"
                     : (cells[row + 1][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column - 1]) + 1));
                  cells[row][column - 1] = cells[row][column - 1] == " "
                     ? "1"
                     : (cells[row][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column - 1]) + 1));
                  cells[row - 1][column - 1] = cells[row - 1][column - 1] == " "
                     ? "1"
                     : (cells[row - 1][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column - 1]) + 1));
               } else if (row == lastRow && column != 0 && column != lastColumn) {
                  cells[row - 1][column] = cells[row - 1][column] == " "
                     ? "1"
                     : (cells[row - 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column]) + 1));
                  cells[row - 1][column + 1] = cells[row - 1][column + 1] == " "
                     ? "1"
                     : (cells[row - 1][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column + 1]) + 1));
                  cells[row][column + 1] = cells[row][column + 1] == " "
                     ? "1"
                     : (cells[row][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column + 1]) + 1));
                  cells[row][column - 1] = cells[row][column - 1] == " "
                     ? "1"
                     : (cells[row][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column - 1]) + 1));
                  cells[row - 1][column - 1] = cells[row - 1][column - 1] == " "
                     ? "1"
                     : (cells[row - 1][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column - 1]) + 1));
               } else if (column == 0 && row != 0 && row != lastRow) {
                  cells[row - 1][column] = cells[row - 1][column] == " "
                     ? "1"
                     : (cells[row - 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column]) + 1));
                  cells[row - 1][column + 1] = cells[row - 1][column + 1] == " "
                     ? "1"
                     : (cells[row - 1][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column + 1]) + 1));
                  cells[row][column + 1] = cells[row][column + 1] == " "
                     ? "1"
                     : (cells[row][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column + 1]) + 1));
                  cells[row + 1][column + 1] = cells[row + 1][column + 1] == " "
                     ? "1"
                     : (cells[row + 1][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column + 1]) + 1));
                  cells[row + 1][column] = cells[row + 1][column] == " "
                     ? "1"
                     : (cells[row + 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column]) + 1));
               } else {
                  cells[row - 1][column] = cells[row - 1][column] == " "
                     ? "1"
                     : (cells[row - 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column]) + 1));
                  cells[row - 1][column + 1] = cells[row - 1][column + 1] == " "
                     ? "1"
                     : (cells[row - 1][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column + 1]) + 1));
                  cells[row][column + 1] = cells[row][column + 1] == " "
                     ? "1"
                     : (cells[row][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column + 1]) + 1));
                  cells[row + 1][column + 1] = cells[row + 1][column + 1] == " "
                     ? "1"
                     : (cells[row + 1][column + 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column + 1]) + 1));
                  cells[row + 1][column] = cells[row + 1][column] == " "
                     ? "1"
                     : (cells[row + 1][column] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column]) + 1));
                  cells[row + 1][column - 1] = cells[row + 1][column - 1] == " "
                     ? "1"
                     : (cells[row + 1][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row + 1][column - 1]) + 1));
                  cells[row][column - 1] = cells[row][column - 1] == " "
                     ? "1"
                     : (cells[row][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row][column - 1]) + 1));
                  cells[row - 1][column - 1] = cells[row - 1][column - 1] == " "
                     ? "1"
                     : (cells[row - 1][column - 1] == "b" ? "b" : "" + (Integer.parseInt(cells[row - 1][column - 1]) + 1));
               }
            } else if (cells[row][column] == " ") {
               cells[row][column] = " ";
            }
         }
      }

      this.grid = cells;
   }

   public final String[][] getGrid() {
      return this.grid;
   }
}
