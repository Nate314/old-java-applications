package launcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** Self-check for the catalog, the play log and the README view. Run from the repo root with java -ea. */
public class LauncherCheck {
   public static void main(String[] args) throws Exception {
      Path root = Paths.get("").toAbsolutePath();
      String runScript = new String(Files.readAllBytes(root.resolve("run.ps1")), StandardCharsets.UTF_8);

      // Every app comes from the gallery, in its order; each has a folder, a run.ps1 entry and a thumbnail.
      List<Catalog.App> apps = Catalog.load(root);
      check(apps.size() == 20, "expected 20 apps, got " + apps.size());
      check(apps.get(0).id.equals("Minesweeper"), "gallery order: Minesweeper first");
      int keys = 0;
      for (Catalog.App a : apps) {
         check(Files.isRegularFile(root.resolve(a.id).resolve("README.md")), a.id + " has a README");
         check(runScript.contains("    " + a.id + " "), a.id + " is in run.ps1");
         check(!a.hint.isEmpty() && !a.name.isEmpty(), a.id + " has a name and a hint");
         check(a.hint.indexOf('&') < 0 || a.hint.contains("& "), a.id + " hint is unescaped");
         check(a.category.matches("game|music|toy|tool"), a.id + " category " + a.category);
         if (a.pressesKeys()) keys++;
         check(Files.isRegularFile(a.thumbnail), a.id + " thumbnail " + a.thumbnail);
      }
      check(keys == 2, "two apps press real keys");
      Catalog.App rubik = find(apps, "RubiksCube2D");
      check(rubik.name.equals("Rubik's Cube 2D"), "entities unescaped: " + rubik.name);
      check(find(apps, "TankGame").original && !find(apps, "Minesweeper").original, "origin from data-origin");
      check(find(apps, "Windows").warning != null && find(apps, "WindowsE").warning != null
         && find(apps, "BibleDownloader").warning == null && find(apps, "DinoGame").warning == null, "Robot apps warn");
      check(find(apps, "Windows").hint.startsWith("Presses the Windows key once, then exits"),
         "the Robot apps describe the real desktop, not the browser's pretend Windows: " + find(apps, "Windows").hint);

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

      // Saves file: every listed save file is one an app's sources name; a round trip through the file keeps
      // bytes as they were; only listed files are written; a file from the web's Download reads the same.
      for (Map.Entry<String, List<String>> app : SaveBundle.FILES.entrySet()) {
         StringBuilder src = new StringBuilder();
         try (java.util.stream.Stream<Path> walk = Files.walk(root.resolve(app.getKey()).resolve("src"))) {
            for (Path j : (Iterable<Path>) walk.filter(f -> f.toString().endsWith(".java"))::iterator) {
               src.append(new String(Files.readAllBytes(j), StandardCharsets.UTF_8));
            }
         }
         for (String name : app.getValue()) check(src.indexOf("\"" + name + "\"") >= 0, app.getKey() + " saves " + name);
      }
      Path home = Files.createTempDirectory("launcher-saves");
      Path other = Files.createTempDirectory("launcher-saves-to");
      String tricky = "BEGINNER=53600\nname=Ren\u00e9e \"q\" \\ tab\t\u00ff\u0001\n";
      Files.write(home.resolve(".minesweeper-best.txt"), tricky.getBytes(StandardCharsets.ISO_8859_1));
      Files.write(home.resolve(".clock.txt"), "tab=2\n".getBytes(StandardCharsets.ISO_8859_1));
      Files.write(home.resolve(".bashrc"), "keep".getBytes(StandardCharsets.ISO_8859_1));
      PlayLog played = new PlayLog(home.resolve(".oldjava-launcher.txt"));
      played.launched("Minesweeper", 1000L);
      SaveBundle out = SaveBundle.collect(home, played, java.time.Instant.ofEpochMilli(5000L));
      check(out.saves.keySet().equals(new java.util.HashSet<>(java.util.Arrays.asList("Minesweeper", "Clock"))), "collected " + out.saves.keySet());
      check(out.played.get("Minesweeper") == 1000L && out.played.size() == 1, "played from the launcher record");
      String json = out.toJson();
      check(json.chars().allMatch(c -> c < 0x80), "the file is plain ASCII");
      SaveBundle back = SaveBundle.parse(json);
      check(back.saves.get("Minesweeper").get("/.minesweeper-best.txt").equals(tricky), "round trip keeps every byte");
      check(back.differs("Minesweeper", other) && !back.differs("Minesweeper", home), "differs");
      List<String> written = back.apply(other, java.util.Arrays.asList("Minesweeper"));
      check(written.equals(java.util.Arrays.asList(".minesweeper-best.txt")), "only the chosen app: " + written);
      check(java.util.Arrays.equals(Files.readAllBytes(other.resolve(".minesweeper-best.txt")), Files.readAllBytes(home.resolve(".minesweeper-best.txt"))), "same bytes");
      // As the web's Download writes it: UTF-8 text, one char per byte, with Then saves, unlisted and hostile paths.
      String web = "{\"oldjava\":\"saves\",\"v\":1,\"at\":\"2026-11-02T10:00:00.000Z\",\"saves\":{"
         + "\"FlappyBird\":{\"/.flappybird-highscore.txt\":\"17\",\"/../../evil.txt\":\"x\",\"/.bashrc\":\"x\"},"
         + "\"FlappyBird.then\":{\"/.flappybird-highscore.txt\":\"3\"},\"StockChecker\":{\"/myStocks.txt\":\"AAPL\"},"
         + "\"Minesweeper\":{\"/.minesweeper-best.txt\":\"\u00e9\u00ff\"}},\"played\":{\"FlappyBird\":1761000000000}}";
      SaveBundle fromWeb = SaveBundle.parse(web);
      check(fromWeb.usable().toString().equals("{FlappyBird=[.flappybird-highscore.txt], Minesweeper=[.minesweeper-best.txt]}"), "usable " + fromWeb.usable());
      check(fromWeb.played.get("FlappyBird") == 1761000000000L, "web played");
      fromWeb.apply(other, java.util.Arrays.asList("FlappyBird", "StockChecker", "Minesweeper"));
      check(new String(Files.readAllBytes(other.resolve(".flappybird-highscore.txt")), StandardCharsets.ISO_8859_1).equals("17"), "web file written");
      check(java.util.Arrays.equals(Files.readAllBytes(other.resolve(".minesweeper-best.txt")), new byte[] {(byte) 0xe9, (byte) 0xff}), "bytes from the web");
      check(!Files.exists(other.resolve(".bashrc")) && !Files.exists(other.resolve("myStocks.txt"))
         && !Files.exists(other.getParent().resolve("evil.txt")), "unlisted files are never written");
      boolean refused = false;
      try { SaveBundle.parse("{\"oldjava\":\"other\"}"); } catch (IllegalArgumentException e) { refused = true; }
      check(refused, "other JSON refused");
      for (Path d : new Path[] {home, other}) {
         try (java.util.stream.Stream<Path> files = Files.list(d)) { for (Path f : (Iterable<Path>) files::iterator) Files.delete(f); }
         Files.delete(d);
      }

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
