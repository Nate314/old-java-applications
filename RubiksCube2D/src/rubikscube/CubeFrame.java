package rubikscube;

import java.awt.Color;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

/**
 * The window: the cube (in 3D, or unfolded as the original net), the w/a/s/d buttons, Mix, and the level ladder. Level n is a solved cube mixed by n
 * moves that the player watches happen, then turns back; solving it at par (n moves) wins three stars and unlocks
 * the next level. Hint shows the next move back, Undo takes a move back and Solve plays the way back.
 */
public final class CubeFrame extends JFrame implements ActionListener {
   private static final File RECORDS_FILE = new File(System.getProperty("user.home", "."), ".rubikscube-levels.txt");
   private static final Color HINT_KEY = new Color(80, 220, 80);
   private static final String STAR = "★";
   private static final String NO_STAR = "☆";
   private JButton[][] stickerButtons = new JButton[6][9];
   private JButton[] moveButtons = new JButton[]{new JButton("w"), new JButton("a"), new JButton("s"), new JButton("d")};
   private JButton mixButton = new JButton("Mix");
   private JLabel[] mixLabels = new JLabel[]{new JLabel("Moves:"), new JLabel("Secs:")};
   private JTextField[] mixFields = new JTextField[]{new JTextField("1000"), new JTextField("0.5")};
   private final JButton prevButton = new JButton("< Prev");
   private final JButton nextButton = new JButton("Next >");
   private final JButton retryButton = new JButton("Retry");
   private final JButton undoButton = new JButton("Undo");
   private final JButton hintButton = new JButton("Hint");
   private final JButton solveButton = new JButton("Solve");
   private final JLabel levelLabel = new JLabel();
   private final JLabel goalLabel = new JLabel();
   private final JLabel movesLabel = new JLabel();
   private final JLabel bestLabel = new JLabel();
   private final JLabel totalLabel = new JLabel();
   private final JLabel messageLabel = new JLabel();
   private final JLabel helpLabel = new JLabel(
         // Lines are broken by hand: the browser build's labels do not wrap.
         "<html>Click a square, then<br>press W A S D (or the<br>buttons) to turn its row,<br>column or ring.<br><br>"
               + "Each level mixes the cube<br>one move more while you<br>watch. Turn it back to<br>solved: at par for 3 stars.<br><br>"
               + "Keys: Z undo, H hint,<br>R retry, N next, P prev.<br>T: Teach me. G: patterns.</html>");
   private static final String HELP_3D =
         "<html>Drag a square to turn<br>its row, column or ring.<br>Drag around the cube<br>(or press the arrow keys)<br>to look at other sides.<br>"
               + "Or click a square, then<br>press W A S D.<br><br>"
               + "Keys: Z undo, H hint,<br>R retry, N next, P prev.<br>V: flat net. T: Teach me.<br>G: patterns to make.</html>";
   private final String helpNet;
   private final JButton viewButton = new JButton();
   private final JButton teachButton = new JButton("Teach me");
   private final CoachPanel coachPanel = new CoachPanel();
   private final JButton patternButton = new JButton("Patterns");
   private final PatternPanel patternPanel = new PatternPanel(this::patternClick);
   /** The Patterns card is up in place of the message and help (and of Teach me). */
   private boolean patterning = false;
   /** The pattern on the card, or null for the list. */
   private Patterns.Pattern pattern = null;
   /** The pattern last shown or made on the cube, while the cube is still that run's (null after a level or Mix). */
   private Patterns.Pattern onCube = null;
   /** Make it is under way: the stickers after each of the pattern's turns from solved, and how many are made. */
   private boolean making = false;
   private final List<int[]> patternPath = new ArrayList<>();
   private int patternDone = 0;
   /** Turns made off the pattern since it was last on its way, to be turned back (last first). */
   private final List<Integer> offTurns = new ArrayList<>();
   /** The turn Show me is at, or -1 when it is not playing. */
   private int showAt = -1;
   private String patternStatus = "";
   /** Teach me is on: the coach card replaces the message and help, and every move is shown before it is made. */
   private boolean coaching = false;
   /** The coach's steps from the cube as it is (null until worked out again), the step and the turn in it now. */
   private List<Coach.Step> coachSteps = null;
   private int coachStep = 0;
   private int coachTurn = 0;
   /** The step the 3D cube was last turned to show (its yellow side up, its front toward the player), or null. */
   private Coach.Step coachShown = null;
   /** The layer turn of the player's last move, to see whether it was the one the coach asked for; -1 for none. */
   private int lastTurn = -1;
   private CubeView3D view = null;
   public int selectedCubie = 0;
   private JPanel panel = new JPanel();
   private int frameWidth = 0;
   private int frameHeight = 0;
   private int cellWidth = 0;
   private int cellHeight = 0;
   protected Cube cube = null;
   private MoveHistory history = null;
   private final LevelRecords records;
   private final Random random = new Random();
   private final Color keyColor;
   private Scrambler scrambler = null;
   /** Plays a level's mix or the Solve moves, one per tick; null when the player has the cube. */
   private Timer playback = null;
   private final Timer clock = new Timer(1000, event -> this.updateStatus());
   private final List<String[]> userMoves = new ArrayList<>();
   private String[] hinted = null;
   /** The level being played (1 to 20), or the one to return to while in free play. */
   private int level = 1;
   private boolean freePlay = false;
   private int par = 0;
   private boolean usedHint = false;
   private boolean complete = false;
   private long startMillis = 0L;
   private long solvedMillis = 0L;

   public CubeFrame() {
      this.setSize(550, 700);
      this.frameWidth = this.getWidth();
      this.frameHeight = this.getHeight();
      this.cellWidth = this.frameWidth / 11;
      this.cellHeight = this.frameHeight / 14;
      this.cube = new Cube(this, this.stickerButtons, this.cellWidth, this.cellHeight);
      this.history = new MoveHistory(this.cube);
      this.records = LevelRecords.load(RECORDS_FILE);
      this.helpNet = this.helpLabel.getText();
      this.view = new CubeView3D(this.cube, this::select, this::applyUserMove);
      this.cube.watcher = this.view;
      this.setTitle("Rubik's Cube");
      this.setLocationRelativeTo(null);
      this.setDefaultCloseOperation(3);
      this.setResizable(true);
      this.add(this.panel);
      this.panel.setLayout(null);
      this.panel.setBackground(new Color(255, 255, 255));
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new MoveKeyDispatcher(this));
      this.keyColor = this.moveButtons[0].getBackground();
      this.moveButtons[0].addActionListener(this);
      this.moveButtons[1].addActionListener(this);
      this.moveButtons[2].addActionListener(this);
      this.moveButtons[3].addActionListener(this);
      this.panel.add(this.moveButtons[0]);
      this.panel.add(this.moveButtons[1]);
      this.panel.add(this.moveButtons[2]);
      this.panel.add(this.moveButtons[3]);
      this.panel.add(this.mixLabels[0]);
      this.panel.add(this.mixLabels[1]);
      this.panel.add(this.mixFields[0]);
      this.panel.add(this.mixFields[1]);
      this.mixButton.addActionListener(this);
      this.panel.add(this.mixButton);
      this.cube.topFace.panel.setBounds(4 * this.cellWidth, this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.frontFace.panel.setBounds(4 * this.cellWidth, 4 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.rightFace.panel.setBounds(7 * this.cellWidth, 4 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.bottomFace.panel.setBounds(4 * this.cellWidth, 7 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.backFace.panel.setBounds(4 * this.cellWidth, 10 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      this.cube.leftFace.panel.setBounds(this.cellWidth, 4 * this.cellHeight, 3 * this.cellWidth, 3 * this.cellHeight);
      for (Face face : this.cube.faces()) {
         face.panel.setBackground(new Color(0, 0, 0));
         this.panel.add(face.panel);
      }
      this.layoutLevelControls();
      this.viewButton.addActionListener(this);
      this.panel.add(this.viewButton);
      this.teachButton.addActionListener(this);
      this.panel.add(this.teachButton);
      this.coachPanel.setVisible(false);
      this.panel.add(this.coachPanel);
      this.patternButton.addActionListener(this);
      this.panel.add(this.patternButton);
      this.patternPanel.setVisible(false);
      this.panel.add(this.patternPanel);
      // Last, so it lies under every other control.
      this.panel.add(this.view);
      this.showView(this.records.netView());
      this.startLevel(this.records.lastLevel());
   }

   /** The level ladder: labels and buttons right of the bottom and back faces, the message and help left of them. */
   private void layoutLevelControls() {
      int x = (int)(7.2 * this.cellWidth);
      int y = (int)(7.15 * this.cellHeight);
      int width = (int)(3.3 * this.cellWidth);
      int half = width / 2 - 3;
      this.levelLabel.setFont(new Font("Dialog", Font.BOLD, 16));
      this.levelLabel.setBounds(x, y, width, 24);
      this.goalLabel.setBounds(x, y + 26, width, 18);
      this.movesLabel.setBounds(x, y + 44, width, 18);
      this.bestLabel.setBounds(x, y + 62, width, 18);
      JButton[] buttons = {this.prevButton, this.nextButton, this.retryButton, this.undoButton, this.hintButton, this.solveButton};
      for (int index = 0; index < buttons.length; index++) {
         buttons[index].setBounds(x + index % 2 * (width - half), y + 88 + index / 2 * 36, half, 30);
         buttons[index].addActionListener(this);
         this.panel.add(buttons[index]);
      }
      this.totalLabel.setBounds(x, y + 196, width, 18);
      int left = (int)(0.3 * this.cellWidth);
      int leftWidth = (int)(3.5 * this.cellWidth);
      this.messageLabel.setFont(new Font("Dialog", Font.BOLD, 14));
      this.messageLabel.setBounds(left, y, leftWidth, 90);
      this.helpLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
      this.helpLabel.setBounds(left, y + 92, leftWidth, 196);
      this.coachPanel.setBounds(left, y - 4, leftWidth + 8, 294);
      this.patternPanel.setBounds(left, y - 4, leftWidth + 8, 294);
      for (JLabel label : new JLabel[]{this.levelLabel, this.goalLabel, this.movesLabel, this.bestLabel, this.totalLabel, this.messageLabel, this.helpLabel}) {
         this.panel.add(label);
      }
   }

   /**
    * Shows the flat net (the original window) or the 3D cube. The 3D cube takes the top half of the window; the
    * w/a/s/d buttons move under the message and Mix under the level ladder.
    */
   private void showView(boolean net) {
      int w = this.cellWidth;
      int h = this.cellHeight;
      for (Face face : this.cube.faces()) {
         face.panel.setVisible(net);
      }
      this.view.setVisible(!net);
      this.view.setBounds(0, 0, this.frameWidth, 7 * h + 2);
      this.view.reset();
      this.viewButton.setText(net ? "3D cube" : "Flat net");
      this.viewButton.setBounds(this.frameWidth - 112, 10, 92, 28);
      this.teachButton.setBounds(12, 10, 116, 28);
      this.patternButton.setBounds(134, 10, 124, 28);
      int keyWidth = (int)(2.75 * w / 3.0);
      int keyHeight = (int)(2.75 * h / 3.0);
      int keyX = net ? w + keyWidth : (int)(5.5 * w) - keyWidth / 2;
      int keyY = net ? (int)(1.75 * h) : (int)(9.0 * h);
      this.moveButtons[0].setBounds(keyX, keyY, keyWidth, keyHeight);
      for (int index = 1; index < 4; index++) {
         this.moveButtons[index].setBounds(keyX + (index - 2) * keyWidth, keyY + keyHeight, keyWidth, keyHeight);
      }
      if (net) {
         this.mixLabels[0].setBounds((int)(7.5 * w), h, (int)(1.25 * w), h / 2);
         this.mixLabels[1].setBounds((int)(8.75 * w), h, (int)(1.25 * w), h / 2);
         this.mixFields[0].setBounds((int)(7.5 * w), (int)(1.5 * h), (int)(1.25 * w), h / 2);
         this.mixFields[1].setBounds((int)(8.75 * w), (int)(1.5 * h), (int)(1.25 * w), h / 2);
         this.mixButton.setBounds((int)(7.5 * w), 2 * h, (int)(2.5 * w), (int)(1.5 * h));
      } else {
         int x = (int)(7.2 * w);
         int y = (int)(11.6 * h);
         this.mixLabels[0].setBounds(x, y, 56, 16);
         this.mixLabels[1].setBounds(x + 60, y, 50, 16);
         this.mixFields[0].setBounds(x, y + 18, 56, 26);
         this.mixFields[1].setBounds(x + 60, y + 18, 46, 26);
         this.mixButton.setBounds(x + 110, y + 18, (int)(3.3 * w) - 110, 26);
      }
      this.helpLabel.setText(net ? this.helpNet : HELP_3D);
      if (this.coaching) {
         this.coachShow();
      } else if (this.making) {
         this.patternGuide();
      } else if (this.hinted != null) {
         this.hintMessage();
      }
      this.records.setNetView(net);
      this.panel.repaint();
   }

   /** V: the other view. */
   final void toggleView() {
      this.showView(this.view.isVisible());
   }

   /** The arrow keys: turn the 3D cube to look at another side. */
   final void orbit(int right, int down) {
      if (this.view.isVisible()) {
         this.view.orbit(right, down);
      }
   }

   /** A click on a square: selects it for the next W/A/S/D. */
   final void select(String faceColor, int cubie) {
      this.cube.selectedFaceColor = faceColor;
      this.selectedCubie = cubie;
      this.markSquare(faceColor, cubie);
   }

   /** Rings one square (or none, for a null face) so the player can see which square W/A/S/D will turn. */
   private void markSquare(String faceColor, int cubie) {
      for (Face face : this.cube.faces()) {
         face.mark(face.colorName().equals(faceColor) ? cubie : 0);
      }
      this.view.mark(faceColor, cubie);
   }

   private boolean busy() {
      return this.scrambler != null || this.playback != null;
   }

   /** Applies a W/A/S/D move to the selected square, counts it and checks for a solve. */
   final void applyUserMove(String key) {
      if (this.selectedCubie < 1 || this.selectedCubie > 9 || this.cube.selectedFaceColor.isEmpty() || this.busy()) {
         return;
      }
      String[] move = {this.cube.selectedFaceColor, key, String.valueOf(this.selectedCubie)};
      CubeGeometry.Turn turn = CubeGeometry.turnOf(move[0], key, this.selectedCubie);
      this.lastTurn = turn == null ? -1 : turn.id();
      Scrambler.apply(this.history, move);
      this.userMoves.add(move);
      this.afterUserMove();
   }

   /** Undo (Z): turns the last move back and takes it off the count. */
   final void undo() {
      if (this.busy() || this.userMoves.isEmpty()) {
         return;
      }
      String[] last = this.userMoves.remove(this.userMoves.size() - 1);
      this.lastTurn = -1;
      this.history.apply(last[0], MoveHistory.inverse(last[1]), Integer.parseInt(last[2]));
      this.select(last[0], Integer.parseInt(last[2]));
      this.afterUserMove();
   }

   private void afterUserMove() {
      this.cube.refresh();
      this.clearHint();
      if (this.startMillis == 0L) {
         this.startMillis = System.currentTimeMillis();
         this.clock.start();
      }
      if (!this.complete && this.cube.isSolved()) {
         this.solved();
      } else if (!this.complete) {
         this.messageLabel.setText("");
      }
      this.updateStatus();
      if (this.coaching) {
         this.coachAfterMove();
      }
      if (this.making) {
         this.patternAfterMove();
      }
   }

   /** The player turned the cube back to solved. */
   private void solved() {
      this.complete = true;
      this.solvedMillis = System.currentTimeMillis();
      this.clock.stop();
      this.history.restart();
      int moves = this.userMoves.size();
      if (this.freePlay) {
         this.message("Solved in " + moves + (moves == 1 ? " move!" : " moves!"));
         return;
      }
      int stars = LevelRecords.starsFor(this.par, moves, this.usedHint);
      int before = this.records.stars(this.level);
      this.records.record(this.level, stars, moves);
      String text = "Level " + this.level + " solved! " + stars(stars);
      if (stars < 3) {
         text += this.usedHint ? "<br>No hints for 3 stars." : "<br>Par is " + this.par + " for 3 stars.";
      } else if (before == 3) {
         text += "<br>Perfect again.";
      }
      if (this.level < LevelRecords.LEVELS) {
         text += "<br>Next (N): level " + (this.level + 1);
      } else {
         text += "<br>That was the last level!";
      }
      this.message(text);
   }

   /** Starts level n: a solved cube mixed with n moves, shown one at a time. */
   final void startLevel(int n) {
      this.stopAll();
      this.level = Math.max(1, Math.min(LevelRecords.LEVELS, n));
      this.freePlay = false;
      this.par = this.level;
      this.records.setLastLevel(this.level);
      this.endPatternRun();
      this.resetRun();
      this.cube.reset();
      this.history.restart();
      this.message("Watch the mix...");
      this.coachAgain();
      int interval = Math.max(60, Math.min(350, 2800 / this.par));
      String[][] previous = {null};
      int[] ticks = {0};
      this.playback = new Timer(interval, event -> {
         if (this.history.depth() < this.par || this.cube.isSolved()) {
            String[] move = Scrambler.randomMoveAfter(previous[0], this.random);
            Scrambler.apply(this.history, move);
            previous[0] = move;
            this.markSquare(move[0], Integer.parseInt(move[2]));
            this.cube.refresh();
            if (++ticks[0] < 20 * this.par + 50) {
               return;
            }
         }
         this.stopPlayback();
         this.par = this.history.depth();
         this.markSquare(null, 0);
         this.message("Your turn:<br>turn it back.");
         this.updateStatus();
         this.coachAgain();
      });
      this.playback.start();
      this.updateStatus();
   }

   /**
    * Solve: plays the way back to solved one move at a time: back along the moves made, or layer by layer the
    * coach's way when that is shorter (after a long Mix). The level is not counted.
    */
   private void solveForPlayer() {
      if (this.busy() || this.history.depth() == 0) {
         return;
      }
      List<Integer> layers = this.shorterThanHistory();
      this.endPatternRun();
      this.clearHint();
      this.clock.stop();
      this.complete = true;
      this.message(layers == null ? "Watch the way back..." : "Watch it solved layer<br>by layer (Teach me<br>shows how).");
      int[] next = {0};
      int interval = Math.max(15, Math.min(300, 6000 / (layers == null ? this.history.depth() : layers.size())));
      this.playback = new Timer(interval, event -> {
         String[] move = layers == null ? this.history.hint() : next[0] < layers.size() ? this.appMove(layers.get(next[0]++)) : null;
         if (move != null) {
            Scrambler.apply(this.history, move);
            this.markSquare(move[0], Integer.parseInt(move[2]));
            this.cube.refresh();
            return;
         }
         this.stopPlayback();
         // Solved now, whichever way it went: nothing left to turn back.
         this.history.restart();
         this.markSquare(null, 0);
         this.message("Solved by the computer.<br>Retry (R) to try it<br>yourself.");
         this.updateStatus();
         this.coachAgain();
      });
      this.playback.start();
      this.updateStatus();
      this.coachAgain();
   }

   /** Hint (H): selects the square of the next move back and lights its key. */
   private void showHint() {
      if (this.busy()) {
         return;
      }
      if (this.coaching) {
         this.coachShow();
         return;
      }
      if (this.making) {
         this.patternGuide();
         return;
      }
      List<Integer> layers = this.shorterThanHistory();
      String[] move = layers == null ? this.history.hint() : this.appMove(layers.get(0));
      if (move == null) {
         this.message(this.cube.isSolved() ? "Already solved." : "No hint here: try<br>Teach me (T).");
         return;
      }
      if (!this.complete) {
         this.usedHint = true;
      }
      this.pointAt(move);
      this.hintMessage();
      this.updateStatus();
   }

   /** Says how to make the hinted move, in the words of the view shown. */
   private void hintMessage() {
      String key = this.hinted[1].toUpperCase();
      this.message(this.view.isVisible() ? "Hint: drag the ringed<br>square along the arrow<br>(or press " + key + ")."
            : "Hint: press " + key + " on<br>the ringed square.");
   }

   /** Rings the square of a move on the cube, with its arrow in 3D, and lights its key in green. */
   private void pointAt(String[] move) {
      this.clearHint();
      this.hinted = move;
      this.select(move[0], Integer.parseInt(move[2]));
      this.keyButton(move[1]).setBackground(HINT_KEY);
      this.view.hint(move[0], Integer.parseInt(move[2]), move[1]);
   }

   private JButton keyButton(String key) {
      for (JButton button : this.moveButtons) {
         if (button.getText().equals(key)) {
            return button;
         }
      }
      return this.moveButtons[0];
   }

   private void clearHint() {
      if (this.hinted != null) {
         this.keyButton(this.hinted[1]).setBackground(this.keyColor);
         this.hinted = null;
         this.view.hint(null, 0, null);
      }
   }

   private void stopPlayback() {
      if (this.playback != null) {
         this.playback.stop();
         this.playback = null;
      }
   }

   private void stopAll() {
      this.stopPlayback();
      if (this.scrambler != null) {
         this.scrambler.stop();
         this.scrambler = null;
      }
      this.clock.stop();
   }

   private void resetRun() {
      this.clearHint();
      this.userMoves.clear();
      this.usedHint = false;
      this.complete = false;
      this.startMillis = 0L;
      this.solvedMillis = 0L;
      this.selectedCubie = 0;
      this.cube.selectedFaceColor = "";
      this.markSquare(null, 0);
   }

   private void message(String html) {
      this.messageLabel.setText("<html>" + html + "</html>");
   }

   private static String stars(int count) {
      StringBuilder text = new StringBuilder();
      for (int index = 0; index < 3; index++) {
         text.append(index < count ? STAR : NO_STAR);
      }
      return text.toString();
   }

   /** Refreshes the level, goal, moves, time and best labels, the buttons and the title. */
   private void updateStatus() {
      int moves = this.userMoves.size();
      long end = this.solvedMillis != 0L ? this.solvedMillis : System.currentTimeMillis();
      long seconds = this.startMillis == 0L ? 0L : (end - this.startMillis) / 1000L;
      String time = seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
      this.movesLabel.setText("Moves: " + moves + "    Time: " + time);
      if (this.onCube != null) {
         this.levelLabel.setText("Pattern");
         this.goalLabel.setText(this.onCube.name + ": " + this.onCube.turns.size() + " turns");
         this.bestLabel.setText("Retry (R) for level " + this.level);
      } else if (this.freePlay) {
         this.levelLabel.setText("Free play");
         this.goalLabel.setText("Mixed: solve it any way");
         this.bestLabel.setText("Retry (R) for level " + this.level);
      } else {
         this.levelLabel.setText("Level " + this.level + " of " + LevelRecords.LEVELS);
         this.goalLabel.setText(this.playback != null && !this.complete ? "Mixing " + this.par + (this.par == 1 ? " move" : " moves")
               : "Par: " + this.par + (this.par == 1 ? " move" : " moves"));
         int best = this.records.stars(this.level);
         this.bestLabel.setText(best == 0 ? "Best: not solved yet" : "Best: " + stars(best) + " in " + this.records.moves(this.level));
      }
      this.totalLabel.setText("Stars: " + this.records.totalStars() + " of " + 3 * LevelRecords.LEVELS);
      boolean idle = !this.busy();
      this.prevButton.setEnabled(idle && this.level > 1);
      this.nextButton.setEnabled(idle && this.level < LevelRecords.LEVELS && this.level < this.records.unlocked());
      this.retryButton.setEnabled(idle || this.playback != null);
      this.undoButton.setEnabled(idle && !this.userMoves.isEmpty());
      this.hintButton.setEnabled(idle && this.history.depth() > 0);
      this.solveButton.setEnabled(idle && this.history.depth() > 0);
      boolean playerSolved = this.solvedMillis != 0L && moves > 0;
      if (playerSolved && !this.freePlay && !this.usedHint) {
         this.setTitle("Rubik's Cube: level " + this.level + " solved in " + moves + (moves == 1 ? " move!" : " moves!"));
      } else if (playerSolved) {
         this.setTitle("Rubik's Cube: solved in " + moves + (moves == 1 ? " move!" : " moves!"));
      } else {
         this.setTitle("Rubik's Cube: " + (this.onCube != null ? this.onCube.name : this.freePlay ? "free play" : "level " + this.level) + ", " + moves + (moves == 1 ? " move" : " moves"));
      }
   }

   // ---- Teach me ----

   /** T: turns Teach me on or off. */
   final void toggleCoach() {
      if (this.patterning && !this.coaching) {
         // One card at a time.
         this.togglePatterns();
      }
      this.coaching = !this.coaching;
      this.teachButton.setText(this.coaching ? "Stop teaching" : "Teach me");
      this.coachPanel.setVisible(this.coaching);
      this.helpLabel.setVisible(!this.coaching);
      this.messageLabel.setVisible(!this.coaching);
      this.coachSteps = null;
      if (this.coaching) {
         this.coachShow();
      } else {
         this.clearHint();
      }
      this.panel.repaint();
   }

   /** The cube changed in a way the coach did not ask for: work the steps out again and show them. */
   private void coachAgain() {
      this.coachSteps = null;
      if (this.coaching) {
         this.coachShow();
      }
   }

   /** After a move: on to the next turn if it was the one asked for, otherwise a new plan from where the cube is. */
   private void coachAfterMove() {
      if (this.coachSteps != null && !this.coachSteps.isEmpty()
            && this.lastTurn == this.coachSteps.get(this.coachStep).turns.get(this.coachTurn)) {
         if (++this.coachTurn == this.coachSteps.get(this.coachStep).turns.size()) {
            this.coachTurn = 0;
            if (++this.coachStep == this.coachSteps.size()) {
               this.coachSteps = null;
            }
         }
      } else {
         this.coachSteps = null;
      }
      this.coachShow();
   }

   /** Shows the coach card for where the cube is, and the next turn on the cube (ringed square, arrow, green key). */
   private void coachShow() {
      if (!this.coaching) {
         return;
      }
      if (this.busy()) {
         this.clearHint();
         this.coachPanel.show(-1, this.complete ? "Watch it solved" : "Watch the mix", this.complete
               ? "Solve is turning the cube back. Then Mix (or Retry) and solve one yourself, a layer at a time."
               : "Then I will show you how to solve it the way people learn to: one layer at a time, starting with a"
                     + " white cross.", "");
         return;
      }
      if (this.coachSteps == null) {
         this.coachSteps = this.plan();
         this.coachStep = 0;
         this.coachTurn = 0;
         int back = this.history.depth();
         if (this.coachSteps != null && back > 0 && back < Coach.turns(this.coachSteps).size()) {
            // A short mix (a level): turning it back is far quicker than the whole method.
            this.coachSteps = null;
            this.coachBack(back);
            return;
         }
      }
      if (this.coachSteps == null) {
         this.clearHint();
         this.coachPanel.show(-1, "Teach me", "I cannot work this cube out. Mix it or Retry (R).", "");
         return;
      }
      if (this.coachSteps.isEmpty()) {
         this.clearHint();
         int moves = this.userMoves.size();
         this.coachPanel.show(7, "Solved!", moves > 0 && this.solvedMillis != 0L
               ? "Solved in " + moves + (moves == 1 ? " move" : " moves") + ". That is the whole method: a white cross,"
                     + " the white corners, the middle layer, then the yellow layer in four steps."
               : "A solved cube. Mix it (or Retry) and I will take you through solving it, one layer at a time.",
               this.freePlay ? "Mix for another go." : "Retry (R) or Next (N) for another go.");
         return;
      }
      Coach.Step step = this.coachSteps.get(this.coachStep);
      if (step != this.coachShown) {
         // Once per step, so looking around between its turns is left alone.
         this.coachShown = step;
         this.view.stand(step.up, step.front);
      }
      int id = step.turns.get(this.coachTurn);
      String[] move = this.appMove(id);
      if (!this.complete) {
         this.usedHint = true;
      }
      this.pointAt(move);
      List<String> colors = new ArrayList<>();
      List<Boolean> clockwise = new ArrayList<>();
      for (int turn : step.turns) {
         colors.add(this.centerColor(Coach.faceOf(turn)));
         clockwise.add(Coach.clockwise(turn));
      }
      String key = move[1].toUpperCase();
      String how = this.view.isVisible() ? "Drag the ringed square along the arrow, or press " + key + "."
            : "Press " + key + " on the ringed square.";
      this.coachPanel.step(step.stage, step.text, colors, clockwise, this.coachTurn, "Turn " + (this.coachTurn + 1)
            + " of " + step.turns.size() + ": " + this.centerColor(Coach.faceOf(id))
            + (Coach.clockwise(id) ? " clockwise" : " back (counterclockwise)") + ". " + how);
      this.updateStatus();
   }

   /** Teach me on a short mix: the way back along the moves made, one turn at a time. */
   private void coachBack(int back) {
      String[] move = this.history.hint();
      if (!this.complete) {
         this.usedHint = true;
      }
      this.pointAt(move);
      String key = move[1].toUpperCase();
      this.coachPanel.show(-1, "Turn the mix back", "This cube is only " + back + (back == 1 ? " turn" : " turns")
            + " from solved, so the quickest way is to undo the mix, one turn at a time. To learn the whole method,"
            + " Mix it (any number of moves) with Teach me on.", (this.view.isVisible()
            ? "Drag the ringed square along the arrow, or press " + key + "." : "Press " + key + " on the ringed square."));
      this.updateStatus();
   }

   /** The coach's steps from the cube as it is, or null if it cannot solve it (it always should). */
   private List<Coach.Step> plan() {
      String[] colors = new String[54];
      Face[] faces = this.cube.faces();
      for (int s = 0; s < 54; s++) {
         colors[s] = faces[s / 9].stickers[s % 9];
      }
      try {
         return Coach.plan(colors);
      } catch (RuntimeException e) {
         System.out.println("Teach me: " + e.getMessage());
         return null;
      }
   }

   /** The coach's turns, when they are fewer than the way back along the moves made (a long Mix); else null. */
   private List<Integer> shorterThanHistory() {
      if (this.history.depth() <= 20) {
         return null;
      }
      List<Coach.Step> steps = this.plan();
      List<Integer> turns = steps == null ? null : Coach.turns(steps);
      return turns != null && !turns.isEmpty() && turns.size() < this.history.depth() ? turns : null;
   }

   /** The color of the center facing that way. */
   private String centerColor(int[] normal) {
      int s = CubeGeometry.stickerAt(normal, normal);
      return this.cube.faces()[s / 9].stickers[s % 9];
   }

   /** A layer turn as a W/A/S/D move {face, key, square}: on the square that faces the viewer most squarely. */
   private String[] appMove(int turnId) {
      String[] best = null;
      double bestScore = -10;
      for (int face = 0; face < 6; face++) {
         for (int cubie = 1; cubie <= 9; cubie++) {
            for (String key : CubeGeometry.KEYS) {
               CubeGeometry.Turn turn = CubeGeometry.turnOf(CubeGeometry.FACE_COLORS[face], key, cubie);
               if (turn != null && turn.id() == turnId) {
                  // The middle square of an edge shows the arrow best.
                  double score = this.view.facingOf(face * 9 + cubie - 1) + (cubie % 2 == 0 ? 0.1 : 0);
                  if (score > bestScore) {
                     bestScore = score;
                     best = new String[]{CubeGeometry.FACE_COLORS[face], key, String.valueOf(cubie)};
                  }
               }
            }
         }
      }
      return best;
   }

   // ---- Patterns ----

   /** What the card says before Show me or Make it. */
   private String patternStart() {
      int number = Patterns.ALL.indexOf(this.pattern) + 1;
      return "Show me (or " + number + " again) makes it on a solved cube while you watch. Make it (M): you turn it,"
            + " one turn at a time.";
   }

   /** G: the Patterns card in place of the message and help (closing Teach me), or back. */
   final void togglePatterns() {
      if (this.coaching && !this.patterning) {
         this.toggleCoach();
      }
      if (this.patterning) {
         this.stopPatternRun();
      }
      this.patterning = !this.patterning;
      this.patternButton.setText(this.patterning ? "Close patterns" : "Patterns");
      this.patternPanel.setVisible(this.patterning);
      this.helpLabel.setVisible(!this.patterning);
      this.messageLabel.setVisible(!this.patterning);
      this.pattern = null;
      this.patternPanel.list(this.records.patternsMade());
      this.panel.repaint();
   }

   /** A click on the Patterns card: a pattern in the list, back to the list, Show me (or Stop) or Make it. */
   private void patternClick(int action) {
      if (action >= 0) {
         this.pickPattern(action);
      } else if (action == PatternPanel.BACK) {
         this.stopPatternRun();
         this.pattern = null;
         this.patternPanel.list(this.records.patternsMade());
      } else if (action == PatternPanel.SHOW) {
         if (this.showAt >= 0 || this.making) {
            this.stopPatternRun();
         } else {
            this.showPattern();
         }
      } else if (action == PatternPanel.MAKE) {
         this.makePattern();
      }
   }

   /** 1 to 7: that pattern's card (opening the Patterns card first). */
   final void pickPattern(int index) {
      if (index < 0 || index >= Patterns.ALL.size() || this.scrambler != null) {
         return;
      }
      if (!this.patterning) {
         this.togglePatterns();
      }
      if (this.pattern == Patterns.ALL.get(index)) {
         // The same number again: Show me (or stop it).
         this.patternClick(PatternPanel.SHOW);
         return;
      }
      this.stopPatternRun();
      this.pattern = Patterns.ALL.get(index);
      this.patternStatus = this.patternStart();
      this.patternCard();
   }

   /** M: Make it, on the pattern whose card is up. */
   final void patternKey(char key) {
      if (this.patterning && this.pattern != null && key == 'm') {
         this.makePattern();
      } else if (this.patterning && this.pattern != null && key == '\b') {
         this.patternClick(PatternPanel.BACK);
      }
   }

   /** Shows the card of the pattern picked: its turns as chips, the next ringed while Show me or Make it runs. */
   private void patternCard() {
      if (this.pattern == null) {
         return;
      }
      List<String> colors = new ArrayList<>();
      List<Boolean> clockwise = new ArrayList<>();
      for (int turn : this.pattern.turns) {
         colors.add(this.centerColor(Coach.faceOf(turn)));
         clockwise.add(Coach.clockwise(turn));
      }
      int now = this.showAt >= 0 ? this.showAt : this.making ? this.patternDone : -1;
      this.patternPanel.card(this.pattern, this.records.patternsMade(), colors, clockwise, now, this.patternStatus,
            this.showAt >= 0, this.making);
   }

   /** A solved cube, out of any level, for the pattern on the card. */
   private void startPatternRun() {
      this.stopAll();
      this.freePlay = true;
      this.resetRun();
      this.cube.reset();
      this.history.restart();
      this.onCube = this.pattern;
      this.making = false;
      this.showAt = -1;
      this.coachSteps = null;
   }

   /** Show me: plays the pattern's turns on a solved cube, then turns the whole cube round to show it off. */
   private void showPattern() {
      if (this.scrambler != null || this.pattern == null) {
         return;
      }
      this.startPatternRun();
      this.showAt = 0;
      this.patternStatus = "Watch: " + this.pattern.turns.size() + " turns from a solved cube...";
      this.playback = new Timer(380, event -> {
         if (this.showAt < this.pattern.turns.size()) {
            String[] move = this.appMove(this.pattern.turns.get(this.showAt++));
            Scrambler.apply(this.history, move);
            this.markSquare(move[0], Integer.parseInt(move[2]));
            this.cube.refresh();
            this.patternCard();
            return;
         }
         this.stopPlayback();
         this.showAt = -1;
         this.markSquare(null, 0);
         this.patternStatus = "That is " + this.pattern.name + ". Now Make it yourself, or Solve to turn it back.";
         this.view.spin();
         this.patternCard();
         this.updateStatus();
      });
      this.playback.start();
      this.patternCard();
      this.updateStatus();
   }

   /** Make it: from a solved cube, the player makes each of the pattern's turns, shown on the cube as for a hint. */
   private void makePattern() {
      if (this.scrambler != null || this.pattern == null) {
         return;
      }
      this.startPatternRun();
      this.making = true;
      this.patternPath.clear();
      int[] stickers = Patterns.solved();
      this.patternPath.add(stickers);
      for (int turn : this.pattern.turns) {
         stickers = Patterns.apply(stickers, Arrays.asList(turn));
         this.patternPath.add(stickers);
      }
      this.patternDone = 0;
      this.offTurns.clear();
      this.patternGuide();
      this.updateStatus();
   }

   /** After a move while making a pattern: how far along it the cube is now, or the turn to take back. */
   private void patternAfterMove() {
      int[] now = this.stickers();
      int along = -1;
      for (int k = this.patternPath.size() - 1; k >= 0 && along < 0; k--) {
         along = Arrays.equals(this.patternPath.get(k), now) ? k : -1;
      }
      if (along >= 0) {
         this.patternDone = along;
         this.offTurns.clear();
      } else if (this.lastTurn >= 0) {
         this.offTurns.add(this.lastTurn);
      } else if (!this.offTurns.isEmpty()) {
         // Undo took back the last turn off the pattern.
         this.offTurns.remove(this.offTurns.size() - 1);
      }
      this.patternGuide();
   }

   /** Points at the next turn of the pattern (or the one that takes a wrong turn back), or celebrates it made. */
   private void patternGuide() {
      if (this.patternDone == this.pattern.turns.size()) {
         this.making = false;
         this.clearHint();
         this.markSquare(null, 0);
         boolean first = !this.records.patternsMade().contains(this.pattern.key);
         this.records.madePattern(this.pattern.key);
         int moves = this.userMoves.size();
         this.patternStatus = "Made it! " + this.pattern.name + " in " + moves + (moves == 1 ? " turn." : " turns.")
               + (first ? " " + this.records.patternsMade().size() + " of " + Patterns.ALL.size() + " made." : "");
         this.view.spin();
         this.patternCard();
         this.updateStatus();
         return;
      }
      boolean off = !this.offTurns.isEmpty();
      int id = off ? Patterns.inverse(this.offTurns.get(this.offTurns.size() - 1)) : this.pattern.turns.get(this.patternDone);
      String[] move = this.appMove(id);
      this.pointAt(move);
      String key = move[1].toUpperCase();
      String how = this.view.isVisible() ? "Drag the ringed square along the arrow, or press " + key + "."
            : "Press " + key + " on the ringed square.";
      this.patternStatus = off ? "That turn is not in the pattern: turn it back (or Undo, Z). " + how
            : "Turn " + (this.patternDone + 1) + " of " + this.pattern.turns.size() + ": " + this.turnWords(id) + ". " + how;
      this.patternCard();
   }

   /** A quarter turn in words: "red clockwise", or "the middle layer" for one that turns no face. */
   private String turnWords(int id) {
      if (id % 6 / 2 == 1) {
         return "the middle layer";
      }
      return this.centerColor(Coach.faceOf(id)) + (Coach.clockwise(id) ? " clockwise" : " back (counterclockwise)");
   }

   /** Stops Show me or Make it, leaving the cube as it is. */
   private void stopPatternRun() {
      if (this.showAt >= 0) {
         this.stopPlayback();
      }
      boolean running = this.showAt >= 0 || this.making;
      this.showAt = -1;
      this.making = false;
      if (running) {
         this.clearHint();
         this.markSquare(null, 0);
         this.patternStatus = this.patternStart();
         this.patternCard();
         this.updateStatus();
      }
   }

   /** A level, Mix or Solve takes the cube: no pattern run any more. */
   private void endPatternRun() {
      this.stopPatternRun();
      this.onCube = null;
   }

   /** The face index of every sticker's color, as {@link Patterns} counts them. */
   private int[] stickers() {
      int[] state = new int[54];
      Face[] faces = this.cube.faces();
      for (int s = 0; s < 54; s++) {
         state[s] = CubeGeometry.faceIndex(faces[s / 9].stickers[s % 9]);
      }
      return state;
   }

   /** The level keys: Z undo, H hint, R retry, N next, P previous. */
   final void levelKey(char key) {
      switch (key) {
         case 'z': this.undo(); break;
         case 'h': this.showHint(); break;
         case 'r': this.retry(); break;
         case 'n': if (this.nextButton.isEnabled()) { this.startLevel(this.level + 1); } break;
         case 'p': if (this.prevButton.isEnabled()) { this.startLevel(this.level - 1); } break;
         default: break;
      }
   }

   private void retry() {
      if (this.scrambler == null) {
         this.startLevel(this.level);
      }
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      Object source = event.getSource();
      for (int index = 0; index < this.moveButtons.length; index++) {
         if (source == this.moveButtons[index]) {
            this.applyUserMove(this.moveButtons[index].getText());
         }
      }
      if (source == this.viewButton) {
         this.toggleView();
      } else if (source == this.teachButton) {
         this.toggleCoach();
      } else if (source == this.patternButton) {
         this.togglePatterns();
      } else if (source == this.undoButton) {
         this.levelKey('z');
      } else if (source == this.hintButton) {
         this.levelKey('h');
      } else if (source == this.retryButton) {
         this.levelKey('r');
      } else if (source == this.nextButton) {
         this.levelKey('n');
      } else if (source == this.prevButton) {
         this.levelKey('p');
      } else if (source == this.solveButton) {
         this.solveForPlayer();
      }

      if (source == this.mixButton && !this.busy()) {
         int moves;
         double seconds;
         try {
            moves = Integer.parseInt(this.mixFields[0].getText().trim());
            seconds = Double.parseDouble(this.mixFields[1].getText().trim());
         } catch (NumberFormatException e) {
            this.setTitle("Rubik's Cube: Moves and Secs must be numbers");
            return;
         }
         this.stopAll();
         this.endPatternRun();
         this.freePlay = true;
         this.resetRun();
         this.message("Mixing...");
         this.scrambler = new Scrambler(Math.max(0, moves), seconds, this.history, this.cube, () -> {
            this.scrambler = null;
            this.message(this.cube.isSolved() ? "Still solved: mix more." : "Mixed. Solve it any<br>way you like, or<br>Teach me (T) how.");
            this.updateStatus();
            this.coachAgain();
         });
         this.setTitle("Rubik's Cube: mixing...");
         this.updateStatus();
         this.scrambler.start();
         this.coachAgain();
      }
   }
}
