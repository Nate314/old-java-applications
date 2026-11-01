package ultimateticktacktoe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public final class GameFrame extends JFrame {
   private static final File RECORD_FILE = new File(System.getProperty("user.home", "."), ".ultimatetictactoe-record.txt");
   /** Room for the mode buttons and the two status lines, taken from the board so the window keeps its old size. */
   private static final int BARS_HEIGHT = 80;
   /** How long the hard computer searches for a move. */
   private static final int HARD_THINK_MILLIS = 700;
   /** A pause before the computer moves, so the player sees their own move land first. */
   private static final int MOVE_DELAY_MILLIS = 350;
   private static final Color OPEN_COLOR = new Color(0, 255, 0);
   private static final Color ACTIVE_COLOR = new Color(255, 255, 0);
   private static final Color X_WON_COLOR = new Color(0, 255, 255);
   private static final Color O_WON_COLOR = new Color(255, 175, 175);
   private static final Color DRAW_COLOR = new Color(170, 170, 170);
   private static final Color LAST_MOVE_COLOR = Color.WHITE;
   private static final Color SELECTED_MODE = new Color(0, 255, 0);
   public GameState state = new GameState();
   public SmallBoard[] boards = new SmallBoard[]{
      new SmallBoard(this, 0), new SmallBoard(this, 1), new SmallBoard(this, 2), new SmallBoard(this, 3), new SmallBoard(this, 4), new SmallBoard(this, 5), new SmallBoard(this, 6), new SmallBoard(this, 7), new SmallBoard(this, 8)
   };
   private final JButton[] modeButtons = new JButton[Mode.values().length];
   private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
   /** The record against the computer on this level; blank in two-player games. */
   private final JLabel tally = new JLabel(" ", SwingConstants.CENTER);
   private final Random random = new Random();
   private final Timer computerTimer = new Timer(MOVE_DELAY_MILLIS, event -> this.computerMove());
   /** Read after JFrame's constructor, because the browser build restores saved files when the first component is created. */
   private final Record record;
   private Mode mode;

   public GameFrame() {
      this.record = Record.load(RECORD_FILE);
      this.mode = this.record.lastMode();
      this.computerTimer.setRepeats(false);
      this.setDefaultCloseOperation(3);
      this.setResizable(false);
      // 3/4 of the screen height, but no wider than the screen, so the board also fits a narrow (portrait) browser window.
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      int boardArea = Math.min(screen.width - 24, screen.height * 3 / 4 - BARS_HEIGHT);
      int boardSize = boardArea / 3;
      JPanel grid = new JPanel();
      grid.setLayout(null);
      grid.setPreferredSize(new Dimension(boardSize * 3, boardSize * 3));
      for (int board = 0; board < this.boards.length; board++) {
         this.boards[board].setBounds(board % 3 * boardSize, board / 3 * boardSize, boardSize - 2, boardSize - 2);
         grid.add(this.boards[board]);
         this.boards[board].layoutCells();
      }

      this.add(this.modeBar(), BorderLayout.NORTH);
      JPanel gridHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
      gridHolder.add(grid);
      this.add(gridHolder, BorderLayout.CENTER);
      JPanel statusLines = new JPanel(new BorderLayout());
      this.status.setFont(new Font("Dialog", Font.BOLD, 14));
      this.tally.setFont(new Font("Dialog", Font.PLAIN, 13));
      statusLines.add(this.status, BorderLayout.NORTH);
      statusLines.add(this.tally, BorderLayout.SOUTH);
      this.add(statusLines, BorderLayout.SOUTH);
      this.refresh();
      this.pack();
      this.setVisible(true);
   }

   /** One button per mode; the current one is lit. */
   private JPanel modeBar() {
      JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
      for (Mode choice : Mode.values()) {
         JButton button = new JButton(choice.label);
         button.setFocusable(false);
         button.addActionListener(event -> this.newGame(choice));
         this.modeButtons[choice.ordinal()] = button;
         bar.add(button);
      }
      return bar;
   }

   /** Starts over in the given mode. */
   private void newGame(Mode choice) {
      this.computerTimer.stop();
      this.mode = choice;
      this.record.setLastMode(choice);
      this.state = new GameState();
      this.refresh();
   }

   private boolean computerToMove() {
      return this.mode.vsComputer() && !this.state.xTurn && this.state.winner == null;
   }

   /** A cell was clicked: play there, or start over if the game has ended. Clicks wait while the computer thinks. */
   public final void cellClicked(int board, int cell) {
      if (this.state.winner != null) {
         this.newGame(this.mode);
         return;
      }
      if (this.computerToMove() || !this.state.play(board, cell)) {
         return;
      }
      this.moved();
   }

   private void computerMove() {
      if (!this.computerToMove()) {
         return;
      }
      int move = this.mode == Mode.HARD
         ? Computer.hardMove(this.state, this.random, HARD_THINK_MILLIS)
         : Computer.easyMove(this.state, this.random);
      this.state.play(move / 9, move % 9);
      this.moved();
   }

   /** After any move: count a finished game against the computer, redraw, and hand the turn to the computer if it is O's. */
   private void moved() {
      if (this.state.winner != null && this.mode.vsComputer()) {
         this.record.add(this.mode, this.state.winner);
      }
      this.refresh();
      if (this.computerToMove()) {
         this.computerTimer.restart();
      }
   }

   /** Redraws every cell from the game state (marks, won/drawn boards, the boards the next move may use, the last move), the mode buttons and the status lines. */
   private void refresh() {
      for (int board = 0; board < this.boards.length; board++) {
         String result = this.state.boardResults[board];
         boolean playable = result == null && this.state.winner == null && (this.state.activeBoard == -1 || this.state.activeBoard == board);
         Color color = "X".equals(result) ? X_WON_COLOR
            : "O".equals(result) ? O_WON_COLOR
            : GameState.DRAW.equals(result) ? DRAW_COLOR
            : playable ? ACTIVE_COLOR
            : OPEN_COLOR;
         for (int cell = 0; cell < 9; cell++) {
            boolean last = this.state.lastMove == board * 9 + cell;
            this.boards[board].cells[cell].show(this.state.marks[board][cell], last ? LAST_MOVE_COLOR : color);
         }
      }

      for (Mode choice : Mode.values()) {
         JButton button = this.modeButtons[choice.ordinal()];
         button.setBackground(choice == this.mode ? SELECTED_MODE : null);
         button.setForeground(choice == this.mode ? Color.BLACK : null);
      }

      String winner = this.state.winner;
      this.setTitle(winner == null ? "Ultimate Tic-Tac-Toe: " + (this.state.xTurn ? "X" : "O") + " to move"
         : GameState.DRAW.equals(winner) ? "Ultimate Tic-Tac-Toe: draw. Click to play again."
         : "Ultimate Tic-Tac-Toe: " + winner + " wins! Click to play again.");
      this.status.setText(this.statusText());
      this.tally.setText(this.mode.vsComputer()
         ? "Your record on " + (this.mode == Mode.HARD ? "Hard" : "Easy") + ": " + this.record.describe(this.mode)
         : " ");
   }

   private String statusText() {
      String winner = this.state.winner;
      if (!this.mode.vsComputer()) {
         return winner == null ? (this.state.xTurn ? "X" : "O") + " to move. The yellow boards are open."
            : (GameState.DRAW.equals(winner) ? "A draw." : winner + " wins!") + " Click any cell to play again.";
      }
      if (winner != null) {
         return ("X".equals(winner) ? "You win!" : "O".equals(winner) ? "The computer wins." : "A draw.") + " Click any cell to play again.";
      }
      return this.state.xTurn ? "Your move (X)." : "The computer (O) is thinking...";
   }
}
