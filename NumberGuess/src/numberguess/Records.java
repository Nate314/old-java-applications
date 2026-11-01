package numberguess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumMap;
import java.util.Map;

/**
 * The fewest guesses each range was won in, kept in a small text file of {@code LEVEL=stars,guesses} lines such as
 * {@code HUNDRED=3,6}. A range never won has no line. The first range is always open; each other one opens once the
 * range before it has {@link #STARS_TO_OPEN} stars.
 */
public final class Records {
	public static final int STARS_TO_OPEN = 2;

	private final File file;
	private final Map<Level, Integer> best = new EnumMap<>(Level.class);

	private Records(File file) {
		this.file = file;
	}

	/** Reads the file; a missing or unreadable file, or a line this version does not know, is skipped. */
	public static Records load(File file) {
		Records records = new Records(file);
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			for (String line; (line = reader.readLine()) != null;) {
				int split = line.indexOf('=');
				String[] values = split > 0 ? line.substring(split + 1).split(",") : new String[0];
				if (values.length != 2) {
					continue;
				}
				try {
					Level level = Level.valueOf(line.substring(0, split).trim());
					int guesses = Integer.parseInt(values[1].trim());
					if (guesses >= 1 && guesses <= level.limit()) {
						records.best.put(level, guesses);
					}
				} catch (IllegalArgumentException e) {
					// Not a record (NumberFormatException is one too): skip it.
				}
			}
		} catch (IOException e) {
			// No file yet: no range won so far.
		}
		return records;
	}

	/** The fewest guesses {@code level} was won in, or 0 if it never was. */
	public int best(Level level) {
		Integer guesses = best.get(level);
		return guesses == null ? 0 : guesses;
	}

	public int stars(Level level) {
		int guesses = best(level);
		return guesses == 0 ? 0 : level.starsFor(guesses);
	}

	public int totalStars() {
		int total = 0;
		for (Level level : Level.values()) {
			total += stars(level);
		}
		return total;
	}

	public boolean isOpen(Level level) {
		return level.ordinal() == 0 || stars(Level.values()[level.ordinal() - 1]) >= STARS_TO_OPEN;
	}

	/** The first open range without three stars (the last range when every one has them). */
	public Level suggested() {
		for (Level level : Level.values()) {
			if (isOpen(level) && stars(level) < 3) {
				return level;
			}
		}
		return Level.values()[Level.values().length - 1];
	}

	/** Keeps a won game if it took fewer guesses than the best one; returns true when it did. */
	public boolean add(Game game) {
		int previous = best(game.level());
		if (!game.isFound() || (previous != 0 && game.getGuesses() >= previous)) {
			return false;
		}
		best.put(game.level(), game.getGuesses());
		save();
		return true;
	}

	private void save() {
		try (Writer writer = new FileWriter(file)) {
			for (Map.Entry<Level, Integer> entry : best.entrySet()) {
				writer.write(entry.getKey().name() + "=" + entry.getKey().starsFor(entry.getValue()) + "," + entry.getValue() + "\n");
			}
		} catch (IOException e) {
			// Could not save: the record lasts until the window closes.
		}
	}
}
