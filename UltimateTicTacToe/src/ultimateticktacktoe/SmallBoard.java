package ultimateticktacktoe;

import java.awt.Color;
import java.awt.Toolkit;
import javax.swing.JPanel;

public final class SmallBoard extends JPanel {
   public boolean unusedFlag = false;
   private String winner = "none";
   public Color backgroundColor = new Color(0, 255, 0);
   public CellButton[] cells = new CellButton[9];
   private int cellWidth;
   private int cellHeight;

   public SmallBoard(GameFrame game, int boardIndex) {
      int boardSize = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 3.5);
      this.setSize(boardSize, boardSize);
      this.setVisible(true);

      for (int cell = 0; cell < 9; cell++) {
         this.cells[cell] = new CellButton(this, game, boardIndex, cell);
      }

      this.cellWidth = this.getWidth() / 3;
      this.cellHeight = this.getHeight() / 3;
      this.setLayout(null);
      this.setBounds(0, 0, this.getWidth(), this.getHeight());
      this.setVisible(true);

      for (int cell = 0; cell < this.cells.length; cell++) {
         this.add(this.cells[cell]);
         this.cells[cell].setVisible(true);
      }

      this.layoutCells();

      for (int cell = 0; cell < this.cells.length; cell++) {
         this.cells[cell].setLocation(cell % 3 * this.cells[cell].getWidth(), cell / 3 * this.cells[cell].getHeight());
      }

      this.repaint();
   }

   public final void layoutCells() {
      this.cellWidth = this.getWidth() / 3;
      this.cellHeight = this.getHeight() / 3;
      this.cells[0].setBounds(0, 0, this.cellWidth, this.cellHeight);
      this.cells[1].setBounds(this.cellWidth, 0, this.cellWidth, this.cellHeight);
      this.cells[2].setBounds(2 * this.cellWidth, 0, this.cellWidth, this.cellHeight);
      this.cells[3].setBounds(0, this.cellHeight, this.cellWidth, this.cellHeight);
      this.cells[4].setBounds(this.cellWidth, this.cellHeight, this.cellWidth, this.cellHeight);
      this.cells[5].setBounds(2 * this.cellWidth, this.cellHeight, this.cellWidth, this.cellHeight);
      this.cells[6].setBounds(0, 2 * this.cellHeight, this.cellWidth, this.cellHeight);
      this.cells[7].setBounds(this.cellWidth, 2 * this.cellHeight, this.cellWidth, this.cellHeight);
      this.cells[8].setBounds(2 * this.cellWidth, 2 * this.cellHeight, this.cellWidth, this.cellHeight);
   }

   public final String checkWinner() {
      String[] symbols = new String[]{"X", "O"};
      String result = "none";
      if (this.winner == "none") {
         for (int symbol = 0; symbol < 2; symbol++) {
            if (this.cells[0].getText() == symbols[symbol] && this.cells[1].getText() == symbols[symbol] && this.cells[2].getText() == symbols[symbol]
               || this.cells[3].getText() == symbols[symbol] && this.cells[4].getText() == symbols[symbol] && this.cells[5].getText() == symbols[symbol]
               || this.cells[6].getText() == symbols[symbol] && this.cells[7].getText() == symbols[symbol] && this.cells[8].getText() == symbols[symbol]
               || this.cells[0].getText() == symbols[symbol] && this.cells[4].getText() == symbols[symbol] && this.cells[8].getText() == symbols[symbol]
               || this.cells[2].getText() == symbols[symbol] && this.cells[4].getText() == symbols[symbol] && this.cells[6].getText() == symbols[symbol]
               || this.cells[0].getText() == symbols[symbol] && this.cells[3].getText() == symbols[symbol] && this.cells[6].getText() == symbols[symbol]
               || this.cells[1].getText() == symbols[symbol] && this.cells[4].getText() == symbols[symbol] && this.cells[7].getText() == symbols[symbol]
               || this.cells[2].getText() == symbols[symbol] && this.cells[5].getText() == symbols[symbol] && this.cells[8].getText() == symbols[symbol]) {
               result = symbols[symbol];
            }

            if (this.cells[0].getText() != ""
               && this.cells[1].getText() != ""
               && this.cells[2].getText() != ""
               && this.cells[3].getText() != ""
               && this.cells[4].getText() != ""
               && this.cells[5].getText() != ""
               && this.cells[6].getText() != ""
               && this.cells[7].getText() != ""
               && this.cells[8].getText() != "") {
               result = "draw";
            }
         }
      }

      this.winner = result;
      return result;
   }
}
