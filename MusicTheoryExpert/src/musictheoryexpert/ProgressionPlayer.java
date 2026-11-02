package musictheoryexpert;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

import synthesizer.MySynthesizer;

/**
 * Plays the Songwriter's progression round and round, one bar of four beats per chord, on a one-shot Swing timer
 * set for each eighth note by the wall clock. It reads the song again at every bar, so a new key, chord, tempo or style is heard at once.
 * Chords play on MIDI channel 3 (piano) and the bass on channel 4 (acoustic bass), clear of the ear trainer's
 * channel 2 and the original demo's channels 0 and 1.
 */
public final class ProgressionPlayer {
	private static final int CHORD_CHANNEL = 3, BASS_CHANNEL = 4;
	private static final int ACOUSTIC_BASS = 32;
	private static final int PREVIEW_MILLIS = 800;
	/** Which note of the chord (3 is the lowest an octave up) each eighth of a Broken bar plays. */
	private static final int[] BROKEN = { 0, 1, 2, 3, 2, 1, 2, 1 };

	/** Told what is sounding: the bar's chord index (-1 for a preview or when stopped), its beat, and the notes. */
	public interface Listener {
		void sounding(int index, int beat, List<Integer> chord, List<Integer> bass);
	}

	private final MySynthesizer synth;
	private final Listener listener;
	private final List<Integer> chordNotes = new ArrayList<>();
	private final List<Integer> bassNotes = new ArrayList<>();
	private Progression song;
	private Timer timer;
	private int index;
	private int eighth;
	/** When the next eighth note is due, by the wall clock, so the beat keeps time however late a timer fires. */
	private long due;

	public ProgressionPlayer(MySynthesizer synth, Listener listener) {
		this.synth = synth;
		this.listener = listener;
		synth.setInstrument(BASS_CHANNEL, ACOUSTIC_BASS);
	}

	public boolean isPlaying() {
		return timer != null && song != null;
	}

	/** Loops {@code song} from its first chord. */
	public void play(Progression song) {
		stop();
		if (song.size() == 0) {
			return;
		}
		this.song = song;
		index = -1;
		eighth = 7;
		due = System.currentTimeMillis();
		tick();
	}

	/** Sounds one chord and its bass for a moment (the chord buttons, when the loop is not playing). */
	public void preview(int[] chord, int bass) {
		stop();
		release();
		for (int note : chord) {
			sound(chordNotes, CHORD_CHANNEL, note, 96);
		}
		sound(bassNotes, BASS_CHANNEL, bass, 96);
		report(-1, 0);
		timer = new Timer(PREVIEW_MILLIS, event -> stop());
		timer.setRepeats(false);
		timer.start();
	}

	public void stop() {
		if (timer != null) {
			timer.stop();
			timer = null;
		}
		song = null;
		release();
		listener.sounding(-1, 0, new ArrayList<>(), new ArrayList<>());
	}

	private int eighthMillis() {
		return 30000 / song.tempo();
	}

	private void tick() {
		if (song.size() == 0) {
			stop();
			return;
		}
		eighth = (eighth + 1) % 8;
		if (eighth == 0) {
			index = (index + 1) % song.size();
		}
		index = Math.min(index, song.size() - 1);
		int[] chord = song.voiced()[index];
		int bass = song.bassOf(song.degree(index));
		switch (song.style()) {
		case BLOCK:
			if (eighth == 0) {
				release();
				for (int note : chord) {
					sound(chordNotes, CHORD_CHANNEL, note, 92);
				}
				sound(bassNotes, BASS_CHANNEL, bass, 100);
			}
			break;
		case PULSE:
			if (eighth % 2 == 0) {
				release();
				for (int note : chord) {
					sound(chordNotes, CHORD_CHANNEL, note, eighth == 0 ? 100 : 76);
				}
				// Root on beats 1 and 2, the fifth above it on beat 3, back to the root on beat 4.
				sound(bassNotes, BASS_CHANNEL, eighth == 4 ? bass + 7 : bass, eighth == 0 ? 104 : 84);
			}
			break;
		default:
			release(chordNotes, CHORD_CHANNEL);
			if (eighth == 0) {
				release(bassNotes, BASS_CHANNEL);
				sound(bassNotes, BASS_CHANNEL, bass, 100);
			}
			int which = BROKEN[eighth];
			sound(chordNotes, CHORD_CHANNEL, which == 3 ? chord[0] + 12 : chord[which], eighth == 0 ? 100 : 84);
		}
		report(index, eighth / 2);
		if (song == null) {
			return; // stopped by the listener
		}
		long now = System.currentTimeMillis();
		due += eighthMillis();
		if (due < now) {
			// Far behind (the page was busy): carry on from now rather than rush to catch up.
			due = now;
		}
		timer = new Timer((int) Math.max(1, due - now), event -> tick());
		timer.setRepeats(false);
		timer.start();
	}

	private void sound(List<Integer> notes, int channel, int note, int velocity) {
		synth.turnOnNote(channel, note, velocity);
		notes.add(note);
	}

	private void release() {
		release(chordNotes, CHORD_CHANNEL);
		release(bassNotes, BASS_CHANNEL);
	}

	private void release(List<Integer> notes, int channel) {
		for (int note : notes) {
			synth.turnOffNote(channel, note);
		}
		notes.clear();
	}

	private void report(int chordIndex, int beat) {
		listener.sounding(chordIndex, beat, new ArrayList<>(chordNotes), new ArrayList<>(bassNotes));
	}
}
