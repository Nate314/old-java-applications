package simplealgorithms;

import java.util.List;
import java.util.Random;

/** Self-check for Hanoi.moves and HanoiTower. Run with java -ea. */
public class HanoiCheck {
	public static void main(String[] args) {
		for (int disks = 1; disks <= 10; disks++) {
			// The original loop's moves are all legal and solve the tower in the fewest moves.
			List<Hanoi.Move> moves = Hanoi.moves(disks);
			check(moves.size() == (1 << disks) - 1, disks + " disks: move count");
			HanoiTower tower = new HanoiTower(disks);
			check(tower.movesLeft() == moves.size(), disks + " disks: movesLeft at the start");
			for (Hanoi.Move move : moves) {
				check(tower.top(tower.pegOf(move.disk)) == move.disk, disks + " disks: " + move + " lifts its top disk");
				int[] next = tower.nextMove();
				int to = tower.target(move);
				check(next[0] == tower.pegOf(move.disk) && next[1] == to, disks + " disks: " + move + " is the solver's move");
				check(tower.move(tower.pegOf(move.disk), to) == move.disk, disks + " disks: " + move + " is legal");
			}
			check(tower.isSolved(), disks + " disks: solved on peg " + tower.goal);
			check(tower.nextMove() == null && tower.movesLeft() == 0, disks + " disks: nothing left");

			// From any position reached by legal random moves, the solver finishes in exactly movesLeft moves.
			Random random = new Random(disks);
			for (int trial = 0; trial < 50; trial++) {
				HanoiTower mixed = new HanoiTower(disks);
				for (int i = 0; i < 40; i++) {
					mixed.move(random.nextInt(3), random.nextInt(3));
				}
				int left = mixed.movesLeft();
				int made = 0;
				for (int[] next = mixed.nextMove(); next != null; next = mixed.nextMove()) {
					check(mixed.move(next[0], next[1]) >= 0, disks + " disks: solver move is legal");
					made++;
					check(made <= left, disks + " disks: solver takes no more than movesLeft");
				}
				check(made == left && mixed.isSolved(), disks + " disks: solver finishes in movesLeft moves");
			}
		}
		HanoiTower tower = new HanoiTower(3);
		check(tower.move(0, 1) == 0 && !tower.canMove(0, 1) && tower.canMove(1, 2), "no disk on a smaller one");
		check(tower.move(2, 0) == -1, "an empty peg has nothing to move");
		checkBack();
		System.out.println("HanoiCheck passed");
	}

	/** The page's Back takes moves back one at a time, hand moves too, and returns to the printed path. */
	private static void checkBack() {
		HanoiPage page = new HanoiPage();
		for (int i = 0; i < 6; i++) {
			page.step();
		}
		check(page.onPath == 6, "six printed moves are on the path");
		int[] before = pegs(page.tower);
		int[] hand = page.tower.nextMove();
		int from = hand[0] == 0 ? 1 : 0; // any legal move that is not the printed one
		int to = 3 - from - hand[0];
		if (!page.tower.canMove(from, to)) {
			int swap = from;
			from = to;
			to = swap;
		}
		page.click(from * LabPanel.WIDTH / 3 + 10, 200);
		page.click(to * LabPanel.WIDTH / 3 + 10, 200);
		check(page.onPath == -1, "a hand move leaves the printed path");
		page.back();
		check(page.onPath == 6 && java.util.Arrays.equals(pegs(page.tower), before), "Back undoes the hand move, back on the path");
		for (int i = 0; i < 6; i++) {
			page.back();
		}
		check(page.onPath == 0 && page.tower.height(0) == page.tower.disks, "Back returns to the start");
		page.back();
		check(page.onPath == 0 && page.tower.height(0) == page.tower.disks, "Back at the start does nothing");
		page.step();
		check(page.onPath == 1, "Step goes on along the printed path after Back");
	}

	private static int[] pegs(HanoiTower tower) {
		int[] pegs = new int[tower.disks];
		for (int disk = 0; disk < pegs.length; disk++) {
			pegs[disk] = tower.pegOf(disk);
		}
		return pegs;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
