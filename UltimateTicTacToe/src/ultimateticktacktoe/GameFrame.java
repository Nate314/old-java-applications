package ultimateticktacktoe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public final class GameFrame extends JFrame {
   private static final File RECORD_FILE = new File(System.getProperty("user.home", "."), ".ultimatetictactoe-record.txt");
   /** Room for the mode buttons, the status line and the record line (or the friend row), taken from the board so the window fits the screen. */
   private static final int BARS_HEIGHT = 96;
   /** Space between the buttons of a row. */
   private static final int GAP = 6;
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
   /** Friend by link: the link (or code) to send, or a friend's to open, with Copy, Open and Take back. Shown in place of the record line. */
   private final JPanel friendRow = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP, 4));
   private final JTextField friendField = new JTextField();
   private final JButton copyButton;
   private final JButton takeBackButton = new JButton("Take back");
   /** The page the game runs on, for links; null on the desktop, which sends the code alone. */
   private final String friendPage;
   /** The moves of this game so far, as a FriendLink code. */
   private String moves = "";
   /** Friend by link: this side has moved, and the next move is the friend's, to arrive in a link. */
   private boolean friendWaiting;
   /** A status line to show instead of the game's, until the next refresh. */
   private String notice;
   /** Read after JFrame's constructor, because the browser build restores saved files when the first component is created. */
   private final Record record;
   private Mode mode;

   /** {@code page} and {@code code} come from the arguments (see Main); either may be null. */
   public GameFrame(String page, String code) {
      this.friendPage = page;
      this.copyButton = new JButton(page == null ? "Copy code" : "Copy link");
      this.record = Record.load(RECORD_FILE);
      this.mode = this.record.lastMode();
      if (this.mode == Mode.FRIEND) {
         this.restoreFriendGame();
      }
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

      this.add(this.modeBar(boardSize * 3), BorderLayout.NORTH);
      JPanel gridHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
      gridHolder.add(grid);
      this.add(gridHolder, BorderLayout.CENTER);
      JPanel statusLines = new JPanel(new BorderLayout());
      this.status.setFont(new Font("Dialog", Font.BOLD, 14));
      this.tally.setFont(new Font("Dialog", Font.PLAIN, 13));
      statusLines.add(this.status, BorderLayout.NORTH);
      // The record line and the friend row share one slot, as tall as the row, and only one shows at a time.
      JPanel lower = new JPanel(new BorderLayout());
      this.layoutFriendRow(boardSize * 3);
      lower.setPreferredSize(new Dimension(boardSize * 3, this.friendRow.getPreferredSize().height));
      lower.add(this.tally, BorderLayout.CENTER);
      lower.add(this.friendRow, BorderLayout.SOUTH);
      statusLines.add(lower, BorderLayout.SOUTH);
      this.add(statusLines, BorderLayout.SOUTH);
      // A friend's link or code from the arguments, unless it is this game before this side answered it (the page reloaded).
      if (code != null && !(this.mode == Mode.FRIEND && this.moves.startsWith(code) && this.moves.length() > code.length())) {
         this.openFriendGame(code);
      }
      this.refresh();
      this.pack();
      this.setVisible(true);
   }

   /** One button per mode; the current one is lit. Short labels where the full ones are wider than the board. */
   private JPanel modeBar(int width) {
      JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP, 4));
      for (Mode choice : Mode.values()) {
         JButton button = new JButton(choice.label);
         button.setFocusable(false);
         button.addActionListener(event -> this.newGame(choice));
         this.modeButtons[choice.ordinal()] = button;
         bar.add(button);
      }
      if (bar.getPreferredSize().width > width) {
         for (Mode choice : Mode.values()) {
            this.modeButtons[choice.ordinal()].setText(choice.shortLabel);
         }
      }
      return bar;
   }

   /** The friend row: the field takes what the buttons leave of the board's width. Enter in the field opens it, like Open. */
   private void layoutFriendRow(int width) {
      JButton open = new JButton("Open");
      for (JButton button : new JButton[]{this.copyButton, open, this.takeBackButton}) {
         button.setFocusable(false);
      }
      this.copyButton.addActionListener(event -> this.copyFriendLink());
      open.addActionListener(event -> this.openFriendGame(this.friendField.getText()));
      this.friendField.addActionListener(event -> this.openFriendGame(this.friendField.getText()));
      this.takeBackButton.addActionListener(event -> this.takeBack());
      this.friendField.setToolTipText("The " + this.thing() + " to send. Paste your friend's here and press Open.");
      int buttons = this.copyButton.getPreferredSize().width + open.getPreferredSize().width + this.takeBackButton.getPreferredSize().width;
      int fieldWidth = Math.max(60, width - buttons - GAP * 5);
      this.friendField.setPreferredSize(new Dimension(fieldWidth, this.copyButton.getPreferredSize().height));
      this.friendRow.add(this.friendField);
      this.friendRow.add(this.copyButton);
      this.friendRow.add(open);
      this.friendRow.add(this.takeBackButton);
   }

   /** What a game is sent as: a link on the web, a code on the desktop. */
   private String thing() {
      return this.friendPage == null ? "code" : "link";
   }

   /** Starts over in the given mode. */
   private void newGame(Mode choice) {
      this.computerTimer.stop();
      boolean backToFriend = choice == Mode.FRIEND && this.mode != Mode.FRIEND;
      this.mode = choice;
      this.record.setLastMode(choice);
      if (backToFriend) {
         this.restoreFriendGame();   // the game with the friend, which another mode only set aside
      } else {
         this.state = new GameState();
         this.moves = "";
         this.friendWaiting = false;
         this.saveFriendGame();
      }
      this.refresh();
   }

   private void restoreFriendGame() {
      GameState saved = FriendLink.replay(this.record.friendMoves());
      boolean resume = saved != null && saved.winner == null;
      this.state = resume ? saved : new GameState();
      this.moves = resume ? this.record.friendMoves() : "";
      this.friendWaiting = resume && this.record.friendWaiting();
   }

   private void saveFriendGame() {
      if (this.mode == Mode.FRIEND) {
         this.record.setFriendGame(this.moves, this.friendWaiting);
      }
   }

   /** Opens a friend's link or code: the game as they left it, with this side to move. */
   private void openFriendGame(String text) {
      String code = FriendLink.codeIn(text);
      GameState game = code.isEmpty() ? null : FriendLink.replay(code);
      if (game == null) {
         this.status.setText("That is not a game " + this.thing() + ".");
         return;
      }
      this.computerTimer.stop();
      this.mode = Mode.FRIEND;
      this.record.setLastMode(Mode.FRIEND);
      this.state = game;
      this.moves = code;
      this.friendWaiting = false;
      this.saveFriendGame();
      this.refresh();
   }

   /** Puts the link (or code) on the clipboard; false where there is no clipboard to use. */
   private boolean copyFriendLink() {
      try {
         Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(FriendLink.message(this.friendPage, this.moves)), null);
         return true;
      } catch (RuntimeException e) {
         return false;   // headless, or the clipboard is busy
      }
   }

   /** Undoes this side's move, before it is sent. */
   private void takeBack() {
      if (!this.friendWaiting || this.moves.isEmpty()) {
         return;
      }
      this.moves = this.moves.substring(0, this.moves.length() - 2);
      this.state = FriendLink.replay(this.moves);
      this.friendWaiting = false;
      this.saveFriendGame();
      this.refresh();
   }

   /** Plays a move and adds it to the game's code. */
   private boolean play(int board, int cell) {
      if (!this.state.play(board, cell)) {
         return false;
      }
      this.moves += FriendLink.moveCode(board * 9 + cell);
      return true;
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
      if (this.computerToMove() || this.mode == Mode.FRIEND && this.friendWaiting || !this.play(board, cell)) {
         return;
      }
      if (this.mode == Mode.FRIEND) {
         // One move per link: the friend answers with the link this copies.
         this.friendWaiting = true;
         this.saveFriendGame();
         if (!this.copyFriendLink()) {
            this.notice = "Copy the " + this.thing() + " and send it.";
         }
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
      this.play(move / 9, move % 9);
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
            this.boards[board].cells[cell].show(this.state.marks[board][cell], last ? LAST_MOVE_COLOR : color, playable, last);
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
      this.status.setText(this.notice != null ? this.notice : this.statusText());
      this.notice = null;
      this.tally.setText(this.mode.vsComputer()
         ? "Your record on " + (this.mode == Mode.HARD ? "Hard" : "Easy") + ": " + this.record.describe(this.mode)
         : " ");
      boolean friend = this.mode == Mode.FRIEND;
      this.tally.setVisible(!friend);
      this.friendRow.setVisible(friend);
      this.friendField.setText(FriendLink.message(this.friendPage, this.moves));
      this.copyButton.setEnabled(!this.moves.isEmpty());
      this.takeBackButton.setEnabled(this.friendWaiting);
      this.friendRow.getParent().validate();
   }

   private String statusText() {
      if (this.mode == Mode.FRIEND) {
         return this.friendStatusText();
      }
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

   /** Kept short, to fit over a phone-sized board: the line does not wrap. */
   private String friendStatusText() {
      String winner = this.state.winner;
      // This side moved last while waiting for the friend, and is the side to move otherwise.
      String side = this.friendWaiting == this.state.xTurn ? "O" : "X";
      if (winner != null) {
         String result = GameState.DRAW.equals(winner) ? "A draw." : side.equals(winner) ? "You win!" : "Your friend wins.";
         return result + (this.friendWaiting ? " Send the " + this.thing() + " to show them." : " Click to play again.");
      }
      if (this.moves.isEmpty()) {
         return "You are X: make the first move.";
      }
      if (this.friendWaiting) {
         return "Copied! Send the " + this.thing() + " to your friend.";
      }
      return "Your move (" + side + "). Theirs is on white.";
   }
}
