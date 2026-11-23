package audiobibledownloader;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/**
 * Where an English word may be broken at the end of a justified line, by syllable rules (between doubled or paired
 * consonants, before a consonant and "le", never inside th, sh, ch, ph, wh, gh, ck or ng), so narrow columns need no
 * wide word gaps. Cautious on purpose: no break leaves fewer than 2 letters before it or 3 after, inflections (-ed,
 * -es, -eth, -est, -s) are never split off, and names in capitals or words with digits or apostrophes are kept whole.
 * A word that has a dash in it breaks only after the dash.
 */
final class Hyphen {
   private static final String DIGRAPHS = " th sh ch ph wh gh ck ng dg ";
   private static final String GLIDES = " bl br cl cr dr fl fr gl gr pl pr tr ";
   private static final String ONSETS = GLIDES + "sc sk sl sm sn sp st sw tw wr ";
   private static final String[] ENDINGS = {"eth", "est", "ed", "es", "s"};

   private Hyphen() {
   }

   /** Indexes into the word where it may break (the head is word.substring(0, i)), in order. */
   static List<Integer> breaks(String word) {
      List<Integer> at = new ArrayList<>();
      for (int i = 1; i < word.length() - 1; i++) {
         char c = word.charAt(i - 1);
         if (dash(c) && Character.isLetter(word.charAt(i))) {
            at.add(i);
         }
      }
      if (!at.isEmpty() || word.indexOf('-') >= 0) {
         return at;
      }
      int from = 0;
      while (from < word.length() && !Character.isLetter(word.charAt(from))) {
         from++;
      }
      int to = word.length();
      while (to > from && !Character.isLetter(word.charAt(to - 1))) {
         to--;
      }
      String core = word.substring(from, to);
      if (core.length() < 5 || !core.substring(1).equals(core.substring(1).toLowerCase())) {
         return at;
      }
      String w = core.toLowerCase();
      for (int i = 0; i < w.length(); i++) {
         if (w.charAt(i) < 'a' || w.charAt(i) > 'z') {
            return at;
         }
      }
      int stem = w.length();
      for (String e : ENDINGS) {
         if (w.endsWith(e) && !w.endsWith("ss")) {
            stem -= e.length();
            break;
         }
      }
      String t = w.substring(0, stem);
      int s = 0;
      while (s < stem && !vowel(t, s)) {
         s++;
      }
      // Each run of consonants between two vowels of the stem.
      while (s < stem) {
         int c0 = s;
         while (c0 < stem && vowel(t, c0)) {
            c0++;
         }
         int c1 = c0;
         while (c1 < stem && !vowel(t, c1)) {
            c1++;
         }
         if (c1 >= stem) {
            break;
         }
         int cut = cut(t, c0, c1);
         if (cut >= 2 && w.length() - cut >= 3 && !w.startsWith("gh", cut - 2)) {
            at.add(from + cut);
         }
         s = c1;
      }
      return at;
   }

   /** Where a run of consonants w[c0, c1) between vowels splits, or -1. */
   private static int cut(String w, int c0, int c1) {
      int n = c1 - c0;
      if (n < 2) {
         return -1;
      }
      String last = w.substring(c1 - 2, c1);
      if (c1 == w.length() - 1 && w.endsWith("le")) {
         // A consonant and "le" make the last syllable: peo-ple, can-dle.
         return joined(w, c1 - 3) ? -1 : c1 - 2;
      }
      if (n == 2) {
         // A consonant and l or r start the next syllable together (be-tray, sa-cred); other pairs split (sis-ter).
         return has(DIGRAPHS, last) ? -1 : has(GLIDES, last) ? c0 : c0 + 1;
      }
      if (has(ONSETS, last) || has(DIGRAPHS, last) && !last.equals("ng")) {
         return joined(w, c1 - 3) ? -1 : c1 - 2;
      }
      return joined(w, c1 - 2) ? -1 : c1 - 1;
   }

   private static boolean has(String list, String two) {
      return list.contains(" " + two + " ");
   }

   /** Whether w[i, i+2) sound as one, so no break goes between them (n and g may part: an-gry, king-dom). */
   private static boolean joined(String w, int i) {
      return i >= 0 && i + 2 <= w.length() && has(DIGRAPHS, w.substring(i, i + 2)) && !w.startsWith("ng", i);
   }

   /** A hyphen, en dash or em dash. */
   private static boolean dash(char c) {
      return c == '-' || c == '\u2013' || c == '\u2014';
   }

   private static boolean vowel(String w, int i) {
      char c = w.charAt(i);
      // y after a consonant is a vowel (ver-y); w after a vowel is part of it (know-ledge stays whole).
      return "aeiou".indexOf(c) >= 0 || c == 'y' && i > 0 && "aeiou".indexOf(w.charAt(i - 1)) < 0
         || c == 'w' && i > 0 && "aeiou".indexOf(w.charAt(i - 1)) >= 0;
   }

   /**
    * The longest head of the word (with a hyphen added, unless it ends in a dash) that fits in {@code room} pixels,
    * and the rest; null when no break fits.
    */
   static String[] split(String word, FontMetrics fm, int room) {
      List<Integer> at = breaks(word);
      for (int k = at.size() - 1; k >= 0; k--) {
         int i = at.get(k);
         String head = dash(word.charAt(i - 1)) ? word.substring(0, i) : word.substring(0, i) + "-";
         if (fm.stringWidth(head) <= room) {
            return new String[]{head, word.substring(i)};
         }
      }
      return null;
   }
}
