package musictheoryexpert;

import java.util.ArrayList;

import synthesizer.MySynthesizer;

public class Notes {
	public enum NoteNames {
		C(0, 0), Cs(1, 1), Df(1, 2), D(2, 3), Ds(3, 4), Ef(3, 5), E(4, 6), Es(5, 7), Ff(4, 8), F(5, 9), Fs(6, 10), Gf(6,
				11), G(7,
						12), Gs(8, 13), Af(8, 14), A(9, 15), As(10, 16), Bf(10, 17), B(11, 18), Bs(12, 19), Cf(11, 20);
		private int index;
		private int in;
		private String[] noteNames = { "C", "Cs", "Df", "D", "Ds", "Ef", "E", "Es", "Ff", "F", "Fs", "Gf", "G", "Gs",
				"Af", "A", "As", "Bf", "B", "Bs", "Cf" };

		NoteNames(int index, int in) {
			this.index = index;
			this.in = in;
		}

		int getIndex() {
			return index;
		}
		
		int getIndex(String note) {
			int result = -1;
			for(int i = 0; i < noteNames.length; i++) {
				if(noteNames[i] == note) result = i;
			}
			return result;
		}

		String getName() {
			return noteNames[in];
		}
		
		String getNameRelativeTo(String note) {
			int noteIndex = getIndex(note);
			int difference = index - noteIndex;
			System.out.println(difference);
			String extra = "";
			String sharpOrFlat = difference < 0 ? "f" : "s";
			for(int i = 0; i < Math.abs(difference); i++) {
				extra += sharpOrFlat;
			}
			return note + extra;
		}
	}

	public enum MusicalTerms {
		SHARP, FLAT, MAJOR, MINOR, NATURAL_MINOR, HARMONIC_MINOR, MELODIC_MINOR
	}

	public static String[] alphabetUpper = { "A", "B", "C", "D", "E", "F", "G" };
	public static String[] alphabetLower = { "a", "b", "c", "d", "e", "f", "g" };
	public static String[] notesSharpBias = { "C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs", "A", "As", "B" };
	public static String[] notesFlatBias = { "C", "Df", "D", "Ef", "E", "F", "Gf", "G", "Af", "A", "Bf", "B" };
	public static int[] majorSteps = { 2, 2, 1, 2, 2, 2, 1 };
	public static int[] naturalMinorSteps = { 2, 1, 2, 2, 1, 2, 2 };
	public static int[] harmonicMinorSteps = { 2, 1, 2, 2, 1, 3, 1 };
	public static int[] melodicMinorSteps = { 2, 1, 2, 2, 2, 2, 1 };

	public Notes(MySynthesizer synth) {
		System.out.println("Testing 123");
		for (int i = -20; i < 70; i++) {
			System.out.println("ID " + i + ": " + getNoteID(getNoteName(i, MusicalTerms.SHARP)));
		}
//		playScale(getScale("A", MusicalTerms.MAJOR), synth);
//		playScale(getScale("A", MusicalTerms.NATURAL_MINOR), synth);
//		playScale(getScale("A", MusicalTerms.HARMONIC_MINOR), synth);
//		playScale(getScale("A", MusicalTerms.MELODIC_MINOR), synth);
		Thread a = new Thread(new Runnable() {
			@Override
			public void run() {
				playChord(getTriadByRomanNumeral(getScale("G", MusicalTerms.MAJOR), 1), 1000, synth);
				playChord(getTriadByRomanNumeral(getScale("G", MusicalTerms.MAJOR), 4), 1000, synth);
				playChord(getTriadByRomanNumeral(getScale("G", MusicalTerms.MAJOR), 5), 1000, synth);
				playChord(getTriadByRomanNumeral(getScale("G", MusicalTerms.MAJOR), 1), 1000, synth);
			}
		});
		Thread b = new Thread(new Runnable() {
			@Override
			public void run() {
				playScale(getTriadByRomanNumeral(getScale("G", MusicalTerms.MAJOR), 1), 1000, synth);
			}
		});
		a.start();
		b.start();
//		System.out.println(NoteNames.C.getNameRelativeTo("D"));
//		System.out.println(NoteNames.C.getIndex("D"));
	}

	public void playScale(int[] scale, int length, MySynthesizer synth) {
		for (int i = 0; i < scale.length; i++) {
			System.out.println(getNoteName(scale[i], MusicalTerms.SHARP));
			playNote(scale[i], length, synth);
		}
		playNote(moveToOctave(scale[0], 5), length, synth);
	}
	
	public void playNote(int note, int length, MySynthesizer synth) {
		synth.playNotes(note, length, 0, 127, 0, 0);
	}

	public void playChord(int[] chord, int length, MySynthesizer synth) {
		ArrayList<Integer> chordAL = new ArrayList<Integer>();
		for(int i = 0; i < chord.length; i++) {
			chordAL.add(chord[i]);
		}
		synth.playChords(chordAL, length, 0, 127, 0, 0);
	}

	public int[] getScale(String key, MusicalTerms scaleType) {
		int[] scale = new int[7];
		int index = 0;
		if (scaleType == MusicalTerms.MAJOR) {
			for (int i = 0; i < scale.length; i++) {
				if (i != 0)
					index += majorSteps[i - 1];
				scale[i] = getNoteID(notesSharpBias[index]);
			}
		} else if (scaleType == MusicalTerms.NATURAL_MINOR) {
			for (int i = 0; i < scale.length; i++) {
				if (i != 0)
					index += naturalMinorSteps[i - 1];
				scale[i] = getNoteID(notesSharpBias[index]);
			}
		} else if (scaleType == MusicalTerms.HARMONIC_MINOR) {
			for (int i = 0; i < scale.length; i++) {
				if (i != 0)
					index += harmonicMinorSteps[i - 1];
				scale[i] = getNoteID(notesSharpBias[index]);
			}
		} else if (scaleType == MusicalTerms.MELODIC_MINOR) {
			for (int i = 0; i < scale.length; i++) {
				if (i != 0)
					index += melodicMinorSteps[i - 1];
				scale[i] = getNoteID(notesSharpBias[index]);
			}
		}
		int startingNote = 60 + getNoteID(key);
		for (int i = 0; i < scale.length; i++) {
			scale[i] += startingNote;
		}
		return scale;
	}

	public int getNoteID(String noteName) {
		int noteID = 400;
		for (int i = 0; i < notesSharpBias.length; i++) {
			if (noteName == notesSharpBias[i]) {
				noteID = i;
				break;
			}
		}
		if (noteID != 400)
			for (int i = 0; i < notesFlatBias.length; i++) {
				if (noteName == notesFlatBias[i]) {
					noteID = i;
					break;
				}
			}
		return noteID;
	}

	public int transpose(int note, int interval) {
		return note + interval;
	}

	public int moveToOctave(int note, int octave) {
		return (note % 12) + (12 * (octave + 1));
	}

	public String getNoteName(int noteID, MusicalTerms bias) {
		int myNoteID = (noteID < 0) ? 11 - ((-1 * noteID) % 12) : noteID % 12;
		return (bias == MusicalTerms.SHARP) ? notesSharpBias[myNoteID]
				: ((bias == MusicalTerms.FLAT) ? notesFlatBias[myNoteID] : "ERR");
	}

	public int[] getTriadByRomanNumeral(int[] scale, int romanNumeral) {
		int[] chord = new int[3];
		romanNumeral--;
		chord[0] = scale[romanNumeral % 7];
		chord[1] = scale[(romanNumeral + 2) % 7];
		chord[2] = scale[(romanNumeral + 4) % 7];
		return chord;
	}
//	
//	public int[] getTriadByRomanNumeral(int[] scale, String romanNumeral) {
//		int romanNumeralNumber = 0;
//		romanNumeral.split("");
//		return getTriadByRomanNumeral(scale, simplealgorithms.Conversions);
//	}
}
