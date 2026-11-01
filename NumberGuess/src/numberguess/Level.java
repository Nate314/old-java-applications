package numberguess;

/**
 * The five ranges to guess in, from 1 to 10 up to 1 to 1,000,000. Each has a par: the most guesses a search that
 * always guesses the middle of what is left can need. Par or better is three stars, two more is two, and the last
 * three guesses before running out are one.
 */
public enum Level {
	TEN(10), HUNDRED(100), THOUSAND(1000), TEN_THOUSAND(10000), MILLION(1000000);

	/** Guesses allowed beyond par before the number is given away. */
	public static final int SPARE = 3;

	private final int max;

	Level(int max) {
		this.max = max;
	}

	public int max() {
		return max;
	}

	/** The fewest guesses that always find the number: the bits needed to write {@code max}. */
	public int par() {
		return 32 - Integer.numberOfLeadingZeros(max);
	}

	public int limit() {
		return par() + SPARE;
	}

	/** Stars for finding the number in {@code guesses}: 3 at par or better, 2 up to par + 2, 1 up to the limit. */
	public int starsFor(int guesses) {
		if (guesses <= par()) {
			return 3;
		}
		if (guesses <= par() + 2) {
			return 2;
		}
		return guesses <= limit() ? 1 : 0;
	}

	/** "1-1,000" style, for the tabs. */
	public String label() {
		return "1-" + commas(max);
	}

	/** 1000000 as "1,000,000". */
	public static String commas(int n) {
		String digits = Integer.toString(Math.abs(n));
		StringBuilder out = new StringBuilder(n < 0 ? "-" : "");
		for (int i = 0; i < digits.length(); i++) {
			if (i > 0 && (digits.length() - i) % 3 == 0) {
				out.append(',');
			}
			out.append(digits.charAt(i));
		}
		return out.toString();
	}
}
