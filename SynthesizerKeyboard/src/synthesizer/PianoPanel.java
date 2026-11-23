package synthesizer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Draws the 13 keys (C to the high C) with the computer key and the note name on each, shows which keys are held,
 * lights the key a lesson is waiting for, and tints blue the note the microphone hears. Keys are numbered 0 to 12 in note order.
 * During a lesson the top of the panel shows the song as falling notes: a bar per note over its key, as long as the
 * note, coming down to land on the keys. The next note sits on the keys and the song moves down a note at each right press.
 * The backing's chord names ride down with the notes, each on a line where its chord starts, and a pill at the top
 * right shows the backing (and turns it on or off when clicked). Outside a lesson the same space shows the notes you play
 * rising from their keys, each as long as it was held, gold once the keyboard knows the song (none in the looper).
 */
public final class PianoPanel extends JPanel {
   public static final int KEYS = 13;
   /** The height of the falling (in a lesson) or rising (free play) notes over the keys (less on a panel under twice as tall). */
   public static final int FALL = 120;
   /** The computer key that plays each key, in note order. */
   public static final String[] LETTERS = {"Z", "S", "X", "D", "C", "V", "G", "B", "H", "N", "J", "M", ","};
   private static final int[] WHITE_KEYS = {0, 2, 4, 5, 7, 9, 11, 12};
   /** Each black key and the white key (by position) it sits right of. */
   private static final int[] BLACK_KEYS = {1, 3, 6, 8, 10};
   private static final int[] BLACK_AFTER = {0, 1, 3, 4, 5};
   private static final Color BACKGROUND = new Color(24, 26, 34);
   private static final Color WHITE = new Color(250, 250, 246);
   private static final Color WHITE_PRESSED = new Color(214, 210, 198);
   private static final Color BLACK = new Color(16, 16, 20);
   private static final Color BLACK_FACE = new Color(44, 44, 54);
   private static final Color BLACK_PRESSED = new Color(60, 60, 74);
   private static final Color SHADOW = new Color(0, 0, 0, 60);
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
   private static final Color CHORD_LINE = new Color(200, 170, 255, 70);
   private static final Color CHORD_TEXT = new Color(214, 192, 255);
   private static final Color PILL = new Color(40, 34, 64, 220);
   private static final Color PILL_OFF = new Color(40, 42, 54, 220);
   private static final Font CHORD_FONT = new Font("SansSerif", Font.BOLD, 12);
   private static final int PILL_HEIGHT = 20;
   /** A beat in milliseconds: notes rise at the falling notes' scale, BEATS_SHOWN beats to the space. */
   private static final double BEAT_MILLIS = 420;
   /** Under reduced motion a played note stays still on its key for this long, then goes. */
   private static final int STILL_MILLIS = 1500;
   private static final Color RISE_TEXT = new Color(120, 128, 150);

   private final boolean[] pressed = new boolean[KEYS];
   private int hint = -1;
   private int heard = -1;
   /** Whether the keys are labelled with their chords (chord keys on) rather than their notes. */
   private boolean chords;
   /** The octave number of the low C (4 for middle C), shown in the note names. */
   private int octave = 4;
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
   /** The song's backing chords (see Backing), whether the backing is on, resting for the microphone, and the chord sounding (-1 for none). */
   private int[] chordKeys;
   private boolean backing = true;
   private boolean resting;
   private int backingChord = -1;
   private final Timer frames = new Timer(16, event -> this.frame());
   /** Whether notes played outside a lesson rise over the keys (off in the looper, whose lanes show them). */
   private boolean rising = true;
   /** The notes rising: {key, pressed at, released at (0 while held)}, in milliseconds. */
   private final List<long[]> trail = new ArrayList<>();
   /** Whether the notes played are the start of a song the keyboard knows (they rise in gold). */
   private boolean found;

   public PianoPanel() {
      this.setFocusable(false);
      this.setBackground(BACKGROUND);
   }

   public void setPressed(int key, boolean down) {
      this.pressed[key] = down;
      if (this.song == null && this.rising) {
         long now = System.currentTimeMillis();
         for (long[] note : this.trail) {
            if (note[0] == key && note[2] == 0) {
               note[2] = now;
            }
         }
         if (down) {
            this.trail.add(new long[]{key, now, 0});
         }
         this.frames.start();
      }
      this.repaint();
   }

   /** Whether notes played outside a lesson rise over the keys; off, the keys fill the panel. */
   public void setRising(boolean on) {
      this.rising = on;
      this.trail.clear();
      this.repaint();
   }

   /** Whether the notes played so far are the start of a known song (see SongFinder). */
   public void setFound(boolean found) {
      if (this.found != found) {
         this.found = found;
         this.repaint();
      }
   }

   /** Lights key {@code key} as the one to press next; -1 for none. */
   public void setHint(int key) {
      if (this.hint != key) {
         this.hint = key;
         this.repaint();
      }
   }

   public void setOctave(int octave) {
      this.octave = octave;
      this.repaint();
   }

   public void setChords(boolean on) {
      this.chords = on;
      this.repaint();
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
      this.trail.clear();
      this.chordKeys = song == null ? null : Backing.chords(song);
      this.next = 0;
      this.missKey = -1;
      this.glideMillis = 0;
      this.toBeat = 0;
      this.repaint();
   }

   public void setBacking(boolean on, boolean resting, int chord) {
      this.backing = on;
      this.resting = resting;
      this.backingChord = chord;
      this.repaint();
   }

   /** The backing pill's text, sized to the panel. */
   private String pillText() {
      String state = !this.backing ? "off" : this.resting ? "rests (mic)" : this.backingChord < 0 ? "on" : Chord.name(this.backingChord);
      return (this.getWidth() < 420 ? "" : "A  ") + "Backing " + state;
   }

   /** The backing pill's left edge and width, measured in its font. */
   private int[] pillSpan() {
      int width = this.getFontMetrics(CHORD_FONT).stringWidth(this.pillText()) + 20;
      return new int[]{this.getWidth() - 6 - width, width};
   }

   /** Whether a point is on the backing pill, shown over the falling notes during a lesson. */
   public boolean backingAt(int x, int y) {
      if (this.song == null || this.keysTop() < PILL_HEIGHT + 12) {
         return false;
      }
      int[] span = this.pillSpan();
      return y >= 6 && y < 6 + PILL_HEIGHT && x >= span[0] && x < span[0] + span[1];
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
      this.trail.removeIf(note -> note[2] > 0 && this.risen(note, now) >= this.keysTop());
      if (now - this.glideStart >= this.glideMillis && (this.missKey < 0 || now - this.missAt >= MISS_MILLIS) && this.trail.isEmpty()) {
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

   /** Where the keys start: below the falling or rising notes, else (in the looper) at the top. */
   int keysTop() {
      return this.song == null && !this.rising ? 0 : Math.min(FALL, this.getHeight() / 2);
   }

   private static boolean still() {
      return Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
   }

   /** How far above the keys a note's bottom has risen by {@code now}, in pixels (0 while held). */
   int risen(long[] note, long now) {
      if (note[2] == 0) {
         return 0;
      }
      if (still()) {
         // Reduced motion: the note waits on its key, then goes all at once.
         return now - note[2] < STILL_MILLIS ? 0 : this.keysTop();
      }
      return (int)Math.round((now - note[2]) * this.perMilli());
   }

   /** How tall a note's bar is by {@code now}: as long as it was (or has been) held, at least a sixth of a beat. */
   int barHeight(long[] note, long now) {
      long held = (note[2] == 0 ? now : note[2]) - note[1];
      return (int)Math.round(Math.max(held, BEAT_MILLIS / 6) * this.perMilli());
   }

   private double perMilli() {
      return this.keysTop() / BEATS_SHOWN / BEAT_MILLIS;
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
      Font barFont = new Font("SansSerif", Font.BOLD, Math.max(11, letterSize * 3 / 4));
      if (this.song != null && top > 0) {
         this.paintFall(g, width, top, barFont);
      } else if (top > 0) {
         this.paintRise(g, width, top, barFont);
      }
      g.translate(0, top);

      // Keys have depth: a front edge (lip) under the face that a press pushes down, and a shadow at the top where they meet the panel.
      int lip = lip(height);
      for (int white = 0; white < WHITE_KEYS.length; white++) {
         int key = WHITE_KEYS[white];
         int x = 2 + white * whiteWidth;
         boolean down = this.pressed[key];
         Color face = down ? WHITE_PRESSED : this.heardAt(key) ? HEARD : key == this.hint ? HINT : WHITE;
         int front = down ? lip / 3 : lip;
         g.setColor(shade(face, 0.72));
         g.fillRoundRect(x + 1, 0, whiteWidth - 2, height - 2, 10, 10);
         g.fillRect(x + 1, 0, whiteWidth - 2, 10);
         g.setColor(face);
         g.fillRoundRect(x + 1, 0, whiteWidth - 2, height - 2 - front, 8, 8);
         g.fillRect(x + 1, 0, whiteWidth - 2, 10);
         this.paintTopShadow(g, x + 1, whiteWidth - 2, down ? 14 : 5);
         this.drawLabels(g, key, x, whiteWidth, height - 8 - front, letterFont, noteFont, LABEL, LABEL_SOFT);
      }

      int blackWidth = whiteWidth * 3 / 5;
      int blackHeight = this.blackHeight();
      int blackLip = Math.max(3, blackHeight / 10);
      for (int black = 0; black < BLACK_KEYS.length; black++) {
         // Each black key casts a soft shadow down and right onto the white keys, shorter while pressed.
         int x = this.blackX(black);
         int reach = this.pressed[BLACK_KEYS[black]] ? 2 : 5;
         g.setColor(SHADOW);
         g.fillRoundRect(x + 1, 0, blackWidth + reach, blackHeight + reach, 10, 10);
      }
      for (int black = 0; black < BLACK_KEYS.length; black++) {
         int key = BLACK_KEYS[black];
         int x = this.blackX(black);
         boolean down = this.pressed[key];
         Color face = down ? BLACK_PRESSED : this.heardAt(key) ? HEARD_BLACK : key == this.hint ? HINT_BLACK : BLACK_FACE;
         // The body shows as a darker rim and, below the face, the sloped front; a press shrinks the front.
         int front = down ? blackLip / 3 : blackLip;
         g.setColor(shade(face, 0.45));
         g.fillRoundRect(x, 0, blackWidth, blackHeight, 8, 8);
         g.fillRect(x, 0, blackWidth, 8);
         g.setColor(face);
         g.fillRoundRect(x + 2, 0, blackWidth - 4, blackHeight - front - 2, 6, 6);
         g.fillRect(x + 2, 0, blackWidth - 4, 6);
         this.paintTopShadow(g, x + 2, blackWidth - 4, down ? 10 : 4);
         Color text = key == this.hint && !this.heardAt(key) && !down ? BLACK : WHITE;
         this.drawLabels(g, key, x, blackWidth, blackHeight - front - 8, letterFont, noteFont, text, text);
      }
   }

   /** The front edge of a white key {@code height} pixels tall: what a press pushes down. */
   static int lip(int height) {
      return Math.max(4, height / 18);
   }

   /** {@code color} with its red, green and blue scaled by {@code factor}. */
   static Color shade(Color color, double factor) {
      return new Color((int)(color.getRed() * factor), (int)(color.getGreen() * factor), (int)(color.getBlue() * factor));
   }

   /** A shadow fading down {@code depth} pixels from the top of a key, deeper while it is pressed (in bands: the shim has no gradient paint). */
   private void paintTopShadow(Graphics2D g, int x, int width, int depth) {
      for (int band = 0; band < depth; band++) {
         g.setColor(new Color(0, 0, 0, 70 * (depth - band) / depth));
         g.fillRect(x, band, width, 1);
      }
   }

   /** The falling notes in the {@code top} pixels above the keys: lanes, then a bar per note, landing on the keys' top edge. */
   private void paintFall(Graphics2D g, int width, int top, Font font) {
      this.paintLanes(g, width, top);
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
      this.paintChords(g, width, top, beat, perBeat);
      // The line the notes land on, in the gold of the next key.
      g.setColor(LANDING);
      g.fillRect(0, top - 3, width, 3);
   }

   /** The space over the keys, darker towards the top (in bands: the shim has no gradient paint), with a lane per key. */
   private void paintLanes(Graphics2D g, int width, int top) {
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
   }

   /** Outside a lesson: the notes played rise out of their keys, a bar each as long as it was held; a line invites the first. */
   private void paintRise(Graphics2D g, int width, int top, Font font) {
      this.paintLanes(g, width, top);
      long now = System.currentTimeMillis();
      g.setFont(font);
      FontMetrics metrics = g.getFontMetrics();
      if (this.trail.isEmpty()) {
         // The longest invite that fits: sliding across the keys plays each one (a glissando).
         String invite = "Play a key, or slide across them: your notes rise here";
         if (metrics.stringWidth(invite) > width - 12) {
            invite = "Play or slide across the keys";
         }
         g.setColor(RISE_TEXT);
         g.drawString(invite, (width - metrics.stringWidth(invite)) / 2, (top + metrics.getAscent()) / 2);
      }
      // The line the notes leave from, under them.
      g.setColor(this.found ? LANDING : LANE_LINE);
      g.fillRect(0, top - 3, width, 3);
      for (long[] note : this.trail) {
         int key = (int)note[0];
         boolean black = isBlack(key);
         int[] span = this.keySpan(key);
         int inset = black ? 2 : Math.max(3, span[1] / 8);
         int bottom = top - this.risen(note, now);
         int barTop = Math.max(0, bottom - this.barHeight(note, now));
         if (bottom <= 0) {
            continue;
         }
         g.setColor(this.found ? (black ? HINT_BLACK : HINT) : black ? COMING_BLACK : COMING);
         g.fillRoundRect(span[0] + inset, barTop, span[1] - 2 * inset, bottom - barTop, 10, 10);
         String name = Song.noteName(key);
         if (bottom - barTop >= metrics.getAscent() + 6 && metrics.stringWidth(name) <= span[1] - 2 * inset) {
            g.setColor(BAR_TEXT);
            g.drawString(name, span[0] + (span[1] - metrics.stringWidth(name)) / 2, barTop + metrics.getAscent() + 3);
         }
      }
   }

   /** The backing's chord names, each on a faint line where its chord starts, and the backing pill on top. */
   private void paintChords(Graphics2D g, int width, int top, double beat, double perBeat) {
      g.setFont(CHORD_FONT);
      FontMetrics metrics = g.getFontMetrics();
      for (int note = 0; note < this.song.length(); note++) {
         if (this.chordKeys[note] < 0) {
            continue;
         }
         int y = (int)Math.round(top - (this.song.start(note) - beat) * perBeat);
         if (y < metrics.getAscent() || y > top) {
            continue;
         }
         g.setColor(CHORD_LINE);
         g.fillRect(0, y - 1, width, 1);
         g.setColor(this.backing ? CHORD_TEXT : LABEL_SOFT);
         g.drawString(Chord.name(this.chordKeys[note]), 6, y - 4);
      }
      if (top < PILL_HEIGHT + 12) {
         return;
      }
      int[] span = this.pillSpan();
      g.setColor(this.backing ? PILL : PILL_OFF);
      g.fillRoundRect(span[0], 6, span[1], PILL_HEIGHT, PILL_HEIGHT, PILL_HEIGHT);
      g.setColor(this.backing && !this.resting ? CHORD_TEXT : LABEL_SOFT);
      g.drawRoundRect(span[0], 6, span[1] - 1, PILL_HEIGHT - 1, PILL_HEIGHT, PILL_HEIGHT);
      g.drawString(this.pillText(), span[0] + 10, 6 + (PILL_HEIGHT + metrics.getAscent()) / 2 - 2);
   }

   /** The computer key's letter at the bottom of a key, with the note name (or chord name) above it. */
   private void drawLabels(Graphics2D g, int key, int x, int keyWidth, int baseline, Font letterFont, Font noteFont, Color letter, Color note) {
      g.setFont(letterFont);
      FontMetrics metrics = g.getFontMetrics();
      g.setColor(letter);
      g.drawString(LETTERS[key], x + (keyWidth - metrics.stringWidth(LETTERS[key])) / 2, baseline);
      int letterHeight = metrics.getAscent();
      g.setFont(noteFont);
      metrics = g.getFontMetrics();
      String name = this.chords ? Chord.name(key) : Song.noteName(key) + (this.octave + key / 12);
      g.setColor(note);
      g.drawString(name, x + (keyWidth - metrics.stringWidth(name)) / 2, baseline - letterHeight - 4);
   }
}
