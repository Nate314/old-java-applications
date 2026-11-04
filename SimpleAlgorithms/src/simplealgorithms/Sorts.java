package simplealgorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Six sorting algorithms that record what they do instead of drawing it: every compare, swap and single write is a
 * {@link Step}, so the Sorting race can play them back side by side, one step at a time. A compare and a write cost
 * one step each and a swap costs two (it writes two places), so the race is fair whatever each sort does.
 */
final class Sorts {
	static final int COMPARE = 0;
	static final int SWAP = 1;
	static final int WRITE = 2;

	static final String[] NAMES = { "Bubble sort", "Selection sort", "Insertion sort", "Merge sort", "Quicksort", "Heapsort" };
	/** How the work grows with n, for random numbers. */
	static final String[] GROWTH = { "n\u00b2", "n\u00b2", "n\u00b2", "n log n", "n log n", "n log n" };
	static final String[] ORDERS = { "Random", "Nearly sorted", "Reversed", "Few values" };

	/** One thing a sort did: compare a and b, swap a and b, or write value b at a. */
	static final class Step {
		final int kind;
		final int a;
		final int b;

		Step(int kind, int a, int b) {
			this.kind = kind;
			this.a = a;
			this.b = b;
		}

		int cost() {
			return kind == SWAP ? 2 : 1;
		}

		/** Does the step to the values. */
		void apply(int[] values) {
			if (kind == SWAP) {
				int t = values[a];
				values[a] = values[b];
				values[b] = t;
			} else if (kind == WRITE) {
				values[a] = b;
			}
		}
	}

	private Sorts() {
	}

	/** The numbers to sort: 1 to n (or a few heights) in one of the {@link #ORDERS}. */
	static int[] numbers(int order, int n, Random random) {
		int[] values = new int[n];
		for (int i = 0; i < n; i++) {
			values[i] = i + 1;
		}
		if (order == 0) {
			shuffle(values, random);
		} else if (order == 1 && n > 1) {
			// Sorted, then a few neighbors swapped.
			for (int i = 0; i < Math.max(1, n / 10); i++) {
				int at = random.nextInt(n - 1);
				int t = values[at];
				values[at] = values[at + 1];
				values[at + 1] = t;
			}
		} else if (order == 2) {
			for (int i = 0; i < n; i++) {
				values[i] = n - i;
			}
		} else if (order == 3) {
			for (int i = 0; i < n; i++) {
				values[i] = Math.max(1, (i % 4 + 1) * n / 4);
			}
			shuffle(values, random);
		}
		return values;
	}

	private static void shuffle(int[] values, Random random) {
		for (int i = values.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int t = values[i];
			values[i] = values[j];
			values[j] = t;
		}
	}

	/** Sorts a copy of the values with the given algorithm (an index into {@link #NAMES}) and returns its steps. */
	static List<Step> trace(int algorithm, int[] values) {
		Recorder r = new Recorder(values);
		switch (algorithm) {
		case 0:
			bubble(r);
			break;
		case 1:
			selection(r);
			break;
		case 2:
			insertion(r);
			break;
		case 3:
			mergeSort(r, 0, r.v.length - 1, new int[r.v.length]);
			break;
		case 4:
			quick(r, 0, r.v.length - 1);
			break;
		default:
			heap(r);
		}
		return r.steps;
	}

	/** Total cost of a list of steps. */
	static int cost(List<Step> steps) {
		int total = 0;
		for (Step step : steps) {
			total += step.cost();
		}
		return total;
	}

	/** Works on its own copy and writes down each compare, swap and write. */
	private static final class Recorder {
		final int[] v;
		final List<Step> steps = new ArrayList<>();

		Recorder(int[] values) {
			v = Arrays.copyOf(values, values.length);
		}

		/** v[i] > v[j], recorded as a compare. */
		boolean greater(int i, int j) {
			steps.add(new Step(COMPARE, i, j));
			return v[i] > v[j];
		}

		void swap(int i, int j) {
			steps.add(new Step(SWAP, i, j));
			int t = v[i];
			v[i] = v[j];
			v[j] = t;
		}

		void write(int i, int value) {
			steps.add(new Step(WRITE, i, value));
			v[i] = value;
		}
	}

	/** Swaps neighbors out of order; stops after a pass with no swaps. */
	private static void bubble(Recorder r) {
		for (int end = r.v.length - 1; end > 0; end--) {
			boolean swapped = false;
			for (int i = 0; i < end; i++) {
				if (r.greater(i, i + 1)) {
					r.swap(i, i + 1);
					swapped = true;
				}
			}
			if (!swapped) {
				return;
			}
		}
	}

	/** Finds the smallest of the rest and swaps it into place. */
	private static void selection(Recorder r) {
		int n = r.v.length;
		for (int i = 0; i < n - 1; i++) {
			int smallest = i;
			for (int j = i + 1; j < n; j++) {
				if (r.greater(smallest, j)) {
					smallest = j;
				}
			}
			if (smallest != i) {
				r.swap(i, smallest);
			}
		}
	}

	/** Takes each value in turn and swaps it left past every bigger one. */
	private static void insertion(Recorder r) {
		for (int i = 1; i < r.v.length; i++) {
			for (int j = i; j > 0 && r.greater(j - 1, j); j--) {
				r.swap(j - 1, j);
			}
		}
	}

	/** Splits in half, sorts each half, and merges them through a spare array. */
	private static void mergeSort(Recorder r, int lo, int hi, int[] spare) {
		if (lo >= hi) {
			return;
		}
		int mid = (lo + hi) / 2;
		mergeSort(r, lo, mid, spare);
		mergeSort(r, mid + 1, hi, spare);
		System.arraycopy(r.v, lo, spare, lo, hi - lo + 1);
		int i = lo;
		int j = mid + 1;
		for (int k = lo; k <= hi; k++) {
			boolean takeRight;
			if (i > mid) {
				takeRight = true;
			} else if (j > hi) {
				takeRight = false;
			} else {
				// The left value now lives only in the spare array, so the compare shows the place being filled and
				// the right value it is weighed against.
				r.steps.add(new Step(COMPARE, k, j));
				takeRight = spare[i] > spare[j];
			}
			r.write(k, takeRight ? spare[j++] : spare[i++]);
		}
	}

	/** Lomuto partition around the last value, then sorts each side. */
	private static void quick(Recorder r, int lo, int hi) {
		while (lo < hi) {
			int store = lo;
			for (int i = lo; i < hi; i++) {
				if (r.greater(hi, i)) {
					if (i != store) {
						r.swap(i, store);
					}
					store++;
				}
			}
			if (store != hi) {
				r.swap(store, hi);
			}
			// Recurse into the smaller side and loop on the bigger, so the stack stays short.
			if (store - lo < hi - store) {
				quick(r, lo, store - 1);
				lo = store + 1;
			} else {
				quick(r, store + 1, hi);
				hi = store - 1;
			}
		}
	}

	/** Builds a max-heap, then swaps the biggest to the end and sifts the new top down. */
	private static void heap(Recorder r) {
		int n = r.v.length;
		for (int i = n / 2 - 1; i >= 0; i--) {
			siftDown(r, i, n);
		}
		for (int end = n - 1; end > 0; end--) {
			r.swap(0, end);
			siftDown(r, 0, end);
		}
	}

	private static void siftDown(Recorder r, int i, int size) {
		while (true) {
			int child = 2 * i + 1;
			if (child >= size) {
				return;
			}
			if (child + 1 < size && r.greater(child + 1, child)) {
				child++;
			}
			if (!r.greater(child, i)) {
				return;
			}
			r.swap(i, child);
			i = child;
		}
	}
}
