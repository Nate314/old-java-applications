package audiobibledownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * A typed passage ("John 3:16", "jn 3 16", "1 cor 13", "Ps 23", "Jude 5") read as a book, chapter and verse, for Go to.
 * Books are matched by id, a few common short forms, the start of the name, then the letters in order.
 */
public final class Passage {
   public final int book;
   public final int chapter;
   /** 0 when only the chapter was typed. */
   public final int verse;

   Passage(int book, int chapter, int verse) {
      this.book = book;
      this.chapter = chapter;
      this.verse = verse;
   }

   /** Short forms that are neither an id nor the start of the name. */
   private static final String[][] ALIASES = {
      {"jn", "JHN"}, {"jhn", "JHN"}, {"mk", "MRK"}, {"mt", "MAT"}, {"lk", "LUK"}, {"jg", "JDG"}, {"jdg", "JDG"},
      {"songofsongs", "SNG"}, {"song", "SNG"}, {"canticles", "SNG"}, {"qoheleth", "ECC"}, {"apocalypse", "REV"},
      {"revelations", "REV"}, {"psalm", "PSA"}, {"ps", "PSA"}, {"pss", "PSA"}, {"prv", "PRO"}, {"jas", "JAS"},
      {"phm", "PHM"}, {"phlm", "PHM"}, {"php", "PHP"}, {"phil", "PHP"}, {"jud", "JUD"}, {"jude", "JUD"}
   };

   /** Well known places, offered before anything is typed. */
   public static final String[] FAMOUS = {"Genesis 1", "Psalm 23", "Isaiah 53", "Matthew 5", "John 1", "John 3:16", "Romans 8", "1 Corinthians 13"};

   public String name() {
      return Books.NAMES[this.book] + " " + this.chapter + (this.verse > 0 ? ":" + this.verse : "");
   }

   /** The best reading of the text, or null. A bare number is a chapter of the open book. */
   public static Passage parse(String text, int openBook) {
      List<Passage> all = matches(text, openBook, 1);
      return all.isEmpty() ? null : all.get(0);
   }

   /** Up to {@code limit} readings, best first: each book the typed name could be, with the numbers typed. */
   public static List<Passage> matches(String text, int openBook, int limit) {
      List<Passage> found = new ArrayList<>();
      String s = text.toLowerCase().trim();
      int i = 0;
      StringBuilder name = new StringBuilder();
      // A leading 1, 2 or 3 (or i, ii, iii) before a name belongs to it: "1 cor", "ii kings".
      if (s.length() > 1 && s.charAt(0) >= '1' && s.charAt(0) <= '3' && startsName(s, 1)) {
         name.append(s.charAt(0));
         i = 1;
      } else {
         for (String roman : new String[]{"iii", "ii", "i"}) {
            if (s.startsWith(roman + " ") && startsName(s, roman.length())) {
               name.append(roman.length());
               i = roman.length();
               break;
            }
         }
      }
      while (i < s.length() && !Character.isDigit(s.charAt(i))) {
         char c = s.charAt(i++);
         if (Character.isLetter(c)) {
            name.append(c);
         }
      }
      int[] numbers = new int[2];
      int count = 0;
      while (i < s.length() && count < 2) {
         if (Character.isDigit(s.charAt(i))) {
            int n = 0;
            while (i < s.length() && Character.isDigit(s.charAt(i))) {
               n = Math.min(9999, n * 10 + s.charAt(i++) - '0');
            }
            numbers[count++] = n;
         } else {
            i++;
         }
      }
      List<Integer> books = new ArrayList<>();
      String typed = name.toString();
      if (typed.isEmpty()) {
         if (count == 0 || openBook < 0) {
            return found;
         }
         books.add(openBook);
      } else {
         books = books(typed);
      }
      for (int book : books) {
         if (found.size() >= limit) {
            break;
         }
         int chapter = count > 0 ? numbers[0] : 1;
         int verse = count > 1 ? numbers[1] : 0;
         if (Books.CHAPTERS[book] == 1 && count == 1) {
            // A one-chapter book's single number is a verse: "Jude 5" is Jude 1:5.
            verse = Math.max(1, chapter);
            chapter = 1;
         }
         found.add(new Passage(book, Math.max(1, Math.min(Books.CHAPTERS[book], chapter)), verse));
      }
      return found;
   }

   private static boolean startsName(String s, int from) {
      int j = from;
      while (j < s.length() && (s.charAt(j) == ' ' || s.charAt(j) == '.')) {
         j++;
      }
      return j < s.length() && Character.isLetter(s.charAt(j));
   }

   /** The books a typed name (letters, with any leading 1-3) could be, best first. */
   static List<Integer> books(String typed) {
      List<Integer> out = new ArrayList<>();
      add(out, Books.find(typed));
      for (String[] alias : ALIASES) {
         if (alias[0].equals(typed)) {
            add(out, Books.find(alias[1]));
         }
      }
      for (int b = 0; b < Books.NAMES.length; b++) {
         if (compact(Books.NAMES[b]).startsWith(typed)) {
            add(out, b);
         }
      }
      for (int b = 0; b < Books.NAMES.length; b++) {
         if (inOrder(typed, compact(Books.NAMES[b]))) {
            add(out, b);
         }
      }
      return out;
   }

   private static void add(List<Integer> out, int book) {
      if (book >= 0 && !out.contains(book)) {
         out.add(book);
      }
   }

   /** "1 Corinthians" as "1corinthians". */
   static String compact(String name) {
      StringBuilder b = new StringBuilder();
      for (char c : name.toLowerCase().toCharArray()) {
         if (Character.isLetterOrDigit(c)) {
            b.append(c);
         }
      }
      return b.toString();
   }

   /** The typed letters appear in the name in order, starting with its first: "mk" in "mark". */
   private static boolean inOrder(String typed, String name) {
      if (typed.isEmpty() || name.isEmpty() || typed.charAt(0) != name.charAt(0)) {
         return false;
      }
      int j = 0;
      for (int k = 0; k < name.length() && j < typed.length(); k++) {
         if (name.charAt(k) == typed.charAt(j)) {
            j++;
         }
      }
      return j == typed.length();
   }
}
