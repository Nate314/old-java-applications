package rubikscube;

import java.awt.Color;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
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
               + "Keys: Z undo, H hint,<br>R retry, N next, P prev.<br>T: Teach me how.</html>");
   private static final String HELP_3D =
         "<html>Drag a square to turn<br>its row, column or ring.<br>Drag around the cube<br>(or press the arrow keys)<br>to look at other sides.<br>"
               + "Or click a square, then<br>press W A S D.<br><br>"
               + "Keys: Z undo, H hint,<br>R retry, N next, P prev.<br>V: the flat net. T: Teach<br>me how to solve it.</html>";
   private final String helpNet;
   private final JButton viewButton = new JButton();
   private final JButton teachButton = new JButton("Teach me");
   private final CoachPanel coachPanel = new CoachPanel();
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
      List<Integer> layers = this.shorterThanHistory();
      String[] move = layers == null ? this.history.hint() : this.appMove(layers.get(0));
      if (move == null) {
         this.message(this.cube.isSolved() ? "Already solved." : "No hint here: try<br>Teach me (T).");
         return;
      }
      this.clearHint();
      this.hinted = move;
      if (!this.complete) {
         this.usedHint = true;
      }
      this.select(move[0], Integer.parseInt(move[2]));
      this.keyButton(move[1]).setBackground(HINT_KEY);
      this.view.hint(move[0], Integer.parseInt(move[2]), move[1]);
      this.hintMessage();
      this.updateStatus();
   }

   /** Says how to make the hinted move, in the words of the view shown. */
   private void hintMessage() {
      String key = this.hinted[1].toUpperCase();
      this.message(this.view.isVisible() ? "Hint: drag the ringed<br>square along the arrow<br>(or press " + key + ")."
            : "Hint: press " + key + " on<br>the ringed square.");
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
      if (this.freePlay) {
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
         this.setTitle("Rubik's Cube: " + (this.freePlay ? "free play" : "level " + this.level) + ", " + moves + (moves == 1 ? " move" : " moves"));
      }
   }

   // ---- Teach me ----

   /** T: turns Teach me on or off. */
   final void toggleCoach() {
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
      this.clearHint();
      this.hinted = move;
      if (!this.complete) {
         this.usedHint = true;
      }
      this.select(move[0], Integer.parseInt(move[2]));
      this.keyButton(move[1]).setBackground(HINT_KEY);
      this.view.hint(move[0], Integer.parseInt(move[2]), move[1]);
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
      this.clearHint();
      this.hinted = move;
      if (!this.complete) {
         this.usedHint = true;
      }
      this.select(move[0], Integer.parseInt(move[2]));
      this.keyButton(move[1]).setBackground(HINT_KEY);
      this.view.hint(move[0], Integer.parseInt(move[2]), move[1]);
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
