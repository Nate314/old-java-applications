package audiobibledownloader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

/**
 * Saves a range of one book's chapters as web pages, the job the app was first written for. It runs on its own
 * thread and fetches one chapter at a time through {@link BibleApi}, which spaces the requests out. Files go to
 * {@code ~/Downloads} (in the browser build, that folder becomes the browser's own downloads).
 */
public class DownloadManager implements Runnable {
   /** Told about progress from the download thread. */
   public interface Progress {
      void progress(int percent, String status);

      void finished(String status);
   }

   private final Progress window;
   private final String translation;
   private final int book;
   private final int chapter1;
   private final int chapter2;
   private final boolean allInOne;
   private final boolean showVerseAndChapter;

   private DownloadManager(Progress window, String translation, int book, int chapter1, int chapter2, boolean allInOne, boolean showVerseAndChapter) {
      this.window = window;
      this.translation = translation;
      this.book = book;
      this.chapter1 = chapter1;
      this.chapter2 = chapter2;
      this.allInOne = allInOne;
      this.showVerseAndChapter = showVerseAndChapter;
   }

   public static void downloadText(Progress window, String translation, int book, int chapter1, int chapter2, boolean allInOne, boolean showVerseAndChapter) {
      new Thread(new DownloadManager(window, translation, book, chapter1, chapter2, allInOne, showVerseAndChapter)).start();
   }

   public static File folder() {
      return new File(System.getProperty("user.home"), "Downloads");
   }

   /** "Luke", "1John": the file name stem for a book, as the original app named its files. */
   public static String stem(int book) {
      return Books.NAMES[book].replace(" ", "");
   }

   /** Downloads, and always ends with {@link Progress#finished}, so the window never waits on a thread that died. */
   @Override
   public void run() {
      try {
         download();
      } catch (RuntimeException ex) {
         this.window.finished("Download failed: " + ex);
      }
   }

   private void download() {
      List<Integer> failed = new ArrayList<>();
      String lastError = "";
      StringBuilder text = new StringBuilder();
      List<String> written = new ArrayList<>();
      int count = this.chapter2 - this.chapter1 + 1;
      this.window.progress(0, "Fetching " + Books.NAMES[this.book] + " " + this.chapter1 + "...");

      for (int chapter = this.chapter1; chapter <= this.chapter2; chapter++) {
         try {
            String body = chapterHtml(BibleApi.fetch(this.translation, this.book, chapter), this.showVerseAndChapter);
            if (this.allInOne) {
               text.append(body);
            } else {
               String name = stem(this.book) + chapter + ".html";
               writeHtml(name, Books.NAMES[this.book] + " " + chapter, body);
               written.add(name);
            }
         } catch (IOException | RuntimeException ex) {
            failed.add(chapter);
            lastError = ex.getMessage();
         }
         int done = chapter - this.chapter1 + 1;
         this.window.progress(done * 100 / count, done < count ? "Fetching " + Books.NAMES[this.book] + " " + (chapter + 1) + "..." : "");
      }

      if (this.allInOne && text.length() > 0) {
         String name = stem(this.book) + ".html";
         try {
            String title = Books.NAMES[this.book] + (count == 1 ? " " + this.chapter1 : " " + this.chapter1 + "-" + this.chapter2);
            writeHtml(name, title, text.toString());
            written.add(name);
         } catch (IOException ex) {
            lastError = "could not write " + name;
         }
      }

      String saved = written.isEmpty() ? "Nothing saved" : "Saved " + (written.size() == 1 ? written.get(0) : written.size() + " files");
      this.window.finished(saved + (failed.isEmpty() ? "" : "; failed: " + failed + " (" + lastError + ")"));
   }

   /** A chapter as HTML: a heading and bold verse numbers, or with them off, only the verse text. */
   static String chapterHtml(Chapter chapter, boolean showVerseAndChapter) {
      StringBuilder html = new StringBuilder();
      if (showVerseAndChapter) {
         html.append("<h2>").append(escape(chapter.title())).append("</h2>\n<p>");
         for (int i = 0; i < chapter.verses.length; i++) {
            html.append("<b>").append(chapter.verseNumbers[i]).append("</b> ").append(escape(chapter.verses[i])).append('\n');
         }
         html.append("</p>\n");
      } else {
         html.append("<p>");
         for (String verse : chapter.verses) {
            html.append(escape(verse)).append('\n');
         }
         html.append("</p>\n");
      }
      return html.toString();
   }

   static String escape(String text) {
      return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
   }

   private void writeHtml(String filename, String title, String body) throws IOException {
      File dir = folder();
      dir.mkdirs();
      String translationName = BibleApi.TRANSLATION_NAMES[Math.max(0, BibleApi.translationIndex(this.translation))];
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, filename)), StandardCharsets.UTF_8))) {
         writer.write("<!doctype html>\n<html><head><meta charset=\"utf-8\"><title>" + escape(title) + "</title>"
            + "<style>body{font:18px/1.6 Georgia,serif;max-width:40em;margin:2em auto;padding:0 1em}b{color:#a33;font-size:.7em;vertical-align:super}</style>"
            + "</head><body>\n" + body + "<p><small>" + escape(translationName) + " (public domain), from bible-api.com</small></p>\n</body></html>\n");
      }
   }
}
