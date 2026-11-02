package launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The apps the launcher lists. The 16 browser apps come from the gallery in web/site/index.html (the one place
 * the site describes them), so a name or hint changed there shows here too. The four desktop-only apps are not in
 * the gallery, so they are described below, from their READMEs.
 */
public final class Catalog {
   /** One app: its folder name (the run.ps1 name), display name, category, hint and where its source came from. */
   public static final class App {
      public final String id;
      public final String name;
      public final String category;   // game, music, toy or tool
      public final String hint;
      public final boolean original;  // original Eclipse source, rather than decompiled from a jar
      public final boolean desktopOnly;
      public final String warning;    // what it does to the real desktop, or null
      public final Path thumbnail;    // null when there is none

      App(String id, String name, String category, String hint, boolean original, boolean desktopOnly,
            String warning, Path thumbnail) {
         this.id = id;
         this.name = name;
         this.category = category;
         this.hint = hint;
         this.original = original;
         this.desktopOnly = desktopOnly;
         this.warning = warning;
         this.thumbnail = thumbnail;
      }

      /** True when the search text is in the name, the id, the category or the hint (any case). */
      public boolean matches(String query) {
         String q = query.trim().toLowerCase();
         if (q.isEmpty()) return true;
         return (name + " " + id + " " + category + " " + hint).toLowerCase().contains(q);
      }
   }

   private static final Pattern CARD = Pattern.compile(
      "<li data-app=\"(\\w+)\"([^>]*)>.*?<img src=\"([^\"]+)\".*?<strong>(.*?)</strong><span>(.*?)</span>");
   private static final Pattern CAT = Pattern.compile("data-cat=\"(\\w+)\"");

   private Catalog() {
   }

   /** Every app: the gallery's, in gallery order, then the desktop-only ones. */
   public static List<App> load(Path repoRoot) throws IOException {
      Path site = repoRoot.resolve("web/site");
      List<App> apps = parseGallery(new String(Files.readAllBytes(site.resolve("index.html")), StandardCharsets.UTF_8), site);
      apps.addAll(desktopOnly());
      return apps;
   }

   /** The gallery's cards; thumbnails resolve against the site folder. */
   static List<App> parseGallery(String html, Path site) {
      List<App> apps = new ArrayList<>();
      Matcher m = CARD.matcher(html);
      while (m.find()) {
         String attrs = m.group(2);
         Matcher cat = CAT.matcher(attrs);
         apps.add(new App(m.group(1), unescape(m.group(4)), cat.find() ? cat.group(1) : "tool", unescape(m.group(5)),
            attrs.contains("data-origin=\"original\""), false, null, site.resolve(m.group(3))));
      }
      return apps;
   }

   static List<App> desktopOnly() {
      List<App> apps = new ArrayList<>();
      apps.add(new App("BibleDownloader", "Bible Downloader", "tool",
         "Downloads NIV Bible chapters from bible.com, as each chapter's MP3 audio or as chapter text saved to HTML. "
            + "Its 2016 page addresses return 404 today, so every chapter fails.",
         false, true, null, null));
      apps.add(new App("DinoGame", "Dino Game bot", "toy",
         "A bot for the Chrome dinosaur game: it watches one screen pixel and presses Space when an obstacle reaches it. "
            + "Click one of its marker squares to stop it.",
         false, true, "It starts pressing Space in whatever window has focus about 2.5 seconds after it opens. "
            + "Open the dinosaur game first.", null));
      apps.add(new App("Windows", "Windows", "toy",
         "Presses the Windows key once, then exits. It has no window.",
         false, true, "It presses the real Windows key as soon as it starts, which opens the Start menu.", null));
      apps.add(new App("WindowsE", "Windows+E", "toy",
         "A small window that presses Win+E a chosen number of times, which opens that many File Explorer windows.",
         false, true, "Clicking its DO IT!!! button opens real File Explorer windows, 10 of them by default.", null));
      return apps;
   }

   static String unescape(String s) {
      return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
         .replace("&apos;", "'").replace("&amp;", "&");
   }
}
