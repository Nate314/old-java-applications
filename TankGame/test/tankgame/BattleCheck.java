package tankgame;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Assert-based self-check of the tank rules and the computer. Run with {@code java -ea}. */
public final class BattleCheck
{
    private static final int W = 484;
    private static final int H = 461;

    /** Controls a test sets by hand. */
    static final class Script implements Controls
    {
        boolean forward, back, left, right, fire;

        public boolean forward() { return forward; }
        public boolean back() { return back; }
        public boolean left() { return left; }
        public boolean right() { return right; }
        public boolean fire() { boolean f = fire; fire = false; return f; }
    }

    public static void main(String[] args) throws Exception
    {
        boolean assertions = false;
        assert assertions = true;
        if (!assertions) throw new IllegalStateException("run with java -ea");
        hitBox();
        shellsHitTheOtherTankOnly();
        shellsBounceOnce();
        tanksAreSolid();
        reloadLimitsFire();
        matchRulesAndRecord();
        computerDestroysASittingTarget();
        computerAgainstComputerFinishes();
        System.out.println("BattleCheck: all checks passed");
    }

    private static Tank tank(Controls c, int x, int y, int rotation)
    {
        return new Tank("T", Color.RED, c, x, y, rotation);
    }

    private static void hitBox()
    {
        Tank t = tank(new Script(), 200, 200, 0);
        assert t.contains(200, 200) && t.contains(249, 249) && !t.contains(252, 200);
        Tank turned = tank(new Script(), 200, 200, 45);
        // A 45 degree turn puts a corner 70 px out along the axis and pulls the old corner in.
        assert turned.contains(268, 200) && !turned.contains(248, 248);
    }

    private static void shellsHitTheOtherTankOnly()
    {
        Tank a = tank(new Script(), 100, 200, 90);
        Tank b = tank(new Script(), 300, 200, 270);
        Shell own = new Shell(a, 100, 200, 1, 0);
        assert !own.hits(a) && !new Shell(a, 300, 200, 1, 0).hits(a);
        assert new Shell(a, 300, 200, 1, 0).hits(b);
        List<Shell> shells = new ArrayList<>();
        a.fireTank(shells);
        Shell s = shells.get(0);
        assert Math.abs(s.x() - 175) < 1e-9 && Math.abs(s.y() - 200) < 1e-9 && s.dx() > 0.99;
        b.hit(s, W, H);
        assert b.health() == Tank.MAX_HEALTH - 1 && b.x() > 300;
        b.hit(s, W, H);
        b.hit(s, W, H);
        assert !b.isAlive() && !s.hits(b) && b.health() == 0;
    }

    private static void shellsBounceOnce()
    {
        Tank a = tank(new Script(), 100, 200, 90);
        Shell s = new Shell(a, 470, 200, 1, 0);
        assert s.move(W, H);                     // 480: still inside
        assert s.move(W, H) && s.dx() < 0;       // 490 bounces back to 478
        assert Math.abs(s.x() - 478) < 1e-9;
        int ticks = 0;
        while (s.move(W, H)) ticks++;
        assert ticks > 40 && s.x() < 0;          // flew out the far side after one bounce
    }

    private static void tanksAreSolid()
    {
        Script drive = new Script();
        Tank a = tank(drive, 100, 200, 90);
        Tank b = tank(new Script(), 260, 200, 270);
        drive.forward = true;
        List<Shell> shells = new ArrayList<>();
        for (int i = 0; i < 100; i++) a.update(W, H, shells, b);
        assert a.distanceTo(b) >= 2 * Tank.SIZE - Tank.SPEED && a.x() < 165 : a.x();
        drive.forward = false;
        drive.back = true;
        a.update(W, H, shells, b);
        assert a.x() < 156;                      // backing away is allowed
    }

    private static void reloadLimitsFire()
    {
        Script s = new Script();
        Tank a = tank(s, 100, 200, 90);
        Tank b = tank(new Script(), 400, 200, 270);
        List<Shell> shells = new ArrayList<>();
        for (int i = 0; i < Tank.RELOAD_TICKS * 2; i++)
        {
            s.fire = true;
            a.update(W, H, shells, b);
        }
        assert shells.size() == 2 : shells.size();
    }

    private static void matchRulesAndRecord() throws Exception
    {
        File file = File.createTempFile("tankgame-record", ".txt");
        file.delete();
        Script redKeys = new Script();
        Script greenKeys = new Script();
        Tank red = new Tank("Red", Color.RED, redKeys, 100, 250, 90);
        Tank green = new Tank("Green", Color.GREEN, greenKeys, 380, 250, 270);
        Battle battle = new Battle(red, green, new ComputerControls(new Random(1)), Record.load(file));
        battle.tick(W, H);
        assert battle.phase() == Battle.Phase.TITLE;
        greenKeys.fire = true;                   // green's fire picks two players
        battle.tick(W, H);
        assert battle.phase() == Battle.Phase.ROUND_START && !battle.vsComputer() && green.controls() == greenKeys;
        greenKeys.fire = true;                   // dropped while the round starts
        for (int i = 0; i < Battle.ROUND_START_TICKS; i++) battle.tick(W, H);
        assert battle.phase() == Battle.Phase.FIGHT && battle.shells().isEmpty();
        // Red faces green across the field: three shots end the round.
        for (int round = 1; round <= Battle.ROUNDS_TO_WIN; round++)
        {
            while (battle.phase() != Battle.Phase.ROUND_OVER)
            {
                redKeys.fire = true;
                battle.tick(W, H);
            }
            assert battle.winner() == red && battle.redWins() == round && green.health() == 0;
            while (battle.phase() == Battle.Phase.ROUND_OVER) battle.tick(W, H);
            if (round < Battle.ROUNDS_TO_WIN)
            {
                assert battle.phase() == Battle.Phase.ROUND_START && battle.round() == round + 1;
                assert green.health() == Tank.MAX_HEALTH && red.x() == 100;
                for (int i = 0; i < Battle.ROUND_START_TICKS; i++) battle.tick(W, H);
            }
        }
        assert battle.phase() == Battle.Phase.MATCH_OVER && battle.winner() == red;
        assert Record.load(file).wins() + Record.load(file).losses() == 0;   // two-player matches are not recorded
        redKeys.fire = true;                     // too early: the match-over screen holds for a moment
        battle.tick(W, H);
        assert battle.phase() == Battle.Phase.MATCH_OVER;
        for (int i = 0; i < Battle.MATCH_OVER_TICKS; i++) battle.tick(W, H);
        redKeys.fire = true;                     // red's fire picks the computer
        battle.tick(W, H);
        assert battle.vsComputer() && green.controls() instanceof ComputerControls && green.name().equals("Computer");
        assert battle.redWins() == 0 && battle.round() == 1;
        int ticks = 0;
        while (battle.phase() != Battle.Phase.MATCH_OVER && ticks++ < 60 * 60 * 10) battle.tick(W, H);
        assert battle.phase() == Battle.Phase.MATCH_OVER : "the computer finished a match against a red tank that sits still";
        assert battle.winner() == green;
        Record saved = Record.load(file);
        assert saved.wins() == 0 && saved.losses() == 1 && saved.toString().equals("0 wins, 1 loss") : saved;
        file.delete();
    }

    private static void computerDestroysASittingTarget()
    {
        ComputerControls ai = new ComputerControls(new Random(2));
        Tank self = tank(ai, 380, 350, 0);
        Tank target = tank(new Script(), 100, 100, 180);
        List<Shell> shells = new ArrayList<>();
        int ticks = 0;
        while (target.isAlive() && ticks++ < 60 * 20)
        {
            ai.plan(self, target, shells, W, H);
            self.update(W, H, shells, target);
            target.update(W, H, shells, self);
            shells.removeIf(s -> {
                if (!s.move(W, H)) return true;
                if (!s.hits(target)) return false;
                target.hit(s, W, H);
                return true;
            });
        }
        assert !target.isAlive() : "the computer did not destroy a still target in 20 s";
        System.out.println("  computer destroyed a still target in " + ticks + " ticks");
    }

    private static void computerAgainstComputerFinishes()
    {
        Random random = new Random(3);
        int redWins = 0;
        for (int match = 0; match < 6; match++)
        {
            ComputerControls left = new ComputerControls(random);
            ComputerControls right = new ComputerControls(random);
            Tank red = new Tank("Red", Color.RED, left, 100, 250, 90);
            Tank green = new Tank("Green", Color.GREEN, new Script(), 380, 250, 270);
            Battle battle = new Battle(red, green, right, null);
            battle.startMatch(true);
            int ticks = 0;
            while (battle.phase() != Battle.Phase.MATCH_OVER && ticks++ < 60 * 60 * 10)
            {
                left.plan(red, green, battle.shells(), W, H);
                battle.tick(W, H);
            }
            assert battle.phase() == Battle.Phase.MATCH_OVER : "a computer-vs-computer match did not end in 10 minutes";
            if (battle.winner() == red) redWins++;
            System.out.println("  match " + (match + 1) + ": " + battle.redWins() + "-" + battle.greenWins() + " in " + ticks / 60 + " s");
        }
        assert redWins > 0 && redWins < 6 : "one side won every match: " + redWins;
    }
}
