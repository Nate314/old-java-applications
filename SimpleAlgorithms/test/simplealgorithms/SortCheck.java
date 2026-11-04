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
					}
					check(Arrays.equals(values, sorted), Sorts.NAMES[algorithm] + " sorts " + n + " " + Sorts.ORDERS[order]);
				}
			}
		}

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
		System.out.println("SortCheck passed");
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
