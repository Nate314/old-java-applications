package launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The saves file shared with the web site's "Your records" (web/site/saves.js): what the apps keep in the home
 * folder, so records can move between the desktop and a browser.
 *
 * <pre>{ "oldjava": "saves", "v": 1, "at": ISO time, "saves": { "App": { "/.file.txt": contents } },
 *   "played": { "App": epoch ms } }</pre>
 *
 * Paths are the web's, where the home folder is "/". Contents are one character per byte (ISO-8859-1), as the
 * web keeps them. Only the files listed in {@link #FILES} are read or written here, so a file can never write
 * anywhere but those names in the home folder.
 */
public final class SaveBundle {
   /** The files each app keeps in the home folder (the names the apps' sources use). */
   static final Map<String, List<String>> FILES = new LinkedHashMap<>();
   static {
      FILES.put("Minesweeper", List.of(".minesweeper-best.txt"));
      FILES.put("FlappyBird", List.of(".flappybird-highscore.txt", ".flappybird-today.txt"));
      FILES.put("DinoGame", List.of(".dinogame.txt"));
      FILES.put("UltimateTicTacToe", List.of(".ultimatetictactoe-record.txt"));
      FILES.put("TankGame", List.of(".tankgame-record.txt"));
      FILES.put("SynthesizerKeyboard", List.of(".synthesizer-songs.txt", ".synthesizer-tune.txt"));
      FILES.put("RubiksCube2D", List.of(".rubikscube-levels.txt"));
      FILES.put("MusicTheoryExpert", List.of(".musictheory-drills.txt"));
      FILES.put("NumberGuess", List.of(".numberguess-record.txt"));
      FILES.put("SpriteFloatyStars", List.of(".spritefloatystars-nights.txt"));
      FILES.put("GroupCreator", List.of(".groupcreator-class.txt"));
      FILES.put("Clock", List.of(".clock.txt"));
      FILES.put("ScreenSaver", List.of(".screensaver.txt"));
      FILES.put("StockChecker", List.of(".stockchecker-desk.txt"));
      FILES.put("BibleDownloader", List.of(".bibledownloader.txt"));
   }

   /** App -> (web path -> contents), in a stable order. */
   final Map<String, Map<String, String>> saves = new LinkedHashMap<>();
   /** App -> a time it was played, epoch ms. */
   final Map<String, Long> played = new LinkedHashMap<>();
   String at = "";

   /** The desktop's saves: every listed file present in {@code home}, and the launcher's play record. */
   static SaveBundle collect(Path home, PlayLog log, Instant now) {
      SaveBundle b = new SaveBundle();
      b.at = now.toString();
      for (Map.Entry<String, List<String>> app : FILES.entrySet()) {
         for (String name : app.getValue()) {
            Path file = home.resolve(name);
            if (!Files.isRegularFile(file)) continue;
            try {
               b.saves.computeIfAbsent(app.getKey(), k -> new LinkedHashMap<>())
                  .put("/" + name, new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1));
            } catch (IOException e) {
               System.err.println("Could not read " + file + ": " + e.getMessage());
            }
         }
      }
      if (log != null) {
         for (String id : FILES.keySet()) if (log.count(id) > 0) b.played.put(id, log.last(id));
      }
      return b;
   }

   /** The files of this bundle the desktop apps use, by app: the web's "Then" saves and unknown files are left out. */
   Map<String, List<String>> usable() {
      Map<String, List<String>> out = new LinkedHashMap<>();
      for (Map.Entry<String, Map<String, String>> app : saves.entrySet()) {
         List<String> known = FILES.getOrDefault(app.getKey(), Collections.emptyList());
         for (String path : app.getValue().keySet()) {
            String name = path.startsWith("/") ? path.substring(1) : "";
            if (known.contains(name)) out.computeIfAbsent(app.getKey(), k -> new ArrayList<>()).add(name);
         }
      }
      return out;
   }

   /** Whether this bundle's copy of an app's files differs from the ones in {@code home}. */
   boolean differs(String app, Path home) {
      for (String name : usable().getOrDefault(app, Collections.emptyList())) {
         Path file = home.resolve(name);
         try {
            String here = Files.isRegularFile(file) ? new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1) : null;
            if (!saves.get(app).get("/" + name).equals(here)) return true;
         } catch (IOException e) {
            return true;
         }
      }
      return false;
   }

   /** Writes the chosen apps' files into {@code home}, replacing those there. Returns the names written. */
   List<String> apply(Path home, List<String> apps) throws IOException {
      List<String> written = new ArrayList<>();
      Map<String, List<String>> usable = usable();
      for (String app : apps) {
         for (String name : usable.getOrDefault(app, Collections.emptyList())) {
            Files.write(home.resolve(name), saves.get(app).get("/" + name).getBytes(StandardCharsets.ISO_8859_1));
            written.add(name);
         }
      }
      return written;
   }

   // ---- JSON ----

   String toJson() {
      StringBuilder out = new StringBuilder("{\n \"oldjava\": \"saves\",\n \"v\": 1,\n \"at\": ");
      quote(out, at);
      out.append(",\n \"saves\": {");
      String sep = "\n  ";
      for (Map.Entry<String, Map<String, String>> app : saves.entrySet()) {
         out.append(sep);
         quote(out, app.getKey());
         out.append(": {");
         String inner = "\n   ";
         for (Map.Entry<String, String> f : app.getValue().entrySet()) {
            out.append(inner);
            quote(out, f.getKey());
            out.append(": ");
            quote(out, f.getValue());
            inner = ",\n   ";
         }
         out.append("\n  }");
         sep = ",\n  ";
      }
      out.append("\n },\n \"played\": {");
      sep = "\n  ";
      for (Map.Entry<String, Long> p : played.entrySet()) {
         out.append(sep);
         quote(out, p.getKey());
         out.append(": ").append(p.getValue());
         sep = ",\n  ";
      }
      return out.append("\n }\n}\n").toString();
   }

   private static void quote(StringBuilder out, String s) {
      out.append('"');
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (c == '"' || c == '\\') out.append('\\').append(c);
         else if (c == '\n') out.append("\\n");
         else if (c < 0x20 || c >= 0x7f) out.append(String.format("\\u%04x", (int) c));
         else out.append(c);
      }
      out.append('"');
   }

   /** Reads a saves file (as written here or by the web site's Download). */
   static SaveBundle parse(String json) {
      Object top = new Reader(json).document();
      if (!(top instanceof Map) || !"saves".equals(((Map<?, ?>) top).get("oldjava"))) {
         throw new IllegalArgumentException("not an Old Java saves file");
      }
      Map<?, ?> root = (Map<?, ?>) top;
      SaveBundle b = new SaveBundle();
      if (root.get("at") instanceof String) b.at = (String) root.get("at");
      if (root.get("saves") instanceof Map) {
         for (Map.Entry<?, ?> app : ((Map<?, ?>) root.get("saves")).entrySet()) {
            if (!(app.getValue() instanceof Map)) continue;
            for (Map.Entry<?, ?> f : ((Map<?, ?>) app.getValue()).entrySet()) {
               if (!(f.getValue() instanceof String) || ((String) f.getValue()).chars().anyMatch(c -> c > 0xff)) continue;
               b.saves.computeIfAbsent((String) app.getKey(), k -> new LinkedHashMap<>()).put((String) f.getKey(), (String) f.getValue());
            }
         }
      }
      if (root.get("played") instanceof Map) {
         for (Map.Entry<?, ?> p : ((Map<?, ?>) root.get("played")).entrySet()) {
            if (p.getValue() instanceof Double) b.played.put((String) p.getKey(), ((Double) p.getValue()).longValue());
         }
      }
      return b;
   }

   /** A small JSON reader: objects become maps, arrays lists, numbers doubles. */
   private static final class Reader {
      private final String s;
      private int i;

      Reader(String s) {
         this.s = s;
      }

      Object document() {
         Object v = value();
         space();
         if (i != s.length()) throw error("text after the end");
         return v;
      }

      private Object value() {
         space();
         if (i >= s.length()) throw error("unexpected end");
         char c = s.charAt(i);
         if (c == '{') {
            i++;
            Map<String, Object> map = new LinkedHashMap<>();
            space();
            if (peek('}')) return map;
            do {
               space();
               String key = string();
               space();
               expect(':');
               map.put(key, value());
               space();
            } while (peek(','));
            expect('}');
            return map;
         }
         if (c == '[') {
            i++;
            List<Object> list = new ArrayList<>();
            space();
            if (peek(']')) return list;
            do {
               list.add(value());
               space();
            } while (peek(','));
            expect(']');
            return list;
         }
         if (c == '"') return string();
         for (String word : Arrays.asList("true", "false", "null")) {
            if (s.startsWith(word, i)) {
               i += word.length();
               return word.equals("null") ? null : Boolean.valueOf(word);
            }
         }
         int start = i;
         while (i < s.length() && "+-.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
         try {
            return Double.valueOf(s.substring(start, i));
         } catch (NumberFormatException e) {
            throw error("unexpected character");
         }
      }

      private String string() {
         expect('"');
         StringBuilder out = new StringBuilder();
         while (true) {
            if (i >= s.length()) throw error("unfinished string");
            char c = s.charAt(i++);
            if (c == '"') return out.toString();
            if (c != '\\') {
               out.append(c);
               continue;
            }
            char e = s.charAt(i++);
            switch (e) {
               case 'n': out.append('\n'); break;
               case 't': out.append('\t'); break;
               case 'r': out.append('\r'); break;
               case 'b': out.append('\b'); break;
               case 'f': out.append('\f'); break;
               case 'u': out.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
               default: out.append(e);
            }
         }
      }

      private void space() {
         while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
      }

      private boolean peek(char c) {
         if (i < s.length() && s.charAt(i) == c) {
            i++;
            return true;
         }
         return false;
      }

      private void expect(char c) {
         if (!peek(c)) throw error("expected " + c);
      }

      private IllegalArgumentException error(String what) {
         return new IllegalArgumentException("not a JSON file (" + what + " at character " + i + ")");
      }
   }
}
