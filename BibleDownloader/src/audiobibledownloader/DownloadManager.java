package audiobibledownloader;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

public class DownloadManager {
   public static String text = "";
   public static String book = "";
   private static int chapter1 = 0;
   public static int currentChapter = 0;
   private static int chapter2 = 0;
   public static boolean allInOne;
   public static boolean showVerseAndChapter;

   public static void downloadAudio(String book, final int chapter1, final int chapter2) {
      DownloadManager.book = book;
      DownloadManager.chapter1 = chapter1;
      DownloadManager.chapter2 = chapter2;
      Thread thread = new Thread(new Runnable() {
         @Override
         public void run() {
            Main.percentage = 0;
            Main.progressBar.repaint();

            for (int i = chapter1; i < chapter2 + 1; i++) {
               DownloadManager.downloadThisChapter(i, true);
            }
         }
      });
      thread.start();
   }

   private static void dowloadThisFile(String url, String filename) {
      try {
         new WebpageDownloader().downloadWebpage(filename, "http:" + url);
      } catch (IOException ex) {
         ex.printStackTrace();
      }
   }

   private static void downloadThisChapter(int chapter, boolean audio) {
      currentChapter = chapter;
      String url = "https://www.bible.com/bible/111/" + book.toLowerCase() + "." + chapter + ".niv.html";
      String a = new WebpageDownloader().readHtmlPage(url);
      if (audio) {
         String[] b = a.split(".mp3");
         String mp3Url = b[1].split("src='")[b[1].split("src='").length - 1] + ".mp3";
         dowloadThisFile(mp3Url, book + chapter + ".mp3");
      } else {
         if (!allInOne) {
            text = "";
         }

         String b = a.split("<div class='columns large-6 reader-center' dir='ltr'")[1];
         String c = b.split("<div class='copyright' ng-hide='copyright'>")[0];
         String d = c.split("\n")[5];
         if (showVerseAndChapter) {
            ArrayList<String> textWords = new ArrayList<>();
            String[] e = d.split("<");

            for (int i = 0; i < e.length; i++) {
               e[i] = e[i].endsWith(">") ? "" : "<" + e[i] + "\n";
            }

            for (int i = 0; i < e.length; i++) {
               if (e[i].startsWith("<span class=\"content\">") || e[i].startsWith("<span class=\"label\">")) {
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
                  } else if (e[i].contains("<span class=\"label\">")) {
                     textWords.add(
                        e[i]
                              .substring(e[i].indexOf("<span class=\"label\">"), e[i].length())
                              .replace("&#8220;", "\"")
                              .replace("&#8221;", "\"")
                              .replace("&#8217;", "'")
                              .replace("&#8212;", "--")
                              .replace("\n", "")
                           + "\n"
                     );
                  }
               }

               text = text
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
            text = text + "<br>";
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
               text = text + textWords.get(ix).replace("<span class=\"content\">", "");
            }

            text = text + "<br>";
         }

         if (!allInOne) {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(book + chapter + ".html"), "utf-8"))) {
               writer.write("<html>" + text + "</html>");
            } catch (IOException ex) {
               ex.printStackTrace();
            }
         } else if (chapter == chapter2) {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(book + ".html"), "utf-8"))) {
               writer.write("<html>" + text + "</html>");
            } catch (IOException ex) {
               ex.printStackTrace();
            }
         }
      }

      text.replace("#", "");
      Main.percentage = (chapter - chapter1 + 1) * 100 / (chapter2 - chapter1 + 1);
      Main.progressBar.repaint();
   }

   public static void downloadText(String book, final int chapter1, final int chapter2) {
      DownloadManager.book = book;
      DownloadManager.chapter1 = chapter1;
      DownloadManager.chapter2 = chapter2;
      Thread thread = new Thread(new Runnable() {
         @Override
         public void run() {
            Main.percentage = 0;
            Main.progressBar.repaint();

            for (int i = chapter1; i < chapter2 + 1; i++) {
               DownloadManager.downloadThisChapter(i, false);
            }
         }
      });
      thread.start();
   }
}
