package audiobibledownloader;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/** Self-check for the book table, the chapter parser, the page layout and the shelf file. Runs offline; use java -ea. */
public class ReaderCheck {
   public static void main(String[] args) throws Exception {
      // 66 books and 1,189 chapters; ordinals round-trip.
      check(Books.IDS.length == 66 && Books.NAMES.length == 66 && Books.CHAPTERS.length == 66, "66 books");
      check(Books.TOTAL == 1189, "1,189 chapters");
      for (int o = 0; o < Books.TOTAL; o++) {
         check(Books.ordinal(Books.bookOf(o), Books.chapterOf(o)) == o, "ordinal round-trip " + o);
      }
      check(Books.find("luk") == 41 && Books.find("XYZ") < 0, "find by id");

      // Go to reads typed passages in the common forms.
      String[][] typed = {
         {"John 3:16", "John 3:16"}, {"jn 3 16", "John 3:16"}, {"jhn3.16", "John 3:16"}, {"  JOHN  3 : 16 ", "John 3:16"},
         {"1 cor 13", "1 Corinthians 13"}, {"1co 13:4", "1 Corinthians 13:4"}, {"ii kings 2", "2 Kings 2"}, {"1 john", "1 John 1"},
         {"Ps 23", "Psalms 23"}, {"psalm 119:105", "Psalms 119:105"}, {"gen", "Genesis 1"}, {"mk 1", "Mark 1"}, {"phil 4:13", "Philippians 4:13"},
         {"song of songs 2", "Song of Solomon 2"}, {"Jude 5", "Jude 1:5"}, {"obad 3", "Obadiah 1:3"}, {"rev 99", "Revelation 22"},
         {"isaiah 0", "Isaiah 1"}, {"5", "Luke 5"}, {"3:16", "Luke 3:16"}, {"Rev. 21:4", "Revelation 21:4"}
      };
      for (String[] t : typed) {
         Passage p = Passage.parse(t[0], 41);
         check(p != null && p.name().equals(t[1]), "go to \"" + t[0] + "\": " + (p == null ? "nothing" : p.name()));
      }
      check(Passage.parse("xyz 3", 41) == null && Passage.parse("", 41) == null && Passage.parse("3", -1) == null, "go to: no such book");
      List<Passage> js = Passage.matches("j 2", 41, 5);
      check(js.size() == 5 && js.get(0).name().equals("Joshua 2") && js.get(1).name().equals("Judges 2"), "go to: books in Bible order");
      for (String famous : Passage.FAMOUS) {
         check(Passage.parse(famous, -1) != null, "well known place " + famous);
      }
      check(BibleApi.url("kjv", Books.find("JHN"), 3).equals("https://bible-api.com/data/kjv/JHN/3"), "chapter address");

      // The parser reads the verse and text fields, with escapes, and folds whitespace.
      String json = "{\"translation\":{\"identifier\":\"web\"},\"verses\":[{\"book_id\":\"JHN\",\"book\":\"John\",\"chapter\":3,"
         + "\"verse\":1,\"text\":\"Now there was a man\\n\"},{\"book_id\":\"JHN\",\"chapter\":3,\"verse\":2,"
         + "\"text\":\"He said, \\\"Rabbi\\\" \\u2014  and  more.\\n\"}]}";
      Chapter chapter = Chapter.parse(Books.find("JHN"), 3, "web", json);
      check(chapter.verses.length == 2 && chapter.verseNumbers[1] == 2, "two verses");
      check(chapter.verses[0].equals("Now there was a man"), "newline trimmed: " + chapter.verses[0]);
      check(chapter.verses[1].equals("He said, \"Rabbi\" \u2014 and more."), "escapes and spaces: " + chapter.verses[1]);
      Chapter pretty = Chapter.parse(Books.find("JHN"), 3, "web", "{\n  \"verses\": [\n    { \"verse\": 16, \"text\": \"For God\" }\n  ]\n}");
      check(pretty.verseNumbers[0] == 16 && pretty.verses[0].equals("For God"), "pretty-printed JSON");
      check(chapter.title().equals("John 3"), "title");
      boolean threw = false;
      try {
         Chapter.parse(0, 1, "web", "{\"error\":\"not found\"}");
      } catch (IllegalArgumentException e) {
         threw = true;
      }
      check(threw, "an answer without verses is refused");

      // Layout: every word is placed once, lines stay inside the page, pages hold their line count.
      StringBuilder long1 = new StringBuilder();
      for (int i = 0; i < 400; i++) {
         long1.append("word").append(i).append(' ');
      }
      Chapter big = new Chapter(Books.find("LUK"), 1, "web", new int[]{1, 2, 3},
         new String[]{long1.toString().trim(), "short verse", "the end"});
      FontMetrics body = metrics(new Font("Serif", Font.PLAIN, 17));
      List<Layout.Page> pages = Layout.paginate(big, body, metrics(new Font("SansSerif", Font.BOLD, 10)), metrics(new Font("Serif", Font.BOLD, 51)), 320, 14);
      int words = 0;
      for (Layout.Page page : pages) {
         for (Layout.Run run : page.runs) {
            check(run.line >= 0 && run.line < 14, "line inside the page");
            if (run.kind == Layout.WORD) {
               words++;
               check(run.x + run.width <= 320 || run.x == 0, "word inside the width");
            }
         }
      }
      check(words == 400 + 2 + 2, "every word placed once: " + words);
      for (Layout.Page page : pages) {
         for (int i = 0; i < page.runs.size(); i++) {
            check(page.runs.get(i).kind != Layout.VERSE_NUMBER || page.runs.get(i + 1).line == page.runs.get(i).line,
               "a verse number stays on its first word's line");
         }
      }
      // Justified: every line but the chapter's last ends at the page's edge; the last stays ragged.
      for (int p = 0; p < pages.size(); p++) {
         int lastLine = p == pages.size() - 1 ? pages.get(p).runs.get(pages.get(p).runs.size() - 1).line : 99;
         for (int ln = 0; ln < 14; ln++) {
            int edge = rightEdge(pages.get(p), ln);
            check(edge < 0 || (ln == lastLine ? edge < 320 : edge == 320), "page " + p + " line " + ln + " justified: " + edge);
         }
      }
      // Hyphens: syllable breaks, cautious ones only, and a narrow page breaks long words or narrows gaps a little
      // instead of spreading them.
      String[][] hyphens = {
         {"darkness", "dark-ness"}, {"people", "peo-ple"}, {"children", "chil-dren"}, {"kingdom", "king-dom"},
         {"commandments", "com-mand-ments"}, {"betray", "be-tray"}, {"angry", "an-gry"}, {"(sanctified,", "(sanc-tified,"},
         {"burnt-offering", "burnt--offering"}, {"returned", "returned"}, {"nothing", "nothing"}, {"knowledge", "knowledge"},
         {"LORD's", "LORD's"}, {"Abraham", "Abraham"}, {"unto", "unto"}, {"word12", "word12"}, {"buckle", "buckle"}
      };
      for (String[] h : hyphens) {
         StringBuilder shown = new StringBuilder(h[0]);
         List<Integer> at = Hyphen.breaks(h[0]);
         for (int k = at.size() - 1; k >= 0; k--) {
            shown.insert((int) at.get(k), '-');
         }
         check(shown.toString().equals(h[1]), "hyphen breaks " + h[0] + ": " + shown);
      }
      String prologue = "In the beginning was the Word, and the Word was with God, and the Word was God. The same was in the beginning"
         + " with God. All things were made through him. Without him was not anything made that has been made. In him was life,"
         + " and the life was the light of men. The light shines in the darkness, and the darkness hasn't overcome it. There came"
         + " a man, sent from God, whose name was John. The same came as a witness, that he might testify about the light,"
         + " that all might believe through him. He was not the light, but was sent that he might testify about the light."
         + " The true light that enlightens everyone was coming into the world. He was in the world, and the world was made"
         + " through him, and the world didn't recognize him. He came to his own, and those who were his own didn't receive"
         + " him. But as many as received him, to them he gave the right to become God's children, to those who believe in"
         + " his name: who were born not of blood, nor of the desire of the flesh, nor of the desire of man, but of God."
         + " Commandments, understanding, righteousness, everlasting congregation, wilderness, uncleanness, testimony.";
      Chapter narrow = new Chapter(Books.find("JHN"), 2, "web", new int[]{1}, new String[]{prologue});
      int hyphenated = 0;
      StringBuilder joined = new StringBuilder();
      for (Layout.Page page : Layout.paginate(narrow, body, metrics(new Font("SansSerif", Font.BOLD, 10)), metrics(new Font("Serif", Font.BOLD, 51)), 150, 14)) {
         for (int i = 0; i < page.runs.size(); i++) {
            Layout.Run run = page.runs.get(i);
            if (run.kind != Layout.WORD) {
               continue;
            }
            check(run.x + run.width <= 150, "hyphenated word inside the width");
            boolean ends = i == page.runs.size() - 1 || page.runs.get(i + 1).line != run.line;
            int gap = ends ? 99 : page.runs.get(i + 1).x - run.x - body.stringWidth(run.text);
            check(gap * 3 >= body.stringWidth(" ") * 2, "a squeezed gap keeps two thirds of a space: " + gap);
            if (ends && run.text.endsWith("-")) {
               hyphenated++;
               joined.append(run.text, 0, run.text.length() - 1);
            } else {
               joined.append(run.text).append(' ');
            }
         }
      }
      check(hyphenated > 0 && joined.toString().trim().equals(prologue), "a narrow page hyphenates, text kept: "
         + hyphenated + " " + joined);
      check(pages.size() > 2, "a long chapter takes several pages");
      check(pages.get(0).runs.get(0).kind == Layout.TITLE, "chapter 1 opens with the book title");
      check(pages.get(0).firstVerse == 1 && pages.get(pages.size() - 1).lastVerse == 3, "first and last verse");
      int third = Layout.pageOf(pages, 3);
      check(pages.get(third).lastVerse >= 3 && (third == 0 || pages.get(third - 1).lastVerse < 3), "verse 3 is first shown on page " + third);
      check(Layout.pageOf(pages, 1) == 0 && Layout.pageOf(pages, 99) == pages.size() - 1, "page of a verse");

      // Poetry: a poem's line breaks are kept, each verse's first line leads and its later lines stand in, a line too
      // long for the page runs on further in, and prose after a poem starts a line of its own.
      Chapter psalm23 = Chapter.parse(Books.find("PSA"), 23, "web", "{\"verses\":[{\"verse\":1,\"text\":\"" + long1.substring(0, 300).trim()
         + "\\n\"},{\"verse\":2,\"text\":\"He makes me lie down:\\n  he leads  me.\\n\"},{\"verse\":3,\"text\":\"He restores my soul;\\n"
         + "he guides me in the paths of righteousness, for his name's sake, all the long day and night.\\n\"},"
         + "{\"verse\":4,\"text\":\"Then plain prose.\"}]}");
      check(psalm23.lines[1].equals("He makes me lie down:\nhe leads me.") && psalm23.verses[1].equals("He makes me lie down: he leads me."),
         "a poem's lines kept, folded for the rest: " + psalm23.lines[1]);
      Chapter stray = Chapter.parse(Books.find("PSA"), 23, "kjv", "{\"verses\":[{\"verse\":1,\"text\":\"The LORD\\nis my shepherd\\n\"},"
         + "{\"verse\":2,\"text\":\"He maketh me\"},{\"verse\":3,\"text\":\"for thou\\nart\"}]}");
      check(stray.lines[0].equals("The LORD is my shepherd") && stray.lines[2].equals("for thou art"), "a lone break is no poem: " + stray.lines[0]);
      List<Layout.Page> poem = Layout.paginate(psalm23, body, metrics(new Font("SansSerif", Font.BOLD, 10)), metrics(new Font("Serif", Font.BOLD, 51)), 320, 30);
      int step = body.stringWidth(Layout.STEP);
      int poemWords = 0;
      for (Layout.Run run : poem.get(0).runs) {
         poemWords += run.kind == Layout.WORD ? 1 : 0;
         check(run.kind != Layout.WORD || run.x + run.width <= 320, "poem word inside the width");
      }
      check(poem.size() == 1 && poemWords == psalm23.verses[0].split(" ").length + 8 + 22 + 3, "every poem word placed once: " + poemWords);
      check(startsLine(poem.get(0), "2", 0) && startsLine(poem.get(0), "he", step) && startsLine(poem.get(0), "3", 0)
         && startsLine(poem.get(0), "4", 0), "verses lead, later lines stand in, prose after a poem on its own line");
      Layout.Run turnover = null;
      for (Layout.Run run : poem.get(0).runs) {
         if (run.verse == 3 && turnover == null && run.line > firstLine(poem.get(0), 3) + 1) {
            turnover = run;
         }
      }
      check(turnover != null && turnover.x == body.stringWidth(Layout.HANG), "a long poem line runs on further in: " + (turnover == null ? "none" : turnover.x));
      check(rightEdge(poem.get(0), firstLine(poem.get(0), 3) + 1) < 320, "poem lines stay ragged");

      // Side by side: two translations verse by verse. Pages come in pairs, each verse starts on the same line of
      // both pages, every word of both is placed once, and a verse only one translation has still gets its place.
      Chapter beside = new Chapter(Books.find("LUK"), 1, "kjv", new int[]{1, 2, 4},
         new String[]{"a much shorter first verse", long1.toString().trim() + " " + long1.toString().trim(), "only here"});
      FontMetrics small = metrics(new Font("SansSerif", Font.BOLD, 10));
      FontMetrics numeral = metrics(new Font("Serif", Font.BOLD, 51));
      List<Layout.Page> pairs = Layout.parallel(big, beside, body, small, numeral, 320, 14, null);
      check(pairs.size() % 2 == 0 && pairs.size() > 4, "pages in pairs: " + pairs.size());
      int[] placed = new int[2];
      for (int p = 0; p < pairs.size(); p += 2) {
         Layout.Page left = pairs.get(p);
         Layout.Page right = pairs.get(p + 1);
         check(left.firstVerse == right.firstVerse && left.lastVerse == right.lastVerse, "a pair shows the same verses");
         for (int v = 1; v <= 4; v++) {
            int a = firstLine(left, v);
            int b = firstLine(right, v);
            check(a < 0 || b < 0 || a == b, "verse " + v + " lines up on pages " + p + ": " + a + " / " + b);
         }
         for (int side = 0; side < 2; side++) {
            for (Layout.Run run : pairs.get(p + side).runs) {
               check(run.line >= 0 && run.line < 14, "parallel line inside the page");
               if (run.kind == Layout.WORD) {
                  placed[side]++;
                  check(run.x + run.width <= 320 || run.x == 0, "parallel word inside the width");
               }
            }
         }
      }
      check(placed[0] == 404 && placed[1] == 5 + 800 + 2, "every word of both placed once: " + placed[0] + " / " + placed[1]);
      check(rightEdge(pairs.get(1), firstLine(pairs.get(1), 1)) < 320, "a short verse's only line stays ragged side by side");
      check(rightEdge(pairs.get(0), firstLine(pairs.get(0), 1)) == 320, "a verse's full lines are justified side by side");
      check(pairs.get(0).runs.get(0).kind == Layout.TITLE && pairs.get(1).runs.get(0).kind == Layout.TITLE, "both open with the title");
      check(pairs.get(Layout.pageOf(pairs, 4)).lastVerse >= 4 && firstLine(pairs.get(pairs.size() - 1), 4) >= 0, "a verse in one translation only");
      List<Layout.Page> stacked = Layout.parallel(big, beside, body, small, numeral, 320, 14, metrics(new Font("Serif", Font.ITALIC, 17)));
      int mine = 0;
      int theirs = 0;
      for (Layout.Page page : stacked) {
         for (Layout.Run run : page.runs) {
            check(run.line >= 0 && run.line < 14, "stacked line inside the page");
            check(run.verse == 0 || run.verse >= page.firstVerse && run.verse <= page.lastVerse, "the running head covers verse " + run.verse);
            mine += run.kind == Layout.WORD ? 1 : 0;
            theirs += run.kind == Layout.ALT_WORD ? 1 : 0;
         }
      }
      check(mine == 404 && theirs == 807, "stacked: both translations placed once: " + mine + " / " + theirs);
      Layout.Page firstStacked = stacked.get(0);
      int lastMine = -1;
      int firstTheirs = 99;
      for (Layout.Run run : firstStacked.runs) {
         if (run.verse == 1 && run.kind == Layout.WORD) {
            lastMine = Math.max(lastMine, run.line);
         } else if (run.verse == 1 && run.kind == Layout.ALT_WORD) {
            firstTheirs = Math.min(firstTheirs, run.line);
         }
      }
      check(lastMine >= 0 && (firstTheirs == 99 || firstTheirs > lastMine), "stacked: the second translation follows the verse");
      check(Shelf.nextCompare("web", "").equals("kjv") && Shelf.nextCompare("kjv", "").equals("web") && Shelf.nextCompare("web", "kjv").equals("asv")
         && Shelf.nextCompare("darby", "bbe").isEmpty() && Shelf.nextCompare("web", "darby").isEmpty(), "compare cycles the other translations, then off");

      // The shelf keeps place, size, read chapters and highlights across a reload; bad lines are skipped.
      File file = File.createTempFile("bibleshelf", ".txt");
      Shelf shelf = Shelf.load(file);
      check(shelf.book == Books.find("LUK") && shelf.chapter == 1 && shelf.translation.equals("web"), "starts at Luke 1");
      shelf.translation = "kjv";
      shelf.book = Books.find("PSA");
      shelf.chapter = 23;
      shelf.verse = 4;
      shelf.size = 2;
      shelf.compare = "asv";
      shelf.night = true;
      shelf.markRead(Books.find("PSA"), 23);
      shelf.markRead(Books.find("REV"), 22);
      check(shelf.toggleMark(Books.find("JHN"), 3, 16), "highlight on");
      check(shelf.toggleMark(Books.find("GEN"), 1, 1), "second highlight");
      shelf.save();
      Shelf again = Shelf.load(file);
      check(again.translation.equals("kjv") && again.book == Books.find("PSA") && again.chapter == 23 && again.verse == 4 && again.size == 2, "place kept");
      check(again.compare.equals("asv") && Shelf.load(new File(file.getPath() + ".missing")).compare.isEmpty(), "side by side kept");
      check(again.night && !Shelf.load(new File(file.getPath() + ".missing")).night, "night pages kept, day by default");
      check(again.readCount() == 2 && again.isRead(Books.find("REV"), 22) && again.readCount(Books.find("PSA")) == 1, "read chapters kept");
      check(again.isMarked(Books.find("JHN"), 3, 16) && again.markCount() == 2, "highlights kept");
      int[] next = again.nextMark(Books.find("JHN"), 3, 16);
      check(next[0] == Books.find("GEN"), "next highlight wraps to Genesis");
      check(!again.toggleMark(Books.find("JHN"), 3, 16) && again.markCount() == 1, "highlight off");
      java.nio.file.Files.write(file.toPath(), "junk\nat=ZZZ.9.9\nsize=x\nread=LUK.99,JHN.1\nmarks=bad\n".getBytes("UTF-8"));
      Shelf damaged = Shelf.load(file);
      check(damaged.book == Books.find("LUK") && damaged.isRead(Books.find("JHN"), 1) && damaged.readCount() == 1 && damaged.markCount() == 0,
         "damaged lines skipped");
      file.delete();

      // Reading plans: every chapter falls on exactly one day, in order, and the days cover the plan.
      check(Plan.find("gospels").chapters == 89 && Plan.find("NEW").chapters == 260 && Plan.find("YEAR").chapters == 1189
         && Plan.find("PSALMS").chapters == 150 && Plan.find("PROVERBS").chapters == 31 && Plan.find("nope") == null, "plan sizes");
      for (Plan plan : Plan.ALL) {
         check(plan.dayStart(0) == 0 && plan.dayStart(plan.days) == plan.chapters, plan.id + " covers its chapters");
         for (int d = 0; d < plan.days; d++) {
            int size = plan.dayStart(d + 1) - plan.dayStart(d);
            check(size == plan.chapters / plan.days || size == plan.chapters / plan.days + 1, plan.id + " even day " + d);
         }
         for (int i = 0; i < plan.chapters; i++) {
            int d = plan.dayOf(i);
            check(plan.dayStart(d) <= i && i < plan.dayStart(d + 1), plan.id + " chapter " + i + " on day " + d);
            check(plan.indexOf(plan.bookAt(i), plan.chapterAt(i)) == i, plan.id + " index round-trip " + i);
         }
         check(plan.index() >= 0 && Plan.ALL[plan.index()] == plan, plan.id + " index");
      }
      Plan gospels = Plan.find("GOSPELS");
      check(gospels.reading(0).equals("Matthew 1-2") && gospels.reading(29).equals("John 19-21"), "gospel days: " + gospels.reading(0) + " / " + gospels.reading(29));
      check(gospels.reading(9).equals("Matthew 27-28, Mark 1"), "a day across two books: " + gospels.reading(9));
      check(Plan.find("PROVERBS").reading(8).equals("Proverbs 9") && Plan.find("PROVERBS").pace().equals("1 chapter a day"), "proverbs");
      check(Plan.find("PSALMS").pace().equals("3 chapters a day") && gospels.pace().equals("2 or 3 chapters a day"), "pace");
      check(gospels.indexOf(Books.find("ACT"), 1) < 0 && gospels.indexOf(Books.find("MAT"), 1) == 0, "chapters outside the plan");
      // Days on the calendar agree with java.time, and dates round-trip.
      for (java.time.LocalDate day = java.time.LocalDate.of(1999, 12, 25); day.getYear() < 2031; day = day.plusDays(7)) {
         int epoch = Plan.epochDay(day.getYear(), day.getMonthValue(), day.getDayOfMonth());
         check(epoch == day.toEpochDay(), "epoch day " + day);
         check(Plan.iso(epoch).equals(day.toString()) && Plan.parseIso(day.toString()) == epoch, "iso " + day);
      }
      check(Plan.shortDate(Plan.parseIso("2026-11-02")).equals("Nov 2"), "short date");
      check(Math.abs(Plan.today() - java.time.LocalDate.now().toEpochDay()) <= 1, "today");
      // Progress: behind, ahead, done days and the first chapter left.
      java.util.BitSet done = new java.util.BitSet();
      check(gospels.behind(done, 0) == 0 && gospels.behind(done, 3) == 3 && gospels.firstUnread(done) == 0, "nothing read");
      done.set(0, gospels.dayStart(3));
      check(gospels.behind(done, 3) == 0 && gospels.daysDone(done) == 3 && !gospels.dayDone(done, 3), "three days read");
      check(gospels.ahead(done, 1) == 1 && gospels.ahead(done, 2) == 0, "ahead: " + gospels.ahead(done, 1));
      check(gospels.readOn(done, 3) == 0 && gospels.firstUnread(done) == gospels.dayStart(3), "first unread");
      done.set(0, gospels.chapters);
      check(gospels.firstUnread(done) < 0 && gospels.daysDone(done) == 30 && gospels.behind(done, 40) == 0, "all read");
      // The shelf keeps the plan, its start and what was read for it; chapters outside it are not counted.
      File planFile = File.createTempFile("bibleplan", ".txt");
      Shelf planned = Shelf.load(planFile);
      check(planned.plan == null && !planned.notePlanRead(Books.find("MAT"), 1), "no plan at first");
      planned.startPlan(gospels, Plan.parseIso("2026-11-02"));
      check(planned.notePlanRead(Books.find("MAT"), 1) && !planned.notePlanRead(Books.find("MAT"), 1), "counted once");
      check(!planned.notePlanRead(Books.find("ACT"), 1) && planned.notePlanRead(Books.find("JHN"), 21), "outside the plan");
      check(planned.planDay(Plan.parseIso("2026-11-04")) == 2, "plan day");
      planned.save();
      Shelf reloaded = Shelf.load(planFile);
      check(reloaded.plan == gospels && Plan.iso(reloaded.planStart).equals("2026-11-02"), "plan kept");
      check(reloaded.planDone().cardinality() == 2 && reloaded.planDone().get(0) && reloaded.planDone().get(88), "plan chapters kept");
      check(new String(java.nio.file.Files.readAllBytes(planFile.toPath()), "UTF-8").contains("plan=GOSPELS\nplanstart=2026-11-02\nplandone=MAT.1,JHN.21\n"), "plan lines");
      reloaded.startPlan(Plan.find("PSALMS"), Plan.parseIso("2026-11-05"));
      check(reloaded.planDone().isEmpty(), "a new plan starts empty");
      reloaded.stopPlan();
      reloaded.save();
      check(Shelf.load(planFile).plan == null, "stopped plan");
      planFile.delete();

      // By heart: a drill asks the first letter of each word; dashes are never asked; two wrong letters give the word.
      int jhn = Books.find("JHN");
      Memory.Card john = new Memory.Card(jhn, 3, 16, "web", "For God so loved — the “world,” 3 times", 0, 100);
      Memory.Drill read = new Memory.Drill(john, Memory.Drill.READ);
      check(read.done() && read.askedCount() == 0 && read.words.length == 9, "reading asks nothing");
      Memory.Drill half = new Memory.Drill(john, Memory.Drill.HALF);
      check(half.askedCount() == 4 && !half.isAsked(0) && half.isAsked(1) && !half.isAsked(4) && half.isAsked(6), "every other word: the 2nd, 4th...");
      check(half.at() == 1 && half.type('g') == Memory.Drill.RIGHT && half.at() == 3, "right letter moves on");
      check(half.type('x') == Memory.Drill.WRONG && half.at() == 3 && half.slips() == 0, "one wrong letter waits");
      check(half.type('q') == Memory.Drill.GIVEN && half.isShown(3) && half.isSlipped(3) && half.slips() == 1, "two wrong letters give the word");
      check(half.type('W') == Memory.Drill.RIGHT && half.at() == 8, "quotes skipped, case ignored");
      check(half.type('-') == Memory.Drill.IGNORED, "punctuation ignored");
      half.give();
      check(half.done() && half.slips() == 2 && !half.passed(), "show word slips; too many slips");
      Memory.Drill memo = new Memory.Drill(john, Memory.Drill.MEMORY);
      check(memo.askedCount() == 8 && !memo.isAsked(4) && memo.allowed() == 1, "from memory asks every word");
      for (char c : "fgsltw".toCharArray()) {
         check(memo.type(c) == Memory.Drill.RIGHT, "typed " + c);
      }
      check(memo.at() == 7 && !memo.done(), "at the number");
      check(memo.type('3') == Memory.Drill.RIGHT && memo.at() == 8, "a number is asked by its first digit");
      memo.give();
      check(memo.done() && memo.slips() == 1 && memo.passed(), "one slip passes");
      Memory.Drill letters = new Memory.Drill(john, Memory.Drill.LETTERS);
      check(letters.showsLetter(0) && !new Memory.Drill(john, Memory.Drill.MEMORY).showsLetter(0), "first letters shown only in that stage");
      for (int i = 0; i < 8 && !letters.done(); i++) {
         char[] three = letters.choices();
         check(three.length == 3 && three[0] != three[1] && three[1] != three[2] && three[0] != three[2], "three different choices");
         boolean has = false;
         for (char c : three) {
            has |= c == Memory.Drill.keyOf(letters.words[letters.at()]);
         }
         check(has, "the right letter is offered");
         letters.type(Memory.Drill.keyOf(letters.words[letters.at()]));
      }
      check(letters.done() && letters.slips() == 0, "letters run done");
      // The schedule: learning stays due until passed; passing doubles the gap; a slip on review goes back to box 1.
      check(john.isDue(100) && john.status(100).equals("Learning"), "learning is due");
      check(!Memory.grade(new Memory.Card(jhn, 3, 17, "web", "x", 2, 105), true, 100), "not due: schedule stays");
      check(Memory.grade(john, false, 100) && john.box == 0 && john.due == 100, "slip while learning keeps learning");
      check(Memory.grade(john, true, 100) && john.box == 1 && john.due == 101 && john.status(100).equals("Next review tomorrow"), "learned");
      check(!Memory.grade(john, true, 100) && john.box == 1, "a second run the same day does not count");
      check(Memory.grade(john, true, 101) && john.box == 2 && john.due == 103, "two days");
      check(Memory.grade(john, true, 104) && john.box == 3 && john.due == 108 && john.status(104).equals("Next review in 4 days"), "four days");
      check(john.status(110).startsWith("Review due since"), "overdue");
      check(Memory.grade(john, false, 110) && john.box == 1 && john.due == 111, "slip on review: back to tomorrow");
      john.box = Memory.GAPS.length;
      john.due = 110;
      check(Memory.grade(john, true, 110) && john.box == Memory.GAPS.length && john.due == 110 + 64, "top box stays");
      // The list: Bible order, due counts, next due wraps; the shelf keeps the verses with their text across a reload.
      File learnFile = File.createTempFile("bibleheart", ".txt");
      Shelf heart = Shelf.load(learnFile);
      heart.memory.put(new Memory.Card(jhn, 3, 16, "kjv", "For God so loved | the world", 2, Plan.today() + 2));
      heart.memory.put(new Memory.Card(Books.find("GEN"), 1, 1, "web", "In the beginning, God created the heavens and the earth.", 0, Plan.today()));
      heart.memory.put(new Memory.Card(Books.find("PSA"), 23, 1, "web", "Yahweh is my shepherd: I shall lack nothing.", 1, Plan.today()));
      check(heart.memory.cards().get(0).book == Books.find("GEN") && heart.memory.size() == 3, "Bible order");
      check(heart.memory.dueCount(Plan.today()) == 2 && heart.memory.learnedCount() == 2, "due and learned counts");
      Memory.Card psalm = heart.memory.find(Books.find("PSA"), 23, 1);
      check(heart.memory.nextDue(psalm, Plan.today()).book == Books.find("GEN"), "next due wraps round");
      heart.save();
      Shelf heart2 = Shelf.load(learnFile);
      Memory.Card back = heart2.memory.find(jhn, 3, 16);
      check(heart2.memory.size() == 3 && back != null && back.text.equals("For God so loved | the world") && back.translation.equals("kjv")
         && back.box == 2 && back.due == Plan.today() + 2, "verses kept with their text");
      java.nio.file.Files.write(learnFile.toPath(), ("learn.JHN.3.16=web|1|2026-11-02|Text\nlearn.ZZZ.1.1=web|1|2026-11-02|x\nlearn.JHN.3=web|1|2026-11-02|x\n"
         + "learn.JHN.3.17=web|9|2026-11-02|x\nlearn.JHN.3.18=xyz|1|2026-11-02|x\nlearn.JHN.3.19=web|1|2026-11-02|\n").getBytes("UTF-8"));
      check(Shelf.load(learnFile).memory.size() == 1, "damaged verse lines skipped");
      learnFile.delete();
      Shelf marked = Shelf.load(File.createTempFile("biblemarks", ".txt"));
      marked.toggleMark(Books.find("ACT"), 2, 1);
      marked.toggleMark(Books.find("1CO"), 13, 4);
      marked.toggleMark(Books.find("ACT"), 1, 8);
      check(marked.marks().get(0)[0] == Books.find("ACT") && marked.marks().get(0)[1] == 1 && marked.marks().get(2)[0] == Books.find("1CO"), "highlights in Bible order");
      int[][] laid = LearnView.layout(body, "a bb ccc dddd eeeee".split(" "), body.stringWidth("a bb ccc"));
      check(laid[0][1] == 0 && laid[2][1] == 0 && laid[3][1] == 1 && laid[3][0] == 0, "verse word wrap");

      // Saved pages escape the text and name files as the original app did.
      check(DownloadManager.stem(Books.find("1JN")).equals("1John"), "file stem");
      String html = DownloadManager.chapterHtml(new Chapter(0, 1, "web", new int[]{1}, new String[]{"a < b & c"}), true);
      check(html.contains("<h2>Genesis 1</h2>") && html.contains("<b>1</b> a &lt; b &amp; c"), "chapter html: " + html);
      check(!DownloadManager.chapterHtml(chapter, false).contains("<b>"), "verse numbers off");
      check(ReaderPanel.commas(1189).equals("1,189") && ReaderPanel.commas(42).equals("42"), "commas");
      pageTurns();
      thumbIndex();
      System.out.println("ReaderCheck: all checks passed");
   }

   /** The thumb index: down an edge of any height, every book in Bible order, each reachable, Genesis at the top and Revelation at the foot. */
   private static void thumbIndex() {
      for (int strip = 100; strip <= 1600; strip += 7) {
         int n = ReaderPanel.tabs(strip);
         check(n >= 1 && n <= 66 && (n == 66 || strip / n >= ReaderPanel.TAB_H), "tabs at least TAB_H tall: " + strip);
         check(ReaderPanel.tabFirst(0, n) == 0 && ReaderPanel.tabFirst(n, n) == 66, "tabs cover the books: " + strip);
         boolean[] seen = new boolean[66];
         int previous = 0;
         for (int dy = 0; dy < strip; dy++) {
            int b = ReaderPanel.bookAtEdge(dy, strip);
            check(b >= previous && b < 66, "books in order down the edge: " + strip + " at " + dy);
            seen[b] = true;
            previous = b;
         }
         for (int b = 0; b < 66; b++) {
            check(seen[b], "every book has a place on the edge: " + Books.NAMES[b] + " at " + strip);
         }
         check(ReaderPanel.bookAtEdge(0, strip) == 0 && ReaderPanel.bookAtEdge(strip - 1, strip) == 65, "Genesis at the top, Revelation at the foot: " + strip);
         check(ReaderPanel.bookAtEdge(-5, strip) == 0 && ReaderPanel.bookAtEdge(strip + 5, strip) == 65, "past the ends: " + strip);
      }
   }

   /** A page turn shows the old pages at its start, the new ones at its end, and the leaf over the spine between. */
   private static void pageTurns() {
      Rectangle[] two = {new Rectangle(0, 0, 100, 50), new Rectangle(100, 0, 100, 50)};
      int red = 0xff0000, green = 0x00ff00, blue = 0x0000ff, yellow = 0xffff00;
      BufferedImage[] before = {solid(red), solid(green)};
      BufferedImage[] after = {solid(blue), solid(yellow)};
      check(turnAt(two, true, before, after, 0, 50) == red && turnAt(two, true, before, after, 0, 150) == green, "turn starts on the old pages");
      check(turnAt(two, true, before, after, 1, 50) == blue && turnAt(two, true, before, after, 1, 150) == yellow, "turn ends on the new pages");
      // A quarter in, the old right page has narrowed to half over the new one; three quarters in, its back lands on the left.
      check(turnAt(two, true, before, after, 0.25, 20) == red && turnAt(two, true, before, after, 0.25, 180) == yellow, "rising leaf, forward");
      int leaf = turnAt(two, true, before, after, 0.25, 125);
      check((leaf & 0xff00) != 0 && (leaf & 0xff00) < 0xff00 && (leaf & 0xff00ff) == 0, "the rising leaf is the old page, shaded: " + Integer.toHexString(leaf));
      check(turnAt(two, true, before, after, 0.75, 20) == red && (turnAt(two, true, before, after, 0.75, 75) & 0xffff00) == 0, "landing leaf, forward");
      check(turnAt(two, false, before, after, 0.25, 180) == green && turnAt(two, false, before, after, 0.25, 20) == blue, "rising leaf, back");
      int back = turnAt(two, false, before, after, 0.75, 125);
      check((back & 0xff) == 0 && (back & 0xff0000) != 0 && turnAt(two, false, before, after, 0.75, 180) == green, "landing leaf, back: " + Integer.toHexString(back));
      Rectangle[] one = {new Rectangle(0, 0, 200, 50)};
      check(turnAt(one, true, new BufferedImage[]{solid(red)}, new BufferedImage[]{solid(blue)}, 0.5, 180) == blue
         && (turnAt(one, true, new BufferedImage[]{solid(red)}, new BufferedImage[]{solid(blue)}, 0.5, 40) & 0xff) == 0, "one page folds away to the left");
      check(turnAt(one, false, new BufferedImage[]{solid(red)}, new BufferedImage[]{solid(blue)}, 0.5, 180) == red, "one page back unfolds from the left");

      // A dragged page stays where the finger holds it, and let go short of over it falls back to where it was.
      PageTurn held = new PageTurn(new javax.swing.JPanel());
      BufferedImage wholeBefore = halves(red, green);
      BufferedImage wholeAfter = halves(blue, yellow);
      held.grab(true, wholeBefore, wholeAfter, two);
      held.hold(0.25);
      check(held.held() && held.running() && heldAt(held, two, after, 125) == turnAt(two, true, before, after, 0.25, 125), "held page at a quarter");
      held.hold(7);
      check(heldAt(held, two, after, 50) == blue && heldAt(held, two, after, 150) == yellow, "held page clamps at over");
      held.hold(0.15);
      int[] fell = {0};
      held.let(false, () -> fell[0]++);
      check(!held.held() && held.running() && fell[0] == 0, "let-go page falls back over time");
      held.settle();
      check(fell[0] == 1 && !held.running(), "a page falling back settles at once for the next turn");
      held.grab(true, wholeBefore, wholeAfter, two);
      held.hold(0.5);
      held.let(true, () -> fell[0]++);
      held.settle();
      check(fell[0] == 1 && held.running(), "a page let go over finishes its turn");
   }

   /** A whole panel picture: the left half one colour and the right half another. */
   private static BufferedImage halves(int left, int right) {
      BufferedImage image = solid(left);
      Graphics2D g = image.createGraphics();
      g.setColor(new Color(right));
      g.fillRect(image.getWidth() / 2, 0, image.getWidth() / 2, image.getHeight());
      g.dispose();
      return image;
   }

   /** The colour at (x, 25) of a held turn drawn over the new pages. */
   private static int heldAt(PageTurn turn, Rectangle[] sheets, BufferedImage[] after, int x) {
      BufferedImage frame = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = frame.createGraphics();
      for (int i = 0; i < sheets.length; i++) {
         g.drawImage(after[i], sheets[i].x, sheets[i].y, sheets[i].width, sheets[i].height, null);
      }
      turn.paint(g, sheets);
      g.dispose();
      return frame.getRGB(x, 25) & 0xffffff;
   }

   /** The colour at (x, 25) of a turn frame drawn over the new pages. */
   private static int turnAt(Rectangle[] sheets, boolean forward, BufferedImage[] before, BufferedImage[] after, double t, int x) {
      BufferedImage frame = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = frame.createGraphics();
      for (int i = 0; i < sheets.length; i++) {
         g.drawImage(after[i], sheets[i].x, sheets[i].y, sheets[i].width, sheets[i].height, null);
      }
      PageTurn.paint(g, sheets, forward, before, after, t);
      g.dispose();
      return frame.getRGB(x, 25) & 0xffffff;
   }

   private static BufferedImage solid(int rgb) {
      BufferedImage image = new BufferedImage(200 * PageTurn.SCALE, 50 * PageTurn.SCALE, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      g.setColor(new Color(rgb));
      g.fillRect(0, 0, image.getWidth(), image.getHeight());
      g.dispose();
      return image;
   }

   /** The line a verse's first word or number is on, or -1 when the page does not show it. */
   /** Where a line's last word ends, or -1 for an empty line. */
   private static int rightEdge(Layout.Page page, int line) {
      int edge = -1;
      for (Layout.Run run : page.runs) {
         if (run.line == line && (run.kind == Layout.WORD || run.kind == Layout.VERSE_NUMBER || run.kind == Layout.ALT_WORD)) {
            edge = Math.max(edge, run.x + run.width);
         }
      }
      return edge;
   }

   /** Whether some run with this text is the first on its line, at x. */
   private static boolean startsLine(Layout.Page page, String text, int x) {
      for (int i = 0; i < page.runs.size(); i++) {
         Layout.Run run = page.runs.get(i);
         if (run.text.equals(text) && run.x == x && (i == 0 || page.runs.get(i - 1).line != run.line)) {
            return true;
         }
      }
      return false;
   }

   private static int firstLine(Layout.Page page, int verse) {
      for (Layout.Run run : page.runs) {
         if (run.verse == verse && (run.kind == Layout.WORD || run.kind == Layout.VERSE_NUMBER)) {
            return run.line;
         }
      }
      return -1;
   }

   private static FontMetrics metrics(Font font) {
      return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics().getFontMetrics(font);
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
