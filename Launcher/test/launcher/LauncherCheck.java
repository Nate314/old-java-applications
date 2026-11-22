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

      // Every app comes from web/src/shared/apps.json, in its order; each has a folder, a run.ps1 entry and a thumbnail.
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
      check(rubik.name.equals("Rubik's Cube 2D"), "name read: " + rubik.name);
      // The JSON text a\"b\\c, then an escaped e-acute, reads as a"b\c and the accented letter.
      check(Catalog.unescape("a\\\"b\\\\c\\" + "u00e9").equals("a\"b\\c" + (char) 0xe9), "JSON escapes undone");
      check(find(apps, "TankGame").original && !find(apps, "Minesweeper").original, "origin from \"origin\"");
      check(find(apps, "Windows").warning != null && find(apps, "WindowsE").warning != null
         && find(apps, "BibleDownloader").warning == null && find(apps, "DinoGame").warning == null, "Robot apps warn");
      check(find(apps, "Windows").hint.startsWith("An on-screen Windows key that stays on top of the real desktop"),
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
      // The newest change seen at a launch is kept in a file beside it, so the play record keeps its old format.
      check(again.seen("TankGame") == null, "nothing seen yet");
      again.launched("TankGame", now, "abc123");
      again.launched("TankGame", now);   // history not read yet: the one seen before stays
      Path seenFile = file.resolveSibling(file.getFileName().toString().replace(".txt", "-seen.txt"));
      check(new PlayLog(file).seen("TankGame").equals("abc123") && new PlayLog(file).count("TankGame") == 5, "seen kept");
      check(new String(Files.readAllBytes(file), StandardCharsets.UTF_8).contains("TankGame 5 "), "play record format unchanged");
      // Time played and the cards' order are kept in their own files beside it too.
      check(again.timeLine("TankGame") == null && again.order().isEmpty(), "no time or order yet");
      again.played("TankGame", 40 * 60_000L);
      again.played("TankGame", 32 * 60_000L);
      again.played("TankGame", 0);   // a start that never ran adds nothing
      again.order("most");
      PlayLog timed = new PlayLog(file);
      check(timed.total("TankGame") == 72 * 60_000L && timed.longest("TankGame") == 40 * 60_000L, "time kept");
      check("1 h 12 min played in all, longest 40 min".equals(timed.timeLine("TankGame")), timed.timeLine("TankGame"));
      check(timed.order().equals("most") && timed.count("TankGame") == 5, "order kept, play record untouched");
      check(PlayLog.duration(59_999).equals("under a minute") && PlayLog.duration(120 * 60_000L).equals("2 h"), "durations");
      Path timeFile = file.resolveSibling(file.getFileName().toString().replace(".txt", "-time.txt"));
      Files.write(timeFile, "junk\nClock 5 x\nTankGame 60000 60000\n".getBytes(StandardCharsets.UTF_8));
      check(new PlayLog(file).total("Clock") == 0 && "1 min played in all".equals(new PlayLog(file).timeLine("TankGame")),
         "damaged time lines skipped; one play shows no longest");
      Files.delete(timeFile);
      Files.delete(file.resolveSibling(file.getFileName().toString().replace(".txt", "-order.txt")));
      Files.delete(seenFile);
      Files.delete(file);

      // Picture glow: the box's size, rounded (corners clear, middle solid), dimmed below the picture, and the tone
      // keeps a colorful picture's hue while a gray one stays gray.
      java.awt.image.BufferedImage red = new java.awt.image.BufferedImage(40, 20, java.awt.image.BufferedImage.TYPE_INT_RGB);
      java.awt.Graphics2D rg = red.createGraphics();
      rg.setColor(new java.awt.Color(230, 40, 30));
      rg.fillRect(0, 0, 40, 20);
      rg.setColor(java.awt.Color.GRAY);
      rg.fillRect(0, 0, 10, 20);   // a gray strip counts for little
      rg.dispose();
      java.awt.Color tone = Thumbs.tone(red);
      check(tone.getRed() > 2 * tone.getGreen() && tone.getRed() > 2 * tone.getBlue() && tone.getRed() < 120, "red tone " + tone);
      java.awt.image.BufferedImage plain = new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB);
      for (int i = 0; i < 64; i++) plain.setRGB(i % 8, i / 8, 0x808080);
      java.awt.Color gray = Thumbs.tone(plain);
      check(gray.getRed() == gray.getGreen() && gray.getGreen() == gray.getBlue(), "a gray picture's tone is gray " + gray);
      java.awt.image.BufferedImage glow = Thumbs.glow(Thumbs.blur(red), tone, 120, 70, 16);
      check(glow.getWidth() == 120 && glow.getHeight() == 70, "glow fills the box");
      check((glow.getRGB(0, 0) >>> 24) == 0 && (glow.getRGB(60, 35) >>> 24) == 255, "glow is rounded and solid inside");
      check((glow.getRGB(100, 35) >> 16 & 255) < 230 && (glow.getRGB(100, 35) >> 16 & 255) > 120, "glow dimmed, still red");
      for (Catalog.App a : apps) new Thumbs().tone(a);   // every real picture gives a tone

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
      // And the other way: every home-folder dotfile an app's sources name travels in the bundle.
      for (Catalog.App a : apps) {
         try (java.util.stream.Stream<Path> walk = Files.walk(root.resolve(a.id).resolve("src"))) {
            for (Path j : (Iterable<Path>) walk.filter(f -> f.toString().endsWith(".java"))::iterator) {
               java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"(\\.[a-z0-9-]+\\.txt)\"")
                  .matcher(new String(Files.readAllBytes(j), StandardCharsets.UTF_8));
               while (m.find()) {
                  check(SaveBundle.FILES.getOrDefault(a.id, List.of()).contains(m.group(1)), a.id + "'s " + m.group(1) + " is in SaveBundle.FILES");
               }
            }
         }
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

      // Then: each app's story from git history, back to the commit the web site's Then pages use.
      String thenPom = new String(Files.readAllBytes(root.resolve("web/then/pom.xml")), StandardCharsets.UTF_8);
      check(thenPom.contains("<then.commit>" + History.THEN + "</then.commit>"), "same first restored commit as the web");
      List<String> ids = new java.util.ArrayList<>();
      for (Catalog.App a : apps) ids.add(a.id);
      History history = History.load(Git.in(root), ids);
      if (!history.available()) {
         System.out.println("LauncherCheck: no git history back to " + History.THEN + ", Then checks skipped");
      } else {
         for (String id : ids) check(history.of(id) != null && !history.of(id).about.isEmpty(), id + " existed then");
         History.Story mine = history.of("Minesweeper");
         check(mine.changed && mine.thenLines > 0 && mine.nowLines > mine.thenLines && !mine.changes.isEmpty(), "Minesweeper grew");
         check(mine.warning == null, "only apps that call Robot.keyPress warn, not ones with key listeners");
         check(history.of("DinoGame").warning != null && history.of("DinoGame").warning.contains("Space"), "the first DinoGame was a screen bot");
         check(history.of("Windows").changed && history.of("Windows").nowLines > history.of("Windows").thenLines, "Windows grew its on-screen key");
         check(LauncherWindow.historyText(find(apps, "Clock"), history.of("Clock"), utc).contains("## Changes since"), "history text");
         // The first restored copy plays with today's original art, not the images taken from other apps.
         Path thenDir = Files.createTempDirectory("launcher-then-art").resolve("then");
         ThenCopy.ensure(Git.in(root), root, thenDir);
         for (String art : ThenCopy.ART) {
            check(java.util.Arrays.equals(Files.readAllBytes(thenDir.resolve(art)), Files.readAllBytes(root.resolve(art))), art + " is today's art");
         }
         check(!Files.exists(thenDir.resolve("NathaNOS/assets/windowsLogo.png")) && Files.isRegularFile(thenDir.resolve("FlappyBird/src/flappyBird/Main.java")), "old art gone, old code kept");
         check(new String(Files.readAllBytes(thenDir.resolve(ThenCopy.MARKER)), StandardCharsets.UTF_8).equals(ThenCopy.stamp(root)), "marker");
         ThenCopy.deleteTree(thenDir.getParent());
      }
      check(History.sentences("One. Two. Three.", 2).equals("One. Two.") && History.sentences("One.", 2).equals("One."), "sentences");
      check(History.firstParagraph("# T\n\nA `b` **c** [d](e)\nmore.\n\nNext").equals("A b c d more."), "first paragraph");
      check(History.lines("rev:Clock/src/a/B.java:12\nrev:Clock/src/C.java:3\nrev:web/x.java:9\n").toString().equals("{Clock=15}"), "lines per app");
      Map<String, List<History.Change>> parsed = History.parseLog("@@h2 100 Two apps: both\n\nClock/README.md\nTankGame/src/A.java\nClock/src/B.java\n@@h1 50 One\n\nClock/x\n");
      check(parsed.get("Clock").size() == 2 && parsed.get("TankGame").size() == 1 && parsed.get("Clock").get(0).millis == 100_000L
         && parsed.get("Clock").get(0).hash.equals("h2") && parsed.get("Clock").get(0).subject.equals("Two apps: both"), "log per app");
      // What is new since the last launch: the changes above the one seen then, or by time when none was kept.
      History.Change c3 = parsed.get("Clock").get(0);
      List<History.Change> three = List.of(new History.Change("h3", 300_000L, "Clock: a third"), c3, parsed.get("Clock").get(1));
      History.Story clockStory = new History.Story(1, 2, true, three, "# Clock\n\nOld.", null);
      check(clockStory.newest().equals("h3"), "newest change");
      check(clockStory.since("h2", 999_999L).size() == 1 && clockStory.since("h3", 0).isEmpty(), "since the seen change");
      check(clockStory.since(null, 60_000L).size() == 2 && clockStory.since("gone", 400_000L).isEmpty(), "since by time");
      Catalog.App clockApp = find(apps, "Clock");
      check(LauncherWindow.tidy(clockApp, "Clock: chimes every quarter").equals("Chimes every quarter")
         && LauncherWindow.tidy(clockApp, "Clock README: words").equals("Words")
         && LauncherWindow.tidy(find(apps, "RubiksCube2D"), "Rubik's Cube: x").equals("Rubik's Cube: x")
         && LauncherWindow.tidy(find(apps, "WindowsE"), "Windows+E: y").equals("Y")
         && LauncherWindow.tidy(find(apps, "Windows"), "WindowsE: y").equals("WindowsE: y")
         && LauncherWindow.tidy(clockApp, "Run the site: z").equals("Run the site: z"), "subjects tidied");
      List<History.Change> four = List.of(new History.Change("h4", 3 * day, "Clock: a fourth"), three.get(0), three.get(1), three.get(2));
      String fresh = LauncherWindow.whatsNew(clockApp, four, 3 * day + 60_000L, utc);
      check(fresh.equals("4 things new since you last played:\n\u2022 A fourth (today)\n\u2022 A third (3 days ago)\n"
         + "2 more in History (Alt+H)."), fresh);
      check(LauncherWindow.whatsNew(clockApp, List.of(three.get(0)), day, utc).equals("New since you last played:\n\u2022 A third (yesterday)"),
         "one change, with its day");
      check(LauncherWindow.whatsNew(clockApp, List.of(), 0, utc) == null, "nothing new, no word");
      // Housekeeping is not news: the same subjects the site's What's new folds away, by the same rule.
      check(!three.get(0).quiet && new History.Change("x", 0, "Clock README: words").quiet
         && new History.Change("x", 0, "Refactor: one helper").quiet && new History.Change("x", 0, "ClockCheck: alarm").quiet
         && !new History.Change("x", 0, "Clock: a world map").quiet, "quiet subjects");
      String kind = new String(Files.readAllBytes(root.resolve("web/src/runtime/lazy/news-kind.ts")), StandardCharsets.UTF_8);
      kind = kind.substring(kind.indexOf("new RegExp(["), kind.indexOf("].join")).replaceAll("//[^\n]*", "");
      java.util.regex.Matcher lit = java.util.regex.Pattern.compile("'((?:[^'\\\\]|\\\\.)*)'|\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(kind);
      List<String> parts = new java.util.ArrayList<>();
      while (lit.find()) parts.add((lit.group(1) != null ? lit.group(1) : lit.group(2)).replace("\\\\", "\\"));
      check(String.join("|", parts).equals(History.QUIET.pattern()), "the launcher's quiet rule matches news-kind.ts: " + parts);
      check(Runner.homeOption("-Xmx1g", Paths.get("/h o")).endsWith("\"-Duser.home=" + Paths.get("/h o").toAbsolutePath() + "\"")
         && Runner.homeOption(null, Paths.get("/h")).startsWith("\"-D"), "then home option");
      // The unpacker refuses entries that would land outside its folder.
      Path unpack = Files.createTempDirectory("launcher-then");
      java.io.ByteArrayOutputStream zipBytes = new java.io.ByteArrayOutputStream();
      try (java.util.zip.ZipOutputStream z = new java.util.zip.ZipOutputStream(zipBytes)) {
         z.putNextEntry(new java.util.zip.ZipEntry("App/ok.txt"));
         z.write('x');
         z.putNextEntry(new java.util.zip.ZipEntry("../evil.txt"));
         z.write('x');
      }
      boolean unsafe = false;
      try { ThenCopy.unzip(new java.io.ByteArrayInputStream(zipBytes.toByteArray()), unpack); } catch (java.io.IOException e) { unsafe = true; }
      check(unsafe && Files.isRegularFile(unpack.resolve("App/ok.txt")) && !Files.exists(unpack.resolveSibling("evil.txt")), "unsafe zip refused");
      Files.delete(unpack.resolve("App/ok.txt"));
      Files.delete(unpack.resolve("App"));
      Files.delete(unpack);

      // The art the Then copy swaps in is the art web/art.ps1 renders, so the two lists cannot drift apart.
      String artScript = new String(Files.readAllBytes(root.resolve("web/art.ps1")), StandardCharsets.UTF_8);
      for (String art : ThenCopy.ART) check(artScript.contains("Out = '" + art + "'"), art + " is rendered by web/art.ps1");

      // Card pitch: the hint wrapped at words into at most two lines that fit, the last cut with "..." when more is left.
      java.awt.FontMetrics fm = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB)
         .createGraphics().getFontMetrics(Ui.font(java.awt.Font.PLAIN, 12f));
      for (Catalog.App a : apps) {
         List<String> two = LauncherWindow.lines(fm, a.hint, 160, 2);
         check(two.size() == 2 && two.stream().allMatch(l -> fm.stringWidth(l) <= 160) && two.get(1).endsWith("...")
            && a.hint.startsWith(two.get(0)), a.id + " card pitch is two lines that fit, from the start of its hint");
      }
      check(LauncherWindow.lines(fm, "Short hint.", 160, 2).equals(List.of("Short hint.")), "a short pitch is one line, uncut");
      check(LauncherWindow.lines(fm, "Unbreakablewordthatiswayfartoolongforacard rest", 60, 2).size() <= 2, "a long word is cut, not looped on");

      // Start menu: every app but the two that press real keys gets a shortcut with its own file name, which runs
      // run.ps1 by its full path; the icon file is one 256 px PNG; Remove deletes the shortcuts, icons and folder.
      Path menu = Files.createTempDirectory("startmenu");
      StartMenu start = new StartMenu("powershell", Paths.get("."), menu.resolve(StartMenu.FOLDER), menu.resolve("icons"));
      java.util.Set<Path> links = new java.util.HashSet<>();
      for (Catalog.App a : apps) {
         check(StartMenu.allowed(a) == !a.pressesKeys(), a.id + " Start menu shortcut only without real keys");
         if (StartMenu.allowed(a)) links.add(start.link(a));
      }
      check(links.size() == 18, "18 distinct shortcut names");
      check(start.link(find(apps, "RubiksCube2D")).getFileName().toString().equals("Rubik's Cube 2D.lnk"), "shortcut named after the app");
      String startArgs = StartMenu.arguments(root, "Clock");
      check(startArgs.contains("-File \"" + root.resolve("run.ps1") + "\"") && startArgs.endsWith(" old Clock"), "shortcut runs run.ps1 old: " + startArgs);
      byte[] ico = StartMenu.ico(StartMenu.icon(new java.awt.image.BufferedImage(427, 456, java.awt.image.BufferedImage.TYPE_INT_ARGB), Ui.ACCENT));
      java.nio.ByteBuffer icoHead = java.nio.ByteBuffer.wrap(ico).order(java.nio.ByteOrder.LITTLE_ENDIAN);
      check(icoHead.getShort(2) == 1 && icoHead.getShort(4) == 1 && icoHead.getInt(14) == ico.length - 22 && icoHead.getInt(18) == 22, "ico header");
      java.awt.image.BufferedImage iconPng = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(ico, 22, ico.length - 22));
      check(iconPng.getWidth() == 256 && iconPng.getHeight() == 256, "ico holds a 256 px PNG");
      Catalog.App clock = find(apps, "Clock");
      Files.createDirectories(start.folder);
      Files.createDirectories(menu.resolve("icons"));
      Files.write(start.link(clock), new byte[] {1});
      Files.write(menu.resolve("icons/Clock.ico"), ico);
      check(start.has(clock) && !start.has(find(apps, "TankGame")), "has a shortcut");
      start.remove(List.of(clock));
      check(!start.has(clock) && !Files.exists(start.folder) && !Files.exists(menu.resolve("icons/Clock.ico")), "remove cleans up");
      Files.delete(menu.resolve("icons"));
      Files.delete(menu);

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
