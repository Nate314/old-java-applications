package minesweeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Random;
import javax.swing.JButton;

public final class MineTile extends JButton implements MouseListener {
   private boolean unused = false;
   private GameFrame game;
   private Color hiddenColor = new Color(0, 255, 0);
   private Color revealedColor = new Color(0, 0, 255);
   private Color bombColor = new Color(255, 0, 0);
   private Color flagColor = new Color(0, 200, 255);
   private Color questionColor = new Color(255, 0, 255);
   private String value;
   private int row;
   private int column;
   private final int[] nearbyRows = new int[3];
   private final int[] nearbyColumns = new int[3];
   private final int[] neighborRows = new int[8];
   private final int[] neighborColumns = new int[8];

   public MineTile(GameFrame game, String value, int row, int column) {
      this.game = game;
      this.value = value;
      this.row = row;
      this.column = column;
      this.setBackground(this.hiddenColor);
      this.setText("");
      this.setForeground(new Color(255, 255, 255));
      this.addMouseListener(this);
      this.addKeyListener(new WindowMoveKeyListener(game));
      this.nearbyRows[0] = row - 1 < 0 ? -1 : row - 1;
      this.nearbyRows[1] = row;
      this.nearbyRows[2] = row + 1 > game.rows - 1 ? -1 : row + 1;
      this.nearbyColumns[0] = column - 1 < 0 ? -1 : column - 1;
      this.nearbyColumns[1] = column;
      this.nearbyColumns[2] = column + 1 > game.columns - 1 ? -1 : column + 1;

      for (int i = 0; i < 3; i++) {
         if (this.nearbyRows[i] == -1 || this.nearbyColumns[i] == -1) {
            this.nearbyRows[i] = -1;
            this.nearbyColumns[i] = -1;
         }
      }

      for (int i = 0; i < 8; i++) {
         this.neighborRows[i] = this.nearbyRows[i / 3];
         this.neighborColumns[i] = this.nearbyColumns[i % 3];
      }
   }

   private void revealWithNeighbors(MouseEvent event) {
      if (this.value == " ") {
         for (int i = 0; i < 8; i++) {
            if (this.neighborRows[i] != -1
               && this.neighborColumns[i] != -1
               && this.game.tiles[this.neighborRows[i]][this.neighborColumns[i]].getBackground() != this.revealedColor
               && !this.game.visited[this.neighborRows[i]][this.neighborColumns[i]]) {
               this.game.tiles[this.neighborRows[i]][this.neighborColumns[i]].mouseClicked(event);
               this.game.visited[this.neighborRows[i]][this.neighborColumns[i]] = true;
            }
         }
      }

      this.reveal();
   }

   private void reveal() {
      if (this.value != "b") {
         this.game.soundPlayer.playNote(this.game.revealNote = this.game.revealNote + this.game.revealNoteDirection * 6, 20, 2, 0, 0);

         try {
            Thread.sleep(2L);
         } catch (InterruptedException exception) {
            exception.printStackTrace();
         }

         if (this.game.revealNote > 100) {
            this.game.revealNoteDirection = -1;
         } else if (this.game.revealNote < 25) {
            this.game.revealNoteDirection = 1;
         }

         this.setBackground(this.revealedColor);
         this.setText(this.value);
      } else {
         this.setBackground(this.bombColor);
         this.setText(this.value);

         for (int i = 0; i < this.game.rows * this.game.columns - 1; i++) {
            if (this.game.tiles[i / this.game.rows][i % this.game.rows].value == "b") {
               this.game.tiles[i / this.game.rows][i % this.game.rows].setBackground(this.bombColor);
               this.game.tiles[i / this.game.rows][i % this.game.rows].setText(this.game.tiles[i / this.game.rows][i % this.game.rows].value);
            }
         }

         int[][] gameOverChords = new int[][]{{49, 67, 71, 75, 78}, {48, 67, 70, 73, 76}};
         this.game.soundPlayer.playChord(gameOverChords[new Random().nextInt(2)], 750, 10, 127, 4096);
         this.game.gameOver = true;
      }
   }

   @Override
   public final void mouseClicked(MouseEvent event) {
      this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
      if (!this.game.gameOver) {
         this.game.visited[this.row][this.column] = true;
         if (event.getButton() == 1) {
            if (this.value == " ") {
               this.revealWithNeighbors(event);
            } else {
               this.reveal();
            }
         }

         if (event.getButton() == 3) {
            this.setBackground(this.getBackground() == this.hiddenColor ? this.flagColor : (this.getBackground() == this.flagColor ? this.questionColor : this.hiddenColor));
            this.setText(this.getText() != "f" && this.getText() != "?" ? "f" : (this.getText() == "f" ? "?" : " "));
         }
      } else {
         this.game.dispose();
         new GameFrame();
      }

      String boardState = "";
      String allRevealedState = "";

      for (int boardRow = 0; boardRow < this.game.rows; boardRow++) {
         for (int boardColumn = 0; boardColumn < this.game.columns; boardColumn++) {
            boardState = boardState + (this.game.tiles[boardRow][boardColumn].getText() == "" ? "0" : "1");
            allRevealedState = allRevealedState + "1";
         }
      }

      if (boardState.equals(allRevealedState)) {
         this.game.gameOver = true;
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
