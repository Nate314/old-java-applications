package numberguess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A won game sent to a friend: the range and every guess, so the friend plays the same number with the guesses to
 * beat, and sees both paths in hindsight afterwards. The code is the range's digit (0 for 1-10 up to 4 for
 * 1-1,000,000) and the guesses in base 36, scrambled so the number is not plain to see, joined by dashes. On the web it
 * rides in a link to the game's page ({@code ?challenge=2k3-1f-9}); the desktop app has no page, so it sends the code
 * alone, which it takes as its argument.
 */
final class Challenge {
	private static final String PARAM = "challenge=";
	// shortcut: the scramble only hides the number from a glance; anyone who reads this file can undo it. Fine for a friendly race.
	private static final long TIMES = 7, SHIFT = 389;

	private final Level level;
	private final List<Integer> guesses;

	private Challenge(Level level, List<Integer> guesses) {
		this.level = level;
		this.guesses = Collections.unmodifiableList(guesses);
	}

	Level level() {
		return level;
	}

	/** The friend's guesses; the last one is the number. */
	List<Integer> guesses() {
		return guesses;
	}

	int number() {
		return guesses.get(guesses.size() - 1);
	}

	/** The code for a won game. */
	static String encode(Game game) {
		long max = game.level().max();
		StringBuilder code = new StringBuilder().append(game.level().ordinal());
		for (int i = 0; i < game.guesses().size(); i++) {
			code.append(i == 0 ? "" : "-").append(Long.toString(((game.guesses().get(i) - 1) * TIMES + SHIFT * (i + 1)) % max, 36));
		}
		return code.toString();
	}

	/** The challenge in a code, or null unless it is a game won within the range's guesses. */
	static Challenge decode(String code) {
		if (code == null || code.length() < 2 || code.charAt(0) < '0' || code.charAt(0) >= '0' + Level.values().length) {
			return null;
		}
		Level level = Level.values()[code.charAt(0) - '0'];
		long max = level.max();
		long undo = 1;
		while (undo * TIMES % max != 1) {
			undo++;
		}
		String[] parts = code.substring(1).split("-");
		if (parts.length == 0 || parts.length > level.limit()) {
			return null;
		}
		List<Integer> guesses = new ArrayList<>();
		try {
			for (int i = 0; i < parts.length; i++) {
				long scrambled = Long.parseLong(parts[i], 36);
				if (scrambled < 0 || scrambled >= max) {
					return null;
				}
				guesses.add((int) (((scrambled - SHIFT * (i + 1) % max + max) % max * undo % max) + 1));
			}
		} catch (NumberFormatException e) {
			return null;
		}
		// Replay it: every guess must count, and the last must find the number.
		Game game = new Game(level, guesses.get(guesses.size() - 1));
		for (int guess : guesses) {
			Game.Verdict verdict = game.guess(guess);
			if (verdict != Game.Verdict.TOO_LOW && verdict != Game.Verdict.TOO_HIGH && verdict != Game.Verdict.CORRECT) {
				return null;
			}
		}
		return game.isFound() ? new Challenge(level, guesses) : null;
	}

	/** The code in a link (the value of challenge=) or the text itself, without spaces and in lower case. */
	static String codeIn(String text) {
		int at = text.indexOf(PARAM);
		String code = text;
		if (at >= 0) {
			code = text.substring(at + PARAM.length()).split("[&#]")[0];
		}
		return code.replaceAll("\\s", "").toLowerCase();
	}

	/** The address of the page a link points at (without its ? query or # part), or null if the text is not a web address. */
	static String pageOf(String text) {
		if (!text.startsWith("http://") && !text.startsWith("https://")) {
			return null;
		}
		return text.split("[?#]")[0];
	}

	/** What to send the friend: a link to the page with the game in it, or the code alone where there is no page. */
	static String message(String page, Game game) {
		return page == null ? encode(game) : page + "?" + PARAM + encode(game);
	}
}
