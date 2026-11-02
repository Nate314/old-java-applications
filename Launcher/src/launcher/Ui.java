package launcher;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** The launcher's colors, fonts and the few hand-drawn widgets it needs on a dark desk. */
final class Ui {
   static final Color DESK = new Color(0x111318);
   static final Color PANEL = new Color(0x181b22);
   static final Color TILE = new Color(0x1f232c);
   static final Color TILE_HOVER = new Color(0x272c38);
   static final Color LINE = new Color(0x2e3341);
   static final Color TEXT = new Color(0xe9ebf1);
   static final Color MUTED = new Color(0x9aa1b2);
   static final Color ACCENT = new Color(0x4f8cff);
   static final Color GO = new Color(0x34c47c);
   static final Color WARN = new Color(0xffb547);

   static final String FAMILY = "Segoe UI";

   private Ui() {
   }

   static Font font(int style, float size) {
      return new Font(FAMILY, style, 1).deriveFont(size);
   }

   /** The color of a category: games orange, music violet, toys yellow, tools green. */
   static Color category(String cat) {
      switch (cat) {
         case "game": return new Color(0xff7a59);
         case "music": return new Color(0xb488ff);
         case "toy": return new Color(0xffc93c);
         default: return new Color(0x3ccf91);
      }
   }

   static String categoryName(String cat) {
      switch (cat) {
         case "game": return "Game";
         case "music": return "Music";
         case "toy": return "Toy";
         default: return "Tool";
      }
   }

   static Graphics2D smooth(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      return g2;
   }

   /** A pill with a colored dot and a word; returns its width. */
   static int chip(Graphics2D g, int x, int y, String text, Color color) {
      g.setFont(font(Font.BOLD, 11.5f));
      FontMetrics fm = g.getFontMetrics();
      int w = fm.stringWidth(text) + 26;
      int h = 20;
      g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 38));
      g.fillRoundRect(x, y, w, h, h, h);
      g.setColor(color);
      g.fillOval(x + 8, y + 7, 6, 6);
      g.drawString(text, x + 18, y + (h + fm.getAscent() - fm.getDescent()) / 2);
      return w;
   }

   /**
    * A flat, rounded button. Never focusable, so the search field keeps the keyboard; Alt plus its mnemonic
    * still presses it. A primary button is filled with the accent; a toggle shows its selected state.
    */
   static final class FlatButton extends JButton {
      private final boolean primary;
      private boolean hover;
      private Color fill = ACCENT;

      FlatButton(String text, boolean primary) {
         super(text);
         this.primary = primary;
         setFocusable(false);
         setContentAreaFilled(false);
         setBorderPainted(false);
         setFocusPainted(false);
         setRolloverEnabled(true);
         setForeground(TEXT);
         setFont(font(primary ? Font.BOLD : Font.PLAIN, primary ? 15f : 13f));
         setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         setBorder(BorderFactory.createEmptyBorder(primary ? 10 : 6, 16, primary ? 10 : 6, 16));
         addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
               hover = true;
               repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
               hover = false;
               repaint();
            }
         });
      }

      void setFill(Color c) {
         fill = c;
         repaint();
      }

      @Override
      protected void paintComponent(Graphics g) {
         Graphics2D g2 = smooth(g);
         int w = getWidth();
         int h = getHeight();
         int arc = primary ? 12 : h;
         Color bg;
         if (!isEnabled()) bg = primary ? LINE : TILE;
         else if (primary) bg = hover ? fill.brighter() : fill;
         else if (isSelected()) bg = new Color(0x2f4f86);
         else bg = hover ? TILE_HOVER : TILE;
         g2.setColor(bg);
         g2.fillRoundRect(0, 0, w, h, arc, arc);
         if (!primary) {
            g2.setColor(isSelected() ? ACCENT : LINE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
         }
         g2.setFont(getFont());
         FontMetrics fm = g2.getFontMetrics();
         String text = getText();
         g2.setColor(isEnabled() ? TEXT : MUTED);
         g2.drawString(text, (w - fm.stringWidth(text)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
         g2.dispose();
      }
   }

   /** A dark search field with a placeholder. */
   static final class SearchField extends JTextField {
      private final String placeholder;

      SearchField(String placeholder) {
         this.placeholder = placeholder;
         setOpaque(false);
         setForeground(TEXT);
         setCaretColor(TEXT);
         setSelectionColor(new Color(0x2f4f86));
         setSelectedTextColor(TEXT);
         setFont(font(Font.PLAIN, 14f));
         setBorder(BorderFactory.createEmptyBorder(8, 34, 8, 12));
         setPreferredSize(new Dimension(280, 38));
      }

      @Override
      protected void paintComponent(Graphics g) {
         Graphics2D g2 = smooth(g);
         int w = getWidth();
         int h = getHeight();
         g2.setColor(TILE);
         g2.fillRoundRect(0, 0, w, h, h, h);
         g2.setColor(isFocusOwner() ? ACCENT : LINE);
         g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
         // magnifying glass
         g2.setColor(MUTED);
         g2.setStroke(new BasicStroke(1.8f));
         g2.drawOval(13, h / 2 - 7, 11, 11);
         g2.drawLine(22, h / 2 + 3, 26, h / 2 + 7);
         if (getText().isEmpty()) {
            g2.setFont(getFont());
            Insets in = getInsets();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(placeholder, in.left, (h + fm.getAscent() - fm.getDescent()) / 2);
         }
         g2.dispose();
         super.paintComponent(g);
      }
   }

   /** A thin rounded scroll bar thumb with no arrows. */
   static final class ThinScrollBarUI extends BasicScrollBarUI {
      @Override
      protected void configureScrollBarColors() {
         trackColor = DESK;
         thumbColor = LINE;
      }

      @Override
      protected JButton createDecreaseButton(int orientation) {
         return zero();
      }

      @Override
      protected JButton createIncreaseButton(int orientation) {
         return zero();
      }

      private static JButton zero() {
         JButton b = new JButton();
         b.setPreferredSize(new Dimension(0, 0));
         return b;
      }

      @Override
      protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) {
         g.setColor(DESK);
         g.fillRect(r.x, r.y, r.width, r.height);
      }

      @Override
      protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
         Graphics2D g2 = smooth(g);
         g2.setColor(isDragging ? MUTED : new Color(0x3a4050));
         g2.fillRoundRect(r.x + 3, r.y + 2, r.width - 6, r.height - 4, r.width - 6, r.width - 6);
         g2.dispose();
      }

      static void apply(JScrollBar bar) {
         bar.setUI(new ThinScrollBarUI());
         bar.setPreferredSize(new Dimension(12, 12));
         bar.setUnitIncrement(32);
      }
   }
}
