package musictheoryexpert;

import javax.swing.Timer;

import synthesizer.MySynthesizer;

/**
 * Plays a question's steps (notes that sound together) one after another on a Swing timer, so nothing waits on the
 * event thread. Starting a new play stops the one before it.
 */
public final class StepPlayer {
	/** Its own channel, so the Notes tab's demo (channels 0 and 1) never cuts these notes off. */
	private static final int CHANNEL = 2;
	private static final int VELOCITY = 100;

	/** Told which notes are sounding: the step's notes, then null when the play is over. */
	public interface Listener {
		void sounding(int[] notes);
	}

	private final MySynthesizer synth;
	private final Listener listener;
	private Timer timer;
	private int[] sounding;

	public StepPlayer(MySynthesizer synth, Listener listener) {
		this.synth = synth;
		this.listener = listener;
	}

	public void play(int[][] steps, int stepMillis) {
		stop();
		playStep(steps, 0, stepMillis);
	}

	private void playStep(int[][] steps, int step, int stepMillis) {
		release();
		if (step == steps.length) {
			timer = null;
			listener.sounding(null);
			return;
		}
		sounding = steps[step];
		for (int note : sounding) {
			synth.turnOnNote(CHANNEL, note, VELOCITY);
		}
		listener.sounding(sounding);
		timer = new Timer(step == steps.length - 1 ? Drill.LAST_STEP_MILLIS : stepMillis,
				event -> playStep(steps, step + 1, stepMillis));
		timer.setRepeats(false);
		timer.start();
	}

	public boolean isPlaying() {
		return timer != null;
	}

	public void stop() {
		if (timer != null) {
			timer.stop();
			timer = null;
			release();
			listener.sounding(null);
		}
	}

	private void release() {
		if (sounding != null) {
			for (int note : sounding) {
				synth.turnOffNote(CHANNEL, note);
			}
			sounding = null;
		}
	}
}
