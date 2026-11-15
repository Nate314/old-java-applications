package animate2;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/** Self-check for Mode 7, Sky writing: wrapping, typing, stars finding their letters and coming back after a fling. Run with java -ea. */
public class SkyWritingCheck {
   private static final int W = 800;
   private static final int H = 600;

   public static void main(String[] args) throws Exception {
      // Cleaning and wrapping.
      check(SkyWriting.clean("hi  there~!").equals("HI THERE!"), "clean upper-cases, drops what the font lacks and doubled spaces");
      check(SkyWriting.clean(" x").equals("X"), "no leading space");
      List<String> one = SkyWriting.wrap("HELLO", W, H);
      check(one.equals(Arrays.asList("HELLO")), "a short word stays on one line: " + one);
      List<String> two = SkyWriting.wrap("HAPPY BIRTHDAY SAM", W, H);
      check(two.size() >= 2, "a long message wraps: " + two);
      check(SkyWriting.dotSize(two, W, H) > SkyWriting.dotSize(Arrays.asList("HAPPY BIRTHDAY SAM"), W, H), "wrapping makes the letters bigger");
      check(SkyWriting.wrap("HI ", W, H).get(0).equals("HI "), "a trailing space is kept for the caret");

      // The stars settle into HELLO: every star has a place in a lit dot, and gets there.
      StarField field = field();
      SkyWriting sky = field.skyWriting();
      check(field.mode() == StarField.SKY_WRITING && field.trails(), "Sky writing opens with the trails on");
      check(field.hintVisible(), "Sky writing shows its help");
      check(sky.text().equals(SkyWriting.FIRST_MESSAGE), "the first message");
      run(field, 4.0);
      check(sky.settled(0.8), "the stars settle into their letters");
      for (int star = 0; star < field.starCount; star++) {
         check(sky.hasPlace(star), "star " + star + " has a place");
      }
      // The places lie on the letters: the middle of HELLO's H has no dot, the H's crossbar does.
      double[] box = box(field);
      double dot = (box[1] - box[0]) / 28.0; // HELLO is 29 dots across, 28 between the first and last dots' middles
      double middle = (box[2] + box[3]) / 2.0;
      check(box[1] - box[0] > W * 0.6, "HELLO spans most of the width (" + (int)(box[1] - box[0]) + " px)");
      check(near(field, box[0] + 2 * dot, middle, 0.45 * dot) > 0, "the H's crossbar has stars");
      check(near(field, box[0] + 2 * dot, middle - 2 * dot, 0.45 * dot) == 0, "the H is open above its crossbar");

      // A fling scatters them; they come back.
      field.startPull(W / 2.0, H / 2.0);
      run(field, 1.0);
      field.release(W / 2.0, H / 2.0);
      field.pointer(W / 2.0, H / 2.0, false); // the pointer leaves, so its wake does not hold stars off the middle
      run(field, 0.3);
      check(!sky.settled(0.8), "a fling scatters the letters");
      run(field, 6.0);
      check(sky.settled(0.8), "the stars find their letters again");

      // Typing: only while writing (the window routes keys), letters go on, Backspace takes off, the cap holds.
      sky.startWriting();
      check(sky.writing(), "Enter starts writing");
      sky.type('!');
      sky.type('\n');
      sky.type('~');
      check(sky.text().equals("HELLO!"), "a letter goes on, a control or unknown key does not: " + sky.text());
      sky.type('\b');
      sky.type('\b');
      check(sky.text().equals("HELL"), "Backspace erases");
      for (int i = 0; i < 60; i++) {
         sky.type('A');
      }
      check(sky.text().length() == SkyWriting.MAX_LENGTH, "at most " + SkyWriting.MAX_LENGTH + " letters");
      for (int i = 0; i < 60; i++) {
         sky.type('\b');
      }
      check(sky.text().isEmpty(), "all erased");
      run(field, 0.1);
      for (int star = 0; star < field.starCount; star++) {
         check(!sky.hasPlace(star), "no message: no places");
      }
      run(field, 4.0);
      double[] spread = box(field);
      check(spread[1] - spread[0] > W * 0.8 && spread[3] - spread[2] > H * 0.8, "no message: the stars spread over the sky");
      for (char c : "WE MISSED YOU".toCharArray()) {
         sky.type(c);
      }
      sky.stopWriting();
      check(!sky.writing(), "Enter keeps the message");
      run(field, 5.0);
      check(sky.settled(0.8), "a new message settles");

      // More stars and fewer: every one gets a place.
      field.addStar();
      field.removeStar();
      field.removeStar();
      run(field, 0.1);
      for (int star = 0; star < field.starCount; star++) {
         check(sky.hasPlace(star), "after a change in stars, star " + star + " has a place");
      }

      // The message is kept between runs.
      File file = File.createTempFile("skywriting", ".txt");
      file.delete(); // no file yet: the first message
      file.deleteOnExit();
      SkyWriting saved = new SkyWriting(field, file);
      saved.startWriting();
      saved.type('\b');
      saved.type('S');
      saved.stopWriting();
      check(new SkyWriting(field, file).text().equals("HELLS"), "the message reads back");

      // It draws, caret and all.
      BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2 = image.createGraphics();
      sky.startWriting();
      sky.drawStars(g2);
      sky.drawCaret(g2);
      g2.dispose();

      System.out.println("SkyWritingCheck: all checks passed");
   }

   private static StarField field() {
      return new StarField(StarField.SKY_WRITING, new Dimension(W, H), 600, 20, false);
   }

   private static void run(StarField field, double seconds) {
      for (double t = 0; t < seconds; t += 0.015) {
         field.advance(0.015, W, H);
      }
   }

   /** Left, right, top and bottom of the stars' places (or of the stars, when they have none). */
   private static double[] box(StarField field) {
      double[] box = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
      SkyWriting sky = field.skyWriting();
      for (int star = 0; star < field.starCount; star++) {
         double x = sky.hasPlace(star) ? sky.placeX(star) : field.centerX(star);
         double y = sky.hasPlace(star) ? sky.placeY(star) : field.centerY(star);
         box[0] = Math.min(box[0], x);
         box[1] = Math.max(box[1], x);
         box[2] = Math.min(box[2], y);
         box[3] = Math.max(box[3], y);
      }
      return box;
   }

   /** How many places are within {@code reach} px of (x, y). */
   private static int near(StarField field, double x, double y, double reach) {
      int count = 0;
      for (int star = 0; star < field.starCount; star++) {
         if (Math.hypot(field.skyWriting().placeX(star) - x, field.skyWriting().placeY(star) - y) < reach) {
            count++;
         }
      }
      return count;
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
