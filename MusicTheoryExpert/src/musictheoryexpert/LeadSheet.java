package musictheoryexpert;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The Songwriter's song written out as a lead sheet: a treble staff with the key signature and 4/4, the chord
 * names over the bars, and the melody as notes and rests (or a slash a beat when there is no melody yet), four bars
 * a line (two while a song of up to four bars has a melody, to give its notes room), ending in a repeat sign because the song loops.
 */
final class LeadSheet {
	/** Written lengths in eighths, longest first: whole, dotted half, half, dotted quarter, quarter, eighth. */
	static final int[] VALUES = { 8, 6, 4, 3, 2, 1 };
	/** Marks for {@link #accidentals}: no sign, or the sign to write (-1 flat, 0 natural, 1 sharp). */
	static final int NONE = 2;
	/** A stem not on a beam. */
	private static final int NO_BEAM = Integer.MIN_VALUE;
	/** A line of music's height before scaling: the chord names, the staff, and room for ledger lines and stems. */
	private static final int LINE_HEIGHT = 110;
	private static final String LETTERS = "CDEFGAB";
	/** Staff steps of the key signature's sharps and flats on a treble staff (F5 is 38, the top line). */
	private static final int[] SHARP_STEPS = { 38, 35, 39, 36, 33, 37, 34 }, FLAT_STEPS = { 34, 37, 33, 36, 32, 35, 31 };
	private static final Color PAPER = new Color(250, 247, 238), INK = new Color(32, 32, 40);
	private static final Color NOW = new Color(255, 226, 150), NOW_INK = new Color(196, 120, 0);

	/** One written note, chord or rest: its start in eighths from the song's start, its written length, its pitches (none for a rest), and whether it is tied to the next mark. */
	static final class Mark {
		final int slot, length;
		final int[] pitches;
		final boolean tied;

		Mark(int slot, int length, int[] pitches, boolean tied) {
			this.slot = slot;
			this.length = length;
			this.pitches = pitches;
			this.tied = tied;
		}

		boolean isRest() {
			return pitches.length == 0;
		}
	}

	private LeadSheet() {
	}

	/**
	 * The melody ({slot, pitch, length} notes) as marks over {@code bars} bars. Notes that start together are one
	 * chord as long as its longest note; a note still sounding when the next one starts is written up to it. Every
	 * gap is a rest, and a note over a bar line, or of a length no single note has, is written as tied notes.
	 */
	static List<Mark> marks(List<int[]> melody, int bars) {
		int end = bars * Progression.EIGHTHS;
		TreeMap<Integer, List<int[]>> starts = new TreeMap<>();
		for (int[] note : melody) {
			if (note[0] >= 0 && note[0] < end) {
				starts.computeIfAbsent(note[0], k -> new ArrayList<>()).add(note);
			}
		}
		List<Mark> marks = new ArrayList<>();
		int at = 0;
		for (Map.Entry<Integer, List<int[]>> start : starts.entrySet()) {
			int slot = start.getKey(), length = 0;
			TreeSet<Integer> pitches = new TreeSet<>();
			for (int[] note : start.getValue()) {
				length = Math.max(length, note[2]);
				pitches.add(note[1]);
			}
			Integer next = starts.higherKey(slot);
			int stop = Math.min(slot + length, next == null ? end : next);
			write(marks, at, slot, new int[0]);
			write(marks, slot, stop, pitches.stream().mapToInt(Integer::intValue).toArray());
			at = stop;
		}
		write(marks, at, end, new int[0]);
		return marks;
	}

	/** From {@code from} to {@code to}, split at the bar lines and into written lengths; a note's pieces are tied. */
	private static void write(List<Mark> marks, int from, int to, int[] pitches) {
		while (from < to) {
			int room = Math.min(to, (from / Progression.EIGHTHS + 1) * Progression.EIGHTHS) - from;
			int value = 1;
			for (int v : VALUES) {
				if (v <= room) {
					value = v;
					break;
				}
			}
			marks.add(new Mark(from, value, pitches, pitches.length > 0 && from + value < to));
			from += value;
		}
	}

	/**
	 * The eighth notes beamed together, as {first, last} mark indexes: each run of two or more eighth notes (not
	 * rests) in the same half bar, as 4/4 is beamed so the beats 1-2 and 3-4 can be read at a glance.
	 */
	static List<int[]> beams(List<Mark> marks) {
		List<int[]> beams = new ArrayList<>();
		for (int first = 0; first < marks.size(); first++) {
			int last = first;
			while (isEighth(marks, first) && isEighth(marks, last + 1) && marks.get(last + 1).slot / 4 == marks.get(first).slot / 4) {
				last++;
			}
			if (last > first) {
				beams.add(new int[] { first, last });
				first = last;
			}
		}
		return beams;
	}

	private static boolean isEighth(List<Mark> marks, int i) {
		return i < marks.size() && marks.get(i).length == 1 && !marks.get(i).isRest();
	}

	/** {staff step, accidental} for a pitch spelled in the key at {@code position}: C4 is step 28, F5 step 38. */
	static int[] spell(int pitch, int position) {
		String name = Progression.noteName(pitch, position);
		int accidental = name.length() == 1 ? 0 : name.charAt(1) == '#' ? 1 : -1;
		int natural = pitch - accidental;
		return new int[] { (Math.floorDiv(natural, 12) - 1) * 7 + LETTERS.indexOf(name.charAt(0)), accidental };
	}

	/** What the key signature at {@code position} does to each letter, C to B: 1 sharp, -1 flat, 0 nothing. */
	static int[] signature(int position) {
		int p = Math.floorMod(position, 12);
		int[] letters = new int[7];
		for (int i = 0; i < (p <= 6 ? p : 12 - p); i++) {
			letters[LETTERS.indexOf((p <= 6 ? "FCGDAEB" : "BEADGCF").charAt(i))] = p <= 6 ? 1 : -1;
		}
		return letters;
	}

	/**
	 * The sign to write before each pitch of each mark ({@link #NONE} for none): only where the note differs from
	 * the key signature or from an earlier sign on that line of the staff in the same bar. A note tied over from the
	 * mark before needs none.
	 */
	static List<int[]> accidentals(List<Mark> marks, int position) {
		int[] key = signature(position);
		List<int[]> signs = new ArrayList<>();
		Map<Integer, Integer> inBar = new TreeMap<>();
		int bar = -1;
		Mark before = null;
		for (Mark mark : marks) {
			if (mark.slot / Progression.EIGHTHS != bar) {
				bar = mark.slot / Progression.EIGHTHS;
				inBar.clear();
			}
			int[] sign = new int[mark.pitches.length];
			for (int i = 0; i < sign.length; i++) {
				int[] spelled = spell(mark.pitches[i], position);
				int current = inBar.getOrDefault(spelled[0], key[Math.floorMod(spelled[0], 7)]);
				boolean tiedOver = before != null && before.tied;
				sign[i] = spelled[1] == current || tiedOver ? NONE : spelled[1];
				inBar.put(spelled[0], spelled[1]);
			}
			signs.add(sign);
			before = mark;
		}
		return signs;
	}

	// ---- drawing ----

	/**
	 * Draws the sheet in {@code box}: {@code playing} is the bar playing (or -1) and {@code beat} its beat, so the
	 * bar is shaded and the notes sounding are inked in gold.
	 */
	static void paint(Graphics2D g, Rectangle box, Progression song, String title, int playing, int beat) {
		g.setColor(PAPER);
		g.fillRoundRect(box.x, box.y, box.width, box.height, 10, 10);
		g.setColor(INK);
		g.setFont(new Font("Serif", Font.BOLD, 22));
		g.drawString(title, box.x + (box.width - g.getFontMetrics().stringWidth(title)) / 2, box.y + 30);
		g.setFont(new Font("Serif", Font.ITALIC, 14));
		String key = song.keyName();
		g.drawString(key, box.x + box.width - 18 - g.getFontMetrics().stringWidth(key), box.y + 30);
		// The tempo as a quarter note "= 100".
		int qx = box.x + 22, qy = box.y + 28;
		g.fillOval(qx, qy - 4, 8, 6);
		g.drawLine(qx + 7, qy - 2, qx + 7, qy - 16);
		g.drawString("= " + song.tempo(), qx + 14, qy + 2);
		if (song.size() == 0) {
			g.setFont(new Font("Serif", Font.ITALIC, 16));
			String hint = "Pick chords (1 to 7) and they are written out here.";
			g.drawString(hint, box.x + (box.width - g.getFontMetrics().stringWidth(hint)) / 2, box.y + box.height / 2);
			return;
		}
		int bars = song.size();
		List<Mark> marks = song.melody().isEmpty() ? new ArrayList<>() : marks(song.melody(), bars);
		int perLine = !marks.isEmpty() && bars <= 4 ? 2 : 4;
		int lines = (bars + perLine - 1) / perLine;
		int gap = 8;
		int p = Math.floorMod(song.position(), 12);
		boolean sharps = p <= 6;
		int signs = sharps ? p : 12 - p;
		int firstBar = 34 + signs * 9 + 26;
		// The music is drawn for an 8 px staff gap, then scaled to fill the sheet: at most 1.3 times, and only as
		// much as leaves a melody's eighth notes 16 px apart.
		double fit = Math.min((box.height - 96) / (double) (lines * LINE_HEIGHT),
				(box.width - 36) / (double) (firstBar + perLine * (marks.isEmpty() ? 100 : 20 + 16 * Progression.EIGHTHS)));
		double scale = Math.max(0.6, Math.min(1.3, fit));
		Graphics2D sheet = g;
		g = (Graphics2D) sheet.create();
		g.translate(box.x + 18, box.y + 46 + Math.max(0, (int) (box.height - 96 - lines * LINE_HEIGHT * scale) / 2));
		g.scale(scale, scale);
		int left = 0, right = (int) ((box.width - 36) / scale);
		int barWidth = (right - firstBar) / perLine;
		List<int[]> signsOf = accidentals(marks, song.position());
		int now = playing < 0 ? -1 : playing * Progression.EIGHTHS + beat * 2;
		// Stems: each mark's direction and, for beamed eighths, where its stem meets the beam (y from the staff's
		// top line; NO_BEAM for a stem of its own length, with a flag on an eighth).
		boolean[] upOf = new boolean[marks.size()];
		int[] stemTo = new int[marks.size()];
		for (int m = 0; m < marks.size(); m++) {
			int[] range = range(marks.get(m), song.position());
			upOf[m] = range[0] + range[1] < 68;
			stemTo[m] = NO_BEAM;
		}
		List<int[]> beamLines = new ArrayList<>(); // {first mark, x0, y0, x1, y1, up}
		for (int[] beam : beams(marks)) {
			int sum = 0;
			for (int m = beam[0]; m <= beam[1]; m++) {
				int[] range = range(marks.get(m), song.position());
				sum += range[0] + range[1];
			}
			boolean up = sum < 68 * (beam[1] - beam[0] + 1); // the way the group's notes lean from the middle line
			int x0 = slotX(marks.get(beam[0]).slot, firstBar, perLine, barWidth), x1 = slotX(marks.get(beam[1]).slot, firstBar, perLine, barWidth);
			int y0 = stemEnd(range(marks.get(beam[0]), song.position()), up, gap), y1 = stemEnd(range(marks.get(beam[1]), song.position()), up, gap);
			y1 = Math.max(y0 - gap, Math.min(y0 + gap, y1)); // a beam rises or falls at most a staff space
			int shift = 0;
			for (int m = beam[0]; m <= beam[1]; m++) {
				int[] range = range(marks.get(m), song.position());
				int at = y0 + (y1 - y0) * (slotX(marks.get(m).slot, firstBar, perLine, barWidth) - x0) / (x1 - x0);
				// Every stem at least two and a half staff spaces long.
				int limit = up ? stepY(0, gap, range[1]) - 5 * gap / 2 : stepY(0, gap, range[0]) + 5 * gap / 2;
				shift = up ? Math.max(shift, at - limit) : Math.min(shift, at - limit);
			}
			y0 -= shift;
			y1 -= shift;
			for (int m = beam[0]; m <= beam[1]; m++) {
				upOf[m] = up;
				stemTo[m] = y0 + (y1 - y0) * (slotX(marks.get(m).slot, firstBar, perLine, barWidth) - x0) / (x1 - x0);
			}
			beamLines.add(new int[] { beam[0], x0, y0, x1, y1, up ? 1 : 0 });
		}
		for (int line = 0; line < lines; line++) {
			int top = line * LINE_HEIGHT + 42;
			int lastBar = Math.min(bars, (line + 1) * perLine) - line * perLine;
			int end = firstBar + lastBar * barWidth;
			if (playing >= line * perLine && playing < line * perLine + lastBar) {
				g.setColor(NOW);
				g.fillRect(firstBar + (playing % perLine) * barWidth + 1, top - 3 * gap, barWidth - 1, 8 * gap);
			}
			g.setColor(INK);
			g.setStroke(new BasicStroke(1f));
			for (int i = 0; i < 5; i++) {
				g.drawLine(left, top + i * gap, end, top + i * gap);
			}
			g.drawLine(left, top, left, top + 4 * gap);
			Look.paintTrebleClef(g, left + 14, top, gap);
			for (int i = 0; i < signs; i++) {
				paintSign(g, left + 42 + i * 9, stepY(top, gap, sharps ? SHARP_STEPS[i] : FLAT_STEPS[i]), sharps ? 1 : -1);
			}
			if (line == 0) {
				g.setFont(new Font("Serif", Font.BOLD, 19));
				int tx = firstBar - 20;
				g.drawString("4", tx, top + 2 * gap - 1);
				g.drawString("4", tx, top + 4 * gap - 1);
			}
			for (int b = 0; b < lastBar; b++) {
				int index = line * perLine + b;
				int bx = firstBar + b * barWidth;
				g.setColor(index == playing ? NOW_INK : INK);
				g.setFont(new Font("SansSerif", Font.BOLD, 15));
				g.drawString(song.chordName(song.degree(index)), bx + 6, top - 2 * gap - 4);
				g.setColor(INK);
				if (index > 0 || b > 0) {
					g.drawLine(bx, top, bx, top + 4 * gap);
				}
				if (marks.isEmpty()) {
					// No melody yet: a slash a beat, as a rhythm part is written.
					g.setStroke(new BasicStroke(3f));
					for (int beatAt = 0; beatAt < 4; beatAt++) {
						int sx = bx + 10 + beatAt * (barWidth - 14) / 4;
						g.setColor(index * Progression.EIGHTHS + beatAt * 2 == now ? NOW_INK : INK);
						g.drawLine(sx, top + 3 * gap, sx + gap, top + gap);
					}
					g.setStroke(new BasicStroke(1f));
					g.setColor(INK);
				}
			}
			// The end: a repeat sign, since the song loops; a line that does not end the song ends in a bar line.
			if (line == lines - 1) {
				g.fillOval(end - 12, top + gap + gap / 2 - 2, 4, 4);
				g.fillOval(end - 12, top + 2 * gap + gap / 2 - 2, 4, 4);
				g.drawLine(end - 6, top, end - 6, top + 4 * gap);
				g.fillRect(end - 3, top, 4, 4 * gap + 1);
			} else {
				g.drawLine(end, top, end, top + 4 * gap);
			}
			for (int m = 0; m < marks.size(); m++) {
				Mark mark = marks.get(m);
				int index = mark.slot / Progression.EIGHTHS;
				if (index / perLine != line) {
					continue;
				}
				int x = slotX(mark.slot, firstBar, perLine, barWidth);
				boolean sounding = now >= mark.slot && now < mark.slot + mark.length;
				g.setColor(sounding ? NOW_INK : INK);
				if (mark.isRest()) {
					if (mark.length == 8) {
						x = firstBar + (index % perLine) * barWidth + barWidth / 2 - gap / 2;
					}
					paintRest(g, x, top, gap, mark.length);
				} else {
					Mark next = m + 1 < marks.size() ? marks.get(m + 1) : null;
					int nextX = next == null ? x + 20 : slotX(next.slot, firstBar, perLine, barWidth);
					if (next != null && next.slot / Progression.EIGHTHS / perLine != line) {
						nextX = end + 6; // tied over to the next line: the tie runs off this line's end
					}
					paintNote(g, mark, signsOf.get(m), x, nextX, top, gap, song.position(), upOf[m],
							stemTo[m] == NO_BEAM ? NO_BEAM : top + stemTo[m]);
				}
			}
			g.setColor(INK);
			for (int[] beam : beamLines) {
				if (marks.get(beam[0]).slot / Progression.EIGHTHS / perLine == line) {
					// A beam half a staff space thick, on the stems' ends, its thickness toward the heads.
					int sx = beam[5] == 1 ? 9 : 0, thick = (beam[5] == 1 ? 1 : -1) * gap / 2;
					g.fillPolygon(new int[] { beam[1] + sx, beam[3] + sx + 1, beam[3] + sx + 1, beam[1] + sx },
							new int[] { top + beam[2], top + beam[4], top + beam[4] + thick, top + beam[2] + thick }, 4);
				}
			}
		}
		g.dispose();
	}

	private static int stepY(int top, int gap, int step) {
		return top + (38 - step) * gap / 2;
	}

	/** A mark's x: its eighth's column in its bar on its line. */
	private static int slotX(int slot, int firstBar, int perLine, int barWidth) {
		return firstBar + (slot / Progression.EIGHTHS % perLine) * barWidth + 12 + slot % Progression.EIGHTHS * (barWidth - 20) / Progression.EIGHTHS;
	}

	/** A note or chord's lowest and highest staff steps. */
	private static int[] range(Mark mark, int position) {
		int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
		for (int pitch : mark.pitches) {
			int step = spell(pitch, position)[0];
			low = Math.min(low, step);
			high = Math.max(high, step);
		}
		return new int[] { low, high };
	}

	/** Where a stem of its own length (three and a half staff spaces past the far head) ends, from the staff's top line. */
	private static int stemEnd(int[] range, boolean up, int gap) {
		return up ? stepY(0, gap, range[1]) - 7 * gap / 2 : stepY(0, gap, range[0]) + 7 * gap / 2;
	}

	/**
	 * A note or chord: ledger lines, signs, heads, its stem (up or down; to {@code stemTo} on a beam, or its own
	 * length and a flag on an eighth when that is {@link #NO_BEAM}), its dot and its tie to {@code nextX}.
	 */
	private static void paintNote(Graphics2D g, Mark mark, int[] signs, int x, int nextX, int top, int gap, int position, boolean up, int stemTo) {
		int[] range = range(mark, position);
		Color ink = g.getColor();
		for (int i = 0; i < mark.pitches.length; i++) {
			int step = spell(mark.pitches[i], position)[0];
			int y = stepY(top, gap, step);
			g.setColor(INK);
			for (int s = 28; s >= step; s -= 2) {
				g.drawLine(x - 3, stepY(top, gap, s), x + 12, stepY(top, gap, s));
			}
			for (int s = 40; s <= step; s += 2) {
				g.drawLine(x - 3, stepY(top, gap, s), x + 12, stepY(top, gap, s));
			}
			g.setColor(ink);
			if (signs[i] != NONE) {
				paintSign(g, x - 9, y, signs[i]);
			}
			if (mark.length >= 4) {
				g.setStroke(new BasicStroke(2f));
				g.drawOval(x, y - 3, 9, 6);
				g.setStroke(new BasicStroke(1f));
			} else {
				g.fillOval(x, y - 3, 10, 7);
			}
			if (mark.length == 3 || mark.length == 6) {
				g.fillOval(x + 13, y - (step % 2 == 0 ? gap / 2 : 0) - 1, 3, 3);
			}
			if (mark.tied) {
				g.drawArc(x + 5, y + 3, Math.max(8, nextX - x), 6, 180, 180);
			}
		}
		if (mark.length == 8) {
			return;
		}
		g.setStroke(new BasicStroke(1.4f));
		int stemX = up ? x + 9 : x;
		int from = stepY(top, gap, up ? range[0] : range[1]), to = stemTo == NO_BEAM ? top + stemEnd(range, up, gap) : stemTo;
		g.drawLine(stemX, from, stemX, to);
		if (mark.length == 1 && stemTo == NO_BEAM) {
			g.drawLine(stemX, to, stemX + 6, to + (up ? gap + 3 : -gap - 3));
			g.drawLine(stemX + 6, to + (up ? gap + 3 : -gap - 3), stemX + 5, to + (up ? 2 * gap : -2 * gap));
		}
		g.setStroke(new BasicStroke(1f));
	}

	/** A sharp, flat or natural sign drawn with lines, centered on {@code y}, its right edge near {@code x}. */
	private static void paintSign(Graphics2D g, int x, int y, int sign) {
		if (sign == 1) {
			g.drawLine(x - 5, y - 6, x - 5, y + 7);
			g.drawLine(x - 2, y - 7, x - 2, y + 6);
			g.setStroke(new BasicStroke(2f));
			g.drawLine(x - 8, y - 1, x + 1, y - 3);
			g.drawLine(x - 8, y + 3, x + 1, y + 1);
			g.setStroke(new BasicStroke(1f));
		} else if (sign == -1) {
			g.drawLine(x - 6, y - 10, x - 6, y + 4);
			g.drawArc(x - 6, y - 3, 6, 7, 270, 180);
			g.drawLine(x - 6, y - 1, x - 3, y - 3);
		} else {
			g.drawLine(x - 6, y - 7, x - 6, y + 3);
			g.drawLine(x - 2, y - 3, x - 2, y + 7);
			g.setStroke(new BasicStroke(2f));
			g.drawLine(x - 6, y - 2, x - 2, y - 3);
			g.drawLine(x - 6, y + 3, x - 2, y + 2);
			g.setStroke(new BasicStroke(1f));
		}
	}

	/** A rest of {@code length} eighths at {@code x}, with a dot for dotted ones. */
	private static void paintRest(Graphics2D g, int x, int top, int gap, int length) {
		int middle = top + 2 * gap;
		if (length >= 4) {
			// Whole rest hangs from the fourth line; half rests sit on the middle line.
			g.fillRect(x, length == 8 ? top + gap : middle - gap / 2, gap + 2, gap / 2);
		} else if (length >= 2) {
			g.setStroke(new BasicStroke(2f));
			g.drawLine(x + 2, middle - 10, x + 6, middle - 5);
			g.drawLine(x + 6, middle - 5, x + 2, middle);
			g.drawLine(x + 2, middle, x + 6, middle + 5);
			g.drawLine(x + 6, middle + 5, x + 3, middle + 9);
			g.setStroke(new BasicStroke(1f));
		} else {
			g.fillOval(x + 1, middle - 6, 4, 4);
			g.setStroke(new BasicStroke(1.4f));
			g.drawLine(x + 3, middle - 3, x + 8, middle - 5);
			g.drawLine(x + 8, middle - 5, x + 4, middle + 7);
			g.setStroke(new BasicStroke(1f));
		}
		if (length == 3 || length == 6) {
			g.fillOval(x + gap + 6, middle - gap / 2 - 1, 3, 3);
		}
	}
}
