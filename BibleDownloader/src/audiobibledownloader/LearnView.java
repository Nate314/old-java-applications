package audiobibledownloader;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import static audiobibledownloader.ReaderPanel.GOLD;
import static audiobibledownloader.ReaderPanel.HEAD;
import static audiobibledownloader.ReaderPanel.HOVER;
import static audiobibledownloader.ReaderPanel.INK;
import static audiobibledownloader.ReaderPanel.MUTED;
import static audiobibledownloader.ReaderPanel.PAPER_EDGE;
import static audiobibledownloader.ReaderPanel.RUBRIC;
import static audiobibledownloader.ReaderPanel.UI;

/**
 * The By heart view of the reader: the verses being learned on the left page, and practice of one of them on the
 * right. A new verse is read, then has every other word hidden, then shows only first letters, then is said from
 * memory, typing the first letter of each word (or picking it from three, for a pointer or a finger). Recalling it
 * from memory schedules its next review ({@link Memory#grade}).
 */
final class LearnView {
   private static final Color SLIP = new Color(0xc0392b);

   private final ReaderPanel panel;
   private final Shelf shelf;
   /** The verse being practiced, or null. */
   private Memory.Card card;
   private Memory.Drill drill;
   /** After a From memory run: what it did, or null. */
   private String result;
   private boolean resultPassed;
   /** On a one-page window: the list is shown instead of the practice page. */
   private boolean listShown = true;
   /** The first row of the list shown, so the picked verse stays in view. */
   private int listTop;
   /** Highlights whose chapter is being fetched to add them. */
   private final Set<String> fetching = new HashSet<>();
   private boolean confirmRemove;
   /** The word that just got a wrong letter, shown red for a moment; -1 for none. */
   private int wrongAt = -1;
   private final Timer wrongTimer;

   LearnView(ReaderPanel panel, Shelf shelf) {
      this.panel = panel;
      this.shelf = shelf;
      this.wrongTimer = new Timer(450, e -> {
         this.wrongAt = -1;
         this.panel.repaint();
      });
      this.wrongTimer.setRepeats(false);
   }

   /** Opening the view: the first verse due, else the one practiced last, else the first. */
   void opened(boolean onePage) {
      int today = Plan.today();
      Memory.Card due = this.shelf.memory.nextDue(null, today);
      if (this.card == null || this.shelf.memory.find(this.card.book, this.card.chapter, this.card.verse) == null || due != null && !this.card.isDue(today)) {
         this.card = null;
         this.drill = null;
         List<Memory.Card> cards = this.shelf.memory.cards();
         Memory.Card first = due != null ? due : cards.isEmpty() ? null : cards.get(0);
         if (first != null) {
            this.begin(first);
         }
      }
      this.listShown = !onePage || due == null || this.card == null;
   }

   private void begin(Memory.Card c) {
      this.card = c;
      this.confirmRemove = false;
      this.stage(c.box == 0 ? Memory.Drill.READ : Memory.Drill.MEMORY);
      this.listShown = false;
   }

   private void stage(int stage) {
      this.drill = new Memory.Drill(this.card, stage);
      this.result = null;
      this.wrongAt = -1;
      this.panel.repaint();
   }

   /** Every highlighted verse not being learned yet. */
   private List<int[]> suggestions() {
      List<int[]> out = new java.util.ArrayList<>();
      for (int[] m : this.shelf.marks()) {
         if (this.shelf.memory.find(m[0], m[1], m[2]) == null) {
            out.add(m);
         }
      }
      return out;
   }

   /** Adds a highlighted verse, fetching its chapter first when it is not in memory. */
   void add(int book, int chapter, int verse) {
      String translation = this.shelf.translation;
      Chapter ready = BibleApi.cached(translation, book, chapter);
      if (ready != null) {
         this.addFrom(ready, verse);
         return;
      }
      String ref = Shelf.verseRef(book, chapter, verse);
      if (!this.fetching.add(ref)) {
         return;
      }
      this.panel.repaint();
      new Thread(() -> {
         Chapter loaded = null;
         String error = null;
         try {
            loaded = BibleApi.fetch(translation, book, chapter);
         } catch (IOException e) {
            error = e.getMessage();
         }
         Chapter result = loaded;
         String message = error;
         SwingUtilities.invokeLater(() -> {
            this.fetching.remove(ref);
            if (result != null) {
               this.addFrom(result, verse);
            } else {
               this.panel.say(Books.NAMES[book] + " " + chapter + ":" + verse + ": " + message, 4000);
               this.panel.repaint();
            }
         });
      }).start();
   }

   private void addFrom(Chapter chapter, int verse) {
      for (int i = 0; i < chapter.verseNumbers.length; i++) {
         if (chapter.verseNumbers[i] == verse && !chapter.verses[i].isEmpty()) {
            Memory.Card c = new Memory.Card(chapter.book, chapter.number, verse, chapter.translation, chapter.verses[i], 0, Plan.today());
            this.shelf.memory.put(c);
            this.shelf.save();
            this.begin(c);
            this.panel.say("Learning " + c.name() + " by heart: read it first", 3000);
            return;
         }
      }
      this.panel.say(Books.NAMES[chapter.book] + " " + chapter.number + " has no verse " + verse + " in this translation", 4000);
   }

   // ---- Input ----

   /** A key in the view; returns whether it was used. */
   boolean key(int code, char c, boolean onePage) {
      if (code == KeyEvent.VK_ESCAPE && onePage && !this.listShown) {
         this.listShown = true;
         this.panel.repaint();
         return true;
      }
      if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) {
         this.step(code == KeyEvent.VK_UP ? -1 : 1);
         return true;
      }
      if (onePage && this.listShown && this.card != null && code == KeyEvent.VK_ENTER) {
         this.listShown = false;
         this.panel.repaint();
         return true;
      }
      if (this.drill == null || onePage && this.listShown) {
         return false;
      }
      boolean typing = this.drill.stage != Memory.Drill.READ && !this.drill.done();
      if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
         if (!typing) {
            this.primary();
         }
         return true;
      }
      if (typing && Character.isLetterOrDigit(c)) {
         this.type(c);
         return true;
      }
      return typing && code != KeyEvent.VK_ESCAPE;
   }

   private void step(int by) {
      List<Memory.Card> cards = this.shelf.memory.cards();
      if (cards.isEmpty()) {
         return;
      }
      int i = this.card == null ? -1 : indexOf(cards, this.card);
      int next = Math.max(0, Math.min(cards.size() - 1, i < 0 ? 0 : i + by));
      if (cards.get(next) != this.card) {
         boolean list = this.listShown;
         this.begin(cards.get(next));
         this.listShown = list;
      }
   }

   private static int indexOf(List<Memory.Card> cards, Memory.Card c) {
      for (int i = 0; i < cards.size(); i++) {
         if (cards.get(i) == c) {
            return i;
         }
      }
      return -1;
   }

   private void type(char c) {
      int at = this.drill.at();
      int r = this.drill.type(c);
      if (r == Memory.Drill.WRONG) {
         this.wrongAt = at;
         this.wrongTimer.restart();
      } else {
         this.wrongAt = -1;
      }
      if (this.drill.done()) {
         this.finished();
      }
      this.panel.repaint();
   }

   private void give() {
      this.drill.give();
      if (this.drill.done()) {
         this.finished();
      }
      this.panel.repaint();
   }

   /** A run is over; From memory moves the verse on its schedule. */
   private void finished() {
      if (this.drill.stage != Memory.Drill.MEMORY) {
         return;
      }
      int today = Plan.today();
      boolean wasLearning = this.card.box == 0;
      boolean passed = this.drill.passed();
      boolean counted = Memory.grade(this.card, passed, today);
      this.shelf.save();
      this.resultPassed = passed;
      int slips = this.drill.slips();
      String slipText = slips == 0 ? "No slips" : slips == 1 ? "1 slip" : slips + " slips";
      int days = this.card.due - today;
      String when = days <= 0 ? "today" : days == 1 ? "tomorrow" : "in " + days + " days";
      if (passed && counted) {
         this.result = slipText + ". " + (wasLearning ? "Learned! First review " : "Next review ") + when + ".";
         this.panel.say(this.card.name() + (wasLearning ? " learned by heart" : " remembered") + ": next review " + when, 3500);
      } else if (passed) {
         this.result = slipText + ". Its review is " + when + ", so the schedule stays.";
      } else if (counted && !wasLearning) {
         this.result = slipText + " (" + this.drill.allowed() + " allowed). It comes back tomorrow; go over the first letters now.";
      } else {
         this.result = slipText + " (" + this.drill.allowed() + " allowed). Go over the first letters, then try again.";
      }
   }

   /** Enter: the main button of the moment. */
   private void primary() {
      if (this.drill.stage < Memory.Drill.MEMORY) {
         this.stage(this.drill.stage + 1);
      } else if (!this.resultPassed) {
         this.stage(Memory.Drill.LETTERS);
      } else {
         Memory.Card next = this.shelf.memory.nextDue(this.card, Plan.today());
         if (next != null) {
            this.begin(next);
         } else {
            this.stage(Memory.Drill.MEMORY);
         }
      }
   }

   private String primaryLabel() {
      if (this.drill.stage < Memory.Drill.MEMORY) {
         return "Next: " + Memory.Drill.STAGES[this.drill.stage + 1].toLowerCase() + " ›";
      }
      if (!this.resultPassed) {
         return "First letters ›";
      }
      return this.shelf.memory.nextDue(this.card, Plan.today()) != null ? "Next verse due ›" : "Say it again ›";
   }

   private void remove() {
      if (!this.confirmRemove) {
         this.confirmRemove = true;
         this.panel.repaint();
         return;
      }
      this.panel.say(this.card.name() + " taken off By heart (it stays highlighted)", 3000);
      this.shelf.memory.remove(this.card);
      this.shelf.save();
      this.card = null;
      this.drill = null;
      this.listShown = true;
      this.panel.repaint();
   }

   // ---- Painting ----

   void paint(Graphics2D g, Rectangle[] sheets, Rectangle[] text) {
      if (sheets.length == 2) {
         this.paintList(g, sheets[0], text[0]);
         this.paintPractice(g, sheets[1], text[1], false);
      } else if (this.listShown || this.card == null) {
         this.paintList(g, sheets[0], text[0]);
      } else {
         this.paintPractice(g, sheets[0], text[0], true);
      }
   }

   private void paintList(Graphics2D g, Rectangle sheet, Rectangle area) {
      int today = Plan.today();
      Memory memory = this.shelf.memory;
      g.setFont(HEAD);
      g.setColor(MUTED);
      g.drawString("BY HEART", area.x, sheet.y + 24);
      String counts = memory.learnedCount() + " LEARNED";
      g.drawString(counts, area.x + area.width - g.getFontMetrics().stringWidth(counts), sheet.y + 24);
      g.setFont(new Font("Serif", Font.BOLD, 26));
      g.setColor(INK);
      g.drawString("By heart", area.x, area.y + 26);
      g.setFont(new Font("Serif", Font.ITALIC, 14));
      g.setColor(MUTED);
      int y = this.panel.wrap(g, "Learn a verse by typing the first letter of each word. Each review comes twice as long after the last: tomorrow, in 2 days, 4, 8...", area.x, area.y + 50, area.width, 18) + 2;

      int due = memory.dueCount(today);
      if (due > 0 && memory.nextDue(null, today) != this.card) {
         String label = (due == 1 ? "Practice the verse due" : "Practice the " + due + " due") + " ›";
         g.setFont(UI);
         Rectangle b = new Rectangle(area.x, y, g.getFontMetrics().stringWidth(label) + 28, 28);
         this.panel.pill(g, b, label, RUBRIC, Color.WHITE);
         this.panel.hit(b, () -> this.begin(memory.nextDue(null, Plan.today())));
         y += 36;
      }

      List<Memory.Card> cards = memory.cards();
      List<int[]> offers = this.suggestions();
      int rowH = 30;
      int bottom = area.y + area.height;
      int offerRows = offers.isEmpty() ? 2 : Math.min(offers.size(), 4) + 1;
      int rows = Math.max(1, (bottom - y - offerRows * 26 - 22) / rowH);
      if (cards.isEmpty()) {
         g.setFont(new Font("Serif", Font.PLAIN, 15));
         g.setColor(INK);
         y = this.panel.wrap(g, "No verses yet.", area.x, y + 18, area.width, 20) + 6;
      } else {
         int picked = this.card == null ? -1 : indexOf(cards, this.card);
         if (picked >= 0 && picked < this.listTop) {
            this.listTop = picked;
         } else if (picked >= this.listTop + rows) {
            this.listTop = picked - rows + 1;
         }
         this.listTop = Math.max(0, Math.min(this.listTop, Math.max(0, cards.size() - rows)));
         for (int i = this.listTop; i < Math.min(cards.size(), this.listTop + rows); i++) {
            this.cardRow(g, cards.get(i), new Rectangle(area.x - 6, y, area.width + 12, rowH - 2), today);
            y += rowH;
         }
         if (cards.size() > rows) {
            g.setFont(new Font("Serif", Font.ITALIC, 12));
            g.setColor(MUTED);
            g.drawString((this.listTop + 1) + "-" + Math.min(cards.size(), this.listTop + rows) + " of " + cards.size() + " (Up and Down)", area.x, y + 12);
            y += 18;
         }
      }

      // Highlights that could be learned.
      y += 10;
      g.setFont(new Font("Serif", Font.BOLD, 15));
      g.setColor(RUBRIC);
      g.drawString("From your highlights", area.x, y + 12);
      y += 20;
      if (offers.isEmpty()) {
         g.setFont(new Font("Serif", Font.ITALIC, 13));
         g.setColor(MUTED);
         this.panel.wrap(g, memory.size() == 0 && this.shelf.markCount() == 0
            ? "Click a verse while reading to highlight it, then add it here."
            : "Every highlight is here. Click a verse while reading to highlight another.", area.x, y + 12, area.width, 17);
         return;
      }
      for (int i = 0; i < offers.size() && y + 26 <= bottom; i++) {
         int[] m = offers.get(i);
         String name = Books.NAMES[m[0]] + " " + m[1] + ":" + m[2];
         g.setFont(new Font("Serif", Font.PLAIN, 14));
         g.setColor(INK);
         g.drawString(name, area.x, y + 17);
         boolean busy = this.fetching.contains(Shelf.verseRef(m[0], m[1], m[2]));
         String label = busy ? "Fetching..." : "+ Learn";
         g.setFont(UI);
         int bw = g.getFontMetrics().stringWidth(label) + 20;
         Rectangle b = new Rectangle(area.x + area.width - bw, y, bw, 24);
         this.panel.pill(g, b, label, busy ? MUTED : PAPER_EDGE, busy ? Color.WHITE : INK);
         if (!busy) {
            this.panel.hit(b, () -> this.add(m[0], m[1], m[2]));
         }
         y += 26;
      }
   }

   /** One verse in the list: its name, where it stands and a pip per box. */
   private void cardRow(Graphics2D g, Memory.Card c, Rectangle r, int today) {
      boolean picked = c == this.card;
      if (picked || this.panel.over(r)) {
         g.setColor(picked ? new Color(0xf3e3b8) : HOVER);
         g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
      }
      g.setFont(new Font("Serif", Font.BOLD, 15));
      g.setColor(picked ? RUBRIC : INK);
      g.drawString(c.name(), r.x + 6, r.y + 19);
      int nameW = g.getFontMetrics().stringWidth(c.name());
      String status = c.status(today);
      g.setFont(new Font("Serif", Font.ITALIC, 12));
      g.setColor(c.isDue(today) ? RUBRIC : MUTED);
      int pips = Memory.GAPS.length * 7;
      int room = r.width - nameW - pips - 24;
      status = ReaderPanel.fit(g.getFontMetrics(), status, Math.max(20, room));
      g.drawString(status, r.x + r.width - 6 - pips - 8 - g.getFontMetrics().stringWidth(status), r.y + 19);
      for (int i = 0; i < Memory.GAPS.length; i++) {
         int px = r.x + r.width - 6 - pips + i * 7;
         g.setColor(i < c.box ? GOLD : PAPER_EDGE);
         g.fillOval(px, r.y + 12, 5, 5);
      }
      this.panel.hit(r, () -> this.begin(c));
   }

   private void paintPractice(Graphics2D g, Rectangle sheet, Rectangle area, boolean onePage) {
      g.setFont(HEAD);
      g.setColor(MUTED);
      if (this.card == null) {
         g.drawString("PRACTICE", area.x, sheet.y + 24);
         this.panel.centeredNote(g, area, "Pick a verse", this.shelf.memory.size() == 0 ? "Add a highlighted verse to start" : "Choose one from the list");
         return;
      }
      Memory.Card c = this.card;
      Memory.Drill d = this.drill;
      int today = Plan.today();
      String head = c.translation.toUpperCase();
      g.drawString(head, area.x + area.width - g.getFontMetrics().stringWidth(head), sheet.y + 24);
      int x = area.x;
      if (onePage) {
         Rectangle back = new Rectangle(area.x, sheet.y + 8, 64, 24);
         this.panel.pill(g, back, "‹ List", PAPER_EDGE, INK);
         this.panel.hit(back, () -> {
            this.listShown = true;
            this.panel.repaint();
         });
      } else {
         g.drawString("PRACTICE", x, sheet.y + 24);
      }

      g.setFont(new Font("Serif", Font.BOLD, 24));
      g.setColor(INK);
      g.drawString(c.name(), area.x, area.y + 24);
      g.setFont(new Font("Serif", Font.ITALIC, 13));
      g.setColor(c.isDue(today) && c.box > 0 ? RUBRIC : MUTED);
      g.drawString(c.status(today), area.x, area.y + 42);

      // The four stages, as chips; any can be picked.
      int cy = area.y + 54;
      g.setFont(UI_SMALL_BOLD);
      FontMetrics fm = g.getFontMetrics();
      int total = 0;
      for (String s : CHIPS) {
         total += fm.stringWidth(s) + 18 + 4;
      }
      boolean numbered = total > area.width;
      int chipX = area.x;
      for (int s = 0; s < Memory.Drill.STAGES.length; s++) {
         String label = numbered ? (s + 1) + (s == d.stage ? " " + CHIPS[s] : "") : CHIPS[s];
         int cw = fm.stringWidth(label) + 18;
         Rectangle chip = new Rectangle(chipX, cy, cw, 22);
         boolean on = s == d.stage;
         g.setColor(on ? RUBRIC : this.panel.over(chip) ? HOVER : new Color(0xf1e8d2));
         g.fillRoundRect(chip.x, chip.y, chip.width, chip.height, 22, 22);
         g.setColor(on ? RUBRIC : PAPER_EDGE);
         g.drawRoundRect(chip.x, chip.y, chip.width, chip.height, 22, 22);
         g.setColor(on ? Color.WHITE : INK);
         g.drawString(label, chip.x + 9, chip.y + 15);
         int stage = s;
         this.panel.hit(chip, () -> this.stage(stage));
         chipX += cw + 4;
      }

      String how;
      if (d.done() && d.stage != Memory.Drill.READ) {
         how = d.stage == Memory.Drill.MEMORY ? (this.resultPassed ? "Said from memory." : "Nearly there.") : "Every word in place.";
      } else {
         switch (d.stage) {
            case Memory.Drill.READ:
               how = "Read it aloud, slowly, two or three times.";
               break;
            case Memory.Drill.HALF:
               how = "Type the first letter of each missing word.";
               break;
            case Memory.Drill.LETTERS:
               how = "Say each word, typing its first letter.";
               break;
            default:
               how = "Say it without looking: type each word's first letter.";
         }
      }
      g.setFont(new Font("Serif", Font.ITALIC, 14));
      g.setColor(MUTED);
      g.drawString(ReaderPanel.fit(g.getFontMetrics(), how, area.width), area.x, cy + 44);

      // The verse, as large as fits between the instructions and the buttons.
      Rectangle verse = new Rectangle(area.x, cy + 56, area.width, area.y + area.height - (cy + 56) - 92);
      this.paintVerse(g, d, verse);

      // Under the verse: letters to pick, or what the run did, and the buttons.
      int by = area.y + area.height - 84;
      if (d.stage != Memory.Drill.READ && !d.done()) {
         char[] choices = d.choices();
         int bx = area.x;
         for (char ch : choices) {
            Rectangle b = new Rectangle(bx, by, 46, 40);
            this.panel.pill(g, b, String.valueOf(ch).toUpperCase(), PAPER_EDGE, INK);
            this.panel.hit(b, () -> this.type(ch));
            bx += 52;
         }
         g.setFont(UI);
         String show = "Show word";
         Rectangle sb = new Rectangle(bx + 8, by + 6, g.getFontMetrics().stringWidth(show) + 22, 28);
         this.panel.pill(g, sb, show, new Color(0xf1e8d2), MUTED);
         this.panel.hit(sb, this::give);
         g.setFont(new Font("Serif", Font.ITALIC, 13));
         g.setColor(d.slips() > d.allowed() ? SLIP : MUTED);
         String slips = d.slips() == 0 ? "No slips yet" : d.slips() + (d.slips() == 1 ? " slip" : " slips");
         g.drawString(ReaderPanel.fit(g.getFontMetrics(), slips + " (" + d.allowed() + " allowed)", area.width), area.x, by + 58);
      } else {
         if (this.result != null) {
            g.setFont(new Font("Serif", Font.PLAIN, 14));
            g.setColor(this.resultPassed ? INK : SLIP);
            g.drawString(ReaderPanel.fit(g.getFontMetrics(), this.result, area.width), area.x, by - 4);
         }
         g.setFont(UI);
         String label = this.primaryLabel();
         Rectangle b = new Rectangle(area.x, by + 6, g.getFontMetrics().stringWidth(label) + 28, 32);
         this.panel.pill(g, b, label, RUBRIC, Color.WHITE);
         this.panel.hit(b, this::primary);
         if (d.stage != Memory.Drill.READ) {
            String again = "Again";
            Rectangle a = new Rectangle(b.x + b.width + 8, b.y, g.getFontMetrics().stringWidth(again) + 26, 32);
            this.panel.pill(g, a, again, PAPER_EDGE, INK);
            this.panel.hit(a, () -> this.stage(this.drill.stage));
         }
         g.setFont(new Font("Serif", Font.ITALIC, 12));
         g.setColor(MUTED);
         g.drawString("or press Enter", b.x + 4, b.y + 50);
      }

      // Taking the verse off the list (click twice).
      g.setFont(new Font("Serif", Font.ITALIC, 12));
      String off = this.confirmRemove ? "Click again to take it off" : "Take off By heart";
      int ow = g.getFontMetrics().stringWidth(off);
      Rectangle o = new Rectangle(area.x + area.width - ow - 4, area.y + area.height - 4, ow + 4, 18);
      g.setColor(this.confirmRemove ? SLIP : this.panel.over(o) ? INK : MUTED);
      g.drawString(off, o.x + 2, o.y + 13);
      this.panel.hit(o, this::remove);
   }

   /** The stages' short names, for the chips. */
   private static final String[] CHIPS = {"Read", "Every other", "First letters", "From memory"};
   private static final Font UI_SMALL_BOLD = new Font("SansSerif", Font.BOLD, 11);

   /** Lays the words out in the largest size that fits, drawing each as the drill shows it. */
   private void paintVerse(Graphics2D g, Memory.Drill d, Rectangle area) {
      int size = 22;
      Font font;
      int lh;
      int[][] place;
      do {
         font = new Font("Serif", Font.PLAIN, size);
         lh = Math.round(size * 1.6f);
         place = layout(g.getFontMetrics(font), d.words, area.width);
         size--;
      } while (size >= 12 && (place.length == 0 ? 0 : place[place.length - 1][1] + 1) * lh > area.height);
      g.setFont(font);
      FontMetrics fm = g.getFontMetrics();
      Stroke stroke = g.getStroke();
      for (int i = 0; i < d.words.length; i++) {
         String w = d.words[i];
         int x = area.x + place[i][0];
         int base = area.y + place[i][1] * lh + fm.getAscent();
         int width = fm.stringWidth(w);
         boolean current = i == d.at() && d.stage != Memory.Drill.READ;
         if (d.isShown(i)) {
            g.setColor(d.isSlipped(i) ? SLIP : INK);
            g.drawString(w, x, base);
            continue;
         }
         int lineFrom = x;
         if (d.showsLetter(i)) {
            String lead = lead(w);
            g.setColor(MUTED);
            g.drawString(lead, x, base);
            lineFrom = x + fm.stringWidth(lead) + 1;
         }
         if (current) {
            g.setColor(i == this.wrongAt ? SLIP : GOLD);
            g.setStroke(new BasicStroke(3f));
         } else {
            g.setColor(PAPER_EDGE);
            g.setStroke(new BasicStroke(2f));
         }
         g.drawLine(lineFrom, base + 3, x + Math.max(width, 10), base + 3);
         g.setStroke(stroke);
      }
   }

   /** A word up to and including its first letter, so opening quotes and brackets stay with it. */
   private static String lead(String word) {
      for (int i = 0; i < word.length(); i++) {
         if (Character.isLetterOrDigit(word.charAt(i))) {
            return word.substring(0, i + 1);
         }
      }
      return word;
   }

   /** Each word's x and line for a width. */
   static int[][] layout(FontMetrics fm, String[] words, int width) {
      int[][] place = new int[words.length][2];
      int space = fm.stringWidth(" ");
      int x = 0;
      int line = 0;
      for (int i = 0; i < words.length; i++) {
         int w = fm.stringWidth(words[i]);
         if (x > 0 && x + w > width) {
            x = 0;
            line++;
         }
         place[i][0] = x;
         place[i][1] = line;
         x += w + space;
      }
      return place;
   }
}
