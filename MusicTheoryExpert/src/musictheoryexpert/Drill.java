package musictheoryexpert;

import java.util.Random;

import musictheoryexpert.Notes.MusicalTerms;

/**
 * The ear training drills, easiest first. Each one plays a question (notes, chords or a scale, as MIDI note numbers)
 * and offers a few names to pick from; {@link #ask(Random)} makes a new question.
 */
public enum Drill {
	MAJOR_MINOR("Major or minor", "Is this chord major or minor?", 520, "Major", "Minor") {
		@Override
		Question ask(Random random) {
			int answer = random.nextInt(2);
			return triad(answer, randomRoot(random), TRIADS[answer]);
		}
	},
	CHORD_QUALITY("Chord qualities", "Which kind of chord is this?", 520, "Major", "Minor", "Diminished", "Augmented") {
		@Override
		Question ask(Random random) {
			int answer = random.nextInt(4);
			return triad(answer, randomRoot(random), TRIADS[answer]);
		}
	},
	BIG_INTERVALS("Five intervals", "How far apart are the two notes?", 640, "Major 2nd", "Major 3rd", "Perfect 4th",
			"Perfect 5th", "Octave") {
		@Override
		Question ask(Random random) {
			int answer = random.nextInt(5);
			return interval(answer, randomRoot(random), BIG_INTERVAL_STEPS[answer]);
		}
	},
	ALL_INTERVALS("All intervals", "Name the interval between the two notes.", 640, "Minor 2nd", "Major 2nd",
			"Minor 3rd", "Major 3rd", "Perfect 4th", "Tritone", "Perfect 5th", "Minor 6th", "Major 6th", "Minor 7th",
			"Major 7th", "Octave") {
		@Override
		Question ask(Random random) {
			int answer = random.nextInt(12);
			return interval(answer, randomRoot(random), answer + 1);
		}
	},
	SCALES("Scales", "Which scale is this?", 360, "Major", "Natural minor", "Harmonic minor", "Melodic minor") {
		@Override
		Question ask(Random random) {
			int answer = random.nextInt(4);
			int[] scale = Notes.getScale(randomKey(random), SCALE_TYPES[answer]);
			int[][] steps = new int[scale.length + 1][];
			for (int i = 0; i < scale.length; i++) {
				steps[i] = new int[] { scale[i] };
			}
			steps[scale.length] = new int[] { scale[0] + 12 };
			return new Question(answer, steps);
		}
	},
	CHORDS_IN_KEY("Chords in a key", "Home chord first. Which chord comes next?", 900, "I", "ii", "IV", "V", "vi") {
		@Override
		Question ask(Random random) {
			int answer = random.nextInt(5);
			int[] scale = Notes.getScale(randomKey(random), MusicalTerms.MAJOR);
			return new Question(answer, new int[][] { Notes.getTriadByRomanNumeral(scale, 1),
					Notes.getTriadByRomanNumeral(scale, DEGREES[answer]) });
		}
	};

	/** Third and fifth above the root of a major, minor, diminished and augmented triad, in half steps. */
	static final int[][] TRIADS = { { 4, 7 }, { 3, 7 }, { 3, 6 }, { 4, 8 } };
	static final int[] BIG_INTERVAL_STEPS = { 2, 4, 5, 7, 12 };
	static final MusicalTerms[] SCALE_TYPES = { MusicalTerms.MAJOR, MusicalTerms.NATURAL_MINOR,
			MusicalTerms.HARMONIC_MINOR, MusicalTerms.MELODIC_MINOR };
	static final int[] DEGREES = { 1, 2, 4, 5, 6 };
	/** Every step but the last sounds for the drill's step time; the last one rings for this long. */
	public static final int LAST_STEP_MILLIS = 1200;

	private final String title;
	private final String prompt;
	private final int stepMillis;
	private final String[] choices;

	Drill(String title, String prompt, int stepMillis, String... choices) {
		this.title = title;
		this.prompt = prompt;
		this.stepMillis = stepMillis;
		this.choices = choices;
	}

	/** A new question for this drill. */
	abstract Question ask(Random random);

	public String title() {
		return title;
	}

	public String prompt() {
		return prompt;
	}

	public int stepMillis() {
		return stepMillis;
	}

	public String choice(int index) {
		return choices[index];
	}

	public int choiceCount() {
		return choices.length;
	}

	/** G3 (55) to F#4 (66), so every question sits in a comfortable range. */
	static int randomRoot(Random random) {
		return 55 + random.nextInt(12);
	}

	static String randomKey(Random random) {
		return Notes.notesSharpBias[random.nextInt(12)];
	}

	/** The three notes one at a time, then together. */
	static Question triad(int answer, int root, int[] intervals) {
		int third = root + intervals[0];
		int fifth = root + intervals[1];
		return new Question(answer, new int[][] { { root }, { third }, { fifth }, { root, third, fifth } });
	}

	/** The two notes one after the other, then together. */
	static Question interval(int answer, int low, int halfSteps) {
		int high = low + halfSteps;
		return new Question(answer, new int[][] { { low }, { high }, { low, high } });
	}

	/** What a question plays (steps of notes sounding together) and the index of the right choice. */
	public static final class Question {
		private final int answer;
		private final int[][] steps;

		Question(int answer, int[][] steps) {
			this.answer = answer;
			this.steps = steps;
		}

		public int answer() {
			return answer;
		}

		public int[][] steps() {
			return steps;
		}
	}
}
