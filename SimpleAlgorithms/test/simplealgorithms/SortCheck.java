package simplealgorithms;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Self-check for Sorts and SortRace. Run with java -ea. */
public class SortCheck {
	public static void main(String[] args) {
		Random random = new Random(7);
		for (int n : new int[] { 1, 2, 3, 16, 32, 64, 128 }) {
			for (int order = 0; order < Sorts.ORDERS.length; order++) {
				int[] numbers = Sorts.numbers(order, n, random);
				int[] sorted = numbers.clone();
				Arrays.sort(sorted);
				for (int algorithm = 0; algorithm < Sorts.NAMES.length; algorithm++) {
					// Playing the recorded steps back sorts the numbers.
					int[] values = numbers.clone();
					List<Sorts.Step> steps = Sorts.trace(algorithm, numbers);
					for (Sorts.Step step : steps) {
						check(step.a >= 0 && step.a < n && (step.kind == Sorts.WRITE || step.b >= 0 && step.b < n),
								Sorts.NAMES[algorithm] + ": step inside the numbers");
						step.apply(values);
						String said = step.describe(values);
						check(!said.isEmpty() && said.indexOf('%') < 0, Sorts.NAMES[algorithm] + ": every step says why");
					}
					check(Arrays.equals(values, sorted), Sorts.NAMES[algorithm] + " sorts " + n + " " + Sorts.ORDERS[order]);
				}
			}
		}

		// The notes name the right values and places.
		check(said(0, new int[] { 2, 1 }, 0).equals("2 is bigger than its right neighbor 1, so they swap."), "bubble compare note");
		check(said(0, new int[] { 2, 1 }, 1).equals("They swap: 2 moves right, toward the end."), "bubble swap note");
		check(said(4, new int[] { 3, 1, 2 }, 0).equals("3 is not smaller than the pivot 2, so it stays on the big side."), "quicksort compare note");
		check(said(4, new int[] { 3, 1, 2 }, 1).equals("1 is smaller than the pivot 2, so it joins the small side."), "quicksort small side note");
		check(said(3, new int[] { 2, 1 }, 0).equals("1 from the right half is smaller than 2 from the left, so 1 goes to place 1."), "merge compare note");
		check(said(3, new int[] { 2, 1 }, 2).equals("One half is used up, so 2, next from the other, goes to place 2."), "merge used-up note");

		// The lessons the race shows hold for these numbers.
		int[] nearly = Sorts.numbers(1, 64, new Random(1));
		check(cost(2, nearly) < cost(3, nearly) && cost(2, nearly) < cost(4, nearly), "insertion sort wins on nearly sorted numbers");
		int[] shuffled = Sorts.numbers(0, 64, new Random(1));
		check(cost(3, shuffled) * 3 < cost(0, shuffled), "merge sort beats bubble sort by far on random numbers");
		int[] reversed = Sorts.numbers(2, 64, new Random(1));
		check(cost(4, reversed) > cost(3, reversed) * 3, "quicksort with the last value as pivot is slow on reversed numbers");
		check(cost(0, Sorts.numbers(1, 64, new Random(1))) < cost(0, shuffled), "bubble sort stops early on nearly sorted numbers");

		// The race gives every lane the same steps, finishes each with the right place, and ends sorted.
		SortRace race = new SortRace(shuffled);
		race.advance(10);
		for (SortRace.Lane lane : race.lanes) {
			check(lane.spent >= 10 && lane.spent <= 11, Sorts.NAMES[lane.algorithm] + " spent the clock's steps");
			check(lane.compares + lane.writes == lane.spent, Sorts.NAMES[lane.algorithm] + " counts compares and writes");
		}
		while (!race.finished()) {
			race.advance(7);
		}
		int[] sorted = shuffled.clone();
		Arrays.sort(sorted);
		for (SortRace.Lane lane : race.lanes) {
			check(Arrays.equals(lane.values, sorted), Sorts.NAMES[lane.algorithm] + " ends sorted in the race");
			check(lane.spent == lane.total, Sorts.NAMES[lane.algorithm] + " spent its whole run");
			check(lane.last == null, Sorts.NAMES[lane.algorithm] + " shows no step once done");
		}
		check(race.place(race.first()) == 1 && race.place(race.last()) > 1, "places: the cheapest first");
		check(race.first().total <= race.last().total, "first and last");

		// Each print starts on the numbers and ends sorted, and seeking to a row's time, even backwards after the
		// end, gives the values that row shows.
		check(race.span == race.last().total && race.reached >= race.span, "the race's length and how far it got");
		for (SortRace.Lane lane : race.lanes) {
			check(Arrays.equals(lane.print[0], shuffled), Sorts.NAMES[lane.algorithm] + "'s print starts on the numbers");
			check(Arrays.equals(lane.print[SortRace.ROWS - 1], sorted), Sorts.NAMES[lane.algorithm] + "'s print ends sorted");
		}
		for (int row : new int[] { SortRace.ROWS - 1, 0, 37, 5, 80 }) {
			race.seek(race.timeOf(row));
			check(race.rowAt(race.clock) == row, "seek lands on row " + row);
			for (SortRace.Lane lane : race.lanes) {
				check(Arrays.equals(lane.values, lane.print[row]), Sorts.NAMES[lane.algorithm] + " at row " + row);
				check(lane.compares + lane.writes == lane.spent, Sorts.NAMES[lane.algorithm] + " counts after a seek");
				check(lane.finished() == lane.swept(), Sorts.NAMES[lane.algorithm] + " is checked once done after a seek");
			}
		}
		race.seek(-5);
		check(race.clock == 0 && race.lanes[0].next == 0, "seek stops at the start");
		race.seek(race.span + 99);
		check(race.clock == race.span && race.finished(), "seek stops at the end");
		System.out.println("SortCheck passed");
	}

	/** What step i of a sort says, with the values after it. */
	private static String said(int algorithm, int[] numbers, int i) {
		int[] values = numbers.clone();
		List<Sorts.Step> steps = Sorts.trace(algorithm, numbers);
		for (int s = 0; s <= i; s++) {
			steps.get(s).apply(values);
		}
		return steps.get(i).describe(values);
	}

	private static int cost(int algorithm, int[] numbers) {
		return Sorts.cost(Sorts.trace(algorithm, numbers));
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}
}
