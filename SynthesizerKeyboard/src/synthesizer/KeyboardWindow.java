package synthesizer;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The keyboard window: a lesson bar over the keys. Keys sound for as long as the computer key or the mouse button is
 * held. Picking a song starts a lesson that lights the next key to press, and Listen plays the song first. Space
 * records a tune of your own, which goes to the clipboard as a link (see Tune) for a friend to hear and learn.
 * With the microphone on (I, or its button), notes sung, whistled or played on a real instrument count like keys.
 * The looper (O) records two bars at a time into up to four layers that play round and round under the keys.
 */
public final class KeyboardWindow extends JFrame implements KeyListener {
   private static final int MOUSE = -1;
   private static final int BEAT_MILLIS = 420;
   private static final int[] FANFARE = {0, 4, 7, 12};
   /** The microphone ignores what it hears until the keyboard's own notes have been silent this long. */
   private static final int ECHO_MILLIS = 500;
   private final MidiPlayer midiPlayer = new MidiPlayer();
   private final SongRecords records = SongRecords.load(new File(System.getProperty("user.home"), ".synthesizer-songs.txt"));
   private final PianoPanel piano = new PianoPanel();
   private final LessonBar bar = new LessonBar(this.records);
   private final File tuneFile = new File(System.getProperty("user.home"), ".synthesizer-tune.txt");
   /** The address of the page this runs on, for tune links; null on the desktop, which sends the code alone. */
   private final String page;
   /** The last tune recorded or received, the sixth song in the menu; null if there is none. */
   private Song tune = Tune.load(this.tuneFile);
   /** The tune being recorded, or null when not recording. */
   private Tune.Recorder recorder;
   private boolean spaceDown;
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
   private final Microphone microphone = new Microphone(new Microphone.Ears() {
      @Override
      public void listening() {
         KeyboardWindow.this.micState(LessonBar.MIC_ON);
         KeyboardWindow.this.bar.setNotice("Listening: sing, whistle or play " + (KeyboardWindow.this.lesson == null ? "a note" : "the gold note"));
      }

      @Override
      public void heard(int midi) {
         KeyboardWindow.this.heard(midi);
      }

      @Override
      public void hearing(int midi, int cents) {
         KeyboardWindow.this.hearing(midi, cents);
      }

      @Override
      public void failed(String why) {
         KeyboardWindow.this.micState(LessonBar.MIC_FAILED);
         KeyboardWindow.this.bar.setNotice(why);
      }
   });
   private int mic = LessonBar.MIC_OFF;
   private final LoopView loopView = new LoopView();
   private final Looper looper = new Looper(this.midiPlayer, new File(System.getProperty("user.home"), ".synthesizer-loop.txt"),
      () -> this.instrument, new Looper.Listener() {
         @Override
         public void changed() {
            KeyboardWindow.this.loopChanged();
         }

         @Override
         public void taken(int layer) {
            KeyboardWindow.this.bar.setNotice(layer < 0 ? "Nothing played in that take: Space to try again"
               : "Layer " + (layer + 1) + " is looping" + (KeyboardWindow.this.looper.loop().full() ? ": all four are full" : ": Space adds another"));
            KeyboardWindow.this.loopChanged();
         }
      });
   /** Whether the looper is on (the strip holds its buttons and its lanes sit over the keys). */
   private boolean looping;
   /** Whether Clear was pressed once and waits for a second press to delete every layer. */
   private boolean clearArmed;

   /** {@code friendCode} is the code of a tune a friend sent (null for none); {@code page} as in the field. */
   public KeyboardWindow(String page, String friendCode) {
      this.page = page;
      int screenWidth = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
      int screenHeight = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
      this.setSize(screenWidth * 3 / 4, Math.max(screenHeight / 2, 340));
      this.setDefaultCloseOperation(3);
      this.addKeyListener(this);
      this.getContentPane().setLayout(new BorderLayout());
      JPanel top = new JPanel(new BorderLayout());
      top.setFocusable(false);
      top.add(this.bar, BorderLayout.NORTH);
      top.add(this.loopView, BorderLayout.CENTER);
      this.getContentPane().add(top, BorderLayout.NORTH);
      this.getContentPane().add(this.piano, BorderLayout.CENTER);
      this.loopView.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            KeyboardWindow.this.loopLaneClicked(KeyboardWindow.this.loopView.laneAt(event.getX(), event.getY()));
         }
      });
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
            KeyboardWindow window = KeyboardWindow.this;
            int chip = window.bar.chipAt(event.getX(), event.getY());
            if (window.bar.micAt(event.getX(), event.getY())) {
               window.toggleMic();
            } else if (window.looping) {
               window.loopButton(window.bar.loopChipAt(event.getX(), event.getY()));
            } else if (window.recorder != null) {
               window.stopRecording();
            } else if (chip == window.menu().length) {
               window.startRecording();
            } else if (chip == window.menu().length + 1) {
               window.startLooper();
            } else if (chip >= 0) {
               window.startSong(chip);
            }
         }
      });
      this.bar.setMenu(this.menu());
      this.updateTitle();
      this.refresh();
      this.setVisible(true);
      if (friendCode != null) {
         this.openFriendsTune(friendCode);
      }
   }

   /** The songs to pick from: the five, then the tune if there is one. */
   private Song[] menu() {
      if (this.tune == null) {
         return Song.ALL;
      }
      Song[] menu = Arrays.copyOf(Song.ALL, Song.ALL.length + 1);
      menu[Song.ALL.length] = this.tune;
      return menu;
   }

   /** Starts a lesson on the tune a friend sent, and plays it to the visitor first. */
   private void openFriendsTune(String code) {
      Song friends = Tune.decode(code, Tune.FRIENDS);
      if (friends == null) {
         this.bar.setNotice("That tune link is not complete");
         return;
      }
      this.keepTune(friends);
      this.startSong(this.menu().length - 1);
      later(700, this::listen);
   }

   private void keepTune(Song song) {
      this.tune = song;
      Tune.save(this.tuneFile, song);
      this.bar.setMenu(this.menu());
   }

   private void startRecording() {
      this.startSong(-1);
      this.recorder = new Tune.Recorder();
      this.bar.setRecording(true, null);
   }

   /** Ends the recording; a tune of two notes or more becomes the sixth song and its link goes to the clipboard. */
   private void stopRecording() {
      Song recorded = this.recorder.song(Tune.MINE);
      this.recorder = null;
      this.bar.setRecording(false, null);
      if (recorded == null || recorded.length() < 2) {
         this.bar.setNotice("Too short: record at least two notes");
         return;
      }
      this.keepTune(recorded);
      this.startSong(this.menu().length - 1);
      this.copyTune();
   }

   /** Puts the tune's link (or code, on the desktop) on the clipboard and says so. */
   private void copyTune() {
      if (this.tune == null) {
         return;
      }
      String what = this.page == null ? "Code" : "Link";
      try {
         Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(Tune.message(this.page, this.tune)), null);
         this.bar.setNotice(what + " copied: send it to a friend");
      } catch (RuntimeException e) {
         this.bar.setNotice("Could not copy the " + what.toLowerCase() + ": " + Tune.encode(this.tune));   // headless, or the clipboard is busy
      }
   }

   /** Turns the looper on: the songs make way for its buttons, and its lanes open over the keys. */
   private void startLooper() {
      this.startSong(-1);
      this.clearArmed = false;
      this.bar.setLooper(this.looper);
      this.loopView.setInstrument(this.instrument);
      if (!this.looping) {
         // The lanes get room of their own, so the keys keep their size.
         this.setSize(this.getWidth(), this.getHeight() + LoopView.HEIGHT);
      }
      this.looping = true;
      this.loopView.setLooper(this.looper);
      this.bar.setNotice(this.looper.loop().layers().isEmpty() ? null : "Your loop is back: P plays it");
   }

   /** Stops the loop and goes back to the songs; the loop is kept for next time. */
   private void stopLooper() {
      this.looper.stop();
      if (this.looping) {
         this.setSize(this.getWidth(), this.getHeight() - LoopView.HEIGHT);
      }
      this.looping = false;
      this.bar.setLooper(null);
      this.loopView.setLooper(null);
      this.bar.setNotice(null);
   }

   private void loopChanged() {
      this.bar.repaint();
      this.loopView.repaint();
   }

   /** A looper button in the strip (LessonBar.LOOP_PLAY and so on), or -1 for none. */
   private void loopButton(int button) {
      if (button < 0) {
         return;
      }
      boolean clear = button == LessonBar.LOOP_CLEAR;
      this.bar.setNotice(null);
      if (button == LessonBar.LOOP_PLAY) {
         this.looper.togglePlay();
      } else if (button == LessonBar.LOOP_RECORD) {
         this.recordLayer();
      } else if (button == LessonBar.LOOP_SLOWER || button == LessonBar.LOOP_FASTER) {
         this.looper.changeTempo(button == LessonBar.LOOP_SLOWER ? -Loop.TEMPO_STEP : Loop.TEMPO_STEP);
      } else if (button == LessonBar.LOOP_CLICK) {
         this.looper.toggleClick();
      } else if (button == LessonBar.LOOP_UNDO) {
         if (!this.looper.undo()) {
            this.bar.setNotice("No layers to undo");
         }
      } else if (clear && this.clearArmed) {
         this.looper.clear();
         this.bar.setNotice("Every layer is gone: Space to start again");
      } else if (clear) {
         this.bar.setNotice(this.looper.loop().layers().isEmpty() ? "No layers to clear" : "Clear again to delete every layer");
      } else if (button == LessonBar.LOOP_SONGS) {
         this.stopLooper();
      }
      this.clearArmed = clear && !this.clearArmed && !this.looper.loop().layers().isEmpty();
   }

   private void recordLayer() {
      if (this.looper.loop().full() && !this.looper.taking()) {
         this.bar.setNotice("All four layers are full: Undo or mute one");
         return;
      }
      this.looper.record();
   }

   /** A click on a lane: a layer's lane mutes or unmutes it, the empty lane records the next layer. */
   private void loopLaneClicked(int lane) {
      if (lane < 0) {
         return;
      }
      if (lane < this.looper.loop().layers().size()) {
         this.looper.toggleMute(lane);
      } else {
         this.loopButton(LessonBar.LOOP_RECORD);
      }
   }

   /** The looper's own keys; false for any other key, which then works as usual. */
   private boolean loopKey(int keyCode) {
      if (keyCode == KeyEvent.VK_SPACE) {
         if (!this.spaceDown) {
            this.spaceDown = true;
            this.loopButton(LessonBar.LOOP_RECORD);
         }
      } else if (keyCode == KeyEvent.VK_P) {
         this.loopButton(LessonBar.LOOP_PLAY);
      } else if (keyCode == KeyEvent.VK_MINUS) {
         this.loopButton(LessonBar.LOOP_SLOWER);
      } else if (keyCode == KeyEvent.VK_EQUALS || keyCode == KeyEvent.VK_PLUS) {
         this.loopButton(LessonBar.LOOP_FASTER);
      } else if (keyCode == KeyEvent.VK_K) {
         this.loopButton(LessonBar.LOOP_CLICK);
      } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
         this.loopButton(LessonBar.LOOP_UNDO);
      } else if (keyCode == KeyEvent.VK_DELETE) {
         this.loopButton(LessonBar.LOOP_CLEAR);
      } else if (keyCode == KeyEvent.VK_O || keyCode == KeyEvent.VK_ESCAPE) {
         this.loopButton(LessonBar.LOOP_SONGS);
      } else if (keyCode >= KeyEvent.VK_1 && keyCode < KeyEvent.VK_1 + Loop.MAX_LAYERS) {
         this.looper.toggleMute(keyCode - KeyEvent.VK_1);
      } else {
         return false;
      }
      return true;
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
      if (this.looping) {
         this.looper.press(note);
      }
      this.played(key);
   }

   /** Lets the microphone go when the window closes without ending the program (NathaNOS disposes its apps' windows). */
   @Override
   public void dispose() {
      this.microphone.stop();
      this.looper.stop();
      super.dispose();
   }

   /** Turns the microphone on or off. */
   private void toggleMic() {
      if (this.mic == LessonBar.MIC_OFF || this.mic == LessonBar.MIC_FAILED) {
         this.micState(LessonBar.MIC_ASKING);
         this.bar.setNotice(null);
         this.microphone.start();
      } else {
         this.microphone.stop();
         this.micState(LessonBar.MIC_OFF);
         this.bar.setNotice(null);
      }
   }

   private void micState(int state) {
      this.mic = state;
      this.bar.setMic(state, -1, 0);
      this.piano.setHeard(-1);
   }

   /** Whether the microphone may be hearing the keyboard itself (a key held, Listen, the fanfare) rather than you. */
   private boolean echoing() {
      return !this.midiPlayer.quietFor(ECHO_MILLIS);
   }

   /** The tuner: the note the microphone hears now (-1 for none), shown on the button and tinted on the keys. */
   private void hearing(int midi, int cents) {
      boolean heard = midi >= 0 && !this.echoing();
      this.bar.setMic(this.mic, heard ? midi : -1, cents);
      this.piano.setHeard(heard ? Math.floorMod(midi, 12) : -1);
   }

   /** A note the microphone heard start: it counts like a key (the key of the same name, in any octave), unsounded. */
   private void heard(int midi) {
      if (this.echoing()) {
         return;
      }
      int key = Math.floorMod(midi, 12);
      if (key == 0 && this.lesson != null && this.lesson.expectedKey() == 12) {
         key = 12;
      }
      this.played(key);
   }

   /** Counts key {@code key}, pressed or heard, in the recording and the lesson, and takes over from Listen. */
   private void played(int key) {
      this.bar.setNotice(null);
      if (this.recorder != null) {
         this.recorder.press(key, System.currentTimeMillis());
         this.bar.setRecording(true, this.recorder.song(Tune.MINE));
         if (this.recorder.full()) {
            this.stopRecording();
         }
      }
      if (this.listenNote >= 0) {
         this.stopListening();
      }
      if (this.lesson != null && !this.lesson.finished()) {
         this.lesson.press(key);
         if (this.lesson.finished()) {
            this.newBest = this.lesson.song().id() != null && this.records.add(this.lesson);
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
         if (this.looping) {
            this.looper.release(held[1]);
         }
      }
   }

   /** Starts song {@code index} of the menu from its first note, or free play for -1. */
   private void startSong(int index) {
      this.stopListening();
      this.bar.setNotice(null);
      this.lesson = index < 0 ? null : new Lesson(this.menu()[index]);
      this.newBest = false;
      this.refresh();
   }

   private int songIndex() {
      Song[] menu = this.menu();
      for (int index = 0; this.lesson != null && index < menu.length; index++) {
         if (menu[index] == this.lesson.song()) {
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
      int songs = this.menu().length;
      boolean recording = this.recorder != null;
      if (this.looping && this.loopKey(keyCode)) {
         return;
      }
      if (keyCode == KeyEvent.VK_SPACE) {
         // Holding Space auto-repeats it; only the first press counts.
         if (this.spaceDown) {
            return;
         }
         this.spaceDown = true;
         if (recording) {
            this.stopRecording();
         } else {
            this.startRecording();
         }
      } else if (keyCode == KeyEvent.VK_ENTER && !recording) {
         this.copyTune();
      } else if (keyCode == KeyEvent.VK_I) {
         this.toggleMic();
      } else if (keyCode == KeyEvent.VK_LEFT && this.octaveShift > -5) {
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
      } else if (recording || this.looping) {
         // Song keys wait until the recording is stopped or the looper is off; the piano keys below still play.
      } else if (keyCode == KeyEvent.VK_O) {
         this.startLooper();
      } else if (keyCode == KeyEvent.VK_T) {
         this.startSong((this.songIndex() + 1) % songs);
      } else if (keyCode >= KeyEvent.VK_1 && keyCode < KeyEvent.VK_1 + songs) {
         this.startSong(keyCode - KeyEvent.VK_1);
      } else if (keyCode == KeyEvent.VK_0) {
         this.startSong(-1);
      } else if (keyCode == KeyEvent.VK_R && this.lesson != null) {
         this.startSong(this.songIndex());
      } else if (keyCode == KeyEvent.VK_L) {
         this.listen();
      }
      this.updateTitle();
      this.loopView.setInstrument(this.instrument);

      int index = this.keyIndex(keyCode);
      if (index >= 0) {
         this.press(keyCode, index);
      }
   }

   @Override
   public final void keyReleased(KeyEvent event) {
      if (event.getKeyCode() == KeyEvent.VK_SPACE) {
         this.spaceDown = false;
      }
      this.release(event.getKeyCode());
   }
}
