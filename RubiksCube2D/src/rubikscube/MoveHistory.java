package rubikscube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every move applied to the cube since a start position, as a path of positions back to that start. A move that
 * returns the cube to a position already on the path (an inverse, or four turns of one slice) cuts the path back to
 * it, so the path never loops and {@link #hint()} always walks the shortest known way back.
 */
public final class MoveHistory {
   private final Cube cube;
   /** positions.get(i) is the cube after moves 0..i-1; positions.get(0) is the start. */
   private final List<String> positions = new ArrayList<>();
   private final List<String[]> moves = new ArrayList<>();
   /** Where each position sits on the path, so a long Mix stays quick to record. */
   private final Map<String, Integer> index = new HashMap<>();

   public MoveHistory(Cube cube) {
      this.cube = cube;
      this.restart();
   }

   /** Makes the cube's current position the start, with no moves. */
   public void restart() {
      this.positions.clear();
      this.moves.clear();
      this.index.clear();
      this.add(this.cube.snapshot());
   }

   private void add(String position) {
      this.index.put(position, this.positions.size());
      this.positions.add(position);
   }

   /** Applies a W/A/S/D move to square {@code cubie} of the face with that color and records it. */
   public void apply(String faceColor, String key, int cubie) {
      new Move(faceColor, key, cubie, this.cube);
      String position = this.cube.snapshot();
      Integer seen = this.index.get(position);
      if (seen != null) {
         while (this.positions.size() > seen + 1) {
            this.index.remove(this.positions.remove(this.positions.size() - 1));
            this.moves.remove(this.moves.size() - 1);
         }
      } else {
         this.add(position);
         this.moves.add(new String[]{faceColor, key, String.valueOf(cubie)});
      }
   }

   /** How many moves the cube is from the start along the recorded path. */
   public int depth() {
      return this.moves.size();
   }

   /** The move that takes the cube one step back toward the start, as {face color, key, cubie}, or null at the start. */
   public String[] hint() {
      if (this.moves.isEmpty()) {
         return null;
      }
      String[] last = this.moves.get(this.moves.size() - 1);
      return new String[]{last[0], inverse(last[1]), last[2]};
   }

   /** The key that undoes {@code key} on the same square: W and S, A and D. */
   public static String inverse(String key) {
      switch (key) {
         case "w": return "s";
         case "s": return "w";
         case "a": return "d";
         default: return "a";
      }
   }
}
