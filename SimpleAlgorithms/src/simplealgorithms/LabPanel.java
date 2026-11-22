package simplealgorithms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The Algorithm Lab: a sheet of graph paper with a tab per page across the top (keys 1 to 8) and the page's
 * buttons along the bottom. It routes clicks, keys and a 30 ms tick to the page that shows.
 */
@SuppressWarnings("serial")
public class LabPanel extends JPanel {
	static final int WIDTH = Page.NARROW ? 480 : 720;
	static final int HEIGHT = Page.NARROW ? 800 : 480;
	/** Narrow, the tabs take two rows of four and the buttons wrap onto two rows of taller (touch) buttons. */
	private static final int TAB_ROWS = Page.NARROW ? 2 : 1;
	private static final int TAB_ROW = Page.NARROW ? 36 : 40;
	private static final int TABS = TAB_ROWS * TAB_ROW;
	private static final int BAR = Page.NARROW ? 96 : 50;
	private static final int BUTTON = Page.NARROW ? 34 : 30;

	private final Page[] pages;
	private int current;
	private boolean dragsOn;
	private final MouseAdapter dragger = new MouseAdapter() {
		@Override
		public void mouseDragged(MouseEvent event) {
			if (dragging) {
				page().drag(event.getX(), event.getY() - TABS);
				repaint();
			}
		}
	};
	private Page.Button pressed;
	/** True while the button is held after a click on the page's area. */
	private boolean dragging;

	public LabPanel(Page... pages) {
		this.pages = pages;
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setFocusable(true);
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				requestFocusInWindow();
				press(event.getX(), event.getY());
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				if (dragging) {
					dragging = false;
					page().release();
					repaint();
				}
				if (pressed != null) {
					pressed = null;
					repaint();
				}
			}

			@Override
			public void mouseMoved(MouseEvent event) {
				int y = event.getY() - TABS;
				boolean inPage = y >= 0 && y < pageHeight();
				page().hover(inPage ? event.getX() : -1, inPage ? y : -1);
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent event) {
				page().hover(-1, -1);
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		syncDrags();
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				LabPanel.this.keyPressed(event);
			}
		});
		new Timer(30, event -> {
			if (page().tick()) {
				repaint();
			}
		}).start();
	}

	private Page page() {
		return pages[current];
	}

	/**
	 * The drag listener is on only while the page showing takes drags (drawing walls, scrubbing prints): in the
	 * browser a panel with one keeps a finger's swipe for itself, so on the other pages an embedded lab lets the
	 * host page scroll.
	 */
	private void syncDrags() {
		if (page().drags() != dragsOn) {
			dragsOn = !dragsOn;
			if (dragsOn) {
				addMouseMotionListener(dragger);
			} else {
				removeMouseMotionListener(dragger);
			}
		}
	}

	private int pageHeight() {
		return getHeight() - TABS - BAR;
	}

	private void show(int index) {
		if (index != current && index >= 0 && index < pages.length) {
			if (dragging) {
				dragging = false;
				page().release();
			}
			page().hidden();
			current = index;
			syncDrags();
			repaint();
		}
	}

	private void press(int x, int y) {
		if (y < TABS) {
			int row = y / TAB_ROW;
			for (int i = pages.length - 1; i >= 0; i--) {
				if (tabRow(i) == row && x >= tabX(i, getWidth())) {
					show(i);
					return;
				}
			}
			return;
		}
		if (y >= getHeight() - BAR) {
			for (Page.Button button : page().buttons) {
				if (button.bounds.contains(x, y)) {
					pressed = button;
					button.action.run();
				}
			}
			repaint();
			return;
		}
		dragging = true;
		page().click(x, y - TABS);
		syncDrags();
		repaint();
	}

	private void keyPressed(KeyEvent event) {
		char key = Character.toLowerCase(event.getKeyChar());
		if (page().key(event)) {
			syncDrags();
			repaint();
			return;
		}
		if (key >= '1' && key < '1' + pages.length) {
			show(key - '1');
			return;
		}
		for (Page.Button button : page().buttons) {
			if (button.key == key) {
				button.action.run();
				repaint();
				return;
			}
		}
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		int w = getWidth();
		int h = getHeight();
		g.setColor(Page.PAPER);
		g.fillRect(0, 0, w, h);
		g.setColor(Page.GRID);
		for (int x = 0; x < w; x += 16) {
			g.drawLine(x, TABS, x, h - BAR);
		}
		for (int y = TABS; y < h - BAR; y += 16) {
			g.drawLine(0, y, w, y);
		}
		paintTabs(g, w);
		Graphics2D content = (Graphics2D) g.create();
		content.translate(0, TABS);
		page().paint(content, w, pageHeight());
		content.dispose();
		paintBar(g, w, h);
	}

	private void paintTabs(Graphics2D g, int w) {
		g.setColor(Page.INK);
		g.fillRect(0, 0, w, TABS);
		for (int i = 0; i < pages.length; i++) {
			int x = tabX(i, w);
			int tabWidth = (i + 1 < pages.length && tabRow(i + 1) == tabRow(i) ? tabX(i + 1, w) : w) - x;
			int top = tabRow(i) * TAB_ROW;
			boolean on = i == current;
			if (on) {
				g.setColor(Page.PAPER);
				// Narrow, a tab in the top row can't reach the paper, so the picked one is a pill in its row.
				g.fillRoundRect(x + 4, top + 6, tabWidth - 8, Page.NARROW ? TAB_ROW - 10 : TABS, 12, 12);
			}
			// Phones have no number keys to match, so narrow tabs drop the numbers.
			String label = Page.NARROW ? pages[i].title : (i + 1) + "  " + pages[i].title;
			Page.centered(g, label, Page.BOLD, on ? Page.INK : new Color(200, 212, 232), x + tabWidth / 2, top + (Page.NARROW ? 24 : 28));
		}
	}

	private int tabRow(int i) {
		return i * TAB_ROWS / pages.length;
	}

	/** Left edge of tab i in its row: each tab is as wide as its label (by letters) plus a margin, so eight fit in 720 px. */
	private int tabX(int i, int w) {
		int before = 0;
		int total = 0;
		for (int j = 0; j < pages.length; j++) {
			if (tabRow(j) != tabRow(i)) {
				continue;
			}
			int share = pages[j].title.length() + (Page.NARROW ? 4 : 9);
			total += share;
			if (j < i) {
				before += share;
			}
		}
		return before * w / total;
	}

	private void paintBar(Graphics2D g, int w, int h) {
		int top = h - BAR;
		g.setColor(new Color(240, 235, 222));
		g.fillRect(0, top, w, BAR);
		g.setColor(Page.PENCIL);
		g.drawLine(0, top, w, top);
		int gap = 8;
		// Rows of buttons that fit the width (one row when wide), each row centered.
		java.util.List<Page.Button> buttons = page().buttons;
		java.util.List<Integer> ends = new java.util.ArrayList<>();
		for (int first = 0; first < buttons.size();) {
			int total = buttonWidth(g, buttons.get(first));
			int end = first + 1;
			while (end < buttons.size() && (!Page.NARROW || total + gap + buttonWidth(g, buttons.get(end)) <= w - 16)) {
				total += gap + buttonWidth(g, buttons.get(end));
				end++;
			}
			ends.add(end);
			first = end;
		}
		int y = top + (BAR - ends.size() * (BUTTON + gap) + gap) / 2;
		int first = 0;
		for (int end : ends) {
			int total = -gap;
			for (int i = first; i < end; i++) {
				total += buttonWidth(g, buttons.get(i)) + gap;
			}
			int x = (w - total) / 2;
			for (int i = first; i < end; i++) {
				paintButton(g, buttons.get(i), x, y);
				x += buttonWidth(g, buttons.get(i)) + gap;
			}
			first = end;
			y += BUTTON + gap;
		}
	}

	private void paintButton(Graphics2D g, Page.Button button, int x, int y) {
		int bw = buttonWidth(g, button);
		int baseline = y + BUTTON / 2 + 5;
		button.bounds.setBounds(x, y, bw, BUTTON);
		g.setColor(button == pressed ? Page.HIGHLIGHT : Color.WHITE);
		g.fillRoundRect(x, y, bw, BUTTON, 14, 14);
		g.setColor(Page.INK);
		g.setStroke(new BasicStroke(1.5f));
		g.drawRoundRect(x, y, bw, BUTTON, 14, 14);
		g.setStroke(new BasicStroke(1f));
		Page.text(g, button.label, Page.BOLD, Page.INK, x + 12, baseline);
		if (!Page.NARROW) {
			Page.text(g, keyName(button.key), Page.SMALL, Page.PENCIL, x + bw - 12 - Page.width(g, keyName(button.key), Page.SMALL), baseline);
		}
	}

	private static String keyName(char key) {
		return key == ' ' ? "Space" : String.valueOf(Character.toUpperCase(key));
	}

	/** Narrow (a phone) the key names are left off, so the buttons fit two rows. */
	private static int buttonWidth(Graphics2D g, Page.Button button) {
		int key = Page.NARROW ? 0 : 10 + Page.width(g, keyName(button.key), Page.SMALL);
		return 12 + Page.width(g, button.label, Page.BOLD) + key + 12;
	}
}
