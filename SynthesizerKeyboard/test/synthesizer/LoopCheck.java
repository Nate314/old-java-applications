package synthesizer;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Assert-based self-check of the looper's loop: takes, quantizing, layers, tempo and the saved file. Run with {@code java -ea}. */
public final class LoopCheck {
   public static void main(String[] args) throws IOException {
      boolean enabled = false;
      assert enabled = true;
      if (!enabled) {
         throw new IllegalStateException("Run with -ea");
      }
      checkTake();
      checkLayers();
      checkTempo();
      checkSaving();
      System.out.println("LoopCheck: all checks passed");
   }

   private static void checkTake() {
      Loop.Take take = new Loop.Take(0);
      take.press(60, -0.4);          // a touch early: the downbeat
      take.release(60, 3.8);
      take.press(64, 4.3);
      take.release(64, 4.5);         // a short tap still lasts a step
      take.press(67, 31.7);          // just after the pass: wraps round to the downbeat
      take.release(67, 33.0);
      take.press(72, -2);            // too early: not in the take
      take.release(72, 1);
      List<Loop.Note> notes = take.finish(0).notes();
      assert notes.size() == 3 : notes.size();
      Loop.Note first = notes.get(0);
      assert first.step == 0 && first.length == 4 && (first.midi == 60 || first.midi == 67);
      Loop.Note second = find(notes, 64);
      assert second.step == 4 && second.length == 1 : second.step + " " + second.length;
      Loop.Note wrapped = find(notes, 67);
      assert wrapped.step == 0 && wrapped.length == 1 : wrapped.step + " " + wrapped.length;

      Loop.Take held = new Loop.Take(0);
      held.press(62, 28);
      assert held.notesAt(30).get(0).length == 2 : "a held note grows as it is held";
      Loop.Note end = held.finish(5).notes().get(0);
      assert end.step == 28 && end.length == 4 : "a note still held ends with the pass";

      Loop.Take secondBar = new Loop.Take(Loop.STEPS_PER_BAR);
      secondBar.press(60, 0);
      secondBar.release(60, 2);
      secondBar.press(62, 20);
      secondBar.release(62, 24);
      Loop.Layer layer = secondBar.finish(33);
      assert layer.program == 33;
      assert layer.notes().get(0).step == 4 && layer.notes().get(0).midi == 62 : "a take from bar 2 wraps into bar 1";
      assert layer.notes().get(1).step == 16 && layer.notes().get(1).length == 2;

      assert new Loop.Take(0).finish(0) == null : "an empty take makes no layer";
      Loop.Take many = new Loop.Take(0);
      for (int i = 0; i < 100; i++) {
         many.press(40 + i % 50, i * 0.3);
         many.release(40 + i % 50, i * 0.3 + 0.1);
      }
      assert many.finish(0).notes().size() == Loop.MAX_NOTES : "a take keeps at most 64 notes";
   }

   private static Loop.Note find(List<Loop.Note> notes, int midi) {
      for (Loop.Note note : notes) {
         if (note.midi == midi) {
            return note;
         }
      }
      throw new AssertionError("no note " + midi);
   }

   private static Loop.Layer layer(int program, int midi) {
      Loop.Take take = new Loop.Take(0);
      take.press(midi, 0);
      take.release(midi, 2);
      return take.finish(program);
   }

   private static void checkLayers() {
      Loop loop = new Loop();
      for (int i = 0; i < Loop.MAX_LAYERS; i++) {
         assert loop.add(layer(i * 8, 60 + i));
      }
      assert loop.full() && !loop.add(layer(0, 60)) : "four layers at most";
      loop.toggleMute(1);
      assert loop.layers().get(1).muted() && !loop.layers().get(0).muted();
      loop.toggleMute(9);
      assert loop.removeLast() && loop.layers().size() == 3 && !loop.full();
      loop.clear();
      assert loop.layers().isEmpty() && !loop.removeLast();
   }

   private static void checkTempo() {
      Loop loop = new Loop();
      assert loop.tempo() == Loop.DEFAULT_TEMPO;
      assert Math.abs(loop.stepMillis() - 60000.0 / 96 / 4) < 1e-9;
      loop.changeTempo(1000);
      assert loop.tempo() == Loop.MAX_TEMPO;
      loop.changeTempo(-1000);
      assert loop.tempo() == Loop.MIN_TEMPO;
   }

   private static void checkSaving() throws IOException {
      Loop loop = new Loop();
      loop.changeTempo(Loop.TEMPO_STEP * 3);
      loop.setClick(false);
      loop.add(layer(32, 48));
      Loop.Take take = new Loop.Take(Loop.STEPS_PER_BAR);
      take.press(64, 12);
      take.release(64, 20);
      loop.add(take.finish(80));
      loop.toggleMute(1);
      Loop back = Loop.decode(loop.encode());
      assert back.encode().equals(loop.encode()) : back.encode();
      assert back.tempo() == 108 && !back.click() && back.layers().size() == 2;
      assert back.layers().get(1).muted() && back.layers().get(1).program == 80;
      assert back.layers().get(1).notes().get(0).step == 28 && back.layers().get(1).notes().get(0).length == 8 : "rings into bar 1";

      Loop odd = Loop.decode("tempo=999\nclick=1\nlayer=0,0,0.4.60;99.1.60;3.0.60;x\nlayer=7\nlayer=200,0,0.1.60\nnonsense\n");
      assert odd.tempo() == Loop.MAX_TEMPO && odd.click();
      assert odd.layers().size() == 1 && odd.layers().get(0).notes().size() == 1 : "bad notes and layers are skipped";

      File file = File.createTempFile("synthesizer-loop", ".txt");
      try {
         loop.save(file);
         assert Loop.load(file).encode().equals(loop.encode());
      } finally {
         file.delete();
      }
      assert Loop.load(new File(file.getPath() + ".missing")).layers().isEmpty() : "no file is an empty loop";
   }
}
