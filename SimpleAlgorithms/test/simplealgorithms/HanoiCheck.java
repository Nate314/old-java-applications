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
		System.out.println("HanoiCheck passed");
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
