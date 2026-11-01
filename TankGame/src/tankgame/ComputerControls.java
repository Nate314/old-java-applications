package tankgame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Drives a tank against the player. It turns toward where the player will be when a shell gets there, keeps a middle
 * distance, backs or drives out of the way of shells it notices, and fires when its aim is close. It has the same
 * speed, turning rate and reload as a player, and a human-sized reaction time, aim error and trigger delay, and it
 * misses some shells, so it can be beaten.
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
    private final Random         random;
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

    public ComputerControls(Random random)
    {
        this.random = random;
    }

    /** Decides this tick's moves for {@code self} against {@code enemy}. */
    public void plan(Tank self, Tank enemy, List<Shell> shells, int width, int height)
    {
        if (sinceLook++ >= REACTION_TICKS)
        {
            sinceLook = 0;
            // Lead the target: where the enemy will be when a shell fired now reaches it.
            double ticks = self.distanceTo(enemy) / Shell.SPEED;
            aimX = enemy.x() + enemy.velocityX() * ticks;
            aimY = enemy.y() + enemy.velocityY() * ticks;
            aimError = (random.nextDouble() * 2 - 1) * AIM_ERROR;
        }
        double wanted = Math.toDegrees(Math.atan2(aimX - self.x(), self.y() - aimY)) + aimError;
        double turn = normalize(wanted - self.rotation());
        left = turn < -Tank.SPEED / 2.0;
        right = turn > Tank.SPEED / 2.0;
        fire = Math.abs(turn) < FIRE_CONE && self.reload() == 0 && random.nextDouble() < TRIGGER_CHANCE;

        double distance = self.distanceTo(enemy);
        forward = distance > FAR && Math.abs(turn) < 60;
        back = distance < NEAR;
        noticed.keySet().retainAll(shells);
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
