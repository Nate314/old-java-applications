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
 * The window: the unfolded cube, the w/a/s/d buttons, Mix, and the level ladder. Level n is a solved cube mixed by n
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
               + "Keys: Z undo, H hint,<br>R retry, N next, P prev.</html>");
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
      this.moveButtons[0]
         .setBounds(
            this.cellWidth + (int)(2.75 * (double)this.cellWidth / 3.0), (int)(1.75 * (double)this.cellHeight), (int)(2.75 * (double)this.cellWidth / 3.0), (int)(2.75 * (double)this.cellHeight / 3.0)
         );
      this.moveButtons[1].setBounds(this.moveButtons[0].getX() - this.moveButtons[0].getWidth(), this.moveButtons[0].getY() + this.moveButtons[0].getHeight(), this.moveButtons[0].getWidth(), this.moveButtons[0].getHeight());
      this.moveButtons[2].setBounds(this.moveButtons[1].getX() + this.moveButtons[1].getWidth(), this.moveButtons[1].getY(), this.moveButtons[0].getWidth(), this.moveButtons[0].getHeight());
      this.moveButtons[3].setBounds(this.moveButtons[2].getX() + this.moveButtons[2].getWidth(), this.moveButtons[1].getY(), this.moveButtons[0].getWidth(), this.moveButtons[0].getHeight());
      this.panel.add(this.moveButtons[0]);
      this.panel.add(this.moveButtons[1]);
      this.panel.add(this.moveButtons[2]);
      this.panel.add(this.moveButtons[3]);
      this.mixLabels[0].setBounds((int)(7.5 * (double)this.cellWidth), this.cellHeight, (int)(1.25 * (double)this.cellWidth), this.cellHeight / 2);
      this.mixLabels[1].setBounds((int)(8.75 * (double)this.cellWidth), this.cellHeight, (int)(1.25 * (double)this.cellWidth), this.cellHeight / 2);
      this.panel.add(this.mixLabels[0]);
      this.panel.add(this.mixLabels[1]);
      this.mixFields[0].setBounds((int)(7.5 * (double)this.cellWidth), (int)(1.5 * (double)this.cellHeight), (int)(1.25 * (double)this.cellWidth), this.cellHeight / 2);
      this.mixFields[1].setBounds((int)(8.75 * (double)this.cellWidth), (int)(1.5 * (double)this.cellHeight), (int)(1.25 * (double)this.cellWidth), this.cellHeight / 2);
      this.panel.add(this.mixFields[0]);
      this.panel.add(this.mixFields[1]);
      this.mixButton.addActionListener(this);
      this.mixButton.setBounds((int)(7.5 * (double)this.cellWidth), 2 * this.cellHeight, (int)(2.5 * (double)this.cellWidth), (int)(1.5 * (double)this.cellHeight));
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
      for (JLabel label : new JLabel[]{this.levelLabel, this.goalLabel, this.movesLabel, this.bestLabel, this.totalLabel, this.messageLabel, this.helpLabel}) {
         this.panel.add(label);
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
      });
      this.playback.start();
      this.updateStatus();
   }

   /** Solve: plays the way back to solved one move at a time. The level is not counted. */
   private void solveForPlayer() {
      if (this.busy() || this.history.depth() == 0) {
         return;
      }
      this.clearHint();
      this.clock.stop();
      this.complete = true;
      this.message("Watch the way back...");
      int interval = Math.max(15, Math.min(300, 6000 / this.history.depth()));
      this.playback = new Timer(interval, event -> {
         String[] move = this.history.hint();
         if (move != null) {
            Scrambler.apply(this.history, move);
            this.markSquare(move[0], Integer.parseInt(move[2]));
            this.cube.refresh();
            return;
         }
         this.stopPlayback();
         this.markSquare(null, 0);
         this.message("Solved by the computer.<br>Retry (R) to try it<br>yourself.");
         this.updateStatus();
      });
      this.playback.start();
      this.updateStatus();
   }

   /** Hint (H): selects the square of the next move back and lights its key. */
   private void showHint() {
      if (this.busy()) {
         return;
      }
      String[] move = this.history.hint();
      if (move == null) {
         this.message(this.cube.isSolved() ? "Already solved." : "No hint here: Retry<br>(R) or Mix.");
         return;
      }
      this.clearHint();
      this.hinted = move;
      if (!this.complete) {
         this.usedHint = true;
      }
      this.select(move[0], Integer.parseInt(move[2]));
      this.keyButton(move[1]).setBackground(HINT_KEY);
      this.message("Hint: press " + move[1].toUpperCase() + " on<br>the ringed square.");
      this.updateStatus();
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
      if (this.complete && !this.freePlay && this.playback == null && !this.usedHint && moves > 0) {
         this.setTitle("Rubik's Cube: level " + this.level + " solved in " + moves + (moves == 1 ? " move!" : " moves!"));
      } else if (this.complete && moves > 0) {
         this.setTitle("Rubik's Cube: solved in " + moves + (moves == 1 ? " move!" : " moves!"));
      } else {
         this.setTitle("Rubik's Cube: " + (this.freePlay ? "free play" : "level " + this.level) + ", " + moves + (moves == 1 ? " move" : " moves"));
      }
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
      if (source == this.undoButton) {
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
            this.message(this.cube.isSolved() ? "Still solved: mix more." : "Mixed. Solve it any<br>way you like.");
            this.updateStatus();
         });
         this.setTitle("Rubik's Cube: mixing...");
         this.updateStatus();
         this.scrambler.start();
      }
   }
}
