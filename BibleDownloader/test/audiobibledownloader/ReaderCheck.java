package audiobibledownloader;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/** Self-check for the book table, the chapter parser, the page layout and the shelf file. Runs offline; use java -ea. */
public class ReaderCheck {
   public static void main(String[] args) throws Exception {
      // 66 books and 1,189 chapters; ordinals round-trip.
      check(Books.IDS.length == 66 && Books.NAMES.length == 66 && Books.CHAPTERS.length == 66, "66 books");
      check(Books.TOTAL == 1189, "1,189 chapters");
      for (int o = 0; o < Books.TOTAL; o++) {
         check(Books.ordinal(Books.bookOf(o), Books.chapterOf(o)) == o, "ordinal round-trip " + o);
      }
      check(Books.find("luk") == 41 && Books.find("XYZ") < 0, "find by id");
      check(BibleApi.url("kjv", Books.find("JHN"), 3).equals("https://bible-api.com/data/kjv/JHN/3"), "chapter address");

      // The parser reads the verse and text fields, with escapes, and folds whitespace.
      String json = "{\"translation\":{\"identifier\":\"web\"},\"verses\":[{\"book_id\":\"JHN\",\"book\":\"John\",\"chapter\":3,"
         + "\"verse\":1,\"text\":\"Now there was a man\\n\"},{\"book_id\":\"JHN\",\"chapter\":3,\"verse\":2,"
         + "\"text\":\"He said, \\\"Rabbi\\\" \\u2014  and  more.\\n\"}]}";
      Chapter chapter = Chapter.parse(Books.find("JHN"), 3, "web", json);
      check(chapter.verses.length == 2 && chapter.verseNumbers[1] == 2, "two verses");
      check(chapter.verses[0].equals("Now there was a man"), "newline trimmed: " + chapter.verses[0]);
      check(chapter.verses[1].equals("He said, \"Rabbi\" \u2014 and more."), "escapes and spaces: " + chapter.verses[1]);
      check(chapter.title().equals("John 3"), "title");
      boolean threw = false;
      try {
         Chapter.parse(0, 1, "web", "{\"error\":\"not found\"}");
      } catch (IllegalArgumentException e) {
         threw = true;
      }
      check(threw, "an answer without verses is refused");

      // Layout: every word is placed once, lines stay inside the page, pages hold their line count.
      StringBuilder long1 = new StringBuilder();
      for (int i = 0; i < 400; i++) {
         long1.append("word").append(i).append(' ');
      }
      Chapter big = new Chapter(Books.find("LUK"), 1, "web", new int[]{1, 2, 3},
         new String[]{long1.toString().trim(), "short verse", "the end"});
      FontMetrics body = metrics(new Font("Serif", Font.PLAIN, 17));
      List<Layout.Page> pages = Layout.paginate(big, body, metrics(new Font("SansSerif", Font.BOLD, 10)), metrics(new Font("Serif", Font.BOLD, 51)), 320, 14);
      int words = 0;
      for (Layout.Page page : pages) {
         for (Layout.Run run : page.runs) {
            check(run.line >= 0 && run.line < 14, "line inside the page");
            if (run.kind == Layout.WORD) {
               words++;
               check(run.x + run.width <= 320 || run.x == 0, "word inside the width");
            }
         }
      }
      check(words == 400 + 2 + 2, "every word placed once: " + words);
      check(pages.size() > 2, "a long chapter takes several pages");
      check(pages.get(0).runs.get(0).kind == Layout.TITLE, "chapter 1 opens with the book title");
      check(pages.get(0).firstVerse == 1 && pages.get(pages.size() - 1).lastVerse == 3, "first and last verse");
      int third = Layout.pageOf(pages, 3);
      check(pages.get(third).lastVerse >= 3 && (third == 0 || pages.get(third - 1).lastVerse < 3), "verse 3 is first shown on page " + third);
      check(Layout.pageOf(pages, 1) == 0 && Layout.pageOf(pages, 99) == pages.size() - 1, "page of a verse");

      // The shelf keeps place, size, read chapters and highlights across a reload; bad lines are skipped.
      File file = File.createTempFile("bibleshelf", ".txt");
      Shelf shelf = Shelf.load(file);
      check(shelf.book == Books.find("LUK") && shelf.chapter == 1 && shelf.translation.equals("web"), "starts at Luke 1");
      shelf.translation = "kjv";
      shelf.book = Books.find("PSA");
      shelf.chapter = 23;
      shelf.verse = 4;
      shelf.size = 2;
      shelf.markRead(Books.find("PSA"), 23);
      shelf.markRead(Books.find("REV"), 22);
      check(shelf.toggleMark(Books.find("JHN"), 3, 16), "highlight on");
      check(shelf.toggleMark(Books.find("GEN"), 1, 1), "second highlight");
      shelf.save();
      Shelf again = Shelf.load(file);
      check(again.translation.equals("kjv") && again.book == Books.find("PSA") && again.chapter == 23 && again.verse == 4 && again.size == 2, "place kept");
      check(again.readCount() == 2 && again.isRead(Books.find("REV"), 22) && again.readCount(Books.find("PSA")) == 1, "read chapters kept");
      check(again.isMarked(Books.find("JHN"), 3, 16) && again.markCount() == 2, "highlights kept");
      int[] next = again.nextMark(Books.find("JHN"), 3, 16);
      check(next[0] == Books.find("GEN"), "next highlight wraps to Genesis");
      check(!again.toggleMark(Books.find("JHN"), 3, 16) && again.markCount() == 1, "highlight off");
      java.nio.file.Files.write(file.toPath(), "junk\nat=ZZZ.9.9\nsize=x\nread=LUK.99,JHN.1\nmarks=bad\n".getBytes("UTF-8"));
      Shelf damaged = Shelf.load(file);
      check(damaged.book == Books.find("LUK") && damaged.isRead(Books.find("JHN"), 1) && damaged.readCount() == 1 && damaged.markCount() == 0,
         "damaged lines skipped");
      file.delete();

      // Saved pages escape the text and name files as the original app did.
      check(DownloadManager.stem(Books.find("1JN")).equals("1John"), "file stem");
      String html = DownloadManager.chapterHtml(new Chapter(0, 1, "web", new int[]{1}, new String[]{"a < b & c"}), true);
      check(html.contains("<h2>Genesis 1</h2>") && html.contains("<b>1</b> a &lt; b &amp; c"), "chapter html: " + html);
      check(!DownloadManager.chapterHtml(chapter, false).contains("<b>"), "verse numbers off");
      check(ReaderPanel.commas(1189).equals("1,189") && ReaderPanel.commas(42).equals("42"), "commas");
      System.out.println("ReaderCheck: all checks passed");
   }

   private static FontMetrics metrics(Font font) {
      return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics().getFontMetrics(font);
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
