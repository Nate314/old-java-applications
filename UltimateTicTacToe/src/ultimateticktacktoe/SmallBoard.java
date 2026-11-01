package ultimateticktacktoe;

import java.awt.Font;
import java.awt.Toolkit;
import javax.swing.JPanel;

public final class SmallBoard extends JPanel {
   public CellButton[] cells = new CellButton[9];
   private int cellWidth;
   private int cellHeight;

   public SmallBoard(GameFrame game, int boardIndex) {
      int boardSize = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 3.5);
      this.setSize(boardSize, boardSize);
      this.setLayout(null);

      for (int cell = 0; cell < 9; cell++) {
         this.cells[cell] = new CellButton(game, boardIndex, cell);
         this.add(this.cells[cell]);
      }

      this.layoutCells();
   }

   public final void layoutCells() {
      this.cellWidth = this.getWidth() / 3;
      this.cellHeight = this.getHeight() / 3;
      // Marks scale with the cell (the original fixed size, 55, suited a 1080p screen).
      Font font = new Font("Times New Roman", Font.BOLD, Math.max(12, this.cellHeight * 2 / 3));
      for (int cell = 0; cell < this.cells.length; cell++) {
         this.cells[cell].setFont(font);
         this.cells[cell].setBounds(cell % 3 * this.cellWidth, cell / 3 * this.cellHeight, this.cellWidth, this.cellHeight);
      }
   }
}
