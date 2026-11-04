package simplealgorithms;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

/**
 * The Sorting race's sound: one short note per tick for the sort being listened to, pitched by the value it just
 * touched, so a sort can be heard as well as seen. The synthesizer opens on a background thread the first time a
 * note is wanted; until then (or without one) the race is silent.
 */
final class SortSound {
	private static final int VIBRAPHONE = 11;
	private static final int LOW = 45;
	private static final int RANGE = 48;

	private volatile MidiChannel channel;
	private boolean opening;
	private boolean on = true;
	private int playing = -1;

	boolean isOn() {
		return on;
	}

	void toggle() {
		on = !on;
		if (!on) {
			silence();
		}
	}

	/** Plays the note for a value from 1 to max, stopping the one before it. */
	void play(int value, int max) {
		if (!on) {
			return;
		}
		MidiChannel ch = channel;
		if (ch == null) {
			open();
			return;
		}
		int note = LOW + (int) Math.round((double) Math.max(0, value - 1) * RANGE / Math.max(1, max - 1));
		if (playing >= 0) {
			ch.noteOff(playing);
		}
		ch.noteOn(note, 70);
		playing = note;
	}

	void silence() {
		MidiChannel ch = channel;
		if (ch != null && playing >= 0) {
			ch.noteOff(playing);
		}
		playing = -1;
	}

	private void open() {
		if (opening) {
			return;
		}
		opening = true;
		Thread opener = new Thread(() -> {
			try {
				Synthesizer synthesizer = MidiSystem.getSynthesizer();
				synthesizer.open();
				MidiChannel first = synthesizer.getChannels()[0];
				first.programChange(VIBRAPHONE);
				channel = first;
			} catch (Exception e) {
				System.err.println("No sound: " + e.getMessage());
			}
		}, "sort-sound");
		opener.setDaemon(true);
		opener.start();
	}
}
