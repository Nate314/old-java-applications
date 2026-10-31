package ultimateticktacktoe;

import java.awt.Toolkit;
import javax.swing.JFrame;

public final class GameFrame extends JFrame {
   public int activeBoard = 2;
   public boolean freeMove = true;
   public boolean xTurn = true;
   public SmallBoard[] boards = new SmallBoard[]{
      new SmallBoard(this, 0), new SmallBoard(this, 1), new SmallBoard(this, 2), new SmallBoard(this, 3), new SmallBoard(this, 4), new SmallBoard(this, 5), new SmallBoard(this, 6), new SmallBoard(this, 7), new SmallBoard(this, 8)
   };

   public GameFrame() {
      this.setTitle("AWT Ultimate Tick Tack Toe");
      this.setDefaultCloseOperation(3);
      this.setSize(Toolkit.getDefaultToolkit().getScreenSize().height * 3 / 4, Toolkit.getDefaultToolkit().getScreenSize().height * 3 / 4);
      this.setVisible(true);
      this.setLayout(null);
      this.setResizable(false);
      int boardWidth = this.getWidth() / 3;
      int boardHeight = this.getHeight() / 3;

      for (int board = 0; board < this.boards.length; board++) {
         this.boards[board].setBounds(board % 3 * boardWidth, board / 3 * boardHeight, boardWidth - 2, boardHeight - 2);
         this.add(this.boards[board]);
         this.boards[board].layoutCells();
      }

      this.setSize(this.getWidth() + 50, this.getHeight() + 50);
   }

   public final String checkWinner() {
      String[] symbols = new String[]{"X", "O"};
      String result = "none";

      for (int symbol = 0; symbol < 2; symbol++) {
         if (this.boards[0].cells[0].getText() == symbols[symbol] && this.boards[1].cells[0].getText() == symbols[symbol] && this.boards[2].cells[0].getText() == symbols[symbol]
            || this.boards[3].cells[0].getText() == symbols[symbol] && this.boards[4].cells[0].getText() == symbols[symbol] && this.boards[5].cells[0].getText() == symbols[symbol]
            || this.boards[6].cells[0].getText() == symbols[symbol] && this.boards[7].cells[0].getText() == symbols[symbol] && this.boards[8].cells[0].getText() == symbols[symbol]
            || this.boards[0].cells[0].getText() == symbols[symbol] && this.boards[4].cells[0].getText() == symbols[symbol] && this.boards[8].cells[0].getText() == symbols[symbol]
            || this.boards[2].cells[0].getText() == symbols[symbol] && this.boards[4].cells[0].getText() == symbols[symbol] && this.boards[6].cells[0].getText() == symbols[symbol]
            || this.boards[0].cells[0].getText() == symbols[symbol] && this.boards[3].cells[0].getText() == symbols[symbol] && this.boards[6].cells[0].getText() == symbols[symbol]
            || this.boards[1].cells[0].getText() == symbols[symbol] && this.boards[4].cells[0].getText() == symbols[symbol] && this.boards[7].cells[0].getText() == symbols[symbol]
            || this.boards[2].cells[0].getText() == symbols[symbol] && this.boards[5].cells[0].getText() == symbols[symbol] && this.boards[8].cells[0].getText() == symbols[symbol]) {
            result = symbols[symbol];
         }
      }

      return result;
   }
}
