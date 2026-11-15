package circlewindow;

/**
 * A shape as a short code for a link (?shape=): its shape letter, its colors letter, then the word or the drawing.
 * Every character is one a link keeps as it is: a word's space, ? and : become _, ~ and *; each 20 x 20 frame of a
 * drawing is 67 characters of 6 lit-or-not cells each (A-Z a-z 0-9 - _), the frames joined by '.'.
 */
final class Link {
   private static final String SHAPES = "CRHSWKD";   // Circle, Ring, Heart, Star, Word, clocK, Draw
   private static final String COLORS = "RBS";       // Rainbow, Blue, Sunset
   private static final String WORD_FROM = " ?:";
   private static final String WORD_TO = "_~*";
   private static final String DIGITS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
   private static final int CELLS = DrawPad.CELLS * DrawPad.CELLS;

   final String shape;
   final String colors;
   /** The word for a Word, the drawing (as DrawPad.text()) for a Draw, otherwise null. */
   final String word;
   final String drawing;

   private Link(String shape, String colors, String word, String drawing) {
      this.shape = shape;
      this.colors = colors;
      this.word = word;
      this.drawing = drawing;
   }

   /** The code of a shape; word and drawing (DrawPad.text()) are only used by a Word and a Draw. */
   static String code(String shape, String colors, String word, String drawing) {
      StringBuilder out = new StringBuilder();
      out.append(SHAPES.charAt(Math.max(0, indexOf(Shapes.ALL, shape))));
      out.append(COLORS.charAt(Math.max(0, indexOf(CircleCreatorWindow.COLOR_NAMES, colors))));
      if (Shapes.WORD.equals(shape)) {
         for (char c : Shapes.cleanWord(word).toCharArray()) {
            int at = WORD_FROM.indexOf(c);
            out.append(at < 0 ? c : WORD_TO.charAt(at));
         }
      } else if (Shapes.DRAW.equals(shape)) {
         String[] frames = drawing.split("\\|");
         for (int f = 0; f < frames.length; f++) {
            if (f > 0) {
               out.append('.');
            }
            String cells = frames[f].replace("/", "");
            for (int i = 0; i < CELLS; i += 6) {
               int digit = 0;
               for (int bit = 0; bit < 6; bit++) {
                  digit = digit * 2 + (i + bit < cells.length() && cells.charAt(i + bit) == '#' ? 1 : 0);
               }
               out.append(DIGITS.charAt(digit));
            }
         }
      }
      return out.toString();
   }

   /** The shape a code (or a whole link with ?shape=) stands for, or null when it is not one. */
   static Link parse(String text) {
      int at = text.indexOf("shape=");
      String code = at < 0 ? text.trim() : text.substring(at + 6);
      for (int i = 0; i < code.length(); i++) {
         if ("&#".indexOf(code.charAt(i)) >= 0) {
            code = code.substring(0, i);
            break;
         }
      }
      if (code.length() < 2 || SHAPES.indexOf(code.charAt(0)) < 0 || COLORS.indexOf(code.charAt(1)) < 0) {
         return null;
      }
      String shape = Shapes.ALL[SHAPES.indexOf(code.charAt(0))];
      String colors = CircleCreatorWindow.COLOR_NAMES[COLORS.indexOf(code.charAt(1))];
      String rest = code.substring(2);
      if (Shapes.WORD.equals(shape)) {
         StringBuilder word = new StringBuilder();
         for (char c : rest.toCharArray()) {
            int to = WORD_TO.indexOf(c);
            word.append(to < 0 ? c : WORD_FROM.charAt(to));
         }
         String clean = Shapes.cleanWord(word.toString());
         return clean.isEmpty() ? null : new Link(shape, colors, clean, null);
      }
      if (Shapes.DRAW.equals(shape)) {
         StringBuilder drawing = new StringBuilder();
         String[] frames = rest.split("\\.");
         for (int f = 0; f < frames.length && f < DrawPad.MAX_FRAMES; f++) {
            if (f > 0) {
               drawing.append('|');
            }
            for (int i = 0; i < CELLS; i++) {
               if (i > 0 && i % DrawPad.CELLS == 0) {
                  drawing.append('/');
               }
               int digit = i / 6 < frames[f].length() ? DIGITS.indexOf(frames[f].charAt(i / 6)) : -1;
               if (digit < 0 && i / 6 < frames[f].length()) {
                  return null;
               }
               drawing.append(digit > 0 && (digit >> (5 - i % 6) & 1) == 1 ? '#' : '.');
            }
         }
         return new Link(shape, colors, null, drawing.toString());
      }
      return new Link(shape, colors, null, null);
   }

   private static int indexOf(String[] names, String name) {
      for (int i = 0; i < names.length; i++) {
         if (names[i].equals(name)) {
            return i;
         }
      }
      return -1;
   }
}
