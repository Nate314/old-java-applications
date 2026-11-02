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
 * The Algorithm Lab: a sheet of graph paper with a tab per page across the top (keys 1 to 4) and the page's
 * buttons along the bottom. It routes clicks, keys and a 30 ms tick to the page that shows.
 */
@SuppressWarnings("serial")
public class LabPanel extends JPanel {
	static final int WIDTH = 720;
	static final int HEIGHT = 480;
	private static final int TABS = 40;
	private static final int BAR = 50;

	private final Page[] pages;
	private int current;
	private Page.Button pressed;

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

	private int pageHeight() {
		return getHeight() - TABS - BAR;
	}

	private void show(int index) {
		if (index != current && index >= 0 && index < pages.length) {
			page().hidden();
			current = index;
			repaint();
		}
	}

	private void press(int x, int y) {
		if (y < TABS) {
			show(x * pages.length / getWidth());
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
		page().click(x, y - TABS);
		repaint();
	}

	private void keyPressed(KeyEvent event) {
		char key = Character.toLowerCase(event.getKeyChar());
		if (page().key(event)) {
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
			int x = i * w / pages.length;
			int tabWidth = (i + 1) * w / pages.length - x;
			boolean on = i == current;
			if (on) {
				g.setColor(Page.PAPER);
				g.fillRoundRect(x + 4, 6, tabWidth - 8, TABS, 12, 12);
			}
			String label = (i + 1) + "  " + pages[i].title;
			Page.centered(g, label, Page.BOLD, on ? Page.INK : new Color(200, 212, 232), x + tabWidth / 2, 28);
		}
	}

	private void paintBar(Graphics2D g, int w, int h) {
		int top = h - BAR;
		g.setColor(new Color(240, 235, 222));
		g.fillRect(0, top, w, BAR);
		g.setColor(Page.PENCIL);
		g.drawLine(0, top, w, top);
		int gap = 8;
		int total = -gap;
		for (Page.Button button : page().buttons) {
			total += buttonWidth(g, button) + gap;
		}
		int x = (w - total) / 2;
		for (Page.Button button : page().buttons) {
			int bw = buttonWidth(g, button);
			button.bounds.setBounds(x, top + 10, bw, 30);
			g.setColor(button == pressed ? Page.HIGHLIGHT : Color.WHITE);
			g.fillRoundRect(x, top + 10, bw, 30, 14, 14);
			g.setColor(Page.INK);
			g.setStroke(new BasicStroke(1.5f));
			g.drawRoundRect(x, top + 10, bw, 30, 14, 14);
			g.setStroke(new BasicStroke(1f));
			Page.text(g, button.label, Page.BOLD, Page.INK, x + 12, top + 30);
			Page.text(g, keyName(button.key), Page.SMALL, Page.PENCIL, x + bw - 12 - Page.width(g, keyName(button.key), Page.SMALL), top + 30);
			x += bw + gap;
		}
	}

	private static String keyName(char key) {
		return key == ' ' ? "Space" : String.valueOf(Character.toUpperCase(key));
	}

	private static int buttonWidth(Graphics2D g, Page.Button button) {
		return 12 + Page.width(g, button.label, Page.BOLD) + 10 + Page.width(g, keyName(button.key), Page.SMALL) + 12;
	}
}
