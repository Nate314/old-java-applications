package audiobibledownloader;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The reader: an open book on a leather desk. Chapters are set as pages (two side by side when the window is wide
 * enough), and turning past the last page opens the next chapter. A contents view shows which chapters have been
 * read, and a save view writes chapters to web pages as the original app did.
 */
public class ReaderPanel extends JPanel implements DownloadManager.Progress {
   private static final long serialVersionUID = 1L;

   private static final Color LEATHER = new Color(0x2f2219);
   private static final Color LEATHER_LIGHT = new Color(0x3d2c20);
   private static final Color PAPER = new Color(0xfbf5e6);
   private static final Color PAPER_EDGE = new Color(0xe3d5b5);
   private static final Color INK = new Color(0x2b2118);
   private static final Color RUBRIC = new Color(0x9e2a2b);
   private static final Color MUTED = new Color(0x8a7a66);
   private static final Color MARK = new Color(0xf6dc7c);
   private static final Color HOVER = new Color(0xefe4c6);
   private static final Color BUTTON = new Color(0x4a3828);
   private static final Color BUTTON_HOVER = new Color(0x654b34);
   private static final Color BUTTON_TEXT = new Color(0xf3e7cf);
   private static final Color GOLD = new Color(0xd8b45e);
   private static final Font UI = new Font("SansSerif", Font.BOLD, 13);
   private static final Font UI_SMALL = new Font("SansSerif", Font.PLAIN, 11);
   private static final Font HEAD = new Font("SansSerif", Font.BOLD, 10);
   private static final int BAR = 56;
   /** Below this width the book shows one page at a time. */
   private static final int TWO_PAGE_WIDTH = 700;

   private enum View { READ, BOOKS, CHAPTERS, SAVE }

   /** A clickable area found while painting. */
   private static final class Hit {
      final Rectangle area;
      final Runnable action;
      /** The verse a word belongs to, or 0. */
      final int verse;

      Hit(Rectangle area, Runnable action, int verse) {
         this.area = area;
         this.action = action;
         this.verse = verse;
      }
   }

   private final Shelf shelf;
   private final List<Hit> hits = new ArrayList<>();
   private View view = View.READ;
   /** The book shown in the chapters and save views. */
   private int pickedBook;

   private Chapter chapter;
   private String loadError;
   /** Bumped on every request, so a slow answer for a chapter no longer wanted is dropped. */
   private int request;
   /** Open the chapter at its last pages (turning back from the next chapter). */
   private boolean landAtEnd;
   private List<Layout.Page> pages;
   private String pagesKey = "";
   /** The first page shown. */
   private int spread;

   private int mouseX = -1;
   private int mouseY = -1;
   private int hoverVerse;
   private String flash;
   private final Timer flashTimer;

   private int saveFrom = 1;
   private int saveTo = 1;
   private boolean saveOneFile = true;
   private boolean saveNumbers = true;
   private boolean saving;
   private int savePercent = 100;
   private String saveStatus = "";

   public ReaderPanel(Shelf shelf) {
      this.shelf = shelf;
      this.pickedBook = shelf.book;
      this.setPreferredSize(new Dimension(900, 600));
      this.setFocusable(true);
      this.flashTimer = new Timer(2600, e -> {
         this.flash = null;
         this.repaint();
      });
      this.flashTimer.setRepeats(false);

      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            ReaderPanel.this.requestFocusInWindow();
            ReaderPanel.this.click(e.getX(), e.getY());
         }

         @Override
         public void mouseMoved(MouseEvent e) {
            ReaderPanel.this.hover(e.getX(), e.getY());
         }

         @Override
         public void mouseExited(MouseEvent e) {
            ReaderPanel.this.hover(-1, -1);
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
      this.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            ReaderPanel.this.key(e.getKeyCode(), e.getKeyChar());
         }
      });
      this.open(shelf.book, shelf.chapter, false);
   }

   // ---- Reading and turning pages ----

   /** Opens a chapter, fetching it if needed. The page shown is the saved verse's, or the last pages. */
   private void open(int book, int number, boolean atEnd) {
      if (book != this.shelf.book || number != this.shelf.chapter) {
         this.shelf.verse = 1;
      }
      this.shelf.book = book;
      this.shelf.chapter = number;
      this.landAtEnd = atEnd;
      this.view = View.READ;
      this.loadError = null;
      int ticket = ++this.request;
      String translation = this.shelf.translation;
      Chapter ready = BibleApi.cached(translation, book, number);
      if (ready != null) {
         this.show(ready);
         return;
      }
      this.chapter = null;
      this.repaint();
      new Thread(() -> {
         Chapter loaded = null;
         String error = null;
         try {
            loaded = BibleApi.fetch(translation, book, number);
         } catch (IOException e) {
            error = e.getMessage();
         }
         Chapter result = loaded;
         String message = error;
         SwingUtilities.invokeLater(() -> {
            if (ticket != this.request) {
               return;
            }
            if (result != null) {
               this.show(result);
            } else {
               this.loadError = message;
               this.repaint();
            }
         });
      }).start();
   }

   private void show(Chapter loaded) {
      this.chapter = loaded;
      this.pages = null;
      this.pagesKey = "";
      this.spread = -1;
      this.repaint();
      this.prefetch();
   }

   /** Fetches the next chapter in the background, so turning into it is instant. */
   private void prefetch() {
      int ordinal = Books.ordinal(this.shelf.book, this.shelf.chapter) + 1;
      if (ordinal >= Books.TOTAL) {
         return;
      }
      String translation = this.shelf.translation;
      int book = Books.bookOf(ordinal);
      int number = Books.chapterOf(ordinal);
      if (BibleApi.cached(translation, book, number) == null) {
         new Thread(() -> {
            try {
               BibleApi.fetch(translation, book, number);
            } catch (IOException e) {
               // Fetched again when it is opened.
            }
         }).start();
      }
   }

   private int perView() {
      return this.getWidth() >= TWO_PAGE_WIDTH ? 2 : 1;
   }

   private void next() {
      if (this.view != View.READ) {
         return;
      }
      if (this.pages != null && this.spread + this.perView() < this.pages.size()) {
         this.turnTo(this.spread + this.perView());
      } else if (this.chapter != null || this.loadError != null) {
         this.nextChapter();
      }
   }

   private void previous() {
      if (this.view != View.READ) {
         return;
      }
      if (this.pages != null && this.spread > 0) {
         this.turnTo(Math.max(0, this.spread - this.perView()));
      } else {
         this.previousChapter();
      }
   }

   private void nextChapter() {
      int ordinal = Books.ordinal(this.shelf.book, this.shelf.chapter) + 1;
      if (ordinal < Books.TOTAL) {
         this.open(Books.bookOf(ordinal), Books.chapterOf(ordinal), false);
      } else {
         this.say("The end of Revelation, the last book");
      }
   }

   private void previousChapter() {
      int ordinal = Books.ordinal(this.shelf.book, this.shelf.chapter) - 1;
      if (ordinal >= 0) {
         this.open(Books.bookOf(ordinal), Books.chapterOf(ordinal), true);
      } else {
         this.say("Genesis 1 is the first chapter");
      }
   }

   private void turnTo(int page) {
      this.spread = page;
      this.placeChanged();
      this.repaint();
   }

   /** Saves the place; a chapter whose last page is open counts as read. */
   private void placeChanged() {
      if (this.pages == null) {
         return;
      }
      this.shelf.verse = this.pages.get(this.spread).firstVerse;
      if (this.spread + this.perView() >= this.pages.size() && !this.shelf.isRead(this.shelf.book, this.shelf.chapter)) {
         this.shelf.markRead(this.shelf.book, this.shelf.chapter);
         this.say(this.chapter.title() + " read: " + this.shelf.readCount() + " of 1,189 chapters");
      }
      this.shelf.save();
   }

   private void cycleTranslation() {
      int i = (BibleApi.translationIndex(this.shelf.translation) + 1) % BibleApi.TRANSLATIONS.length;
      this.shelf.translation = BibleApi.TRANSLATIONS[i];
      this.shelf.save();
      this.say(BibleApi.TRANSLATION_NAMES[i]);
      if (this.view == View.READ) {
         this.open(this.shelf.book, this.shelf.chapter, false);
      }
   }

   private void resize(int step) {
      int size = Math.max(0, Math.min(Shelf.SIZES.length - 1, this.shelf.size + step));
      if (size != this.shelf.size) {
         this.shelf.size = size;
         this.shelf.save();
         this.pagesKey = "";
         this.repaint();
      }
   }

   private void toggleMark(int verse) {
      boolean on = this.shelf.toggleMark(this.shelf.book, this.shelf.chapter, verse);
      this.shelf.save();
      this.say((on ? "Highlighted " : "Highlight removed: ") + Books.NAMES[this.shelf.book] + " " + this.shelf.chapter + ":" + verse);
      this.repaint();
   }

   private void nextMark() {
      int[] mark = this.shelf.nextMark(this.shelf.book, this.shelf.chapter, this.shelf.verse);
      if (mark == null) {
         this.say("No highlights yet: click a verse to highlight it");
         return;
      }
      this.open(mark[0], mark[1], false);
      this.shelf.verse = mark[2];
      this.spread = -1;
      this.say("Highlight: " + Books.NAMES[mark[0]] + " " + mark[1] + ":" + mark[2]);
      this.repaint();
   }

   private void say(String message) {
      this.flash = message;
      this.flashTimer.restart();
      this.repaint();
   }

   // ---- Input ----

   private void click(int x, int y) {
      for (int i = this.hits.size() - 1; i >= 0; i--) {
         Hit hit = this.hits.get(i);
         if (hit.area.contains(x, y)) {
            hit.action.run();
            return;
         }
      }
      // Anywhere else on the book: the left half turns back, the right half forward.
      if (this.view == View.READ && y < this.getHeight() - BAR) {
         if (x < this.getWidth() / 2) {
            this.previous();
         } else {
            this.next();
         }
      }
   }

   private void hover(int x, int y) {
      this.mouseX = x;
      this.mouseY = y;
      int verse = 0;
      boolean onHit = false;
      for (Hit hit : this.hits) {
         if (hit.area.contains(x, y)) {
            onHit = true;
            if (hit.verse > 0) {
               verse = hit.verse;
            }
         }
      }
      this.setCursor(Cursor.getPredefinedCursor(onHit ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
      this.hoverVerse = verse;
      this.repaint();
   }

   private void key(int code, char c) {
      switch (code) {
         case KeyEvent.VK_RIGHT:
         case KeyEvent.VK_PAGE_DOWN:
         case KeyEvent.VK_SPACE:
            this.next();
            return;
         case KeyEvent.VK_LEFT:
         case KeyEvent.VK_PAGE_UP:
         case KeyEvent.VK_BACK_SPACE:
            this.previous();
            return;
         case KeyEvent.VK_HOME:
            if (this.view == View.READ && this.pages != null) {
               this.turnTo(0);
            }
            return;
         case KeyEvent.VK_ESCAPE:
            this.back();
            return;
         case KeyEvent.VK_ENTER:
            if (this.view == View.READ && this.loadError != null) {
               this.open(this.shelf.book, this.shelf.chapter, this.landAtEnd);
            }
            return;
         default:
      }
      switch (Character.toLowerCase(c)) {
         case 'b':
            this.showBooks();
            break;
         case 'n':
            this.nextChapter();
            break;
         case 'p':
            this.previousChapter();
            break;
         case 't':
            this.cycleTranslation();
            break;
         case 'm':
            this.nextMark();
            break;
         case 's':
            this.showSave(this.shelf.book);
            break;
         case '+':
         case '=':
            this.resize(1);
            break;
         case '-':
            this.resize(-1);
            break;
         default:
      }
   }

   private void back() {
      if (this.view == View.CHAPTERS || this.view == View.SAVE && !this.saving) {
         this.view = this.view == View.SAVE ? View.READ : View.BOOKS;
      } else if (this.view == View.BOOKS) {
         this.view = View.READ;
      }
      this.repaint();
   }

   private void showBooks() {
      this.view = this.view == View.BOOKS ? View.READ : View.BOOKS;
      this.repaint();
   }

   private void showChapters(int book) {
      this.pickedBook = book;
      this.view = View.CHAPTERS;
      this.repaint();
   }

   private void showSave(int book) {
      if (!this.saving && book != this.pickedBook || this.view != View.SAVE && !this.saving) {
         this.pickedBook = book;
         this.saveFrom = book == this.shelf.book ? this.shelf.chapter : 1;
         this.saveTo = book == this.shelf.book ? this.shelf.chapter : Books.CHAPTERS[book];
      }
      this.view = View.SAVE;
      this.repaint();
   }

   private void startSave() {
      if (this.saving) {
         return;
      }
      this.saving = true;
      this.savePercent = 0;
      this.saveStatus = "";
      DownloadManager.downloadText(this, this.shelf.translation, this.pickedBook, this.saveFrom, this.saveTo, this.saveOneFile, this.saveNumbers);
      this.repaint();
   }

   @Override
   public void progress(int percent, String status) {
      SwingUtilities.invokeLater(() -> {
         this.savePercent = percent;
         this.saveStatus = status;
         this.repaint();
      });
   }

   @Override
   public void finished(String status) {
      SwingUtilities.invokeLater(() -> {
         this.saving = false;
         this.savePercent = 100;
         this.saveStatus = status;
         this.repaint();
      });
   }

   // ---- Painting ----

   private Font body() {
      return new Font("Serif", Font.PLAIN, Shelf.SIZES[this.shelf.size]);
   }

   private int lineHeight() {
      return Math.round(Shelf.SIZES[this.shelf.size] * 1.55f);
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      this.hits.clear();
      int w = this.getWidth();
      int h = this.getHeight();
      g.setColor(LEATHER);
      g.fillRect(0, 0, w, h);
      g.setColor(LEATHER_LIGHT);
      g.drawRoundRect(6, 6, w - 13, h - BAR - 4, 14, 14);

      Rectangle book = new Rectangle(20, 16, w - 40, h - BAR - 26);
      Rectangle[] sheets = this.perView() == 2
         ? new Rectangle[]{new Rectangle(book.x, book.y, book.width / 2, book.height), new Rectangle(book.x + book.width / 2, book.y, book.width - book.width / 2, book.height)}
         : new Rectangle[]{book};
      this.paintPaper(g, book, sheets);
      Rectangle[] text = new Rectangle[sheets.length];
      for (int i = 0; i < sheets.length; i++) {
         Rectangle s = sheets[i];
         int outer = sheets.length == 2 ? 38 : 24;
         int inner = sheets.length == 2 ? 30 : 24;
         int left = sheets.length == 2 && i == 1 ? inner : outer;
         int right = sheets.length == 2 && i == 0 ? inner : outer;
         text[i] = new Rectangle(s.x + left, s.y + 40, s.width - left - right, s.height - 40 - 34);
      }
      switch (this.view) {
         case BOOKS:
            this.paintBooks(g, sheets, text);
            break;
         case CHAPTERS:
            this.paintChapters(g, sheets, text);
            break;
         case SAVE:
            this.paintSave(g, sheets, text);
            break;
         default:
            this.paintRead(g, sheets, text);
      }
      this.paintBar(g, w, h);
   }

   private void paintPaper(Graphics2D g, Rectangle book, Rectangle[] sheets) {
      // Page edges under the book, like the stack of paper in a thick book.
      for (int i = 3; i >= 1; i--) {
         g.setColor(i % 2 == 0 ? PAPER_EDGE : new Color(0xcdbd98));
         g.fillRoundRect(book.x - i, book.y + i, book.width + 2 * i, book.height, 6, 6);
      }
      g.setColor(PAPER);
      g.fillRect(book.x, book.y, book.width, book.height);
      if (sheets.length == 2) {
         // The spine: shading that deepens toward the fold.
         int fold = sheets[1].x;
         for (int i = 0; i < 26; i++) {
            int a = (26 - i) * 3;
            g.setColor(new Color(120, 90, 50, a / 2));
            g.fillRect(fold - i - 1, book.y, 1, book.height);
            g.fillRect(fold + i, book.y, 1, book.height);
         }
         g.setColor(new Color(120, 90, 50, 110));
         g.drawLine(fold, book.y, fold, book.y + book.height - 1);
      }
   }

   private void paintRead(Graphics2D g, Rectangle[] sheets, Rectangle[] text) {
      int lh = this.lineHeight();
      int lines = Math.max(1, text[0].height / lh);
      if (this.chapter == null || this.chapter.book != this.shelf.book || this.chapter.number != this.shelf.chapter
            || !this.chapter.translation.equals(this.shelf.translation)) {
         String ref = Books.NAMES[this.shelf.book] + " " + this.shelf.chapter;
         if (this.loadError != null) {
            this.centered(g, text[0], ref, this.loadError, "Try again");
            this.hits.add(new Hit(this.lastButton, () -> this.open(this.shelf.book, this.shelf.chapter, this.landAtEnd), 0));
         } else {
            this.centered(g, text[0], "Turning to " + ref + "...", "from bible-api.com", null);
         }
         return;
      }
      Font body = this.body();
      Font small = new Font("SansSerif", Font.BOLD, Math.max(9, Shelf.SIZES[this.shelf.size] * 6 / 10));
      Font numeral = new Font("Serif", Font.BOLD, Shelf.SIZES[this.shelf.size] * 3);
      Font title = new Font("Serif", Font.BOLD, Shelf.SIZES[this.shelf.size] * 17 / 10);
      FontMetrics bodyMetrics = g.getFontMetrics(body);
      String key = this.chapter.title() + "/" + this.chapter.translation + "/" + this.shelf.size + "/" + text[0].width + "/" + lines;
      if (!key.equals(this.pagesKey)) {
         this.pages = Layout.paginate(this.chapter, bodyMetrics, g.getFontMetrics(small), g.getFontMetrics(numeral), text[0].width, lines);
         this.pagesKey = key;
      }
      int per = sheets.length;
      if (this.spread < 0 || this.spread >= this.pages.size()) {
         int page = this.landAtEnd ? this.pages.size() - 1 : Layout.pageOf(this.pages, this.shelf.verse);
         this.spread = page - page % per;
         this.landAtEnd = false;
         this.placeChanged();
      } else if (this.spread % per != 0) {
         this.spread -= this.spread % per;
      }

      int space = bodyMetrics.stringWidth(" ");
      for (int s = 0; s < per; s++) {
         int index = this.spread + s;
         Rectangle area = text[s];
         if (index >= this.pages.size()) {
            this.paintChapterEnd(g, area);
            continue;
         }
         Layout.Page page = this.pages.get(index);
         this.runningHead(g, sheets[s], area, s, per, page);
         for (Layout.Run run : page.runs) {
            int top = area.y + run.line * lh;
            int base = top + lh * 3 / 4;
            int x = area.x + run.x;
            switch (run.kind) {
               case Layout.TITLE: {
                  g.setFont(title);
                  g.setColor(INK);
                  int tw = g.getFontMetrics().stringWidth(run.text);
                  int ty = area.y + lh * 2 - lh / 4;
                  g.drawString(run.text, area.x + (area.width - tw) / 2, ty);
                  g.setColor(RUBRIC);
                  int mid = area.x + area.width / 2;
                  g.drawLine(mid - 40, ty + 12, mid - 6, ty + 12);
                  g.fillOval(mid - 3, ty + 9, 6, 6);
                  g.drawLine(mid + 6, ty + 12, mid + 40, ty + 12);
                  break;
               }
               case Layout.NUMERAL:
                  g.setFont(numeral);
                  g.setColor(RUBRIC);
                  g.drawString(run.text, x, top + lh * 2 - lh / 4);
                  break;
               case Layout.VERSE_NUMBER:
                  this.word(g, run, x, top, lh, space);
                  g.setFont(small);
                  g.setColor(RUBRIC);
                  g.drawString(run.text, x, base - Shelf.SIZES[this.shelf.size] * 4 / 10);
                  break;
               default:
                  this.word(g, run, x, top, lh, space);
                  g.setFont(body);
                  g.setColor(INK);
                  g.drawString(run.text, x, base);
            }
         }
      }
      this.ribbon(g, sheets[per - 1]);
   }

   /** The highlight or hover tint behind a word, and its click area. */
   private void word(Graphics2D g, Layout.Run run, int x, int top, int lh, int space) {
      boolean marked = this.shelf.isMarked(this.shelf.book, this.shelf.chapter, run.verse);
      if (marked || run.verse == this.hoverVerse) {
         g.setColor(marked ? MARK : HOVER);
         g.fillRect(x - 1, top + 3, run.width + space + 1, lh - 4);
      }
      int verse = run.verse;
      this.hits.add(new Hit(new Rectangle(x - 1, top, run.width + space + 1, lh), () -> this.toggleMark(verse), verse));
   }

   private void runningHead(Graphics2D g, Rectangle sheet, Rectangle area, int side, int per, Layout.Page page) {
      g.setFont(HEAD);
      g.setColor(MUTED);
      String ref = Books.NAMES[this.shelf.book].toUpperCase() + " " + this.shelf.chapter + ":" + page.firstVerse
         + (page.lastVerse > page.firstVerse ? "-" + page.lastVerse : "");
      FontMetrics fm = g.getFontMetrics();
      int x = per == 2 && side == 0 ? area.x : area.x + area.width - fm.stringWidth(ref);
      g.drawString(ref, x, sheet.y + 24);
      String folio = (this.spread + side + 1) + " / " + this.pages.size();
      g.drawString(folio, area.x + (area.width - fm.stringWidth(folio)) / 2, sheet.y + sheet.height - 14);
      if (per == 1 || side == 1) {
         g.drawString(this.shelf.translation.toUpperCase(), area.x, sheet.y + 24);
      }
   }

   /** The facing page after a chapter's last page: what comes next. */
   private void paintChapterEnd(Graphics2D g, Rectangle area) {
      int ordinal = Books.ordinal(this.shelf.book, this.shelf.chapter) + 1;
      String next = ordinal < Books.TOTAL ? Books.NAMES[Books.bookOf(ordinal)] + " " + Books.chapterOf(ordinal) : null;
      if (next == null) {
         this.centered(g, area, "The end", "Revelation is the last book", null);
         return;
      }
      this.centered(g, area, "Next: " + next, "Turn the page to keep reading", "Next chapter \u203a");
      this.hits.add(new Hit(this.lastButton, this::nextChapter, 0));
   }

   private Rectangle lastButton;

   /** A heading, a line and an optional button, centered in an area; the button's bounds go to lastButton. */
   private void centered(Graphics2D g, Rectangle area, String heading, String line, String button) {
      int cy = area.y + area.height / 2 - 30;
      g.setFont(new Font("Serif", Font.BOLD, 22));
      g.setColor(INK);
      FontMetrics fm = g.getFontMetrics();
      g.drawString(heading, area.x + (area.width - fm.stringWidth(heading)) / 2, cy);
      g.setFont(new Font("Serif", Font.ITALIC, 15));
      g.setColor(MUTED);
      fm = g.getFontMetrics();
      g.drawString(line, area.x + (area.width - fm.stringWidth(line)) / 2, cy + 28);
      if (button != null) {
         g.setFont(UI);
         int bw = g.getFontMetrics().stringWidth(button) + 28;
         this.lastButton = new Rectangle(area.x + (area.width - bw) / 2, cy + 46, bw, 30);
         this.pill(g, this.lastButton, button, RUBRIC, Color.WHITE);
      }
   }

   private void pill(Graphics2D g, Rectangle r, String label, Color fill, Color ink) {
      boolean over = r.contains(this.mouseX, this.mouseY);
      g.setColor(over ? fill.brighter() : fill);
      g.fillRoundRect(r.x, r.y, r.width, r.height, r.height, r.height);
      g.setColor(ink);
      g.setFont(UI);
      FontMetrics fm = g.getFontMetrics();
      g.drawString(label, r.x + (r.width - fm.stringWidth(label)) / 2, r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
   }

   /** The red ribbon bookmark hanging over the top of the page. */
   private void ribbon(Graphics2D g, Rectangle sheet) {
      int x = sheet.x + sheet.width / 2 - 7;
      int y = sheet.y - 4;
      g.setColor(new Color(0x7d1d22));
      g.fillPolygon(new int[]{x, x + 14, x + 14, x + 7, x}, new int[]{y, y, y + 36, y + 30, y + 36}, 5);
      g.setColor(new Color(0xa8323a));
      g.fillRect(x + 2, y, 4, 32);
   }

   private void paintBooks(Graphics2D g, Rectangle[] sheets, Rectangle[] text) {
      int[] ot = range(0, Books.NEW_TESTAMENT);
      int[] nt = range(Books.NEW_TESTAMENT, Books.IDS.length);
      if (sheets.length == 2) {
         this.bookList(g, text[0], "Old Testament", ot, 2);
         this.bookList(g, text[1], "New Testament", nt, 2);
      } else {
         this.bookList(g, text[0], "Books", range(0, Books.IDS.length), 3);
      }
      g.setFont(HEAD);
      g.setColor(MUTED);
      String total = commas(this.shelf.readCount()) + " OF 1,189 CHAPTERS READ";
      g.drawString(total, text[0].x, sheets[0].y + 24);
      if (this.shelf.markCount() > 0) {
         String marks = this.shelf.markCount() + (this.shelf.markCount() == 1 ? " HIGHLIGHT (M)" : " HIGHLIGHTS (M)");
         Rectangle last = text[text.length - 1];
         g.drawString(marks, last.x + last.width - g.getFontMetrics().stringWidth(marks), sheets[0].y + 24);
      }
   }

   private static int[] range(int from, int to) {
      int[] r = new int[to - from];
      for (int i = 0; i < r.length; i++) {
         r[i] = from + i;
      }
      return r;
   }

   /** Book names in columns, each with a thin bar of how much of it has been read. */
   private void bookList(Graphics2D g, Rectangle area, String heading, int[] books, int columns) {
      g.setFont(new Font("Serif", Font.BOLD, 17));
      g.setColor(RUBRIC);
      g.drawString(heading, area.x, area.y + 14);
      int top = area.y + 28;
      int rows = Math.max(20, (books.length + columns - 1) / columns);
      int rowH = Math.max(14, Math.min(30, (area.y + area.height - top) / rows));
      int colW = area.width / columns;
      Font name = new Font("Serif", Font.PLAIN, Math.min(15, Math.max(11, rowH - 6)));
      for (int i = 0; i < books.length; i++) {
         int b = books[i];
         int x = area.x + (i / rows) * colW;
         int y = top + (i % rows) * rowH;
         Rectangle r = new Rectangle(x - 3, y, colW - 6, rowH);
         boolean over = r.contains(this.mouseX, this.mouseY);
         if (over) {
            g.setColor(HOVER);
            g.fillRect(r.x, r.y, r.width, r.height);
         }
         g.setFont(name);
         g.setColor(b == this.shelf.book ? RUBRIC : INK);
         g.drawString(Books.NAMES[b], x, y + rowH - 7);
         int read = this.shelf.readCount(b);
         int barW = colW - 16;
         g.setColor(PAPER_EDGE);
         g.fillRect(x, y + rowH - 4, barW, 2);
         if (read > 0) {
            g.setColor(read == Books.CHAPTERS[b] ? GOLD : RUBRIC);
            g.fillRect(x, y + rowH - 4, Math.max(2, barW * read / Books.CHAPTERS[b]), 2);
         }
         this.hits.add(new Hit(r, () -> this.showChapters(b), 0));
      }
   }

   private void paintChapters(Graphics2D g, Rectangle[] sheets, Rectangle[] text) {
      int b = this.pickedBook;
      Rectangle info = text[0];
      Rectangle grid = sheets.length == 2 ? text[1] : new Rectangle(text[0].x, text[0].y + 150, text[0].width, text[0].height - 150);
      g.setFont(HEAD);
      g.setColor(MUTED);
      g.drawString(b < Books.NEW_TESTAMENT ? "OLD TESTAMENT" : "NEW TESTAMENT", info.x, sheets[0].y + 24);
      g.setFont(new Font("Serif", Font.BOLD, 30));
      g.setColor(INK);
      g.drawString(Books.NAMES[b], info.x, info.y + 34);
      int read = this.shelf.readCount(b);
      g.setFont(new Font("Serif", Font.ITALIC, 15));
      g.setColor(MUTED);
      g.drawString(Books.CHAPTERS[b] == 1 ? (read == 1 ? "One chapter, read" : "One chapter") : Books.CHAPTERS[b] + " chapters, " + read + " read", info.x, info.y + 60);
      int first = 1;
      while (first < Books.CHAPTERS[b] && this.shelf.isRead(b, first)) {
         first++;
      }
      int start = read == Books.CHAPTERS[b] ? 1 : first;
      g.setFont(UI);
      String go = (read == 0 || read == Books.CHAPTERS[b] ? "Start reading" : "Continue at " + start) + " \u203a";
      Rectangle goButton = new Rectangle(info.x, info.y + 80, g.getFontMetrics().stringWidth(go) + 30, 32);
      this.pill(g, goButton, go, RUBRIC, Color.WHITE);
      this.hits.add(new Hit(goButton, () -> this.open(b, start, false), 0));
      String save = "Save as a web page";
      Rectangle saveButton = new Rectangle(goButton.x + goButton.width + 10, goButton.y, g.getFontMetrics().stringWidth(save) + 30, 32);
      this.pill(g, saveButton, save, PAPER_EDGE, INK);
      this.hits.add(new Hit(saveButton, () -> this.showSave(b), 0));
      if (sheets.length == 2) {
         g.setFont(new Font("Serif", Font.ITALIC, 14));
         g.setColor(MUTED);
         String[] notes = {"Filled squares are chapters you have", "read to their last page. Pick any", "chapter to open it.", "", "Esc goes back to the books."};
         for (int i = 0; i < notes.length; i++) {
            g.drawString(notes[i], info.x, info.y + 150 + i * 20);
         }
      }

      int n = Books.CHAPTERS[b];
      int cell = 46;
      while (cell > 18) {
         int cols = Math.max(1, grid.width / cell);
         if (((n + cols - 1) / cols) * cell <= grid.height) {
            break;
         }
         cell--;
      }
      int cols = Math.max(1, grid.width / cell);
      Font num = new Font("SansSerif", Font.BOLD, Math.max(9, cell / 3));
      g.setFont(num);
      FontMetrics fm = g.getFontMetrics();
      for (int c = 1; c <= n; c++) {
         int x = grid.x + ((c - 1) % cols) * cell;
         int y = grid.y + ((c - 1) / cols) * cell;
         Rectangle r = new Rectangle(x + 2, y + 2, cell - 4, cell - 4);
         boolean isRead = this.shelf.isRead(b, c);
         boolean here = b == this.shelf.book && c == this.shelf.chapter;
         boolean over = r.contains(this.mouseX, this.mouseY);
         g.setColor(isRead ? (over ? RUBRIC.brighter() : RUBRIC) : (over ? HOVER : PAPER));
         g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
         g.setColor(isRead ? RUBRIC : PAPER_EDGE);
         g.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);
         if (here) {
            g.setColor(GOLD);
            g.drawRoundRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2, 7, 7);
            g.drawRoundRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4, 8, 8);
         }
         String label = String.valueOf(c);
         g.setColor(isRead ? Color.WHITE : INK);
         g.drawString(label, r.x + (r.width - fm.stringWidth(label)) / 2, r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
         int chosen = c;
         this.hits.add(new Hit(r, () -> this.open(b, chosen, false), 0));
      }
   }

   private void paintSave(Graphics2D g, Rectangle[] sheets, Rectangle[] text) {
      int b = this.pickedBook;
      Rectangle area = text[0];
      g.setFont(HEAD);
      g.setColor(MUTED);
      g.drawString("SAVE AS A WEB PAGE", area.x, sheets[0].y + 24);
      g.setFont(new Font("Serif", Font.BOLD, 26));
      g.setColor(INK);
      g.drawString(Books.NAMES[b], area.x, area.y + 30);
      g.setFont(new Font("Serif", Font.ITALIC, 14));
      g.setColor(MUTED);
      g.drawString(BibleApi.TRANSLATION_NAMES[BibleApi.translationIndex(this.shelf.translation)] + " (T changes it)", area.x, area.y + 52);

      int y = area.y + 78;
      this.stepper(g, area.x, y, "From chapter", this.saveFrom, d -> {
         this.saveFrom = Math.max(1, Math.min(Books.CHAPTERS[b], this.saveFrom + d));
         this.saveTo = Math.max(this.saveTo, this.saveFrom);
      });
      this.stepper(g, area.x, y + 38, "To chapter", this.saveTo, d -> {
         this.saveTo = Math.max(1, Math.min(Books.CHAPTERS[b], this.saveTo + d));
         this.saveFrom = Math.min(this.saveFrom, this.saveTo);
      });
      this.check(g, area.x, y + 86, "One file for all the chapters", this.saveOneFile, () -> this.saveOneFile = !this.saveOneFile);
      this.check(g, area.x, y + 114, "Chapter headings and verse numbers", this.saveNumbers, () -> this.saveNumbers = !this.saveNumbers);

      int count = this.saveTo - this.saveFrom + 1;
      String file = this.saveOneFile || count == 1 ? DownloadManager.stem(b) + (this.saveOneFile ? "" : String.valueOf(this.saveFrom)) + ".html" : count + " files, " + DownloadManager.stem(b) + this.saveFrom + ".html ...";
      g.setFont(UI);
      String label = this.saving ? "Saving..." : "Save " + (count == 1 ? "1 chapter" : count + " chapters");
      Rectangle button = new Rectangle(area.x, y + 146, g.getFontMetrics().stringWidth(label) + 30, 32);
      this.pill(g, button, label, this.saving ? MUTED : RUBRIC, Color.WHITE);
      if (!this.saving) {
         this.hits.add(new Hit(button, this::startSave, 0));
      }
      g.setFont(new Font("Serif", Font.ITALIC, 13));
      g.setColor(MUTED);
      g.drawString(file, button.x + button.width + 12, button.y + 21);

      // The original app's progress bar: red, filling green.
      Rectangle bar = new Rectangle(area.x, y + 194, Math.min(area.width, 340), 18);
      g.setColor(new Color(0xc0392b));
      g.fillRoundRect(bar.x, bar.y, bar.width, bar.height, 6, 6);
      g.setColor(new Color(0x2e9e4f));
      g.fillRoundRect(bar.x, bar.y, bar.width * this.savePercent / 100, bar.height, 6, 6);
      g.setColor(Color.WHITE);
      g.setFont(new Font("SansSerif", Font.BOLD, 11));
      g.drawString(this.savePercent + "%", bar.x + 6, bar.y + 13);
      g.setColor(INK);
      g.setFont(new Font("Serif", Font.PLAIN, 13));
      this.wrap(g, this.saveStatus, bar.x, bar.y + 34, area.width, 17);

      Rectangle notes = sheets.length == 2 ? text[1] : new Rectangle(area.x, bar.y + 70, area.width, area.height);
      g.setFont(new Font("Serif", Font.ITALIC, 14));
      g.setColor(MUTED);
      int ny = notes.y + (sheets.length == 2 ? 20 : 0);
      if (this.isBrowser()) {
         ny = this.wrap(g, "Files go to your Downloads folder: the browser saves them for you.", notes.x, ny, notes.width, 20) + 10;
      } else {
         ny = this.wrap(g, "Files go to your Downloads folder:", notes.x, ny, notes.width, 20);
         g.drawString(fitEnd(g.getFontMetrics(), DownloadManager.folder().getPath(), notes.width), notes.x, ny);
         ny += 30;
      }
      ny = this.wrap(g, "Chapters come from bible-api.com, which asks for no more than 15 requests every 30 seconds, so one chapter is fetched every 2 seconds. Luke's 24 chapters take under a minute.", notes.x, ny, notes.width, 20) + 10;
      if (sheets.length == 2) {
         this.wrap(g, "Esc goes back to reading.", notes.x, ny, notes.width, 20);
      }
   }

   private boolean isBrowser() {
      return "/".equals(System.getProperty("user.home"));
   }

   private interface Step {
      void by(int delta);
   }

   private void stepper(Graphics2D g, int x, int y, String label, int value, Step step) {
      g.setFont(new Font("Serif", Font.PLAIN, 15));
      g.setColor(INK);
      g.drawString(label, x, y + 20);
      int bx = x + 112;
      Rectangle minus = new Rectangle(bx, y, 30, 28);
      Rectangle plus = new Rectangle(bx + 86, y, 30, 28);
      this.pill(g, minus, "\u2212", PAPER_EDGE, INK);
      this.pill(g, plus, "+", PAPER_EDGE, INK);
      g.setFont(new Font("Serif", Font.BOLD, 18));
      g.setColor(INK);
      String v = String.valueOf(value);
      g.drawString(v, bx + 58 - g.getFontMetrics().stringWidth(v) / 2, y + 21);
      if (!this.saving) {
         this.hits.add(new Hit(minus, () -> step.by(-1), 0));
         this.hits.add(new Hit(plus, () -> step.by(1), 0));
      }
   }

   private void check(Graphics2D g, int x, int y, String label, boolean on, Runnable toggle) {
      g.setColor(on ? RUBRIC : PAPER);
      g.fillRoundRect(x, y, 18, 18, 4, 4);
      g.setColor(on ? RUBRIC : MUTED);
      g.drawRoundRect(x, y, 18, 18, 4, 4);
      if (on) {
         g.setColor(Color.WHITE);
         g.drawLine(x + 4, y + 9, x + 8, y + 13);
         g.drawLine(x + 8, y + 13, x + 14, y + 5);
         g.drawLine(x + 4, y + 10, x + 8, y + 14);
         g.drawLine(x + 8, y + 14, x + 14, y + 6);
      }
      g.setFont(new Font("Serif", Font.PLAIN, 15));
      g.setColor(INK);
      g.drawString(label, x + 28, y + 14);
      if (!this.saving) {
         this.hits.add(new Hit(new Rectangle(x - 2, y - 4, 28 + g.getFontMetrics().stringWidth(label) + 4, 26), toggle, 0));
      }
   }

   /** Draws text wrapped to a width; returns the y below the last line. */
   private int wrap(Graphics2D g, String text, int x, int y, int width, int lh) {
      FontMetrics fm = g.getFontMetrics();
      StringBuilder line = new StringBuilder();
      for (String word : text.split(" ")) {
         String tried = line.length() == 0 ? word : line + " " + word;
         if (fm.stringWidth(tried) > width && line.length() > 0) {
            g.drawString(line.toString(), x, y);
            y += lh;
            line = new StringBuilder(word);
         } else {
            line = new StringBuilder(tried);
         }
      }
      if (line.length() > 0) {
         g.drawString(line.toString(), x, y);
         y += lh;
      }
      return y;
   }

   /** The toolbar under the book. */
   private void paintBar(Graphics2D g, int w, int h) {
      int y = h - BAR + 10;
      int bh = 34;
      boolean wide = w >= TWO_PAGE_WIDTH;
      boolean reading = this.view == View.READ;
      g.setFont(UI);
      int x = 14;
      x = this.barButton(g, x, y, bh, wide ? "\u2039 Prev" : "\u2039", this::previous, reading);
      x = this.barButton(g, x, y, bh, this.view == View.BOOKS ? "Close" : "Books", this::showBooks, true);
      int right = w - 14;
      right = this.barButtonRight(g, right, y, bh, wide ? "Next \u203a" : "\u203a", this::next, reading);
      if (wide) {
         right = this.barButtonRight(g, right, y, bh, "Save", () -> this.showSave(this.shelf.book), true);
         right = this.barButtonRight(g, right, y, bh, "A+", () -> this.resize(1), this.shelf.size < Shelf.SIZES.length - 1);
         right = this.barButtonRight(g, right, y, bh, "A\u2212", () -> this.resize(-1), this.shelf.size > 0);
      }
      right = this.barButtonRight(g, right, y, bh, this.shelf.translation.toUpperCase(), this::cycleTranslation, true);
      if (!wide) {
         right = this.barButtonRight(g, right, y, bh, "Save", () -> this.showSave(this.shelf.book), true);
      }

      // The middle: where you are.
      int mid = (x + right) / 2;
      int room = right - x - 12;
      g.setFont(new Font("Serif", Font.BOLD, 16));
      g.setColor(BUTTON_TEXT);
      String top = fit(g.getFontMetrics(), Books.NAMES[this.shelf.book] + " " + this.shelf.chapter, room);
      g.drawString(top, mid - g.getFontMetrics().stringWidth(top) / 2, y + 15);
      g.setFont(UI_SMALL);
      g.setColor(new Color(0xb9a888));
      String sub = null;
      for (String option : this.subtitles()) {
         if (sub == null || g.getFontMetrics().stringWidth(sub) > room) {
            sub = option;
         }
      }
      sub = fit(g.getFontMetrics(), sub, room);
      g.drawString(sub, mid - g.getFontMetrics().stringWidth(sub) / 2, y + 31);

      // A passing message floats just above the toolbar.
      if (this.flash != null) {
         g.setFont(new Font("SansSerif", Font.BOLD, 12));
         FontMetrics fm = g.getFontMetrics();
         String text = fit(fm, this.flash, w - 60);
         int tw = fm.stringWidth(text) + 28;
         int ty = h - BAR - 46;
         g.setColor(new Color(43, 33, 24, 230));
         g.fillRoundRect(w / 2 - tw / 2, ty, tw, 26, 26, 26);
         g.setColor(GOLD);
         g.drawString(text, w / 2 - tw / 2 + 14, ty + 17);
      }
   }

   /** Toolbar subtitles from longest to shortest; the longest that fits is shown. */
   private String[] subtitles() {
      if (this.view == View.READ && this.pages != null && this.chapter != null && this.loadError == null && this.spread >= 0) {
         int last = Math.min(this.pages.size(), this.spread + this.perView());
         String pageText = last - this.spread > 1 ? "pages " + (this.spread + 1) + "-" + last : "page " + (this.spread + 1);
         return new String[]{pageText + " of " + this.pages.size() + "  \u00b7  click a verse to highlight it", pageText + " of " + this.pages.size(), last + " / " + this.pages.size()};
      }
      String read = commas(this.shelf.readCount());
      return new String[]{read + " of 1,189 chapters read", read + " of 1,189 read", read + " read"};
   }

   static String commas(int n) {
      String digits = String.valueOf(n);
      return n >= 1000 ? digits.substring(0, digits.length() - 3) + "," + digits.substring(digits.length() - 3) : digits;
   }

   /** Keeps the end of a long path, which says the most. */
   private static String fitEnd(FontMetrics fm, String text, int width) {
      String t = text;
      while (t.length() > 1 && fm.stringWidth("..." + t) > width) {
         t = t.substring(1);
      }
      return t.equals(text) ? text : "..." + t;
   }

   private static String fit(FontMetrics fm, String text, int width) {
      if (fm.stringWidth(text) <= width) {
         return text;
      }
      String t = text;
      while (t.length() > 1 && fm.stringWidth(t + "...") > width) {
         t = t.substring(0, t.length() - 1);
      }
      return t + "...";
   }

   private int barButton(Graphics2D g, int x, int y, int h, String label, Runnable action, boolean enabled) {
      int bw = this.drawBarButton(g, x, y, h, label, action, enabled);
      return x + bw + 6;
   }

   private int barButtonRight(Graphics2D g, int right, int y, int h, String label, Runnable action, boolean enabled) {
      g.setFont(UI);
      int bw = g.getFontMetrics().stringWidth(label) + 24;
      this.drawBarButton(g, right - bw, y, h, label, action, enabled);
      return right - bw - 6;
   }

   private int drawBarButton(Graphics2D g, int x, int y, int h, String label, Runnable action, boolean enabled) {
      g.setFont(UI);
      FontMetrics fm = g.getFontMetrics();
      int bw = fm.stringWidth(label) + 24;
      Rectangle r = new Rectangle(x, y, bw, h);
      boolean over = enabled && r.contains(this.mouseX, this.mouseY);
      g.setColor(over ? BUTTON_HOVER : BUTTON);
      g.fillRoundRect(x, y, bw, h, 10, 10);
      g.setColor(enabled ? BUTTON_TEXT : new Color(0x8c7a62));
      g.drawString(label, x + 12, y + (h + fm.getAscent() - fm.getDescent()) / 2);
      if (enabled) {
         this.hits.add(new Hit(r, action, 0));
      }
      return bw;
   }
}
