package windowse;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;

/**
 * A ready-made list, shown where the storyboard's frames go while the list is empty: the screen it leaves, its name and
 * its keys. Pointing at it (or Tab to it) shows its screen on the little monitor; a click (or Space) puts it on the list.
 */
final class ReadyList extends JButton {
   /** The lists offered, each with what it shows on Windows. */
   static final Recipe[] RECIPES = {
      new Recipe("Side by side", WindowsEWindow.SIDE_BY_SIDE, "two File Explorer windows, one on each half"),
      new Recipe("Big window", Step.parse("E-Up"), "one File Explorer window, maximized"),
      new Recipe("Task view", Step.parse("Ex4-Tab"), "four File Explorer windows, all at a glance"),
      new Recipe("Hide all", Step.parse("Ex3-D"), "three File Explorer windows, then Win+D hides them all"),
   };
   private static final Font NAME = new Font("SansSerif", Font.BOLD, 12);
   private static final Font KEYS = new Font("SansSerif", Font.PLAIN, 11);
   private static final Color EDGE = new Color(170, 176, 190);
   private static final Color INK = new Color(28, 32, 40);
   private static final Color MUTED = new Color(92, 98, 110);
   private static final Color HOVER = new Color(222, 236, 252);

   static final class Recipe {
      final String name;
      final List<Step> steps;
      final String shows;
      final Screen after;

      Recipe(String name, List<Step> steps, String shows) {
         this.name = name;
         this.steps = steps;
         this.shows = shows;
         this.after = Screen.after(steps);
      }

      /** Its keys as the frames write them: "E, Left, E, Right". */
      String keys() {
         StringBuilder s = new StringBuilder();
         for (Step step : this.steps) {
            s.append(s.length() == 0 ? "" : ", ").append(StepPill.text(step));
         }
         return s.toString();
      }
   }

   final Recipe recipe;
   private boolean hover;

   /** onPeek: its screen is wanted on the monitor (null: no longer); onPick: put it on the list. */
   ReadyList(Recipe recipe, Consumer<Recipe> onPeek, Consumer<Recipe> onPick) {
      this.recipe = recipe;
      this.setText("");
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      String says = "Ready list " + recipe.name + ", " + WindowsEWindow.labels(recipe.steps) + ": " + recipe.shows + ". Puts these steps on the list";
      this.setToolTipText(says);
      this.getAccessibleContext().setAccessibleName(says);
      this.addActionListener(event -> onPick.accept(recipe));
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseEntered(MouseEvent event) {
            ReadyList.this.lit(true);
            onPeek.accept(recipe);
         }

         @Override
         public void mouseExited(MouseEvent event) {
            ReadyList.this.lit(false);
            onPeek.accept(null);
         }
      });
      this.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(java.awt.event.FocusEvent event) {
            onPeek.accept(recipe);
         }

         @Override
         public void focusLost(java.awt.event.FocusEvent event) {
            onPeek.accept(null);
         }
      });
   }

   private void lit(boolean hover) {
      this.hover = hover;
      this.repaint();
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int w = this.getWidth();
      int h = this.getHeight();
      boolean on = this.hover || this.isFocusOwner();
      g.setColor(on ? HOVER : Color.WHITE);
      g.fillRoundRect(0, 0, w, h, 12, 12);
      g.setColor(on ? KeyCap.ACCENT : EDGE);
      g.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
      if (this.isFocusOwner()) {
         g.drawRoundRect(2, 2, w - 5, h - 5, 10, 10);
      }
      // The screen it leaves on the left (16:10), its name and keys beside it.
      int sh = h - 10;
      int sw = Math.min(sh * 8 / 5, w * 9 / 20);
      sh = sw * 5 / 8;
      MiniScreen.paint(g, this.recipe.after, 5, (h - sh) / 2, sw, sh);
      int tx = 5 + sw + 8;
      int tw = w - tx - 6;
      g.setColor(INK);
      g.setFont(NAME);
      g.drawString(this.recipe.name, tx, 20);
      g.setColor(MUTED);
      g.setFont(KEYS);
      FontMetrics fm = g.getFontMetrics();
      List<String> lines = ShortcutBoard.wrap(this.recipe.keys(), fm, tw, 2);
      for (int i = 0; i < lines.size(); i++) {
         g.drawString(lines.get(i), tx, 36 + 14 * i);
      }
   }
}
