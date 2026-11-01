package synthesizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * The strip above the keys. In free play it lists the songs with their best stars; during a song it shows the
 * notes coming up (the computer key and note name, wider for longer notes) with the next one lit; after a song it
 * shows the stars won.
 */
public final class LessonBar extends JPanel {
   public static final int HEIGHT = 100;
   private static final Color BACKGROUND = new Color(32, 35, 46);
   private static final Color CHIP = new Color(52, 57, 74);
   private static final Color PLAYED = new Color(46, 92, 70);
   private static final Color NEXT = new Color(255, 196, 60);
   private static final Color LISTEN = new Color(96, 164, 255);
   private static final Color TEXT = new Color(238, 240, 246);
   private static final Color SOFT = new Color(160, 166, 184);
   private static final Color STAR_OFF = new Color(88, 94, 112);
   private static final Font HEADLINE = new Font("SansSerif", Font.BOLD, 16);
   private static final Font HELP = new Font("SansSerif", Font.PLAIN, 12);
   private static final Font CHIP_FONT = new Font("SansSerif", Font.BOLD, 13);
   private static final Font LETTER_FONT = new Font("SansSerif", Font.BOLD, 18);
   private static final int CHIP_TOP = 38;
   private static final int MARGIN = 10;

   private final SongRecords records;
   private Lesson lesson;
   private int listenNote = -1;
   private boolean newBest;

   public LessonBar(SongRecords records) {
      this.records = records;
      this.setFocusable(false);
      this.setPreferredSize(new Dimension(0, HEIGHT));
   }

   /** Shows {@code lesson} (null for free play); {@code listenNote} is the note being played to the visitor, or -1. */
   public void show(Lesson lesson, int listenNote, boolean newBest) {
      this.lesson = lesson;
      this.listenNote = listenNote;
      this.newBest = newBest;
      this.repaint();
   }

   /** In free play, the song whose button is at a point, or -1. */
   public int songAt(int x, int y) {
      if (this.lesson != null || y < CHIP_TOP || y > HEIGHT - MARGIN || x < MARGIN) {
         return -1;
      }
      int song = (x - MARGIN) / this.menuChipWidth();
      return song < Song.ALL.length ? song : -1;
   }

   private int menuChipWidth() {
      return Math.max(1, (this.getWidth() - 2 * MARGIN) / Song.ALL.length);
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(BACKGROUND);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      if (this.lesson == null) {
         this.header(g, "Free play", "Pick a song: click it, or T or 1-5");
         this.paintMenu(g);
      } else if (this.lesson.finished()) {
         this.header(g, this.lesson.song().title() + ": done!", "R again   T next song   0 free play");
         this.paintResult(g);
      } else if (this.listenNote >= 0) {
         this.header(g, "Listening: " + this.lesson.song().title(), "Press any piano key to play it yourself");
         this.paintNotes(g, this.listenNote, LISTEN);
      } else {
         this.header(g, this.lesson.song().title(), "Note " + (this.lesson.position() + 1) + " of " + this.lesson.song().length()
            + "   misses " + this.lesson.misses() + "   L listen   R restart   0 free play");
         this.paintNotes(g, this.lesson.position(), NEXT);
      }
   }

   private void header(Graphics2D g, String headline, String help) {
      g.setFont(HELP);
      FontMetrics helpMetrics = g.getFontMetrics();
      int helpWidth = helpMetrics.stringWidth(help);
      g.setFont(HEADLINE);
      g.setColor(TEXT);
      g.drawString(headline, MARGIN, 24);
      int headlineWidth = g.getFontMetrics().stringWidth(headline);
      g.setFont(HELP);
      g.setColor(SOFT);
      // Right-aligned when there is room, else dropped: the chips below say enough on a narrow window.
      if (headlineWidth + helpWidth + 3 * MARGIN <= this.getWidth()) {
         g.drawString(help, this.getWidth() - MARGIN - helpWidth, 24);
      }
   }

   private void paintMenu(Graphics2D g) {
      int chipWidth = this.menuChipWidth();
      int chipHeight = HEIGHT - CHIP_TOP - MARGIN;
      for (int song = 0; song < Song.ALL.length; song++) {
         int x = MARGIN + song * chipWidth;
         g.setColor(CHIP);
         g.fillRoundRect(x + 2, CHIP_TOP, chipWidth - 4, chipHeight, 10, 10);
         g.setFont(CHIP_FONT);
         g.setColor(TEXT);
         // Too narrow for a name: just the number to press.
         String label = chipWidth < 90 ? String.valueOf(song + 1) : (song + 1) + "  " + Song.ALL[song].shortTitle();
         FontMetrics metrics = g.getFontMetrics();
         if (metrics.stringWidth(label) > chipWidth - 12) {
            while (label.length() > 4 && metrics.stringWidth(label + "..") > chipWidth - 12) {
               label = label.substring(0, label.length() - 1);
            }
            label = label.trim() + "..";
         }
         g.drawString(label, x + (chipWidth - metrics.stringWidth(label)) / 2, CHIP_TOP + 20);
         this.paintStars(g, x + chipWidth / 2, CHIP_TOP + 38, 7, this.records.stars(Song.ALL[song]));
      }
   }

   /** The song's notes from just before {@code current}, as many as fit; the current one in {@code lit}. */
   private void paintNotes(Graphics2D g, int current, Color lit) {
      Song song = this.lesson.song();
      int unit = 34;
      int chipHeight = HEIGHT - CHIP_TOP - MARGIN;
      int x = MARGIN;
      for (int note = Math.max(0, current - 2); note < song.length(); note++) {
         int width = (int)(unit * Math.max(1, Math.min(2, song.beats(note))));
         if (x + width > this.getWidth() - MARGIN) {
            break;
         }
         boolean now = note == current;
         g.setColor(now ? lit : note < current ? PLAYED : CHIP);
         g.fillRoundRect(x + 2, CHIP_TOP, width - 4, chipHeight, 10, 10);
         int key = song.key(note);
         g.setColor(now ? BACKGROUND : TEXT);
         g.setFont(LETTER_FONT);
         FontMetrics metrics = g.getFontMetrics();
         g.drawString(PianoPanel.LETTERS[key], x + (width - metrics.stringWidth(PianoPanel.LETTERS[key])) / 2, CHIP_TOP + 23);
         g.setFont(HELP);
         metrics = g.getFontMetrics();
         g.setColor(now ? BACKGROUND : SOFT);
         String name = Song.noteName(key);
         g.drawString(name, x + (width - metrics.stringWidth(name)) / 2, CHIP_TOP + 42);
         x += width;
      }
   }

   private void paintResult(Graphics2D g) {
      int stars = this.lesson.stars();
      this.paintStars(g, MARGIN + 54, CHIP_TOP + 26, 16, stars);
      int misses = this.lesson.misses();
      g.setFont(HEADLINE);
      g.setColor(TEXT);
      String line = misses == 0 ? "No misses!" : misses == 1 ? "1 miss" : misses + " misses";
      g.drawString(line, MARGIN + 120, CHIP_TOP + 22);
      if (this.newBest) {
         int lineWidth = g.getFontMetrics().stringWidth(line);
         g.setColor(NEXT);
         g.drawString("New best!", MARGIN + 136 + lineWidth, CHIP_TOP + 22);
      }
      g.setFont(HELP);
      g.setColor(SOFT);
      g.drawString(stars == 3 ? "Perfect. Try another song." : "Three stars for no misses. Press R to try again.", MARGIN + 120, CHIP_TOP + 42);
   }

   /** Three stars centred on (centerX, centerY), the first {@code lit} of them gold. */
   private void paintStars(Graphics2D g, int centerX, int centerY, int radius, int lit) {
      for (int star = 0; star < 3; star++) {
         g.setColor(star < lit ? NEXT : STAR_OFF);
         star(g, centerX + (star - 1) * radius * 5 / 2, centerY, radius);
      }
   }

   private static void star(Graphics2D g, int centerX, int centerY, int radius) {
      int[] xs = new int[10];
      int[] ys = new int[10];
      for (int point = 0; point < 10; point++) {
         double r = point % 2 == 0 ? radius : radius * 0.45;
         double angle = -Math.PI / 2 + point * Math.PI / 5;
         xs[point] = centerX + (int)Math.round(r * Math.cos(angle));
         ys[point] = centerY + (int)Math.round(r * Math.sin(angle));
      }
      g.fillPolygon(xs, ys, 10);
   }
}
