package screensaver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * The fish that live in the Aquarium saver, kept between runs: three move in the first time it is shown, then one
 * more on each later day it is shown, until the tank is full. Each fish keeps its name, the day it moved in and how
 * many flakes it has eaten, which is how it grows.
 */
class Tank {
   static final int FIRST_FISH = 3;
   static final int MAX_FISH = 12;
   static final String[] SPECIES = {"clownfish", "blue tang", "neon tetra", "angelfish", "goldfish", "yellow tang"};
   static final String[] NAMES = {"Pip", "Marlow", "Juniper", "Biscuit", "Coral", "Ziggy", "Olive", "Pebble", "Rocket",
      "Mabel", "Sunny", "Waffles", "Indigo", "Clementine", "Moss", "Tango", "Noodle", "Saffron", "Pickle", "Comet",
      "Basil", "Kiwi", "Domino", "Marigold"};

   /** One resident. */
   static class Fish {
      final int species;
      final String name;
      /** The day number (see dayNumber) it moved in. */
      final int born;
      int meals;

      Fish(int species, String name, int born, int meals) {
         this.species = species;
         this.name = name;
         this.born = born;
         this.meals = meals;
      }

      String title() {
         return this.name + " the " + SPECIES[this.species];
      }
   }

   final List<Fish> fish = new ArrayList<>();
   /** The day number the last fish moved in, or -1 before the tank was first shown. */
   int lastArrival = -1;

   /** Days since 1 January 1970 for the date's local year, month and day (so it turns over at local midnight). */
   static int dayNumber(Date date) {
      return civilDays(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
   }

   /** Days from 1970-01-01 to this date (a year after 1 AD) in the Gregorian calendar. */
   static int civilDays(int year, int month, int day) {
      int y = month <= 2 ? year - 1 : year;
      int era = y / 400;
      int yearOfEra = y - era * 400;
      int dayOfYear = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
      int dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
      return era * 146097 + dayOfEra - 719468;
   }

   /**
    * Moves fish in for a look on this day: the first fish on the very first look, one on any later day, none if a
    * fish already came today (or the clock went back) or the tank is full. Returns the newcomers.
    */
   List<Fish> arrive(int today, Random random) {
      List<Fish> newcomers = new ArrayList<>();
      if (this.lastArrival >= 0 && today <= this.lastArrival) {
         return newcomers;
      }
      int count = this.lastArrival < 0 && this.fish.isEmpty() ? FIRST_FISH : 1;
      for (int i = 0; i < count && this.fish.size() < MAX_FISH; i++) {
         Fish fish = new Fish(this.rarestSpecies(random), this.freshName(random), today, 0);
         this.fish.add(fish);
         newcomers.add(fish);
      }
      this.lastArrival = today;
      return newcomers;
   }

   /** The species with the fewest fish so far, so the tank stays varied; ties are broken at random. */
   private int rarestSpecies(Random random) {
      int[] counts = new int[SPECIES.length];
      for (Fish fish : this.fish) {
         counts[fish.species]++;
      }
      int fewest = Integer.MAX_VALUE;
      for (int count : counts) {
         fewest = Math.min(fewest, count);
      }
      List<Integer> picks = new ArrayList<>();
      for (int s = 0; s < counts.length; s++) {
         if (counts[s] == fewest) {
            picks.add(s);
         }
      }
      return picks.get(random.nextInt(picks.size()));
   }

   private String freshName(Random random) {
      List<String> free = new ArrayList<>();
      for (String name : NAMES) {
         if (this.named(name) == null) {
            free.add(name);
         }
      }
      return free.isEmpty() ? NAMES[random.nextInt(NAMES.length)] : free.get(random.nextInt(free.size()));
   }

   Fish named(String name) {
      for (Fish fish : this.fish) {
         if (fish.name.equals(name)) {
            return fish;
         }
      }
      return null;
   }

   /** How big a fish has grown, from 0 (just moved in) to 1: a little each day and each flake, full after a fortnight. */
   static double growth(Fish fish, int today) {
      return Math.max(0, Math.min(1, (today - fish.born + fish.meals / 4.0) / 14.0));
   }

   /** Reads one saved line, already split on spaces; returns false if it is not a tank line. */
   boolean read(String[] parts) {
      try {
         if (parts[0].equals("fishday") && parts.length == 2) {
            this.lastArrival = Integer.parseInt(parts[1]);
            return true;
         }
         if (parts[0].equals("fish") && parts.length == 5) {
            int species = Integer.parseInt(parts[1]);
            if (species >= 0 && species < SPECIES.length && this.fish.size() < MAX_FISH && this.named(parts[4]) == null) {
               this.fish.add(new Fish(species, parts[4], Integer.parseInt(parts[2]), Math.max(0, Integer.parseInt(parts[3]))));
            }
            return true;
         }
      } catch (NumberFormatException e) {
         return true;
      }
      return false;
   }

   /** The lines read reads back: "fishday 20394" and "fish 0 20394 7 Pip" (species, day, meals, name). */
   String lines() {
      StringBuilder out = new StringBuilder();
      if (this.lastArrival >= 0) {
         out.append("fishday ").append(this.lastArrival).append('\n');
      }
      for (Fish fish : this.fish) {
         out.append("fish ").append(fish.species).append(' ').append(fish.born).append(' ').append(fish.meals).append(' ')
            .append(fish.name).append('\n');
      }
      return out.toString();
   }
}
