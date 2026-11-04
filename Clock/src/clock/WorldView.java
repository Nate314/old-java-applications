package clock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The World tab: where everyone is in their day. A 24-hour dial (noon at the top, midnight at the bottom) has a hand
 * for here and for each picked city; under it a strip per city lays out here's day hour by hour in that city's time,
 * colored night, morning or evening, and working hours, with the hours that suit everyone ringed in green. Dragging
 * the strips (or Left and Right) moves the time shown, to plan a call.
 */
final class WorldView implements Tab {
   static final int MAX_CITIES = 5;
   static final int WORK_FROM = 9;
   static final int WORK_TO = 17;
   static final int AWAKE_FROM = 7;
   static final int AWAKE_TO = 22;

   static final Color NIGHT = new Color(22, 34, 84);
   static final Color AWAKE = new Color(150, 200, 235);
   static final Color WORK = new Color(255, 214, 90);
   private static final Color[] COLORS = {
      Ui.HAND, new Color(0, 90, 200), new Color(0, 130, 60), new Color(140, 40, 170), new Color(220, 110, 0),
      new Color(120, 80, 40)
   };
   private static final String[] DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
   private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July", "August",
      "September", "October", "November", "December"};
   private static final long MINUTE = Countdown.MINUTE;
   private static final long HOUR = 60 * MINUTE;

   private final Main clock;
   /** The instant shown, or -1 to follow the time now. */
   private long shown = -1;
   private boolean picking;
   private String message;
   private Rectangle[] buttons;
   private Rectangle strips;
   private long dragDay;
   private boolean dragging;
   private Rectangle[] cityBoxes;
   private Rectangle done;

   WorldView(Main clock) {
      this.clock = clock;
   }

   private List<Zone> cities() {
      return this.clock.state.cities;
   }

   /** Here first, then the picked cities. */
   private List<Zone> zones() {
      List<Zone> zones = new ArrayList<>();
      zones.add(Zone.HERE);
      zones.addAll(this.cities());
      return zones;
   }

   private long shown(long now) {
      return this.shown < 0 ? now : this.shown;
   }

   private boolean twentyFour() {
      return this.clock.state.twentyFour;
   }

   @Override
   public String label(long now) {
      return "World";
   }

   @Override
   public String hint() {
      return this.picking ? "Click a city to add or remove it   Enter: done"
            : "Drag the strips or Left/Right: plan   Space: now   A: cities";
   }

   // ---- Painting ----

   @Override
   public void paint(Graphics2D g, Rectangle area, long now) {
      if (this.picking) {
         this.paintPicker(g, area, now);
         return;
      }
      long t = this.shown(now);
      List<Zone> zones = this.zones();
      int gap = Math.max(4, area.height / 50);
      int buttonHeight = clamp(area.height / 9, 24, 44);
      int statusHeight = clamp(area.height / 16, 12, 18);
      int rowHeight = clamp(area.height * 45 / 100 / zones.size(), 16, 34);
      Rectangle buttonRow = new Rectangle(area.x, area.y + area.height - buttonHeight, area.width, buttonHeight);
      Rectangle status = new Rectangle(area.x, buttonRow.y - gap - statusHeight, area.width, statusHeight);
      Rectangle rows = new Rectangle(area.x, status.y - gap - rowHeight * zones.size(), area.width, rowHeight * zones.size());
      Rectangle top = new Rectangle(area.x, area.y, area.width, rows.y - gap - area.y);

      int side = Math.max(40, Math.min(top.height, top.width * 55 / 100));
      Rectangle dial = new Rectangle(top.x, top.y + (top.height - side) / 2, side, side);
      this.paintDial(g, dial, zones, t);
      this.paintInfo(g, new Rectangle(dial.x + side + gap, top.y, top.width - side - gap, top.height), t, now);

      boolean[] together = together(zones, Zone.HERE.midnight(t), WORK_FROM, WORK_TO);
      this.paintRows(g, rows, zones, t, together);

      g.setColor(Ui.INK);
      Ui.line(g, this.plan(zones, t, together), Ui.LABELS, status);

      this.buttons = Ui.row(buttonRow, 4, gap);
      Ui.button(g, this.buttons[0], "Earlier", false);
      Ui.button(g, this.buttons[1], "Now", this.shown >= 0);
      Ui.button(g, this.buttons[2], "Later", false);
      Ui.button(g, this.buttons[3], "Cities", false);
   }

   /** The 24-hour dial: the hours as a colored ring, and a hand per zone at its local time. */
   private void paintDial(Graphics2D g, Rectangle box, List<Zone> zones, long t) {
      int r = box.width / 2 - 2;
      int cx = box.x + box.width / 2;
      int cy = box.y + box.height / 2;
      int face = r * 64 / 100;
      for (int hour = 0; hour < 24; hour++) {
         g.setColor(color(hour));
         // A hair wider than an hour, so no seam shows between neighbours.
         Ui.band(g, cx, cy, r, face - 1, (hour - 12) / 24.0, 1 / 24.0 + 0.002);
      }
      g.setColor(Ui.FACE);
      g.fillOval(cx - face, cy - face, 2 * face, 2 * face);
      g.setColor(Ui.INK);
      g.setStroke(new BasicStroke(Math.max(1, r / 50f)));
      g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
      g.drawOval(cx - face, cy - face, 2 * face, 2 * face);

      // The hour numbers round the ring: every hour on a big dial, every third on a small one.
      int every = r >= 110 ? 1 : r >= 70 ? 2 : 3;
      Font numbers = new Font(Ui.LABELS, Font.BOLD, Math.max(7, (r - face) * 42 / 100));
      int labelRadius = (r + face) / 2;
      for (int hour = 0; hour < 24; hour += every) {
         double angle = angle(hour);
         Rectangle spot = new Rectangle(cx + (int) Math.round(labelRadius * Math.sin(angle)) - r / 4,
               cy - (int) Math.round(labelRadius * Math.cos(angle)) - (r - face) / 2, r / 2, r - face);
         g.setColor(color(hour) == NIGHT ? Ui.FACE : Ui.INK);
         Ui.center(g, Zone.hour(hour, this.twentyFour()), numbers, spot);
      }

      // A hand per zone, here's the thickest; each named near its tip where the name has room.
      List<Rectangle> taken = new ArrayList<>();
      Font names = new Font(Ui.LABELS, Font.BOLD, Math.max(7, r / 9));
      FontMetrics metrics = g.getFontMetrics(names);
      for (int i = zones.size() - 1; i >= 0; i--) {
         double angle = angle(zones.get(i).hourOfDay(t));
         g.setColor(COLORS[i % COLORS.length]);
         g.setStroke(new BasicStroke(i == 0 ? Math.max(2, r / 22f) : Math.max(1.5f, r / 45f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
         int tipX = cx + (int) Math.round((r - 2) * Math.sin(angle));
         int tipY = cy - (int) Math.round((r - 2) * Math.cos(angle));
         g.drawLine(cx, cy, tipX, tipY);
         int dot = Math.max(3, r / 16);
         g.fillOval(tipX - dot, tipY - dot, 2 * dot, 2 * dot);
      }
      for (int i = 0; i < zones.size(); i++) {
         double angle = angle(zones.get(i).hourOfDay(t));
         String name = zones.get(i).name;
         int width = metrics.stringWidth(name) + 4;
         int height = metrics.getAscent() + 2;
         for (int step : new int[]{48, 30, 12}) {
            int radius = face * step / 64;
            Rectangle label = new Rectangle(cx + (int) Math.round(radius * Math.sin(angle)) - width / 2,
                  cy - (int) Math.round(radius * Math.cos(angle)) - height / 2, width, height);
            if (!overlaps(label, taken)) {
               taken.add(label);
               g.setColor(Ui.FACE);
               g.fillRoundRect(label.x, label.y, label.width, label.height, 4, 4);
               g.setColor(COLORS[i % COLORS.length]);
               Ui.center(g, name, names, label);
               break;
            }
         }
      }
      g.setColor(Ui.INK);
      int hub = Math.max(2, r / 20);
      g.fillOval(cx - hub, cy - hub, 2 * hub, 2 * hub);
   }

   /** Beside the dial: here's time and date, and how far from now it is. */
   private void paintInfo(Graphics2D g, Rectangle box, long t, long now) {
      int line = box.height / 7;
      g.setColor(Ui.FAINT);
      Ui.line(g, "Here", Ui.LABELS, new Rectangle(box.x, box.y + line / 2, box.width, line * 8 / 10));
      g.setColor(Ui.INK);
      Ui.line(g, Zone.HERE.time(t, this.twentyFour()), Ui.DIGITS, new Rectangle(box.x, box.y + line * 3 / 2, box.width, line * 2));
      Ui.line(g, date(Zone.HERE.day(t)), Ui.LABELS, new Rectangle(box.x, box.y + line * 7 / 2, box.width, line * 8 / 10));
      g.setColor(this.shown < 0 ? Ui.FAINT : Ui.HAND);
      Ui.line(g, this.shown < 0 ? "Now" : away(t - now), Ui.LABELS, new Rectangle(box.x, box.y + line * 5, box.width, line));
   }

   /** A row per zone: its name, its time, and here's day hour by hour in its time. */
   private void paintRows(Graphics2D g, Rectangle box, List<Zone> zones, long t, boolean[] together) {
      int rowHeight = box.height / zones.size();
      int nameWidth = box.width * 26 / 100;
      int timeWidth = box.width * 18 / 100;
      this.strips = new Rectangle(box.x + nameWidth + timeWidth, box.y, box.width - nameWidth - timeWidth, box.height);
      double cell = this.strips.width / 24.0;
      long dayStart = Zone.HERE.midnight(t);
      long hereDay = Zone.HERE.day(t);
      int hereOffset = Zone.HERE.offset(t);
      Font hours = Ui.fit(g, Ui.LABELS, this.twentyFour() ? "22" : "12p", (int) cell - 1, rowHeight * 55 / 100, 14);
      boolean numbered = cell >= 11;

      for (int i = 0; i < zones.size(); i++) {
         Zone zone = zones.get(i);
         int y = box.y + i * rowHeight;
         // The name, with a dot in its hand's color, and how far ahead or behind here it is when there is room.
         int dot = Math.max(3, rowHeight / 6);
         g.setColor(COLORS[i % COLORS.length]);
         g.fillOval(box.x + 2, y + rowHeight / 2 - dot, 2 * dot, 2 * dot);
         Rectangle name = new Rectangle(box.x + 4 + 2 * dot, y, nameWidth - 6 - 2 * dot, rowHeight);
         long dayDiff = zone.day(t) - hereDay;
         String detail = i == 0 ? "" : Zone.difference(zone.offset(t) - hereOffset)
               + (dayDiff > 0 ? ", tomorrow" : dayDiff < 0 ? ", yesterday" : "");
         g.setColor(Ui.INK);
         if (rowHeight >= 26 && !detail.isEmpty()) {
            left(g, zone.name, Ui.fit(g, Ui.LABELS, zone.name, name.width, rowHeight * 45 / 100, 16),
                  new Rectangle(name.x, y + 1, name.width, rowHeight * 55 / 100));
            g.setColor(Ui.FAINT);
            left(g, detail, Ui.fit(g, Ui.LABELS, detail, name.width, rowHeight * 34 / 100, 12),
                  new Rectangle(name.x, y + rowHeight * 55 / 100, name.width, rowHeight * 40 / 100));
         } else {
            left(g, zone.name, Ui.fit(g, Ui.LABELS, zone.name, name.width, rowHeight * 55 / 100, 16), name);
         }
         g.setColor(Ui.INK);
         String time = zone.time(t, this.twentyFour());
         Ui.center(g, time, Ui.fit(g, Ui.DIGITS, time, timeWidth - 4, rowHeight * 70 / 100, 22),
               new Rectangle(box.x + nameWidth, y, timeWidth, rowHeight));

         // The strip: one cell per hour of here's day, in this zone's hour.
         for (int h = 0; h < 24; h++) {
            int x0 = this.strips.x + (int) Math.round(h * cell);
            int x1 = this.strips.x + (int) Math.round((h + 1) * cell);
            int local = (int) Math.floor(zone.hourOfDay(dayStart + h * HOUR + 30 * MINUTE));
            Color color = color(local);
            g.setColor(color);
            g.fillRect(x0, y + 1, x1 - x0, rowHeight - 2);
            if (local == 0 && h > 0) {
               // A new day starts there.
               g.setColor(Ui.FACE);
               g.fillRect(x0, y + 1, Math.max(1, rowHeight / 14), rowHeight - 2);
            }
            if (numbered) {
               g.setColor(color == NIGHT ? Ui.FACE : Ui.INK);
               Ui.center(g, Zone.hour(local, this.twentyFour()), hours, new Rectangle(x0, y, x1 - x0, rowHeight));
            }
         }
      }

      // The hours that suit everyone, ringed in green across all the strips.
      g.setColor(Ui.FASTEST);
      g.setStroke(new BasicStroke(Math.max(2, rowHeight / 9f)));
      for (int h = 0; h < 24; h++) {
         if (together[h] && (h == 0 || !together[h - 1])) {
            int end = h;
            while (end < 24 && together[end]) {
               end++;
            }
            int x0 = this.strips.x + (int) Math.round(h * cell);
            int x1 = this.strips.x + (int) Math.round(end * cell);
            g.drawRect(x0, box.y, x1 - x0, box.height - 1);
         }
      }

      // The time shown, as a red line across the strips.
      int x = this.strips.x + (int) Math.round(Math.max(0, Math.min(1, (t - dayStart) / (double) Zone.DAY)) * this.strips.width);
      g.setColor(Ui.HAND);
      g.setStroke(new BasicStroke(Math.max(2, rowHeight / 10f)));
      g.drawLine(x, box.y - 3, x, box.y + box.height + 2);
      int tip = Math.max(4, rowHeight / 5);
      g.fillPolygon(new int[]{x - tip, x + tip, x}, new int[]{box.y - 3 - tip, box.y - 3 - tip, box.y - 3}, 3);
   }

   /** The line under the strips: when everyone is at work, or failing that awake. */
   private String plan(List<Zone> zones, long t, boolean[] together) {
      if (zones.size() < 2) {
         return "Pick cities to see their time (Cities or A)";
      }
      String work = runs(together);
      if (work != null) {
         return "Everyone at work: " + work + " here";
      }
      long dayStart = Zone.HERE.midnight(t);
      String awake = runs(together(zones, dayStart, AWAKE_FROM, AWAKE_TO));
      if (awake != null) {
         return "No working hour suits everyone. All awake: " + awake + " here";
      }
      int best = mostAwake(zones, dayStart);
      return "Never all awake. Best: " + Zone.clock(best, 0, this.twentyFour()) + " here, "
            + awakeAt(zones, dayStart, best) + " of " + zones.size() + " awake";
   }

   /** The hour of the day starting at {@code dayStart} with the most zones awake (the earliest of equals). */
   static int mostAwake(List<Zone> zones, long dayStart) {
      int best = 0;
      for (int h = 1; h < 24; h++) {
         if (awakeAt(zones, dayStart, h) > awakeAt(zones, dayStart, best)) {
            best = h;
         }
      }
      return best;
   }

   private static int awakeAt(List<Zone> zones, long dayStart, int h) {
      return between(zones, dayStart, h, AWAKE_FROM, AWAKE_TO);
   }

   /** How many zones are between {@code from} and {@code to} o'clock in the middle of hour {@code h} of the day. */
   private static int between(List<Zone> zones, long dayStart, int h, int from, int to) {
      int count = 0;
      for (Zone zone : zones) {
         double local = zone.hourOfDay(dayStart + h * HOUR + 30 * MINUTE);
         count += local >= from && local < to ? 1 : 0;
      }
      return count;
   }

   /** The first stretch of true hours as "9 am to 11 am", plus how many more there are. */
   private String runs(boolean[] hours) {
      String first = null;
      int more = 0;
      for (int h = 0; h < 24; h++) {
         if (hours[h] && (h == 0 || !hours[h - 1])) {
            int end = h;
            while (end < 24 && hours[end]) {
               end++;
            }
            if (first == null) {
               first = Zone.clock(h, 0, this.twentyFour()) + " to " + Zone.clock(end % 24, 0, this.twentyFour());
            } else {
               more++;
            }
         }
      }
      return first == null ? null : more == 0 ? first : first + " (and " + more + " more)";
   }

   /** The picker: every city as a button, chosen ones filled in. */
   private void paintPicker(Graphics2D g, Rectangle area, long now) {
      int gap = Math.max(3, area.height / 80);
      int buttonHeight = clamp(area.height / 9, 24, 44);
      int headerHeight = clamp(area.height / 14, 14, 22);
      Rectangle header = new Rectangle(area.x, area.y, area.width, headerHeight);
      g.setColor(this.message != null ? Ui.HAND : Ui.INK);
      Ui.line(g, this.message != null ? this.message
            : "Pick up to " + MAX_CITIES + " cities: " + this.cities().size() + " chosen", Ui.LABELS, header);

      int columns = area.width >= 440 ? 4 : 3;
      int rows = (Zone.CITIES.length + columns - 1) / columns;
      Rectangle grid = new Rectangle(area.x, header.y + headerHeight + gap, area.width,
            area.height - headerHeight - buttonHeight - 3 * gap);
      int cellWidth = (grid.width - gap * (columns - 1)) / columns;
      int cellHeight = Math.max(10, (grid.height - (rows - 1)) / rows);
      Font font = Ui.fit(g, Ui.LABELS, "Johannesburg 12:00 pm", cellWidth - 8, cellHeight * 70 / 100, 14);
      FontMetrics metrics = g.getFontMetrics(font);
      g.setFont(font);
      this.cityBoxes = new Rectangle[Zone.CITIES.length];
      for (int i = 0; i < Zone.CITIES.length; i++) {
         Zone zone = Zone.CITIES[i];
         Rectangle cell = new Rectangle(grid.x + i / rows * (cellWidth + gap), grid.y + i % rows * (cellHeight + 1),
               cellWidth, cellHeight);
         this.cityBoxes[i] = cell;
         boolean chosen = this.cities().contains(zone);
         g.setColor(chosen ? Ui.INK : Ui.FACE);
         g.fillRoundRect(cell.x, cell.y, cell.width, cell.height, 6, 6);
         int baseline = cell.y + (cell.height + metrics.getAscent() - metrics.getDescent()) / 2;
         g.setColor(chosen ? Ui.BACKGROUND : Ui.INK);
         g.drawString(zone.name, cell.x + 4, baseline);
         String time = zone.time(now, this.twentyFour());
         g.setColor(chosen ? Ui.FACE : Ui.FAINT);
         g.drawString(time, cell.x + cell.width - 4 - metrics.stringWidth(time), baseline);
      }
      this.done = new Rectangle(area.x + area.width / 3, area.y + area.height - buttonHeight, area.width / 3, buttonHeight);
      Ui.button(g, this.done, "Done", true);
   }

   // ---- Input ----

   @Override
   public void press(int x, int y, boolean rightButton, long now) {
      if (this.picking) {
         if (this.done != null && this.done.contains(x, y)) {
            this.closePicker();
            return;
         }
         int which = Ui.indexOf(this.cityBoxes, Ui.hit(this.cityBoxes, x, y));
         if (which >= 0) {
            this.toggle(Zone.CITIES[which]);
         }
         return;
      }
      int which = Ui.indexOf(this.buttons, Ui.hit(this.buttons, x, y));
      if (which >= 0) {
         this.button(which, now);
      } else if (this.strips != null && this.strips.contains(x, y)) {
         this.dragging = true;
         this.dragDay = Zone.HERE.midnight(this.shown(now));
         this.drag(x, y, now);
      }
   }

   private void button(int which, long now) {
      switch (which) {
         case 0:
            this.step(-1, now);
            break;
         case 1:
            this.shown = -1;
            break;
         case 2:
            this.step(1, now);
            break;
         default:
            this.picking = true;
            this.message = null;
      }
   }

   /** Moves the time shown to the next (or the last) whole hour here. */
   private void step(int direction, long now) {
      long t = this.shown(now);
      long intoHour = Math.floorMod(Zone.HERE.local(t), HOUR);
      this.shown = direction > 0 ? t - intoHour + HOUR : intoHour == 0 ? t - HOUR : t - intoHour;
   }

   /** Picks or drops a city; at most {@link #MAX_CITIES}. */
   void toggle(Zone zone) {
      if (this.cities().remove(zone)) {
         this.message = null;
      } else if (this.cities().size() < MAX_CITIES) {
         this.cities().add(zone);
         this.message = null;
      } else {
         this.message = "Up to " + MAX_CITIES + " cities: take one off first";
      }
   }

   private void closePicker() {
      this.picking = false;
      this.message = null;
   }

   @Override
   public void drag(int x, int y, long now) {
      if (this.dragging && this.strips != null) {
         double fraction = Math.max(0, Math.min(1, (x - this.strips.x) / (double) this.strips.width));
         // Snapped to quarter hours.
         this.shown = this.dragDay + Math.round(fraction * 96) * 15 * MINUTE;
      }
   }

   @Override
   public void release(int x, int y, long now) {
      this.dragging = false;
   }

   @Override
   public boolean key(int keyCode, long now) {
      if (this.picking) {
         if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_A
               || keyCode == KeyEvent.VK_SPACE) {
            this.closePicker();
            return true;
         }
         return false;
      }
      switch (keyCode) {
         case KeyEvent.VK_LEFT:
            this.step(-1, now);
            return true;
         case KeyEvent.VK_RIGHT:
            this.step(1, now);
            return true;
         case KeyEvent.VK_SPACE:
         case KeyEvent.VK_ENTER:
         case KeyEvent.VK_HOME:
         case KeyEvent.VK_ESCAPE:
            this.shown = -1;
            return true;
         case KeyEvent.VK_A:
            this.picking = true;
            this.message = null;
            return true;
         default:
            return false;
      }
   }

   // ---- Helpers ----

   /**
    * For each hour of the day starting at {@code dayStart}, whether every zone is between {@code from} and {@code to}
    * o'clock at that hour's middle.
    */
   static boolean[] together(List<Zone> zones, long dayStart, int from, int to) {
      boolean[] hours = new boolean[24];
      for (int h = 0; h < 24; h++) {
         hours[h] = zones.size() > 1 && between(zones, dayStart, h, from, to) == zones.size();
      }
      return hours;
   }

   /** Night, awake (morning and evening) or working hours. */
   static Color color(int hour) {
      return hour >= WORK_FROM && hour < WORK_TO ? WORK : hour >= AWAKE_FROM && hour < AWAKE_TO ? AWAKE : NIGHT;
   }

   /** Turns clockwise from 12 o'clock, as radians, with noon at the top. */
   private static double angle(double hour) {
      return 2 * Math.PI * (hour - 12) / 24;
   }

   /** "Tuesday, November 3". */
   static String date(long day) {
      int[] civil = Zone.civil(day);
      return DAYS[Zone.weekday(day)] + ", " + MONTHS[civil[1] - 1] + " " + civil[2];
   }

   /** How far the time shown is from now: "In 3 h 15 min", "2 h ago". */
   static String away(long millis) {
      long minutes = Math.round(Math.abs(millis) / (double) MINUTE);
      if (minutes == 0) {
         return "Now";
      }
      String amount = minutes >= 60 * 24 ? minutes / (60 * 24) + " d " + minutes % (60 * 24) / 60 + " h"
            : minutes >= 60 ? minutes / 60 + " h" + (minutes % 60 == 0 ? "" : " " + minutes % 60 + " min")
            : minutes + " min";
      return millis > 0 ? "In " + amount : amount + " ago";
   }

   private static void left(Graphics2D g, String text, Font font, Rectangle box) {
      g.setFont(font);
      FontMetrics metrics = g.getFontMetrics(font);
      g.drawString(text, box.x, box.y + (box.height + metrics.getAscent() - metrics.getDescent()) / 2);
   }

   private static boolean overlaps(Rectangle box, List<Rectangle> others) {
      for (Rectangle other : others) {
         // Rectangle.intersects, which the browser's AWT does not have.
         if (box.x < other.x + other.width && other.x < box.x + box.width
               && box.y < other.y + other.height && other.y < box.y + box.height) {
            return true;
         }
      }
      return false;
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}
