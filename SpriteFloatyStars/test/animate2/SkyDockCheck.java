package animate2;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/** Self-check for the buttons along the bottom of a stars' window (SkyDock). Run with java -ea. */
public class SkyDockCheck {
   public static void main(String[] args) {
      // On a phone, a laptop window and a big one, in every mode: the buttons fit inside the window, never overlap,
      // are tall enough for a finger, and a press in the middle of each finds its key.
      for (int width : new int[]{320, 360, 400, 640, 800}) {
         for (int mode = 0; mode < StarField.MODE_NAMES.length; mode++) {
            StarField field = field(mode, width, 700);
            List<SkyDock.Chip> chips = field.dockChips();
            check(chips.size() == (mode == StarField.SKY_WRITING ? 7 : 6), width + " mode " + mode + ": all the buttons");
            for (int i = 0; i < chips.size(); i++) {
               Rectangle box = chips.get(i).box;
               check(box.x >= 0 && box.x + box.width <= width && box.y >= 0 && box.y + box.height <= 700, width + " mode " + mode + ": " + chips.get(i).label + " inside the window " + box);
               check(box.height >= 40, width + ": " + chips.get(i).label + " is at least 40 px tall");
               check(field.dockKeyAt((int)box.getCenterX(), (int)box.getCenterY()) == chips.get(i).key, width + ": a press finds " + chips.get(i).label);
               for (int j = i + 1; j < chips.size(); j++) {
                  check(!box.intersects(chips.get(j).box), width + ": " + chips.get(i).label + " and " + chips.get(j).label + " apart");
               }
            }
            check(SkyDock.top(chips, 700) > 700 - 160, width + " mode " + mode + ": at most two rows (" + SkyDock.top(chips, 700) + ")");
         }
      }

      // Pressing a button does what its key does, and the stars are not pulled or burst by that press.
      StarField field = field(0, 400, 700);
      StarHand hand = new StarHand(field);
      int[] titles = {0};
      field.onChange(() -> titles[0]++);
      press(hand, field, find(field, KeyEvent.VK_T));
      check(field.trails() && !field.pulling(), "Trails turns the trails on, without a pull");
      check(titles[0] == 1, "the window hears about the change");
      paint(field);
      check(chip(field, KeyEvent.VK_T).lit, "Trails shows lit while on");
      press(hand, field, find(field, KeyEvent.VK_RIGHT));
      check(field.mode() == 1, "the arrow after the name goes on to the next sky");
      paint(field);
      press(hand, field, find(field, KeyEvent.VK_LEFT));
      press(hand, field, find(field, KeyEvent.VK_LEFT));
      check(field.mode() == StarField.SKY_WRITING, "the arrow before it goes back, round to Sky writing");
      paint(field);
      check(chip(field, KeyEvent.VK_ENTER) != null && chip(field, KeyEvent.VK_ENTER).label.equals("Write"), "Sky writing has Write");
      press(hand, field, find(field, KeyEvent.VK_ENTER));
      check(field.skyWriting().writing(), "Write starts a message");
      run(field, 12.0);
      check(field.dockAlpha() == 255, "the buttons stay while writing");
      check(chip(field, KeyEvent.VK_ENTER).label.equals("Done"), "Write reads Done while writing");
      press(hand, field, find(field, KeyEvent.VK_ENTER));
      check(!field.skyWriting().writing(), "Done keeps the message");
      press(hand, field, find(field, KeyEvent.VK_S));
      check(field.music().on(), "Music turns star music on");
      press(hand, field, find(field, KeyEvent.VK_S));
      check(!field.music().on(), "and off");

      // Left alone, the buttons fade to a ghost that still answers a press; moving the pointer brings them back.
      field = field(0, 400, 700);
      hand = new StarHand(field);
      Rectangle burst = find(field, KeyEvent.VK_SPACE);
      check(field.dockAlpha() == 255, "the buttons show when the window opens");
      run(field, 10.0);
      check(field.dockAlpha() == StarField.DOCK_GHOST, "the buttons fade to a ghost by themselves");
      field.pointer(200, 100, true);
      check(field.dockAlpha() == 255, "the pointer brings them back");
      run(field, 10.0);
      hand.mousePressed(event(field, MouseEvent.MOUSE_PRESSED, burst));
      check(!field.pulling(), "a press on a faded button is still the button's");
      hand.mouseReleased(event(field, MouseEvent.MOUSE_RELEASED, burst));
      Rectangle sky = new Rectangle(190, 90, 20, 20);
      hand.mousePressed(event(field, MouseEvent.MOUSE_PRESSED, sky));
      check(field.pulling(), "a press on the sky above them pulls the stars");
      hand.mouseReleased(event(field, MouseEvent.MOUSE_RELEASED, sky));

      // The settings window's tiles and Star Catcher's sky have no buttons.
      StarField tile = new StarField(0, new Dimension(400, 300), 50, 4, false);
      tile.setSize(400, 300);
      paint(tile);
      check(tile.dockChips().isEmpty() && tile.dockAlpha() == 0, "no buttons on a tile");

      System.out.println("SkyDockCheck: all checks passed");
   }

   private static StarField field(int mode, int width, int height) {
      StarField field = new StarField(mode, new Dimension(width, height), 200, 12, false);
      field.setSize(width, height);
      // Silent: the notes go nowhere instead of to the synthesizer.
      field.music().listen(new StarMusic.Voice() {
         @Override
         public void on(int note, int velocity) {
         }

         @Override
         public void off(int note) {
         }
      });
      field.dock();
      paint(field);
      return field;
   }

   private static void paint(StarField field) {
      BufferedImage image = new BufferedImage(field.getWidth(), field.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      field.paint(g);
      g.dispose();
   }

   private static void run(StarField field, double seconds) {
      for (double t = 0; t < seconds; t += 0.015) {
         field.advance(0.015, field.getWidth(), field.getHeight());
      }
      paint(field);
   }

   private static SkyDock.Chip chip(StarField field, int key) {
      for (SkyDock.Chip chip : field.dockChips()) {
         if (chip.key == key) {
            return chip;
         }
      }
      return null;
   }

   private static Rectangle find(StarField field, int key) {
      SkyDock.Chip chip = chip(field, key);
      check(chip != null, "a button for key " + key);
      return chip.box;
   }

   private static void press(StarHand hand, StarField field, Rectangle box) {
      hand.mousePressed(event(field, MouseEvent.MOUSE_PRESSED, box));
      hand.mouseReleased(event(field, MouseEvent.MOUSE_RELEASED, box));
   }

   private static MouseEvent event(StarField field, int id, Rectangle box) {
      return new MouseEvent(field, id, 0L, 0, (int)box.getCenterX(), (int)box.getCenterY(), 1, false, MouseEvent.BUTTON1);
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
