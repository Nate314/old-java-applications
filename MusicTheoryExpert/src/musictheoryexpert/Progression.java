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
	/** Eighth notes in a bar: a melody note starts on one of them, its slot being {@code bar * 8 + eighth}. */
	public static final int EIGHTHS = 8;
	/** The longest melody note, in eighths: one bar. */
	public static final int LONGEST_NOTE = 8;

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
	/**
	 * The melody played over the loop, as {slot, pitch, length in eighths}, the pitch kept as if the key's major were C
	 * major, so it moves round the circle with the chords and stays put for the relative minor, which has the same notes.
	 */
	private final List<int[]> melody = new ArrayList<>();

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

	// ---- the melody ----

	/** How far the major key at {@code position} sits from C: -5 (G) to +6 (F#), so a melody moves the shorter way. */
	static int shift(int position) {
		int tonic = tonicAt(position, false);
		return tonic > 6 ? tonic - 12 : tonic;
	}

	/** Adds a note starting at {@code slot}, in place of one already there at the same pitch. */
	public void addNote(int slot, int pitch, int length) {
		if (slot < 0 || slot >= MAX_CHORDS * EIGHTHS || pitch < 0 || pitch > 127) {
			return;
		}
		int inC = pitch - shift(position);
		melody.removeIf(note -> note[0] == slot && note[1] == inC);
		melody.add(new int[] { slot, inC, Math.max(1, Math.min(LONGEST_NOTE, length)) });
	}

	public void clearMelody() {
		melody.clear();
	}

	/** The melody in this key, as {slot, pitch, length}, each pitch an octave at a time into the keyboard's C3 to C7. */
	public List<int[]> melody() {
		List<int[]> notes = new ArrayList<>();
		for (int[] note : melody) {
			int pitch = note[1] + shift(position);
			while (pitch < Look.LOW_NOTE) {
				pitch += 12;
			}
			while (pitch > Look.HIGH_NOTE) {
				pitch -= 12;
			}
			notes.add(new int[] { note[0], pitch, note[2] });
		}
		return notes;
	}

	/** The melody's notes that start at {@code slot}, as {slot, pitch, length}. */
	public List<int[]> notesAt(int slot) {
		List<int[]> notes = melody();
		notes.removeIf(note -> note[0] != slot);
		return notes;
	}

	// ---- the song as a link ----

	/** Melody notes are written in these 64 characters, which a web address carries as they are. */
	private static final String DIGITS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
	/** A longer code is not one this tab wrote (the melody's 64 eighths could hold every note of the keyboard). */
	static final int MAX_CODE = 4000;

	/**
	 * The whole song in a few characters, for a link: the key's place on the circle (0 to b) and M or m, the chords
	 * as digits, then the tempo, the style and voicing, and the melody, four characters a note (its eighth, its pitch
	 * as if in C in two, its length), all separated by dots. The original demo is {@code 1M1451.100.00}.
	 */
	public String code() {
		StringBuilder code = new StringBuilder(Integer.toString(position, 12)).append(minor ? 'm' : 'M');
		for (int degree : degrees) {
			code.append(degree);
		}
		code.append('.').append(tempo).append('.').append(style.ordinal()).append(voicing.ordinal());
		if (!melody.isEmpty()) {
			code.append('.');
			for (int[] note : melody) {
				code.append(DIGITS.charAt(note[0])).append(DIGITS.charAt(note[1] / 64)).append(DIGITS.charAt(note[1] % 64))
						.append(DIGITS.charAt(note[2]));
			}
		}
		return code.toString();
	}

	/** The song in a code {@link #code} wrote, or null if the text is not one (nothing in it is trusted). */
	public static Progression fromCode(String code) {
		// Checked by hand: the browser build's regular expressions do not read every pattern the JDK's do.
		List<String> list = new ArrayList<>();
		for (int start = 0, dot; code != null && code.length() <= MAX_CODE && start <= code.length(); start = dot + 1) {
			dot = code.indexOf('.', start);
			dot = dot < 0 ? code.length() : dot;
			list.add(code.substring(start, dot));
		}
		list.add("");
		String[] parts = list.toArray(new String[0]);
		if (parts.length < 4 || parts.length > 5 || !parts[parts.length - 1].isEmpty()
				|| !only(parts[0], 0, 1, "0123456789ab") || !only(parts[0], 1, 2, "Mm") || parts[0].length() > 10
				|| !only(parts[0], 2, parts[0].length(), "1234567") || parts[1].length() > 3 || !only(parts[1], 0, parts[1].length(), "0123456789")
				|| parts[1].length() < 2 || parts[2].length() != 2 || !only(parts[2], 0, 1, "012") || !only(parts[2], 1, 2, "01")
				|| parts.length == 5 && (parts[3].length() % 4 != 0 || !only(parts[3], 0, parts[3].length(), DIGITS))) {
			return null;
		}
		Progression song = new Progression();
		song.degrees.clear();
		song.position = Integer.parseInt(parts[0].substring(0, 1), 12);
		song.minor = parts[0].charAt(1) == 'm';
		for (char degree : parts[0].substring(2).toCharArray()) {
			song.add(degree - '0');
		}
		int tempo = Integer.parseInt(parts[1]);
		if (tempo < MIN_TEMPO || tempo > MAX_TEMPO) {
			return null;
		}
		song.tempo = tempo;
		song.style = Style.values()[parts[2].charAt(0) - '0'];
		song.voicing = Voicing.values()[parts[2].charAt(1) - '0'];
		String notes = parts.length > 4 ? parts[3] : "";
		for (int i = 0; i < notes.length(); i += 4) {
			int slot = DIGITS.indexOf(notes.charAt(i)), pitch = DIGITS.indexOf(notes.charAt(i + 1)) * 64 + DIGITS.indexOf(notes.charAt(i + 2));
			int length = DIGITS.indexOf(notes.charAt(i + 3));
			if (pitch > 127 || length < 1 || length > LONGEST_NOTE) {
				return null;
			}
			song.melody.add(new int[] { slot, pitch, length });
		}
		return song;
	}

	/** True when {@code text} has characters {@code from} to {@code to} (at least one) and all of them are in {@code allowed}. */
	private static boolean only(String text, int from, int to, String allowed) {
		if (to > text.length() || from >= to) {
			return false;
		}
		for (int i = from; i < to; i++) {
			if (allowed.indexOf(text.charAt(i)) < 0) {
				return false;
			}
		}
		return true;
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
					} else if (name.equals("melody")) {
						for (String note : value.isEmpty() ? new String[0] : value.split(",")) {
							String[] parts = note.trim().split(":");
							try {
								int slot = Integer.parseInt(parts[0]), pitch = Integer.parseInt(parts[1]);
								int length = Math.max(1, Math.min(LONGEST_NOTE, Integer.parseInt(parts[2])));
								if (parts.length == 3 && slot >= 0 && slot < MAX_CHORDS * EIGHTHS && pitch >= 0 && pitch <= 127) {
									song.melody.add(new int[] { slot, pitch, length });
								}
							} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
								// Not a note: skip it and keep the rest.
							}
						}
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
		StringBuilder notes = new StringBuilder();
		for (int[] note : melody) {
			notes.append(notes.length() == 0 ? "" : ",").append(note[0] + ":" + note[1] + ":" + note[2]);
		}
		try (Writer writer = new FileWriter(file)) {
			writer.write("key=" + position + "\nminor=" + minor + "\nchords=" + chords + "\ntempo=" + tempo + "\nstyle="
					+ style.name() + "\nvoicing=" + voicing.name() + "\nmelody=" + notes + "\n");
		} catch (IOException e) {
			// Could not save: the song lasts until the window closes.
		}
	}
}
