package musictheoryexpert;

import static musictheoryexpert.Look.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import musictheoryexpert.Progression.Preset;
import musictheoryexpert.Progression.Style;
import musictheoryexpert.Progression.Voicing;
import synthesizer.MySynthesizer;

/**
 * The Songwriter tab: pick a key on the circle of fifths, build a progression of up to eight chords from the key's
 * seven, and hear it loop with a bass line. The original program played one progression (I-IV-V-I in G major) and
 * the tab opens on it; the chords still come from the original scale and triad code. Melody mode (R) turns the
 * computer keyboard and the on-screen keys into a flute to play over the loop, and keeps what is played as a melody.
 */
@SuppressWarnings("serial")
public final class Songwriter extends JPanel {
	private static final String CHORD_KEYS = "1234567";
	/** Typed melody keys, as on a piano and as in the Chord finder: A is C, W is C#, S is D, up to ; (E an octave up). */
	private static final String PIANO_KEYS = "awsedftgyhujkolp;";
	private static final Color OUT_OF_KEY = new Color(150, 153, 162);
	private static final Color IN_KEY_BLACK = new Color(78, 84, 108);
	private static final Color MELODY_BLACK = new Color(40, 140, 76);
	private static final Color RECORD = new Color(232, 70, 70);
	private static final int TEMPO_STEP = 5;
	private static final int HEADER = 52;
	private static final Color SUGGESTED = new Color(255, 196, 60, 150);
	private static final Color BASS = new Color(120, 170, 255);
	private static final Color BASS_BLACK = new Color(60, 100, 200);
	private static final Color GOLD_BLACK = new Color(214, 150, 20);

	private final Progression song;
	private final File file;
	private final ProgressionPlayer player;
	/** The progression's chord being changed (the next chord picked replaces it), or -1. */
	private int selected = -1;
	/** The chord the loop is on, or -1. */
	private int playing = -1;
	private int beat;
	private List<Integer> sounding = new ArrayList<>();
	private List<Integer> soundingBass = new ArrayList<>();
	/** The chord last sounded (from the loop or a button), for the line under the buttons; 0 for none. */
	private int heardDegree;
	private String notice = "";
	/** Melody mode: the piano keys play the melody, and what is played while the loop plays is kept. */
	private boolean melodyMode;
	/** The typed keys' lowest note (A types it). */
	private int typedBase = 60;
	/** Melody notes held down, each with {the loop slot it started on (-1 when not looping), when it started}. */
	private final Map<Integer, long[]> held = new HashMap<>();
	private final Set<Integer> keysDown = new HashSet<>();
	/** The note held by the mouse, or -1. */
	private int mouseNote = -1;
	private int keysTop;

	// Where things were last drawn, for the mouse.
	private final List<Rectangle> chordButtons = new ArrayList<>();
	private final List<Rectangle> slots = new ArrayList<>();
	private final List<Rectangle> presetButtons = new ArrayList<>();
	private Rectangle playButton = new Rectangle(), slower = new Rectangle(), faster = new Rectangle();
	private Rectangle styleButton = new Rectangle(), voicingButton = new Rectangle(), clearButton = new Rectangle();
	private Rectangle melodyButton = new Rectangle(), melodyRoll = new Rectangle();
	private int circleX, circleY, radius;
	private Object hover;

	public Songwriter(MySynthesizer synth, File file) {
		this.file = file;
		this.song = Progression.load(file);
		this.player = new ProgressionPlayer(synth, (index, beat, chord, bass) -> {
			if (index >= 0 && !isShowing()) {
				// Its tab was left: the loop stops rather than play over the ear trainer.
				player().stop();
				return;
			}
			playing = index;
			this.beat = beat;
			sounding = chord;
			soundingBass = bass;
			if (index >= 0) {
				heardDegree = song.degree(index);
			}
			repaint();
		});
		setBackground(BACKGROUND);
		setFocusable(true);
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				requestFocusInWindow();
				int key = keyAt(event.getX(), event.getY());
				if (key >= 0) {
					setMelodyMode(true);
					mouseNote = key;
					press(key);
				} else {
					click(targetAt(event.getX(), event.getY()));
				}
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				if (mouseNote >= 0) {
					letGo(mouseNote);
					mouseNote = -1;
				}
			}

			@Override
			public void mouseMoved(MouseEvent event) {
				Object target = targetAt(event.getX(), event.getY());
				if (!same(target, hover)) {
					hover = target;
					setCursor(Cursor.getPredefinedCursor(target == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent event) {
				hover = null;
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	private ProgressionPlayer player() {
		return player;
	}

	public Progression song() {
		return song;
	}

	public boolean isPlaying() {
		return player.isPlaying();
	}

	// ---- what the controls do ----

	public void togglePlay() {
		notice = "";
		if (player.isPlaying()) {
			player.stop();
		} else if (song.size() == 0) {
			notice = "Add a chord first: click one above, or press 1 to 7.";
		} else {
			player.play(song);
		}
		repaint();
	}

	/** Adds the chord on {@code degree}, or puts it in place of the selected one. */
	public void pick(int degree) {
		notice = "";
		if (selected >= 0) {
			song.set(selected, degree);
			selected = -1;
		} else if (!song.add(degree)) {
			notice = "Eight chords is the most. Click one to change it, or Backspace removes the last.";
		}
		heardDegree = degree;
		if (!player.isPlaying()) {
			player.preview(song.triad(degree), song.bassOf(degree));
		}
		changed();
	}

	public void removeChord() {
		if (song.size() == 0) {
			return;
		}
		song.remove(selected >= 0 ? selected : song.size() - 1);
		selected = -1;
		if (song.size() == 0) {
			player.stop();
		}
		changed();
	}

	public void clearChords() {
		song.clear();
		song.clearMelody();
		selected = -1;
		player.stop();
		notice = "Cleared. Pick chords to start a new progression.";
		changed();
	}

	public void setKey(int position, boolean minor) {
		song.setKey(position, minor);
		notice = "";
		if (!player.isPlaying()) {
			heardDegree = 1;
			player.preview(song.triad(1), song.bassOf(1));
		}
		changed();
	}

	public void applyPreset(Preset preset) {
		song.apply(preset);
		selected = -1;
		notice = "";
		player.play(song);
		changed();
	}

	/** Writes {@code degrees} (1 to 7, up to eight) as the progression in the given key and starts it playing. */
	public void takeChords(int position, boolean minor, int[] degrees) {
		song.setKey(position, minor);
		song.clear();
		for (int degree : degrees) {
			song.add(degree);
		}
		selected = -1;
		notice = "";
		if (song.size() > 0) {
			player.play(song);
		} else {
			player.stop();
		}
		changed();
	}

	public void changeTempo(int by) {
		song.setTempo(song.tempo() + by);
		changed();
	}

	public void nextStyle() {
		song.setStyle(Style.values()[(song.style().ordinal() + 1) % Style.values().length]);
		changed();
	}

	public void nextVoicing() {
		song.setVoicing(Voicing.values()[(song.voicing().ordinal() + 1) % Voicing.values().length]);
		changed();
	}

	// ---- the melody ----

	public boolean isMelodyMode() {
		return melodyMode;
	}

	public void setMelodyMode(boolean on) {
		if (on == melodyMode) {
			return;
		}
		melodyMode = on;
		if (!on) {
			for (int note : new ArrayList<>(held.keySet())) {
				letGo(note);
			}
			keysDown.clear();
		}
		notice = "";
		repaint();
	}

	/** Starts a melody note: it sounds until let go, and is written where it started if the loop is playing. */
	public void press(int note) {
		if (note < LOW_NOTE || note > HIGH_NOTE || held.containsKey(note)) {
			return;
		}
		player.liveOn(note);
		held.put(note, new long[] { player.nearestSlot(), System.currentTimeMillis() });
		repaint();
	}

	/** Ends a melody note; one started while the loop played joins the melody, as long as it was held (in eighths). */
	public void letGo(int note) {
		long[] start = held.remove(note);
		if (start == null) {
			return;
		}
		player.liveOff(note);
		if (start[0] >= 0 && player.isPlaying()) {
			song.addNote((int) start[0], note, Math.round((System.currentTimeMillis() - start[1]) / (float) player.eighthMillis()));
			changed();
		} else {
			repaint();
		}
	}

	public void eraseMelody() {
		song.clearMelody();
		notice = "Melody erased. Play the loop and the keys again to write a new one.";
		changed();
	}

	public void shiftTyped(int octaves) {
		typedBase = Math.max(LOW_NOTE, Math.min(HIGH_NOTE - 16, typedBase + 12 * octaves));
		notice = "Typed keys now start at C" + (typedBase / 12 - 1) + ".";
		repaint();
	}

	/** Lets go of a typed melody key. */
	public void keyReleased(KeyEvent event) {
		if (keysDown.remove(event.getKeyCode())) {
			int piano = PIANO_KEYS.indexOf(Character.toLowerCase(event.getKeyChar()));
			if (piano >= 0) {
				letGo(typedBase + piano);
			}
		}
	}

	private void changed() {
		song.save(file);
		repaint();
	}

	/** Handles a key press on this tab; returns true when the key was used. */
	public boolean keyPressed(KeyEvent event) {
		int code = event.getKeyCode();
		char key = Character.toLowerCase(event.getKeyChar());
		int chord = CHORD_KEYS.indexOf(key);
		int piano = PIANO_KEYS.indexOf(key);
		if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
			return false;
		}
		if (melodyMode && piano >= 0 && key != KeyEvent.CHAR_UNDEFINED) {
			// A held key repeats; only its first press counts.
			if (keysDown.add(code)) {
				press(typedBase + piano);
			}
		} else if (melodyMode && (code == KeyEvent.VK_Z || code == KeyEvent.VK_X)) {
			shiftTyped(code == KeyEvent.VK_X ? 1 : -1);
		} else if (melodyMode && (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE)) {
			eraseMelody();
		} else if (code == KeyEvent.VK_R) {
			setMelodyMode(!melodyMode);
		} else if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ENTER) {
			togglePlay();
		} else if (chord >= 0 && key != KeyEvent.CHAR_UNDEFINED) {
			pick(chord + 1);
		} else if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
			removeChord();
		} else if (code == KeyEvent.VK_ESCAPE) {
			selected = -1;
			repaint();
		} else if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
			setKey(song.position() + (code == KeyEvent.VK_RIGHT ? 1 : -1), song.isMinor());
		} else if (code == KeyEvent.VK_M) {
			setKey(song.position(), !song.isMinor());
		} else if (key == '-' || code == KeyEvent.VK_DOWN) {
			changeTempo(-TEMPO_STEP);
		} else if (key == '+' || key == '=' || code == KeyEvent.VK_UP) {
			changeTempo(TEMPO_STEP);
		} else if (code == KeyEvent.VK_S) {
			nextStyle();
		} else if (code == KeyEvent.VK_V) {
			nextVoicing();
		} else if (code == KeyEvent.VK_C) {
			clearChords();
		} else if (code == KeyEvent.VK_P) {
			Preset current = song.matchingPreset();
			applyPreset(Preset.values()[current == null ? 0 : (current.ordinal() + 1) % Preset.values().length]);
		} else {
			return false;
		}
		return true;
	}

	private void click(Object target) {
		if (target == null) {
			return;
		}
		if (target instanceof Integer) {
			int code = (Integer) target;
			setKey(code % 100, code >= 200);
		} else if (target == playButton) {
			togglePlay();
		} else if (target == slower || target == faster) {
			changeTempo(target == faster ? TEMPO_STEP : -TEMPO_STEP);
		} else if (target == styleButton) {
			nextStyle();
		} else if (target == voicingButton) {
			nextVoicing();
		} else if (target == clearButton) {
			clearChords();
		} else if (target == melodyButton) {
			setMelodyMode(!melodyMode);
		} else if (chordButtons.contains(target)) {
			pick(chordButtons.indexOf(target) + 1);
		} else if (presetButtons.contains(target)) {
			applyPreset(Preset.values()[presetButtons.indexOf(target)]);
		} else if (slots.contains(target)) {
			int index = slots.indexOf(target);
			if (index >= song.size() || index == selected) {
				selected = -1;
			} else {
				selected = index;
				notice = "";
				int degree = song.degree(index);
				heardDegree = degree;
				if (!player.isPlaying()) {
					player.preview(song.voiced()[index], song.bassOf(degree));
				}
			}
			repaint();
		}
	}

	private static boolean same(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}

	// ---- layout ----

	private int rightX() {
		return circleX + radius + 28;
	}

	/** Works out where everything goes, so painting and the mouse agree. */
	private void layOut() {
		int width = getWidth(), height = getHeight();
		radius = Math.max(90, Math.min(140, Math.min((width - 380) / 2, (height - HEADER - 200) / 2)));
		circleX = 18 + radius;
		circleY = HEADER + 14 + radius;
		int x = rightX();
		int w = Math.max(280, width - x - 16);
		chordButtons.clear();
		int gap = 6;
		int cw = (w - gap * 6) / 7;
		for (int i = 0; i < 7; i++) {
			chordButtons.add(new Rectangle(x + i * (cw + gap), HEADER + 30, cw, 62));
		}
		slots.clear();
		int sw = (w - gap * (Progression.MAX_CHORDS - 1)) / Progression.MAX_CHORDS;
		for (int i = 0; i < Progression.MAX_CHORDS; i++) {
			slots.add(new Rectangle(x + i * (sw + gap), HEADER + 128, sw, 62));
		}
		int row = HEADER + 202;
		playButton = new Rectangle(x, row, 112, 40);
		slower = new Rectangle(x + 122, row, 34, 40);
		faster = new Rectangle(x + 122 + 34 + 76, row, 34, 40);
		clearButton = new Rectangle(x + w - 58, row, 58, 40);
		int melodyX = faster.x + faster.width + 8;
		melodyButton = new Rectangle(melodyX, row, Math.max(64, clearButton.x - 8 - melodyX), 40);
		int half = (w - gap) / 2;
		styleButton = new Rectangle(x, row + 48, half, 32);
		voicingButton = new Rectangle(x + half + gap, row + 48, half, 32);
		presetButtons.clear();
		int pw = (w - gap * 2) / 3;
		for (int i = 0; i < Preset.values().length; i++) {
			presetButtons.add(new Rectangle(x + (i % 3) * (pw + gap), row + 112 + (i / 3) * 36, pw, 30));
		}
		// In melody mode the melody's piano roll takes the famous progressions' place.
		melodyRoll = new Rectangle(x, row + 112, w, 66);
		keysTop = Math.max(row + 112 + 36 + 76, height - 96);
	}

	private int keyboardHeight() {
		return Math.max(40, getHeight() - 12 - keysTop);
	}

	/** The on-screen key under the point, or -1. */
	private int keyAt(int x, int y) {
		layOut();
		return Look.keyAt(16, keysTop, getWidth() - 32, keyboardHeight(), x, y);
	}

	private Object targetAt(int x, int y) {
		layOut();
		double dx = x - circleX, dy = y - circleY;
		double distance = Math.sqrt(dx * dx + dy * dy);
		if (distance <= radius && distance >= radius * 0.36) {
			double degrees = Math.toDegrees(Math.atan2(dy, dx)) + 90 + 15;
			int position = Math.floorMod((int) Math.floor(degrees / 30), 12);
			return distance >= radius * 0.66 ? 100 + position : 200 + position;
		}
		List<Rectangle> all = new ArrayList<>(chordButtons);
		all.addAll(slots.subList(0, Math.min(slots.size(), song.size() + (selected >= 0 ? 1 : 0))));
		if (!melodyMode) {
			all.addAll(presetButtons);
		}
		all.add(melodyButton);
		all.add(playButton);
		all.add(slower);
		all.add(faster);
		all.add(styleButton);
		all.add(voicingButton);
		all.add(clearButton);
		for (Rectangle box : all) {
			if (box.contains(x, y)) {
				return box;
			}
		}
		return null;
	}

	// ---- drawing ----

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		layOut();
		g.setColor(BACKGROUND);
		g.fillRect(0, 0, getWidth(), getHeight());
		paintHeader(g);
		paintCircle(g);
		paintChordButtons(g);
		paintSlots(g);
		paintControls(g);
		if (melodyMode) {
			paintMelodyRoll(g);
		} else {
			paintPresets(g);
		}
		paintInfo(g, keysTop - 14);
		paintKeys(g, keysTop);
	}

	private void paintHeader(Graphics2D g) {
		g.setColor(CARD);
		g.fillRect(0, 0, getWidth(), HEADER);
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 22));
		g.drawString("Songwriter", 18, 34);
		String key = song.keyName() + "   " + Progression.signature(song.position());
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		g.drawString(key, getWidth() - 18 - g.getFontMetrics().stringWidth(key), 33);
	}

	/**
	 * The circle of fifths: major keys round the outside, their relative minors inside. The key's own six chords
	 * (I, IV and V outside, vi, ii and iii inside) are the three neighbors in both rings.
	 */
	private void paintCircle(Graphics2D g) {
		int p = song.position();
		int heardPosition = -1;
		boolean heardMinor = false;
		if (heardDegree > 0 && !sounding.isEmpty()) {
			int root = song.triad(heardDegree)[0];
			String quality = song.quality(heardDegree);
			for (int i = 0; i < 12; i++) {
				if (quality.equals("") && Progression.tonicAt(i, false) == Math.floorMod(root, 12)) {
					heardPosition = i;
				} else if (quality.equals("m") && Progression.tonicAt(i, true) == Math.floorMod(root, 12)) {
					heardPosition = i;
					heardMinor = true;
				}
			}
		}
		for (int ring = 0; ring < 2; ring++) {
			boolean minor = ring == 1;
			double outer = radius * (minor ? 0.66 : 1.0), inner = radius * (minor ? 0.36 : 0.66);
			for (int i = 0; i < 12; i++) {
				int distance = Math.min(Math.floorMod(i - p, 12), Math.floorMod(p - i, 12));
				boolean home = i == p && minor == song.isMinor();
				Color fill = home ? BLUE : distance <= 1 ? SELECTED : CARD;
				if (i == heardPosition && minor == heardMinor) {
					fill = GOLD;
				} else if (same(hover, (minor ? 200 : 100) + i)) {
					fill = home ? BLUE_HOVER : CARD_HOVER;
				}
				fillSegment(g, i, inner, outer, fill);
				g.setColor(fill == GOLD ? BACKGROUND : distance <= 1 || home ? TEXT : SOFT);
				String name = Progression.keyName(i, minor);
				name = name.substring(0, name.indexOf(' '));
				g.setFont(new Font("SansSerif", Font.BOLD, minor ? 12 : 15));
				double angle = Math.toRadians(-90 + i * 30);
				double middle = (outer + inner) / 2;
				int tx = circleX + (int) Math.round(Math.cos(angle) * middle);
				int ty = circleY + (int) Math.round(Math.sin(angle) * middle);
				g.drawString(name, tx - g.getFontMetrics().stringWidth(name) / 2, ty + (minor ? 4 : 5));
			}
		}
		g.setColor(BACKGROUND);
		g.setStroke(new BasicStroke(2f));
		for (int i = 0; i < 12; i++) {
			double angle = Math.toRadians(-90 + i * 30 - 15);
			g.drawLine(circleX + (int) (Math.cos(angle) * radius * 0.36), circleY + (int) (Math.sin(angle) * radius * 0.36),
					circleX + (int) (Math.cos(angle) * radius), circleY + (int) (Math.sin(angle) * radius));
		}
		int ring = (int) (radius * 0.66);
		g.drawOval(circleX - ring, circleY - ring, ring * 2, ring * 2);
		g.setStroke(new BasicStroke(1f));
		// The middle names the key.
		String tonic = song.keyName();
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 20));
		String note = tonic.substring(0, tonic.indexOf(' '));
		g.drawString(note, circleX - g.getFontMetrics().stringWidth(note) / 2, circleY + 2);
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		String mode = song.isMinor() ? "minor" : "major";
		g.drawString(mode, circleX - g.getFontMetrics().stringWidth(mode) / 2, circleY + 18);
		// Under it, how to use it.
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		int y = circleY + radius + 22;
		g.drawString("Click a key, or Left / Right to move a fifth.", 16, y);
		g.drawString("M: major or its relative minor.", 16, y + 17);
		g.drawString("Shaded: the key's six chords.", 16, y + 34);
	}

	/** One twelfth of a ring, as a polygon along both arcs. */
	private void fillSegment(Graphics2D g, int position, double inner, double outer, Color color) {
		int steps = 8;
		int[] xs = new int[2 * (steps + 1)];
		int[] ys = new int[2 * (steps + 1)];
		double start = Math.toRadians(-90 + position * 30 - 15);
		for (int k = 0; k <= steps; k++) {
			double angle = start + Math.toRadians(30.0 * k / steps);
			xs[k] = circleX + (int) Math.round(Math.cos(angle) * outer);
			ys[k] = circleY + (int) Math.round(Math.sin(angle) * outer);
			double back = start + Math.toRadians(30.0 * (steps - k) / steps);
			xs[steps + 1 + k] = circleX + (int) Math.round(Math.cos(back) * inner);
			ys[steps + 1 + k] = circleY + (int) Math.round(Math.sin(back) * inner);
		}
		g.setColor(color);
		g.fillPolygon(xs, ys, xs.length);
	}

	private void paintChordButtons(Graphics2D g) {
		int x = rightX();
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		g.drawString("Chords in " + song.keyName(), x, HEADER + 22);
		int last = selected > 0 ? song.degree(selected - 1) : selected < 0 && song.size() > 0 ? song.degree(song.size() - 1) : 0;
		int[] next = Progression.usualNext(last);
		for (int i = 0; i < 7; i++) {
			int degree = i + 1;
			Rectangle b = chordButtons.get(i);
			boolean lit = degree == heardDegree && !sounding.isEmpty();
			g.setColor(lit ? GOLD : hover == b ? CARD_HOVER : CARD);
			g.fillRoundRect(b.x, b.y, b.width, b.height, 12, 12);
			for (int n : next) {
				if (n == degree && !lit) {
					g.setColor(SUGGESTED);
					g.setStroke(new BasicStroke(2f));
					g.drawRoundRect(b.x + 1, b.y + 1, b.width - 2, b.height - 2, 12, 12);
					g.setStroke(new BasicStroke(1f));
					g.setColor(GOLD);
					g.fillOval(b.x + b.width - 11, b.y + 5, 6, 6);
				}
			}
			Color text = lit ? BACKGROUND : TEXT;
			g.setColor(text);
			String numeral = song.numeral(degree);
			g.setFont(Look.fitFont(g, numeral, Font.BOLD, 18, b.width - 8));
			g.drawString(numeral, b.x + (b.width - g.getFontMetrics().stringWidth(numeral)) / 2, b.y + 25);
			String name = song.chordName(degree);
			g.setFont(Look.fitFont(g, name, Font.PLAIN, 14, b.width - 8));
			g.drawString(name, b.x + (b.width - g.getFontMetrics().stringWidth(name)) / 2, b.y + 44);
			g.setColor(lit ? BACKGROUND : DIM);
			g.setFont(new Font("SansSerif", Font.PLAIN, 10));
			g.drawString(String.valueOf(degree), b.x + 5, b.y + b.height - 5);
		}
		if (next.length > 0) {
			// Beside the heading: what the gold rings mean.
			g.setColor(GOLD);
			g.setFont(new Font("SansSerif", Font.PLAIN, 12));
			Rectangle end = chordButtons.get(6);
			String hint = Look.fit(g, "Gold: often comes after " + song.numeral(last), end.x + end.width - x - 170);
			g.drawString(hint, end.x + end.width - g.getFontMetrics().stringWidth(hint), HEADER + 22);
		}
	}

	private void paintSlots(Graphics2D g) {
		int x = rightX();
		Rectangle first = slots.get(0);
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		g.drawString("Your progression", x, first.y - 8);
		for (int i = 0; i < slots.size(); i++) {
			Rectangle s = slots.get(i);
			if (i >= song.size()) {
				g.setColor(CARD);
				g.drawRoundRect(s.x, s.y, s.width - 1, s.height - 1, 12, 12);
				if (i == song.size()) {
					g.setColor(DIM);
					g.setFont(new Font("SansSerif", Font.PLAIN, 20));
					g.drawString("+", s.x + s.width / 2 - 6, s.y + s.height / 2 + 7);
				}
				continue;
			}
			int degree = song.degree(i);
			boolean now = i == playing;
			g.setColor(now ? SELECTED : hover == s ? CARD_HOVER : CARD);
			g.fillRoundRect(s.x, s.y, s.width, s.height, 12, 12);
			if (now || i == selected) {
				g.setColor(i == selected ? BLUE : GOLD);
				g.setStroke(new BasicStroke(3f));
				g.drawRoundRect(s.x + 1, s.y + 1, s.width - 3, s.height - 3, 12, 12);
				g.setStroke(new BasicStroke(1f));
			}
			g.setColor(TEXT);
			String numeral = song.numeral(degree);
			g.setFont(Look.fitFont(g, numeral, Font.BOLD, 17, s.width - 6));
			g.drawString(numeral, s.x + (s.width - g.getFontMetrics().stringWidth(numeral)) / 2, s.y + 24);
			g.setColor(SOFT);
			String name = song.chordName(degree);
			g.setFont(Look.fitFont(g, name, Font.PLAIN, 13, s.width - 6));
			g.drawString(name, s.x + (s.width - g.getFontMetrics().stringWidth(name)) / 2, s.y + 42);
			// Four beat dots, lit up to the beat being played.
			for (int b = 0; b < 4; b++) {
				g.setColor(now && b <= beat ? GOLD : DIM);
				int dot = 5;
				int dx = s.x + s.width / 2 - 2 * (dot + 3) + b * (dot + 3) + 1;
				g.fillOval(dx, s.y + s.height - 13, dot, dot);
			}
		}
		g.setColor(DIM);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		String help = selected >= 0 ? "Pick a chord to put in its place. Esc: keep it." : "Click a chord to change it. Backspace: remove.";
		Rectangle last = slots.get(slots.size() - 1);
		g.drawString(Look.fit(g, help, last.x + last.width - x - 120), x + 120, first.y - 8);
	}

	private void paintControls(Graphics2D g) {
		boolean on = player.isPlaying();
		Rectangle b = playButton;
		g.setColor(hover == b ? BLUE_HOVER : BLUE);
		g.fillRoundRect(b.x, b.y, b.width, b.height, 22, 22);
		g.setColor(Color.WHITE);
		int cx = b.x + 22, cy = b.y + b.height / 2;
		if (on) {
			g.fillRect(cx - 7, cy - 8, 14, 16);
		} else {
			g.fillPolygon(new int[] { cx - 6, cx + 9, cx - 6 }, new int[] { cy - 9, cy, cy + 9 }, 3);
		}
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		g.drawString(on ? "Stop" : "Play", b.x + 42, cy + 6);

		paintSmallButton(g, slower, "-");
		paintSmallButton(g, faster, "+");
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		String tempo = String.valueOf(song.tempo());
		int middle = (slower.x + slower.width + faster.x) / 2;
		g.drawString(tempo, middle - g.getFontMetrics().stringWidth(tempo) / 2, slower.y + 19);
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.drawString("beats/min", middle - g.getFontMetrics().stringWidth("beats/min") / 2, slower.y + 34);

		paintSmallButton(g, clearButton, "Clear");
		paintMelodyButton(g);
		paintOption(g, styleButton, "Play chords", song.style().title(), "S");
		paintOption(g, voicingButton, "Voicing", song.voicing().title(), "V");
	}

	/** Melody (R): blue while melody mode is on, with a red dot while what is played is being recorded. */
	private void paintMelodyButton(Graphics2D g) {
		Rectangle b = melodyButton;
		g.setColor(melodyMode ? hover == b ? BLUE_HOVER : BLUE : hover == b ? CARD_HOVER : CARD);
		g.fillRoundRect(b.x, b.y, b.width, b.height, 12, 12);
		boolean recording = melodyMode && player.isPlaying();
		g.setColor(recording ? RECORD : melodyMode ? Color.WHITE : SOFT);
		g.fillOval(b.x + 9, b.y + b.height / 2 - 5, 10, 10);
		g.setColor(melodyMode ? Color.WHITE : TEXT);
		g.setFont(Look.fitFont(g, "Melody", Font.BOLD, 14, b.width - 30));
		g.drawString("Melody", b.x + 24, b.y + b.height / 2 + 5);
	}

	/**
	 * The melody as a piano roll over the progression's bars: one column per eighth note, higher notes higher, the
	 * bar being played shaded and the beat playing marked in gold.
	 */
	private void paintMelodyRoll(Graphics2D g) {
		Rectangle r = melodyRoll;
		List<int[]> notes = song.melody();
		int bars = Math.max(1, song.size());
		int columns = bars * Progression.EIGHTHS;
		notes.removeIf(note -> note[0] >= columns); // past the last chord: kept, but not played until there are chords there
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		g.drawString("Melody (R)", r.x, r.y - 8);
		String count = notes.isEmpty() ? "No notes yet" : (notes.size() == 1 ? "1 note" : notes.size() + " notes") + ". Backspace erases it";
		g.setColor(DIM);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		count = Look.fit(g, count, r.width - 90);
		g.drawString(count, r.x + r.width - g.getFontMetrics().stringWidth(count), r.y - 8);
		g.setColor(CARD);
		g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
		int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
		for (int[] note : notes) {
			low = Math.min(low, note[1]);
			high = Math.max(high, note[1]);
		}
		if (low > high) {
			low = 60 + song.tonic();
			high = low + 12;
		}
		// At least an octave tall, centered on the notes.
		while (high - low < 12) {
			high++;
			if (high - low < 12) {
				low--;
			}
		}
		double column = (r.width - 8) / (double) columns;
		int left = r.x + 4, top = r.y + 4, rows = high - low + 1;
		double row = (r.height - 8) / (double) rows;
		if (playing >= 0) {
			g.setColor(SELECTED);
			g.fillRect(left + (int) (playing * 8 * column), r.y + 2, (int) (8 * column), r.height - 4);
			g.setColor(new Color(255, 196, 60, 110));
			g.fillRect(left + (int) ((playing * 8 + beat * 2) * column), r.y + 2, (int) Math.ceil(2 * column), r.height - 4);
		}
		g.setColor(DIM);
		for (int bar = 1; bar < bars; bar++) {
			int bx = left + (int) (bar * 8 * column);
			g.drawLine(bx, r.y + 4, bx, r.y + r.height - 4);
		}
		g.setColor(RIGHT);
		for (int[] note : notes) {
			int nx = left + (int) (note[0] * column);
			int nw = Math.max(3, (int) (Math.min(note[2], columns - note[0]) * column) - 1);
			int ny = top + (int) ((high - note[1]) * row);
			g.fillRoundRect(nx, ny, nw, Math.max(3, (int) row), 4, 4);
		}
		if (notes.isEmpty()) {
			g.setColor(SOFT);
			g.setFont(new Font("SansSerif", Font.PLAIN, 13));
			String hint = player.isPlaying() ? "Play the keys now: every note is written into the loop."
					: "Press Play (Space), then play the keys over the chords.";
			hint = Look.fit(g, hint, r.width - 16);
			g.drawString(hint, r.x + (r.width - g.getFontMetrics().stringWidth(hint)) / 2, r.y + r.height / 2 + 5);
		}
	}

	private void paintSmallButton(Graphics2D g, Rectangle b, String label) {
		g.setColor(hover == b ? CARD_HOVER : CARD);
		g.fillRoundRect(b.x, b.y, b.width, b.height, 12, 12);
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, label.length() == 1 ? 20 : 14));
		g.drawString(label, b.x + (b.width - g.getFontMetrics().stringWidth(label)) / 2, b.y + b.height / 2 + (label.length() == 1 ? 7 : 5));
	}

	private void paintOption(Graphics2D g, Rectangle b, String label, String value, String key) {
		g.setColor(hover == b ? CARD_HOVER : CARD);
		g.fillRoundRect(b.x, b.y, b.width, b.height, 12, 12);
		g.setFont(new Font("SansSerif", Font.PLAIN, 13));
		g.setColor(SOFT);
		g.drawString(label + ":", b.x + 10, b.y + 21);
		int labelWidth = g.getFontMetrics().stringWidth(label + ": ");
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		g.drawString(Look.fit(g, value, b.width - labelWidth - 34), b.x + 10 + labelWidth, b.y + 21);
		g.setColor(DIM);
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.drawString(key, b.x + b.width - 10 - g.getFontMetrics().stringWidth(key), b.y + 20);
	}

	private void paintPresets(Graphics2D g) {
		Rectangle first = presetButtons.get(0);
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		g.drawString("Famous progressions (P)", first.x, first.y - 8);
		Preset current = song.matchingPreset();
		for (int i = 0; i < presetButtons.size(); i++) {
			Preset preset = Preset.values()[i];
			Rectangle b = presetButtons.get(i);
			g.setColor(preset == current ? SELECTED : hover == b ? CARD_HOVER : CARD);
			g.fillRoundRect(b.x, b.y, b.width, b.height, 10, 10);
			g.setColor(preset == current ? GOLD : TEXT);
			g.setFont(Look.fitFont(g, preset.title(), Font.BOLD, 13, b.width - 12));
			g.drawString(preset.title(), b.x + 8, b.y + 20);
		}
	}

	/** One line over the keyboard: a notice, or the chord sounding and its notes. */
	private void paintInfo(Graphics2D g, int y) {
		String line;
		Color color = SOFT;
		if (!notice.isEmpty()) {
			line = notice;
			color = GOLD;
		} else if (melodyMode && !player.isPlaying()) {
			line = "Melody: type A W S E D F T G Y H U J K (C" + (typedBase / 12 - 1)
					+ " up), Z / X: octave, or click the keys. Grey keys are not in " + song.keyName() + ".";
			color = TEXT;
		} else if (melodyMode && heardDegree > 0) {
			line = "Recording over " + song.chordName(heardDegree) + ": its notes " + song.noteNames(heardDegree)
					+ " are gold, the safest to land on.";
			color = TEXT;
		} else if (heardDegree > 0 && !sounding.isEmpty()) {
			line = song.chordName(heardDegree) + " (" + song.numeral(heardDegree) + " in " + song.keyName() + "): "
					+ song.noteNames(heardDegree);
			color = TEXT;
		} else if (song.size() == 0) {
			line = "Pick chords from the key above, or a famous progression, then press Play.";
		} else {
			line = "Space plays your progression round and round, with a bass line. Click the keys below (or R) to add a melody.";
		}
		g.setColor(color);
		g.setFont(Look.fitFont(g, line, Font.BOLD, 14, getWidth() - 32));
		g.drawString(line, 16, y);
	}

	/**
	 * The keyboard, with the chord's notes in gold and the bass in blue; in melody mode the notes out of the key are
	 * greyed and the melody's notes are green.
	 */
	private void paintKeys(Graphics2D g, int top) {
		Color[] colors = new Color[HIGH_NOTE + 1];
		if (melodyMode) {
			Set<Integer> inKey = new HashSet<>();
			for (int note : song.scale()) {
				inKey.add(Math.floorMod(note, 12));
			}
			for (int note = LOW_NOTE; note <= HIGH_NOTE; note++) {
				boolean in = inKey.contains(note % 12);
				colors[note] = isBlack(note) ? in ? IN_KEY_BLACK : null : in ? null : OUT_OF_KEY;
			}
		}
		for (int note : sounding) {
			colors[note] = isBlack(note) ? GOLD_BLACK : GOLD;
		}
		for (int note : soundingBass) {
			colors[note] = isBlack(note) ? BASS_BLACK : BASS;
		}
		for (int note : player.melodySounding()) {
			colors[note] = isBlack(note) ? MELODY_BLACK : RIGHT;
		}
		Look.paintKeyboard(g, 16, top, getWidth() - 32, keyboardHeight(), colors);
	}
}
