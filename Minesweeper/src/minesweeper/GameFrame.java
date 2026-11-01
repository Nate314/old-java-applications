package minesweeper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public final class GameFrame extends JFrame {
   private static final File BEST_TIMES_FILE = new File(System.getProperty("user.home", "."), ".minesweeper-best.txt");
   /** Room for the level buttons and the status line, taken from the board so the window keeps its old size. */
   private static final int BARS_HEIGHT = 64;
   private static final Color SELECTED_LEVEL = new Color(0, 255, 0);
   private static final int[] WIN_NOTES = {72, 76, 79, 84, 88, 91, 96};
   public boolean gameOver = false;
   private final Level level;
   private final BestTimes bestTimes;
   public int rows;
   public int columns;
   public SoundPlayer soundPlayer;
   public int revealNote = 60;
   public int revealNoteDirection = 1;
   public MineTile[][] tiles;
   private JPanel panel = new JPanel();
   private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
   private final Timer clock = new Timer(1000, event -> this.updateStatus());
   private int tileWidth;
   private int tileHeight;
   /** Null until the first reveal, so the first click is never a mine. */
   private String[][] grid;
   private final boolean[][] revealed;
   final boolean[][] flagged;
   private int flagCount = 0;
   /** When the first tile was revealed (the clock starts then), or 0 before that. */
   private long startMillis;
   private long finishMillis;

   public GameFrame(SoundPlayer soundPlayer) {
      this(soundPlayer, null, null);
   }

   /**
    * Without best times, reads them from the file. That happens here, after JFrame's constructor, because the browser
    * build restores saved files when the first component is created.
    */
   private GameFrame(SoundPlayer soundPlayer, BestTimes bestTimes, Point location) {
      this.soundPlayer = soundPlayer;
      this.bestTimes = bestTimes != null ? bestTimes : BestTimes.load(BEST_TIMES_FILE);
      this.level = this.bestTimes.lastLevel();
      this.rows = this.columns = this.level.size;
      this.tiles = new MineTile[this.rows][this.columns];
      this.revealed = new boolean[this.rows][this.columns];
      this.flagged = new boolean[this.rows][this.columns];
      int windowSize = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 3.0 / 4.0);
      this.tileWidth = this.tileHeight = Math.max(16, (windowSize - BARS_HEIGHT) / this.rows);
      this.setDefaultCloseOperation(3);
      this.addKeyListener(new WindowMoveKeyListener(this));
      this.add(this.levelBar(), BorderLayout.NORTH);
      this.status.setFont(new Font("Dialog", Font.BOLD, 14));
      this.add(this.status, BorderLayout.SOUTH);
      // Centered, for when the buttons or the status line are wider than a small board.
      JPanel boardHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
      boardHolder.add(this.panel);
      this.add(boardHolder, BorderLayout.CENTER);
      this.panel.setBackground(new Color(0, 0, 0, 0));
      this.panel.setLayout(null);
      this.panel.setPreferredSize(new Dimension(this.rows * this.tileWidth, this.columns * this.tileHeight));

      for (int row = 0; row < this.rows; row++) {
         for (int column = 0; column < this.columns; column++) {
            this.tiles[row][column] = new MineTile(this, row, column);
            this.panel.add(this.tiles[row][column]);
            this.tiles[row][column].setVisible(true);
            this.tiles[row][column].setBounds(row * this.tileWidth, column * this.tileHeight, this.tileWidth, this.tileHeight);
         }
      }

      this.updateStatus();
      this.pack();
      if (location != null) {
         this.setLocation(location);
      }
      this.setVisible(true);
   }

   /** One button per level; the current one is lit. Not focusable, so the arrow keys keep moving the window. */
   private JPanel levelBar() {
      JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
      for (Level choice : Level.values()) {
         JButton button = new JButton(choice.label);
         button.setFocusable(false);
         if (choice == this.level) {
            button.setBackground(SELECTED_LEVEL);
            button.setForeground(Color.BLACK);
         }
         button.addActionListener(event -> this.play(choice));
         bar.add(button);
      }
      return bar;
   }

   /** Left click on a tile: reveals it (and a blank region), or ends the game on a mine. */
   void revealFrom(int row, int column) {
      if (this.grid == null) {
         this.grid = new MineFieldGenerator(this.rows, this.columns, this.level.mines, row, column, new Random()).getGrid();
         this.startMillis = System.currentTimeMillis();
         this.clock.start();
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
         this.finish();
         this.setTitle("Minesweeper: you hit a mine. Click to play again.");
         this.status.setText("Boom! Click any tile to play again.");
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
         this.finish();
         for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.columns; c++) {
               if (MineFieldGenerator.isMine(this.grid, r, c)) {
                  this.tiles[r][c].showFlag();
               }
            }
         }
         long time = this.finishMillis - this.startMillis;
         long before = this.bestTimes.best(this.level);
         boolean record = this.bestTimes.record(this.level, time);
         for (int note : WIN_NOTES) {
            this.soundPlayer.queueNote(note);
         }
         this.setTitle("Minesweeper: you win in " + BestTimes.format(time) + "! Click to play again.");
         this.status.setText("You win in " + BestTimes.format(time) + "! "
            + (!record ? "Best: " + BestTimes.format(before) : before == 0 ? "First " + this.level.label + " win." : "New best!"));
      }
   }

   private void finish() {
      this.gameOver = true;
      this.finishMillis = System.currentTimeMillis();
      this.clock.stop();
   }

   boolean isRevealed(int row, int column) {
      return this.revealed[row][column];
   }

   void setFlagged(int row, int column, boolean flag) {
      if (this.flagged[row][column] != flag) {
         this.flagged[row][column] = flag;
         this.flagCount += flag ? 1 : -1;
         this.updateStatus();
      }
   }

   /** A new game on the same level, where this window was. */
   void restart() {
      this.play(this.level);
   }

   /** A new game on that level, which is remembered for the next start. */
   void play(Level choice) {
      this.bestTimes.setLastLevel(choice);
      this.dispose();
      new GameFrame(this.soundPlayer, this.bestTimes, this.getLocation());
   }

   @Override
   public void dispose() {
      this.clock.stop();
      super.dispose();
   }

   private void updateStatus() {
      if (this.gameOver) {
         return;
      }
      int minesLeft = this.level.mines - this.flagCount;
      long elapsed = this.startMillis == 0 ? 0 : System.currentTimeMillis() - this.startMillis;
      long best = this.bestTimes.best(this.level);
      this.setTitle("Minesweeper: " + minesLeft + " mines left");
      this.status.setText(minesLeft + " mines left    Time " + elapsed / 1000 / 60 + ":" + (elapsed / 1000 % 60 < 10 ? "0" : "")
         + elapsed / 1000 % 60 + "    Best " + (best == 0 ? "none yet" : BestTimes.format(best)));
   }
}
