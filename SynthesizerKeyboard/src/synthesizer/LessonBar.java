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
 * The strip above the keys. In free play it lists the songs with their best stars, the recorded tune if there is
 * one, and Record; during a song it shows the notes coming up (the computer key and note name, wider for longer
 * notes) with the next one lit; after a song it shows the stars won. While recording it shows the notes so far.
 * The microphone button sits at the top right, and doubles as a tuner while the microphone listens.
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
   private static final Color RECORD = new Color(232, 72, 72);
   private static final Color STAR_OFF = new Color(88, 94, 112);
   private static final Color IN_TUNE = new Color(110, 214, 140);
   /** The microphone's states, for setMic. */
   public static final int MIC_OFF = 0;
   public static final int MIC_ASKING = 1;
   public static final int MIC_ON = 2;
   public static final int MIC_FAILED = 3;
   private static final int MIC_TOP = 8;
   private static final int MIC_HEIGHT = 24;
   private static final Font HEADLINE = new Font("SansSerif", Font.BOLD, 16);
   private static final Font HELP = new Font("SansSerif", Font.PLAIN, 12);
   private static final Font CHIP_FONT = new Font("SansSerif", Font.BOLD, 13);
   private static final Font LETTER_FONT = new Font("SansSerif", Font.BOLD, 18);
   private static final int CHIP_TOP = 38;
   private static final int MARGIN = 10;
   private static final int NOTE_UNIT = 34;

   private final SongRecords records;
   private Lesson lesson;
   private int listenNote = -1;
   private boolean newBest;
   /** The songs in the free play menu, which ends with a Record chip. */
   private Song[] menu = Song.ALL;
   /** Whether recording is on, and the notes so far (null before the first). */
   private boolean recording;
   private Song recorded;
   /** A line shown in gold in place of the help, such as "Link copied", until it is cleared; null for none. */
   private String notice;
   private int mic = MIC_OFF;
   /** The note the microphone hears (MIDI), or -1, and how far off it is in cents. */
   private int micNote = -1;
   private int micCents;

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

   public void setMenu(Song[] menu) {
      this.menu = menu;
      this.repaint();
   }

   /** Shows the recording ({@code soFar} is null before the first note), or ends it. */
   public void setRecording(boolean on, Song soFar) {
      this.recording = on;
      this.recorded = soFar;
      this.repaint();
   }

   public void setNotice(String notice) {
      this.notice = notice;
      this.repaint();
   }

   /** Shows the microphone's state, and while it is on the note it hears ({@code midi} -1 for none). */
   public void setMic(int state, int midi, int cents) {
      if (state != this.mic || midi != this.micNote || cents != this.micCents) {
         this.mic = state;
         this.micNote = midi;
         this.micCents = cents;
         this.repaint();
      }
   }

   /** Whether a point is on the microphone button. */
   public boolean micAt(int x, int y) {
      return y >= MIC_TOP && y < MIC_TOP + MIC_HEIGHT && x >= this.micLeft() && x < this.getWidth() - MARGIN;
   }

   /** The button shows a word or the note heard when there is room, else the microphone alone. */
   private int micWidth() {
      return this.getWidth() < 420 ? 34 : 132;
   }

   private int micLeft() {
      return this.getWidth() - MARGIN - this.micWidth();
   }

   /** In free play, the chip at a point: a song's index in the menu, the menu's length for Record, or -1. */
   public int chipAt(int x, int y) {
      if (this.recording || this.lesson != null || y < CHIP_TOP || y > HEIGHT - MARGIN || x < MARGIN) {
         return -1;
      }
      int chip = (x - MARGIN) / this.menuChipWidth();
      return chip <= this.menu.length ? chip : -1;
   }

   private int menuChipWidth() {
      return Math.max(1, (this.getWidth() - 2 * MARGIN) / (this.menu.length + 1));
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(BACKGROUND);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      if (this.recording) {
         int notes = this.recorded == null ? 0 : this.recorded.length();
         this.header(g, "Recording", RECORD, (notes == 0 ? "Play your tune" : notes + " of " + Tune.MAX_NOTES + " notes")
            + "   Space or click here to stop");
         if (this.recorded == null) {
            this.paintLine(g, "Play the keys. Your timing sets the note lengths.");
         } else {
            this.paintNotes(g, this.recorded, this.lastFitting(this.recorded), notes - 1, RECORD);
         }
      } else if (this.lesson == null) {
         this.header(g, "Free play", TEXT, "Pick a song: click it, or T or 1-" + this.menu.length + "   Space record your own");
         this.paintMenu(g);
      } else if (this.lesson.finished()) {
         this.header(g, this.lesson.song().title() + ": done!", TEXT,
            this.onTune() ? "R again   Space record a reply   0 free play" : "R again   T next song   0 free play");
         this.paintResult(g);
      } else if (this.listenNote >= 0) {
         this.header(g, "Listening: " + this.lesson.song().title(), TEXT, "Press any piano key to play it yourself");
         this.paintNotes(g, this.lesson.song(), Math.max(0, this.listenNote - 2), this.listenNote, LISTEN);
      } else {
         this.header(g, this.lesson.song().title(), TEXT, "Note " + (this.lesson.position() + 1) + " of " + this.lesson.song().length()
            + "   misses " + this.lesson.misses() + "   L listen   R restart   0 free play");
         this.paintNotes(g, this.lesson.song(), Math.max(0, this.lesson.position() - 2), this.lesson.position(), NEXT);
      }
   }

   /** Whether the lesson is on a recorded tune (which keeps no records) rather than one of the five songs. */
   private boolean onTune() {
      return this.lesson != null && this.lesson.song().id() == null;
   }

   /** The headline (with a red dot while recording), and the help, or the notice in gold, right-aligned. */
   private void header(Graphics2D g, String headline, Color headlineColor, String help) {
      g.setFont(HEADLINE);
      g.setColor(headlineColor);
      int x = MARGIN;
      if (headlineColor == RECORD) {
         g.fillOval(x, 12, 14, 14);
         x += 20;
      }
      g.drawString(headline, x, 24);
      int headlineEnd = x + g.getFontMetrics().stringWidth(headline);
      this.paintMic(g);
      int rightEdge = this.micLeft() - MARGIN;
      String right = this.notice != null ? this.notice : help;
      g.setFont(this.notice != null ? HEADLINE : HELP);
      g.setColor(this.notice != null ? NEXT : SOFT);
      int rightWidth = g.getFontMetrics().stringWidth(right);
      // Right-aligned when there is room, else dropped: the chips below say enough on a narrow window.
      if (headlineEnd + rightWidth + MARGIN <= rightEdge) {
         g.drawString(right, rightEdge - rightWidth, 24);
      }
   }

   /**
    * The microphone button: "Sing along" while off; while on, the note heard and how far off it is (green when
    * within 10 cents), or "Listening..." in the quiet.
    */
   private void paintMic(Graphics2D g) {
      int left = this.micLeft();
      int width = this.micWidth();
      boolean on = this.mic == MIC_ON;
      g.setColor(on ? new Color(70, 40, 46) : CHIP);
      g.fillRoundRect(left, MIC_TOP, width, MIC_HEIGHT, MIC_HEIGHT, MIC_HEIGHT);
      if (on) {
         g.setColor(RECORD);
         g.drawRoundRect(left, MIC_TOP, width - 1, MIC_HEIGHT - 1, MIC_HEIGHT, MIC_HEIGHT);
      }
      // The microphone: a capsule on a cradle and a stem.
      int cx = left + 17;
      g.setColor(on ? RECORD : this.mic == MIC_FAILED ? STAR_OFF : TEXT);
      g.fillRoundRect(cx - 3, MIC_TOP + 4, 7, 10, 7, 7);
      g.drawArc(cx - 6, MIC_TOP + 6, 13, 11, 180, 180);
      g.drawLine(cx, MIC_TOP + 17, cx, MIC_TOP + 20);
      g.drawLine(cx - 3, MIC_TOP + 20, cx + 4, MIC_TOP + 20);
      if (this.mic == MIC_FAILED) {
         g.setColor(RECORD);
         g.drawLine(cx - 7, MIC_TOP + 20, cx + 8, MIC_TOP + 3);
      }
      if (width < 100) {
         return;
      }
      String label;
      Color color = TEXT;
      if (this.mic == MIC_OFF) {
         label = "Sing along";
      } else if (this.mic == MIC_ASKING) {
         label = "Allow mic...";
         color = NEXT;
      } else if (this.mic == MIC_FAILED) {
         label = "No mic";
         color = SOFT;
      } else if (this.micNote < 0) {
         label = "Listening...";
         color = SOFT;
      } else {
         label = Song.noteName(Math.floorMod(this.micNote, 12)) + (this.micNote / 12 - 1)
            + "  " + (this.micCents > 0 ? "+" : "") + this.micCents + "¢";
         color = Math.abs(this.micCents) <= 10 ? IN_TUNE : NEXT;
      }
      g.setFont(CHIP_FONT);
      g.setColor(color);
      g.drawString(label, left + 30, MIC_TOP + 17);
   }

   private void paintLine(Graphics2D g, String line) {
      g.setFont(HELP);
      g.setColor(SOFT);
      g.drawString(line, MARGIN, CHIP_TOP + 26);
   }

   private void paintMenu(Graphics2D g) {
      int chipWidth = this.menuChipWidth();
      int chipHeight = HEIGHT - CHIP_TOP - MARGIN;
      for (int song = 0; song <= this.menu.length; song++) {
         int x = MARGIN + song * chipWidth;
         boolean record = song == this.menu.length;
         g.setColor(CHIP);
         g.fillRoundRect(x + 2, CHIP_TOP, chipWidth - 4, chipHeight, 10, 10);
         g.setFont(CHIP_FONT);
         g.setColor(TEXT);
         // Too narrow for a name: just the number to press.
         String label = record ? (chipWidth < 90 ? "Rec" : "Record")
            : chipWidth < 90 ? String.valueOf(song + 1) : (song + 1) + "  " + this.menu[song].shortTitle();
         FontMetrics metrics = g.getFontMetrics();
         if (metrics.stringWidth(label) > chipWidth - 12) {
            while (label.length() > 4 && metrics.stringWidth(label + "..") > chipWidth - 12) {
               label = label.substring(0, label.length() - 1);
            }
            label = label.trim() + "..";
         }
         g.drawString(label, x + (chipWidth - metrics.stringWidth(label)) / 2, CHIP_TOP + 20);
         if (record) {
            g.setColor(RECORD);
            g.fillOval(x + chipWidth / 2 - 7, CHIP_TOP + 31, 14, 14);
         } else if (this.menu[song].id() == null) {
            g.setFont(HELP);
            g.setColor(SOFT);
            String notes = this.menu[song].length() + " notes";
            g.drawString(notes, x + (chipWidth - g.getFontMetrics().stringWidth(notes)) / 2, CHIP_TOP + 42);
         } else {
            this.paintStars(g, x + chipWidth / 2, CHIP_TOP + 38, 7, this.records.stars(this.menu[song]));
         }
      }
   }

   private static int noteWidth(Song song, int note) {
      return (int)(NOTE_UNIT * Math.max(1, Math.min(2, song.beats(note))));
   }

   /** The note from which the song's last notes fill the strip. */
   private int lastFitting(Song song) {
      int room = this.getWidth() - 2 * MARGIN;
      int first = song.length();
      while (first > 0 && room >= noteWidth(song, first - 1)) {
         first--;
         room -= noteWidth(song, first);
      }
      return first;
   }

   /** The song's notes from {@code first}, as many as fit; note {@code current} in {@code lit}. */
   private void paintNotes(Graphics2D g, Song song, int first, int current, Color lit) {
      int chipHeight = HEIGHT - CHIP_TOP - MARGIN;
      int x = MARGIN;
      for (int note = first; note < song.length(); note++) {
         int width = noteWidth(song, note);
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
      if (this.newBest && !this.onTune()) {
         int lineWidth = g.getFontMetrics().stringWidth(line);
         g.setColor(NEXT);
         g.drawString("New best!", MARGIN + 136 + lineWidth, CHIP_TOP + 22);
      }
      g.setFont(HELP);
      g.setColor(SOFT);
      g.drawString(this.onTune() ? "Now record a reply: press Space."
         : stars == 3 ? "Perfect. Try another song." : "Three stars for no misses. Press R to try again.", MARGIN + 120, CHIP_TOP + 42);
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
