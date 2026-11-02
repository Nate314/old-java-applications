package launcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.List;

/** Self-check for the catalog, the play log and the README view. Run from the repo root with java -ea. */
public class LauncherCheck {
   public static void main(String[] args) throws Exception {
      Path root = Paths.get("").toAbsolutePath();
      String runScript = new String(Files.readAllBytes(root.resolve("run.ps1")), StandardCharsets.UTF_8);

      // Every app: 16 from the gallery, in its order, and the 4 desktop-only ones; each has a folder and a run.ps1 entry.
      List<Catalog.App> apps = Catalog.load(root);
      check(apps.size() == 20, "expected 20 apps, got " + apps.size());
      check(apps.get(0).id.equals("Minesweeper"), "gallery order: Minesweeper first");
      int desktop = 0;
      for (Catalog.App a : apps) {
         check(Files.isRegularFile(root.resolve(a.id).resolve("README.md")), a.id + " has a README");
         check(runScript.contains("    " + a.id + " "), a.id + " is in run.ps1");
         check(!a.hint.isEmpty() && !a.name.isEmpty(), a.id + " has a name and a hint");
         check(a.hint.indexOf('&') < 0 || a.hint.contains("& "), a.id + " hint is unescaped");
         check(a.category.matches("game|music|toy|tool"), a.id + " category " + a.category);
         if (a.desktopOnly) desktop++;
         else check(Files.isRegularFile(a.thumbnail), a.id + " thumbnail " + a.thumbnail);
      }
      check(desktop == 4, "four desktop-only apps");
      Catalog.App rubik = find(apps, "RubiksCube2D");
      check(rubik.name.equals("Rubik's Cube 2D"), "entities unescaped: " + rubik.name);
      check(find(apps, "TankGame").original && !find(apps, "Minesweeper").original, "origin from data-origin");
      check(find(apps, "Windows").warning != null && find(apps, "DinoGame").warning != null
         && find(apps, "WindowsE").warning != null && find(apps, "BibleDownloader").warning == null, "Robot apps warn");

      // Search looks at the name, id, category and hint, ignoring case and spaces around it.
      check(find(apps, "FlappyBird").matches("  FLAP "), "search by name");
      check(find(apps, "Clock").matches("stopwatch"), "search by hint");
      check(find(apps, "TankGame").matches("game"), "search by category");
      check(!find(apps, "Clock").matches("tank"), "no false match");

      // The play log counts launches, keeps the latest time and survives a reload; damaged lines are skipped.
      Path file = Files.createTempFile("launcher", ".txt");
      Files.write(file, "junk line\nClock x 5\n".getBytes(StandardCharsets.UTF_8));
      ZoneId utc = ZoneId.of("UTC");
      long day = 86_400_000L;
      long now = 20_000 * day + 3_600_000L;
      PlayLog log = new PlayLog(file);
      check(log.count("Clock") == 0, "damaged line skipped");
      check(log.describe("Clock", now, utc).startsWith("New"), "new app");
      log.launched("Clock", now - 3 * day);
      log.launched("Clock", now - day);
      log.launched("TankGame", now);
      PlayLog again = new PlayLog(file);
      check(again.count("Clock") == 2 && again.last("Clock") == now - day, "reloaded");
      check(again.describe("Clock", now, utc).equals("Played twice, last yesterday"), again.describe("Clock", now, utc));
      check(again.describe("TankGame", now, utc).equals("Played once, last today"), again.describe("TankGame", now, utc));
      again.launched("TankGame", now);
      again.launched("TankGame", now);
      check(again.describe("TankGame", now + 4 * day, utc).equals("Played 3 times, last 4 days ago"), "days ago");
      Files.delete(file);

      // README view: headings, lists, code and inline marks, with HTML escaped.
      String html = ReadmeView.toHtml("# Title\n\n- **Bold** item with `code`\n- <b>raw</b>\n\n```\nx < y\n```\n| a | b |\n|---|---|\n");
      check(html.contains("<h1") && html.contains("Title</h1>"), "heading");
      check(html.contains("<ul") && html.contains("<b>Bold</b>") && html.contains("</ul>"), "list and bold");
      check(html.contains("&lt;b&gt;raw"), "raw HTML escaped");
      check(html.contains("x &lt; y\n</pre>"), "code block");
      check(html.contains("a  /  b") && !html.contains("---"), "table as text");

      System.out.println("LauncherCheck: all checks passed");
   }

   private static Catalog.App find(List<Catalog.App> apps, String id) {
      for (Catalog.App a : apps) if (a.id.equals(id)) return a;
      throw new AssertionError("no app " + id);
   }

   private static void check(boolean ok, String what) {
      if (!ok) throw new AssertionError(what);
   }
}
