package minesweeper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import javax.swing.JButton;

/**
 * A tile. It is still a button (focus, keys, screen reader names), but draws itself: a raised grass tile while hidden,
 * sand with a colored number once revealed, a planted flag, a mine. Changes animate: a revealed tile pops away (later
 * the farther it is from the click, so an opening ripples out), a flag is planted, a mine bursts in.
 */
public final class MineTile extends JButton implements MouseListener {
   private static final int HIDDEN = 0;
   private static final int FLAG = 1;
   private static final int QUESTION = 2;

   /** What the tile shows. */
   private static final int SHOW_HIDDEN = 0;
   private static final int SHOW_VALUE = 1;
   private static final int SHOW_MINE = 2;
   private static final int SHOW_HIT = 3;
   private static final int SHOW_WRONG_FLAG = 4;

   static final Color GRASS_LIGHT = new Color(128, 204, 84);
   static final Color GRASS_DARK = new Color(114, 190, 72);
   static final Color GRASS_EDGE = new Color(58, 112, 36);
   private static final Color GRASS_SHINE = new Color(176, 232, 134);
   private static final Color GRASS_HOVER = new Color(158, 222, 112);
   static final Color SAND_LIGHT = new Color(238, 228, 206);
   static final Color SAND_DARK = new Color(228, 216, 190);
   static final Color SAND_LINE = new Color(214, 199, 170);
   static final Color HIT = new Color(229, 57, 53);
   private static final Color FLAG_RED = new Color(222, 44, 44);
   static final Color POLE = new Color(52, 52, 58);
   private static final Color MINE = new Color(34, 34, 40);
   private static final Color CURSOR = new Color(20, 60, 160);
   /** Number colors, 1 to 8: the classic blue, green, red, purple, then orange, teal, gray and silver. */
   private static final Color[] NUMBER = {
      new Color(25, 103, 210), new Color(46, 125, 50), new Color(211, 47, 47), new Color(106, 27, 154),
      new Color(230, 110, 0), new Color(0, 131, 143), new Color(66, 66, 66), new Color(130, 130, 130)};

   /** How long each animation runs, in ms. */
   static final int POP_MS = 160;
   private static final int PLANT_MS = 140;
   private static final int BURST_MS = 260;
   private static final int PULSE_MS = 320;
   private static final int BLOOM_MS = 420;
   /** A number whose mines are all flagged, with nothing else hidden around it, sinks into the sand over this long. */
   private static final int SETTLE_MS = 360;
   /** How far a settled number fades toward the sand, and how small it gets. */
   private static final double SETTLE_FADE = 0.72;
   private static final double SETTLE_SCALE = 0.78;
   /** A won field grows back as flat meadow (unlike the raised grass of a hidden tile), some tiles with a flower. */
   private static final Color MEADOW_LIGHT = new Color(170, 214, 112);
   private static final Color MEADOW_DARK = new Color(158, 204, 100);
   private static final Color[] PETALS = {
      Color.WHITE, new Color(255, 214, 64), new Color(244, 143, 177), new Color(179, 157, 219)};

   private GameFrame game;
   /** The tile's x index: it sits at {@code row * tileWidth}, so this is the column a player sees. */
   private int row;
   /** The tile's y index, the row a player sees. */
   private int column;
   private int mark = HIDDEN;
   private int shown = SHOW_HIDDEN;
   private String value = " ";
   /** How a hint (H, or the computer's reasoning while it plays) marks this tile. */
   static final int HINT_NONE = 0;
   /** A number the reasoning reads: a gold ring. */
   static final int HINT_CLUE = 1;
   /** A hidden tile worked out to be safe: it glows. */
   static final int HINT_SAFE = 2;
   /** A hidden tile worked out to be a mine: tinted red. */
   static final int HINT_MINE = 3;
   private static final Color CLUE_GOLD = new Color(245, 176, 0);
   private static final Color SAFE_GLOW = new Color(255, 252, 214);
   private int hint = HINT_NONE;
   /** Odds (O): the chance this hidden tile is a mine, 0 to 1, or -1 when not shown. */
   private double odds = -1;
   /** Once the game is over: the chance this tile was a mine when the player guessed on it, or -1 if it was no guess. */
   private double gamble = -1;
   private static final Color GAMBLE = new Color(123, 31, 162);
   private boolean cursor;
   private boolean hover;
   private boolean pressed;
   /** When the current change starts and how long it runs (start 0: nothing animates). */
   private long animStart;
   private int animMs;
   private long pulseStart;
   /** When a won field's meadow starts growing on this tile (0: never). */
   private long bloomStart;
   /** How this number stands by its flags ({@link MineFieldGenerator#standing}), and when it began to settle. */
   private int standing = MineFieldGenerator.OPEN;
   private long settleStart;

   public MineTile(GameFrame game, int row, int column) {
      this.game = game;
      this.row = row;
      this.column = column;
      this.setText("");
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.addMouseListener(this);
      this.addMouseMotionListener(new MouseAdapter() {
         @Override
         public void mouseDragged(MouseEvent event) {
            MineTile.this.dragged(event);
         }
      });
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent event) {
            MineTile.this.keyPressed(event);
         }
      });
      this.addKeyListener(new WindowMoveKeyListener(game));
      this.describe("hidden");
   }

   /**
    * Keyboard play: Space or Enter reveals the tile (on a number, opens around it), F (or the menu key, or Shift+F10)
    * cycles its flag, and the arrow keys move to the next tile. Shift+arrows still move the window.
    */
   private void keyPressed(KeyEvent event) {
      int key = event.getKeyCode();
      if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_ENTER) {
         this.game.moveCursor(this);
         this.play(true);
      } else if (key == KeyEvent.VK_F || key == KeyEvent.VK_CONTEXT_MENU || key == KeyEvent.VK_F10 && event.isShiftDown()) {
         this.game.moveCursor(this);
         this.play(false);
      } else if (event.isShiftDown()) {
         return;
      } else if (key == KeyEvent.VK_LEFT) {
         this.game.focusTile(this.row - 1, this.column);
      } else if (key == KeyEvent.VK_RIGHT) {
         this.game.focusTile(this.row + 1, this.column);
      } else if (key == KeyEvent.VK_UP) {
         this.game.focusTile(this.row, this.column - 1);
      } else if (key == KeyEvent.VK_DOWN) {
         this.game.focusTile(this.row, this.column + 1);
      } else {
         return;
      }
      event.consume();
   }

   /** Marks the tile for a hint ({@link #HINT_CLUE}, {@link #HINT_SAFE}, {@link #HINT_MINE}) or clears it ({@link #HINT_NONE}). */
   void setHint(int role) {
      if (this.hint != role) {
         this.hint = role;
         this.repaint();
      }
   }

   /** Shows this hidden tile's chance of being a mine (0 to 1), or nothing (-1). */
   void setOdds(double chance) {
      if (this.odds != chance) {
         this.odds = chance;
         this.repaint();
         if (this.shown == SHOW_HIDDEN && this.mark == HIDDEN) {
            this.describe(chance < 0 ? "hidden" : "hidden, " + Odds.percent(chance) + " chance of a mine");
         }
      }
   }

   /** Tags this tile as one the player guessed on, with its chance of being a mine at the time (shown once the game ends). */
   void setGamble(double risk) {
      this.gamble = risk;
      this.repaint();
      this.getAccessibleContext().setAccessibleName(this.getAccessibleContext().getAccessibleName() + ", your guess, " + Odds.percent(risk) + " chance of a mine");
   }

   /** Shows or hides the keyboard cursor on this tile. */
   void setCursor(boolean on) {
      this.cursor = on;
      this.repaint();
   }

   /** What a screen reader says for the tile: its state, then where it is ("3 mines near, row 2, column 5"). */
   private void describe(String state) {
      this.getAccessibleContext().setAccessibleName(state + ", row " + (this.column + 1) + ", column " + (this.row + 1));
   }

   /** The tile's x and y index, so a new game can put the keyboard cursor back on the same spot. */
   int[] spot() {
      return new int[]{this.row, this.column};
   }

   /** Reveals the tile's number (or blank), popping the grass away after {@code delayMs}. */
   void showValue(String value, int delayMs) {
      this.mark = HIDDEN;
      this.game.setFlagged(this.row, this.column, false);
      this.value = value;
      this.shown = SHOW_VALUE;
      this.animate(delayMs, POP_MS);
      this.describeValue();
   }

   private void describeValue() {
      this.describe((this.value.trim().isEmpty() ? "clear" : this.value.equals("1") ? "1 mine near" : this.value + " mines near")
         + (this.standing == MineFieldGenerator.DONE ? ", all flagged" : this.standing == MineFieldGenerator.OVER ? ", too many flags" : ""));
   }

   /**
    * A revealed number's standing by its flags: settled (it fades into the sand, after its grass has popped off),
    * over-flagged (ringed in red), or open.
    */
   void setStanding(int standing) {
      if (this.standing == standing || this.shown != SHOW_VALUE) {
         return;
      }
      this.standing = standing;
      this.settleStart = 0;
      if (standing == MineFieldGenerator.DONE) {
         long now = System.currentTimeMillis();
         this.settleStart = Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"))
            ? now - SETTLE_MS : Math.max(now, this.animStart + this.animMs);
         this.game.animating(this);
      }
      this.describeValue();
      this.repaint();
   }

   /** Shows a mine after {@code delayMs}: the one that was hit on red, the others on sand. A flagged mine keeps its flag. */
   void showMine(boolean hit, int delayMs) {
      if (this.mark == FLAG && !hit) {
         return;
      }
      this.shown = hit ? SHOW_HIT : SHOW_MINE;
      this.animate(delayMs, BURST_MS);
      this.describe(hit ? "mine, hit" : "mine");
   }

   /** Crosses out a flag that was not on a mine, once the game is lost. */
   void showWrongFlag(int delayMs) {
      this.shown = SHOW_WRONG_FLAG;
      this.animate(delayMs, PLANT_MS);
      this.describe("wrong flag");
   }

   /** Shows a mine as flagged once the game is won: the flag is planted after {@code delayMs}. */
   void showFlag(int delayMs) {
      boolean had = this.mark == FLAG;
      this.mark = FLAG;
      if (!had) {
         this.animate(delayMs, PLANT_MS);
      }
      this.describe("flagged mine");
   }

   /**
    * A lost game is undone: the mines go back under the grass, the one hit with a flag on it, wrong flags come off
    * (the loss showed them to be wrong) and the guess tags go.
    */
   void undoLoss() {
      this.gamble = -1;
      if (this.shown == SHOW_VALUE) {
         this.describeValue();
      } else if (this.shown != SHOW_HIDDEN) {
         if (this.shown == SHOW_HIT) {
            this.mark = FLAG;
            this.animate(0, PLANT_MS);
         } else if (this.shown == SHOW_WRONG_FLAG) {
            this.mark = HIDDEN;
         }
         this.shown = SHOW_HIDDEN;
         this.describe(this.mark == FLAG ? "flag" : this.mark == QUESTION ? "question mark" : "hidden");
      }
      this.repaint();
   }

   boolean isFlagged() {
      return this.mark == FLAG;
   }

   /**
    * The board is won: this opened tile grows back as meadow after {@code delayMs} (and after its own grass has
    * popped off), maybe with a flower. With reduced motion it is there at once.
    */
   void bloom(int delayMs) {
      if (this.shown != SHOW_VALUE) {
         return;
      }
      long now = System.currentTimeMillis();
      if (Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"))) {
         this.bloomStart = now - BLOOM_MS;
         this.repaint();
         return;
      }
      this.bloomStart = Math.max(now + delayMs, this.animStart + this.animMs);
      this.game.animating(this);
   }

   /** A short glow on a hidden tile: the tiles a number would open, when its flags do not add up yet. */
   void pulse() {
      if (this.shown == SHOW_HIDDEN && this.mark != FLAG) {
         this.pulseStart = System.currentTimeMillis();
         this.game.animating(this);
      }
   }

   private void animate(int delayMs, int durationMs) {
      this.animStart = System.currentTimeMillis() + delayMs;
      this.animMs = durationMs;
      this.game.animating(this);
   }

   /** True while an animation still has frames to draw. */
   boolean isAnimating(long now) {
      return this.animStart != 0 && now < this.animStart + this.animMs || this.pulseStart != 0 && now < this.pulseStart + PULSE_MS
         || this.bloomStart != 0 && now < this.bloomStart + BLOOM_MS || this.settleStart != 0 && now < this.settleStart + SETTLE_MS;
   }

   // ---- drawing ----

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int s = Math.min(this.getWidth(), this.getHeight());
      long now = System.currentTimeMillis();
      // 0 before the change starts, 1 once it is done.
      double t = this.animStart == 0 ? 1 : Math.max(0, Math.min(1, (now - this.animStart) / (double) this.animMs));
      boolean dark = (this.row + this.column) % 2 == 1;

      if (this.shown == SHOW_HIDDEN || this.shown == SHOW_WRONG_FLAG) {
         this.paintGrass(g, s, dark, 1);
         if (this.odds >= 0 && this.shown == SHOW_HIDDEN) {
            this.paintOdds(g, s);
         }
         if (this.shown == SHOW_WRONG_FLAG || this.mark == FLAG && now >= this.animStart) { // a won game's flags wait their turn
            this.paintFlag(g, s, this.shown == SHOW_WRONG_FLAG ? 1 : t);
         } else if (this.mark == QUESTION) {
            this.paintQuestion(g, s);
         }
         if (this.shown == SHOW_WRONG_FLAG) {
            this.paintCross(g, s, t);
         }
      } else if (t <= 0) {
         this.paintGrass(g, s, dark, 1);
         if (this.mark == FLAG) {
            this.paintFlag(g, s, 1);
         }
      } else {
         this.paintSand(g, s, dark);
         if (this.shown == SHOW_VALUE) {
            this.paintNumber(g, s, now);
         } else {
            if (this.shown == SHOW_HIT) {
               g.setColor(withAlpha(HIT, (int) (255 * Math.min(1, t * 2))));
               g.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
            this.paintMine(g, s, overshoot(t));
         }
         if (t < 1 && this.shown == SHOW_VALUE) { // the grass pops off: shrinks and fades toward the middle
            this.paintGrass(g, s, dark, 1 - ease(t));
         }
         if (this.bloomStart != 0 && now >= this.bloomStart) {
            this.paintMeadow(g, s, dark, Math.min(1, (now - this.bloomStart) / (double) BLOOM_MS));
         }
      }
      this.paintHint(g, s);
      if (this.gamble >= 0) {
         this.paintGamble(g, s);
      }
      if (this.cursor) {
         g.setColor(CURSOR);
         g.setStroke(new BasicStroke(Math.max(2, s / 14f)));
         int inset = Math.max(2, s / 14);
         g.drawRoundRect(inset, inset, this.getWidth() - 2 * inset - 1, this.getHeight() - 2 * inset - 1, s / 4, s / 4);
      }
   }

   /** A raised tile of grass, scaled about its middle by {@code scale} (and faded with it). */
   private void paintGrass(Graphics2D g, int s, boolean dark, double scale) {
      int w = this.getWidth();
      int h = this.getHeight();
      if (scale >= 1) {
         g.setColor(GRASS_EDGE);
         g.fillRect(0, 0, w, h);
      }
      int alpha = (int) Math.round(255 * Math.min(1, scale * 1.4));
      int iw = (int) Math.round((w - 2) * scale);
      int ih = (int) Math.round((h - 2) * scale);
      if (iw < 2 || ih < 2) {
         return;
      }
      int x = (w - iw) / 2;
      int y = (h - ih) / 2;
      int arc = Math.max(4, s / 4);
      boolean lit = this.hover && !this.pressed && this.shown == SHOW_HIDDEN && !this.game.gameOver;
      double glow = this.pulseStart == 0 ? 0 : 1 - Math.min(1, (System.currentTimeMillis() - this.pulseStart) / (double) PULSE_MS);
      Color base = this.pressed && scale >= 1 ? SAND_DARK : lit ? GRASS_HOVER : dark ? GRASS_DARK : GRASS_LIGHT;
      if (glow > 0) {
         base = mix(base, Color.WHITE, glow * 0.6);
      }
      int bevel = this.pressed ? 0 : Math.max(2, s / 12);
      g.setColor(withAlpha(GRASS_EDGE.darker(), alpha));
      g.fillRoundRect(x, y + bevel, iw, ih - bevel, arc, arc);
      g.setColor(withAlpha(this.pressed ? SAND_LINE : GRASS_SHINE, alpha));
      g.fillRoundRect(x, y, iw, ih - bevel, arc, arc);
      g.setColor(withAlpha(base, alpha));
      g.fillRoundRect(x + 1, y + Math.max(1, bevel / 2), iw - 2, ih - bevel - Math.max(1, bevel / 2), arc, arc);
   }

   /**
    * A hint's mark: a gold ring around a number it reads; a warm glow and a light ring on a tile it found safe; a red
    * tint and ring on a tile it found to be a mine. Only hidden tiles glow or tint (the rest are already settled).
    */
   private void paintHint(Graphics2D g, int s) {
      boolean hidden = this.shown == SHOW_HIDDEN;
      if (this.hint == HINT_NONE || this.hint != HINT_CLUE && !hidden || this.hint == HINT_CLUE && hidden) {
         return;
      }
      int w = this.getWidth();
      int h = this.getHeight();
      int inset = Math.max(2, s / 10);
      int arc = Math.max(6, s / 3);
      float stroke = Math.max(2.5f, s / 11f);
      g.setStroke(new BasicStroke(stroke));
      if (this.hint == HINT_CLUE) {
         g.setColor(withAlpha(CLUE_GOLD, 45));
         g.fillRoundRect(inset, inset, w - 2 * inset, h - 2 * inset, arc, arc);
         g.setColor(CLUE_GOLD);
      } else {
         boolean safe = this.hint == HINT_SAFE;
         g.setColor(safe ? withAlpha(SAFE_GLOW, 150) : withAlpha(HIT, 170));
         g.fillRoundRect(inset, inset, w - 2 * inset, h - 2 * inset, arc, arc);
         g.setColor(safe ? Color.WHITE : new Color(190, 20, 20));
      }
      g.drawRoundRect(inset, inset, w - 2 * inset - 1, h - 2 * inset - 1, arc, arc);
   }

   /**
    * Odds on the grass: a light glow at 0% (safe), otherwise red, deeper the likelier a mine, with the percent in white
    * on a plain tile big enough to read it (a flag or question mark keeps its place).
    */
   private void paintOdds(Graphics2D g, int s) {
      int w = this.getWidth();
      int h = this.getHeight();
      int inset = Math.max(2, s / 10);
      int arc = Math.max(6, s / 3);
      boolean safe = this.odds < 1e-9;
      g.setColor(safe ? withAlpha(SAFE_GLOW, 150) : withAlpha(HIT, (int) (20 + 200 * this.odds)));
      g.fillRoundRect(inset, inset, w - 2 * inset, h - 2 * inset, arc, arc);
      if (this.mark != HIDDEN || s < 22) {
         return;
      }
      String text = Odds.percent(this.odds);
      g.setFont(new Font("Dialog", Font.BOLD, Math.max(9, (int) (s * 0.3))));
      FontMetrics fm = g.getFontMetrics();
      int x = (w - fm.stringWidth(text)) / 2;
      int y = (h + fm.getAscent() - fm.getDescent()) / 2;
      if (!safe) { // white on red, with a dark edge so it reads on light pink too
         g.setColor(new Color(90, 10, 10));
         g.drawString(text, x + 1, y + 1);
      }
      g.setColor(safe ? GRASS_EDGE : Color.WHITE);
      g.drawString(text, x, y);
   }

   /** A guess the player made: a purple ring, and the chance it was a mine on a tag along the bottom (if it fits). */
   private void paintGamble(Graphics2D g, int s) {
      int w = this.getWidth();
      int h = this.getHeight();
      int inset = Math.max(1, s / 16);
      g.setColor(GAMBLE);
      g.setStroke(new BasicStroke(Math.max(2, s / 14f)));
      g.drawRoundRect(inset, inset, w - 2 * inset - 1, h - 2 * inset - 1, s / 4, s / 4);
      if (s < 22) {
         return;
      }
      String text = Odds.percent(this.gamble);
      g.setFont(new Font("Dialog", Font.BOLD, Math.max(8, (int) (s * 0.28))));
      FontMetrics fm = g.getFontMetrics();
      int tw = fm.stringWidth(text) + 6;
      int th = fm.getAscent() + 2;
      int x = (w - tw) / 2;
      int y = h - th - inset;
      g.fillRoundRect(x, y, tw, th, th, th);
      g.setColor(Color.WHITE);
      g.drawString(text, x + 3, y + fm.getAscent());
   }

   /**
    * A won tile: flat meadow spreading from the middle as {@code b} goes from 0 to 1, then (on about half the tiles,
    * picked by place so a board always blooms the same) a flower or a tuft of grass pops up.
    */
   private void paintMeadow(Graphics2D g, int s, boolean dark, double b) {
      int w = this.getWidth();
      int h = this.getHeight();
      // A round patch that grows into the square: its corners fill in last.
      double f = ease(Math.min(1, b * 1.6));
      int pw = (int) Math.ceil(w * f);
      int ph = (int) Math.ceil(h * f);
      int arc = (int) Math.round(s * (1 - f));
      g.setColor(dark ? MEADOW_DARK : MEADOW_LIGHT);
      g.fillRoundRect((w - pw) / 2, (h - ph) / 2, pw, ph, arc, arc);
      double grow = overshoot(Math.max(0, b - 0.4) / 0.6);
      if (b <= 0.4) {
         return;
      }
      int mix = this.row * 73856093 ^ this.column * 19349663;
      int kind = Math.floorMod(mix ^ mix >>> 13, 8);
      // Not dead center, so a field of them looks sown rather than tiled.
      int cx = w / 2 + (kind % 3 - 1) * s / 8;
      int cy = h / 2 + (kind % 2 == 0 ? -1 : 1) * s / 10;
      if (kind < PETALS.length) {
         double pr = s * 0.13 * grow;
         int pd = (int) Math.round(pr * 1.5);
         g.setColor(PETALS[kind]);
         for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5 - Math.PI / 2;
            g.fillOval((int) Math.round(cx + Math.cos(a) * pr - pd / 2.0), (int) Math.round(cy + Math.sin(a) * pr - pd / 2.0), pd, pd);
         }
         int c = Math.max(2, (int) Math.round(pr * 0.9));
         g.setColor(kind == 1 ? new Color(230, 120, 20) : new Color(250, 200, 40));
         g.fillOval(cx - c / 2, cy - c / 2, c, c);
      } else if (kind < 6) {
         g.setColor(GRASS_EDGE);
         g.setStroke(new BasicStroke(Math.max(1.2f, s / 22f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
         int base = cy + (int) (s * 0.14);
         int tall = (int) (s * 0.24 * grow);
         for (int i = -1; i <= 1; i++) {
            g.drawLine(cx + i * s / 14, base, cx + i * s / 7, base - tall + Math.abs(i) * tall / 3);
         }
      }
   }

   /** Revealed ground: a flat sand square, with a faint line where it meets the next one. */
   private void paintSand(Graphics2D g, int s, boolean dark) {
      int w = this.getWidth();
      int h = this.getHeight();
      g.setColor(dark ? SAND_DARK : SAND_LIGHT);
      g.fillRect(0, 0, w, h);
      g.setColor(SAND_LINE);
      g.drawLine(w - 1, 0, w - 1, h - 1);
      g.drawLine(0, h - 1, w - 1, h - 1);
   }

   /**
    * The number in its color. Settled ({@link MineFieldGenerator#DONE}) it sinks into the sand, smaller and faded, so
    * the numbers still to work on stand out; over-flagged it sits in a red ring, since a flag around it is wrong.
    */
   private void paintNumber(Graphics2D g, int s, long now) {
      String text = this.value.trim();
      if (text.isEmpty()) {
         return;
      }
      int w = this.getWidth();
      int h = this.getHeight();
      if (this.standing == MineFieldGenerator.OVER) {
         int inset = Math.max(2, s / 10);
         int arc = Math.max(6, s / 3);
         g.setColor(withAlpha(HIT, 40));
         g.fillRoundRect(inset, inset, w - 2 * inset, h - 2 * inset, arc, arc);
         g.setColor(HIT);
         g.setStroke(new BasicStroke(Math.max(2f, s / 14f)));
         g.drawRoundRect(inset, inset, w - 2 * inset - 1, h - 2 * inset - 1, arc, arc);
      }
      double settled = this.standing != MineFieldGenerator.DONE || now < this.settleStart ? 0 : ease(Math.min(1, (now - this.settleStart) / (double) SETTLE_MS));
      int n = Math.max(1, Math.min(8, Integer.parseInt(text)));
      g.setFont(new Font("Dialog", Font.BOLD, Math.max(9, (int) (s * 0.62 * (1 - (1 - SETTLE_SCALE) * settled)))));
      FontMetrics fm = g.getFontMetrics();
      int x = (w - fm.stringWidth(text)) / 2;
      int y = (h + fm.getAscent() - fm.getDescent()) / 2;
      boolean dark = (this.row + this.column) % 2 == 1;
      g.setColor(settled == 0 ? NUMBER[n - 1] : mix(NUMBER[n - 1], dark ? SAND_DARK : SAND_LIGHT, SETTLE_FADE * settled));
      g.drawString(text, x, y);
   }

   /** A flag on a pole, planted (growing from the ground) as {@code t} goes from 0 to 1. */
   private void paintFlag(Graphics2D g, int s, double t) {
      drawFlag(g, this.getWidth() / 2, this.getHeight() / 2, s, t);
   }

   /** The flag of {@link #paintFlag} in a square of side {@code s} centered on (cx, cy); the scoreboard draws one too. */
   static void drawFlag(Graphics2D g, int cx, int cy, int s, double t) {
      double grow = 0.4 + 0.6 * overshoot(t);
      int ground = (int) (cy + s * 0.30);
      int poleX = (int) (cx - s * 0.10);
      int top = (int) (ground - s * 0.56 * grow);
      int stroke = Math.max(2, s / 16);
      g.setColor(POLE);
      g.fillRoundRect((int) (cx - s * 0.22), ground - stroke / 2, (int) (s * 0.40), Math.max(2, s / 12), stroke, stroke);
      g.fillRect(poleX - stroke / 2, top, stroke, ground - top);
      int flagW = (int) (s * 0.36 * grow);
      int flagH = (int) (s * 0.30 * grow);
      g.setColor(FLAG_RED);
      g.fillPolygon(new int[]{poleX + stroke / 2, poleX + stroke / 2 + flagW, poleX + stroke / 2},
         new int[]{top, top + flagH / 2, top + flagH}, 3);
   }

   private void paintQuestion(Graphics2D g, int s) {
      g.setFont(new Font("Dialog", Font.BOLD, Math.max(9, (int) (s * 0.6))));
      FontMetrics fm = g.getFontMetrics();
      int x = (this.getWidth() - fm.stringWidth("?")) / 2;
      int y = (this.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
      g.setColor(GRASS_EDGE);
      g.drawString("?", x + 1, y + 1);
      g.setColor(Color.WHITE);
      g.drawString("?", x, y);
   }

   /** A round mine with eight spikes and a shine, at {@code scale} of its full size. */
   private void paintMine(Graphics2D g, int s, double scale) {
      if (scale <= 0) {
         return;
      }
      int cx = this.getWidth() / 2;
      int cy = this.getHeight() / 2;
      double r = s * 0.24 * scale;
      double spike = s * 0.36 * scale;
      g.setColor(MINE);
      g.setStroke(new BasicStroke(Math.max(1.5f, (float) (s / 13.0 * scale)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      for (int i = 0; i < 4; i++) {
         double a = Math.PI / 4 * i;
         int dx = (int) Math.round(Math.cos(a) * spike);
         int dy = (int) Math.round(Math.sin(a) * spike);
         g.drawLine(cx - dx, cy - dy, cx + dx, cy + dy);
      }
      int d = (int) Math.round(2 * r);
      g.fillOval(cx - d / 2, cy - d / 2, d, d);
      int shine = Math.max(2, d / 4);
      g.setColor(new Color(255, 255, 255, 200));
      g.fillOval(cx - d / 4 - shine / 2, cy - d / 4 - shine / 2, shine, shine);
   }

   /** A red cross over a wrong flag, drawn stroke by stroke as {@code t} goes from 0 to 1. */
   private void paintCross(Graphics2D g, int s, double t) {
      int m = (int) (s * 0.2);
      int w = this.getWidth();
      int h = this.getHeight();
      g.setColor(new Color(160, 20, 20));
      g.setStroke(new BasicStroke(Math.max(2, s / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      double a = Math.min(1, t * 2);
      double b = Math.max(0, t * 2 - 1);
      g.drawLine(m, m, (int) (m + (w - 2 * m) * a), (int) (m + (h - 2 * m) * a));
      if (b > 0) {
         g.drawLine(w - m, m, (int) (w - m - (w - 2 * m) * b), (int) (m + (h - 2 * m) * b));
      }
   }

   private static double ease(double t) {
      return 1 - (1 - t) * (1 - t);
   }

   /** Eases past 1 and settles back, for things that pop in. */
   private static double overshoot(double t) {
      double c = 1.7;
      double u = t - 1;
      return 1 + (c + 1) * u * u * u + c * u * u;
   }

   private static Color withAlpha(Color c, int alpha) {
      return alpha >= 255 ? c : new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, alpha));
   }

   private static Color mix(Color a, Color b, double f) {
      return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * f), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * f),
         (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * f));
   }

   // ---- mouse ----

   @Override
   public final void mouseClicked(MouseEvent event) {
      if (this.game.wasDragged()) { // the press dragged the zoomed board
         return;
      }
      this.game.moveCursor(null);
      if (this.game.gameOver || event.getButton() == MouseEvent.BUTTON1) {
         this.play(true);
      } else if (event.getButton() == MouseEvent.BUTTON3) {
         this.play(false);
      }
   }

   /** A left click (reveal, or open around a number) or a right click (cycle the flag), from the mouse or the keyboard. */
   private void play(boolean reveal) {
      this.game.takeOver();
      if (this.game.gameOver) {
         if (this.shown != SHOW_HIT || !this.game.undoMine()) {
            this.game.restart();
         }
         return;
      }

      if (this.game.isRevealed(this.row, this.column)) {
         if (reveal) {
            this.game.chord(this.row, this.column);
         }
         return;
      }

      if (reveal && this.mark != FLAG) {
         this.game.revealFrom(this.row, this.column);
      } else if (!reveal) {
         this.mark = (this.mark + 1) % 3;
         if (this.mark == FLAG) {
            this.animate(0, PLANT_MS);
         } else {
            this.repaint();
         }
         this.describe(this.mark == FLAG ? "flag" : this.mark == QUESTION ? "question mark" : "hidden");
         this.game.setFlagged(this.row, this.column, this.mark == FLAG);
      }
   }

   /** The computer (Watch) flags this tile, planted like a player's flag. */
   void plantFlag() {
      if (this.mark == FLAG) {
         return;
      }
      this.mark = FLAG;
      this.animate(0, PLANT_MS);
      this.describe("flag");
      this.game.setFlagged(this.row, this.column, true);
   }

   @Override
   public final void mousePressed(MouseEvent event) {
      this.game.pressBoard(this.getX() + event.getX(), this.getY() + event.getY());
      if (event.getButton() == MouseEvent.BUTTON1 && this.shown == SHOW_HIDDEN && this.mark != FLAG && !this.game.gameOver) {
         this.pressed = true;
         this.repaint();
      }
   }

   /** Zoomed, a press that moves drags the board (in the board's own pixels, since this tile moves with it). */
   private void dragged(MouseEvent event) {
      if (this.game.dragBoard(this.getX() + event.getX(), this.getY() + event.getY()) && this.pressed) {
         this.pressed = false;
         this.repaint();
      }
   }

   @Override
   public final void mouseReleased(MouseEvent event) {
      if (this.pressed) {
         this.pressed = false;
         this.repaint();
      }
   }

   @Override
   public final void mouseEntered(MouseEvent event) {
      this.hover = true;
      this.repaint();
   }

   @Override
   public final void mouseExited(MouseEvent event) {
      this.hover = false;
      this.pressed = false;
      this.repaint();
   }
}
