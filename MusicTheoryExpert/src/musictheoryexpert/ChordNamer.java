package musictheoryexpert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Names the notes held on the Chord finder: which chord they make (with its inversion and spelling), the other names
 * the same notes go by, the keys the chord belongs to, and the keys a list of chords fits. Pitch classes are 0 (C)
 * to 11 (B); a set of them is a 12-bit mask.
 */
public final class ChordNamer {
	/** Natural notes' pitch classes, by letter (C D E F G A B). */
	private static final int[] LETTER_PITCH = { 0, 2, 4, 5, 7, 9, 11 };
	private static final String LETTERS = "CDEFGAB";
	/** The major scale's step above the root for degrees 1 to 13 (index 0 unused), to name each chord tone. */
	private static final int[] DEGREE_STEP = { 0, 0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21 };
	private static final String[] NUMERALS = { "", "I", "II", "III", "IV", "V", "VI", "VII" };
	private static final int[] MAJOR_STEPS = { 0, 2, 4, 5, 7, 9, 11 };
	private static final int[] NATURAL_MINOR_STEPS = { 0, 2, 3, 5, 7, 8, 10 };
	private static final int[] HARMONIC_MINOR_STEPS = { 0, 2, 3, 5, 7, 8, 11 };
	private static final String[] INTERVALS = { "Octave", "Minor 2nd", "Major 2nd", "Minor 3rd", "Major 3rd",
			"Perfect 4th", "Tritone", "Perfect 5th", "Minor 6th", "Major 6th", "Minor 7th", "Major 7th" };
	/** The letter step (degree) each interval's top note is written on, so a tritone from C is F#, not Gb. */
	private static final int[] INTERVAL_DEGREES = { 1, 2, 2, 3, 3, 4, 4, 5, 6, 6, 7, 7 };

	/** A chord type: its symbol after the root, its name, and its notes as steps above the root and degrees. */
	public enum Quality {
		MAJOR("", "major", new int[] { 0, 4, 7 }, 1, 3, 5),
		MINOR("m", "minor", new int[] { 0, 3, 7 }, 1, 3, 5),
		DOMINANT_7("7", "dominant seventh", new int[] { 0, 4, 7, 10 }, 1, 3, 5, 7),
		MAJOR_7("maj7", "major seventh", new int[] { 0, 4, 7, 11 }, 1, 3, 5, 7),
		MINOR_7("m7", "minor seventh", new int[] { 0, 3, 7, 10 }, 1, 3, 5, 7),
		DIMINISHED("dim", "diminished", new int[] { 0, 3, 6 }, 1, 3, 5),
		AUGMENTED("aug", "augmented", new int[] { 0, 4, 8 }, 1, 3, 5),
		SUS4("sus4", "suspended fourth", new int[] { 0, 5, 7 }, 1, 4, 5),
		SUS2("sus2", "suspended second", new int[] { 0, 2, 7 }, 1, 2, 5),
		HALF_DIMINISHED("m7b5", "half-diminished seventh", new int[] { 0, 3, 6, 10 }, 1, 3, 5, 7),
		DIMINISHED_7("dim7", "diminished seventh", new int[] { 0, 3, 6, 9 }, 1, 3, 5, 7),
		SIXTH("6", "major sixth", new int[] { 0, 4, 7, 9 }, 1, 3, 5, 6),
		MINOR_6("m6", "minor sixth", new int[] { 0, 3, 7, 9 }, 1, 3, 5, 6),
		ADD9("add9", "added ninth", new int[] { 0, 2, 4, 7 }, 1, 9, 3, 5),
		NINTH("9", "dominant ninth", new int[] { 0, 2, 4, 7, 10 }, 1, 9, 3, 5, 7),
		MINOR_MAJOR_7("m(maj7)", "minor major seventh", new int[] { 0, 3, 7, 11 }, 1, 3, 5, 7),
		SEVEN_SUS4("7sus4", "seventh, suspended fourth", new int[] { 0, 5, 7, 10 }, 1, 4, 5, 7),
		MINOR_ADD9("m(add9)", "minor, added ninth", new int[] { 0, 2, 3, 7 }, 1, 9, 3, 5),
		AUGMENTED_7("aug7", "augmented seventh", new int[] { 0, 4, 8, 10 }, 1, 3, 5, 7),
		MAJOR_9("maj9", "major ninth", new int[] { 0, 2, 4, 7, 11 }, 1, 9, 3, 5, 7),
		MINOR_9("m9", "minor ninth", new int[] { 0, 2, 3, 7, 10 }, 1, 9, 3, 5, 7),
		SIX_NINE("6/9", "six-nine", new int[] { 0, 2, 4, 7, 9 }, 1, 9, 3, 5, 6),
		POWER("5", "power chord", new int[] { 0, 7 }, 1, 5);

		private final String symbol;
		private final String title;
		private final int[] steps;
		private final int[] degrees;

		Quality(String symbol, String title, int[] steps, int... degrees) {
			this.symbol = symbol;
			this.title = title;
			this.steps = steps;
			this.degrees = degrees;
		}

		public String symbol() {
			return symbol;
		}

		public String title() {
			return title;
		}

		public int[] steps() {
			return steps.clone();
		}

		/** Has a perfect fifth that can be left out (a chord of four or more notes). */
		boolean fifthOptional() {
			return steps.length >= 4 && contains(steps, 7);
		}

		/** Minor third and no major third: written with a lower case numeral. */
		boolean minorThird() {
			return contains(steps, 3) && !contains(steps, 4);
		}
	}

	/** One way to name a set of notes: a chord on {@code root} with {@code bass} lowest. */
	public static final class Reading {
		public final int root;
		public final Quality quality;
		public final int bass;
		/** The fifth is not held. */
		public final boolean noFifth;
		/** The written name of each pitch class the chord holds, by pitch class (null for the others). */
		private final String[] spelled;
		/** Each chord tone's label ("R", "b3", "5", "b7"...), by pitch class (null for the others). */
		private final String[] labels;

		Reading(int root, Quality quality, int bass, boolean noFifth) {
			this.root = root;
			this.quality = quality;
			this.bass = bass;
			this.noFifth = noFifth;
			this.spelled = spell(root, quality.steps, quality.degrees);
			this.labels = new String[12];
			for (int i = 0; i < quality.steps.length; i++) {
				labels[(root + quality.steps[i]) % 12] = toneLabel(quality.steps[i], quality.degrees[i]);
			}
		}

		/** For example "C", "F#m7" or "C/E". */
		public String name() {
			String name = spelled[root] + quality.symbol;
			return bass == root ? name : name + "/" + spelled[bass];
		}

		/** For example "C major, first inversion" or "G dominant seventh, no fifth". */
		public String description() {
			StringBuilder text = new StringBuilder(spelled[root]).append(' ').append(quality.title);
			if (noFifth) {
				text.append(", no fifth");
			}
			if (bass != root) {
				int degree = degreeOf(bass);
				String inversion = degree == 3 ? "first inversion" : degree == 5 ? "second inversion"
						: degree == 7 ? "third inversion" : spelled[bass] + " in the bass";
				text.append(", ").append(inversion);
			} else if (quality.steps.length > 2) {
				text.append(", root position");
			}
			return text.toString();
		}

		private int degreeOf(int pitch) {
			for (int i = 0; i < quality.steps.length; i++) {
				if ((root + quality.steps[i]) % 12 == pitch) {
					return quality.degrees[i];
				}
			}
			return 0;
		}

		/** The written name of pitch class {@code pitch} in this chord, or null when the chord does not hold it. */
		public String spelling(int pitch) {
			return spelled[Math.floorMod(pitch, 12)];
		}

		/** The chord tone's label, such as "R", "3", "b7", or null. */
		public String label(int pitch) {
			return labels[Math.floorMod(pitch, 12)];
		}

		int mask() {
			return maskOf(root, quality.steps);
		}

		/** Lower is the more usual name: plain chords first, root in the bass, all notes held. */
		int rank() {
			return quality.ordinal() + (bass == root ? 0 : 8) + (noFifth ? 20 : 0);
		}
	}

	/** Where a chord sits in a key: the key (a circle of fifths position, as the Songwriter keeps it) and degree. */
	public static final class InKey {
		public final int position;
		public final boolean minor;
		/** 1 to 7 */
		public final int degree;
		public final String numeral;

		InKey(int position, boolean minor, int degree, String numeral) {
			this.position = position;
			this.minor = minor;
			this.degree = degree;
			this.numeral = numeral;
		}

		/** For example "V7 in C major". */
		public String text() {
			return numeral + " in " + keyName();
		}

		public String keyName() {
			String name = Progression.keyName(position, minor);
			return Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}
	}

	/** A key a list of chords fits, and how many of them it holds. */
	public static final class KeyFit {
		public final int position;
		public final boolean minor;
		public final int fits;
		/** Each chord's degree in the key's natural scale, or 0 when it is not in the key. */
		public final int[] degrees;

		KeyFit(int position, boolean minor, int fits, int[] degrees) {
			this.position = position;
			this.minor = minor;
			this.fits = fits;
			this.degrees = degrees;
		}

		public String keyName() {
			String name = Progression.keyName(position, minor);
			return Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}
	}

	private ChordNamer() {
	}

	// ---- naming held notes ----

	/** The pitch classes of {@code notes} (MIDI numbers), as a mask. */
	public static int maskOf(List<Integer> notes) {
		int mask = 0;
		for (int note : notes) {
			mask |= 1 << Math.floorMod(note, 12);
		}
		return mask;
	}

	static int maskOf(int root, int[] steps) {
		int mask = 0;
		for (int step : steps) {
			mask |= 1 << ((root + step) % 12);
		}
		return mask;
	}

	/** Every chord name for {@code notes} (MIDI numbers, any order), the most usual first; empty if none fits. */
	public static List<Reading> name(List<Integer> notes) {
		List<Reading> readings = new ArrayList<>();
		int mask = maskOf(notes);
		if (Integer.bitCount(mask) < 2) {
			return readings;
		}
		int bass = Math.floorMod(Collections.min(notes), 12);
		for (int root = 0; root < 12; root++) {
			if ((mask & 1 << root) == 0) {
				continue;
			}
			for (Quality quality : Quality.values()) {
				int full = maskOf(root, quality.steps);
				if (full == mask) {
					readings.add(new Reading(root, quality, bass, false));
				} else if (quality.fifthOptional() && (full & ~(1 << (root + 7) % 12)) == mask) {
					readings.add(new Reading(root, quality, bass, true));
				}
			}
		}
		readings.sort((a, b) -> Integer.compare(a.rank(), b.rank()));
		return readings;
	}

	/** For two pitch classes: "Major 3rd: C up to E", from the lowest note up. Null for any other count. */
	public static String interval(List<Integer> notes) {
		int mask = maskOf(notes);
		if (Integer.bitCount(mask) != 2) {
			return null;
		}
		int low = Math.floorMod(Collections.min(notes), 12);
		int high = Integer.numberOfTrailingZeros(mask & ~(1 << low));
		int size = Math.floorMod(high - low, 12);
		String[] names = spellings(notes);
		return INTERVALS[size] + ": " + names[low] + " up to " + names[high];
	}

	/**
	 * How to write each pitch class of {@code notes}, by pitch class: as the chord's best name spells it, as an
	 * interval for two, else each note's usual name.
	 */
	public static String[] spellings(List<Integer> notes) {
		List<Reading> readings = name(notes);
		String[] names = new String[12];
		int mask = maskOf(notes);
		if (!readings.isEmpty()) {
			for (int pitch = 0; pitch < 12; pitch++) {
				names[pitch] = readings.get(0).spelling(pitch);
			}
		} else if (Integer.bitCount(mask) == 2) {
			int low = Math.floorMod(Collections.min(notes), 12);
			int size = Math.floorMod(Integer.numberOfTrailingZeros(mask & ~(1 << low)) - low, 12);
			names = spell(low, new int[] { 0, size }, new int[] { 1, INTERVAL_DEGREES[size] });
		}
		for (int pitch = 0; pitch < 12; pitch++) {
			if (names[pitch] == null && (mask & 1 << pitch) != 0) {
				names[pitch] = noteName(pitch);
			}
		}
		return names;
	}

	/** The usual written name of one pitch class: "C", "F#", "Bb". */
	public static String noteName(int pitch) {
		return spell(Math.floorMod(pitch, 12), new int[] { 0 }, new int[] { 1 })[Math.floorMod(pitch, 12)];
	}

	/**
	 * Names each note of a chord on {@code root} by letter: the degree sets the letter (a third is two letters up), so
	 * Ab major is Ab C Eb, never G# C D#. Of the root's two names, the one with fewer sharps and flats wins.
	 */
	static String[] spell(int root, int[] steps, int[] degrees) {
		String[] best = null;
		int bestCost = Integer.MAX_VALUE;
		for (int letter = 0; letter < 7; letter++) {
			int rootAccidental = signed(root - LETTER_PITCH[letter]);
			if (Math.abs(rootAccidental) > 1) {
				continue;
			}
			String[] names = new String[12];
			int cost = 0;
			for (int i = 0; i < steps.length; i++) {
				int pitch = (root + steps[i]) % 12;
				int toneLetter = (letter + degrees[i] - 1) % 7;
				int accidental = signed(pitch - LETTER_PITCH[toneLetter]);
				cost += Math.abs(accidental) + (Math.abs(accidental) > 1 ? 4 : 0);
				names[pitch] = LETTERS.charAt(toneLetter) + accidentals(accidental);
			}
			// Ties go to the usual root names: Db, Eb, F#, Ab, Bb.
			boolean usual = rootAccidental == 0 || (rootAccidental < 0) == (root != 6);
			if (cost < bestCost || cost == bestCost && usual) {
				best = names;
				bestCost = cost;
			}
		}
		return best;
	}

	private static boolean contains(int[] values, int value) {
		for (int v : values) {
			if (v == value) {
				return true;
			}
		}
		return false;
	}

	private static int signed(int difference) {
		int d = Math.floorMod(difference, 12);
		return d > 6 ? d - 12 : d;
	}

	private static String accidentals(int count) {
		return count == 0 ? "" : count == 1 ? "#" : count == 2 ? "##" : count == -1 ? "b" : "bb";
	}

	/** "R" for the root, else the degree with b or # for how far it is from the major scale's: "b3", "5", "#5". */
	static String toneLabel(int step, int degree) {
		if (degree == 1) {
			return "R";
		}
		int difference = step + (degree > 7 ? 12 : 0) - DEGREE_STEP[degree];
		return accidentals(difference) + degree;
	}

	/** The notes of {@code quality} on {@code root} in root position, from the root between F3 and E4. */
	public static List<Integer> build(int root, Quality quality) {
		int base = 48 + Math.floorMod(root, 12);
		if (base < 53) {
			base += 12;
		}
		List<Integer> notes = new ArrayList<>();
		for (int step : quality.steps) {
			notes.add(base + step);
		}
		return notes;
	}

	// ---- keys ----

	private static int[] scaleSteps(boolean minor, boolean harmonic) {
		return !minor ? MAJOR_STEPS : harmonic ? HARMONIC_MINOR_STEPS : NATURAL_MINOR_STEPS;
	}

	/** The Songwriter's circle position for a key on {@code tonic}. */
	static int positionOf(int tonic, boolean minor) {
		for (int position = 0; position < 12; position++) {
			if (Progression.tonicAt(position, minor) == tonic) {
				return position;
			}
		}
		throw new IllegalArgumentException("tonic " + tonic);
	}

	/**
	 * The degree (1 to 7) of the chord's root in the key on {@code tonic}, when every note of the chord is in the key's
	 * scale (for minor keys, natural or harmonic minor, which gives the major V and the diminished vii°); else 0.
	 */
	static int degreeIn(int tonic, boolean minor, int root, Quality quality) {
		int mask = maskOf(root, quality.steps);
		for (int pass = 0; pass < (minor ? 2 : 1); pass++) {
			int[] scale = scaleSteps(minor, pass == 1);
			int scaleMask = maskOf(tonic, scale);
			if ((mask & ~scaleMask) != 0) {
				continue;
			}
			for (int d = 0; d < 7; d++) {
				if ((tonic + scale[d]) % 12 == root && (pass == 0 || harmonicChord(d + 1, quality))) {
					return d + 1;
				}
			}
		}
		return 0;
	}

	/**
	 * The chords a minor key borrows from harmonic minor, its raised seventh making them: the major V (and V7), the
	 * diminished vii° (and vii°7, viiø7) and the augmented III+.
	 */
	private static boolean harmonicChord(int degree, Quality quality) {
		int[] steps = quality.steps;
		return degree == 5 && contains(steps, 4) && contains(steps, 7)
				|| degree == 7 && contains(steps, 3) && contains(steps, 6)
				|| degree == 3 && contains(steps, 4) && contains(steps, 8);
	}

	/** For example "V7", "ii", "vii°7", "IVmaj7", "iiø7", "III+". */
	static String numeral(int degree, Quality quality) {
		String numeral = NUMERALS[degree];
		if (quality.minorThird()) {
			numeral = numeral.toLowerCase();
		}
		switch (quality) {
		case MAJOR:
		case MINOR:
			return numeral;
		case DIMINISHED:
			return numeral + "°";
		case AUGMENTED:
			return numeral + "+";
		case HALF_DIMINISHED:
			return numeral + "ø7";
		case DIMINISHED_7:
			return numeral + "°7";
		default:
			String symbol = quality.symbol;
			return numeral + (quality.minorThird() && symbol.startsWith("m") ? symbol.substring(1) : symbol);
		}
	}

	/** Every key the chord belongs to, the home chords first, then V, IV, vi, ii, iii and vii; major keys first. */
	public static List<InKey> keysOf(int root, Quality quality) {
		List<InKey> keys = new ArrayList<>();
		String order = "1546237";
		for (int i = 0; i < order.length(); i++) {
			for (int mode = 0; mode < 2; mode++) {
				boolean minor = mode == 1;
				for (int tonic = 0; tonic < 12; tonic++) {
					int degree = degreeIn(tonic, minor, root, quality);
					if (degree == order.charAt(i) - '0') {
						keys.add(new InKey(positionOf(tonic, minor), minor, degree, numeral(degree, quality)));
					}
				}
			}
		}
		return keys;
	}

	/**
	 * The keys that hold the most of {@code chords} (each a root and quality), best first, at most {@code count}.
	 * Ties go to a key whose home chord is the first or last chord, then to major keys and fewer sharps or flats.
	 */
	public static List<KeyFit> fitKeys(List<Reading> chords, int count) {
		List<KeyFit> fits = new ArrayList<>();
		List<Integer> scores = new ArrayList<>();
		if (chords.isEmpty()) {
			return fits;
		}
		for (int mode = 0; mode < 2; mode++) {
			boolean minor = mode == 1;
			for (int tonic = 0; tonic < 12; tonic++) {
				int[] degrees = new int[chords.size()];
				int fit = 0;
				for (int i = 0; i < chords.size(); i++) {
					Reading chord = chords.get(i);
					degrees[i] = degreeIn(tonic, minor, chord.root, chord.quality);
					fit += degrees[i] > 0 ? 1 : 0;
				}
				if (fit == 0) {
					continue;
				}
				int position = positionOf(tonic, minor);
				int score = fit * 100;
				if (homeChord(chords.get(0), tonic, minor)) {
					score += 20;
				}
				if (homeChord(chords.get(chords.size() - 1), tonic, minor)) {
					score += 10;
				}
				score += (minor ? 0 : 5) - Math.min(position, 12 - position);
				int at = 0;
				while (at < scores.size() && scores.get(at) >= score) {
					at++;
				}
				scores.add(at, score);
				fits.add(at, new KeyFit(position, minor, fit, degrees));
			}
		}
		return fits.subList(0, Math.min(count, fits.size()));
	}

	private static boolean homeChord(Reading chord, int tonic, boolean minor) {
		return chord.root == tonic && chord.quality.minorThird() == minor && degreeIn(tonic, minor, chord.root, chord.quality) == 1;
	}
}
