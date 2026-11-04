package audiobibledownloader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.TreeSet;

/**
 * What the reader remembers between runs, in {@code ~/.bibledownloader.txt}: the translation, the place (book,
 * chapter and the first verse on the open page), the text size, every chapter read to its end, and the highlighted
 * verses, the reading plan being followed (which plan, the day it started and the chapters read for it), and the
 * verses being learned by heart, with their text and next review. One
 * {@code key=value} line each, so the file is easy to read and the web title screen can show it.
 */
public final class Shelf {
   public static final int[] SIZES = {15, 17, 20};

   private final File file;
   public String translation = "web";
   public int book = Books.find("LUK");
   public int chapter = 1;
   /** The verse at the top of the open page, so a different text size or window keeps the place. */
   public int verse = 1;
   public int size = 1;
   private final BitSet read = new BitSet(Books.TOTAL);
   /** "LUK.1.3": a highlighted verse, in any translation. */
   private final TreeSet<String> marks = new TreeSet<>();
   /** The reading plan being followed, or null. */
   public Plan plan;
   /** The day the plan started (days since 1970-01-01, local time): its day 1. */
   public int planStart;
   /** Chapters read for the plan since it started, by ordinal among all 1,189. */
   private final BitSet planRead = new BitSet(Books.TOTAL);
   /** Verses being learned by heart, each on its own {@code learn.JHN.3.16=} line. */
   public final Memory memory = new Memory();

   public Shelf(File file) {
      this.file = file;
   }

   public static Shelf load(File file) {
      Shelf shelf = new Shelf(file);
      if (!file.isFile()) {
         return shelf;
      }
      try (InputStream in = new FileInputStream(file)) {
         for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0) {
               try {
                  shelf.apply(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
               } catch (RuntimeException e) {
                  // A damaged line is skipped; the rest of the file still counts.
               }
            }
         }
      } catch (IOException e) {
         System.err.println("Could not read " + file + ": " + e);
      }
      return shelf;
   }

   private void apply(String key, String value) {
      switch (key) {
         case "translation":
            if (BibleApi.translationIndex(value) >= 0) {
               this.translation = value;
            }
            break;
         case "at": {
            String[] p = value.split("\\.");
            int b = Books.find(p[0]);
            if (b >= 0 && p.length == 3) {
               this.book = b;
               this.chapter = clamp(Integer.parseInt(p[1]), 1, Books.CHAPTERS[b]);
               this.verse = Math.max(1, Integer.parseInt(p[2]));
            }
            break;
         }
         case "size":
            this.size = clamp(Integer.parseInt(value), 0, SIZES.length - 1);
            break;
         case "read":
            for (String ref : value.split(",")) {
               int ordinal = parseChapter(ref);
               if (ordinal >= 0) {
                  this.read.set(ordinal);
               }
            }
            break;
         case "marks":
            for (String ref : value.split(",")) {
               String[] p = ref.trim().split("\\.");
               if (p.length == 3 && parseChapter(p[0] + "." + p[1]) >= 0) {
                  this.marks.add(ref.trim().toUpperCase());
               }
            }
            break;
         case "plan":
            this.plan = Plan.find(value);
            break;
         case "planstart":
            this.planStart = Plan.parseIso(value);
            break;
         case "plandone":
            for (String ref : value.split(",")) {
               int ordinal = parseChapter(ref);
               if (ordinal >= 0) {
                  this.planRead.set(ordinal);
               }
            }
            break;
         default:
            if (key.startsWith("learn.")) {
               this.memory.put(Memory.Card.parse(key.substring(6), value));
            }
      }
   }

   /** "LUK.3" to its ordinal, or -1. */
   private static int parseChapter(String ref) {
      String[] p = ref.trim().split("\\.");
      if (p.length != 2) {
         return -1;
      }
      int b = Books.find(p[0]);
      try {
         int c = Integer.parseInt(p[1]);
         return b >= 0 && c >= 1 && c <= Books.CHAPTERS[b] ? Books.ordinal(b, c) : -1;
      } catch (NumberFormatException e) {
         return -1;
      }
   }

   private static String refs(BitSet chapters) {
      List<String> refs = new ArrayList<>();
      for (int i = chapters.nextSetBit(0); i >= 0; i = chapters.nextSetBit(i + 1)) {
         refs.add(Books.IDS[Books.bookOf(i)] + "." + Books.chapterOf(i));
      }
      return String.join(",", refs);
   }

   public void save() {
      String text = "translation=" + this.translation + "\n"
         + "place=" + Books.NAMES[this.book] + " " + this.chapter + ":" + this.verse + "\n"
         + "at=" + Books.IDS[this.book] + "." + this.chapter + "." + this.verse + "\n"
         + "size=" + this.size + "\n"
         + "read=" + refs(this.read) + "\n"
         + "marks=" + String.join(",", this.marks) + "\n"
         + (this.plan == null ? "" : "plan=" + this.plan.id + "\n"
            + "planstart=" + Plan.iso(this.planStart) + "\n"
            + "plandone=" + refs(this.planRead) + "\n");
      StringBuilder learning = new StringBuilder();
      for (Memory.Card card : this.memory.cards()) {
         learning.append("learn.").append(card.ref()).append('=').append(card.line()).append('\n');
      }
      text += learning;
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8))) {
         writer.write(text);
      } catch (IOException e) {
         System.err.println("Could not save " + this.file + ": " + e);
      }
   }

   public boolean isRead(int book, int chapter) {
      return this.read.get(Books.ordinal(book, chapter));
   }

   public void markRead(int book, int chapter) {
      this.read.set(Books.ordinal(book, chapter));
   }

   public int readCount() {
      return this.read.cardinality();
   }

   public int readCount(int book) {
      int count = 0;
      for (int c = 1; c <= Books.CHAPTERS[book]; c++) {
         count += this.isRead(book, c) ? 1 : 0;
      }
      return count;
   }

   /** Starts a plan on a day, from its first chapter; any other plan and what was read for it are dropped. */
   public void startPlan(Plan plan, int day) {
      this.plan = plan;
      this.planStart = day;
      this.planRead.clear();
   }

   public void stopPlan() {
      this.plan = null;
      this.planRead.clear();
   }

   /** Counts a chapter read to its end for the plan; returns whether it is in the plan and was not counted yet. */
   public boolean notePlanRead(int book, int chapter) {
      if (this.plan == null || this.plan.indexOf(book, chapter) < 0 || this.planRead.get(Books.ordinal(book, chapter))) {
         return false;
      }
      this.planRead.set(Books.ordinal(book, chapter));
      return true;
   }

   /** The chapters read for the plan, indexed from the plan's first chapter (as {@link Plan} counts them). */
   public BitSet planDone() {
      BitSet done = new BitSet();
      if (this.plan != null) {
         for (int i = 0; i < this.plan.chapters; i++) {
            if (this.planRead.get(this.plan.first + i)) {
               done.set(i);
            }
         }
      }
      return done;
   }

   /** The plan's day for a date, 0 on the day it started (negative before, past the last day after). */
   public int planDay(int today) {
      return today - this.planStart;
   }

   public static String verseRef(int book, int chapter, int verse) {
      return Books.IDS[book] + "." + chapter + "." + verse;
   }

   public boolean isMarked(int book, int chapter, int verse) {
      return this.marks.contains(verseRef(book, chapter, verse));
   }

   /** Highlights the verse, or takes the highlight off; returns whether it is highlighted now. */
   public boolean toggleMark(int book, int chapter, int verse) {
      String ref = verseRef(book, chapter, verse);
      if (this.marks.remove(ref)) {
         return false;
      }
      this.marks.add(ref);
      return true;
   }

   /** Every highlighted verse as {book, chapter, verse}, in Bible order. */
   public List<int[]> marks() {
      List<int[]> out = new ArrayList<>();
      for (String ref : this.marks) {
         String[] p = ref.split("\\.");
         out.add(new int[]{Books.find(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])});
      }
      out.sort((a, b) -> Long.compare(key(a[0], a[1], a[2]), key(b[0], b[1], b[2])));
      return out;
   }

   public int markCount() {
      return this.marks.size();
   }

   /** The first highlighted verse after this one, wrapping to the start of the Bible; null when there are none. */
   public int[] nextMark(int book, int chapter, int verse) {
      int[] first = null;
      int[] next = null;
      long here = key(book, chapter, verse);
      long best = Long.MAX_VALUE;
      long lowest = Long.MAX_VALUE;
      for (String ref : this.marks) {
         String[] p = ref.split("\\.");
         int b = Books.find(p[0]);
         int c = Integer.parseInt(p[1]);
         int v = Integer.parseInt(p[2]);
         long k = key(b, c, v);
         if (k < lowest) {
            lowest = k;
            first = new int[]{b, c, v};
         }
         if (k > here && k < best) {
            best = k;
            next = new int[]{b, c, v};
         }
      }
      return next != null ? next : first;
   }

   private static long key(int book, int chapter, int verse) {
      return (long) Books.ordinal(book, chapter) * 1000 + verse;
   }

   private static int clamp(int v, int lo, int hi) {
      return Math.max(lo, Math.min(hi, v));
   }
}
