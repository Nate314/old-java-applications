package synthesizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * A tune recorded on the keyboard, sent to a friend as a short code: two characters per note, the key as a letter by
 * its place on the keyboard ({@code a} for C, {@code b} for C#, up to {@code m} for the high C) and the length in half
 * beats as a digit 1-8, so {@code e2c2a4} is E D C with a long C. On the web the code rides in a link to the keyboard's
 * page ({@code ?tune=e2c2a4}); the desktop app has no page, so it sends the code alone, which it takes as its argument.
 * The last tune recorded or received is kept in {@code ~/.synthesizer-tune.txt}.
 */
final class Tune {
   static final int MAX_NOTES = 64;
   static final String MINE = "Your tune";
   static final String FRIENDS = "A friend's tune";
   private static final String PARAM = "tune=";
   /** A beat is BEAT_MILLIS long in Listen, so a recorded tune plays back at about the speed it was played. */
   private static final int BEAT_MILLIS = 420;

   private Tune() {
   }

   static String encode(Song song) {
      StringBuilder code = new StringBuilder();
      for (int note = 0; note < song.length(); note++) {
         code.append((char)('a' + song.key(note))).append((char)('0' + (int)Math.round(song.beats(note) * 2)));
      }
      return code.toString();
   }

   /** The tune in a code, titled {@code title}; null if it is not one (or has fewer than two notes). */
   static Song decode(String code, String title) {
      if (code == null || code.length() % 2 != 0 || code.length() < 4 || code.length() > 2 * MAX_NOTES) {
         return null;
      }
      int[] keys = new int[code.length() / 2];
      double[] beats = new double[keys.length];
      for (int note = 0; note < keys.length; note++) {
         keys[note] = code.charAt(2 * note) - 'a';
         int halves = code.charAt(2 * note + 1) - '0';
         if (keys[note] < 0 || keys[note] >= PianoPanel.KEYS || halves < 1 || halves > 8) {
            return null;
         }
         beats[note] = halves / 2.0;
      }
      return new Song(title, keys, beats);
   }

   /** The code in a pasted link (the value of tune=) or code (the text itself), without spaces and in lower case. */
   static String codeIn(String text) {
      int at = text.indexOf(PARAM);
      String code = text;
      if (at >= 0) {
         code = text.substring(at + PARAM.length());
         int end = indexOfAny(code, "&#");
         code = end < 0 ? code : code.substring(0, end);
      }
      return code.replaceAll("\\s", "").toLowerCase();
   }

   /** The address of the page a link points at (without its ? query or # part), or null if the text is not a web address. */
   static String pageOf(String text) {
      if (!text.startsWith("http://") && !text.startsWith("https://")) {
         return null;
      }
      int end = indexOfAny(text, "?#");
      return end < 0 ? text : text.substring(0, end);
   }

   /** What to send the friend: a link to the page with the tune in it, or the code alone where there is no page. */
   static String message(String page, Song song) {
      return page == null ? encode(song) : page + "?" + PARAM + encode(song);
   }

   /** The tune kept in {@code file} ({@code MINE=code} or {@code FRIEND=code}), or null if there is none. */
   static Song load(File file) {
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
         String line = reader.readLine();
         int split = line == null ? -1 : line.indexOf('=');
         if (split > 0) {
            String kind = line.substring(0, split).trim();
            return decode(line.substring(split + 1).trim(), kind.equals("FRIEND") ? FRIENDS : MINE);
         }
      } catch (IOException e) {
         // No tune yet.
      }
      return null;
   }

   static void save(File file, Song song) {
      try (Writer writer = new FileWriter(file)) {
         writer.write((FRIENDS.equals(song.title()) ? "FRIEND" : "MINE") + "=" + encode(song) + "\n");
      } catch (IOException e) {
         // Could not save: the tune lasts until the window closes.
      }
   }

   private static int indexOfAny(String text, String chars) {
      for (int i = 0; i < text.length(); i++) {
         if (chars.indexOf(text.charAt(i)) >= 0) {
            return i;
         }
      }
      return -1;
   }

   /**
    * Records key presses with their times. A note lasts until the next one starts, rounded to half beats (half a
    * beat to four beats); the last note gets two beats.
    */
   static final class Recorder {
      private final List<long[]> notes = new ArrayList<>();

      /** Records a press of key {@code key} at {@code millis}; false once the tune is full. */
      boolean press(int key, long millis) {
         if (this.full()) {
            return false;
         }
         this.notes.add(new long[]{key, millis});
         return true;
      }

      int length() {
         return this.notes.size();
      }

      boolean full() {
         return this.notes.size() >= MAX_NOTES;
      }

      /** The notes so far as a song titled {@code title}; null before the first note. */
      Song song(String title) {
         if (this.notes.isEmpty()) {
            return null;
         }
         int[] keys = new int[this.notes.size()];
         double[] beats = new double[keys.length];
         for (int note = 0; note < keys.length; note++) {
            keys[note] = (int)this.notes.get(note)[0];
            if (note + 1 < keys.length) {
               long gap = this.notes.get(note + 1)[1] - this.notes.get(note)[1];
               beats[note] = Math.max(1, Math.min(8, Math.round(gap * 2.0 / BEAT_MILLIS))) / 2.0;
            } else {
               beats[note] = 2;
            }
         }
         return new Song(title, keys, beats);
      }
   }
}
