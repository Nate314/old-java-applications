package synthesizer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

public final class KeyboardWindow extends JFrame implements ActionListener, KeyListener {
   private static final int CLICK_NOTE_MILLIS = 150;
   private MidiPlayer midiPlayer = new MidiPlayer();
   private JLayeredPane layeredPane = new JLayeredPane();
   private JPanel whiteKeyPanel = new JPanel();
   private JPanel blackKeyPanel = new JPanel();
   private JButton[] whiteKeys = new KeyButton[8];
   private JButton[] blackKeys = new KeyButton[5];
   private int[] whiteKeyNotes = new int[]{60, 62, 64, 65, 67, 69, 71, 72};
   private int[] blackKeyNotes = new int[]{61, 63, 66, 68, 70};
   /** Computer keys in note order from C (60) to C (72), and the on-screen key each one lights. */
   private int[] keyCodes = new int[]{
      KeyEvent.VK_Z, KeyEvent.VK_S, KeyEvent.VK_X, KeyEvent.VK_D, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_G,
      KeyEvent.VK_B, KeyEvent.VK_H, KeyEvent.VK_N, KeyEvent.VK_J, KeyEvent.VK_M, KeyEvent.VK_COMMA
   };
   private JButton[] keyButtons;
   private int screenWidth = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
   private int screenHeight = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
   private int whiteKeyWidth;
   private int laidOutWidth = -1;
   private int laidOutHeight = -1;
   private Color whiteKeyColor = new Color(255, 255, 255);
   private Color blackKeyColor = new Color(0, 0, 0);
   private Color pressedWhiteKeyColor = new Color(175, 175, 175);
   private Color pressedBlackKeyColor = new Color(50, 50, 50);
   private int octaveShift = 0;
   private int instrument = 0;
   /** Notes currently sounding, by the key code that started them, so a release stops the right note after an octave change. */
   private Map<Integer, Integer> heldNotes = new HashMap<>();

   public KeyboardWindow() {
      this.setSize(this.screenWidth * 3 / 4, this.screenHeight / 2);
      this.setDefaultCloseOperation(3);
      this.addKeyListener(this);
      this.add(this.layeredPane);
      this.whiteKeyPanel.setLayout(null);
      this.blackKeyPanel.setLayout(null);
      this.blackKeyPanel.setBackground(new Color(0, 0, 0, 0));

      for (int whiteIndex = 0; whiteIndex < this.whiteKeys.length; whiteIndex++) {
         this.whiteKeys[whiteIndex] = this.createKey(this.whiteKeyColor);
         this.whiteKeyPanel.add(this.whiteKeys[whiteIndex]);
      }

      for (int blackIndex = 0; blackIndex < this.blackKeys.length; blackIndex++) {
         this.blackKeys[blackIndex] = this.createKey(this.blackKeyColor);
         this.blackKeyPanel.add(this.blackKeys[blackIndex]);
      }

      JButton[] white = this.whiteKeys;
      JButton[] black = this.blackKeys;
      this.keyButtons = new JButton[]{white[0], black[0], white[1], black[1], white[2], white[3], black[2], white[4], black[3], white[5], black[4], white[6], white[7]};
      this.layeredPane.add(this.whiteKeyPanel, Integer.valueOf(0), 0);
      this.layeredPane.add(this.blackKeyPanel, Integer.valueOf(1), 0);
      this.updateTitle();
      this.setVisible(true);
   }

   private JButton createKey(Color color) {
      JButton key = new KeyButton();
      key.addActionListener(this);
      key.setBackground(color);
      // Keep keyboard focus on the window so the computer keyboard keeps working after a click.
      key.setFocusable(false);
      return key;
   }

   /** Sizes the keys to the window; called from paint() only when the window size changed. */
   private void layoutKeys() {
      this.whiteKeyWidth = this.getWidth() / 8;
      this.whiteKeyPanel.setBounds(0, 0, this.getWidth(), this.getHeight());
      this.blackKeyPanel.setBounds(0, 0, this.getWidth(), this.getHeight() * 3 / 5);

      for (int whiteIndex = 0; whiteIndex < this.whiteKeys.length; whiteIndex++) {
         this.whiteKeys[whiteIndex].setBounds(whiteIndex * this.whiteKeyWidth, 0, this.whiteKeyWidth, this.whiteKeyPanel.getHeight());
      }

      for (int blackIndex = 0; blackIndex < this.blackKeys.length; blackIndex++) {
         int skipE = blackIndex < 2 ? 0 : this.whiteKeyWidth;
         this.blackKeys[blackIndex].setBounds(skipE + this.whiteKeyWidth / 2 + blackIndex * this.whiteKeyWidth + this.whiteKeyWidth / 8, 0, this.whiteKeyWidth - this.whiteKeyWidth / 4, this.blackKeyPanel.getHeight());
      }
   }

   @Override
   public final void paint(Graphics graphics) {
      if (this.getWidth() != this.laidOutWidth || this.getHeight() != this.laidOutHeight) {
         this.laidOutWidth = this.getWidth();
         this.laidOutHeight = this.getHeight();
         this.layoutKeys();
      }
      super.paint(graphics);
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      for (int whiteIndex = 0; whiteIndex < this.whiteKeys.length; whiteIndex++) {
         if (event.getSource() == this.whiteKeys[whiteIndex]) {
            this.midiPlayer.playNote(this.whiteKeyNotes[whiteIndex] + 12 * this.octaveShift, CLICK_NOTE_MILLIS);
         }
      }

      for (int blackIndex = 0; blackIndex < this.blackKeys.length; blackIndex++) {
         if (event.getSource() == this.blackKeys[blackIndex]) {
            this.midiPlayer.playNote(this.blackKeyNotes[blackIndex] + 12 * this.octaveShift, CLICK_NOTE_MILLIS);
         }
      }
   }

   private int keyIndex(int keyCode) {
      for (int index = 0; index < this.keyCodes.length; index++) {
         if (this.keyCodes[index] == keyCode) {
            return index;
         }
      }
      return -1;
   }

   private boolean isBlack(JButton key) {
      for (JButton black : this.blackKeys) {
         if (black == key) {
            return true;
         }
      }
      return false;
   }

   private void updateTitle() {
      this.setTitle("Synthesizer: instrument " + this.instrument + ", octave " + (this.octaveShift >= 0 ? "+" : "") + this.octaveShift);
   }

   @Override
   public final void keyTyped(KeyEvent event) {
   }

   @Override
   public final void keyPressed(KeyEvent event) {
      int keyCode = event.getKeyCode();
      if (keyCode == KeyEvent.VK_LEFT && this.octaveShift > -5) {
         this.octaveShift--;
      } else if (keyCode == KeyEvent.VK_RIGHT && this.octaveShift < 4) {
         this.octaveShift++;
      } else if (keyCode == KeyEvent.VK_UP) {
         if (this.instrument < 120) {
            this.instrument++;
         }

         this.midiPlayer.setInstrument(this.instrument);
      } else if (keyCode == KeyEvent.VK_DOWN) {
         if (this.instrument > 0) {
            this.instrument--;
         }

         this.midiPlayer.setInstrument(this.instrument);
      }
      this.updateTitle();

      int index = this.keyIndex(keyCode);
      // Holding a key auto-repeats key presses; only the first one starts the note.
      if (index >= 0 && !this.heldNotes.containsKey(keyCode)) {
         int note = 60 + index + 12 * this.octaveShift;
         this.heldNotes.put(keyCode, note);
         this.midiPlayer.noteOn(note);
         JButton key = this.keyButtons[index];
         key.setBackground(this.isBlack(key) ? this.pressedBlackKeyColor : this.pressedWhiteKeyColor);
      }
   }

   @Override
   public final void keyReleased(KeyEvent event) {
      int index = this.keyIndex(event.getKeyCode());
      Integer note = this.heldNotes.remove(event.getKeyCode());
      if (index >= 0 && note != null) {
         this.midiPlayer.noteOff(note);
         JButton key = this.keyButtons[index];
         key.setBackground(this.isBlack(key) ? this.blackKeyColor : this.whiteKeyColor);
      }
   }
}
