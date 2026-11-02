package tankgame;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The rules of a match, one tick at a time: three hits destroy a tank, destroying the other tank wins the round, and
 * the first to win three rounds wins the match. Red's fire key starts a match against the computer, green's fire key
 * a match for two players. Each round is fought on the next battlefield of walls (see {@link Arena}). Knows nothing
 * about Swing, so the self-check can play whole matches.
 */
public class Battle
{
    public enum Phase
    {
        /** Waiting for a fire key to pick the opponent. */
        TITLE,
        /** The round's name is shown and the tanks wait. */
        ROUND_START,
        FIGHT,
        /** A tank was destroyed; the result is shown before the next round. */
        ROUND_OVER,
        /** Someone won three rounds; a fire key starts the next match. */
        MATCH_OVER
    }

    public static final int  ROUNDS_TO_WIN      = 3;
    public static final int  ROUND_START_TICKS  = 75;
    public static final int  ROUND_OVER_TICKS   = 110;
    /** A pause before the match-over screen takes fire presses, so a last shot does not skip it. */
    public static final int  MATCH_OVER_TICKS   = 90;
    /** Watch: the computer starts a match after the title (or the last match's result) has shown this long. */
    public static final int  WATCH_START_TICKS  = 120;
    private final Tank       red;
    private final Tank       green;
    private final Controls   greenKeys;
    private final Controls   redKeys;
    /** Watch: the computer drives red too. */
    private final ComputerControls redComputer = new ComputerControls(new Random());
    private boolean          watching;
    /** The computer drove red at some point in this match, so it does not count in the record. */
    private boolean          watchedMatch;
    private final ComputerControls computer;
    private final Record     record;
    private final List<Shell> shells            = new ArrayList<>();
    private final List<Spark> sparks            = new ArrayList<>();
    /** Marks this tick's blasts left on the ground: x, y and radius. */
    private final List<double[]> scorches       = new ArrayList<>();
    private static final Color CHIPS            = new Color(170, 165, 155);
    private List<Arena.Layout> layouts          = Arena.LAYOUTS;
    private Arena            arena              = Arena.EMPTY;
    /** The round, field width and height the arena was placed for, so it is placed again when one changes. */
    private int              arenaRound         = -1;
    private int              arenaWidth;
    private int              arenaHeight;
    private Phase            phase              = Phase.TITLE;
    private int              phaseTicks;
    private boolean          vsComputer;
    private int              redWins;
    private int              greenWins;
    private int              round;
    /** The winner of the round or match just finished; null for a round where both tanks fell. */
    private Tank             winner;

    public Battle(Tank red, Tank green, ComputerControls computer, Record record)
    {
        this.red = red;
        this.green = green;
        this.greenKeys = green.controls();
        this.redKeys = red.controls();
        this.computer = computer;
        this.record = record;
    }

    /** Starts a new match, against the computer or for two players. */
    public void startMatch(boolean againstComputer)
    {
        vsComputer = againstComputer;
        watchedMatch = watching;
        green.setControls(againstComputer ? computer : greenKeys);
        green.setName(againstComputer ? "Computer" : "Green");
        redWins = greenWins = 0;
        round = 0;
        nextRound();
    }

    private void nextRound()
    {
        round++;
        red.reset();
        green.reset();
        shells.clear();
        winner = null;
        enter(Phase.ROUND_START);
    }

    private void enter(Phase next)
    {
        phase = next;
        phaseTicks = 0;
    }

    /** The battlefields the rounds take turns on (a test can play on an open field). */
    public void setLayouts(List<Arena.Layout> layouts)
    {
        this.layouts = layouts;
        arenaRound = -1;
    }

    /** Places the round's battlefield; the title shows the first round's. */
    private void placeArena(int width, int height)
    {
        int r = Math.max(round, 1);
        if (r == arenaRound && width == arenaWidth && height == arenaHeight) return;
        arena = Arena.build(layouts.get((r - 1) % layouts.size()), width, height, red, green);
        arenaRound = r;
        arenaWidth = width;
        arenaHeight = height;
    }

    public void tick(int width, int height)
    {
        scorches.clear();
        placeArena(width, height);
        sparks.removeIf(spark -> !spark.move());
        red.fadeFlash();
        green.fadeFlash();
        if (phase != Phase.TITLE)
        {
            smoke(red);
            smoke(green);
        }
        phaseTicks++;
        switch (phase)
        {
            case TITLE:
            case MATCH_OVER:
                boolean redFire = red.controls().fire();
                boolean greenFire = greenKeys.fire();
                if (phase == Phase.MATCH_OVER && phaseTicks < MATCH_OVER_TICKS) break;
                if (redFire || watching && phaseTicks >= WATCH_START_TICKS) startMatch(true);
                else if (greenFire) startMatch(false);
                break;
            case ROUND_START:
                // Presses while waiting are dropped, so nobody fires the instant the round starts.
                red.controls().fire();
                green.controls().fire();
                if (phaseTicks >= ROUND_START_TICKS) enter(Phase.FIGHT);
                break;
            case FIGHT:
                fight(width, height);
                break;
            case ROUND_OVER:
                shells.removeIf(shell -> !fly(shell, width, height));
                if (phaseTicks >= ROUND_OVER_TICKS) finishRound();
                break;
        }
    }

    private void fight(int width, int height)
    {
        if (vsComputer) computer.plan(green, red, shells, width, height, arena);
        if (watching) redComputer.plan(red, green, shells, width, height, arena);
        drive(red, green, width, height);
        drive(green, red, width, height);
        shells.removeIf(shell -> !fly(shell, width, height) || strike(shell, red, width, height)
                        || strike(shell, green, width, height));
        if (red.isAlive() && green.isAlive()) return;
        explodeIfDead(red);
        explodeIfDead(green);
        winner = red.isAlive() ? red : green.isAlive() ? green : null;
        if (winner == red) redWins++;
        if (winner == green) greenWins++;
        enter(Phase.ROUND_OVER);
    }

    /** Moves a tank one tick, with a muzzle flash when it fires. */
    private void drive(Tank tank, Tank other, int width, int height)
    {
        int fired = shells.size();
        tank.update(width, height, shells, other, arena);
        if (shells.size() > fired)
        {
            Shell shell = shells.get(fired);
            Spark.burst(sparks, shell.x(), shell.y(), 8, 2.5, new Color(255, 160, 60));
        }
    }

    /** Moves a shell one tick, chipping a wall it bounces off; returns false once it is gone. */
    private boolean fly(Shell shell, int width, int height)
    {
        boolean flying = shell.move(width, height, arena);
        if (shell.bounced()) Spark.burst(sparks, shell.x(), shell.y(), 5, 2, CHIPS);
        if (shell.burst())
        {
            Spark.burst(sparks, shell.x(), shell.y(), 14, 3, CHIPS);
            scorches.add(new double[] { shell.x(), shell.y(), 9 });
        }
        return flying;
    }

    /** Applies a hit if the shell struck the tank; returns whether the shell is used up. */
    private boolean strike(Shell shell, Tank tank, int width, int height)
    {
        if (!shell.hits(tank)) return false;
        tank.hit(shell, width, height, arena);
        Spark.burst(sparks, shell.x(), shell.y(), 12, 4, tank.color());
        return true;
    }

    private void explodeIfDead(Tank tank)
    {
        if (tank.isAlive()) return;
        Spark.burst(sparks, tank.x(), tank.y(), 70, 9, tank.color());
        scorches.add(new double[] { tank.x(), tank.y(), 62 });
    }

    /** A damaged tank smokes, more with each hit, and a wreck smokes most. */
    private void smoke(Tank tank)
    {
        double chance = new double[] { 0.4, 0.22, 0.08, 0 }[tank.health()];
        if (Math.random() < chance) Spark.smoke(sparks, tank.x(), tank.y());
    }

    private void finishRound()
    {
        if (redWins < ROUNDS_TO_WIN && greenWins < ROUNDS_TO_WIN)
        {
            nextRound();
            return;
        }
        winner = redWins > greenWins ? red : green;
        if (vsComputer && !watchedMatch && record != null) record.add(winner == red);
        enter(Phase.MATCH_OVER);
    }

    /**
     * Watch (C): the computer drives red as well, starting matches against itself, until C again. Turned off in a
     * match, red is the player's again; that match does not count in the record.
     */
    public void toggleWatch()
    {
        watching = !watching;
        watchedMatch |= watching;
        red.setControls(watching ? redComputer : redKeys);
        // The computer's first match starts a moment after C, not at once.
        if (phase == Phase.TITLE || phase == Phase.MATCH_OVER) phaseTicks = Math.min(phaseTicks, MATCH_OVER_TICKS);
    }

    public boolean watching()
    {
        return watching;
    }

    public Phase phase()
    {
        return phase;
    }

    public int phaseTicks()
    {
        return phaseTicks;
    }

    public boolean vsComputer()
    {
        return vsComputer;
    }

    public int redWins()
    {
        return redWins;
    }

    public int greenWins()
    {
        return greenWins;
    }

    public int round()
    {
        return round;
    }

    public Tank winner()
    {
        return winner;
    }

    public Tank red()
    {
        return red;
    }

    public Tank green()
    {
        return green;
    }

    public Record record()
    {
        return record;
    }

    public List<Shell> shells()
    {
        return shells;
    }

    public List<Spark> sparks()
    {
        return sparks;
    }

    /** The battlefield of this round (or of the first round, on the title). */
    public Arena arena()
    {
        return arena;
    }

    /** Blast marks this tick left on the ground: x, y and radius each. */
    public List<double[]> scorches()
    {
        return scorches;
    }
}
