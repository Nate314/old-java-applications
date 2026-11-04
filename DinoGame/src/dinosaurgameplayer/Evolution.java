package dinosaurgameplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Breeds better bots. A generation is 20 dinosaurs, each run by its own Brain, racing the same course at once (a
 * seeded Runner each, so they all meet the same obstacles). When the last one crashes, or the ones left cross the
 * finish line, the five that ran furthest go on unchanged, 13 children of the best eight join them, and two new
 * random ones keep fresh ideas coming. Each generation runs a new course. No drawing here; EvolveView draws it.
 */
final class Evolution {
   static final int SIZE = 20;
   static final int KEEP = 5;
   static final int PARENTS = 8;
   static final int NEWCOMERS = 2;
   /** A run that reaches this score has finished the course and stops there. */
   static final int FINISH = 3000;
   /** Steps (1.5 s at normal speed) the result of a generation shows before the next starts. */
   static final int PAUSE_STEPS = 90;
   /** Generations of best scores kept for the chart and the saved file. */
   static final int HISTORY = 60;

   /** One dinosaur of a generation: its brain at the keys of its own runner. */
   static final class Dino {
      final Brain brain;
      final Runner runner;
      private double jumpLeft;
      private double duckLeft;
      private boolean finished;
      private int ducks;

      Dino(Brain brain, long seed) {
         this.brain = brain;
         this.runner = new Runner(seed);
         this.runner.restart();
      }

      boolean running() {
         return this.runner.state() == Runner.State.RUNNING && !this.finished;
      }

      boolean finished() {
         return this.finished;
      }

      int score() {
         return Math.min(FINISH, this.runner.score());
      }

      /** Times it ducked this run. */
      int ducks() {
         return this.ducks;
      }

      /** The original bot's rule with this brain's numbers, then one game step. */
      void step() {
         if (!this.running()) {
            return;
         }
         Brain b = this.brain;
         if (this.jumpLeft > 0) {
            this.jumpLeft -= Runner.STEP_MS;
            if (this.jumpLeft <= 0) {
               this.runner.release();
            }
         } else if (!this.runner.isJumping() && this.runner.inkAt(b.jumpX, b.jumpY)) {
            if (this.duckLeft > 0) {
               this.duckLeft = 0;
               this.runner.duck(false);
            }
            this.runner.press();
            this.jumpLeft = b.jumpMs;
         }
         if (this.duckLeft > 0) {
            this.duckLeft -= Runner.STEP_MS;
            if (this.duckLeft <= 0) {
               this.runner.duck(false);
            }
         } else if (b.ducks() && !this.runner.isJumping() && this.runner.inkAt(b.duckX, b.duckY)) {
            this.runner.duck(true);
            this.duckLeft = b.duckMs;
            this.ducks++;
         }
         this.runner.step();
         if (this.runner.score() >= FINISH) {
            this.finished = true;
         }
      }
   }

   private final Random random;
   private final List<Dino> dinos = new ArrayList<>();
   private final List<Integer> history = new ArrayList<>();
   private int generation;
   private Brain champion;
   private int championScore;
   private int pause;
   private String[] result = new String[0];
   private boolean newRecord;

   /**
    * Starts at generation 1 with the original bot and 19 random brains, or carries on from a saved champion
    * (generation, best score and chart), whose first generation is the champion, 14 of its children and five
    * random brains.
    */
   Evolution(long seed, Brain champion, int generation, int championScore, List<Integer> history) {
      this.random = new Random(seed);
      List<Brain> first = new ArrayList<>();
      if (champion == null) {
         first.add(Brain.original());
      } else {
         this.champion = champion;
         this.championScore = championScore;
         this.generation = generation;
         this.history.addAll(history.subList(Math.max(0, history.size() - HISTORY), history.size()));
         first.add(champion);
         for (int i = 0; i < 14; i++) {
            first.add(champion.child(champion, this.random));
         }
      }
      while (first.size() < SIZE) {
         first.add(Brain.random(this.random));
      }
      this.begin(first);
   }

   private void begin(List<Brain> brains) {
      this.generation++;
      this.dinos.clear();
      long course = this.random.nextLong();
      for (Brain b : brains) {
         this.dinos.add(new Dino(b, course));
      }
   }

   /**
    * One step of every dinosaur still running. Returns true at the step a generation ends (its result is ready
    * to save); the next one starts after the pause.
    */
   boolean step() {
      if (this.pause > 0) {
         if (--this.pause == 0) {
            this.breed();
         }
         return false;
      }
      boolean any = false;
      for (Dino d : this.dinos) {
         d.step();
         any |= d.running();
      }
      if (any) {
         return false;
      }
      this.finish();
      return true;
   }

   /** Ranks the generation, keeps the best brain ever and writes the result lines. */
   private void finish() {
      List<Dino> ranked = this.ranked();
      Dino best = ranked.get(0);
      this.newRecord = best.score() > this.championScore;
      if (this.newRecord || this.champion == null) {
         this.champion = best.brain;
         this.championScore = Math.max(this.championScore, best.score());
      }
      this.history.add(best.score());
      if (this.history.size() > HISTORY) {
         this.history.remove(0);
      }
      int finishers = 0;
      int duckers = 0;
      for (Dino d : this.dinos) {
         finishers += d.finished() ? 1 : 0;
         duckers += d.ducks() > 0 ? 1 : 0;
      }
      String first = "Generation " + this.generation + ": the best ran " + best.score()
         + (this.newRecord ? ", a new record." : ". The record is " + this.championScore + ".");
      String second = finishers > 0 ? finishers + " of " + SIZE + " crossed the finish line at " + FINISH + "."
         : duckers + " of " + SIZE + " ducked at least once. The best five go on, and breed the next "
            + (SIZE - KEEP - NEWCOMERS) + ".";
      this.result = new String[] { first, second };
      this.pause = PAUSE_STEPS;
   }

   /** The next generation: the best five as they are, 13 children of the best eight, two newcomers. */
   private void breed() {
      List<Dino> ranked = this.ranked();
      List<Brain> next = new ArrayList<>();
      for (int i = 0; i < KEEP; i++) {
         next.add(ranked.get(i).brain);
      }
      while (next.size() < SIZE - NEWCOMERS) {
         Brain a = ranked.get(this.random.nextInt(PARENTS)).brain;
         Brain b = ranked.get(this.random.nextInt(PARENTS)).brain;
         next.add(a.child(b, this.random));
      }
      while (next.size() < SIZE) {
         next.add(Brain.random(this.random));
      }
      this.begin(next);
   }

   /** Furthest first; on a tie the one earlier in the generation (the older brain) first. */
   private List<Dino> ranked() {
      List<Dino> ranked = new ArrayList<>(this.dinos);
      Collections.sort(ranked, (a, b) -> Integer.compare(b.score(), a.score()));
      return ranked;
   }

   // ---- what is on screen ----

   List<Dino> dinos() {
      return this.dinos;
   }

   /** A dinosaur still running (its runner shows the course where the race is), or else the furthest. */
   Dino leader() {
      Dino furthest = this.dinos.get(0);
      for (Dino d : this.dinos) {
         if (d.running()) {
            return d;
         }
         if (d.score() > furthest.score()) {
            furthest = d;
         }
      }
      return furthest;
   }

   int running() {
      int n = 0;
      for (Dino d : this.dinos) {
         n += d.running() ? 1 : 0;
      }
      return n;
   }

   int generation() {
      return this.generation;
   }

   Brain champion() {
      return this.champion;
   }

   int championScore() {
      return this.championScore;
   }

   List<Integer> history() {
      return this.history;
   }

   /** Between generations: the two lines saying how the last one went; otherwise null. */
   String[] result() {
      return this.pause > 0 ? this.result : null;
   }

   boolean newRecord() {
      return this.newRecord;
   }
}
