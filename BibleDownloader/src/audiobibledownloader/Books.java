package audiobibledownloader;

/** The 66 books, with bible-api.com's book ids and the chapter counts its translations use. */
public final class Books {
   public static final String[] IDS = {
      "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA", "1KI", "2KI", "1CH", "2CH", "EZR", "NEH",
      "EST", "JOB", "PSA", "PRO", "ECC", "SNG", "ISA", "JER", "LAM", "EZK", "DAN", "HOS", "JOL", "AMO", "OBA", "JON",
      "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL",
      "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH", "PHP", "COL", "1TH", "2TH", "1TI", "2TI",
      "TIT", "PHM", "HEB", "JAS", "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV"
   };
   public static final String[] NAMES = {
      "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
      "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah", "Esther", "Job", "Psalms", "Proverbs",
      "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel",
      "Amos", "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi",
      "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
      "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon",
      "Hebrews", "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation"
   };
   public static final int[] CHAPTERS = {
      50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3,
      9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
      28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22
   };
   /** Books before this index are the Old Testament. */
   public static final int NEW_TESTAMENT = 39;
   /** 1,189: every chapter of every book. */
   public static final int TOTAL;
   private static final int[] FIRST = new int[IDS.length];

   static {
      int total = 0;
      for (int i = 0; i < IDS.length; i++) {
         FIRST[i] = total;
         total += CHAPTERS[i];
      }
      TOTAL = total;
   }

   private Books() {
   }

   /** The chapter's place among all 1,189 (0 is Genesis 1). */
   public static int ordinal(int book, int chapter) {
      return FIRST[book] + chapter - 1;
   }

   public static int bookOf(int ordinal) {
      int book = 0;
      while (book + 1 < IDS.length && FIRST[book + 1] <= ordinal) {
         book++;
      }
      return book;
   }

   public static int chapterOf(int ordinal) {
      return ordinal - FIRST[bookOf(ordinal)] + 1;
   }

   /** The book with this id (any case), or -1. */
   public static int find(String id) {
      for (int i = 0; i < IDS.length; i++) {
         if (IDS[i].equalsIgnoreCase(id)) {
            return i;
         }
      }
      return -1;
   }
}
