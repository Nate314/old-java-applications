package main;

import java.awt.Point;
import java.util.List;

/** Checks how the start menu sorts the apps into kinds and flows its headings and rows into columns. */
public final class StartMenuCheck {
   public static void main(String[] args) {
      String[] apps = {"BibleDownloader", "CircleSpawner", "Clock", "DinoGame", "FlappyBird", "GroupCreator", "Minesweeper",
         "MusicTheoryExpert", "NumberGuess", Launcher.PAINT, "RubiksCube2D", "ScreenSaver", "SimpleAlgorithms", "SpriteFloatyStars",
         "StockChecker", "SynthesizerKeyboard", "TankGame", "UltimateTicTacToe"};
      int[] perKind = new int[StartMenu.KINDS.length];
      for (String app : apps) {
         String kind = StartMenu.kind(app);
         check(!kind.equals("Other"), app + " is listed under " + kind);
         perKind[List.of(StartMenu.KINDS).indexOf(kind)]++;
      }
      check(perKind[0] == 8 && perKind[1] == 2 && perKind[2] == 3 && perKind[3] == 5, "8 games, 2 music, 3 toys, 5 tools");
      check(StartMenu.kind("MyGame").equals("Other"), "a jar added by hand goes under Other");

      // The 18 apps under their four headings, as the menu lists them.
      boolean[] entries = new boolean[22];
      for (int at : new int[] {0, 9, 12, 16}) {
         entries[at] = true;
      }
      for (int room : new int[] {600, 560, 417, 300, 200, 100, 58}) {
         List<Point> places = StartMenu.flow(entries, room);
         check(places.size() == entries.length, "every entry gets a place, room " + room);
         for (int i = 0; i < places.size(); i++) {
            Point p = places.get(i);
            int bottom = p.y + (entries[i] ? StartMenu.HEADING : StartMenu.ROW);
            check(bottom <= room || p.y == 0, "entry " + i + " fits its column, room " + room);
            if (entries[i] && i > 0) {
               int end = i + 1;
               while (end < entries.length && !entries[end]) {
                  end++;
               }
               boolean fits = StartMenu.HEADING + (end - i - 1) * StartMenu.ROW <= room;
               check(!fits || places.get(end - 1).x == p.x, "a kind that fits a column is not split, entry " + i + ", room " + room);
            }
            if (entries[i]) {
               check(i + 1 < places.size() && places.get(i + 1).x == p.x, "heading " + i + " keeps its first app in its column, room " + room);
            }
            if (i > 0) {
               Point before = places.get(i - 1);
               check(p.x == before.x ? p.y > before.y : p.x == before.x + 1 && p.y == 0, "entries run down, then on to the next column, room " + room);
            }
         }
      }
      List<Point> roomy = StartMenu.flow(entries, 417);
      check(roomy.get(entries.length - 1).x == 1, "at 900 x 600 the menu takes two columns");
      System.out.println("StartMenuCheck: all checks passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
      System.out.println("ok - " + what);
   }
}
