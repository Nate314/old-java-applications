package musictheoryexpert;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * The keyboard's hit test agrees with its drawing, in one row (wide tabs) and in two rows (phones): every key from
 * C3 to C7 shows, and every pixel painted for a key answers that key.
 */
public class KeyboardCheck {
	public static void main(String[] args) {
		check(720, 116, false);
		check(368, 116, true);
		check(368, 108, true);
		check(368, 60, false);
		// Two rows double a phone's key width.
		assert whiteWidth(368, 116) >= 2 * whiteWidth(368, 60) - 1 : whiteWidth(368, 116) + " vs " + whiteWidth(368, 60);
		// C5 sits over C3 in two rows.
		assert left(368, 116, Look.SPLIT) == left(368, 116, Look.LOW_NOTE);
		// A lit key wears its name, white or black, wide or narrow, and the name stays inside the key.
		for (int note : new int[] { 60, 61, 66, 95 }) {
			assert named(720, 116, note, true) && named(368, 116, note, true) : "no name on " + note;
		}
		// A black key too thin for its name (the Ear training tab's 375 px keyboard) has it written just under it.
		assert named(375, 90, 61, false) : "no name under C#4";
		System.out.println("KeyboardCheck passed");
	}

	private static void check(int width, int height, boolean twoRows) {
		assert Look.twoRows(width < Look.NARROW, height) == twoRows : width + "x" + height;
		for (int note = Look.LOW_NOTE; note <= Look.HIGH_NOTE; note++) {
			BufferedImage image = paint(width, height, note);
			int hits = 0;
			// Inner pixels only: the round corners leave a pixel or two of the hit box unpainted.
			for (int y = 2; y < height - 2; y++) {
				for (int x = 2; x < width - 2; x++) {
					if (inside(image, x, y)) {
						hits++;
						int at = Look.keyAt(0, 0, width, height, width < Look.NARROW, x, y);
						assert at == note : "pixel " + x + "," + y + " of " + note + " answers " + at + " at " + width + "x" + height;
					}
				}
			}
			assert hits > 20 : note + " barely shows at " + width + "x" + height;
		}
		assert Look.keyAt(0, 0, width, height, width < Look.NARROW, width / 2, height) == -1;
	}

	private static boolean inside(BufferedImage image, int x, int y) {
		for (int dy = -2; dy <= 2; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				if (image.getRGB(x + dx, y + dy) != Color.RED.getRGB()) {
					return false;
				}
			}
		}
		return true;
	}

	private static BufferedImage paint(int width, int height, int note) {
		return paint(width, height, note, null);
	}

	private static BufferedImage paint(int width, int height, int note, String[] names) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		Color[] colors = new Color[Look.HIGH_NOTE + 1];
		colors[note] = Color.RED;
		Look.paintKeyboard(g, 0, 0, width, height, width < Look.NARROW, colors, names);
		g.dispose();
		return image;
	}

	/** True when naming the red key {@code note} paints its name, on that key only when {@code onKey}. */
	private static boolean named(int width, int height, int note, boolean onKey) {
		String[] names = new String[Look.HIGH_NOTE + 1];
		names[note] = Look.isBlack(note) ? "F#" : "G";
		BufferedImage plain = paint(width, height, note), image = paint(width, height, note, names);
		int text = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (image.getRGB(x, y) != plain.getRGB(x, y)) {
					int at = Look.keyAt(0, 0, width, height, width < Look.NARROW, x, y);
					assert !onKey || at == note : "the name of " + note + " spills onto " + at + " at " + x + "," + y;
					text++;
				}
			}
		}
		return text > 10;
	}

	/** The width of C3's key in red pixels along its bottom row. */
	private static int whiteWidth(int width, int height) {
		BufferedImage image = paint(width, height, Look.LOW_NOTE);
		int count = 0;
		for (int x = 0; x < width; x++) {
			count += image.getRGB(x, height - 3) == Color.RED.getRGB() ? 1 : 0;
		}
		return count;
	}

	/** The leftmost red pixel of {@code note}. */
	private static int left(int width, int height, int note) {
		BufferedImage image = paint(width, height, note);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (image.getRGB(x, y) == Color.RED.getRGB()) {
					return x;
				}
			}
		}
		return -1;
	}
}
