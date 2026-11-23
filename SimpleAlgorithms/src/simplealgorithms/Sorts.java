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
	/** How each sort works, in a line. */
	static final String[] METHODS = { "swaps neighbors that are out of order; each pass carries the biggest left to the end.",
			"finds the smallest of the rest and swaps it into the next place.",
			"takes each number in turn and steps it left past every bigger one.",
			"sorts each half, then merges them by taking the smaller front number each time.",
			"takes the last number as the pivot, puts smaller ones before it, then sorts each side.",
			"makes a heap (each parent bigger than its children), then moves its top to the end, again and again." };

	/**
	 * One thing a sort did: compare a and b, swap a and b, or write value b at a. Its note says why, in words, with %a
	 * and %b for the values at a and b after the step, %A and %B for their places (counted from 1) and %c for c.
	 */
	static final class Step {
		final int kind;
		final int a;
		final int b;
		final String note;
		/** A value the note names that is not in the list (merge sort's left value, kept in its spare array). */
		final int c;

		Step(int kind, int a, int b, String note, int c) {
			this.kind = kind;
			this.a = a;
			this.b = b;
			this.note = note;
			this.c = c;
		}

		/** The note with its values filled in, for the values after this step. */
		String describe(int[] values) {
			StringBuilder out = new StringBuilder();
			for (int i = 0; i < note.length(); i++) {
				char ch = note.charAt(i);
				char code = i + 1 < note.length() ? note.charAt(i + 1) : ' ';
				if (ch != '%' || "abcAB".indexOf(code) < 0) {
					out.append(ch);
					continue;
				}
				i++;
				out.append(code == 'a' ? values[a] : code == 'b' ? values[b] : code == 'c' ? c : code == 'A' ? a + 1 : b + 1);
			}
			return out.toString();
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

		/** v[i] > v[j], recorded as a compare with the note for its answer. */
		boolean greater(int i, int j, String ifGreater, String ifNot) {
			boolean greater = v[i] > v[j];
			steps.add(new Step(COMPARE, i, j, greater ? ifGreater : ifNot, 0));
			return greater;
		}

		void swap(int i, int j, String note) {
			steps.add(new Step(SWAP, i, j, note, 0));
			int t = v[i];
			v[i] = v[j];
			v[j] = t;
		}

		void write(int i, int value, String note) {
			steps.add(new Step(WRITE, i, value, note, 0));
			v[i] = value;
		}
	}

	/** Swaps neighbors out of order; stops after a pass with no swaps. */
	private static void bubble(Recorder r) {
		for (int end = r.v.length - 1; end > 0; end--) {
			boolean swapped = false;
			for (int i = 0; i < end; i++) {
				if (r.greater(i, i + 1, "%a is bigger than its right neighbor %b, so they swap.",
						"%a is not bigger than its right neighbor %b, so they stay.")) {
					r.swap(i, i + 1, "They swap: %b moves right, toward the end.");
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
				if (r.greater(smallest, j, "%b is smaller than %a, the smallest so far, so %b is the new smallest.",
						"%b is not smaller than %a, the smallest so far.")) {
					smallest = j;
				}
			}
			if (smallest != i) {
				r.swap(i, smallest, "The smallest of the rest, %a, swaps into place %A.");
			}
		}
	}

	/** Takes each value in turn and swaps it left past every bigger one. */
	private static void insertion(Recorder r) {
		for (int i = 1; i < r.v.length; i++) {
			for (int j = i; j > 0 && r.greater(j - 1, j, "%a is bigger than %b, so %b steps left past it.",
					"%a is not bigger than %b, so %b has found its place."); j--) {
				r.swap(j - 1, j, "%a steps left to place %A.");
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
			String note = "One half is used up, so %a, next from the other, goes to place %A.";
			if (i > mid) {
				takeRight = true;
			} else if (j > hi) {
				takeRight = false;
			} else {
				// The left value now lives only in the spare array, so the compare shows the place being filled and
				// the right value it is weighed against, and its note names the left value as c.
				takeRight = spare[i] > spare[j];
				r.steps.add(new Step(COMPARE, k, j, takeRight ? "%b from the right half is smaller than %c from the left, so %b goes to place %A."
						: "%c from the left half is not bigger than %b from the right, so %c goes to place %A.", spare[i]));
				note = "%a goes to place %A.";
			}
			r.write(k, takeRight ? spare[j++] : spare[i++], note);
		}
	}

	/** Lomuto partition around the last value, then sorts each side. */
	private static void quick(Recorder r, int lo, int hi) {
		while (lo < hi) {
			int store = lo;
			for (int i = lo; i < hi; i++) {
				if (r.greater(hi, i, "%b is smaller than the pivot %a, so it joins the small side.",
						"%b is not smaller than the pivot %a, so it stays on the big side.")) {
					if (i != store) {
						r.swap(i, store, "%b swaps to the end of the small side, place %B.");
					}
					store++;
				}
			}
			if (store != hi) {
				r.swap(store, hi, "The pivot %a swaps to place %A, between the two sides: its final place.");
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
			r.swap(0, end, "The biggest left, %b, moves from the top of the heap to place %B, where it stays.");
			siftDown(r, 0, end);
		}
	}

	private static void siftDown(Recorder r, int i, int size) {
		while (true) {
			int child = 2 * i + 1;
			if (child >= size) {
				return;
			}
			if (child + 1 < size && r.greater(child + 1, child, "Of the two children, %a is bigger than %b.",
					"Of the two children, %a is not bigger than %b.")) {
				child++;
			}
			if (!r.greater(child, i, "Child %a is bigger than its parent %b, so they swap.",
					"Child %a is not bigger than its parent %b, so the heap is in order here.")) {
				return;
			}
			r.swap(i, child, "%a moves up and %b sinks down the heap.");
			i = child;
		}
	}
}
