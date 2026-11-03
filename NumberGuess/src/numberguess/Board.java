package numberguess;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The game board: tabs for the five ranges and You pick along the top, the verdict, and two number lines. The top
 * line is the whole range with what is still possible lit; the bottom one zooms in on just that part, so a click on
 * it guesses the number under the pointer, even in 1 to 1,000,000. The guesses so far sit underneath as chips.
 * The Magic tab is a card trick instead: see {@link MagicCards}, drawn by {@link MagicView}. The Duel tab is a race
 * against the computer to find each other's number: see {@link Duel}, drawn by {@link DuelView}.
 */
@SuppressWarnings("serial")
public final class Board extends JPanel {
	public static final int WIDTH = 640, HEIGHT = 350;

	static final Color BACKGROUND = new Color(22, 25, 36);
	static final Color CARD = new Color(36, 40, 56);
	static final Color CARD_HOVER = new Color(50, 55, 76);
	static final Color SELECTED = new Color(60, 70, 110);
	static final Color TEXT = new Color(236, 238, 245);
	static final Color SOFT = new Color(152, 158, 180);
	static final Color DIM = new Color(86, 92, 112);
	static final Color RULED_OUT = new Color(48, 52, 68);
	static final Color POSSIBLE = new Color(72, 196, 160);
	static final Color POSSIBLE_DARK = new Color(40, 120, 100);
	static final Color LOW = new Color(90, 150, 255);
	static final Color HIGH = new Color(240, 100, 96);
	static final Color GOLD = new Color(255, 198, 64);
	static final Color WARN = new Color(255, 170, 70);
	static final Color RIGHT = new Color(80, 205, 120);

	private static final int MARGIN = 20;
	private static final int TABS_Y = 10, TAB_HEIGHT = 44;
	private static final int FULL_Y = 150, BAR_HEIGHT = 12;
	private static final int ZOOM_Y = 198, ZOOM_HEIGHT = 36;
	private static final int CHIPS_Y = 264;
	/** The tabs after the five ranges. */
	private static final int DUEL_TAB = Level.values().length, PICK_TAB = DUEL_TAB + 1, MAGIC_TAB = DUEL_TAB + 2;

	private final Records records;
	private final Random random = new Random();
	private final Runnable modeChanged;
	private Level level;
	private Game game;
	private MindReader reader;
	private MagicCards magic;
	private final MagicView magicView = new MagicView(this::repaint);
	private Duel duel;
	private final DuelView duelView = new DuelView(this::repaint);
	/** Makes the computer's guess in a duel, a moment after yours. */
	private final Timer computerTurn = new Timer(DuelView.thinkMillis(), event -> computerGuesses());
	private String headline = "";
	private String subline = "";
	private Color headlineColor = TEXT;
	private boolean newBest;
	private Level opened;

	// The lit part of the top line slides to its new size instead of jumping.
	private double shownLow, shownHigh;
	private final Timer slide = new Timer(15, event -> step());

	// Where things were last drawn, for the mouse.
	private final List<Rectangle> tabs = new ArrayList<>();
	private Rectangle zoom = new Rectangle();
	private int hoverTab = -1;
	/** Where the pointer is over the zoomed line, or -1. */
	private int hoverX = -1;

	/** {@code modeChanged} runs when the board needs other buttons (a switch of tab, or a step of the card trick). */
	public Board(Records records, Runnable modeChanged) {
		this.records = records;
		this.modeChanged = modeChanged;
		setBackground(BACKGROUND);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setFocusable(true);
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				click(event.getX(), event.getY());
			}

			@Override
			public void mouseMoved(MouseEvent event) {
				hover(event.getX(), event.getY());
			}

			@Override
			public void mouseExited(MouseEvent event) {
				hoverTab = -1;
				hoverX = -1;
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		computerTurn.setRepeats(false);
		start(records.suggested());
	}

	// ---- state ----

	public boolean isPicking() {
		return reader != null;
	}

	public boolean isMagic() {
		return magic != null;
	}

	public boolean isDuel() {
		return duel != null;
	}

	public boolean isOver() {
		return isDuel() ? duel.isOver() : isMagic() ? magic.isDone() : isPicking() ? reader.isOver() : game.isOver();
	}

	/** The open tab: a range's ordinal, then Duel, You pick and Magic. */
	private int tab() {
		return isDuel() ? DUEL_TAB : isMagic() ? MAGIC_TAB : isPicking() ? PICK_TAB : level.ordinal();
	}

	/** Which buttons the window needs: 0 for guessing, 1 for You pick, 2 for Magic. */
	private int kind() {
		return isMagic() ? 2 : isPicking() ? 1 : 0;
	}

	/** Tells the window when a switch of tab needs other buttons. */
	private void switched(int kindBefore) {
		if (kind() != kindBefore) {
			modeChanged.run();
		}
	}

	public Game game() {
		return game;
	}

	/** A new number to guess in {@code level}. */
	public void start(Level level) {
		int before = kind();
		this.level = level;
		reader = null;
		magic = null;
		endDuel();
		game = new Game(level, random);
		newBest = false;
		opened = null;
		say("Guess a number from 1 to " + Level.commas(level.max()), TEXT,
				"Par " + level.par() + ": guess the middle of what is left. " + level.limit() + " guesses before it is given away.");
		reset();
		switched(before);
	}

	/** You think of a number and the computer guesses it. */
	public void startPicking() {
		int before = kind();
		reader = new MindReader();
		game = null;
		magic = null;
		endDuel();
		askReader();
		reset();
		switched(before);
	}

	/** The card trick: a fresh shuffle of the six cards. */
	public void startMagic() {
		magic = new MagicCards(random);
		reader = null;
		game = null;
		endDuel();
		magicView.dealt(false);
		askMagic();
		slide.stop();
		repaint();
		modeChanged.run();
	}

	/** A new duel: you hide a number, the computer has hidden one. */
	public void startDuel() {
		int before = kind();
		endDuel();
		duel = new Duel(random);
		reader = null;
		game = null;
		magic = null;
		say("Duel: hide a number from 1 to " + Duel.MAX, TEXT, "Then race the computer to find each other's. You guess first each round.");
		slide.stop();
		repaint();
		switched(before);
	}

	private void endDuel() {
		computerTurn.stop();
		duel = null;
	}

	/** The same game again: a new number in this range, a new duel, or a new round of You pick. */
	public void again() {
		if (isDuel()) {
			startDuel();
		} else if (isMagic()) {
			if (!magicView.isFlipping()) {
				startMagic();
			}
		} else if (isPicking()) {
			startPicking();
		} else {
			start(opened != null ? opened : level);
		}
	}

	/** Page Up / Page Down: the tab before or after this one. */
	public void moveTab(int by) {
		int index = tab() + by;
		// Locked ranges are passed over, so Duel, You pick and Magic can be reached from the keyboard from the start.
		while (index > 0 && index < DUEL_TAB && !records.isOpen(Level.values()[index])) {
			index += by;
		}
		if (index >= 0 && index <= MAGIC_TAB) {
			openTab(index);
		}
	}

	private void reset() {
		shownLow = low();
		shownHigh = high();
		slide.stop();
		repaint();
	}

	/** The top of the range being played. */
	int max() {
		return isDuel() ? Duel.MAX : isPicking() ? MindReader.MAX : level.max();
	}

	private int low() {
		return isPicking() ? Math.min(reader.low(), MindReader.MAX) : game.low();
	}

	private int high() {
		return isPicking() ? Math.max(reader.high(), 1) : game.high();
	}

	private List<Integer> guesses() {
		return isPicking() ? reader.guesses() : game.guesses();
	}

	private void say(String headline, Color color, String subline) {
		this.headline = headline;
		this.headlineColor = color;
		this.subline = subline;
	}

	/** Shows a short message in place of the subline, for a guess that could not be used. */
	public void notice(String text) {
		subline = text;
		repaint();
	}

	/** A typed or clicked guess. */
	public void guess(int number) {
		if (isDuel()) {
			duelGuess(number);
			return;
		}
		if (isPicking() || isMagic()) {
			return;
		}
		if (game.isOver()) {
			again();
			return;
		}
		String range = "It is from " + Level.commas(game.low()) + " to " + Level.commas(game.high()) + ".";
		switch (game.guess(number)) {
		case OUT_OF_RANGE:
			say("Out of range", WARN, "Pick a number from 1 to " + Level.commas(level.max()) + ". Not counted.");
			break;
		case RULED_OUT:
			say(Level.commas(number) + " is already ruled out", WARN, range + " Not counted.");
			break;
		case TOO_LOW:
		case TOO_HIGH:
			boolean low = number < game.number();
			if (game.isOver()) {
				say("Out of guesses. It was " + Level.commas(game.number()), HIGH, "Press Enter or click the line for a new number.");
			} else {
				say(Level.commas(number) + (low ? " is too low" : " is too high"), low ? LOW : HIGH,
						"It is from " + Level.commas(game.low()) + " to " + Level.commas(game.high()) + ". "
								+ guessesLeft(game.guessesLeft()) + ".");
			}
			break;
		case CORRECT:
			win();
			break;
		default:
			break;
		}
		slide.start();
		repaint();
	}

	private static String guessesLeft(int left) {
		return left + (left == 1 ? " guess left" : " guesses left");
	}

	private void win() {
		Level next = level.ordinal() + 1 < Level.values().length ? Level.values()[level.ordinal() + 1] : null;
		boolean wasOpen = next != null && records.isOpen(next);
		newBest = records.add(game);
		opened = next != null && !wasOpen && records.isOpen(next) ? next : null;
		int guesses = game.getGuesses();
		String more = opened != null ? " " + opened.label() + " is open: Enter plays it."
				: game.stars() < 3 ? " Par is " + level.par() + "." : " Press Enter for another.";
		say("Got it: " + Level.commas(game.number()) + " in " + guesses + (guesses == 1 ? " guess" : " guesses"), RIGHT,
				(newBest ? "New best! " : "") + more.trim());
	}

	// ---- Duel ----

	private void duelGuess(int number) {
		if (duel.isOver()) {
			startDuel();
			return;
		}
		if (duel.isHiding()) {
			if (duel.hide(number)) {
				say("Round 1: your guess", TEXT, "Its number is from 1 to " + Duel.MAX + ". Type a guess or click your line.");
			} else {
				notice("Hide a number from 1 to " + Duel.MAX + ".");
			}
			repaint();
			return;
		}
		if (!duel.isYourTurn()) {
			notice("Wait for the computer's guess.");
			return;
		}
		Game yours = duel.yours();
		String range = "Its number is from " + yours.low() + " to " + yours.high() + ".";
		switch (duel.guess(number)) {
		case OUT_OF_RANGE:
			say("Out of range", WARN, "Guess a number from 1 to " + Duel.MAX + ". Not counted.");
			break;
		case RULED_OUT:
			say(number + " is already ruled out", WARN, range + " Not counted.");
			break;
		case CORRECT:
			say("Found it: " + number + " in " + guesses(yours.getGuesses()) + "!", RIGHT, "The computer has this round's guess to tie...");
			computerThinks();
			break;
		default:
			boolean low = number < yours.number();
			say(number + (low ? " is too low" : " is too high"), low ? LOW : HIGH, "Its number is from " + yours.low() + " to "
					+ yours.high() + ". The computer is guessing yours...");
			computerThinks();
			break;
		}
		repaint();
	}

	private void computerThinks() {
		duelView.moved();
		computerTurn.restart();
	}

	private void computerGuesses() {
		if (!isDuel() || !duel.isComputersTurn()) {
			return;
		}
		int guess = duel.computerGuess();
		duelView.moved();
		Game yours = duel.yours(), its = duel.its();
		if (duel.isOver()) {
			records.add(duel.result());
			String rematch = " Enter or a click on the line for a rematch.";
			switch (duel.result()) {
			case WON:
				say("You win! " + yours.number() + " in " + guesses(yours.getGuesses()), RIGHT,
						"The computer had your " + duel.hidden() + " down to " + its.low() + "-" + its.high() + "." + rematch);
				break;
			case DRAWN:
				say("A draw: both found in " + guesses(yours.getGuesses()), GOLD, "It guessed your " + duel.hidden() + " too." + rematch);
				break;
			default:
				say("The computer found your " + duel.hidden() + " in " + guesses(its.getGuesses()), HIGH, "Its number was " + yours.number()
						+ "." + rematch);
				break;
			}
		} else {
			String verdict = guess < duel.hidden() ? "too low" : "too high";
			say("Round " + duel.round() + ": your guess", TEXT, "It guessed " + guess + ", " + verdict + ". Its number is from "
					+ yours.low() + " to " + yours.high() + ".");
		}
		repaint();
	}

	private static String guesses(int count) {
		return count + (count == 1 ? " guess" : " guesses");
	}

	// ---- You pick ----

	private void askReader() {
		if (reader.isFound()) {
			int guesses = reader.guesses().size();
			say("Your number is " + reader.guess() + "!", RIGHT, "Found in " + guesses + (guesses == 1 ? " guess" : " guesses")
					+ ". Guessing the middle never needs more than " + MindReader.par() + ".");
		} else if (reader.isCaught()) {
			say("Wait, that can't be!", HIGH, caughtReason() + " Did your number change?");
		} else {
			int number = reader.guesses().size();
			say("Is it " + reader.guess() + "?", TEXT, number == 1
					? "Think of a number from 1 to " + MindReader.MAX + ". Is yours lower, higher, or this one?"
					: "Guess " + number + ". It is from " + reader.low() + " to " + reader.high() + ", if you told the truth.");
		}
	}

	private String caughtReason() {
		if (reader.high() < 1) {
			return "You said lower than 1.";
		}
		if (reader.low() > MindReader.MAX) {
			return "You said higher than " + MindReader.MAX + ".";
		}
		return "You said higher than " + (reader.low() - 1) + " and lower than " + (reader.high() + 1) + ".";
	}

	public void higher() {
		answer(1);
	}

	public void lower() {
		answer(-1);
	}

	public void correct() {
		answer(0);
	}

	private void answer(int direction) {
		if (!isPicking()) {
			return;
		}
		if (reader.isOver()) {
			startPicking();
			return;
		}
		if (direction > 0) {
			reader.higher();
		} else if (direction < 0) {
			reader.lower();
		} else {
			reader.correct();
		}
		askReader();
		slide.start();
		repaint();
	}

	// ---- Magic ----

	private void askMagic() {
		if (!magic.isDone()) {
			int card = magic.current() + 1;
			say("Is your number on this card?", TEXT, card == 1
					? "Think of a number from 1 to " + MagicCards.MAX + " and keep it secret. Card 1 of " + MagicCards.CARDS + "."
					: "Card " + card + " of " + MagicCards.CARDS + ". Just yes or no, I will do the rest.");
		} else if (magic.number() == 0) {
			say("Not on any card?", HIGH, "Every number from 1 to " + MagicCards.MAX + " is on at least one card. Enter tries again.");
		} else if (magicView.isExplaining()) {
			say("How it works", GOLD, "Each card's smallest number is its key. Add the keys of the cards you said yes to.");
		} else {
			say("Your number is " + magic.number() + "!", RIGHT, "Read from six yes or no answers. S shows how, Enter does it again.");
		}
	}

	/** Your number is ({@code yes}) or is not on the card shown; after the reveal, either one starts again. */
	public void magicAnswer(boolean yes) {
		if (!isMagic()) {
			return;
		}
		if (magicView.isFlipping()) {
			return; // a quick second click on No or Yes must not skip the reveal
		}
		if (magic.isDone()) {
			startMagic();
			return;
		}
		magic.answer(yes);
		magicView.dealt(magic.isDone() && magic.number() > 0);
		askMagic();
		repaint();
		if (magic.isDone()) {
			modeChanged.run();
		}
	}

	/** After the reveal: shows or hides how the trick works. */
	public void explain() {
		if (canExplain()) {
			magicView.toggleExplaining();
			askMagic();
			repaint();
			modeChanged.run();
		}
	}

	/** True after the reveal of a number (not after answers that put it on no card). */
	public boolean canExplain() {
		return isMagic() && magic.isDone() && magic.number() > 0;
	}

	public boolean isExplaining() {
		return isMagic() && magicView.isExplaining();
	}

	// ---- mouse ----

	private void openTab(int index) {
		if (index == MAGIC_TAB) {
			startMagic();
			return;
		}
		if (index == PICK_TAB) {
			startPicking();
			return;
		}
		if (index == DUEL_TAB) {
			startDuel();
			return;
		}
		Level target = Level.values()[index];
		if (records.isOpen(target)) {
			start(target);
		} else {
			notice("Win " + Level.values()[index - 1].label() + " with " + Records.STARS_TO_OPEN + " stars (par + 2 or better) to open "
					+ target.label() + ".");
		}
	}

	private void click(int x, int y) {
		requestFocusInWindow();
		layOut();
		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).contains(x, y)) {
				openTab(i);
				return;
			}
		}
		if (isDuel()) {
			int number = duelView.numberAt(duel, x, y);
			if (number > 0 || duel.isOver() && y > TABS_Y + TAB_HEIGHT) {
				guess(number);
			}
			return;
		}
		if (zoom.contains(x, y) && !isPicking() && !isMagic()) {
			guess(numberAt(x));
		}
	}

	private void hover(int x, int y) {
		layOut();
		int tab = -1;
		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).contains(x, y)) {
				tab = i;
			}
		}
		if (isDuel()) {
			boolean changed = duelView.hover(duel, x, y);
			if (tab != hoverTab || changed) {
				hoverTab = tab;
				setCursor(Cursor.getPredefinedCursor(tab >= 0 || duelView.hoverNumber(duel) > 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
				repaint();
			}
			return;
		}
		int over = zoom.contains(x, y) ? x : -1;
		if (tab != hoverTab || over != hoverX) {
			hoverTab = tab;
			hoverX = over;
			setCursor(Cursor.getPredefinedCursor(tab >= 0 || hoverNumber() > 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			repaint();
		}
	}

	/** The number a click on the zoomed line would guess now, or 0. */
	private int hoverNumber() {
		return hoverX >= 0 && game != null && !isOver() ? numberAt(hoverX) : 0;
	}

	/** The still possible number under {@code x} on the zoomed line. */
	private int numberAt(int x) {
		long count = (long) high() - low() + 1;
		long offset = (long) (x - zoom.x) * count / zoom.width;
		return (int) Math.max(low(), Math.min(high(), low() + offset));
	}

	// ---- drawing ----

	private void step() {
		double low = low(), high = high();
		shownLow += (low - shownLow) * 0.22;
		shownHigh += (high - shownHigh) * 0.22;
		if (Math.abs(shownLow - low) < 0.01 * max() / getWidth() + 0.01 && Math.abs(shownHigh - high) < 0.01 * max() / getWidth() + 0.01) {
			shownLow = low;
			shownHigh = high;
			slide.stop();
		}
		repaint();
	}

	private void layOut() {
		int width = getWidth();
		int count = MAGIC_TAB + 1;
		int gap = 6;
		int tabWidth = (width - 2 * MARGIN - gap * (count - 1)) / count;
		tabs.clear();
		for (int i = 0; i < count; i++) {
			tabs.add(new Rectangle(MARGIN + i * (tabWidth + gap), TABS_Y, tabWidth, TAB_HEIGHT));
		}
		zoom = new Rectangle(MARGIN, ZOOM_Y, width - 2 * MARGIN, ZOOM_HEIGHT);
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
		paintTabs(g);
		// The trick keeps its secret while the last card turns over.
		boolean flipping = isMagic() && magicView.isFlipping();
		g.setColor(flipping ? GOLD : headlineColor);
		g.setFont(new Font("SansSerif", Font.BOLD, 22));
		g.drawString(flipping ? "Let me see..." : headline, MARGIN, 86);
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 13));
		g.drawString(flipping ? "" : subline, MARGIN, 108);
		if (isMagic()) {
			magicView.paint(g, magic, new Rectangle(MARGIN, 122, getWidth() - 2 * MARGIN, getHeight() - 122 - 12));
			return;
		}
		if (isDuel()) {
			duelView.paint(g, duel, records, new Rectangle(MARGIN, 122, getWidth() - 2 * MARGIN, getHeight() - 122 - 12));
			return;
		}
		paintGuessMeter(g);
		paintFullLine(g);
		paintZoom(g);
		paintChips(g);
	}

	private void paintTabs(Graphics2D g) {
		for (int i = 0; i < tabs.size(); i++) {
			Rectangle tab = tabs.get(i);
			boolean duelTab = i == DUEL_TAB;
			boolean picking = i == PICK_TAB;
			boolean magicTab = i == MAGIC_TAB;
			Level tabLevel = i < DUEL_TAB ? Level.values()[i] : null;
			boolean selected = i == tab();
			boolean open = tabLevel == null || records.isOpen(tabLevel);
			g.setColor(selected ? SELECTED : i == hoverTab ? CARD_HOVER : CARD);
			g.fillRoundRect(tab.x, tab.y, tab.width, tab.height, 10, 10);
			if (selected) {
				g.setColor(POSSIBLE);
				g.fillRoundRect(tab.x + 8, tab.y + tab.height - 3, tab.width - 16, 3, 3, 3);
			}
			g.setFont(new Font("SansSerif", Font.BOLD, 13));
			g.setColor(open ? TEXT : DIM);
			String label = duelTab ? "Duel" : picking ? "You pick" : magicTab ? "Magic" : tabLevel.label();
			if (g.getFontMetrics().stringWidth(label) > tab.width - 6) {
				g.setFont(new Font("SansSerif", Font.BOLD, 11));
			}
			FontMetrics metrics = g.getFontMetrics();
			g.drawString(label, tab.x + (tab.width - metrics.stringWidth(label)) / 2, tab.y + 19);
			int centerX = tab.x + tab.width / 2;
			if (tabLevel == null) {
				g.setFont(new Font("SansSerif", Font.PLAIN, 11));
				g.setColor(SOFT);
				String sub = duelTab ? "Race me" : picking ? "I guess yours" : "Card trick";
				g.drawString(sub, centerX - g.getFontMetrics().stringWidth(sub) / 2, tab.y + 35);
			} else if (!open) {
				paintLock(g, centerX, tab.y + 31);
			} else {
				int stars = records.stars(tabLevel);
				for (int s = 0; s < 3; s++) {
					paintStar(g, centerX - 16 + s * 16, tab.y + 31, 6, s < stars ? GOLD : DIM);
				}
			}
		}
	}

	/** Dots for the guesses allowed: used ones filled, par marked by a gap. */
	private void paintGuessMeter(Graphics2D g) {
		if (isPicking()) {
			return;
		}
		int limit = level.limit();
		int used = game.getGuesses();
		int size = 9, gap = 4;
		int x = MARGIN;
		int y = 117;
		for (int i = 0; i < limit; i++) {
			int dotX = x + i * (size + gap) + (i >= level.par() ? 6 : 0);
			boolean usedDot = i < used;
			boolean par = i < level.par();
			g.setColor(usedDot ? (game.isFound() && i == used - 1 ? RIGHT : par ? SOFT : WARN) : par ? RULED_OUT : CARD);
			g.fillOval(dotX, y, size, size);
			if (!usedDot) {
				g.setColor(par ? DIM : WARN.darker().darker());
				g.drawOval(dotX, y, size, size);
			}
		}
	}

	private int xOn(Rectangle line, double value, double from, double to) {
		if (to <= from) {
			return line.x + line.width / 2;
		}
		return (int) Math.round(line.x + (value - from) / (to - from) * line.width);
	}

	private void paintFullLine(Graphics2D g) {
		Rectangle line = new Rectangle(MARGIN, FULL_Y, getWidth() - 2 * MARGIN, BAR_HEIGHT);
		int max = max();
		g.setColor(RULED_OUT);
		g.fillRoundRect(line.x, line.y, line.width, line.height, BAR_HEIGHT, BAR_HEIGHT);
		// Each number owns a slice of the line: number n covers n - 1 to n.
		int left = xOn(line, shownLow - 1, 0, max);
		int right = Math.max(left + 3, xOn(line, shownHigh, 0, max));
		boolean caught = isPicking() && reader.isCaught();
		if (!caught) {
			g.setColor(isOver() ? RIGHT : POSSIBLE);
			g.fillRoundRect(left, line.y, right - left, line.height, 6, 6);
		}
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.setColor(SOFT);
		g.drawString("1", line.x + 4, line.y + line.height + 14);
		String end = Level.commas(max);
		g.drawString(end, line.x + line.width - 4 - g.getFontMetrics().stringWidth(end), line.y + line.height + 14);
		// A marker over the line for each guess, the latest one labelled.
		List<Integer> guesses = guesses();
		for (int i = 0; i < guesses.size(); i++) {
			int guess = guesses.get(i);
			int x = xOn(line, guess - 0.5, 0, max);
			g.setColor(chipColor(i));
			g.fillPolygon(new int[] { x - 4, x + 4, x }, new int[] { line.y - 8, line.y - 8, line.y - 1 }, 3);
			if (i == guesses.size() - 1) {
				String label = Level.commas(guess);
				int width = g.getFontMetrics().stringWidth(label);
				int labelX = Math.max(line.x, Math.min(line.x + line.width - width, x - width / 2));
				g.drawString(label, labelX, line.y - 11);
			}
		}
		// The magnifier: the lit part of the top line opens out into the zoomed line below.
		if (!caught) {
			g.setColor(POSSIBLE_DARK);
			g.drawLine(left, line.y + line.height + 1, zoom.x, zoom.y - 2);
			g.drawLine(right, line.y + line.height + 1, zoom.x + zoom.width, zoom.y - 2);
		}
	}

	private void paintZoom(Graphics2D g) {
		Rectangle line = zoom;
		g.setColor(CARD);
		g.fillRoundRect(line.x, line.y, line.width, line.height, 8, 8);
		if (isPicking() && reader.isCaught()) {
			g.setColor(HIGH);
			g.setFont(new Font("SansSerif", Font.BOLD, 14));
			String text = "No number fits those answers";
			g.drawString(text, line.x + (line.width - g.getFontMetrics().stringWidth(text)) / 2, line.y + 23);
			paintZoomEnds(g, "", "");
			return;
		}
		int low = low(), high = high();
		long count = (long) high - low + 1;
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		FontMetrics metrics = g.getFontMetrics();
		if (count <= line.width / 4) {
			// Few enough to draw one cell per number.
			boolean labels = metrics.stringWidth(Level.commas(high)) + 4 <= line.width / count;
			for (long i = 0; i < count; i++) {
				int number = (int) (low + i);
				int x0 = xOn(line, i, 0, count), x1 = xOn(line, i + 1, 0, count);
				g.setColor(cellColor(number));
				g.fillRoundRect(x0 + 1, line.y + 3, x1 - x0 - 2, line.height - 6, 6, 6);
				if (labels) {
					String label = Level.commas(number);
					g.setColor(number == hoverNumber() || isOver() ? BACKGROUND : TEXT);
					g.drawString(label, (x0 + x1 - metrics.stringWidth(label)) / 2, line.y + 23);
				}
			}
		} else {
			g.setColor(POSSIBLE_DARK);
			g.fillRoundRect(line.x + 1, line.y + 3, line.width - 2, line.height - 6, 6, 6);
			// Ticks every tenth of what is left.
			g.setColor(POSSIBLE);
			for (int i = 1; i < 10; i++) {
				int x = line.x + line.width * i / 10;
				g.fillRect(x, line.y + (i == 5 ? 6 : 12), 1, line.height - (i == 5 ? 12 : 24));
			}
			if (hoverNumber() > 0) {
				int x0 = xOn(line, hoverNumber() - low, 0, count), x1 = xOn(line, hoverNumber() - low + 1, 0, count);
				g.setColor(GOLD);
				g.fillRect(x0, line.y + 3, Math.max(2, x1 - x0), line.height - 6);
			}
		}
		if (isPicking() && !reader.isOver()) {
			int guess = reader.guess();
			int x = xOn(line, guess - low + 0.5, 0, count);
			g.setColor(GOLD);
			g.fillPolygon(new int[] { x - 6, x + 6, x }, new int[] { line.y - 9, line.y - 9, line.y }, 3);
		}
		paintZoomEnds(g, low == high ? "" : Level.commas(low), low == high ? "" : Level.commas(high));
		if (hoverNumber() > 0) {
			paintBubble(g, hoverNumber(), xOn(line, hoverNumber() - low + 0.5, 0, count), zoom);
		}
	}

	private Color cellColor(int number) {
		if (isOver()) {
			return RIGHT;
		}
		if (number == hoverNumber()) {
			return GOLD;
		}
		if (isPicking() && number == reader.guess()) {
			return SELECTED;
		}
		return POSSIBLE_DARK;
	}

	private void paintZoomEnds(Graphics2D g, String low, String high) {
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.setColor(SOFT);
		int y = zoom.y + zoom.height + 14;
		g.drawString(low, zoom.x, y);
		g.drawString(high, zoom.x + zoom.width - g.getFontMetrics().stringWidth(high), y);
		String middle = isOver() ? "" : isPicking() ? "What is still possible" : "Click to guess";
		g.drawString(middle, zoom.x + (zoom.width - g.getFontMetrics().stringWidth(middle)) / 2, y);
	}

	/** The number under the pointer, in a bubble above {@code zoom}, the line it is on. */
	static void paintBubble(Graphics2D g, int number, int x, Rectangle zoom) {
		g.setFont(new Font("SansSerif", Font.BOLD, 14));
		String text = Level.commas(number);
		int width = g.getFontMetrics().stringWidth(text) + 16;
		int left = Math.max(zoom.x, Math.min(zoom.x + zoom.width - width, x - width / 2));
		int top = zoom.y - 30;
		g.setColor(GOLD);
		g.fillRoundRect(left, top, width, 24, 10, 10);
		g.fillPolygon(new int[] { x - 5, x + 5, x }, new int[] { top + 23, top + 23, top + 29 }, 3);
		g.setColor(BACKGROUND);
		g.drawString(text, left + 8, top + 17);
	}

	/** The colour of guess {@code index}: blue when too low, red when too high, green when right. */
	private Color chipColor(int index) {
		List<Integer> guesses = guesses();
		int guess = guesses.get(index);
		if (!isPicking()) {
			return guess < game.number() ? LOW : guess > game.number() ? HIGH : RIGHT;
		}
		if (index + 1 < guesses.size()) {
			return guesses.get(index + 1) > guess ? LOW : HIGH;
		}
		return reader.isFound() ? RIGHT : reader.isCaught() ? WARN : GOLD;
	}

	private void paintChips(Graphics2D g) {
		List<Integer> guesses = guesses();
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		FontMetrics metrics = g.getFontMetrics();
		int x = MARGIN, y = CHIPS_Y, height = 22;
		int right = getWidth() - MARGIN;
		if (isOver() && !(isPicking() && reader.isCaught())) {
			int stars = isPicking() ? 0 : game.stars();
			if (!isPicking()) {
				for (int s = 0; s < 3; s++) {
					paintStar(g, right - 72 + s * 26, y + 26, 11, s < stars ? GOLD : DIM);
				}
			}
		}
		for (int i = 0; i < guesses.size(); i++) {
			String label = Level.commas(guesses.get(i));
			int width = metrics.stringWidth(label) + 26;
			if (x + width > right - 90) {
				x = MARGIN;
				y += height + 6;
				if (y + height > getHeight()) {
					break;
				}
			}
			Color color = chipColor(i);
			g.setColor(CARD);
			g.fillRoundRect(x, y, width, height, 10, 10);
			g.setColor(color);
			g.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
			g.drawString(label, x + 8, y + 16);
			int arrowX = x + width - 11;
			if (color == LOW) {
				g.fillPolygon(new int[] { arrowX - 4, arrowX + 4, arrowX }, new int[] { y + 14, y + 14, y + 8 }, 3);
			} else if (color == HIGH) {
				g.fillPolygon(new int[] { arrowX - 4, arrowX + 4, arrowX }, new int[] { y + 8, y + 8, y + 14 }, 3);
			} else {
				g.fillOval(arrowX - 3, y + 8, 7, 7);
			}
			x += width + 6;
		}
	}

	static void paintStar(Graphics2D g, int centerX, int centerY, int radius, Color color) {
		int[] xs = new int[10], ys = new int[10];
		for (int i = 0; i < 10; i++) {
			double angle = -Math.PI / 2 + i * Math.PI / 5;
			double r = i % 2 == 0 ? radius : radius * 0.45;
			xs[i] = (int) Math.round(centerX + r * Math.cos(angle));
			ys[i] = (int) Math.round(centerY + r * Math.sin(angle));
		}
		g.setColor(color);
		g.fillPolygon(xs, ys, 10);
	}

	private static void paintLock(Graphics2D g, int centerX, int centerY) {
		g.setColor(DIM);
		g.drawRoundRect(centerX - 4, centerY - 8, 8, 10, 6, 6);
		g.fillRoundRect(centerX - 6, centerY - 3, 12, 9, 3, 3);
	}
}
