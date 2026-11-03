package audiobibledownloader;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Verses learned by heart. Each verse is kept with its text, so it can be practiced without fetching, and moves
 * through boxes as it is recalled: box 0 while it is being learned, then box 1 (review tomorrow), box 2 (two days)
 * and so on, each gap twice the last. Recalling it from memory on a review day moves it up a box; slipping sends it
 * back to box 1, so it comes back tomorrow.
 */
public final class Memory {
   /** Days from a passed review to the next one, by the box the verse moves into (box 1 is GAPS[0]). */
   public static final int[] GAPS = {1, 2, 4, 8, 16, 32, 64};

   /** One verse being learned. */
   public static final class Card {
      public final int book;
      public final int chapter;
      public final int verse;
      public final String translation;
      public final String text;
      /** 0 while learning; 1 to GAPS.length once recalled from memory. */
      public int box;
      /** The day of the next review (days since 1970-01-01, local time). */
      public int due;

      public Card(int book, int chapter, int verse, String translation, String text, int box, int due) {
         this.book = book;
         this.chapter = chapter;
         this.verse = verse;
         this.translation = translation;
         this.text = text.replaceAll("\\s+", " ").trim();
         this.box = box;
         this.due = due;
      }

      public String ref() {
         return Shelf.verseRef(this.book, this.chapter, this.verse);
      }

      /** "John 3:16". */
      public String name() {
         return Books.NAMES[this.book] + " " + this.chapter + ":" + this.verse;
      }

      /** Whether a From memory run today counts for the schedule: still learning, or its review day has come. */
      public boolean isDue(int today) {
         return this.box == 0 || this.due <= today;
      }

      /** Where it stands, as the list shows it: "Learning", "Review today", "Next review tomorrow"... */
      public String status(int today) {
         if (this.box == 0) {
            return "Learning";
         }
         if (this.due <= today) {
            return today - this.due >= 1 ? "Review due since " + Plan.shortDate(this.due) : "Review today";
         }
         int days = this.due - today;
         return "Next review " + (days == 1 ? "tomorrow" : "in " + days + " days");
      }

      /** The saved form after {@code learn.JHN.3.16=}: translation, box, review day and text. */
      String line() {
         return this.translation + "|" + this.box + "|" + Plan.iso(this.due) + "|" + this.text;
      }

      /** Reads a saved card; throws on a damaged line. */
      static Card parse(String ref, String value) {
         String[] r = ref.split("\\.");
         String[] p = value.split("\\|", 4);
         int b = Books.find(r[0]);
         int c = Integer.parseInt(r[1]);
         int v = Integer.parseInt(r[2]);
         int box = Integer.parseInt(p[1]);
         if (r.length != 3 || b < 0 || c < 1 || c > Books.CHAPTERS[b] || v < 1 || box < 0 || box > GAPS.length
               || BibleApi.translationIndex(p[0]) < 0 || p[3].trim().isEmpty()) {
            throw new IllegalArgumentException("bad verse " + ref);
         }
         return new Card(b, c, v, p[0], p[3], box, Plan.parseIso(p[2]));
      }

      long key() {
         return (long) Books.ordinal(this.book, this.chapter) * 1000 + this.verse;
      }
   }

   private final TreeMap<Long, Card> cards = new TreeMap<>();

   /** Every verse, in Bible order. */
   public List<Card> cards() {
      return new ArrayList<>(this.cards.values());
   }

   public int size() {
      return this.cards.size();
   }

   public Card find(int book, int chapter, int verse) {
      return this.cards.get((long) Books.ordinal(book, chapter) * 1000 + verse);
   }

   public void put(Card card) {
      this.cards.put(card.key(), card);
   }

   public void remove(Card card) {
      this.cards.remove(card.key());
   }

   /** How many are due today (learning ones included). */
   public int dueCount(int today) {
      int n = 0;
      for (Card card : this.cards.values()) {
         n += card.isDue(today) ? 1 : 0;
      }
      return n;
   }

   /** How many have been recalled from memory at least once. */
   public int learnedCount() {
      int n = 0;
      for (Card card : this.cards.values()) {
         n += card.box > 0 ? 1 : 0;
      }
      return n;
   }

   /** The first verse due after the given one, in Bible order and wrapping round; null when none is due. */
   public Card nextDue(Card after, int today) {
      Card first = null;
      for (Card card : this.cards.values()) {
         if (card != after && card.isDue(today)) {
            if (after != null && card.key() > after.key()) {
               return card;
            }
            if (first == null) {
               first = card;
            }
         }
      }
      return first;
   }

   /**
    * Moves a verse on after a From memory run. Only a run on a day it is due counts: passing moves it up a box and sets
    * its next review GAPS later; slipping on a review sends it back to box 1 (tomorrow), and slipping while learning
    * keeps it learning. Returns whether the schedule changed.
    */
   public static boolean grade(Card card, boolean passed, int today) {
      if (!card.isDue(today)) {
         return false;
      }
      if (passed) {
         card.box = Math.min(GAPS.length, card.box + 1);
      } else if (card.box > 0) {
         card.box = 1;
      }
      card.due = card.box == 0 ? today : today + GAPS[card.box - 1];
      return true;
   }

   // ---- Practice ----

   /** One run through a verse at one stage: the words, which are asked, and how the typing has gone. */
   public static final class Drill {
      public static final int READ = 0;
      public static final int HALF = 1;
      public static final int LETTERS = 2;
      public static final int MEMORY = 3;
      public static final String[] STAGES = {"Read it", "Every other word", "First letters", "From memory"};

      public static final int IGNORED = 0;
      public static final int RIGHT = 1;
      public static final int WRONG = 2;
      /** Wrong twice, so the word was shown and counts as a slip. */
      public static final int GIVEN = 3;

      public final Card card;
      public final int stage;
      public final String[] words;
      /** Each word's first letter or digit, lower case; 0 for a word with none (a dash), which is never asked. */
      private final char[] keys;
      private final boolean[] asked;
      private final boolean[] shown;
      private final boolean[] slipped;
      private int at;
      private int wrongHere;
      private int slips;
      private int askedCount;

      public Drill(Card card, int stage) {
         this.card = card;
         this.stage = stage;
         this.words = card.text.split(" ");
         int n = this.words.length;
         this.keys = new char[n];
         this.asked = new boolean[n];
         this.shown = new boolean[n];
         this.slipped = new boolean[n];
         int askable = 0;
         for (int i = 0; i < n; i++) {
            this.keys[i] = keyOf(this.words[i]);
            if (this.keys[i] != 0) {
               // Every other word hides the 2nd, 4th... so the verse still reads from its first word.
               this.asked[i] = stage == LETTERS || stage == MEMORY || stage == HALF && askable % 2 == 1;
               askable++;
            }
            this.shown[i] = !this.asked[i];
            this.askedCount += this.asked[i] ? 1 : 0;
         }
         this.at = this.nextAsked(0);
      }

      static char keyOf(String word) {
         for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (Character.isLetterOrDigit(c)) {
               return Character.toLowerCase(c);
            }
         }
         return 0;
      }

      private int nextAsked(int from) {
         int i = from;
         while (i < this.words.length && !this.asked[i]) {
            i++;
         }
         return i;
      }

      /** The word being asked, or words.length when every word is in place. */
      public int at() {
         return this.at;
      }

      public boolean done() {
         return this.at >= this.words.length;
      }

      public boolean isAsked(int i) {
         return this.asked[i];
      }

      public boolean isShown(int i) {
         return this.shown[i];
      }

      /** Whether the word was shown after two wrong letters or a "Show word". */
      public boolean isSlipped(int i) {
         return this.slipped[i];
      }

      /** Whether a hidden word shows its first letter (the First letters stage). */
      public boolean showsLetter(int i) {
         return this.stage == LETTERS && !this.shown[i];
      }

      public int slips() {
         return this.slips;
      }

      public int askedCount() {
         return this.askedCount;
      }

      /** Slips a run may have and still pass: one in ten words, at least one. */
      public int allowed() {
         return Math.max(1, this.askedCount / 10);
      }

      public boolean passed() {
         return this.done() && this.slips <= this.allowed();
      }

      /** The first letter typed for the word being asked. */
      public int type(char c) {
         if (this.done() || !Character.isLetterOrDigit(c)) {
            return IGNORED;
         }
         if (Character.toLowerCase(c) == this.keys[this.at]) {
            this.reveal(false);
            return RIGHT;
         }
         if (++this.wrongHere >= 2) {
            this.reveal(true);
            return GIVEN;
         }
         return WRONG;
      }

      /** Shows the word being asked; it counts as a slip. */
      public void give() {
         if (!this.done()) {
            this.reveal(true);
         }
      }

      private void reveal(boolean slip) {
         this.shown[this.at] = true;
         this.slipped[this.at] = slip;
         this.slips += slip ? 1 : 0;
         this.wrongHere = 0;
         this.at = this.nextAsked(this.at + 1);
      }

      /**
       * Three letters to pick from for the word being asked, for a pointer or a finger: its own and two others from the
       * verse (or the alphabet), in an order that depends on the word, so the right one is not always in one place.
       */
      public char[] choices() {
         if (this.done()) {
            return new char[0];
         }
         char right = this.keys[this.at];
         List<Character> others = new ArrayList<>();
         for (int k = 1; k < this.keys.length && others.size() < 2; k++) {
            char c = this.keys[(this.at + k * 7) % this.keys.length];
            if (c != 0 && c != right && !others.contains(c) && Character.isLetter(c)) {
               others.add(c);
            }
         }
         for (char c = 'a'; others.size() < 2; c++) {
            if (c != right && !others.contains(c)) {
               others.add(c);
            }
         }
         char[] out = {others.get(0), others.get(1), 'x'};
         int place = (this.at * 5 + this.words[this.at].length()) % 3;
         out[2] = out[place];
         out[place] = right;
         return out;
      }
   }
}
