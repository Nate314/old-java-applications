package ultimateticktacktoe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
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
   /** On a wide, short screen the bars go in a column beside the board (at least this wide, at most COLUMN_MAX), which takes their height instead. */
   private static final int COLUMN_MIN = 300;
   private static final int COLUMN_MAX = 340;
   /** Space between the buttons of a row. */
   private static final int GAP = 6;
   /** How long the hard computer searches for a move. */
   private static final int HARD_THINK_MILLIS = 700;
   /** A pause before the computer moves, so the player sees their own move land first. */
   private static final int MOVE_DELAY_MILLIS = 350;
   /** Watch: each side searches this briefly, so a click to take over is not held up for long. */
   private static final int WATCH_THINK_MILLIS = 150;
   /** Watch: the next game starts this long after one ends. */
   private static final int WATCH_NEXT_GAME_MILLIS = 3000;
   /** Review: how long the search looks at each position of the game, one position per tick (the page waits that long). */
   private static final int REVIEW_THINK_MILLIS = 120;
   /** Hints: the search on the position grows this long per tick, until it has had HINT_THINK_MILLIS in all. */
   private static final int HINT_TICK_MILLIS = 60;
   private static final int HINT_THINK_MILLIS = 900;
   private static final Color TEXT = new Color(230, 235, 245);
   private static final Color QUIET_TEXT = new Color(150, 162, 186);
   private static final Color BUTTON = new Color(46, 58, 86);
   public GameState state = new GameState();
   public SmallBoard[] boards = new SmallBoard[]{
      new SmallBoard(this, 0), new SmallBoard(this, 1), new SmallBoard(this, 2), new SmallBoard(this, 3), new SmallBoard(this, 4), new SmallBoard(this, 5), new SmallBoard(this, 6), new SmallBoard(this, 7), new SmallBoard(this, 8)
   };
   private final JButton[] modeButtons = new JButton[Mode.values().length];
   private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
   /** The record against the computer on this level; blank in two-player games. */
   private final JLabel tally = new JLabel(" ", SwingConstants.CENTER);
   /** The bars' width (the board's, or the column's beside it), which the record line (with the button beside it) must fit in. */
   private int barsWidth;
   private final Random random = new Random();
   private final Timer computerTimer = new Timer(MOVE_DELAY_MILLIS, event -> this.computerMove());
   /** Hard and Watch: the computer's search, grown a slice per tick so the board shows where it is looking (and the page never waits long); null when it is not thinking. */
   private Computer.Think computerThink;
   private String computerThinkFor;
   private int computerSpent;
   private final Timer thinkTimer = new Timer(HINT_TICK_MILLIS, event -> this.computerThinkStep());
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
   /** When each cell's mark, each board's win and the game's end started to draw in; 0 for ones already drawn. */
   final long[] placedAt = new long[81];
   final long[] boardWonAt = new long[9];
   long gameWonAt;
   /** The last move's trail to the board it sends the next player to: when it sets off (0 for none), from which cell, to which board. */
   long trailAt;
   int trailFrom = -1;
   int trailTo = -1;
   /** When the outlines of the boards the next move may use last thickened for a moment (a trail landing, a refused click); 0 for never. */
   long pulseAt;
   /** Repaints the cells every frame until the last drawing-in is done. */
   private final Timer animation = new Timer(16, event -> this.animate());
   private long animateUntil;
   /** When the last move's drawing that matters to the next move (its mark, a won board, the trail's landing) is done. */
   private long settleAt;
   /** The move under the mouse or the keyboard cursor (board * 9 + cell), or -1; and the boards it would send the opponent to. */
   private int previewMove = -1;
   private boolean[] previewTargets;
   /** Where the opponent could win a small board after the previewed move (board * 9 + cell), or null. */
   private boolean[] previewDangers;
   /** The cell the arrow keys are on, or -1 once the mouse is used. */
   private int cursorMove = -1;
   /** The game's status line, shown again when a preview's line ends. */
   private String statusLine = " ";
   /** Watch (W): the computer plays both sides, game after game, until a click or W again. */
   private boolean watching;
   /** The computer played X in this game, so it does not count in the record. */
   private boolean computerPlayedX;
   private final Timer nextGameTimer = new Timer(WATCH_NEXT_GAME_MILLIS, event -> this.newGame(this.mode));
   /** Review (R, or the Review button once a game ends): the game looked over move by move; null while playing. */
   private Review review;
   /** The position on the board while reviewing: the game after this many moves. */
   private int reviewAt;
   /** Analyses one position of the review per tick, so the board can be stepped through while the rest are worked out. */
   private final Timer reviewTimer = new Timer(30, event -> this.analyseStep());
   private final JPanel modeBar;
   private final ReviewGraph graph;
   /** Beside the board on a landscape screen: the two sides, who is to move and the boards each has won; null above and below the board. */
   private FaceOff faceOff;
   /** Review game: next to the record once a game ends, and in place of Open in the friend row. */
   private final JButton reviewButton = new JButton("Review game");
   private final JButton friendReviewButton = new JButton("Review");
   private final JButton openButton = new JButton("Open");
   private final JPanel reviewButtonHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, 4));
   /** While reviewing, in place of the record line: step back and forward, the next mistake, and Done. */
   private final JPanel reviewRow = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP, 4));
   /** How to play (L, or the button beside the record before the first move): the lesson step shown, or -1 while playing. */
   private int lessonStep = -1;
   /** The step's task is done; a click (or Next) goes on. */
   private boolean lessonDone;
   /** The game set aside while the lesson borrows the board. */
   private GameState lessonSaved;
   private final JButton learnButton = new JButton("How to play");
   /** While the lesson runs, in place of the mode buttons: Back, Again, Next (Play after the last step) and Done. */
   private final JPanel lessonBar = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP, 4));
   private final JButton lessonBack = new JButton("Back");
   private final JButton lessonNext = new JButton("Next");
   /** Hints (H): on your turn, every move you may make shows its chances from Hard's search, the best in a gold ring. */
   private boolean hints;
   private final JButton hintsButton = new JButton("Hints");
   /** The search behind the hints, the position (game code) it is for, and how long it has searched so far. */
   private Computer.Think think;
   private String thinkFor;
   private int thinkSpent;
   /** What the board shows: each move's chances for the side to move (NaN where none), and the best; null while hidden. */
   private double[] hintChances;
   private double[] hintDoubts;
   private int hintBest = -1;
   private final Timer hintTimer = new Timer(HINT_TICK_MILLIS, event -> this.hintStep());
   /** Hints were shown in this game against the computer, so it does not count in the record. */
   private boolean hinted;
   /** The game's notes (M turns them off and on; the setting is kept in the record file). */
   private final Sounds sounds;
   private final JButton soundButton = new JButton("Sound");

   /** {@code page} and {@code code} come from the arguments (see Main); either may be null. */
   public GameFrame(String page, String code) {
      this.friendPage = page;
      this.copyButton = new JButton(page == null ? "Copy code" : "Copy link");
      this.record = Record.load(RECORD_FILE);
      this.mode = this.record.lastMode();
      this.sounds = Sounds.open(this.record.soundOn());
      if (this.mode == Mode.FRIEND) {
         this.restoreFriendGame();
      }
      this.computerTimer.setRepeats(false);
      this.nextGameTimer.setRepeats(false);
      this.setDefaultCloseOperation(3);
      this.setResizable(false);
      // 3/4 of the screen height, but no wider than the screen, so the board also fits a narrow (portrait) browser window.
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      int stacked = Math.min(screen.width - 24, screen.height * 3 / 4 - BARS_HEIGHT);
      // A landscape screen often has room beside the board: the bars move into a column there and the board takes their height.
      int beside = Math.min(screen.height * 3 / 4, screen.width - 24 - COLUMN_MIN - GAP * 4);
      boolean column = beside > stacked;
      int boardSize = Math.max(stacked, beside) / 3;
      int gap = Math.max(4, boardSize / 28);
      this.barsWidth = column ? Math.min(COLUMN_MAX, screen.width - 24 - GAP * 4 - boardSize * 3) : boardSize * 3;
      JPanel grid = new JPanel();
      grid.setLayout(null);
      grid.setPreferredSize(new Dimension(boardSize * 3, boardSize * 3));
      for (int board = 0; board < this.boards.length; board++) {
         this.boards[board].setBounds(board % 3 * boardSize + gap / 2, board / 3 * boardSize + gap / 2, boardSize - gap, boardSize - gap);
         grid.add(this.boards[board]);
         this.boards[board].layoutCells();
      }

      // The mode buttons and the review's chart share the top slot; only one shows at a time.
      this.modeBar = this.modeBar(this.barsWidth, column);
      Dimension topSize = new Dimension(this.barsWidth, this.modeBar.getPreferredSize().height);
      this.graph = new ReviewGraph(this, topSize);
      JPanel top = new JPanel(new BorderLayout());
      top.setPreferredSize(topSize);
      top.add(this.modeBar, BorderLayout.CENTER);
      top.add(this.graph, BorderLayout.NORTH);
      this.layoutLessonBar();
      top.add(this.lessonBar, BorderLayout.SOUTH);
      this.graph.setVisible(false);
      JPanel gridHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, column ? GAP * 2 : 0, 0));
      gridHolder.add(grid);
      this.add(gridHolder, BorderLayout.CENTER);
      JPanel statusLines = new JPanel(new BorderLayout());
      this.status.setFont(new Font("Dialog", Font.BOLD, 14));
      this.tally.setFont(new Font("Dialog", Font.PLAIN, 13));
      this.status.setForeground(TEXT);
      this.tally.setForeground(QUIET_TEXT);
      statusLines.add(this.status, BorderLayout.NORTH);
      // The record line and the friend row share one slot, as tall as the row, and only one shows at a time.
      JPanel lower = new JPanel(new BorderLayout());
      this.layoutFriendRow(this.barsWidth, column);
      if (!column) {   // in the column the face-off takes up any change in height instead, and the link has a row of its own
         lower.setPreferredSize(new Dimension(this.barsWidth, this.friendRow.getPreferredSize().height));
      }
      lower.add(this.tally, BorderLayout.CENTER);
      lower.add(this.friendRow, BorderLayout.SOUTH);
      this.layoutReviewRow();
      lower.add(this.reviewButtonHolder, BorderLayout.EAST);
      lower.add(this.reviewRow, BorderLayout.NORTH);
      statusLines.add(lower, BorderLayout.SOUTH);
      // Beside the board, the buttons line up with its top edge and the status lines with its bottom edge.
      JPanel side = new JPanel(new BorderLayout());
      if (column) {
         this.faceOff = new FaceOff(still());
         side.add(this.faceOff, BorderLayout.CENTER);
         side.setPreferredSize(new Dimension(this.barsWidth, boardSize * 3));   // the column keeps its width whatever its rows hold
         side.add(top, BorderLayout.NORTH);
         side.add(statusLines, BorderLayout.SOUTH);
         this.add(side, BorderLayout.EAST);
      } else {
         this.add(top, BorderLayout.NORTH);
         this.add(statusLines, BorderLayout.SOUTH);
      }
      // A friend's link or code from the arguments, unless it is this game before this side answered it (the page reloaded).
      if (code != null && !(this.mode == Mode.FRIEND && this.moves.startsWith(code) && this.moves.length() > code.length())) {
         this.openFriendGame(code);
      }
      // The whole window sits on the boards' dark color.
      for (JPanel panel : new JPanel[]{(JPanel) this.getContentPane(), this.modeBar, top, side, gridHolder, grid, statusLines, lower, this.friendRow, this.reviewButtonHolder, this.reviewRow, this.lessonBar}) {
         panel.setBackground(CellButton.BACKGROUND);
      }
      this.refresh();
      this.pack();
      this.setVisible(true);
   }

   /** One button per mode; the current one is lit. Short labels where the full ones are wider than the board. In a column, two to a row. */
   private JPanel modeBar(int width, boolean column) {
      JPanel bar = new JPanel(column ? new GridLayout(0, 2, GAP, GAP) : new FlowLayout(FlowLayout.CENTER, GAP, 4));
      for (Mode choice : Mode.values()) {
         JButton button = new JButton(choice.label);
         button.setFocusable(false);
         button.addActionListener(event -> {
            this.takeOver();
            this.newGame(choice);
         });
         this.modeButtons[choice.ordinal()] = button;
         bar.add(button);
      }
      this.soundButton.setFocusable(false);
      this.soundButton.setToolTipText("The game's notes on or off (M)");
      this.soundButton.addActionListener(event -> this.toggleSound());
      bar.add(this.soundButton);
      if (bar.getPreferredSize().width > width) {
         for (Mode choice : Mode.values()) {
            this.modeButtons[choice.ordinal()].setText(choice.shortLabel);
         }
      }
      if (bar.getPreferredSize().width > width) {
         this.soundButton.setText("♪");   // a note, on a phone-sized board
      }
      if (bar.getPreferredSize().width > width) {
         bar.remove(this.soundButton);   // no room on a short screen: M still works
      }
      if (!column) {
         return bar;
      }
      JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
      bar.setBackground(CellButton.BACKGROUND);
      holder.add(bar);
      return holder;
   }

   /** The friend row: the field takes what the buttons leave of the board's width. Enter in the field opens it, like Open. */
   private void layoutFriendRow(int width, boolean column) {
      JButton open = this.openButton;
      for (JButton button : new JButton[]{this.copyButton, open, this.takeBackButton, this.friendReviewButton}) {
         button.setFocusable(false);
      }
      this.copyButton.addActionListener(event -> this.copyFriendLink());
      open.addActionListener(event -> this.openFriendGame(this.friendField.getText()));
      this.friendField.addActionListener(event -> this.openFriendGame(this.friendField.getText()));
      this.takeBackButton.addActionListener(event -> this.takeBack());
      this.friendField.setToolTipText("The " + this.thing() + " to send. Paste your friend's here and press Open.");
      // Review takes Open's place once the game ends, so room is kept for the wider of the two.
      int others = Math.max(open.getPreferredSize().width, this.friendReviewButton.getPreferredSize().width) + this.takeBackButton.getPreferredSize().width;
      if (width - others - this.copyButton.getPreferredSize().width - GAP * 5 < 60) {
         this.copyButton.setText("Copy");   // a short board: Take back must stay on the row
      }
      int fieldWidth = Math.max(40, width - others - this.copyButton.getPreferredSize().width - GAP * 5);
      JPanel buttons = this.friendRow;
      if (column) {
         // Beside the board there is height to spare: the link gets the column's whole width, the buttons a row under it.
         this.copyButton.setText(this.friendPage == null ? "Copy code" : "Copy link");
         fieldWidth = width - GAP * 2;
         buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP, 0));
         buttons.setBackground(CellButton.BACKGROUND);
         this.friendRow.setLayout(new BorderLayout(0, 4));
         this.friendRow.add(this.friendField, BorderLayout.NORTH);
         this.friendRow.add(buttons, BorderLayout.SOUTH);
      } else {
         this.friendRow.add(this.friendField);
      }
      this.friendField.setPreferredSize(new Dimension(fieldWidth, this.copyButton.getPreferredSize().height));
      buttons.add(this.copyButton);
      buttons.add(open);
      buttons.add(this.friendReviewButton);
      buttons.add(this.takeBackButton);
      this.friendReviewButton.addActionListener(event -> this.startReview());
   }

   /** The Review game button, and the row that steps through a review: back, forward, the next mistake, and Done. */
   private void layoutReviewRow() {
      this.reviewButton.setFocusable(false);
      this.reviewButton.addActionListener(event -> this.startReview());
      this.reviewButtonHolder.add(this.reviewButton);
      this.learnButton.setFocusable(false);
      this.learnButton.setToolTipText("Learn the rules in six moves (L)");
      this.learnButton.addActionListener(event -> this.startLesson());
      this.reviewButtonHolder.add(this.learnButton);
      this.hintsButton.setFocusable(false);
      this.hintsButton.setToolTipText("Show how good each move is (H)");
      this.hintsButton.addActionListener(event -> this.toggleHints());
      this.reviewButtonHolder.add(this.hintsButton);
      this.addReviewButton("<", "Back a move (left arrow)", () -> this.reviewJump(this.reviewAt - 1, false));
      this.addReviewButton(">", "Forward a move (right arrow)", () -> this.reviewJump(this.reviewAt + 1, true));
      this.addReviewButton("Next mistake", "The next mistake or blunder (N)", this::nextMistake);
      this.addReviewButton("Done", "Back to the game (Esc)", this::endReview);
   }

   private void layoutLessonBar() {
      this.addBarButton(this.lessonBack, "The step before", () -> this.lessonGo(this.lessonStep - 1), this.lessonBar);
      this.addBarButton(new JButton("Again"), "This step from the start", () -> this.lessonGo(this.lessonStep), this.lessonBar);
      this.addBarButton(this.lessonNext, "The next step", this::lessonNext, this.lessonBar);
      this.addBarButton(new JButton("Done"), "Back to the game (Esc)", this::endLesson, this.lessonBar);
      // Dressed like the mode buttons they stand in for; Next lights up once the step is done (refreshLesson).
      for (java.awt.Component button : this.lessonBar.getComponents()) {
         button.setBackground(BUTTON);
         button.setForeground(TEXT);
      }
      this.lessonBar.setVisible(false);
   }

   private void addReviewButton(String label, String name, Runnable action) {
      this.addBarButton(new JButton(label), name, action, this.reviewRow);
   }

   private void addBarButton(JButton button, String name, Runnable action, JPanel row) {
      button.setFocusable(false);
      button.setToolTipText(name);
      button.getAccessibleContext().setAccessibleName(name);
      button.addActionListener(event -> action.run());
      row.add(button);
   }

   /** What a game is sent as: a link on the web, a code on the desktop. */
   private String thing() {
      return this.friendPage == null ? "code" : "link";
   }

   /** Starts over in the given mode. */
   private void newGame(Mode choice) {
      this.stopReview();
      this.lessonStep = -1;
      this.computerTimer.stop();
      this.stopThinking();
      this.nextGameTimer.stop();
      this.computerPlayedX = this.watching;
      this.hinted = false;
      boolean backToFriend = choice == Mode.FRIEND && this.mode != Mode.FRIEND;
      this.mode = choice;
      this.record.setLastMode(choice);
      if (backToFriend) {
         this.restoreFriendGame();   // the game with the friend, which another mode only set aside
      } else {
         this.setState(new GameState());
         this.moves = "";
         this.friendWaiting = false;
         this.saveFriendGame();
      }
      this.refresh();
      this.moveSoon();
   }

   /**
    * W: the computer plays both sides (a short Hard search each, so the page waits little) until W again or a click on
    * a cell. From two players or Friend by link it first starts a game against the computer on Easy, so the game with
    * a friend is only set aside. A game the computer played X in does not count in the record.
    */
   final void toggleWatch() {
      if (this.review != null || this.lessonStep >= 0) {
         return;
      }
      if (!this.watching && !this.mode.vsComputer()) {
         this.newGame(Mode.EASY);
      }
      this.watching = !this.watching;
      this.computerPlayedX |= this.watching;
      this.computerTimer.stop();
      this.nextGameTimer.stop();
      this.refresh();
      if (this.watching && this.state.winner != null) {
         this.nextGameTimer.restart();
      }
      this.moveSoon();
   }

   /** The player clicked: X (and the mode buttons) are theirs again. */
   private void takeOver() {
      if (this.watching) {
         this.toggleWatch();
      }
   }

   private void restoreFriendGame() {
      GameState saved = FriendLink.replay(this.record.friendMoves());
      boolean resume = saved != null && saved.winner == null;
      this.setState(resume ? saved : new GameState());
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
      this.takeOver();
      this.computerTimer.stop();
      this.mode = Mode.FRIEND;
      this.record.setLastMode(Mode.FRIEND);
      this.setState(game);
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
      this.setState(FriendLink.replay(this.moves));
      this.friendWaiting = false;
      this.saveFriendGame();
      this.refresh();
   }

   /** A new position, shown at once: nothing is left drawing in. */
   private void setState(GameState state) {
      this.state = state;
      Arrays.fill(this.placedAt, 0);
      Arrays.fill(this.boardWonAt, 0);
      this.gameWonAt = 0;
      this.trailAt = 0;
      this.pulseAt = 0;
   }

   /**
    * Plays a move and adds it to the game's code. Its mark draws in, then the line through a board it wins and that
    * board's big mark, then the end of the game.
    */
   private boolean play(int board, int cell) {
      if (!this.drawIn(board, cell)) {
         return false;
      }
      this.moves += FriendLink.moveCode(board * 9 + cell);
      return true;
   }

   /** Plays a move on the board shown and starts it drawing in (see play); false if the move is not allowed. */
   private boolean drawIn(int board, int cell) {
      String before = this.state.boardResults[board];
      boolean x = this.state.xTurn;
      if (!this.state.play(board, cell)) {
         return false;
      }
      long now = System.currentTimeMillis();
      this.placedAt[board * 9 + cell] = now;
      this.sounds.mark(cell, x);
      long end = now + CellButton.MARK_MS;
      // The rule seen: a trail flies from the cell just played to the board it sends the next player to, and that board's outline pulses as it lands.
      this.trailAt = 0;
      if (this.state.winner == null && this.state.activeBoard >= 0 && !still()) {
         this.trailAt = now + CellButton.MARK_MS / 2;
         this.trailFrom = board * 9 + cell;
         this.trailTo = this.state.activeBoard;
         this.pulseAt = this.trailAt + CellButton.TRAIL_MS;
      }
      if (before == null && this.state.boardResults[board] != null) {
         this.boardWonAt[board] = end;
         this.sounded(GameState.winningLine(this.state.marks[board]), x, (int) (end - now), CellButton.LINE_MS, 0);
         end += CellButton.LINE_MS + CellButton.BIG_MARK_MS;
      }
      if (this.state.winner != null) {
         this.gameWonAt = end;
         this.sounded(GameState.winningLine(this.state.boardResults), x, (int) (end - now), CellButton.GAME_OVER_MS, 12);
         end += CellButton.GAME_OVER_MS;
      }
      // The computer (and the hints' search, which hold up the page) waits for the trail to land, not for its pulse.
      this.settleAt = Math.max(end, this.trailAt == 0 ? 0 : this.pulseAt);
      this.animateUntil = Math.max(this.animateUntil, Math.max(end, this.trailAt == 0 ? 0 : this.pulseAt + CellButton.PULSE_MS));
      this.animation.start();
      return true;
   }

   /** The visitor's system asks for less motion (the browser build's shim answers; the desktop JDK says null). */
   private static boolean still() {
      return Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
   }

   /** A board or the game just finished: its winning line's notes in time with the drawing, or a draw's two notes. */
   private void sounded(int[] line, boolean x, int delay, int millis, int octave) {
      if (line == null) {
         this.sounds.drawn(delay);
      } else {
         this.sounds.line(line, x, delay, millis, octave);
      }
   }

   /** M or Sound: the game's notes off or on, kept for next time. */
   final void toggleSound() {
      this.sounds.setOn(!this.sounds.isOn());
      this.record.setSoundOn(this.sounds.isOn());
      this.refresh();
   }

   private void animate() {
      this.repaintCells();
      if (System.currentTimeMillis() > this.animateUntil) {
         this.animation.stop();
      }
   }

   private void repaintCells() {
      for (SmallBoard board : this.boards) {
         for (CellButton cell : board.cells) {
            cell.repaint();
         }
      }
   }

   /** True if the next move may go on that board (the boards outlined in the color of the side to move). */
   final boolean playable(int board) {
      return this.state.boardResults[board] == null && this.state.winner == null && (this.state.activeBoard == -1 || this.state.activeBoard == board);
   }

   /** The move previewed with a ghost mark, or -1. */
   final int previewMove() {
      return this.previewMove >= 0 && this.canPreview(this.previewMove) ? this.previewMove : -1;
   }

   /** The boards the previewed move sends the opponent to, or null when nothing is previewed (or the move ends the game). */
   final boolean[] previewTargets() {
      return this.previewTargets;
   }

   /** True if the opponent could win a small board on that cell after the previewed move. */
   final boolean previewDanger(int move) {
      return this.previewDangers != null && this.previewDangers[move];
   }

   /** The cell the arrow keys are on, or -1. */
   final int cursorMove() {
      return this.cursorMove;
   }

   /** True if the player at this screen may make that move now (not while the computer or a friend is to move). */
   private boolean canPreview(int move) {
      return (this.lessonStep >= 0 ? !this.lessonDone : this.ours()) && this.state.canPlay(move / 9, move % 9);
   }

   /** A game is on (not a lesson or a review) and the next move is made at this screen. */
   private boolean ours() {
      return this.lessonStep < 0 && this.review == null && !this.watching && !this.computerToMove() && !(this.mode == Mode.FRIEND && this.friendWaiting);
   }

   /** The mouse (or, with {@code keyboard}, the arrow keys) is on that cell: show what playing there would do. */
   final void preview(int move, boolean keyboard) {
      if (!keyboard && this.cursorMove >= 0) {
         this.cursorMove = -1;   // the mouse took over; the cursor ring goes (repainted below)
      }
      this.previewMove = move;
      this.updatePreview();
   }

   /** The mouse left that cell; the keyboard cursor keeps its own preview. */
   final void endPreview(int move) {
      if (this.previewMove == move && this.cursorMove != move) {
         this.previewMove = -1;
         this.updatePreview();
      }
   }

   /** Moves the keyboard cursor (and focus) to a cell, and previews it. */
   final void focusCell(int board, int cell) {
      this.cursorMove = board * 9 + cell;
      this.boards[board].cells[cell].requestFocusInWindow();
      this.preview(this.cursorMove, true);
   }

   /**
    * Works out where the previewed move sends the opponent (by playing it on a copy), outlines those boards, and says
    * so in the status line ("Sends O to the top right board."); with no move to preview, the game's own line is back.
    */
   private void updatePreview() {
      this.previewTargets = null;
      this.previewDangers = null;
      String hint = null;
      if (this.previewMove() >= 0) {
         int board = this.previewMove / 9;
         GameState after = this.state.copy();
         after.play(board, this.previewMove % 9);
         if (after.winner != null) {
            hint = GameState.DRAW.equals(after.winner) ? "This move ends the game in a draw." : "This move wins the game!";
         } else {
            this.previewTargets = new boolean[9];
            for (int other = 0; other < 9; other++) {
               this.previewTargets[other] = after.boardResults[other] == null && (after.activeBoard == -1 || after.activeBoard == other);
            }
            boolean wins = after.boardResults[board] != null && !GameState.DRAW.equals(after.boardResults[board]);
            // Short enough to fit over a phone-sized board: the line does not wrap.
            String where = after.activeBoard == -1 ? "any open board" : "the " + CellButton.PLACES[after.activeBoard] + (wins ? "" : " board");
            hint = wins ? "Wins the board! They go to " + where + "." : "Sends " + this.opponent() + " to " + where + ".";
            // The answer it hands them: every square where they would then win a board, and whether that wins the game.
            int[] replies = after.boardWinningMoves();
            if (replies.length > 0) {
               this.previewDangers = new boolean[81];
               boolean game = false;
               for (int reply : replies) {
                  this.previewDangers[reply] = true;
                  GameState next = after.copy();
                  next.play(reply / 9, reply % 9);
                  game |= next.winner != null && !GameState.DRAW.equals(next.winner);
               }
               hint = "Careful: " + this.opponent() + (game ? " could win the game!" : " can win there!");
            }
         }
      }
      this.status.setText(hint != null ? hint : this.statusLine);
      this.repaintCells();
   }

   /** Who moves after the player previewing a move, as the status line names them. */
   private String opponent() {
      return this.lessonStep >= 0 ? "O" : this.mode.vsComputer() ? "the computer" : this.mode == Mode.FRIEND ? "your friend" : this.state.xTurn ? "O" : "X";
   }

   private boolean computerToMove() {
      return this.lessonStep < 0 && this.state.winner == null && (this.watching || this.mode.vsComputer() && !this.state.xTurn);
   }

   /** A cell was clicked: play there, or start over if the game has ended. Clicks wait while the computer thinks. */
   public final void cellClicked(int board, int cell) {
      if (this.lessonStep >= 0) {
         this.lessonClick(board, cell);
         return;
      }
      if (this.review != null) {
         return;   // the board shows a past position; the review row and the keys step through it
      }
      this.takeOver();
      if (this.state.winner != null) {
         this.newGame(this.mode);
         return;
      }
      if (this.computerToMove() || this.mode == Mode.FRIEND && this.friendWaiting) {
         return;
      }
      if (!this.play(board, cell)) {
         this.sounds.refused();
         this.refusedClick(board);
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

   /** A click where the move may not go: the status line says where it may, and those boards' outlines pulse. */
   private void refusedClick(int board) {
      this.notice = this.playable(board) ? "That square is taken."
         : this.state.activeBoard >= 0 ? "Not there: play on the " + CellButton.PLACES[this.state.activeBoard] + " board."
         : "That board is finished: pick an outlined one.";
      this.pulseAt = System.currentTimeMillis();
      this.animateUntil = Math.max(this.animateUntil, this.pulseAt + CellButton.PULSE_MS);
      this.animation.start();
      this.refresh();
   }

   private void computerMove() {
      if (!this.computerToMove()) {
         return;
      }
      int win = Computer.winningMove(this.state, this.state.legalMoves());
      if (win >= 0 || !this.watching && this.mode != Mode.HARD) {
         this.computerPlays(win >= 0 ? win : Computer.easyMove(this.state, this.random));
         return;
      }
      this.computerThink = new Computer.Think(this.state);
      this.computerThinkFor = this.moves;
      this.computerSpent = 0;
      this.thinkTimer.restart();
   }

   /** One slice of the computer's search, the board showing where it looks; once its time is up it plays the move it searched most. */
   private void computerThinkStep() {
      if (this.computerThink == null || !this.computerToMove() || !this.moves.equals(this.computerThinkFor)) {
         this.stopThinking();   // the game moved on (a new game, a lesson, a take-over)
         this.repaintCells();
         return;
      }
      this.computerThink.grow(this.random, HINT_TICK_MILLIS);
      this.computerSpent += HINT_TICK_MILLIS;
      if (this.computerSpent < (this.watching ? WATCH_THINK_MILLIS : HARD_THINK_MILLIS)) {
         this.repaintCells();
         return;
      }
      int move = this.computerThink.best();
      this.stopThinking();
      this.computerPlays(move);
   }

   private void stopThinking() {
      this.thinkTimer.stop();
      this.computerThink = null;
   }

   private void computerPlays(int move) {
      this.play(move / 9, move % 9);
      this.moved();
   }

   /** While the computer thinks: how much of its search each move has had, the most searched at 1; null otherwise. */
   final double[] thinking() {
      return this.computerThink == null ? null : this.computerThink.attention();
   }

   /** After any move: count a finished game against the computer, redraw, and hand the turn to the computer if it is O's. */
   private void moved() {
      if (this.state.winner != null && this.mode.vsComputer() && !this.computerPlayedX && !this.hinted) {
         this.record.add(this.mode, this.state.winner);
      }
      this.refresh();
      if (this.watching && this.state.winner != null) {
         this.nextGameTimer.restart();
      }
      this.moveSoon();
   }

   /** Hands the turn to the computer if it is its move. */
   private void moveSoon() {
      if (this.computerToMove()) {
         // The computer waits for the move to draw in (a won board takes longer), since a Hard search holds up the page.
         this.computerTimer.setInitialDelay((int) Math.max(MOVE_DELAY_MILLIS, this.settleAt - System.currentTimeMillis()));
         this.computerTimer.restart();
      }
   }

   /** Redraws every cell from the game state (marks, won/drawn boards, the boards the next move may use, the last move), the mode buttons and the status lines. */
   private void refresh() {
      for (int board = 0; board < this.boards.length; board++) {
         for (int cell = 0; cell < 9; cell++) {
            this.boards[board].cells[cell].show(this.state.marks[board][cell], this.playable(board), this.state.lastMove == board * 9 + cell);
         }
      }

      for (Mode choice : Mode.values()) {
         JButton button = this.modeButtons[choice.ordinal()];
         button.setBackground(choice == this.mode ? CellButton.X_COLOR : BUTTON);
         button.setForeground(choice == this.mode ? Color.BLACK : TEXT);
      }
      this.soundButton.setBackground(this.sounds.isOn() ? CellButton.X_COLOR : BUTTON);
      this.soundButton.setForeground(this.sounds.isOn() ? Color.BLACK : TEXT);
      this.soundButton.getAccessibleContext().setAccessibleName(this.sounds.isOn() ? "Sound on (M)" : "Sound off (M)");

      String winner = this.state.winner;
      this.refreshFaceOff();
      if (this.lessonStep >= 0) {
         this.refreshLesson();
         return;
      }
      if (this.review != null) {
         this.refreshReview();
         return;
      }
      this.setTitle(this.watching ? "Ultimate Tic-Tac-Toe: the computer plays both sides"
         : winner == null ? "Ultimate Tic-Tac-Toe: " + (this.state.xTurn ? "X" : "O") + " to move"
         : GameState.DRAW.equals(winner) ? "Ultimate Tic-Tac-Toe: draw. Click to play again."
         : "Ultimate Tic-Tac-Toe: " + winner + " wins! Click to play again.");
      this.updateHints();
      this.statusLine = this.notice != null ? this.notice : this.hintsShown() ? this.hintText() : this.statusText();
      this.notice = null;
      this.updatePreview();
      // Once a game ends, Review game sits beside the record (shortened to leave it room), or in place of Open.
      // Before the first move, How to play sits there instead.
      boolean reviewable = winner != null && !this.watching;
      boolean fresh = winner == null && this.moves.isEmpty() && !this.watching;
      String level = this.mode == Mode.HARD ? "Hard" : "Easy";
      boolean friend = this.mode == Mode.FRIEND;
      // One button beside the record: Review game at the end, How to play before the first move (unless hints are on), else Hints.
      this.reviewButton.setVisible(reviewable);
      this.learnButton.setVisible(fresh && !this.hints);
      this.hintsButton.setVisible(winner == null && !(fresh && !this.hints));
      String[] tallies = this.watching ? new String[]{"Games the computer plays X in do not count.", "Computer's games don't count."}
         : this.hinted && winner == null ? new String[]{"With hints, this game won't count.", "Hints on: won't count."}
         : this.mode.vsComputer() ? new String[]{level + ": " + this.record.describe(this.mode), level + ": " + this.record.describeShort(this.mode), this.record.describeShort(this.mode)}
         : new String[]{" "};
      this.fitTally(tallies, !friend && !this.watching);
      this.hintsButton.setBackground(this.hints ? CellButton.X_COLOR : BUTTON);
      this.hintsButton.setForeground(this.hints ? Color.BLACK : TEXT);
      this.showSlots(false, !friend, friend, !friend && !this.watching);
      this.openButton.setVisible(!reviewable);
      this.friendReviewButton.setVisible(reviewable);
      this.friendField.setText(FriendLink.message(this.friendPage, this.moves));
      this.copyButton.setEnabled(!this.moves.isEmpty());
      this.takeBackButton.setEnabled(this.friendWaiting);
   }

   /** The face-off beside the board: each side's name as the status line calls it, and the boards each has won. */
   private void refreshFaceOff() {
      if (this.faceOff == null) {
         return;
      }
      String mine = this.friendWaiting == this.state.xTurn ? "O" : "X";   // Friend by link: the side this screen plays
      String xName = this.watching ? "Computer" : this.lessonStep >= 0 || this.mode.vsComputer() ? "You"
         : this.mode == Mode.FRIEND ? ("X".equals(mine) ? "You" : "Friend") : "Player X";
      String oName = this.watching || this.lessonStep < 0 && this.mode.vsComputer() ? "Computer" : this.lessonStep >= 0 ? "O"
         : this.mode == Mode.FRIEND ? ("O".equals(mine) ? "You" : "Friend") : "Player O";
      String winner = this.state.winner;
      this.faceOff.show(xName, oName, this.state.boardResults, winner == null ? (this.state.xTurn ? "X" : "O") : null, winner,
         this.review == null && this.computerToMove());
   }

   /** Shows the first of the lines (longest first) that fits beside the button on the record line, the full one as its tooltip. */
   private void fitTally(String[] lines, boolean withButton) {
      int room = this.barsWidth - (withButton ? this.reviewButtonHolder.getPreferredSize().width : 0) - GAP * 2;
      FontMetrics metrics = this.tally.getFontMetrics(this.tally.getFont());
      String fit = lines[lines.length - 1];
      for (String line : lines) {
         if (metrics.stringWidth(line) <= room) {
            fit = line;
            break;
         }
      }
      this.tally.setText(fit);
      this.tally.setToolTipText(fit.equals(lines[0]) ? null : lines[0]);
   }

   /** Shows the review's chart and row, or the mode buttons with the record line, the friend row and Review game. */
   private void showSlots(boolean reviewing, boolean tally, boolean friend, boolean reviewButton) {
      this.modeBar.setVisible(!reviewing && this.lessonStep < 0);
      this.lessonBar.setVisible(this.lessonStep >= 0);
      this.graph.setVisible(reviewing);
      this.reviewRow.setVisible(reviewing);
      this.tally.setVisible(tally && !reviewing);
      this.friendRow.setVisible(friend && !reviewing);
      this.reviewButtonHolder.setVisible(reviewButton && !reviewing);
      this.getContentPane().validate();
      this.graph.repaint();
   }

   // ---- how to play ----

   /** L or How to play: the six steps of the lesson on this board; the game is set aside until Done. */
   final void startLesson() {
      if (this.lessonStep >= 0 || this.review != null) {
         return;
      }
      this.takeOver();
      this.computerTimer.stop();
      this.nextGameTimer.stop();
      this.lessonSaved = this.state;
      this.boards[4].cells[4].requestFocusInWindow();   // for Esc and the arrow keys
      this.lessonGo(0);
   }

   /** Shows a step from its start. */
   private void lessonGo(int step) {
      if (step < 0 || step >= Lesson.STEPS.length) {
         return;
      }
      this.lessonStep = step;
      this.lessonDone = false;
      this.setState(Lesson.STEPS[step].start());
      this.previewMove = -1;
      this.cursorMove = -1;
      this.refresh();
   }

   /** The next step; after the last, Play starts a game against the computer (on Easy unless one was on). */
   private void lessonNext() {
      if (this.lessonStep + 1 < Lesson.STEPS.length) {
         this.lessonGo(this.lessonStep + 1);
      } else {
         this.endLesson();
         this.newGame(this.mode.vsComputer() ? this.mode : Mode.EASY);
      }
   }

   /** Done (Esc, or L again): the game that was set aside is back as it was. */
   final void endLesson() {
      if (this.lessonStep < 0) {
         return;
      }
      this.lessonStep = -1;
      this.setState(this.lessonSaved);
      this.refresh();
      this.moveSoon();
   }

   /** A move that does the step's task is played; any other is turned away, with why. Once done, a click goes on. */
   private void lessonClick(int board, int cell) {
      if (this.lessonDone) {
         this.lessonNext();
         return;
      }
      String wrong = !this.state.canPlay(board, cell) ? "Not there: an empty square on an outlined board."
         : Lesson.STEPS[this.lessonStep].wrong(this.state, board * 9 + cell);
      if (wrong != null) {
         this.notice = wrong;
         this.sounds.refused();
      } else {
         this.drawIn(board, cell);
         this.lessonDone = true;
      }
      this.refresh();
   }

   private void refreshLesson() {
      Lesson.Step step = Lesson.STEPS[this.lessonStep];
      String wrong = this.notice;
      this.notice = null;
      this.statusLine = this.lessonDone ? step.done : step.task;
      this.updatePreview();
      if (wrong != null) {
         this.status.setText(wrong);   // over the preview of the cell just clicked, until the mouse moves on
      }
      this.setTitle("Ultimate Tic-Tac-Toe: how to play, step " + (this.lessonStep + 1) + " of " + Lesson.STEPS.length);
      this.tally.setText(step.rule);
      this.lessonBack.setEnabled(this.lessonStep > 0);
      this.lessonNext.setText(this.lessonStep + 1 < Lesson.STEPS.length ? "Next" : "Play");
      this.lessonNext.setBackground(this.lessonDone ? CellButton.X_COLOR : BUTTON);
      this.lessonNext.setForeground(this.lessonDone ? Color.BLACK : TEXT);
      this.showSlots(false, true, false, false);
   }

   // ---- hints ----

   /** H or Hints: on (or off) for every game until turned off again. */
   final void toggleHints() {
      this.hints = !this.hints;
      this.refresh();
   }

   /** Hints are on and it is your move in a game going on. */
   private boolean hintsWanted() {
      return this.hints && this.state.winner == null && this.ours();
   }

   /** The board shows the hints for the position on it (they are left off in a lesson and a review). */
   private boolean hintsShown() {
      return this.hintsWanted() && this.hintChances != null && this.moves.equals(this.thinkFor);
   }

   /** Starts a search for a new position once its last move has drawn in, or stops the search when hints are not wanted. */
   private void updateHints() {
      if (!this.hintsWanted()) {
         this.hintTimer.stop();
         this.hintChances = null;
         return;
      }
      this.hinted |= this.mode.vsComputer();
      if (!this.moves.equals(this.thinkFor)) {
         this.think = new Computer.Think(this.state);
         this.thinkFor = this.moves;
         this.thinkSpent = 0;
      }
      this.takeHints();
      if (this.thinkSpent < HINT_THINK_MILLIS && !this.hintTimer.isRunning()) {
         this.hintTimer.setInitialDelay((int) Math.max(HINT_TICK_MILLIS, this.settleAt - System.currentTimeMillis()));
         this.hintTimer.restart();
      }
   }

   /** One slice of the search; the board shows the chances settle, and the status line names the best once done. */
   private void hintStep() {
      if (!this.hintsWanted() || !this.moves.equals(this.thinkFor)) {
         this.hintTimer.stop();
         return;
      }
      this.think.grow(this.random, HINT_TICK_MILLIS);
      this.thinkSpent += HINT_TICK_MILLIS;
      this.takeHints();
      if (this.thinkSpent >= HINT_THINK_MILLIS) {
         this.hintTimer.stop();
         this.refresh();   // the status line and the cells' names for screen readers
      } else {
         this.repaintCells();
      }
   }

   private void takeHints() {
      this.hintChances = this.think.chances();
      this.hintDoubts = this.think.doubts();
      this.hintBest = this.think.best();
   }

   /** Short, to fit over a phone-sized board. */
   private String hintText() {
      return this.thinkSpent < HINT_THINK_MILLIS || this.hintBest < 0 ? "Hints: thinking..."
         : "Hints: the gold ring is best (" + percent(this.hintChances[this.hintBest]) + ").";
   }

   /** The chances of that move for the side to move, or NaN when the board shows no hint for it. */
   final double hintChance(int move) {
      return this.hintsShown() ? this.hintChances[move] : Double.NaN;
   }

   /** The search's pick while hints show, else -1. */
   final int hintBest() {
      return this.hintsShown() ? this.hintBest : -1;
   }

   /**
    * How a move compares with the pick, graded as a review would grade it, counting only the shortfall beyond what the
    * search is unsure of (Think.doubts); null where no hint shows.
    */
   final Review.Grade hintGrade(int move) {
      double chance = this.hintChance(move);
      return Double.isNaN(chance) || this.hintBest < 0 ? null
         : Review.Grade.of(move == this.hintBest, Math.max(0, this.hintChances[this.hintBest] - chance - this.hintDoubts[move]));
   }

   // ---- review ----

   /** The review being shown, or null. */
   final Review review() {
      return this.review;
   }

   /** The position on the board while reviewing (the game after this many moves). */
   final int reviewAt() {
      return this.reviewAt;
   }

   /** R or Review game, once a game has ended: look it over from the end, while the computer analyses each position. */
   final void startReview() {
      if (this.review != null || this.lessonStep >= 0 || this.watching || this.state.winner == null) {
         return;
      }
      this.review = Review.of(this.moves);
      if (this.review == null || this.review.length() == 0) {
         this.review = null;
         return;
      }
      this.reviewAt = this.review.length();
      this.previewMove = -1;
      this.cursorMove = -1;
      this.boards[4].cells[4].requestFocusInWindow();   // for the arrow keys
      this.reviewTimer.start();
      this.refresh();
   }

   /** Done (Esc, or R again): back to the finished game. */
   final void endReview() {
      if (this.review == null) {
         return;
      }
      this.stopReview();
      this.setState(FriendLink.replay(this.moves));
      this.refresh();
   }

   private void stopReview() {
      this.reviewTimer.stop();
      this.review = null;
   }

   /** Shows the game after {@code at} moves; {@code drawIn} draws the last of them in, for stepping forward. */
   final void reviewJump(int at, boolean drawIn) {
      if (this.review == null) {
         return;
      }
      at = Math.max(0, Math.min(this.review.length(), at));
      if (at == this.reviewAt) {
         return;
      }
      boolean animate = drawIn && at > 0 && at == this.reviewAt + 1;
      this.reviewAt = at;
      if (animate) {
         this.setState(this.review.positions[at - 1].copy());
         int move = this.review.moves[at - 1];
         this.drawIn(move / 9, move % 9);
      } else {
         this.setState(this.review.positions[at].copy());
      }
      this.refresh();
   }

   /** Jumps to the next mistake or blunder (either side's), around to the first after the last. */
   private void nextMistake() {
      if (this.review == null) {
         return;
      }
      int next = this.review.nextBad(this.reviewAt - 1);
      if (next >= 0) {
         this.reviewJump(next + 1, true);
      } else {
         this.notice = this.review.done() ? "No mistakes or blunders in this game." : "None found yet: still analysing.";
         this.refresh();
      }
   }

   /** The arrow keys, Home, End, N and Esc (or R) while reviewing, and R to start one; true if the key was used. */
   final boolean reviewKey(int keyCode) {
      if (keyCode == KeyEvent.VK_M) {
         this.toggleSound();
         return true;
      }
      if (this.lessonStep >= 0) {
         if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_L) {
            this.endLesson();
            return true;
         }
         return keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_R;   // not while the lesson has the board; the arrows and Enter still play
      }
      if (this.review == null) {
         if (keyCode == KeyEvent.VK_R && this.state.winner != null && !this.watching) {
            this.startReview();
            return true;
         }
         if (keyCode == KeyEvent.VK_L) {
            this.startLesson();
            return true;
         }
         if (keyCode == KeyEvent.VK_H) {
            this.toggleHints();
            return true;
         }
         return false;
      }
      switch (keyCode) {
         case KeyEvent.VK_LEFT:
         case KeyEvent.VK_UP:
            this.reviewJump(this.reviewAt - 1, false);
            break;
         case KeyEvent.VK_RIGHT:
         case KeyEvent.VK_DOWN:
            this.reviewJump(this.reviewAt + 1, true);
            break;
         case KeyEvent.VK_HOME:
            this.reviewJump(0, false);
            break;
         case KeyEvent.VK_END:
            this.reviewJump(this.review.length(), false);
            break;
         case KeyEvent.VK_N:
            this.nextMistake();
            break;
         case KeyEvent.VK_ESCAPE:
         case KeyEvent.VK_R:
            this.endReview();
            break;
         case KeyEvent.VK_TAB:
            return false;   // still moves focus
         default:
            break;   // Enter, Space and W do nothing on a past position
      }
      return true;
   }

   /** One position's search per tick; the chart, the badges and the status line fill in as it goes. */
   private void analyseStep() {
      if (this.review == null || !this.review.analyseNext(this.random, REVIEW_THINK_MILLIS)) {
         this.reviewTimer.stop();
      }
      if (this.review != null) {
         this.refresh();
      }
   }

   /** The grade of the move that reached the position shown, or null. */
   final Review.Grade reviewGrade() {
      return this.review == null || this.reviewAt == 0 ? null : this.review.grade(this.reviewAt - 1);
   }

   /** Where the search would have played instead of the move shown, when that move was an inaccuracy or worse; else -1. */
   final int reviewBetter() {
      Review.Grade grade = this.reviewGrade();
      return grade != null && grade.slip() ? this.review.better[this.reviewAt - 1] : -1;
   }

   private void refreshReview() {
      this.statusLine = this.notice != null ? this.notice : this.reviewStatus();
      this.notice = null;
      this.status.setText(this.statusLine);
      this.setTitle("Ultimate Tic-Tac-Toe review: " + (this.reviewBetter() >= 0 ? "the gold ring is a better move." : this.reviewSummary()));
      this.showSlots(true, false, false, false);
      this.repaintCells();
   }

   /** "Move 12, you: Mistake (62% to 41%)", kept short to fit over a phone-sized board: the line does not wrap. */
   private String reviewStatus() {
      Review review = this.review;
      if (this.reviewAt == 0) {
         return "The start. Step through with > or the arrows.";
      }
      int k = this.reviewAt - 1;
      if (this.reviewAt == review.length() && review.done()) {
         int turn = review.turningPoint();
         return this.endText() + (turn >= 0 ? " Turning point: move " + (turn + 1) + "." : " No real mistakes.");
      }
      String text = "Move " + this.reviewAt + ", " + this.side(review.byX(k)) + ": ";
      Review.Grade grade = review.grade(k);
      if (grade == null) {
         return text + "analysing...";
      }
      text += grade.label;
      if (grade.slip()) {
         text += " (" + percent(review.moverChancesAfter(k)) + ", best " + percent(review.moverChancesInstead(k)) + ")";
      }
      return text;
   }

   /** The result, as the review's last position shows it. */
   private String endText() {
      String winner = this.review.positions[this.review.length()].winner;
      return GameState.DRAW.equals(winner) ? "A draw." : capital(this.side("X".equals(winner))) + " won.";
   }

   /** "You: 1 mistake. The computer: no slips." for the title bar; how far the analysis is until it is done. */
   private String reviewSummary() {
      if (!this.review.done()) {
         return "analysing " + this.review.analysed() + " of " + (this.review.length() + 1) + " positions.";
      }
      return capital(this.side(true)) + ": " + this.slips(true) + ". " + capital(this.side(false)) + ": " + this.slips(false) + ".";
   }

   /** One side's inaccuracies, mistakes and blunders, like "2 mistakes, 1 blunder", or "no slips". */
   private String slips(boolean x) {
      int[] counts = this.review.count(x);
      StringBuilder text = new StringBuilder();
      for (Review.Grade grade : new Review.Grade[]{Review.Grade.INACCURACY, Review.Grade.MISTAKE, Review.Grade.BLUNDER}) {
         int count = counts[grade.ordinal()];
         if (count > 0) {
            text.append(text.length() == 0 ? "" : ", ").append(count).append(' ').append(count == 1 ? grade.label.toLowerCase() : grade.plural);
         }
      }
      return text.length() == 0 ? "no slips" : text.toString();
   }

   /** What the review calls a side: you and the computer in a game against it, else X and O. */
   private String side(boolean x) {
      return this.mode.vsComputer() && !this.computerPlayedX ? (x ? "you" : "the computer") : x ? "X" : "O";
   }

   private static String capital(String text) {
      return Character.toUpperCase(text.charAt(0)) + text.substring(1);
   }

   private static String percent(double chances) {
      return Math.round(chances * 100) + "%";
   }

   private String statusText() {
      if (this.mode == Mode.FRIEND) {
         return this.friendStatusText();
      }
      String winner = this.state.winner;
      if (this.watching) {
         return winner == null ? "Computer vs computer: click to take over."
            : (GameState.DRAW.equals(winner) ? "A draw." : winner + " wins.") + " Next game...";
      }
      if (!this.mode.vsComputer()) {
         return winner == null ? (this.state.xTurn ? "X" : "O") + " to move, " + this.whereToPlay()
            : (GameState.DRAW.equals(winner) ? "A draw." : winner + " wins!") + " Click any cell to play again.";
      }
      if (winner != null) {
         return ("X".equals(winner) ? "You win!" : "O".equals(winner) ? "The computer wins." : "A draw.") + " Click any cell to play again.";
      }
      return this.state.xTurn ? "Your move (X), " + this.whereToPlay() : "The computer (O) is thinking...";
   }

   /** Where the side to move may play, kept short to fit over a phone-sized board: "on the top right board." */
   private String whereToPlay() {
      return this.moves.isEmpty() ? "anywhere to start."
         : this.state.activeBoard >= 0 ? "on the " + CellButton.PLACES[this.state.activeBoard] + " board."
         : "on any outlined board.";
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
      return "Your move (" + side + "). Theirs is on the light tile.";
   }
}
