package main;

/** Checks the Restart and Shut Down timeline: the fade to black, the logo, and the fade back to the desktop. */
public final class PowerCheck {
   public static void main(String[] args) {
      int boot = Power.DARKEN + Power.OFF;
      int end = Power.length(true);
      check(Power.shade(0, true, false) == 0, "the desktop starts uncovered");
      check(Power.shade(Power.DARKEN / 2, true, false) > 127, "it darkens fast, then slows");
      check(Power.shade(Power.DARKEN, false, false) == 255, "Shut Down reaches black");
      check(Power.length(false) == boot, "Shut Down ends once black has held");
      check(Power.logo(boot - 1, true, false) == 0, "no logo before the screen is black");
      check(Power.logo(boot + Power.LOGO_IN, true, false) == 255, "the logo shows fully");
      check(Power.logo(boot + Power.LOGO_IN, false, false) == 0, "Shut Down shows no logo");
      check(Power.logo(boot + Power.LOGO_IN + Power.LOGO_HOLD + Power.LOGO_OUT, true, false) == 0, "the logo fades before the desktop returns");
      check(Power.shade(end - Power.LIGHTEN - 1, true, false) == 255, "black holds until the logo is gone");
      check(Power.shade(end, true, false) == 0, "Restart ends on the desktop");
      check(Power.shade(1, true, true) == 255, "reduced motion: black at once");
      check(Power.logo(boot, true, true) == 255, "reduced motion: the logo at once");
      check(Power.shade(end - Power.LIGHTEN, true, true) == 0, "reduced motion: the desktop at once");
      System.out.println("PowerCheck: all checks passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
      System.out.println("ok - " + what);
   }
}
