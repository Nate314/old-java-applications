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
