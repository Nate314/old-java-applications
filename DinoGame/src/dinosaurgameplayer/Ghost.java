package dinosaurgameplayer;

import java.util.List;

/**
 * Your best run, kept so you can race it: its course and every key you pressed, by step. A Runner is the same game
 * every time for the same course and keys, so playing the keys back on a new Runner of that course runs the same run,
 * jump for jump, and crashes where it crashed.
 */
final class Ghost {
   final long course;
   final int score;
   private final int[] inputs;
   private Runner runner;
   private int next;

   Ghost(long course, int score, int[] inputs) {
      this.course = course;
      this.score = score;
      this.inputs = inputs;
   }

   /** The run runner just played (call it once the run has crashed). */
   static Ghost of(Runner runner) {
      List<Integer> in = runner.inputs();
      int[] inputs = new int[in.size()];
      for (int i = 0; i < inputs.length; i++) {
         inputs[i] = in.get(i);
      }
      return new Ghost(runner.course(), runner.score(), inputs);
   }

   /** Back to the start of the run, running. */
   void start() {
      this.runner = new Runner(this.course);
      this.runner.restart();
      this.next = 0;
   }

   /** One step of the run, with the keys pressed before it (nothing once it has crashed). */
   void step() {
      while (this.next < this.inputs.length && this.inputs[this.next] / 4 == this.runner.steps()) {
         int input = this.inputs[this.next++] % 4;
         if (input == Runner.PRESS) {
            this.runner.press();
         } else if (input == Runner.RELEASE) {
            this.runner.release();
         } else {
            this.runner.duck(input == Runner.DUCK);
         }
      }
      this.runner.step();
   }

   /** The run being played back, or null before start. */
   Runner runner() {
      return this.runner;
   }

   String encode() {
      StringBuilder s = new StringBuilder().append(this.course).append(',').append(this.score);
      for (int n : this.inputs) {
         s.append(',').append(n);
      }
      return s.toString();
   }

   /** The ghost encode wrote, or null when the text is damaged. */
   static Ghost decode(String text) {
      String[] n = text.trim().split(",");
      if (n.length < 2) {
         return null;
      }
      try {
         int[] inputs = new int[n.length - 2];
         for (int i = 0; i < inputs.length; i++) {
            inputs[i] = Integer.parseInt(n[i + 2].trim());
            if (inputs[i] < 0 || i > 0 && inputs[i] / 4 < inputs[i - 1] / 4) {
               return null;
            }
         }
         int score = Integer.parseInt(n[1].trim());
         return score < 0 ? null : new Ghost(Long.parseLong(n[0].trim()), score, inputs);
      } catch (NumberFormatException e) {
         return null;
      }
   }
}
