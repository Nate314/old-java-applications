package audiobibledownloader;

import java.util.BitSet;
import java.util.Date;

/**
 * A reading plan: a run of chapters in Bible order spread evenly over a number of days, so each day has a short
 * reading. Day {@code d} (from 0) holds the plan's chapters {@code dayStart(d)} up to {@code dayStart(d + 1)}.
 * Which chapters have been read for the plan is a {@link BitSet} over the plan's own chapters, kept by {@link Shelf}.
 */
public final class Plan {
   public static final Plan[] ALL = {
      new Plan("GOSPELS", "The Gospels in 30 days", "Matthew, Mark, Luke and John: the life of Jesus told four times over.", "MAT", "JHN", 30),
      new Plan("PROVERBS", "Proverbs in a month", "One chapter of Proverbs a day, one for each day of the month.", "PRO", "PRO", 31),
      new Plan("PSALMS", "Psalms in 50 days", "All 150 psalms, three a day: songs, laments and prayers.", "PSA", "PSA", 50),
      new Plan("NEW", "The New Testament in 90 days", "Matthew to Revelation: the Gospels, Acts, the letters and Revelation.", "MAT", "REV", 90),
      new Plan("YEAR", "The Bible in a year", "Genesis to Revelation, every one of the 1,189 chapters, in a year.", "GEN", "REV", 365)
   };
   private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

   public final String id;
   public final String name;
   public final String blurb;
   /** The first chapter's ordinal among all 1,189. */
   public final int first;
   public final int chapters;
   public final int days;

   private Plan(String id, String name, String blurb, String fromBook, String toBook, int days) {
      this.id = id;
      this.name = name;
      this.blurb = blurb;
      int to = Books.find(toBook);
      this.first = Books.ordinal(Books.find(fromBook), 1);
      this.chapters = Books.ordinal(to, Books.CHAPTERS[to]) - this.first + 1;
      this.days = days;
   }

   /** The plan with this id, or null. */
   public static Plan find(String id) {
      for (Plan plan : ALL) {
         if (plan.id.equalsIgnoreCase(id)) {
            return plan;
         }
      }
      return null;
   }

   public int index() {
      for (int i = 0; i < ALL.length; i++) {
         if (ALL[i] == this) {
            return i;
         }
      }
      return -1;
   }

   /** The plan's first chapter (0 is its first) on a day; {@code dayStart(days)} is the number of chapters. */
   public int dayStart(int day) {
      return (int) ((long) day * this.chapters / this.days);
   }

   /** The day a chapter of the plan is read on. */
   public int dayOf(int index) {
      return (int) (((long) index + 1) * this.days + this.chapters - 1) / this.chapters - 1;
   }

   /** The plan's index for a chapter, or -1 when the chapter is not in the plan. */
   public int indexOf(int book, int chapter) {
      int i = Books.ordinal(book, chapter) - this.first;
      return i >= 0 && i < this.chapters ? i : -1;
   }

   public int bookAt(int index) {
      return Books.bookOf(this.first + index);
   }

   public int chapterAt(int index) {
      return Books.chapterOf(this.first + index);
   }

   /** "1 chapter a day", "3 chapters a day", or "2 or 3 chapters a day" when the days are not all the same. */
   public String pace() {
      int low = this.chapters / this.days;
      if (this.chapters % this.days == 0) {
         return low + (low == 1 ? " chapter a day" : " chapters a day");
      }
      return low + " or " + (low + 1) + " chapters a day";
   }

   /** A day's reading, chapters of one book joined: "Matthew 28, Mark 1-2", "Psalms 4-6", "Proverbs 9". */
   public String reading(int day) {
      StringBuilder text = new StringBuilder();
      int from = this.dayStart(day);
      int to = this.dayStart(day + 1);
      int i = from;
      while (i < to) {
         int book = this.bookAt(i);
         int j = i;
         while (j + 1 < to && this.bookAt(j + 1) == book) {
            j++;
         }
         if (text.length() > 0) {
            text.append(", ");
         }
         text.append(Books.NAMES[book]).append(' ').append(this.chapterAt(i));
         if (j > i) {
            text.append('-').append(this.chapterAt(j));
         }
         i = j + 1;
      }
      return text.toString();
   }

   /** How many of a day's chapters are read. */
   public int readOn(BitSet done, int day) {
      int count = 0;
      for (int i = this.dayStart(day); i < this.dayStart(day + 1); i++) {
         count += done.get(i) ? 1 : 0;
      }
      return count;
   }

   public boolean dayDone(BitSet done, int day) {
      return this.readOn(done, day) == this.dayStart(day + 1) - this.dayStart(day);
   }

   /** Days before {@code today} that still have a chapter to read. */
   public int behind(BitSet done, int today) {
      int count = 0;
      for (int d = 0; d < Math.min(today, this.days); d++) {
         count += this.dayDone(done, d) ? 0 : 1;
      }
      return count;
   }

   /** Whole days read beyond today, counted from tomorrow while they run on unbroken. */
   public int ahead(BitSet done, int today) {
      int d = Math.max(0, today + 1);
      while (d < this.days && this.dayDone(done, d)) {
         d++;
      }
      return d - Math.max(0, today + 1);
   }

   public int daysDone(BitSet done) {
      int count = 0;
      for (int d = 0; d < this.days; d++) {
         count += this.dayDone(done, d) ? 1 : 0;
      }
      return count;
   }

   /** The plan's first unread chapter, or -1 when every chapter is read. */
   public int firstUnread(BitSet done) {
      int i = done.nextClearBit(0);
      return i < this.chapters ? i : -1;
   }

   // ---- Days on the calendar, counted as days since 1970-01-01 in local time ----

   /** Today, in local time. Date's getters, not Calendar's, because Calendar uses UTC in the browser (TeaVM). */
   @SuppressWarnings("deprecation")
   public static int today() {
      Date now = new Date();
      return epochDay(now.getYear() + 1900, now.getMonth() + 1, now.getDate());
   }

   /** Days since 1970-01-01 for a date (the proleptic Gregorian calendar, as java.time counts them). */
   public static int epochDay(int year, int month, int day) {
      int y = month <= 2 ? year - 1 : year;
      int era = (y >= 0 ? y : y - 399) / 400;
      int yearOfEra = y - era * 400;
      int dayOfYear = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
      int dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
      return era * 146097 + dayOfEra - 719468;
   }

   /** {year, month, day} for a day since 1970-01-01. */
   public static int[] date(int epochDay) {
      int z = epochDay + 719468;
      int era = (z >= 0 ? z : z - 146096) / 146097;
      int dayOfEra = z - era * 146097;
      int yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365;
      int dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100);
      int mp = (5 * dayOfYear + 2) / 153;
      int day = dayOfYear - (153 * mp + 2) / 5 + 1;
      int month = mp < 10 ? mp + 3 : mp - 9;
      return new int[]{yearOfEra + era * 400 + (month <= 2 ? 1 : 0), month, day};
   }

   /** "2026-11-02". */
   public static String iso(int epochDay) {
      int[] d = date(epochDay);
      return d[0] + "-" + (d[1] < 10 ? "0" : "") + d[1] + "-" + (d[2] < 10 ? "0" : "") + d[2];
   }

   /** "2026-11-02" back to its day, or throws for anything else. */
   public static int parseIso(String text) {
      String[] p = text.trim().split("-");
      if (p.length != 3) {
         throw new IllegalArgumentException("not a date: " + text);
      }
      int month = Integer.parseInt(p[1]);
      int day = Integer.parseInt(p[2]);
      if (month < 1 || month > 12 || day < 1 || day > 31) {
         throw new IllegalArgumentException("not a date: " + text);
      }
      return epochDay(Integer.parseInt(p[0]), month, day);
   }

   /** "Nov 2". */
   public static String shortDate(int epochDay) {
      int[] d = date(epochDay);
      return MONTHS[d[1] - 1] + " " + d[2];
   }
}
