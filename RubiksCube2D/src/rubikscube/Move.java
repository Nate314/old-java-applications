package rubikscube;

/**
 * Applies a W/A/S/D move to square {@code cubie} (1 to 9) of the face with that color: the quarter turn of the row,
 * column or ring through that square that carries it up, left, down or right in the net (see {@link CubeGeometry}).
 *
 * The recovered app shifted strips of stickers between faces by hand, and 144 of its 216 moves left some stickers
 * in a different spot on their new face than a real cube would (A on the top row put the front's three stickers
 * on the left face in reverse order), so a corner's three colors came apart. Each sticker still lands on the face
 * it used to, so the moves feel the same in the net.
 */
public final class Move {
   public Move(String faceColor, String key, int cubie, Cube cube) {
      CubeGeometry.Turn turn = CubeGeometry.turnOf(faceColor, key, cubie);
      if (turn != null) {
         CubeGeometry.apply(turn, cube.faces());
         cube.turned(turn);
      }
   }
}
