package minesweeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;

public final class MineTile extends JButton implements MouseListener {
   private static final int HIDDEN = 0;
   private static final int FLAG = 1;
   private static final int QUESTION = 2;
   private GameFrame game;
   private Color hiddenColor = new Color(0, 255, 0);
   private Color revealedColor = new Color(0, 0, 255);
   private Color bombColor = new Color(255, 0, 0);
   private Color flagColor = new Color(0, 200, 255);
   private Color questionColor = new Color(255, 0, 255);
   private int row;
   private int column;
   private int mark = HIDDEN;

   public MineTile(GameFrame game, int row, int column) {
      this.game = game;
      this.row = row;
      this.column = column;
      this.setBackground(this.hiddenColor);
      this.setText("");
      this.setForeground(new Color(255, 255, 255));
      this.setFocusPainted(false);
      this.addMouseListener(this);
      this.addKeyListener(new WindowMoveKeyListener(game));
   }

   void showValue(String value) {
      this.mark = HIDDEN;
      this.game.setFlagged(this.row, this.column, false);
      this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
      this.setBackground(this.revealedColor);
      this.setText(value);
   }

   void showMine() {
      this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
      this.setBackground(this.bombColor);
      this.setText("b");
   }

   /** Shows a mine as flagged once the game is won. */
   void showFlag() {
      this.mark = FLAG;
      this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
      this.setBackground(this.flagColor);
      this.setText("f");
   }

   @Override
   public final void mouseClicked(MouseEvent event) {
      if (this.game.gameOver) {
         this.game.restart();
         return;
      }

      if (this.game.isRevealed(this.row, this.column)) {
         return;
      }

      if (event.getButton() == MouseEvent.BUTTON1 && this.mark != FLAG) {
         this.game.revealFrom(this.row, this.column);
      } else if (event.getButton() == MouseEvent.BUTTON3) {
         this.mark = (this.mark + 1) % 3;
         this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
         this.setBackground(this.mark == FLAG ? this.flagColor : this.mark == QUESTION ? this.questionColor : this.hiddenColor);
         this.setText(this.mark == FLAG ? "f" : this.mark == QUESTION ? "?" : "");
         this.game.setFlagged(this.row, this.column, this.mark == FLAG);
      }
   }

   @Override
   public final void mousePressed(MouseEvent event) {
   }

   @Override
   public final void mouseReleased(MouseEvent event) {
   }

   @Override
   public final void mouseEntered(MouseEvent event) {
   }

   @Override
   public final void mouseExited(MouseEvent event) {
   }
}
