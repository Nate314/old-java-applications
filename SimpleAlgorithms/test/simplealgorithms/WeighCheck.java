package simplealgorithms;

import java.util.Arrays;
import java.util.Random;

/** Self-check for Weighing, the Weigh tab's boxes. Run with java -ea. */
public class WeighCheck {
	public static void main(String[] args) {
		// The fewest weighings that always suffice, for 1 to 8 boxes.
		int[] fewest = { 0, 1, 3, 5, 7, 10, 13, 16 };
		for (int n = 1; n <= 8; n++) {
			check(Weighing.fewestSure(n) == fewest[n - 1], "always enough for " + n);
		}

		// Nothing weighed: every order is possible, every box in the first column.
		Weighing fresh = new Weighing(new int[] { 40, 10, 30, 20 });
		check(fresh.orders() == 24, "4 boxes: 24 orders");
		check(Arrays.equals(fresh.columns(), new int[4]), "one column at first");
		check(fresh.orders(1, 3) == 12, "one weighing halves 24 orders");

		// A chain: B < D and D < C prove B < C, and weighing B and C again tells nothing new.
		fresh.weigh(1, 3);
		fresh.weigh(3, 2);
		check(fresh.isLighter(1, 2) && fresh.known(2, 1), "lighter through a chain");
		check(!fresh.direct(1, 2) && fresh.direct(1, 3), "lines only between neighbors in the chain");
		check(fresh.orders() == 4, "B < D < C leaves 4 orders of 4 boxes");
		Weighing.Result again = fresh.weigh(1, 2);
		check(again.before == again.after && fresh.wasted() == 1, "a weighing of a known pair proves nothing");
		check(fresh.columns()[1] == 0 && fresh.columns()[3] == 1 && fresh.columns()[2] == 2, "columns follow the chain");
		check(fresh.bestPair() != null && !fresh.known(fresh.bestPair()[0], fresh.bestPair()[1]), "the hint is an unweighed pair");

		// Weighing the hint every time always sorts, never wastes a weighing, and keeps the true order possible.
		Random random = new Random(3);
		int[] worst = new int[9];
		for (int trial = 0; trial < 300; trial++) {
			int n = 3 + trial % 6;
			Weighing w = Weighing.random(n, random);
			int[] weights = w.weights();
			for (int a = 0; a < n; a++) {
				for (int b = a + 1; b < n; b++) {
					check(weights[a] != weights[b] && weights[a] >= 10 && weights[a] <= 99, "different weights, 10 to 99 g");
				}
			}
			int[] pair;
			while ((pair = w.bestPair()) != null) {
				w.weigh(pair[0], pair[1]);
				check(w.orders() >= 1, "the true order stays possible");
			}
			check(w.sorted() && w.wasted() == 0, "the hint sorts without waste");
			// Sorted: the columns are the boxes' ranks by weight.
			int[] columns = w.columns();
			for (int a = 0; a < n; a++) {
				int rank = 0;
				for (int b = 0; b < n; b++) {
					if (weights[b] < weights[a]) {
						rank++;
					}
				}
				check(columns[a] == rank, "sorted boxes stand in one row by weight");
			}
			worst[n] = Math.max(worst[n], w.log.size());
			check(w.compares(3) >= n - 1, "merge sort compares counted");
		}
		// Halving the orders as evenly as it can, the hint stays within one weighing of the best always possible.
		for (int n = 3; n <= 8; n++) {
			check(worst[n] <= Weighing.fewestSure(n) + 1, n + " boxes by hint: at most " + (Weighing.fewestSure(n) + 1) + ", took " + worst[n]);
		}
		System.out.println("WeighCheck passed");
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
