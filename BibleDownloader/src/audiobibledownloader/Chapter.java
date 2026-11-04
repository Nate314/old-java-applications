package audiobibledownloader;

import java.util.ArrayList;
import java.util.List;

/** One chapter's verses in one translation, as bible-api.com returns them. */
public final class Chapter {
   public final int book;
   public final int number;
   public final String translation;
   /** Verse numbers, in order. */
   public final int[] verseNumbers;
   /** Verse text, one entry per verse number, with line breaks and runs of spaces folded to one space. */
   public final String[] verses;

   Chapter(int book, int number, String translation, int[] verseNumbers, String[] verses) {
      this.book = book;
      this.number = number;
      this.translation = translation;
      this.verseNumbers = verseNumbers;
      this.verses = verses;
   }

   public String title() {
      return Books.NAMES[this.book] + " " + this.number;
   }

   /**
    * Reads the verses out of a {@code /data/<translation>/<BOOK>/<chapter>} answer. Only the "verse" and "text" fields
    * of each entry of the "verses" array are used, so a small scanner is enough and no JSON library is needed.
    */
   public static Chapter parse(int book, int number, String translation, String json) {
      int start = json.indexOf("\"verses\"");
      if (start < 0) {
         throw new IllegalArgumentException("no verses in the answer");
      }
      List<Integer> numbers = new ArrayList<>();
      List<String> texts = new ArrayList<>();
      int at = start;
      while (true) {
         int verseKey = json.indexOf("\"verse\":", at);
         if (verseKey < 0) {
            break;
         }
         int i = skipSpace(json, verseKey + "\"verse\":".length());
         int end = i;
         while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
         }
         int verse = Integer.parseInt(json.substring(i, end));
         int textKey = json.indexOf("\"text\":", end);
         if (textKey < 0) {
            break;
         }
         int quote = json.indexOf('"', textKey + "\"text\":".length());
         StringBuilder text = new StringBuilder();
         at = readString(json, quote + 1, text);
         numbers.add(verse);
         texts.add(text.toString().replaceAll("\\s+", " ").trim());
      }
      if (numbers.isEmpty()) {
         throw new IllegalArgumentException("no verses in the answer");
      }
      int[] verseNumbers = new int[numbers.size()];
      for (int k = 0; k < verseNumbers.length; k++) {
         verseNumbers[k] = numbers.get(k);
      }
      return new Chapter(book, number, translation, verseNumbers, texts.toArray(new String[0]));
   }

   /** The index of the first character at or after {@code i} that is not JSON whitespace. */
   static int skipSpace(String json, int i) {
      while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
         i++;
      }
      return i;
   }

   /** Reads a JSON string body starting just after its opening quote; returns the index after the closing quote. */
   static int readString(String json, int i, StringBuilder out) {
      while (i < json.length()) {
         char c = json.charAt(i++);
         if (c == '"') {
            return i;
         }
         if (c != '\\') {
            out.append(c);
            continue;
         }
         char e = json.charAt(i++);
         switch (e) {
            case 'n':
            case 'r':
            case 't':
               out.append(' ');
               break;
            case 'b':
            case 'f':
               break;
            case 'u':
               out.append((char) Integer.parseInt(json.substring(i, i + 4), 16));
               i += 4;
               break;
            default:
               out.append(e);
         }
      }
      throw new IllegalArgumentException("unterminated string");
   }
}
