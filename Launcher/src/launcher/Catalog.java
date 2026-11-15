package launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The apps the launcher lists, all 20 from web/src/shared/apps.json (the one place the site describes them, and the
 * list its home page renders), so a name or hint changed there shows here too. The two that press real
 * keys with java.awt.Robot (Windows, WindowsE) get their desktop hint and a warning from below: in the browser
 * they press keys on a pretend Windows, here on the real one.
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

   /** One app in apps.json: a flat object (the file keeps one per line, with no nested objects). */
   private static final Pattern ENTRY = Pattern.compile("\\{[^{}]*\\}");
   /** A string field of it; numbers and booleans are not read here. */
   private static final Pattern FIELD = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

   /** The apps that press real keys: { desktop hint, warning }, written from their READMEs. */
   private static final Map<String, String[]> REAL_KEYS = Map.of(
      "Windows", new String[] {"Presses the Windows key once, then exits. It has no window.",
         "It presses the real Windows key as soon as it starts, which opens the Start menu."},
      "WindowsE", new String[] {
         "A keyboard of Windows shortcuts that presses the one you pick a chosen number of times: Win+E by default, which opens that many File Explorer windows.",
         "Clicking its DO IT!!! button presses real Windows shortcuts: 10 File Explorer windows by default."});

   private Catalog() {
   }

   /** Every app, in gallery order. */
   public static List<App> load(Path repoRoot) throws IOException {
      Path web = repoRoot.resolve("web");
      // Thumbnails are the site's static files (web/public/shots).
      return parse(new String(Files.readAllBytes(web.resolve("src/shared/apps.json")), StandardCharsets.UTF_8),
         web.resolve("public"));
   }

   /** The apps in apps.json; thumbnails resolve against the site folder. */
   static List<App> parse(String json, Path site) {
      List<App> apps = new ArrayList<>();
      Matcher entry = ENTRY.matcher(json);
      while (entry.find()) {
         Map<String, String> f = new HashMap<>();
         Matcher field = FIELD.matcher(entry.group());
         while (field.find()) f.put(field.group(1), unescape(field.group(2)));
         String id = f.get("id");
         String[] keys = REAL_KEYS.get(id);
         apps.add(new App(id, f.get("name"), f.getOrDefault("cat", "tool"), keys != null ? keys[0] : f.get("hint"),
            "original".equals(f.get("origin")), keys != null ? keys[1] : null, site.resolve(f.get("shot"))));
      }
      return apps;
   }

   /** Undoes a JSON string's backslash escapes, the four-digit unicode ones included. */
   static String unescape(String s) {
      StringBuilder out = new StringBuilder(s.length());
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c != '\\' || i + 1 == s.length()) {
            out.append(c);
            continue;
         }
         char e = s.charAt(++i);
         switch (e) {
            case 'n': out.append('\n'); break;
            case 't': out.append('\t'); break;
            case 'r': out.append('\r'); break;
            case 'b': out.append('\b'); break;
            case 'f': out.append('\f'); break;
            case 'u': out.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; break;
            default: out.append(e);
         }
      }
      return out.toString();
   }
}
