package minesweeper;

import java.awt.Color;
import java.awt.Toolkit;
import java.util.List;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;

public final class GameFrame extends JFrame {
   private static final int MINE_COUNT = 18;
   public boolean gameOver = false;
   private int gridSize = 15;
   public int rows = this.gridSize;
   public int columns = this.gridSize;
   public SoundPlayer soundPlayer;
   public int revealNote = 60;
   public int revealNoteDirection = 1;
   public MineTile[][] tiles = new MineTile[this.rows][this.columns];
   private JPanel panel = new JPanel();
   private int tileWidth;
   private int tileHeight;
   /** Null until the first reveal, so the first click is never a mine. */
   private String[][] grid;
   private final boolean[][] revealed = new boolean[this.rows][this.columns];
   final boolean[][] flagged = new boolean[this.rows][this.columns];
   private int flagCount = 0;

   public GameFrame(SoundPlayer soundPlayer) {
      this.soundPlayer = soundPlayer;
      int windowSize = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 3.0 / 4.0);
      this.setSize(windowSize, windowSize);
      this.setDefaultCloseOperation(3);
      this.updateTitle();
      this.addKeyListener(new WindowMoveKeyListener(this));
      this.add(this.panel);
      this.tileWidth = this.getWidth() / this.rows;
      this.tileHeight = this.getHeight() / this.rows;
      this.panel.setBackground(new Color(0, 0, 0, 0));
      this.panel.setLayout(null);
      this.panel.setBounds(0, 0, this.getWidth(), this.getHeight());
      this.panel.setVisible(true);

      for (int row = 0; row < this.rows; row++) {
         for (int column = 0; column < this.columns; column++) {
            this.tiles[row][column] = new MineTile(this, row, column);
            this.panel.add(this.tiles[row][column]);
            this.tiles[row][column].setVisible(true);
            this.tiles[row][column].setBounds(row * this.tileWidth, column * this.tileHeight, this.tileWidth, this.tileHeight);
         }
      }

      this.setSize(this.getWidth() + this.tileWidth, this.getHeight() + this.tileHeight);
      this.setVisible(true);
   }

   /** Left click on a tile: reveals it (and a blank region), or ends the game on a mine. */
   void revealFrom(int row, int column) {
      if (this.grid == null) {
         this.grid = new MineFieldGenerator(this.rows, this.columns, MINE_COUNT, row, column, new Random()).getGrid();
      }

      if (MineFieldGenerator.isMine(this.grid, row, column)) {
         for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.columns; c++) {
               if (MineFieldGenerator.isMine(this.grid, r, c)) {
                  this.tiles[r][c].showMine();
               }
            }
         }
         int[][] gameOverChords = new int[][]{{49, 67, 71, 75, 78}, {48, 67, 70, 73, 76}};
         this.soundPlayer.playChord(gameOverChords[new Random().nextInt(2)]);
         this.gameOver = true;
         this.setTitle("Minesweeper: you hit a mine. Click to play again.");
         return;
      }

      List<int[]> cells = MineFieldGenerator.reveal(this.grid, this.revealed, this.flagged, row, column);
      for (int[] cell : cells) {
         this.tiles[cell[0]][cell[1]].showValue(this.grid[cell[0]][cell[1]]);
         this.soundPlayer.queueNote(this.revealNote = this.revealNote + this.revealNoteDirection * 6);
         if (this.revealNote > 100) {
            this.revealNoteDirection = -1;
         } else if (this.revealNote < 25) {
            this.revealNoteDirection = 1;
         }
      }

      if (MineFieldGenerator.isWon(this.grid, this.revealed)) {
         this.gameOver = true;
         this.setTitle("Minesweeper: you win! Click to play again.");
      }
   }

   boolean isRevealed(int row, int column) {
      return this.revealed[row][column];
   }

   void setFlagged(int row, int column, boolean flag) {
      if (this.flagged[row][column] != flag) {
         this.flagged[row][column] = flag;
         this.flagCount += flag ? 1 : -1;
         this.updateTitle();
      }
   }

   void restart() {
      this.dispose();
      new GameFrame(this.soundPlayer);
   }

   private void updateTitle() {
      this.setTitle("Minesweeper: " + (MINE_COUNT - this.flagCount) + " mines left");
   }
}
