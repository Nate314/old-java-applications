package numberguess;

import java.util.ArrayList;
import java.util.List;

/**
 * Looks back at a game: how much each guess narrowed what was possible, compared with guessing the middle, and the
 * guesses the middle (halving) search would have made for the same number.
 */
public final class Hindsight {

	/** What a guess did. HALVED: it was a middle guess. LUCKY: off the middle, but it left no more than the middle would. COSTLY: it left more. */
	public enum Read {
		HALVED, LUCKY, COSTLY
	}

	private Hindsight() {
	}

	/** Guess {@code guess} when {@code low} to {@code high} were possible and the number was {@code number}. */
	public static Read read(int low, int high, int guess, int number) {
		long count = (long) high - low + 1;
		// The middle guess (one of the two middles when the count is even) never leaves more than count / 2.
		if (guess == low + (count - 1) / 2 || guess == low + count / 2) {
			return Read.HALVED;
		}
		long left = guess == number ? 0 : guess < number ? (long) high - guess : (long) guess - low;
		return left <= count / 2 ? Read.LUCKY : Read.COSTLY;
	}

	/** How many numbers were still possible before the first guess and after each one: {@code guesses.size() + 1} counts. */
	public static List<Long> left(Level level, int number, List<Integer> guesses) {
		Game game = new Game(level, number, Integer.MAX_VALUE);
		List<Long> left = new ArrayList<>();
		left.add((long) level.max());
		for (int guess : guesses) {
			game.guess(guess);
			left.add(game.isFound() ? 0 : (long) game.high() - game.low() + 1);
		}
		return left;
	}

	/** The read of each guess, in order. */
	public static List<Read> reads(Level level, int number, List<Integer> guesses) {
		Game game = new Game(level, number, Integer.MAX_VALUE);
		List<Read> reads = new ArrayList<>();
		for (int guess : guesses) {
			reads.add(read(game.low(), game.high(), guess, number));
			game.guess(guess);
		}
		return reads;
	}

	/** The guesses that always guessing the middle makes to find {@code number}. */
	public static List<Integer> halving(Level level, int number) {
		Game game = new Game(level, number, Integer.MAX_VALUE);
		while (!game.isFound()) {
			game.guess((game.low() + game.high()) / 2);
		}
		return game.guesses();
	}
}
