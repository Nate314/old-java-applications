package tankgame;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
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
        MATCH_OVER,
        /** The player lays walls on the grid for a battlefield of their own (B); a fire key fights on it. */
        BUILD
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
    /** How far the field shakes, in px: set by hits and blasts, dying away each tick. */
    private double           shake;
    /** Counts the tanks destroyed so far, so the panel can play each one's blast in slow motion. */
    private int              blasts;
    private List<Arena.Layout> layouts          = Arena.LAYOUTS;
    /** The player's battlefield on the grid; with any wall on it, every round is fought there. */
    private boolean[][]      field              = new boolean[Arena.GRID][Arena.GRID];
    private boolean          refused;
    /** Whether this window is too small for the player's battlefield to leave the tanks a way through. */
    private boolean          fieldBlocked;
    private Arena            arena              = Arena.EMPTY;
    /** The round, field width and height the arena was placed for, so it is placed again when one changes. */
    private int              arenaRound         = -1;
    private int              arenaWidth;
    private int              arenaHeight;
    /** The battle's sounds (V turns them off); silent until the window gives it the synthesizer's. */
    private Sounds           sounds             = Sounds.silent();
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

    /** The player's battlefield, as read from the record (see {@link Arena#cells}). */
    public void setField(boolean[][] cells)
    {
        field = cells;
        useField();
    }

    /** Rounds are fought on the player's battlefield if it has any wall, else on the four layouts in turn. */
    private void useField()
    {
        setLayouts(hasField() || phase == Phase.BUILD ? Collections.singletonList(Arena.custom(field)) : Arena.LAYOUTS);
    }

    public boolean hasField()
    {
        for (boolean[] row : field)
            for (boolean wall : row)
                if (wall) return true;
        return false;
    }

    /** B: from the title or match-over screen, opens the builder with both tanks on their starting spots; again, back. */
    public void toggleBuild()
    {
        if (phase == Phase.BUILD) enter(Phase.TITLE);
        else if (phase == Phase.TITLE || phase == Phase.MATCH_OVER)
        {
            red.reset();
            green.reset();
            shells.clear();
            sparks.clear();
            supply = null;
            round = 0;
            refused = false;
            enter(Phase.BUILD);
        }
        useField();
    }

    /** Leaves the builder and starts a match (on the battlefield just built, if it has any wall). */
    public void fightHere(boolean againstComputer)
    {
        if (phase != Phase.BUILD) return;
        toggleBuild();
        startMatch(againstComputer);
    }

    /** Whether a cell of the grid holds a wall. */
    public boolean cell(int col, int row)
    {
        return field[row][col];
    }

    /**
     * Lays or lifts a wall in a cell while building, saved at once. Cells on or beside a tank stay empty, and so does
     * a cell whose wall would leave the tanks no way to reach each other (see {@link #refused}).
     */
    public void setCell(int col, int row, boolean wall, int width, int height)
    {
        if (phase != Phase.BUILD || col < 0 || row < 0 || col >= Arena.GRID || row >= Arena.GRID
                        || field[row][col] == wall || wall && !Arena.cellFree(col, row, width, height, red, green)) return;
        field[row][col] = wall;
        if (wall && !Arena.build(Arena.custom(field), width, height, red, green).connected(width, height, red, green))
        {
            field[row][col] = false;
            refused = true;
        }
        else saveField();
    }

    /** A new press in the builder: forgets the last refusal, which otherwise shows for the rest of the stroke. */
    public void newStroke()
    {
        refused = false;
    }

    /** Whether the last wall laid was left out because it would shut the tanks apart (a match could never end). */
    public boolean refused()
    {
        return refused;
    }

    /** Lifts every wall of the player's battlefield, so rounds go back to the four layouts. */
    public void clearField()
    {
        field = new boolean[Arena.GRID][Arena.GRID];
        refused = false;
        saveField();
    }

    private void saveField()
    {
        useField();
        if (record != null) record.setField(hasField() ? Arena.text(field) : "");
    }

    /** Places the round's battlefield; the title shows the first round's. */
    private void placeArena(int width, int height)
    {
        int r = Math.max(round, 1);
        if (r == arenaRound && width == arenaWidth && height == arenaHeight) return;
        arena = Arena.build(layouts.get((r - 1) % layouts.size()), width, height, red, green);
        // A battlefield built in a wider window can shut the tanks apart in a narrower one: then fight on the layouts.
        fieldBlocked = phase != Phase.BUILD && hasField() && !arena.connected(width, height, red, green);
        if (fieldBlocked) arena = Arena.build(Arena.LAYOUTS.get((r - 1) % Arena.LAYOUTS.size()), width, height, red, green);
        arenaRound = r;
        arenaWidth = width;
        arenaHeight = height;
    }

    public void tick(int width, int height)
    {
        scorches.clear();
        shake = shake < 0.3 ? 0 : shake * 0.8;
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
            case BUILD:
                boolean redGo = red.controls().fire();
                boolean greenGo = greenKeys.fire();
                if (redGo || greenGo) fightHere(redGo);
                break;
            case ROUND_START:
                // Presses while waiting are dropped, so nobody fires the instant the round starts.
                red.controls().fire();
                green.controls().fire();
                if (phaseTicks >= ROUND_START_TICKS)
                {
                    enter(Phase.FIGHT);
                    sounds.fight();
                }
                break;
            case FIGHT:
                fight(width, height);
                break;
            case ROUND_OVER:
                shells.removeIf(shell -> !fly(shell, width, height));
                if (phaseTicks >= ROUND_OVER_TICKS) finishRound();
                break;
        }
        boolean fighting = phase == Phase.FIGHT;
        sounds.engines(fighting && moving(red), red.turboTicks() > 0, fighting && moving(green), green.turboTicks() > 0);
    }

    private static boolean moving(Tank tank)
    {
        return tank.velocityX() != 0 || tank.velocityY() != 0;
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
            sounds.fire();
            Spark.burst(sparks, shell.x(), shell.y(), 8, 2.5, new Color(255, 160, 60));
        }
        // Turbo: a trail of yellow sparks from the back of a moving tank.
        if (tank.turboTicks() > 0 && moving(tank))
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
        boolean falling = !supply.landed();
        supply.tick();
        if (falling && supply.landed()) sounds.landed();
        for (Tank tank : new Tank[] { red, green })
        {
            if (!supply.takenBy(tank)) continue;
            tank.give(supply.power());
            sounds.take();
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
        sounds.crateShot();
        Spark.burst(sparks, supply.x(), supply.y(), 20, 3.5, new Color(170, 120, 66));
        Spark.blast(sparks, supply.x(), supply.y(), 40);
        shake(4);
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
        if (shell.bounced())
        {
            Spark.burst(sparks, shell.x(), shell.y(), 5, 2, CHIPS);
            sounds.ricochet();
        }
        if (shell.burst())
        {
            sounds.burst();
            Spark.burst(sparks, shell.x(), shell.y(), 14, 3, CHIPS);
            Spark.blast(sparks, shell.x(), shell.y(), 22);
            scorches.add(new double[] { shell.x(), shell.y(), 9 });
            shake(2);
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
        if (damaged) Spark.blast(sparks, shell.x(), shell.y(), 34);
        // The last hit's sound is the destroyed tank's boom (explodeIfDead).
        if (damaged && tank.isAlive()) sounds.hit();
        else if (popped) sounds.shieldPop();
        else if (!damaged) sounds.ricochet();
        shake(damaged ? 7 : 3);
        return true;
    }

    private void explodeIfDead(Tank tank)
    {
        if (tank.isAlive()) return;
        Spark.burst(sparks, tank.x(), tank.y(), 70, 9, tank.color());
        Spark.blast(sparks, tank.x(), tank.y(), 150);
        Spark.scraps(sparks, tank.x(), tank.y(), 14, tank.color());
        shake(12);
        sounds.destroyed();
        blasts++;
        scorches.add(new double[] { tank.x(), tank.y(), 62 });
    }

    private void shake(double px)
    {
        shake = Math.max(shake, px);
    }

    /** How far the field shakes this tick, in px. */
    public double shake()
    {
        return shake;
    }

    /** How many tanks have been destroyed since the game started. */
    public int blasts()
    {
        return blasts;
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
        sounds.matchOver(vsComputer && !watching && winner != red);
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

    /** The sounds the battle plays (the window's on the synthesizer, a self-check's silent ones). */
    void setSounds(Sounds sounds)
    {
        this.sounds = sounds;
    }

    /** V: the sound on or off, saved with the record. */
    public void toggleSound()
    {
        sounds.setOn(!sounds.isOn());
        if (record != null) record.setSound(sounds.isOn());
    }

    public boolean sound()
    {
        return sounds.isOn();
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

    /** Whether the player's battlefield was set aside for this round because it shuts the tanks apart at this size. */
    public boolean fieldBlocked()
    {
        return fieldBlocked;
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
