package tankgame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Drives a tank against the player. It turns toward where the player will be when a shell gets there, keeps a middle
 * distance, backs or drives out of the way of shells it notices, and fires when its aim is close. It has the same
 * speed, turning rate and reload as a player, and a human-sized reaction time, aim error and trigger delay, and it
 * misses some shells, so it can be beaten. When a wall is in the way it does not fire, but drives round to a spot with
 * a clear shot, and it backs out and turns when it finds itself stuck against a wall. It goes for a supply crate it can
 * reach about as soon as the player can, unless the crate would do it no good (a repair at full health, a second
 * shield).
 */
public class ComputerControls implements Controls
{
    /** Ticks between fresh looks at the player (about a sixth of a second). */
    private static final int     REACTION_TICKS = 10;
    private static final double  AIM_ERROR      = 10;
    private static final double  FIRE_CONE      = 6;
    /** Chance per tick of firing once aimed and reloaded, so it waits about half a second on average. */
    private static final double  TRIGGER_CHANCE = 0.035;
    private static final double  NOTICE_CHANCE  = 0.65;
    private static final int     DODGE_LOOKAHEAD = 40;
    private static final double  NEAR           = 170;
    private static final double  FAR            = 280;
    /** Spacing of the spots it considers when looking for a clear shot round a wall. */
    private static final int     SPOT_STEP      = 20;
    private static final int     UNSTICK_TICKS  = 20;
    /** It goes for a crate this close, or one at most this much farther from it than from the enemy. */
    private static final double  CRATE_NEAR     = 160;
    private static final double  CRATE_LEAD     = 40;
    /** It gives up on a crate it has driven toward this long (6 s), say when the other tank sits between them. */
    private static final int     CRATE_PATIENCE = 360;
    /** It fights instead of fetching a crate when the enemy is this close. */
    private static final double  CRATE_FIGHT    = Tank.SIZE * 2.6;
    private final Random         random;
    /** The crate it last went for, and for how many ticks. */
    private Supply               chased;
    private int                  chaseTicks;
    /** Whether each incoming shell was noticed, decided once per shell. */
    private final Map<Shell, Boolean> noticed   = new HashMap<>();
    private boolean              forward;
    private boolean              back;
    private boolean              left;
    private boolean              right;
    private boolean              fire;
    private int                  sinceLook      = REACTION_TICKS;
    private double               aimX;
    private double               aimY;
    private double               aimError;
    /** Whether a shell fired now could reach the player, seen at the last look. */
    private boolean              clearShot      = true;
    /** Where it is driving to get a clear shot, or null. */
    private double[]             spot;
    private double               lastX;
    private double               lastY;
    private int                  unstick;

    public ComputerControls(Random random)
    {
        this.random = random;
    }

    /** Decides this tick's moves for {@code self} against {@code enemy} on a field with no walls. */
    public void plan(Tank self, Tank enemy, List<Shell> shells, int width, int height)
    {
        plan(self, enemy, shells, width, height, Arena.EMPTY);
    }

    /** Decides this tick's moves for {@code self} against {@code enemy} with no supply crate about. */
    public void plan(Tank self, Tank enemy, List<Shell> shells, int width, int height, Arena arena)
    {
        plan(self, enemy, shells, width, height, arena, null);
    }

    /** Decides this tick's moves for {@code self} against {@code enemy}; {@code supply} is the crate on the field or null. */
    public void plan(Tank self, Tank enemy, List<Shell> shells, int width, int height, Arena arena, Supply supply)
    {
        if (sinceLook++ >= REACTION_TICKS)
        {
            sinceLook = 0;
            // Lead the target: where the enemy will be when a shell fired now reaches it.
            double ticks = self.distanceTo(enemy) / Shell.SPEED;
            aimX = enemy.x() + enemy.velocityX() * ticks;
            aimY = enemy.y() + enemy.velocityY() * ticks;
            aimError = (random.nextDouble() * 2 - 1) * AIM_ERROR;
            clearShot = arena.clear(self.x(), self.y(), enemy.x(), enemy.y())
                            && arena.clear(self.x(), self.y(), aimX, aimY);
            if (clearShot) spot = null;
            else if (spot == null) spot = findSpot(self, enemy, width, height, arena);
            // Wanted to move but did not: stuck on a wall (or the other tank), so back out and turn.
            if ((forward || back) && Math.hypot(self.x() - lastX, self.y() - lastY) < 3) unstick = UNSTICK_TICKS;
            lastX = self.x();
            lastY = self.y();
        }
        noticed.keySet().retainAll(shells);
        fire = false;
        if (unstick > 0)
        {
            unstick--;
            spot = null;
            forward = left = false;
            back = right = true;
            return;
        }
        if (supply != chased)
        {
            chased = supply;
            chaseTicks = 0;
        }
        if (supply != null && chaseTicks < CRATE_PATIENCE && wants(self, enemy, supply))
        {
            chaseTicks++;
            steer(self, supply.x(), supply.y(), 0);
            forward = Math.abs(turnTo(self, supply.x(), supply.y(), 0)) < 50;
            back = false;
            dodgeShells(self, shells, width, height);
            return;
        }
        if (spot != null)
        {
            if (Math.hypot(spot[0] - self.x(), spot[1] - self.y()) < Tank.SPEED * 2) spot = null;
            else
            {
                steer(self, spot[0], spot[1], 0);
                forward = Math.abs(turnTo(self, spot[0], spot[1], 0)) < 40;
                back = false;
                dodgeShells(self, shells, width, height);
                return;
            }
        }
        double turn = steer(self, aimX, aimY, aimError);
        fire = clearShot && Math.abs(turn) < FIRE_CONE && self.reload() == 0 && random.nextDouble() < TRIGGER_CHANCE;

        double distance = self.distanceTo(enemy);
        forward = distance > FAR && Math.abs(turn) < 60;
        back = distance < NEAR;
        dodgeShells(self, shells, width, height);
    }

    /** Whether the crate is worth driving to: it helps, the enemy is not much closer to it, and not right here. */
    static boolean wants(Tank self, Tank enemy, Supply supply)
    {
        if (self.distanceTo(enemy) < CRATE_FIGHT) return false;
        if (supply.power() == Supply.Power.REPAIR && self.health() == Tank.MAX_HEALTH) return false;
        if (supply.power() == Supply.Power.SHIELD && self.shielded()) return false;
        double mine = Math.hypot(supply.x() - self.x(), supply.y() - self.y());
        double theirs = Math.hypot(supply.x() - enemy.x(), supply.y() - enemy.y());
        return mine < CRATE_NEAR || mine < theirs + CRATE_LEAD;
    }

    /** Turns toward a point; returns how far off it still is, in degrees. */
    private double steer(Tank self, double x, double y, double error)
    {
        double turn = turnTo(self, x, y, error);
        left = turn < -Tank.SPEED / 2.0;
        right = turn > Tank.SPEED / 2.0;
        return turn;
    }

    private static double turnTo(Tank self, double x, double y, double error)
    {
        double wanted = Math.toDegrees(Math.atan2(x - self.x(), self.y() - y)) + error;
        return normalize(wanted - self.rotation());
    }

    /**
     * The nearest spot at a middle distance from the enemy with a clear shot at it and room for the tank, preferring
     * spots it can drive straight to; null if there is none.
     */
    private static double[] findSpot(Tank self, Tank enemy, int width, int height, Arena arena)
    {
        double[] best = null;
        double bestCost = Double.MAX_VALUE;
        for (int x = Tank.SIZE; x <= width - Tank.SIZE; x += SPOT_STEP)
        {
            for (int y = Tank.SIZE; y <= height - Tank.SIZE; y += SPOT_STEP)
            {
                double range = Math.hypot(x - enemy.x(), y - enemy.y());
                if (range < NEAR || range > FAR + 60 || arena.blocks(x, y, Tank.SIZE + 2)) continue;
                // A wide berth, so the shot is still clear from wherever near the spot the tank stops.
                if (arena.firstHit(x, y, enemy.x(), enemy.y(), Shell.RADIUS + SPOT_STEP) != null) continue;
                double cost = Math.hypot(x - self.x(), y - self.y());
                if (arena.firstHit(self.x(), self.y(), x, y, Tank.SIZE * 0.6) != null) cost += 200;
                if (cost < bestCost)
                {
                    bestCost = cost;
                    best = new double[] { x, y };
                }
            }
        }
        return best;
    }

    /** Drives forward or back out of the way of a shell about to hit, if one is noticed. */
    private void dodgeShells(Tank self, List<Shell> shells, int width, int height)
    {
        int dodge = dodge(self, shells, width, height);
        if (dodge != 0)
        {
            forward = dodge > 0;
            back = dodge < 0;
        }
    }

    /** +1 to drive forward, -1 to back away from a shell about to hit, 0 when no noticed shell is coming. */
    private int dodge(Tank self, List<Shell> shells, int width, int height)
    {
        boolean threat = false;
        double stayClear = Double.MAX_VALUE;
        double forwardClear = Double.MAX_VALUE;
        double backClear = Double.MAX_VALUE;
        for (Shell shell : shells)
        {
            if (shell.owner() == self) continue;
            if (!noticed.computeIfAbsent(shell, s -> random.nextDouble() < NOTICE_CHANCE)) continue;
            double stay = closest(self, shell, 0);
            if (stay > Tank.SIZE * 1.5) continue;
            threat = true;
            stayClear = Math.min(stayClear, stay);
            forwardClear = Math.min(forwardClear, closest(self, shell, 1));
            backClear = Math.min(backClear, closest(self, shell, -1));
        }
        if (!threat || Math.max(forwardClear, backClear) <= stayClear) return 0;
        return forwardClear >= backClear ? 1 : -1;
    }

    /** The closest a shell comes to the tank's center over the next ticks if the tank drives that way (0 stays). */
    private static double closest(Tank self, Shell shell, int direction)
    {
        double heading = Math.toRadians(self.rotation());
        double best = Double.MAX_VALUE;
        for (int t = 1; t <= DODGE_LOOKAHEAD; t++)
        {
            double tx = self.x() + Math.sin(heading) * Tank.SPEED * direction * t;
            double ty = self.y() - Math.cos(heading) * Tank.SPEED * direction * t;
            double sx = shell.x() + shell.dx() * Shell.SPEED * t;
            double sy = shell.y() + shell.dy() * Shell.SPEED * t;
            best = Math.min(best, Math.hypot(sx - tx, sy - ty));
        }
        return best;
    }

    private static double normalize(double degrees)
    {
        double d = Math.floorMod((int) Math.round(degrees), 360);
        return d > 180 ? d - 360 : d;
    }

    @Override
    public boolean forward()
    {
        return forward;
    }

    @Override
    public boolean back()
    {
        return back;
    }

    @Override
    public boolean left()
    {
        return left;
    }

    @Override
    public boolean right()
    {
        return right;
    }

    @Override
    public boolean fire()
    {
        boolean f = fire;
        fire = false;
        return f;
    }
}
