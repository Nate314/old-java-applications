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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;

import musictheoryexpert.ChordNamer.InKey;
import musictheoryexpert.ChordNamer.KeyFit;
import musictheoryexpert.ChordNamer.Quality;
import musictheoryexpert.ChordNamer.Reading;
import synthesizer.MySynthesizer;

/**
 * The Chord finder tab: hold notes on the keyboard (click keys, or type on the computer keyboard like a piano) and it
 * names the chord they make, spells it on a grand staff, lists the keys it belongs to and its other names. Chords
 * added to a trail are matched to the keys that hold them all, and any of those keys writes the trail into the
 * Songwriter. It also builds any chord from a root and a type. The original demo plays from its header.
 */
@SuppressWarnings("serial")
public final class ChordFinder extends JPanel {
	/** Typed keys, as on a piano: A is C, W is C#, S is D, and so on up to ; (E an octave up). */
	private static final String PIANO_KEYS = "awsedftgyhujkolp;";
	private static final int TRAIL_SIZE = 8;
	private static final int HEADER = 46;
	private static final int ARPEGGIO_MILLIS = 170;
	private static final Quality[] BUILD_QUALITIES = { Quality.MAJOR, Quality.MINOR, Quality.DOMINANT_7, Quality.MAJOR_7,
			Quality.MINOR_7, Quality.DIMINISHED, Quality.AUGMENTED, Quality.SUS2, Quality.SUS4, Quality.HALF_DIMINISHED,
			Quality.DIMINISHED_7, Quality.SIXTH, Quality.MINOR_6, Quality.ADD9, Quality.NINTH };
	private static final Color HELD = new Color(120, 170, 255);
	private static final Color HELD_BLACK = new Color(60, 100, 200);
	private static final Color GOLD_BLACK = new Color(214, 150, 20);
	private static final Color LINE = new Color(150, 155, 172, 170);

	/** Writes chords into the Songwriter: a key and the chords' degrees. */
	public interface Songwriting {
		void write(int position, boolean minor, int[] degrees);
	}

	private final StepPlayer player;
	private final Runnable demo;
	private final Songwriting songwriting;
	private final TreeSet<Integer> held = new TreeSet<>();
	private List<Reading> readings = new ArrayList<>();
	private final List<Reading> trail = new ArrayList<>();
	private final List<List<Integer>> trailNotes = new ArrayList<>();
	private List<KeyFit> fits = new ArrayList<>();
	/** The typed keys' lowest note (A types it). */
	private int typedBase = 60;
	private final Set<Integer> keysDown = new HashSet<>();
	private int[] sounding;
	private int buildRoot = 7;
	private Quality buildQuality = Quality.MAJOR;
	private String notice = "";
	private Object hover;

	// Where things were last drawn, for the mouse.
	private Rectangle card = new Rectangle(), staff = new Rectangle(), keyboard = new Rectangle();
	private Rectangle demoButton = new Rectangle(), playButton = new Rectangle(), clearButton = new Rectangle();
	private Rectangle addButton = new Rectangle();
	private final List<Rectangle> slots = new ArrayList<>();
	private final List<Rectangle> removers = new ArrayList<>();
	private final List<Rectangle> fitButtons = new ArrayList<>();
	private final List<Rectangle> rootButtons = new ArrayList<>();
	private final List<Rectangle> qualityButtons = new ArrayList<>();
	private int keysLine, trailLine, buildLine;

	public ChordFinder(MySynthesizer synth, Runnable demo, Songwriting songwriting) {
		this.demo = demo;
		this.songwriting = songwriting;
		this.player = new StepPlayer(synth, notes -> {
			sounding = notes;
			repaint();
		});
		held.addAll(ChordNamer.build(buildRoot, buildQuality));
		changed();
		setBackground(BACKGROUND);
		setFocusable(true);
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				requestFocusInWindow();
				click(targetAt(event.getX(), event.getY()), event.getButton() == MouseEvent.BUTTON3);
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

	public List<Integer> held() {
		return new ArrayList<>(held);
	}

	public List<Reading> readings() {
		return readings;
	}

	public List<Reading> trail() {
		return trail;
	}

	public List<KeyFit> fits() {
		return fits;
	}

	// ---- what the controls do ----

	/** Holds or lets go of {@code note}; a note pressed sounds on its own. */
	public void toggle(int note) {
		if (note < LOW_NOTE || note > HIGH_NOTE) {
			return;
		}
		notice = "";
		if (!held.remove(note)) {
			held.add(note);
			player.play(new int[][] { { note } }, ARPEGGIO_MILLIS);
		}
		changed();
	}

	/** Holds exactly {@code notes} and plays them. */
	public void hold(List<Integer> notes) {
		held.clear();
		held.addAll(notes);
		notice = "";
		changed();
		play();
	}

	/** Plays the held notes low to high, then all together. */
	public void play() {
		if (held.isEmpty()) {
			notice = "Hold some notes first: click keys, or type A W S E D F T G Y H U J K.";
			repaint();
			return;
		}
		List<Integer> notes = held();
		int[][] steps = new int[notes.size() + 1][];
		for (int i = 0; i < notes.size(); i++) {
			steps[i] = new int[] { notes.get(i) };
		}
		steps[notes.size()] = toArray(notes);
		player.play(steps, ARPEGGIO_MILLIS);
	}

	public void clearHeld() {
		held.clear();
		player.stop();
		notice = "";
		changed();
	}

	/** Builds {@code quality} on {@code root} on the keyboard and plays it. */
	public void build(int root, Quality quality) {
		buildRoot = Math.floorMod(root, 12);
		buildQuality = quality;
		hold(ChordNamer.build(buildRoot, quality));
	}

	/** Adds the chord held to the trail. */
	public void addToTrail() {
		if (readings.isEmpty()) {
			notice = held.isEmpty() ? "Hold a chord first, then Add puts it in the trail."
					: "These notes have no chord name to add. Try taking one away.";
		} else if (trail.size() >= TRAIL_SIZE) {
			notice = "The trail holds eight chords. Backspace takes the last one off.";
		} else {
			trail.add(readings.get(0));
			trailNotes.add(held());
			notice = "";
			refit();
		}
		repaint();
	}

	public void removeFromTrail(int index) {
		if (index >= 0 && index < trail.size()) {
			trail.remove(index);
			trailNotes.remove(index);
			refit();
			repaint();
		}
	}

	public void clearTrail() {
		trail.clear();
		trailNotes.clear();
		refit();
		repaint();
	}

	/** Writes the trail's chords that are in {@code fit}'s key into the Songwriter. */
	public void writeIn(KeyFit fit) {
		List<Integer> degrees = new ArrayList<>();
		for (int degree : fit.degrees) {
			if (degree > 0) {
				degrees.add(degree);
			}
		}
		songwriting.write(fit.position, fit.minor, toArray(degrees));
	}

	public void shiftTyped(int octaves) {
		typedBase = Math.max(LOW_NOTE, Math.min(HIGH_NOTE - 16, typedBase + 12 * octaves));
		notice = "Typed keys now start at C" + (typedBase / 12 - 1) + ".";
		repaint();
	}

	private void refit() {
		fits = ChordNamer.fitKeys(trail, 3);
	}

	private void changed() {
		List<Integer> notes = held();
		readings = ChordNamer.name(notes);
		if (!readings.isEmpty()) {
			buildRoot = readings.get(0).root;
			buildQuality = readings.get(0).quality;
		}
		repaint();
	}

	/** Handles a key press on this tab; returns true when the key was used. */
	public boolean keyPressed(KeyEvent event) {
		int code = event.getKeyCode();
		char key = Character.toLowerCase(event.getKeyChar());
		int piano = PIANO_KEYS.indexOf(key);
		if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
			return false;
		}
		if (piano >= 0 && key != KeyEvent.CHAR_UNDEFINED) {
			// A held key repeats; only its first press counts.
			if (keysDown.add(code)) {
				toggle(typedBase + piano);
			}
		} else if (code == KeyEvent.VK_SPACE) {
			play();
		} else if (code == KeyEvent.VK_ENTER) {
			addToTrail();
		} else if (code == KeyEvent.VK_BACK_SPACE) {
			removeFromTrail(trail.size() - 1);
		} else if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_DELETE) {
			clearHeld();
		} else if (code == KeyEvent.VK_Z || code == KeyEvent.VK_X) {
			shiftTyped(code == KeyEvent.VK_X ? 1 : -1);
		} else {
			return false;
		}
		return true;
	}

	public void keyReleased(KeyEvent event) {
		keysDown.remove(event.getKeyCode());
	}

	private void click(Object target, boolean right) {
		if (target == null) {
			return;
		}
		if (target instanceof Integer) {
			toggle((Integer) target);
		} else if (target == demoButton) {
			demo.run();
		} else if (target == playButton) {
			play();
		} else if (target == clearButton) {
			clearHeld();
		} else if (target == addButton) {
			addToTrail();
		} else if (removers.contains(target)) {
			removeFromTrail(removers.indexOf(target));
		} else if (slots.contains(target)) {
			int index = slots.indexOf(target);
			if (right) {
				removeFromTrail(index);
			} else {
				hold(trailNotes.get(index));
			}
		} else if (fitButtons.contains(target)) {
			writeIn(fits.get(fitButtons.indexOf(target)));
		} else if (rootButtons.contains(target)) {
			build(rootButtons.indexOf(target), buildQuality);
		} else if (qualityButtons.contains(target)) {
			build(buildRoot, BUILD_QUALITIES[qualityButtons.indexOf(target)]);
		}
	}

	private static boolean same(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}

	// ---- layout ----

	/** Works out where everything goes, so painting and the mouse agree. */
	private void layOut() {
		int width = getWidth(), height = getHeight();
		int x = 16, w = width - 32;
		demoButton = new Rectangle(width - 16 - 128, 9, 128, 28);
		card = new Rectangle(x, HEADER + 6, w, 150);
		int staffWidth = w >= 600 ? 230 : 0;
		staff = new Rectangle(card.x + card.width - staffWidth, card.y, staffWidth, card.height);
		playButton = new Rectangle(staff.x - 12 - 84, card.y + card.height - 38, 84, 28);
		clearButton = new Rectangle(playButton.x - 8 - 70, playButton.y, 70, 28);
		keysLine = card.y + card.height + 20;
		trailLine = keysLine + 26;
		int gap = 6;
		addButton = new Rectangle(x, trailLine + 8, 96, 40);
		slots.clear();
		removers.clear();
		int slotLeft = addButton.x + addButton.width + 10;
		int sw = (x + w - slotLeft - gap * (TRAIL_SIZE - 1)) / TRAIL_SIZE;
		for (int i = 0; i < TRAIL_SIZE; i++) {
			Rectangle slot = new Rectangle(slotLeft + i * (sw + gap), trailLine + 8, sw, 40);
			slots.add(slot);
			removers.add(new Rectangle(slot.x + slot.width - 16, slot.y, 16, 16));
		}
		fitButtons.clear();
		int fitTop = trailLine + 54;
		int fw = Math.min(150, (w - 170 - gap * 2) / 3);
		for (int i = 0; i < fits.size(); i++) {
			fitButtons.add(new Rectangle(x + w - (3 - i) * fw - (2 - i) * gap, fitTop, fw, 26));
		}
		buildLine = fitTop + 26 + 22;
		rootButtons.clear();
		int label = 52;
		int rw = (w - label - gap * 11) / 12;
		for (int i = 0; i < 12; i++) {
			rootButtons.add(new Rectangle(x + label + i * (rw + gap), buildLine - 16, rw, 24));
		}
		qualityButtons.clear();
		int qw = (w - label - gap * (BUILD_QUALITIES.length - 1)) / BUILD_QUALITIES.length;
		for (int i = 0; i < BUILD_QUALITIES.length; i++) {
			qualityButtons.add(new Rectangle(x + label + i * (qw + gap), buildLine + 14, qw, 24));
		}
		int keysTop = buildLine + 48;
		keyboard = new Rectangle(x, keysTop, w, Math.max(50, height - 8 - keysTop));
	}

	private Object targetAt(int x, int y) {
		layOut();
		int note = Look.keyAt(keyboard.x, keyboard.y, keyboard.width, keyboard.height, x, y);
		if (note >= 0) {
			return note;
		}
		List<Rectangle> all = new ArrayList<>();
		all.add(demoButton);
		all.add(playButton);
		all.add(clearButton);
		all.add(addButton);
		// The remove corners first: they sit on top of their slots.
		all.addAll(removers.subList(0, trail.size()));
		all.addAll(slots.subList(0, trail.size()));
		all.addAll(fitButtons);
		all.addAll(rootButtons);
		all.addAll(qualityButtons);
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
		paintCard(g);
		paintStaff(g);
		paintKeys(g);
		paintTrail(g);
		paintBuild(g);
		paintKeyboard(g);
	}

	private void paintHeader(Graphics2D g) {
		g.setColor(CARD);
		g.fillRect(0, 0, getWidth(), HEADER);
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 22));
		g.drawString("Chord finder", 18, 31);
		int titleEnd = 18 + g.getFontMetrics().stringWidth("Chord finder") + 16;
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		String hint = "Type A W S E D F T G Y H U J K like a piano (C" + (typedBase / 12 - 1) + " up), Z / X: octave";
		g.drawString(Look.fit(g, hint, demoButton.x - titleEnd - 10), titleEnd, 29);
		paintButton(g, demoButton, "Original demo", false);
	}

	private void paintButton(Graphics2D g, Rectangle b, String label, boolean primary) {
		g.setColor(primary ? hover == b ? BLUE_HOVER : BLUE : hover == b ? CARD_HOVER : SELECTED);
		g.fillRoundRect(b.x, b.y, b.width, b.height, 12, 12);
		g.setColor(primary ? Color.WHITE : TEXT);
		g.setFont(Look.fitFont(g, label, Font.BOLD, 13, b.width - 10));
		g.drawString(label, b.x + (b.width - g.getFontMetrics().stringWidth(label)) / 2, b.y + b.height / 2 + 5);
	}

	/** The big name of what is held, what it is, its notes, and its other names. */
	private void paintCard(Graphics2D g) {
		g.setColor(CARD);
		g.fillRoundRect(card.x, card.y, card.width, card.height, 16, 16);
		int x = card.x + 16, right = staff.x - 12;
		List<Integer> notes = held();
		int count = Integer.bitCount(ChordNamer.maskOf(notes));
		String big, line;
		String[] spelled = ChordNamer.spellings(notes);
		if (notes.isEmpty()) {
			big = "Play some notes";
			line = "Click keys on the keyboard below, or build a chord from a root and a type.";
		} else if (count == 1) {
			big = spelled[notes.get(0) % 12];
			line = notes.size() == 1 ? "One note. Add more to make an interval or a chord." : "One note, in " + notes.size() + " octaves.";
		} else if (!readings.isEmpty() && count > 2) {
			big = readings.get(0).name();
			line = readings.get(0).description();
		} else if (count == 2) {
			String interval = ChordNamer.interval(notes);
			big = interval.substring(0, interval.indexOf(':'));
			line = interval.substring(interval.indexOf(':') + 2) + (readings.isEmpty() ? "" : ", or " + readings.get(0).name() + " (a power chord)");
		} else {
			big = "No usual name";
			line = nearMiss(notes);
		}
		g.setColor(TEXT);
		g.setFont(Look.fitFont(g, big, Font.BOLD, 40, right - x));
		g.drawString(big, x, card.y + 48);
		g.setColor(SOFT);
		g.setFont(Look.fitFont(g, line, Font.PLAIN, 15, right - x));
		g.drawString(line, x, card.y + 72);

		// The notes, lowest first, each with what it is in the chord.
		Reading best = readings.isEmpty() ? null : readings.get(0);
		List<Integer> pitches = new ArrayList<>();
		for (int note : notes) {
			if (!pitches.contains(note % 12)) {
				pitches.add(note % 12);
			}
		}
		int cx = x;
		for (int pitch : pitches) {
			String name = spelled[pitch];
			String label = best == null ? "" : best.label(pitch);
			boolean lit = isSounding(pitch);
			g.setColor(lit ? GOLD : SELECTED);
			g.fillRoundRect(cx, card.y + 82, 44, 24, 10, 10);
			g.setColor(lit ? BACKGROUND : TEXT);
			g.setFont(new Font("SansSerif", Font.BOLD, 14));
			g.drawString(name, cx + (44 - g.getFontMetrics().stringWidth(name)) / 2, card.y + 99);
			g.setColor(SOFT);
			g.setFont(new Font("SansSerif", Font.PLAIN, 11));
			g.drawString(label, cx + (44 - g.getFontMetrics().stringWidth(label)) / 2, card.y + 118);
			cx += 50;
			if (cx + 44 > right) {
				break;
			}
		}
		// Other names for the same notes.
		List<String> others = new ArrayList<>();
		for (int i = 1; i < readings.size() && others.size() < 3; i++) {
			String name = readings.get(i).name();
			if (!others.contains(name) && !name.equals(big)) {
				others.add(name);
			}
		}
		int bottom = card.y + card.height - 10;
		String also = notice.isEmpty() ? others.isEmpty() ? "" : "Also: " + String.join(", ", others) : notice;
		g.setColor(notice.isEmpty() ? SOFT : GOLD);
		g.setFont(new Font("SansSerif", notice.isEmpty() ? Font.PLAIN : Font.BOLD, 13));
		g.drawString(Look.fit(g, also, clearButton.x - x - 10), x, bottom);
		paintButton(g, clearButton, "Clear", false);
		paintButton(g, playButton, "Play", true);
	}

	/** For notes with no name: the chords one note away ("Without D: C major"). */
	private String nearMiss(List<Integer> notes) {
		List<String> near = new ArrayList<>();
		for (int note : notes) {
			List<Integer> rest = new ArrayList<>(notes);
			rest.removeIf(n -> n % 12 == note % 12);
			List<Reading> named = ChordNamer.name(rest);
			if (!named.isEmpty() && Integer.bitCount(ChordNamer.maskOf(rest)) > 2) {
				String text = "without " + ChordNamer.spellings(notes)[note % 12] + " it is " + named.get(0).name();
				if (!near.contains(text)) {
					near.add(text);
				}
			}
		}
		return near.isEmpty() ? "No chord holds just these notes. Try taking one away."
				: Character.toUpperCase(near.get(0).charAt(0)) + String.join("; ", near).substring(1) + ".";
	}

	private boolean isSounding(int pitch) {
		if (sounding != null) {
			for (int note : sounding) {
				if (note % 12 == pitch) {
					return true;
				}
			}
		}
		return false;
	}

	/** The held notes on a grand staff, spelled as the chord's name spells them. */
	private void paintStaff(Graphics2D g) {
		if (staff.width == 0) {
			return;
		}
		int gap = 7;
		int left = staff.x + 12, right = staff.x + staff.width - 12;
		int trebleTop = staff.y + 46, bassTop = trebleTop + 4 * gap + 3 * gap;
		g.setColor(new Color(30, 32, 43));
		g.fillRoundRect(staff.x + 4, staff.y + 6, staff.width - 10, staff.height - 12, 12, 12);
		g.setColor(LINE);
		g.setStroke(new BasicStroke(1f));
		for (int i = 0; i < 5; i++) {
			g.drawLine(left, trebleTop + i * gap, right, trebleTop + i * gap);
			g.drawLine(left, bassTop + i * gap, right, bassTop + i * gap);
		}
		g.drawLine(left, trebleTop, left, bassTop + 4 * gap);
		g.drawLine(right, trebleTop, right, bassTop + 4 * gap);
		paintTrebleClef(g, left + 14, trebleTop, gap);
		paintBassClef(g, left + 8, bassTop, gap);

		List<Integer> notes = held();
		String[] spelled = ChordNamer.spellings(notes);
		int noteX = left + 96;
		int lastStep = Integer.MIN_VALUE;
		boolean shifted = false;
		int accidentalColumn = 0;
		for (int note : notes) {
			String name = spelled[note % 12];
			int accidental = name.length() - 1;
			if (name.endsWith("b")) {
				accidental = -accidental;
			}
			int natural = note - accidental;
			int step = (natural / 12 - 1) * 7 + "CDEFGAB".indexOf(name.charAt(0));
			boolean treble = note >= 60;
			// Steps from each staff's top line: F5 is step 38, A3 step 26.
			int y = treble ? trebleTop + (38 - step) * gap / 2 : bassTop + (26 - step) * gap / 2;
			int top = treble ? trebleTop : bassTop, bottom = top + 4 * gap;
			// A second above the note before sits to its right.
			shifted = step - lastStep == 1 && !shifted;
			lastStep = step;
			int hx = noteX + (shifted ? 11 : 0);
			g.setColor(LINE);
			for (int ly = bottom + gap; ly <= y; ly += gap) {
				g.drawLine(hx - 4, ly, hx + 15, ly);
			}
			for (int ly = top - gap; ly >= y; ly -= gap) {
				g.drawLine(hx - 4, ly, hx + 15, ly);
			}
			g.setColor(isSounding(note % 12) ? GOLD : TEXT);
			g.setStroke(new BasicStroke(2f));
			g.drawOval(hx, y - 3, 11, 7);
			g.setStroke(new BasicStroke(1f));
			if (accidental != 0) {
				String mark = name.substring(1);
				g.setFont(new Font("Serif", Font.BOLD, 14));
				int ax = noteX - 6 - g.getFontMetrics().stringWidth(mark) - (accidentalColumn % 2) * 11;
				g.drawString(mark, ax, y + 5);
				accidentalColumn++;
			}
		}
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		String caption = notes.isEmpty() ? "Held notes show here" : "As written";
		g.drawString(caption, staff.x + (staff.width - g.getFontMetrics().stringWidth(caption)) / 2, staff.y + staff.height - 14);
	}

	/** A treble clef drawn from arcs: the curl round the G line, the stem, and the hook at the foot. */
	private void paintTrebleClef(Graphics2D g, int x, int top, int gap) {
		g.setColor(TEXT);
		g.setStroke(new BasicStroke(2f));
		int gLine = top + 3 * gap;
		g.drawArc(x - gap, gLine - gap, 2 * gap, 2 * gap, 90, 300);
		g.drawArc(x - gap - 3, gLine - 2 * gap, 2 * gap + 9, 3 * gap, 300, 220);
		g.drawLine(x + 2, top - 2 * gap + 2, x - 1, top + 5 * gap + 2);
		g.drawArc(x - 2, top - 2 * gap - 1, gap + 2, 2 * gap, 200, -220);
		g.fillOval(x - gap + 1, top + 5 * gap - 2, 5, 5);
		g.setStroke(new BasicStroke(1f));
	}

	/** A bass clef: the dot on the F line, the curve and the two dots either side of it. */
	private void paintBassClef(Graphics2D g, int x, int top, int gap) {
		g.setColor(TEXT);
		g.fillOval(x - 1, top + gap - 3, 7, 7);
		g.setStroke(new BasicStroke(2f));
		g.drawArc(x, top, 2 * gap + 2, 2 * gap + 4, 180, -200);
		g.drawLine(x + 2 * gap + 1, top + gap + 6, x + 2, top + 4 * gap - 2);
		g.setStroke(new BasicStroke(1f));
		g.fillOval(x + 2 * gap + 8, top + gap / 2 - 2, 4, 4);
		g.fillOval(x + 2 * gap + 8, top + gap + gap / 2 - 2, 4, 4);
	}

	/** One row: the keys the chord belongs to. */
	private void paintKeys(Graphics2D g) {
		int x = card.x;
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		g.drawString("In these keys", x, keysLine);
		int cx = x + g.getFontMetrics().stringWidth("In these keys") + 12;
		int end = card.x + card.width;
		Reading best = readings.isEmpty() ? null : readings.get(0);
		List<InKey> keys = best == null || Integer.bitCount(ChordNamer.maskOf(held())) < 3 ? new ArrayList<>()
				: ChordNamer.keysOf(best.root, best.quality);
		g.setFont(new Font("SansSerif", Font.PLAIN, 13));
		if (keys.isEmpty()) {
			g.setColor(DIM);
			g.drawString(best == null ? held.isEmpty() ? "Hold a chord to see the keys it belongs to." : "These notes are not one chord." : "Not a chord of any major or minor key.",
					cx, keysLine);
			return;
		}
		for (int i = 0; i < keys.size(); i++) {
			String text = keys.get(i).text();
			int w = g.getFontMetrics().stringWidth(text) + 16;
			String more = "+" + (keys.size() - i) + " more";
			if (cx + w > end - (i < keys.size() - 1 ? g.getFontMetrics().stringWidth(more) + 8 : 0)) {
				g.setColor(DIM);
				g.drawString(more, cx, keysLine);
				break;
			}
			g.setColor(i == 0 ? SELECTED : CARD);
			g.fillRoundRect(cx, keysLine - 16, w, 22, 10, 10);
			g.setColor(i == 0 ? TEXT : SOFT);
			g.drawString(text, cx + 8, keysLine);
			cx += w + 6;
		}
	}

	/** The trail of chords, and the keys that hold them, each a button that writes them into the Songwriter. */
	private void paintTrail(Graphics2D g) {
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		g.drawString("Your chords", card.x, trailLine);
		g.setColor(DIM);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		String help = trail.isEmpty() ? "Add chords from a song you know (Enter), and see which key it is in."
				: "Click one to hear it again. Backspace takes the last one off.";
		g.drawString(Look.fit(g, help, card.width - 100), card.x + 96, trailLine);
		paintButton(g, addButton, "Add (Enter)", true);
		for (int i = 0; i < slots.size(); i++) {
			Rectangle s = slots.get(i);
			if (i >= trail.size()) {
				g.setColor(CARD);
				g.drawRoundRect(s.x, s.y, s.width - 1, s.height - 1, 12, 12);
				continue;
			}
			g.setColor(hover == s ? CARD_HOVER : CARD);
			g.fillRoundRect(s.x, s.y, s.width, s.height, 12, 12);
			Reading chord = trail.get(i);
			String name = chord.name();
			g.setColor(TEXT);
			g.setFont(Look.fitFont(g, name, Font.BOLD, 16, s.width - 18));
			g.drawString(name, s.x + (s.width - g.getFontMetrics().stringWidth(name)) / 2, s.y + 20);
			if (!fits.isEmpty()) {
				int degree = fits.get(0).degrees[i];
				String numeral = degree > 0 ? ChordNamer.numeral(degree, chord.quality) : "not in key";
				g.setColor(degree > 0 ? GOLD : DIM);
				g.setFont(Look.fitFont(g, numeral, Font.PLAIN, 12, s.width - 6));
				g.drawString(numeral, s.x + (s.width - g.getFontMetrics().stringWidth(numeral)) / 2, s.y + 35);
			}
			Rectangle r = removers.get(i);
			g.setColor(hover == r ? WRONG : DIM);
			g.setFont(new Font("SansSerif", Font.BOLD, 11));
			g.drawString("x", r.x + 4, r.y + 12);
		}
		int fitTop = trailLine + 54;
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 13));
		String fitLine = trail.isEmpty() ? "" : fits.isEmpty() ? "No major or minor key holds these chords."
				: "Click a key to write these in the Songwriter:";
		int fitLeft = fitButtons.isEmpty() ? card.x + card.width : fitButtons.get(0).x;
		g.drawString(Look.fit(g, fitLine, fitLeft - card.x - 8), card.x, fitTop + 18);
		for (int i = 0; i < fitButtons.size(); i++) {
			KeyFit fit = fits.get(i);
			Rectangle b = fitButtons.get(i);
			String label = fit.keyName() + "  " + fit.fits + "/" + trail.size();
			paintButton(g, b, label, i == 0);
		}
	}

	/** Build a chord: a root and a type. */
	private void paintBuild(Graphics2D g) {
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		g.drawString("Build", card.x, buildLine);
		g.drawString("type", card.x, buildLine + 30);
		Reading best = readings.isEmpty() ? null : readings.get(0);
		for (int i = 0; i < 12; i++) {
			paintChip(g, rootButtons.get(i), ChordNamer.noteName(i), best != null && best.root == i);
		}
		for (int i = 0; i < BUILD_QUALITIES.length; i++) {
			String symbol = BUILD_QUALITIES[i].symbol();
			paintChip(g, qualityButtons.get(i), symbol.isEmpty() ? "maj" : symbol, best != null && best.quality == BUILD_QUALITIES[i]);
		}
	}

	private void paintChip(Graphics2D g, Rectangle b, String label, boolean on) {
		g.setColor(on ? BLUE : hover == b ? CARD_HOVER : CARD);
		g.fillRoundRect(b.x, b.y, b.width, b.height, 10, 10);
		g.setColor(on ? Color.WHITE : TEXT);
		g.setFont(Look.fitFont(g, label, Font.BOLD, 13, b.width - 4));
		g.drawString(label, b.x + (b.width - g.getFontMetrics().stringWidth(label)) / 2, b.y + 17);
	}

	/** The keyboard: held notes blue, the notes sounding gold. */
	private void paintKeyboard(Graphics2D g) {
		Color[] colors = new Color[HIGH_NOTE + 1];
		for (int note : held) {
			colors[note] = isBlack(note) ? HELD_BLACK : HELD;
		}
		if (sounding != null) {
			for (int note : sounding) {
				colors[note] = isBlack(note) ? GOLD_BLACK : GOLD;
			}
		}
		Look.paintKeyboard(g, keyboard.x, keyboard.y, keyboard.width, keyboard.height, colors);
	}

	private static int[] toArray(List<Integer> list) {
		int[] array = new int[list.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}
}
