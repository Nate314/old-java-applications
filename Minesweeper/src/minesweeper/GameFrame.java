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
import java.util.Collections;
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
   private static final int BARS_HEIGHT = 118;
   /** How long dealing a no-guess board may take before a random one is played instead (it takes a few ms). */
   private static final int DEAL_BUDGET_MS = 1500;
   private static final Color SELECTED_LEVEL = MineTile.GRASS_LIGHT;
   private static final int[] WIN_NOTES = {72, 76, 79, 84, 88, 91, 96};
   /** An opening ripples out from the click: each ring of tiles pops this many ms after the one inside it. */
   private static final int RIPPLE_MS = 22;
   /** Mines burst in this many ms per tile of distance from the one hit; a win's flags plant at the same pace. */
   private static final int BURST_STEP_MS = 45;
   /** A won field grows back as meadow just behind the wave of flags. */
   private static final int BLOOM_LAG_MS = 120;
   /** Watch: the computer makes a move this often, and starts the next game this long after one ends. */
   private static final int WATCH_STEP_MS = 280;
   private static final int WATCH_NEXT_GAME_MS = 2600;
   /** Zoom (Z): a zoomed board shows this many tiles across and is dragged to see the rest. */
   private static final int ZOOM_TILES = 8;
   /** The strip under and right of a zoomed board where the bars show which part of it is in view. */
   private static final int GUTTER = 6;
   /** A press must move this far (window pixels) before it drags the board instead of playing the tile. */
   private static final int DRAG_SLOP = 8;
   /** Offsets meaning "center the board", for a new game or a fresh zoom. */
   private static final int CENTER = Integer.MIN_VALUE;
   /** Height of the second row of buttons on a narrow screen. */
   private static final int SECOND_ROW = 30;
   /** Zoom (Z), kept for the next game; null until the first window, which turns it on for a phone-sized screen. */
   private static Boolean zoom;
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
   /** Holds the tiles; zoomed, it is the window onto the bigger board, which moves under it. */
   private final JPanel field = new JPanel(null);
   private final JPanel acrossBar = new JPanel();
   private final JPanel downBar = new JPanel();
   private JButton zoomButton;
   /** A phone-sized screen: the buttons take two rows, so the window is no wider than the board. */
   private final boolean narrow;
   /** How far the zoomed board is dragged from its top left corner, in window pixels. */
   private int offsetX = CENTER;
   private int offsetY = CENTER;
   /** Where a press on the board began (in the board's own pixels) and the offsets then; dragged once it moves. */
   private int pressX;
   private int pressY;
   private int pressOffsetX;
   private int pressOffsetY;
   private boolean dragged;
   /** Mines left, time and best while playing; what happened once a game ends, with what to do next beside it. */
   private final Scoreboard score = new Scoreboard();
   private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
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
   /** Odds (O): every hidden tile shows its chance of being a mine. Kept for the next game, not saved. */
   private boolean showOdds;
   /** The odds were shown in this game, which counts as one hint. */
   private boolean oddsCounted;
   private JButton oddsButton;
   private Color plainButton;
   private Color plainText;
   /** The player's guesses this game, tagged on the board when it ends. */
   private final Luck luck = new Luck();
   /** The chance the move being made hits a mine, read just before it (0 when certain, -1 when not known). */
   private double moveRisk = -1;
   /** The mine the player just hit, and what the board showed instead, so a click on it can undo the loss; null otherwise. */
   private int[] hit;
   private Logic.Deduction lossHint;
   /** Mines undone this game; like a hint, one sets no best time. */
   private int undone;
   /** The count on a number with more flags around it than that (ringed in red), or null when there is none. */
   private String overFlagged;

   public GameFrame(SoundPlayer soundPlayer) {
      this(soundPlayer, null, null, null, false, false);
   }

   /**
    * Without best times, reads them from the file. That happens here, after JFrame's constructor, because the browser
    * build restores saved files when the first component is created.
    */
   private GameFrame(SoundPlayer soundPlayer, BestTimes bestTimes, Point location, int[] cursorSpot, boolean watching, boolean odds) {
      this.soundPlayer = soundPlayer;
      this.bestTimes = bestTimes != null ? bestTimes : BestTimes.load(BEST_TIMES_FILE);
      this.level = this.bestTimes.lastLevel();
      this.rows = this.columns = this.level.size;
      this.tiles = new MineTile[this.rows][this.columns];
      this.revealed = new boolean[this.rows][this.columns];
      this.flagged = new boolean[this.rows][this.columns];
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      this.narrow = screen.getWidth() < 640;
      if (zoom == null) { // where a whole Classic board leaves tiles too small to tap
         zoom = this.narrow;
      }
      int windowSize = (int)(screen.getHeight() * 3.0 / 4.0);
      this.tileWidth = this.tileHeight = Math.max(16, (windowSize - BARS_HEIGHT - (this.narrow ? SECOND_ROW : 0)) / this.rows);
      this.setDefaultCloseOperation(3);
      this.addKeyListener(new WindowMoveKeyListener(this));
      this.add(this.levelBar(), BorderLayout.NORTH);
      JPanel scoreRow = new JPanel(new BorderLayout());
      scoreRow.add(this.score, BorderLayout.CENTER);
      scoreRow.add(this.actions, BorderLayout.EAST);
      JPanel lines = new JPanel(new BorderLayout());
      this.hintAction.setFont(new Font("Dialog", Font.BOLD, 13));
      this.hintReason.setFont(new Font("Dialog", Font.PLAIN, 13));
      lines.add(scoreRow, BorderLayout.NORTH);
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
      this.field.setBackground(new Color(0, 0, 0, 0));
      this.panel.add(this.field);
      for (JPanel bar : new JPanel[]{this.acrossBar, this.downBar}) {
         bar.setBackground(new Color(120, 120, 120, 150));
         this.panel.add(bar);
      }

      for (int row = 0; row < this.rows; row++) {
         for (int column = 0; column < this.columns; column++) {
            this.tiles[row][column] = new MineTile(this, row, column);
            this.field.add(this.tiles[row][column]);
            this.tiles[row][column].setVisible(true);
         }
      }
      this.layoutTiles();

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
      if (odds) {
         this.toggleOdds();
      }
   }

   /**
    * One button per level; the current one is lit; then the game's own buttons, on a second row on a narrow screen.
    * Not focusable, so the arrow keys keep moving the window.
    */
   private JPanel levelBar() {
      JPanel levels = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
      JPanel bar = this.narrow ? new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4)) : levels;
      for (Level choice : Level.values()) {
         JButton button = new JButton(choice.label);
         button.setFocusable(false);
         if (choice == this.level) {
            button.setBackground(SELECTED_LEVEL);
            button.setForeground(Color.BLACK);
         }
         button.addActionListener(event -> this.play(choice));
         levels.add(button);
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
      this.oddsButton = new JButton("Odds");
      this.oddsButton.setFocusable(false);
      this.oddsButton.setToolTipText("Show every hidden tile's chance of being a mine (O)");
      this.oddsButton.addActionListener(event -> this.toggleOdds());
      this.plainButton = this.oddsButton.getBackground();
      this.plainText = this.oddsButton.getForeground();
      bar.add(this.oddsButton);
      this.zoomButton = new JButton("Zoom");
      this.zoomButton.setFocusable(false);
      this.zoomButton.setToolTipText("Bigger tiles: drag the board to see the rest (Z)");
      this.zoomButton.addActionListener(event -> this.toggleZoom());
      this.lightZoom();
      bar.add(this.zoomButton);
      if (!this.narrow) {
         return bar;
      }
      JPanel rows = new JPanel(new BorderLayout());
      rows.add(levels, BorderLayout.NORTH);
      rows.add(bar, BorderLayout.SOUTH);
      return rows;
   }

   /** Left click on a tile: reveals it (and a blank region), or ends the game on a mine. */
   void revealFrom(int row, int column) {
      if (this.grid != null && !this.revealed[row][column]) {
         this.noteRisk(Collections.singletonList(new int[]{row, column}));
      }
      this.reveal(row, column, row, column);
      this.refreshOdds();
      this.coach();
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
      this.noteRisk(targets);
      if (!this.watching) {
         this.bestTimes.learned(Coach.CHORD);
      }
      for (int[] cell : targets) {
         if (this.gameOver) {
            return;
         }
         this.reveal(cell[0], cell[1], row, column);
      }
      this.refreshOdds();
      this.coach();
   }

   /**
    * Before a player's move opens those tiles: notes how likely (from the numbers alone) it was to hit a mine, so the
    * game's guesses can be shown when it ends. For several tiles (opening around a number) that is the riskiest one's.
    */
   private void noteRisk(List<int[]> cells) {
      this.moveRisk = -1;
      double[][] odds = this.watching ? null : Odds.of(this.view(), this.level.mines);
      if (odds == null) {
         if (!this.watching) {
            this.luck.unread++;
         }
         return;
      }
      int[] worst = cells.get(0);
      for (int[] cell : cells) {
         if (odds[cell[0]][cell[1]] > odds[worst[0]][worst[1]]) {
            worst = cell;
         }
      }
      this.moveRisk = odds[worst[0]][worst[1]];
      this.luck.add(worst[0] * this.columns + worst[1], this.moveRisk);
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
            this.score.say("Boom! The computer had to guess. Next game...", Scoreboard.LOST);
         } else {
            this.hit = new int[]{row, column};
            this.lossHint = instead;
            this.setTitle("Minesweeper: you hit a mine. Click the red mine to undo it, or another tile to play again.");
            this.ended("Boom! You hit a mine.", Scoreboard.LOST);
            if (instead != null) {
               this.showDeduction(instead, "No need to guess: the glowing tiles were safe.");
            } else if (this.moveRisk > 1e-9) {
               String[] lines = this.luck.lossLines();
               this.setHintLines(lines[0], lines[1]);
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
      this.settleNumbers();

      if (MineFieldGenerator.isWon(this.grid, this.revealed)) {
         this.finish();
         for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.columns; c++) {
               int delay = BURST_STEP_MS * distance(r, c, row, column);
               if (MineFieldGenerator.isMine(this.grid, r, c)) {
                  this.tiles[r][c].showFlag(delay);
               } else {
                  this.tiles[r][c].bloom(delay + BLOOM_LAG_MS);
               }
            }
         }
         long time = this.finishMillis - this.startMillis;
         for (int note : WIN_NOTES) {
            this.soundPlayer.queueNote(note);
         }
         if (!this.computerPlayed) {
            int guesses = this.luck.guesses();
            String cleared = this.luck.clearedLine();
            this.setHintLines(this.oddsCounted ? "Cleared with the odds showing." : this.undone > 0 ? "Cleared, with " + (this.undone == 1 ? "a mine" : this.undone + " mines") + " undone."
               : this.hints > 0 ? "Cleared with " + this.hints + (this.hints == 1 ? " hint." : " hints.")
               : cleared != null ? cleared
               : this.fairBoard ? "Cleared by logic alone, no guesses." : "Cleared without a single guess.",
               guesses > 0 ? this.luck.winLine() : this.luck.unread > 0 ? this.luck.unreadLine()
               : this.undone > 0 ? "A game with a mine undone sets no best time." : this.hints > 0 ? "A game won with hints sets no best time." : " ");
         }
         if (this.computerPlayed) {
            // Not a best time: the computer played some or all of it.
            String who = this.watching ? "The computer" : "You and the computer";
            this.setTitle("Minesweeper: " + who.toLowerCase() + " cleared it in " + BestTimes.format(time) + ".");
            if (this.watching) {
               this.score.say(who + " cleared it in " + BestTimes.format(time) + ". Next game...", Scoreboard.WON);
            } else {
               this.ended(who + " cleared it in " + BestTimes.format(time) + ".", Scoreboard.WON);
            }
            return;
         }
         if (this.hints > 0 || this.undone > 0) {
            this.setTitle("Minesweeper: you win in " + BestTimes.format(time) + "! Click to play again.");
            this.ended("You win in " + BestTimes.format(time) + (this.hints > 0 ? " with hints." : " with a mine undone."), Scoreboard.WON);
            return;
         }
         long before = this.bestTimes.best(this.level);
         boolean record = this.bestTimes.record(this.level, time);
         this.setTitle("Minesweeper: you win in " + BestTimes.format(time) + "! Click to play again.");
         this.ended("You win in " + BestTimes.format(time) + "! "
            + (!record ? "Best: " + BestTimes.format(before) : before == 0 ? "First " + this.level.label + " win." : "New best!"), Scoreboard.WON);
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
      this.unmark();
      if (!this.computerPlayed && MineFieldGenerator.isWon(this.grid, this.revealed)) {
         this.bestTimes.learned(Coach.CHORD); // a player who can win needs no lessons
      }
      for (int i = 0; i < this.luck.guesses(); i++) {
         int cell = this.luck.cells.get(i);
         this.tiles[cell / this.columns][cell % this.columns].setGamble(this.luck.risks.get(i));
      }
   }

   /**
    * A click on the red mine (or U) after the player hit it: the move is undone and the game goes on, with a flag on
    * that mine and the clock running again. False when there is nothing to undo.
    */
   boolean undoMine() {
      if (!this.gameOver || this.hit == null) {
         return false;
      }
      this.flagCount = 0;
      for (int r = 0; r < this.rows; r++) {
         for (int c = 0; c < this.columns; c++) {
            this.tiles[r][c].undoLoss();
            this.flagged[r][c] = this.tiles[r][c].isFlagged(); // the mine now flagged, the wrong flags gone
            this.flagCount += this.flagged[r][c] ? 1 : 0;
         }
      }
      this.luck.takeBack(this.moveRisk);
      this.undone++;
      this.moves++;
      this.gameOver = false;
      this.startMillis += System.currentTimeMillis() - this.finishMillis; // the time spent looking at the loss is not counted
      this.clock.start();
      this.updateStatus();
      this.settleNumbers();
      this.refreshOdds();
      if (this.lossHint != null) {
         this.showDeduction(this.lossHint, "Undone. These glowing tiles were safe to open.");
      } else if (!this.showOdds) {
         this.setHintLines("Undone: a flag now marks that mine.", "A game with a mine undone sets no best time.");
      }
      this.hit = null;
      this.lossHint = null;
      this.actions.removeAll();
      this.actions.revalidate();
      this.actions.repaint();
      return true;
   }

   boolean isRevealed(int row, int column) {
      return this.revealed[row][column];
   }

   void setFlagged(int row, int column, boolean flag) {
      if (this.flagged[row][column] != flag) {
         this.flagged[row][column] = flag;
         this.flagCount += flag ? 1 : -1;
         this.moves++;
         if (flag && !this.watching) {
            this.bestTimes.learned(Coach.FLAG);
         }
         this.settleNumbers();
         this.clearHint();
         this.updateStatus();
      }
   }

   /** Every revealed number shows how it stands by its flags: settled ones fade into the sand, over-flagged ones go red. */
   private void settleNumbers() {
      this.overFlagged = null;
      if (this.grid == null) {
         return;
      }
      for (int r = 0; r < this.rows; r++) {
         for (int c = 0; c < this.columns; c++) {
            int standing = MineFieldGenerator.standing(this.grid, this.revealed, this.flagged, r, c);
            this.tiles[r][c].setStanding(standing);
            if (standing == MineFieldGenerator.OVER && this.overFlagged == null) {
               this.overFlagged = this.grid[r][c];
            }
         }
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
         this.scrollTo(tile);
      }
   }

   /** Zoom only helps boards wider than the view it shows; Beginner's tiles are big enough already. */
   private boolean zoomed() {
      return zoom && this.rows > ZOOM_TILES + 1;
   }

   /** Z or the Zoom button: bigger tiles on a board dragged to see the rest, or the whole board. Kept for the next game. */
   void toggleZoom() {
      zoom = !zoom;
      this.lightZoom();
      this.offsetX = this.offsetY = CENTER;
      this.layoutTiles();
      if (this.cursor != null) {
         this.scrollTo(this.cursor);
      }
   }

   private void lightZoom() {
      this.zoomButton.setBackground(zoom ? SELECTED_LEVEL : this.plainButton);
      this.zoomButton.setForeground(zoom ? Color.BLACK : this.plainText);
   }

   /** Places the tiles: the whole board, or zoomed, the dragged part of the bigger one with bars saying which part. */
   private void layoutTiles() {
      int board = this.rows * this.tileWidth;
      boolean zoomed = this.zoomed();
      int view = zoomed ? board - GUTTER : board;
      int size = zoomed ? view / ZOOM_TILES : this.tileWidth;
      int content = size * this.rows;
      this.offsetX = !zoomed ? 0 : clamp(this.offsetX == CENTER ? (content - view) / 2 : this.offsetX, content, view);
      this.offsetY = !zoomed ? 0 : clamp(this.offsetY == CENTER ? (content - view) / 2 : this.offsetY, content, view);
      this.field.setBounds(0, 0, view, view);
      for (int row = 0; row < this.rows; row++) {
         for (int column = 0; column < this.columns; column++) {
            this.tiles[row][column].setBounds(row * size - this.offsetX, column * size - this.offsetY, size, size);
         }
      }
      this.acrossBar.setVisible(zoomed);
      this.downBar.setVisible(zoomed);
      if (zoomed) {
         this.acrossBar.setBounds(this.offsetX * view / content, view + 2, view * view / content, GUTTER - 2);
         this.downBar.setBounds(view + 2, this.offsetY * view / content, GUTTER - 2, view * view / content);
      }
      this.panel.repaint();
   }

   /** An offset that keeps the view on the board. */
   static int clamp(int offset, int content, int view) {
      return Math.max(0, Math.min(offset, content - view));
   }

   /** The offset that brings [start, start + size) into view with a tile to spare on each side (clamped later). */
   static int follow(int offset, int start, int size, int view) {
      if (start - size < offset) {
         return start - size;
      }
      return start + 2 * size > offset + view ? start + 2 * size - view : offset;
   }

   /** Zoomed, drags the board so the tile (the keyboard's or the computer's) is in view. */
   private void scrollTo(MineTile tile) {
      if (!this.zoomed()) {
         return;
      }
      int x = follow(this.offsetX, tile.getX() + this.offsetX, tile.getWidth(), this.field.getWidth());
      int y = follow(this.offsetY, tile.getY() + this.offsetY, tile.getHeight(), this.field.getHeight());
      if (x != this.offsetX || y != this.offsetY) {
         this.offsetX = x;
         this.offsetY = y;
         this.layoutTiles();
      }
   }

   /** A press on a tile, at (x, y) in the board's own pixels: a drag may follow. */
   void pressBoard(int x, int y) {
      this.pressX = x;
      this.pressY = y;
      this.pressOffsetX = this.offsetX;
      this.pressOffsetY = this.offsetY;
      this.dragged = false;
   }

   /** The press moved to (x, y): zoomed, past a few pixels, the board follows it. True while it is being dragged. */
   boolean dragBoard(int x, int y) {
      if (!this.zoomed() || !this.dragged && Math.abs(x - this.pressX) + Math.abs(y - this.pressY) < DRAG_SLOP) {
         return false;
      }
      this.dragged = true;
      this.offsetX = this.pressOffsetX - (x - this.pressX);
      this.offsetY = this.pressOffsetY - (y - this.pressY);
      this.layoutTiles();
      return true;
   }

   /** The last press dragged the board, so its release plays no tile. */
   boolean wasDragged() {
      return this.dragged;
   }

   /** A new game on the same level, where this window was. */
   void restart() {
      this.play(this.level);
   }

   /** A new game on that level, which is remembered for the next start. */
   void play(Level choice) {
      this.bestTimes.setLastLevel(choice);
      this.dispose();
      new GameFrame(this.soundPlayer, this.bestTimes, this.getLocation(), this.cursor == null ? null : this.cursor.spot(), this.watching,
         this.showOdds);
   }

   /** W: the computer plays this board (and the games after it) until W again, a click or a key on a tile. */
   void toggleWatch() {
      this.watching = !this.watching;
      if (this.watching) {
         this.watchTimer.start();
      } else {
         this.watchTimer.stop();
         this.moveCursor(null);
         if (this.gameOver && this.actions.getComponentCount() == 0) { // it would have dealt the next game itself
            this.ended("Your turn: deal the next board.", Scoreboard.PLAIN);
         }
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

   /** O or the Odds button: shows or hides every hidden tile's chance of being a mine. Counts as a hint. */
   void toggleOdds() {
      this.showOdds = !this.showOdds;
      this.oddsButton.setBackground(this.showOdds ? SELECTED_LEVEL : this.plainButton);
      this.oddsButton.setForeground(this.showOdds ? Color.BLACK : this.plainText);
      this.clearHint();
      this.refreshOdds();
   }

   /**
    * Puts each hidden tile's odds on it (or takes them off, with Odds off or the game over) and, while playing, says
    * under the status line what they show.
    */
   private void refreshOdds() {
      double[][] odds = this.showOdds && !this.gameOver && this.grid != null ? Odds.of(this.view(), this.level.mines) : null;
      double safest = 1;
      for (int r = 0; r < this.rows; r++) {
         for (int c = 0; c < this.columns; c++) {
            double chance = odds == null || this.revealed[r][c] ? -1 : odds[r][c];
            this.tiles[r][c].setOdds(chance);
            if (chance >= 0 && !this.flagged[r][c]) {
               safest = Math.min(safest, chance);
            }
         }
      }
      if (!this.showOdds || this.gameOver) {
         return;
      }
      if (this.grid == null) {
         this.setHintLines("Odds: each tile's chance of being a mine.", "They show after the first click, which is always safe.");
      } else if (odds == null) {
         this.setHintLines("Too many ways to count the odds here.", "They come back as more of the board opens.");
      } else {
         if (!this.watching && !this.oddsCounted) {
            this.oddsCounted = true;
            this.hints++;
         }
         this.setHintLines(safest < 1e-9 ? "Odds: the 0% tiles are safe to open." : "Odds: no tile is safe for certain.",
            safest < 1e-9 ? "The redder a tile, the likelier a mine." : "The safest is " + Odds.percent(safest) + " likely to be a mine.");
      }
   }

   /** Marks a deduction on the board (its numbers ringed, its tiles glowing or red) with a line saying what and why. */
   private void showDeduction(Logic.Deduction step, String action) {
      this.unmark();
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
      this.unmark();
      this.showTip();
   }

   private void unmark() {
      for (MineTile tile : this.hinted) {
         tile.setHint(MineTile.HINT_NONE);
      }
      this.hinted.clear();
   }

   /**
    * Between hints: how to start, a first game's lesson, or what kind of board this is and where the help is (with
    * Odds on, what the odds say).
    */
   private void showTip() {
      String board = this.bestTimes.noGuess() ? "No guess: logic alone clears every board." : "Random boards: some need a lucky guess.";
      if (this.showOdds) {
         this.refreshOdds();
      } else if (this.overFlagged != null && !this.watching) {
         this.setHintLines("Too many flags: the red " + this.overFlagged + " has more flags than " + (this.overFlagged.equals("1") ? "its 1 mine." : "its " + this.overFlagged + " mines."),
            "One of its flags is wrong: right click (or long press) to take it off.");
      } else if (this.grid == null && !this.watching) {
         this.setHintLines("Click any tile to start: the first is always safe.", board);
      } else if (!this.coach()) {
         this.setHintLines(board, this.bestTimes.noGuess() ? "Stuck? Hint (H) shows the next step and why." : "No guess (G) deals boards logic can clear.");
      }
   }

   /**
    * A first game, until the player has flagged a mine and opened around a number: the lesson they are on, shown on
    * the board (a gold number and the tiles it settles) with what to do. True when it set the lines.
    */
   private boolean coach() {
      int lesson = this.bestTimes.lessons();
      if (lesson == Coach.DONE || this.grid == null || this.gameOver || this.watching || this.showOdds || this.overFlagged != null) {
         return false;
      }
      Coach step = lesson == Coach.FLAG ? Coach.flag(this.view()) : Coach.chord(this.grid, this.revealed, this.flagged, this.view());
      this.unmark();
      if (step.number >= 0) {
         this.markHint(step.number, MineTile.HINT_CLUE);
         this.scrollTo(this.tiles[step.number / this.columns][step.number % this.columns]); // zoomed, the example may be off the view
      }
      for (int cell : step.cells) {
         this.markHint(cell, step.mines ? MineTile.HINT_MINE : MineTile.HINT_SAFE);
      }
      this.setHintLines(step.action, step.reason);
      return true;
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
         this.score.say("Computer playing: click to take over", Scoreboard.PLAIN);
         return;
      }
      this.setTitle("Minesweeper: " + minesLeft + " mines left");
      this.score.counters(minesLeft, elapsed, best);
   }

   /**
    * A game the player was in has ended: says what happened and puts what to do next beside it, as buttons a finger
    * can find (a click on the board still does the same: the red mine undoes, any other tile starts again).
    */
   private void ended(String text, int mood) {
      this.score.say(text, mood);
      if (this.hit != null) {
         this.actions.add(this.action("Undo", "Take back the mine and play this board on (U)", this::undoMine));
         this.actions.add(this.action("New game", "Deal a new board (F2)", this::restart));
      } else {
         this.actions.add(this.action("Again", "Deal a new board (F2)", this::restart));
         if (this.level.ordinal() + 1 < Level.values().length) {
            Level next = Level.values()[this.level.ordinal() + 1];
            this.actions.add(this.action("Next: " + next.label, "A bigger board (" + (next.ordinal() + 1) + ")", () -> this.play(next)));
         }
      }
      this.actions.revalidate();
      this.actions.repaint();
   }

   private JButton action(String label, String tip, Runnable run) {
      JButton button = new JButton(label);
      button.setFocusable(false);
      button.setToolTipText(tip);
      button.addActionListener(event -> run.run());
      return button;
   }
}
