package ultimateticktacktoe;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public final class CellButton extends JButton implements ActionListener {
   private GameFrame game;
   private int boardIndex;
   private int cellIndex;
   private Color xColor = new Color(0, 0, 255);
   private Color oColor = new Color(255, 0, 0);

   public CellButton(GameFrame game, int boardIndex, int cellIndex) {
      this.game = game;
      this.cellIndex = cellIndex;
      this.boardIndex = boardIndex;
      this.setBackground(new Color(0, 255, 0));
      this.setFont(new Font("Times New Roman", 1, 55));
      this.setText("");
      this.setFocusPainted(false);
      this.addActionListener(this);
   }

   /** Where a board sits in the big grid, or a square in its board, read out by screen readers. */
   private static final String[] PLACES = {"top left", "top", "top right", "left", "middle", "right", "bottom left", "bottom", "bottom right"};

   /**
    * Shows a mark ("", "X" or "O") on the given board color. A screen reader reads the mark, whether the next move
    * may go there, and where the square is ("empty, open, top left board, middle square"; "O, last move, ...").
    */
   final void show(String mark, Color background, boolean open, boolean last) {
      this.setText(mark);
      this.setForeground("X".equals(mark) ? this.xColor : this.oColor);
      this.setBackground(background);
      this.getAccessibleContext().setAccessibleName((mark.isEmpty() ? "empty" + (open ? ", open" : "") : mark + (last ? ", last move" : ""))
         + ", " + PLACES[this.boardIndex] + " board, " + PLACES[this.cellIndex] + " square");
   }

   @Override
   public final void actionPerformed(ActionEvent e) {
      this.game.cellClicked(this.boardIndex, this.cellIndex);
   }
}
