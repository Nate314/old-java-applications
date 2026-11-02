package musictheoryexpert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import musictheoryexpert.Notes.MusicalTerms;

/**
 * The Songwriter tab's song: a key on the circle of fifths (major or its relative minor) and up to eight chords,
 * kept as scale degrees (1 to 7), so changing the key moves the whole progression with it. The chords themselves
 * come from the original code's {@link Notes#getScale} and {@link Notes#getTriadByRomanNumeral}.
 */
public final class Progression {
	public static final int MAX_CHORDS = 8;
	public static final int MIN_TEMPO = 60, MAX_TEMPO = 180;

	/** How a chord's notes are placed: as the original code builds them, or moved as little as possible. */
	public enum Voicing {
		SMOOTH("Smooth"), ROOT("Root position");

		private final String title;

		Voicing(String title) {
			this.title = title;
		}

		public String title() {
			return title;
		}
	}

	/** How each bar plays its chord. */
	public enum Style {
		BLOCK("Held"), PULSE("Beats"), ARPEGGIO("Broken");

		private final String title;

		Style(String title) {
			this.title = title;
		}

		public String title() {
			return title;
		}
	}

	/** A well-known progression; {@code minor} ones switch to the relative minor. */
	public enum Preset {
		ORIGINAL("The original demo", false, 1, 4, 5, 1),
		POP("Pop", false, 1, 5, 6, 4),
		FIFTIES("Fifties", false, 1, 6, 4, 5),
		JAZZ("Jazz ii-V-I", false, 2, 5, 1, 1),
		CANON("Pachelbel's Canon", false, 1, 5, 6, 3, 4, 1, 4, 5),
		MINOR("Minor loop", true, 1, 6, 3, 7);

		private final String title;
		private final boolean minor;
		private final int[] degrees;

		Preset(String title, boolean minor, int... degrees) {
			this.title = title;
			this.minor = minor;
			this.degrees = degrees;
		}

		public String title() {
			return title;
		}

		public boolean minor() {
			return minor;
		}

		public int[] degrees() {
			return degrees.clone();
		}
	}

	/** For each degree, the degrees that most often follow it (the usual chord progression map). */
	private static final int[][] NEXT = { {}, { 4, 5, 6 }, { 5, 7 }, { 6, 4 }, { 5, 1, 2 }, { 1, 6 }, { 2, 4 }, { 1 } };
	private static final String[] NUMERALS = { "", "I", "II", "III", "IV", "V", "VI", "VII" };
	/** The lowest and highest note a smoothly voiced chord may use (E3 to C6). */
	private static final int LOWEST = 52, HIGHEST = 84;

	/** 0 to 11: how many fifths up from C the key's major sits (0 = C, 1 = G, ..., 11 = F). */
	private int position = 1;
	private boolean minor;
	private final List<Integer> degrees = new ArrayList<>();
	private int tempo = 100;
	private Style style = Style.BLOCK;
	private Voicing voicing = Voicing.SMOOTH;

	/** G major, I-IV-V-I: the progression the original program played. */
	public Progression() {
		apply(Preset.ORIGINAL);
	}

	// ---- the key ----

	public int position() {
		return position;
	}

	public boolean isMinor() {
		return minor;
	}

	/** Moves to {@code position} on the circle (wrapping), major or its relative minor. */
	public void setKey(int position, boolean minor) {
		this.position = Math.floorMod(position, 12);
		this.minor = minor;
	}

	/** The key's home note, 0 (C) to 11 (B). */
	public int tonic() {
		return tonicAt(position, minor);
	}

	public static int tonicAt(int position, boolean minor) {
		return Math.floorMod(position * 7 + (minor ? 9 : 0), 12);
	}

	/** Keys past F# on the circle are written with flats; the rest with sharps. */
	public static boolean flatSide(int position) {
		return Math.floorMod(position, 12) > 6;
	}

	/** A note name such as "F#" or "Bb" for pitch class {@code pitch}, spelled for the key at {@code position}. */
	public static String noteName(int pitch, int position) {
		String name = Notes.getNoteName(pitch, flatSide(position) ? MusicalTerms.FLAT : MusicalTerms.SHARP);
		return name.length() == 1 ? name : name.charAt(0) + (name.charAt(1) == 's' ? "#" : "b");
	}

	/** For example "G major" or "E minor". */
	public static String keyName(int position, boolean minor) {
		String name = noteName(tonicAt(position, minor), position);
		return (minor ? name.toLowerCase().charAt(0) + name.substring(1) : name) + (minor ? " minor" : " major");
	}

	public String keyName() {
		String name = keyName(position, minor);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	/** For example "1 sharp", "3 flats" or "no sharps or flats". */
	public static String signature(int position) {
		int p = Math.floorMod(position, 12);
		int count = p <= 6 ? p : 12 - p;
		if (count == 0) {
			return "no sharps or flats";
		}
		return count + (p <= 6 ? " sharp" : " flat") + (count == 1 ? "" : "s");
	}

	/** The key's seven notes from the original code: tonic in the fourth octave (C4 to B4). */
	public int[] scale() {
		return Notes.getScale(Notes.notesSharpBias[tonic()], minor ? MusicalTerms.NATURAL_MINOR : MusicalTerms.MAJOR);
	}

	/** The root position triad on {@code degree} (1 to 7), built by the original code. */
	public int[] triad(int degree) {
		return Notes.getTriadByRomanNumeral(scale(), degree);
	}

	/** "", "m", "dim" or "aug" for the triad on {@code degree}. */
	public String quality(int degree) {
		int[] t = triad(degree);
		int third = t[1] - t[0], fifth = t[2] - t[0];
		return third == 4 ? (fifth == 8 ? "aug" : "") : (fifth == 6 ? "dim" : "m");
	}

	/** For example "Em" or "F#dim". */
	public String chordName(int degree) {
		return noteName(triad(degree)[0], position) + quality(degree);
	}

	/** For example "IV", "vi" or "vii°": upper case for major chords, lower case for minor and diminished. */
	public String numeral(int degree) {
		String quality = quality(degree);
		String numeral = NUMERALS[degree];
		if (quality.equals("m") || quality.equals("dim")) {
			numeral = numeral.toLowerCase();
		}
		return numeral + (quality.equals("dim") ? "°" : quality.equals("aug") ? "+" : "");
	}

	/** For example "C E G". */
	public String noteNames(int degree) {
		StringBuilder names = new StringBuilder();
		for (int note : triad(degree)) {
			names.append(names.length() == 0 ? "" : " ").append(noteName(note, position));
		}
		return names.toString();
	}

	/** The degrees that usually follow {@code degree}. */
	public static int[] usualNext(int degree) {
		return degree >= 1 && degree <= 7 ? NEXT[degree].clone() : new int[0];
	}

	// ---- the chords ----

	public int size() {
		return degrees.size();
	}

	public int degree(int index) {
		return degrees.get(index);
	}

	public int[] degrees() {
		int[] all = new int[degrees.size()];
		for (int i = 0; i < all.length; i++) {
			all[i] = degrees.get(i);
		}
		return all;
	}

	/** Adds a chord at the end; false when the progression is already full. */
	public boolean add(int degree) {
		if (degrees.size() >= MAX_CHORDS || degree < 1 || degree > 7) {
			return false;
		}
		degrees.add(degree);
		return true;
	}

	public void set(int index, int degree) {
		if (degree >= 1 && degree <= 7) {
			degrees.set(index, degree);
		}
	}

	public void remove(int index) {
		degrees.remove(index);
	}

	public void clear() {
		degrees.clear();
	}

	/** Takes the preset's chords, switching to the relative major or minor when the preset needs the other one. */
	public void apply(Preset preset) {
		minor = preset.minor();
		degrees.clear();
		for (int degree : preset.degrees()) {
			degrees.add(degree);
		}
	}

	/** The preset whose chords and mode these are, or null. */
	public Preset matchingPreset() {
		for (Preset preset : Preset.values()) {
			if (preset.minor() == minor && java.util.Arrays.equals(preset.degrees(), degrees())) {
				return preset;
			}
		}
		return null;
	}

	// ---- how it plays ----

	public int tempo() {
		return tempo;
	}

	public void setTempo(int tempo) {
		this.tempo = Math.max(MIN_TEMPO, Math.min(MAX_TEMPO, tempo));
	}

	public Style style() {
		return style;
	}

	public void setStyle(Style style) {
		this.style = style;
	}

	public Voicing voicing() {
		return voicing;
	}

	public void setVoicing(Voicing voicing) {
		this.voicing = voicing;
	}

	/**
	 * The notes each chord plays, in order: as the original code builds them, or each moved as little as it can from
	 * the one before (the first one stays as built, unless that reaches past C6).
	 */
	public int[][] voiced() {
		int[][] chords = new int[degrees.size()][];
		for (int i = 0; i < chords.length; i++) {
			int[] root = triad(degrees.get(i));
			chords[i] = voicing == Voicing.ROOT ? root : nearest(root, i == 0 ? root : chords[i - 1]);
		}
		return chords;
	}

	/** The bass note for the chord on {@code degree}: its root in the third octave (C3 to B3), whatever the voicing. */
	public int bassOf(int degree) {
		return 48 + Math.floorMod(triad(degree)[0], 12);
	}

	/**
	 * The close-position voicing of {@code triad}'s notes (any inversion, any octave between E3 and C6) whose notes
	 * move least from {@code previous}; ties go to the one nearest the middle of that range.
	 */
	static int[] nearest(int[] triad, int[] previous) {
		int[] best = triad;
		int bestCost = Integer.MAX_VALUE;
		int bestCenter = Integer.MAX_VALUE;
		for (int inversion = 0; inversion < 3; inversion++) {
			for (int low = LOWEST; low <= HIGHEST; low++) {
				if (Math.floorMod(low - triad[inversion], 12) != 0) {
					continue;
				}
				int[] candidate = new int[3];
				candidate[0] = low;
				for (int k = 1; k < 3; k++) {
					int pitch = triad[(inversion + k) % 3];
					candidate[k] = candidate[k - 1] + Math.floorMod(pitch - candidate[k - 1], 12);
				}
				if (candidate[2] > HIGHEST) {
					continue;
				}
				int cost = 0;
				for (int k = 0; k < 3; k++) {
					cost += Math.abs(candidate[k] - previous[k]);
				}
				int center = Math.abs(candidate[0] + candidate[1] + candidate[2] - 3 * 68);
				if (cost < bestCost || cost == bestCost && center < bestCenter) {
					best = candidate;
					bestCost = cost;
					bestCenter = center;
				}
			}
		}
		return best;
	}

	// ---- saving ----

	/** Reads a saved song; anything missing or unreadable keeps the default (G major, I-IV-V-I). */
	public static Progression load(File file) {
		Progression song = new Progression();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			for (String line; (line = reader.readLine()) != null;) {
				int split = line.indexOf('=');
				if (split <= 0) {
					continue;
				}
				String name = line.substring(0, split).trim();
				String value = line.substring(split + 1).trim();
				try {
					if (name.equals("key")) {
						song.position = Math.floorMod(Integer.parseInt(value), 12);
					} else if (name.equals("minor")) {
						song.minor = Boolean.parseBoolean(value);
					} else if (name.equals("chords")) {
						song.degrees.clear();
						for (String degree : value.isEmpty() ? new String[0] : value.split(",")) {
							song.add(Integer.parseInt(degree.trim()));
						}
					} else if (name.equals("tempo")) {
						song.setTempo(Integer.parseInt(value));
					} else if (name.equals("style")) {
						song.style = Style.valueOf(value);
					} else if (name.equals("voicing")) {
						song.voicing = Voicing.valueOf(value);
					}
				} catch (IllegalArgumentException e) {
					// Not a value this version knows: keep the default.
				}
			}
		} catch (IOException e) {
			// Nothing saved yet.
		}
		return song;
	}

	public void save(File file) {
		StringBuilder chords = new StringBuilder();
		for (int degree : degrees) {
			chords.append(chords.length() == 0 ? "" : ",").append(degree);
		}
		try (Writer writer = new FileWriter(file)) {
			writer.write("key=" + position + "\nminor=" + minor + "\nchords=" + chords + "\ntempo=" + tempo + "\nstyle="
					+ style.name() + "\nvoicing=" + voicing.name() + "\n");
		} catch (IOException e) {
			// Could not save: the song lasts until the window closes.
		}
	}
}
