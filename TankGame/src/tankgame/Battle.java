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
 * <p>
 * With supply drops on, a crate floats down by parachute a few seconds into each round and again a while after each one
 * is taken or shot: the first tank to drive over it gets its power (see {@link Supply.Power}).
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
    /** Supply drops: the first crate comes 2.5 s into a round, the next 5 to 9 s after one is taken or shot. */
    public static final int  FIRST_DROP_TICKS   = 150;
    public static final int  DROP_GAP_TICKS     = 300;
    public static final int  DROP_GAP_RANDOM    = 240;
    /** How long the name of a power just taken shows over the tank. */
    public static final int  NOTICE_TICKS       = 90;
    /** Crates land at least this far below the top, clear of the score bar. */
    private static final int DROP_TOP           = 80;
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
    private boolean          supplies;
    private Random           dropRandom         = new Random();
    /** The crate on the field (falling or landed), or null. */
    private Supply           supply;
    private int              supplyClock;
    private int              nextDrop           = FIRST_DROP_TICKS;
    /** The last power taken, by which tank, and ticks since, for the line that rises over it. */
    private Supply.Power     noticePower;
    private Tank             noticeTank;
    private int              noticeTicks;

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
        supply = null;
        supplyClock = 0;
        nextDrop = FIRST_DROP_TICKS;
        noticePower = null;
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
        noticeTicks++;
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
        if (vsComputer) computer.plan(green, red, shells, width, height, arena, supply);
        if (watching) redComputer.plan(red, green, shells, width, height, arena, supply);
        drive(red, green, width, height);
        drive(green, red, width, height);
        shells.removeIf(shell -> !fly(shell, width, height) || strike(shell, red, width, height)
                        || strike(shell, green, width, height) || shootCrate(shell));
        supplies(width, height);
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
        // Turbo: a trail of yellow sparks from the back of a moving tank.
        if (tank.turboTicks() > 0 && (tank.velocityX() != 0 || tank.velocityY() != 0))
        {
            double heading = Math.toRadians(tank.rotation());
            Spark.burst(sparks, tank.x() - Math.sin(heading) * Tank.SIZE, tank.y() + Math.cos(heading) * Tank.SIZE, 1,
                            1.5, Supply.Power.TURBO.color);
        }
    }

    /** Counts down to the next crate and drops it, lets it fall, and gives its power to the first tank over it. */
    private void supplies(int width, int height)
    {
        if (!supplies) return;
        if (supply == null)
        {
            if (++supplyClock >= nextDrop) supply = drop(width, height);
            return;
        }
        supply.tick();
        for (Tank tank : new Tank[] { red, green })
        {
            if (!supply.takenBy(tank)) continue;
            tank.give(supply.power());
            Spark.burst(sparks, supply.x(), supply.y(), 24, 3.5, supply.power().color);
            noticePower = supply.power();
            noticeTank = tank;
            noticeTicks = 0;
            crateGone();
            return;
        }
    }

    /** A shell that hits a landed crate blows it up, and is used up. */
    private boolean shootCrate(Shell shell)
    {
        if (supply == null || !supply.hitBy(shell)) return false;
        Spark.burst(sparks, supply.x(), supply.y(), 20, 3.5, new Color(170, 120, 66));
        scorches.add(new double[] { supply.x(), supply.y(), 14 });
        crateGone();
        return true;
    }

    private void crateGone()
    {
        supply = null;
        supplyClock = 0;
        nextDrop = DROP_GAP_TICKS + dropRandom.nextInt(DROP_GAP_RANDOM);
    }

    /**
     * A crate of a random power at a random spot clear of the walls and the score bar and not right next to a tank;
     * null (try again next tick) if none of the spots tried is free.
     */
    private Supply drop(int width, int height)
    {
        Supply.Power[] powers = Supply.Power.values();
        Supply.Power power = powers[dropRandom.nextInt(powers.length)];
        int margin = Supply.HALF + 10;
        for (int i = 0; i < 40; i++)
        {
            double x = margin + dropRandom.nextDouble() * Math.max(1, width - 2 * margin);
            double y = DROP_TOP + dropRandom.nextDouble() * Math.max(1, height - DROP_TOP - margin);
            if (arena.blocks(x, y, Supply.HALF + 6)) continue;
            if (Math.hypot(red.x() - x, red.y() - y) < Tank.SIZE + 60) continue;
            if (Math.hypot(green.x() - x, green.y() - y) < Tank.SIZE + 60) continue;
            return new Supply(power, x, y);
        }
        return null;
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
        boolean shielded = tank.shielded();
        boolean damaged = tank.hit(shell, width, height, arena);
        boolean popped = shielded && !tank.shielded();
        Spark.burst(sparks, shell.x(), shell.y(), popped ? 22 : 12, 4,
                        damaged ? tank.color() : popped ? Supply.Power.SHIELD.color : CHIPS);
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

    /** Supply drops (X): on or off, saved with the record. Turning them off takes away the crate on the field. */
    public void setSupplies(boolean on)
    {
        supplies = on;
        if (!on) supply = null;
    }

    public void toggleSupplies()
    {
        setSupplies(!supplies);
        if (record != null) record.setSupplies(supplies);
    }

    public boolean supplies()
    {
        return supplies;
    }

    /** Where crates fall and what they hold (a test can fix it). */
    void setDropRandom(Random random)
    {
        dropRandom = random;
    }

    /** The crate on the field, falling or landed, or null. */
    public Supply supply()
    {
        return supply;
    }

    /** The last power a tank took this round, or null. */
    public Supply.Power noticePower()
    {
        return noticePower;
    }

    public Tank noticeTank()
    {
        return noticeTank;
    }

    public int noticeTicks()
    {
        return noticeTicks;
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
