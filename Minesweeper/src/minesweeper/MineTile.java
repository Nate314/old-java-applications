package minesweeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
   /** The tile's x index: it sits at {@code row * tileWidth}, so this is the column a player sees. */
   private int row;
   /** The tile's y index, the row a player sees. */
   private int column;
   private int mark = HIDDEN;
   /** The color for the tile's state; the keyboard cursor shows it darker. */
   private Color color;
   private boolean cursor;

   public MineTile(GameFrame game, int row, int column) {
      this.game = game;
      this.row = row;
      this.column = column;
      this.fill(this.hiddenColor);
      this.setText("");
      this.setForeground(new Color(255, 255, 255));
      this.setFocusPainted(false);
      this.addMouseListener(this);
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent event) {
            MineTile.this.keyPressed(event);
         }
      });
      this.addKeyListener(new WindowMoveKeyListener(game));
      this.describe("hidden");
   }

   /**
    * Keyboard play: Space or Enter reveals the tile, F (or the menu key, or Shift+F10) cycles its flag, and the arrow
    * keys move to the next tile. Shift+arrows still move the window (see WindowMoveKeyListener).
    */
   private void keyPressed(KeyEvent event) {
      int key = event.getKeyCode();
      if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_ENTER) {
         this.game.moveCursor(this);
         this.play(true);
      } else if (key == KeyEvent.VK_F || key == KeyEvent.VK_CONTEXT_MENU || key == KeyEvent.VK_F10 && event.isShiftDown()) {
         this.game.moveCursor(this);
         this.play(false);
      } else if (event.isShiftDown()) {
         return;
      } else if (key == KeyEvent.VK_LEFT) {
         this.game.focusTile(this.row - 1, this.column);
      } else if (key == KeyEvent.VK_RIGHT) {
         this.game.focusTile(this.row + 1, this.column);
      } else if (key == KeyEvent.VK_UP) {
         this.game.focusTile(this.row, this.column - 1);
      } else if (key == KeyEvent.VK_DOWN) {
         this.game.focusTile(this.row, this.column + 1);
      } else {
         return;
      }
      event.consume();
   }

   private void fill(Color color) {
      this.color = color;
      this.setBackground(this.cursor ? new Color(color.getRed() / 2, color.getGreen() / 2, color.getBlue() / 2) : color);
   }

   /** Shows or hides the keyboard cursor on this tile. */
   void setCursor(boolean on) {
      this.cursor = on;
      this.fill(this.color);
   }

   /** What a screen reader says for the tile: its state, then where it is ("3 mines near, row 2, column 5"). */
   private void describe(String state) {
      this.getAccessibleContext().setAccessibleName(state + ", row " + (this.column + 1) + ", column " + (this.row + 1));
   }

   /** The tile's x and y index, so a new game can put the keyboard cursor back on the same spot. */
   int[] spot() {
      return new int[]{this.row, this.column};
   }

   void showValue(String value) {
      this.mark = HIDDEN;
      this.game.setFlagged(this.row, this.column, false);
      this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
      this.fill(this.revealedColor);
      this.setText(value);
      this.describe(value.trim().isEmpty() ? "clear" : value.equals("1") ? "1 mine near" : value + " mines near");
   }

   void showMine() {
      this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
      this.fill(this.bombColor);
      this.setText("b");
      this.describe("mine");
   }

   /** Shows a mine as flagged once the game is won. */
   void showFlag() {
      this.mark = FLAG;
      this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
      this.fill(this.flagColor);
      this.setText("f");
      this.describe("flagged mine");
   }

   @Override
   public final void mouseClicked(MouseEvent event) {
      this.game.moveCursor(null);
      if (this.game.gameOver || event.getButton() == MouseEvent.BUTTON1) {
         this.play(true);
      } else if (event.getButton() == MouseEvent.BUTTON3) {
         this.play(false);
      }
   }

   /** A left click (reveal) or a right click (cycle the flag), from the mouse or the keyboard. */
   private void play(boolean reveal) {
      if (this.game.gameOver) {
         this.game.restart();
         return;
      }

      if (this.game.isRevealed(this.row, this.column)) {
         return;
      }

      if (reveal && this.mark != FLAG) {
         this.game.revealFrom(this.row, this.column);
      } else if (!reveal) {
         this.mark = (this.mark + 1) % 3;
         this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
         this.fill(this.mark == FLAG ? this.flagColor : this.mark == QUESTION ? this.questionColor : this.hiddenColor);
         this.setText(this.mark == FLAG ? "f" : this.mark == QUESTION ? "?" : "");
         this.describe(this.mark == FLAG ? "flag" : this.mark == QUESTION ? "question mark" : "hidden");
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
