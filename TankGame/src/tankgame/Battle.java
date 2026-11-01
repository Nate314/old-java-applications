package tankgame;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules of a match, one tick at a time: three hits destroy a tank, destroying the other tank wins the round, and
 * the first to win three rounds wins the match. Red's fire key starts a match against the computer, green's fire key
 * a match for two players. Knows nothing about Swing, so the self-check can play whole matches.
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
    private final Tank       red;
    private final Tank       green;
    private final Controls   greenKeys;
    private final ComputerControls computer;
    private final Record     record;
    private final List<Shell> shells            = new ArrayList<>();
    private final List<Spark> sparks            = new ArrayList<>();
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
        this.computer = computer;
        this.record = record;
    }

    /** Starts a new match, against the computer or for two players. */
    public void startMatch(boolean againstComputer)
    {
        vsComputer = againstComputer;
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

    public void tick(int width, int height)
    {
        sparks.removeIf(spark -> !spark.move());
        red.fadeFlash();
        green.fadeFlash();
        phaseTicks++;
        switch (phase)
        {
            case TITLE:
            case MATCH_OVER:
                boolean redFire = red.controls().fire();
                boolean greenFire = greenKeys.fire();
                if (phase == Phase.MATCH_OVER && phaseTicks < MATCH_OVER_TICKS) break;
                if (redFire) startMatch(true);
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
                shells.removeIf(shell -> !shell.move(width, height));
                if (phaseTicks >= ROUND_OVER_TICKS) finishRound();
                break;
        }
    }

    private void fight(int width, int height)
    {
        if (vsComputer) computer.plan(green, red, shells, width, height);
        red.update(width, height, shells, green);
        green.update(width, height, shells, red);
        shells.removeIf(shell -> !shell.move(width, height) || strike(shell, red, width, height)
                        || strike(shell, green, width, height));
        if (red.isAlive() && green.isAlive()) return;
        explodeIfDead(red);
        explodeIfDead(green);
        winner = red.isAlive() ? red : green.isAlive() ? green : null;
        if (winner == red) redWins++;
        if (winner == green) greenWins++;
        enter(Phase.ROUND_OVER);
    }

    /** Applies a hit if the shell struck the tank; returns whether the shell is used up. */
    private boolean strike(Shell shell, Tank tank, int width, int height)
    {
        if (!shell.hits(tank)) return false;
        tank.hit(shell, width, height);
        Spark.burst(sparks, shell.x(), shell.y(), 12, 4, tank.color());
        return true;
    }

    private void explodeIfDead(Tank tank)
    {
        if (!tank.isAlive()) Spark.burst(sparks, tank.x(), tank.y(), 70, 9, tank.color());
    }

    private void finishRound()
    {
        if (redWins < ROUNDS_TO_WIN && greenWins < ROUNDS_TO_WIN)
        {
            nextRound();
            return;
        }
        winner = redWins > greenWins ? red : green;
        if (vsComputer && record != null) record.add(winner == red);
        enter(Phase.MATCH_OVER);
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
}
