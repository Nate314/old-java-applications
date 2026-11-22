package groupcreator;

import java.util.Arrays;
import java.util.List;

/** Checks the badges' initials and that names next to each other in the class get hues far apart. */
public class BadgeCheck {
   public static void main(String[] args) {
      assert Badge.initials("Alice").equals("A");
      assert Badge.initials("  bob ").equals("B");
      assert Badge.initials("Mary Jane").equals("MJ");
      assert Badge.initials("mary-jane").equals("MJ");
      assert Badge.initials("Jean Paul Sartre").equals("JP");
      assert Badge.initials("").equals("");
      assert Badge.initials("   ").equals("");

      List<String> people = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");
      for (int i = 0; i + 1 < people.size(); i++) {
         float gap = Math.abs(Badge.hue(people.get(i), people) - Badge.hue(people.get(i + 1), people));
         gap = Math.min(gap, 1 - gap);
         assert gap > 0.3f : "neighbours " + i + " look alike: " + gap;
      }
      // The same name keeps its hue; one not in the class still gets one.
      assert Badge.hue("C", people) == Badge.hue("C", people);
      float stray = Badge.hue("Zed", people);
      assert stray >= 0 && stray < 1;
      System.out.println("BadgeCheck passed");
   }
}
