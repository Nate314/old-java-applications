package ultimateticktacktoe;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public final class CellButton extends JButton implements ActionListener {
   private SmallBoard board;
   private GameFrame game;
   private int boardIndex;
   private int cellIndex;
   private boolean clicked = false;
   private Color defaultColor = this.getBackground();
   private Color xColor = new Color(0, 0, 255);
   private Color oColor = new Color(255, 0, 0);

   public CellButton(SmallBoard board, GameFrame game, int boardIndex, int cellIndex) {
      this.board = board;
      this.game = game;
      this.cellIndex = cellIndex;
      this.boardIndex = boardIndex;
      this.setBackground(new Color(0, 255, 0));
      this.setFont(new Font("Times New Roman", 1, 55));
      this.setText("");
      this.setForeground(this.defaultColor);
      this.addActionListener(this);
   }

   @Override
   public final void actionPerformed(ActionEvent e) {
      if ((this.game.activeBoard == this.boardIndex || this.game.freeMove) && !this.clicked) {
         this.clicked = true;
         this.game.freeMove = false;
         this.game.activeBoard = this.cellIndex;

         for (int boardIdx = 0; boardIdx < this.game.boards.length; boardIdx++) {
            for (int cellIdx = 0; cellIdx < this.board.cells.length; cellIdx++) {
               this.game.boards[boardIdx].cells[cellIdx].setBackground(this.game.boards[boardIdx].backgroundColor);
            }
         }

         for (int cellIdx = 0; cellIdx < this.board.cells.length; cellIdx++) {
            this.game.activeBoard = this.cellIndex;
            this.game.boards[this.game.activeBoard].cells[cellIdx].setBackground(new Color(255, 255, 0));
         }

         this.game.checkWinner();
         if (this.getForeground() == this.defaultColor) {
            if (this.game.xTurn) {
               this.setText("X");
               this.setForeground(this.xColor);
               this.board.layoutCells();
            }

            if (!this.game.xTurn) {
               this.setText("O");
               this.setForeground(this.oColor);
               this.board.layoutCells();
            }

            this.game.xTurn = !this.game.xTurn;
         }

         String winner;
         if ((winner = this.board.checkWinner()) != "none") {
            if (winner == "X" && this.board.backgroundColor != new Color(0, 255, 0)) {
               for (int cellIdx = 0; cellIdx < this.board.cells.length; cellIdx++) {
                  this.board.backgroundColor = new Color(0, 255, 255);
                  this.board.cells[cellIdx].setBackground(this.board.backgroundColor);
               }

               return;
            }

            if (winner == "O" && this.board.backgroundColor != new Color(0, 255, 0)) {
               for (int cellIdx = 0; cellIdx < this.board.cells.length; cellIdx++) {
                  this.board.backgroundColor = new Color(255, 175, 175);
                  this.board.cells[cellIdx].setBackground(this.board.backgroundColor);
               }
            }
         }
      }
   }
}
