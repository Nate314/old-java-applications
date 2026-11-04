package musictheoryexpert;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import synthesizer.MySynthesizer;

@SuppressWarnings("serial")
public class Main extends JFrame {

	public static final long CLOCK_TICK = 500;
	private MySynthesizer synth = new MySynthesizer(this);
	private JTabbedPane tpane = new JTabbedPane();
	private EarTrainer trainer;
	private Songwriter songwriter;
	private ChordFinder finder;

	public Main(Boolean visibility) {
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setTitle("Music Theory Expert");
		trainer = new EarTrainer(synth,
				DrillRecords.load(new File(System.getProperty("user.home"), ".musictheory-drills.txt")));
		songwriter = new Songwriter(synth, new File(System.getProperty("user.home"), ".musictheory-songwriter.txt"));
		// The Chord finder took the place of the Notes tab, whose test button played the original demo; it still does.
		finder = new ChordFinder(synth, () -> new Notes(synth), (position, minor, degrees) -> {
			tpane.setSelectedIndex(1);
			songwriter.takeChords(position, minor, degrees);
		});
		tpane.addTab("Ear training", trainer);
		tpane.addTab("Songwriter", songwriter);
		tpane.addTab("Chord finder", finder);
		this.add(tpane);
		this.setSize(780, 600);
		// Keys reach the showing tab whichever component has the focus.
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> this.isActive() && dispatch(event));
		this.setVisible(visibility);
//		synth.playNotes(synthesizer.Notes.A4, 300, 0, 127, 0, 0);
//		synth.playNotes(synthesizer.Notes.B4, 300, 0, 127, 0, 0);
//		synth.playNotes(synthesizer.Notes.C4, 300, 0, 127, 0, 0);
		// The I-IV-V-I demo used to play on start; it now plays from the Notes tab's test button.
	}
	
	// Set before any component is created, so the window really uses the system look.
	private static void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			setLookAndFeel();
			new Main(true);
		});
	}

	private boolean dispatch(KeyEvent event) {
		Component tab = tpane.getSelectedComponent();
		if (event.getID() == KeyEvent.KEY_RELEASED && tab == finder) {
			finder.keyReleased(event);
		}
		return event.getID() == KeyEvent.KEY_PRESSED && (tab == trainer && trainer.keyPressed(event)
				|| tab == songwriter && songwriter.keyPressed(event) || tab == finder && finder.keyPressed(event));
	}

	/** The tabs, for self-checks. */
	JTabbedPane tabs() {
		return tpane;
	}

	ChordFinder finder() {
		return finder;
	}

	Songwriter songwriter() {
		return songwriter;
	}
}
