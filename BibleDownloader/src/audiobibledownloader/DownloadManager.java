package audiobibledownloader;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** One download of a chapter range, run on its own thread. Each download keeps its own state. */
public class DownloadManager implements Runnable {
   private final Main window;
   private final String book;
   private final int chapter1;
   private final int chapter2;
   private final boolean audio;
   private final boolean allInOne;
   private final boolean showVerseAndChapter;
   private String text = "";

   private DownloadManager(Main window, String book, int chapter1, int chapter2, boolean audio, boolean allInOne, boolean showVerseAndChapter) {
      this.window = window;
      this.book = book;
      this.chapter1 = chapter1;
      this.chapter2 = chapter2;
      this.audio = audio;
      this.allInOne = allInOne;
      this.showVerseAndChapter = showVerseAndChapter;
   }

   public static void downloadAudio(Main window, String book, int chapter1, int chapter2) {
      new Thread(new DownloadManager(window, book, chapter1, chapter2, true, false, false)).start();
   }

   public static void downloadText(Main window, String book, int chapter1, int chapter2, boolean allInOne, boolean showVerseAndChapter) {
      new Thread(new DownloadManager(window, book, chapter1, chapter2, false, allInOne, showVerseAndChapter)).start();
   }

   @Override
   public void run() {
      List<Integer> failed = new ArrayList<>();
      String lastError = "";
      this.window.showProgress(0, "");

      for (int chapter = this.chapter1; chapter <= this.chapter2; chapter++) {
         try {
            this.downloadThisChapter(chapter);
         } catch (FileNotFoundException ex) {
            failed.add(chapter);
            lastError = this.book + " " + chapter + " does not exist";
         } catch (IOException | RuntimeException ex) {
            // RuntimeException: the page markup did not match (a missing split marker) or the URL was invalid.
            failed.add(chapter);
            lastError = this.book + " " + chapter + ": " + ex.getClass().getSimpleName();
            ex.printStackTrace();
         }
         this.window.showProgress((chapter - this.chapter1 + 1) * 100 / (this.chapter2 - this.chapter1 + 1), lastError);
      }

      if (!this.audio && this.allInOne && !this.text.isEmpty()) {
         try {
            writeHtml(this.book + ".html", this.text);
         } catch (IOException ex) {
            ex.printStackTrace();
            lastError = "could not write " + this.book + ".html";
         }
      }

      this.window.downloadFinished(failed.isEmpty() ? "done" : "failed: " + failed + (failed.size() == 1 ? " (" + lastError + ")" : ""));
   }

   private void downloadThisChapter(int chapter) throws IOException {
      String url = "https://www.bible.com/bible/111/" + this.book.toLowerCase() + "." + chapter + ".niv.html";
      String a = new WebpageDownloader().readHtmlPage(url);
      if (this.audio) {
         String[] b = a.split(".mp3");
         String mp3Url = b[1].split("src='")[b[1].split("src='").length - 1] + ".mp3";
         // The page links the audio without a scheme ("//host/file.mp3").
         new WebpageDownloader().downloadWebpage(this.book + chapter + ".mp3", mp3Url.startsWith("//") ? "https:" + mp3Url : mp3Url);
         return;
      }

      String chapterText = "";
      String b = a.split("<div class='columns large-6 reader-center' dir='ltr'")[1];
      String c = b.split("<div class='copyright' ng-hide='copyright'>")[0];
      String d = c.split("\n")[5];
      if (this.showVerseAndChapter) {
         String[] e = d.split("<");

         for (int i = 0; i < e.length; i++) {
            e[i] = e[i].endsWith(">") ? "" : "<" + e[i] + "\n";
         }

         for (int i = 0; i < e.length; i++) {
            chapterText = chapterText
               + (
                  e[i].contains("<div class=\"label\">")
                     ? "<br><b>Chapter " + e[i].substring("<div class=\"label\">".length()) + "</b><br>"
                     : (
                        e[i].contains("<span class=\"label\">")
                           ? "<b>" + e[i].substring("<span class=\"label\">".length()) + "</b>"
                           : (e[i].contains("<span class=\"content\">") ? e[i].substring("<span class=\"content\">".length()) : "")
                     )
               );
         }
      } else {
         chapterText = chapterText + "<br>";
         String[] e = d.split("<span>|</span>");
         ArrayList<String> textWords = new ArrayList<>();

         for (int i = 0; i < e.length; i++) {
            if (e[i].contains("<span class=\"content\">")) {
               textWords.add(
                  e[i]
                        .substring(e[i].indexOf("<span class=\"content\">"), e[i].length())
                        .replace("&#8220;", "\"")
                        .replace("&#8221;", "\"")
                        .replace("&#8217;", "'")
                        .replace("&#8212;", "--")
                        .replace("\n", "")
                     + "\n"
               );
            }
         }

         for (int ix = 0; ix < textWords.size(); ix++) {
            chapterText = chapterText + textWords.get(ix).replace("<span class=\"content\">", "");
         }

         chapterText = chapterText + "<br>";
      }

      if (this.allInOne) {
         this.text = this.text + chapterText;
      } else {
         writeHtml(this.book + chapter + ".html", chapterText);
      }
   }

   private static void writeHtml(String filename, String body) throws IOException {
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {
         writer.write("<html><head><meta charset=\"utf-8\"></head><body>" + body + "</body></html>");
      }
   }
}
