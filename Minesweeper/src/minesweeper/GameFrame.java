package minesweeper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
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
   /** Room for the level buttons, the status line and the two hint lines, taken from the board so the window keeps its old size. */
   private static final int BARS_HEIGHT = 104;
   /** How long dealing a no-guess board may take before a random one is played instead (it takes a few ms). */
   private static final int DEAL_BUDGET_MS = 1500;
   private static final Color SELECTED_LEVEL = MineTile.GRASS_LIGHT;
   private static final int[] WIN_NOTES = {72, 76, 79, 84, 88, 91, 96};
   /** An opening ripples out from the click: each ring of tiles pops this many ms after the one inside it. */
   private static final int RIPPLE_MS = 22;
   /** Mines burst in this many ms per tile of distance from the one hit; a win's flags plant at the same pace. */
   private static final int BURST_STEP_MS = 45;
   /** Watch: the computer makes a move this often, and starts the next game this long after one ends. */
   private static final int WATCH_STEP_MS = 280;
   private static final int WATCH_NEXT_GAME_MS = 2600;
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
   /** Tiles with an animation running, repainted every frame until they are done. */
   private final List<MineTile> animating = new ArrayList<>();
   private final Timer animation = new Timer(16, event -> this.animate());
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
   /** The tile the keyboard is on, shown darker, or null while playing with the mouse. */
   private MineTile cursor;
   /** Watch (W): the computer is playing this board. */
   private boolean watching;
   /** The computer made a move in this game, so its time is not a best time. */
   private boolean computerPlayed;
   private final Timer watchTimer = new Timer(WATCH_STEP_MS, event -> this.watchStep());
   private final Random random = new Random();
   /** Under the status line: what a hint (or the computer) worked out and why, or a tip between hints. */
   private final JLabel hintAction = new JLabel(" ", SwingConstants.CENTER);
   private final JLabel hintReason = new JLabel(" ", SwingConstants.CENTER);
   /** Tiles a hint marked, cleared by the next move. */
   private final List<MineTile> hinted = new ArrayList<>();
   /** This board was dealt to need no guess (No guess was on and one turned up in time). */
   private boolean fairBoard;
   /** Hints shown in this game; a game won with hints sets no best time. */
   private int hints;
   /** Moves made in this game, so asking twice for the same hint counts once. */
   private int moves;
   private int hintedAtMove = -1;

   public GameFrame(SoundPlayer soundPlayer) {
      this(soundPlayer, null, null, null, false);
   }

   /**
    * Without best times, reads them from the file. That happens here, after JFrame's constructor, because the browser
    * build restores saved files when the first component is created.
    */
   private GameFrame(SoundPlayer soundPlayer, BestTimes bestTimes, Point location, int[] cursorSpot, boolean watching) {
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
      JPanel lines = new JPanel(new BorderLayout());
      this.hintAction.setFont(new Font("Dialog", Font.BOLD, 13));
      this.hintReason.setFont(new Font("Dialog", Font.PLAIN, 13));
      lines.add(this.status, BorderLayout.NORTH);
      lines.add(this.hintAction, BorderLayout.CENTER);
      lines.add(this.hintReason, BorderLayout.SOUTH);
      this.add(lines, BorderLayout.SOUTH);
      this.showTip();
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
      if (cursorSpot != null) { // a new game started from the keyboard keeps the cursor where it was
         this.focusTile(Math.min(cursorSpot[0], this.rows - 1), Math.min(cursorSpot[1], this.columns - 1));
      }
      if (watching) {
         this.toggleWatch();
      }
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
      JButton noGuess = new JButton("No guess");
      noGuess.setFocusable(false);
      noGuess.setToolTipText("Deal boards that logic alone can clear (G)");
      if (this.bestTimes.noGuess()) {
         noGuess.setBackground(SELECTED_LEVEL);
         noGuess.setForeground(Color.BLACK);
      }
      noGuess.addActionListener(event -> this.toggleNoGuess());
      bar.add(noGuess);
      JButton hint = new JButton("Hint");
      hint.setFocusable(false);
      hint.setToolTipText("Show the next tile that can be worked out, and why (H)");
      hint.addActionListener(event -> this.hint());
      bar.add(hint);
      return bar;
   }

   /** Left click on a tile: reveals it (and a blank region), or ends the game on a mine. */
   void revealFrom(int row, int column) {
      this.reveal(row, column, row, column);
   }

   /**
    * Left click (or Space) on a revealed number: once its flags add up, reveals its other hidden neighbors, which may
    * end the game if a flag was wrong. While they do not add up, those neighbors glow briefly.
    */
   void chord(int row, int column) {
      this.clearHint();
      List<int[]> targets = MineFieldGenerator.chordTargets(this.grid, this.revealed, this.flagged, row, column);
      if (targets.isEmpty()) {
         for (int[] cell : MineFieldGenerator.hiddenNeighbors(this.revealed, this.flagged, row, column)) {
            this.tiles[cell[0]][cell[1]].pulse();
         }
         return;
      }
      for (int[] cell : targets) {
         if (this.gameOver) {
            return;
         }
         this.reveal(cell[0], cell[1], row, column);
      }
   }

   /** Reveals (row, column); the tiles ripple out from (fromRow, fromColumn), the tile that was clicked. */
   private void reveal(int row, int column, int fromRow, int fromColumn) {
      if (this.grid == null) {
         if (this.bestTimes.noGuess()) {
            this.grid = FairBoard.deal(this.rows, this.columns, this.level.mines, row, column, new Random(), DEAL_BUDGET_MS);
            this.fairBoard = this.grid != null;
         }
         if (this.grid == null) {
            this.grid = new MineFieldGenerator(this.rows, this.columns, this.level.mines, row, column, new Random()).getGrid();
         }
         this.startMillis = System.currentTimeMillis();
         this.clock.start();
      }

      if (this.revealed[row][column]) {
         return;
      }

      this.clearHint();
      this.moves++;
      if (MineFieldGenerator.isMine(this.grid, row, column)) {
         // What could have been worked out instead, read from the board before this tile.
         Logic.Deduction instead = this.watching ? null : Logic.hint(this.view(), this.flagged, this.level.mines, true);
         for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.columns; c++) {
               int delay = BURST_STEP_MS * distance(r, c, row, column);
               if (MineFieldGenerator.isMine(this.grid, r, c)) {
                  this.tiles[r][c].showMine(r == row && c == column, delay);
               } else if (this.tiles[r][c].isFlagged()) {
                  this.tiles[r][c].showWrongFlag(delay);
               }
            }
         }
         int[][] gameOverChords = new int[][]{{49, 67, 71, 75, 78}, {48, 67, 70, 73, 76}};
         this.soundPlayer.playChord(gameOverChords[new Random().nextInt(2)]);
         this.finish();
         if (this.watching) {
            this.setTitle("Minesweeper: the computer hit a mine.");
            this.status.setText("Boom! The computer had to guess. Next game...");
         } else {
            this.setTitle("Minesweeper: you hit a mine. Click to play again.");
            this.status.setText("Boom! Click any tile to play again.");
            if (instead != null) {
               this.showDeduction(instead, "No need to guess: the glowing tiles were safe.");
            } else {
               this.setHintLines("That one was a guess.", this.bestTimes.noGuess() ? " " : "No guess (G) deals boards logic can clear.");
            }
         }
         return;
      }

      List<int[]> cells = MineFieldGenerator.reveal(this.grid, this.revealed, this.flagged, row, column);
      for (int[] cell : cells) {
         this.tiles[cell[0]][cell[1]].showValue(this.grid[cell[0]][cell[1]], RIPPLE_MS * distance(cell[0], cell[1], fromRow, fromColumn));
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
                  this.tiles[r][c].showFlag(BURST_STEP_MS * distance(r, c, row, column));
               }
            }
         }
         long time = this.finishMillis - this.startMillis;
         for (int note : WIN_NOTES) {
            this.soundPlayer.queueNote(note);
         }
         if (!this.computerPlayed) {
            this.setHintLines(this.hints > 0 ? "Cleared with " + this.hints + (this.hints == 1 ? " hint." : " hints.")
               : this.fairBoard ? "Cleared by logic alone, no guesses." : "Cleared!",
               this.hints > 0 ? "A game won with hints sets no best time." : " ");
         }
         if (this.computerPlayed) {
            // Not a best time: the computer played some or all of it.
            String who = this.watching ? "The computer" : "You and the computer";
            this.setTitle("Minesweeper: " + who.toLowerCase() + " cleared it in " + BestTimes.format(time) + ".");
            this.status.setText(who + " cleared it in " + BestTimes.format(time) + (this.watching ? ". Next game..." : ". Click to play again."));
            return;
         }
         if (this.hints > 0) {
            this.setTitle("Minesweeper: you win in " + BestTimes.format(time) + "! Click to play again.");
            this.status.setText("You win in " + BestTimes.format(time) + " with hints. Click to play again.");
            return;
         }
         long before = this.bestTimes.best(this.level);
         boolean record = this.bestTimes.record(this.level, time);
         this.setTitle("Minesweeper: you win in " + BestTimes.format(time) + "! Click to play again.");
         this.status.setText("You win in " + BestTimes.format(time) + "! "
            + (!record ? "Best: " + BestTimes.format(before) : before == 0 ? "First " + this.level.label + " win." : "New best!"));
      }
   }

   /** Steps between two tiles, counting a diagonal step as one, so equal distances form rings around a tile. */
   private static int distance(int row, int column, int otherRow, int otherColumn) {
      return Math.max(Math.abs(row - otherRow), Math.abs(column - otherColumn));
   }

   /** Keeps repainting that tile every frame until its animation is done. */
   void animating(MineTile tile) {
      if (!this.animating.contains(tile)) {
         this.animating.add(tile);
      }
      tile.repaint();
      if (!this.animation.isRunning()) {
         this.animation.start();
      }
   }

   private void animate() {
      long now = System.currentTimeMillis();
      for (int i = this.animating.size() - 1; i >= 0; i--) {
         MineTile tile = this.animating.get(i);
         tile.repaint();
         if (!tile.isAnimating(now)) {
            this.animating.remove(i);
         }
      }
      if (this.animating.isEmpty()) {
         this.animation.stop();
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
         this.moves++;
         this.clearHint();
         this.updateStatus();
      }
   }

   /** Moves the keyboard cursor (and the focus) to the tile at that x and y index, if there is one. */
   void focusTile(int x, int y) {
      if (x >= 0 && y >= 0 && x < this.rows && y < this.columns) {
         this.moveCursor(this.tiles[x][y]);
         this.tiles[x][y].requestFocusInWindow();
      }
   }

   /** Shows the keyboard cursor on that tile, or nowhere (null) once the mouse is used. */
   void moveCursor(MineTile tile) {
      if (this.cursor != null) {
         this.cursor.setCursor(false);
      }
      this.cursor = tile;
      if (tile != null) {
         tile.setCursor(true);
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
      new GameFrame(this.soundPlayer, this.bestTimes, this.getLocation(), this.cursor == null ? null : this.cursor.spot(), this.watching);
   }

   /** W: the computer plays this board (and the games after it) until W again, a click or a key on a tile. */
   void toggleWatch() {
      this.watching = !this.watching;
      if (this.watching) {
         this.watchTimer.start();
      } else {
         this.watchTimer.stop();
         this.moveCursor(null);
      }
      this.updateStatus();
   }

   /** The player clicked or pressed a tile's key: the computer stops and the board is theirs. */
   void takeOver() {
      if (this.watching) {
         this.toggleWatch();
      }
   }

   boolean isWatching() {
      return this.watching;
   }

   /** One move of the computer's, shown with the cursor ring; after a game, the next one. */
   private void watchStep() {
      if (this.gameOver) {
         if (System.currentTimeMillis() - this.finishMillis >= WATCH_NEXT_GAME_MS) {
            this.restart();
         }
         return;
      }
      Solver.Move move = Solver.next(this.view(), this.level.mines - this.flagCount, this.random);
      if (move == null) {
         return;
      }
      this.computerPlayed = true;
      MineTile tile = this.tiles[move.row][move.column];
      this.moveCursor(tile);
      if (move.flag) {
         tile.plantFlag();
      } else {
         this.revealFrom(move.row, move.column);
      }
      if (move.why != null) {
         this.showDeduction(move.why, move.flag ? "Computer: a mine, so it flags it." : "Computer: safe, so it opens it.");
      } else if (!move.sure && !this.gameOver) {
         this.setHintLines("Computer: nothing is certain, so it guesses.", "It picks the tile least likely to be a mine.");
      }
   }

   /**
    * H or the Hint button: marks the next tile (or tiles) that can be worked out from the numbers, gold-rings the
    * numbers that show it, and says why under the status line. Flags are not trusted, so a wrong one is pointed out.
    */
   void hint() {
      this.takeOver();
      if (this.gameOver) {
         return;
      }
      if (this.grid == null) {
         this.setHintLines("Click any tile to start.", "The first click is always safe.");
         return;
      }
      Logic.Deduction step = Logic.hint(this.view(), this.flagged, this.level.mines, false);
      if (step == null) {
         this.setHintLines("Nothing is certain here.", "On a random board you sometimes have to guess.");
         return;
      }
      if (this.hintedAtMove != this.moves) {
         this.hintedAtMove = this.moves;
         this.hints++;
      }
      String action = step.action();
      if (!step.mines) {
         for (int cell : step.cells) {
            if (this.flagged[cell / this.columns][cell % this.columns]) {
               action = "Safe: a flag there is wrong.";
               break;
            }
         }
      }
      this.showDeduction(step, action);
   }

   /** G or the No guess button: deal boards that logic alone can clear, or random ones; starts a new game. */
   void toggleNoGuess() {
      this.bestTimes.setNoGuess(!this.bestTimes.noGuess());
      this.restart();
   }

   /** Marks a deduction on the board (its numbers ringed, its tiles glowing or red) with a line saying what and why. */
   private void showDeduction(Logic.Deduction step, String action) {
      this.clearHint();
      for (int cell : step.numbers) {
         this.markHint(cell, MineTile.HINT_CLUE);
      }
      for (int cell : step.cells) {
         this.markHint(cell, step.mines ? MineTile.HINT_MINE : MineTile.HINT_SAFE);
      }
      this.setHintLines(action, step.reason);
   }

   private void markHint(int cell, int role) {
      MineTile tile = this.tiles[cell / this.columns][cell % this.columns];
      tile.setHint(role);
      this.hinted.add(tile);
   }

   /** Takes a hint's marks off the board and puts the tip back. */
   private void clearHint() {
      for (MineTile tile : this.hinted) {
         tile.setHint(MineTile.HINT_NONE);
      }
      this.hinted.clear();
      this.showTip();
   }

   /** Between hints: what kind of board this is and where the help is. */
   private void showTip() {
      if (this.bestTimes.noGuess()) {
         this.setHintLines("No guess: logic alone clears every board.", "Stuck? Hint (H) shows the next step and why.");
      } else {
         this.setHintLines("Random boards: some need a lucky guess.", "No guess (G) deals boards logic can clear.");
      }
   }

   private void setHintLines(String action, String reason) {
      this.hintAction.setText(action);
      this.hintReason.setText(reason);
   }

   /** The board as a player sees it, for the computer: hidden, flagged or a revealed count. */
   private int[][] view() {
      int[][] view = new int[this.rows][this.columns];
      for (int r = 0; r < this.rows; r++) {
         for (int c = 0; c < this.columns; c++) {
            String value = this.revealed[r][c] ? this.grid[r][c] : null;
            view[r][c] = value == null ? (this.flagged[r][c] ? Solver.FLAG : Solver.HIDDEN) : value.trim().isEmpty() ? 0 : Integer.parseInt(value);
         }
      }
      return view;
   }

   @Override
   public void dispose() {
      this.watchTimer.stop();
      this.clock.stop();
      this.animation.stop();
      super.dispose();
   }

   private void updateStatus() {
      if (this.gameOver) {
         return;
      }
      int minesLeft = this.level.mines - this.flagCount;
      long elapsed = this.startMillis == 0 ? 0 : System.currentTimeMillis() - this.startMillis;
      long best = this.bestTimes.best(this.level);
      if (this.watching) {
         this.setTitle("Minesweeper: the computer is playing");
         this.status.setText("Computer playing: click to take over");
         return;
      }
      this.setTitle("Minesweeper: " + minesLeft + " mines left");
      this.status.setText(minesLeft + " mines left    Time " + elapsed / 1000 / 60 + ":" + (elapsed / 1000 % 60 < 10 ? "0" : "")
         + elapsed / 1000 % 60 + "    Best " + (best == 0 ? "none yet" : BestTimes.format(best)));
   }
}
