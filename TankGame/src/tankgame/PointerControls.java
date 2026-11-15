package tankgame;

/**
 * Red's keys, plus the mouse or a finger on the field. Hold the pointer down and the tank turns and drives to it,
 * following it as it moves; a quick tap turns the tank to face the tapped spot and fires there once it does (a tap on
 * the tank itself fires straight ahead). On the title card or the match-over screen a tap acts as the fire key.
 */
public class PointerControls implements Controls
{
    /** A press shorter than this (about a quarter of a second) that barely moves is a tap. */
    static final int     TAP_TICKS  = 15;
    static final double  TAP_SLOP   = 12;
    /** It drives only while facing within this many degrees of the pointer, so it turns on the spot first. */
    static final double  DRIVE_CONE = 50;
    /** It stops this close to the pointer. */
    static final double  ARRIVED    = Tank.SIZE * 0.6;
    private final Controls keys;
    private Tank         tank;
    private boolean      held;
    private int          heldTicks;
    private double       pressX;
    private double       pressY;
    /** Where the held pointer is, or null. */
    private double[]     target;
    /** Where a tap asked the tank to fire, until it does; null when nothing is waiting. */
    private double[]     aim;
    /** A tap on the title card or the match-over screen, read as one press of fire. */
    private boolean      start;

    public PointerControls(Controls keys)
    {
        this.keys = keys;
    }

    /** The tank these controls drive, needed to steer toward the pointer. */
    public void drive(Tank tank)
    {
        this.tank = tank;
    }

    public void press(double x, double y)
    {
        held = true;
        heldTicks = 0;
        pressX = x;
        pressY = y;
        target = new double[] { x, y };
        aim = null;
    }

    public void drag(double x, double y)
    {
        if (held) target = new double[] { x, y };
    }

    public void release(double x, double y)
    {
        if (!held) return;
        held = false;
        target = null;
        if (heldTicks < TAP_TICKS && Math.hypot(x - pressX, y - pressY) <= TAP_SLOP)
        {
            // A tap on the tank itself fires along the barrel.
            aim = tank != null && tank.contains(x, y) ? new double[] { Double.NaN, Double.NaN } : new double[] { x, y };
        }
    }

    /** A tap that starts a match: fire() reads it once. */
    public void tapToStart()
    {
        start = true;
    }

    /** Where the tank is heading for (driving to it while held, or about to fire at it), or null; for the marker. */
    public double[] goal()
    {
        if (target != null) return target;
        return aim != null && !Double.isNaN(aim[0]) ? aim : null;
    }

    public boolean holding()
    {
        return held;
    }

    /** A hold, not the start of a tap: long enough or moved far enough to drive. */
    private boolean driving()
    {
        return held && (heldTicks >= TAP_TICKS || Math.hypot(target[0] - pressX, target[1] - pressY) > TAP_SLOP);
    }

    /** Degrees the tank must still turn clockwise to face the pointer (negative: counterclockwise); 0 with none. */
    private double turn()
    {
        double[] point = target != null ? target : aim;
        if (tank == null || point == null || Double.isNaN(point[0])) return 0;
        if (Math.hypot(point[0] - tank.x(), point[1] - tank.y()) < ARRIVED) return 0;
        double wanted = Math.toDegrees(Math.atan2(point[0] - tank.x(), tank.y() - point[1]));
        double d = Math.floorMod((int) Math.round(wanted - tank.rotation()), 360);
        return d > 180 ? d - 360 : d;
    }

    @Override
    public boolean forward()
    {
        if (keys.forward()) return true;
        return driving() && Math.abs(turn()) < DRIVE_CONE
                        && Math.hypot(target[0] - tank.x(), target[1] - tank.y()) > ARRIVED;
    }

    @Override
    public boolean back()
    {
        return keys.back();
    }

    @Override
    public boolean left()
    {
        return keys.left() || turn() < -Tank.SPEED / 2.0;
    }

    @Override
    public boolean right()
    {
        return keys.right() || turn() > Tank.SPEED / 2.0;
    }

    /** Also the pointer's clock: the battle reads fire once every tick, in every phase but between rounds. */
    @Override
    public boolean fire()
    {
        if (held) heldTicks++;
        boolean pressed = keys.fire();
        if (start)
        {
            start = false;
            return true;
        }
        if (pressed) return true;
        if (aim != null && tank != null && tank.reload() == 0 && Math.abs(turn()) <= Tank.SPEED / 2.0)
        {
            aim = null;
            return true;
        }
        return false;
    }
}
