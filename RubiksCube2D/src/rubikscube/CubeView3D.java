package rubikscube;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The cube in 3D: 26 cubies drawn in perspective, whose layers turn on screen as each move is made. Dragging a
 * square turns its row, column or ring the way the drag goes, the layer following the pointer until it is let go;
 * dragging the background (or the arrow keys) turns the whole cube to look at another side; a click selects a square for W/A/S/D. Moves that come faster than a turn takes
 * to show (a long Mix) are skipped to the latest position.
 */
final class CubeView3D extends JPanel implements Cube.Watcher {
   private static final int TURN_MILLIS = 240;
   private static final double DEFAULT_YAW = -0.62;
   private static final double DEFAULT_PITCH = 0.48;
   private static final double CAMERA = 9.0;
   private static final double HALF = 0.47;
   private static final double STICKER = 0.40;
   private static final Color BODY = new Color(22, 22, 28);
   private static final Color RING = new Color(255, 255, 255);
   private static final Color ARROW = new Color(255, 255, 255);
   private static final Color ARROW_EDGE = new Color(20, 20, 20);
   private static final Color[] SKY = {new Color(226, 233, 246), new Color(252, 253, 255)};
   private final Cube cube;
   private final BiConsumer<String, Integer> onSelect;
   private final Consumer<String> onTurn;
   /** The colors shown at each sticker position: the cube as it was before the turns still waiting to be shown. */
   private final String[] shown = new String[54];
   private final List<CubeGeometry.Turn> pending = new ArrayList<>();
   private final List<String[]> pendingAfter = new ArrayList<>();
   private final Timer timer = new Timer(16, event -> this.tick());
   private long turnStart = 0L;
   private double turnAngle = 0.0;
   private double yaw = DEFAULT_YAW;
   private double pitch = DEFAULT_PITCH;
   private double targetYaw = DEFAULT_YAW;
   private double targetPitch = DEFAULT_PITCH;
   /** A showing-off lap of the whole cube (Patterns): when it began (0 for none) and the yaw it began from. */
   private long spinStart = 0L;
   private double spinFrom = 0.0;
   /**
    * A turn of the whole cube applied before yaw and pitch, as rows (world x, y and z of a point are its dot products
    * with them): Teach me stands the cube yellow side up with it. It never changes what a drag or key does.
    */
   private int[][] basis = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
   /** Mirrors: how far out beyond its side each hidden side is drawn (which sets its perspective), and its size. */
   private static final double MIRROR_GAP = 1.0;
   private static final double MIRROR_SCALE = 0.45;
   private boolean mirrors = true;
   /** The frame's row of buttons over the top of the view, which mirrors keep below. */
   private static final int TOP_ROW = 44;
   /** Each side's mirror on screen (its center), gliding to where it belongs; null while that side is in view. */
   private final double[][] mirrorAt = new double[6][];
   /** The edge of the view each side's mirror stands at: -1 left, 1 right (kept while its side faces straight away). */
   private final int[] mirrorSide = {1, 1, 1, 1, 1, 1};
   /** The mirrors as last drawn, for clicks: {face, x0, y0, ...}. */
   final List<int[]> mirrorHits = new ArrayList<>();
   private boolean mirrorsGliding = false;
   private int marked = -1;
   private int hinted = -1;
   private String hintKey = null;
   /** The sticker polygons as last drawn, back to front, for clicks: {sticker, x0, y0, x1, y1, ...}. */
   private final List<int[]> hits = new ArrayList<>();
   private int pressX;
   private int pressY;
   private int pressSticker = -1;
   private boolean dragged = false;
   private long pressMillis;
   /** Whether a drag may turn a layer now (the frame takes no moves while a mix plays or a cube is painted). */
   BooleanSupplier canTurn = () -> true;
   /**
    * The layer a drag is turning by hand: it follows the pointer (liveFraction of a quarter turn, below 0 the other
    * way) and becomes a move when let go past half way or flicked; otherwise it springs back. Null for none.
    */
   private CubeGeometry.Turn liveTurn = null;
   private String liveKey = null;
   private double liveFraction = 0.0;
   /** The pointer's travel on screen for a quarter turn of the live layer. */
   private double[] liveAlong = null;
   /** How much of a quarter turn a let-go drag had shown, so the move's own turn goes on from there. */
   private double resumeFrom = 0.0;
   private int turnsSeen = 0;
   /** The pointer's travel for a quarter turn, in cube units (a sticker's width is about 0.8). */
   private static final double QUARTER_TRAVEL = 1.6;
   /**
    * Show me how (level 1 until it is first solved): a fingertip drags the square of the move back part way, the
    * layer following it, and lets it spring back, over and over until the player presses the cube. -1 for none.
    */
   private int demoSticker = -1;
   private CubeGeometry.Turn demoTurn = null;
   private long demoStart = 0L;
   private double demoFraction = 0.0;
   private static final int DEMO_MILLIS = 2600;
   private static final Color FINGER = new Color(255, 255, 255, 210);
   private static final Color FINGER_EDGE = new Color(20, 20, 20, 150);
   /** A solve's moment: the stars won (of 3; -1 for none, in free play) popping in over the cube, with a caption. */
   private long cheerStart = 0L;
   private int cheerStars = -1;
   private String cheerText = "";
   private boolean cheerStill = false;
   static final int CHEER_MILLIS = 4200;
   private static final Color GOLD = new Color(255, 196, 40);
   private static final Color GOLD_EDGE = new Color(150, 96, 0);
   private static final Color STAR_EMPTY = new Color(40, 50, 80, 60);
   /**
    * The round strip at the foot of the view (a level only): the mix filling its pips one per move, then a pip for
    * each move made against par with the stars still in reach, then Next once solved. A null label for none.
    */
   private String stripLabel = null;
   private int stripPar = 0;
   private int stripDone = 0;
   private int stripStars = -1;
   private boolean stripNext = false;
   private long stripChanged = 0L;
   /** The strip and its Next button as last drawn {x, y, w, h}, or null. */
   private int[] stripBox = null;
   private int[] stripButton = null;
   /** Next pressed on the strip. */
   Runnable onNext = () -> { };
   private static final Color INK = new Color(30, 40, 70);
   private static final Color OVER = new Color(196, 110, 0);
   private static final Color FAR_OVER = new Color(190, 40, 40);
   private static final Color NEXT = new Color(36, 92, 200);
   /**
    * The turn arrow: a curved arrow round the layer that is turning, its head riding with the layer, which stays a
    * moment after the turn and fades, so a mix or a Solve can be followed turn by turn.
    */
   private CubeGeometry.Turn arcTurn = null;
   private long arcEnd = 0L;
   private long lastTurned = 0L;
   /** No arrow for the newest turn: it came hard on the one before (a long Mix, keys held), or a drag made it. */
   private boolean quick = false;
   static final int ARC_FADE = 600;
   /** How far out from the turning axis the arrow runs: just over the cube's surface (1.5). */
   private static final double ARC_OVER = 1.53;
   private static final double ARC_SPAN = Math.PI * 0.5;

   CubeView3D(Cube cube, BiConsumer<String, Integer> onSelect, Consumer<String> onTurn) {
      this.cube = cube;
      this.onSelect = onSelect;
      this.onTurn = onTurn;
      this.setLayout(null);
      this.reset();
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            CubeView3D.this.pressed(event.getX(), event.getY(), event.getWhen());
         }

         @Override
         public void mouseDragged(MouseEvent event) {
            CubeView3D.this.draggedTo(event.getX(), event.getY());
         }

         @Override
         public void mouseReleased(MouseEvent event) {
            CubeView3D.this.released(event.getWhen());
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
   }

   // ---- what the cube does ----

   @Override
   public void turned(CubeGeometry.Turn turn) {
      this.turnsSeen++;
      this.cheerStart = 0L;
      long now = System.currentTimeMillis();
      this.quick = now - this.lastTurned < 100;
      this.lastTurned = now;
      this.pending.add(turn);
      this.pendingAfter.add(this.cubeColors());
      if (this.pending.size() > 6) {
         // Far behind (a long Mix): jump to the position before the newest turn and show only that one.
         String[] before = this.pendingAfter.get(this.pending.size() - 2);
         System.arraycopy(before, 0, this.shown, 0, 54);
         CubeGeometry.Turn last = this.pending.get(this.pending.size() - 1);
         String[] after = this.pendingAfter.get(this.pending.size() - 1);
         this.pending.clear();
         this.pendingAfter.clear();
         this.pending.add(last);
         this.pendingAfter.add(after);
         this.turnStart = 0L;
      }
      this.wake();
   }

   @Override
   public void reset() {
      this.pending.clear();
      this.pendingAfter.clear();
      System.arraycopy(this.cubeColors(), 0, this.shown, 0, 54);
      this.turnStart = 0L;
      this.turnAngle = 0.0;
      this.cheerStart = 0L;
      this.arcTurn = null;
      this.repaint();
   }

   private String[] cubeColors() {
      String[] colors = new String[54];
      Face[] faces = this.cube.faces();
      for (int s = 0; s < 54; s++) {
         colors[s] = faces[s / 9].stickers[s % 9];
      }
      return colors;
   }

   /** Rings square {@code cubie} (1 to 9) of that face, or none for a null face. */
   void mark(String faceColor, int cubie) {
      int face = faceColor == null ? -1 : CubeGeometry.faceIndex(faceColor);
      this.marked = face < 0 || cubie < 1 ? -1 : face * 9 + cubie - 1;
      this.repaint();
   }

   /** Draws an arrow for the hinted move on its square, and turns the cube to show it; a null key clears it. */
   void hint(String faceColor, int cubie, String key) {
      int face = faceColor == null ? -1 : CubeGeometry.faceIndex(faceColor);
      this.hinted = face < 0 || key == null ? -1 : face * 9 + cubie - 1;
      this.hintKey = key;
      if (this.hinted >= 0 && this.facingOf(this.hinted) < 0.3) {
         this.lookAt(CubeGeometry.NORMAL[this.hinted]);
      }
      this.repaint();
   }

   /**
    * Starts the Show me how drag of that move's layer turn, or stops it for a null face. It drags whichever square
    * makes the same turn and faces the viewer most squarely (a bottom layer by its row on the front, not the bottom
    * face seen edge on), and turns the cube to show it only when none faces the viewer.
    */
   void demo(String faceColor, int cubie, String key) {
      CubeGeometry.Turn turn = faceColor == null || key == null ? null : CubeGeometry.turnOf(faceColor, key, cubie);
      this.demoSticker = -1;
      this.demoTurn = turn;
      for (int s = 0; turn != null && s < 54; s++) {
         for (String k : CubeGeometry.KEYS) {
            CubeGeometry.Turn same = CubeGeometry.turnOf(CubeGeometry.FACE_COLORS[s / 9], k, s % 9 + 1);
            if (same != null && same.id() == turn.id() && (this.demoSticker < 0 || this.facingOf(s) > this.facingOf(this.demoSticker))) {
               this.demoSticker = s;
            }
         }
      }
      this.demoFraction = 0.0;
      this.demoStart = System.currentTimeMillis();
      if (this.demoSticker >= 0 && this.facingOf(this.demoSticker) < 0.3) {
         this.lookAt(CubeGeometry.NORMAL[this.demoSticker]);
      }
      this.wake();
   }

   boolean demoing() {
      return this.demoSticker >= 0;
   }

   /** Turns the whole cube once round, slowly, so every side of it shows (a pattern just made). */
   void spin() {
      this.spinStart = System.currentTimeMillis();
      this.spinFrom = this.targetYaw;
      this.wake();
   }

   /**
    * The cube was solved: a lap of the whole cube while the stars won pop in above it one by one and fade, so the
    * moment reads at a glance. {@code still} (reduced motion) shows them at once and keeps the cube where it is.
    */
   void cheer(int stars, String text, boolean still) {
      this.cheerStart = System.currentTimeMillis();
      this.cheerStars = stars;
      this.cheerText = text;
      this.cheerStill = still;
      if (!still) {
         this.spin();
      }
      this.wake();
   }

   boolean cheering() {
      return this.cheerStart != 0L;
   }

   /**
    * Sets the round strip: {@code label} (null hides it), the level's par, the moves done (mixed, or made by the
    * player), the stars still in reach (-1 to show none) and whether it is a Next button.
    */
   void strip(String label, int par, int done, int stars, boolean next) {
      if (done != this.stripDone || !java.util.Objects.equals(label, this.stripLabel)) {
         this.stripChanged = System.currentTimeMillis();
      }
      boolean same = java.util.Objects.equals(label, this.stripLabel) && par == this.stripPar && done == this.stripDone
            && stars == this.stripStars && next == this.stripNext;
      this.stripLabel = label;
      this.stripPar = par;
      this.stripDone = done;
      this.stripStars = stars;
      this.stripNext = next;
      if (!same) {
         this.wake();
      }
   }

   String stripLabel() {
      return this.stripLabel;
   }

   int[] stripButton() {
      return this.stripButton;
   }

   int[] stripBox() {
      return this.stripBox;
   }

   /** Shows or hides the mirrors of the hidden sides. */
   void setMirrors(boolean on) {
      this.mirrors = on;
      java.util.Arrays.fill(this.mirrorAt, null);
      this.mirrorHits.clear();
      this.repaint();
   }

   boolean mirrors() {
      return this.mirrors;
   }

   /** Turns the view by a step: an arrow key. */
   void orbit(int right, int down) {
      this.spinStart = 0L;
      this.targetYaw -= right * Math.PI / 4;
      this.targetPitch = clampPitch(this.targetPitch - down * Math.PI / 8);
      this.wake();
   }

   /**
    * Teach me: shows the cube with {@code up} on top (standing it up at once if another side was up) and turns it
    * to look at {@code front} and the side to its right from a little above.
    */
   void stand(int[] up, int[] front) {
      int[] right = {up[1] * front[2] - up[2] * front[1], up[2] * front[0] - up[0] * front[2], up[0] * front[1] - up[1] * front[0]};
      boolean jump = dot(this.basis[1], up) != 1;
      if (jump) {
         this.basis = new int[][]{right, up.clone(), front.clone()};
      }
      this.lookAt(front);
      this.targetPitch = DEFAULT_PITCH;
      if (jump) {
         this.yaw = this.targetYaw;
         this.pitch = this.targetPitch;
      }
      this.wake();
   }

   private static int dot(int[] a, int[] b) {
      return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
   }

   private void lookAt(int[] worldNormal) {
      double[] n = this.based(new double[]{worldNormal[0], worldNormal[1], worldNormal[2]});
      int[] normal = {(int) Math.round(n[0]), (int) Math.round(n[1]), (int) Math.round(n[2])};
      if (normal[1] != 0) {
         this.targetPitch = normal[1] * Math.abs(DEFAULT_PITCH);
      } else {
         double toward = Math.atan2(-normal[0], normal[2]);
         // The nearest equivalent angle, so the cube turns the short way round.
         double base = toward + DEFAULT_YAW;
         this.targetYaw = base + Math.round((this.yaw - base) / (2 * Math.PI)) * 2 * Math.PI;
         this.targetPitch = Math.abs(this.pitch) < 0.2 ? DEFAULT_PITCH : Math.signum(this.pitch) * Math.abs(DEFAULT_PITCH);
      }
      this.wake();
   }

   private static double clampPitch(double value) {
      return Math.max(-1.45, Math.min(1.45, value));
   }

   // ---- animation ----

   private void wake() {
      if (!this.timer.isRunning()) {
         this.timer.start();
      }
      this.repaint();
   }

   private void tick() {
      boolean busy = false;
      if (!this.pending.isEmpty()) {
         long now = System.currentTimeMillis();
         // Turns queued behind this one make it quicker.
         double length = TURN_MILLIS / (1.0 + 0.6 * (this.pending.size() - 1));
         if (this.turnStart == 0L) {
            // A turn a drag already took part way starts where the drag left it (the inverse of the easing below).
            double from = this.resumeFrom < 0.5 ? Math.sqrt(this.resumeFrom / 2) : 1 - Math.sqrt((1 - this.resumeFrom) / 2);
            this.turnStart = now - (long) (from * length);
            this.resumeFrom = 0.0;
         }
         double t = Math.min(1.0, (now - this.turnStart) / length);
         this.turnAngle = Math.PI / 2 * (t < 0.5 ? 2 * t * t : 1 - 2 * (1 - t) * (1 - t));
         if (t >= 1.0) {
            System.arraycopy(this.pendingAfter.remove(0), 0, this.shown, 0, 54);
            this.arcTurn = this.pending.remove(0);
            this.arcEnd = now;
            this.turnAngle = 0.0;
            this.turnStart = 0L;
         }
         busy = true;
      }
      if (this.liveTurn != null && this.pressSticker < 0) {
         // A drag let go short of half way: the layer springs back.
         this.liveFraction *= 0.6;
         if (Math.abs(this.liveFraction) < 0.01) {
            this.liveTurn = null;
         }
         busy = true;
      }
      if (this.demoSticker >= 0) {
         // A rest, a slow pull to 40% of a quarter turn, a hold, then a spring back (the drag let go short).
         double t = (System.currentTimeMillis() - this.demoStart) % DEMO_MILLIS / (double) DEMO_MILLIS;
         double pull = t < 0.2 ? 0 : t < 0.55 ? (t - 0.2) / 0.35 : t < 0.7 ? 1 : t < 0.82 ? 1 - (t - 0.7) / 0.12 : 0;
         this.demoFraction = 0.4 * pull * pull * (3 - 2 * pull);
         busy = true;
      }
      if (this.spinStart != 0L) {
         double t = Math.min(1.0, (System.currentTimeMillis() - this.spinStart) / 3600.0);
         double eased = t < 0.5 ? 2 * t * t : 1 - 2 * (1 - t) * (1 - t);
         this.yaw = this.targetYaw = this.spinFrom - 2 * Math.PI * eased;
         this.spinStart = t >= 1.0 ? 0L : this.spinStart;
         busy = true;
      }
      if (this.cheerStart != 0L) {
         this.cheerStart = System.currentTimeMillis() - this.cheerStart >= CHEER_MILLIS ? 0L : this.cheerStart;
         busy = true;
      }
      if (this.arcTurn != null) {
         this.arcTurn = System.currentTimeMillis() - this.arcEnd >= ARC_FADE ? null : this.arcTurn;
         busy = true;
      }
      if (System.currentTimeMillis() - this.stripChanged < STRIP_POP) {
         busy = true;
      }
      double dy = this.targetYaw - this.yaw;
      double dp = this.targetPitch - this.pitch;
      if (Math.abs(dy) > 0.002 || Math.abs(dp) > 0.002) {
         this.yaw += dy * 0.2;
         this.pitch += dp * 0.2;
         busy = true;
      } else {
         this.yaw = this.targetYaw;
         this.pitch = this.targetPitch;
      }
      if (!busy && !this.mirrorsGliding) {
         this.timer.stop();
      }
      this.repaint();
   }

   // ---- the pointer ----

   private void pressed(int x, int y, long when) {
      this.spinStart = 0L;
      this.cheerStart = 0L;
      this.demo(null, 0, null);
      this.liveTurn = null;
      this.pressMillis = when;
      this.pressX = x;
      this.pressY = y;
      this.dragged = false;
      this.pressSticker = this.stickerAt(x, y);
   }

   private void draggedTo(int x, int y) {
      int dx = x - this.pressX;
      int dy = y - this.pressY;
      if (this.pressSticker < 0) {
         // Looking around: the drag turns the whole cube.
         this.targetYaw = this.yaw = this.yaw + dx * 0.012;
         this.targetPitch = this.pitch = clampPitch(this.pitch + dy * 0.012);
         this.pressX = x;
         this.pressY = y;
         this.dragged = true;
         this.repaint();
      } else if (this.liveTurn != null) {
         this.liveFraction = Math.max(-1, Math.min(1, (dx * this.liveAlong[0] + dy * this.liveAlong[1])
               / (this.liveAlong[0] * this.liveAlong[0] + this.liveAlong[1] * this.liveAlong[1])));
         this.repaint();
      } else if (!this.dragged && dx * dx + dy * dy >= 14 * 14) {
         this.dragged = true;
         String key = this.keyAlong(this.pressSticker, dx, dy);
         this.onSelect.accept(CubeGeometry.FACE_COLORS[this.pressSticker / 9], this.pressSticker % 9 + 1);
         if (this.pending.isEmpty() && this.canTurn.getAsBoolean()) {
            this.liveKey = key;
            this.liveTurn = CubeGeometry.turnOf(CubeGeometry.FACE_COLORS[this.pressSticker / 9], key, this.pressSticker % 9 + 1);
            this.liveAlong = this.screenAlong(this.pressSticker, key, QUARTER_TRAVEL);
            this.draggedTo(x, y);
         } else {
            // A turn still showing, or no moves taken now: the drag asks for its move at once, as before.
            this.onTurn.accept(key);
         }
      }
   }

   private void released(long when) {
      if (this.liveTurn != null && this.pressSticker >= 0) {
         double f = this.liveFraction;
         boolean flick = when - this.pressMillis < 300 && Math.abs(f) > 0.15;
         if (Math.abs(f) >= 0.5 || flick) {
            CubeGeometry.Turn taken = this.liveTurn;
            this.liveTurn = null;
            this.resumeFrom = Math.abs(f);
            int seen = this.turnsSeen;
            this.onTurn.accept(f > 0 ? this.liveKey : MoveHistory.inverse(this.liveKey));
            if (this.turnsSeen == seen) {
               // The move was not taken after all: spring back.
               this.resumeFrom = 0.0;
               this.liveTurn = taken;
            } else {
               // The player's own hand made it: no arrow needed.
               this.quick = true;
            }
         }
         this.wake();
      } else if (this.pressSticker >= 0 && !this.dragged) {
         this.onSelect.accept(CubeGeometry.FACE_COLORS[this.pressSticker / 9], this.pressSticker % 9 + 1);
      } else if (!this.dragged && this.stripNext && this.stripButton != null && this.pressX >= this.stripButton[0]
            && this.pressX < this.stripButton[0] + this.stripButton[2] && this.pressY >= this.stripButton[1]
            && this.pressY < this.stripButton[1] + this.stripButton[3]) {
         this.onNext.run();
      } else if (!this.dragged) {
         // A click on a mirror turns its side round to the front.
         for (int[] mirror : this.mirrorHits) {
            if (inside(mirror, this.pressX, this.pressY)) {
               this.lookAt(CubeGeometry.NORMAL[mirror[0] * 9]);
            }
         }
      }
      this.pressSticker = -1;
   }

   /** The key whose move carries the sticker most nearly along the screen direction (dx, dy). */
   String keyAlong(int sticker, int dx, int dy) {
      String best = "w";
      double bestCos = -2;
      double length = Math.hypot(dx, dy);
      for (String key : CubeGeometry.KEYS) {
         double[] e = this.screenAlong(sticker, key, 0.4);
         double ex = e[0];
         double ey = e[1];
         double cos = (ex * dx + ey * dy) / (Math.hypot(ex, ey) * length + 1e-9);
         if (cos > bestCos) {
            bestCos = cos;
            best = key;
         }
      }
      return best;
   }

   /** Where on screen sticker s moves when carried {@code by} cube units the way {@code key} turns it. */
   double[] screenAlong(int sticker, String key, double by) {
      double[] from = this.screen(this.surface(sticker, 0, null));
      double[] to = this.screen(this.surface(sticker, by, CubeGeometry.keyDirection(sticker / 9, key)));
      return new double[]{to[0] - from[0], to[1] - from[1]};
   }

   /** The topmost sticker drawn under (x, y), or -1. */
   int stickerAt(int x, int y) {
      for (int i = this.hits.size() - 1; i >= 0; i--) {
         int[] hit = this.hits.get(i);
         if (inside(hit, x, y)) {
            return hit[0];
         }
      }
      return -1;
   }

   private static boolean inside(int[] poly, int x, int y) {
      boolean in = false;
      int n = (poly.length - 1) / 2;
      for (int i = 0, j = n - 1; i < n; j = i++) {
         int xi = poly[1 + 2 * i];
         int yi = poly[2 + 2 * i];
         int xj = poly[1 + 2 * j];
         int yj = poly[2 + 2 * j];
         if ((yi > y) != (yj > y) && x < (double)(xj - xi) * (y - yi) / (yj - yi) + xi) {
            in = !in;
         }
      }
      return in;
   }

   // ---- 3D ----

   /** A point on sticker s's face: its center, moved {@code by} along {@code along} (world coordinates). */
   private double[] surface(int s, double by, int[] along) {
      int[] p = CubeGeometry.POS[s];
      int[] n = CubeGeometry.NORMAL[s];
      double[] point = new double[3];
      for (int k = 0; k < 3; k++) {
         point[k] = p[k] + n[k] * 0.5 + (along == null ? 0 : along[k] * by);
      }
      return this.view(point);
   }

   /** World to view coordinates: turned by the basis, then by yaw about y, then by pitch about x. */
   private double[] view(double[] v) {
      return this.view(v, this.yaw, this.pitch);
   }

   private double[] view(double[] world, double yaw, double pitch) {
      double[] v = this.based(world);
      double cy = Math.cos(yaw);
      double sy = Math.sin(yaw);
      double x = v[0] * cy + v[2] * sy;
      double z = -v[0] * sy + v[2] * cy;
      double cp = Math.cos(pitch);
      double sp = Math.sin(pitch);
      return new double[]{x, v[1] * cp - z * sp, v[1] * sp + z * cp};
   }

   private double[] based(double[] v) {
      double[] out = new double[3];
      for (int i = 0; i < 3; i++) {
         out[i] = this.basis[i][0] * v[0] + this.basis[i][1] * v[1] + this.basis[i][2] * v[2];
      }
      return out;
   }

   /** How many cube sizes the view must fit across (0 for no limit): wide leaves the mirrors room beside a tall cube. */
   double across = 0;

   private double scale() {
      double scale = Math.min(this.getWidth(), this.stage()) / 5.6;
      return this.across > 0 ? Math.min(scale, this.getWidth() / this.across) : scale;
   }

   /** View coordinates to the panel, in perspective. */
   private double[] screen(double[] v) {
      double k = this.scale() * CAMERA / (CAMERA - v[2]);
      return new double[]{this.getWidth() / 2.0 + v[0] * k, this.stage() / 2.0 - v[1] * k};
   }

   /** The view's height above the round strip's room at its foot, kept free whether it shows or not (so the cube never jumps). */
   private double stage() {
      return this.getHeight() - STRIP_ROOM;
   }

   /** How squarely sticker s faces the viewer once the cube has turned to where it is going (1 head on). */
   double facingOf(int s) {
      int[] normal = CubeGeometry.NORMAL[s];
      return this.view(new double[]{normal[0], normal[1], normal[2]}, this.targetYaw, this.targetPitch)[2];
   }

   /** How squarely a world normal faces the viewer (1 head on, below 0 facing away). */
   private double facing(int[] normal) {
      double[] n = this.view(new double[]{normal[0], normal[1], normal[2]});
      return n[2];
   }

   /** {@code v} turned by angle about a world axis. */
   private static double[] turn(double[] v, int axis, double angle) {
      int a = (axis + 1) % 3;
      int b = (axis + 2) % 3;
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      double[] out = v.clone();
      out[a] = v[a] * c - v[b] * s;
      out[b] = v[a] * s + v[b] * c;
      return out;
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int width = this.getWidth();
      int height = this.getHeight();
      for (int band = 0; band < 16; band++) {
         g.setColor(mix(SKY[0], SKY[1], band / 15.0));
         g.fillRect(0, band * height / 16, width, height / 16 + 1);
      }
      double size = this.scale();
      this.drawShadow(g, size);

      if (this.mirrors) {
         this.drawMirrors(g);
      }
      boolean demo = this.pending.isEmpty() && this.liveTurn == null && this.demoTurn != null;
      CubeGeometry.Turn turn = demo ? this.demoTurn : this.pending.isEmpty() ? this.liveTurn : this.pending.get(0);
      int axis = turn == null ? 0 : turn.axis;
      double fraction = demo ? this.demoFraction : this.liveFraction;
      double angle = turn == null ? 0 : turn.dir * (this.pending.isEmpty() ? fraction * Math.PI / 2 : this.turnAngle);
      // The camera in world coordinates, to draw the slabs along the turning axis from far to near.
      double[] camera = this.world(new double[]{0, 0, CAMERA});
      Integer[] slabs = {-1, 0, 1};
      java.util.Arrays.sort(slabs, (p, q) -> Double.compare(Math.abs(camera[axis] - q), Math.abs(camera[axis] - p)));
      this.hits.clear();
      for (int slab : slabs) {
         List<double[]> cubies = new ArrayList<>();
         for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
               for (int z = -1; z <= 1; z++) {
                  int[] p = {x, y, z};
                  if (p[axis] != slab || (x == 0 && y == 0 && z == 0)) {
                     continue;
                  }
                  boolean turning = turn != null && p[turn.axis] == turn.layer;
                  double[] center = turning ? turn(new double[]{x, y, z}, axis, angle) : new double[]{x, y, z};
                  double distance = dist(center, camera);
                  cubies.add(new double[]{distance, x, y, z, turning ? 1 : 0});
               }
            }
         }
         cubies.sort((p, q) -> Double.compare(q[0], p[0]));
         for (double[] c : cubies) {
            this.drawCubie(g, new int[]{(int) c[1], (int) c[2], (int) c[3]}, c[4] > 0 ? angle : 0, axis);
         }
      }
      this.drawTurnArrow(g);
      this.drawHint(g);
      if (demo) {
         this.drawFinger(g);
      }
      this.drawStrip(g);
      if (this.cheerStart != 0L) {
         this.drawCheer(g);
      }
   }

   /** How long the newest pip of the strip takes to pop in. */
   private static final int STRIP_POP = 260;
   private static final int STRIP_ROOM = 32;

   /**
    * The round strip: a pill at the foot of the view with the label (or the Next button), a pip per move up to par
    * (past it a +n in amber, red once 2 stars are out of reach too) and the stars a solve now would win.
    */
   private void drawStrip(Graphics2D g) {
      this.stripButton = null;
      this.stripBox = null;
      if (this.stripLabel == null) {
         return;
      }
      int h = STRIP_ROOM - 6;
      g.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 13));
      java.awt.FontMetrics metrics = g.getFontMetrics();
      int labelW = metrics.stringWidth(this.stripLabel) + (this.stripNext ? 34 : 0);
      int over = Math.max(0, this.stripDone - this.stripPar);
      String extra = over > 0 ? "+" + over : "";
      int extraW = over > 0 ? metrics.stringWidth(extra) + 6 : 0;
      int starsW = this.stripStars >= 0 ? 52 : 0;
      int par = Math.max(1, this.stripPar);
      int room = this.getWidth() - 24 - 24 - labelW - 10 - extraW - starsW;
      int pip = Math.max(5, Math.min(14, room / par));
      int w = 12 + labelW + 10 + pip * par + extraW + starsW + 12;
      int x = (this.getWidth() - w) / 2;
      int y = this.getHeight() - h - 4;
      this.stripBox = new int[]{x, y, w, h};
      g.setColor(withAlpha(Color.WHITE, 225));
      g.fillRoundRect(x, y, w, h, h, h);
      g.setColor(withAlpha(INK, 70));
      g.drawRoundRect(x, y, w, h, h, h);
      int left = x + 12;
      int base = y + (h + metrics.getAscent() - metrics.getDescent()) / 2;
      if (this.stripNext) {
         // The Next button: a filled pill of its own at the strip's left, its arrow drawn (no font has to have it).
         this.stripButton = new int[]{x + 3, y + 3, labelW + 6, h - 6};
         g.setColor(NEXT);
         g.fillRoundRect(x + 3, y + 3, labelW + 6, h - 6, h - 6, h - 6);
         g.setColor(Color.WHITE);
         g.drawString(this.stripLabel, left + 4, base);
         int ax = left + labelW - 22;
         int cy = y + h / 2;
         g.fillPolygon(new int[]{ax, ax + 9, ax}, new int[]{cy - 6, cy, cy + 6}, 3);
      } else {
         g.setColor(INK);
         g.drawString(this.stripLabel, left, base);
      }
      left += labelW + 10;
      long age = System.currentTimeMillis() - this.stripChanged;
      int cy = y + h / 2;
      for (int i = 0; i < par; i++) {
         boolean filled = i < this.stripDone;
         // The newest pip grows past its size and settles, so each move lands on the strip.
         double pop = filled && i == this.stripDone - 1 && age < STRIP_POP ? 1 + 0.6 * (1 - age / (double) STRIP_POP) : 1;
         double d = Math.max(3, pip - 4) * pop;
         int px = (int) Math.round(left + i * pip + pip / 2.0 - d / 2);
         int py = (int) Math.round(cy - d / 2);
         if (filled) {
            g.setColor(INK);
            g.fillOval(px, py, (int) Math.round(d), (int) Math.round(d));
         } else {
            g.setColor(withAlpha(INK, 110));
            g.drawOval(px, py, (int) Math.round(d), (int) Math.round(d));
         }
      }
      left += pip * par;
      if (over > 0) {
         g.setColor(this.stripDone > 2 * this.stripPar + 2 ? FAR_OVER : OVER);
         g.drawString(extra, left + 4, base);
         left += extraW;
      }
      for (int i = 0; i < 3 && this.stripStars >= 0; i++) {
         int[][] star = starAt(left + 10 + i * 15, cy, 7);
         g.setColor(i < this.stripStars ? GOLD : STAR_EMPTY);
         g.fillPolygon(star[0], star[1], 10);
         g.setColor(i < this.stripStars ? GOLD_EDGE : withAlpha(ARROW_EDGE, 90));
         g.drawPolygon(star[0], star[1], 10);
      }
   }

   /** The solve's stars and caption on a plate over the cube: each star pops in a beat after the last, then all fade. */
   private void drawCheer(Graphics2D g) {
      long t = System.currentTimeMillis() - this.cheerStart;
      double fade = Math.max(0, Math.min(1, (CHEER_MILLIS - t) / 700.0)) * (this.cheerStill ? 1 : Math.min(1, t / 200.0));
      double r = this.scale() * 0.42;
      g.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, Math.max(14, (int) (r * 0.75))));
      java.awt.FontMetrics metrics = g.getFontMetrics();
      boolean stars = this.cheerStars >= 0;
      int plateW = (int) Math.max(stars ? r * 7.4 : 0, metrics.stringWidth(this.cheerText) + r);
      int plateH = (int) ((stars ? r * 2.6 : 0) + metrics.getHeight() + r * 0.6);
      int cx = this.getWidth() / 2;
      int top = (int) this.stage() / 2 - plateH / 2;
      g.setColor(withAlpha(Color.WHITE, (int) (215 * fade)));
      g.fillRoundRect(cx - plateW / 2, top, plateW, plateH, (int) r, (int) r);
      g.setColor(withAlpha(new Color(40, 50, 80), (int) (70 * fade)));
      g.drawRoundRect(cx - plateW / 2, top, plateW, plateH, (int) r, (int) r);
      int cy = (int) (top + r * 0.3 + r * 1.1);
      for (int i = 0; stars && i < 3; i++) {
         // Pops in 350 ms apart: grows past full size and settles (none of that when still).
         double u = this.cheerStill ? 1 : Math.max(0, Math.min(1, (t - 250 - 350 * i) / 300.0));
         double pop = u < 0.7 ? u / 0.7 * 1.25 : 1.25 - 0.25 * (u - 0.7) / 0.3;
         if (pop <= 0) {
            continue;
         }
         boolean won = i < this.cheerStars;
         int[][] star = starAt(cx + (i - 1) * r * 2.3, cy - (i == 1 ? r * 0.2 : 0), r * (won ? pop : Math.min(pop, 1)));
         g.setColor(withAlpha(won ? GOLD : STAR_EMPTY, (int) ((won ? 255 : 60) * fade)));
         g.fillPolygon(star[0], star[1], 10);
         g.setColor(withAlpha(won ? GOLD_EDGE : ARROW_EDGE, (int) ((won ? 255 : 90) * fade)));
         g.setStroke(new BasicStroke(2f));
         g.drawPolygon(star[0], star[1], 10);
      }
      g.setStroke(new BasicStroke(1f));
      int ty = (int) (top + plateH - r * 0.3 - metrics.getDescent());
      g.setColor(withAlpha(new Color(30, 40, 70), (int) (255 * fade)));
      g.drawString(this.cheerText, cx - metrics.stringWidth(this.cheerText) / 2, ty);
   }

   /** A five-pointed star of outer radius r, point up: its corners' x and y (the web shim has no Polygon). */
   private static int[][] starAt(double cx, double cy, double r) {
      int[][] star = new int[2][10];
      for (int k = 0; k < 10; k++) {
         double a = -Math.PI / 2 + k * Math.PI / 5;
         double d = k % 2 == 0 ? r : r * 0.45;
         star[0][k] = (int) Math.round(cx + d * Math.cos(a));
         star[1][k] = (int) Math.round(cy + d * Math.sin(a));
      }
      return star;
   }

   /** The Show me how fingertip: on the square, carried with it while it pulls the layer. */
   private void drawFinger(Graphics2D g) {
      if (this.facing(CubeGeometry.NORMAL[this.demoSticker]) <= 0.05) {
         return;
      }
      // Glued to the square as its layer turns, so the finger never slides off the cube.
      double[] point = new double[3];
      for (int k = 0; k < 3; k++) {
         point[k] = CubeGeometry.POS[this.demoSticker][k] + CubeGeometry.NORMAL[this.demoSticker][k] * 0.5;
      }
      double[] at = this.screen(this.view(turn(point, this.demoTurn.axis, this.demoTurn.dir * this.demoFraction * Math.PI / 2)));
      double r = this.scale() * 0.24;
      // Pressed in (smaller, firmer) while pulling, lifted (larger, lighter) at rest.
      double press = Math.min(1, this.demoFraction / 0.1);
      double radius = r * (1.15 - 0.2 * press);
      int x = (int) Math.round(at[0] - radius);
      int y = (int) Math.round(at[1] - radius);
      int d = (int) Math.round(2 * radius);
      g.setColor(withAlpha(FINGER, (int) (150 + 60 * press)));
      g.fillOval(x, y, d, d);
      g.setColor(FINGER_EDGE);
      g.setStroke(new BasicStroke(2f));
      g.drawOval(x, y, d, d);
      g.setStroke(new BasicStroke(1f));
   }

   private double[] world(double[] v) {
      // The inverse of view(): undo the pitch, then the yaw.
      double cp = Math.cos(this.pitch);
      double sp = Math.sin(this.pitch);
      double y = v[1] * cp + v[2] * sp;
      double z = -v[1] * sp + v[2] * cp;
      double cy = Math.cos(this.yaw);
      double sy = Math.sin(this.yaw);
      double[] b = {v[0] * cy - z * sy, y, v[0] * sy + z * cy};
      // And the basis (its transpose turns it back).
      double[] out = new double[3];
      for (int i = 0; i < 3; i++) {
         out[i] = this.basis[0][i] * b[0] + this.basis[1][i] * b[1] + this.basis[2][i] * b[2];
      }
      return out;
   }

   private static double dist(double[] a, double[] b) {
      double dx = a[0] - b[0];
      double dy = a[1] - b[1];
      double dz = a[2] - b[2];
      return dx * dx + dy * dy + dz * dz;
   }

   private void drawCubie(Graphics2D g, int[] p, double angle, int axis) {
      for (int k = 0; k < 3; k++) {
         for (int sign = -1; sign <= 1; sign += 2) {
            int[] normal = new int[3];
            normal[k] = sign;
            double[] n = this.view(turn(new double[]{normal[0], normal[1], normal[2]}, axis, angle));
            double[] center = new double[3];
            for (int i = 0; i < 3; i++) {
               center[i] = p[i] + normal[i] * HALF;
            }
            double[] c = this.view(turn(center, axis, angle));
            // Seen only from outside: the face must point toward the camera.
            if (n[0] * -c[0] + n[1] * -c[1] + n[2] * (CAMERA - c[2]) <= 0) {
               continue;
            }
            int a = (k + 1) % 3;
            int b = (k + 2) % 3;
            double light = 0.58 + 0.42 * Math.max(0, n[0] * -0.35 + n[1] * 0.62 + n[2] * 0.70);
            g.setColor(shade(BODY, light));
            this.fillSquare(g, center, a, b, HALF, 0, axis, angle, null);
            if (p[k] == sign) {
               int sticker = CubeGeometry.stickerAt(p, normal);
               boolean ringed = sticker == this.marked || sticker == this.hinted;
               int[] face = this.fillSquare(null, center, a, b, ringed ? STICKER * 0.62 : STICKER, ringed ? 0.06 : 0.09, axis, angle, null);
               gloss(g, face, shade(colorOf(this.shown[sticker]), light), n);
               int[] hit = this.fillSquare(null, center, a, b, HALF, 0, axis, angle, null);
               hit[0] = sticker;
               this.hits.add(hit);
               if (ringed) {
                  g.setColor(RING);
                  g.setStroke(new BasicStroke(2f));
                  this.fillSquare(g, center, a, b, STICKER * 0.86, 0.1, axis, angle, Boolean.TRUE);
                  g.setStroke(new BasicStroke(1f));
               }
            }
         }
      }
   }

   /** Halfway between the light (upper left, in front) and the viewer: a face turned this way mirrors the light at you. */
   private static final double[] GLINT = unit(-0.35, 0.62, 0.70 + 1.0);

   private static double[] unit(double x, double y, double z) {
      double length = Math.sqrt(x * x + y * y + z * z);
      return new double[]{x / length, y / length, z / length};
   }

   /**
    * A sticker as glossy plastic: brighter toward the light's corner and darker away from it, with a sheen from that
    * corner that flares into a glint as the face turns to mirror the light at the viewer ({@code n}, its view normal).
    */
   private static void gloss(Graphics2D g, int[] poly, Color color, double[] n) {
      int corners = (poly.length - 1) / 2;
      int[] xs = new int[corners];
      int[] ys = new int[corners];
      int lit = 0;
      int dark = 0;
      for (int i = 0; i < corners; i++) {
         xs[i] = poly[1 + 2 * i];
         ys[i] = poly[2 + 2 * i];
         lit = xs[i] + ys[i] < xs[lit] + ys[lit] ? i : lit;
         dark = xs[i] + ys[i] > xs[dark] + ys[dark] ? i : dark;
      }
      g.setPaint(new GradientPaint(xs[lit], ys[lit], shade(color, 1.18), xs[dark], ys[dark], shade(color, 0.8)));
      g.fillPolygon(xs, ys, corners);
      double glint = Math.pow(Math.max(0, n[0] * GLINT[0] + n[1] * GLINT[1] + n[2] * GLINT[2]), 24);
      int alpha = (int) Math.round(60 + 180 * glint);
      g.setPaint(new GradientPaint(xs[lit], ys[lit], new Color(255, 255, 255, alpha),
            (xs[lit] + xs[dark]) / 2f, (ys[lit] + ys[dark]) / 2f, new Color(255, 255, 255, 0)));
      g.fillPolygon(xs, ys, corners);
   }

   /**
    * The cube's shadow on the floor under it: as wide as its eight corners dropped straight down, so it breathes
    * wider as a corner turns toward the viewer and narrower face on, soft at the rim and darker under the middle.
    */
   private void drawShadow(Graphics2D g, double size) {
      double left = 0;
      double right = 0;
      for (int i = 0; i < 8; i++) {
         double x = this.view(new double[]{(i & 1) == 0 ? -1.5 : 1.5, (i & 2) == 0 ? -1.5 : 1.5, (i & 4) == 0 ? -1.5 : 1.5})[0];
         left = Math.min(left, x);
         right = Math.max(right, x);
      }
      double middle = this.getWidth() / 2.0 + (left + right) / 2 * size;
      double floor = this.stage() / 2.0 + size * 2.45;
      for (int ring = 0; ring < 5; ring++) {
         double w = (right - left) * size * (1.2 - ring * 0.17);
         double h = size * (0.75 - ring * 0.12);
         g.setColor(new Color(40, 50, 80, 13 + ring * 3));
         g.fillOval((int) Math.round(middle - w / 2), (int) Math.round(floor - h / 2), (int) Math.round(w), (int) Math.round(h));
      }
   }

   /**
    * Draws (fills, or outlines when {@code outline} is set) a square with cut corners centered on {@code center} in the
    * plane of axes a and b, after the layer turn. Returns {sticker slot, x0, y0, ...} of its corners on screen.
    */
   private int[] fillSquare(Graphics2D g, double[] center, int a, int b, double half, double cut, int axis, double angle, Boolean outline) {
      double[][] corners = cut <= 0
            ? new double[][]{{-half, -half}, {half, -half}, {half, half}, {-half, half}}
            : new double[][]{{-half + cut, -half}, {half - cut, -half}, {half, -half + cut}, {half, half - cut},
                  {half - cut, half}, {-half + cut, half}, {-half, half - cut}, {-half, -half + cut}};
      int[] xs = new int[corners.length];
      int[] ys = new int[corners.length];
      int[] out = new int[1 + 2 * corners.length];
      for (int i = 0; i < corners.length; i++) {
         double[] point = center.clone();
         point[a] += corners[i][0];
         point[b] += corners[i][1];
         double[] s = this.screen(this.view(turn(point, axis, angle)));
         xs[i] = (int) Math.round(s[0]);
         ys[i] = (int) Math.round(s[1]);
         out[1 + 2 * i] = xs[i];
         out[2 + 2 * i] = ys[i];
      }
      if (g != null) {
         if (outline != null) {
            g.drawPolygon(xs, ys, corners.length);
         } else {
            g.fillPolygon(xs, ys, corners.length);
         }
      }
      return out;
   }

   /**
    * Mirrors: each side turned away from the viewer is shown again at the left or right edge of the view, smaller, as
    * a mirror standing beyond that side would show it (so it reads back to front). A side behind and to the right
    * stands at the right, one underneath at the bottom. Mirrors fade in as their side turns away and glide (behind
    * the cube) when it swaps edges.
    */
   private void drawMirrors(Graphics2D g) {
      this.mirrorHits.clear();
      this.mirrorsGliding = false;
      double width = this.getWidth();
      double height = this.stage();
      for (int face = 0; face < 6; face++) {
         int[] n = CubeGeometry.NORMAL[face * 9];
         double[] v = this.view(new double[]{n[0], n[1], n[2]});
         if (-v[2] <= 0.05) {
            this.mirrorAt[face] = null;
            continue;
         }
         int alpha = (int) Math.round(255 * Math.min(1.0, (-v[2] - 0.05) * 4));
         int k = n[0] != 0 ? 0 : n[1] != 0 ? 1 : 2;
         int a = (k + 1) % 3;
         int b = (k + 2) % 3;
         double[] middle = new double[3];
         middle[k] = n[k] * (1.5 + MIRROR_GAP);
         List<int[]> polys = new ArrayList<>();
         polys.add(this.fillSquare(null, middle, a, b, 1.5 * MIRROR_SCALE + 0.06, 0.12, 0, 0, null));
         for (int s = face * 9; s < face * 9 + 9; s++) {
            double[] center = middle.clone();
            center[a] = CubeGeometry.POS[s][a] * MIRROR_SCALE;
            center[b] = CubeGeometry.POS[s][b] * MIRROR_SCALE;
            int[] poly = this.fillSquare(null, center, a, b, STICKER * MIRROR_SCALE, 0.09 * MIRROR_SCALE, 0, 0, null);
            poly[0] = s;
            polys.add(poly);
         }
         int[] frame = polys.get(0);
         double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
         for (int i = 1; i < frame.length; i += 2) {
            minX = Math.min(minX, frame[i]);
            maxX = Math.max(maxX, frame[i]);
            minY = Math.min(minY, frame[i + 1]);
            maxY = Math.max(maxY, frame[i + 1]);
         }
         if (v[0] < -0.02 || v[0] > 0.02) {
            this.mirrorSide[face] = v[0] < 0 ? -1 : 1;
         }
         double halfW = (maxX - minX) / 2;
         double halfH = (maxY - minY) / 2;
         double up = v[1] / Math.max(1e-6, Math.hypot(v[0], v[1]));
         double cy = height / 2;
         // Above the middle the mirrors stay under the row of buttons.
         double reach = up > 0 ? cy - TOP_ROW - halfH : height - 8 - halfH - cy;
         double[] target = {width / 2 + this.mirrorSide[face] * (width / 2 - halfW - 10), cy - up * Math.max(0, reach)};
         double[] at = this.mirrorAt[face];
         if (at == null) {
            at = this.mirrorAt[face] = target;
         } else {
            at[0] += (target[0] - at[0]) * 0.25;
            at[1] += (target[1] - at[1]) * 0.25;
            this.mirrorsGliding |= Math.abs(target[0] - at[0]) + Math.abs(target[1] - at[1]) > 0.5;
         }
         int dx = (int) Math.round(at[0] - (minX + maxX) / 2);
         int dy = (int) Math.round(at[1] - (minY + maxY) / 2);
         for (int i = 0; i < polys.size(); i++) {
            int[] poly = polys.get(i);
            int corners = (poly.length - 1) / 2;
            int[] xs = new int[corners];
            int[] ys = new int[corners];
            for (int c = 0; c < corners; c++) {
               xs[c] = poly[1 + 2 * c] += dx;
               ys[c] = poly[2 + 2 * c] += dy;
            }
            g.setColor(i == 0 ? withAlpha(BODY, alpha * 3 / 4) : withAlpha(shade(colorOf(this.shown[poly[0]]), 0.92), alpha));
            g.fillPolygon(xs, ys, corners);
         }
         frame[0] = face;
         this.mirrorHits.add(frame);
      }
      if (this.mirrorsGliding && !this.timer.isRunning()) {
         this.timer.start();
      }
   }

   private static Color withAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   /** An arrow across the hinted square, the way its move carries it. */
   private void drawHint(Graphics2D g) {
      if (this.hinted < 0 || this.hintKey == null || !this.pending.isEmpty() || this.liveTurn != null) {
         return;
      }
      int[] along = CubeGeometry.keyDirection(this.hinted / 9, this.hintKey);
      int[] n = CubeGeometry.NORMAL[this.hinted];
      if (this.facing(n) <= 0.05) {
         return;
      }
      double[] tail = this.screen(this.surface(this.hinted, -0.5, along));
      double[] tip = this.screen(this.surface(this.hinted, 0.95, along));
      drawArrow(g, new double[][]{tail, tip}, 255);
   }

   /**
    * The turn arrow round the layer turning now (its head riding with the layer), or the one just turned, fading. None
    * while a drag holds a layer or the turns come too fast to follow.
    */
   private void drawTurnArrow(Graphics2D g) {
      if (this.liveTurn != null) {
         return;
      }
      if (!this.pending.isEmpty()) {
         if (!this.quick) {
            drawArrow(g, this.arc(this.pending.get(0), this.turnAngle / (Math.PI / 2)), 255);
         }
      } else if (this.arcTurn != null && !this.quick) {
         double left = 1 - (System.currentTimeMillis() - this.arcEnd) / (double) ARC_FADE;
         drawArrow(g, this.arc(this.arcTurn, 1), (int) (255 * Math.max(0, Math.min(1, left))));
      }
   }

   /**
    * The turn arrow's points on screen, tail to tip: round the turning layer's axis over the middle of its squares, on
    * the side most toward the viewer (and a little above), swept the way the layer turns as far as {@code progress} (0 to 1).
    */
   double[][] arc(CubeGeometry.Turn turn, double progress) {
      int a = (turn.axis + 1) % 3;
      int b = (turn.axis + 2) % 3;
      double[] unitA = new double[3];
      double[] unitB = new double[3];
      unitA[a] = 1;
      unitB[b] = 1;
      double[] ea = this.view(unitA);
      double[] eb = this.view(unitB);
      double toward = Math.atan2(eb[2] + 0.35 * eb[1], ea[2] + 0.35 * ea[1]);
      double from = toward - turn.dir * ARC_SPAN / 2;
      double sweep = turn.dir * ARC_SPAN * Math.max(0.15, Math.min(1, progress));
      double[][] points = new double[25][];
      for (int i = 0; i < points.length; i++) {
         double phi = from + sweep * i / (points.length - 1);
         // On the layer's squares: out from the axis to just over the cube's surface, round the corners.
         double out = ARC_OVER / Math.max(Math.abs(Math.cos(phi)), Math.abs(Math.sin(phi)));
         double[] p = new double[3];
         p[turn.axis] = turn.layer;
         p[a] = out * Math.cos(phi);
         p[b] = out * Math.sin(phi);
         points[i] = this.screen(this.view(p));
      }
      return points;
   }

   /** A white arrow with a dark rim along the points (tail to tip), {@code alpha} of 255 opaque. */
   private static void drawArrow(Graphics2D g, double[][] points, int alpha) {
      double[] tip = points[points.length - 1];
      double[] before = points[points.length - 2];
      double dx = tip[0] - before[0];
      double dy = tip[1] - before[1];
      double length = Math.hypot(dx, dy);
      double whole = 0;
      for (int i = 1; i < points.length; i++) {
         whole += Math.hypot(points[i][0] - points[i - 1][0], points[i][1] - points[i - 1][1]);
      }
      if (length < 0.5 || whole < 4 || alpha <= 0) {
         return;
      }
      double ux = dx / length;
      double uy = dy / length;
      double head = Math.min(20, whole * 0.4);
      int[] xs = {(int) tip[0], (int)(tip[0] - ux * head - uy * head * 0.6), (int)(tip[0] - ux * head + uy * head * 0.6)};
      int[] ys = {(int) tip[1], (int)(tip[1] - uy * head + ux * head * 0.6), (int)(tip[1] - uy * head - ux * head * 0.6)};
      Path2D.Double shaft = new Path2D.Double();
      shaft.moveTo(points[0][0], points[0][1]);
      for (int i = 1; i < points.length - 1; i++) {
         shaft.lineTo(points[i][0], points[i][1]);
      }
      shaft.lineTo(tip[0] - ux * head * 0.8, tip[1] - uy * head * 0.8);
      g.setColor(withAlpha(ARROW_EDGE, alpha));
      g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.draw(shaft);
      g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.drawPolygon(xs, ys, 3);
      g.fillPolygon(xs, ys, 3);
      g.setColor(withAlpha(ARROW, alpha));
      g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.draw(shaft);
      g.setStroke(new BasicStroke(1f));
      int[] inner = {(int)(tip[0] - ux * 2.5), xs[1] + (int)(ux * 2 + uy * 1.2), xs[2] + (int)(ux * 2 - uy * 1.2)};
      int[] innerY = {(int)(tip[1] - uy * 2.5), ys[1] + (int)(uy * 2 - ux * 1.2), ys[2] + (int)(uy * 2 + ux * 1.2)};
      g.fillPolygon(inner, innerY, 3);
   }

   /** Rich cube colors for the 3D view (the net keeps the app's original pure colors). */
   static Color colorOf(String name) {
      switch (name) {
         case "blue": return new Color(28, 92, 230);
         case "red": return new Color(214, 34, 44);
         case "yellow": return new Color(250, 214, 20);
         case "green": return new Color(22, 168, 72);
         case "orange": return new Color(255, 128, 16);
         case "white": return new Color(245, 245, 245);
         default: return Color.GRAY;
      }
   }

   private static Color shade(Color color, double light) {
      return new Color(clamp(color.getRed() * light), clamp(color.getGreen() * light), clamp(color.getBlue() * light));
   }

   static Color mix(Color a, Color b, double t) {
      return new Color(clamp(a.getRed() + (b.getRed() - a.getRed()) * t), clamp(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            clamp(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
   }

   private static int clamp(double v) {
      return Math.max(0, Math.min(255, (int) Math.round(v)));
   }
}
