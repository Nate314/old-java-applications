package simplealgorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sorting by weighing: boxes of hidden, different weights that can only be compared two at a time on a balance.
 * It keeps what the weighings so far prove (who is lighter than whom, through any chain), counts the orders of the
 * boxes that still fit those weighings, and finds the weighing that splits those orders most evenly.
 */
final class Weighing {
	/** One weighing: the lighter and heavier box, and how many orders were possible before and after it. */
	static final class Result {
		final int lighter;
		final int heavier;
		final long before;
		final long after;

		Result(int lighter, int heavier, long before, long after) {
			this.lighter = lighter;
			this.heavier = heavier;
			this.before = before;
			this.after = after;
		}
	}

	final int n;
	private final int[] weights;
	/** lighter[a][b]: the weighings prove box a lighter than box b. */
	private final boolean[][] lighter;
	final List<Result> log = new ArrayList<>();

	Weighing(int[] weights) {
		this.weights = weights.clone();
		n = weights.length;
		lighter = new boolean[n][n];
	}

	/** n boxes with different weights from 10 to 99 grams, in random order. */
	static Weighing random(int n, Random random) {
		List<Integer> pool = new ArrayList<>();
		for (int w = 10; w <= 99; w++) {
			pool.add(w);
		}
		int[] weights = new int[n];
		for (int i = 0; i < n; i++) {
			weights[i] = pool.remove(random.nextInt(pool.size()));
		}
		return new Weighing(weights);
	}

	int weight(int box) {
		return weights[box];
	}

	int[] weights() {
		return weights.clone();
	}

	boolean isLighter(int a, int b) {
		return lighter[a][b];
	}

	boolean known(int a, int b) {
		return lighter[a][b] || lighter[b][a];
	}

	/** Puts a and b on the scale: records which is lighter, and everything that follows from it. */
	Result weigh(int a, int b) {
		int light = weights[a] < weights[b] ? a : b;
		int heavy = light == a ? b : a;
		long before = orders();
		for (int x = 0; x < n; x++) {
			if (x != light && !lighter[x][light]) {
				continue;
			}
			for (int y = 0; y < n; y++) {
				if (y == heavy || lighter[heavy][y]) {
					lighter[x][y] = true;
				}
			}
		}
		Result result = new Result(light, heavy, before, orders());
		log.add(result);
		return result;
	}

	/** How many orders of the boxes, lightest first, fit every weighing so far. */
	long orders() {
		return orders(-1, -1);
	}

	/** The orders that would be left if a turned out lighter than b (with a = -1: no extra weighing). */
	long orders(int a, int b) {
		int[] below = new int[n];
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				if (lighter[y][x]) {
					below[x] |= 1 << y;
				}
			}
		}
		if (a >= 0) {
			below[b] |= 1 << a;
		}
		// ways[set]: the orders in which the boxes of set can come first, lightest first.
		long[] ways = new long[1 << n];
		ways[0] = 1;
		for (int set = 0; set < ways.length; set++) {
			if (ways[set] == 0) {
				continue;
			}
			for (int x = 0; x < n; x++) {
				if ((set & 1 << x) == 0 && (below[x] & ~set) == 0) {
					ways[set | 1 << x] += ways[set];
				}
			}
		}
		return ways[ways.length - 1];
	}

	boolean sorted() {
		return orders() == 1;
	}

	/** The unweighed pair whose weighing leaves the fewest orders however it tips, or null once sorted. */
	int[] bestPair() {
		int[] best = null;
		long bestWorst = Long.MAX_VALUE;
		for (int a = 0; a < n; a++) {
			for (int b = a + 1; b < n; b++) {
				if (!known(a, b)) {
					long worst = Math.max(orders(a, b), orders(b, a));
					if (worst < bestWorst) {
						bestWorst = worst;
						best = new int[] { a, b };
					}
				}
			}
		}
		return best;
	}

	/** Each box's column in a picture of what is known: the longest chain of boxes proven lighter than it. */
	int[] columns() {
		int[] column = new int[n];
		// Boxes with fewer boxes below them come first, so every box below is placed before the box above.
		for (int count = 0; count < n; count++) {
			for (int x = 0; x < n; x++) {
				if (lighterCount(x) == count) {
					for (int y = 0; y < n; y++) {
						if (lighter[y][x]) {
							column[x] = Math.max(column[x], column[y] + 1);
						}
					}
				}
			}
		}
		return column;
	}

	private int lighterCount(int x) {
		int count = 0;
		for (int y = 0; y < n; y++) {
			if (lighter[y][x]) {
				count++;
			}
		}
		return count;
	}

	/** True if a is proven lighter than b with no box proven between them: a line in the picture. */
	boolean direct(int a, int b) {
		if (!lighter[a][b]) {
			return false;
		}
		for (int c = 0; c < n; c++) {
			if (lighter[a][c] && lighter[c][b]) {
				return false;
			}
		}
		return true;
	}

	/** Weighings that proved nothing the earlier ones had not. */
	int wasted() {
		int wasted = 0;
		for (Result result : log) {
			if (result.after == result.before) {
				wasted++;
			}
		}
		return wasted;
	}

	/** The fewest weighings that always suffice for n boxes: log2 of n! rounded up (exact up to 11 boxes). */
	static int fewestSure(int n) {
		long orders = factorial(n);
		int weighings = 0;
		while (1L << weighings < orders) {
			weighings++;
		}
		return weighings;
	}

	static long factorial(int n) {
		long f = 1;
		for (int i = 2; i <= n; i++) {
			f *= i;
		}
		return f;
	}

	/** How many compares a sort of the lab's race makes on these boxes, in their starting order. */
	int compares(int algorithm) {
		int count = 0;
		for (Sorts.Step step : Sorts.trace(algorithm, weights)) {
			if (step.kind == Sorts.COMPARE) {
				count++;
			}
		}
		return count;
	}
}
