package tankgame;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        wallsStopShellsAndTanks();
        arenasFitTheField();
        matchRulesAndRecord();
        computerDestroysASittingTarget();
        computerShootsRoundTheBunker();
        computerAgainstComputerFinishes();
        watchPlaysOnItsOwn();
        powersFromCrates();
        cratesFallAndAreTaken();
        suppliesSetting();
        computerAgainstComputerWithSupplies();
        System.out.println("BattleCheck: all checks passed");
    }

    /** Watch (C): from the title the computer starts matches against itself, they are not recorded, and C hands red back. */
    private static void watchPlaysOnItsOwn() throws Exception
    {
        File file = File.createTempFile("tankgame-watch", ".txt");
        file.delete();
        Script redKeys = new Script();
        Tank red = new Tank("Red", Color.RED, redKeys, 100, 250, 90);
        Tank green = new Tank("Green", Color.GREEN, new Script(), 380, 250, 270);
        Battle battle = new Battle(red, green, new ComputerControls(new Random(2)), Record.load(file));
        battle.toggleWatch();
        assert battle.watching() && red.controls() instanceof ComputerControls;
        int ticks = 0;
        while (battle.phase() == Battle.Phase.TITLE && ticks++ < Battle.WATCH_START_TICKS + 5) battle.tick(W, H);
        assert battle.phase() == Battle.Phase.ROUND_START && battle.vsComputer() : "watch did not start a match";
        ticks = 0;
        while (battle.phase() != Battle.Phase.MATCH_OVER && ticks++ < 60 * 60 * 20) battle.tick(W, H);
        assert battle.phase() == Battle.Phase.MATCH_OVER : "computer against computer did not finish a match";
        assert Record.load(file).wins() + Record.load(file).losses() == 0 : "a watched match was recorded";
        ticks = 0;
        while (battle.phase() == Battle.Phase.MATCH_OVER && ticks++ < Battle.WATCH_START_TICKS + 5) battle.tick(W, H);
        assert battle.phase() == Battle.Phase.ROUND_START : "watch did not start the next match";
        battle.toggleWatch();
        assert !battle.watching() && red.controls() == redKeys;
        file.delete();
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

    private static void wallsStopShellsAndTanks()
    {
        Tank a = tank(new Script(), 100, 200, 90);
        Arena bunker = Arena.build(Arena.LAYOUTS.get(1), W, H, a);
        Arena.Wall wall = bunker.walls().get(0);
        assert bunker.name().equals("The Bunker") && Math.abs(wall.left - 229) < 1e-9 && Math.abs(wall.right - 255) < 1e-9;
        // A shell flying right bounces off the wall's left side, then bursts on the next wall it meets.
        Shell s = new Shell(a, 200, 200, 1, 0);
        assert s.move(W, H, bunker) && s.move(W, H, bunker) && s.move(W, H, bunker) && s.bounced() && s.dx() < 0;
        assert s.x() <= wall.left - Shell.RADIUS + 1e-6 : s.x();
        Shell twice = new Shell(a, 470, 200, 1, 0);   // off the edge, then into the wall
        int ticks = 0;
        while (twice.move(W, H, bunker) && ticks++ < 100) { }
        assert twice.burst() && twice.x() >= wall.right && ticks < 100 : twice.x();
        assert !bunker.clear(150, 230, 350, 230) && bunker.clear(150, 125, 350, 125);
        // A tank cannot drive into the wall, but slides along it.
        Script drive = new Script();
        Tank t = tank(drive, 150, 200, 135);
        Tank far = tank(new Script(), 420, 420, 0);
        List<Shell> shells = new ArrayList<>();
        drive.forward = true;
        for (int i = 0; i < 40; i++) t.update(W, H, shells, far, bunker);
        assert t.x() + Tank.SIZE <= wall.left && t.y() > 260 : t.x() + "," + t.y();
        // A barrel poking through the wall fires into it (and the shell bounces back), not past it.
        Script gun = new Script();
        Tank close = tank(gun, (int) wall.left - Tank.SIZE - 1, 230, 90);
        gun.fire = true;
        close.update(W, H, shells, far, bunker);
        Shell point = shells.get(0);
        assert shells.size() == 1 && point.move(W, H, bunker) && point.bounced() && point.x() < wall.left;
    }

    private static void arenasFitTheField()
    {
        for (int width : new int[] { W, 344 })
        {
            Tank red = tank(new Script(), 100, 250, 90);
            Tank green = tank(new Script(), width - 120, 250, 270);
            for (Arena.Layout layout : Arena.LAYOUTS)
            {
                Arena arena = Arena.build(layout, width, H, red, green);
                assert !arena.walls().isEmpty() : layout.name() + " lost every wall at width " + width;
                assert !arena.blocks(red.x(), red.y(), Tank.SIZE) && !arena.blocks(green.x(), green.y(), Tank.SIZE);
            }
        }
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
        battle.setLayouts(Arena.OPEN);           // the rules on an open field; walls are checked on their own
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
        battle.setLayouts(Arena.LAYOUTS);        // the computer has to find red behind the walls
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

    /** Red sits behind the bunker: the computer has no straight shot and must drive round the wall to get one. */
    private static void computerShootsRoundTheBunker()
    {
        ComputerControls ai = new ComputerControls(new Random(4));
        Tank self = tank(ai, 380, 230, 270);
        Tank target = tank(new Script(), 100, 230, 90);
        Arena bunker = Arena.build(Arena.LAYOUTS.get(1), W, H, self, target);
        assert !bunker.clear(self.x(), self.y(), target.x(), target.y());
        List<Shell> shells = new ArrayList<>();
        int ticks = 0;
        while (target.isAlive() && ticks++ < 60 * 30)
        {
            ai.plan(self, target, shells, W, H, bunker);
            self.update(W, H, shells, target, bunker);
            target.update(W, H, shells, self, bunker);
            shells.removeIf(s -> {
                if (!s.move(W, H, bunker)) return true;
                if (!s.hits(target)) return false;
                target.hit(s, W, H, bunker);
                return true;
            });
            assert !bunker.blocks(self.x(), self.y(), Tank.SIZE);
        }
        assert !target.isAlive() : "the computer did not get round the bunker in 30 s";
        System.out.println("  computer got round the bunker in " + ticks + " ticks");
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
                left.plan(red, green, battle.shells(), W, H, battle.arena());
                battle.tick(W, H);
            }
            assert battle.phase() == Battle.Phase.MATCH_OVER : "a computer-vs-computer match did not end in 10 minutes";
            if (battle.winner() == red) redWins++;
            System.out.println("  match " + (match + 1) + ": " + battle.redWins() + "-" + battle.greenWins() + " in " + ticks / 60 + " s");
        }
        assert redWins > 0 && redWins < 6 : "one side won every match: " + redWins;
    }

    /** Each crate's power: repair, shield, triple shot, ricochet and turbo; and a crate is taken or shot only once landed. */
    private static void powersFromCrates()
    {
        Script s = new Script();
        Tank a = tank(s, 100, 200, 90);
        Tank b = tank(new Script(), 400, 200, 270);
        Shell shot = new Shell(b, 100, 200, -1, 0);
        a.hit(shot, W, H);
        a.give(Supply.Power.REPAIR);
        assert a.health() == Tank.MAX_HEALTH;
        a.give(Supply.Power.REPAIR);
        assert a.health() == Tank.MAX_HEALTH : "repair went over full health";

        a.give(Supply.Power.SHIELD);
        assert a.shielded() && !a.hit(shot, W, H) && a.health() == Tank.MAX_HEALTH && !a.shielded();
        assert a.hit(shot, W, H) && a.health() == Tank.MAX_HEALTH - 1 : "the shield stopped more than one hit";

        List<Shell> shells = new ArrayList<>();
        a.give(Supply.Power.TRIPLE);
        for (int i = 0; i < Tank.TRIPLE_SHOTS; i++) a.fireTank(shells);
        assert shells.size() == 3 * Tank.TRIPLE_SHOTS && a.tripleShots() == 0 : shells.size();
        // Facing right (90): the middle shell flies straight, the others fan up and down.
        assert Math.abs(shells.get(0).dy()) < 1e-9 && shells.get(1).dy() < 0 && shells.get(2).dy() > 0 : "no fan";
        a.fireTank(shells);
        assert shells.size() == 3 * Tank.TRIPLE_SHOTS + 1 : "triple shot did not run out";
        // A volley hits a tank once at most; the next volley can hit again.
        Tank target = tank(new Script(), 400, 200, 0);
        assert target.hit(shells.get(0), W, H) && !target.hit(shells.get(1), W, H) && !target.hit(shells.get(2), W, H);
        assert target.hit(shells.get(3), W, H) && target.health() == Tank.MAX_HEALTH - 2 : target.health();

        a.reset();
        assert a.tripleShots() == 0 && !a.shielded() && a.ricochetTicks() == 0 && a.turboTicks() == 0;
        a.give(Supply.Power.RICOCHET);
        shells.clear();
        a.fireTank(shells);
        Shell r = shells.get(0);
        int bounces = 0;
        while (r.move(W, H)) if (r.bounced()) bounces++;
        assert r.ricochet() && bounces == Shell.RICOCHET_BOUNCES : bounces;

        a.give(Supply.Power.TURBO);
        s.forward = true;
        double x = a.x();
        a.update(W, H, shells, b);
        assert Math.abs(a.x() - x - Tank.TURBO_SPEED) < 1e-9 : a.x() - x;
        s.forward = false;
        for (int i = 0; i < Tank.TURBO_TICKS; i++) a.update(W, H, shells, b);
        s.forward = true;
        x = a.x();
        a.update(W, H, shells, b);
        assert Math.abs(a.x() - x - Tank.SPEED) < 1e-9 && a.turboTicks() == 0 : "turbo did not wear off";

        Supply crate = new Supply(Supply.Power.SHIELD, 300, 300);
        Tank over = tank(new Script(), 300, 300, 0);
        Shell atCrate = new Shell(b, 300, 300, 1, 0);
        assert !crate.takenBy(over) && !crate.hitBy(atCrate) : "a falling crate was taken";
        for (int i = 0; i < Supply.FALL_TICKS; i++) crate.tick();
        assert crate.landed() && crate.takenBy(over) && crate.hitBy(atCrate);
        assert !crate.takenBy(tank(new Script(), 300, 420, 0)) && !crate.hitBy(new Shell(b, 340, 300, 1, 0));
    }

    /** With supply drops on, crates fall a few seconds into a round, clear of walls and tanks, and the computer takes them. */
    private static void cratesFallAndAreTaken() throws Exception
    {
        File file = File.createTempFile("tankgame-crates", ".txt");
        file.delete();
        Tank red = new Tank("Red", Color.RED, new Script(), 100, 250, 90);
        Tank green = new Tank("Green", Color.GREEN, new Script(), 380, 250, 270);
        Battle battle = new Battle(red, green, new ComputerControls(new Random(5)), Record.load(file));
        battle.setDropRandom(new Random(5));
        battle.setSupplies(true);
        battle.startMatch(false);
        while (battle.phase() != Battle.Phase.FIGHT) battle.tick(W, H);
        for (int i = 1; i < Battle.FIRST_DROP_TICKS; i++)
        {
            battle.tick(W, H);
            assert battle.supply() == null : "a crate came early";
        }
        int ticks = 0;
        while (battle.supply() == null && ticks++ < 20) battle.tick(W, H);
        Supply crate = battle.supply();
        assert crate != null && !crate.landed() : "no crate fell";
        assert !battle.arena().blocks(crate.x(), crate.y(), Supply.HALF) && crate.y() >= 80;
        assert Math.hypot(crate.x() - red.x(), crate.y() - red.y()) > Tank.SIZE + 50;
        assert Math.hypot(crate.x() - green.x(), crate.y() - green.y()) > Tank.SIZE + 50;
        for (int i = 0; i < Supply.FALL_TICKS; i++) battle.tick(W, H);
        assert battle.supply() == crate && crate.landed() : "nobody moved, but the crate went";

        battle.setSupplies(false);
        assert battle.supply() == null : "turning supplies off left the crate";
        for (int i = 0; i < Battle.DROP_GAP_TICKS * 3; i++)
        {
            battle.tick(W, H);
            assert battle.supply() == null : "a crate fell with supplies off";
        }

        // Watch: both computers drive; between them they should take crates of several kinds.
        battle.setSupplies(true);
        battle.toggleWatch();
        battle.startMatch(true);
        ticks = 0;
        Set<Supply.Power> taken = new HashSet<>();
        while (taken.size() < 3 && ticks++ < 60 * 60 * 10)
        {
            battle.tick(W, H);
            if (battle.noticePower() != null && battle.noticeTicks() == 0) taken.add(battle.noticePower());
        }
        assert taken.size() >= 3 : "the computers took only " + taken;
        System.out.println("  computers took " + taken + " in " + ticks / 60 + " s");
        file.delete();
    }

    /** X: supply drops are on by default, and turning them off is saved beside the record without losing it. */
    private static void suppliesSetting() throws Exception
    {
        File file = File.createTempFile("tankgame-supplies", ".txt");
        file.delete();
        assert Record.load(file).supplies() : "supply drops should start on";
        Record record = Record.load(file);
        record.add(true);
        record.add(false);
        Battle battle = new Battle(tank(new Script(), 100, 250, 90), tank(new Script(), 380, 250, 270),
                        new ComputerControls(new Random(1)), record);
        battle.setSupplies(record.supplies());
        battle.toggleSupplies();
        assert !battle.supplies();
        Record back = Record.load(file);
        assert !back.supplies() && back.wins() == 1 && back.losses() == 1 : back;
        String text = new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8");
        assert text.startsWith("1,1\n") && text.contains("supplies=off") : text;
        back.add(true);
        assert !Record.load(file).supplies() && Record.load(file).wins() == 2 : "a win forgot the setting";
        battle.toggleSupplies();
        assert Record.load(file).supplies() && Record.load(file).wins() == 1;
        file.delete();
    }

    /** Computer against computer with crates still finishes matches, without one side winning every one. */
    private static void computerAgainstComputerWithSupplies()
    {
        Random random = new Random(11);
        int redWins = 0;
        for (int match = 0; match < 6; match++)
        {
            ComputerControls left = new ComputerControls(random);
            Tank red = new Tank("Red", Color.RED, left, 100, 250, 90);
            Tank green = new Tank("Green", Color.GREEN, new Script(), 380, 250, 270);
            Battle battle = new Battle(red, green, new ComputerControls(random), null);
            battle.setDropRandom(random);
            battle.setSupplies(true);
            battle.startMatch(true);
            int ticks = 0;
            while (battle.phase() != Battle.Phase.MATCH_OVER && ticks++ < 60 * 60 * 10)
            {
                left.plan(red, green, battle.shells(), W, H, battle.arena(), battle.supply());
                battle.tick(W, H);
            }
            assert battle.phase() == Battle.Phase.MATCH_OVER : "a match with crates did not end in 10 minutes";
            if (battle.winner() == red) redWins++;
            System.out.println("  with crates, match " + (match + 1) + ": " + battle.redWins() + "-" + battle.greenWins()
                            + " in " + ticks / 60 + " s");
        }
        assert redWins > 0 && redWins < 6 : "one side won every match with crates: " + redWins;
    }
}
