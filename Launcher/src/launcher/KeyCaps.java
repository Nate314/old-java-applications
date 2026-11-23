package launcher;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JLabel;

/**
 * Wrapping text that draws the keys it names ("Space", "W A S D", "(T)", "Press E", "Win+E", "arrow keys") as
 * keycaps, so a first look at the detail pane shows what to press before Play. A JLabel, so screen readers get
 * the plain text.
 */
final class KeyCaps extends JLabel {
   private static final Pattern WORD = Pattern.compile("\\S+");
   private static final Pattern NAMED = Pattern.compile("Space|Enter|Esc|Tab|Shift|Backspace|Delete|Win\\+[A-Z]");
   private static final String TRAIL = ").,;:!?";
   private static final int PAD = 5;     // inside a cap, either side of its letters
   private static final int GAP = 2;     // outside a cap, so caps in a row ("W A S D") stand apart

   private final int width;
   private List<int[]> keys = List.of();

   /** Text wrapped to this width (borders included); \n starts a new line. */
   KeyCaps(int width) {
      this.width = width;
   }

   @Override
   public void setText(String text) {
      setText(text, true);
   }

   /** The text, its keys as caps or (for text not written for this, like an old README) all plain. */
   void setText(String text, boolean caps) {
      keys = text == null || !caps ? List.of() : find(text);
      super.setText(text);
      revalidate();
      repaint();
   }

   /** Where the keys are in the text: {start, end} pairs, in order. */
   static List<int[]> find(String s) {
      List<int[]> words = new ArrayList<>();
      List<Boolean> bracketed = new ArrayList<>(), closed = new ArrayList<>();
      Matcher m = WORD.matcher(s);
      while (m.find()) {
         int a = m.start(), b = m.end();
         boolean open = s.charAt(a) == '(';
         if (open) a++;
         while (b > a && TRAIL.indexOf(s.charAt(b - 1)) >= 0) b--;
         words.add(new int[] {a, b});
         bracketed.add(open);
         closed.add(open && b < m.end() && s.charAt(b) == ')');
      }
      List<int[]> keys = new ArrayList<>();
      for (int i = 0; i < words.size(); i++) {
         int[] w = words.get(i);
         String t = s.substring(w[0], w[1]);
         if (NAMED.matcher(t).matches()) keys.add(w);
         else if (t.equals("arrow") && word(s, words, i + 1).equals("keys")) {
            keys.add(new int[] {w[0], words.get(++i)[1]});
         } else if (t.length() == 1 && (t.charAt(0) >= 'A' && t.charAt(0) <= 'Z' || Character.isDigit(t.charAt(0)))) {
            // "A" and "I" are mostly words and a digit mostly a number: those are keys only as "(A ...", "(9)",
            // "I for ...", "press 9" or in a row of keys ("W A S D"); "(4 days ago)" is not one.
            boolean digit = Character.isDigit(t.charAt(0));
            boolean plain = !digit && t.charAt(0) != 'A' && !t.equals("I");
            if (plain || (digit ? closed.get(i) : bracketed.get(i)) || word(s, words, i + 1).equals("for")
               || word(s, words, i - 1).equalsIgnoreCase("press")
               || t.equals("A") && (letter(s, words, i - 1) || letter(s, words, i + 1))) keys.add(w);
         }
      }
      return keys;
   }

   private static String word(String s, List<int[]> words, int i) {
      return i < 0 || i >= words.size() ? "" : s.substring(words.get(i)[0], words.get(i)[1]);
   }

   private static boolean letter(String s, List<int[]> words, int i) {
      if (i < 0 || i >= words.size() || words.get(i)[1] - words.get(i)[0] != 1) return false;
      char c = s.charAt(words.get(i)[0]);
      return c >= 'B' && c <= 'Z' && c != 'I';
   }

   private Font capFont() {
      return getFont().deriveFont(Font.BOLD, getFont().getSize2D() - 1.5f);
   }

   /** The end of the next run of text from p to end that is all one key or all plain. */
   private int piece(int p, int end, int j) {
      if (j < keys.size() && keys.get(j)[0] == p) return keys.get(j)[1];
      return j < keys.size() && keys.get(j)[0] < end ? keys.get(j)[0] : end;
   }

   /**
    * Lays the text out word by word (a key and the punctuation round it stay together; "arrow keys" is one) and,
    * when g is not null, draws it. Returns the height.
    */
   private int layout(Graphics2D g, int room) {
      String s = getText();
      if (s == null || s.isEmpty()) return 0;
      FontMetrics fm = getFontMetrics(getFont()), cm = getFontMetrics(capFont());
      int line = fm.getHeight(), space = fm.charWidth(' ');
      int x = 0, y = 0, k = 0;
      for (int i = 0; i < s.length(); ) {
         char c = s.charAt(i);
         if (c == '\n') {
            x = 0;
            y += line;
         }
         if (c == ' ' || c == '\n') {
            i++;
            continue;
         }
         int end = i;
         while (end < s.length() && s.charAt(end) != ' ' && s.charAt(end) != '\n') end++;
         while (k < keys.size() && keys.get(k)[1] <= i) k++;
         if (k < keys.size() && keys.get(k)[0] < end && keys.get(k)[1] > end) end = keys.get(k)[1];   // "arrow keys"
         int w = draw(null, s, i, end, k, 0, 0, fm, cm);
         if (x > 0 && x + w > room) {
            x = 0;
            y += line;
         }
         if (g != null) draw(g, s, i, end, k, x, y + fm.getAscent(), fm, cm);
         x += w + space;
         i = end;
      }
      return y + line;
   }

   /** Draws one word from x (or with g null only measures it); returns its width. */
   private int draw(Graphics2D g, String s, int p, int end, int j, int x, int base, FontMetrics fm, FontMetrics cm) {
      int x0 = x;
      while (p < end) {
         boolean cap = j < keys.size() && keys.get(j)[0] == p;
         int q = piece(p, end, j);
         String t = s.substring(p, q);
         p = q;
         if (cap) j++;
         int w = cap ? cm.stringWidth(t) + 2 * PAD : fm.stringWidth(t);
         if (g == null) {
            x += cap ? w + 2 * GAP : w;
            continue;
         }
         if (cap) {
            int top = base - fm.getAscent() + 2, h = fm.getAscent() + fm.getDescent() - 3;
            x += GAP;
            g.setColor(new Color(0, 0, 0, 110));   // the key's lower edge, as on a real keycap
            g.fillRoundRect(x, top + 2, w, h, 8, 8);
            g.setColor(new Color(0x3a4050));
            g.fillRoundRect(x, top, w, h, 8, 8);
            g.setColor(new Color(0x5c6478));
            g.drawRoundRect(x, top, w - 1, h - 1, 8, 8);
            g.setFont(capFont());
            g.setColor(Ui.TEXT);
            g.drawString(t, x + PAD, base - 1);
            x += w + GAP;
         } else {
            g.setFont(getFont());
            g.setColor(getForeground());
            g.drawString(t, x, base);
            x += w;
         }
      }
      return x - x0;
   }

   /** The width to wrap at: the one it was given, or less when a scroll bar takes some of the pane. */
   private int room() {
      Insets in = getInsets();
      return (getWidth() > 0 ? Math.min(getWidth(), width) : width) - in.left - in.right;
   }

   /** Narrowed after its height was worked out: work it out again for the new width. */
   @Override
   public void setBounds(int x, int y, int w, int h) {
      boolean resized = w != getWidth();
      super.setBounds(x, y, w, h);
      if (resized && getPreferredSize().height != h) javax.swing.SwingUtilities.invokeLater(this::revalidate);
   }

   @Override
   protected void paintComponent(Graphics g) {
      if (isOpaque()) {
         g.setColor(getBackground());
         g.fillRect(0, 0, getWidth(), getHeight());
      }
      Graphics2D g2 = Ui.smooth(g);
      g2.translate(getInsets().left, getInsets().top);
      layout(g2, room());
      g2.dispose();
   }

   @Override
   public Dimension getPreferredSize() {
      Insets in = getInsets();
      return new Dimension(width, layout(null, room()) + in.top + in.bottom);
   }

   @Override
   public Dimension getMaximumSize() {
      return getPreferredSize();
   }

   @Override
   public Dimension getMinimumSize() {
      return getPreferredSize();
   }
}
