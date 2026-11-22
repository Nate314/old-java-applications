package simplealgorithms;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Self-check for the phone layout. Run with java -ea -Djava.awt.headless=true -Dsimplealgorithms.narrow=true: the
 * lab is 480 x 800, every tab can be picked by a click in its row, and on every page, in several states, nothing is
 * drawn outside the lab and the bar's buttons fit inside it without overlapping. Only the pages that take drags
 * listen for them, so on the others a swipe over an embedded lab scrolls the page around it.
 */
public class LayoutCheck {
	private static final int SENTINEL = new Color(255, 0, 255).getRGB();

	public static void main(String[] args) throws Exception {
		// The lab's timer keeps the JVM alive, so it exits either way.
		try {
			run();
		} catch (Throwable failure) {
			failure.printStackTrace();
			System.exit(1);
		}
		System.out.println("LayoutCheck passed");
		System.exit(0);
	}

	private static void run() throws Exception {
		check(Page.NARROW, "run with -Dsimplealgorithms.narrow=true");
		check(LabPanel.WIDTH == 480 && LabPanel.HEIGHT == 800, "the narrow lab is 480 x 800");
		SortPage sort = new SortPage();
		// Sound off before the panel's timer starts, so the race never opens a synthesizer.
		Field sound = SortPage.class.getDeclaredField("sound");
		sound.setAccessible(true);
		((SortSound) sound.get(sort)).toggle();
		check(!((SortSound) sound.get(sort)).isOn(), "the race is silent");
		Page[] pages = { sort, new HanoiPage(), new RootPage(), new TaylorPage(), new PathPage(), new DividePage(), new WeighPage(), new PrintoutPage() };
		LabPanel lab = new LabPanel(pages);
		lab.setSize(LabPanel.WIDTH, LabPanel.HEIGHT);

		Method press = LabPanel.class.getDeclaredMethod("press", int.class, int.class);
		press.setAccessible(true);
		Method tabX = LabPanel.class.getDeclaredMethod("tabX", int.class, int.class);
		tabX.setAccessible(true);
		Field current = LabPanel.class.getDeclaredField("current");
		current.setAccessible(true);
		// Two rows of four tabs, each picked by a click on its middle.
		for (int i = pages.length - 1; i >= 0; i--) {
			int left = (int) tabX.invoke(lab, i, LabPanel.WIDTH);
			int right = i % 4 == 3 ? LabPanel.WIDTH : (int) tabX.invoke(lab, i + 1, LabPanel.WIDTH);
			press.invoke(lab, (left + right) / 2, i / 4 * 36 + 18);
			check((int) current.get(lab) == i, "a click on tab " + pages[i].title + " shows it");
			fits(lab, pages[i].title);
			check((lab.getMouseMotionListeners().length == 2) == (pages[i] instanceof PathPage),
					pages[i].title + ": listens for drags only if it takes them");
		}

		// Other states: keys each page takes, a few presses each, checked after every press.
		String[] keys = { "svsvbbbnoooo", "s-------++++++++ssbr", "nnnnnsss", "c++++++cp", "mamaaaaxs", "csssssssssscnnnnn", "?   w++++    ", "h" };
		for (int i = 0; i < pages.length; i++) {
			show(lab, i);
			for (char key : keys[i].toCharArray()) {
				key(lab, key);
				fits(lab, pages[i].title + " after " + key);
				if (pages[i] == sort) {
					check((lab.getMouseMotionListeners().length == 2) == sort.drags(), "Sorting race: listens for drags only in Prints");
				}
			}
		}
		// Weigh until sorted: the results list fits too.
		show(lab, 6);
		for (int i = 0; i < 25; i++) {
			key(lab, ' ');
		}
		fits(lab, "Weigh, sorted");
	}

	private static void show(LabPanel lab, int index) throws Exception {
		Method show = LabPanel.class.getDeclaredMethod("show", int.class);
		show.setAccessible(true);
		show.invoke(lab, index);
	}

	private static void key(LabPanel lab, char c) throws Exception {
		Method m = LabPanel.class.getDeclaredMethod("keyPressed", KeyEvent.class);
		m.setAccessible(true);
		int code = c == ' ' ? KeyEvent.VK_SPACE : c == '-' ? KeyEvent.VK_MINUS : KeyEvent.getExtendedKeyCodeForChar(c);
		m.invoke(lab, new KeyEvent(lab, KeyEvent.KEY_PRESSED, 0, 0, code, c));
	}

	/** Paints the lab on a bigger sheet: nothing may land outside it, and the bar's buttons sit in the bar, apart. */
	private static void fits(LabPanel lab, String what) throws Exception {
		BufferedImage image = new BufferedImage(LabPanel.WIDTH + 200, LabPanel.HEIGHT + 100, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(SENTINEL));
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
		// paintComponent itself, unclipped (paint would clip to the lab and hide anything drawn past it).
		lab.paintComponent(g);
		g.dispose();
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				// The graph paper's lines end on the edge pixel itself (cut off on screen), so the check starts one past it.
				if (x > LabPanel.WIDTH || y > LabPanel.HEIGHT) {
					check(image.getRGB(x, y) == SENTINEL, what + ": drawn outside the lab at " + x + ", " + y);
				}
			}
		}
		Field field = LabPanel.class.getDeclaredField("pages");
		field.setAccessible(true);
		Field current = LabPanel.class.getDeclaredField("current");
		current.setAccessible(true);
		Page page = ((Page[]) field.get(lab))[(int) current.get(lab)];
		for (Page.Button button : page.buttons) {
			Rectangle r = button.bounds;
			check(r.x >= 8 && r.x + r.width <= LabPanel.WIDTH - 8 && r.y >= LabPanel.HEIGHT - 96 && r.y + r.height <= LabPanel.HEIGHT,
					what + ": button " + button.label + " in the bar, at " + r);
			for (Page.Button other : page.buttons) {
				check(other == button || !other.bounds.intersects(r), what + ": " + button.label + " and " + other.label + " apart");
			}
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}
}
