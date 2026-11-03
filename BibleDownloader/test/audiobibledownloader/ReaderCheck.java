package audiobibledownloader;

import java.awt.Font;
import java.awt.FontMetrics;
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
      check(BibleApi.url("kjv", Books.find("JHN"), 3).equals("https://bible-api.com/data/kjv/JHN/3"), "chapter address");

      // The parser reads the verse and text fields, with escapes, and folds whitespace.
      String json = "{\"translation\":{\"identifier\":\"web\"},\"verses\":[{\"book_id\":\"JHN\",\"book\":\"John\",\"chapter\":3,"
         + "\"verse\":1,\"text\":\"Now there was a man\\n\"},{\"book_id\":\"JHN\",\"chapter\":3,\"verse\":2,"
         + "\"text\":\"He said, \\\"Rabbi\\\" \\u2014  and  more.\\n\"}]}";
      Chapter chapter = Chapter.parse(Books.find("JHN"), 3, "web", json);
      check(chapter.verses.length == 2 && chapter.verseNumbers[1] == 2, "two verses");
      check(chapter.verses[0].equals("Now there was a man"), "newline trimmed: " + chapter.verses[0]);
      check(chapter.verses[1].equals("He said, \"Rabbi\" \u2014 and more."), "escapes and spaces: " + chapter.verses[1]);
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
      check(pages.size() > 2, "a long chapter takes several pages");
      check(pages.get(0).runs.get(0).kind == Layout.TITLE, "chapter 1 opens with the book title");
      check(pages.get(0).firstVerse == 1 && pages.get(pages.size() - 1).lastVerse == 3, "first and last verse");
      int third = Layout.pageOf(pages, 3);
      check(pages.get(third).lastVerse >= 3 && (third == 0 || pages.get(third - 1).lastVerse < 3), "verse 3 is first shown on page " + third);
      check(Layout.pageOf(pages, 1) == 0 && Layout.pageOf(pages, 99) == pages.size() - 1, "page of a verse");

      // The shelf keeps place, size, read chapters and highlights across a reload; bad lines are skipped.
      File file = File.createTempFile("bibleshelf", ".txt");
      Shelf shelf = Shelf.load(file);
      check(shelf.book == Books.find("LUK") && shelf.chapter == 1 && shelf.translation.equals("web"), "starts at Luke 1");
      shelf.translation = "kjv";
      shelf.book = Books.find("PSA");
      shelf.chapter = 23;
      shelf.verse = 4;
      shelf.size = 2;
      shelf.markRead(Books.find("PSA"), 23);
      shelf.markRead(Books.find("REV"), 22);
      check(shelf.toggleMark(Books.find("JHN"), 3, 16), "highlight on");
      check(shelf.toggleMark(Books.find("GEN"), 1, 1), "second highlight");
      shelf.save();
      Shelf again = Shelf.load(file);
      check(again.translation.equals("kjv") && again.book == Books.find("PSA") && again.chapter == 23 && again.verse == 4 && again.size == 2, "place kept");
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
      System.out.println("ReaderCheck: all checks passed");
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
