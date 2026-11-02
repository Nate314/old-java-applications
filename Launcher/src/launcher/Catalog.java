package launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The apps the launcher lists, all 20 from the gallery in web/site/index.html (the one place the site describes
 * them), so a name or hint changed there shows here too. The two that press real keys with java.awt.Robot
 * (Windows, WindowsE) get their desktop hint and a warning from below: in the browser they press keys on a
 * pretend Windows, here on the real one.
 */
public final class Catalog {
   /** One app: its folder name (the run.ps1 name), display name, category, hint and where its source came from. */
   public static final class App {
      public final String id;
      public final String name;
      public final String category;   // game, music, toy or tool
      public final String hint;
      public final boolean original;  // original Eclipse source, rather than decompiled from a jar
      public final String warning;    // what it does to the real desktop, or null
      public final Path thumbnail;    // null when there is none

      App(String id, String name, String category, String hint, boolean original, String warning,
            Path thumbnail) {
         this.id = id;
         this.name = name;
         this.category = category;
         this.hint = hint;
         this.original = original;
         this.warning = warning;
         this.thumbnail = thumbnail;
      }

      /** True for the apps that press real keys on the desktop. */
      public boolean pressesKeys() {
         return warning != null;
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

   /** The apps that press real keys: { desktop hint, warning }, written from their READMEs. */
   private static final Map<String, String[]> REAL_KEYS = Map.of(
      "Windows", new String[] {"Presses the Windows key once, then exits. It has no window.",
         "It presses the real Windows key as soon as it starts, which opens the Start menu."},
      "WindowsE", new String[] {
         "A small window that presses Win+E a chosen number of times, which opens that many File Explorer windows.",
         "Clicking its DO IT!!! button opens real File Explorer windows, 10 of them by default."});

   private Catalog() {
   }

   /** Every app, in gallery order. */
   public static List<App> load(Path repoRoot) throws IOException {
      Path site = repoRoot.resolve("web/site");
      return parseGallery(new String(Files.readAllBytes(site.resolve("index.html")), StandardCharsets.UTF_8), site);
   }

   /** The gallery's cards; thumbnails resolve against the site folder. */
   static List<App> parseGallery(String html, Path site) {
      List<App> apps = new ArrayList<>();
      Matcher m = CARD.matcher(html);
      while (m.find()) {
         String attrs = m.group(2);
         Matcher cat = CAT.matcher(attrs);
         String id = m.group(1);
         String[] keys = REAL_KEYS.get(id);
         apps.add(new App(id, unescape(m.group(4)), cat.find() ? cat.group(1) : "tool",
            keys != null ? keys[0] : unescape(m.group(5)), attrs.contains("data-origin=\"original\""),
            keys != null ? keys[1] : null, site.resolve(m.group(3))));
      }
      return apps;
   }

   static String unescape(String s) {
      return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
         .replace("&apos;", "'").replace("&amp;", "&");
   }
}
