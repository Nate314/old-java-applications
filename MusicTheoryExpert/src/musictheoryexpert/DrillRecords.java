package musictheoryexpert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumMap;
import java.util.Map;

/**
 * The best score for each drill, kept in a small text file of {@code DRILL=stars,score} lines such as
 * {@code SCALES=2,9}. A drill never finished has no line. The first drill is always open; each other one opens
 * once the drill before it has a star.
 */
public final class DrillRecords {
	private final File file;
	private final Map<Drill, Integer> best = new EnumMap<>(Drill.class);

	private DrillRecords(File file) {
		this.file = file;
	}

	/** Reads the file; a missing or unreadable file, or a line this version does not know, is skipped. */
	public static DrillRecords load(File file) {
		DrillRecords records = new DrillRecords(file);
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			for (String line; (line = reader.readLine()) != null;) {
				int split = line.indexOf('=');
				String[] values = split > 0 ? line.substring(split + 1).split(",") : new String[0];
				if (values.length != 2) {
					continue;
				}
				try {
					Drill drill = Drill.valueOf(line.substring(0, split).trim());
					int score = Integer.parseInt(values[1].trim());
					if (score >= 0 && score <= Round.QUESTIONS) {
						records.best.put(drill, score);
					}
				} catch (IllegalArgumentException e) {
					// Not a record (NumberFormatException is one too): skip it.
				}
			}
		} catch (IOException e) {
			// No file yet: no drill finished so far.
		}
		return records;
	}

	/** The best score for {@code drill} (0 to 10), or -1 if it was never finished. */
	public int score(Drill drill) {
		Integer score = best.get(drill);
		return score == null ? -1 : score;
	}

	/** The best stars for {@code drill} (0 to 3). */
	public int stars(Drill drill) {
		return Round.starsFor(Math.max(0, score(drill)));
	}

	public int totalStars() {
		int total = 0;
		for (Drill drill : Drill.values()) {
			total += stars(drill);
		}
		return total;
	}

	public boolean isOpen(Drill drill) {
		return drill.ordinal() == 0 || stars(Drill.values()[drill.ordinal() - 1]) > 0;
	}

	/** The first open drill without three stars (the last drill when every one has them). */
	public Drill suggested() {
		for (Drill drill : Drill.values()) {
			if (isOpen(drill) && stars(drill) < 3) {
				return drill;
			}
		}
		return Drill.values()[Drill.values().length - 1];
	}

	/** Keeps a finished round if it beats the best one; returns true when it does. */
	public boolean add(Round round) {
		if (!round.isOver() || round.score() <= score(round.drill())) {
			return false;
		}
		best.put(round.drill(), round.score());
		save();
		return true;
	}

	private void save() {
		try (Writer writer = new FileWriter(file)) {
			for (Map.Entry<Drill, Integer> entry : best.entrySet()) {
				writer.write(entry.getKey().name() + "=" + Round.starsFor(entry.getValue()) + "," + entry.getValue() + "\n");
			}
		} catch (IOException e) {
			// Could not save: the record lasts until the window closes.
		}
	}
}
