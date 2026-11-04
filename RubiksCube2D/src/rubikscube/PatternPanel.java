package rubikscube;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.swing.JPanel;

/**
 * The Patterns card, in place of the message and help: a list of the {@link Patterns}, each with a small picture of
 * the cube wearing it and a tick once made by hand, or one pattern's card: its picture, what it is, its turns as
 * chips, a line on what is happening, and Show me and Make it buttons. Painted by hand like the Teach me card.
 */
final class PatternPanel extends JPanel {
   /** What a click asks for: {@link #BACK} to the list, {@link #SHOW} or {@link #MAKE}; 0 and up pick that pattern. */
   static final int BACK = -1;
   static final int SHOW = -2;
   static final int MAKE = -3;
   private static final Color GOLD = new Color(214, 160, 30);
   private static final Color BUTTON = new Color(40, 96, 220);
   private static final int ROW = 33;
   private final IntConsumer onClick;
   private final List<Rectangle> targets = new ArrayList<>();
   private final List<Integer> actions = new ArrayList<>();
   private Set<String> made = Collections.emptySet();
   /** The pattern on the card, or null for the list. */
   private Patterns.Pattern pattern = null;
   private List<String> chipColors = Collections.emptyList();
   private List<Boolean> chipClockwise = Collections.emptyList();
   /** The next turn to make (ringed, those before it faded), or -1 for plain chips. */
   private int chipNow = -1;
   private String status = "";
   private boolean showing = false;
   private boolean making = false;
   private int hover = Integer.MIN_VALUE;

   PatternPanel(IntConsumer onClick) {
      this.onClick = onClick;
      this.setLayout(null);
      this.setOpaque(false);
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            int action = PatternPanel.this.actionAt(event.getX(), event.getY());
            if (action != Integer.MIN_VALUE) {
               PatternPanel.this.onClick.accept(action);
            }
         }

         @Override
         public void mouseMoved(MouseEvent event) {
            int action = PatternPanel.this.actionAt(event.getX(), event.getY());
            if (action != PatternPanel.this.hover) {
               PatternPanel.this.hover = action;
               PatternPanel.this.repaint();
            }
         }

         @Override
         public void mouseExited(MouseEvent event) {
            PatternPanel.this.hover = Integer.MIN_VALUE;
            PatternPanel.this.repaint();
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
   }

   /** The list of patterns, ticking those in {@code made}. */
   void list(Set<String> made) {
      this.made = made;
      this.pattern = null;
      this.repaint();
   }

   /**
    * One pattern's card: its turns as chips (with turn {@code now} next, or -1 for none), a status line, and
    * whether Show me or Make it is under way (Show me then reads Stop, and Make it Start again while making).
    */
   void card(Patterns.Pattern pattern, Set<String> made, List<String> colors, List<Boolean> clockwise, int now,
         String status, boolean showing, boolean making) {
      this.pattern = pattern;
      this.made = made;
      this.chipColors = colors;
      this.chipClockwise = clockwise;
      this.chipNow = now;
      this.status = status;
      this.showing = showing;
      this.making = making;
      this.repaint();
   }

   private int actionAt(int x, int y) {
      for (int i = 0; i < this.targets.size(); i++) {
         if (this.targets.get(i).contains(x, y)) {
            return this.actions.get(i);
         }
      }
      return Integer.MIN_VALUE;
   }

   private void target(int x, int y, int width, int height, int action) {
      this.targets.add(new Rectangle(x, y, width, height));
      this.actions.add(action);
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int width = this.getWidth();
      int height = this.getHeight();
      CardPaint.card(g, width, height);
      this.targets.clear();
      this.actions.clear();
      if (this.pattern == null) {
         this.paintList(g, width, height);
      } else {
         this.paintCard(g, width, height);
      }
   }

   private void paintList(Graphics2D g, int width, int height) {
      int x = 9;
      g.setFont(new Font("Dialog", Font.BOLD, 10));
      g.setColor(CardPaint.MUTED);
      int count = 0;
      for (Patterns.Pattern each : Patterns.ALL) {
         count += this.made.contains(each.key) ? 1 : 0;
      }
      g.drawString("PATTERNS", x, 16);
      String tally = count + " of " + Patterns.ALL.size() + " made";
      g.drawString(tally, width - x - g.getFontMetrics().stringWidth(tally), 16);
      int y = 24;
      for (int i = 0; i < Patterns.ALL.size(); i++) {
         Patterns.Pattern each = Patterns.ALL.get(i);
         if (this.hover == i) {
            g.setColor(new Color(226, 234, 250));
            g.fillRoundRect(4, y, width - 8, ROW - 2, 8, 8);
         }
         drawCube(g, Patterns.apply(Patterns.solved(), each.turns), x + 13, y + 15, 5);
         g.setColor(CardPaint.INK);
         g.setFont(new Font("Dialog", Font.BOLD, 12));
         g.drawString((i + 1) + ". " + each.name, x + 32, y + 13);
         g.setFont(new Font("Dialog", Font.PLAIN, 10));
         g.setColor(CardPaint.MUTED);
         g.drawString(each.turns.size() + " turns", x + 32, y + 26);
         if (this.made.contains(each.key)) {
            g.setColor(CardPaint.DONE);
            g.drawString("✓ made", x + 82, y + 26);
         }
         this.target(4, y, width - 8, ROW - 2, i);
         y += ROW;
      }
      g.setFont(new Font("Dialog", Font.PLAIN, 11));
      g.setColor(CardPaint.MUTED);
      g.drawString("Pick one (or press 1-7).", x, height - 9);
   }

   private void paintCard(Graphics2D g, int width, int height) {
      int x = 9;
      int inner = width - 2 * x;
      Patterns.Pattern p = this.pattern;
      g.setFont(new Font("Dialog", Font.BOLD, 10));
      g.setColor(this.hover == BACK ? CardPaint.NOW : CardPaint.MUTED);
      g.drawString("< ALL PATTERNS", x, 16);
      this.target(0, 0, 110, 24, BACK);
      int number = Patterns.ALL.indexOf(p) + 1;
      g.setColor(CardPaint.MUTED);
      String place = number + " of " + Patterns.ALL.size();
      g.drawString(place, width - x - g.getFontMetrics().stringWidth(place), 16);
      // The cube wearing the pattern, then its name and turn count beside it.
      drawCube(g, Patterns.apply(Patterns.solved(), p.turns), x + 24, 62, 10);
      int textX = x + 56;
      g.setColor(CardPaint.INK);
      g.setFont(new Font("Dialog", Font.BOLD, 13));
      int y = CardPaint.wrap(g, p.name, textX, 42, width - textX - x, 15, 3);
      g.setFont(new Font("Dialog", Font.PLAIN, 11));
      g.setColor(CardPaint.MUTED);
      g.drawString(p.turns.size() + " quarter turns", textX, y + 15);
      if (this.made.contains(p.key)) {
         g.setColor(CardPaint.DONE);
         g.drawString("✓ made by hand", textX, y + 29);
      }
      g.setColor(CardPaint.INK);
      g.setFont(new Font("Dialog", Font.PLAIN, 12));
      y = CardPaint.wrap(g, p.about, x, 110, inner, 14, 4);
      CardPaint.chips(g, x, y + 8, this.chipColors, this.chipClockwise, this.chipNow);
      y += 8 + CardPaint.chipRows(this.chipColors.size()) * CardPaint.PITCH;
      g.setFont(new Font("Dialog", Font.PLAIN, 11));
      boolean done = this.status.startsWith("Made it");
      g.setColor(done ? GOLD.darker() : CardPaint.INK);
      if (done) {
         g.setFont(new Font("Dialog", Font.BOLD, 12));
      }
      int buttonY = height - 34;
      CardPaint.wrap(g, this.status, x, y + 14, inner, 13, Math.max(1, (buttonY - y - 6) / 13));
      int half = (inner - 6) / 2;
      this.button(g, x, buttonY, half, this.showing || this.making ? "Stop" : "Show me", SHOW, false);
      this.button(g, x + half + 6, buttonY, half, this.making ? "Start again" : "Make it", MAKE, true);
   }

   private void button(Graphics2D g, int x, int y, int width, String label, int action, boolean primary) {
      boolean over = this.hover == action;
      Color fill = primary ? (over ? BUTTON.darker() : BUTTON) : over ? new Color(222, 230, 246) : Color.WHITE;
      g.setColor(fill);
      g.fillRoundRect(x, y, width, 26, 10, 10);
      g.setColor(primary ? BUTTON.darker() : CardPaint.EDGE);
      g.drawRoundRect(x, y, width - 1, 25, 10, 10);
      g.setFont(new Font("Dialog", Font.BOLD, 12));
      g.setColor(primary ? Color.WHITE : CardPaint.INK);
      int textWidth = g.getFontMetrics().stringWidth(label);
      g.drawString(label, x + (width - textWidth) / 2, y + 17);
      this.target(x, y, width, 26, action);
   }

   /**
    * A small cube seen from above its front right corner, its top, front and right faces showing {@code state}
    * (each sticker's face index, as {@link Patterns#apply} gives), centered at (cx, cy) with stickers about
    * {@code unit} pixels across.
    */
   static void drawCube(Graphics2D g, int[] state, int cx, int cy, int unit) {
      int[][] normals = {{0, 1, 0}, {0, 0, 1}, {1, 0, 0}};
      double[] shades = {1.0, 0.86, 0.7};
      int[] xs = new int[4];
      int[] ys = new int[4];
      for (int f = 0; f < 3; f++) {
         int[] n = normals[f];
         // The face's two in-face directions are the axes its normal does not lie along.
         int a = n[0] != 0 ? 1 : 0;
         int b = n[2] != 0 ? 1 : 2;
         // The cube's dark body under the stickers.
         project(new double[]{1.5 * n[0], 1.5 * n[1], 1.5 * n[2]}, a, b, 1.5, cx, cy, unit, xs, ys);
         g.setColor(new Color(34, 36, 44));
         g.fillPolygon(xs, ys, 4);
         for (int s = 0; s < 54; s++) {
            if (!same(CubeGeometry.NORMAL[s], n)) {
               continue;
            }
            int[] pos = CubeGeometry.POS[s];
            project(new double[]{pos[0] + 0.5 * n[0], pos[1] + 0.5 * n[1], pos[2] + 0.5 * n[2]}, a, b, 0.43, cx, cy, unit, xs, ys);
            Color color = CubeView3D.colorOf(CubeGeometry.FACE_COLORS[state[s]]);
            g.setColor(CubeView3D.mix(color, Color.BLACK, 1.0 - shades[f]));
            g.fillPolygon(xs, ys, 4);
         }
      }
   }

   /** The four corners of a square of half-size {@code half} around {@code center}, lying along axes a and b, on screen. */
   private static void project(double[] center, int a, int b, double half, int cx, int cy, int unit, int[] xs, int[] ys) {
      double[][] corners = {{-half, -half}, {half, -half}, {half, half}, {-half, half}};
      for (int k = 0; k < 4; k++) {
         double[] point = center.clone();
         point[a] += corners[k][0];
         point[b] += corners[k][1];
         // Isometric: x runs down to the right, z down to the left, y straight up.
         xs[k] = (int) Math.round(cx + (point[0] - point[2]) * 0.866 * unit);
         ys[k] = (int) Math.round(cy + ((point[0] + point[2]) * 0.5 - point[1]) * unit);
      }
   }

   private static boolean same(int[] a, int[] b) {
      return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
   }
}
