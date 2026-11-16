package numberguess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Hot and cold: find a number from 1 to {@link #MAX}, but each answer only says whether this guess is warmer (closer to
 * the number) or colder than the one before, never higher or lower. The halfway point between the last two guesses
 * splits what is still possible, so every number can be guessed, even one already ruled out: a guess on the far side
 * of what is left is how to split it in half.
 */
public final class Warmth {
	public static final int MAX = 100;
	/** The fewest guesses that always find the number ({@link #par(int)} of {@link #MAX}, checked by the self-check). */
	public static final int PAR = 8;
	public static final int LIMIT = PAR + 4;

	public enum Read {
		FIRST, WARMER, COLDER, SAME, FOUND, OUT_OF_RANGE, REPEAT, OVER
	}

	private final int number;
	private final List<Integer> guesses = new ArrayList<>();
	private final List<Read> reads = new ArrayList<>();
	private int low = 1, high = MAX;

	public Warmth(Random random) {
		this(random.nextInt(MAX) + 1);
	}

	/** A game with a known number, for the self-check. */
	Warmth(int number) {
		this.number = number;
	}

	public Read guess(int guess) {
		if (isOver()) {
			return Read.OVER;
		}
		if (guess < 1 || guess > MAX) {
			return Read.OUT_OF_RANGE;
		}
		Integer last = previous();
		if (last != null && last == guess) {
			return Read.REPEAT;
		}
		guesses.add(guess);
		Read read;
		if (guess == number) {
			low = high = guess;
			read = Read.FOUND;
		} else if (last == null) {
			read = Read.FIRST;
		} else {
			int now = Math.abs(guess - number), before = Math.abs(last - number);
			read = now < before ? Read.WARMER : now > before ? Read.COLDER : Read.SAME;
			// Twice the halfway point, so it stays a whole number: warmer keeps guess's side of it, the same keeps it alone.
			int twice = guess + last;
			boolean above = (guess > last) == (read == Read.WARMER);
			if (read == Read.SAME) {
				low = high = twice / 2;
			} else if (above) {
				low = Math.max(low, twice / 2 + 1);
			} else {
				high = Math.min(high, (twice - 1) / 2);
			}
		}
		reads.add(read);
		return read;
	}

	/** The guess before the latest one would be compared with, or null before the first. */
	public Integer previous() {
		return guesses.isEmpty() ? null : guesses.get(guesses.size() - 1);
	}

	/** True if {@code n} fits every answer: inside what is left, and not a guess already made. */
	public boolean isPossible(int n) {
		return isFound() ? n == number : n >= low && n <= high && !guesses.contains(n);
	}

	/** How many numbers still fit every answer. */
	public int left() {
		int count = 0;
		for (int n = low; n <= high; n++) {
			count += isPossible(n) ? 1 : 0;
		}
		return count;
	}

	public int low() {
		return low;
	}

	public int high() {
		return high;
	}

	/** The secret number; only shown once the game is over. */
	public int number() {
		return number;
	}

	public List<Integer> guesses() {
		return Collections.unmodifiableList(guesses);
	}

	public List<Read> reads() {
		return Collections.unmodifiableList(reads);
	}

	public int guessesLeft() {
		return LIMIT - guesses.size();
	}

	public boolean isFound() {
		return !reads.isEmpty() && reads.get(reads.size() - 1) == Read.FOUND;
	}

	public boolean isOver() {
		return isFound() || guessesLeft() <= 0;
	}

	/**
	 * The fewest guesses that always find a number from 1 to {@code max}, worked out by trying every guess against every
	 * answer. The state is what is left (low to high), and the last guess, which is not the number. Earlier wrong
	 * guesses inside what is left are treated as still possible, which can only make the count larger, so it is a count
	 * a real strategy reaches.
	 */
	static int par(int max) {
		int n = max + 1;
		byte[] best = new byte[n * n * n]; // guesses needed from each state, 0 while not yet known
		for (int k = 1; k < 64; k++) {
			byte[] next = best.clone();
			for (int lo = 1; lo <= max; lo++) {
				for (int hi = lo; hi <= max; hi++) {
					for (int last = 0; last <= max; last++) {
						int at = (lo * n + hi) * n + last;
						if (best[at] != 0) {
							continue;
						}
						int[] left = without(lo, hi, last, -1);
						if (left == null || left[0] == left[1]) {
							next[at] = 1;
							continue;
						}
						for (int guess = 1; guess <= max && next[at] == 0 && k > 1; guess++) {
							if (guess != last && solves(left[0], left[1], last, guess, best, n, k - 1)) {
								next[at] = (byte) k;
							}
						}
					}
				}
			}
			best = next;
			if (best[(n + max) * n] != 0) {
				return best[(n + max) * n];
			}
		}
		return -1;
	}

	/** lo to hi with the ends that are {@code a} or {@code b} taken off, or null if nothing is left. */
	private static int[] without(int lo, int hi, int a, int b) {
		while (lo <= hi && (lo == a || lo == b)) {
			lo++;
		}
		while (hi >= lo && (hi == a || hi == b)) {
			hi--;
		}
		return lo > hi ? null : new int[] { lo, hi };
	}

	/** True if every answer to {@code guess} leaves a state solved within {@code k} more guesses. */
	private static boolean solves(int lo, int hi, int last, int guess, byte[] best, int n, int k) {
		int twice = guess + last;
		// No last guess: the only answer is "not it". Otherwise below the halfway point, above it, and on it.
		int[][] parts = last == 0 ? new int[][] { { lo, hi } }
				: new int[][] { { lo, Math.min(hi, (twice - 1) / 2) }, { Math.max(lo, twice / 2 + 1), hi },
						twice % 2 == 0 ? new int[] { twice / 2, twice / 2 } : new int[] { 1, 0 } };
		for (int[] part : parts) {
			int[] left = without(Math.max(part[0], lo), Math.min(part[1], hi), guess, last);
			if (left != null) {
				int needed = best[(left[0] * n + left[1]) * n + guess];
				if (needed == 0 || needed > k) {
					return false;
				}
			}
		}
		return true;
	}
}
