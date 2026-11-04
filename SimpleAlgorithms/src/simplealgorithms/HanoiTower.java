package simplealgorithms;

/**
 * A Tower of Hanoi on three pegs (0 left, 1 middle, 2 right) that the Algorithm Lab plays on. All disks start on
 * peg 0, disk 0 being the smallest, and the goal is to stack them all on peg 2.
 */
public class HanoiTower {
	public final int disks;
	public final int goal = 2;
	private final int[] pegOf;

	public HanoiTower(int disks) {
		this.disks = disks;
		this.pegOf = new int[disks];
	}

	public int pegOf(int disk) {
		return pegOf[disk];
	}

	/** The smallest disk on the peg, or -1 when it is empty. */
	public int top(int peg) {
		for (int disk = 0; disk < disks; disk++) {
			if (pegOf[disk] == peg) {
				return disk;
			}
		}
		return -1;
	}

	/** Number of disks on the peg. */
	public int height(int peg) {
		int count = 0;
		for (int peg2 : pegOf) {
			if (peg2 == peg) {
				count++;
			}
		}
		return count;
	}

	/** True when the top disk of from may go on to: never onto a smaller disk. */
	public boolean canMove(int from, int to) {
		int disk = top(from);
		int under = top(to);
		return from != to && disk >= 0 && (under < 0 || under > disk);
	}

	/** Moves the top disk of from to to, if allowed, and returns it (or -1). */
	public int move(int from, int to) {
		if (!canMove(from, to)) {
			return -1;
		}
		int disk = top(from);
		pegOf[disk] = to;
		return disk;
	}

	/** The peg a move from {@link Hanoi#moves} carries its disk to. */
	public int target(Hanoi.Move move) {
		return (pegOf[move.disk] + (move.right ? 1 : 2)) % 3;
	}

	public boolean isSolved() {
		return height(goal) == disks;
	}

	/**
	 * The first move of a shortest solution from this position, as {from, to}, or null when solved. Going from the
	 * largest disk down, each disk not yet on the peg it needs (the goal, for the largest) must get there, and every
	 * smaller disk must first gather on the third peg. The smallest disk that has to move moves first.
	 */
	public int[] nextMove() {
		int[] next = null;
		int need = goal;
		for (int disk = disks - 1; disk >= 0; disk--) {
			if (pegOf[disk] != need) {
				next = new int[] { pegOf[disk], need };
				need = 3 - pegOf[disk] - need;
			}
		}
		return next;
	}

	/** Fewest moves left to solve from this position (2^disks - 1 from the start). */
	public int movesLeft() {
		int left = 0;
		int need = goal;
		for (int disk = disks - 1; disk >= 0; disk--) {
			if (pegOf[disk] != need) {
				left += 1 << disk;
				need = 3 - pegOf[disk] - need;
			}
		}
		return left;
	}
}
