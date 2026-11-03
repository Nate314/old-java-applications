package launcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Each app's story since it was first restored, read from the repo's git history: how many lines of Java it had
 * then and has now, the commits that changed its folder since, and its README as it was then. "First restored"
 * is the commit the web site's Then pages are built from (web/then/pom.xml), so both mean the same version.
 */
public final class History {
   /** master after #9: the recovered apps with the light bug fixes of #6, before the reworks from #10 on. */
   public static final String THEN = "5a28e4608fc254e9cc075768a598a965b7fe2d24";

   /** One commit that changed an app's folder. */
   public static final class Change {
      public final long millis;
      public final String subject;

      Change(long millis, String subject) {
         this.millis = millis;
         this.subject = subject;
      }
   }

   /** One app's then and now. */
   public static final class Story {
      public final int thenLines;
      public final int nowLines;
      public final boolean changed;        // its sources differ from the first restored ones
      public final List<Change> changes;   // newest first
      public final String readme;          // its README as first restored
      public final String about;           // that README's first paragraph, as plain text
      public final String warning;         // what the first restored version does to the real desktop, or null

      Story(int thenLines, int nowLines, boolean changed, List<Change> changes, String readme, String warning) {
         this.thenLines = thenLines;
         this.nowLines = nowLines;
         this.changed = changed;
         this.changes = changes;
         this.readme = readme;
         this.about = firstParagraph(readme);
         this.warning = warning;
      }
   }

   static final History NONE = new History(Collections.emptyMap());

   private final Map<String, Story> stories;

   private History(Map<String, Story> stories) {
      this.stories = stories;
   }

   /** False when there is no git, or no history going back to the first restored commit (a shallow clone). */
   public boolean available() {
      return !stories.isEmpty();
   }

   /** The app's story, or null for an app that did not exist when first restored (or without history). */
   public Story of(String id) {
      return stories.get(id);
   }

   /** Reads every app's story; with no usable git it returns a history that is not available. */
   public static History load(Git git, Collection<String> ids) {
      try {
         git.run("cat-file", "-e", THEN + "^{commit}");
         Map<String, Integer> thenLines = lines(git.run("grep", "-c", "-e", "^", THEN, "--", "*.java"));
         Map<String, Integer> nowLines = lines(git.run("grep", "-c", "-e", "^", "HEAD", "--", "*.java"));
         List<String> srcs = new ArrayList<>(List.of("diff", "--name-only", THEN, "HEAD", "--"));
         for (String id : ids) srcs.add(id + "/src");
         Set<String> changed = folders(git.run(srcs.toArray(new String[0])).split("\n"));
         Set<String> pressKeys = folders(git.run("grep", "-l", "-F", "-e", ".keyPress(", THEN, "--", "*.java")
            .replace(THEN + ":", "").split("\n"));
         Map<String, List<Change>> changes = changes(git, ids);

         Map<String, Story> out = new LinkedHashMap<>();
         for (String id : ids) {
            String readme;
            try {
               readme = git.run("show", THEN + ":" + id + "/README.md");
            } catch (IOException missing) {
               continue;   // not an app yet when first restored (the launcher itself)
            }
            String warning = pressKeys.contains(id) ? runningNote(readme) : null;
            out.put(id, new Story(thenLines.getOrDefault(id, 0), nowLines.getOrDefault(id, 0), changed.contains(id),
               changes.getOrDefault(id, Collections.emptyList()), readme, warning));
         }
         return new History(out);
      } catch (IOException e) {
         return NONE;
      }
   }

   /** Lines of Java per app folder, from {@code git grep -c} output ({@code rev:App/src/x.java:123}). */
   static Map<String, Integer> lines(String grep) {
      Map<String, Integer> out = new HashMap<>();
      for (String line : grep.split("\n")) {
         int a = line.indexOf(':');
         int b = line.lastIndexOf(':');
         if (a < 0 || b <= a) continue;
         String path = line.substring(a + 1, b);
         int slash = path.indexOf('/');
         if (slash < 0 || !path.startsWith("/src/", slash)) continue;
         try {
            out.merge(path.substring(0, slash), Integer.parseInt(line.substring(b + 1).trim()), Integer::sum);
         } catch (NumberFormatException ignored) {
            // not a count line
         }
      }
      return out;
   }

   private static Set<String> folders(String[] paths) {
      Set<String> out = new HashSet<>();
      for (String p : paths) {
         int slash = p.indexOf('/');
         if (slash > 0) out.add(p.substring(0, slash));
      }
      return out;
   }

   /** The commits since first restored that touched each app's folder, newest first. Merges are left out. */
   private static Map<String, List<Change>> changes(Git git, Collection<String> ids) throws IOException {
      List<String> args = new ArrayList<>(List.of("log", "--no-merges", "--format=@@%ct %s", "--name-only",
         THEN + "..HEAD", "--"));
      args.addAll(ids);
      return parseLog(git.run(args.toArray(new String[0])));
   }

   static Map<String, List<Change>> parseLog(String log) {
      Map<String, List<Change>> out = new HashMap<>();
      Change current = null;
      Set<String> seen = new HashSet<>();
      for (String line : log.split("\n")) {
         line = line.trim();
         if (line.startsWith("@@")) {
            int space = line.indexOf(' ');
            try {
               current = new Change(Long.parseLong(line.substring(2, space)) * 1000L, line.substring(space + 1).trim());
            } catch (RuntimeException e) {
               current = null;
            }
            seen.clear();
         } else if (current != null && !line.isEmpty()) {
            int slash = line.indexOf('/');
            if (slash > 0 && seen.add(line.substring(0, slash))) {
               out.computeIfAbsent(line.substring(0, slash), k -> new ArrayList<>()).add(current);
            }
         }
      }
      return out;
   }

   /** The README's first paragraph after its heading, with the Markdown marks taken out. */
   static String firstParagraph(String md) {
      StringBuilder p = new StringBuilder();
      for (String line : md.replace("\r", "").split("\n")) {
         if (line.startsWith("#")) continue;
         if (line.trim().isEmpty()) {
            if (p.length() > 0) break;
            continue;
         }
         p.append(p.length() > 0 ? " " : "").append(line.trim());
      }
      return plain(p.toString());
   }

   /** The last paragraph of the README's Running section: what the app does when started, for the warning. */
   static String runningNote(String md) {
      String note = null;
      boolean in = false;
      for (String line : md.replace("\r", "").split("\n")) {
         if (line.startsWith("## ")) {
            if (in) break;
            in = line.trim().equals("## Running");
         } else if (in && !line.trim().isEmpty() && !line.startsWith("From the repo root")) {
            note = line.trim();
         }
      }
      return plain(note != null ? note : "As first restored, it presses real keys with java.awt.Robot.");
   }

   /** The text's first {@code n} sentences (a sentence ends at ". "). */
   static String sentences(String text, int n) {
      int end = -1;
      for (int i = 0; i < n; i++) {
         end = text.indexOf(". ", end + 1);
         if (end < 0) return text;
      }
      return text.substring(0, end + 1);
   }

   static String plain(String s) {
      return s.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1").replace("**", "").replace("`", "");
   }
}
