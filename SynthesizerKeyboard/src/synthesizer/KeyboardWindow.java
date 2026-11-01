package synthesizer;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.Timer;

/**
 * The keyboard window: a lesson bar over the keys. Keys sound for as long as the computer key or the mouse button is
 * held. Picking a song starts a lesson that lights the next key to press, and Listen plays the song first.
 */
public final class KeyboardWindow extends JFrame implements KeyListener {
   private static final int MOUSE = -1;
   private static final int BEAT_MILLIS = 420;
   private static final int[] FANFARE = {0, 4, 7, 12};
   private final MidiPlayer midiPlayer = new MidiPlayer();
   private final SongRecords records = SongRecords.load(new File(System.getProperty("user.home"), ".synthesizer-songs.txt"));
   private final PianoPanel piano = new PianoPanel();
   private final LessonBar bar = new LessonBar(this.records);
   /** Computer keys in note order from C (60) to C (72). */
   private final int[] keyCodes = new int[]{
      KeyEvent.VK_Z, KeyEvent.VK_S, KeyEvent.VK_X, KeyEvent.VK_D, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_G,
      KeyEvent.VK_B, KeyEvent.VK_H, KeyEvent.VK_N, KeyEvent.VK_J, KeyEvent.VK_M, KeyEvent.VK_COMMA
   };
   private int octaveShift = 0;
   private int instrument = 0;
   /** Keys sounding, by what holds them (a key code, or MOUSE): {key index, MIDI note}, so a release after an octave change stops the right note. */
   private final Map<Integer, int[]> heldNotes = new HashMap<>();
   /** The song being played along with, or null in free play. */
   private Lesson lesson;
   private boolean newBest;
   /** The note Listen is playing, or -1; {@code listenRun} changes when it stops, so its pending timers do nothing. */
   private int listenNote = -1;
   private int listenRun = 0;
   /** The key and MIDI note Listen is sounding, or -1. */
   private int listenKey = -1;
   private int listenMidi;

   public KeyboardWindow() {
      int screenWidth = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
      int screenHeight = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
      this.setSize(screenWidth * 3 / 4, Math.max(screenHeight / 2, 340));
      this.setDefaultCloseOperation(3);
      this.addKeyListener(this);
      this.getContentPane().setLayout(new BorderLayout());
      this.getContentPane().add(this.bar, BorderLayout.NORTH);
      this.getContentPane().add(this.piano, BorderLayout.CENTER);
      this.piano.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            int key = KeyboardWindow.this.piano.keyAt(event.getX(), event.getY());
            if (key >= 0) {
               KeyboardWindow.this.press(MOUSE, key);
            }
         }

         @Override
         public void mouseReleased(MouseEvent event) {
            KeyboardWindow.this.release(MOUSE);
         }
      });
      this.bar.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            int song = KeyboardWindow.this.bar.songAt(event.getX(), event.getY());
            if (song >= 0) {
               KeyboardWindow.this.startSong(song);
            }
         }
      });
      this.updateTitle();
      this.refresh();
      this.setVisible(true);
   }

   private int keyIndex(int keyCode) {
      for (int index = 0; index < this.keyCodes.length; index++) {
         if (this.keyCodes[index] == keyCode) {
            return index;
         }
      }
      return -1;
   }

   private int midiNote(int key) {
      return 60 + key + 12 * this.octaveShift;
   }

   private void updateTitle() {
      this.setTitle("Synthesizer: instrument " + this.instrument + ", octave " + (this.octaveShift >= 0 ? "+" : "") + this.octaveShift);
   }

   /** Shows the lesson state on the bar and lights the next key. */
   private void refresh() {
      this.bar.show(this.lesson, this.listenNote, this.newBest);
      this.piano.setHint(this.lesson == null || this.listenNote >= 0 ? -1 : this.lesson.expectedKey());
   }

   /** Starts sounding key {@code key}, held by {@code holder} (a key code or MOUSE), and counts it in the lesson. */
   private void press(int holder, int key) {
      // Holding a key auto-repeats key presses; only the first one starts the note.
      if (this.heldNotes.containsKey(holder)) {
         return;
      }
      int note = this.midiNote(key);
      this.heldNotes.put(holder, new int[]{key, note});
      this.midiPlayer.noteOn(note);
      this.piano.setPressed(key, true);
      if (this.listenNote >= 0) {
         this.stopListening();
      }
      if (this.lesson != null && !this.lesson.finished()) {
         this.lesson.press(key);
         if (this.lesson.finished()) {
            this.newBest = this.records.add(this.lesson);
            this.fanfare();
         }
      }
      this.refresh();
   }

   private void release(int holder) {
      int[] held = this.heldNotes.remove(holder);
      if (held != null) {
         this.midiPlayer.noteOff(held[1]);
         this.piano.setPressed(held[0], false);
      }
   }

   /** Starts song {@code index} of Song.ALL from its first note, or free play for -1. */
   private void startSong(int index) {
      this.stopListening();
      this.lesson = index < 0 ? null : new Lesson(Song.ALL[index]);
      this.newBest = false;
      this.refresh();
   }

   private int songIndex() {
      for (int index = 0; this.lesson != null && index < Song.ALL.length; index++) {
         if (Song.ALL[index] == this.lesson.song()) {
            return index;
         }
      }
      return -1;
   }

   /** Plays the current song (the first one in free play) to the visitor, lighting each key; any piano key takes over. */
   private void listen() {
      if (this.lesson == null || this.lesson.finished()) {
         this.startSong(Math.max(0, this.songIndex()));
      }
      this.stopListening();
      this.listenStep(this.listenRun, 0);
   }

   private void listenStep(int run, int note) {
      if (run != this.listenRun) {
         return;
      }
      Song song = this.lesson.song();
      if (note >= song.length()) {
         this.stopListening();
         return;
      }
      int millis = (int)(song.beats(note) * BEAT_MILLIS);
      this.listenNote = note;
      this.listenKey = song.key(note);
      this.listenMidi = this.midiNote(this.listenKey);
      this.midiPlayer.noteOn(this.listenMidi);
      this.piano.setPressed(this.listenKey, true);
      this.refresh();
      later(millis * 85 / 100, () -> {
         if (run == this.listenRun) {
            this.endListenNote();
         }
      });
      later(millis, () -> this.listenStep(run, note + 1));
   }

   private boolean isHeld(int key) {
      for (int[] held : this.heldNotes.values()) {
         if (held[0] == key) {
            return true;
         }
      }
      return false;
   }

   /** Stops the note Listen is sounding, unless the visitor has taken over on that same key. */
   private void endListenNote() {
      if (this.listenKey >= 0 && !this.isHeld(this.listenKey)) {
         this.midiPlayer.noteOff(this.listenMidi);
         this.piano.setPressed(this.listenKey, false);
      }
      this.listenKey = -1;
   }

   private void stopListening() {
      this.listenRun++;
      this.endListenNote();
      if (this.listenNote >= 0) {
         this.listenNote = -1;
         this.refresh();
      }
   }

   /** A quick rising arpeggio when a song is finished. */
   private void fanfare() {
      for (int step = 0; step < FANFARE.length; step++) {
         int note = this.midiNote(FANFARE[step]) + 12;
         int millis = step == FANFARE.length - 1 ? 500 : 160;
         later(250 + step * 90, () -> this.midiPlayer.playNote(note, millis));
      }
   }

   private static void later(int millis, Runnable action) {
      Timer timer = new Timer(Math.max(1, millis), event -> action.run());
      timer.setRepeats(false);
      timer.start();
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
      } else if (keyCode == KeyEvent.VK_T) {
         this.startSong((this.songIndex() + 1) % Song.ALL.length);
      } else if (keyCode >= KeyEvent.VK_1 && keyCode < KeyEvent.VK_1 + Song.ALL.length) {
         this.startSong(keyCode - KeyEvent.VK_1);
      } else if (keyCode == KeyEvent.VK_0) {
         this.startSong(-1);
      } else if (keyCode == KeyEvent.VK_R && this.lesson != null) {
         this.startSong(this.songIndex());
      } else if (keyCode == KeyEvent.VK_L) {
         this.listen();
      }
      this.updateTitle();

      int index = this.keyIndex(keyCode);
      if (index >= 0) {
         this.press(keyCode, index);
      }
   }

   @Override
   public final void keyReleased(KeyEvent event) {
      this.release(event.getKeyCode());
   }
}
