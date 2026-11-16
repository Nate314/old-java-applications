package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/**
 * Opens above the taskbar clock, as on Windows: an analog clock with the time to the second and the long date, and a
 * month calendar with today lit. The arrows page the months, the month's name comes back to today, and a click on a
 * day says how many days away it is.
 */
public class CalendarFlyout extends JInternalFrame {
   static final String[] MONTHS = {
      "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
   };
   private static final String[] WEEKDAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
   private static final Color BACKGROUND = new Color(26, 35, 54);
   private static final Color DIM = new Color(110, 128, 156);
   private static final Color BLUE = new Color(0, 120, 215);
   private static final Color BRIGHT = new Color(0, 150, 255);
   private static final int PAD = 14;
   private static final int FACE = 84;
   private static final int TOP = PAD * 2 + FACE;
   private static final int MONTH_ROW = 34;
   private static final int WEEKDAY_ROW = 22;
   private static final int CELL_HEIGHT = 32;
   private static final int FOOTER = 34;
   private static final int WIDTH = 292;
   private static final int HEIGHT = TOP + MONTH_ROW + WEEKDAY_ROW + 6 * CELL_HEIGHT + FOOTER;

   private final Face face = new Face();
   // Repaints the face itself: in the browser, repainting the frame alone does not redraw what is inside it.
   private final Timer ticker = new Timer(1000, e -> this.face.repaint());
   private int year;
   private int month;
   /** The clicked day, as a day number (see days), or Integer.MIN_VALUE for none. */
   private int picked = Integer.MIN_VALUE;

   public CalendarFlyout(Main screen) {
      super("Calendar", false, false, false);
      this.setVisible(false);
      this.setBorder(null);
      this.setCursor(screen.getCursor());
      ((BasicInternalFrameUI)this.getUI()).setNorthPane(null);
      int width = Math.min(WIDTH, screen.getWidth() - 8);
      int bottom = screen.getHeight() - screen.launcherWindow.getHeight();
      this.setSize(width, HEIGHT);
      this.setLocation(screen.getWidth() - width - 4, Math.max(0, bottom - HEIGHT - 4));
      this.add(this.face);
   }

   public void toggleVisibility() {
      if (this.isVisible()) {
         this.hideMenu();
         return;
      }
      int[] today = today();
      this.year = today[0];
      this.month = today[1];
      this.picked = Integer.MIN_VALUE;
      this.setVisible(true);
      this.moveToFront();
      this.ticker.start();
   }

   public void hideMenu() {
      this.setVisible(false);
      this.ticker.stop();
   }

   /** Today as {year, month 0-11, day}, from Date's local-time getters (in TeaVM, Calendar's time zone is UTC). */
   @SuppressWarnings("deprecation")
   static int[] today() {
      Date now = new Date();
      return new int[] {1900 + now.getYear(), now.getMonth(), now.getDate()};
   }

   /** Days since 1970-01-01 of a date (month 0-11; days past the month's end roll over), proleptic Gregorian. */
   static int days(int year, int month, int day) {
      int y = year + Math.floorDiv(month, 12);
      int m = Math.floorMod(month, 12) + 1;
      y -= m <= 2 ? 1 : 0;
      int era = Math.floorDiv(y, 400);
      int yoe = y - era * 400;
      int doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + day - 1;
      int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
      return era * 146097 + doe - 719468;
   }

   /** The date of a day number, as {year, month 0-11, day}. */
   static int[] date(int days) {
      int z = days + 719468;
      int era = Math.floorDiv(z, 146097);
      int doe = z - era * 146097;
      int yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
      int doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
      int mp = (5 * doy + 2) / 153;
      int day = doy - (153 * mp + 2) / 5 + 1;
      int m = mp < 10 ? mp + 3 : mp - 9;
      return new int[] {yoe + era * 400 + (m <= 2 ? 1 : 0), m - 1, day};
   }

   /** 0 for Sunday to 6 for Saturday (1970-01-01 was a Thursday). */
   static int weekday(int days) {
      return Math.floorMod(days + 4, 7);
   }

   /** The day number of the calendar's first cell: the Sunday on or before the 1st of the month. */
   static int firstCell(int year, int month) {
      int first = days(year, month, 1);
      return first - weekday(first);
   }

   /** What the footer says of a picked day, counted from today. */
   static String distance(int picked, int today) {
      int[] d = date(picked);
      String name = WEEKDAYS[weekday(picked)] + ", " + MONTHS[d[1]] + " " + d[2] + (d[0] != date(today)[0] ? ", " + d[0] : "");
      int away = picked - today;
      if (away == 0) {
         return name + ": today";
      }
      int n = Math.abs(away);
      String count = n == 1 ? (away > 0 ? "tomorrow" : "yesterday") : away > 0 ? "in " + n + " days" : n + " days ago";
      return name + ": " + count;
   }

   /** What the footer says when no day is picked: how far through the year today is. */
   static String yearLine(int today) {
      int year = date(today)[0];
      int dayOfYear = today - days(year, 0, 1) + 1;
      return "Day " + dayOfYear + " of " + (days(year + 1, 0, 1) - days(year, 0, 1));
   }

   private Rectangle previous() {
      return new Rectangle(this.getWidth() - PAD - 60, TOP + 3, 28, 28);
   }

   private Rectangle next() {
      return new Rectangle(this.getWidth() - PAD - 28, TOP + 3, 28, 28);
   }

   private Rectangle title() {
      return new Rectangle(PAD - 4, TOP + 3, this.getWidth() - 2 * PAD - 64, 28);
   }

   private int cellWidth() {
      return (this.getWidth() - 2 * PAD) / 7;
   }

   private Rectangle cell(int i) {
      int w = this.cellWidth();
      return new Rectangle(PAD + i % 7 * w, TOP + MONTH_ROW + WEEKDAY_ROW + i / 7 * CELL_HEIGHT, w, CELL_HEIGHT);
   }

   private void page(int by) {
      int[] d = date(days(this.year, this.month + by, 1));
      this.year = d[0];
      this.month = d[1];
      this.face.repaint();
   }

   private void click(int x, int y) {
      if (this.previous().contains(x, y)) {
         this.page(-1);
      } else if (this.next().contains(x, y)) {
         this.page(1);
      } else if (this.title().contains(x, y)) {
         int[] today = today();
         this.year = today[0];
         this.month = today[1];
         this.picked = Integer.MIN_VALUE;
         this.face.repaint();
      } else {
         for (int i = 0; i < 42; i++) {
            if (this.cell(i).contains(x, y)) {
               int day = firstCell(this.year, this.month) + i;
               this.picked = this.picked == day ? Integer.MIN_VALUE : day;
               this.face.repaint();
            }
         }
      }
   }

   /** Everything in the flyout, painted in one component and hit-tested by click. */
   private final class Face extends JComponent {
      private int hoverX = -1;
      private int hoverY = -1;

      Face() {
         MouseAdapter mouse = new MouseAdapter() {
            // On the press, not the click: in the browser a click whose press came right after the pointer jumped in was lost.
            @Override
            public void mousePressed(MouseEvent e) {
               if (e.getButton() == MouseEvent.BUTTON1) {
                  CalendarFlyout.this.click(e.getX(), e.getY());
               }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
               Face.this.hoverX = e.getX();
               Face.this.hoverY = e.getY();
               Face.this.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
               Face.this.hoverX = -1;
               Face.this.repaint();
            }
         };
         this.addMouseListener(mouse);
         this.addMouseMotionListener(mouse);
      }

      private boolean hovered(Rectangle r) {
         return this.hoverX >= 0 && r.contains(this.hoverX, this.hoverY);
      }

      @Override
      @SuppressWarnings("deprecation")
      protected void paintComponent(Graphics g) {
         AppIcon.smooth(g);
         CalendarFlyout flyout = CalendarFlyout.this;
         int w = this.getWidth();
         g.setColor(BACKGROUND);
         g.fillRect(0, 0, w, this.getHeight());
         g.setColor(BLUE);
         g.fillRect(0, 0, w, 3);

         Date now = new Date();
         this.paintClock((Graphics2D) g, PAD, PAD, now);
         int hour = now.getHours() % 12 == 0 ? 12 : now.getHours() % 12;
         String time = hour + ":" + two(now.getMinutes()) + ":" + two(now.getSeconds()) + (now.getHours() < 12 ? " AM" : " PM");
         int textX = PAD * 2 + FACE;
         g.setColor(FlatButton.TEXT);
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
         g.drawString(AppIcon.fit(time, g.getFontMetrics(), w - textX - 6), textX, PAD + 34);
         g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
         FontMetrics metrics = g.getFontMetrics();
         g.drawString(WEEKDAYS[now.getDay()], textX, PAD + 58);
         g.drawString(AppIcon.fit(MONTHS[now.getMonth()] + " " + now.getDate() + ", " + (1900 + now.getYear()), metrics, w - textX - 6), textX, PAD + 76);
         g.setColor(LauncherWindow.HOVER);
         g.fillRect(PAD, TOP - 1, w - 2 * PAD, 1);

         int[] today = today();
         int todayNumber = days(today[0], today[1], today[2]);
         boolean thisMonth = flyout.year == today[0] && flyout.month == today[1];
         Rectangle title = flyout.title();
         if (this.hovered(title) && !thisMonth) {
            g.setColor(LauncherWindow.HOVER);
            g.fillRoundRect(title.x, title.y, title.width, title.height, 8, 8);
         }
         g.setColor(FlatButton.TEXT);
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
         g.drawString(MONTHS[flyout.month] + " " + flyout.year, PAD, TOP + 23);
         this.paintArrow(g, flyout.previous(), -1);
         this.paintArrow(g, flyout.next(), 1);

         int cw = flyout.cellWidth();
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
         metrics = g.getFontMetrics();
         g.setColor(DIM);
         for (int i = 0; i < 7; i++) {
            String name = WEEKDAYS[i].substring(0, 2);
            g.drawString(name, PAD + i * cw + (cw - metrics.stringWidth(name)) / 2, TOP + MONTH_ROW + 15);
         }

         int first = firstCell(flyout.year, flyout.month);
         g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
         metrics = g.getFontMetrics();
         for (int i = 0; i < 42; i++) {
            int number = first + i;
            int[] d = date(number);
            Rectangle c = flyout.cell(i);
            int size = Math.min(c.width, c.height) - 4;
            int cx = c.x + (c.width - size) / 2;
            int cy = c.y + (c.height - size) / 2;
            if (number == todayNumber) {
               g.setColor(BLUE);
               g.fillOval(cx, cy, size, size);
            } else if (this.hovered(c)) {
               g.setColor(LauncherWindow.HOVER);
               g.fillOval(cx, cy, size, size);
            }
            if (number == flyout.picked) {
               g.setColor(BRIGHT);
               g.drawOval(cx - 1, cy - 1, size + 1, size + 1);
            }
            g.setColor(number == todayNumber ? Color.WHITE : d[1] == flyout.month ? FlatButton.TEXT : DIM);
            String label = String.valueOf(d[2]);
            g.drawString(label, c.x + (c.width - metrics.stringWidth(label)) / 2, c.y + (c.height + metrics.getAscent() - metrics.getDescent()) / 2);
         }

         int footerY = this.getHeight() - FOOTER;
         g.setColor(LauncherWindow.HOVER);
         g.fillRect(PAD, footerY, w - 2 * PAD, 1);
         g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
         metrics = g.getFontMetrics();
         String footer = flyout.picked == Integer.MIN_VALUE ? yearLine(todayNumber) + ". Click a day to count to it." : distance(flyout.picked, todayNumber);
         g.setColor(flyout.picked == Integer.MIN_VALUE ? DIM : FlatButton.TEXT);
         g.drawString(AppIcon.fit(footer, metrics, w - 2 * PAD), PAD, footerY + 22);
      }

      private void paintArrow(Graphics g, Rectangle r, int direction) {
         if (this.hovered(r)) {
            g.setColor(LauncherWindow.HOVER);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
         }
         g.setColor(FlatButton.TEXT);
         int mx = r.x + r.width / 2;
         int my = r.y + r.height / 2;
         g.fillPolygon(new int[] {mx + 3 * direction, mx - 3 * direction, mx - 3 * direction}, new int[] {my, my - 6, my + 6}, 3);
      }

      @SuppressWarnings("deprecation")
      private void paintClock(Graphics2D g, int x, int y, Date now) {
         int r = FACE / 2;
         int cx = x + r;
         int cy = y + r;
         g.setColor(new Color(36, 48, 72));
         g.fillOval(x, y, FACE, FACE);
         g.setColor(LauncherWindow.ON);
         g.drawOval(x, y, FACE, FACE);
         g.setColor(DIM);
         for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            int inner = i % 3 == 0 ? r - 10 : r - 6;
            g.setStroke(new BasicStroke(i % 3 == 0 ? 2.5f : 1.5f));
            g.drawLine(cx + (int) Math.round(Math.sin(a) * inner), cy - (int) Math.round(Math.cos(a) * inner),
               cx + (int) Math.round(Math.sin(a) * (r - 3)), cy - (int) Math.round(Math.cos(a) * (r - 3)));
         }
         double seconds = now.getSeconds();
         double minutes = now.getMinutes() + seconds / 60;
         double hours = now.getHours() % 12 + minutes / 60;
         g.setColor(FlatButton.TEXT);
         hand(g, cx, cy, hours / 12, r * 0.5, 4f);
         hand(g, cx, cy, minutes / 60, r * 0.75, 3f);
         g.setColor(BRIGHT);
         hand(g, cx, cy, seconds / 60, r * 0.82, 1.5f);
         g.fillOval(cx - 3, cy - 3, 6, 6);
         g.setStroke(new BasicStroke(1f));
      }

      private void hand(Graphics2D g, int cx, int cy, double turn, double length, float width) {
         double a = Math.PI * 2 * turn;
         g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
         g.drawLine(cx, cy, cx + (int) Math.round(Math.sin(a) * length), cy - (int) Math.round(Math.cos(a) * length));
      }
   }

   private static String two(int n) {
      return (n < 10 ? "0" : "") + n;
   }
}
