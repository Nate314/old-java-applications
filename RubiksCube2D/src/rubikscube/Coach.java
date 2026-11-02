package rubikscube;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Teach me: solves the cube from whatever position it is in the way people learn to, layer by layer (the beginner's
 * method), as steps a person can follow: the white cross, the white corners, the middle layer, a yellow cross, the
 * yellow edges, the yellow corners to their spots, then twisting them. Each step says what it is for and lists its
 * quarter turns of the outer layers ({@link CubeGeometry.Turn} ids).
 *
 * The faces are the colors of the centers wherever they are (a middle-layer move carries the centers along), white
 * at the bottom and yellow on top. The cross is the shortest way to bring each white edge home, found by a search
 * over where the cross edges can be; the rest are the method's usual move patterns, picked and repeated as the
 * method says, so they always come out the same way for the same cube.
 */
final class Coach {
   static final String[] STAGES = {"White cross", "White corners", "Middle layer", "Yellow cross", "Yellow edges",
      "Yellow corners", "Twist the corners"};
   private static final String SEXY = "R U R' U'";
   private static final String RIGHT_IN = "U R U' R' U' F' U F";
   private static final String LEFT_IN = "U' L' U L U F U' F'";
   private static final String CROSS = "F R U R' U' F'";
   private static final String SUNE = "R U R' U R U2 R'";
   private static final String CYCLE = "U R U' L' U R' U' L";
   private static final String TWIST = "R' D' R D";
   private static final int[][] NORMALS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
   /** The sticker at each cubie position and face normal: index pos * 6 + normal, or -1. */
   private static final int[] STICKER_AT = new int[27 * 6];
   private static final List<int[]> EDGES = new ArrayList<>();
   private static final List<int[]> CORNERS = new ArrayList<>();
   /** The 24 ways an edge can sit: slot 2e holds edge e's two stickers in order (a, b), slot 2e + 1 as (b, a). */
   private static final int[] SLOT_A = new int[24];
   private static final int[] SLOT_B = new int[24];
   private static final int[] SLOT_OF_FIRST = new int[54];
   /** The 12 quarter turns of the outer layers. */
   private static final int[] OUTER = new int[12];
   private static final int[][] SLOT_MOVE = new int[18][24];

   /**
    * One thing to do: its stage (index into {@link #STAGES}), what it is for, its quarter turns, and the yellow side
    * and the side to hold in front while making them (the normals they face).
    */
   static final class Step {
      final int stage;
      final String text;
      final List<Integer> turns = new ArrayList<>();
      final int[] up;
      final int[] front;

      Step(int stage, String text, int[] up, int[] front) {
         this.stage = stage;
         this.text = text;
         this.up = up;
         this.front = front;
      }
   }

   static {
      Arrays.fill(STICKER_AT, -1);
      for (int s = 0; s < 54; s++) {
         STICKER_AT[posIndex(CubeGeometry.POS[s]) * 6 + normalIndex(CubeGeometry.NORMAL[s])] = s;
      }
      Arrays.fill(SLOT_OF_FIRST, -1);
      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               int[] p = {x, y, z};
               int nonzero = Math.abs(x) + Math.abs(y) + Math.abs(z);
               if (nonzero == 3) {
                  CORNERS.add(p);
               } else if (nonzero == 2) {
                  int[] st = stickersOf(p);
                  int e = EDGES.size();
                  EDGES.add(p);
                  SLOT_A[2 * e] = st[0];
                  SLOT_B[2 * e] = st[1];
                  SLOT_A[2 * e + 1] = st[1];
                  SLOT_B[2 * e + 1] = st[0];
                  SLOT_OF_FIRST[st[0]] = 2 * e;
                  SLOT_OF_FIRST[st[1]] = 2 * e + 1;
               }
            }
         }
      }
      int count = 0;
      for (int id = 0; id < 18; id++) {
         if (id % 6 / 2 != 1) {
            OUTER[count++] = id;
         }
         int[] dest = CubeGeometry.destinations(id);
         for (int slot = 0; slot < 24; slot++) {
            SLOT_MOVE[id][slot] = SLOT_OF_FIRST[dest[SLOT_A[slot]]];
         }
      }
   }

   private final int[] state;
   private final int[] up;
   private final int[] down;
   private final int[][] sides = new int[4][];
   private final List<Step> steps = new ArrayList<>();
   private Step current = null;
   private int turnCount = 0;

   private Coach(int[] state) {
      this.state = state;
      int[] white = null;
      for (int[] n : NORMALS) {
         if (CubeGeometry.FACE_COLORS[this.center(n)].equals("white")) {
            white = n;
         }
      }
      this.down = white;
      this.up = neg(white);
      int k = 0;
      for (int[] n : NORMALS) {
         if (dot(n, this.up) == 0) {
            this.sides[k++] = n;
         }
      }
   }

   private Coach(Coach other) {
      this.state = other.state.clone();
      this.up = other.up;
      this.down = other.down;
      System.arraycopy(other.sides, 0, this.sides, 0, 4);
   }

   /**
    * The steps that solve a cube whose stickers ({@link CubeGeometry} order) have these colors; empty when it is
    * solved.
    */
   static List<Step> plan(String[] colors) {
      int[] state = new int[54];
      for (int s = 0; s < 54; s++) {
         state[s] = CubeGeometry.faceIndex(colors[s]);
      }
      Coach coach = new Coach(state);
      coach.cross();
      coach.corners();
      coach.middle();
      coach.yellowCross();
      coach.yellowEdges();
      coach.yellowCorners();
      coach.twist();
      if (!coach.allSolved()) {
         throw new IllegalStateException("The coach did not reach a solved cube");
      }
      return coach.steps;
   }

   /** The turns of every step, in order. */
   static List<Integer> turns(List<Step> steps) {
      List<Integer> all = new ArrayList<>();
      for (Step step : steps) {
         all.addAll(step.turns);
      }
      return all;
   }

   /** The id of the quarter turn of the outer layer facing {@code normal}, clockwise as seen facing it when cw. */
   static int faceTurn(int[] normal, boolean cw) {
      int axis = normal[0] != 0 ? 0 : normal[1] != 0 ? 1 : 2;
      int layer = normal[axis];
      int dir = cw ? -layer : layer;
      return axis * 6 + (layer + 1) * 2 + (dir > 0 ? 1 : 0);
   }

   /** The normal of the outer layer an outer turn moves. */
   static int[] faceOf(int id) {
      int[] n = new int[3];
      n[id / 6] = id % 6 / 2 - 1;
      return n;
   }

   /** True when outer turn {@code id} is clockwise as seen facing the layer it moves. */
   static boolean clockwise(int id) {
      int layer = id % 6 / 2 - 1;
      int dir = id % 2 == 1 ? 1 : -1;
      return dir == -layer;
   }

   // ---- the stages ----

   private void cross() {
      List<int[]> homes = new ArrayList<>();
      for (int[] side : this.sides) {
         homes.add(add(this.down, side));
      }
      List<int[]> placed = new ArrayList<>();
      while (!homes.isEmpty()) {
         int[] best = null;
         int bestIndex = 0;
         for (int i = 0; i < homes.size(); i++) {
            List<int[]> trial = new ArrayList<>(placed);
            trial.add(homes.get(i));
            int[] path = this.crossPath(trial);
            if (best == null || path.length < best.length) {
               best = path;
               bestIndex = i;
            }
         }
         int[] home = homes.remove(bestIndex);
         String side = this.colorAt(sub(home, this.down));
         this.begin(0, "Bring the white-" + side + " edge down beside the white center, its " + side
               + " side against the " + side + " center.", sub(home, this.down));
         for (int id : best) {
            this.turn(id);
         }
         this.end();
         placed.add(home);
      }
   }

   /** The shortest outer turns that bring every one of these bottom edges home, keeping the others there. */
   private int[] crossPath(List<int[]> homes) {
      int k = homes.size();
      int[] goal = new int[k];
      int[] start = new int[k];
      for (int i = 0; i < k; i++) {
         int slot = 2 * edgeIndex(homes.get(i));
         goal[i] = slot;
         int a = this.center(CubeGeometry.NORMAL[SLOT_A[slot]]);
         int b = this.center(CubeGeometry.NORMAL[SLOT_B[slot]]);
         for (int j = 0; j < 24; j++) {
            if (this.state[SLOT_A[j]] == a && this.state[SLOT_B[j]] == b) {
               start[i] = j;
            }
         }
      }
      int from = encode(start);
      int to = encode(goal);
      if (from == to) {
         return new int[0];
      }
      int size = 1;
      for (int i = 0; i < k; i++) {
         size *= 24;
      }
      int[] previous = new int[size];
      byte[] how = new byte[size];
      Arrays.fill(previous, -1);
      int[] queue = new int[size];
      int head = 0;
      int tail = 0;
      queue[tail++] = from;
      previous[from] = from;
      int[] digits = new int[k];
      while (head < tail) {
         int code = queue[head++];
         for (int i = 0, c = code; i < k; i++, c /= 24) {
            digits[i] = c % 24;
         }
         for (int m = 0; m < 12; m++) {
            int[] move = SLOT_MOVE[OUTER[m]];
            int next = 0;
            for (int i = k - 1; i >= 0; i--) {
               next = next * 24 + move[digits[i]];
            }
            if (previous[next] >= 0) {
               continue;
            }
            previous[next] = code;
            how[next] = (byte) m;
            if (next == to) {
               List<Integer> path = new ArrayList<>();
               for (int at = to; at != from; at = previous[at]) {
                  path.add(0, OUTER[how[at]]);
               }
               int[] out = new int[path.size()];
               for (int i = 0; i < out.length; i++) {
                  out[i] = path.get(i);
               }
               return out;
            }
            queue[tail++] = next;
         }
      }
      throw new IllegalStateException("No way to the cross");
   }

   private static int encode(int[] digits) {
      int code = 0;
      for (int i = digits.length - 1; i >= 0; i--) {
         code = code * 24 + digits[i];
      }
      return code;
   }

   private void corners() {
      List<int[]> homes = new ArrayList<>();
      for (int[] side : this.sides) {
         homes.add(add(add(this.down, side), cross(this.up, side)));
      }
      while (!homes.isEmpty()) {
         int bestIndex = this.cheapest(homes, (coach, home) -> coach.placeCorner(home));
         int[] home = homes.remove(bestIndex);
         if (this.solved(home)) {
            continue;
         }
         int[] front = this.frontOf(home);
         String a = this.colorAt(front);
         String b = this.colorAt(cross(this.up, front));
         boolean stuck = dot(this.locate(home), this.up) < 0;
         this.begin(1, "Put the white-" + a + "-" + b + " corner between the " + a + " and " + b + " centers. "
               + (stuck ? "It is stuck in the bottom layer, so lift it out first. " : "")
               + "Turn yellow to bring it right above its spot, then repeat " + b + ", yellow, " + b
               + " back, yellow back until it drops in, white side down.", front);
         this.placeCorner(home);
         this.end();
      }
   }

   private void placeCorner(int[] home) {
      if (this.solved(home)) {
         return;
      }
      int[] at = this.locate(home);
      if (dot(at, this.up) < 0) {
         this.alg(SEXY, this.frontOf(at));
      }
      int[] above = add(home, scale(this.up, 2));
      this.upUntil(coach -> same(coach.locate(home), above));
      int[] front = this.frontOf(home);
      for (int n = 0; !this.solved(home); n++) {
         if (n == 6) {
            throw new IllegalStateException("Corner did not drop in");
         }
         this.alg(SEXY, front);
      }
   }

   private void middle() {
      List<int[]> homes = new ArrayList<>();
      for (int[] side : this.sides) {
         homes.add(add(side, cross(this.up, side)));
      }
      while (!homes.isEmpty()) {
         int bestIndex = this.cheapest(homes, (coach, home) -> coach.placeEdge(home));
         int[] home = homes.remove(bestIndex);
         if (this.solved(home)) {
            continue;
         }
         int[] front = this.frontOf(home);
         String a = this.colorAt(front);
         String b = this.colorAt(cross(this.up, front));
         boolean stuck = dot(this.locate(home), this.up) == 0;
         this.begin(2, "Put the " + a + "-" + b + " edge in the middle layer, between the " + a + " and " + b
               + " centers. " + (stuck ? "It is in the middle layer the wrong way, so swap it up to the top first. " : "")
               + "Turn yellow until its side color sits over its own center, then swing it down with eight moves"
               + " that leave the white layer as it was.", front);
         this.placeEdge(home);
         this.end();
      }
   }

   private void placeEdge(int[] home) {
      if (this.solved(home)) {
         return;
      }
      int[] at = this.locate(home);
      if (dot(at, this.up) == 0) {
         this.alg(RIGHT_IN, this.frontOf(at));
      }
      this.upUntil(coach -> coach.edgeOverItsCenter(home));
      int[] side = sub(this.locate(home), this.up);
      int top = this.state[sticker(this.locate(home), this.up)];
      this.alg(top == this.center(cross(this.up, side)) ? RIGHT_IN : LEFT_IN, side);
      if (!this.solved(home)) {
         throw new IllegalStateException("Edge did not go in");
      }
   }

   private boolean edgeOverItsCenter(int[] home) {
      int[] at = this.locate(home);
      if (dot(at, this.up) != 1) {
         return false;
      }
      int[] side = sub(at, this.up);
      return this.state[sticker(at, side)] == this.center(side);
   }

   private void yellowCross() {
      for (int front : this.search(CROSS, 3, coach -> coach.crossUp())) {
         int[] f = this.sides[front];
         this.begin(3, "Turn the yellow edges up into a yellow cross. With " + this.colorAt(f) + " in front: "
               + this.colorAt(f) + ", " + this.colorAt(cross(this.up, f)) + ", yellow, then the same two back the"
               + " other way round. Each go turns more of them up.", f);
         this.alg(CROSS, f);
         this.end();
      }
   }

   private boolean crossUp() {
      for (int[] side : this.sides) {
         if (this.state[sticker(add(this.up, side), this.up)] != this.center(this.up)) {
            return false;
         }
      }
      return true;
   }

   private void yellowEdges() {
      for (int sunes = 0; sunes <= 4; sunes++) {
         int[] fronts = new int[sunes];
         for (int combo = 0; combo < 1 << 2 * sunes; combo++) {
            Coach trial = new Coach(this);
            for (int i = 0; i < sunes; i++) {
               fronts[i] = combo >> 2 * i & 3;
               trial.alg(SUNE, this.sides[fronts[i]]);
            }
            for (int k = 0; k < 4; k++) {
               if (trial.topEdgesHome()) {
                  for (int front : fronts) {
                     int[] f = this.sides[front];
                     String r = this.colorAt(cross(this.up, f));
                     this.begin(4, "Move the yellow edges around the top until each matches the center below it."
                           + " With " + this.colorAt(f) + " in front: " + r + ", yellow, " + r + " back, yellow, "
                           + r + ", yellow twice, " + r + " back.", f);
                     this.alg(SUNE, f);
                     this.end();
                  }
                  this.begin(4, "Turn yellow so every yellow edge matches the center below it.", this.sides[0]);
                  this.turnUp(k);
                  this.end();
                  return;
               }
               trial.turnUp(1);
            }
         }
      }
      throw new IllegalStateException("Yellow edges not found");
   }

   private boolean topEdgesHome() {
      for (int[] side : this.sides) {
         if (!this.solved(add(this.up, side))) {
            return false;
         }
      }
      return true;
   }

   private void yellowCorners() {
      for (int front : this.search(CYCLE, 3, coach -> coach.topCornersInPlace())) {
         int[] f = this.sides[front];
         String r = this.colorAt(cross(this.up, f));
         this.begin(5, "Move the yellow corners to their own spots (they may still be twisted). With "
               + this.colorAt(f) + " in front this trades three corners around and keeps the front right one where it"
               + " is: yellow, " + r + ", yellow back, the left side back, yellow, " + r + " back, yellow back, the"
               + " left side.", f);
         this.alg(CYCLE, f);
         this.end();
      }
   }

   private boolean topCornersInPlace() {
      if (!this.topEdgesHome()) {
         return false;
      }
      for (int[] side : this.sides) {
         int[] home = add(add(this.up, side), cross(this.up, side));
         if (!same(this.locate(home), home)) {
            return false;
         }
      }
      return true;
   }

   private void twist() {
      int[] front = this.sides[0];
      int[] spot = add(add(this.up, front), cross(this.up, front));
      String r = this.colorAt(cross(this.up, front));
      int pending = 0;
      for (int i = 0; i < 4; i++) {
         Coach ahead = new Coach(this);
         ahead.turnUp(pending);
         if (ahead.state[sticker(spot, this.up)] != ahead.center(this.up)) {
            if (pending % 4 != 0) {
               this.begin(6, "Turn yellow to bring the next twisted corner to the front right, between "
                     + this.colorAt(front) + " and " + r + ".", front);
               this.turnUp(pending);
               this.end();
            }
            pending = 0;
            this.begin(6, "Twist the corner at the front right (" + this.colorAt(front) + " in front): repeat " + r
                  + " back, white back, " + r + ", white until yellow faces up. The white layers come apart for now;"
                  + " they come back when every corner is done.", front);
            for (int n = 0; this.state[sticker(spot, this.up)] != this.center(this.up); n++) {
               if (n == 6) {
                  throw new IllegalStateException("Corner did not twist");
               }
               this.alg(TWIST, front);
            }
            this.end();
         }
         pending++;
      }
      if (pending % 4 != 0) {
         this.begin(6, "Turn yellow to line up the last layer.", front);
         this.turnUp(pending);
         this.end();
      }
   }

   // ---- searching ----

   private interface Placer {
      void place(Coach coach, int[] home);
   }

   /** The index of the home whose piece the placer puts in with the fewest turns. */
   private int cheapest(List<int[]> homes, Placer placer) {
      int best = 0;
      int fewest = Integer.MAX_VALUE;
      for (int i = 0; i < homes.size(); i++) {
         Coach trial = new Coach(this);
         placer.place(trial, homes.get(i));
         if (trial.turnCount < fewest) {
            fewest = trial.turnCount;
            best = i;
         }
      }
      return best;
   }

   /** The fewest goes of a move pattern (as the side each go faces) that reach the goal, trying up to max goes. */
   private int[] search(String moves, int max, Predicate<Coach> goal) {
      for (int length = 0; length <= max; length++) {
         for (int combo = 0; combo < 1 << 2 * length; combo++) {
            Coach trial = new Coach(this);
            int[] fronts = new int[length];
            for (int i = 0; i < length; i++) {
               fronts[i] = combo >> 2 * i & 3;
               trial.alg(moves, this.sides[fronts[i]]);
            }
            if (goal.test(trial)) {
               return fronts;
            }
         }
      }
      throw new IllegalStateException("No way found for " + moves);
   }

   /** Turns the top layer the fewest quarter turns until the test passes. */
   private void upUntil(Predicate<Coach> test) {
      for (int k = 0; k < 4; k++) {
         Coach trial = new Coach(this);
         trial.turnUp(k);
         if (test.test(trial)) {
            this.turnUp(k);
            return;
         }
      }
      throw new IllegalStateException("Turning the top did not help");
   }

   // ---- turning ----

   private void begin(int stage, String text, int[] front) {
      this.current = new Step(stage, text, this.up, front);
      this.steps.add(this.current);
   }

   private void end() {
      if (this.current.turns.isEmpty()) {
         this.steps.remove(this.current);
      }
      this.current = null;
   }

   private void turn(int id) {
      int[] dest = CubeGeometry.destinations(id);
      int[] before = this.state.clone();
      for (int s = 0; s < 54; s++) {
         this.state[dest[s]] = before[s];
      }
      this.turnCount++;
      if (this.current != null) {
         this.current.turns.add(id);
      }
   }

   /** k quarter turns of the top, clockwise; three is one turn back. */
   private void turnUp(int k) {
      switch (k % 4) {
         case 1: this.turn(faceTurn(this.up, true)); break;
         case 2: this.turn(faceTurn(this.up, true)); this.turn(faceTurn(this.up, true)); break;
         case 3: this.turn(faceTurn(this.up, false)); break;
         default: break;
      }
   }

   /** Applies moves in face notation (U D F B R L, ' for back, 2 for twice) with yellow up and that side in front. */
   private void alg(String moves, int[] front) {
      int[] right = cross(this.up, front);
      for (String token : moves.split(" ")) {
         int[] normal;
         switch (token.charAt(0)) {
            case 'U': normal = this.up; break;
            case 'D': normal = this.down; break;
            case 'F': normal = front; break;
            case 'B': normal = neg(front); break;
            case 'R': normal = right; break;
            default: normal = neg(right); break;
         }
         boolean back = token.endsWith("'");
         int id = faceTurn(normal, !back);
         this.turn(id);
         if (token.endsWith("2")) {
            this.turn(id);
         }
      }
   }

   // ---- looking at the cube ----

   private int center(int[] normal) {
      return this.state[sticker(normal, normal)];
   }

   private String colorAt(int[] normal) {
      return CubeGeometry.FACE_COLORS[this.center(normal)];
   }

   private boolean solved(int[] pos) {
      for (int s : stickersOf(pos)) {
         if (this.state[s] != this.center(CubeGeometry.NORMAL[s])) {
            return false;
         }
      }
      return true;
   }

   private boolean allSolved() {
      for (int s = 0; s < 54; s++) {
         if (this.state[s] != this.center(CubeGeometry.NORMAL[s])) {
            return false;
         }
      }
      return true;
   }

   /** Where the piece that belongs at {@code home} is now. */
   private int[] locate(int[] home) {
      int[] wanted = new int[0];
      for (int s : stickersOf(home)) {
         wanted = append(wanted, this.center(CubeGeometry.NORMAL[s]));
      }
      Arrays.sort(wanted);
      for (int[] p : wanted.length == 3 ? CORNERS : EDGES) {
         int[] colors = new int[0];
         for (int s : stickersOf(p)) {
            colors = append(colors, this.state[s]);
         }
         Arrays.sort(colors);
         if (Arrays.equals(colors, wanted)) {
            return p;
         }
      }
      throw new IllegalStateException("Piece not found");
   }

   /** The side that has {@code p} at its front right, with yellow up. */
   private int[] frontOf(int[] p) {
      for (int[] side : this.sides) {
         if (dot(p, side) == 1 && dot(p, cross(this.up, side)) == 1) {
            return side;
         }
      }
      throw new IllegalStateException("No front");
   }

   // ---- geometry ----

   private static int[] stickersOf(int[] pos) {
      int[] out = new int[0];
      for (int k = 0; k < 3; k++) {
         if (pos[k] != 0) {
            int[] normal = new int[3];
            normal[k] = pos[k];
            out = append(out, sticker(pos, normal));
         }
      }
      return out;
   }

   private static int sticker(int[] pos, int[] normal) {
      return STICKER_AT[posIndex(pos) * 6 + normalIndex(normal)];
   }

   private static int posIndex(int[] p) {
      return (p[0] + 1) * 9 + (p[1] + 1) * 3 + p[2] + 1;
   }

   private static int normalIndex(int[] n) {
      for (int i = 0; i < 6; i++) {
         if (same(NORMALS[i], n)) {
            return i;
         }
      }
      return -1;
   }

   private static int edgeIndex(int[] p) {
      for (int e = 0; e < EDGES.size(); e++) {
         if (same(EDGES.get(e), p)) {
            return e;
         }
      }
      return -1;
   }

   private static int[] append(int[] a, int v) {
      int[] out = Arrays.copyOf(a, a.length + 1);
      out[a.length] = v;
      return out;
   }

   private static boolean same(int[] a, int[] b) {
      return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
   }

   private static int dot(int[] a, int[] b) {
      return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
   }

   private static int[] cross(int[] a, int[] b) {
      return new int[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
   }

   private static int[] add(int[] a, int[] b) {
      return new int[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
   }

   private static int[] sub(int[] a, int[] b) {
      return new int[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
   }

   private static int[] scale(int[] a, int k) {
      return new int[]{a[0] * k, a[1] * k, a[2] * k};
   }

   private static int[] neg(int[] a) {
      return scale(a, -1);
   }
}
