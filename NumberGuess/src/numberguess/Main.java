package numberguess;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * The window: the {@link Board} above, and below it the original text field and SUBMIT button for typing a guess,
 * or (in You pick) Lower, That's it and Higher for answering the computer's guess (and a switch to allow one lie), or (in Magic) Yes and No for
 * each card, then How it works and Again.
 */
@SuppressWarnings("serial")
public class Main extends JFrame implements ActionListener {

	private final JPanel panel = new JPanel();
	private final JButton button = new JButton("SUBMIT");
	private final JTextField textField = new JTextField();
	private final JButton lower = new JButton("Lower");
	private final JButton correct = new JButton("That's it!");
	private final JButton higher = new JButton("Higher");
	private final JButton fib = new JButton("Let me lie once");
	private final JButton yes = new JButton("Yes, it's there");
	private final JButton no = new JButton("No");
	private final JButton how = new JButton("How it works");
	private final JButton again = new JButton("Again");
	private final JButton send = new JButton("Challenge a friend");
	private final Board board;
	/** The page the game runs on (the browser build passes it), for the links it sends; null on the desktop. */
	private final String page;

	/** {@code challenge}: a friend's won game to play first, or null. */
	public Main(String page, Challenge challenge) {
		this.page = page;
		this.setTitle("Number Guess");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Read after the window exists, so the browser build has put the saved file back.
		Records records = Records.load(new File(System.getProperty("user.home"), ".numberguess-record.txt"));
		board = new Board(records, this::showButtons);
		if (challenge != null) {
			board.startChallenge(challenge);
		}
		panel.add(textField);
		panel.add(button);
		panel.add(lower);
		panel.add(correct);
		panel.add(higher);
		panel.add(fib);
		panel.add(yes);
		panel.add(no);
		panel.add(how);
		panel.add(again);
		panel.add(send);
		this.add(board, BorderLayout.CENTER);
		this.add(panel, BorderLayout.SOUTH);
		textField.setColumns(10);
		button.addActionListener(this);
		// Enter in the text field submits too.
		textField.addActionListener(this);
		lower.addActionListener(event -> answer(board::lower));
		correct.addActionListener(event -> answer(board::correct));
		higher.addActionListener(event -> answer(board::higher));
		fib.addActionListener(event -> answer(this::toggleLies));
		yes.addActionListener(event -> answer(() -> board.magicAnswer(true)));
		no.addActionListener(event -> answer(() -> board.magicAnswer(false)));
		how.addActionListener(event -> answer(board::explain));
		again.addActionListener(event -> answer(board::again));
		send.addActionListener(event -> sendChallenge());
		KeyAdapter keys = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				Main.this.keyPressed(event);
			}
		};
		textField.addKeyListener(keys);
		board.addKeyListener(keys);
		showButtons();
		this.pack();
		this.setVisible(true);
		textField.requestFocus();
	}

	/** Shows the text field in guessing, the three answers in You pick, and in Magic yes and no or how and again. */
	private void showButtons() {
		boolean picking = board.isPicking();
		boolean magic = board.isMagic();
		boolean typing = !picking && !magic;
		textField.setVisible(typing);
		button.setVisible(typing);
		lower.setVisible(picking);
		correct.setVisible(picking);
		higher.setVisible(picking);
		fib.setVisible(picking);
		fib.setText(board.isLying() ? "No lies" : "Let me lie once");
		boolean read = magic && board.isOver();
		yes.setVisible(magic && !read);
		no.setVisible(magic && !read);
		how.setVisible(read && board.canExplain());
		how.setText(board.isExplaining() ? "Hide how" : "How it works");
		again.setVisible(read);
		send.setVisible(typing && board.canChallenge());
		panel.revalidate();
		panel.repaint();
		if (!typing) {
			board.requestFocusInWindow();
		} else {
			textField.requestFocusInWindow();
		}
	}

	/** Puts a link to this won game (or its code, on the desktop) on the clipboard, for a friend to beat. */
	private void sendChallenge() {
		String what = page == null ? "Code" : "Link";
		String message = Challenge.message(page, board.game());
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(message), null);
			int guesses = board.game().getGuesses();
			board.notice(what + " copied: your friend gets the same number and " + guesses + (guesses == 1 ? " guess" : " guesses") + " to beat.");
		} catch (RuntimeException e) {
			board.notice("Could not copy the " + what.toLowerCase() + ". Send this: " + message); // headless, or the clipboard is busy
		}
		textField.requestFocusInWindow();
	}

	private void toggleLies() {
		board.toggleLies();
		showButtons();
	}

	private void answer(Runnable action) {
		action.run();
		board.requestFocusInWindow();
	}

	private void keyPressed(KeyEvent event) {
		int code = event.getKeyCode();
		if (code == KeyEvent.VK_PAGE_UP || code == KeyEvent.VK_PAGE_DOWN) {
			board.moveTab(code == KeyEvent.VK_PAGE_UP ? -1 : 1);
		} else if (event.getSource() != board || !board.isPicking() && !board.isMagic()) {
			return;
		} else if (board.isMagic()) {
			if (!magicKey(code)) {
				return;
			}
		} else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_H) {
			board.higher();
		} else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_L) {
			board.lower();
		} else if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_C || code == KeyEvent.VK_Y) {
			board.correct();
		} else if (code == KeyEvent.VK_F) {
			toggleLies();
		} else {
			return;
		}
		event.consume();
	}

	/** Magic: Y or Up for yes, N or Down for no; after the reveal S shows how and Enter or Space goes again. */
	private boolean magicKey(int code) {
		if (!board.isOver() && (code == KeyEvent.VK_Y || code == KeyEvent.VK_UP)) {
			answer(() -> board.magicAnswer(true));
		} else if (!board.isOver() && (code == KeyEvent.VK_N || code == KeyEvent.VK_DOWN)) {
			answer(() -> board.magicAnswer(false));
		} else if (board.isOver() && code == KeyEvent.VK_S) {
			answer(board::explain);
		} else if (board.isOver() && (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE)) {
			answer(board::again);
		} else {
			return false;
		}
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (board.isPicking() || board.isMagic()) {
			return;
		}
		String text = textField.getText().trim().replace(",", "");
		textField.setText("");
		textField.requestFocus();
		if (board.isOver()) {
			board.again();
			return;
		}
		try {
			board.guess(Integer.parseInt(text));
		} catch (NumberFormatException e) {
			board.notice(text.isEmpty() ? (board.isDuel() ? "Type a number, or click the line." : "Type a guess, or click the lower line.") : "Type a whole number from 1 to "
					+ Level.commas(board.max()) + ".");
		}
	}

	/**
	 * Arguments, both optional: a challenge code or link a friend sent, and the address of the page the game runs on
	 * (the browser build passes its own address, which carries ?challenge= when opened from a friend's link).
	 */
	public static void main(String args[]) {
		String page = null;
		Challenge challenge = null;
		for (String arg : args) {
			String argPage = Challenge.pageOf(arg);
			page = argPage != null ? argPage : page;
			if (argPage == null || arg.contains("challenge=")) {
				Challenge decoded = Challenge.decode(Challenge.codeIn(arg));
				challenge = decoded != null ? decoded : challenge;
			}
		}
		String sendPage = page;
		Challenge first = challenge;
		SwingUtilities.invokeLater(() -> new Main(sendPage, first));
	}
}
