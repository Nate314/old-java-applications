package minesweeper;

import java.awt.Color;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;

public final class GameFrame extends JFrame {
   public boolean gameOver = false;
   private int gridSize = 15;
   public int rows = this.gridSize;
   public int columns = this.gridSize;
   public SoundPlayer soundPlayer = new SoundPlayer();
   public int revealNote = 60;
   public int revealNoteDirection = 1;
   public MineTile[][] tiles = new MineTile[this.rows][this.columns];
   private JPanel panel = new JPanel();
   private int tileWidth;
   private int tileHeight;
   public boolean[][] visited = new boolean[this.rows][this.columns];

   public GameFrame() {
      int windowSize = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 3.0 / 4.0);
      this.setSize(windowSize, windowSize);
      this.setDefaultCloseOperation(3);
      this.setTitle("AWT Minesweeper");
      this.setVisible(true);
      this.addKeyListener(new WindowMoveKeyListener(this));
      this.add(this.panel);
      this.tileWidth = this.getWidth() / this.rows;
      this.tileHeight = this.getHeight() / this.rows;
      this.panel.setBackground(new Color(0, 0, 0, 0));
      this.panel.setLayout(null);
      this.panel.setBounds(0, 0, this.getWidth(), this.getHeight());
      this.panel.setVisible(true);

      try {
         String[][] grid = new MineFieldGenerator(this.rows, this.columns).getGrid();

         for (int row = 0; row < this.rows; row++) {
            for (int column = 0; column < this.columns; column++) {
               this.tiles[row][column] = new MineTile(this, grid[row][column], row, column);
               this.panel.add(this.tiles[row][column]);
               this.tiles[row][column].setVisible(true);
               this.tiles[row][column].setBounds(row * this.tileWidth, column * this.tileHeight, this.tileWidth, this.tileHeight);
               this.visited[row][column] = false;
            }
         }

         this.setSize(this.getWidth() + this.tileWidth, this.getHeight() + this.tileHeight);
         this.repaint();
      } catch (NumberFormatException exception) {
         this.setVisible(false);
         new GameFrame();
      }
   }
}
