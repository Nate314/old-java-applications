package numberguess;

import java.awt.BorderLayout;
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
 * or (in You pick) Lower, That's it and Higher for answering the computer's guess, or (in Magic) Yes and No for
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
	private final JButton yes = new JButton("Yes, it's there");
	private final JButton no = new JButton("No");
	private final JButton how = new JButton("How it works");
	private final JButton again = new JButton("Again");
	private final Board board;

	public Main() {
		this.setTitle("Number Guess");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Read after the window exists, so the browser build has put the saved file back.
		Records records = Records.load(new File(System.getProperty("user.home"), ".numberguess-record.txt"));
		board = new Board(records, this::showButtons);
		panel.add(textField);
		panel.add(button);
		panel.add(lower);
		panel.add(correct);
		panel.add(higher);
		panel.add(yes);
		panel.add(no);
		panel.add(how);
		panel.add(again);
		this.add(board, BorderLayout.CENTER);
		this.add(panel, BorderLayout.SOUTH);
		textField.setColumns(10);
		button.addActionListener(this);
		// Enter in the text field submits too.
		textField.addActionListener(this);
		lower.addActionListener(event -> answer(board::lower));
		correct.addActionListener(event -> answer(board::correct));
		higher.addActionListener(event -> answer(board::higher));
		yes.addActionListener(event -> answer(() -> board.magicAnswer(true)));
		no.addActionListener(event -> answer(() -> board.magicAnswer(false)));
		how.addActionListener(event -> answer(board::explain));
		again.addActionListener(event -> answer(board::again));
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
		boolean read = magic && board.isOver();
		yes.setVisible(magic && !read);
		no.setVisible(magic && !read);
		how.setVisible(read && board.canExplain());
		how.setText(board.isExplaining() ? "Hide how" : "How it works");
		again.setVisible(read);
		panel.revalidate();
		panel.repaint();
		if (!typing) {
			board.requestFocusInWindow();
		} else {
			textField.requestFocusInWindow();
		}
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
			board.notice(text.isEmpty() ? "Type a guess, or click the lower line." : "Type a whole number from 1 to "
					+ Level.commas(board.game().level().max()) + ".");
		}
	}

	public static void main(String args[]) {
		SwingUtilities.invokeLater(Main::new);
	}
}
