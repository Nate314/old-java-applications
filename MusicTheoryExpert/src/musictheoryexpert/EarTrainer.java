package musictheoryexpert;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

import musictheoryexpert.Notes.MusicalTerms;
import synthesizer.MySynthesizer;

/**
 * The Ear training tab: a list of drills on the left and the current round on the right. Play sounds the question,
 * the answer buttons (or keys 1 to 9, 0, - and =) name it, and the keyboard at the bottom shows the notes once it is
 * answered. A right answer moves on by itself; a wrong one plays the question again with its notes lit.
 */
@SuppressWarnings("serial")
public final class EarTrainer extends JPanel {
	private static final String CHOICE_KEYS = "1234567890-=";
	private static final int[] FANFARE = { 72, 76, 79, 84 };
	private static final int RIGHT_PAUSE_MILLIS = 1000;
	/** The keyboard runs from C3 to C7, which holds every note a question plays. */
	private static final int LOW_NOTE = 48, HIGH_NOTE = 96;

	private static final Color BACKGROUND = new Color(24, 26, 34);
	private static final Color CARD = new Color(38, 41, 54);
	private static final Color CARD_HOVER = new Color(52, 56, 72);
	private static final Color SELECTED = new Color(62, 70, 104);
	private static final Color TEXT = new Color(236, 237, 242);
	private static final Color SOFT = new Color(150, 155, 172);
	private static final Color DIM = new Color(92, 96, 112);
	private static final Color GOLD = new Color(255, 196, 60);
	private static final Color BLUE = new Color(86, 132, 255);
	private static final Color BLUE_HOVER = new Color(116, 156, 255);
	private static final Color RIGHT = new Color(64, 186, 108);
	private static final Color WRONG = new Color(226, 86, 86);
	private static final Color WHITE_KEY = new Color(246, 246, 242);
	private static final Color BLACK_KEY = new Color(16, 16, 20);

	private final DrillRecords records;
	private final StepPlayer player;
	private final Random random = new Random();
	private Round round;
	/** False until the round's first question is played. */
	private boolean started;
	private int chosen = -1;
	private int[] sounding;
	private Timer pause;
	private boolean newBest;
	private Drill opened;
	private String notice = "";

	// Where things were last drawn, for the mouse.
	private final List<Rectangle> drillRows = new ArrayList<>();
	private final List<Rectangle> choiceBoxes = new ArrayList<>();
	private Rectangle playButton = new Rectangle();
	private Rectangle againButton = new Rectangle();
	private Rectangle nextButton = new Rectangle();
	private Object hover;

	public EarTrainer(MySynthesizer synth, DrillRecords records) {
		this.records = records;
		this.player = new StepPlayer(synth, notes -> {
			sounding = notes;
			repaint();
		});
		setBackground(BACKGROUND);
		setFocusable(true);
		select(records.suggested());
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				requestFocusInWindow();
				click(targetAt(event.getX(), event.getY()));
			}

			@Override
			public void mouseMoved(MouseEvent event) {
				Object target = targetAt(event.getX(), event.getY());
				if (target != hover) {
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

	public Round round() {
		return round;
	}

	/** Starts a new, not yet played round of {@code drill}. */
	public void select(Drill drill) {
		stopAll();
		round = new Round(drill, random);
		started = false;
		chosen = -1;
		newBest = false;
		opened = null;
		notice = "";
		repaint();
	}

	/** Space: play the first question, play it again, or (after a wrong answer) move on. */
	public void primary() {
		notice = "";
		if (round.isOver()) {
			select(round.drill());
			return;
		}
		if (round.answered()) {
			nextQuestion();
		} else {
			started = true;
			playQuestion();
		}
	}

	public void answer(int choice) {
		if (!started || round.answered() || choice < 0 || choice >= round.drill().choiceCount()) {
			return;
		}
		chosen = choice;
		notice = "";
		boolean right = round.answer(choice);
		if (round.isOver()) {
			finish();
		} else if (right) {
			player.stop();
			pause = new Timer(RIGHT_PAUSE_MILLIS, event -> nextQuestion());
			pause.setRepeats(false);
			pause.start();
		} else {
			// Hear it again, this time with its notes lit.
			playQuestion();
		}
		repaint();
	}

	/** Handles a key press on this tab; returns true when the key was used. */
	public boolean keyPressed(KeyEvent event) {
		int code = event.getKeyCode();
		char key = Character.toLowerCase(event.getKeyChar());
		int choice = CHOICE_KEYS.indexOf(key);
		if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ENTER) {
			primary();
		} else if (choice >= 0 && key != KeyEvent.CHAR_UNDEFINED) {
			answer(choice);
		} else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) {
			Drill[] drills = Drill.values();
			int index = round.drill().ordinal() + (code == KeyEvent.VK_UP ? -1 : 1);
			if (index >= 0 && index < drills.length) {
				tryOpen(drills[index]);
			}
		} else if (code == KeyEvent.VK_R) {
			select(round.drill());
		} else if (code == KeyEvent.VK_N) {
			Drill next = nextDrill();
			if (next != null) {
				tryOpen(next);
			}
		} else {
			return false;
		}
		return true;
	}

	/** The drill after this one, or null at the end. */
	private Drill nextDrill() {
		int index = round.drill().ordinal() + 1;
		return index < Drill.values().length ? Drill.values()[index] : null;
	}

	private void tryOpen(Drill drill) {
		if (records.isOpen(drill)) {
			select(drill);
		} else {
			notice = "Win a star on " + Drill.values()[drill.ordinal() - 1].title() + " to open " + drill.title() + ".";
			repaint();
		}
	}

	private void click(Object target) {
		if (target == null) {
			return;
		}
		if (target == playButton) {
			primary();
		} else if (target == againButton) {
			select(round.drill());
		} else if (target == nextButton && nextDrill() != null) {
			tryOpen(nextDrill());
		} else if (drillRows.contains(target)) {
			tryOpen(Drill.values()[drillRows.indexOf(target)]);
		} else if (choiceBoxes.contains(target)) {
			answer(choiceBoxes.indexOf(target));
		}
	}

	private void nextQuestion() {
		stopAll();
		round.next();
		chosen = -1;
		playQuestion();
		repaint();
	}

	private void playQuestion() {
		player.play(round.question().steps(), round.drill().stepMillis());
	}

	private void finish() {
		boolean wasOpen = nextDrill() != null && records.isOpen(nextDrill());
		newBest = records.add(round);
		if (nextDrill() != null && !wasOpen && records.isOpen(nextDrill())) {
			opened = nextDrill();
		}
		if (round.stars() > 0) {
			int[][] steps = new int[FANFARE.length][];
			for (int i = 0; i < FANFARE.length; i++) {
				steps[i] = new int[] { FANFARE[i] };
			}
			player.play(steps, 140);
		} else {
			player.stop();
		}
	}

	private void stopAll() {
		if (pause != null) {
			pause.stop();
			pause = null;
		}
		player.stop();
	}

	// ---- drawing ----

	private int mainX() {
		return 236;
	}

	private int choiceColumns(int count) {
		return count <= 3 ? count : count == 4 ? 2 : count <= 6 ? 3 : 4;
	}

	/** Works out where everything goes, so painting and the mouse agree. */
	private void layOut() {
		int width = getWidth();
		drillRows.clear();
		for (int i = 0; i < Drill.values().length; i++) {
			drillRows.add(new Rectangle(14, 66 + i * 58, mainX() - 32, 50));
		}
		int x = mainX();
		int mainWidth = Math.max(200, width - x - 18);
		playButton = new Rectangle(x, 128, Math.min(mainWidth, 300), 52);
		choiceBoxes.clear();
		int count = round.drill().choiceCount();
		int columns = choiceColumns(count);
		int gap = 10;
		int boxWidth = (mainWidth - gap * (columns - 1)) / columns;
		for (int i = 0; i < count; i++) {
			choiceBoxes.add(new Rectangle(x + (i % columns) * (boxWidth + gap), 200 + (i / columns) * 54, boxWidth, 44));
		}
		int buttonWidth = Math.min(180, (mainWidth - gap) / 2);
		againButton = new Rectangle(x, 284, buttonWidth, 46);
		nextButton = new Rectangle(x + buttonWidth + gap, 284, buttonWidth, 46);
	}

	private Object targetAt(int x, int y) {
		layOut();
		for (Rectangle row : drillRows) {
			if (row.contains(x, y)) {
				return row;
			}
		}
		if (round.isOver()) {
			if (againButton.contains(x, y)) {
				return againButton;
			}
			return nextDrill() != null && nextButton.contains(x, y) ? nextButton : null;
		}
		if (playButton.contains(x, y)) {
			return playButton;
		}
		if (started && !round.answered()) {
			for (Rectangle box : choiceBoxes) {
				if (box.contains(x, y)) {
					return box;
				}
			}
		}
		return null;
	}

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
		paintDrills(g);
		int x = mainX();
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		g.drawString(round.drill().prompt(), x, 82);
		paintProgress(g, x, 106);
		int used;
		if (round.isOver()) {
			paintResult(g, x);
			used = 380;
		} else {
			paintPlayButton(g);
			paintChoices(g);
			Rectangle last = choiceBoxes.get(choiceBoxes.size() - 1);
			paintFeedback(g, x, last.y + last.height + 28);
			used = last.y + last.height + 64;
		}
		// The keyboard fills the bottom: up to 116 px tall, never over the feedback.
		int top = Math.max(used, getHeight() - 130);
		paintPiano(g, x, top, Math.max(200, getWidth() - x - 18), Math.max(40, getHeight() - 14 - top));
	}

	private void paintHeader(Graphics2D g) {
		g.setColor(CARD);
		g.fillRect(0, 0, getWidth(), 52);
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 22));
		g.drawString("Ear Training", 18, 34);
		String total = records.totalStars() + " / " + Drill.values().length * 3;
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		int width = g.getFontMetrics().stringWidth(total);
		g.drawString(total, getWidth() - 18 - width, 33);
		paintStar(g, getWidth() - 34 - width, 27, 10, GOLD);
	}

	private void paintDrills(Graphics2D g) {
		Drill[] drills = Drill.values();
		for (int i = 0; i < drills.length; i++) {
			Rectangle row = drillRows.get(i);
			boolean open = records.isOpen(drills[i]);
			boolean selected = drills[i] == round.drill();
			g.setColor(selected ? SELECTED : row == hover ? CARD_HOVER : CARD);
			g.fillRoundRect(row.x, row.y, row.width, row.height, 12, 12);
			g.setColor(open ? (selected ? GOLD : SOFT) : DIM);
			g.setFont(new Font("SansSerif", Font.BOLD, 13));
			g.drawString(String.valueOf(i + 1), row.x + 12, row.y + 30);
			g.setColor(open ? TEXT : DIM);
			g.setFont(new Font("SansSerif", Font.BOLD, 14));
			g.drawString(drills[i].title(), row.x + 32, row.y + 22);
			if (open) {
				int stars = records.stars(drills[i]);
				for (int s = 0; s < 3; s++) {
					paintStar(g, row.x + 40 + s * 18, row.y + 36, 7, s < stars ? GOLD : DIM);
				}
				int best = records.score(drills[i]);
				if (best >= 0) {
					g.setColor(SOFT);
					g.setFont(new Font("SansSerif", Font.PLAIN, 12));
					g.drawString("best " + best + "/" + Round.QUESTIONS, row.x + 98, row.y + 41);
				}
			} else {
				paintLock(g, row.x + 32, row.y + 29);
				g.setColor(DIM);
				g.setFont(new Font("SansSerif", Font.PLAIN, 12));
				g.drawString("star on " + (i) + " opens it", row.x + 48, row.y + 41);
			}
		}
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		int y = 66 + drills.length * 58 + 12;
		g.drawString("Up / Down: drill   R: restart", 16, y);
		g.drawString("Space: play   1-9: answer", 16, y + 18);
	}

	private void paintProgress(Graphics2D g, int x, int y) {
		for (int i = 0; i < Round.QUESTIONS; i++) {
			int result = round.result(i);
			Color color = result == Round.RIGHT ? RIGHT : result == Round.WRONG ? WRONG : DIM;
			int cx = x + 8 + i * 22;
			g.setColor(color);
			g.fillOval(cx - 7, y - 7, 14, 14);
			if (i == round.index() && started && !round.isOver()) {
				g.setColor(TEXT);
				g.drawOval(cx - 9, y - 9, 18, 18);
			}
		}
		g.setFont(new Font("SansSerif", Font.BOLD, 14));
		String score = "Score " + round.score() + (round.streak() >= 2 ? "   Streak " + round.streak() : "");
		g.setColor(round.streak() >= 3 ? GOLD : SOFT);
		g.drawString(score, x + Round.QUESTIONS * 22 + 14, y + 5);
	}

	private void paintPlayButton(Graphics2D g) {
		Rectangle b = playButton;
		g.setColor(hover == b ? BLUE_HOVER : BLUE);
		g.fillRoundRect(b.x, b.y, b.width, b.height, 26, 26);
		boolean next = started && round.answered();
		int cx = b.x + 28, cy = b.y + b.height / 2;
		g.setColor(Color.WHITE);
		if (next) {
			g.fillPolygon(new int[] { cx - 8, cx + 2, cx - 8 }, new int[] { cy - 9, cy, cy + 9 }, 3);
			g.fillPolygon(new int[] { cx + 2, cx + 12, cx + 2 }, new int[] { cy - 9, cy, cy + 9 }, 3);
		} else {
			g.fillPolygon(new int[] { cx - 7, cx + 11, cx - 7 }, new int[] { cy - 11, cy, cy + 11 }, 3);
		}
		String label = !started ? "Play question 1" : next ? "Next question" : "Hear it again";
		g.setFont(new Font("SansSerif", Font.BOLD, 17));
		g.drawString(label, b.x + 52, cy + 6);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		FontMetrics metrics = g.getFontMetrics();
		g.drawString("Space", b.x + b.width - 14 - metrics.stringWidth("Space"), cy + 5);
	}

	private void paintChoices(Graphics2D g) {
		Drill drill = round.drill();
		boolean answered = round.answered();
		for (int i = 0; i < choiceBoxes.size(); i++) {
			Rectangle box = choiceBoxes.get(i);
			Color fill = CARD;
			if (answered && i == round.question().answer()) {
				fill = RIGHT;
			} else if (answered && i == chosen) {
				fill = WRONG;
			} else if (started && !answered && box == hover) {
				fill = CARD_HOVER;
			}
			g.setColor(fill);
			g.fillRoundRect(box.x, box.y, box.width, box.height, 12, 12);
			boolean live = started && !answered;
			g.setColor(live || fill != CARD ? TEXT : SOFT);
			g.setFont(fitFont(g, drill.choice(i), Font.BOLD, 16, box.width - 36));
			g.drawString(drill.choice(i), box.x + 14, box.y + 28);
			g.setFont(new Font("SansSerif", Font.PLAIN, 12));
			g.setColor(live || fill != CARD ? SOFT : DIM);
			if (fill != CARD && fill != CARD_HOVER) {
				g.setColor(new Color(255, 255, 255, 200));
			}
			String key = String.valueOf(CHOICE_KEYS.charAt(i));
			g.drawString(key, box.x + box.width - 12 - g.getFontMetrics().stringWidth(key), box.y + 27);
		}
	}

	private void paintFeedback(Graphics2D g, int x, int y) {
		Drill.Question question = round.question();
		String line;
		String detail = "";
		Color color = SOFT;
		if (!notice.isEmpty()) {
			line = notice;
		} else if (!started) {
			line = "Listen, then pick the answer. Ten questions: 6 right wins a star.";
		} else if (!round.answered()) {
			line = "What did you hear?";
		} else {
			String answer = round.drill().choice(question.answer());
			boolean right = chosen == question.answer();
			line = right ? "Right: " + answer + "." : "It was " + answer + ", not " + round.drill().choice(chosen) + ".";
			color = right ? RIGHT : WRONG;
			detail = "Notes: " + noteNames(question.steps());
		}
		g.setColor(color);
		g.setFont(fitFont(g, line, Font.BOLD, 16, getWidth() - x - 18));
		g.drawString(line, x, y);
		if (!detail.isEmpty()) {
			g.setColor(SOFT);
			g.setFont(fitFont(g, detail, Font.PLAIN, 14, getWidth() - x - 18));
			g.drawString(detail, x, y + 22);
		}
	}

	private void paintResult(Graphics2D g, int x) {
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 40));
		String score = round.score() + " / " + Round.QUESTIONS;
		g.drawString(score, x, 186);
		int starsX = x + g.getFontMetrics().stringWidth(score) + 30;
		for (int s = 0; s < 3; s++) {
			paintStar(g, starsX + s * 44, 172, 18, s < round.stars() ? GOLD : DIM);
		}
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		String verdict = round.stars() == 3 ? "Perfect round!" : round.stars() > 0 ? "Drill passed." : "Six right wins a star. Try again!";
		if (newBest && round.stars() > 0) {
			verdict += "  New best!";
		}
		g.setColor(round.stars() > 0 ? GOLD : SOFT);
		g.drawString(verdict, x, 230);
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 14));
		String extra = "Longest streak: " + round.bestStreak();
		if (opened != null) {
			extra += ".  Opened: " + opened.title() + ".";
		}
		g.drawString(fit(g, extra, getWidth() - x - 18), x, 256);
		if (!notice.isEmpty()) {
			g.drawString(fit(g, notice, getWidth() - x - 18), x, 360);
		}
		paintButton(g, againButton, "Again", "R", hover == againButton ? BLUE_HOVER : BLUE);
		if (nextDrill() != null) {
			boolean open = records.isOpen(nextDrill());
			paintButton(g, nextButton, "Next drill", "N", !open ? CARD : hover == nextButton ? CARD_HOVER : SELECTED);
		}
	}

	private void paintButton(Graphics2D g, Rectangle b, String label, String key, Color fill) {
		g.setColor(fill);
		g.fillRoundRect(b.x, b.y, b.width, b.height, 22, 22);
		g.setColor(TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		g.drawString(label, b.x + 18, b.y + 29);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		g.setColor(SOFT);
		g.drawString(key, b.x + b.width - 14 - g.getFontMetrics().stringWidth(key), b.y + 28);
	}

	/** The keyboard: every key a question can use, with its notes lit once it has been answered. */
	private void paintPiano(Graphics2D g, int x, int y, int width, int height) {
		boolean reveal = started && round.answered() && !round.isOver() || round.isOver() && sounding != null;
		boolean[] lit = new boolean[HIGH_NOTE + 1];
		boolean[] heard = new boolean[HIGH_NOTE + 1];
		if (reveal && sounding != null) {
			for (int note : sounding) {
				lit[Math.min(HIGH_NOTE, Math.max(LOW_NOTE, note))] = true;
			}
		}
		if (reveal && !round.isOver()) {
			for (int[] step : round.question().steps()) {
				for (int note : step) {
					heard[Math.min(HIGH_NOTE, Math.max(LOW_NOTE, note))] = true;
				}
			}
		}
		int whites = 0;
		for (int note = LOW_NOTE; note <= HIGH_NOTE; note++) {
			whites += isBlack(note) ? 0 : 1;
		}
		int keyWidth = Math.max(4, width / whites);
		int left = x + (width - keyWidth * whites) / 2;
		int white = 0;
		for (int note = LOW_NOTE; note <= HIGH_NOTE; note++) {
			if (isBlack(note)) {
				continue;
			}
			int kx = left + white * keyWidth;
			g.setColor(lit[note] ? GOLD : heard[note] ? new Color(255, 226, 150) : WHITE_KEY);
			g.fillRoundRect(kx + 1, y, keyWidth - 2, height, 6, 6);
			if (note % 12 == 0) {
				g.setColor(DIM);
				g.setFont(new Font("SansSerif", Font.PLAIN, 10));
				g.drawString("C" + (note / 12 - 1), kx + 3, y + height - 6);
			}
			white++;
		}
		white = 0;
		int blackWidth = keyWidth * 3 / 5;
		for (int note = LOW_NOTE; note <= HIGH_NOTE; note++) {
			if (!isBlack(note)) {
				white++;
				continue;
			}
			int kx = left + white * keyWidth - blackWidth / 2;
			g.setColor(lit[note] ? GOLD : heard[note] ? new Color(214, 160, 40) : BLACK_KEY);
			g.fillRoundRect(kx, y, blackWidth, height * 3 / 5, 4, 4);
		}
	}

	static boolean isBlack(int note) {
		int pitch = note % 12;
		return pitch == 1 || pitch == 3 || pitch == 6 || pitch == 8 || pitch == 10;
	}

	/** The distinct notes of a question in the order they first sound, as names such as "F#4". */
	static String noteNames(int[][] steps) {
		StringBuilder names = new StringBuilder();
		List<Integer> seen = new ArrayList<>();
		for (int[] step : steps) {
			for (int note : step) {
				if (!seen.contains(note)) {
					seen.add(note);
					names.append(names.length() == 0 ? "" : " ")
							.append(Notes.getNoteName(note, MusicalTerms.SHARP).replace('s', '#')).append(note / 12 - 1);
				}
			}
		}
		return names.toString();
	}

	private static void paintStar(Graphics2D g, int cx, int cy, int radius, Color color) {
		int[] xs = new int[10];
		int[] ys = new int[10];
		for (int i = 0; i < 10; i++) {
			double angle = -Math.PI / 2 + i * Math.PI / 5;
			double r = i % 2 == 0 ? radius : radius * 0.45;
			xs[i] = cx + (int) Math.round(Math.cos(angle) * r);
			ys[i] = cy + (int) Math.round(Math.sin(angle) * r);
		}
		g.setColor(color);
		g.fillPolygon(xs, ys, 10);
	}

	private static void paintLock(Graphics2D g, int x, int y) {
		g.setColor(DIM);
		g.drawOval(x + 2, y - 2, 8, 10);
		g.fillRoundRect(x, y + 3, 12, 10, 3, 3);
	}

	/** A SansSerif font of {@code style}, shrunk from {@code size} until {@code text} fits in {@code width}. */
	private static Font fitFont(Graphics2D g, String text, int style, int size, int width) {
		Font font = new Font("SansSerif", style, size);
		while (size > 10 && g.getFontMetrics(font).stringWidth(text) > width) {
			font = new Font("SansSerif", style, --size);
		}
		return font;
	}

	/** {@code text} cut short with "..." to fit in {@code width} at the current font. */
	private static String fit(Graphics2D g, String text, int width) {
		FontMetrics metrics = g.getFontMetrics();
		if (metrics.stringWidth(text) <= width) {
			return text;
		}
		while (text.length() > 1 && metrics.stringWidth(text + "...") > width) {
			text = text.substring(0, text.length() - 1);
		}
		return text + "...";
	}
}
