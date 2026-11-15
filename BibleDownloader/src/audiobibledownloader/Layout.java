package audiobibledownloader;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

/**
 * Sets a chapter as printed pages: a title on a book's first chapter, a large chapter numeral beside the first two
 * lines, small raised verse numbers, and words wrapped to the page width. Pages hold a fixed number of lines.
 */
public final class Layout {
   public static final int WORD = 0;
   public static final int VERSE_NUMBER = 1;
   public static final int TITLE = 2;
   public static final int NUMERAL = 3;
   /** A word of the second translation, set under the first's verse when two share one page. */
   public static final int ALT_WORD = 4;
   /** Lines a book title takes at the top of its first chapter. */
   public static final int TITLE_LINES = 3;
   /** Lines the chapter numeral spans. */
   public static final int NUMERAL_LINES = 2;

   /** A word, verse number or heading, placed on one line of a page. */
   public static final class Run {
      public final int kind;
      public final String text;
      public final int x;
      public final int line;
      public final int width;
      /** The verse the run belongs to (0 for headings). */
      public final int verse;

      Run(int kind, String text, int x, int line, int width, int verse) {
         this.kind = kind;
         this.text = text;
         this.x = x;
         this.line = line;
         this.width = width;
         this.verse = verse;
      }
   }

   public static final class Page {
      public final List<Run> runs = new ArrayList<>();
      /** The first verse that starts or continues on this page. */
      public int firstVerse;
      public int lastVerse;
   }

   private Layout() {
   }

   /**
    * @param body  verse text
    * @param small verse numbers
    * @param numeral the chapter numeral
    * @param width page width in pixels
    * @param lines lines per page (at least 4)
    */
   public static List<Page> paginate(Chapter chapter, FontMetrics body, FontMetrics small, FontMetrics numeral, int width, int lines) {
      lines = Math.max(lines, TITLE_LINES + NUMERAL_LINES + 1);
      List<Page> pages = new ArrayList<>();
      Page page = new Page();
      pages.add(page);
      int line = 0;
      if (chapter.number == 1) {
         page.runs.add(new Run(TITLE, Books.NAMES[chapter.book], 0, 0, width, 0));
         line = TITLE_LINES;
      }
      String digits = String.valueOf(chapter.number);
      int numeralWidth = numeral.stringWidth(digits);
      page.runs.add(new Run(NUMERAL, digits, 0, line, numeralWidth, 0));
      int indentUntil = line + NUMERAL_LINES;
      int indent = numeralWidth + body.stringWidth("  ");
      int space = body.stringWidth(" ");
      int x = indent;
      page.firstVerse = chapter.verseNumbers[0];

      for (int v = 0; v < chapter.verses.length; v++) {
         int verse = chapter.verseNumbers[v];
         String[] words = chapter.verses[v].isEmpty() ? new String[0] : chapter.verses[v].split(" ");
         // Verse 1 starts beside the chapter numeral, so it needs no number of its own, as in a printed Bible.
         String label = v == 0 && verse == 1 ? null : String.valueOf(verse);
         for (int w = -1; w < words.length; w++) {
            String text = w < 0 ? label : words[w];
            if (text == null) {
               continue;
            }
            int kind = w < 0 ? VERSE_NUMBER : WORD;
            int textWidth = kind == VERSE_NUMBER ? small.stringWidth(text) + 2 : body.stringWidth(text);
            int left = line < indentUntil ? indent : 0;
            // A verse number stays with the first word of its verse.
            int need = kind == VERSE_NUMBER && words.length > 0 ? textWidth + body.stringWidth(words[0]) : textWidth;
            if (x > left && x + need > width) {
               line++;
               x = line < indentUntil ? indent : 0;
               if (line >= lines) {
                  page = new Page();
                  pages.add(page);
                  page.firstVerse = verse;
                  line = 0;
                  indentUntil = 0;
                  x = 0;
               }
            }
            page.runs.add(new Run(kind, text, x, line, textWidth, verse));
            page.lastVerse = verse;
            x += textWidth + (kind == VERSE_NUMBER ? 1 : space);
         }
      }
      return pages;
   }

   /**
    * Sets two translations of a chapter verse by verse, so each verse starts on the same line in both. Side by side,
    * the pages alternate: the first translation's page, then the second's page for the same verses, so a two-page
    * spread is one pair. Stacked (one page at a time), each verse is followed by the second translation's text
    * ({@link #ALT_WORD}, measured with {@code stacked}, its font; null for side by side). A verse missing from one
    * translation leaves a gap there.
    */
   public static List<Page> parallel(Chapter first, Chapter second, FontMetrics body, FontMetrics small, FontMetrics numeral,
         int width, int lines, FontMetrics stacked) {
      lines = Math.max(lines, TITLE_LINES + NUMERAL_LINES + 1);
      java.util.TreeMap<Integer, String[]> verses = new java.util.TreeMap<>();
      Chapter[] both = {first, second};
      for (int t = 0; t < 2; t++) {
         for (int v = 0; v < both[t].verses.length; v++) {
            verses.computeIfAbsent(both[t].verseNumbers[v], k -> new String[]{"", ""})[t] = both[t].verses[v];
         }
      }
      int columns = stacked != null ? 1 : 2;
      List<Page> pages = new ArrayList<>();
      Page[] pair = addPair(pages, columns, verses.firstKey());
      int line = 0;
      if (first.number == 1) {
         for (Page page : pair) {
            page.runs.add(new Run(TITLE, Books.NAMES[first.book], 0, 0, width, 0));
         }
         line = TITLE_LINES;
      }
      String digits = String.valueOf(first.number);
      int indent = numeral.stringWidth(digits) + body.stringWidth("  ");
      for (Page page : pair) {
         page.runs.add(new Run(NUMERAL, digits, 0, line, indent - body.stringWidth("  "), 0));
      }
      int indentUntil = line + NUMERAL_LINES;
      for (java.util.Map.Entry<Integer, String[]> entry : verses.entrySet()) {
         int verse = entry.getKey();
         // Verse 1 beside the chapter numeral needs no number, as in paginate.
         String label = verse == 1 && line < indentUntil ? null : String.valueOf(verse);
         List<List<Run>> blocks = blocks(entry.getValue(), label, verse, stacked, line, indentUntil, indent, body, small, width);
         // A verse that does not fit runs on over the page, unless fewer than 3 lines are left: then it starts the next pages.
         if (line + height(blocks) > lines && line > 0 && lines - line < 3) {
            pair = addPair(pages, columns, verse);
            line = 0;
            indentUntil = 0;
            blocks = blocks(entry.getValue(), String.valueOf(verse), verse, stacked, 0, 0, 0, body, small, width);
         }
         int height = height(blocks);
         for (int row = 0; row < height; row++) {
            if (line >= lines) {
               pair = addPair(pages, columns, verse);
               line = 0;
            }
            for (int c = 0; c < columns; c++) {
               pair[c].lastVerse = verse;
               for (Run r : blocks.get(c)) {
                  if (r.line == row) {
                     pair[c].runs.add(new Run(r.kind, r.text, r.x, line, r.width, verse));
                  }
               }
            }
            line++;
         }
         for (Page page : pair) {
            page.lastVerse = verse;
         }
      }
      return pages;
   }

   /** One verse's runs per column, at lines counted from the verse's first line ({@code start} on the page). */
   private static List<List<Run>> blocks(String[] texts, String label, int verse, FontMetrics stacked, int start, int indentUntil,
         int indent, FontMetrics body, FontMetrics small, int width) {
      List<List<Run>> blocks = new ArrayList<>();
      if (stacked != null) {
         List<Run> runs = wrap(texts[0], label, WORD, verse, start, indentUntil, indent, body, small, width);
         int h = height(java.util.Collections.singletonList(runs));
         for (Run r : wrap(texts[1], null, ALT_WORD, verse, start + h, indentUntil, indent, stacked, small, width)) {
            runs.add(new Run(r.kind, r.text, r.x, r.line + h, r.width, verse));
         }
         blocks.add(runs);
      } else {
         for (int t = 0; t < 2; t++) {
            blocks.add(wrap(texts[t], label, WORD, verse, start, indentUntil, indent, body, small, width));
         }
      }
      return blocks;
   }

   private static int height(List<List<Run>> blocks) {
      int height = 0;
      for (List<Run> runs : blocks) {
         height = Math.max(height, runs.isEmpty() ? 0 : runs.get(runs.size() - 1).line + 1);
      }
      return height;
   }

   private static Page[] addPair(List<Page> pages, int columns, int verse) {
      Page[] pair = new Page[columns];
      for (int c = 0; c < columns; c++) {
         pair[c] = new Page();
         pair[c].firstVerse = verse;
         pair[c].lastVerse = verse;
         pages.add(pair[c]);
      }
      return pair;
   }

   /**
    * One verse's words wrapped to the width, at lines counted from 0 (the verse's first line, which is {@code start}
    * on the page, so lines before {@code indentUntil} are indented past the chapter numeral).
    */
   private static List<Run> wrap(String text, String label, int kind, int verse, int start, int indentUntil, int indent,
         FontMetrics body, FontMetrics small, int width) {
      List<Run> runs = new ArrayList<>();
      String[] words = text.isEmpty() ? new String[0] : text.split(" ");
      if (words.length == 0) {
         return runs;
      }
      int row = 0;
      int x = start < indentUntil ? indent : 0;
      int space = body.stringWidth(" ");
      for (int w = -1; w < words.length; w++) {
         String t = w < 0 ? label : words[w];
         if (t == null) {
            continue;
         }
         boolean number = w < 0;
         int tw = number ? small.stringWidth(t) + 2 : body.stringWidth(t);
         int left = start + row < indentUntil ? indent : 0;
         int need = number ? tw + body.stringWidth(words[0]) : tw;
         if (x > left && x + need > width) {
            row++;
            x = start + row < indentUntil ? indent : 0;
         }
         runs.add(new Run(number ? VERSE_NUMBER : kind, t, x, row, tw, verse));
         x += tw + (number ? 1 : space);
      }
      return runs;
   }

   /** The first page that shows this verse (the last page for a verse past the end). */
   public static int pageOf(List<Page> pages, int verse) {
      for (int i = 0; i < pages.size(); i++) {
         if (pages.get(i).lastVerse >= verse) {
            return i;
         }
      }
      return pages.size() - 1;
   }
}
