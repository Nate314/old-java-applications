package synthesizer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Draws the 13 keys (C to the high C) with the computer key and the note name on each, shows which keys are held,
 * lights the key a lesson is waiting for, and tints blue the note the microphone hears. Keys are numbered 0 to 12 in note order.
 * During a lesson the top of the panel shows the song as falling notes: a bar per note over its key, as long as the
 * note, coming down to land on the keys. The next note sits on the keys and the song moves down a note at each right press.
 */
public final class PianoPanel extends JPanel {
   public static final int KEYS = 13;
   /** The height of the falling notes over the keys during a lesson (less on a panel under twice as tall). */
   public static final int FALL = 120;
   /** The computer key that plays each key, in note order. */
   public static final String[] LETTERS = {"Z", "S", "X", "D", "C", "V", "G", "B", "H", "N", "J", "M", ","};
   private static final int[] WHITE_KEYS = {0, 2, 4, 5, 7, 9, 11, 12};
   /** Each black key and the white key (by position) it sits right of. */
   private static final int[] BLACK_KEYS = {1, 3, 6, 8, 10};
   private static final int[] BLACK_AFTER = {0, 1, 3, 4, 5};
   private static final Color BACKGROUND = new Color(24, 26, 34);
   private static final Color WHITE = new Color(250, 250, 246);
   private static final Color WHITE_PRESSED = new Color(175, 175, 175);
   private static final Color BLACK = new Color(16, 16, 20);
   private static final Color BLACK_PRESSED = new Color(80, 80, 88);
   private static final Color HINT = new Color(255, 196, 60);
   private static final Color HINT_BLACK = new Color(226, 152, 24);
   private static final Color HEARD = new Color(160, 200, 255);
   private static final Color HEARD_BLACK = new Color(56, 104, 196);
   private static final Color LABEL = new Color(60, 60, 70);
   private static final Color LABEL_SOFT = new Color(140, 140, 150);
   private static final Color LANE_LINE = new Color(44, 48, 62);
   private static final Color BLACK_LANE = new Color(0, 0, 0, 60);
   private static final Color COMING = new Color(110, 150, 240);
   private static final Color COMING_BLACK = new Color(72, 100, 190);
   private static final Color PLAYED = new Color(110, 214, 140);
   private static final Color BAR_TEXT = new Color(16, 18, 28);
   private static final Color LANDING = new Color(255, 196, 60, 130);
   /** How many beats of the song the falling notes show above the keys. */
   private static final double BEATS_SHOWN = 4.5;
   private static final int MISS_MILLIS = 450;

   private final boolean[] pressed = new boolean[KEYS];
   private int hint = -1;
   private int heard = -1;
   /** The song shown falling, or null when there is no lesson (the keys then fill the panel). */
   private Song song;
   /** The note drawn in gold, the one on the keys. */
   private int next;
   /** The scroll, in beats from the song's start: it glides from {@code fromBeat} to {@code toBeat} over {@code glideMillis}. */
   private double fromBeat;
   private double toBeat;
   private long glideStart;
   private int glideMillis;
   /** The key whose lane flashes red for a wrong note, or -1. */
   private int missKey = -1;
   private long missAt;
   private final Timer frames = new Timer(16, event -> this.frame());

   public PianoPanel() {
      this.setFocusable(false);
      this.setBackground(BACKGROUND);
   }

   public void setPressed(int key, boolean down) {
      this.pressed[key] = down;
      this.repaint();
   }

   /** Lights key {@code key} as the one to press next; -1 for none. */
   public void setHint(int key) {
      if (this.hint != key) {
         this.hint = key;
         this.repaint();
      }
   }

   /** Tints the keys of pitch class {@code pitchClass} (0 for C, both Cs) as the note the microphone hears; -1 for none. */
   public void setHeard(int pitchClass) {
      if (this.heard != pitchClass) {
         this.heard = pitchClass;
         this.repaint();
      }
   }

   /** Shows {@code song} falling from its first note, or hides the falling notes for null. */
   public void setSong(Song song) {
      this.song = song;
      this.next = 0;
      this.missKey = -1;
      this.glideMillis = 0;
      this.toBeat = 0;
      this.repaint();
   }

   /** Moves the song down until note {@code note} sits on the keys (past the end for its length), over {@code millis}. */
   public void follow(int note, int millis) {
      if (this.song != null) {
         this.next = note;
         this.glide(this.song.start(note), millis);
      }
   }

   /** Listen: note {@code note} sits on the keys now and the song moves down at tempo while it sounds for {@code millis}. */
   public void play(int note, int millis) {
      if (this.song != null) {
         this.next = note;
         this.toBeat = this.song.start(note);
         this.glide(this.toBeat + this.song.beats(note), millis);
      }
   }

   /** Flashes key {@code key}'s lane red, for a wrong note. */
   public void miss(int key) {
      this.missKey = key;
      this.missAt = System.currentTimeMillis();
      this.frames.start();
   }

   private void glide(double beat, int millis) {
      this.fromBeat = this.shownBeat();
      this.toBeat = beat;
      this.glideStart = System.currentTimeMillis();
      this.glideMillis = millis;
      this.frames.start();
      this.repaint();
   }

   /** The scroll now, in beats. */
   double shownBeat() {
      long elapsed = System.currentTimeMillis() - this.glideStart;
      if (elapsed >= this.glideMillis) {
         return this.toBeat;
      }
      return this.fromBeat + (this.toBeat - this.fromBeat) * elapsed / this.glideMillis;
   }

   private void frame() {
      long now = System.currentTimeMillis();
      if (now - this.glideStart >= this.glideMillis && (this.missKey < 0 || now - this.missAt >= MISS_MILLIS)) {
         this.missKey = -1;
         this.frames.stop();
      }
      this.repaint();
   }

   private boolean heardAt(int key) {
      return this.heard >= 0 && key % 12 == this.heard;
   }

   public static boolean isBlack(int key) {
      for (int black : BLACK_KEYS) {
         if (black == key) {
            return true;
         }
      }
      return false;
   }

   /** Where the keys start: below the falling notes during a lesson, else at the top. */
   int keysTop() {
      return this.song == null ? 0 : Math.min(FALL, this.getHeight() / 2);
   }

   private int whiteWidth() {
      return Math.max(1, (this.getWidth() - 4) / WHITE_KEYS.length);
   }

   private int blackHeight() {
      return (this.getHeight() - this.keysTop()) * 3 / 5;
   }

   private int blackX(int blackIndex) {
      int whiteWidth = this.whiteWidth();
      return 2 + (BLACK_AFTER[blackIndex] + 1) * whiteWidth - whiteWidth * 3 / 10;
   }

   /** The left edge and width of key {@code key}. */
   private int[] keySpan(int key) {
      int whiteWidth = this.whiteWidth();
      for (int black = 0; black < BLACK_KEYS.length; black++) {
         if (BLACK_KEYS[black] == key) {
            return new int[]{this.blackX(black), whiteWidth * 3 / 5};
         }
      }
      for (int white = 0; white < WHITE_KEYS.length; white++) {
         if (WHITE_KEYS[white] == key) {
            return new int[]{2 + white * whiteWidth, whiteWidth};
         }
      }
      throw new IllegalArgumentException("No key " + key);
   }

   /** The key at a point in this panel, or -1 for none (the falling notes are not keys). */
   public int keyAt(int x, int y) {
      int whiteWidth = this.whiteWidth();
      y -= this.keysTop();
      if (y < 0 || y >= this.getHeight() - this.keysTop()) {
         return -1;
      }
      if (y < this.blackHeight()) {
         for (int black = 0; black < BLACK_KEYS.length; black++) {
            int left = this.blackX(black);
            if (x >= left && x < left + whiteWidth * 3 / 5) {
               return BLACK_KEYS[black];
            }
         }
      }
      int white = (x - 2) / whiteWidth;
      return x < 2 || white >= WHITE_KEYS.length ? -1 : WHITE_KEYS[white];
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D)graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int width = this.getWidth();
      int top = this.keysTop();
      int height = this.getHeight() - top;
      int whiteWidth = this.whiteWidth();
      g.setColor(BACKGROUND);
      g.fillRect(0, 0, width, this.getHeight());
      int letterSize = Math.max(12, Math.min(30, whiteWidth / 3));
      Font letterFont = new Font("SansSerif", Font.BOLD, letterSize);
      Font noteFont = new Font("SansSerif", Font.PLAIN, Math.max(10, letterSize * 3 / 5));
      if (this.song != null && top > 0) {
         this.paintFall(g, width, top, new Font("SansSerif", Font.BOLD, Math.max(11, letterSize * 3 / 4)));
      }
      g.translate(0, top);

      for (int white = 0; white < WHITE_KEYS.length; white++) {
         int key = WHITE_KEYS[white];
         int x = 2 + white * whiteWidth;
         g.setColor(this.pressed[key] ? WHITE_PRESSED : this.heardAt(key) ? HEARD : key == this.hint ? HINT : WHITE);
         // Rounded at the bottom only: the top is square where the key meets the panel's edge (or the falling notes).
         g.fillRoundRect(x + 1, 0, whiteWidth - 2, height - 2, 10, 10);
         g.fillRect(x + 1, 0, whiteWidth - 2, 10);
         this.drawLabels(g, key, x, whiteWidth, height - 10, letterFont, noteFont, LABEL, LABEL_SOFT);
      }

      int blackWidth = whiteWidth * 3 / 5;
      int blackHeight = this.blackHeight();
      for (int black = 0; black < BLACK_KEYS.length; black++) {
         int key = BLACK_KEYS[black];
         int x = this.blackX(black);
         g.setColor(this.pressed[key] ? BLACK_PRESSED : this.heardAt(key) ? HEARD_BLACK : key == this.hint ? HINT_BLACK : BLACK);
         g.fillRoundRect(x, 0, blackWidth, blackHeight, 8, 8);
         g.fillRect(x, 0, blackWidth, 8);
         Color text = key == this.hint && !this.heardAt(key) && !this.pressed[key] ? BLACK : WHITE;
         this.drawLabels(g, key, x, blackWidth, blackHeight - 8, letterFont, noteFont, text, text);
      }
   }

   /** The falling notes in the {@code top} pixels above the keys: lanes, then a bar per note, landing on the keys' top edge. */
   private void paintFall(Graphics2D g, int width, int top, Font font) {
      // Darker towards the top, in bands (the browser's shim has no gradient paint).
      for (int band = 0; band < 8; band++) {
         g.setColor(new Color(0, 0, 0, 90 - band * 11));
         g.fillRect(0, top * band / 8, width, top * (band + 1) / 8 - top * band / 8);
      }
      int whiteWidth = this.whiteWidth();
      g.setColor(LANE_LINE);
      for (int white = 0; white <= WHITE_KEYS.length; white++) {
         g.drawLine(2 + white * whiteWidth, 0, 2 + white * whiteWidth, top);
      }
      g.setColor(BLACK_LANE);
      for (int key : BLACK_KEYS) {
         int[] span = this.keySpan(key);
         g.fillRect(span[0], 0, span[1], top);
      }
      long sinceMiss = System.currentTimeMillis() - this.missAt;
      if (this.missKey >= 0 && sinceMiss < MISS_MILLIS) {
         int[] span = this.keySpan(this.missKey);
         int strength = (int)(170 * (MISS_MILLIS - sinceMiss) / MISS_MILLIS);
         for (int band = 0; band < 8; band++) {
            g.setColor(new Color(232, 72, 72, strength * (band + 1) / 8));
            g.fillRect(span[0], top * band / 8, span[1], top * (band + 1) / 8 - top * band / 8);
         }
      }

      double beat = this.shownBeat();
      double perBeat = top / BEATS_SHOWN;
      g.setFont(font);
      FontMetrics metrics = g.getFontMetrics();
      for (int note = 0; note < this.song.length(); note++) {
         double start = this.song.start(note);
         // Bars sink behind the keys once played, so they stop at the keys' edge.
         int bottom = Math.min(top, (int)Math.round(top - (start - beat) * perBeat));
         // A gap at each bar's top keeps repeated notes on one key apart.
         int barTop = (int)Math.round(top - (start + this.song.beats(note) - beat) * perBeat) + 3;
         if (barTop >= bottom || bottom <= 0) {
            continue;
         }
         int key = this.song.key(note);
         boolean black = isBlack(key);
         int[] span = this.keySpan(key);
         int inset = black ? 2 : Math.max(3, span[1] / 8);
         g.setColor(note < this.next ? PLAYED : note == this.next ? (black ? HINT_BLACK : HINT) : black ? COMING_BLACK : COMING);
         g.fillRoundRect(span[0] + inset, barTop, span[1] - 2 * inset, bottom - barTop, 10, 10);
         if (bottom - barTop >= metrics.getAscent() + 6) {
            g.setColor(BAR_TEXT);
            g.drawString(LETTERS[key], span[0] + (span[1] - metrics.stringWidth(LETTERS[key])) / 2, bottom - 5);
         }
      }
      // The line the notes land on, in the gold of the next key.
      g.setColor(LANDING);
      g.fillRect(0, top - 3, width, 3);
   }

   /** The computer key's letter at the bottom of a key, with the note name above it. */
   private void drawLabels(Graphics2D g, int key, int x, int keyWidth, int baseline, Font letterFont, Font noteFont, Color letter, Color note) {
      g.setFont(letterFont);
      FontMetrics metrics = g.getFontMetrics();
      g.setColor(letter);
      g.drawString(LETTERS[key], x + (keyWidth - metrics.stringWidth(LETTERS[key])) / 2, baseline);
      int letterHeight = metrics.getAscent();
      g.setFont(noteFont);
      metrics = g.getFontMetrics();
      String name = Song.noteName(key);
      g.setColor(note);
      g.drawString(name, x + (keyWidth - metrics.stringWidth(name)) / 2, baseline - letterHeight - 4);
   }
}
